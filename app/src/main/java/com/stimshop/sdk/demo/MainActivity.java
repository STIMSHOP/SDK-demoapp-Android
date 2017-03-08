package com.stimshop.sdk.demo;

import android.Manifest.permission;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.stimshop.sdk.common.StimShop;
import com.stimshop.sdk.common.detection.Detector;
import com.stimshop.sdk.common.detection.Proximity;
import com.stimshop.sdk.common.messages.Messages;
import com.stimshop.sdk.common.messages.broadcaster.AbstractBroadcaster;
import com.stimshop.sdk.common.messages.broadcaster.StimShopBroadcastReceiver;
import com.stimshop.sdk.common.utils.Timber;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.Unbinder;

public class MainActivity extends AppCompatActivity {

    public static final int BLUETOOTH_ENABLE_REQUEST_ID = 6;

    // The stimshop object
    StimShop stimShop;

    // The signal that has been detected last
    Optional<String> latestSignal = Optional.absent();
    Proximity latestProximity = Proximity.UNKNOWN;
    int signalDetectedCount = 0;

    // Other objects
    private Unbinder unbinder;  // ButterKnife unbinder
    Snackbar permSnackbar;

    // region Our views

    @BindView(R.id.sw_initialize)
    SwitchCompat initializeSwitch;

    @BindView(R.id.sw_toggle_detection)
    SwitchCompat toggleDetectionSwitch;

    @BindView(R.id.sw_jack_status)
    SwitchCompat jackStatusSwitch;

    @BindView(R.id.sw_microphone_status)
    SwitchCompat jackMicrophoneStatusSwitch;

    @BindView(R.id.linear_log)
    LinearLayout sigLogLinearLayout;

    @BindView(R.id.main_layout)
    LinearLayout mainLayout;

    // endregion

    // region Activity lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);

        initializeSwitch.setEnabled(false);
        toggleDetectionSwitch.setEnabled(false);
        toggleDetectionSwitch.setChecked(false);
        jackStatusSwitch.setEnabled(false);
        jackStatusSwitch.setChecked(false);
        jackMicrophoneStatusSwitch.setEnabled(false);
        jackMicrophoneStatusSwitch.setChecked(false);
        initPermissionSnackbar();

