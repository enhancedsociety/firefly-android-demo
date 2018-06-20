package com.enhancedsociety.firefly;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class BLEActivity extends AppCompatActivity {

    private static final String TAG = "coisoBLE";
    private BluetoothLeAdvertiser advertiser;

    static byte[] convertTo3ByteArray(int i) {

        byte[] ret = new byte[3];
        ret[0] = (byte) (i & 0xff);
        ret[1] = (byte) ((i >> 8) & 0xff);
        ret[2] = (byte) (0x00);

        return ret;
    }

    public static UUID getGuidFromByteArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        return uuid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean isMultipleAdvertisementSupported = BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported();
        boolean isOffloadedFilteringSupported = BluetoothAdapter.getDefaultAdapter().isOffloadedFilteringSupported();
        boolean isOffloadedScanBatchingSupported = BluetoothAdapter.getDefaultAdapter().isOffloadedScanBatchingSupported();
        Log.wtf(TAG, "tests " + isMultipleAdvertisementSupported + " " + isOffloadedFilteringSupported + " " + isOffloadedScanBatchingSupported);

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();


        try {
            String batata = "batatavelha";

            // https://github.com/ricmoo/BLECast#protocol

            ByteArrayOutputStream block = new ByteArrayOutputStream();
            /* TODO support multi advertisement
                With multi advertisement we will need to properly set Termination,
                Partial and Index bits.
                This will be rather difficult because on some Android phones (such
                as mine) isMultipleAdvertisementSupported is false, which means
                we need to flick between advertisements to pretend we are sending
                multiple service uuids. And I'm assuming this tricks the wallet, which
                might not be the case and we may end up needing to exclude a number of
                devices instead.
            */
            block.write(0x00); // Termination + Partial + Index bits
            // Firefly specific byte
            // https://github.com/firefly/wallet/blob/master/source/firefly/firefly.ino#L375
            block.write(0x02);// block data - command byte
            block.write(batata.getBytes());// block data

            byte c[] = block.toByteArray();

            Log.wtf(TAG, "len block " + c.length);

            CRC24 crc = new CRC24();
            for (byte b : c) {
                crc.update(b);
            }
            byte[] crcHeader = convertTo3ByteArray(crc.getValue());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(crcHeader); // Checksum of the remaining block
            outputStream.write(c); // Remaining block

            byte[] unciphered = outputStream.toByteArray();
            Log.wtf(TAG, "len unciphered " + unciphered.length);

            Cipher cifra = android.os.Build.VERSION.SDK_INT >= 26 ? Cipher.getInstance("AES_128/ECB/NoPadding") : Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec sks = new SecretKeySpec("29231d6f2761547092e6a81664fd0eb7".getBytes(), "AES");
            cifra.init(Cipher.ENCRYPT_MODE, sks);
            byte[] ciphered = cifra.doFinal(unciphered);
            Log.wtf(TAG, "len ciphered " + ciphered.length);

            UUID serializedUUID = getGuidFromByteArray(ciphered);
            Log.wtf(TAG, "final uuid " + serializedUUID);
            ParcelUuid pUuid = new ParcelUuid(serializedUUID);

            AdvertiseData data = new AdvertiseData.Builder()
                    .addServiceUuid(pUuid)
                    .build();

            AdvertiseCallback advertisingCallback = new AdvertiseCallback() {
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    super.onStartSuccess(settingsInEffect);
                    Log.wtf(TAG, "Advertising onStartSuccess");
                }

                @Override
                public void onStartFailure(int errorCode) {
                    Log.e(TAG, "Advertising onStartFailure: " + errorCode);
                    super.onStartFailure(errorCode);
                }
            };

            advertiser.startAdvertising(settings, data, advertisingCallback);
            //startActivity(new Intent(BLEActivity.this, SignatureActivity.class));
            //finish();

        } catch (Exception e) {
            Log.wtf(TAG, "NOPE " + e.getLocalizedMessage());
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        advertiser.stopAdvertising(new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.wtf(TAG, "Stopped advertising");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.wtf(TAG, "Stopped advertising with error " + errorCode);
            }
        });
    }
}
