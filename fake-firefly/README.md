# Firefly fakery

This app `gatt-scan.py` (rename pending) is meant to emulate the firefly wallet, by showing qr codes on screen and scanning for BLE annoucements to catch BLEcast transmissions. It is meant to be an aid in developing and testing the Android Firefly demo app.

Its behaviour will most likely differ from Firefly since the author does not have one available (although I intend to build it) but hopefully over time it will be a perfect simulation.

## Preparations

```
pip install -r requirements.txt
```

`python-dbus` and virtualenv don't play nice, so it needs some witchcraft rather than a `pip install`

```sh
cp -r /usr/lib/python3.6/site-packages/_dbus_*.so env/lib/python3.6/site-packages/

cp -r /usr/lib/python3.6/site-packages/dbus env/lib/python3.6/site-packages
```

## Run!

```sh
sudo ./env/bin/python gatt-scan.py
```