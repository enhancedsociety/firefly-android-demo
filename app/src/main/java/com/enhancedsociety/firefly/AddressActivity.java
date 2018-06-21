package com.enhancedsociety.firefly;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigInteger;

import github.nisrulz.qreader.QRDataListener;
import github.nisrulz.qreader.QREader;

public class AddressActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 123;
    private static final String TAG = "coiso";
    private SurfaceView mySurfaceView;
    private QREader QRReader;

    public static String decodeICAP(String s) {
        String temp = s.substring(9);
        int index = temp.indexOf("?") > 0 ? temp.indexOf("?") : temp.length();
        String address = new BigInteger(temp.substring(0, index), 36).toString(16);
        while (address.length() < 40) {
            address = "0" + address;
        }
        return address;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.wtf(TAG, "onCreate");
    }

    private void setupQRReader() {
        if (mySurfaceView == null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            } else {
                setContentView(R.layout.activity_main);

                final TextView label = findViewById(R.id.textLabel);
                final TextView text = findViewById(R.id.textView);

                mySurfaceView = findViewById(R.id.camera_view);

                QRReader = new QREader.Builder(this, mySurfaceView, new QRDataListener() {
                    @Override
                    public void onDetected(final String data) {
                        if (data.startsWith("IBAN:XE")) {
                            final String address = "0x" + decodeICAP(data);
                            if (!address.equals(text.getText()) && !address.equals("0x")) {
                                final Button btn = findViewById(R.id.button2);
                                btn.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        btn.setVisibility(View.VISIBLE);
                                    }
                                });
                                btn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Intent i = new Intent(AddressActivity.this, BLEActivity.class);
                                        i.putExtra("eth_address", address);
                                        startActivity(i);
                                        finish();
                                    }
                                });
                                text.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        label.setText("ETHEREUM ADDRESS");
                                        text.setText(address);
                                    }
                                });
                            }
                        } else {
                            text.post(new Runnable() {
                                @Override
                                public void run() {
                                    label.setText("");
                                    text.setText("");
                                }
                            });
                        }
                    }
                }).facing(QREader.BACK_CAM)
                        .enableAutofocus(true)
                        .height(mySurfaceView.getHeight())
                        .width(mySurfaceView.getWidth())
                        .build();

            }
        } else {
//            Log.wtf(TAG, "surfaceview is not null");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        Log.wtf(TAG, "onResume");

        setupQRReader();

        if (QRReader != null) {
            QRReader.initAndStart(mySurfaceView);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
//        Log.wtf(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    setupQRReader();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }
            default: {
                Log.wtf(TAG, "response for unknown request " + requestCode + ", ignoring");
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.wtf(TAG, "onPause");

        if (QRReader != null) {
            // Cleanup in onPause()
            // --------------------
            QRReader.releaseAndCleanup();
        }
    }
}
