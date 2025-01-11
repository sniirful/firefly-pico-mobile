package mobile.pico.firefly

import android.app.Activity.MODE_PRIVATE
import android.content.SharedPreferences
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread

object FirstLaunch {
    private lateinit var sharedPreferences: SharedPreferences

    fun ComponentActivity.getUrl(callback: (String) -> Unit) {
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("firefly_pico_url", null)
        if (savedUrl != null) {
            callback(savedUrl)
        } else {
            promptForUrl(0, callback)
        }
    }

    private fun ComponentActivity.promptForUrl(retries: Int, callback: (String) -> Unit) {
        val editText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 16f
        }
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                20 * resources.displayMetrics.density.toInt(), // dp to px
                0,
                20 * resources.displayMetrics.density.toInt(), // dp to px
                0
            )
            addView(editText)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("${if (retries > 0) "Wrong or unreachable URL. " else ""}Insert the ${if (retries > 0) "correct " else ""}Firefly Pico instance URL:")
            .setView(linearLayout).setPositiveButton("OK") { _, _ ->
                val url = editText.text.toString()
                if (isValidUrl(url)) {
                    sharedPreferences.edit().putString("firefly_pico_url", url).apply()
                    callback(url)
                } else {
                    promptForUrl(retries + 1, callback)
                }
            }.setCancelable(false).create()
        dialog.show()

        // this enables the keyboard animation
        ViewCompat.setWindowInsetsAnimationCallback(
            editText,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    return WindowInsetsCompat.CONSUMED
                }
            }
        )
    }

    private fun isValidUrl(url: String): Boolean {
        var isValid = false
        thread {
            isValid = try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                connection.responseCode == 200
            } catch (e: MalformedURLException) {
                false
            }
        }.join()

        return isValid
    }
}
