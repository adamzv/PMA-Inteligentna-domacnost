from flask import Flask, render_template, request, jsonify, redirect
from apscheduler.schedulers.background import BackgroundScheduler
from peewee import DoesNotExist

from models import db, Dht, Senzor
from helpers import poslat_notifikaciu, vrat_cislo_dveri
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
logger = logging.getLogger('app.py')
logger.setLevel(logging.INFO)

shFormatter = logging.Formatter('%(asctime)-25s %(name)-25s ' + ' %(levelname)-7s %(message)s')

sh = logging.StreamHandler()
sh.setFormatter(shFormatter)
sh.setLevel(logging.DEBUG)

logger.addHandler(sh)

imf_push_api = ''
imf_push_appguid = ''

iot_client = None

device_id = 'int_domacnost1'
device_type = 'ESP8266'

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
                'org': iot_org,
                'auth-key': iot_auth_key,
                'auth-token': iot_auth_token,
                'clean-session': 'true'
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
                'org': iot_org,
                'auth-key': iot_auth_key,
                'auth-token': iot_auth_token,
                'clean-session': 'true'
            }
            iot_client = ibmiotf.application.Client(options, logger.handlers)
        except Exception as e:
            print(e)
        postgresql_uri = vcap['compose-for-postgresql'][0]['credentials']['uri']
        db.init('compose', dsn=postgresql_uri)


def event_callback(event):
    payload = json.loads(event.payload)
    if event.event == 'dht':
        temp = float(payload['d']['t'])
        humidity = float(payload['d']['h'])
        # Zapíše vlhkosť a teplotu do databázy
        Dht.create(device_id='int_domacnost1', teplota=temp, vlhkost=humidity)
        print(f'T:{temp} H:{humidity}')
    if event.event == 'led':
        print(payload)
        # TODO
    if event.event == 'pir':
        print(payload)
        sprava = 'Detegovaný pohyb ' + str(datetime.now())[0:16]
        poslat_notifikaciu(imf_push_api, imf_push_appguid, sprava)
    if event.event == 'servo':
        print(payload)
        # TODO


# V debug móde sa background task vykoná dvakrát,
# preto som dočasne nastavil v app.run use_reloader na False
def dht_background_command():
    command = {'senzor': 'dht'}
    if iot_client is not None:
        iot_client.connect()
        iot_client.publishCommand(device_type, 'int_domacnost1', 'dht', 'json', command)


if iot_client is not None:
    iot_client.setKeepAliveInterval(60)
    iot_client.connect()
    iot_client.deviceEventCallback = event_callback

    iot_client.subscribeToDeviceCommands()

    iot_client.subscribeToDeviceEvents(event='dht')
    iot_client.subscribeToDeviceEvents(event='svetlo')
    iot_client.subscribeToDeviceEvents(event='pir')
    iot_client.subscribeToDeviceEvents(event='servo')

# BackgroundScheduler, ktorý každých 10 minút pošle command
# na získanie aktuálnych hodnôt teploty a vlhkosti
scheduler = BackgroundScheduler()
scheduler.add_job(func=dht_background_command, trigger='interval', seconds=600)
scheduler.start()

# On IBM Cloud Cloud Foundry, get the port number from the environment variable PORT
# When running this app on the local machine, default the port to 8000
port = int(os.getenv('PORT', 8000))

#test
resp = ""

@app.route('/')
def root():
    # vyberie posledné, aktuálne, hodnoty teploty a vlhkosti
    posl_hodnota = Dht.select().order_by(Dht.id.desc()).get()
    return render_template('index.html', cas=posl_hodnota.cas, temp=posl_hodnota.teplota, humidity=posl_hodnota.vlhkost, resp=resp)


# Endpoint na zistenie poslednej nameranej hodnoty teploty a vlhkosti
@app.route('/api/dht', methods=['GET'])
def temp_route():
    posl_hodnota = Dht.select().order_by(Dht.id.desc()).get()
    return jsonify(responseCode=200, cas=posl_hodnota.cas, teplota=posl_hodnota.teplota, vlhkost=posl_hodnota.vlhkost)


