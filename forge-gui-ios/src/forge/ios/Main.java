package forge.ios;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.apache.commons.lang3.tuple.Pair;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSTimeZone;
import org.robovm.apple.foundation.NSBundle;
import org.robovm.apple.foundation.NSProcessInfo;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIPasteboard;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.backends.iosrobovm.IOSFiles;

import forge.Forge;
import forge.assets.ImageCache;
import forge.interfaces.IDeviceAdapter;

public class Main extends IOSApplication.Delegate {
    private static int initCounter = 0;
    private static final Object initLock = new Object();

    // iOS 26+ requires os_log with public specifier - NSLog is fully redacted
    // Older iOS: use NSLog (Foundation.log) which idevicesyslog captures reliably
    private static void log(String message) {
        try {
            if (OSLogPrintStream.shouldUseOSLog()) {
                ForgeOSLog.log(message);
            } else {
                org.robovm.apple.foundation.Foundation.log("%@",
                        new org.robovm.apple.foundation.NSString("FORGE: " + message));
            }
        } catch (Throwable t) {
            // Last resort - may not be visible on older iOS
            try {
                org.robovm.apple.foundation.Foundation.log("%@",
                        new org.robovm.apple.foundation.NSString("FORGE: " + message));
            } catch (Throwable t2) {
                // Nothing we can do
            }
        }
    }

    // Static initializer runs when class is loaded, before main()
    static {
        synchronized (initLock) {
            initCounter++;
            log("Static initializer starting (count=" + initCounter + ")");
        }
        try {
            // Create a custom timezone without file system access
            NSTimeZone systemTimeZone = NSTimeZone.getSystemTimeZone();
            if (systemTimeZone != null) {
                String tzName = systemTimeZone.getName();
                long offsetSeconds = systemTimeZone.getSecondsFromGMT();
                int offsetMillis = (int) (offsetSeconds * 1000);

                // Set both the property AND the default timezone programmatically
                System.setProperty("user.timezone", tzName != null ? tzName : "UTC");
                TimeZone.setDefault(new SimpleTimeZone(offsetMillis, tzName != null ? tzName : "UTC"));
                log("Timezone set to " + tzName);
            } else {
                // Fallback if systemTimeZone is null
                System.setProperty("user.timezone", "America/Los_Angeles");
                TimeZone.setDefault(new SimpleTimeZone(-8 * 3600 * 1000, "America/Los_Angeles"));
                log("Timezone set to fallback PST");
            }
        } catch (Throwable e) {
            log("Exception in static initializer: " + e.getMessage());
            e.printStackTrace();
            // Catch everything including Errors to prevent static initializer failure
            try {
                System.setProperty("user.timezone", "America/Los_Angeles");
                TimeZone.setDefault(new SimpleTimeZone(-8 * 3600 * 1000, "America/Los_Angeles"));
            } catch (Throwable ignored) {
                // If even the fallback fails, continue anyway
            }
            log("Static initializer completed (count=" + initCounter + ")");
        }
    }

    private static int createAppCounter = 0;

    private void copyEssentialResources(final String assetsDir) {
        log("copyEssentialResources() starting");
        try {
            String bundlePath = NSBundle.getMainBundle().getBundlePath();
            File bundleResDir = new File(bundlePath, "res");

            log("Bundle res dir: " + bundleResDir.getAbsolutePath());

            if (bundleResDir.exists() && bundleResDir.isDirectory()) {
                // Copy essential small resource directories to avoid watchdog timeout

                // Copy languages directory (9 small files)
                File bundleLangDir = new File(bundleResDir, "languages");
                File docsLangDir = new File(assetsDir + "/res", "languages");

                if (bundleLangDir.exists() && !docsLangDir.exists()) {
                    log("Copying languages directory...");
                    copyDirectory(bundleLangDir, docsLangDir);
                    log("Languages copied successfully");
                } else if (docsLangDir.exists()) {
                    log("Languages directory already exists");
                } else {
                    log("Languages directory not found in bundle");
                }

                // Copy defaults directory (placeholder images like no_card.jpg)
                File bundleDefaultsDir = new File(bundleResDir, "defaults");
                File docsDefaultsDir = new File(assetsDir + "/res", "defaults");

                if (bundleDefaultsDir.exists() && !docsDefaultsDir.exists()) {
                    log("Copying defaults directory...");
                    copyDirectory(bundleDefaultsDir, docsDefaultsDir);
                    log("Defaults copied successfully");
                } else if (docsDefaultsDir.exists()) {
                    log("Defaults directory already exists");
                } else {
                    log("Defaults directory not found in bundle");
                }
            } else {
                log("No bundled resources found at: " + bundleResDir.getAbsolutePath());
            }
        } catch (Exception e) {
            log("Error copying essential resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void copyFile(final File source, final File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }

    private void copyDirectory(final File source, final File dest) throws IOException {
        if (!dest.exists()) {
            dest.mkdirs();
        }

        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(dest, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, destFile);
                } else {
                    // Only copy if destination doesn't exist (don't overwrite user changes)
                    if (!destFile.exists()) {
                        copyFile(file, destFile);
                        log("Copied " + file.getName());
                    }
                }
            }
        }
    }

