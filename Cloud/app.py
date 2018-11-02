from datetime import datetime
from cloudant import Cloudant
from flask import Flask, render_template, request, jsonify, redirect
from apscheduler.schedulers.background import BackgroundScheduler
import atexit
import os
import json
import requests

import ibmiotf.application

app = Flask(__name__, static_url_path='')

db_name = 'iot'
client = None
db = None

imf_push_api = ""
iot_client = None

if 'VCAP_SERVICES' in os.environ:
    vcap = json.loads(os.getenv('VCAP_SERVICES'))
    print('Found VCAP_SERVICES')
    if 'cloudantNoSQLDB' in vcap:
        creds = vcap['cloudantNoSQLDB'][0]['credentials']
        user = creds['username']
        password = creds['password']
        url = 'https://' + creds['host']
        client = Cloudant(user, password, url=url, connect=True)
        db = client.create_database(db_name, throw_on_exists=False)
    if 'iotf-service' in vcap:
        iot_creds = vcap['iotf-service'][0]['credentials']
        iot_org = iot_creds['org']
        iot_auth_key = iot_creds['apiKey']
        iot_auth_token = iot_creds['apiToken']
    if 'imfpush' in vcap:
        imf_push_api = vcap['imfpush'][0]['credentials']['apikey']
        try:
            options = {
                "org": iot_org,
                "auth-key": iot_auth_key,
                "auth-token": iot_auth_token,
                "clean-session": "true"
            }
            iot_client = ibmiotf.application.Client(options)
        except Exception as e:
            print(e)
elif "CLOUDANT_URL" in os.environ:
    client = Cloudant(os.environ['CLOUDANT_USERNAME'], os.environ['CLOUDANT_PASSWORD'], url=os.environ['CLOUDANT_URL'],
                      connect=True)
    db = client.create_database(db_name, throw_on_exists=False)
elif os.path.isfile('vcap-local.json'):
    with open('vcap-local.json') as f:
        vcap = json.load(f)
        print('Found local VCAP_SERVICES')
        creds = vcap['services']['cloudantNoSQLDB'][0]['credentials']
        user = creds['username']
        password = creds['password']
        url = 'https://' + creds['host']
        client = Cloudant(user, password, url=url, connect=True)
        db = client.create_database(db_name, throw_on_exists=False)

        iot_creds = vcap['services']['iotf-service'][0]
        iot_org = iot_creds['org']
        iot_auth_key = iot_creds['apiKey']
        iot_auth_token = iot_creds['apiToken']

        imf_push_api = vcap['services']['imfpush'][0]['credentials']['apikey']
        try:
            options = {
                "org": iot_org,
                "auth-key": iot_auth_key,
                "auth-token": iot_auth_token,
                "clean-session": "true"
            }
            iot_client = ibmiotf.application.Client(options)
        except Exception as e:
            print(e)

cas = ""
temp = 0
humidity = 0


def command_callback(event):
    payload = json.loads(event.payload)
    if event.event == "dht":
        global temp
        global humidity
        temp = int(payload["d"]["t"])
        humidity = int(payload["d"]["h"])

        jsonDocument = {
            "cas": cas,
            "teplota": temp,
            "vlhkost": humidity
        }
        novyDokument = db.create_document(jsonDocument)
        print(f"T:{temp} H:{humidity}")


# V debug móde sa background task vykoná dvakrát,
# preto som dočasne nastavil v app.run use_reloader na False
def dht_background_task():
    global cas
    cas = str(datetime.now())
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

temp = 0
humidity = 0


@app.route('/')
def root():
    return render_template('index.html', cas=cas, temp=temp, humidity=humidity)


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
    return jsonify(responseCode=200, cas=cas, teplota=temp, vlhkost=humidity)


# Endpoint slúžiaci na odomknutie/zamknutie dverí (vchodové, garážové)
@app.route('/api/dvere', methods=['POST'])
def dvere_route():
    return jsonify("TODO")


@app.route('/api/notifikacia', methods=['POST'])
def app_notifikacia():
    sprava = request.form.get("sprava")

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
        'message': {'alert': sprava},
    }
    push_notification = requests.post(***REMOVED***,
                                      headers=push_headers, data=json.dumps(push_payload), verify=False)
    return redirect('/')


@atexit.register
def shutdown():
    if client:
        client.disconnect()


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=port, debug=True, use_reloader=False)
