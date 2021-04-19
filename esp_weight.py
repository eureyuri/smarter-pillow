from hx711_gpio import HX711
from machine import Pin
import socket
import network
import json
import uasyncio
from env import WIFI_EESID, WIFI_PASS

isOn = False

# pin_OUT = Pin(12, Pin.IN)
# pin_SCK = Pin(13, Pin.OUT)
# hx711 = HX711(pin_SCK, pin_OUT)
# hx711.set_gain(128)


def connect_wifi():
    sta_if = network.WLAN(network.STA_IF)

    if not sta_if.isconnected():
        sta_if.active(True)
        sta_if.connect(WIFI_EESID, WIFI_PASS)
        while not sta_if.isconnected():
            pass

    return sta_if.ifconfig()


def get_res_body(res):
    return json.loads(res.decode("UTF-8").replace("'", '"'))


def success_response(message=""):
    return {
        "success": True,
        "message": message
    }


async def send_weight():
    print('thread started')
    while isOn:
        # hx711.tare()
        # value = hx711.read()
        print('value')
        await uasyncio.sleep_ms(100)
    print('thread ended')


async def _main():
    global isOn

    ip_address = connect_wifi()
    addr = (ip_address[0], 80)
    print(addr)

    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(addr)
    s.listen(1)
    s.settimeout(1)

    is_thread_start = False

    while True:
        try:
            (conn, address) = s.accept()
        except OSError:
            pass
        else:
            print('Connected by', address)
            rec = conn.recv(4096)
            rec = rec.split(b'\r\n\r\n')
            req = rec[0].split(b'\r\n')[0].split(b' ')
            method = req[0]
            path = req[1]
            print(method, path)

            if method == b'POST' and path == b'/set_get_weight':
                isOn = False  # First stop the thread running if it is
                isOn = get_res_body(rec[1])['state']
                data = success_response()
                is_thread_start = isOn
            else:
                data = {
                    "success": False,
                    "message": "This method has not been implemented"
                }

            if is_thread_start:
                uasyncio.create_task(send_weight())
                is_thread_start = False

            if data.get('success'):
                conn.send("HTTP/1.1 200 OK\r\n'Content-Type: application/json'\r\n\r\n")
            else:
                conn.send("HTTP/1.1 501 Not Implemented\r\n'Content-Type: application/json'\r\n\r\n")

            data = json.dumps(data)
            conn.sendall(data)
            conn.close()

        await uasyncio.sleep_ms(100)


def main():
    uasyncio.run(_main())
    while True:
        pass
