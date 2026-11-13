package com.s2007.autopay;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * S2007-AUTO-PAY — configuration screen.
 * The SMS listener itself runs silently in the background (no service needed).
 */
public class MainActivity extends Activity {

    private static final int REQ_SMS = 11;
    private static final int COLOR_RED = 0xFFD32F2F;
    private static final int COLOR_TEXT = 0xFF191417;
    private static final int COLOR_TEXT2 = 0xFF6B6266;

    private SharedPreferences sp;
    private EditText urlEt, secretEt;
    private TextView statusTv, smsSt, accSt, admSt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("s2007", Context.MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(245, 243, 244));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(40));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        /* ---------- header ---------- */
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable hg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{0xFFE53935, 0xFF9A1616});
        hg.setCornerRadius(dp(24));
        header.setBackground(hg);
        header.setPadding(dp(24), dp(26), dp(24), dp(26));

        TextView title = new TextView(this);
        title.setText("S2007-AUTO-PAY");
        title.setTextColor(Color.WHITE);
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title);

        TextView sub = new TextView(this);
        sub.setText("bKash & Rocket SMS \u2192 Firebase \u2192 Website\nAuto balance check \u2014 runs in background");
        sub.setTextColor(0xFFFFDAD6);
        sub.setTextSize(12.5f);
        sub.setLineSpacing(dp(2), 1f);
        header.addView(sub, margins(dp(0), dp(8), dp(0), dp(0)));
        root.addView(header, margins(dp(0), dp(0), dp(0), dp(14)));

        /* ---------- global status line ---------- */
        statusTv = statusLine();
        statusTv.setTextSize(14);
        statusTv.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(statusTv, margins(dp(4), dp(0), dp(4), dp(14)));

        /* ---------- config card ---------- */
        LinearLayout cfgCard = card("Firebase configuration");
        root.addView(cfgCard, margins(dp(0), dp(0), dp(0), dp(14)));

        urlEt = input("Realtime Database URL", "https://YOUR-PROJECT-default-rtdb.firebaseio.com");
        cfgCard.addView(urlEt, margins(dp(0), dp(0), dp(0), dp(10)));

        secretEt = input("Database secret (legacy token)", "Database secret");
        cfgCard.addView(secretEt, margins(dp(0), dp(0), dp(0), dp(14)));

        LinearLayout cfgBtns = new LinearLayout(this);
        cfgBtns.setOrientation(LinearLayout.HORIZONTAL);
        Button save = button("SAVE", COLOR_RED, Color.WHITE);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit()
                        .putString("url", Db.baseUrl(urlEt.getText().toString()))
                        .putString("secret", secretEt.getText().toString().trim())
                        .apply();
                toast("Configuration saved");
                refresh();
            }
        });
        Button test = button("TEST CONNECTION", 0x14D32F2F, COLOR_RED);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { testConnection(); }
        });
        cfgBtns.addView(save, weighted(dp(0), dp(0), dp(8), dp(0)));
        cfgBtns.addView(test, weighted(dp(0), dp(0), dp(0), dp(0)));
        cfgCard.addView(cfgBtns);

        /* ---------- permissions card ---------- */
        LinearLayout permCard = card("Permissions & background");
        root.addView(permCard, margins(dp(0), dp(0), dp(0), dp(14)));

        smsSt = statusLine();
        accSt = statusLine();
        admSt = statusLine();
        permCard.addView(smsSt);
        permCard.addView(accSt);
        permCard.addView(admSt, margins(dp(0), dp(0), dp(0), dp(12)));

        Button bSms = button("GRANT SMS PERMISSION", COLOR_RED, Color.WHITE);
        bSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { askSms(); }
        });
        permCard.addView(bSms, margins(dp(0), dp(0), dp(0), dp(8)));

        Button bAcc = button("ENABLE ACCESSIBILITY", 0x14D32F2F, COLOR_RED);
        bAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        permCard.addView(bAcc, margins(dp(0), dp(0), dp(0), dp(8)));

        Button bAdm = button("ENABLE DEVICE ADMIN", 0x14D32F2F, COLOR_RED);
        bAdm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { askAdmin(); }
        });
        permCard.addView(bAdm);

        /* ---------- info card ---------- */
        LinearLayout info = card("How it works");
        TextView infoTv = new TextView(this);
        infoTv.setText(
                "1. Save the Firebase URL + database secret above.\n" +
                "2. Grant the three permissions (SMS, Accessibility, Device admin).\n" +
                "3. Done \u2014 the app now listens 24/7 (also after reboot, no UI needed).\n\n" +
                "Every bKash / Rocket SMS with a TrxID is saved to:\n" +
                "smsPayments/{TRXID} \u2014 method, amount, number, time.\n\n" +
                "On the website the buyer submits amount + TrxID in the Wallet; " +
                "when the TrxID matches, the balance is added automatically. " +
                "One TrxID can only be used once.");
        infoTv.setTextColor(COLOR_TEXT2);
        infoTv.setTextSize(12.5f);
        infoTv.setLineSpacing(dp(3), 1f);
        info.addView(infoTv, margins(dp(0), dp(4), dp(0), dp(0)));
        root.addView(info);

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    /* ================= actions ================= */

    private void refresh() {
        urlEt.setText(sp.getString("url", ""));
        secretEt.setText(sp.getString("secret", ""));
        boolean cfg = !sp.getString("url", "").isEmpty() && !sp.getString("secret", "").isEmpty();
        smsSt.setText("SMS permission:  " + (hasSms() ? "GRANTED \u2714" : "NOT GRANTED \u2716"));
        accSt.setText("Accessibility:  " + (accessOn() ? "ON \u2714" : "OFF \u2716"));
        admSt.setText("Device admin:  " + (adminOn() ? "ACTIVE \u2714" : "INACTIVE \u2716"));
        statusTv.setText("Status:  " + (cfg
                ? "CONFIGURED \u2714  \u2014 listening for payment SMS"
                : "NOT CONFIGURED \u2716  \u2014 save URL + secret first"));
        statusTv.setTextColor(cfg ? 0xFF1A7F37 : COLOR_RED);
    }

    private void askSms() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{
                    Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS}, REQ_SMS);
        } else {
            toast("Already granted at install");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_SMS) {
            refresh();
            toast(hasSms() ? "SMS permission granted" : "SMS permission denied");
        }
    }

    private void askAdmin() {
        ComponentName cn = new ComponentName(this, AdminReceiver.class);
        Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn);
        i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Keeps S2007-AUTO-PAY protected and active on this device.");
        try {
            startActivity(i);
        } catch (Exception e) {
            toast("Device admin not available on this phone");
        }
    }

    private void testConnection() {
        final String base = Db.baseUrl(urlEt.getText().toString());
        final String secret = secretEt.getText().toString().trim();
        if (base.isEmpty() || secret.isEmpty()) {
            toast("Enter URL and secret first");
            return;
        }
        toast("Testing\u2026");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String res = Db.request(base + "/.json?auth=" + Db.auth(secret) + "&shallow=true", "GET", null);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (res != null && !res.contains("error")) toast("Connected \u2714 Database reachable");
                        else toast("Failed \u2716 Check URL / secret / internet");
                    }
                });
            }
        }).start();
    }

    /* ================= checks ================= */

    private boolean hasSms() {
        if (Build.VERSION.SDK_INT < 23) return true;
        return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean accessOn() {
        String svc = getPackageName() + "/" + KeepAliveAccessibilityService.class.getCanonicalName();
        String set = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return set != null && set.contains(svc);
    }

    private boolean adminOn() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(new ComponentName(this, AdminReceiver.class));
    }

    /* ================= tiny UI helpers ================= */

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private LinearLayout card(String title) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), 0x1A191417);
        c.setBackground(bg);
        c.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(COLOR_TEXT);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        c.addView(t, margins(dp(0), dp(0), dp(0), dp(12)));
        return c;
    }

    private EditText input(String hint, String desc) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(13.5f);
        e.setTextColor(COLOR_TEXT);
        e.setHintTextColor(COLOR_TEXT2);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(248, 246, 247));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0x1A191417);
        e.setBackground(bg);
        e.setPadding(dp(14), dp(13), dp(14), dp(13));
        e.setSingleLine(true);
        e.setContentDescription(desc);
        return e;
    }

    private Button button(String label, int bg, int fg) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(true);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(fg);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(dp(999));
        b.setBackground(g);
        b.setPadding(dp(18), dp(14), dp(18), dp(14));
        b.setGravity(Gravity.CENTER);
        return b;
    }

    private TextView statusLine() {
        TextView t = new TextView(this);
        t.setTextSize(13);
        t.setTextColor(COLOR_TEXT2);
        t.setPadding(dp(4), dp(6), dp(4), dp(6));
        return t;
    }

    private LinearLayout.LayoutParams margins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(l, t, r, b);
        return p;
    }

    private LinearLayout.LayoutParams weighted(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(l, t, r, b);
        return p;
    }
}
