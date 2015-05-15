package jp.kshoji.stlviewer.activity;


import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import jp.kshoji.stlviewer.R;

public class WebViewActivity extends Activity {

    private WebView webView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);

        webView = (WebView) findViewById(R.id.webView1);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/radartheory.html");

    }

}