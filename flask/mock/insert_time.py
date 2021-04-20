import requests
import json
from datetime import datetime

BASE_URL = 'http://127.0.0.1:8080'
URL = BASE_URL + '/insert_time'


def send(date, time):
    data = {
        'datetime': date.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
        'time': time,
    }

    requests.post(URL, data=json.dumps(data), headers={'Content-Type': 'application/json'})

# 4/18 -- 2hrs, bad sleep
d = datetime(2021, 4, 18, 0, 0, 0, 0)
send(d, 2.0)

# 4/19 -- 4hrs, bad sleep
d = datetime(2021, 4, 19, 0, 0, 0, 0)
send(d, 4.0)

# 4/20 -- 5.5hrs, good sleep
d = datetime(2021, 4, 20, 0, 0, 0, 0)
send(d, 5.5)
