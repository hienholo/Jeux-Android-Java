package com.iot.jeux_mobile.capteur;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.view.animation.LinearInterpolator;

import com.iot.jeux_mobile.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class JeuxCap2 extends AppCompatActivity implements SensorEventListener {

    private ImageView vaisseauJoueur;
    private TextView scoreTextView;
    private ProgressBar barreVie;

    private float thresholdGyro = 0.2f;
    private float defilementVaisseau;
    private long durationEnnemi = 4000L;
    private int score = 0;
    private int vieRestante = 100;

    private final int NOMBRE_ENNEMIS_SUIVEURS = 2;
    private final List<ImageView> ennemis = new ArrayList<>();
    private final List<ImageView> ennemisSuiveurs = new ArrayList<>();
    private final List<Animator> animationsEnnemi = new ArrayList<>();
    private final List<ImageView> projectilesJoueur = new ArrayList<>();
    private final List<ImageView> projectilesEnnemisSuiveurs = new ArrayList<>(); // Liste pour les projectiles des suiveurs
    private final Random random = new Random();
    private final float VITESSE_SUIVEUR = 5f;
    private Long[] delayListEnnemi = {500L, 1000L, 1500L, 2000L, 2500L};

    private Handler handler = new Handler();
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private int screenWidth;
    private int screenHeight;
    private ConstraintLayout gameLayout;

    private final int nombreEnnemis = 5;
    private MediaPlayer gameOverSound, backgroundMusic;


    private void initSounds() {
        gameOverSound = MediaPlayer.create(this, R.raw.game_over);
        initBackgroundMusic();
    }
    private void initBackgroundMusic() {
        backgroundMusic = MediaPlayer.create(this, R.raw.background_music); // <-- Ton fichier mp3 dans res/raw
        backgroundMusic.setLooping(true); // Boucle infinie
        backgroundMusic.setVolume(0.5f, 0.5f); // Volume modéré
        backgroundMusic.start();
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jeux_cap2);

        gameLayout = findViewById(R.id.modesolo2);
        vaisseauJoueur = findViewById(R.id.chat);
        scoreTextView = findViewById(R.id.scoregame2);
        barreVie = findViewById(R.id.progressVie);
        barreVie.setMax(100);
        barreVie.setProgress(vieRestante);

        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        defilementVaisseau = (screenWidth / 3.0f);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        creerEnnemis();
        creerEnnemisSuiveurs();
        bougerEnnemis();
        bougerEnnemisSuiveurs();
        shuffleDelayEnnemi();
        updateScore();
        detecterCollisions();
        initSounds();


        handler.postDelayed(this::tirerProjectile, 1000);
        handler.postDelayed(this::tirerProjectileEnnemiSuiveur, 2000); // Démarrer les tirs des suiveurs
        ViewCompat.setOnApplyWindowInsetsListener(gameLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void creerEnnemis() {
        for (int i = 0; i < nombreEnnemis; i++) {
            ImageView ennemi = new ImageView(this);
            ennemi.setImageResource(R.drawable.vaisseau_ennemi);
            int ennemiWidth = 80;
            int ennemiHeight = 80;
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ennemiWidth, ennemiHeight);
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            ennemi.setLayoutParams(params);
            ennemi.setY(-ennemiHeight);
            ennemi.setX(random.nextInt(screenWidth - ennemiWidth));
            ennemi.setVisibility(View.VISIBLE);
            gameLayout.addView(ennemi);
            ennemis.add(ennemi);
        }
    }

    private void creerEnnemisSuiveurs() {
        for (int i = 0; i < NOMBRE_ENNEMIS_SUIVEURS; i++) {
            ImageView ennemiSuiveur = new ImageView(this);
            ennemiSuiveur.setImageResource(R.drawable.vaisseau_ennemi_suiveur);
            int ennemiWidth = 70;
            int ennemiHeight = 70;
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ennemiWidth, ennemiHeight);
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            ennemiSuiveur.setLayoutParams(params);
            // Initial positionnement des ennemis suiveurs
            if (random.nextBoolean()) { // Apparition à gauche ou à droite
                ennemiSuiveur.setX(random.nextFloat() < 0.5 ? -ennemiWidth : screenWidth);
                ennemiSuiveur.setY(random.nextInt(screenHeight / 2)); // Apparition sur la moitié supérieure de l'écran
            } else { // Apparition en haut
                ennemiSuiveur.setX(random.nextInt(screenWidth - ennemiWidth));
                ennemiSuiveur.setY(-ennemiHeight);
            }
            ennemiSuiveur.setVisibility(View.VISIBLE);
            gameLayout.addView(ennemiSuiveur);
            ennemisSuiveurs.add(ennemiSuiveur);
        }
    }

    private void bougerEnnemisSuiveurs() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (ImageView ennemiSuiveur : ennemisSuiveurs) {
                    if (ennemiSuiveur.getVisibility() == View.VISIBLE) {
                        float joueurX = vaisseauJoueur.getX() + (vaisseauJoueur.getWidth() / 2f);
                        float joueurY = vaisseauJoueur.getY() + (vaisseauJoueur.getHeight() / 2f);
                        float ennemiX = ennemiSuiveur.getX() + (ennemiSuiveur.getWidth() / 2f);
                        float ennemiY = ennemiSuiveur.getY() + (ennemiSuiveur.getHeight() / 2f);

                        // Calcul de la direction vers le joueur
                        float dx = joueurX - ennemiX;
                        float dy = joueurY - ennemiY;
                        float distance = (float) Math.sqrt(dx * dx + dy * dy);

                        // Normalisation de la direction
                        if (distance > 0) {
                            dx /= distance;
                            dy /= distance;
                        }

                        // Déplacement de l'ennemi suiveur
                        ennemiSuiveur.setX(ennemiSuiveur.getX() + dx * VITESSE_SUIVEUR);
                        ennemiSuiveur.setY(ennemiSuiveur.getY() + dy * VITESSE_SUIVEUR * 0.5f); // Réduire la vitesse verticale si nécessaire

                        // Si l'ennemi suiveur sort de l'écran, le repositionner
                        if (ennemiSuiveur.getX() < -ennemiSuiveur.getWidth() || ennemiSuiveur.getX() > screenWidth || ennemiSuiveur.getY() > screenHeight) {
                            ennemiSuiveur.setVisibility(View.INVISIBLE);
                            repositionnerEnnemiSuiveur(ennemiSuiveur);
                        }
                    }
                }
                handler.postDelayed(this, 30);
            }
        }, 30);
    }

    private void repositionnerEnnemiSuiveur(ImageView ennemiSuiveur) {
        // L'ennemi suiveur peut apparaître sur les bords gauche, droit ou supérieur de l'écran
        if (random.nextBoolean()) {
            ennemiSuiveur.setX(random.nextFloat() < 0.5 ? -ennemiSuiveur.getWidth() : screenWidth); // Gauche ou droite
            ennemiSuiveur.setY(random.nextInt(screenHeight / 2));
        } else {
            ennemiSuiveur.setX(random.nextInt(screenWidth - ennemiSuiveur.getWidth()));
            ennemiSuiveur.setY(-ennemiSuiveur.getHeight()); // Haut
        }
        ennemiSuiveur.setVisibility(View.VISIBLE);
    }

    private void tirerProjectile() {
        ImageView projectile = new ImageView(this);
        projectile.setImageResource(R.drawable.missle);
        int projectileWidth = 50;
        int projectileHeight = 70;
        projectile.setLayoutParams(new android.view.ViewGroup.LayoutParams(projectileWidth, projectileHeight));
        float projectileX = vaisseauJoueur.getX() + (vaisseauJoueur.getWidth() / 2f) - (projectileWidth / 2f);
        float projectileY = vaisseauJoueur.getY() - projectileHeight;
        projectile.setX(projectileX);
        projectile.setY(projectileY);
        gameLayout.addView(projectile);
        projectilesJoueur.add(projectile);

        ObjectAnimator projectileAnimator = ObjectAnimator.ofFloat(projectile, "translationY", projectileY, -projectileHeight);
        projectileAnimator.setDuration(1000);
        projectileAnimator.setInterpolator(new LinearInterpolator());
        projectileAnimator.start();
        projectileAnimator.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationEnd(Animator animation) {
                gameLayout.removeView(projectile);
                projectilesJoueur.remove(projectile);
            }
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        });

        handler.postDelayed(this::tirerProjectile, 300);
    }



    private void tirerProjectileEnnemiSuiveur() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (ImageView ennemiSuiveur : ennemisSuiveurs) {
                    if (ennemiSuiveur.getVisibility() == View.VISIBLE) {
                        // Probabilité de tirer (par exemple, 25% de chance à chaque frame)
                        if (random.nextInt(100) < 25) {
                            ImageView projectile = new ImageView(JeuxCap2.this);
                            projectile.setImageResource(R.drawable.missle_ennemi); // Assurez-vous d'avoir une image pour les projectiles ennemis
                            int projectileWidth = 40;
                            int projectileHeight = 60;
                            projectile.setLayoutParams(new android.view.ViewGroup.LayoutParams(projectileWidth, projectileHeight));
                            float projectileX = ennemiSuiveur.getX() + (ennemiSuiveur.getWidth() / 2f) - (projectileWidth / 2f);
                            float projectileY = ennemiSuiveur.getY() + ennemiSuiveur.getHeight() / 2f; // Ajuster le point de départ du projectile
                            projectile.setX(projectileX);
                            projectile.setY(projectileY);
                            gameLayout.addView(projectile);
                            projectilesEnnemisSuiveurs.add(projectile);

                            ObjectAnimator projectileAnimator = ObjectAnimator.ofFloat(projectile, "translationY", projectileY, screenHeight + projectileHeight);
                            projectileAnimator.setDuration(1500); // Ajuster la vitesse du projectile ennemi si nécessaire
                            projectileAnimator.setInterpolator(new LinearInterpolator());
                            projectileAnimator.start();

                            projectileAnimator.addListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {}

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    gameLayout.removeView(projectile);
                                    projectilesEnnemisSuiveurs.remove(projectile);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {}

                                @Override
                                public void onAnimationRepeat(Animator animation) {}
                            });
                        }
                    }
                }
                handler.postDelayed(this, 500); // Ajuster la fréquence de tir des ennemis
            }
        }, 500);
    }


    private void detecterCollisions() {
        Rect rectVaisseau = new Rect((int) vaisseauJoueur.getX(), (int) vaisseauJoueur.getY(),
                (int) (vaisseauJoueur.getX() + vaisseauJoueur.getWidth()),
                (int) (vaisseauJoueur.getY() + vaisseauJoueur.getHeight()));

        for (ImageView ennemi : ennemis) {
            if (ennemi.getVisibility() == View.VISIBLE) {
                Rect rectEnnemi = new Rect((int) ennemi.getX(), (int) ennemi.getY(),
                        (int) (ennemi.getX() + ennemi.getWidth()),
                        (int) (ennemi.getY() + ennemi.getHeight()));
                if (Rect.intersects(rectVaisseau, rectEnnemi)) {
                    Log.d("Collision", "Vaisseau touché ! Vie avant : " + vieRestante);
                    gererCollisionVaisseauEnnemi();
                    ennemi.setVisibility(View.INVISIBLE);
                    repositionnerEnnemi(ennemi);
                }
            }
        }

        for (ImageView ennemiSuiveur : ennemisSuiveurs) {
            if (ennemiSuiveur.getVisibility() == View.VISIBLE) {
                Rect rectEnnemiSuiveur = new Rect((int) ennemiSuiveur.getX(), (int) ennemiSuiveur.getY(),
                        (int) (ennemiSuiveur.getX() + ennemiSuiveur.getWidth()),
                        (int) (ennemiSuiveur.getY() + ennemiSuiveur.getHeight()));
                if (Rect.intersects(rectVaisseau, rectEnnemiSuiveur)) {
                    Log.d("Collision", "Vaisseau touché par un suiveur ! Vie avant : " + vieRestante);
                    gererCollisionVaisseauEnnemi();
                    ennemiSuiveur.setVisibility(View.INVISIBLE);
                    repositionnerEnnemiSuiveur(ennemiSuiveur);
                }
            }
        }

        List<ImageView> projectilesToRemove = new ArrayList<>();
        for (ImageView projectile : projectilesJoueur) {
            Rect rectProjectile = new Rect((int) projectile.getX(), (int) projectile.getY(),
                    (int) (projectile.getX() + projectile.getWidth()),
                    (int) (projectile.getY() + projectile.getHeight()));

            for (ImageView ennemi : ennemis) {
                if (ennemi.getVisibility() == View.VISIBLE) {
                    Rect rectEnnemi = new Rect((int) ennemi.getX(), (int) ennemi.getY(),
                            (int) (ennemi.getX() + ennemi.getWidth()),
                            (int) (ennemi.getY() + ennemi.getHeight()));
                    if (Rect.intersects(rectProjectile, rectEnnemi)) {
                        score++;
                        updateScore();
                        ennemi.setVisibility(View.INVISIBLE);
                        projectilesToRemove.add(projectile);
                        repositionnerEnnemi(ennemi);
                        break;
                    }
                }
            }
            for (ImageView ennemiSuiveur : ennemisSuiveurs) {
                if (ennemiSuiveur.getVisibility() == View.VISIBLE) {
                    Rect rectEnnemiSuiveur = new Rect((int) ennemiSuiveur.getX(), (int) ennemiSuiveur.getY(),
                            (int) (ennemiSuiveur.getX() + ennemiSuiveur.getWidth()),
                            (int) (ennemiSuiveur.getY() + ennemiSuiveur.getHeight()));
                    if (Rect.intersects(rectProjectile, rectEnnemiSuiveur)) {
                        score += 2;
                        updateScore();
                        ennemiSuiveur.setVisibility(View.INVISIBLE);
                        projectilesToRemove.add(projectile);
                        repositionnerEnnemiSuiveur(ennemiSuiveur);
                        break;
                    }
                }
            }
        }
        for (ImageView projectileToRemove : projectilesToRemove) {
            gameLayout.removeView(projectileToRemove);
            projectilesJoueur.remove(projectileToRemove);
        }

        // Vérification de collision entre ennemis suiveurs
        for (int i = 0; i < ennemisSuiveurs.size(); i++) {
            for (int j = i + 1; j < ennemisSuiveurs.size(); j++) { // Evite de comparer un ennemi avec lui-même
                ImageView ennemiSuiveur1 = ennemisSuiveurs.get(i);
                ImageView ennemiSuiveur2 = ennemisSuiveurs.get(j);

                if (ennemiSuiveur1.getVisibility() == View.VISIBLE && ennemiSuiveur2.getVisibility() == View.VISIBLE) {
                    Rect rectEnnemiSuiveur1 = new Rect((int) ennemiSuiveur1.getX(), (int) ennemiSuiveur1.getY(),
                            (int) (ennemiSuiveur1.getX() + ennemiSuiveur1.getWidth()),
                            (int) (ennemiSuiveur1.getY() + ennemiSuiveur1.getHeight()));
                    Rect rectEnnemiSuiveur2 = new Rect((int) ennemiSuiveur2.getX(), (int) ennemiSuiveur2.getY(),
                            (int) (ennemiSuiveur2.getX() + ennemiSuiveur2.getWidth()),
                            (int) (ennemiSuiveur2.getY() + ennemiSuiveur2.getHeight()));

                    if (Rect.intersects(rectEnnemiSuiveur1, rectEnnemiSuiveur2)) {
                        Log.d("Collision", "Ennemis suiveurs se sont touchés !");
                        // Ici, on ne fait rien de spécial pour le gameplay, mais on log l'événement.
                        // Si on voulait les faire réapparaître :
                        repositionnerEnnemiSuiveur(ennemiSuiveur1);
                        repositionnerEnnemiSuiveur(ennemiSuiveur2);
                    }
                }
            }
        }

        // Collision entre les projectiles des joueurs et les ennemis
        List<ImageView> projectilesEnnemisToRemove = new ArrayList<>();
        for (ImageView projectileEnnemi : projectilesEnnemisSuiveurs) {
            Rect rectProjectileEnnemi = new Rect((int) projectileEnnemi.getX(), (int) projectileEnnemi.getY(),
                    (int) (projectileEnnemi.getX() + projectileEnnemi.getWidth()),
                    (int) (projectileEnnemi.getY() + projectileEnnemi.getHeight()));

            if (vaisseauJoueur.getVisibility() == View.VISIBLE) {
                if (Rect.intersects(rectProjectileEnnemi, rectVaisseau)) {
                    Log.d("Collision", "Vaisseau touché par un projectile ennemi ! Vie avant : " + vieRestante);
                    gererCollisionVaisseauEnnemi();
                    projectilesEnnemisToRemove.add(projectileEnnemi);
                }
            }
        }

        for (ImageView projectileEnnemiToRemove : projectilesEnnemisToRemove) {
            gameLayout.removeView(projectileEnnemiToRemove);
            projectilesEnnemisSuiveurs.remove(projectileEnnemiToRemove);
        }

        handler.postDelayed(this::detecterCollisions, 50);
    }

    private void repositionnerEnnemi(ImageView ennemi) {
        ennemi.setY(-ennemi.getHeight());
        ennemi.setX(random.nextInt(screenWidth - ennemi.getWidth()));
        ennemi.setVisibility(View.VISIBLE);

        int index = ennemis.indexOf(ennemi);
        Animator old = animationsEnnemi.get(index);
        if (old != null) old.cancel();

        ObjectAnimator newAnimator = ObjectAnimator.ofFloat(ennemi, "translationY", ennemi.getY(), screenHeight + 100f);
        newAnimator.setDuration(durationEnnemi);
        newAnimator.setRepeatCount(ValueAnimator.INFINITE);
        newAnimator.setInterpolator(new LinearInterpolator());
        newAnimator.setStartDelay(delayListEnnemi[index % delayListEnnemi.length]);
        newAnimator.start();

        animationsEnnemi.set(index, newAnimator);
    }

    private void gererCollisionVaisseauEnnemi() {
        vieRestante -= 20;
        if (vieRestante < 0) vieRestante = 0;
        barreVie.setProgress(vieRestante);
        vibratePhone(this, 200);

        if (vieRestante <= 0) {
            Log.d("GameOver", "Vies épuisées");
            gameOverSound.start();
            arreterJeu();
        }
    }

    private void arreterJeu() {
        for (Animator animator : animationsEnnemi) {
            if (animator != null) animator.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        afficherGameOverDialog();
    }

    private void afficherGameOverDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View gameOverView = inflater.inflate(R.layout.game_over_dialog, null);
        ImageView gameOverImage = gameOverView.findViewById(R.id.gameOverImage);
        TextView finalScoreText = gameOverView.findViewById(R.id.finalScoreText);
        Button replayButton = gameOverView.findViewById(R.id.replayButton);
        Button quitButton = gameOverView.findViewById(R.id.quitButton);

        gameOverImage.setImageResource(R.drawable.game_over);
        finalScoreText.setText("Score final : " + score);

        builder.setView(gameOverView)
                .setCancelable(false);

        AlertDialog gameOverDialog = builder.create();
        gameOverDialog.show();

        replayButton.setOnClickListener(v -> {
            rejouer();
            gameOverDialog.dismiss();
        });

        quitButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void rejouer() {
        score = 0;
        vieRestante = 100;
        barreVie.setProgress(vieRestante);
        scoreTextView.setText("SCORE\n" + score);

        for (int i = 0; i < ennemis.size(); i++) {
            ImageView ennemi = ennemis.get(i);
            ennemi.setY(-ennemi.getHeight());
            ennemi.setX(random.nextInt(screenWidth - ennemi.getWidth()));
            ennemi.setVisibility(View.VISIBLE);
            Animator old = animationsEnnemi.get(i);
            if (old != null) old.cancel();
            ObjectAnimator newAnimator = ObjectAnimator.ofFloat(ennemi, "translationY", ennemi.getY(), screenHeight + 100f);
            newAnimator.setDuration(durationEnnemi);
            newAnimator.setRepeatCount(ValueAnimator.INFINITE);
            newAnimator.setInterpolator(new LinearInterpolator());
            newAnimator.setStartDelay(delayListEnnemi[i % delayListEnnemi.length]);
            newAnimator.start();
            animationsEnnemi.set(i, newAnimator);
        }

        for (ImageView ennemiSuiveur : ennemisSuiveurs) {
            repositionnerEnnemiSuiveur(ennemiSuiveur);
        }

        for (ImageView projectile : projectilesJoueur) {
            gameLayout.removeView(projectile);
        }
        projectilesJoueur.clear();
        projectilesEnnemisSuiveurs.clear(); // Clear les projectiles ennemis

        handler.postDelayed(this::detecterCollisions, 50);
        handler.postDelayed(this::tirerProjectile, 1000);
        handler.postDelayed(this::tirerProjectileEnnemiSuiveur, 2000); // Redémarrer les tirs des suiveurs
    }

    private void updateScore() {
        scoreTextView.setText("SCORE\n" + score);
    }

    private void bougerEnnemis() {
        float finalY = screenHeight + 100f;

        shuffleDelayEnnemiList();

        for (int i = 0; i < ennemis.size(); i++) {
            ImageView ennemi = ennemis.get(i);
            ObjectAnimator animator = ObjectAnimator.ofFloat(ennemi, "translationY", ennemi.getY(), finalY);
            animator.setDuration(durationEnnemi);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.setStartDelay(delayListEnnemi[i % delayListEnnemi.length]);
            animator.start();
            animationsEnnemi.add(animator);
        }
    }

    private void shuffleDelayEnnemiList() {
        List<Long> delayAsList = new ArrayList<>(Arrays.asList(delayListEnnemi));
        Collections.shuffle(delayAsList);
        delayListEnnemi = delayAsList.toArray(new Long[0]);
    }

    private void shuffleDelayEnnemi() {
        handler.postDelayed(() -> {
            shuffleDelayEnnemiList();
            handler.postDelayed(this::shuffleDelayEnnemi, 500);
        }, 500);
    }

    private void vibratePhone(Context context, long milliseconds) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(milliseconds);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        for (Animator animator : animationsEnnemi) {
            animator.resume();
        }
        // bougerEnnemisSuiveurs(); // Redémarrer le mouvement des suiveurs au cas où
        detecterCollisions();
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }

        sensorManager.unregisterListener(this);
        for (Animator animator : animationsEnnemi) {
            animator.pause();
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.release();
            backgroundMusic = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == gyroscope) {
            float yRotationSpeed = event.values[1];

            if (yRotationSpeed > thresholdGyro) {
                deplacerVaisseauDroite();
            } else if (yRotationSpeed < -thresholdGyro) {
                deplacerVaisseauGauche();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void deplacerVaisseauDroite() {
        float nextX = vaisseauJoueur.getX() + defilementVaisseau;
        if (nextX < screenWidth - vaisseauJoueur.getWidth()) {
            vaisseauJoueur.setX(nextX);
        }
    }

    private void deplacerVaisseauGauche() {
        float nextX = vaisseauJoueur.getX() - defilementVaisseau;
        if (nextX > 0) {
            vaisseauJoueur.setX(nextX);
        }
    }
}

