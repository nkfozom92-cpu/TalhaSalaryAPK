package com.talha.salary;

import android.app.Activity;
import android.app.DownloadManager;
import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.URLUtil;
import android.webkit.CookieManager;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> uploadCallback;
    private static final int FILE_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.WHITE);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(10, 8, 10, 8);
        toolbar.setBackgroundColor(Color.rgb(20, 110, 65));

        Button downloadButton = new Button(this);
        downloadButton.setText("⬇ Download");
        downloadButton.setTextColor(Color.WHITE);
        downloadButton.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(20, 110, 65)
            )
        );

        downloadButton.setOnClickListener(v -> {
            Toast.makeText(this,
                "ওয়েবসাইটের PDF বা ছবি Download বাটনে চাপ দিন",
                Toast.LENGTH_LONG).show();
        });

        toolbar.addView(downloadButton);

        webView = new WebView(this);
        layout.addView(toolbar,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));

        layout.addView(webView,
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1
            ));

        setContentView(layout);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());

        webView.setDownloadListener((url, userAgent,
                contentDisposition, mimeType, contentLength) -> {

            try {
                DownloadManager.Request request =
                    new DownloadManager.Request(Uri.parse(url));

                String cookie =
                    CookieManager.getInstance().getCookie(url);

                if (cookie != null) {
                    request.addRequestHeader("Cookie", cookie);
                }

                if (userAgent != null) {
                    request.addRequestHeader("User-Agent", userAgent);
                }

                String filename = URLUtil.guessFileName(
                    url, contentDisposition, mimeType
                );

                filename = filename.replaceAll(
                    "[\\\\/:*?\"<>|]", "_"
                );

                request.setTitle(filename);
                request.setDescription("TALHA SALARY Download");
                request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                );

                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, filename
                );

                DownloadManager manager =
                    (DownloadManager) getSystemService(
                        Context.DOWNLOAD_SERVICE
                    );

                manager.enqueue(request);

                Toast.makeText(this,
                    "Download শুরু হয়েছে",
                    Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this,
                    "Download failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        });

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

                try {
                    startActivityForResult(
                        params.createIntent(), FILE_REQUEST
                    );
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
