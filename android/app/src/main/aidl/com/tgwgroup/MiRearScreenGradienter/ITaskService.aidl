package com.tgwgroup.MiRearScreenGradienter;

interface ITaskService {
    void destroy() = 16777114; // Shizuku required
    
    /**
     * Execute shell command
     */
    String executeShellCommandWithResult(String cmd) = 1;

    /**
     * Start sending KEYCODE_WAKEUP to display 1 every 100ms
     */
    void startWakeupLoop() = 2;

    /**
     * Stop the wakeup loop
     */
    void stopWakeupLoop() = 3;
}
