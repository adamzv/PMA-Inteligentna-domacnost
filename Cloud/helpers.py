import requests
import json


def poslat_notifikaciu(apikey, appguid, sprava):
    headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Accept': 'application/json',
    }
    payload = {
        'grant_type': 'urn:ibm:params:oauth:grant-type:apikey',
        'apikey': apikey,
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
    push_notification = requests.post(f'http://imfpush.eu-de.bluemix.net/imfpush/v1/apps/{appguid}/messages',
                                      headers=push_headers, data=json.dumps(push_payload), verify=False)
    return push_notification.status_code