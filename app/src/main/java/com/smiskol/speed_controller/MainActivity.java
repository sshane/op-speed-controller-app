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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
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
import android.widget.Toast;
import android.widget.Toolbar;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Properties;

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
    TextInputLayout ipEditTextLayout;
    LinearLayout buttonLayout;
    Button addButton;
    Button decreaseButton;

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
        ipEditTextLayout = findViewById(R.id.ipEditTextLayout);
        buttonLayout = findViewById(R.id.buttonLayout);
        addButton = findViewById(R.id.addButton);
        decreaseButton = findViewById(R.id.decreaseButton);

        setUpFont();
        doAnimations();
        cardPermissionLogic();
        setUpMainCard();
        startListeners();

        createNotificationChannel();

        listenSwitch.setChecked(isMyServiceRunning(ListenerService.class)); //set switch to appropriate value upon start up if activity gets closed

        if (preferences.getBoolean("ghostRider", false)) {
            doGhostRider(); //play a little tune ;)
        }
    }

    public void startListeners() {
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RunSpeedChange().execute(8);
            }
        });

        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RunSpeedChange().execute(-8);
            }
        });

        final Intent listenerService = new Intent(MainActivity.this, ListenerService.class);

        listenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!ipEditText.getText().toString().equals("") && ipEditText.getText().toString().length() >= 7) {
                        if (preferences.getBoolean("filePutSuccessfully", false)) {
                            if (preferences.getBoolean("clickedInfo", false)) {
                                if (preferences.getBoolean("hasInstalled", false)) {
                                    ipEditText.setEnabled(false);
                                    listeningTextView.setText("Testing connection");
                                    makeSnackbar("Testing connection...");
                                    new CheckEON().execute();
                                } else {
                                    installDialog("install");
                                    listenSwitch.setChecked(false);
                                }
                            } else {
                                installDialog("modified");
                                listenSwitch.setChecked(false);
                            }
                        } else {
                            installDialog("put");
                            listenSwitch.setChecked(false);
                        }
                    } else {
                        listenSwitch.setChecked(false);
                        makeSnackbar("Please enter an IP!");
                        Animation mShakeAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake);
                        ipEditTextLayout.startAnimation(mShakeAnimation);
                    }


                } else {
                    stopService(listenerService);
                    listeningTextView.setText("Not Listening");
                    buttonLayout.setVisibility(View.GONE);
                    makeSnackbar("Stopped service!");
                    ipEditText.setEnabled(true);
                }
            }
        });
    }

    public void dialogAfterPut(Boolean success) {
        if (success) {
            preferences.edit().putBoolean("filePutSuccessfully", true).apply();
            installDialog("install");
        } else {
            installDialog("error");
        }
    }

    public void installDialog(String status) {
        if (status.equals("put")) {
            if (alertTitle.getParent() != null) {
                ((ViewGroup) alertTitle.getParent()).removeView(alertTitle);
            }
            alertTitle.setText("Easy, Tiger!");
            alertTitle.setVisibility(View.VISIBLE);
            alertTitle.setTypeface(semibold);
            AlertDialog successDialog = new AlertDialog.Builder(MainActivity.this).setCustomTitle(alertTitle)
                    .setMessage("We first need a few files installed/modified on your EON to receive requests made by this app. Press the button to upload the Python listener file to your EON. Further instructions will follow.")
                    .setPositiveButton("I'm ready!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new PutListenerFile().execute();
                            listenSwitch.setEnabled(false);
                        }
                    })
                    .show();

            TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
            Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
            tmpMessage.setTypeface(regular);
            tmpButton.setTypeface(semibold);
        } else if (status.equals("install")) {
            if (alertTitle.getParent() != null) {
                ((ViewGroup) alertTitle.getParent()).removeView(alertTitle);
            }
            alertTitle.setText("Hurray!");
            listenSwitch.setEnabled(true);
            alertTitle.setVisibility(View.VISIBLE);
            alertTitle.setTypeface(semibold);
            AlertDialog successDialog = new AlertDialog.Builder(MainActivity.this).setCustomTitle(alertTitle)
                    .setMessage("It uploaded successfully. Now you will need to SSH into your EON to manually modify your car's respective carstate.py file. Instructions can be found by clicking the info button.")
                    .setPositiveButton("Info", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ShaneSmiskol/op-speed-controller-app/blob/master/INSTRUCTIONS.md"));
                            startActivity(browserIntent);
                            installDialog("modified");
                            preferences.edit().putBoolean("clickedInfo", true).apply();
                        }
                    })
                    .show();

            TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
            Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
            tmpMessage.setTypeface(regular);
            tmpButton.setTypeface(semibold);
        } else if (status.equals("error")) {
            if (alertTitle.getParent() != null) {
                ((ViewGroup) alertTitle.getParent()).removeView(alertTitle);
            }
            listenSwitch.setEnabled(true);
            alertTitle.setText("This is awkward...");
            alertTitle.setVisibility(View.VISIBLE);
            alertTitle.setTypeface(semibold);
            AlertDialog successDialog = new AlertDialog.Builder(MainActivity.this).setCustomTitle(alertTitle)
                    .setMessage("It seems we were unable to connect to your EON. Ensure the IP is correct, and that you're on the same network!")
                    .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new PutListenerFile().execute();
                            listenSwitch.setEnabled(false);
                        }
                    })
                    .show();

            TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
            Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
            tmpMessage.setTypeface(regular);
            tmpButton.setTypeface(semibold);
        } else if (status.equals("modified")) {
            if (alertTitle.getParent() != null) {
                ((ViewGroup) alertTitle.getParent()).removeView(alertTitle);
            }
            alertTitle.setText("Are you ready?");
            alertTitle.setVisibility(View.VISIBLE);
            alertTitle.setTypeface(semibold);
            AlertDialog successDialog = new AlertDialog.Builder(MainActivity.this).setCustomTitle(alertTitle)
                    .setMessage("Press the button to verify you've modified your car's carstate.py file. If you haven't yet, click the info button to get instructions.")
                    .setPositiveButton("It's done!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            preferences.edit().putBoolean("hasInstalled", true).apply();
                            preferences.edit().putBoolean("clickedInfo", true).apply();
                            makeSnackbar("You're ready to go!");
                        }
                    })
                    .setNegativeButton("Info", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ShaneSmiskol/op-speed-controller-app/blob/master/INSTRUCTIONS.md"));
                            startActivity(browserIntent);
                            preferences.edit().putBoolean("clickedInfo", true).apply();
                            installDialog("modified");
                        }
                    })
                    .show();

            TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
            Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
            tmpMessage.setTypeface(regular);
            tmpButton.setTypeface(semibold);
        }
    }

    public void warningDialog() {
        if (alertTitle.getParent() != null) {
            ((ViewGroup) alertTitle.getParent()).removeView(alertTitle);
        }
        alertTitle.setText("Warning");
        alertTitle.setVisibility(View.VISIBLE);
        alertTitle.setTypeface(semibold);
        AlertDialog successDialog = new AlertDialog.Builder(MainActivity.this).setCustomTitle(alertTitle)
                .setMessage("Please note that it's best to initially set your cruise control to a high speed, then alter it with this app to your desired, lower, speed. Setting it low, then increasing it with this app will cause unwanted cruise control behavior. Proceed at your own risk.\nYou can always tap the cruise control stock up or down to reset the system if anything goes wrong!")
                .setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferences.edit().putBoolean("warning", true).apply();
                    }
                })
                .setCancelable(false)
                .show();

        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public class CheckEON extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... v) {
            return new SSHClass().testConnection(MainActivity.this, ipEditText.getText().toString());
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                final Intent listenerService = new Intent(MainActivity.this, ListenerService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(listenerService);
                } else {
                    startService(listenerService);
                }
                preferences.edit().putString("eonIP", ipEditText.getText().toString()).apply();
                makeSnackbar("Started service!");
                listeningTextView.setText("Listening...");
                buttonLayout.setVisibility(View.VISIBLE);
                if (!preferences.getBoolean("warning", false)) {
                    warningDialog();
                }
            } else {
                listenSwitch.setChecked(false);
                makeSnackbar("Couldn't connect to EON! Perhaps wrong IP?");
            }
        }
    }

    public class PutListenerFile extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... v) {
            String eonIP = ipEditText.getText().toString();
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

            StrictMode.setThreadPolicy(policy);
            try {
                JSch jsch = new JSch();
                File file = new File(getFilesDir(), "eon_id.ppk");
                jsch.addIdentity(file.getAbsolutePath());
                Session session = jsch.getSession("root", eonIP, 8022);

                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                prop.put("PreferredAuthentications", "publickey");
                session.setConfig(prop);

                session.connect(5000);

                Channel channel = session.openChannel("sftp");
                channel.connect();

                ChannelSftp sftp = (ChannelSftp) channel;

                File speedControllerFile = new File(getFilesDir(), "speed_controller.py");

                sftp.put(speedControllerFile.getAbsolutePath(), "/data/openpilot/selfdrive/speed_controller.py");

                channel.disconnect();
                session.disconnect();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                dialogAfterPut(true);
            } else {
                dialogAfterPut(false);
            }
        }
    }

    public class RunSpeedChange extends AsyncTask<Integer, Void, String[]> {

        protected String[] doInBackground(Integer... speedChange) {
            Boolean result = new SSHClass().runSpeedChange(MainActivity.this, ipEditText.getText().toString(), speedChange[0].intValue());
            return new String[]{result.toString(), String.valueOf(speedChange[0].intValue())};
        }

        protected void onPostExecute(String... result) {
            if (result[0].equals("false")) {
                listenSwitch.setChecked(false);
                makeSnackbar("Couldn't connect to EON! Perhaps wrong IP?");
            } else {
                if (result[1].equals("8")) {
                    makeSnackbar("Increased speed!");
                }else{
                    makeSnackbar("Decreased speed!");
                }
            }
        }
    }

    public void cardPermissionLogic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setInterpolator(new DecelerateInterpolator());
            fadeIn.setDuration(1200);
            mainCard.startAnimation(fadeIn);
            mainCard.setVisibility(View.VISIBLE);
            permissionCard.setVisibility(View.GONE);
        } else {
            infoDialog();
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
                writeSupportingFiles();
                if (preferences.getBoolean("firstRun", true)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if (alertTitle.getParent() != null) {
                            ((ViewGroup) alertTitle.getParent()).removeView(alertTitle);
                        }
                        alertTitle.setText("Granted!");
                        alertTitle.setVisibility(View.VISIBLE);
                        alertTitle.setTypeface(semibold);
                        AlertDialog successDialog = new AlertDialog.Builder(this).setCustomTitle(alertTitle)
                                .setMessage("Awesome! Permission granted. I've written the EON private key to a file for us to read when we make connections to the EON later.")
                                .setPositiveButton("Sensational", new DialogInterface.OnClickListener() {
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
                        preferences.edit().putBoolean("firstRun", false).apply();
                    } else {
                        if (alertTitle.getParent() != null) {
                            ((ViewGroup) alertTitle.getParent()).removeView(alertTitle);
                        }
                        alertTitle.setText("Uh oh!");
                        alertTitle.setVisibility(View.VISIBLE);
                        alertTitle.setTypeface(semibold);
                        AlertDialog deniedDialog = new AlertDialog.Builder(this).setCustomTitle(alertTitle)
                                .setMessage("You've denied the storage permission. We need this to write the EON private key to a file so we can make connections over SSH. Please accept the permission.")
                                .setPositiveButton("Fine, Retry", new DialogInterface.OnClickListener() {
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
                }
                return;
            }
        }
    }

    public void infoDialog() {
        if (alertTitle.getParent() != null) {
            ((ViewGroup) alertTitle.getParent()).removeView(alertTitle);
        }
        alertTitle.setText("Welcome!");
        alertTitle.setVisibility(View.VISIBLE);
        alertTitle.setTypeface(semibold);
        AlertDialog successDialog = new AlertDialog.Builder(this).setCustomTitle(alertTitle)
                .setMessage("op Speed Controller is an app that can control your openpilot-supported car's speed via SSH commands either in-app or with a Bluetooth remote.")
                .setPositiveButton("Whatever, dude", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).setCancelable(false)
                .show();

        TextView tmpMessage = successDialog.getWindow().findViewById(android.R.id.message);
        Button tmpButton = successDialog.getWindow().findViewById(android.R.id.button1);
        tmpMessage.setTypeface(regular);
        tmpButton.setTypeface(semibold);
    }

    public void doGhostRider() {
        final MediaPlayer mMediaPlayer;
        mMediaPlayer = MediaPlayer.create(this, R.raw.ghost_riders);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mMediaPlayer.release();
            }
        });
        mMediaPlayer.start();
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
        listeningTextView.setTypeface(regular);
        ipEditText.setTypeface(regular);
        addButton.setTypeface(semibold);
        decreaseButton.setTypeface(semibold);
    }

    public void doAnimations() {
        TranslateAnimation titleAnimation = new TranslateAnimation(0f, 0f, -300f, 0f);
        titleAnimation.setDuration(900);
        titleAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
        titleText.startAnimation(titleAnimation);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
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

    public void writeSupportingFiles() {
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
            makeSnackbar("Error writing support files.");
        }

        try {
            File file = new File(getFilesDir(), "speed_controller.py");
            if (!file.exists() || file.length() == 0) {
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fOut);

                String speedController = "import sys\n" +
                        "live_speed_file = '/data/live_speed_file'\n" +
                        "\n" +
                        "def write_file(a):\n" +
                        "    try:\n" +
                        "        with open(live_speed_file, 'r') as speed:\n" +
                        "            modified_speed=float(speed.read())+a\n" +
                        "        with open(live_speed_file, 'w') as speed:\n" +
                        "            speed.write(str(modified_speed))\n" +
                        "    except: #in case file doesn't exist or is empty\n" +
                        "        with open(live_speed_file, 'w') as speed:\n" +
                        "            speed.write(str(28.0))\n" +
                        "\n" +
                        "if __name__ == \"__main__\":\n" +
                        "    write_file(int(sys.argv[1]))";

                osw.write(speedController);

                osw.close();
                System.out.println("File written");
            } else {
                System.out.println("File already written");
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
            makeSnackbar("Error writing support files.");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.ghost_rider:
                if (preferences.getBoolean("ghostRider", false)) {
                    preferences.edit().putBoolean("ghostRider", false).apply();
                    makeSnackbar("Ghost Rider disabled!");
                } else {
                    preferences.edit().putBoolean("ghostRider", true).apply();
                    makeSnackbar("Ghost Rider enabled!");
                    doGhostRider();
                }
                return true;

            case R.id.ebay_link:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ebay.com/sch/i.html?_from=R40&_trksid=m570.l1313&_nkw=Car+Bluetooth4.0+Media+Button+Music+Steering+Wheel+Control+for+Smartphone&_sacat=0"));
                startActivity(browserIntent);
                return true;

            case R.id.instructions:
                Intent browserIntent2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ShaneSmiskol/op-speed-controller-app/blob/master/INSTRUCTIONS.md"));
                startActivity(browserIntent2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void makeSnackbar(String s) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), s, Snackbar.LENGTH_SHORT);

        snackbar.show();
    }

}
