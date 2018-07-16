import json
import os
import uuid
from collections import defaultdict

import dbus
import gatt
import qrcode
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
from eth_account.account import Account
from eth_account.messages import defunct_hash_message
from PIL import Image
from web3.auto import w3
from web3.iban import Iban


# Fully pretend a laptop is firefly
# - show address qrcode at startup
# - receive blecast of message to sign
# - show sig:r/ and sig:s/ qrcodes after signing (assume sig:v is 27?)


bus = dbus.SystemBus()

device_uuids = defaultdict(set)


#eth_account = Account.privateKeyToAccount(os.urandom(32))
eth_account = Account.privateKeyToAccount(bytes.fromhex(
    'cf53d57f0d9cdcd2ab0aadf227b5c99bead260195e862cba5c8e782f4d0619d1'))

firefly_key = "29231d6f2761547092e6a81664fd0eb7"

# unsigned to signed


def sbyte(n):
    return int.from_bytes(n.to_bytes(1, byteorder="big", signed=False), byteorder="big", signed=True)


def decrypt(b):
    obj = Cipher(algorithms.AES(bytes.fromhex(firefly_key)),
                 modes.ECB(), backend=default_backend()).decryptor()
    res = obj.update(b) + obj.finalize()
    return res


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
            for u in uuids:

                # - decrypt with firefly_key
                ack_uuid = uuid.UUID('urn:uuid:{}'.format(u))

                decrypted_payload = decrypt(ack_uuid.bytes)

                # - verify it contains crc24 + data

                pl_crc = decrypted_payload[:3]
                pl_data = decrypted_payload[3:]

                crc_val = crc24(pl_data)
                ver_crc = (crc_val).to_bytes(3, byteorder='big', signed=False)

                if pl_crc != ver_crc:
                    # print("CRC24 mismatch\nCalculated {}\nExpected {}".format(
                    #    [str(sbyte(b)) for b in ver_crc],
                    #    [str(sbyte(b)) for b in pl_crc]
                    #    ))
                    break

                # henceforth we know it's a firefly message

                print('MAC Address ', device.mac_address)
                print('UUID Payload ', u)
                print("Decrypted Payload {}".format(
                    [str(sbyte(b)) for b in decrypted_payload]))

                # - break data based on firefly semantics

                blecast_ctl = pl_data[0].to_bytes(1, byteorder="big")
                firefly_cmd = pl_data[1].to_bytes(1, byteorder="big")
                firefly_data = pl_data[2:]

                # print(blecast_ctl)

                if blecast_ctl != b'\x00':
                    print(
                        'messages dependent on multiple advertisements are not supported yet')
                    break

                # - sign actual payload

                if firefly_cmd != b'\x02':
                    print('unsupported firefly command {}'.format(firefly_cmd))
                    self.stop()
                    break

                print()
                resp = input(
                    'sign "{}"? (y/N) '.format(str(firefly_data))).strip()

                if resp.lower() != 'y' and resp.lower() != 'yes':
                    break

                message_hash = defunct_hash_message(primitive=firefly_data)
                signed_message = w3.eth.account.signHash(
                    message_hash, private_key=eth_account.privateKey)

                recovered = w3.eth.account.recoverHash(
                    message_hash, vrs=(
                        28,
                        hex(signed_message['r']),
                        hex(signed_message['s'])
                    ))
                assert(recovered == eth_account.address)

                # print('r\t', hex(signed_message['r']))
                # print('s\t', hex(signed_message['s']))
                # print('v\t', hex(signed_message['v']))
                # print()
                # print(firefly_data)
                # print([str(sbyte(b)) for b in firefly_data])
                # print()
                # print(message_hash.hex())
                # print(len(message_hash))
                # print([str(sbyte(b)) for b in message_hash])
                # print()
                # print(signed_message)
                # print()

                sig_s = 'SIG:S/{}'.format(hex(signed_message['s']))
                sig_r = 'SIG:R/{}'.format(hex(signed_message['r']))

                qr_s = qrcode.make(sig_s)
                qr_r = qrcode.make(sig_r)

                w, h = qr_s.size

                new_im = Image.new('L', (w*2, h))

                new_im.paste(qr_r)
                new_im.paste(qr_s, (w, 0))
                new_im.show()

                self.stop()


print('ADDRESS {}'.format(eth_account.address))

iban_str = 'IBAN:{}'.format(Iban.fromAddress(eth_account.address).toString())
print(iban_str)
img = qrcode.make(iban_str)
img.show(title='Ethereum Address')
print()

manager = AnyDeviceManager(adapter_name='hci0')
manager.start_discovery()

manager.run()
