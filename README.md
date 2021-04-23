# Smarter Pillow
Combating sleeping disorders and enhancing the quality of sleep using Internet of Things.

## Demo
<p align="center">
  <a href="https://www.youtube.com/watch?v=DvXWixMz54I"><img src="https://img.youtube.com/vi/DvXWixMz54I/0.jpg" alt="demo video"></a>
</p>

## Description
Smarter Pillow is the next generation solution for quality sleep. It utilizes a weight sensor and microphone to track your movement and snoring to automatically adjust the pillow so that you can easily breathe while asleep. Users can also always track how well they have been sleeping on any day. We have also developed a new way to gently wake up the user -- shake and wake.

### Motivation
"50 - 70 million US adults have a sleep disorder. 48.0% report snoring."

"We spend about one-third of our life either sleeping or attempting to do so"

Sleeping is a part of everyday life, yet sleeping disorder is a very common issue. One of the common disorder is snoring. Snoring occurs when breathing is partially obstructed and is also considered as a respiratory failure. Not only does it affect your sleeping behavior, but it also affects the sleep quality of your loved ones. This is where we came up with the idea of a Smarter Pillow for better sleep and to combat sleeping disorder, specifically snoring.

### Functionality
<p align="center">
  <img height="500" src="https://github.com/eureyuri/smarter-pillow/blob/main/website/images/architecture.png?raw=true">
</p>

Our frontend is an Android application written in Java. Here, we display the daily statistics and display visualization of the collected data. It also deals with the setting and canceling of the alarm as well as calling the backend for starting certain processes and collecting sound data through the builtin microphone while sleeping.

The embedded system component is implemented using the ESP8266 and micropython. It is connected to the weight sensor and sends the collected data to our backend. For the pillow, we also have the air pumps and air bags connected for lifting the head up and down. The air pump is connected to a smarter plug that is turned on/off with the utilization of the IFTTT API. We send the requests to IFTTT to turn on/off when we observe snoring or the alarm is on.

Lastly, the backend Flask server communicates the frontend Android app, ESP8266, IFTTT API, and the mongoDB database. We have our Flask server running on the EC2 linux instance together with the MongoDB database. It is also in charge of doing the calculations for our analytics.

## Installation
### Python
Create a virtual environment with venv and start it
```bash
python3 -m venv ./venv
source venv/bin/activate
```
To install dependencies
```bash
pip install -r requirements.txt
```
To add dependencies
```bash
pip freeze > requirements.txt
```

## Implementation
### Android App
The user interacts with our android app. ...

### Python Flask Server
The Flask server interfaces with the Android App and ESP8266 and communicates with our mongodb database on AWS EC2 linux instance.
The API endpoints are as follows.

#### Switch on/off weight sensor on ESP8266
```
POST /set_weight_sensor
{
  "state": true || false
}
```

#### To add weight data to database
```
POST /insert_weight
{
  "value": 0
}
```

#### To add snore data to database
```
POST /insert_sound
{
  "datetime": "",
  "loudness": 0,
  "snore": true || false
}
```

#### To add time slept for a day to database
```
POST /insert_time
{
  "datetime": "",
  "time": 0.0,
}
```

#### Set pillow height
We can control the height of the two pillows separately.
Currently, we only support max and min inflation. 
Internally, this is connected to IFTTT, which makes the call to Kasa API
to turn on/off the air pump connected to the pillow. 
```
POST /set_pillow_height
{
  "pillow": "lower" || "upper",
  "state": true || false
}
```

#### Get overall sleep quality
The sleep quality is determined by how often you move around your head on the pillow and snore.
The movement can be seen if there is some variance in the value of the weight sensor.
```
GET /sleep_quality
{
  "datetime": ""
}
```
Success response has the sleep quality in percentage
```json
{
  "time": 0.0,
  "sleep_quality": 0.9,
  "movement": 0.1,
  "snore": 0.1
}
```

#### Get movement data
```
GET /movement
{
  "datetime": ""
}
```
Success response
```json
[
  {
    "datetime": "",
    "movement": 0
  }
]
```

#### Get snore data
```
GET /snore
{
  "datetime": ""
}
```
with the datetime in the format `2017-10-13T10:53:53.000Z`

Success response
```json
[
  {
    "datetime": "",
    "loudness": 0
  }
]
```

#### Get time slept data
```
GET /time
{
  "datetime": ""
}
```

Success response
```json
{
  "time": 0.0
}
```


### ESP8266
The ESP8266 chip is in charge of interfacing with the pressure sensor and sending the data to our backend Flask server. 
Therefore, it serves both as a client and a server. As a server, it exposes the endpoint
```
POST /set_get_weight
{
  "state": true || false
}
```
When the `state` is set to `true`, we will spawn a new thread that will act as a client to the Flask server.
Here, we will continuously collect the sensor values, until `state` is set to `false`, from the load cell connected to 
the HX711 amplifier and make a post request with the collected weight data. 

### AWS
We have a linux instance running on our AWS EC2 cloud. On the instance we have mongodb installed, which we use to 
store our data.

Our database name is `smarter_pillow` and we have 3 collections: `weight`, `snore`, and `time`.
The schema is as follows.

#### weight
```
[
  {
    "datetime": ISODate(""),
    "value": 0 
  }
]
```

#### snore
```
[
  {
    "datetime": ISODate(""),
    "loudness": 0,
    "snore": true || false
  }
]
```

#### time
```
[
  {
    "datetime": ISODate(""),
    "time": 0.0
  }
]
```

We also run our Flask server in the background on EC2. We use the linux command `screen` for terminal multiplexer.
We start `server.py` and detach by `ctrl+a d`. If we need to resume, we can `screen -r`
```bash
$ screen -S flask
$ python3 server.py
```
Remember to also specify the environment variables
```bash
$ export MONGO_USERNAME=""
$ export MONGO_PASS=""
$ export IFTTT_KEY=''
$ export IFTTT_KEY2=''
$ export SSH_ADDRESS=''
$ export ESP_URL=''
```

## References
- Library to interface with HX711 load cell amplifier for weight sensor 
  [robert-hh/hx711](https://github.com/robert-hh/hx711/tree/1ca0d87b58eb47f4810241a01e4181880e891b29)
- Wiring the weight sensor with HX711: https://circuitjournal.com/50kg-load-cells-with-HX711
- Deploying a Flask app on EC2 https://www.codementor.io/@jqn/deploy-a-flask-app-on-aws-ec2-13hp1ilqy2
- Linux screen command: https://linuxize.com/post/how-to-use-linux-screen/

## Authors
- Eurey Noguchi
- Da Shi
- Shiyu Li
