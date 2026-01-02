package com.tgwgroup.MiRearScreenGradienter;

import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.Keep;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Service running in Shizuku process with shell permissions.
 */
public class TaskService extends ITaskService.Stub {
    private static final String TAG = "TaskService";

    @Keep
    public TaskService() {
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public String executeShellCommandWithResult(String cmd) throws RemoteException {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()), 8192);
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
            process.waitFor();

            return output.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error executing command: " + cmd, e);
            return "Error: " + e.getMessage();
        }
    }

    private volatile boolean isWakeupLoopRunning = false;
    private Thread wakeupThread = null;

    @Override
    public void startWakeupLoop() {
        if (isWakeupLoopRunning)
            return;
        isWakeupLoopRunning = true;
        wakeupThread = new Thread(() -> {
            while (isWakeupLoopRunning) {
                try {
                    // Execute input keyevent directly
                    ProcessBuilder pb = new ProcessBuilder("sh", "-c", "input -d 1 keyevent KEYCODE_WAKEUP");
                    pb.start().waitFor();
                    Thread.sleep(100);
                } catch (Exception e) {
                    Log.e(TAG, "Wakeup loop error", e);
                }
            }
        });
        wakeupThread.start();
    }

    @Override
    public void stopWakeupLoop() {
        isWakeupLoopRunning = false;
        if (wakeupThread != null) {
            wakeupThread.interrupt();
            wakeupThread = null;
        }
    }
}
