package forge.util;

import forge.FThreads;

public class OperatingSystem {
    private static String os = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return os.contains("win");
    }
 
    public static boolean isMac() {
        return os.contains("mac");
    }
 
    public static boolean isUnix() {
        return os.contains("nix") || os.contains("nux") || os.contains("aix");
    }
 
    public static boolean isSolaris() {
        return os.contains("sunos");
    }

    //utility for preventing system from sleeping
    private static boolean preventSleep;
    private static java.util.concurrent.ScheduledFuture<?> delayedKeepAwakeTask;

    public static void preventSystemSleep(boolean preventSleep0) {
        if (preventSleep == preventSleep0) { return; }
        preventSleep = preventSleep0;

        if (delayedKeepAwakeTask != null) { //ensure current delayed task canceled if needed
            delayedKeepAwakeTask.cancel(false);
            delayedKeepAwakeTask = null;
        }

        if (preventSleep) { //ensure task scheduled from EDT thread
            FThreads.invokeInEdtNowOrLater(keepSystemAwake);
        }
    }

    private static Runnable keepSystemAwake = new Runnable() {
        @Override
        public void run() {
            if (!preventSleep) { return; } //ensure this flag is still set

            try {
                //use robot to simulate user action so system standby timer resets
                java.awt.Robot robot = new java.awt.Robot();
                robot.keyPress(0xF002); //simulate F15 key press since that won't do anything noticable

                delayedKeepAwakeTask = ThreadUtil.delay(30000, keepSystemAwake); //repeat every 30 seconds until flag cleared
            }
            catch (Exception e) {
                preventSleep = false;
                delayedKeepAwakeTask = null;
                e.printStackTrace();
            }
        }
    };
}
