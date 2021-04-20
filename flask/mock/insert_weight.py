import requests
import json
from datetime import datetime, timedelta
import random

BASE_URL = 'http://127.0.0.1:8080'
URL = BASE_URL + '/mock_insert_weight'


def get_val_good(i):
    if i < 20:
        return 0
    if i % 2000:
        return random.randint(0, 19)
    else:
        return 20


def get_val_bad(i):
    if i < 20:
        return 0
    if i % 100 == 0:
        return random.randint(0, 19)
    else:
        return 20


def loop_and_send(date, sec, get_val):
    for i in range(sec):
        val = get_val(i)
        data = {
            'datetime': date.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
            'value': val
        }
        date += timedelta(seconds=1)

        requests.post(URL, data=json.dumps(data), headers={'Content-Type': 'application/json'})


# 4/18 -- 2hrs, bad sleep
d = datetime(2021, 4, 18, 0, 0, 0, 0)
loop_and_send(d, 7200, get_val_bad)

# 4/19 -- 4hrs, good sleep
d = datetime(2021, 4, 19, 0, 0, 0, 0)
loop_and_send(d, 14400, get_val_good)

# 4/20 -- 5.5hrs, bad sleep
d = datetime(2021, 4, 20, 0, 0, 0, 0)
loop_and_send(d, 19800, get_val_bad)
