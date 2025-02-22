package forge.util;

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
    //private static boolean preventSleep;
    //private static java.util.concurrent.ScheduledFuture<?> delayedKeepAwakeTask;

    public static void preventSystemSleep(boolean preventSleep0) {
        //Disabling this for now. Mashing a key every 30 seconds has become a problematic solution for preventing sleep
        //on modern devices which sometimes do handle keys like "F15".
        //We could possibly implement the mouse-movement based solution here as an alternative: https://stackoverflow.com/a/53419439
        //It's possible though that we don't actually need it. Supposedly modern OSes can use network traffic as
        //an indication that the computer shouldn't put the CPU to sleep.
        /*
        if (preventSleep == preventSleep0) { return; }
        preventSleep = preventSleep0;

        if (delayedKeepAwakeTask != null) { //ensure current delayed task canceled if needed
            delayedKeepAwakeTask.cancel(false);
            delayedKeepAwakeTask = null;
        }

        if (preventSleep) { //ensure task scheduled from EDT thread
            FThreads.invokeInEdtNowOrLater(keepSystemAwake);
        }*/
    }

    /*
    private static Runnable keepSystemAwake = new Runnable() {
        @Override
        public void run() {
            if (!preventSleep) { return; } //ensure this flag is still set

            try {
                //use robot to simulate user action so system standby timer resets
                java.awt.Robot robot = new java.awt.Robot();
                if (isMac())
                    robot.keyPress(KeyEvent.VK_F4); // F15 increases Display Brightness by default. Switch to F4 (F1 is help in some programs, e.g. Discord)
                else
                    robot.keyPress(KeyEvent.VK_F15); //simulate F15 key press since that won't do anything noticeable

                delayedKeepAwakeTask = ThreadUtil.delay(30000, keepSystemAwake); //repeat every 30 seconds until flag cleared
            }
            catch (Exception e) {
                preventSleep = false;
                delayedKeepAwakeTask = null;
                e.printStackTrace();
            }
        }
    };
     */
}
