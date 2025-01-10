package mobile.pico.firefly

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import mobile.pico.firefly.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                WebViewScreen("http://10.10.10.10:6976")
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun WebViewScreen(url: String) {
        val canGoBack = remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            onDispose {
                webView.destroy()
            }
        }

        BackHandler(enabled = canGoBack.value) {
            webView.goBack()
        }

        AndroidView(factory = { context ->
            WebView(context).apply {
                overScrollMode = WebView.OVER_SCROLL_NEVER

                webView = this
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        canGoBack.value = view?.canGoBack() ?: false
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                }
                CookieManager.getInstance().setAcceptCookie(true)
                fitsSystemWindows = true

                loadUrl(url)
            }
        }, modifier = Modifier.fillMaxSize(), update = { view ->
            webView = view
        })
    }

    override fun onResume() {
        super.onResume()
        CookieManager.getInstance().flush()
    }

    override fun onPause() {
        super.onPause()
        webView.saveState(Bundle())
    }
}
