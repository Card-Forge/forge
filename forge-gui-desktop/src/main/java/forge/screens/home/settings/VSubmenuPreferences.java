package forge.screens.home.settings;

import forge.control.FControl.CloseAction;
import forge.control.KeyboardShortcuts;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.game.GameLogEntryType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.*;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.toolbox.FSkin.SkinnedTextField;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;


/**
 * Assembles Swing components of preferences submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuPreferences implements IVSubmenu<CSubmenuPreferences> {

    /** */
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("Preferences"));

    /** */
    private final JPanel pnlPrefs = new JPanel();
    private final FScrollPane scrContent = new FScrollPane(pnlPrefs, false,
    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    private final FLabel btnReset = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("btnReset")).build();
    private final FLabel btnDeleteMatchUI = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("btnDeleteMatchUI")).build();
    private final FLabel btnDeleteEditorUI = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("btnDeleteEditorUI")).build();
    private final FLabel btnDeleteWorkshopUI = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("btnDeleteWorkshopUI")).build();
    private final FLabel btnUserProfileUI = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("btnUserProfileUI")).build();
    private final FLabel btnContentDirectoryUI = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("btnContentDirectoryUI")).build();
    private final FLabel btnResetJavaFutureCompatibilityWarnings = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("btnResetJavaFutureCompatibilityWarnings")).build();
    private final FLabel btnClearImageCache = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("btnClearImageCache")).build();
    private final FLabel btnTokenPreviewer = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("btnTokenPreviewer")).build();

    private final FLabel btnPlayerName = new FLabel.Builder().opaque(true).hoverable(true).text("").build();

    private final JCheckBox cbRemoveSmall = new OptionsCheckBox(localizer.getMessage("cbRemoveSmall"));
    private final JCheckBox cbCardBased = new OptionsCheckBox(localizer.getMessage("cbCardBased"));
    private final JCheckBox cbSingletons = new OptionsCheckBox(localizer.getMessage("cbSingletons"));
    private final JCheckBox cbRemoveArtifacts = new OptionsCheckBox(localizer.getMessage("cbRemoveArtifacts"));
    private final JCheckBox cbAnte = new OptionsCheckBox(localizer.getMessage("cbAnte"));
    private final JCheckBox cbAnteMatchRarity = new OptionsCheckBox(localizer.getMessage("cbAnteMatchRarity"));
    private final JCheckBox cbEnableAICheats = new OptionsCheckBox(localizer.getMessage("cbEnableAICheats"));
    private final JCheckBox cbManaBurn = new OptionsCheckBox(localizer.getMessage("cbManaBurn"));
    private final JCheckBox cbManaLostPrompt = new OptionsCheckBox(localizer.getMessage("cbManaLostPrompt"));
    private final JCheckBox cbDevMode = new OptionsCheckBox(localizer.getMessage("cbDevMode"));
    private final JCheckBox cbLoadCardsLazily = new OptionsCheckBox(localizer.getMessage("cbLoadCardsLazily"));
    private final JCheckBox cbLoadArchivedFormats = new OptionsCheckBox(localizer.getMessage("cbLoadArchivedFormats"));
    private final JCheckBox cbWorkshopSyntax = new OptionsCheckBox(localizer.getMessage("cbWorkshopSyntax"));
    private final JCheckBox cbEnforceDeckLegality = new OptionsCheckBox(localizer.getMessage("cbEnforceDeckLegality"));
    private final JCheckBox cbExperimentalRestore = new OptionsCheckBox(localizer.getMessage("cbExperimentalRestore"));
    private final JCheckBox cbPerformanceMode = new OptionsCheckBox(localizer.getMessage("cbPerformanceMode"));
    private final JCheckBox cbSROptimize = new OptionsCheckBox(localizer.getMessage("cbSROptimize"));
    private final JCheckBox cbFilteredHands = new OptionsCheckBox(localizer.getMessage("cbFilteredHands"));
    private final JCheckBox cbImageFetcher = new OptionsCheckBox(localizer.getMessage("cbImageFetcher"));
    private final JCheckBox cbDisableCardImages = new OptionsCheckBox(localizer.getMessage("lblDisableCardImages"));
    private final JCheckBox cbCloneImgSource = new OptionsCheckBox(localizer.getMessage("cbCloneImgSource"));
    private final JCheckBox cbScaleLarger = new OptionsCheckBox(localizer.getMessage("cbScaleLarger"));
    private final JCheckBox cbRenderBlackCardBorders = new OptionsCheckBox(localizer.getMessage("cbRenderBlackCardBorders"));
    private final JCheckBox cbLargeCardViewers = new OptionsCheckBox(localizer.getMessage("cbLargeCardViewers"));
    private final JCheckBox cbSmallDeckViewer = new OptionsCheckBox(localizer.getMessage("cbSmallDeckViewer"));
    private final JCheckBox cbDisplayFoil = new OptionsCheckBox(localizer.getMessage("cbDisplayFoil"));
    private final JCheckBox cbRandomFoil= new OptionsCheckBox(localizer.getMessage("cbRandomFoil"));
    private final JCheckBox cbRandomArtInPools = new OptionsCheckBox(localizer.getMessage("cbRandomArtInPools"));
    private final JCheckBox cbEnableSounds = new OptionsCheckBox(localizer.getMessage("cbEnableSounds"));
    private final JCheckBox cbEnableMusic = new OptionsCheckBox(localizer.getMessage("cbEnableMusic"));
    private final JCheckBox cbAltSoundSystem = new OptionsCheckBox(localizer.getMessage("cbAltSoundSystem"));
    private final JCheckBox cbUiForTouchScreen = new OptionsCheckBox(localizer.getMessage("cbUiForTouchScreen"));
    private final JCheckBox cbTimedTargOverlay = new OptionsCheckBox(localizer.getMessage("cbTimedTargOverlay"));
    private final JCheckBox cbCompactMainMenu = new OptionsCheckBox(localizer.getMessage("cbCompactMainMenu"));
    private final JCheckBox cbDetailedPaymentDesc = new OptionsCheckBox(localizer.getMessage("cbDetailedPaymentDesc"));
    private final JCheckBox cbGrayText = new OptionsCheckBox(localizer.getMessage("cbGrayText"));
    private final JCheckBox cbPromptFreeBlocks = new OptionsCheckBox(localizer.getMessage("cbPromptFreeBlocks"));
    private final JCheckBox cbPauseWhileMinimized = new OptionsCheckBox(localizer.getMessage("cbPauseWhileMinimized"));
    private final JCheckBox cbCompactPrompt = new OptionsCheckBox(localizer.getMessage("cbCompactPrompt"));
    private final JCheckBox cbEscapeEndsTurn = new OptionsCheckBox(localizer.getMessage("cbEscapeEndsTurn"));
    private final JCheckBox cbPreselectPrevAbOrder = new OptionsCheckBox(localizer.getMessage("cbPreselectPrevAbOrder"));
    private final JCheckBox cbHideReminderText = new OptionsCheckBox(localizer.getMessage("cbHideReminderText"));
    private final JCheckBox cbCardTextUseSansSerif = new OptionsCheckBox(localizer.getMessage("cbCardTextUseSansSerif"));
    private final JCheckBox cbCardTextHideReminder = new OptionsCheckBox(localizer.getMessage("cbCardTextHideReminder"));
    private final JCheckBox cbOpenPacksIndiv = new OptionsCheckBox(localizer.getMessage("cbOpenPacksIndiv"));
    private final JCheckBox cbTokensInSeparateRow = new OptionsCheckBox(localizer.getMessage("cbTokensInSeparateRow"));
    private final JCheckBox cbStackCreatures = new OptionsCheckBox(localizer.getMessage("cbStackCreatures"));
    private final JCheckBox cbFilterLandsByColorId = new OptionsCheckBox(localizer.getMessage("cbFilterLandsByColorId"));
    private final JCheckBox cbShowStormCount = new OptionsCheckBox(localizer.getMessage("cbShowStormCount"));
    private final JCheckBox cbRemindOnPriority = new OptionsCheckBox(localizer.getMessage("cbRemindOnPriority"));
    private final JCheckBox cbUseSentry = new OptionsCheckBox(localizer.getMessage("cbUseSentry"));
    private final JCheckBox cbEnableUnknownCards = new OptionsCheckBox(localizer.getMessage("lblEnableUnknownCards"));
    private final JCheckBox cbEnableNonLegalCards = new OptionsCheckBox(localizer.getMessage("lblEnableNonLegalCards"));
    private final JCheckBox cbAllowCustomCardsDeckConformance = new OptionsCheckBox(localizer.getMessage("lblAllowCustomCardsInDecks"));
    private final JCheckBox cbUseExperimentalNetworkStream = new OptionsCheckBox(localizer.getMessage("lblExperimentalNetworkCompatibility"));
    private final JCheckBox cbCardArtCoreExpansionsOnlyOpt = new OptionsCheckBox(localizer.getMessage("lblPrefArtExpansionOnly"));
    private final JCheckBox cbSmartCardArtSelectionOpt = new OptionsCheckBox(localizer.getMessage("lblSmartCardArtOpt"));
    private final JCheckBox cbShowDraftRanking = new OptionsCheckBox(localizer.getMessage("lblShowDraftRankingOverlay"));

    private final Map<FPref, KeyboardShortcutField> shortcutFields = new HashMap<>();

    // ComboBox items are added in CSubmenuPreferences since this is just the View.
    private final FComboBoxPanel<GameLogEntryType> cbpGameLogEntryType = new FComboBoxPanel<>(localizer.getMessage("cbpGameLogEntryType")+":");
    private final FComboBoxPanel<CloseAction> cbpCloseAction = new FComboBoxPanel<>(localizer.getMessage("cbpCloseAction")+":");
    private final FComboBoxPanel<String> cbpDefaultFontSize = new FComboBoxPanel<>(localizer.getMessage("cbpDefaultFontSize")+":");
    private final FComboBoxPanel<String> cbpCardArtFormat = new FComboBoxPanel<>(localizer.getMessage("cbpCardArtFormat")+":");
    private final FComboBoxPanel<String> cbpCardArtPreference = new FComboBoxPanel<>(localizer.getMessage("lblPreferredArt")+":");
    private final FComboBoxPanel<String> cbpMulliganRule = new FComboBoxPanel<>(localizer.getMessage("cbpMulliganRule")+":");
    private final FComboBoxPanel<String> cbpSoundSets = new FComboBoxPanel<>(localizer.getMessage("cbpSoundSets")+":");
    private final FComboBoxPanel<String> cbpMusicSets = new FComboBoxPanel<>(localizer.getMessage("cbpMusicSets")+":");
    private final FComboBoxPanel<String> cbpAiProfiles = new FComboBoxPanel<>(localizer.getMessage("cbpAiProfiles")+":");
    private final FComboBoxPanel<String> cbpAiSideboardingMode = new FComboBoxPanel<>(localizer.getMessage("cbpAiSideboardingMode")+":");
    private final FComboBoxPanel<String> cbpStackAdditions = new FComboBoxPanel<>(localizer.getMessage("cbpStackAdditions")+":");
    private final FComboBoxPanel<String> cbpLandPlayed = new FComboBoxPanel<>(localizer.getMessage("cbpLandPlayed")+":");
    private final FComboBoxPanel<String> cbpDisplayCurrentCardColors = new FComboBoxPanel<>(localizer.getMessage("cbpDisplayCurrentCardColors")+":");
    private final FComboBoxPanel<String> cbpAutoYieldMode = new FComboBoxPanel<>(localizer.getMessage("cbpAutoYieldMode")+":");
    private final FComboBoxPanel<String> cbpCounterDisplayType = new FComboBoxPanel<>(localizer.getMessage("cbpCounterDisplayType")+":");
    private final FComboBoxPanel<String> cbpCounterDisplayLocation =new FComboBoxPanel<>(localizer.getMessage("cbpCounterDisplayLocation")+":");
    private final FComboBoxPanel<String> cbpGraveyardOrdering = new FComboBoxPanel<>(localizer.getMessage("cbpGraveyardOrdering")+":");
    private final FComboBoxPanel<String> cbpDefaultLanguage = new FComboBoxPanel<>(localizer.getMessage("cbpSelectLanguage")+":");
    private final FComboBoxPanel<String> cbpAutoUpdater = new FComboBoxPanel<>(localizer.getMessage("cbpAutoUpdater")+":");
    private final FComboBoxPanel<String> cbpSwitchStates = new FComboBoxPanel<>(localizer.getMessage("cbpSwitchStates")+":");

    /**
     * Constructor.
     */
    VSubmenuPreferences() {

        pnlPrefs.setOpaque(false);
        pnlPrefs.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        // Spacing between components is defined here.
        final String sectionConstraints = "w 80%!, h 42px!, gap 25px 0 80px 20px, span 2 1";
        final String titleConstraints = "w 80%!, h 22px!, gap 25px 0 0 0px, span 2 1";
        final String comboBoxConstraints = "w 80%!, h 25px!, gap 25px 0 0 0px, span 2 1";
        final String descriptionConstraints = "w 80%!, h 22px!, gap 28px 0 0 20px, span 2 1";

        // Troubleshooting
        pnlPrefs.add(new SectionLabel(localizer.getMessage("Troubleshooting")), sectionConstraints);

        // Reset buttons
        final String twoButtonConstraints1 = "w 38%!, h 30px!, gap 25px 0 0 10px";
        final String twoButtonConstraints2 = "w 38%!, h 30px!, gap 0 0 0 10px";
        pnlPrefs.add(btnReset, twoButtonConstraints1);
        pnlPrefs.add(btnDeleteMatchUI, twoButtonConstraints2);
        pnlPrefs.add(btnDeleteEditorUI, twoButtonConstraints1);
        pnlPrefs.add(btnDeleteWorkshopUI, twoButtonConstraints2);
        pnlPrefs.add(btnUserProfileUI, twoButtonConstraints1);
        pnlPrefs.add(btnContentDirectoryUI, twoButtonConstraints2);
        pnlPrefs.add(btnClearImageCache, twoButtonConstraints1);
        pnlPrefs.add(btnTokenPreviewer, twoButtonConstraints2);

        // General Configuration
        pnlPrefs.add(new SectionLabel(localizer.getMessage("GeneralConfiguration")), sectionConstraints);

        // language

        pnlPrefs.add(cbpAutoUpdater, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlAutoUpdater")), descriptionConstraints);

        pnlPrefs.add(cbpDefaultLanguage, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlSelectLanguage")), descriptionConstraints);

        pnlPrefs.add(getPlayerNamePanel(), titleConstraints + ", h 26px!");
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlPlayerName")), descriptionConstraints);

        pnlPrefs.add(cbCompactMainMenu, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlCompactMainMenu")), descriptionConstraints);

        pnlPrefs.add(cbUseSentry, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlUseSentry")), descriptionConstraints);

        pnlPrefs.add(btnResetJavaFutureCompatibilityWarnings, "w 300px!, h 30px!, gap 27px 0 0 20px, span 2 1");

        // Gameplay Options
        pnlPrefs.add(new SectionLabel(localizer.getMessage("GamePlay")), sectionConstraints);

        pnlPrefs.add(cbpMulliganRule, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlpMulliganRule")), descriptionConstraints);

        pnlPrefs.add(cbpAiProfiles, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlpAiProfiles")), descriptionConstraints);

        pnlPrefs.add(cbAnte, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlAnte")), descriptionConstraints);

        pnlPrefs.add(cbAnteMatchRarity, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlAnteMatchRarity")), descriptionConstraints);

        pnlPrefs.add(cbEnableAICheats, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlEnableAICheats")), descriptionConstraints);

        pnlPrefs.add(cbManaBurn, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlManaBurn")), descriptionConstraints);

        pnlPrefs.add(cbManaLostPrompt, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlManaLostPrompt")), descriptionConstraints);

        pnlPrefs.add(cbpStackAdditions, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlpStackAdditions")), descriptionConstraints);

        pnlPrefs.add(cbpLandPlayed, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlpLandPlayed")), descriptionConstraints);
        
        pnlPrefs.add(cbEnforceDeckLegality, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlEnforceDeckLegality")), descriptionConstraints);

        pnlPrefs.add(cbPerformanceMode, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlPerformanceMode")), descriptionConstraints);

        pnlPrefs.add(cbpAiSideboardingMode, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlpAiSideboardingMode")), descriptionConstraints);

        pnlPrefs.add(cbExperimentalRestore, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlExperimentalRestore")), descriptionConstraints);

        pnlPrefs.add(cbFilteredHands, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlFilteredHands")), descriptionConstraints);

        pnlPrefs.add(cbCloneImgSource, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlCloneImgSource")), descriptionConstraints);

        pnlPrefs.add(cbPromptFreeBlocks, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlPromptFreeBlocks")), descriptionConstraints);

        pnlPrefs.add(cbPauseWhileMinimized, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlPauseWhileMinimized")), descriptionConstraints);

        pnlPrefs.add(cbEscapeEndsTurn, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlEscapeEndsTurn")), descriptionConstraints);

        pnlPrefs.add(cbDetailedPaymentDesc, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlDetailedPaymentDesc")), descriptionConstraints);

        pnlPrefs.add(cbGrayText, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlGrayText")), descriptionConstraints);

        pnlPrefs.add(cbShowStormCount, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlShowStormCount")), descriptionConstraints);

        pnlPrefs.add(cbRemindOnPriority, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlRemindOnPriority")), descriptionConstraints);

        pnlPrefs.add(cbPreselectPrevAbOrder, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlPreselectPrevAbOrder")), descriptionConstraints);

        pnlPrefs.add(cbpGraveyardOrdering, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlpGraveyardOrdering")), descriptionConstraints);

        pnlPrefs.add(cbpAutoYieldMode, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlpAutoYieldMode")), descriptionConstraints);

        // Deck building options
        pnlPrefs.add(new SectionLabel(localizer.getMessage("RandomDeckGeneration")), sectionConstraints);

        pnlPrefs.add(cbRemoveSmall, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlRemoveSmall")), descriptionConstraints);

        pnlPrefs.add(cbSingletons, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlSingletons")), descriptionConstraints);

        pnlPrefs.add(cbRemoveArtifacts, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlRemoveArtifacts")), descriptionConstraints);

        pnlPrefs.add(cbCardBased, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlCardBased")), descriptionConstraints);

        // Deck Editor options
        pnlPrefs.add(new SectionLabel(localizer.getMessage("DeckEditorOptions")), sectionConstraints);

        pnlPrefs.add(cbFilterLandsByColorId, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlFilterLandsByColorId")), descriptionConstraints);

        pnlPrefs.add(cbpCardArtPreference, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlPreferredArt")), descriptionConstraints);

        pnlPrefs.add(cbCardArtCoreExpansionsOnlyOpt, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlPrefArtExpansionOnly")), descriptionConstraints);

        pnlPrefs.add(cbSmartCardArtSelectionOpt, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlSmartCardArtOpt")), "w 80%!, h 22px!, gap 28px 0 0 0, span 2 1");
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlSmartCardArtOptNote")), descriptionConstraints);

        //Draft Ranking Overlay
        pnlPrefs.add(cbShowDraftRanking, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlShowDraftRankingOverlay")), descriptionConstraints);

        // Advanced
        pnlPrefs.add(new SectionLabel(localizer.getMessage("AdvancedSettings")), sectionConstraints);

        pnlPrefs.add(cbDevMode, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlDevMode")), descriptionConstraints);

        pnlPrefs.add(cbWorkshopSyntax, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlWorkshopSyntax")), descriptionConstraints);

        pnlPrefs.add(cbpGameLogEntryType, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlGameLogEntryType")), descriptionConstraints);

        pnlPrefs.add(cbpCloseAction, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlCloseAction")), descriptionConstraints);

        pnlPrefs.add(cbLoadCardsLazily, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlLoadCardsLazily")), descriptionConstraints);

        pnlPrefs.add(cbLoadArchivedFormats, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlLoadArchivedFormats")), descriptionConstraints);

        pnlPrefs.add(cbEnableUnknownCards, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlEnableUnknownCards")), descriptionConstraints);

        pnlPrefs.add(cbEnableNonLegalCards, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlEnableNonLegalCards")), descriptionConstraints);

        pnlPrefs.add(cbAllowCustomCardsDeckConformance, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlAllowCustomCardsInDecks")), descriptionConstraints);

        pnlPrefs.add(cbUseExperimentalNetworkStream, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlExperimentalNetworkCompatibility")), descriptionConstraints);

        // Graphic Options
        pnlPrefs.add(new SectionLabel(localizer.getMessage("GraphicOptions")), sectionConstraints + ", gaptop 2%");

        pnlPrefs.add(cbpDefaultFontSize, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlDefaultFontSize")), descriptionConstraints);

        pnlPrefs.add(cbpCardArtFormat, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlCardArtFormat")), descriptionConstraints);

        pnlPrefs.add(cbImageFetcher, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlImageFetcher")), descriptionConstraints);

        pnlPrefs.add(cbDisableCardImages, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlDisableCardImages")), descriptionConstraints);

        pnlPrefs.add(cbDisplayFoil, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlDisplayFoil")), descriptionConstraints);

        pnlPrefs.add(cbRandomFoil, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlRandomFoil")), descriptionConstraints);

        pnlPrefs.add(cbScaleLarger, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlScaleLarger")), descriptionConstraints);

        pnlPrefs.add(cbRenderBlackCardBorders, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlRenderBlackCardBorders")), descriptionConstraints);

        pnlPrefs.add(cbLargeCardViewers, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlLargeCardViewers")), descriptionConstraints);

        pnlPrefs.add(cbSmallDeckViewer, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlSmallDeckViewer")), descriptionConstraints);

        pnlPrefs.add(cbRandomArtInPools, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlRandomArtInPools")), descriptionConstraints);

        pnlPrefs.add(cbUiForTouchScreen, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlUiForTouchScreen")), descriptionConstraints);

        pnlPrefs.add(cbCompactPrompt, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlCompactPrompt")), descriptionConstraints);

        /*pnlPrefs.add(cbStackCardView, titleConstraints); TODO: Show this checkbox when setting can support being enabled
        pnlPrefs.add(new NoteLabel("Show cards and abilities on Stack in card view rather than list view."), descriptionConstraints);*/

        pnlPrefs.add(cbHideReminderText, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlHideReminderText")), descriptionConstraints);

        pnlPrefs.add(cbCardTextUseSansSerif, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlCardTextUseSansSerif")), descriptionConstraints);

        pnlPrefs.add(cbCardTextHideReminder, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlCardTextHideReminder")), descriptionConstraints);

        pnlPrefs.add(cbOpenPacksIndiv, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlOpenPacksIndiv")), descriptionConstraints);

        pnlPrefs.add(cbTokensInSeparateRow, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlTokensInSeparateRow")), descriptionConstraints);

        pnlPrefs.add(cbStackCreatures, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlStackCreatures")), descriptionConstraints);

        pnlPrefs.add(cbTimedTargOverlay, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlTimedTargOverlay")), descriptionConstraints);

        pnlPrefs.add(cbpCounterDisplayType, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlCounterDisplayType")), descriptionConstraints);

        pnlPrefs.add(cbpCounterDisplayLocation, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlCounterDisplayLocation")), descriptionConstraints);

        pnlPrefs.add(cbpDisplayCurrentCardColors, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlDisplayCurrentCardColors")), descriptionConstraints);

        pnlPrefs.add(cbpSwitchStates, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlSwitchStates")), descriptionConstraints);

        // Sound options
        pnlPrefs.add(new SectionLabel(localizer.getMessage("SoundOptions")), sectionConstraints + ", gaptop 2%");

        pnlPrefs.add(cbEnableSounds, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlEnableSounds")), descriptionConstraints);

        pnlPrefs.add(cbpSoundSets, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlpSoundSets")), descriptionConstraints);

        pnlPrefs.add(cbEnableMusic, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlEnableMusic")), descriptionConstraints);

        pnlPrefs.add(cbpMusicSets, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlpMusicSets")), descriptionConstraints);

        pnlPrefs.add(cbAltSoundSystem, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlAltSoundSystem")), descriptionConstraints);
        pnlPrefs.add(cbSROptimize, titleConstraints);
        pnlPrefs.add(new NoteLabel(localizer.getMessage("nlSrOptimize")), descriptionConstraints);
        // Keyboard shortcuts
        pnlPrefs.add(new SectionLabel(localizer.getMessage("KeyboardShortcuts")), sectionConstraints);

        final List<Shortcut> shortcuts = KeyboardShortcuts.getKeyboardShortcuts();

        for (final Shortcut s : shortcuts) {
            pnlPrefs.add(new FLabel.Builder().text(s.getDescription())
                    .fontAlign(SwingConstants.RIGHT).build(), "w 50%!, h 22px!, gap 0 2% 0 20px");
            KeyboardShortcutField field = new KeyboardShortcutField(s);
            pnlPrefs.add(field, "w 25%!");
            shortcutFields.put(s.getPrefKey(), field);
        }
    }

    public void reloadShortcuts() {
        for (Map.Entry<FPref, KeyboardShortcutField> e : shortcutFields.entrySet()) {
            e.getValue().reload(e.getKey());
        }
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrContent, "w 98%!, h 98%!, gap 1% 0 1% 0");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SETTINGS;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return localizer.getMessage("Preferences");    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_PREFERENCES;
    }

    /** Consolidates checkbox styling in one place. */
    @SuppressWarnings("serial")
    private final class OptionsCheckBox extends FCheckBox {
        private OptionsCheckBox(final String txt0) {
            super(txt0);
            this.setFont(FSkin.getBoldFont());
        }
    }

    /** Consolidates section title label styling in one place. */
    @SuppressWarnings("serial")
    private final class SectionLabel extends SkinnedLabel {
        private SectionLabel(final String txt0) {
            super(txt0);
            this.setBorder(new FSkin.MatteSkinBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
            setHorizontalAlignment(SwingConstants.CENTER);
            this.setFont(FSkin.getRelativeBoldFont(16));
            this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }

    /** Consolidates notation label styling in one place. */
    @SuppressWarnings("serial")
    private final class NoteLabel extends SkinnedLabel {
        private NoteLabel(final String txt0) {
            super(txt0);
            this.setFont(FSkin.getItalicFont());
            this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }

    /**
     * A FTextField plus a "codeString" property, that stores keycodes for the
     * shortcut. Also, an action listener that handles translation of keycodes
     * into characters and (dis)assembly of keycode stack.
     */
    @SuppressWarnings("serial")
    public class KeyboardShortcutField extends SkinnedTextField {
        private String codeString;

        /**
         * A JTextField plus a "codeString" property, that stores keycodes for
         * the shortcut. Also, an action listener that handles translation of
         * keycodes into characters and (dis)assembly of keycode stack.
         *
         * @param shortcut0 &emsp; Shortcut object
         */
        public KeyboardShortcutField(final Shortcut shortcut0) {
            super();
            this.setEditable(false);
            this.setFont(FSkin.getRelativeFont(14));
            final FPref prefKey = shortcut0.getPrefKey();
            reload(prefKey);

            this.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(final KeyEvent evt) {
                    KeyboardShortcuts.addKeyCode(evt);
                }
            });

            this.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(final FocusEvent evt) {
                    KeyboardShortcutField.this.setBackground(FSkin.getColor(FSkin.Colors.CLR_ACTIVE));
                }

                @Override
                public void focusLost(final FocusEvent evt) {
                    FModel.getPreferences().setPref(prefKey, getCodeString());
                    FModel.getPreferences().save();
                    shortcut0.attach();
                    KeyboardShortcutField.this.setBackground(Color.white);
                }
            });
        }

        public void reload(FPref prefKey) {
            this.setCodeString(FModel.getPreferences().getPref(prefKey));
        }

        /**
         * Gets the code string.
         *
         * @return String
         */
        public final String getCodeString() {
            return this.codeString;
        }

        /**
         * Sets the code string.
         *
         * @param str0
         *            &emsp; The new code string (space delimited)
         */
        public final void setCodeString(final String str0) {
            if ("null".equals(str0)) {
                return;
            }

            this.codeString = str0.trim();

            final List<String> codes = new ArrayList<>(Arrays.asList(this.codeString.split(" ")));
            final List<String> displayText = new ArrayList<>();

            for (final String s : codes) {
                if (!s.isEmpty()) {
                    displayText.add(KeyEvent.getKeyText(Integer.valueOf(s)));
                }
            }

            this.setText(StringUtils.join(displayText, ' '));
        }
    }

    public final FComboBoxPanel<String> getCbpAutoUpdater() {
        return cbpAutoUpdater;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public final JCheckBox getCbCompactMainMenu() {
        return cbCompactMainMenu;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public final JCheckBox getCbUseSentry() {
        return cbUseSentry;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public final JCheckBox getCbRemoveSmall() {
        return cbRemoveSmall;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public final JCheckBox getCbCardBased() {
        return cbCardBased;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public final JCheckBox getCbSingletons() {
        return cbSingletons;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRemoveArtifacts() {
        return cbRemoveArtifacts;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbFilterLandsByColorId() {
        return cbFilterLandsByColorId;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbEnableAICheats() {
        return cbEnableAICheats;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbEnableUnknownCards() {
        return cbEnableUnknownCards;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbEnableNonLegalCards() {
        return cbEnableNonLegalCards;
    }
    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbAllowCustomCardsDeckConformance() {
        return cbAllowCustomCardsDeckConformance;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbUseExperimentalNetworkStream() {
        return cbUseExperimentalNetworkStream;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbImageFetcher() {
        return cbImageFetcher;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbDisableCardImages() {
        return cbDisableCardImages;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbDisplayFoil() {
        return cbDisplayFoil;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRandomFoil() {
        return cbRandomFoil;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbAnte() {
        return cbAnte;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbAnteMatchRarity() {
        return cbAnteMatchRarity;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbManaBurn() {
        return cbManaBurn;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbScaleLarger() {
        return cbScaleLarger;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRenderBlackCardBorders() {
        return cbRenderBlackCardBorders;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbLargeCardViewers() {
        return cbLargeCardViewers;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbSmallDeckViewer() {
        return cbSmallDeckViewer;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRandomArtInPools() {
        return cbRandomArtInPools;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbDevMode() {
        return cbDevMode;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbLoadCardsLazily() {
        return cbLoadCardsLazily;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbLoadArchivedFormats() {
        return cbLoadArchivedFormats;
    }

    public JCheckBox getCbWorkshopSyntax() {
        return cbWorkshopSyntax;
    }

    public FComboBoxPanel<String> getCbpMulliganRule() {
        return cbpMulliganRule;
    }

    public FComboBoxPanel<String> getSoundSetsComboBoxPanel() {
        return cbpSoundSets;
    }

    public FComboBoxPanel<String> getMusicSetsComboBoxPanel() {
        return cbpMusicSets;
    }

    public FComboBoxPanel<String> getAiProfilesComboBoxPanel() {
        return cbpAiProfiles;
    }

    public FComboBoxPanel<String> getAiSideboardingModeComboBoxPanel() {
        return cbpAiSideboardingMode;
    }

    public FComboBoxPanel<String> getCbpStackAdditionsComboBoxPanel() {
        return cbpStackAdditions;
    }
  
    public FComboBoxPanel<String> getCbpLandPlayedComboBoxPanel() {
        return cbpLandPlayed;
    }
    
    public FComboBoxPanel<GameLogEntryType> getGameLogVerbosityComboBoxPanel() {
        return cbpGameLogEntryType;
    }

    public FComboBoxPanel<String> getDisplayColorIdentity() {
        return cbpDisplayCurrentCardColors;
    }

    public FComboBoxPanel<String> getSwitchStates() {
        return cbpSwitchStates;
    }

    public FComboBoxPanel<CloseAction> getCloseActionComboBoxPanel() {
        return cbpCloseAction;
    }

    public FComboBoxPanel<String> getCbpDefaultFontSizeComboBoxPanel() {
        return cbpDefaultFontSize;
    }

    public FComboBoxPanel<String> getCbpCardArtFormatComboBoxPanel() {
        return cbpCardArtFormat;
    }

    public FComboBoxPanel<String> getCbpDefaultLanguageComboBoxPanel() {
        return cbpDefaultLanguage;
    }


    public FComboBoxPanel<String> getAutoYieldModeComboBoxPanel() {
        return cbpAutoYieldMode;
    }

    public FComboBoxPanel<String> getCounterDisplayTypeComboBoxPanel() {
        return cbpCounterDisplayType;
    }

    public FComboBoxPanel<String> getCounterDisplayLocationComboBoxPanel() {
        return cbpCounterDisplayLocation;
    }

    public FComboBoxPanel<String> getCbpCardArtPreference() {
        return cbpCardArtPreference;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbCardArtCoreExpansionsOnlyOpt() { return cbCardArtCoreExpansionsOnlyOpt; }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbSmartCardArtSelectionOpt() { return cbSmartCardArtSelectionOpt; }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbShowDraftRanking() { return cbShowDraftRanking; }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbEnforceDeckLegality() {
        return cbEnforceDeckLegality;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbPerformanceMode() {
        return cbPerformanceMode;
    }

    public JCheckBox getCbExperimentalRestore() {
        return cbExperimentalRestore;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbFilteredHands() {
        return cbFilteredHands;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbCloneImgSource() {
        return cbCloneImgSource;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbPromptFreeBlocks() {
        return cbPromptFreeBlocks;
    }

    public JCheckBox getCbPauseWhileMinimized() {
        return cbPauseWhileMinimized;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbEnableSounds() {
        return cbEnableSounds;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbEnableMusic() {
        return cbEnableMusic;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbAltSoundSystem() {
        return cbAltSoundSystem;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbSROptimize() {
        return cbSROptimize;
    }
    
    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbTimedTargOverlay() {
        return cbTimedTargOverlay;
    }

    public final JCheckBox getCbUiForTouchScreen() {
        return cbUiForTouchScreen;
    }

    public final JCheckBox getCbCompactPrompt() {
        return cbCompactPrompt;
    }

    public final JCheckBox getCbEscapeEndsTurn() {
        return cbEscapeEndsTurn;
    }

    public final JCheckBox getCbHideReminderText() {
        return cbHideReminderText;
    }

    public JCheckBox getCbCardTextUseSansSerif() {
        return cbCardTextUseSansSerif;
    }

    public final JCheckBox getCbCardTextHideReminder() {
        return cbCardTextHideReminder;
    }

    public final JCheckBox getCbOpenPacksIndiv() {
        return cbOpenPacksIndiv;
    }

    public final JCheckBox getCbTokensInSeparateRow() {
        return cbTokensInSeparateRow;
    }

    public final JCheckBox getCbStackCreatures() {
        return cbStackCreatures;
    }

    public final JCheckBox getCbManaLostPrompt() {
    	return cbManaLostPrompt;
    }
    
    public final JCheckBox getCbDetailedPaymentDesc() {
        return cbDetailedPaymentDesc;
    }

    public final JCheckBox getCbGrayText() {
        return cbGrayText;
    }

    public final JCheckBox getCbShowStormCount() {
        return cbShowStormCount;
    }

    public final JCheckBox getCbRemindOnPriority() { return cbRemindOnPriority; }

    public final JCheckBox getCbPreselectPrevAbOrder() {
        return cbPreselectPrevAbOrder;
    }

    public final FComboBoxPanel<String> getCbpGraveyardOrdering() {
        return cbpGraveyardOrdering;
    }

    /** @return {@link forge.toolbox.FLabel} */
    public FLabel getBtnReset() {
        return btnReset;
    }

    public FLabel getBtnPlayerName() {
        return btnPlayerName;
    }

    //========== Overridden from IVDoc

    public final FLabel getBtnDeleteMatchUI() {
        return btnDeleteMatchUI;
    }

    public final FLabel getBtnDeleteEditorUI() {
        return btnDeleteEditorUI;
    }

    public final FLabel getBtnDeleteWorkshopUI() {
        return btnDeleteWorkshopUI;
    }

    public final FLabel getBtnContentDirectoryUI() { return btnContentDirectoryUI; }

    public final FLabel getBtnUserProfileUI() { return btnUserProfileUI; }

    public final FLabel getBtnClearImageCache() { return btnClearImageCache; }
    public final FLabel getBtnTokenPreviewer() { return btnTokenPreviewer; }

    public final FLabel getBtnResetJavaFutureCompatibilityWarnings() {
        return btnResetJavaFutureCompatibilityWarnings;
    }

    /* (non-Javadoc)
		 * @see forge.gui.framework.IVDoc#getDocumentID()
		 */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_PREFERENCES;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CSubmenuPreferences getLayoutControl() {
        return CSubmenuPreferences.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    private JPanel getPlayerNamePanel() {
        JPanel p = new JPanel(new MigLayout("insets 0, gap 0!"));
        p.setOpaque(false);
        FLabel lbl = new FLabel.Builder().text(localizer.getMessage("lblPlayerName") +": ").fontSize(12).fontStyle(Font.BOLD).build();
        p.add(lbl, "aligny top, h 100%, gap 4px 0 0 0");
        p.add(btnPlayerName, "aligny top, h 100%, w 200px!");
        return p;
    }
}
