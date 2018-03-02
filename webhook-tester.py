import requests

URL = 'https://canary.discordapp.com/api/webhooks/268966324976353280/egF152k_1n85P4SCoWyBCb6kTm8PaHyoCFbG3w73jR23BwWqNwGosjfHpmlr9opA0fbF'
payload = {
    'username': 'Webhook Tester',
    'attachments': []
}

while True:
    payload['content'] = input('> ')
    requests.post(URL, data=payload)
