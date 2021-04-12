# smarter-pillow

# Requirements
## Python
Create a virtual environment with venv and start it
```
python3 -m venv ./venv
source venv/bin/activate
```
To install dependencies
```
pip install -r requirements.txt
```
To add dependencies
```
pip freeze > requirements.txt
```

## Android App
The user interacts with our android app. ...

## Python Flask Server
The Flask server interfaces with the Android App and ESP8266 and communicates with our mongodb database on AWS EC2 linux instance.

## ESP8266
The ESP8266 chip is in charge of interfacing with the pressure sensor and sending the data to our backend Flask server. 

## AWS
We have a linux instance running on our AWS EC2 cloud. On the instance we have mongodb installed, which we use to 
store our data.

Our database name is `smarter_pillow` and we have 2 collections: `movement` and `snore`.
The schema is as follows.

### movement
```json
{
  
}
```

### snore
```json
{
  
}
```