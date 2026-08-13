import forge.CardStorageReader;
import forge.StaticData;
import forge.gui.GuiBase;
import forge.gui.download.GuiDownloadService;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.model.FModel;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.util.CardTranslation;
import forge.util.FSerializableFunction;
import forge.util.ImageFetcher;
import forge.util.Lang;
import forge.util.Localizer;

import forge.gamemodes.match.HostedMatch;

import org.jupnp.UpnpServiceConfiguration;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ForgeMatrixWriter {
    private ForgeMatrixWriter() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: ForgeMatrixWriter <forge-gui-dir> <input.tsv> <output.dat>");
            System.exit(2);
        }

        Path forgeGuiDir = Path.of(args[0]).toAbsolutePath().normalize();
        Path input = Path.of(args[1]).toAbsolutePath().normalize();
        Path output = Path.of(args[2]).toAbsolutePath().normalize();

        GuiBase.setInterface(new HeadlessGui(forgeGuiDir));
        FModel.loadDynamicGamedata();
        Lang.createInstance("en-US");
        Localizer.getInstance().initialize("en-US", forgeGuiDir.resolve("res/languages").toString());
        CardTranslation.preloadTranslation("en-US", forgeGuiDir.resolve("res/languages").toString());
        Path emptyCustomEditions = Files.createTempDirectory("forge-empty-custom-editions");

        StaticData cards = new StaticData(
                new CardStorageReader(forgeGuiDir.resolve("res/cardsfolder").toString(), CardStorageReader.ProgressObserver.emptyObserver, false),
                new CardStorageReader(forgeGuiDir.resolve("res/tokenscripts").toString(), CardStorageReader.ProgressObserver.emptyObserver, false),
                null,
                null,
                forgeGuiDir.resolve("res/editions").toString(),
                emptyCustomEditions.toString(),
                forgeGuiDir.resolve("res/blockdata").toString(),
                forgeGuiDir.resolve("res/setlookup").toString(),
                "",
                true,
                false,
                false,
                false);

        HashMap<String, List<Map.Entry<PaperCard, Integer>>> matrix = new HashMap<>();
        int rows = 0;
        int skippedCards = 0;
        int skippedCommanders = 0;
        Map<String, Integer> unknownCommanders = new LinkedHashMap<>();
        Map<String, Integer> unknownCards = new LinkedHashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\t");
                if (parts.length < 3) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }

                String commanderName = parts[0].trim();
                String cardName = parts[1].trim();
                int weight = Integer.parseInt(parts[2].trim());
                if (weight <= 0) {
                    continue;
                }

                PaperCard commander = cards.getCommonCards().getUniqueByName(commanderName);
                if (commander == null) {
                    skippedCommanders++;
                    unknownCommanders.merge(commanderName, 1, Integer::sum);
                    continue;
                }

                PaperCard card = cards.getCommonCards().getUniqueByName(cardName);
                if (card == null) {
                    skippedCards++;
                    unknownCards.merge(cardName, 1, Integer::sum);
                    continue;
                }

                matrix.computeIfAbsent(commander.getName(), key -> new ArrayList<>())
                        .add(new AbstractMap.SimpleEntry<>(card, weight));
                rows++;
            }
        }

        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(Files.newOutputStream(output))) {
            objectOutput.writeObject(matrix);
        }

        System.out.printf(
                "Wrote %d commanders, %d card weights, skipped %d unknown commanders and %d unknown cards%n",
                matrix.size(), rows, skippedCommanders, skippedCards);
        printUnknowns("Unknown commanders", unknownCommanders);
        printUnknowns("Unknown cards", unknownCards);
    }

    private static void printUnknowns(String label, Map<String, Integer> unknowns) {
        if (unknowns.isEmpty()) {
            return;
        }
        System.out.printf("%s (%d unique):%n", label, unknowns.size());
        unknowns.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(100)
                .forEach(entry -> System.out.printf("  %s\t%d%n", entry.getKey(), entry.getValue()));
    }

    private static final class HeadlessGui implements IGuiBase {
        private final String assetsDir;

        private HeadlessGui(Path forgeGuiDir) {
            this.assetsDir = forgeGuiDir.toString() + File.separator;
        }

        @Override public boolean isRunningOnDesktop() { return true; }
        @Override public boolean isLibgdxPort() { return false; }
        @Override public String getCurrentVersion() { return "headless"; }
        @Override public void invokeInEdtNow(Runnable runnable) { runnable.run(); }
        @Override public void invokeInEdtLater(Runnable runnable) { runnable.run(); }
        @Override public void invokeInEdtAndWait(Runnable proc) { proc.run(); }
        @Override public void runBackgroundTask(String message, Runnable task) { task.run(); }
        @Override public boolean isGuiThread() { return true; }
        @Override public String getAssetsDir() { return assetsDir; }
        @Override public ImageFetcher getImageFetcher() { return null; }
        @Override public ISkinImage getSkinIcon(FSkinProp skinProp) { return null; }
        @Override public ISkinImage getUnskinnedIcon(String path) { return null; }
        @Override public ISkinImage getCardArt(PaperCard card, boolean backFace) { return null; }
        @Override public ISkinImage createLayeredImage(PaperCard card, FSkinProp background, String overlayFilename, float opacity) { return null; }
        @Override public void clearImageCache() { }
        @Override public String encodeSymbols(String str, boolean formatReminderText) { return str; }
        @Override public int getAvatarCount() { return 0; }
        @Override public int getSleevesCount() { return 0; }
        @Override public float getScreenScale() { return 1.0f; }
        @Override public void preventSystemSleep(boolean preventSleep) { }
        @Override public void download(GuiDownloadService service, Consumer<Boolean> callback) { callback.accept(false); }
        @Override public void copyToClipboard(String text) { }
        @Override public void browseToUrl(String url) throws IOException, URISyntaxException { }
        @Override public void showCardList(String title, String message, List<PaperCard> list) { }
        @Override public boolean showBoxedProduct(String title, String message, List<PaperCard> list) { return false; }
        @Override public void showBugReportDialog(String title, String text, boolean showExitAppBtn) { }
        @Override public void showImageDialog(ISkinImage image, String message, String title) { }
        @Override public int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption) { return defaultOption; }
        @Override public String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions, boolean isNumeric) { return initialInput; }
        @Override public String showFileDialog(String title, String defaultDir) { return defaultDir; }
        @Override public File getSaveFile(File defaultFile) { return defaultFile; }
        @Override public <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices) { return destChoices; }
        @Override public <T> List<T> getChoices(String message, int min, int max, Collection<T> choices, Collection<T> selected, FSerializableFunction<T, String> display) { return new ArrayList<>(selected); }
        @Override public PaperCard chooseCard(String title, String message, List<PaperCard> list) { return list.isEmpty() ? null : list.get(0); }
        @Override public boolean isSupportedAudioFormat(File file) { return false; }
        @Override public IAudioClip createAudioClip(String filename) { return null; }
        @Override public IAudioMusic createAudioMusic(String filename) { return null; }
        @Override public void startAltSoundSystem(String filename, boolean isSynchronized) { }
        @Override public void showSpellShop() { }
        @Override public void showBazaar() { }
        @Override public IGuiGame getNewGuiGame() { return null; }
        @Override public HostedMatch hostMatch() { return null; }
        @Override public UpnpServiceConfiguration getUpnpPlatformService() { return null; }
        @Override public boolean hasNetGame() { return false; }
    }
}
