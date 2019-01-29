package com.smiskol.speed_controller;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {
    List<String> myList;
    Thread listenerThread;
    Boolean alreadyRunning = false;
    //MediaButtonIntentReceiver r;
    private boolean mIsInForegroundMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
        //buttonListener();

        Switch switch1 = findViewById(R.id.switch1);
        final TextView textView1 = findViewById(R.id.textView1);

        final Intent listenerService = new Intent(this, ListenerService.class);

        Boolean isServiceRunning=isMyServiceRunning(ListenerService.class);

        switch1.setChecked(isServiceRunning); //set switch to appropriate value upon start up if activity gets closed
        if (isServiceRunning){
            textView1.setText("Listening");
        }

        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(listenerService);
                    } else {
                        startService(listenerService);
                    }
                    textView1.setText("Listening");
                    makeSnackbar("Started service!");
                } else {
                    stopService(listenerService);
                    textView1.setText("Not Listening");
                    makeSnackbar("Stopped service!");
                }
            }
        });


        //startService(new Intent(this, ListenerService.class));

        //startListener(findViewById(R.id.button));

        final MediaPlayer mMediaPlayer;
        mMediaPlayer = MediaPlayer.create(this, R.raw.silence);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mMediaPlayer.release();
            }
        });
        mMediaPlayer.start();
        /*
        MediaSession.Callback callback = new MediaSession.Callback() {
            @Override
            public void onPlay() {

            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                if (!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                } else {
                    mMediaPlayer.start();
                }
                return true;
            }
        };
        MediaSession mediaSession = new MediaSession(this,
                "f"); // Debugging tag, any string
        mediaSession.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(callback);


        PlaybackState state = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_FAST_FORWARD | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_STOP)
                .setState(PlaybackState.STATE_PLAYING, 0, 1, SystemClock.elapsedRealtime())
                .build();
        mediaSession.setPlaybackState(state);

// Call this when you start playback after receiving audio focus
        mediaSession.setActive(true);*/

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
            String description = "This is a notification to make sure the listener never gets closed by the system.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
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


    /*@Override
    protected void onStop()
    {
        unregisterReceiver(r);
        super.onStop();
    }*/

    public void startListener(View view) {
        /*IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        filter.setPriority(999999);
        r = new MediaButtonIntentReceiver();
        registerReceiver(r, filter);*/


        MediaSession ms = new MediaSession(getApplicationContext(), getPackageName());
        ms.setActive(true);

        ms.setCallback(new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                KeyEvent keyEvent = (KeyEvent) mediaButtonIntent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyEvent.getKeyCode()) {
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            if (mIsInForegroundMode) {
                                makeSnackbar("Play/Pause");
                            } else {
                                Toast.makeText(MainActivity.this, "Play/Pause", Toast.LENGTH_SHORT).show();
                            }
                            break;

                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            if (mIsInForegroundMode) {
                                makeSnackbar("Next");
                            } else {
                                Toast.makeText(MainActivity.this, "Next", Toast.LENGTH_SHORT).show();
                            }
                            break;

                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            if (mIsInForegroundMode) {
                                makeSnackbar("Previous");
                            } else {
                                Toast.makeText(MainActivity.this, "Previous", Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        PendingIntent mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
        // you can button by receiver after terminating your app
        ms.setMediaButtonReceiver(mediaButtonReceiverPendingIntent);

        // play dummy audio
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
        at.play();

        // a little sleep
        at.stop();
        at.release();
    }

    public void connectSSH() {
        String eonIP = "192.168.1.32";
        writePrivateKeyFile();
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

            session.connect();

            ChannelExec channelssh = (ChannelExec) session.openChannel("exec");
            //ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //channelssh.setOutputStream(baos);


            //channelssh.setCommand("cd /data/HELLOTHERE; mkdir ITSFINALLYWORKING");
            channelssh.connect();
            channelssh.disconnect();
            //Toast.makeText(this, baos.toString(), Toast.LENGTH_SHORT).show();

            //return baos.toString();
        } catch (Exception e) {
            Toast.makeText(this, "Can't connect to EON." +
                    "", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void writePrivateKeyFile() {
        try {
            // catches IOException below
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

            /* We have to use the openFileOutput()-method
             * the ActivityContext provides, to
             * protect your file from others and
             * This is done for security-reasons.
             * We chose MODE_WORLD_READABLE, because
             *  we have nothing to hide in our file */
            File file = new File(getFilesDir(), "eon_id.ppk");
            if (!file.exists() || file.length() == 0) {
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter osw = new OutputStreamWriter(fOut);
                Toast.makeText(this, file.getAbsolutePath(), Toast.LENGTH_SHORT).show();


                // Write the string to the file
                osw.write(privateKey);

                osw.close();
            } //else do nothing, already written

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    public void buttonListener() {
        myList = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("logcat -c");
        } catch (Exception e) {

        }

        listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Process process = Runtime.getRuntime().exec("logcat -d");
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));

                        StringBuilder log = new StringBuilder();
                        String line = "";
                        while ((line = bufferedReader.readLine()) != null) {
                            //Toast.makeText(MainActivity.this, line, Toast.LENGTH_SHORT).show();
                            if (line.contains("action=ACTION_UP, keyCode=KEYCODE_MEDIA_NEXT") && !myList.contains(line)) {
                                myList.add(line);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        System.out.println("Next");
                                        makeSnackbar("Next");
                                        Toast.makeText(MainActivity.this, "Next", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                //connectSSH();
                            } else if (line.contains("action=ACTION_UP, keyCode=KEYCODE_MEDIA_PREVIOUS") && !myList.contains(line)) {
                                myList.add(line);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        System.out.println("Previous");
                                        makeSnackbar("Previous");
                                        Toast.makeText(MainActivity.this, "Previous", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else if (line.contains("action=ACTION_UP, keyCode=KEYCODE_MEDIA_PLAY_PAUSE") && !myList.contains(line)) {
                                myList.add(line);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        makeSnackbar("Play/Pause");
                                        System.out.println("Play/Pause");
                                        Toast.makeText(MainActivity.this, "Play/Pause", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        }
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        if (!alreadyRunning) {
            listenerThread.start();
            alreadyRunning = true;
        }
    }

    public void makeSnackbar(String s) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), s, Snackbar.LENGTH_SHORT);

        snackbar.show();
    }

}
