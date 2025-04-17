package com.iot.jeux_mobile.capteur;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class JeuLabyrintheSolo extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new LabyrinthView(this));
    }

    public class LabyrinthView extends View {
        private static final int ROWS = 20;
        private static final int COLS = 10;
        private Paint wallPaint, playerPaint, finishPaint;
        private int[][] maze;
        private int playerRow = ROWS - 1;
        private int playerCol = 0;

        // Coordonnées de la reine (accessible)
        private final int finishRow = 1;
        private final int finishCol = 9;

        public LabyrinthView(Context context) {
            super(context);
            wallPaint = new Paint();
            wallPaint.setColor(Color.BLACK);
            wallPaint.setStrokeWidth(4);

            playerPaint = new Paint();
            playerPaint.setColor(Color.RED);

            finishPaint = new Paint();
            finishPaint.setColor(Color.GREEN);

            initMaze();
        }

        private void initMaze() {
            // 1 = mur, 0 = chemin
            maze = new int[][]{
                    {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                    {1, 0, 0, 0, 0, 0, 0, 1, 0, 0}, // <- reine ici (1,9)
                    {1, 0, 1, 1, 1, 1, 0, 1, 0, 1},
                    {1, 0, 1, 0, 0, 1, 0, 0, 0, 1},
                    {1, 0, 1, 0, 1, 1, 1, 1, 0, 1},
                    {1, 0, 1, 0, 0, 0, 0, 1, 0, 1},
                    {1, 0, 1, 1, 1, 1, 0, 1, 0, 1},
                    {1, 0, 0, 0, 0, 1, 0, 1, 0, 1},
                    {1, 1, 1, 1, 0, 1, 0, 1, 0, 1},
                    {1, 0, 0, 1, 0, 1, 0, 0, 0, 1},
                    {1, 0, 1, 1, 0, 1, 1, 1, 0, 1},
                    {1, 0, 0, 0, 0, 0, 0, 1, 0, 1},
                    {1, 1, 1, 1, 1, 1, 0, 1, 0, 1},
                    {1, 0, 0, 0, 0, 1, 0, 1, 0, 1},
                    {1, 0, 1, 1, 0, 1, 0, 1, 0, 1},
                    {1, 0, 1, 0, 0, 1, 0, 1, 0, 1},
                    {1, 0, 1, 0, 1, 1, 0, 1, 0, 1},
                    {1, 0, 1, 0, 0, 0, 0, 1, 0, 1},
                    {1, 0, 1, 1, 1, 1, 1, 1, 0, 1},
                    {1, 0, 0, 0, 0, 0, 0, 0, 0, 1}
            };
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cellWidth = getWidth() / (float) COLS;
            float cellHeight = getHeight() / (float) ROWS;

            // Dessin des murs
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
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

            // Dessiner le joueur
            canvas.drawCircle(
                    (playerCol + 0.5f) * cellWidth,
                    (playerRow + 0.5f) * cellHeight,
                    Math.min(cellWidth, cellHeight) / 3,
                    playerPaint
            );

            // Dessiner la reine (fin)
            canvas.drawCircle(
                    (finishCol + 0.5f) * cellWidth,
                    (finishRow + 0.5f) * cellHeight,
                    Math.min(cellWidth, cellHeight) / 3,
                    finishPaint
            );
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float y = event.getY();
                float cellWidth = getWidth() / (float) COLS;
                float cellHeight = getHeight() / (float) ROWS;

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

                if (playerRow == finishRow && playerCol == finishCol) {
                    Toast.makeText(getContext(), "🎉 Bravo ! Tu as trouvé la reine !", Toast.LENGTH_LONG).show();
                }

                return true;
            }
            return super.onTouchEvent(event);
        }

        private boolean canMove(int row, int col) {
            return row >= 0 && row < ROWS && col >= 0 && col < COLS && maze[row][col] == 0;
        }
    }
}
