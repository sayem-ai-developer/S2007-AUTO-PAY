package com.s2007.autopay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * After reboot the manifest-registered SmsReceiver starts receiving SMS
 * again automatically. This receiver only pings the database so the admin
 * can see the phone restarted and the listener is alive.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences sp = context.getSharedPreferences("s2007", Context.MODE_PRIVATE);
        final String base = Db.baseUrl(sp.getString("url", ""));
        final String secret = sp.getString("secret", "");
        if (base.isEmpty() || secret.isEmpty()) return;

        final PendingResult pr = goAsync();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("time", System.currentTimeMillis());
                    Db.request(base + "/appStatus/lastBoot.json?auth=" + Db.auth(secret), "PUT", o.toString());
                } catch (Throwable ignored) {
                } finally {
                    pr.finish();
                }
            }
        }).start();
    }
}
