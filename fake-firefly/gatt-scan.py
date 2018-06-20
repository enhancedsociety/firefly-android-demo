import gatt
import json
import dbus

from collections import defaultdict

bus = dbus.SystemBus()

device_uuids = defaultdict(set)

firefly_key = "29231d6f2761547092e6a81664fd0eb7"

# TODO
# Fully pretend a laptop is firefly
# - add qrcode https://pypi.org/project/qrcode/
# - show address at startup
# - show sig:r/ and sig:s/ after signing (assume sig:v is 28?)


def crc24(octets):
    INIT = 0xB704CE
    POLY = 0x1864CFB
    crc = INIT
    for octet in octets:
        crc ^= (octet << 16)
        for _i in range(8):
            crc <<= 1
            if crc & 0x1000000:
                crc ^= POLY
    return crc & 0xFFFFFF


class AnyDeviceManager(gatt.DeviceManager):
    def device_discovered(self, device):

        mac_address = device.mac_address.replace(':', '_').upper()
        path = '/org/bluez/%s/dev_%s' % (self.adapter_name, mac_address)
        device_object = bus.get_object("org.bluez", path)
        d = dbus.Interface(device_object, "org.bluez.Device1")
        device_properties = dbus.Interface(
            d, "org.freedesktop.DBus.Properties")
        uuids = [str(x)
                 for x in device_properties.Get("org.bluez.Device1", "UUIDs")]

        if uuids:
            device_uuids[device.mac_address].union(set(uuids))
            print(device.mac_address)
            for u in uuids:
                # TODO
                # - decrypt with firefly_key
                # - verify it contains crc24 + data
                # - break data based on firefly semantics
                # - sign actual payload
                print('\t{}'.format(u))


manager = AnyDeviceManager(adapter_name='hci0')
manager.start_discovery()

manager.run()
