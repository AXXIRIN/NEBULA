package com.axxirin.nebula;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import org.json.JSONObject;

public class FloatingService extends Service {
  static final int BG = 0xFF0A0A1A, PANEL = 0xFF13132B, LINE = 0xFF26264D, CYAN = 0xFF5EF2FF, VIOLET = 0xFFB388FF, PINK = 0xFFFF6EC7;
  WindowManager wm;
  FrameLayout win;
  TextView bubble;
  WebView web;
  WindowManager.LayoutParams wp, bp;
  File cwd;
  boolean mini = false;
  final Handler ui = new Handler(Looper.getMainLooper());

  @Override public IBinder onBind(Intent i) { return null; }
  @Override public int onStartCommand(Intent i, int f, int s) { return START_STICKY; }

  int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

  WindowManager.LayoutParams params(int w, int h, boolean focus) {
    int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        | (focus ? 0 : WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    WindowManager.LayoutParams p = new WindowManager.LayoutParams(w, h, type, flags, PixelFormat.TRANSLUCENT);
    p.gravity = Gravity.TOP | Gravity.START;
    return p;
  }

  TextView tv(String s, int sp, int col) {
    TextView t = new TextView(this);
    t.setText(s); t.setTextSize(sp); t.setTextColor(col);
    t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    t.setGravity(Gravity.CENTER);
    return t;
  }

  abstract class Touch implements View.OnTouchListener {
    float rx, ry; int a, b; boolean moved;
    void down() {}
    abstract void move(int dx, int dy);
    void up() {}
    public boolean onTouch(View v, MotionEvent e) {
      switch (e.getActionMasked()) {
        case MotionEvent.ACTION_DOWN: rx = e.getRawX(); ry = e.getRawY(); moved = false; down(); break;
        case MotionEvent.ACTION_MOVE:
          int dx = (int) (e.getRawX() - rx), dy = (int) (e.getRawY() - ry);
          if (Math.abs(dx) + Math.abs(dy) > dp(6)) moved = true;
          move(dx, dy); break;
        case MotionEvent.ACTION_UP: up(); break;
      }
      return true;
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    cwd = getFilesDir();
    if (Build.VERSION.SDK_INT >= 26) {
      getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("nb", "NEBULA", NotificationManager.IMPORTANCE_MIN));
    }
    Notification.Builder nb = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "nb") : new Notification.Builder(this);
    startForeground(1, nb.setContentTitle("NEBULA aktif").setSmallIcon(android.R.drawable.ic_menu_info_details).build());
    buildWindow();
    buildBubble();
    wm.addView(win, wp);
  }

