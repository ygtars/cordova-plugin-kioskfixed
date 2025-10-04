package jk.cordova.plugin.kiosk;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import org.apache.cordova.*;
import android.widget.*;
import android.view.Window;
import android.view.View;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.view.ViewGroup.LayoutParams;
import java.lang.Integer;
import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class KioskActivity extends CordovaActivity {

    public static volatile boolean running = false;
    public static volatile Set<Integer> allowedKeys = Collections.EMPTY_SET;

    private StatusBarOverlay statusBarOverlay = null;

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("KioskActivity started");
        running = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("KioskActivity stopped");
        running = false;
    }

    @Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    System.out.println("KioskActivity onCreate start");

    // Cordova initialization
    super.init();

    // Prevent multiple instances
    if (running) {
        System.out.println("Another KioskActivity running, finishing this one");
        finish();
        return;
    }

    // Güvenli launchUrl
    if (launchUrl == null || launchUrl.isEmpty()) {
        launchUrl = "file:///android_asset/www/index.html";
        System.out.println("launchUrl was null, using default: " + launchUrl);
    }

    try {
        loadUrl(launchUrl);
    } catch (Exception e) {
        System.out.println("Error loading launchUrl: " + e.getMessage());
        e.printStackTrace();
    }

    // Fullscreen ve status bar gizleme
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

    View decorView = getWindow().getDecorView();
    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    ActionBar actionBar = getActionBar();
    if (actionBar != null) actionBar.hide();

    // StatusBar overlay (izinleri kontrol et)
    try {
        statusBarOverlay = StatusBarOverlay.createOrObtainPermission(this);
    } catch (Exception e) {
        System.out.println("StatusBarOverlay failed: " + e.getMessage());
        e.printStackTrace();
    }

    running = true;
    System.out.println("KioskActivity onCreate end");
}


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (statusBarOverlay != null) {
            statusBarOverlay.destroy(this);
            statusBarOverlay = null;
        }
    }

    @Override
    protected void onPause() {
            super.onPause();
            ActivityManager activityManager = (ActivityManager) getApplicationContext()
                    .getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.moveTaskToFront(getTaskId(), 0);
    }     
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        System.out.println("onKeyDown event: keyCode = " + event.getKeyCode());
        return ! allowedKeys.contains(event.getKeyCode()); // prevent event from being propagated if not allowed
    }
    
    @Override
    public void finish() {
        System.out.println("Never finish...");
        // super.finish();
    }

    // http://www.andreas-schrade.de/2015/02/16/android-tutorial-how-to-create-a-kiosk-mode-in-android/
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(!hasFocus) {
            System.out.println("Focus lost - closing system dialogs");
            
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
            
            ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
            am.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
            
            // sometime required to close opened notification area
            Timer timer = new Timer();
            timer.schedule(new TimerTask(){
                public void run() {
                    Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    sendBroadcast(closeDialog);
                }
            }, 500); // 0.5 second
        }
    }
}

