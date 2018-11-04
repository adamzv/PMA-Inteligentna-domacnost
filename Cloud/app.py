from flask import Flask, render_template, request, jsonify, redirect
from apscheduler.schedulers.background import BackgroundScheduler
from models import db, Dht, Senzor
import atexit
import os
import json
import requests
import logging

import ibmiotf.application

app = Flask(__name__, static_url_path='')

# Pokiaľ Watson IoT klientovi neposkytnete vlastný log handler, klient si vytvorí vlastný,
# ktorý bude vytvárať .log súbory každého pripojenia/odpojenia.
# Preto som si vytvoril vlastný log handler, ktorý nebude vytvárať súbory, pretože sú momentálne
# nepotrebné.
logger = logging.getLogger("app.py")
logger.setLevel(logging.INFO)

shFormatter = logging.Formatter('%(asctime)-25s %(name)-25s ' + ' %(levelname)-7s %(message)s')

sh = logging.StreamHandler()
sh.setFormatter(shFormatter)
sh.setLevel(logging.DEBUG)

logger.addHandler(sh)

client = None

imf_push_api = ""
imf_push_appguid = ""

iot_client = None

device_id = "int_domacnost1"

if 'VCAP_SERVICES' in os.environ:
    vcap = json.loads(os.getenv('VCAP_SERVICES'))
    print('Found VCAP_SERVICES')
    if 'iotf-service' in vcap:
        iot_creds = vcap['iotf-service'][0]['credentials']
        iot_org = iot_creds['org']
        iot_auth_key = iot_creds['apiKey']
        iot_auth_token = iot_creds['apiToken']
    if 'imfpush' in vcap:
        imf_push_api = vcap['imfpush'][0]['credentials']['apikey']
        imf_push_appguid = vcap['imfpush'][0]['credentials']['appGuid']
        try:
            options = {
                "org": iot_org,
                "auth-key": iot_auth_key,
                "auth-token": iot_auth_token,
                "clean-session": "true"
            }
            iot_client = ibmiotf.application.Client(options, logHandlers=logger.handlers)
        except Exception as e:
            print(e)
    if 'compose-for-postgresql' in vcap:
        postgresql_uri = vcap['compose-for-postgresql'][0]['credentials']['uri']
        db.init('compose', dsn=postgresql_uri)
elif os.path.isfile('vcap-local.json'):
    with open('vcap-local.json') as f:
        vcap = json.load(f)['VCAP_SERVICES']
        print('Found local VCAP_SERVICES')
        iot_creds = vcap['iotf-service'][0]['credentials']
        iot_org = iot_creds['org']
        iot_auth_key = iot_creds['apiKey']
        iot_auth_token = iot_creds['apiToken']

        imf_push_api = vcap['imfpush'][0]['credentials']['apikey']
        imf_push_appguid = vcap['imfpush'][0]['credentials']['appGuid']
        try:
            options = {
                "org": iot_org,
                "auth-key": iot_auth_key,
                "auth-token": iot_auth_token,
                "clean-session": "true"
            }
            iot_client = ibmiotf.application.Client(options, logger.handlers)
        except Exception as e:
            print(e)
        postgresql_uri = vcap['compose-for-postgresql'][0]['credentials']['uri']
        db.init('compose', dsn=postgresql_uri)


def command_callback(event):
    payload = json.loads(event.payload)
    if event.event == "dht":
        temp = float(payload["d"]["t"])
        humidity = float(payload["d"]["h"])

        row = Dht.create(device_id="int_domacnost1", teplota=temp, vlhkost=humidity)
        print(f"T:{temp} H:{humidity}")


# V debug móde sa background task vykoná dvakrát,
# preto som dočasne nastavil v app.run use_reloader na False
def dht_background_task():
    command = {"senzor": "dht"}
    if iot_client is not None:
        iot_client.connect()
        iot_client.publishCommand("ESP8266", "int_domacnost1", "dht", "json", command)


if iot_client is not None:
    iot_client.setKeepAliveInterval(60)
    iot_client.connect()
    iot_client.deviceEventCallback = command_callback
    i = iot_client.subscribeToDeviceCommands()
    iot_client.subscribeToDeviceEvents(event="dht")


scheduler = BackgroundScheduler()
scheduler.add_job(func=dht_background_task, trigger="interval", seconds=600)
scheduler.start()

# On IBM Cloud Cloud Foundry, get the port number from the environment variable PORT
# When running this app on the local machine, default the port to 8000
port = int(os.getenv('PORT', 8000))


@app.route('/')
def root():
    db.connect()
    # vyberie posledné, aktuálne, hodnoty teploty a vlhkosti
    posl_hodnota = Dht.select().order_by(Dht.id.desc()).get()
    db.close()
    return render_template('index.html', cas=posl_hodnota.cas, temp=posl_hodnota.teplota, humidity=posl_hodnota.vlhkost)


# Endpoint na ovládanie svetla
@app.route('/api/svetlo', methods=['POST'])
def svetlo_route():
    # {"senzor": "led", "miestnost": cislo_miestnosti, "status": "on/off"}
    # nastav silent na True, v prípade, že zlyhá json parse vráti None
    command = request.get_json(silent=True)

    if command is not None:
        if iot_client is None:
            return jsonify(responseCode=503, status="watson iot neodpovedá")
        else:
            iot_client.connect()
            iot_client.publishCommand("ESP8266", "int_domacnost1", "svetlo", "json", command)
        return jsonify(responseCode=200, status="ok")
    else:
        return jsonify(responseCode=400, status="zlý request")


# Endpoint na zistenie poslednej nameranej hodnoty teploty a vlhkosti
@app.route('/api/dht', methods=['GET'])
def temp_route():
    db.connect()
    posl_hodnota = Dht.select().order_by(Dht.id.desc()).get()
    db.close()
    return jsonify(responseCode=200, cas=posl_hodnota.cas, teplota=posl_hodnota.teplota, vlhkost=posl_hodnota.vlhkost)


# Endpoint slúžiaci na odomknutie/zamknutie dverí (vchodové, garážové)
@app.route('/api/dvere', methods=['POST'])
def dvere_route():
    return jsonify("TODO")


@app.route('/api/notifikacia', methods=['POST'])
def app_notifikacia():
    sprava = request.form.get("sprava")
    heslo = request.form.get("heslo")
    if heslo == "piroskovci":
        headers = {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Accept': 'application/json',
        }
        payload = {
            'grant_type': 'urn:ibm:params:oauth:grant-type:apikey',
            'apikey': imf_push_api,
        }
        response = requests.post('https://iam.bluemix.net/identity/token', headers=headers, data=payload, verify=False)
        access = response.json()['access_token']

        push_headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'Accept-Language': 'en-US',
            'Authorization': access,
        }
        push_payload = {
            'message': {
                'alert': sprava
            },
        }
        push_notification = requests.post(f'http://imfpush.eu-de.bluemix.net/imfpush/v1/apps/{imf_push_appguid}/messages',
                                          headers=push_headers, data=json.dumps(push_payload), verify=False)
        return jsonify(push_notification.status_code)
    else:
        return jsonify(400)


@atexit.register
def shutdown():
    if client:
        client.disconnect()


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=port, debug=True, use_reloader=False)
