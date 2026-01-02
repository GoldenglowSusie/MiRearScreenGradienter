package com.tgwgroup.MiRearScreenGradienter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import io.flutter.embedding.android.FlutterActivity

class RearActivity : FlutterActivity() {
    companion object {
        @Volatile
        private var currentInstance: RearActivity? = null
        
        fun finishIfExists() {
            currentInstance?.let {
                android.util.Log.d("RearActivity", "🔴 finishIfExists() called, finishing activity")
                it.finish()
            } ?: android.util.Log.w("RearActivity", "⚠️ finishIfExists() called but no instance exists")
        }
    }
    
    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.d("RearActivity", "🔴 Received CLOSE_REAR broadcast, calling finish()")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set current instance
        currentInstance = this
        
        // Only keep screen on - removed FLAG_SHOW_WHEN_LOCKED and FLAG_TURN_SCREEN_ON
        // to allow the activity to finish normally
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Register broadcast receiver to close this activity
        val filter = IntentFilter("com.tgwgroup.MiRearScreenGradienter.CLOSE_REAR")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }
    }

    override fun configureFlutterEngine(flutterEngine: io.flutter.embedding.engine.FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // Add MethodChannel for rear display info
        io.flutter.plugin.common.MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "com.tgwgroup.MiRearScreenGradienter/control"
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "getRearDisplayInfo" -> {
                    val info = getRearDisplayInfoDirect()
                    result.success(info)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun getRearDisplayInfoDirect(): Map<String, Any> {
        android.util.Log.d("RearActivity", "getRearDisplayInfoDirect called")
        
        // Get TaskService from KeepAliveService
        val taskService = KeepAliveService.getTaskService()
        if (taskService == null) {
            android.util.Log.e("RearActivity", "TaskService is null")
            return mapOf(
                "width" to 0,
                "height" to 0,
                "dpi" to 0,
                "cutoutLeft" to 0,
                "cutoutTop" to 0,
                "cutoutRight" to 0,
                "cutoutBottom" to 0
            )
        }

        val executor = com.tgwgroup.MiRearScreenGradienter.RearDisplayHelper.CommandExecutor { cmd ->
            try {
                val result = taskService.executeShellCommandWithResult(cmd)
                android.util.Log.d("RearActivity", "Executed: $cmd, result length: ${result.length}")
                result
            } catch (e: Exception) {
                android.util.Log.e("RearActivity", "Command failed: $cmd", e)
                ""
            }
        }
        
        val info = com.tgwgroup.MiRearScreenGradienter.RearDisplayHelper.getRearDisplayInfo(executor)
        android.util.Log.d("RearActivity", "Display Info: ${info.width}x${info.height}, dpi=${info.densityDpi}")
        android.util.Log.d("RearActivity", "Cutout: left=${info.cutout.left}, top=${info.cutout.top}, right=${info.cutout.right}, bottom=${info.cutout.bottom}")
        
        return mapOf(
            "width" to info.width,
            "height" to info.height,
            "dpi" to info.densityDpi,
            "cutoutLeft" to info.cutout.left,
            "cutoutTop" to info.cutout.top,
            "cutoutRight" to info.cutout.right,
            "cutoutBottom" to info.cutout.bottom
        )
    }

    override fun onResume() {
        super.onResume()
        // Re-apply flag in onResume
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("RearActivity", "🔴 onDestroy() called")
        try {
            unregisterReceiver(closeReceiver)
        } catch (e: Exception) {
            // Ignore if already unregistered
        }
        // Clear instance
        if (currentInstance == this) {
            currentInstance = null
        }
    }

    override fun getInitialRoute(): String {
        return "/rear"
    }
}
