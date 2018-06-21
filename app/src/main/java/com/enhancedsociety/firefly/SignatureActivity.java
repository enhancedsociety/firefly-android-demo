package com.enhancedsociety.firefly;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
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

import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SignatureException;

import github.nisrulz.qreader.QRDataListener;
import github.nisrulz.qreader.QREader;

public class SignatureActivity extends AppCompatActivity {
    public static final String A_TEST_MESSAGE = "A test message";
    public static final byte SIG_V = (byte) 28;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 124;
    private static final String TAG = "coiso";
    private SurfaceView mySurfaceView;
    private QREader QRReader;

    private String sig_r = "";
    private String sig_s = "";
    private String eth_address;
    private String original_message;

    public static String verifySig(String msg, String r_hex, String s_hex) {
        byte[] r = Numeric.hexStringToByteArray(r_hex);
        byte[] s = Numeric.hexStringToByteArray(s_hex);
        Sign.SignatureData sigData = new Sign.SignatureData(SIG_V, r, s);

        String prefixed_msg = "\u0019Ethereum Signed Message:\n" + msg.length() + msg;
        byte[] msg_hashed_b = Hash.sha3(prefixed_msg.getBytes());
//        Log.wtf(TAG, "Message hash " + Numeric.toHexString(msg_hashed_b));
//        Log.wtf(TAG, "Sig R " + r_hex);
//        Log.wtf(TAG, "Sig S " + s_hex);
//        Log.wtf(TAG, "Sig V " + Numeric.toHexString(new byte[]{SIG_V}));

        try {
            BigInteger pubkey = Sign.signedMessageToKey(prefixed_msg.getBytes(), sigData);
            String address = Keys.getAddress(pubkey);
            return "0x" + address;
        } catch (SignatureException e) {
            Log.w(TAG, e.getLocalizedMessage());
            return "";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        eth_address = getIntent().getStringExtra("eth_address");
        original_message = getIntent().getStringExtra("original_message");

        BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser().stopAdvertising(new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.wtf(TAG, "stopAdvertising.onStartSuccess");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.wtf(TAG, "stopAdvertising.onStartFailure=" + errorCode);
            }
        });
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
        final String verification_addr = verifySig(original_message, sig_r, sig_s);

        final TextView label = findViewById(R.id.textLabel);
        final TextView text = findViewById(R.id.textView);

        text.post(new Runnable() {
            @Override
            public void run() {
                Log.wtf(TAG, "VERIFICATION ADDRESS " + verification_addr);
                Log.wtf(TAG, "SIGNING ADDRESS " + eth_address);
                label.setText("signature is");
                text.setText(verification_addr.equals(eth_address) ? "CORRECT" : "WRONG");
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
                        finishAffinity();
                    }
                });
            }
        });

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
