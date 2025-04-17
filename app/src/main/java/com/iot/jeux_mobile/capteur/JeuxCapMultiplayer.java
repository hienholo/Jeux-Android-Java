package com.iot.jeux_mobile.capteur;

import android.Manifest;
import android.bluetooth.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.iot.jeux_mobile.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

 public class JeuxCapMultiplayer extends AppCompatActivity implements SensorEventListener {

    // Views
    private TextView playerScoreText;
    private TextView opponentScoreText;
    private GameView gameView;
    private Button resetButton;

    // Game state
    private int playerScore = 0;
    private int opponentScore = 0;
    private boolean gameOver = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();

    // Sensors
    private SensorManager sensorManager;
    private Sensor accelerometer;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThread connectedThread;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jeux_cap_multiplayer);

        initViews();
        checkAndRequestPermissions();
    }

     public void redraw() {
//         Canvas canvas = holder.lockCanvas();
//         if (canvas != null) {
//             try {
//                 drawGame(canvas);
//             } finally {
//                 holder.unlockCanvasAndPost(canvas);
//             }
//         }
     }
    private void initViews() {
        playerScoreText = findViewById(R.id.playerScoreText);
        opponentScoreText = findViewById(R.id.opponentScoreText);
        //gameView = findViewById(R.id.gameView);
        resetButton = findViewById(R.id.resetButton);

        resetButton.setOnClickListener(v -> resetGame());
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non supporté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            setupConnection();
        }
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        REQUEST_BLUETOOTH_PERMISSIONS);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        REQUEST_LOCATION_PERMISSION);
                return false;
            }
        }
        return true;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void setupConnection() {
        boolean isHost = getIntent().getBooleanExtra("isHost", false);
        if (isHost) {
            new AcceptThread().start();
        } else {
            BluetoothDevice device = getIntent().getParcelableExtra("device");
            if (device != null) {
                new ConnectThread(device).start();
            }
        }
        initSensors();
    }

    private void resetGame() {
        playerScore = 0;
        opponentScore = 0;
        gameOver = false;
        updateScores();
        gameView.reset();
    }

    private void updateScores() {
        runOnUiThread(() -> {
            playerScoreText.setText("Vous: " + playerScore);
            opponentScoreText.setText("Adv: " + opponentScore);
        });
    }

    public void gameOver() {
        gameOver = true;
        sendMessage("GAME_OVER");
        runOnUiThread(this::showGameOverDialog);
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Partie terminée")
                .setMessage("Votre score: " + playerScore + "\nScore adversaire: " + opponentScore)
                .setPositiveButton("Rejouer", (dialog, which) -> resetGame())
                .setNegativeButton("Quitter", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!gameOver && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gameView.updateBall(-event.values[0], event.values[1]);
            playerScore += random.nextInt(2) + 2; // 2 ou 3 points
            updateScores();
            sendMessage("SCORE:" + playerScore);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void sendMessage(String message) {
        if (connectedThread != null) {
            connectedThread.write(message.getBytes());
        }
    }

    /* Bluetooth Thread Classes */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                if (ActivityCompat.checkSelfPermission(JeuxCapMultiplayer.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            "JeuxCapMultiplayer", MY_UUID);
                }
            } catch (IOException e) {
                Toast.makeText(JeuxCapMultiplayer.this, "Erreur création socket", Toast.LENGTH_SHORT).show();
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                if (socket != null) {
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) { /* Ignore */ }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { /* Ignore */ }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                if (ActivityCompat.checkSelfPermission(JeuxCapMultiplayer.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                }
            } catch (IOException e) {
                Toast.makeText(JeuxCapMultiplayer.this, "Erreur création socket", Toast.LENGTH_SHORT).show();
            }
            mmSocket = tmp;
        }

        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
        public void run() {
            if (ActivityCompat.checkSelfPermission(JeuxCapMultiplayer.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery();
            }

            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) { /* Ignore */ }
                return;
            }

            manageConnectedSocket(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { /* Ignore */ }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        bluetoothSocket = socket;
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final byte[] mmBuffer = new byte[1024];

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { /* Ignore */ }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            int numBytes;
            while (true) {
                try {
                    numBytes = mmInStream.read(mmBuffer);
                    String message = new String(mmBuffer, 0, numBytes);
                    processMessage(message);
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { /* Ignore */ }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { /* Ignore */ }
        }
    }

    private void processMessage(final String message) {
        if (message.startsWith("SCORE:")) {
            opponentScore = Integer.parseInt(message.substring(6));
            handler.post(this::updateScores);
        } else if (message.equals("GAME_OVER")) {
            gameOver = true;
            handler.post(this::showGameOverDialog);
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS || requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initBluetooth();
            } else {
                Toast.makeText(this, "Permissions requises", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                setupConnection();
            } else {
                Toast.makeText(this, "Le Bluetooth doit être activé", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) { /* Ignore */ }
        }
    }
    /* GameView Inner Class */
    public class GameView extends SurfaceView implements SurfaceHolder.Callback {
        private final SurfaceHolder holder;
        private final Paint ballPaint = new Paint();
        private final Paint opponentBallPaint = new Paint();
        private final Paint holePaint = new Paint();
        private final Paint backgroundPaint = new Paint();

        private float ballX, ballY;
        private float opponentBallX, opponentBallY;
        private float ballRadius = 30f;
        private float holeRadius = 50f;

        private ArrayList<Hole> holes = new ArrayList<>();
        private float width, height;
        private Handler holeHandler = new Handler();
        private static final int HOLE_SPAWN_INTERVAL = 3000;
        private Random random = new Random();
        private boolean gameRunning = true;
        private Thread gameThread;

        public GameView(Context context) {
            super(context);
            holder = getHolder();
            holder.addCallback(this);

            // Configuration des peintures
            ballPaint.setColor(Color.BLUE);
            ballPaint.setStyle(Paint.Style.FILL);

            opponentBallPaint.setColor(Color.RED);
            opponentBallPaint.setStyle(Paint.Style.FILL);

            holePaint.setColor(Color.BLACK);
            holePaint.setStyle(Paint.Style.FILL);

            backgroundPaint.setColor(Color.WHITE);
            backgroundPaint.setStyle(Paint.Style.FILL);
        }

        // Méthode principale de mise à jour du jeu
        public void update() {
            while (gameRunning) {
                if (!holder.getSurface().isValid()) continue;

                Canvas canvas = holder.lockCanvas();
                try {
                    synchronized (holder) {
                        // Dessin principal
                        drawGame(canvas);
                    }
                } finally {
                    if (canvas != null) {
                        holder.unlockCanvasAndPost(canvas);
                    }
                }

                // Limite à ~60 FPS
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void drawGame(Canvas canvas) {
            // Fond d'écran
            canvas.drawRect(0, 0, width, height, backgroundPaint);

            // Dessiner les trous
            for (Hole hole : holes) {
                canvas.drawCircle(hole.x, hole.y, holeRadius, holePaint);
            }

            // Dessiner les balles
            canvas.drawCircle(ballX, ballY, ballRadius, ballPaint);
            canvas.drawCircle(opponentBallX, opponentBallY, ballRadius, opponentBallPaint);
        }

        public void updateBall(float dx, float dy) {
            synchronized (holder) {
                ballX += dx * 5;
                ballY += dy * 5;

                // Garder la balle dans les limites
                ballX = Math.max(ballRadius, Math.min(width - ballRadius, ballX));
                ballY = Math.max(ballRadius, Math.min(height - ballRadius, ballY));

                checkCollisions();
            }
        }

        public void updateOpponentBall(float x, float y) {
            synchronized (holder) {
                opponentBallX = width/2 - x * 5;
                opponentBallY = height/2 - y * 5;

                opponentBallX = Math.max(ballRadius, Math.min(width - ballRadius, opponentBallX));
                opponentBallY = Math.max(ballRadius, Math.min(height - ballRadius, opponentBallY));
            }
        }

        private void checkCollisions() {
            for (Hole hole : holes) {
                if (distance(ballX, ballY, hole.x, hole.y) < holeRadius) {
                    ((JeuxCapMultiplayer) getContext()).gameOver();
                    break;
                }
            }
        }

        private float distance(float x1, float y1, float x2, float y2) {
            return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
        }

        public void reset() {
            synchronized (holder) {
                ballX = width / 2;
                ballY = height / 2;
                opponentBallX = width / 2;
                opponentBallY = height / 2;
                holes.clear();
                startHoleSpawning();
            }
        }

        private void startHoleSpawning() {
            holeHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (gameRunning) {
                        addNewHole();
                        holeHandler.postDelayed(this, HOLE_SPAWN_INTERVAL);
                    }
                }
            }, HOLE_SPAWN_INTERVAL);
        }

        public void addNewHole() {
            synchronized (holder) {
                float x = random.nextFloat() * (width - 2 * holeRadius) + holeRadius;
                float y = random.nextFloat() * (height - 2 * holeRadius) + holeRadius;
                float dx = (random.nextFloat() - 0.5f) * 6;
                float dy = (random.nextFloat() - 0.5f) * 6;
                holes.add(new Hole(x, y, dx, dy));
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            width = getWidth();
            height = getHeight();

            gameRunning = true;
            gameThread = new Thread(this::update);
            gameThread.start();

            reset();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            gameRunning = false;
            holeHandler.removeCallbacksAndMessages(null);
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private class Hole {
            float x, y, dx, dy;

            Hole(float x, float y, float dx, float dy) {
                this.x = x;
                this.y = y;
                this.dx = dx;
                this.dy = dy;
            }

            void updatePosition() {
                x += dx;
                y += dy;

                // Rebond sur les bords
                if (x < holeRadius || x > width - holeRadius) dx = -dx;
                if (y < holeRadius || y > height - holeRadius) dy = -dy;
            }
        }
    }
}