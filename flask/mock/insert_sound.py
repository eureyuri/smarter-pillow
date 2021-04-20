import requests
import json
from datetime import datetime, timedelta
import random

BASE_URL = 'http://127.0.0.1:8080'
URL = BASE_URL + '/insert_sound'


def get_val_good(i):
    if i < 100:
        return random.randint(0, 5)
    if i % 500:
        return random.randint(10, 20)
    else:
        return random.randint(5, 10)


def get_val_bad(i):
    if i < 100:
        return random.randint(1, 10)
    if i < 5000:
        return random.randint(10, 20)
    elif i < 5500:
        return random.randint(35, 45)
    elif i < 12000:
        return random.randint(10, 20)
    elif i < 12200:
        return random.randint(40, 45)
    elif i < 13000:
        return random.randint(15, 20)
    elif i < 13600:
        return random.randint(40, 45)
    else:
        return random.randint(5, 10)


def check_is_snore(val):
    return val > 30


def loop_and_send(date, sec, get_val):
    for i in range(sec):
        val = get_val(i)
        is_snore = check_is_snore(val)

        data = {
            'datetime': date.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
            'loudness': val,
            'snore': is_snore
        }
        date += timedelta(seconds=1)

        requests.post(URL, data=json.dumps(data), headers={'Content-Type': 'application/json'})


# 4/18 -- 2hrs, bad sleep
d = datetime(2021, 4, 18, 0, 0, 0, 0)
loop_and_send(d, 7200, get_val_good)

# 4/19 -- 4hrs, good sleep
d = datetime(2021, 4, 19, 0, 0, 0, 0)
loop_and_send(d, 14400, get_val_bad)

# 4/20 -- 5.5hrs, bad sleep
d = datetime(2021, 4, 20, 0, 0, 0, 0)
loop_and_send(d, 19800, get_val_bad)
