from cloudant import Cloudant
from flask import Flask, render_template, request, jsonify, redirect
import atexit
import os
import json

import ibmiotf.application

app = Flask(__name__, static_url_path='')

db_name = 'mydb'
client = None
db = None

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

iot_client = None


def command_callback(event):
    payload = json.loads(event.payload)
    if event.event == "temp":
        global temp
        global humidity
        temp = int(payload["d"]["t"])
        humidity = int(payload["d"]["h"])
        print(f"T:{temp} H:{humidity}")


if iot_client is not None:
    iot_client.setKeepAliveInterval(60)
    iot_client.connect()
    iot_client.deviceEventCallback = command_callback
    i = iot_client.subscribeToDeviceCommands()
    iot_client.subscribeToDeviceEvents(event="temp")


# On IBM Cloud Cloud Foundry, get the port number from the environment variable PORT
# When running this app on the local machine, default the port to 8000
port = int(os.getenv('PORT', 8000))

temp = 0
humidity = 0

@app.route('/')
def root():
    return app.send_static_file('index.html')


# Endpoint na ovládanie svetla
# TODO
@app.route('/api/svetlo/', methods=['GET', 'POST'])
def svetlo_route():
    mydata = {"d": {"set": "TODO"}}
    print(mydata)
    iot_client.connect()
    iot_client.publishCommand("ESP8266", "12345", "svetlo", "json", mydata)
    return jsonify("TODO")


# Endpoint na zistenie poslednej nameranej hodnoty teploty a vlhkosti
# TODO
@app.route('/dht', methods=['GET', 'POST'])
def temp_route():
    comm = {"d": {"sensor": "DHT11"}}
    iot_client.publishCommand("ESP8266", "12345", "temp", "json", comm)
    return jsonify(temp=temp, humidity=humidity)


@atexit.register
def shutdown():
    if client:
        client.disconnect()


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=port, debug=True)
