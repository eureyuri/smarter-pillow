from flask import Flask
from flask import jsonify
from flask import request
from pymongo import MongoClient
from dotenv import load_dotenv
import os
import json
from bson import ObjectId
import requests
import pickle


class JSONEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, ObjectId):
            return str(o)
        return json.JSONEncoder.default(self, o)


load_dotenv()

app = Flask(__name__)
ssh_address = "ec2-18-206-197-126.compute-1.amazonaws.com"
MODEL_FILE = 'model.sav'
IFTTT_KEY = os.getenv('IFTTT_KEY')


# Had to change to accept TCP port 27017 for inbound
def connect_to_mongo():
    global gestures
    try:
        uri = "mongodb://" + ssh_address + ":27017"
        client = MongoClient(uri)

        auth = client.gestures.authenticate(os.getenv('MONGO_USERNAME'), os.getenv('MONGO_PASS'))
        if not auth:
            print("MongoDB authentication failure: Please check the username or password")
            return None

        db = client.gestures
        collection = db.gestures
        gestures = collection
    except Exception as e:
        print("MongoDB connection failure: Please check the connection details")
        print(e)


@app.route('/', methods=['GET'])
def show():
    cursor = gestures.find({})
    res = []
    for document in cursor:
        res.append(document)

    return JSONEncoder().encode(res)


@app.route('/insert', methods=['POST'])
def insert_gesture():
    print(json.loads(request.data.decode()))
    gestures.insert_one(json.loads(request.data.decode()))
    print("inserted")

    return "inserted"


# TEST: curl -X POST -H "Content-Type: application/json" -d '{"state": true, "pillow": "lower"}' 127.0.0.1:8080/set_pillow_height
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
            requests.post('https://maker.ifttt.com/trigger/weight_sensor2_on/with/key/' + IFTTT_KEY)
        elif not data['state']:
            requests.post('https://maker.ifttt.com/trigger/weight_sensor2_off/with/key/' + IFTTT_KEY)

    return "adjusted"


if __name__ == "__main__":
    connect_to_mongo()
    app.run(debug=True, host="127.0.0.1", port=8080)
