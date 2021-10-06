package forge.card;

import forge.ImageCache;
import forge.ImageKeys;
import forge.Singletons;
import forge.StaticData;
import forge.gamesimulationtests.util.CardDatabaseHelper;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.Localizer;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.ObjectFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

@PrepareForTest(value = {FModel.class, Singletons.class, ResourceBundle.class,
        ImageCache.class, ImageIO.class, ImageKeys.class,
        ForgeConstants.class, Localizer.class})
@SuppressStaticInitializationFor({"forge.ImageCache", "forge.localinstance.properties.ForgeConstants"})
public class ForgeCardMockTestCase extends PowerMockTestCase {

    public static final String MOCKED_LOCALISED_STRING = "any localised string";

    protected static String getUserDir() {
        // Adapted - reduced version from ForgeProfileProperties (which is private)
        final String osName = System.getProperty("os.name");
        final String homeDir = System.getProperty("user.home");

        if (StringUtils.isEmpty(osName) || StringUtils.isEmpty(homeDir)) {
            throw new RuntimeException("cannot determine OS and user home directory");
        }

        final String fallbackDataDir = TextUtil.concatNoSpace(homeDir, "/.forge/");

        if (StringUtils.containsIgnoreCase(osName, "windows")) {
            String appRoot = System.getenv().get("APPDATA");
            if (StringUtils.isEmpty(appRoot)) {
                appRoot = fallbackDataDir;
            }
            return appRoot + File.separator + "Forge" + File.separator;
        } else if (StringUtils.containsIgnoreCase(osName, "mac os x")) {
            return TextUtil.concatNoSpace(homeDir, "/Library/Application Support/Forge/");
        }
        // Linux and everything else
        return fallbackDataDir;
    }

    protected void initForgeConstants() throws IllegalAccessException {
        PowerMockito.mockStatic(ForgeConstants.class);
        // Path Sep
        Field fPathSep = PowerMockito.field(ForgeConstants.class, "PATH_SEPARATOR");
        fPathSep.set(ForgeConstants.class, File.separator);
        // Assets Dir
        String assetDir = "../forge-gui/";
        Field fAssetsDir = PowerMockito.field(ForgeConstants.class, "ASSETS_DIR");
        fAssetsDir.set(ForgeConstants.class, assetDir);
        // User Dir
        String homeDir = ForgeCardMockTestCase.getUserDir();
        Field fUserDir = PowerMockito.field(ForgeConstants.class, "USER_DIR");
        fUserDir.set(ForgeConstants.class, homeDir);
        // User Pref Dir
        String prefDir = homeDir + "preferences" + File.separator;
        Field fUserPrefsDir = PowerMockito.field(ForgeConstants.class, "USER_PREFS_DIR");
        fUserPrefsDir.set(ForgeConstants.class, prefDir);
        // Main Pref File
        String mainPrefFile = prefDir + "forge.preferences";
        Field fMainPrefFile = PowerMockito.field(ForgeConstants.class, "MAIN_PREFS_FILE");
        fMainPrefFile.set(ForgeConstants.class, mainPrefFile);
        // Res Dir
        String resDir = assetDir + "res" + File.separator;
        Field fResDir = PowerMockito.field(ForgeConstants.class, "RES_DIR");
        fResDir.set(ForgeConstants.class, resDir);
        // Card Data Dir
        String cardDir = resDir + "cardsfolder" + File.separator;
        Field fCardDataDir = PowerMockito.field(ForgeConstants.class, "CARD_DATA_DIR");
        fCardDataDir.set(ForgeConstants.class, cardDir);
        // Editions Dir
        String editionsDir = resDir + "editions" + File.separator;
        Field fEditionsDir = PowerMockito.field(ForgeConstants.class, "EDITIONS_DIR");
        fEditionsDir.set(ForgeConstants.class, editionsDir);
        // Block Data Dir
        String blockDataDir = resDir + "blockdata" + File.separator;
        Field fBlockData = PowerMockito.field(ForgeConstants.class, "BLOCK_DATA_DIR");
        fBlockData.set(ForgeConstants.class, blockDataDir);
        // User Custom Dir
        String userCustomDir = homeDir + "custom" + File.separator;
        Field fUserCustomDir = PowerMockito.field(ForgeConstants.class, "USER_CUSTOM_DIR");
        fUserCustomDir.set(ForgeConstants.class, userCustomDir);
        // User Custom card Dir
        String userCustomCardDir = userCustomDir + "cards" + File.separator;
        Field fUserCustoCardDir = PowerMockito.field(ForgeConstants.class, "USER_CUSTOM_CARDS_DIR");
        fUserCustoCardDir.set(ForgeConstants.class, userCustomCardDir);
        // User Custom Edition Dir
        String userCustomEditionDir = userCustomDir + "editions" + File.separator;
        Field fUserCustomEditionDir = PowerMockito.field(ForgeConstants.class, "USER_CUSTOM_EDITIONS_DIR");
        fUserCustomEditionDir.set(ForgeConstants.class, userCustomEditionDir);
        // Lang Dir
        String langDir = resDir + "languages" + File.separator;
        Field fLangDir = PowerMockito.field(ForgeConstants.class, "LANG_DIR");
        fLangDir.set(ForgeConstants.class, langDir);
    }

    protected void setMock(Localizer mock) {
        try {
            Field instance = Localizer.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeMethod
    protected void initMocks() throws Exception {
        //Loading a card also automatically loads the image, which we do not want (even if it wouldn't cause exceptions).
        //The static initializer block in ImageCache can't fully be mocked (https://code.google.com/p/powermock/issues/detail?id=256), so we also need to mess with ImageIO...
        initCardImageMocks();
        initForgeConstants();
        //Mocking some more static stuff
        initForgePreferences();
        initializeStaticData();
    }

    protected void initForgePreferences() throws IllegalAccessException {
        PowerMockito.mockStatic(Singletons.class);
        PowerMockito.mockStatic(FModel.class);
        ForgePreferences forgePreferences = new ForgePreferences();

        ResourceBundle dummyResourceBundle = new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return key;
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.emptyEnumeration();
            }
        };

        PowerMockito.mockStatic(ResourceBundle.class);
        PowerMockito.when(ResourceBundle.getBundle("en-US", Locale.ENGLISH)).thenReturn(dummyResourceBundle);
        Localizer localizerMock = PowerMockito.mock(Localizer.class);
        setMock(localizerMock);
        PowerMockito.field(Localizer.class, "resourceBundle").set(localizerMock, dummyResourceBundle);
        PowerMockito.when(localizerMock.getMessage(Mockito.anyString())).thenReturn(MOCKED_LOCALISED_STRING);
        PowerMockito.when(FModel.getPreferences()).thenReturn(forgePreferences);
    }

    protected void initCardImageMocks() {
        //make sure that loading images only happens in a GUI environment, so we no longer need to mock this
        PowerMockito.mockStatic(ImageIO.class);
        PowerMockito.mockStatic(ImageCache.class);
        PowerMockito.mockStatic(ImageKeys.class);
        PowerMockito.when(ImageKeys.hasImage(Mockito.any(PaperCard.class), Mockito.anyBoolean())).thenReturn(true);
    }

    protected void initializeStaticData() {
        StaticData data = CardDatabaseHelper.getStaticDataToPopulateOtherMocks();
        PowerMockito.when(FModel.getMagicDb()).thenReturn(data);
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }
}
