package com.rakuten.tech.mobile.remoteconfig.sample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.rakuten.tech.mobile.remoteconfig.RemoteConfig
import com.rakuten.tech.mobile.remoteconfig.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.activity = this
    }

    fun onGetStringClick() {
        val key = binding.key.text.toString()
        val fallback = binding.fallback.text.toString()

        val value = RemoteConfig.instance().getString(key, fallback)

        showToast("$key = $value")
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
