package com.rakuten.tech.mobile.remoteconfig.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.rakuten.tech.mobile.remoteconfig.FetchConfigCompletionListener
import com.rakuten.tech.mobile.remoteconfig.RemoteConfig
import com.rakuten.tech.mobile.remoteconfig.sample.databinding.ActivityMainBinding

class MainActivity @VisibleForTesting constructor(
    private val remoteConfig: RemoteConfig
) : AppCompatActivity() {

    constructor(): this(RemoteConfig.instance())

    private lateinit var binding: ActivityMainBinding

    private val key get() = binding.key.text.toString()
    private val fallback get() = binding.fallback.text.toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.activity = this
    }

    fun onGetStringClick() = showConfigToast(key) { remoteConfig.getString(key, fallback) }

    fun onGetBooleanClick() = showConfigToast(key) { remoteConfig.getBoolean(key, fallback.toBoolean()) }

    fun onGetLongClick() = showConfigToast(key) { remoteConfig.getNumber(key, fallback.toLong()) }

    fun onGetShortClick() = showConfigToast(key) { remoteConfig.getNumber(key, fallback.toShort()) }

    fun onGetDoubleClick() = showConfigToast(key) { remoteConfig.getNumber(key, fallback.toDouble())}

    fun onGetFloatClick() = showConfigToast(key) { remoteConfig.getNumber(key, fallback.toFloat()) }

    fun onGetIntClick() = showConfigToast(key) { remoteConfig.getNumber(key, fallback.toInt())}

    fun onGetByteClick() = showConfigToast(key) { remoteConfig.getNumber(key, fallback.toByte())}

    fun onGetConfigClick() = showConfigToast("Config") { remoteConfig.getConfig() }

    fun onFetchConfigClick() = remoteConfig.fetchAndApplyConfig(object: FetchConfigCompletionListener {
        override fun onFetchError(ex: Exception) {
            displayToast("Error retrieving config values: ${ex.message}")
        }

        override fun onFetchComplete(config: Map<String, String>) {
            displayToast("Config = $config")
        }

        private fun displayToast(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()}
        }
    })


    private fun <T> showConfigToast(title: String, configGetter: () -> T) {
        val message = try {
            "$title = ${configGetter.invoke()}"
        } catch (e: Exception) {
            Log.e("Remote Config Sample", "Error retrieving remote config value.", e)

            "Error retrieving config values: ${e.message}"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT)
            .show()
    }
}
