package com.enhancedsociety.firefly;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import okio.ByteString;

public class BLEActivity extends AppCompatActivity {

    public static final String KEY = "29231d6f2761547092e6a81664fd0eb7";
    public static final int BLECAST_DURATION_MS = 500;
    // TODO the content to sign shouldn't be hardcoded
    public static final String BATATA = "batatavelha";
    private static final String TAG = "coisoBLE";
    private String eth_address;
    private AdvertiseCallback advertisingCallback;

    public static UUID getGuidFromByteArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        return uuid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        eth_address = getIntent().getStringExtra("eth_address");

        advertisingCallback = new AdvertiseCallback() {
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
                .setConnectable(false)
                .build();


        try {
            // Based on
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
            block.write(BATATA.getBytes());// block data

            byte c[] = block.toByteArray();

            //Log.wtf(TAG, "len block " + c.length);

            CRC24 crc = new CRC24();
            for (byte b : c) {
                crc.update(b);
            }
            int crc_val = crc.getValue();
            //Log.wtf(TAG, "crc val " + crc_val);
            byte[] crcHeader = ByteBuffer.allocate(4).putInt(crc_val).array();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(crcHeader[1]); // Checksum of the remaining block as 3 bytes
            outputStream.write(crcHeader[2]);
            outputStream.write(crcHeader[3]);
            outputStream.write(c); // Remaining block

            byte[] unciphered = outputStream.toByteArray();

            Cipher cifra = android.os.Build.VERSION.SDK_INT >= 26 ? Cipher.getInstance("AES_128/ECB/NoPadding") : Cipher.getInstance("AES/ECB/NoPadding");
            byte[] ba = ByteString.decodeHex(KEY).toByteArray();
            SecretKeySpec sks = new SecretKeySpec(ba, "AES");
            cifra.init(Cipher.ENCRYPT_MODE, sks);
            byte[] ciphered = cifra.doFinal(unciphered);
            //Log.wtf(TAG, "len ciphered " + ciphered.length);

            //Log.wtf(TAG, "len unciphered " + unciphered.length);
            //String joined = Arrays.toString(unciphered);
            //Log.wtf(TAG, "unciphered " + joined);

            UUID serializedUUID = getGuidFromByteArray(ciphered);
            Log.wtf(TAG, "final uuid " + serializedUUID);
            ParcelUuid pUuid = new ParcelUuid(serializedUUID);

            AdvertiseData data = new AdvertiseData.Builder()
                    .addServiceUuid(pUuid)
                    .build();


            BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser().startAdvertising(settings, data, advertisingCallback);

            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser().stopAdvertising(advertisingCallback);
                    Intent i = new Intent(BLEActivity.this, SignatureActivity.class);
                    i.putExtra("eth_address", eth_address);
                    i.putExtra("original_message", BATATA);
                    startActivity(i);
                    finish();
                }
            }, BLECAST_DURATION_MS);

        } catch (Exception e) {
            Log.wtf(TAG, "Unexpected error " + e.getLocalizedMessage());
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.wtf(TAG, "onPause");

        BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser().stopAdvertising(advertisingCallback);
    }
}
