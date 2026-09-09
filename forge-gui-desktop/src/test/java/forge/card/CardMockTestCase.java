package forge.card;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import forge.GuiDesktop;
import forge.ImageKeys;
import forge.StaticData;
import forge.gamesimulationtests.util.CardDatabaseHelper;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.Lang;
import forge.util.Localizer;

/**
 * Base class for tests that need a populated card database but no running game.
 *
 * <p>
 * This used to extend PowerMock's {@code PowerMockTestCase}. PowerMock 2.0.9 is not
 * usable with TestNG 7.10: it declares {@code @ObjectFactory IObjectFactory
 * create(ITestContext)}, and {@code org.testng.IObjectFactory} was removed in TestNG
 * 7.10. Resolving that method throws {@link NoClassDefFoundError} while TestNG scans the
 * class for annotations, and TestNG reports the class as holding zero tests instead of
 * failing, so every subclass silently stopped running. See issue #11183.
 * </p>
 *
 * <p>
 * Static mocking is now Mockito's own {@link Mockito#mockStatic}, available since the
 * inline mock maker became the default in Mockito 5. Two consequences of losing
 * PowerMock's per-class classloader are handled deliberately:
 * </p>
 * <ul>
 * <li>Static state is no longer isolated per test class, so {@link #releaseMocks()} closes
 * every static mock and clears the {@link Localizer} singleton after each method.</li>
 * <li>{@link PaperCard} caches its {@code hasImage} answer, and the cards come from a
 * process-wide {@link StaticData}. A test class that needs a different answer, or a
 * differently loaded database, must build its own via
 * {@link CardDatabaseHelper#createStaticData(boolean)} rather than share the cached one.</li>
 * </ul>
 */
public class CardMockTestCase {

    public static final String MOCKED_LOCALISED_STRING = "any localised string";

    protected MockedStatic<FModel> fModelMock;
    protected MockedStatic<ImageKeys> imageKeysMock;

    /**
     * The {@link Localizer} that was installed when this class last replaced it with a mock,
     * put back after every method.
     *
     * <p>
     * Clearing the singleton instead breaks every AITest-based class that runs later in the
     * same JVM: those initialise the Localizer through {@code FModel.initialize()} exactly
     * once, behind a static {@code initialized} flag, and once the singleton is null
     * {@code Localizer.getInstance()} silently hands out a bare instance whose resourceBundle
     * is null.
     * </p>
     */
    private static Localizer localizerBeforeMocking;

    @BeforeMethod
    public void initMocks() throws Exception {
        // BaseGameSimulationTest.runGame() calls this again part-way through a test, and
        // Mockito refuses to open a second static mock for a class that already has one.
        releaseMocks();
        initForgeSingletons();
        initCardImageMocks();
        initForgePreferences();
        initializeStaticData();
    }

    @AfterMethod(alwaysRun = true)
    public void releaseMocks() {
        if (imageKeysMock != null) {
            imageKeysMock.close();
            imageKeysMock = null;
        }
        if (fModelMock != null) {
            fModelMock.close();
            fModelMock = null;
        }
        // Undo our own mock and nothing else. Leaving the mock in place, or clearing the
        // singleton outright, breaks every AITest-based class that runs later in this JVM.
        Localizer current = getLocalizerInstance();
        if (current != null && Mockito.mockingDetails(current).isMock()) {
            setLocalizerInstance(localizerBeforeMocking);
        }
    }

    /**
     * {@code ForgeConstants.ASSETS_DIR} is read from {@code GuiBase.getInterface()}, and
     * every other path constant is derived from it, so installing the desktop
     * implementation before the class is first touched gives the real constants their real
     * values. This replaces the old {@code @SuppressStaticInitializationFor} plus
     * reflective assignment of {@code static final} fields, which no plain-reflection
     * approach can do on JDK 17.
     */
    protected void initForgeSingletons() {
        if (GuiBase.getInterface() == null) {
            GuiBase.setInterface(new GuiDesktop());
        }
        // FModel.initialize() normally creates the Lang instance, and it is mocked away
        // here. Without it the card loader dies: CardFace.assignMissingFieldsToVariant
        // calls Lang.getInstance().getNickName() for every card with a flavor-name
        // variant, and one NPE there aborts the whole parallel load batch, leaving a
        // partially populated database that reports most cards as unknown.
        Lang.createInstance("en-US");
    }

    protected void setMock(Localizer mock) {
        Localizer current = getLocalizerInstance();
        if (current == null || !Mockito.mockingDetails(current).isMock()) {
            localizerBeforeMocking = current;
        }
        setLocalizerInstance(mock);
    }

    private static Localizer getLocalizerInstance() {
        try {
            Field instance = Localizer.class.getDeclaredField("instance");
            instance.setAccessible(true);
            return (Localizer) instance.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setLocalizerInstance(Localizer mock) {
        try {
            Field instance = Localizer.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void initForgePreferences() throws IllegalAccessException {
        fModelMock = Mockito.mockStatic(FModel.class);

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

        Localizer localizerMock = Mockito.mock(Localizer.class);
        setMock(localizerMock);
        setLocalizerResourceBundle(localizerMock, dummyResourceBundle);
        Mockito.when(localizerMock.getMessage(Mockito.anyString())).thenReturn(MOCKED_LOCALISED_STRING);

        ForgePreferences forgePreferences = new ForgePreferences();
        fModelMock.when(FModel::getPreferences).thenReturn(forgePreferences);
    }

    private static void setLocalizerResourceBundle(Localizer target, ResourceBundle bundle) {
        try {
            Field resourceBundle = Localizer.class.getDeclaredField("resourceBundle");
            resourceBundle.setAccessible(true);
            resourceBundle.set(target, bundle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Card images are only loaded in a GUI environment, so nothing here needs
     * {@code ImageIO} or {@code ImageCache} stubbed. {@code ImageKeys.hasImage} does need
     * stubbing: {@code CardDb} consults it when picking between reprints, so the art
     * preference assertions depend on the answer.
     */
    protected void initCardImageMocks() {
        imageKeysMock = Mockito.mockStatic(ImageKeys.class);
        imageKeysMock.when(() -> ImageKeys.hasImage(Mockito.any(PaperCard.class), Mockito.anyBoolean()))
                .thenReturn(true);
    }

    protected void initializeStaticData() {
        StaticData data = CardDatabaseHelper.getStaticDataToPopulateOtherMocks();
        fModelMock.when(FModel::getMagicDb).thenReturn(data);
    }
}
