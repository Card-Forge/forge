package forge.api;

import forge.CardStorageReader;
import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardType;
import forge.game.card.CardUtil;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.Lang;
import forge.util.Localizer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.util.Map;

/** Explicit resource bootstrap for a dedicated Java engine process, without FModel or Swing. */
public final class EngineResources {
    private EngineResources() { }

    public static synchronized StaticData load(Path resourceDirectory) {
        if (StaticData.instance() != null) {
            throw new IllegalStateException("Forge data is already initialized; reuse StaticData.instance()");
        }
        Path root = resourceDirectory.toAbsolutePath().normalize();
        for (String required : new String[]{"cardsfolder", "tokenscripts", "editions", "languages", "blockdata", "lists"}) {
            if (!Files.isDirectory(root.resolve(required))) {
                throw new IllegalArgumentException("Missing Forge resource directory: " + root.resolve(required));
            }
        }
        for (String required : new String[]{"lists/TypeLists.txt", "lists/NonStackingKWList.txt", "languages/en-US.properties"}) {
            if (!Files.isRegularFile(root.resolve(required))) {
                throw new IllegalArgumentException("Missing Forge resource file: " + root.resolve(required));
            }
        }
        Lang.createInstance("en-US");
        Localizer.getInstance().initialize("en-US", root.resolve("languages").toString());
        // Printing selection consults local image availability even when no GUI is running.
        // These optional directories are only read; bootstrap neither creates nor downloads art.
        ImageKeys.initializeDirs(imagePath(root, "cards"), Map.of(), imagePath(root, "tokens"),
                imagePath(root, "icons"), imagePath(root, "boosters"), imagePath(root, "fatpacks"),
                imagePath(root, "boosterboxes"), imagePath(root, "precons"), imagePath(root, "tournamentpacks"));
        if (!CardType.Constant.LOADED.isSet()) {
            var types = FileSection.parseSections(FileUtil.readFile(root.resolve("lists/TypeLists.txt").toFile()));
            types.forEach(CardType.Helper::parseTypes);
            CardType.Constant.LOADED.set();
        }
        for (String keyword : FileUtil.readFile(root.resolve("lists/NonStackingKWList.txt").toFile())) {
            if (keyword.length() > 1 && !CardUtil.NON_STACKING_LIST.contains(keyword)) {
                CardUtil.NON_STACKING_LIST.add(keyword);
            }
        }
        return new StaticData(new CardStorageReader(root.resolve("cardsfolder").toString(), null, false),
                new CardStorageReader(root.resolve("tokenscripts").toString(), null, false), null, null,
                root.resolve("editions").toString(), root.resolve("custom/editions").toString(),
                root.resolve("blockdata").toString(), root.resolve("setlookup").toString(),
                // The catalog is a browser, not a tournament-legality filter. Keep scripted
                // casual cards and scripts without an assigned printing available too.
                "latest", true, true, false, false);
    }

    private static String imagePath(Path root, String category) {
        return root.resolve("pics").resolve(category) + File.separator;
    }
}
