package com.tgwgroup.MiRearScreenGradienter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import rikka.shizuku.Shizuku

class KeepAliveService : Service() {
    companion object {
        private const val TAG = "KeepAliveService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "KeepAliveChannel"
        
        @Volatile
        private var serviceTaskService: ITaskService? = null
        
        fun getTaskService(): ITaskService? = serviceTaskService
    }
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var taskService: ITaskService? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Reconnection interval
    private val RECONNECT_INTERVAL_MS = 1000L
    
    // Kill subscreencenter every 500ms
    private val KILL_INTERVAL_MS = 500L

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName("com.tgwgroup.MiRearScreenGradienter", TaskService::class.java.name)
    )
    .daemon(false)
    .processNameSuffix("task_service")
    .debuggable(false)
    .version(1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "√ TaskService connected")
            taskService = ITaskService.Stub.asInterface(binder)
            serviceTaskService = taskService
            
            // Cancel any pending reconnect attempts
            handler.removeCallbacks(reconnectRunnable)
            
            try {
                // Start wakeup loop
                taskService?.startWakeupLoop()
                Log.d(TAG, "√ Wakeup loop started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start wakeup loop", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "⚠ TaskService disconnected - will attempt to reconnect")
            taskService = null
            serviceTaskService = null
            
            // Schedule reconnection
            handler.postDelayed(reconnectRunnable, RECONNECT_INTERVAL_MS)
        }
    }
    
    // Reconnection runnable
    private val reconnectRunnable = object : Runnable {
        override fun run() {
            if (taskService == null) {
                Log.d(TAG, "🔄 Attempting to reconnect TaskService...")
                bindTaskService()
                // Keep trying every second until connected
                handler.postDelayed(this, RECONNECT_INTERVAL_MS)
            }
        }
    }
    
    // Kill subscreencenter runnable
    private val killSubscreenRunnable = object : Runnable {
        override fun run() {
            try {
                if (taskService != null) {
                    taskService?.executeShellCommandWithResult("am force-stop com.xiaomi.subscreencenter")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to kill subscreencenter: ${e.message}")
            }
            // Schedule next kill
            handler.postDelayed(this, KILL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Acquire WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "MiRearScreenGradienter::KeepAlive"
        )
        wakeLock?.acquire()
        
        // Bind TaskService
        bindTaskService()
        
        // Start continuous subscreencenter killing
        handler.post(killSubscreenRunnable)
    }

    private fun bindTaskService() {
        try {
            if (taskService != null) {
                Log.d(TAG, "TaskService already bound")
                return
            }
            
            if (!Shizuku.pingBinder()) {
                Log.w(TAG, "Shizuku not available")
                return
            }
            
            if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Shizuku.bindUserService(serviceArgs, serviceConnection)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind TaskService", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Stop all handlers
        handler.removeCallbacks(reconnectRunnable)
        handler.removeCallbacks(killSubscreenRunnable)
        
        // Stop wakeup loop and destroy service
        try {
            taskService?.stopWakeupLoop()
            taskService?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy TaskService", e)
        }
        
        // Unbind TaskService
        try {
            Shizuku.unbindUserService(serviceArgs, serviceConnection, true)
        } catch (e: Exception) {
            // Ignore
        }
        
        // Clear static reference
        serviceTaskService = null
        
        // Release WakeLock
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Keep Alive Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mi Rear Screen Gradienter")
            .setContentText("Running in background to keep rear screen active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
