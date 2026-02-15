package forge.app;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import forge.util.HWInfo;
import forge.Forge;
import forge.adventure.util.Config;
import io.sentry.protocol.Device;
import io.sentry.protocol.OperatingSystem;
import forge.sound.SoundSystem;
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
        if (SharedLibraryLoader.isMac) {
            Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
        }
        //increase MemoryStack to 1MB, default is 64kb
        Configuration.STACK_SIZE.set(1024);
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        HWInfo hw = null;
        int totalRAM = 0;
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
            totalRAM = Math.round(si.getHardware().getMemory().getTotal() / 1024f / 1024f);
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

        boolean hasAnyDimArg = (widthArg != null || heightArg != null);
        boolean hasBothDims = (widthArg != null && heightArg != null);

        boolean manualWindowSize = hasAnyDimArg;
        
        // Only disable desktop auto-orientation when the user *really* overrides it
        boolean overrideOrientation = portraitArg || landscapeArg || hasBothDims;
        Forge.setDesktopAutoOrientation(!overrideOrientation);

        // Determine desired portrait/landscape only if we are overriding orientation.
        boolean isPortrait = false;
        if (portraitArg) isPortrait = true;
        else if (landscapeArg) isPortrait = false;
        else if (hasBothDims) isPortrait = (heightArg > widthArg);

        // Initialize window size
        int windowWidth = 0, windowHeight = 0;

        if (manualWindowSize) {
            float aspect = getPrimaryScreenAspect(); // width/height

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

            // If user explicitly overrode orientation (portrait/landscape or both dims), normalize by swapping
            if (overrideOrientation) {
                if (isPortrait && windowHeight < windowWidth) {
                    int tmp = windowHeight; windowHeight = windowWidth; windowWidth = tmp;
                } else if (!isPortrait && windowWidth < windowHeight) {
                    int tmp = windowHeight; windowHeight = windowWidth; windowWidth = tmp;
                }
            }
        }

        ApplicationListener start = Forge.getApp(hw, new Lwjgl3Clipboard(), new Main.DesktopAdapter(switchOrientationFile),
            assetsDir, false, isPortrait, totalRAM, false, 0);

        // If no manual window size is supplied, use the configured one (and adjust for portrait mode if needed)
        if (!manualWindowSize) {
            windowWidth = Config.instance().getSettingData().width;
            windowHeight = Config.instance().getSettingData().height;
            if (isPortrait && (windowHeight < windowWidth)) {
                // swap width/height
                int tmp = windowHeight;
                windowHeight = windowWidth;
                windowWidth = tmp;
            } else if (!isPortrait && (windowWidth < windowHeight)) {
                // swap width/height
                int tmp = windowHeight;
                windowHeight = windowWidth;
                windowWidth = tmp;
            }
        }

        if (Config.instance().getSettingData().fullScreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
            config.setAutoIconify(true);
            config.setHdpiMode(HdpiMode.Logical);
        } else {
            config.setWindowedMode(windowWidth, windowHeight);
            config.setResizable(false);
        }
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
            public void focusGained() {
                super.focusGained();
                SoundSystem.instance.setWindowFocus(true);
            }

            @Override
            public void focusLost() {
                super.focusLost();
                SoundSystem.instance.setWindowFocus(false);
            }
        });

        config.setHdpiMode(HdpiMode.Logical);

        new Lwjgl3Application(start, config);
    }

    private static float getPrimaryScreenAspect() {
        DisplayMode dm = Lwjgl3ApplicationConfiguration.getDisplayMode();
        if (dm == null || dm.height == 0) return 16f / 9f; // sane fallback
        return (float) dm.width / (float) dm.height; // width/height
    }
}
