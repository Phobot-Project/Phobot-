package com.teamphobot.robotcontroller;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * MainActivity is the single screen of the PHOBOT Controller app.
 *
 * Architecture flow:
 *
 *   Button press (e.g., FORWARD)
 *     -> RobotController.forward()
 *       -> KobukiPacket.forward()  (builds AA 55 06 01 04 64 00 00 00 checksum)
 *         -> FtdiDevice.send(packet)  (USB bulk transfer to Kobuki)
 *
 *   Voice button press
 *     -> Android speech recognizer
 *       -> AudioCommandManager.mapSpeechToCommand("forward")
 *         -> RobotController.forward()
 *           -> (same flow as above)
 *
 * The FtdiDevice class initializes the Kobuki's FTDI FT232R chip with
 * control transfers (115200 baud, 8N1) before any data is sent.
 * Without this initialization, bulk transfers fail with result -1.
 */
public class MainActivity extends AppCompatActivity
        implements ConnectionManager.ConnectionListener,
                   RobotController.CommandResultListener {

    private static final String TAG = "MainActivity";

    private static final int REQ_RECORD_AUDIO = 1001;
    private static final int REQ_SPEECH = 2001;

    private ConnectionManager connectionManager;
    private RobotController robotController;

    // UI elements
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

        // Find UI elements
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

        // Set up connection and robot controller
        connectionManager = new ConnectionManager(this);
        connectionManager.setConnectionListener(this);
        connectionManager.registerReceiver();

        robotController = new RobotController(connectionManager);
        robotController.setCommandResultListener(this);

        // Button handlers
        btnConnect.setOnClickListener(v -> {
            appendLog("Connecting...");
            connectionManager.connect();
        });

        btnDisconnect.setOnClickListener(v -> {
            appendLog("Disconnecting...");
            connectionManager.disconnect();
        });

        btnForward.setOnClickListener(v -> robotController.forward());
        btnBackward.setOnClickListener(v -> robotController.backward());
        btnLeft.setOnClickListener(v -> robotController.left());
        btnRight.setOnClickListener(v -> robotController.right());
        btnStop.setOnClickListener(v -> robotController.stop());
        btnVoiceCommand.setOnClickListener(v -> startVoiceRecognition());

        // Start disconnected
        setUiDisconnected("Not connected yet");
        appendLog("App started. Press Connect after attaching USB.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Send stop command before closing to be safe
        if (connectionManager.isConnected()) {
            robotController.stop();
        }
        connectionManager.unregisterReceiver();
        connectionManager.disconnect();
    }

    // =====================================================
    // ConnectionManager.ConnectionListener callbacks
    // =====================================================

    @Override
    public void onConnected(String deviceName, int vendorId, int productId) {
        runOnUiThread(() -> {
            tvConnectionStatus.setText("Status: Connected");
            tvConnectionStatus.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.connected_bg));
            tvDeviceInfo.setText(deviceName
                    + " (VID=0x" + Integer.toHexString(vendorId)
                    + " PID=0x" + Integer.toHexString(productId) + ")");
            tvLastError.setText("");

            setMovementButtonsEnabled(true);
            btnConnect.setEnabled(false);
            btnDisconnect.setEnabled(true);
            btnVoiceCommand.setEnabled(true);

            appendLog("Connected: " + deviceName
                    + " VID=0x" + Integer.toHexString(vendorId)
                    + " PID=0x" + Integer.toHexString(productId));
            appendLog("FTDI initialized: 115200 baud, 8N1");
            appendLog("Kobuki ready. Send STOP first, then test movement.");
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
    // RobotController.CommandResultListener callbacks
    // =====================================================

    @Override
    public void onCommandSent(String command, String hexData) {
        runOnUiThread(() -> {
            tvLastAction.setText("Sent: " + command);
            tvLastError.setText("");
            appendLog("Sent " + command + " [" + hexData + "]");
        });
    }

    @Override
    public void onCommandFailed(String command, String reason) {
        runOnUiThread(() -> {
            tvLastAction.setText("Failed: " + command);
            tvLastError.setText(reason);
            appendLog("FAIL " + command + ": " + reason);
        });
    }

    // =====================================================
    // Voice commands
    // =====================================================

    private void startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQ_RECORD_AUDIO);
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
            appendLog("Voice: not available on this device");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_RECORD_AUDIO) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                tvAudioStatus.setText("Microphone permission denied");
                appendLog("Voice: mic permission denied");
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
            appendLog("Voice: robot not connected");
            return;
        }

        String command = AudioCommandManager.mapSpeechToCommand(spokenText);

        if (command == null) {
            tvAudioStatus.setText("No valid command detected");
            appendLog("Voice: no match for \"" + spokenText + "\"");
            return;
        }

        tvAudioStatus.setText("Matched: " + command);
        appendLog("Voice: matched " + command);

        switch (command) {
            case RobotController.CMD_FORWARD:  robotController.forward();  break;
            case RobotController.CMD_BACKWARD: robotController.backward(); break;
            case RobotController.CMD_LEFT:     robotController.left();     break;
            case RobotController.CMD_RIGHT:    robotController.right();    break;
            case RobotController.CMD_STOP:     robotController.stop();     break;
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

        setMovementButtonsEnabled(false);
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
        btnVoiceCommand.setEnabled(false);

        appendLog("Disconnected: " + reason);
    }

    private void setMovementButtonsEnabled(boolean enabled) {
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
