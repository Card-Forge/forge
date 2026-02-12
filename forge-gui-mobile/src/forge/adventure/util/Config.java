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
import forge.adventure.data.*;
import forge.card.*;
import forge.deck.Deck;
import forge.deck.DeckProxy;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

    public Deck starterDeck(ColorSet color, DifficultyData difficultyData, AdventureModes mode, int index, CardEdition starterEdition) {
        switch (mode) {
            case Constructed:
                for (ObjectMap.Entry<String, String> entry : difficultyData.constructedStarterDecks) {
                    if (ColorSet.fromNames(entry.key.toCharArray()).getColor() == color.getColor()) {
                        return CardUtil.getDeck(entry.value, false, false, "", false, false);
                    }
                }
            case Standard:
                // Check for edition-specific starter decks first
                if (starterEdition != null && configData.starterDecksByEdition != null) {
                    ObjectMap<String, String> editionDecks = configData.starterDecksByEdition.get(starterEdition.getCode());
                    if (editionDecks != null) {
                        for (ObjectMap.Entry<String, String> entry : editionDecks) {
                            if (ColorSet.fromNames(entry.key.toCharArray()).getColor() == color.getColor()) {
                                return CardUtil.getDeck(entry.value, false, false, "", false, false);
                            }
                        }
                    }
                }
                // Fall back to default starter decks (JSON generation with edition filter)
                for (ObjectMap.Entry<String, String> entry : difficultyData.starterDecks) {
                    if (ColorSet.fromNames(entry.key.toCharArray()).getColor() == color.getColor()) {
                        return CardUtil.getDeck(entry.value, false, false, "", false, false, starterEdition, true);
                    }
                }
            case Chaos:
                return DeckgenUtil.getRandomOrPreconOrThemeDeck("", false, false, false, configData.allowedEditions);
            case Custom:
                return DeckProxy.getAllCustomStarterDecks().get(index).getDeck();
            case Pile:
                for (ObjectMap.Entry<String, String> entry : difficultyData.pileDecks) {
                    if (ColorSet.fromNames(entry.key.toCharArray()).getColor() == color.getColor()) {
                        return CardUtil.getDeck(entry.value, false, false, "", false, false);
                    }
                }
            case Commander:
                for (ObjectMap.Entry<String, String> entry : difficultyData.commanderDecks) {
                    if (ColorSet.fromNames(entry.key.toCharArray()).getColor() == color.getColor()) {
                        return CardUtil.getDeck(entry.value, false, false, "", false, false);
                    }
                };
        }
        return null;
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

    public DifficultyData getDifficultyByName(String difficultyName) {
        return Arrays.stream(configData.difficulties)
                .filter(d -> d.name.equals(difficultyName))
                .findAny().orElse(null);
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