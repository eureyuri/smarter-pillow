# smarter-pillow
## Description
### Motivation

### Functionality


## Demo
TODO

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

Our database name is `smarter_pillow` and we have 2 collections: `weight` and `snore`.
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

## References
- Library to interface with HX711 load cell amplifier for weight sensor 
  [robert-hh/hx711](https://github.com/robert-hh/hx711/tree/1ca0d87b58eb47f4810241a01e4181880e891b29)
- Wiring the weight sensor with HX711: https://circuitjournal.com/50kg-load-cells-with-HX711

## Authors
- Eurey Noguchi
- Da Shi
- Shiyu Li