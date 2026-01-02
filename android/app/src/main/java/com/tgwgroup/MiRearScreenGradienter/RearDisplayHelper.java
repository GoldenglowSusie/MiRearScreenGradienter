package com.tgwgroup.MiRearScreenGradienter;

import android.graphics.Rect;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for rear display information.
 * Adapted from the reference project.
 */
public class RearDisplayHelper {
    private static final String TAG = "RearDisplayHelper";

    public interface CommandExecutor {
        String execute(String command);
    }

    /**
     * Rear Display Info Data Class
     */
    public static class RearDisplayInfo {
        public int width;           // Width (pixels)
        public int height;          // Height (pixels)
        public int densityDpi;      // DPI
        public Rect cutout;         // Cutout area (insets)

        public RearDisplayInfo() {
            // Default values (Xiaomi 14 Ultra rear screen)
            width = 1200;
            height = 2200;
            densityDpi = 440;
            cutout = new Rect(0, 0, 0, 0);
        }

        @Override
        public String toString() {
            return String.format("RearDisplayInfo{width=%d, height=%d, dpi=%d, cutout=%s}",
                width, height, densityDpi, cutout.toString());
        }

        public boolean hasCutout() {
            return cutout.left > 0 || cutout.top > 0 || cutout.right > 0 || cutout.bottom > 0;
        }
    }

    /**
     * Get rear display info using the provided executor.
     */
    public static RearDisplayInfo getRearDisplayInfo(CommandExecutor executor) {
        RearDisplayInfo info = new RearDisplayInfo();

        if (executor == null) {
            Log.w(TAG, "⚠️ Executor is null, using default info");
            return info;
        }

        try {
            // Execute dumpsys display
            String result = executor.execute("dumpsys display");
            if (result == null || result.isEmpty()) {
                Log.w(TAG, "⚠️ dumpsys display returned empty, using default info");
                return info;
            }

            // Parse
            parseRearDisplayInfo(result, info);

            Log.d(TAG, "✓ Rear Info: " + info.toString());

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to get rear display info", e);
        }

        return info;
    }

    private static void parseRearDisplayInfo(String dumpsys, RearDisplayInfo info) {
        try {
            // Method 1: Parse from mViewports
            Pattern viewportPattern = Pattern.compile(
                "displayId=1[^}]*deviceWidth=(\\d+),\\s*deviceHeight=(\\d+)"
            );
            Matcher viewportMatcher = viewportPattern.matcher(dumpsys);
            if (viewportMatcher.find()) {
                info.width = Integer.parseInt(viewportMatcher.group(1));
                info.height = Integer.parseInt(viewportMatcher.group(2));
            }

            // Method 2: Find Display 1 DisplayDeviceInfo block
            int display1DeviceStart = -1;

            Pattern uniqueIdPattern = Pattern.compile("displayId=1[^}]*uniqueId='([^']+)'");
            Matcher uniqueIdMatcher = uniqueIdPattern.matcher(dumpsys);
            String display1UniqueId = null;
            if (uniqueIdMatcher.find()) {
                display1UniqueId = uniqueIdMatcher.group(1);
            }

            int searchPos = 0;
            while (true) {
                int idx = dumpsys.indexOf("DisplayDeviceInfo", searchPos);
                if (idx == -1) break;

                int checkEnd = Math.min(idx + 2000, dumpsys.length());
                String snippet = dumpsys.substring(idx, checkEnd);

                boolean isDisplay1 = false;
                if (display1UniqueId != null && snippet.contains(display1UniqueId)) {
                    isDisplay1 = true;
                } else if (snippet.contains(info.width + " x " + info.height)) {
                    isDisplay1 = true;
                }

                if (isDisplay1) {
                    display1DeviceStart = idx;
                    break;
                }
                searchPos = idx + 17;
            }

            String display1Block = "";
            if (display1DeviceStart != -1) {
                int nextBlockIdx = dumpsys.indexOf("DisplayDeviceInfo", display1DeviceStart + 17);
                display1Block = nextBlockIdx > 0
                    ? dumpsys.substring(display1DeviceStart, nextBlockIdx)
                    : dumpsys.substring(display1DeviceStart, Math.min(display1DeviceStart + 3000, dumpsys.length()));
            } else {
                display1Block = "";
            }

            // Parse DPI
            if (!display1Block.isEmpty()) {
                Pattern dpiPattern = Pattern.compile("density\\s+(\\d+)");
                Matcher dpiMatcher = dpiPattern.matcher(display1Block);
                if (dpiMatcher.find()) {
                    info.densityDpi = Integer.parseInt(dpiMatcher.group(1));
                }
            }

            // Parse Cutout
            info.cutout = parseCutoutFromDumpsys(display1Block);

        } catch (Exception e) {
            Log.e(TAG, "❌ Parse exception", e);
        }
    }

    private static Rect parseCutoutFromDumpsys(String display1Block) {
        Rect cutout = new Rect(0, 0, 0, 0);

        try {
            // MIUI format: Rect(left, top - right, bottom)
            Pattern miuiPattern = Pattern.compile("DisplayCutout\\{insets=Rect\\((\\d+),\\s*(\\d+)\\s*-\\s*(\\d+),\\s*(\\d+)\\)");
            Matcher miuiMatcher = miuiPattern.matcher(display1Block);

            if (miuiMatcher.find()) {
                cutout.left = Integer.parseInt(miuiMatcher.group(1));
                cutout.top = Integer.parseInt(miuiMatcher.group(2));
                cutout.right = Integer.parseInt(miuiMatcher.group(3));
                cutout.bottom = Integer.parseInt(miuiMatcher.group(4));
                return cutout;
            }

            // Standard format
            Pattern standardPattern = Pattern.compile("DisplayCutout\\{insets=Rect\\((\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\)");
            Matcher standardMatcher = standardPattern.matcher(display1Block);

            if (standardMatcher.find()) {
                cutout.left = Integer.parseInt(standardMatcher.group(1));
                cutout.top = Integer.parseInt(standardMatcher.group(2));
                cutout.right = Integer.parseInt(standardMatcher.group(3));
                cutout.bottom = Integer.parseInt(standardMatcher.group(4));
                return cutout;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Parse cutout exception", e);
        }

        return cutout;
    }
}
