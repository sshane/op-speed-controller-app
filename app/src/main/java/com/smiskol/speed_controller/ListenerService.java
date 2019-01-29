package com.smiskol.speed_controller;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.List;

public class ListenerService extends Service {

    public Context context = this;
    public Handler handler = null;
    public static Runnable runnable = null;

    List<String> myList;
    Thread listenerThread;
    Boolean alreadyRunning = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        System.out.println("Service created!");
        //Toast.makeText(this, "Service created!", Toast.LENGTH_LONG).show();
        startListener();

        new Thread() {
            @Override
            public void run() {
                playAudio();
            }
        }.start();


        /*handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                Toast.makeText(context, "Service running", Toast.LENGTH_SHORT).show();
                handler.postDelayed(runnable, 10000);
            }
        };

        handler.postDelayed(runnable, 15000);*/
    }

    public void playAudio() {

        // play dummy audio
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
        at.play();

        // a little sleep
        at.stop();
        at.release();
    }

    public void startListener() {

        MediaSessionCompat ms = new MediaSessionCompat(getApplicationContext(), getPackageName());

        ms.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_FAST_FORWARD | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1, SystemClock.elapsedRealtime())
                .build();
        ms.setPlaybackState(state);

        ms.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                KeyEvent keyEvent = (KeyEvent) mediaButtonIntent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyEvent.getKeyCode()) {
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            Toast.makeText(context, "Play/Pause", Toast.LENGTH_SHORT).show();
                            break;

                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            Toast.makeText(context, "Next", Toast.LENGTH_SHORT).show();
                            break;

                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            Toast.makeText(context, "Previous", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        PendingIntent mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
        // you can button by receiver after terminating your app
        //ms.setMediaButtonReceiver(mediaButtonReceiverPendingIntent);

        ms.setActive(true);
    }

    @Override
    public void onDestroy() {
        /* IF YOU WANT THIS SERVICE KILLED WITH THE APP THEN UNCOMMENT THE FOLLOWING LINE */
        //handler.removeCallbacks(runnable);
        System.out.println("Service stopped");
        //Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show();
    }

    /*@Override
    public void onStart(Intent intent, int startid) {
        Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "1")
                .setContentTitle("Bluetooth Listener Running")
                .setContentText("Tap to configure")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        Notification notification = mBuilder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;


        notificationManager.notify(1, notification);
        startForeground(1, notification);

        return START_NOT_STICKY;
    }
}