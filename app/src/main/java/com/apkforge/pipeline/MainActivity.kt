package com.apkforge.pipeline

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.versionLine).text =
            getString(
                R.string.version_line,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            )
        findViewById<TextView>(R.id.buildType).text = BuildConfig.BUILD_TYPE
        findViewById<TextView>(R.id.buildStamp).text = getString(R.string.build_stamp_placeholder)
    }
}
