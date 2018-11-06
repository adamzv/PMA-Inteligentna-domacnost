from flask import Flask, render_template, request, jsonify, redirect
from apscheduler.schedulers.background import BackgroundScheduler
from models import db, Dht, Senzor
from helpers import poslat_notifikaciu
from datetime import datetime
import os
import json
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

imf_push_api = ""
imf_push_appguid = ""

iot_client = None

device_id = "int_domacnost1"
device_type = "ESP8266"

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


def event_callback(event):
    payload = json.loads(event.payload)
    if event.event == "dht":
        temp = float(payload["d"]["t"])
        humidity = float(payload["d"]["h"])
        # Zapíše vlhkosť a teplotu do databázy
        Dht.create(device_id="int_domacnost1", teplota=temp, vlhkost=humidity)
        print(f"T:{temp} H:{humidity}")
    if event.event == "led":
        print(payload)
        # TODO
    if event.event == "pir":
        print(payload)
        sprava = "Detegovaný pohyb " + str(datetime.now())[0:16]
        poslat_notifikaciu(imf_push_api, imf_push_appguid, sprava)
    if event.event == "servo":
        print(payload)
        # TODO


# V debug móde sa background task vykoná dvakrát,
# preto som dočasne nastavil v app.run use_reloader na False
def dht_background_command():
    command = {"senzor": "dht"}
    if iot_client is not None:
        iot_client.connect()
        iot_client.publishCommand(device_type, "int_domacnost1", "dht", "json", command)


if iot_client is not None:
    iot_client.setKeepAliveInterval(60)
    iot_client.connect()
    iot_client.deviceEventCallback = event_callback

    iot_client.subscribeToDeviceCommands()

    iot_client.subscribeToDeviceEvents(event="dht")
    iot_client.subscribeToDeviceEvents(event="svetlo")
    iot_client.subscribeToDeviceEvents(event="pir")
    iot_client.subscribeToDeviceEvents(event="servo")

# BackgroundScheduler, ktorý každých 10 minút pošle command
# na získanie aktuálnych hodnôt teploty a vlhkosti
scheduler = BackgroundScheduler()
scheduler.add_job(func=dht_background_command, trigger="interval", seconds=600)
scheduler.start()

# On IBM Cloud Cloud Foundry, get the port number from the environment variable PORT
# When running this app on the local machine, default the port to 8000
port = int(os.getenv('PORT', 8000))


@app.route('/')
def root():
    # vyberie posledné, aktuálne, hodnoty teploty a vlhkosti
    db.connect()
    posl_hodnota = Dht.select().order_by(Dht.id.desc()).get()
    db.close()
    return render_template('index.html', cas=posl_hodnota.cas, temp=posl_hodnota.teplota, humidity=posl_hodnota.vlhkost)


# Endpoint na zistenie poslednej nameranej hodnoty teploty a vlhkosti
@app.route('/api/dht', methods=['GET'])
def temp_route():
    db.connect()
    posl_hodnota = Dht.select().order_by(Dht.id.desc()).get()
    db.close()
    return jsonify(responseCode=200, cas=posl_hodnota.cas, teplota=posl_hodnota.teplota, vlhkost=posl_hodnota.vlhkost)


# Endpoint na ovládanie svetla
@app.route('/api/svetlo', methods=['POST'])
def svetlo_route():
    # {"senzor": "led", "miestnost": cislo_miestnosti, "status": "on/off"}
    # nastav silent na True, v prípade, že zlyhá json parse vráti None
    response = request.get_json(silent=True)

    if response is not None:
        if iot_client is None:
            return jsonify(responseCode=503, status="watson iot neodpovedá")
        else:
            iot_client.connect()
            iot_client.publishCommand(device_type, device_id, "svetlo", "json", response)
        return jsonify(responseCode=200, status="ok")
    else:
        return jsonify(responseCode=400, status="zlý request")


# Endpoint na nastavenie alarmu
@app.route('/api/alarm', methods=['POST'])
def alarm_route():
    # TODO 1 kontrola vstupných dát (on/off), aby sme nezapínali už zapnutý alarm
    # {"senzor": "pir", "status": "on/off"}
    response = request.get_json(silent=True)
    if response is not None:
        if iot_client is None:
            return jsonify(responseCode=503, status="watson iot neodpovedá")
        else:
            # TODO tu bude kontrolovat status zariadenia
            status = response["status"]
            print(status)
            iot_client.connect()
            iot_client.publishCommand(device_type, device_id, "pir", "json", response)
            return jsonify(responseCode=200, status="ok")
    else:
        return jsonify(responseCode=400, status="zlý request")


# Endpoint slúžiaci na odomknutie/zamknutie dverí (vchodové, garážové)
@app.route('/api/dvere', methods=['POST'])
def dvere_route():
    # TODO 2 kontrola vstupných dát (on/off), aby sme neotvárali už otvorené dvere
    # {"senzor": "servo", "hodnota": "dvere/garaz", "status": "on/off"}
    response = request.get_json(silent=True)
    if response is not None:
        if iot_client is None:
            return jsonify(responseCode=503, status="watson iot neodpovedá")
        else:
            print(response["hodnota"], response["status"])
            # TODO dokoncit kontrolu
            iot_client.connect()
            iot_client.publishCommand(device_type, device_id, "servo", "json", response)
            return jsonify(responseCode=200, status="ok")
    else:
        return jsonify(responseCode=400, status="zlý request")


# Dočasný endpoint slúžiaci na testovanie notifikácii
@app.route('/api/notifikacia', methods=['POST'])
def app_notifikacia():
    sprava = request.form.get("sprava")
    heslo = request.form.get("heslo")
    if heslo == "piroskovci":
        status_code = poslat_notifikaciu(imf_push_api, imf_push_appguid, sprava)
        return jsonify(status_code)
    else:
        return jsonify(400)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=port, debug=True, use_reloader=False)
