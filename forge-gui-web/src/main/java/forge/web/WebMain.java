package forge.web;

import forge.error.ExceptionHandler;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.IHasForgeLog;
import forge.util.Localizer;
import forge.web.console.ServerConsole;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/** The program's entry point. It loads Forge, opens the server window, starts the web service with this launch's link token and opens the link. The token is new on each launch, so a link from an earlier launch no longer works. */
public final class WebMain implements IHasForgeLog {
    /** How long the process waits after the last browser leaves before it quits, unless the console is set to keep it open. */
    private static final long IDLE_MILLIS = 15_000;

    private WebMain() {}

    public static void main(final String[] args) throws Exception {
        final WebGuiBase ui = new WebGuiBase();
        GuiBase.setInterface(ui);
        ExceptionHandler.registerErrorHandling();
        GuiBase.logHWInfo();
        final CountDownLatch quit = new CountDownLatch(1);
        // The console uses the player's language, which is known before FModel loads the rest
        Localizer.getInstance().initialize(FModel.getPreferences().getPref(FPref.UI_LANGUAGE), ForgeConstants.LANG_DIR);
        // The console opens before the cards are read, because reading them takes long enough to look like a failure
        final ServerConsole console = ServerConsole.open(ui, quit::countDown);
        FModel.initialize(console, prefs -> null, false);
        // The server starts without the data for card-based deck generation, which takes seconds to load and is not loaded at all with lazy card loading
        if (!FModel.getPreferences().getPrefBoolean(FPref.LOAD_CARD_SCRIPTS_LAZILY)) {
            final Thread deckGen = new Thread(FModel::loadDeckGenMatrix, "DeckGenData");
            deckGen.setDaemon(true);
            deckGen.start();
        }
        final ServerControl service = new ServerControl(ui, IDLE_MILLIS, quit::countDown, console != null, newToken());
        // Ctrl+C or a machine shutdown skips the finally below, and without this hook a forwarded port would stay open on the router
        Runtime.getRuntime().addShutdownHook(new Thread(service::stop, "ForgeWebShutdown"));
        try {
            if (console != null) {
                console.starting(Localizer.getInstance().getMessage("lblStartingServer"));
            }
            Exception failure = null;
            try {
                service.start();
            } catch (final Exception e) {
                // The console stays open to show the error and let the host start again, but without a console there is nothing to wait for
                if (console == null) {
                    throw e;
                }
                netLog.error(e, "Could not start the server");
                failure = e;
            }
            if (console != null) {
                console.attach(service, failure);
            }
            if (service.running() && !Boolean.getBoolean("forge.web.noBrowser")) {
                openBrowser(service.url(), ui);
            }
            quit.await();
        } finally {
            service.stop();
            if (console != null) {
                console.close();
            }
        }
        // Some engine and netplay threads are not daemons
        System.exit(0);
    }

    private static String newToken() {
        final byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static void openBrowser(final String url, final WebGuiBase ui) {
        for (final String exe : appModeBrowsers()) {
            if (new File(exe).isFile()) {
                try {
                    // --app gives a window without tabs or an address bar
                    new ProcessBuilder(exe, "--app=" + url).start();
                    return;
                } catch (final IOException e) {
                    netLog.warn(e, "Could not start {}", exe);
                }
            }
        }
        try {
            ui.browseToUrl(url);
        } catch (final IOException | URISyntaxException e) {
            netLog.warn(e, "Open {} in a browser", url);
        }
    }

    private static List<String> appModeBrowsers() {
        final List<String> candidates = new ArrayList<>();
        final String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            for (final String base : new String[]{System.getenv("ProgramFiles(x86)"), System.getenv("ProgramFiles"), System.getenv("LOCALAPPDATA")}) {
                if (base != null) {
                    candidates.add(base + "\\Microsoft\\Edge\\Application\\msedge.exe");
                    candidates.add(base + "\\Google\\Chrome\\Application\\chrome.exe");
                }
            }
        } else if (os.contains("mac")) {
            candidates.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
            candidates.add("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge");
            candidates.add("/Applications/Chromium.app/Contents/MacOS/Chromium");
        } else {
            for (final String dir : System.getenv().getOrDefault("PATH", "").split(File.pathSeparator)) {
                for (final String name : new String[]{"chromium", "chromium-browser", "google-chrome", "microsoft-edge"}) {
                    candidates.add(dir + File.separator + name);
                }
            }
        }
        return candidates;
    }
}
