package com.smiskol.speed_controller;

import android.Manifest;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;


import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    private boolean mIsInForegroundMode;
    SharedPreferences preferences;
    CardView permissionCard;
    CardView mainCard;
    Switch listenSwitch;
    TextView listeningTextView;
    TextView titleText;
    EditText ipEditText;
    TextView cardViewText;
    Button permissionButton;
    Typeface semibold;
    Typeface regular;
    TextView alertTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        permissionCard = findViewById(R.id.cardViewPermission);
        mainCard = findViewById(R.id.cardViewMain);
        listenSwitch = findViewById(R.id.listenSwitch);
        listeningTextView = findViewById(R.id.listeningText);
        titleText = findViewById(R.id.title_text);
        ipEditText = findViewById(R.id.ipEditText);
        cardViewText = findViewById(R.id.cardViewText);
        permissionButton = findViewById(R.id.permissionButton);
        semibold = ResourcesCompat.getFont(this, R.font.proxima_semibold);
        regular = ResourcesCompat.getFont(this, R.font.proxima_regular);
        alertTitle = findViewById(R.id.alertTextTemp);

        setUpFont();
        doAnimations();
        cardPermissionLogic();
        setUpMainCard();
        startListeners();
        /*if (preferences.getBoolean("firstRun", false)) {
            runOnFirstRun();
        }*/
        createNotificationChannel();


        Boolean isServiceRunning = isMyServiceRunning(ListenerService.class);

        listenSwitch.setChecked(isServiceRunning); //set switch to appropriate value upon start up if activity gets closed
        if (isServiceRunning) {
            listeningTextView.setText("Listening");
        }

        /*final MediaPlayer mMediaPlayer; //play a little tune ;)
        mMediaPlayer = MediaPlayer.create(this, R.raw.ghost_riders);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mMediaPlayer.release();
            }
        });
        mMediaPlayer.start();*/
    }

    public void startListeners() {
        final Intent listenerService = new Intent(this, ListenerService.class);

        listenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!ipEditText.getText().toString().equals("") && ipEditText.getText().toString().length() >= 7) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(listenerService);
                        } else {
                            startService(listenerService);
                        }
                        preferences.edit().putString("eonIP", ipEditText.getText().toString()).apply();
                        listeningTextView.setText("Listening");
                        makeSnackbar("Started service!");
                        ipEditText.setEnabled(false);
                    } else {
                        listenSwitch.setChecked(false);
                        makeSnackbar("Please enter an IP!");
                        Animation mShakeAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake);
                        TextInputLayout mEditTextLayout = findViewById(R.id.ipEditTextLayout);
                        mEditTextLayout.startAnimation(mShakeAnimation);
                    }
                } else {
                    stopService(listenerService);
                    listeningTextView.setText("Not Listening");
                    makeSnackbar("Stopped service!");
                    ipEditText.setEnabled(true);
                }
            }
        });
    }

    public void cardPermissionLogic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
            fadeIn.setDuration(1200);
            mainCard.startAnimation(fadeIn);
            mainCard.setVisibility(View.VISIBLE);
            permissionCard.setVisibility(View.GONE);

        }
    }

    public void requestPermission(View view) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                69);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 69: {
                writePrivateKeyFile();
                preferences.edit().putBoolean("firstRun", false).apply();
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (alertTitle.getParent() != null) {
                        ((ViewGroup) alertTitle.getParent()).removeView(alertTitle); // <- fix
                    }
                    alertTitle.setText("Granted!");
                    alertTitle.setVisibility(View.VISIBLE);
                    alertTitle.setTypeface(semibold);
                    AlertDialog successDialog = new AlertDialog.Builder(this).setCustomTitle(alertTitle)
                            .setMessage("Awesome! Permission granted. I've written the EON private key to a file for us to read when we make connections to the EON later.")
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    startCardViewAnimation();
                                }
                            }).show();

                    TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
                    Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
                    tmpMessage.setTypeface(regular);
                    tmpButton.setTypeface(semibold);

                } else {
                    if (alertTitle.getParent() != null) {
                        ((ViewGroup) alertTitle.getParent()).removeView(alertTitle); // <- fix
                    }
                    alertTitle.setText("Uh oh!");
                    alertTitle.setVisibility(View.VISIBLE);
                    alertTitle.setTypeface(semibold);
                    AlertDialog deniedDialog = new AlertDialog.Builder(this).setCustomTitle(alertTitle)
                            .setMessage("You've denied the storage permission. We need this to write the EON private key to a file so we can make connections over SSH. Please accept the permission.")
                            .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermission(findViewById(R.id.permissionButton));
                                }
                            })
                            .show();

                    TextView tmpMessage = deniedDialog.getWindow().findViewById(android.R.id.message);
                    Button tmpButton = deniedDialog.getWindow().findViewById(android.R.id.button1);
                    tmpMessage.setTypeface(regular);
                    tmpButton.setTypeface(semibold);
                }
                return;
            }
        }
    }


    public void setUpMainCard() {
        ipEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ipEditText.setFocusable(true);
                ipEditText.setFocusableInTouchMode(true);

                return false;
            }
        });
        ipEditText.setText(preferences.getString("eonIP", ""));
    }

    public void setUpFont() {
        permissionButton.setTypeface(semibold);
        cardViewText.setTypeface(regular);
        titleText.setTypeface(semibold);
    }

    public void doAnimations() {
        TranslateAnimation titleAnimation = new TranslateAnimation(0f, 0f, -300f, 0f);
        titleAnimation.setDuration(900);
        titleAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
        titleText.startAnimation(titleAnimation);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(1200);

        permissionCard.startAnimation(fadeIn);
    }

    public void startCardViewAnimation() {
        TranslateAnimation animateOut = new TranslateAnimation(0f, -1000f, 0f, 0f);
        TranslateAnimation animateIn = new TranslateAnimation(1000f, 0f, 0f, 0f);
        animateOut.setDuration(900);
        animateOut.setFillAfter(true);
        animateIn.setDuration(900);
        animateOut.setInterpolator(new DecelerateInterpolator(1.5f));
        animateIn.setInterpolator(new DecelerateInterpolator(1.5f));
        permissionCard.startAnimation(animateOut);
        mainCard.setVisibility(View.VISIBLE);
        mainCard.startAnimation(animateIn);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                permissionCard.setVisibility(View.GONE);
            }
        }, 900);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Listener Notification";
            String description = "This is a notification to make sure the Bluetooth listener never gets closed by the system.";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsInForegroundMode = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsInForegroundMode = true;
    }

    public void writePrivateKeyFile() {
        try {
            File file = new File(getFilesDir(), "eon_id.ppk");
            if (!file.exists() || file.length() == 0) {
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fOut);

                String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
                        "MIIEogIBAAKCAQEAvol16t9E6vieTSmrdylhws3JsGeeZxoeloIAKhAmuQmrAZTP\n" +
                        "VXkTqVbt23gPuYdDIm0YGw+AzLVVwbeoBL2fJ3dOBO3iwPS02chQ2e0pEjlY+KFz\n" +
                        "kLE9BpyZiqwEluSrJU1qlc036NlwrWftNOIpC8ZshXgTvDTnBK1taWvIBXUA06B/\n" +
                        "RawO5IMrInP11REkzqHu15c0aHv3mWnBEPo7Z5hXdtQOGhAA5JNNAIY69LimiYi1\n" +
                        "AD2rcbNonCF1qYGLX6qrWihdt8EretTk7unAMF2zlq95viFEkVDtCEcxCEEt89Y3\n" +
                        "3dbL4M0oEksGdS4Y+AKCsSBACHPKiazuLspgiQIDAQABAoIBAQCEhXr8RxnaC92e\n" +
                        "cZMOqDuUkCjthsRHlYUczYJrvxwPqsfDq8qg+jtQlmONN+5H7eolsZcIizncJ2tj\n" +
                        "9ubnlTNy8anUB9ikuA5pQsfpKuhcAoL9Ot30DzIQvS6Vopr2kEjxAu1VD40JaOLT\n" +
                        "2OrE02AVDodANYoUZv8e47irkAlosQqvAvw1ZwdV+Jho/lt5yXOU8FSbYCW24ga6\n" +
                        "uj1q4bwf96ppMR0S+3VNkgW9ojURdSy2N9HScf3A+91AyjR65a7I5N1CXNvTKePz\n" +
                        "JWnSr1JEajcJWMUrgLSVdJ2d/ohZC7N2nUkx3SaQpUHq+OUedaxQ5VbA89mQaW/4\n" +
                        "UTUaBg7hAoGBAOgNRIsS6u0GDod3G14cod1uJKVbwPxT3yh9TjMtzjTg/2PTmvjP\n" +
                        "8LYVtcEqES9p/rriFuTgIUyLyBIr4+mwGbE097cK7zq72Lva8fWpZ+KfAYcr3Y3l\n" +
                        "uJEu0/BT+aJei6DrdrEz909SzriTzrkLzo5SjyiDId3N0RTVk5xszD2tAoGBANIz\n" +
                        "Yjy8T9wNp619JHigyPlR7rzmPPIHYdFuQbizC6LziA5PWkBSGwWzLltTk4xyr/TS\n" +
                        "vi68PmGwhajhn9XVP1DeYEshPJV/0BbFBlKlGcee+JyWZziHMtzjTp0C3LxwEE6C\n" +
                        "xQBlHez1oD9wrR5LfYRL9pKFMC+L6IpEz9bvRpHNAoGBANmqaFsT2a2Pet1yygcT\n" +
                        "UHnGMTWyxWlquu7d6xZypvRPAQCAouM1GhOSdbTFYu1YvYpLPTJfUpzcmUUCSn0P\n" +
                        "pGnmx125MgGj5n7/tuq6hym6ANLsQJwzmVcF1+OcwZKeoNbHR8ScfCS6BhJ5AvXs\n" +
                        "r0otAv/7US8fOjoSxK18GHDZAn9YrVTESq1mKFyU1DaOrUYb6HTPPFJ5yKN7twgC\n" +
                        "44YFOLgtUUzB1eGQhgcIgDm/BqM0pbOWA9RNYisBFC5aB5yugSIej+b/Kuyern/8\n" +
                        "XaqCjI5VgR4Kuv66MSr5EjwNQzmd5Y02nXIChZ0VJnPiU/af2WwsZAPwCxYPPvhv\n" +
                        "tIIRAoGAPLxtzP7rcHi76uESO5e1O2/otgWo3ytjpszYv8boH3i42OpNrX0Bkbr+\n" +
                        "qaU43obY4trr4A1pIIyVID32aYq9yEbFTFIhYJaFhhxEzstEL3OQMLakyRS0w9Vs\n" +
                        "2trgYpUlSBLIOmPNxonJIfnozphLGOnKNe0RWgGR8BnwhRYzu+k=\n" +
                        "-----END RSA PRIVATE KEY-----\n";

                osw.write(privateKey);

                osw.close();
                System.out.println("File written");
            } else {
                System.out.println("File already written");
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void makeSnackbar(String s) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), s, Snackbar.LENGTH_SHORT);

        snackbar.show();
    }

}
