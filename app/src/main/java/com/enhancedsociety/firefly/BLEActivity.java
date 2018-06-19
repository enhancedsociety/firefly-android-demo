package com.enhancedsociety.firefly;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.UUID;

public class BLEActivity extends AppCompatActivity {

    private static final String TAG = "coisoBLE";
    private BluetoothLeAdvertiser advertiser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
    }

    @Override
    protected void onResume() {
        super.onResume();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        ParcelUuid pUuid = new ParcelUuid(UUID.fromString("f9851787-f749-4a00-8c58-a586ae35278f"));
        ParcelUuid pUuid2 = new ParcelUuid(UUID.fromString("a63d3871-5df1-48bc-a786-bd109ae70958"));

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
        startActivity(new Intent(BLEActivity.this, SignatureActivity.class));
        finish();
    }
}
