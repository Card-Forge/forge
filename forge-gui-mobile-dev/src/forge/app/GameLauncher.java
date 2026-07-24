package forge.app;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Graphics.Monitor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import forge.util.HWInfo;
import forge.Forge;
import forge.adventure.util.Config;
import io.sentry.protocol.Device;
import io.sentry.protocol.OperatingSystem;
import org.lwjgl.system.JNI;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.macosx.EnumerationMutationHandler;
import org.lwjgl.system.macosx.LibSystem;
import org.lwjgl.system.macosx.ObjCRuntime;
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

        boolean fullScreen = Config.instance().getSettingData().fullScreen;
        // The borderless window setup below is platform-neutral, but the mode is gated
        // to macOS for now because it has only been validated there.
        boolean borderlessFullScreen = forge.util.OperatingSystem.isMac()
                && Config.instance().getSettingData().borderlessFullScreen;
        if (borderlessFullScreen) {
            Monitor monitor = Lwjgl3ApplicationConfiguration.getPrimaryMonitor();
            DisplayMode displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode(monitor);
            config.setWindowedMode(displayMode.width, displayMode.height);
            config.setWindowPosition(monitor.virtualX, monitor.virtualY);
            config.setDecorated(false);
        } else if (fullScreen) {
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
            public void created(Lwjgl3Window window) {
                if (borderlessFullScreen) {
                    MacOSPresentation.setBorderlessVisible(true);
                }
            }

            @Override
            public void iconified(boolean isIconified) {
                if (borderlessFullScreen) {
                    MacOSPresentation.setBorderlessVisible(!isIconified);
                }
            }

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
                Forge.setWindowFocus(true);
            }

            @Override
            public void focusLost() {
                super.focusLost();
                Forge.setWindowFocus(false);
            }
        });

        config.setHdpiMode(HdpiMode.Logical);

        new Lwjgl3Application(start, config);
    }

    /**
     * Applies the AppKit presentation state (hidden menu bar and Dock, working Cmd-M
     * minimize) for the borderless fullscreen window, using LWJGL's bundled macOS
     * ObjC-runtime bindings so no new dependency is required.
     * <p>
     * GLFW exposes no API for NSApplication presentation options, and its own
     * borderless-fullscreen path keeps the window at a level above the menu bar,
     * which prevents normal use of other applications while the game is visible.
     * If that gap is ever closed upstream (GLFW -> LWJGL -> libGDX), this bridge
     * can be removed. Only invoked while borderless fullscreen is active on macOS.
     */
    private static final class MacOSPresentation {
        // NSApplicationPresentationOptions values from AppKit's NSApplication.h.
        private static final long DEFAULT = 0;
        private static final long HIDE_DOCK = 1L << 1;
        private static final long HIDE_MENU_BAR = 1L << 3;
        private static final long OBJC_MSG_SEND = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
        private static final long NS_APPLICATION = ObjCRuntime.objc_getClass("NSApplication");
        private static final long NS_MENU = ObjCRuntime.objc_getClass("NSMenu");
        private static final long NS_STRING = ObjCRuntime.objc_getClass("NSString");
        private static final long SHARED_APPLICATION = ObjCRuntime.sel_registerName("sharedApplication");
        private static final long SET_PRESENTATION_OPTIONS = ObjCRuntime.sel_registerName("setPresentationOptions:");
        private static final long MAIN_MENU = ObjCRuntime.sel_registerName("mainMenu");
        private static final long WINDOWS_MENU = ObjCRuntime.sel_registerName("windowsMenu");
        private static final long SET_WINDOWS_MENU = ObjCRuntime.sel_registerName("setWindowsMenu:");
        private static final long ALLOC = ObjCRuntime.sel_registerName("alloc");
        private static final long INIT_WITH_TITLE = ObjCRuntime.sel_registerName("initWithTitle:");
        private static final long ADD_ITEM = ObjCRuntime.sel_registerName("addItemWithTitle:action:keyEquivalent:");
        private static final long SET_SUBMENU = ObjCRuntime.sel_registerName("setSubmenu:");
        private static final long PERFORM_MINIATURIZE = ObjCRuntime.sel_registerName("performMiniaturize:");
        private static final long STRING_WITH_UTF8 = ObjCRuntime.sel_registerName("stringWithUTF8String:");
        private static final long DISPATCH_SYNC = LibSystem.getLibrary().getFunctionAddress("dispatch_sync_f");
        private static final long MAIN_QUEUE = LibSystem.getLibrary().getFunctionAddress("_dispatch_main_q");
        private static final long PTHREAD_MAIN = LibSystem.getLibrary().getFunctionAddress("pthread_main_np");
        // LWJGL ships no libdispatch callback type, so EnumerationMutationHandler is
        // borrowed here purely because it generates a native callback with the
        // void (*)(void *) signature that dispatch_sync_f requires; its ObjC
        // enumeration purpose is irrelevant.
        private static final EnumerationMutationHandler SET_PRESENTATION_OPTIONS_CALLBACK =
                EnumerationMutationHandler.create(MacOSPresentation::setPresentationOptions);

        private static void setBorderlessVisible(boolean visible) {
            long options = visible ? HIDE_DOCK | HIDE_MENU_BAR : DEFAULT;
            if (JNI.invokeI(PTHREAD_MAIN) != 0) {
                setPresentationOptions(options);
            } else {
                JNI.invokePPPV(MAIN_QUEUE, options, SET_PRESENTATION_OPTIONS_CALLBACK.address(), DISPATCH_SYNC);
            }
        }

        private static void setPresentationOptions(long options) {
            long application = JNI.invokePPJ(NS_APPLICATION, SHARED_APPLICATION, OBJC_MSG_SEND);
            if (options != DEFAULT) {
                ensureMinimizeMenu(application);
            }
            JNI.invokePPPV(application, SET_PRESENTATION_OPTIONS, options, OBJC_MSG_SEND);
        }

        private static void ensureMinimizeMenu(long application) {
            if (JNI.invokePPJ(application, WINDOWS_MENU, OBJC_MSG_SEND) != 0) {
                return;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                long empty = createString(stack, "");
                long windowTitle = createString(stack, "Window");
                long minimizeTitle = createString(stack, "Minimize");
                long minimizeKey = createString(stack, "m");
                long mainMenu = JNI.invokePPJ(application, MAIN_MENU, OBJC_MSG_SEND);
                long windowMenu = JNI.invokePPPP(JNI.invokePPJ(NS_MENU, ALLOC, OBJC_MSG_SEND), INIT_WITH_TITLE,
                        windowTitle, OBJC_MSG_SEND);
                long windowMenuItem = JNI.invokePPPPPP(mainMenu, ADD_ITEM, windowTitle, 0, empty, OBJC_MSG_SEND);
                JNI.invokePPPV(windowMenuItem, SET_SUBMENU, windowMenu, OBJC_MSG_SEND);
                JNI.invokePPPV(application, SET_WINDOWS_MENU, windowMenu, OBJC_MSG_SEND);
                JNI.invokePPPPPP(windowMenu, ADD_ITEM, minimizeTitle, PERFORM_MINIATURIZE, minimizeKey, OBJC_MSG_SEND);
            }
        }

        private static long createString(MemoryStack stack, String value) {
            return JNI.invokePPPP(NS_STRING, STRING_WITH_UTF8, MemoryUtil.memAddress(stack.UTF8(value)), OBJC_MSG_SEND);
        }
    }

    private static float getPrimaryScreenAspect() {
        DisplayMode dm = Lwjgl3ApplicationConfiguration.getDisplayMode();
        if (dm == null || dm.height == 0) return 16f / 9f; // sane fallback
        return (float) dm.width / (float) dm.height; // width/height
    }
}