        stimShop = StimShop.get();
    }

    @Override
    protected void onResume() {
        // Register the broadcast receiver at activity level
        LocalBroadcastManager.getInstance(this).registerReceiver(stimshopReceiver,
                new IntentFilter(AbstractBroadcaster.ACTION_RECEIVE_MESSAGES));

        initializeSwitch.setChecked(stimShop.isReady());
        toggleDetectionSwitch.setEnabled(stimShop.isReady());
        toggleDetectionSwitch.setChecked(stimShop.isDetecting());
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permSnackbar.show();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Don't forget to unregister the receiver. Else you'll leak the context, and hence, memory!
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stimshopReceiver);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_ENABLE_REQUEST_ID) {
            if (resultCode == RESULT_OK) {
                stimShop.stopDetection();
                stimShop.startDetection();
            } else if (resultCode == RESULT_CANCELED) {
                stimShop.stopDetection();
            }
        }
    }

    // endregion

    // Handles the listen switch
    @OnCheckedChanged(R.id.sw_toggle_detection)
    public void onToggleAudioDetection(CompoundButton view, boolean isChecked) {
        if (isChecked) {
            Timber.d("Starting detection");
            latestSignal = Optional.absent();
            stimShop.startDetection();
        } else {
            Timber.d("Stopping detection");
            stimShop.stopDetection();
        }
    }

    // Listens to the detection messages
    private StimShopBroadcastReceiver stimshopReceiver = new StimShopBroadcastReceiver() {

        @Override
        protected void onGenericError(Context context, int code, @Nullable String details) {
            switch (code) {
                // Init messages, they require
                case Messages.Error.CANNOT_CHECK_API_KEY:
                case Messages.Error.NO_DETECTORS_AVAILABLE:
                    initializeSwitch.setChecked(false);
                    toggleDetectionSwitch.setChecked(false);
                    toggleDetectionSwitch.setEnabled(false);
                    break;

                case Messages.Error.REQUIRE_INTERNET_CONNECTIVITY:
                    initializeSwitch.setChecked(false);
                    toggleDetectionSwitch.setChecked(false);
                    toggleDetectionSwitch.setEnabled(false);
                    break;

                case Messages.Error.REQUIRE_BLUETOOTH_ENABLED:
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), permission.BLUETOOTH)
                            == PackageManager.PERMISSION_GRANTED) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_REQUEST_ID);
                    }
                    break;
            }

            if (!Strings.isNullOrEmpty(details)) {
                Toast.makeText(MainActivity.this, "STIMSHOP: " + details, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onPermissionRequiredError(Context context, ArrayList<String> permissions, @Nullable String details) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission.RECORD_AUDIO)) {
                permSnackbar.show();
            } else {
                stimshopMultiplePermissionsRequest(permissions);
            }
        }

        @Override
        protected void onGenericInfo(Context context, int code, @Nullable String details) {
            if (!Strings.isNullOrEmpty(details)) {
                Toast.makeText(MainActivity.this, details, Toast.LENGTH_SHORT).show();
            }
            switch (code) {
                case Messages.Info.EXTERNAL_MICROPHONE_CONNECTED:
                    jackStatusSwitch.setChecked(true);
                    break;
                case Messages.Info.EXTERNAL_MICROPHONE_DISCONNECTED:
                    jackStatusSwitch.setChecked(false);
                    break;
            }
        }

        @Override
        protected void onDetectionStarted(Context context) {
            Timber.d("Detection started");
            Toast.makeText(MainActivity.this, "Detection started", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onDetectionStopped(Context context) {
            Timber.d("Detection stopped");
            Toast.makeText(MainActivity.this, "Detection stopped", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onStimShopReady(Context context) {
            initializeSwitch.setChecked(true);
            toggleDetectionSwitch.setEnabled(true);
            Toast.makeText(MainActivity.this, "StimShop is ready", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onSignalDetected(Context context, Detector.Type detectorType, String signalCode, Proximity proximity) {
            if (!Objects.equal(signalCode, latestSignal.orNull()) || proximity != latestProximity) {
                Toast.makeText(MainActivity.this,
                        "Signal " + signalCode + " detected with proximity " + proximity.name(),
                        Toast.LENGTH_SHORT).show();
                latestSignal = Optional.of(signalCode);
                latestProximity = proximity;
            }

            Timber.d("Signal detected: %s", signalCode);
            TextView tv = new TextView(context);
            tv.setTextSize(24);
            tv.setSingleLine(true);
            tv.setTextColor(Color.BLACK);
            tv.setText(++signalDetectedCount + " - " + signalCode);
            sigLogLinearLayout.addView(tv, 0);
        }
    };

    /**
     * Checks each permissions in the passed ArrayList and requests only the required ones
     *
     * @param listPermissions Must be containing {@link android.Manifest.permission} members only
     */
    private void stimshopMultiplePermissionsRequest(ArrayList<String> listPermissions) {

        if (listPermissions != null) {
            if (listPermissions.size() > 0) {

                ArrayList<String> missingPermissions = new ArrayList<>();
                for (String permission : listPermissions) {
                    if (permission != null) {
                        if (permission.contains("android.permission")) {
                            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                                missingPermissions.add(permission);
                            }
                        }
                    }
                }
                String[] permissions = new String[missingPermissions.size()];
                missingPermissions.toArray(permissions);
                stimshopMultiplePermissionsRequest(permissions);
            }
        }
    }


    /**
     * Checks each permissions in the passed ArrayList and requests only the required ones.
     * TO BE SAFE, YOU SHOULD USE {@link MainActivity#stimshopMultiplePermissionsRequest(ArrayList)} INSTEAD
     *
     * @param permissions Must be containing {@link android.Manifest.permission} members only
     * @return false if an error occurs
     */
    private boolean stimshopMultiplePermissionsRequest(String[] permissions) {

        // TODO: remove logging
        if (permissions == null) return false;
        if (permissions.length == 0) return false;
        for (String p : permissions) {
            if (!p.contains("android.permission")) return false;
        }
        ActivityCompat.requestPermissions(this, permissions, Messages.Error.REQUIRE_PERMISSIONS);
        return true;
    }


    /**
     * Inits a snackbar that will display a rationale about the RECORD_AUDIO permission when needed
     */
    private void initPermissionSnackbar() {
        // Critical permission has not been granted and must be requested.

        // Provide an additional rationale to the user if the permission was not granted
        // and the user would benefit from additional context for the use of the permission.
        // Display a SnackBar with a button to request the missing permission.
        permSnackbar = Snackbar.make(mainLayout, "Recording audio is required to detect signals.",
                Snackbar.LENGTH_INDEFINITE);
        permSnackbar.setAction("OK", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stimshopMultiplePermissionsRequest(StimShop.REQUIRED_PERMISSIONS);
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case Messages.Error.REQUIRE_PERMISSIONS:
                for (int i = 0; i < permissions.length; i++) {
                    // If the granted permission is RECORD_AUDIO, dismiss the snackbar and restart
                    // the Stimshop SDK so it can initialize this time
                    if (permissions[i].equals(permission.RECORD_AUDIO)) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            permSnackbar.dismiss();
                            stimShop.restart();
                        } else {
                            toggleDetectionSwitch.setEnabled(false);
                            toggleDetectionSwitch.setChecked(false);
                        }
                        break;
                    }
                }
                break;
            default:
                Timber.e("%d is not a valid permission requestCode", requestCode);
        }
    }
}