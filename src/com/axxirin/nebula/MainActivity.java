package com.axxirin.nebula;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends Activity {
  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    if (!Settings.canDrawOverlays(this)) {
      Toast.makeText(this, "Aktifkan 'Tampil di atas aplikasi lain' untuk NEBULA, lalu buka lagi", Toast.LENGTH_LONG).show();
      startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
    } else {
      startService(new Intent(this, FloatingService.class));
    }
    finish();
  }
}
