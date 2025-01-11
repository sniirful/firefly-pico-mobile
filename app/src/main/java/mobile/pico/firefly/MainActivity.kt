package mobile.pico.firefly

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import mobile.pico.firefly.FirstLaunch.getUrl
import mobile.pico.firefly.ui.theme.AppTheme
import mobile.pico.firefly.ui.theme.DarkColorScheme
import mobile.pico.firefly.ui.theme.LightColorScheme
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

class MainActivity : ComponentActivity() {
    // this cannot be lateinit because of the activity pausing,
    // if the dialog is pending the app crashes
    private var webView: WebView? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    getUrl { url ->
                        setContent {
                            WebViewScreen(isSystemInDarkTheme(), url, padding)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun WebViewScreen(darkTheme: Boolean, url: String, padding: PaddingValues) {
        val canGoBack = remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            onDispose {
                webView?.destroy()
            }
        }

        BackHandler(enabled = canGoBack.value) {
            webView?.goBack()
            canGoBack.value = webView?.canGoBack() ?: false
        }

        AndroidView(factory = { context ->
            WebView(context).apply {
                overScrollMode = WebView.OVER_SCROLL_NEVER

                webView = this
                setBackgroundColor(
                    (if (darkTheme) DarkColorScheme else LightColorScheme).background.toArgb()
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?, request: WebResourceRequest?
                    ): WebResourceResponse? {
                        try {
                            if (request?.method == "GET" && request.url?.toString()?.let { url ->
                                    url.endsWith(".css") || url.endsWith(".js") || url.endsWith(".html") || url.endsWith(
                                        "/"
                                    )
                                } == true) {
                                val requestURL = request.url.toString()
                                val connection =
                                    URL(requestURL).openConnection() as HttpURLConnection
                                connection.requestMethod = "GET"
                                connection.connect()

                                val inputStream = connection.inputStream
                                val responseBody =
                                    inputStream.bufferedReader().use { it.readText() }
                                val modifiedBody = responseBody.replace(
                                    Regex("""env\(safe-area-inset-left(, ?\d+px)?\)"""),
                                    "${padding.calculateLeftPadding(LayoutDirection.Ltr).value}px"
                                ).replace(
                                    Regex("""env\(safe-area-inset-top(, ?\d+px)?\)"""),
                                    "${padding.calculateTopPadding().value}px"
                                ).replace(
                                    Regex("""env\(safe-area-inset-right(, ?\d+px)?\)"""),
                                    "${padding.calculateRightPadding(LayoutDirection.Ltr).value}px"
                                ).replace(
                                    Regex("""env\(safe-area-inset-bottom(, ?\d+px)?\)"""),
                                    "${padding.calculateBottomPadding().value}px"
                                )

                                return WebResourceResponse(
                                    URLConnection.guessContentTypeFromName(requestURL),
                                    "utf-8",
                                    connection.responseCode,
                                    connection.responseMessage,
                                    connection.headerFields.mapValues { it.value.joinToString(",") },
                                    modifiedBody.byteInputStream()
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return null
                    }

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
        webView?.saveState(Bundle())
    }
}
