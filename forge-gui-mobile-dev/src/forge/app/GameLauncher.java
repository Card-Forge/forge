package forge.app;

import com.badlogic.gdx.ApplicationListener;
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
        ApplicationListener start = Forge.getApp(hw, new Lwjgl3Clipboard(), new Main.DesktopAdapter(switchOrientationFile),
            assetsDir, false, false, totalRAM, false, 0);

        int windowWidth = Config.instance().getSettingData().width;
        int windowHeight = Config.instance().getSettingData().height;
        for(String arg : args) {
            if(arg.startsWith("width="))
                windowWidth = Integer.parseInt(arg.substring(6));
            if(arg.startsWith("height="))
                windowHeight = Integer.parseInt(arg.substring(7));
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
        });

        config.setHdpiMode(HdpiMode.Logical);

        new Lwjgl3Application(start, config);
    }
}
