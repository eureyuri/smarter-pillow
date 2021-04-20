from flask import Flask
from flask import request
from flask import Response
from pymongo import MongoClient
from dotenv import load_dotenv
import os
import json
import requests
from datetime import datetime, date, timedelta


app = Flask(__name__)

load_dotenv()
IFTTT_KEY = os.getenv('IFTTT_KEY')
IFTTT_KEY2 = os.getenv('IFTTT_KEY2')
SSH_ADDRESS = os.getenv('SSH_ADDRESS')
ESP_URL = os.getenv('ESP_URL')

# MongoDB Collections
db_weight = None
db_snore = None
db_time = None


# Had to change to accept TCP port 27017 for inbound
def connect_to_mongo():
    global db_weight, db_snore, db_time
    try:
        uri = "mongodb://" + SSH_ADDRESS + ":27017"
        client = MongoClient(uri)

        auth = client.smarter_pillow.authenticate(os.getenv('MONGO_USERNAME'), os.getenv('MONGO_PASS'))
        if not auth:
            print("MongoDB authentication failure: Please check the username or password")
            return None

        db = client.smarter_pillow
        db_weight = db.weight
        db_snore = db.snore
        db_time = db.time
    except Exception as e:
        print("MongoDB connection failure: Please check the connection details")
        print(e)


def json_serial(obj):
    """JSON serializer for objects not serializable by default json code"""
    if isinstance(obj, (datetime, date)):
        return obj.isoformat()
    raise TypeError("Type %s not serializable" % type(obj))


def get_formatted_datetime(time):
    return datetime.strptime(time, "%Y-%m-%dT%H:%M:%S.000Z")


def get_snore_data(time):
    cursor_snore = db_snore.find({
        'datetime': {
            '$gte': time,
            '$lte': time + timedelta(days=1)
        }
    }, {'_id': False})

    res_snore = []
    for document in cursor_snore:
        res_snore.append(document)

    return res_snore


def get_weight_data(time):
    cursor_weight = db_weight.find({
        'datetime': {
            '$gte': time,
            '$lte': time + timedelta(days=1)
        }
    }, {'_id': False})

    res_weight = []
    for document in cursor_weight:
        res_weight.append(document)

    return res_weight


def calculate_movement_percentage(data):
    movement_count = 0
    prev_movement = data[0]['value']
    for d in data:
        current_movement = d['value']
        if abs(prev_movement - current_movement) >= 0.5:
            movement_count += 1
        prev_movement = current_movement
    return movement_count / len(data)


def calculate_snore_percentage(data):
    snore_count = 0
    for d in data:
        if d['snore']:
            snore_count += 1
    return snore_count / len(data)


@app.route('/', methods=['GET'])
def index():
    return "<h1>Smarter Pillow Backend!</h1>"


@app.route('/set_weight_sensor', methods=['POST'])
def set_weight_sensor():
    is_set_sensor = json.loads(request.data.decode())['state']
    data = {
        'state': is_set_sensor
    }
    requests.post(ESP_URL + '/set_get_weight', data=json.dumps(data),
                  headers={'Content-Type': 'application/json'})
    return "set get weight"


@app.route('/sleep_quality', methods=['GET'])
def sleep_quality():
    time = get_formatted_datetime(json.loads(request.data.decode())['datetime'])
    snore_data = get_snore_data(time)
    weight_data = get_weight_data(time)
    time = db_time.find_one({
        'datetime': {
            '$gte': time,
            '$lte': time + timedelta(days=1)
        }
    }, {'_id': False})

    if len(snore_data) > 0 and len(weight_data) > 0:
        movement = calculate_movement_percentage(weight_data)
        snore = calculate_snore_percentage(snore_data)
        overall_quality = 1 - (movement + snore) / 2

        res = {
            "time": time,
            "sleep_quality": overall_quality,
            "movement": movement,
            "snore": snore
        }
    else:
        # Not enough data to show
        res = {
            "time": 0.0,
            "sleep_quality": 0,
            "movement": 0,
            "snore": 0
        }

    print(res)
    return Response(json.dumps(res, default=json_serial), mimetype='application/json')


@app.route('/snore', methods=['GET'])
def get_snore():
    time = get_formatted_datetime(json.loads(request.data.decode())['datetime'])
    res = get_snore_data(time)

    return Response(json.dumps(res, default=json_serial),  mimetype='application/json')


@app.route('/insert_sound', methods=['POST'])
def insert_sound():
    data = json.loads(request.data.decode())
    data['datetime'] = datetime.strptime(data['datetime'], "%Y-%m-%dT%H:%M:%S.000Z")
    db_snore.insert_one(data)
    print("inserted snore")

    return "inserted snore"


@app.route('/movement', methods=['GET'])
def get_movement():
    time = get_formatted_datetime(json.loads(request.data.decode())['datetime'])
    res = get_weight_data(time)

    return Response(json.dumps(res, default=json_serial),  mimetype='application/json')


@app.route('/insert_weight', methods=['POST'])
def insert_weight():
    data = json.loads(request.data.decode())
    data['datetime'] = datetime.now().strftime("%Y-%m-%dT%H:%M:%S.000Z")  # to string
    data['datetime'] = datetime.strptime(data['datetime'], "%Y-%m-%dT%H:%M:%S.000Z")  # to datetime
    db_weight.insert_one(data)
    print("inserted weight")

    return "inserted weight"


@app.route('/mock_insert_weight', methods=['POST'])
def mock_insert_weight():
    data = json.loads(request.data.decode())
    data['datetime'] = datetime.strptime(data['datetime'], "%Y-%m-%dT%H:%M:%S.000Z")
    db_weight.insert_one(data)
    print("inserted weight")

    return "inserted weight"


@app.route('/time', methods=['GET'])
def get_sleep_time():
    time = get_formatted_datetime(json.loads(request.data.decode())['datetime'])
    res = get_weight_data(time)

    return Response(json.dumps(res, default=json_serial),  mimetype='application/json')


@app.route('/insert_time', methods=['POST'])
def insert_sleep_time():
    data = json.loads(request.data.decode())
    data['datetime'] = datetime.strptime(data['datetime'], "%Y-%m-%dT%H:%M:%S.000Z")
    db_time.insert_one(data)
    print("inserted time")

    return "inserted time"


@app.route('/set_pillow_height', methods=['POST'])
def set_pillow_height():
    data = json.loads(request.data.decode())
    if data['pillow'] == 'lower':
        if data['state']:
            requests.post('https://maker.ifttt.com/trigger/weight_sensor1_on/with/key/' + IFTTT_KEY)
        elif not data['state']:
            requests.post('https://maker.ifttt.com/trigger/weight_sensor1_off/with/key/' + IFTTT_KEY)
    elif data['pillow'] == 'upper':
        if data['state']:
            requests.post('https://maker.ifttt.com/trigger/weight_sensor2_on/with/key/' + IFTTT_KEY2)
        elif not data['state']:
            requests.post('https://maker.ifttt.com/trigger/weight_sensor2_off/with/key/' + IFTTT_KEY2)

    return "adjusted"


if __name__ == "__main__":
    connect_to_mongo()
    app.run(debug=True, host="0.0.0.0", port=80)
