package com.smiskol.speed_controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaButtonIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        /*
        System.out.println("Button pressed.");
        Toast.makeText(context, "Button pressed.", Toast.LENGTH_SHORT).show();
        KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
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
        */
    }
}