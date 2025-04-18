package com.marinov.colegioetapa;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String URL = "https://areaexclusiva.colegioetapa.com.br/home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ajusta padding conforme as “system bars” (status/navigation)
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
            Toast.makeText(this, "Erro: WebView não foi inicializado corretamente!", Toast.LENGTH_LONG).show();
            return;
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // Habilitar JavaScript, mas com moderação
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Permitir carregamento de imagens e outros recursos
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Verificar se há conexão de rede antes de carregar o site
        if (isNetworkAvailable()) {
            webView.loadUrl(URL);
        } else {
            Toast.makeText(this, "Sem conexão de rede!", Toast.LENGTH_SHORT).show();
        }

        // Navegação interna e tratamento de erros
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Toast.makeText(MainActivity.this, "Erro ao carregar: " + error.getDescription(), Toast.LENGTH_LONG).show();
            }
        });

        // Configura download no WebView
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            // Cria uma requisição de download
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Baixando arquivo...");
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType));

            // Inicia o download
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);

            // Exibe uma notificação
            Toast.makeText(getApplicationContext(), "Download iniciado...", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        // Volta no histórico do WebView se possível
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausa o WebView quando o app não está em primeiro plano
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();  // Pausa os timers do WebView
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Retoma o WebView quando o app volta ao primeiro plano
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();  // Retoma os timers do WebView
        }
    }

    @Override
    protected void onDestroy() {
        // Limpeza do WebView para evitar vazamentos de memória
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
