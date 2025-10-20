package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ObjectMap;
import forge.CardStorageReader;
import forge.Forge;
import forge.ImageKeys;
import forge.StaticData;
import forge.adventure.data.*;
import forge.card.*;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgeProfileProperties;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.FileUtil;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

/**
 * Main resource class to access files from the selected adventure
 */
public class Config {
    private static Config currentConfig;
    private final String commonDirectoryName = "common";
    private final String prefix;
    private final String commonPrefix;
    private final HashMap<String, FileHandle> Cache = new HashMap<>();
    private ConfigData configData;
    private final String[] adventures;
    private SettingData settingsData;
    private String Lang = "en-us";
    private final String plane;
    private ObjectMap<String, ObjectMap<String, Sprite>> atlasSprites = new ObjectMap<>();
    private ObjectMap<PointOfInterestData, Array<Sprite>> poiSprites = new ObjectMap<>();
    private ObjectMap<String, ObjectMap<String, Array<Sprite>>> animatedSprites = new ObjectMap<>();

    static public Config instance() {
        if (currentConfig == null)
            currentConfig = new Config();
        return currentConfig;
    }

    private Config() {

        String path = resPath();
        FilenameFilter planesFilter = (file, s) -> (!s.contains(".") && !s.equals(commonDirectoryName));

        adventures = new File(GuiBase.isAndroid() ? ForgeConstants.ADVENTURE_DIR : path + "/res/adventure").list(planesFilter);
        try {
            settingsData = new Json().fromJson(SettingData.class, new FileHandle(ForgeConstants.USER_ADVENTURE_DIR + "settings.json"));

        } catch (Exception e) {
            settingsData = new SettingData();
        }
        if (settingsData.plane == null || settingsData.plane.isEmpty()) {
            if (adventures != null && adventures.length >= 1) {
                //init Shandalar as default plane if found...
                for (String plane : adventures) {
                    if (plane.equalsIgnoreCase("Shandalar"))
                        settingsData.plane = plane;
                }
                //if can't find shandalar, just get any random plane available
                if (settingsData.plane == null || settingsData.plane.isEmpty())
                    settingsData.plane = Aggregates.random(adventures);
            }
        }
        plane = settingsData.plane;

        if (settingsData.width == 0 || settingsData.height == 0) {
            settingsData.width = 1280;
            settingsData.height = 720;
        }
        if (settingsData.videomode == null || settingsData.videomode.isEmpty())
            settingsData.videomode = "720p";
        //reward card display fine tune
        if (settingsData.rewardCardAdj == null || settingsData.rewardCardAdj == 0f)
            settingsData.rewardCardAdj = 1f;
        //tooltip fine tune
        if (settingsData.cardTooltipAdj == null || settingsData.cardTooltipAdj == 0f)
            settingsData.cardTooltipAdj = 1f;
        //reward card display fine tune landscape
        if (settingsData.rewardCardAdjLandscape == null || settingsData.rewardCardAdjLandscape == 0f)
            settingsData.rewardCardAdjLandscape = 1f;
        //tooltip fine tune landscape
        if (settingsData.cardTooltipAdjLandscape == null || settingsData.cardTooltipAdjLandscape == 0f)
            settingsData.cardTooltipAdjLandscape = 1f;


        //prefix = "forge-gui/res/adventure/Shandalar/";
        prefix = getPlanePath(settingsData.plane);
        commonPrefix = resPath() + "/res/adventure/" + commonDirectoryName + "/";

        currentConfig = this;
        if (FModel.getPreferences() != null)
            Lang = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_LANGUAGE);
        FileHandle file = new FileHandle(prefix + "config.json");
        //TODO: Plane's config file should be merged with the common config file.
        if(!file.exists())
            file = new FileHandle(commonPrefix + "config.json");
        try {
            configData = new Json().fromJson(ConfigData.class, file);
        } catch (Exception e) {
            e.printStackTrace();
            configData = new ConfigData();
        }


    }

    private String resPath() {

        return GuiBase.isAndroid() ? ForgeConstants.ASSETS_DIR : Files.exists(Paths.get("./res")) ? "./" : Files.exists(Paths.get("./forge-gui/")) ? "./forge-gui/" : "../forge-gui";
    }

    public String getPlanePath(String plane) {
        if (plane.startsWith("<user>")) {
            return ForgeConstants.USER_ADVENTURE_DIR + "/userplanes/" + plane.substring("<user>".length()) + "/";
        } else {
            return resPath() + "/res/adventure/" + plane + "/";
        }
    }

    public ConfigData getConfigData() {
        return configData;
    }

    public int getBlurDivisor() {
        int val = 1;
        try {
            switch(settingsData.videomode) {
                case "720p":
                case "768p":
                    val = 8;
                    break;
                case "900p":
                case "1080p":
                    val = 16;
                    break;
                case "1440p":
                case "2160p":
                    val = 32;
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            return val;
        }
        return val;
    }
    public String getPrefix() {
        return prefix;
    }

    public String getFilePath(String path) {
        return prefix + path;
    }

    public String getCommonFilePath(String path) {
        return commonPrefix + path;
    }

    public FileHandle getFile(String path) {
        if (Cache.containsKey(path)) return Cache.get(path);

        //if (Cache.containsKey(commonPath)) return Cache.get(commonPath);

        //not cached, look for resource
        System.out.print("Looking for resource " + path + "... ");
        String fullPath = (prefix + path).replace("//", "/");
        String fileName = fullPath.replaceFirst("[.][^.]+$", "");
        String ext = fullPath.substring(fullPath.lastIndexOf('.'));
        String langFile = fileName + "-" + Lang + ext;

        for (int iter = 1; iter <= 2; iter++) {

            if (Files.exists(Paths.get(langFile))) {
                System.out.println("Found!");
                Cache.put(path, new FileHandle(langFile));
                break;
            } else if (Files.exists(Paths.get(fullPath))) {
                System.out.println("Found!");
                Cache.put(path, new FileHandle(fullPath));
                break;
            }
            //no local resource, check common resources
            fullPath = (commonPrefix + path).replace("//", "/");
            fileName = fullPath.replaceFirst("[.][^.]+$", "");
            langFile = fileName + "-" + Lang + ext;
        }
        return Cache.get(path);
    }

    public String getPlane() {
        return plane.replace("<user>", "user_");
    }

    public String[] colorIdNames() {

        return configData.colorIdNames;
    }

    public String[] colorIds() {

        return configData.colorIds;
    }

    public String[] starterEditionNames() {

        return configData.starterEditionNames;
    }

    public String[] starterEditions() {

        return configData.starterEditions;
    }

    public Deck starterDeck(ColorSet color, DifficultyData difficultyData, AdventureModes mode, int index,
                            CardEdition starterEdition, PaperCard commander) {
        switch (mode) {
            case Constructed:
                for (ObjectMap.Entry<String, String> entry : difficultyData.constructedStarterDecks) {
                    if (ColorSet.fromNames(entry.key.toCharArray()).getColor() == color.getColor()) {
                        return CardUtil.getDeck(entry.value, false, false, "", false, false);
                    }
                }
            case Standard:

                for (ObjectMap.Entry<String, String> entry : difficultyData.starterDecks) {
                    if (ColorSet.fromNames(entry.key.toCharArray()).getColor() == color.getColor()) {
                        return CardUtil.getDeck(entry.value, false, false, "", false, false, starterEdition, true);
                    }
                }
            case Chaos:
                return DeckgenUtil.getRandomOrPreconOrThemeDeck("", false, false, false);
            case Custom:
                return DeckProxy.getAllCustomStarterDecks().get(index).getDeck();
            case Pile:
                for (ObjectMap.Entry<String, String> entry : difficultyData.pileDecks) {
                    if (ColorSet.fromNames(entry.key.toCharArray()).getColor() == color.getColor()) {
                        return CardUtil.getDeck(entry.value, false, false, "", false, false);
                    }
                }
            case Commander:
                return buildCommanderPile(commander);
        }
        return null;
    }

    static boolean inCI(PaperCard pc, PaperCard commander) {
        byte cmdMask = commander.getRules().getColorIdentity().getColor();
        return pc.getRules().getColorIdentity().getMissingColors(cmdMask).isColorless();
    }
    static boolean cmcBetween(PaperCard pc, int lo, int hi) {
        int mv = pc.getRules().getManaCost().getCMC();
        return mv >= lo && mv <= hi;
    }
    static boolean typeIs(PaperCard pc, CardType.CoreType t) { return pc.getRules().getType().hasType(t); }
    static boolean isLand(PaperCard pc) { return typeIs(pc, CardType.CoreType.Land); }
    static boolean isCreature(PaperCard pc) { return typeIs(pc, CardType.CoreType.Creature); }
    static boolean isNonlandPermanent(PaperCard pc) { return !isLand(pc) && pc.getRules().getType().isPermanent(); }

    static boolean textContains(PaperCard pc, String s) {
        return pc.getRules().getOracleText().toLowerCase(Locale.ROOT).contains(s);
    }

    // Ramp via “tap: add …” (covers most rocks/dorks/lands with mana ability)
    static boolean isRampTapAdd(PaperCard pc) {
        if (isLand(pc)) return false; // lands go to LANDS bucket
        String t = pc.getRules().getOracleText().toLowerCase(Locale.ROOT);
        return t.contains("{t}: add") || t.matches("(?s).*\\{t}.*add\\s*\\{.*"); // loose but effective
    }

    // Draw effects
    static boolean isDraw(PaperCard pc) { return textContains(pc,"draw"); }

    // Removal “destroy” or “exile”
    static boolean isRemoval(PaperCard pc) {
        String t = pc.getRules().getOracleText().toLowerCase(Locale.ROOT);
        return t.contains("destroy target") || t.contains("exile target") || t.contains("exile all") || t.contains("destroy all");
    }

    static boolean isFixingMultiColorLand(PaperCard pc, byte cmdMask) {
        if (!isLand(pc)) return false;
        var r = pc.getRules();
        var t = r.getOracleText().toLowerCase(Locale.ROOT);

        if (r.getType().isBasic()) return false;

        int provided = 0;
        if (r.getType().hasSubtype("Plains"))   provided |= 1;
        if (r.getType().hasSubtype("Island"))   provided |= 2;
        if (r.getType().hasSubtype("Swamp"))    provided |= 4;
        if (r.getType().hasSubtype("Mountain")) provided |= 8;
        if (r.getType().hasSubtype("Forest"))   provided |= 16;

        if (t.contains("add {w}")) provided |= 1;
        if (t.contains("add {u}")) provided |= 2;
        if (t.contains("add {b}")) provided |= 4;
        if (t.contains("add {r}")) provided |= 8;
        if (t.contains("add {g}")) provided |= 16;

        if (t.contains("add one mana of any color")
                || t.contains("add one mana of any one color")
                || t.contains("add two mana in any combination of colors")
                || t.contains("add one mana of any of")) {
            provided |= cmdMask;
        }

        int overlap = provided & cmdMask;
        if (Integer.bitCount(overlap) < 2) return false;

        boolean tapped =
                t.contains("enters the battlefield tapped")
                        || t.contains("enters tapped")
                        || t.contains("enters the battlefield unless")
                        || t.contains("enters tapped unless")
                        || t.contains("as this enters") && t.contains("tapped");

        return tapped;
    }

    static List<PaperCard> poolWhere(CardDb pool, Predicate<PaperCard> p) {
        return pool.getAllCards(p);
    }
    static void addSingletons(Deck d, Collection<PaperCard> picks, int n, Set<String> used) {
        Collections.shuffle((List<?>) picks, new Random());
        for (PaperCard pc : picks) {
            if (used.add(pc.getName())) { d.getOrCreate(DeckSection.Main).add(pc,1); if (--n==0) break; }
        }
    }

    public static Deck buildCommanderPile(PaperCard commander) {
        Deck d = new Deck("Commander Pile");
        CardDb all = StaticData.instance().getCommonCards();

        byte cmdMask = commander.getRules().getColorIdentity().getColor();
        int colors = Integer.bitCount(cmdMask);

        // ---- LANDS: 40 total ----
        int basicsPct = switch (colors) { case 1->100; case 2->80; case 3->60; case 4->40; default->20; };
        int basics = Math.round(40 * basicsPct / 100f);
        int nonBasics = 40 - basics;

        Map<String, Integer> basicNeed = getBasicLandMap(cmdMask, basics);
        for (Map.Entry<String,Integer> e : basicNeed.entrySet()) {
            PaperCard print = StaticData.instance().getCommonCards().getCard(e.getKey());
            d.getMain().add(print, e.getValue());
        }
        List<PaperCard> fixers = poolWhere(all, pc -> isFixingMultiColorLand(pc, cmdMask));
        Set<String> used = new HashSet<>();
        addSingletons(d, fixers, nonBasics, used);
        List<PaperCard> ramp = poolWhere(all, pc -> inCI(pc, commander) && !isLand(pc) && isRampTapAdd(pc) && cmcBetween(pc,2,4));
        addSingletons(d, ramp, 10, used);
        List<PaperCard> draw = poolWhere(all, pc -> inCI(pc, commander) && isDraw(pc) && cmcBetween(pc,1,4));
        addSingletons(d, draw, 19, used);
        List<PaperCard> kill = poolWhere(all, pc -> inCI(pc, commander) && isRemoval(pc) && cmcBetween(pc,1,6));
        addSingletons(d, kill, 10, used);
        List<PaperCard> dudes = poolWhere(all, pc -> inCI(pc, commander) && isCreature(pc) && cmcBetween(pc,1,6));
        addSingletons(d, dudes, 15, used);
        List<PaperCard> perms = poolWhere(all, pc -> inCI(pc, commander) && isNonlandPermanent(pc) && cmcBetween(pc,1,6));
        addSingletons(d, perms, 5, used);

        // Fill to 99 if short (not sure if this can happen?)
        int need = 99 - d.getMain().countAll();
        if (need > 0) {
            List<PaperCard> filler = poolWhere(all, pc -> inCI(pc, commander) && cmcBetween(pc,1,4) && !used.contains(pc.getName()));
            addSingletons(d, filler, need, used);
        }

        return d;
    }

    private static Map<String, Integer> getBasicLandMap(byte cmdMask, int basics) {
        Map<String, Integer> basicNeed = new LinkedHashMap<>();
        if ((cmdMask & 1)!=0) basicNeed.put("Plains", 0);
        if ((cmdMask & 2)!=0) basicNeed.put("Island", 0);
        if ((cmdMask & 4)!=0) basicNeed.put("Swamp", 0);
        if ((cmdMask & 8)!=0) basicNeed.put("Mountain", 0);
        if ((cmdMask &16)!=0) basicNeed.put("Forest", 0);
        int per = basics / Math.max(1,basicNeed.size()), rem = basics % Math.max(1,basicNeed.size());
        for (String b : basicNeed.keySet()) basicNeed.put(b, per + (rem-->0?1:0));
        return basicNeed;
    }


    public TextureAtlas getAtlas(String spriteAtlas) {
        String fileName = getFile(spriteAtlas).path();
        TextureAtlas atlas = Forge.getAssets().manager().get(fileName, TextureAtlas.class, false);
        if (atlas == null) {
            Forge.getAssets().manager().load(fileName, TextureAtlas.class);
            Forge.getAssets().manager().finishLoadingAsset(fileName);
            atlas = Forge.getAssets().manager().get(fileName, TextureAtlas.class, false);
        }
        return atlas;
    }

    public Sprite getItemSprite(String itemName) {
        return getAtlasSprite(forge.adventure.util.Paths.ITEMS_ATLAS, itemName);
    }

    public Sprite getAtlasSprite(String atlasName, String itemName) {
        Sprite sprite;
        ObjectMap<String, Sprite> sprites = atlasSprites.get(atlasName);
        if (sprites == null) {
            sprites = new ObjectMap<>();
        }
        sprite = sprites.get(itemName);
        if (sprite == null) {
            sprite = getAtlas(atlasName).createSprite(itemName);
            if (sprite != null) {
                sprites.put(itemName, sprite);
                atlasSprites.put(atlasName, sprites);
            }
        }
        return sprite;
    }

    public Array<Sprite> getPOISprites(PointOfInterestData d) {
        Array<Sprite> sprites = poiSprites.get(d);
        if (sprites == null) {
            sprites = getAtlas(d.spriteAtlas).createSprites(d.sprite);
            poiSprites.put(d, sprites);
        }
        return sprites;
    }

    public Array<Sprite> getAnimatedSprites(String path, String animationName) {
        Array<Sprite> sprites;
        ObjectMap<String, Array<Sprite>> mapSprites = animatedSprites.get(path);
        if (mapSprites == null) {
            mapSprites = new ObjectMap<>();
        }
        sprites = mapSprites.get(animationName);
        if (sprites == null) {
            sprites = getAtlas(path).createSprites(animationName);
            if (sprites != null) {
                mapSprites.put(animationName, sprites);
                animatedSprites.put(path, mapSprites);
            }
        }
        return sprites;
    }

    public SettingData getSettingData() {
        return settingsData;
    }

    public Array<String> getAllAdventures() {
        String path = ForgeConstants.USER_ADVENTURE_DIR + "/userplanes/";
        Array<String> adventures = new Array<>();
        if (new File(path).exists())
            adventures.addAll(new File(path).list());
        for (int i = 0; i < adventures.size; i++) {
            adventures.set(i, "<user>" + adventures.get(i));
        }
        adventures.addAll(this.adventures);
        return adventures;
    }

    public void saveSettings() {

        Json json = new Json(JsonWriter.OutputType.json);
        FileHandle handle = new FileHandle(ForgeProfileProperties.getUserDir() + "/adventure/settings.json");
        handle.writeString(json.prettyPrint(json.toJson(settingsData, SettingData.class)), false);

    }

    public void loadResources() {
        RewardData.getAllCards();//initialize before loading custom cards
        final CardRules.Reader rulesReader = new CardRules.Reader();
        ImageKeys.ADVENTURE_CARD_PICS_DIR = Config.currentConfig.getCommonFilePath(forge.adventure.util.Paths.CUSTOM_CARDS_PICS);// not the cleanest solution
        File[] customCards = new File(getCommonFilePath(forge.adventure.util.Paths.CUSTOM_CARDS)).listFiles();
        if (customCards == null)
            return;
        for (File cardFile : customCards) {
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(cardFile);
                rulesReader.reset();
                final List<String> lines = FileUtil.readAllLines(new InputStreamReader(fileInputStream, Charset.forName(CardStorageReader.DEFAULT_CHARSET_NAME)), true);
                CardRules rules = rulesReader.readCard(lines, com.google.common.io.Files.getNameWithoutExtension(cardFile.getName()));
                rules.setCustom();
                PaperCard card = new PaperCard(rules, CardEdition.UNKNOWN_CODE, CardRarity.Special) {
                    @Override
                    public String getImageKey(boolean altState) {
                        return ImageKeys.ADVENTURECARD_PREFIX + getName();
                    }
                };
                CardDb db = rules.isVariant() ? FModel.getMagicDb().getVariantCards() : FModel.getMagicDb().getCommonCards();
                db.addCard(card);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}