# Endpoint na ovládanie svetla
# {"senzor": "led", "miestnost": cislo_miestnosti, "status": "on/off"}
@app.route('/api/svetlo', methods=['POST'])
def svetlo_route():
    # nastav silent na True, v prípade, že zlyhá json parse vráti None
    response = request.get_json(silent=True)

    if response is not None:
        if iot_client is None:
            return jsonify(responseCode=503, status='watson iot neodpovedá')
        else:
            if response['senzor'] == 'led':
                try:
                    led = Senzor.get(Senzor.device_id == device_id,
                                     Senzor.typ_senzoru == 'led', Senzor.miestnost == int(response['miestnost']))
                except DoesNotExist:
                    return jsonify(responseCode=400, status=f'miestnosť {response["miestnost"]} neexistuje')
                else:
                    if (response['status'] == 'on' and led.status == 'off') or (response['status'] == 'off' and led.status == 'on'):
                        led.status = response['status']
                        led.save()
                        iot_client.connect()
                        iot_client.publishCommand(device_type, device_id, 'svetlo', 'json', response)
                    else:
                        return jsonify(responseCode=400, status=f'požiadavka: {response["status"]}, stav led: {led.status}')
            return jsonify(responseCode=200, status='ok')
    else:
        return jsonify(responseCode=400, status='zlý request')


# Endpoint na nastavenie alarmu
# {"senzor": "pir", "status": "on/off"}
@app.route('/api/alarm', methods=['POST'])
def alarm_route():
    response = request.get_json(silent=True)
    global resp
    resp = response
    print(response)
    if response is not None:
        if iot_client is None:
            return jsonify(responseCode=503, status='watson iot neodpovedá')
        else:
            if response['senzor'] == 'pir':
                try:
                    pir = Senzor.get(Senzor.device_id == device_id, Senzor.typ_senzoru == 'pir')
                except DoesNotExist:
                    return jsonify(responseCode=400, status='pir senzor neexistuje')
                else:
                    if (response['status'] == 'on' and pir.status == 'off') or (response['status'] == 'off' and pir.status == 'on'):
                        pir.status = response['status']
                        pir.save()
                        iot_client.connect()
                        iot_client.publishCommand(device_type, device_id, 'pir', 'json', response)
                    else:
                        return jsonify(responseCode=400, status=f'požiadavka: {response["status"]}, stav pir: {pir.status}')
            return jsonify(responseCode=200, status='ok')
    else:
        return jsonify(responseCode=400, status='zlý request')


# Endpoint slúžiaci na odomknutie/zamknutie dverí (vchodové, garážové)
# {"senzor": "servo", "hodnota": "dvere/garaz", "status": "on/off"}
@app.route('/api/dvere', methods=['POST'])
def dvere_route():
    response = request.get_json(silent=True)
    if response is not None:
        if iot_client is None:
            return jsonify(responseCode=503, status='watson iot neodpovedá')
        else:
            try:
                dvere_int = vrat_cislo_dveri(response['hodnota'])
                dvere = Senzor.get(Senzor.device_id == device_id, Senzor.typ_senzoru == 'servo', Senzor.miestnost == dvere_int)
            except DoesNotExist:
                return jsonify(responseCode=400, status=f'niektorá hodnota je zlá: {response["senzor"]}, {response["hodnota"]}')
            else:
                if (response['status'] == 'on' and dvere.status == 'off') or (response['status'] == 'off' and dvere.status == 'on'):
                    dvere.status = response['status']
                    dvere.save()
                    iot_client.connect()
                    iot_client.publishCommand(device_type, device_id, 'servo', 'json', response)
                else:
                    return jsonify(responseCode=400, status=f'požiadavka: {response["status"]}, stav dverí: {dvere.status}')
            return jsonify(responseCode=200, status='ok')
    else:
        return jsonify(responseCode=400, status='zlý request')


# Dočasný endpoint slúžiaci na testovanie notifikácii
@app.route('/api/notifikacia', methods=['POST'])
def app_notifikacia():
    sprava = request.form.get('sprava')
    heslo = request.form.get('heslo')
    if heslo == device_id:  # bezpečné heslo :)
        status_code = poslat_notifikaciu(imf_push_api, imf_push_appguid, sprava)
        return jsonify(status_code)
    else:
        return jsonify(400)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=port, debug=True, use_reloader=False)
