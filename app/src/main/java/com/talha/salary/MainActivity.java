package com.talha.salary;

import android.app.Activity;
import android.app.DownloadManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.URLUtil;
import android.webkit.CookieManager;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> uploadCallback;
    private static final int FILE_REQUEST = 100;
    private static final int STORAGE_REQUEST = 101;

    private String pendingUrl;
    private String pendingUserAgent;
    private String pendingMime;
    private String pendingFilename;

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

        webView.setDownloadListener((url, userAgent,
                contentDisposition, mimeType, contentLength) -> {

            String filename = URLUtil.guessFileName(
                url, contentDisposition, mimeType
            );

            filename = filename.replaceAll(
                "[\\\\/:*?\"<>|]", "_"
            );

            startDownload(url, userAgent, mimeType, filename);
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

    private void startDownload(String url, String userAgent,
                               String mimeType, String filename) {

        pendingUrl = url;
        pendingUserAgent = userAgent;
        pendingMime = mimeType;
        pendingFilename = filename;

        if (Build.VERSION.SDK_INT <= 28 &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_REQUEST
            );
            return;
        }

        enqueueDownload();
    }

    private void enqueueDownload() {
        try {
            DownloadManager.Request request =
                new DownloadManager.Request(Uri.parse(pendingUrl));

            String cookie = CookieManager.getInstance()
                .getCookie(pendingUrl);

            if (cookie != null) {
                request.addRequestHeader("Cookie", cookie);
            }

            if (pendingUserAgent != null) {
                request.addRequestHeader("User-Agent", pendingUserAgent);
            }

            if (pendingMime != null) {
                request.setMimeType(pendingMime);
            }

            request.setTitle(pendingFilename);
            request.setDescription("TALHA SALARY Download");
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                pendingFilename
            );

            DownloadManager manager =
                (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            manager.enqueue(request);

            Toast.makeText(this,
                "Download শুরু হয়েছে",
                Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this,
                "Download failed: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == STORAGE_REQUEST) {
            if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enqueueDownload();
            } else {
                Toast.makeText(this,
                    "Storage permission denied",
                    Toast.LENGTH_SHORT).show();
            }
        }
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
