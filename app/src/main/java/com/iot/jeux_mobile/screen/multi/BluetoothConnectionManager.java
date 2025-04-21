package com.iot.jeux_mobile.screen.multi;

import android.bluetooth.BluetoothSocket;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothConnectionManager {
    private static BluetoothConnectionManager instance;
    private BluetoothSocket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService readExecutor;
    private volatile boolean isListening = false;
    private MessageListener messageListener;
    private boolean isClosed = false;

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    private BluetoothConnectionManager() {}

    public static synchronized BluetoothConnectionManager getInstance() {
        if (instance == null) {
            instance = new BluetoothConnectionManager();
        }
        return instance;
    }

    public void init(BluetoothSocket socket) {
        this.socket = socket;
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            this.isClosed = false;
        } catch (Exception e) {
            Log.e("BT_INIT", "Initialization error", e);
        }
    }

    public void sendMessage(String message) {
        writeExecutor.execute(() -> {
            try {
                if (writer != null && !isClosed) {
                    writer.println(message);
                    Log.d("BT_SEND", "Sent: " + message);
                }
            } catch (Exception e) {
                Log.e("BT_SEND", "Send error", e);
            }
        });
    }

    public void listen(MessageListener listener) {
        if (isListening) return;

        this.messageListener = listener;
        this.isListening = true;

        readExecutor = Executors.newSingleThreadExecutor();
        readExecutor.execute(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null && !isClosed) {
                    final String finalLine = line;
                    new android.os.Handler(Looper.getMainLooper()).post(() -> {
                        if (messageListener != null) {
                            messageListener.onMessageReceived(finalLine);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("BT", "Read error", e);
            }
        });
    }

    public void close() {
        if (isClosed) return;
        isClosed = true;

        try {
            if (socket != null) socket.close();
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (readExecutor != null) readExecutor.shutdownNow();
        } catch (Exception e) {
            Log.w("BT_CLOSE", "Error closing resources", e);
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !isClosed;
    }
}