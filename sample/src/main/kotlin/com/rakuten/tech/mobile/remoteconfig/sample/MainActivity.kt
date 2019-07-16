package com.rakuten.tech.mobile.remoteconfig.sample

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
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

    fun onGetStringClick() = showToast(key, remoteConfig.getString(key, fallback))

    fun onGetBooleanClick() = showToast(key, remoteConfig.getBoolean(key, fallback.toBoolean()))

    fun onGetLongClick() = showToast(key, remoteConfig.getNumber(key, fallback.toLong()))

    fun onGetShortClick() = showToast(key, remoteConfig.getNumber(key, fallback.toShort()))

    fun onGetDoubleClick() = showToast(key, remoteConfig.getNumber(key, fallback.toDouble()))

    fun onGetFloatClick() = showToast(key, remoteConfig.getNumber(key, fallback.toFloat()))

    fun onGetIntClick() = showToast(key, remoteConfig.getNumber(key, fallback.toInt()))

    fun onGetByteClick() = showToast(key, remoteConfig.getNumber(key, fallback.toByte()))

    fun onGetConfigClick() = showToast("Config", remoteConfig.getConfig())

    private fun showToast(key: String, value: Any) {
        Toast.makeText(
            this,
            "$key = $value",
            Toast.LENGTH_SHORT
        ).show()
    }
}
