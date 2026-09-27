package forge.api;

import forge.StaticData;
import forge.ai.AiProfileUtil;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.concurrent.*;

/** Portable GUI services for the shared human controller; no Swing windows or network services. */
final class HeadlessPlatform {
    private static final ExecutorService UI = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Mana Table input");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile Thread uiThread;
    private static volatile MatchSession active;
    private static boolean initialized;

    static synchronized void initialize(Path resources, Path profile) {
        if (initialized) return;
        System.setProperty("forge.resources", resources.toAbsolutePath().normalize() + File.separator);
        System.setProperty("forge.userDir", profile.resolve("engine-profile").toAbsolutePath().toString());
        System.setProperty("forge.cacheDir", profile.resolve("engine-cache").toAbsolutePath().toString());
        GuiBase.setUsingAppDirectory(true);
        var base = (IGuiBase) Proxy.newProxyInstance(IGuiBase.class.getClassLoader(), new Class<?>[]{IGuiBase.class}, (proxy, method, args) -> {
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            return switch (method.getName()) {
                case "isRunningOnDesktop" -> true;
                case "isLibgdxPort", "hasNetGame", "isSupportedAudioFormat" -> false;
                case "isGuiThread" -> Thread.currentThread() == uiThread;
                case "getAssetsDir" -> resources.toAbsolutePath().getParent() + File.separator;
                case "getCurrentVersion" -> "Mana Table beta";
                case "getScreenScale" -> 1f;
                case "getAvatarCount", "getSleevesCount" -> 1;
                case "encodeSymbols" -> args[0];
                case "invokeInEdtLater" -> { later((Runnable) args[0]); yield null; }
                case "invokeInEdtNow", "invokeInEdtAndWait" -> { andWait((Runnable) args[0]); yield null; }
                case "runBackgroundTask" -> { CompletableFuture.runAsync((Runnable) args[1]); yield null; }
                case "getNewGuiGame" -> active == null ? null : active.gui();
                case "showBugReportDialog" -> { if (active != null) active.fail(String.valueOf(args[1])); yield null; }
                case "showOptionDialog", "showInputDialog", "getChoices", "order", "chooseCard" -> {
                    if (active == null) throw new IllegalStateException("No active match");
                    yield active.platformDialog(method.getName(), args);
                }
                case "getSkinIcon", "getUnskinnedIcon", "getCardArt", "createLayeredImage", "getImageFetcher",
                     "createAudioClip", "createAudioMusic", "getUpnpPlatformService" -> null;
                case "clearImageCache", "preventSystemSleep", "copyToClipboard", "startAltSoundSystem" -> null;
                case "toString" -> "Mana Table platform";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Desktop service is not available: " + method.getName());
            };
        });
        GuiBase.setInterface(base);
        var prefs = FModel.getPreferences();
        prefs.setPref(FPref.PLAYER_NAME, "You");
        prefs.setPref(FPref.UI_SELECT_FROM_CARD_DISPLAYS, false);
        prefs.setPref(FPref.UI_ORDER_HAND, false);
        prefs.setPref(FPref.UI_SHOW_ACTIONABLE_HIGHLIGHTS, true);
        prefs.setPref(FPref.UI_SHOW_AUTOTAP_PREVIEW, true);
        // A land play must return to a visible decision, even with nothing else to cast.
        // Auto-passing here skips publication of the new board and can run into the AI's turn.
        prefs.setPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, false);
        prefs.setPref(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD, "NEVER");
        prefs.setPref(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS, "NEVER");
        AiProfileUtil.loadAllProfiles(resources.resolve("ai").toString());
        StaticData.instance().setMulliganRule(forge.MulliganDefs.MulliganRule.London);
        initialized = true;
    }

    static void activate(MatchSession session) { active = session; }

    static void later(Runnable task) {
        MatchSession owner = active;
        UI.execute(() -> run(task, owner));
    }

    private static void run(Runnable task, MatchSession owner) {
        uiThread = Thread.currentThread();
        try {
            task.run();
            if (owner != null) owner.publishInput();
        } catch (Throwable error) {
            if (owner != null) owner.fail(error);
            else error.printStackTrace(System.err);
        }
    }

    private static void andWait(Runnable task) throws Exception {
        if (Thread.currentThread() == uiThread) { task.run(); return; }
        MatchSession owner = active;
        UI.submit(() -> run(task, owner)).get();
    }
}