    @Override
    protected IOSApplication createApplication() {
        synchronized (initLock) {
            createAppCounter++;
            log("createApplication() starting (count=" + createAppCounter + ")");
        }
        try {
            // Use the app bundle as assetsDir so resources are read directly from there
            // This avoids copying files and hitting watchdog timeout
            String bundlePath = NSBundle.getMainBundle().getBundlePath();
            // Ensure path ends with / so relative paths are appended correctly
            final String assetsDir = bundlePath.endsWith("/") ? bundlePath : bundlePath + "/";
            log("Assets dir (bundle): " + assetsDir);

            // Set writable directories for iOS using libGDX IOSFiles (Documents directory)
            // This avoids iOS sandbox violations when trying to write to the read-only app bundle
            String documentsPath = new IOSFiles().getExternalStoragePath();
            log("Documents dir (writable): " + documentsPath);
            System.setProperty("forge.ios.userDir", documentsPath);
            System.setProperty("forge.ios.cacheDir", documentsPath + "cache/");

            // Enable the static/replacement-ability memo on iOS. It's data-correct (proven 0-stale) and
            // cuts allocations + speeds up AI evaluation — helps the constrained iPad CPU (fewer combat
            // timeouts) and the jetsam memory ceiling. Set here, before any game/CardState class loads.
            System.setProperty("forge.staticMemo", "on");

            // Clear card cache when a new build is deployed. The cache stores
            // pre-parsed card rules for fast startup, but stale caches cause bugs
            // (e.g., duplicate triggers). Compare the app's CFBundleVersion to a
            // stored marker — if they differ, this is a new deploy.
            String appBuild = NSBundle.getMainBundle().getInfoDictionaryObject("CFBundleVersion").toString();
            File cacheDir = new File(documentsPath + "cache/db/");
            cacheDir.mkdirs();
            File buildMarker = new File(cacheDir, ".build_version");
            String cachedBuild = "";
            if (buildMarker.exists()) {
                try {
                    byte[] bytes = new byte[(int) buildMarker.length()];
                    FileInputStream fis = new FileInputStream(buildMarker);
                    fis.read(bytes);
                    fis.close();
                    cachedBuild = new String(bytes).trim();
                } catch (IOException e) { /* treat as mismatch */ }
            }
            if (!appBuild.equals(cachedBuild)) {
                File cardCache = new File(cacheDir, "cardcache.bin");
                File cardsDb = new File(cacheDir, "cardsdb.bin");
                if (cardCache.exists()) { cardCache.delete(); log("Cleared stale cardcache.bin (build " + cachedBuild + " -> " + appBuild + ")"); }
                if (cardsDb.exists()) { cardsDb.delete(); log("Cleared stale cardsdb.bin"); }
                try {
                    FileOutputStream fos = new FileOutputStream(buildMarker);
                    fos.write(appBuild.getBytes());
                    fos.close();
                } catch (IOException e) {
                    log("Could not write build marker: " + e.getMessage());
                }
            } else {
                log("Card cache up-to-date for build " + appBuild);
            }

            final IOSApplicationConfiguration config = new IOSApplicationConfiguration();
            config.useAccelerometer = false;
            config.useCompass = false;
            config.useAudio = true;  // ObjectAL/OpenAL audio (music + SFX); see IOSAdapter.isSupportedAudioFormat
            // Play game audio even when the device ringer/silent switch is on
            // (ObjectAL honorSilentSwitch = !overrideRingerSwitch). Without this,
            // there is NO sound on a device in silent mode — the simulator ignores
            // the silent switch, which masked it. Standard behavior for a game.
            config.overrideRingerSwitch = true;
            config.preferredFramesPerSecond = 60;  // Smooth 60 FPS rendering
            config.preventScreenDimming = true;  // Keep screen on during gameplay

            // Detect if running on iPad (UIUserInterfaceIdiomPad = 1)
            boolean isTablet = org.robovm.apple.uikit.UIDevice.getCurrentDevice().getUserInterfaceIdiom()
                == org.robovm.apple.uikit.UIUserInterfaceIdiom.Pad;

            // Mark this as the iOS port (mirrors the Android launcher's setIsAndroid) so shared
            // modules branch on GuiBase.isIOS() instead of sniffing libGDX's ApplicationType.
            forge.gui.GuiBase.setIsIOS(true);

            // Log physical device RAM (MB) for diagnostics. Upstream's getApp no
            // longer takes a RAM hint (the feature branch's cache-sizing lever);
            // if jetsam pressure resurfaces, reintroduce via HWInfo.
            try {
                long deviceRamMB = NSProcessInfo.getSharedProcessInfo().getPhysicalMemory() / (1024L * 1024L);
                log("Physical device RAM: " + deviceRamMB + " MB");
            } catch (Throwable t) {
                log("Could not read physical memory: " + t.getMessage());
            }

            final ApplicationListener app = Forge.getApp(null, new IOSClipboard(), new IOSAdapter(), assetsDir, false, isTablet, 0);
            IOSApplication iosApp = new IOSApplication(app, config);

            // NOTE: sound effects now play through libGDX Music (AVAudioPlayer) on iOS
            // (see forge.sound.AudioClip), NOT OpenAL. The OpenAL effects engine that
            // libGDX auto-initializes is therefore unused; it is permanently suspended
            // in didBecomeActive so its render cycle can't beat against the AVAudioPlayer
            // music. The old world-gen sfxEngineControl toggle is intentionally NOT
            // registered anymore - un-suspending OpenAL would reintroduce the beat, and
            // AVAudioPlayer effects don't suffer the world-gen mixer-starvation static.

            // Re-apply System.out/err redirection - IOSApplication replaces them with FoundationLogPrintStream
            // which doesn't appear in Console.app on iOS 26+
            try {
                OSLogPrintStream outStream = new OSLogPrintStream("", false);
                OSLogPrintStream errStream = new OSLogPrintStream("[ERR] ", true);
                System.setOut(outStream);
                System.setErr(errStream);
            } catch (Throwable t) {
                log("Failed to re-apply System.out/err redirection: " + t.getMessage());
            }
            return iosApp;
        } catch (Throwable e) {
            log("Exception in createApplication(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void didReceiveMemoryWarning(UIApplication application) {
        // iOS memory-pressure relief valve. Forge previously ignored every memory warning and never reclaimed
        // card textures mid-game, so a long game ratcheted native texture RSS toward the jetsam kill with no
        // relief. Drop the whole texture cache (on-screen cards reload via needsUpdate) and force a GC — on the
        // GL render thread, since textures are GL objects. Turns a hard jetsam kill into a recoverable flush.
        log("didReceiveMemoryWarning -> flushing texture caches");
        try {
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        ImageCache.getInstance().disposeTextures();
                        System.gc();
                    } catch (Throwable t) {
                        log("memory-warning flush failed: " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            log("could not post memory-warning flush: " + t.getMessage());
        }
    }

    @Override
    public void didBecomeActive(UIApplication application) {
        // super MUST run first so ObjectAL's audio-session interruption recovery works.
        super.didBecomeActive(application);
        // Permanently suspend the OpenAL effects engine. Sound effects now play through
        // libGDX Music (AVAudioPlayer) on iOS (see forge.sound.AudioClip), so OpenAL is
        // unused - but libGDX auto-initializes it, and an ACTIVE OpenAL 3D-mixer render
        // cycle beats against the AVAudioPlayer music/effects (mediaserverd) clock,
        // producing the slow periodic music crackle. Suspending it (alcSuspendContext)
        // stops that render cycle so everything is one clock domain = no beat. Re-applied
        // on every foreground; the first didBecomeActive fires at launch after the audio
        // device is open. Music (a separate AVAudioPlayer subsystem) is unaffected -
        // verified previously: suspending OpenAL never stopped the background music.
        try {
            OpenALManager mgr = OpenALManager.sharedInstance();
            String diag;
            if (mgr != null) {
                mgr.setManuallySuspended(true);
                diag = "OpenAL effects engine suspended (manuallySuspended="
                        + mgr.isManuallySuspended() + "); effects route via AVAudioPlayer";
            } else {
                diag = "OpenAL suspend SKIPPED (sharedInstance null)";
            }
            log(diag);
        } catch (Throwable t) {
            log("OpenAL suspend failed: " + t.getMessage());
        }
    }

    public static void main(String[] args) {
        // Redirect System.out and System.err to os_log FIRST
        // This ensures all logging (900+ calls) is visible in Console.app
        // Note: IOSApplication will replace these, so we re-apply in createApplication()
        try {
            OSLogPrintStream outStream = new OSLogPrintStream("", false);
            OSLogPrintStream errStream = new OSLogPrintStream("[ERR] ", true);
            System.setOut(outStream);
            System.setErr(errStream);
        } catch (Throwable t) {
            log("Failed to redirect System.out/err: " + t.getMessage());
        }

        // Raise RLIMIT_NOFILE before anything opens files/sockets. iOS processes default to a
        // 256-fd soft limit; this app legitimately holds ~80 (card DB, logs, textures in flight)
        // and image-download bursts open dozens of sockets on top — one spike past the limit and
        // EVERYTHING degrades at once: texture opens fail (EMFILE surfaces as GdxRuntimeException
        // "Couldn't load file" -> blank card frames), and even DNS breaks (getaddrinfo needs an
        // fd -> "Unable to resolve host"). Raising soft to min(hard, OPEN_MAX) is cheap and safe.
        FileDescriptorLimit.raise();

        // Crash reporting — mirrors the desktop launcher's Sentry setup (same project DSN).
        // Events are only SENT when the user's USE_SENTRY preference allows it
        // (BugReporter.isSentryEnabled); the init itself is passive. Never let telemetry
        // setup break app launch.
        try {
            io.sentry.Sentry.init(options -> {
                // Do NOT scan the classpath for config (desktop leaves this on; on a JVM it's
                // harmless). On iOS both the sentry.properties probe (external configuration)
                // and the sentry-debug-meta.properties probe open EVERY bundled jar via the
                // ClassLoader and the JarFile cache pins them open — 87 jars x 2 fds ate ~177
                // of the 256-fd soft limit and starved the whole app (blank textures, failed
                // DNS). There is no external config on iOS anyway: the DSN is set right here.
                options.setEnableExternalConfiguration(false);
                options.setDebugMetaLoader(io.sentry.internal.debugmeta.NoOpDebugMetaLoader.getInstance());
                options.setRelease(forge.util.BuildInfo.getVersionString());
                options.setEnvironment("iOS");
                options.setTag("Platform", "iOS/RoboVM");
                options.setShutdownTimeoutMillis(5000);
                // The MOBILE project DSN (project 2, like Android's manifest — desktop reports
                // to project 3), in the HTTPS no-port form: sentry.asgardsrealm.net is served
                // through a Cloudflare Tunnel, so 443 is the only reachable path — the legacy
                // ":9000" endpoints in the checked-in sentry.properties files predate the
                // tunnel and cannot work through it.
                if (options.getDsn() == null || options.getDsn().isEmpty())
                    options.setDsn("https://a0b8dbad9b8a49cfa51bf65d462e8dae@sentry.asgardsrealm.net/2");
            });
        } catch (Throwable t) {
            log("Sentry init failed (continuing without crash reporting): " + t.getMessage());
        }
        try {
            // Use iOS NSTimeZone API to avoid sandbox violations when accessing /etc/localtime
            NSTimeZone systemTimeZone = NSTimeZone.getSystemTimeZone();
            if (systemTimeZone != null) {
                String tzName = systemTimeZone.getName();
                long offsetSeconds = systemTimeZone.getSecondsFromGMT();
                int offsetMillis = (int) (offsetSeconds * 1000);

                // Set both the property AND the default timezone to prevent /etc/localtime access
                System.setProperty("user.timezone", tzName != null ? tzName : "UTC");
                TimeZone.setDefault(new SimpleTimeZone(offsetMillis, tzName != null ? tzName : "UTC"));
                log("Timezone set to " + tzName + " in main()");
            }

            final NSAutoreleasePool pool = new NSAutoreleasePool();
            log("Calling UIApplication.main()");
            UIApplication.main(args, null, Main.class);
            log("UIApplication.main() returned");
            pool.close();
        } catch (Throwable e) {
            log("Exception in main(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    //special clipboard that works on iOS
    private static final class IOSClipboard implements com.badlogic.gdx.utils.Clipboard {
        @Override
        public boolean hasContents() {
            return UIPasteboard.getGeneralPasteboard().toString().length() > 0;
        }

        @Override
        public String getContents() {
            return UIPasteboard.getGeneralPasteboard().getString();
        }

        @Override
        public void setContents(final String contents0) {
            UIPasteboard.getGeneralPasteboard().setString(contents0);
        }
    }

    private static final class IOSAdapter implements IDeviceAdapter {
        private static final int IO_BUFFER_SIZE = 8192;  // 8 KB buffer for file I/O operations

        @Override
        public boolean isConnectedToInternet() {
            return true;
        }

        @Override
        public boolean isConnectedToWifi() {
            return true;
        }

        @Override
        public String getDownloadsDir() {
            return new IOSFiles().getExternalStoragePath();
        }

        @Override
        public String getVersionString() {
            return "0.0";
        }

        @Override
        public String getLatestChanges(String commitsAtom, Date buildDateOriginal, Date maxDate) {
            return "";
        }

        @Override
        public String getReleaseTag(String releaseAtom) {
            return "";
        }

        @Override
        public boolean openFile(final String filename) {
            return new IOSFiles().local(filename).exists();
        }

        @Override
        public void setLandscapeMode(final boolean landscapeMode) {
            // TODO implement this
        }

        @Override
        public void preventSystemSleep(boolean preventSleep) {
            // TODO implement this
        }

        @Override
        public boolean isTablet() {
            return Gdx.graphics.getWidth() > Gdx.graphics.getHeight();
        }

        @Override
        public void restart() {
            // Not possible on iOS
        }

        @Override
        public void exit() {
            // Not possible on iOS
        }

        @Override
        public void closeSplashScreen() {
            //only for desktop mobile-dev
        }

        @Override
        public void convertToJPEG(InputStream input, OutputStream output) throws IOException {
            // iOS compatibility: Simply copy the input stream to output
            // No conversion needed - Scryfall already provides JPEG images
            byte[] buffer = new byte[IO_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
        }

        @Override
        public void convertToPNG(InputStream input, OutputStream output) throws IOException {
            // Same rationale as convertToJPEG: pass the bytes through
            // (upstream's iOS adapter is a no-op here; a copy is strictly safer)
            byte[] buffer = new byte[IO_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
        }

        @Override
        public Pair<Integer, Integer> getRealScreenSize(boolean real) {
            return Pair.of(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }

        @Override
        public ArrayList<String> getGamepads() {
            return new ArrayList<>();
        }

        @Override
        public org.jupnp.UpnpServiceConfiguration getUpnpPlatformService() {
            // not used on iOS (jupnp is compile-only: provided scope in pom)
            return null;
        }

        @Override
        public boolean isSupportedAudioFormat(File file) {
            // Implemented directly (not via the interface default) for two reasons:
            // the default uses a Set.of interface-static field whose <clinit>
            // doesn't run on MobiVM (NPE), and iOS decodes a different format set.
            // All Forge audio ships as .mp3; ObjectAL decodes mp3/m4a/aac/wav/caf
            // /aiff via AudioToolbox (SFX) and AVAudioPlayer (music). iOS has no
            // native Ogg Vorbis decoder, so .ogg is intentionally excluded.
            if (file == null) {
                return false;
            }
            String path = file.getPath().toLowerCase();
            return path.endsWith(".mp3") || path.endsWith(".m4a") || path.endsWith(".aac")
                    || path.endsWith(".wav") || path.endsWith(".caf") || path.endsWith(".aiff");
        }

        @Override
        public boolean needFileAccess() {
            return false;
        }

        @Override
        public void requestFileAcces() {

        }
    }
}