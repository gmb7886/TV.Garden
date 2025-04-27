package com.marinov.tvgarden;

import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import android.widget.Toast; // Mantido para toasts de download
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private static final String URL = "https://tv.garden/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ajusta padding conforme as system bars
        View mainLayout = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout,
                (v, insets) -> {
                    Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom);
                    return insets;
                }
        );

        // Inicializa o WebView
        webView = findViewById(R.id.webview);
        if (webView == null) {
            // WebView não inicializado corretamente
            return;
        }

        // Configurações básicas do WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        // Habilita full-screen de vídeo
        settings.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Configuração de cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        } else {
            CookieSyncManager.createInstance(this);
            CookieSyncManager.getInstance().startSync();
        }

        // Restaurar cookies salvos
        SharedPreferences prefs = getSharedPreferences("cookies", MODE_PRIVATE);
        String savedCookies = prefs.getString("saved_cookies", null);
        if (savedCookies != null) {
            for (String cookie : savedCookies.split(";")) {
                cookieManager.setCookie(URL, cookie.trim());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.flush();
            } else {
                CookieSyncManager.getInstance().sync();
            }
        }

        // Configura WebChromeClient para suportar vídeo em tela cheia
        webView.setWebChromeClient(new WebChromeClient() {
            private View customView;
            private WebChromeClient.CustomViewCallback customViewCallback;
            private int originalSystemUiVisibility;

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // Entrar em full-screen
                customView = view;
                customViewCallback = callback;
                originalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.addView(customView, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_IMMERSIVE);
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                // Sair do full-screen
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.removeView(customView);
                getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
                customViewCallback.onCustomViewHidden();
                webView.setVisibility(View.VISIBLE);
                customView = null;
            }
        });

        // Carrega o site se houver conexão
        if (isNetworkAvailable()) {
            webView.loadUrl(URL);
        } else {
            // Sem conexão: sem exibição de Toast de erro
            return;
        }

        // Listener para downloads (mantém toasts de download)
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Toast.makeText(MainActivity.this, "Download iniciado: " + url, Toast.LENGTH_SHORT).show();
            // Lógica de download a implementar
        });

        // WebViewClient para navegação interna e persistência de cookies
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Sincroniza cookies
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    cookieManager.flush();
                } else {
                    CookieSyncManager.getInstance().sync();
                }
                // Salva cookies
                String allCookies = cookieManager.getCookie(URL);
                prefs.edit().putString("saved_cookies", allCookies).apply();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                // Erro de carregamento ignorado (sem Toast de erro)
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.getVisibility() == View.GONE) {
                // Se estiver em full-screen, sair dele
                WebChromeClient.CustomViewCallback cb = null;
                // O callback já tratou a remoção
                return true;
            } else if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
        }
        super.onDestroy();
    }

    // Verifica se há conexão de rede disponível
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
