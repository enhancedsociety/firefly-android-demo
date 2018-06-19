package com.enhancedsociety.firefly;

import android.Manifest;
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

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SignatureException;

import github.nisrulz.qreader.QRDataListener;
import github.nisrulz.qreader.QREader;

public class SignatureActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 124;
    private static final String TAG = "coiso";
    public static final String A_TEST_MESSAGE = "A test message";
    private SurfaceView mySurfaceView;
    private QREader QRReader;

    private String sig_r = "";
    private String sig_s = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void setupQRReader() {
        if (mySurfaceView == null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            } else {
                setContentView(R.layout.activity_sig);

                mySurfaceView = findViewById(R.id.camera_view);

                Button b = findViewById(R.id.button2);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final TextView label = findViewById(R.id.textLabel);
                        final TextView text = findViewById(R.id.textView);
                        sig_r = "";
                        sig_s = "";
                        text.post(new Runnable() {
                            @Override
                            public void run() {
                                label.setText("");
                                text.setText("");
                            }
                        });
                        final Button btn = findViewById(R.id.button3);
                        btn.post(new Runnable() {
                            @Override
                            public void run() {
                                btn.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });

                QRReader = new QREader.Builder(this, mySurfaceView, new QRDataListener() {
                    @Override
                    public void onDetected(final String data) {
                        // Log.wtf(TAG, "sig_r " + sig_r.isEmpty() + " sig_s " + sig_s.isEmpty() + " data " + data);
                        if (data.startsWith("SIG:R/")) {
                            String parsed_data = data.substring(6);
                            if (!sig_r.equals(parsed_data)) {
                                sig_r = parsed_data;
                                checkSigIsRead();
                            }
                        } else if (data.startsWith("SIG:S/")) {
                            String parsed_data = data.substring(6);
                            if (!sig_s.equals(parsed_data)) {
                                sig_s = parsed_data;
                                checkSigIsRead();
                            }
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

    public void checkSigIsRead() {
        if (sig_r.isEmpty() || sig_s.isEmpty()) {
            return;
        }
        final String verification_addr = verifySig(A_TEST_MESSAGE, sig_r, sig_s);

        final TextView label = findViewById(R.id.textLabel);
        final TextView text = findViewById(R.id.textView);

        text.post(new Runnable() {
            @Override
            public void run() {
                label.setText("VERIFICATION ADDRESS");
                text.setText(verification_addr);
                final Button btn = findViewById(R.id.button3);
                btn.post(new Runnable() {
                    @Override
                    public void run() {
                        btn.setVisibility(View.VISIBLE);
                    }
                });
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            }
        });

    }

    public static String verifySig(String msg, String r_hex, String s_hex) {
        byte[] r = Numeric.hexStringToByteArray(r_hex);
        byte[] s = Numeric.hexStringToByteArray(s_hex);
        Sign.SignatureData sigData = new Sign.SignatureData((byte) 28, r, s);

        try {
            BigInteger b = Sign.signedMessageToKey(msg.getBytes(), sigData);
            return Keys.getAddress(b);
        } catch (SignatureException e) {
            Log.w(TAG, e.getLocalizedMessage());
            return "";
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
                    setupQRReader();
                } else {
                    finish();
                }
                return;
            }
            default: {
//                Log.wtf(TAG, "response for unknown request " + requestCode + ", ignoring");
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        Log.wtf(TAG, "onPause");

        if (QRReader != null) {
            QRReader.releaseAndCleanup();
        }
    }
}