  void buildWindow() {
    wp = params(dp(320), dp(460), true);
    wp.x = dp(16); wp.y = dp(80);
    wp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

    win = new FrameLayout(this);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(BG); bg.setCornerRadius(dp(12)); bg.setStroke(dp(1), LINE);
    win.setBackground(bg);
    win.setClipToOutline(true);

    LinearLayout col = new LinearLayout(this);
    col.setOrientation(LinearLayout.VERTICAL);

    LinearLayout bar = new LinearLayout(this);
    bar.setBackgroundColor(PANEL);
    bar.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = tv("● nebula.exe", 12, VIOLET);
    title.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
    title.setPadding(dp(12), 0, 0, 0);
    TextView mn = tv("—", 16, CYAN), cl = tv("✕", 14, PINK);
    mn.setOnClickListener(v -> minimize());
    cl.setOnClickListener(v -> stopSelf());
    bar.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));
    bar.addView(mn, new LinearLayout.LayoutParams(dp(44), -1));
    bar.addView(cl, new LinearLayout.LayoutParams(dp(44), -1));
    title.setOnTouchListener(new Touch() {
      void down() { a = wp.x; b = wp.y; }
      void move(int dx, int dy) { wp.x = a + dx; wp.y = b + dy; wm.updateViewLayout(win, wp); }
    });

    web = new WebView(this);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowUniversalAccessFromFileURLs(true);
    web.setBackgroundColor(BG);
    web.setWebViewClient(new WebViewClient());
    web.addJavascriptInterface(new Bridge(), "Android");
    web.loadUrl("file:///android_asset/index.html");

    col.addView(bar, new LinearLayout.LayoutParams(-1, dp(34)));
    col.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));
    win.addView(col, new FrameLayout.LayoutParams(-1, -1));

    TextView grip = tv("◢", 16, VIOLET);
    grip.setOnTouchListener(new Touch() {
      void down() { a = wp.width; b = wp.height; }
      void move(int dx, int dy) {
        wp.width = Math.max(dp(240), a + dx); wp.height = Math.max(dp(300), b + dy);
        wm.updateViewLayout(win, wp);
      }
    });
    win.addView(grip, new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.BOTTOM | Gravity.END));
  }

  void buildBubble() {
    bubble = tv("N", 22, CYAN);
    GradientDrawable g = new GradientDrawable();
    g.setShape(GradientDrawable.OVAL); g.setColor(BG); g.setStroke(dp(2), VIOLET);
    bubble.setBackground(g);
    bp = params(dp(56), dp(56), false);
    bubble.setOnTouchListener(new Touch() {
      void down() { a = bp.x; b = bp.y; }
      void move(int dx, int dy) { bp.x = a + dx; bp.y = b + dy; wm.updateViewLayout(bubble, bp); }
      void up() { if (!moved) restore(); }
    });
  }

  void minimize() { bp.x = wp.x; bp.y = wp.y; wm.removeView(win); wm.addView(bubble, bp); mini = true; }
  void restore() { wm.removeView(bubble); wm.addView(win, wp); mini = false; }

  @Override public void onDestroy() {
    try { wm.removeView(mini ? bubble : win); } catch (Exception e) {}
    try { win.removeAllViews(); web.destroy(); } catch (Exception e) {}
    super.onDestroy();
  }

  class Bridge {
    File notesFile() { return new File(getFilesDir(), "notes.json"); }

    @JavascriptInterface public String loadNotes() {
      try {
        File f = notesFile();
        if (!f.exists()) return "[]";
        return new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
      } catch (Exception e) { return "[]"; }
    }

    @JavascriptInterface public void saveNotes(String json) {
      try {
        File t = new File(getFilesDir(), "notes.tmp");
        FileOutputStream o = new FileOutputStream(t);
        o.write(json.getBytes("UTF-8")); o.getFD().sync(); o.close();
        t.renameTo(notesFile());
      } catch (Exception e) {}
    }

    @JavascriptInterface public void exec(final String cmd, final int id) {
      new Thread(() -> {
        String out;
        try {
          String c = cmd.trim();
          if (c.equals("cd") || c.startsWith("cd ")) {
            String p = c.substring(2).trim();
            File d = p.isEmpty() ? getFilesDir() : (p.startsWith("/") ? new File(p) : new File(cwd, p));
            d = d.getCanonicalFile();
            if (d.isDirectory()) { cwd = d; out = ""; } else out = "cd: " + p + ": tidak ada";
          } else {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd).directory(cwd).redirectErrorStream(true);
            pb.environment().put("HOME", getFilesDir().getPath());
            final Process pr = pb.start();
            ui.postDelayed(() -> pr.destroy(), 30000);
            BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String ln;
            while ((ln = br.readLine()) != null && sb.length() < 20000) sb.append(ln).append('\n');
            out = sb.toString();
          }
        } catch (Exception e) { out = "error: " + e.getMessage(); }
        final String o = out, cw = cwd.getPath();
        ui.post(() -> web.evaluateJavascript("onExec(" + id + "," + JSONObject.quote(o) + "," + JSONObject.quote(cw) + ")", null));
      }).start();
    }
  }
}
