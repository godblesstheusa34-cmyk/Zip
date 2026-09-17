package com.s24ultra.fluidlauncher

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Display
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.s24ultra.fluidlauncher.data.LauncherModel
import kotlin.concurrent.thread

class LauncherActivity : ComponentActivity() {
    private lateinit var home: FluidHomeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        enableEdgeToEdge()
        requestFastestDisplayMode()
        home = FluidHomeView(this)
        setContentView(home)
        ViewCompat.setOnApplyWindowInsetsListener(home) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        home.onOpenTuning = ::showTuning
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { home.closeDrawer() }
        })
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun loadApps() {
        val size = (72 * resources.displayMetrics.density).toInt()
        thread(name = "app-loader") {
            val loaded = LauncherModel.load(this, size)
            runOnUiThread { if (!isFinishing) home.apps = loaded }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestFastestDisplayMode() {
        val display = if (android.os.Build.VERSION.SDK_INT >= 30) display else windowManager.defaultDisplay
        val current = display?.mode ?: return
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull(Display.Mode::getRefreshRate) ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }

    private fun showTuning() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(16))
        }
        slider(box, "Wave height", home.config.waveAmplitude, 20f, 120f) { home.config.waveAmplitude = it }
        slider(box, "Wave width", home.config.waveWidth, 80f, 320f) { home.config.waveWidth = it }
        slider(box, "Vertical spread", home.config.fingerVerticalSpread, 120f, 500f) { home.config.fingerVerticalSpread = it }
        slider(box, "Spring stiffness", home.config.springStiffness, 80f, 500f) { home.config.springStiffness = it }
        slider(box, "Drawer curvature", home.config.appDrawerCurvature, 0f, 1f) { home.config.appDrawerCurvature = it }
        AlertDialog.Builder(this)
            .setTitle("Fluid motion")
            .setView(box)
            .setNegativeButton("Reset") { _, _ ->
                val defaults = com.s24ultra.fluidlauncher.config.TransitionConfig()
                home.config.apply {
                    waveAmplitude = defaults.waveAmplitude; waveWidth = defaults.waveWidth
                    fingerVerticalSpread = defaults.fingerVerticalSpread
                    springStiffness = defaults.springStiffness
                    appDrawerCurvature = defaults.appDrawerCurvature
                }
                home.invalidate()
            }
            .setPositiveButton("Done", null)
            .show()
    }

    private fun slider(parent: LinearLayout, title: String, value: Float, min: Float, max: Float, change: (Float) -> Unit) {
        val label = TextView(this).apply {
            text = "$title · ${"%.1f".format(value)}"
            setTextColor(Color.DKGRAY)
            textSize = 15f
            setPadding(0, dp(12), 0, 0)
        }
        val seek = SeekBar(this).apply {
            progress = (((value - min) / (max - min)) * 1000).toInt()
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                    val newValue = min + (max - min) * progress / 1000f
                    label.text = "$title · ${"%.1f".format(newValue)}"
                    change(newValue); home.invalidate()
                }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            })
        }
        parent.addView(label, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        parent.addView(seek, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
