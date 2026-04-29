package com.teamphobot.robotcontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity is the app screen for PHOBOT Controller.
 * Supports USB, Bluetooth, and WiFi connection modes.
 * All modes send identical binary packets to the robot.
 */
public class MainActivity extends AppCompatActivity
        implements ConnectionManager.ConnectionListener,
                   RobotController.CommandResultListener {

    private static final String TAG = "MainActivity";
    private static final int REQ_RECORD_AUDIO = 1001;
    private static final int REQ_SPEECH = 2001;
    private static final int REQ_BT_PERMISSION = 3001;

    private ConnectionManager connectionManager;
    private RobotController robotController;

    // UI elements
    private RadioGroup rgConnectionType;
    private LinearLayout layoutBluetooth;
    private LinearLayout layoutWifi;
    private Button btnSelectBtDevice;
    private TextView tvBtDeviceName;
    private EditText etWifiIp;
    private EditText etWifiPort;

    private TextView tvConnectionStatus;
    private TextView tvDeviceInfo;
    private TextView tvRecognizedSpeech;
    private TextView tvAudioStatus;
    private TextView tvLastAction;
    private TextView tvLastError;
    private TextView tvEventLog;

    private Button btnConnect;
    private Button btnDisconnect;
    private Button btnForward;
    private Button btnBackward;
    private Button btnLeft;
    private Button btnRight;
    private Button btnStop;
    private Button btnVoiceCommand;

    private final StringBuilder eventLog = new StringBuilder();
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connection type selector
        rgConnectionType = findViewById(R.id.rgConnectionType);
        layoutBluetooth  = findViewById(R.id.layoutBluetooth);
        layoutWifi       = findViewById(R.id.layoutWifi);
        btnSelectBtDevice = findViewById(R.id.btnSelectBtDevice);
        tvBtDeviceName    = findViewById(R.id.tvBtDeviceName);
        etWifiIp   = findViewById(R.id.etWifiIp);
        etWifiPort = findViewById(R.id.etWifiPort);

        // Status and controls
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvDeviceInfo       = findViewById(R.id.tvDeviceInfo);
        tvRecognizedSpeech = findViewById(R.id.tvRecognizedSpeech);
        tvAudioStatus      = findViewById(R.id.tvAudioStatus);
        tvLastAction       = findViewById(R.id.tvLastAction);
        tvLastError        = findViewById(R.id.tvLastError);
        tvEventLog         = findViewById(R.id.tvEventLog);

        btnConnect      = findViewById(R.id.btnConnect);
        btnDisconnect   = findViewById(R.id.btnDisconnect);
        btnForward      = findViewById(R.id.btnForward);
        btnBackward     = findViewById(R.id.btnBackward);
        btnLeft         = findViewById(R.id.btnLeft);
        btnRight        = findViewById(R.id.btnRight);
        btnStop         = findViewById(R.id.btnStop);
        btnVoiceCommand = findViewById(R.id.btnVoiceCommand);

        // Set up managers
        connectionManager = new ConnectionManager(this);
        connectionManager.setConnectionListener(this);
        connectionManager.registerReceiver();

        robotController = new RobotController(connectionManager);
        robotController.setCommandResultListener(this);

        // Connection type radio buttons
        rgConnectionType.setOnCheckedChangeListener((group, checkedId) -> {
            layoutBluetooth.setVisibility(View.GONE);
            layoutWifi.setVisibility(View.GONE);

            if (checkedId == R.id.rbUsb) {
                connectionManager.setMode(ConnectionManager.Mode.USB);
                appendLog("Mode: USB");
            } else if (checkedId == R.id.rbBluetooth) {
                connectionManager.setMode(ConnectionManager.Mode.BLUETOOTH);
                layoutBluetooth.setVisibility(View.VISIBLE);
                appendLog("Mode: Bluetooth");
            } else if (checkedId == R.id.rbWifi) {
                connectionManager.setMode(ConnectionManager.Mode.WIFI);
                layoutWifi.setVisibility(View.VISIBLE);
                appendLog("Mode: WiFi");
            }
        });

        // Bluetooth device picker
        btnSelectBtDevice.setOnClickListener(v -> showBluetoothDevicePicker());

        // Connect / Disconnect
        btnConnect.setOnClickListener(v -> handleConnect());

        btnDisconnect.setOnClickListener(v -> {
            robotController.stop();
            appendLog("Disconnecting...");
            connectionManager.disconnect();
        });

        // Movement buttons
        btnForward.setOnClickListener(v -> robotController.forward());
        btnBackward.setOnClickListener(v -> robotController.backward());
        btnLeft.setOnClickListener(v -> robotController.left());
        btnRight.setOnClickListener(v -> robotController.right());
        btnStop.setOnClickListener(v -> robotController.stop());
        btnVoiceCommand.setOnClickListener(v -> startVoiceRecognition());

        setUiDisconnected("Not connected yet");
        appendLog("App started. Select connection mode and press Connect.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        robotController.shutdown();
        connectionManager.unregisterReceiver();
        connectionManager.disconnect();
    }

    // =====================================================
    // Connect handler
    // =====================================================

    private void handleConnect() {
        ConnectionManager.Mode mode = connectionManager.getMode();

        switch (mode) {
            case USB:
                appendLog("Connecting via USB...");
                connectionManager.connect();
                break;

            case BLUETOOTH:
                if (!ensureBluetoothPermission()) return;
                appendLog("Connecting via Bluetooth...");
                connectionManager.connect();
                break;

            case WIFI:
                String ip = etWifiIp.getText().toString().trim();
                String portStr = etWifiPort.getText().toString().trim();
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show();
                    return;
                }
                connectionManager.setWifiTarget(ip, port);
                appendLog("Connecting via WiFi to " + ip + ":" + port + "...");
                connectionManager.connect();
                break;
        }
    }

    // =====================================================
    // Bluetooth device picker
    // =====================================================

    @SuppressLint("MissingPermission")
    private void showBluetoothDevicePicker() {
        if (!ensureBluetoothPermission()) return;

        BluetoothHelper btHelper = connectionManager.getBluetoothHelper();

        if (!btHelper.isBluetoothAvailable()) {
            Toast.makeText(this, "Bluetooth is not available on this device", Toast.LENGTH_LONG).show();
            return;
        }

        if (!btHelper.isBluetoothEnabled()) {
            Toast.makeText(this, "Please turn on Bluetooth in Settings", Toast.LENGTH_LONG).show();
            return;
        }

        List<BluetoothDevice> paired = btHelper.getPairedDevices();

        if (paired.isEmpty()) {
            Toast.makeText(this, "No paired Bluetooth devices. Pair the HC-05/HC-06 in Settings first.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] names = new String[paired.size()];
        for (int i = 0; i < paired.size(); i++) {
            BluetoothDevice d = paired.get(i);
            try {
                String name = d.getName();
                names[i] = (name != null ? name : "Unknown") + "\n" + d.getAddress();
            } catch (SecurityException e) {
                names[i] = d.getAddress();
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Bluetooth Device")
                .setItems(names, (dialog, which) -> {
                    BluetoothDevice picked = paired.get(which);
                    connectionManager.setBluetoothDevice(picked);
                    try {
                        String name = picked.getName();
                        tvBtDeviceName.setText(name != null ? name : picked.getAddress());
                    } catch (SecurityException e) {
                        tvBtDeviceName.setText(picked.getAddress());
                    }
                    appendLog("BT device selected: " + tvBtDeviceName.getText());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean ensureBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        REQ_BT_PERMISSION);
                return false;
            }
        }
        return true;
    }

    // =====================================================
    // ConnectionManager.ConnectionListener
    // =====================================================

    @Override
    public void onConnected(String deviceInfo, ConnectionManager.Mode mode) {
        runOnUiThread(() -> {
            tvConnectionStatus.setText("Status: Connected (" + mode.name() + ")");

            int bgColor;
            switch (mode) {
                case BLUETOOTH: bgColor = ContextCompat.getColor(this, R.color.bt_bg); break;
                case WIFI:      bgColor = ContextCompat.getColor(this, R.color.wifi_bg); break;
                default:        bgColor = ContextCompat.getColor(this, R.color.connected_bg); break;
            }
            tvConnectionStatus.setBackgroundColor(bgColor);
            tvDeviceInfo.setText(deviceInfo);
            tvLastError.setText("");

            setMovementEnabled(true);
            btnConnect.setEnabled(false);
            btnDisconnect.setEnabled(true);
            btnVoiceCommand.setEnabled(true);

            appendLog("Connected: " + deviceInfo);
        });
    }

    @Override
    public void onDisconnected(String reason) {
        runOnUiThread(() -> setUiDisconnected(reason));
    }

    @Override
    public void onError(String errorMessage) {
        runOnUiThread(() -> {
            tvLastError.setText(errorMessage);
            appendLog("ERROR: " + errorMessage);
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    // =====================================================
    // RobotController.CommandResultListener
    // =====================================================

    @Override
    public void onCommandSent(String command) {
        runOnUiThread(() -> {
            tvLastAction.setText("Sent: " + command);
            tvLastError.setText("");
            appendLog("Sent: " + CommandEncoder.label(command));
        });
    }

    @Override
    public void onCommandFailed(String command, String reason) {
        runOnUiThread(() -> {
            tvLastAction.setText("Failed: " + command);
            tvLastError.setText(reason);
            appendLog("FAIL: " + CommandEncoder.label(command) + " - " + reason);
        });
    }

    // =====================================================
    // Voice commands
    // =====================================================

    private void startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a robot command");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        try {
            startActivityForResult(intent, REQ_SPEECH);
        } catch (ActivityNotFoundException e) {
            tvAudioStatus.setText("Speech recognition not available");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                tvAudioStatus.setText("Microphone permission denied");
            }
        }

        if (requestCode == REQ_BT_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appendLog("Bluetooth permission granted");
            } else {
                appendLog("Bluetooth permission denied");
                Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0).toLowerCase().trim();
                tvRecognizedSpeech.setText("Heard: \"" + spokenText + "\"");
                handleVoiceCommand(spokenText);
            }
        }
    }

    private void handleVoiceCommand(String spokenText) {
        if (!connectionManager.isConnected()) {
            tvAudioStatus.setText("Robot is not connected");
            return;
        }
        String command = AudioCommandManager.mapSpeechToCommand(spokenText);
        if (command == null) {
            tvAudioStatus.setText("No valid command detected");
            return;
        }
        tvAudioStatus.setText("Matched: " + command);
        appendLog("Voice: " + command);
        switch (command) {
            case CommandEncoder.CMD_FORWARD:  robotController.forward(); break;
            case CommandEncoder.CMD_BACKWARD: robotController.backward(); break;
            case CommandEncoder.CMD_LEFT:     robotController.left(); break;
            case CommandEncoder.CMD_RIGHT:    robotController.right(); break;
            case CommandEncoder.CMD_STOP:     robotController.stop(); break;
        }
    }

    // =====================================================
    // UI helpers
    // =====================================================

    private void setUiDisconnected(String reason) {
        tvConnectionStatus.setText("Status: Disconnected");
        tvConnectionStatus.setBackgroundColor(
                ContextCompat.getColor(this, R.color.disconnected_bg));
        tvDeviceInfo.setText("No device detected");
        setMovementEnabled(false);
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
        btnVoiceCommand.setEnabled(false);
        appendLog("Disconnected: " + reason);
    }

    private void setMovementEnabled(boolean enabled) {
        btnForward.setEnabled(enabled);
        btnBackward.setEnabled(enabled);
        btnLeft.setEnabled(enabled);
        btnRight.setEnabled(enabled);
        btnStop.setEnabled(enabled);
    }

    private void appendLog(String message) {
        String time = timeFormat.format(new Date());
        eventLog.append(time).append("  ").append(message).append("\n");
        tvEventLog.setText(eventLog.toString());
        Log.d(TAG, message);
    }
}
