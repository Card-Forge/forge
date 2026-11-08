package forge.app;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import forge.util.HWInfo;
import forge.util.InterfaceSize;
import forge.Forge;
import forge.adventure.util.Config;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.screens.match.arena.ArenaTheme;
import io.sentry.protocol.Device;
import io.sentry.protocol.OperatingSystem;
import org.lwjgl.system.Configuration;
import oshi.SystemInfo;

import java.nio.file.Files;
import java.nio.file.Paths;

public class GameLauncher {
    public GameLauncher(final String versionString, final String[] args) {
        String assetsDir = Files.exists(Paths.get("./res")) ? "./" : "../forge-gui/";

        // Place the file "switch_orientation.ini" to your assets folder to make the game switch to landscape orientation (unless desktopMode = true)
        String switchOrientationFile = assetsDir + "switch_orientation.ini";
        // This should fix MAC-OS startup without the need for -XstartOnFirstThread parameter
        Lwjgl3ApplicationConfiguration.useGlfwAsync();
        //increase MemoryStack to 1MB, default is 64kb
        Configuration.STACK_SIZE.set(1024);
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        HWInfo hw = null;
        try {
            SystemInfo si = new SystemInfo();
            // Device Info
            Device device = new Device();
            device.setId(si.getHardware().getComputerSystem().getHardwareUUID());
            device.setName(si.getHardware().getComputerSystem().getManufacturer() + " - " + si.getHardware().getComputerSystem().getModel());
            device.setModel(si.getHardware().getComputerSystem().getModel());
            device.setManufacturer(si.getHardware().getComputerSystem().getManufacturer());
            device.setMemorySize(si.getHardware().getMemory().getTotal());
            device.setChipset(si.getHardware().getComputerSystem().getBaseboard().getManufacturer() + " " + si.getHardware().getComputerSystem().getBaseboard().getModel());
            device.setCpuDescription(si.getHardware().getProcessor().getProcessorIdentifier().getName());
            // OS Info
            OperatingSystem os = new OperatingSystem();
            os.setName(si.getOperatingSystem().getFamily());
            os.setVersion(si.getOperatingSystem().getVersionInfo().getVersion());
            os.setBuild(si.getOperatingSystem().getVersionInfo().getBuildNumber());
            os.setRawDescription(si.getOperatingSystem() + " x" + si.getOperatingSystem().getBitness());
            hw = new HWInfo(device, os, false);
        } catch (Exception e) {
             e.printStackTrace();
        }

        // Retrieve command line parameters
        Integer widthArg = null;
        Integer heightArg = null;
        boolean portraitArg = false;
        boolean landscapeArg = false;
        for(String arg : args) {
            if(arg.startsWith("width=")) widthArg = Integer.parseInt(arg.substring(6));
            else if(arg.startsWith("height=")) heightArg = Integer.parseInt(arg.substring(7));
            else if(arg.equalsIgnoreCase("portrait")) portraitArg = true;
            else if(arg.equalsIgnoreCase("landscape")) landscapeArg = true;
        }

        boolean hasBothDims = widthArg != null && heightArg != null;
        // Only disable desktop auto-orientation when the user *really* overrides it
        boolean overrideOrientation = portraitArg || landscapeArg || hasBothDims;
        Forge.setDesktopAutoOrientation(!overrideOrientation);

        // Determine desired portrait/landscape only if we are overriding orientation.
        boolean isPortrait = false;
        if (portraitArg) isPortrait = true;
        else if (landscapeArg) isPortrait = false;
        else if (hasBothDims) isPortrait = heightArg > widthArg;

        ApplicationListener start = Forge.getApp(hw, new Lwjgl3Clipboard(), new Main.DesktopAdapter(switchOrientationFile),
            assetsDir, isPortrait, false, 0);

        // Initialize window size
        int windowWidth, windowHeight;

        if (widthArg != null || heightArg != null) {
            float aspect = getPrimaryScreenAspect();

            // If explicit portrait/landscape requested, coerce aspect direction
            if (portraitArg && aspect > 1f) aspect = 1f / aspect;
            if (landscapeArg && aspect < 1f) aspect = 1f / aspect;

            if (widthArg != null && heightArg == null) {
                windowWidth = widthArg;
                windowHeight = Math.max(1, Math.round(windowWidth / aspect));
            } else if (heightArg != null && widthArg == null) {
                windowHeight = heightArg;
                windowWidth = Math.max(1, Math.round(windowHeight * aspect));
            } else { // both provided
                windowWidth = widthArg;
                windowHeight = heightArg;
            }
        } else {
            windowWidth = Config.instance().getSettingData().width;
            windowHeight = Config.instance().getSettingData().height;
        }
        // If user explicitly overrode orientation, normalize by swapping
        if (overrideOrientation && isPortrait == windowHeight < windowWidth) {
            int tmp = windowHeight; windowHeight = windowWidth; windowWidth = tmp;
        }

        //a resizable window that remembers itself, opening maximized the first time; fullscreen is borderless on its monitor
        ForgePreferences launchPrefs = readPreferences();
        DesktopWindow.configure(config, launchPrefs, Config.instance().getSettingData().fullScreen,
                widthArg != null || heightArg != null ? windowWidth : 0, widthArg != null || heightArg != null ? windowHeight : 0,
                windowWidth, windowHeight);
        //how large text and controls are drawn: Utils.scale reads it when the window exists
        InterfaceSize.set(launchPrefs == null ? InterfaceSize.AUTO : launchPrefs.getPref(FPref.UI_INTERFACE_SIZE));
        config.setHdpiMode(HdpiMode.Logical);
        if (isArenaThemeOn()) {
            applyFrameRate(config); //with the Arena look off, keep libGDX's frame pacing exactly as before
        }
        //the setting can be switched while Forge runs: the pacing follows it straight away, both ways
        ArenaTheme.addSettingListener(GameLauncher::followArenaSetting);
        config.setTitle("Forge - " + versionString);
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public boolean closeRequested() {
                //use the device adpater to exit properly
                if (Forge.safeToClose)
                    Forge.exit(true);
                return false;
            }

            @Override
            public void created(Lwjgl3Window window) {
                DesktopWindow.attach(window);
            }

            @Override
            public void focusGained() {
                super.focusGained();
                Forge.setWindowFocus(true);
            }

            @Override
            public void focusLost() {
                super.focusLost();
                Forge.setWindowFocus(false);
            }
        });

        new Lwjgl3Application(start, config);
    }

    /**
     * Whether the Arena look is on ({@link FPref#UI_ARENA_THEME}), read from the preferences file before the model
     * is loaded. Switching it in the settings later changes the frame pacing at once ({@link #followArenaSetting}).
     */
    private static boolean isArenaThemeOn() {
        ForgePreferences prefs = readPreferences();
        return prefs != null && prefs.getPrefBoolean(FPref.UI_ARENA_THEME);
    }

    private static ForgePreferences launchPreferences;

    /** The preferences file as it is before the model loads (null if it cannot be read). */
    private static ForgePreferences readPreferences() {
        if (launchPreferences == null) {
            try {
                launchPreferences = new ForgePreferences();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return launchPreferences;
    }

    // libGDX's own pacing, which Forge used before the Arena look (the Lwjgl3ApplicationConfiguration defaults)
    private static final int PLAIN_FOREGROUND_FPS = 0, PLAIN_IDLE_FPS = 60;

    /**
     * The Arena look setting was read or changed (render thread): runs at the monitor's refresh rate while it is on and
     * goes back to libGDX's own pacing when it is off. The running application's configuration is changed in place;
     * its loop reads the foreground and idle rates every frame.
     */
    private static void followArenaSetting(boolean on) {
        try {
            if (!(Gdx.graphics instanceof Lwjgl3Graphics)) {
                return;
            }
            Lwjgl3Graphics graphics = (Lwjgl3Graphics) Gdx.graphics;
            int hz = on ? refreshRate() : 0;
            for (Lwjgl3ApplicationConfiguration live : liveConfigs(graphics)) {
                live.setForegroundFPS(on ? hz + 1 : PLAIN_FOREGROUND_FPS);
                live.setIdleFPS(on ? hz : PLAIN_IDLE_FPS);
            }
            graphics.setVSync(true); //on in both cases (libGDX's default); a driver ignoring it still has the cap
        } catch (Exception e) {
            e.printStackTrace(); //pacing only: never worth breaking the settings screen for
        }
    }

    /** The configuration objects the running application paces itself by: the window's and the application's own. */
    private static java.util.List<Lwjgl3ApplicationConfiguration> liveConfigs(Lwjgl3Graphics graphics) throws ReflectiveOperationException {
        java.util.List<Lwjgl3ApplicationConfiguration> out = new java.util.ArrayList<>(2);
        Lwjgl3Window window = graphics.getWindow();
        java.lang.reflect.Method getConfig = Lwjgl3Window.class.getDeclaredMethod("getConfig");
        getConfig.setAccessible(true);
        out.add((Lwjgl3ApplicationConfiguration) getConfig.invoke(window));
        if (Gdx.app instanceof Lwjgl3Application) {
            java.lang.reflect.Field config = Lwjgl3Application.class.getDeclaredField("config");
            config.setAccessible(true);
            Lwjgl3ApplicationConfiguration appConfig = (Lwjgl3ApplicationConfiguration) config.get(Gdx.app);
            if (appConfig != null && appConfig != out.get(0)) {
                out.add(appConfig);
            }
        }
        return out;
    }

    private static int refreshRate() {
        int hz = 60;
        try {
            DisplayMode dm = Lwjgl3ApplicationConfiguration.getDisplayMode();
            if (dm != null && dm.refreshRate > 0) {
                hz = dm.refreshRate;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return Math.max(30, Math.min(hz, 360));
    }

    /**
     * Runs at the monitor's refresh rate (120 fps on a 120 Hz screen) instead of libGDX's 60 fps assumptions: vsync
     * paces the frames, the foreground cap is the refresh rate (plus one, so the limiter never fights vsync, and so
     * it still holds where a driver ignores vsync), and the idle poll runs at the refresh rate too, so a frame asked
     * for by input (hover, drag) is not held back up to 1/60 s by libGDX's idle sleep while rendering on demand.
     */
    public static void applyFrameRate(Lwjgl3ApplicationConfiguration config) {
        int hz = refreshRate();
        config.useVsync(true);
        config.setForegroundFPS(hz + 1);
        config.setIdleFPS(hz);
        System.out.println("Display refresh rate " + hz + " Hz: rendering up to " + hz + " fps");
    }

    private static float getPrimaryScreenAspect() {
        DisplayMode dm = Lwjgl3ApplicationConfiguration.getDisplayMode();
        if (dm == null || dm.height == 0) return 16f / 9f; // sane fallback
        return (float) dm.width / (float) dm.height; // width/height
    }
}
