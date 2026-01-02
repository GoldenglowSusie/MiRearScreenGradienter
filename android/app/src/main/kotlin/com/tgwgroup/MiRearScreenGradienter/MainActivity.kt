package com.tgwgroup.MiRearScreenGradienter

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import rikka.shizuku.Shizuku

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.tgwgroup.MiRearScreenGradienter/control"
    private val TAG = "MainActivity"
    
    private var taskService: ITaskService? = null
    
    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName("com.tgwgroup.MiRearScreenGradienter", TaskService::class.java.name)
    )
    .daemon(false)
    .processNameSuffix("task_service")
    .debuggable(false)
    .version(1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "TaskService connected")
            taskService = ITaskService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "TaskService disconnected")
            taskService = null
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindTaskService()
        } else {
            Shizuku.requestPermission(0)
        }
    }
    
    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            bindTaskService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        checkAndRequestShizukuPermission()
    }

    private fun checkAndRequestShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(0)
                } else {
                    bindTaskService()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check Shizuku permission", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        unbindTaskService()
    }

    private fun bindTaskService() {
        if (taskService != null) return
        try {
            Shizuku.bindUserService(serviceArgs, serviceConnection)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind TaskService", e)
        }
    }

    private fun unbindTaskService() {
        if (taskService != null) {
            Shizuku.unbindUserService(serviceArgs, serviceConnection, true)
            taskService = null
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "startLevel" -> {
                    startLevel()
                    result.success(null)
                }
                "stopLevel" -> {
                    stopLevel()
                    result.success(null)
                }
                "getRearDisplayInfo" -> {
                    val info = getRearDisplayInfo()
                    result.success(info)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun startLevel() {
        Thread {
            try {
                if (taskService == null) {
                    Log.d(TAG, "TaskService not connected, attempting to bind...")
                    bindTaskService()
                    
                    // Wait for connection (up to 2 seconds)
                    var retries = 0
                    while (taskService == null && retries < 20) {
                        Thread.sleep(100)
                        retries++
                    }
                    
                    if (taskService == null) {
                        Log.e(TAG, "Failed to bind TaskService after waiting")
                        return@Thread
                    }
                }
                
                // 1. Kill subscreencenter
                Log.d(TAG, "Killing subscreencenter...")
                taskService?.executeShellCommandWithResult("am force-stop com.xiaomi.subscreencenter")

                // 2. Wake up rear screen (Display 1)
                Log.d(TAG, "Waking up rear screen...")
                taskService?.executeShellCommandWithResult("input -d 1 keyevent KEYCODE_WAKEUP")
                Thread.sleep(200)

                // Start KeepAliveService
                val serviceIntent = android.content.Intent(this, KeepAliveService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }

                // 3. Launch RearActivity on Display 1
                Log.d(TAG, "Launching RearActivity...")
                val cmd = "am start --display 1 -n com.tgwgroup.MiRearScreenGradienter/.RearActivity"
                taskService?.executeShellCommandWithResult(cmd)

            } catch (e: Exception) {
                Log.e(TAG, "Error starting level", e)
            }
        }.start()
    }

    private fun stopLevel() {
        Thread {
            try {
                Log.d(TAG, "🔴 Stopping level...")
                
                // Directly call RearActivity's finish method
                RearActivity.finishIfExists()
                
                // Explicitly stop wakeup loop and DESTROY the service process
                try {
                    taskService?.stopWakeupLoop()
                    taskService?.destroy() // Force kill the process
                    Log.d(TAG, "✓ Wakeup loop stopped and TaskService destroyed from MainActivity")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to destroy TaskService from MainActivity", e)
                }
                
                // Wait a moment for activity to close
                Thread.sleep(300)
                
                // NOW stop KeepAliveService
                val serviceIntent = android.content.Intent(this, KeepAliveService::class.java)
                stopService(serviceIntent)
                
                // Send broadcast as final backup
                val intent = android.content.Intent("com.tgwgroup.MiRearScreenGradienter.CLOSE_REAR")
                sendBroadcast(intent)
                
                Log.d(TAG, "✓ Stop sequence completed")
                

                
            } catch (e: Exception) {
                Log.e(TAG, "Error in stopLevel", e)
            }
        }.start()
    }

    private fun getRearDisplayInfo(): Map<String, Any> {
        Log.d(TAG, "getRearDisplayInfo called, taskService = $taskService")
        val executor = RearDisplayHelper.CommandExecutor { cmd -> 
            try {
                val result = taskService?.executeShellCommandWithResult(cmd) ?: ""
                Log.d(TAG, "Executed: $cmd, result length: ${result.length}")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Remote execution failed for cmd: $cmd", e)
                ""
            }
        }
        val info = RearDisplayHelper.getRearDisplayInfo(executor)
        
        Log.d(TAG, "RearDisplayInfo: width=${info.width}, height=${info.height}, dpi=${info.densityDpi}")
        Log.d(TAG, "Cutout: left=${info.cutout.left}, top=${info.cutout.top}, right=${info.cutout.right}, bottom=${info.cutout.bottom}")
        
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
}
