package com.iptv.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;

/** Full-screen WebView around your IPTV server. The address is asked once and remembered. */
public class MainActivity extends Activity {
    private WebView web;
    private FrameLayout root;
    private View custom;
    private WebChromeClient.CustomViewCallback customCb;
    private WebChromeClient chrome;
    private SharedPreferences prefs;
    private boolean dialogOpen = false;
    private long lastBack = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        prefs = getSharedPreferences("iptv", MODE_PRIVATE);

        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0A0C11);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0A0C11);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        web.addJavascriptInterface(new Bridge(), "AndroidApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) askAddress(true);
            }
        });
        chrome = new WebChromeClient() {
            @Override
            public void onShowCustomView(View v, CustomViewCallback cb) {
                if (custom != null) { cb.onCustomViewHidden(); return; }
                custom = v;
                customCb = cb;
                root.addView(v, new FrameLayout.LayoutParams(-1, -1));
                web.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (custom == null) return;
                root.removeView(custom);
                custom = null;
                web.setVisibility(View.VISIBLE);
                if (customCb != null) customCb.onCustomViewHidden();
                customCb = null;
            }
        };
        web.setWebChromeClient(chrome);

        String u = prefs.getString("url", "");
        if (u.isEmpty()) askAddress(false); else web.loadUrl(u);
    }

    /** Lets the page rotate the phone to landscape while a video plays. */
    public class Bridge {
        @JavascriptInterface
        public void landscape(final boolean on) {
            runOnUiThread(() -> setRequestedOrientation(on
                    ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));
        }
    }

    private void askAddress(final boolean error) {
        if (dialogOpen) return;
        dialogOpen = true;
        final EditText in = new EditText(this);
        in.setSingleLine(true);
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        in.setText(prefs.getString("url", "http://"));
        in.setSelection(in.getText().length());
        AlertDialog.Builder d = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(error ? "Can't reach the server" : "Server address")
                .setMessage("Example: http://192.168.1.50:8080")
                .setView(in)
                .setCancelable(prefs.contains("url"))
                .setPositiveButton("Connect", (dlg, w) -> {
                    String u = in.getText().toString().trim();
                    if (u.isEmpty() || u.equals("http://")) {
                        web.post(() -> askAddress(false));
                        return;
                    }
                    if (!u.startsWith("http")) u = "http://" + u;
                    prefs.edit().putString("url", u).apply();
                    web.loadUrl(u);
                })
                .setOnDismissListener(dlg -> dialogOpen = false);
        if (error && prefs.contains("url")) {
            d.setNeutralButton("Retry", (dlg, w) -> web.loadUrl(prefs.getString("url", "")));
        }
        d.show();
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) {
            if (custom != null) { chrome.onHideCustomView(); return true; }
            long now = System.currentTimeMillis();
            if (now - lastBack < 450) { finish(); return true; }   // press Back twice quickly to exit
            lastBack = now;
            // let the page handle Back itself (close player, go up one level ...)
            web.evaluateJavascript("(function(){var t=document.activeElement||document.body;"
                    + "t.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',keyCode:27,which:27,bubbles:true,cancelable:true}))})()", null);
            return true;
        }
        if (code == KeyEvent.KEYCODE_MENU) { askAddress(false); return true; }   // change server address
        return super.onKeyDown(code, e);
    }

    @Override
    public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override protected void onPause() { super.onPause(); web.onPause(); }
    @Override protected void onResume() { super.onResume(); web.onResume(); }
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
