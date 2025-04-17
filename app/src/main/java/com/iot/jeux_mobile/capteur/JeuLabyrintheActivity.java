package com.iot.jeux_mobile.capteur;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import com.iot.jeux_mobile.R;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class JeuLabyrintheActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new LabyrinthView(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LabyrinthView.releaseMediaPlayers();
    }

    public static class LabyrinthView extends View {
        private int baseRows = 10;
        private int baseCols = 10;
        private int currentRows, currentCols;
        private Paint wallPaint, playerPaint, queenPaint, textPaint, timePaint;
        private int[][] maze;
        private int playerRow, playerCol;
        private int queenRow, queenCol;
        private int currentLevel = 1;
        private final int MAX_LEVEL = 4;
        private float wallThickness = 4f;
        private Random random = new Random();

        // Chronomètre
        private long startTime = 0;
        private long elapsedTime = 0;
        private boolean timerRunning = false;
        private SharedPreferences prefs;
        private String PREFS_NAME = "LabyrinthPrefs";

        // Audio
        private static MediaPlayer moveSound;
        private static MediaPlayer victoryMusic;
        private boolean soundsEnabled = true;

        public LabyrinthView(Context context) {
            super(context);
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            initPaints();
            initSounds(context);
            generateNewLevel();
        }

        private void initPaints() {
            wallPaint = new Paint();
            wallPaint.setColor(Color.BLACK);
            wallPaint.setStyle(Paint.Style.FILL);

            playerPaint = new Paint();
            playerPaint.setColor(Color.RED);
            playerPaint.setStyle(Paint.Style.FILL);

            queenPaint = new Paint();
            queenPaint.setColor(Color.MAGENTA);
            queenPaint.setStyle(Paint.Style.FILL);

            textPaint = new Paint();
            textPaint.setColor(Color.BLUE);
            textPaint.setTextSize(50);
            textPaint.setFakeBoldText(true);

            timePaint = new Paint();
            timePaint.setColor(Color.DKGRAY);
            timePaint.setTextSize(40);
            timePaint.setFakeBoldText(true);
        }

        private void initSounds(Context context) {
            // Son de déplacement
            moveSound = MediaPlayer.create(context, R.raw.move_sound);
            moveSound.setVolume(0.3f, 0.3f);

            // Musique de victoire
            victoryMusic = MediaPlayer.create(context, R.raw.victory_music);
            victoryMusic.setVolume(0.7f, 0.7f);
            victoryMusic.setLooping(false);
        }

        public static void releaseMediaPlayers() {
            if (moveSound != null) {
                moveSound.release();
                moveSound = null;
            }
            if (victoryMusic != null) {
                victoryMusic.release();
                victoryMusic = null;
            }
        }

        private void generateNewLevel() {
            resetTimer();
            currentRows = baseRows + (currentLevel * 2);
            currentCols = baseCols + (currentLevel * 2);
            wallThickness = Math.max(2f, 6f - currentLevel);

            generateRandomMaze();
            placeQueenInCorner();
            placePlayerOppositeQueen();

            while (!isSolvable(playerRow, playerCol, queenRow, queenCol)) {
                generateRandomMaze();
                placeQueenInCorner();
                placePlayerOppositeQueen();
            }

            startTimer();
            invalidate();
        }

        private void generateRandomMaze() {
            maze = new int[currentRows][currentCols];
            float wallProbability = 0.25f + (currentLevel * 0.05f);

            for (int i = 0; i < currentRows; i++) {
                for (int j = 0; j < currentCols; j++) {
                    maze[i][j] = random.nextFloat() < wallProbability ? 1 : 0;
                }
            }

            for (int i = 0; i < currentRows; i++) {
                maze[i][0] = 1;
                maze[i][currentCols-1] = 1;
            }
            for (int j = 0; j < currentCols; j++) {
                maze[0][j] = 1;
                maze[currentRows-1][j] = 1;
            }
        }

        private void placeQueenInCorner() {
            int corner = (currentLevel-1) % 4;
            switch (corner) {
                case 0: queenRow = 1; queenCol = currentCols - 2; break;
                case 1: queenRow = currentRows - 2; queenCol = currentCols - 2; break;
                case 2: queenRow = 1; queenCol = 1; break;
                case 3: queenRow = currentRows - 2; queenCol = 1; break;
            }
            maze[queenRow][queenCol] = 0;
        }

        private void placePlayerOppositeQueen() {
            if (queenRow < currentRows/2) playerRow = currentRows - 2;
            else playerRow = 1;

            if (queenCol < currentCols/2) playerCol = currentCols - 2;
            else playerCol = 1;

            maze[playerRow][playerCol] = 0;
        }

        private boolean isSolvable(int startRow, int startCol, int endRow, int endCol) {
            if (maze[startRow][startCol] == 1 || maze[endRow][endCol] == 1) return false;

            boolean[][] visited = new boolean[currentRows][currentCols];
            Queue<int[]> queue = new LinkedList<>();
            queue.add(new int[]{startRow, startCol});
            visited[startRow][startCol] = true;

            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

            while (!queue.isEmpty()) {
                int[] current = queue.poll();
                if (current[0] == endRow && current[1] == endCol) return true;

                for (int[] dir : directions) {
                    int newRow = current[0] + dir[0];
                    int newCol = current[1] + dir[1];

                    if (newRow >= 0 && newRow < currentRows && newCol >= 0 && newCol < currentCols
                            && maze[newRow][newCol] == 0 && !visited[newRow][newCol]) {
                        visited[newRow][newCol] = true;
                        queue.add(new int[]{newRow, newCol});
                    }
                }
            }
            return false;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cellWidth = getWidth() / (float) currentCols;
            float cellHeight = getHeight() / (float) currentRows;

            wallPaint.setStrokeWidth(wallThickness);
            for (int row = 0; row < currentRows; row++) {
                for (int col = 0; col < currentCols; col++) {
                    if (maze[row][col] == 1) {
                        canvas.drawRect(
                                col * cellWidth,
                                row * cellHeight,
                                (col + 1) * cellWidth,
                                (row + 1) * cellHeight,
                                wallPaint
                        );
                    }
                }
            }

            float playerSize = Math.min(cellWidth, cellHeight) / 3;
            canvas.drawCircle(
                    (playerCol + 0.5f) * cellWidth,
                    (playerRow + 0.5f) * cellHeight,
                    playerSize,
                    playerPaint
            );

            float queenSize = Math.min(cellWidth, cellHeight) / 3;
            canvas.drawCircle(
                    (queenCol + 0.5f) * cellWidth,
                    (queenRow + 0.5f) * cellHeight,
                    queenSize,
                    queenPaint
            );

            canvas.drawText("Niveau " + currentLevel, 30, 50, textPaint);
            canvas.drawText(currentCols + "x" + currentRows, 30, 90, textPaint);

            if (timerRunning) {
                elapsedTime = SystemClock.elapsedRealtime() - startTime;
            }
            canvas.drawText("Temps: " + formatTime(elapsedTime), 30, 140, timePaint);

            long bestTime = prefs.getLong("best_time_" + currentLevel, 0);
            if (bestTime > 0) {
                canvas.drawText("Record: " + formatTime(bestTime), getWidth() - 350, 50, timePaint);
            }
        }

        private String formatTime(long millis) {
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }

        private void startTimer() {
            startTime = SystemClock.elapsedRealtime() - elapsedTime;
            timerRunning = true;
        }

        private void pauseTimer() {
            elapsedTime = SystemClock.elapsedRealtime() - startTime;
            timerRunning = false;
        }

        private void resetTimer() {
            startTime = 0;
            elapsedTime = 0;
            timerRunning = false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                playMoveSound();

                float x = event.getX();
                float y = event.getY();
                float cellWidth = getWidth() / (float) currentCols;
                float cellHeight = getHeight() / (float) currentRows;

                float px = (playerCol + 0.5f) * cellWidth;
                float py = (playerRow + 0.5f) * cellHeight;

                float dx = x - px;
                float dy = y - py;

                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 0 && canMove(playerRow, playerCol + 1)) playerCol++;
                    else if (dx < 0 && canMove(playerRow, playerCol - 1)) playerCol--;
                } else {
                    if (dy > 0 && canMove(playerRow + 1, playerCol)) playerRow++;
                    else if (dy < 0 && canMove(playerRow - 1, playerCol)) playerRow--;
                }

                invalidate();

                if (playerRow == queenRow && playerCol == queenCol) {
                    pauseTimer();
                    checkAndSaveBestTime();
                    playVictorySound();
                    showLevelCompleteDialog();
                }

                return true;
            }
            return super.onTouchEvent(event);
        }

        private void playMoveSound() {
            if (soundsEnabled && moveSound != null) {
                moveSound.seekTo(0);
                moveSound.start();
            }
        }

        private void playVictorySound() {
            if (soundsEnabled && victoryMusic != null) {
                victoryMusic.seekTo(0);
                victoryMusic.start();
            }
        }

        private void checkAndSaveBestTime() {
            long bestTime = prefs.getLong("best_time_" + currentLevel, Long.MAX_VALUE);
            if (elapsedTime < bestTime) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("best_time_" + currentLevel, elapsedTime);
                editor.apply();
            }
        }

        private boolean canMove(int row, int col) {
            return row >= 0 && row < currentRows && col >= 0 && col < currentCols && maze[row][col] == 0;
        }

        private void showLevelCompleteDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Reine sauvée!");

            long bestTime = prefs.getLong("best_time_" + currentLevel, Long.MAX_VALUE);
            boolean isNewRecord = elapsedTime < bestTime;

            String timeMessage = "Votre temps: " + formatTime(elapsedTime) + "\n";
            if (isNewRecord) {
                timeMessage += "Nouveau record!";
            } else if (bestTime != Long.MAX_VALUE) {
                timeMessage += "Record: " + formatTime(bestTime);
            }

            builder.setMessage("Niveau " + currentLevel + " complété!\n" + timeMessage);

            builder.setNeutralButton(soundsEnabled ? "Désactiver sons" : "Activer sons",
                    (dialog, which) -> {
                        soundsEnabled = !soundsEnabled;
                    });

            if (currentLevel < MAX_LEVEL) {
                builder.setPositiveButton("Niveau suivant", (dialog, which) -> {
                    currentLevel++;
                    generateNewLevel();
                });
            } else {
                builder.setMessage("Victoire royale!\n" + timeMessage);
                builder.setPositiveButton("Rejouer", (dialog, which) -> {
                    currentLevel = 1;
                    generateNewLevel();
                });
            }

            builder.setNegativeButton("Quitter", (dialog, which) -> {
                ((Activity)getContext()).finish();
            });

            builder.setCancelable(false);
            builder.show();
        }
    }
}