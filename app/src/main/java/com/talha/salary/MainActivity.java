package com.talha.salary;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> uploadCallback;
    private static final int FILE_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params) {

                if (uploadCallback != null) {
                    uploadCallback.onReceiveValue(null);
                }

                uploadCallback = callback;

                Intent intent = params.createIntent();

                try {
                    startActivityForResult(intent, FILE_REQUEST);
                } catch (Exception e) {
                    uploadCallback = null;
                    Toast.makeText(MainActivity.this,
                        "File picker unavailable",
                        Toast.LENGTH_SHORT).show();
                    return false;
                }

                return true;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    request.grant(request.getResources());
                });
            }
        });

        webView.loadUrl("https://talha-salary.edgeone.dev/");
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_REQUEST && uploadCallback != null) {
            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    results = new Uri[]{uri};
                }
            }

            uploadCallback.onReceiveValue(results);
            uploadCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
