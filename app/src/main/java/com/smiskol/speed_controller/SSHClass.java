package com.smiskol.speed_controller;

import android.content.Context;
import android.os.StrictMode;
import android.widget.Toast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.File;
import java.util.Properties;

public class SSHClass {

    public Boolean testConnection(Context context, String eonIP) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        try {
            JSch jsch = new JSch();
            File file = new File(context.getFilesDir(), "eon_id.ppk");
            jsch.addIdentity(file.getAbsolutePath());
            Session session = jsch.getSession("root", eonIP, 8022);

            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            prop.put("PreferredAuthentications", "publickey");
            session.setConfig(prop);

            session.connect(5000);

            session.disconnect();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean runSpeedChange(Context context, String eonIP, Integer speedChange) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        try {
            JSch jsch = new JSch();
            File file = new File(context.getFilesDir(), "eon_id.ppk");
            jsch.addIdentity(file.getAbsolutePath());
            Session session = jsch.getSession("root", eonIP, 8022);

            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            prop.put("PreferredAuthentications", "publickey");
            session.setConfig(prop);

            session.connect(5000);

            ChannelExec channelssh = (ChannelExec) session.openChannel("exec");

            channelssh.setCommand("python /data/openpilot/selfdrive/speed_controller.py "+speedChange.toString());
            channelssh.connect();
            channelssh.disconnect();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /*public void testCommand(Context context, String eonIP) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        try {
            JSch jsch = new JSch();
            File file = new File(context.getFilesDir(), "eon_id.ppk");
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

            channelssh.setCommand("python /data/openpilot/selfdrive/speed_controller.py");
            channelssh.connect();
            channelssh.disconnect();
            //Toast.makeText(this, baos.toString(), Toast.LENGTH_SHORT).show();

            //return baos.toString();
        } catch (Exception e) {
            Toast.makeText(this, "Can't connect to EON.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }*/
}
