package forge.screens.home.settings;

import forge.control.FControl.CloseAction;
import forge.control.KeyboardShortcuts;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.game.GameLogEntryType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.*;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.toolbox.FSkin.SkinnedTextField;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * Assembles Swing components of preferences submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuPreferences implements IVSubmenu<CSubmenuPreferences> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Preferences");

    /** */
    private final JPanel pnlPrefs = new JPanel();
    private final FScrollPane scrContent = new FScrollPane(pnlPrefs, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    private final FLabel btnReset = new FLabel.Builder().opaque(true).hoverable(true).text("Reset to Default Settings").build();
    private final FLabel btnDeleteMatchUI = new FLabel.Builder().opaque(true).hoverable(true).text("Reset Match Layout").build();
    private final FLabel btnDeleteEditorUI = new FLabel.Builder().opaque(true).hoverable(true).text("Reset Editor Layout").build();
    private final FLabel btnDeleteWorkshopUI = new FLabel.Builder().opaque(true).hoverable(true).text("Reset Workshop Layout").build();
    private final FLabel btnUserProfileUI = new FLabel.Builder().opaque(true).hoverable(true).text("Open User Directory").build();
    private final FLabel btnContentDirectoryUI = new FLabel.Builder().opaque(true).hoverable(true).text("Open Content Directory").build();
    private final FLabel btnResetJavaFutureCompatibilityWarnings = new FLabel.Builder().opaque(true).hoverable(true).text("Reset Java Compatibility Warnings").build();
    private final FLabel btnPlayerName = new FLabel.Builder().opaque(true).hoverable(true).text("").build();

    private final JCheckBox cbRemoveSmall = new OptionsCheckBox("Remove Small Creatures");
    private final JCheckBox cbSingletons = new OptionsCheckBox("Singleton Mode");
    private final JCheckBox cbRemoveArtifacts = new OptionsCheckBox("Remove Artifacts");
    private final JCheckBox cbAnte = new OptionsCheckBox("Play for Ante");
    private final JCheckBox cbAnteMatchRarity = new OptionsCheckBox("Match Ante Rarity");
    private final JCheckBox cbEnableAICheats = new OptionsCheckBox("Allow AI Cheating");
    private final JCheckBox cbManaBurn = new OptionsCheckBox("Mana Burn");
    private final JCheckBox cbManaLostPrompt = new OptionsCheckBox("Prompt Mana Pool Emptying");
    private final JCheckBox cbDevMode = new OptionsCheckBox("Developer Mode");
    private final JCheckBox cbLoadCardsLazily = new OptionsCheckBox("Load Card Scripts Lazily");
    private final JCheckBox cbWorkshopSyntax = new OptionsCheckBox("Workshop Syntax Checker");
    private final JCheckBox cbEnforceDeckLegality = new OptionsCheckBox("Deck Conformance");
    private final JCheckBox cbImageFetcher = new OptionsCheckBox("Automatically Download Missing Card Art");
    private final JCheckBox cbCloneImgSource = new OptionsCheckBox("Clones Use Original Card Art");
    private final JCheckBox cbScaleLarger = new OptionsCheckBox("Scale Image Larger");
    private final JCheckBox cbRenderBlackCardBorders = new OptionsCheckBox("Render Black Card Borders");
    private final JCheckBox cbLargeCardViewers = new OptionsCheckBox("Use Large Card Viewers");
    private final JCheckBox cbSmallDeckViewer = new OptionsCheckBox("Use Small Deck Viewer");
    private final JCheckBox cbDisplayFoil = new OptionsCheckBox("Display Foil Overlay");
    private final JCheckBox cbRandomFoil = new OptionsCheckBox("Random Foil");
    private final JCheckBox cbRandomArtInPools = new OptionsCheckBox("Randomize Card Art in Generated Card Pools");
    private final JCheckBox cbEnableSounds = new OptionsCheckBox("Enable Sounds");
    private final JCheckBox cbEnableMusic = new OptionsCheckBox("Enable Music");
    private final JCheckBox cbAltSoundSystem = new OptionsCheckBox("Use Alternate Sound System");
    private final JCheckBox cbUiForTouchScreen = new OptionsCheckBox("Enhance UI for Touchscreens");
    private final JCheckBox cbTimedTargOverlay = new OptionsCheckBox("Enable Targeting Overlay Optimization");
    private final JCheckBox cbCompactMainMenu = new OptionsCheckBox("Use Compact Main Sidebar Menu");
    private final JCheckBox cbDetailedPaymentDesc = new OptionsCheckBox("Spell Description in Payment Prompt");
    private final JCheckBox cbPromptFreeBlocks = new OptionsCheckBox("Free Block Handling");
    private final JCheckBox cbPauseWhileMinimized = new OptionsCheckBox("Pause While Minimized");
    private final JCheckBox cbCompactPrompt = new OptionsCheckBox("Compact Prompt");
    private final JCheckBox cbEscapeEndsTurn = new OptionsCheckBox("Use Escape Key to End Turn");
    private final JCheckBox cbPreselectPrevAbOrder = new OptionsCheckBox("Preselect Last Order of Abilities");
    private final JCheckBox cbHideReminderText = new OptionsCheckBox("Hide Reminder Text");
    private final JCheckBox cbOpenPacksIndiv = new OptionsCheckBox("Open Packs Individually");
    private final JCheckBox cbTokensInSeparateRow = new OptionsCheckBox("Display Tokens in a Separate Row");
    private final JCheckBox cbStackCreatures = new OptionsCheckBox("Stack Creatures");
    private final JCheckBox cbFilterLandsByColorId = new OptionsCheckBox("Filter Lands by Color in Activated Abilities");
    private final JCheckBox cbShowStormCount = new OptionsCheckBox("Show Storm Count in Prompt Pane");

    private final Map<FPref, KeyboardShortcutField> shortcutFields = new HashMap<>();

    // ComboBox items are added in CSubmenuPreferences since this is just the View.
    private final FComboBoxPanel<GameLogEntryType> cbpGameLogEntryType = new FComboBoxPanel<>("Game Log Verbosity:");
    private final FComboBoxPanel<CloseAction> cbpCloseAction = new FComboBoxPanel<>("Close Action:");
    private final FComboBoxPanel<String> cbpAiProfiles = new FComboBoxPanel<>("AI Personality:");
    private final FComboBoxPanel<String> cbpDisplayCurrentCardColors = new FComboBoxPanel<>("Show Detailed Card Color:");
    private final FComboBoxPanel<String> cbpAutoYieldMode = new FComboBoxPanel<>("Auto-Yield:");
    private final FComboBoxPanel<String> cbpCounterDisplayType = new FComboBoxPanel<>("Counter Display Type:");
    private final FComboBoxPanel<String> cbpCounterDisplayLocation = new FComboBoxPanel<>("Counter Display Location:");

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
        pnlPrefs.add(new SectionLabel("Troubleshooting"), sectionConstraints);

        // Reset buttons
        final String twoButtonConstraints1 = "w 38%!, h 30px!, gap 25px 0 0 10px";
        final String twoButtonConstraints2 = "w 38%!, h 30px!, gap 0 0 0 10px";
        pnlPrefs.add(btnReset, twoButtonConstraints1);
        pnlPrefs.add(btnDeleteMatchUI, twoButtonConstraints2);
        pnlPrefs.add(btnDeleteEditorUI, twoButtonConstraints1);
        pnlPrefs.add(btnDeleteWorkshopUI, twoButtonConstraints2);
        pnlPrefs.add(btnUserProfileUI, twoButtonConstraints1);
        pnlPrefs.add(btnContentDirectoryUI, twoButtonConstraints2);

        // General Configuration
        pnlPrefs.add(new SectionLabel("General Configuration"), sectionConstraints);

        pnlPrefs.add(getPlayerNamePanel(), titleConstraints + ", h 26px!");
        pnlPrefs.add(new NoteLabel("Sets the name that you will be referred to by Forge during gameplay."), descriptionConstraints);

        pnlPrefs.add(cbCompactMainMenu, titleConstraints);
        pnlPrefs.add(new NoteLabel("Enable for a space efficient sidebar that displays only one menu group at a time (RESTART REQUIRED)."), descriptionConstraints);

        pnlPrefs.add(btnResetJavaFutureCompatibilityWarnings, "w 300px!, h 30px!, gap 27px 0 0 20px, span 2 1");

        // Gameplay Options
        pnlPrefs.add(new SectionLabel("Gameplay"), sectionConstraints);

        pnlPrefs.add(cbpAiProfiles, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel("Choose your AI opponent."), descriptionConstraints);

        pnlPrefs.add(cbAnte, titleConstraints);
        pnlPrefs.add(new NoteLabel("Determines whether or not the game is played for ante."), descriptionConstraints);

        pnlPrefs.add(cbAnteMatchRarity, titleConstraints);
        pnlPrefs.add(new NoteLabel("Attempts to make antes the same rarity for all players."), descriptionConstraints);

        pnlPrefs.add(cbEnableAICheats, titleConstraints);
        pnlPrefs.add(new NoteLabel("Allow the AI to cheat to gain advantage (for personalities that have cheat shuffling options set)."), descriptionConstraints);

        pnlPrefs.add(cbManaBurn, titleConstraints);
        pnlPrefs.add(new NoteLabel("Play with mana burn (from pre-Magic 2010 rules)."), descriptionConstraints);

        pnlPrefs.add(cbManaLostPrompt, titleConstraints);
        pnlPrefs.add(new NoteLabel("When enabled, you get a warning if passing priority would cause you to lose mana in your mana pool."), descriptionConstraints);

        pnlPrefs.add(cbEnforceDeckLegality, titleConstraints);
        pnlPrefs.add(new NoteLabel("Enforces deck legality relevant to each environment (minimum deck sizes, max card count etc)."), descriptionConstraints);

        pnlPrefs.add(cbCloneImgSource, titleConstraints);
        pnlPrefs.add(new NoteLabel("When enabled clones will use their original art instead of the cloned card's art."), descriptionConstraints);

        pnlPrefs.add(cbPromptFreeBlocks, titleConstraints);
        pnlPrefs.add(new NoteLabel("When enabled, if you would have to pay 0 to block, pay automatically without prompt."), descriptionConstraints);

        pnlPrefs.add(cbPauseWhileMinimized, titleConstraints);
        pnlPrefs.add(new NoteLabel("When enabled, Forge pauses when minimized (primarily for AI vs AI)."), descriptionConstraints);

        pnlPrefs.add(cbEscapeEndsTurn, titleConstraints);
        pnlPrefs.add(new NoteLabel("When enabled, Escape key functions as an alternative shortcut to end the current turn."), descriptionConstraints);

        pnlPrefs.add(cbDetailedPaymentDesc, titleConstraints);
        pnlPrefs.add(new NoteLabel("When enabled, detailed spell/ability descriptions are shown when choosing targets and paying costs."), descriptionConstraints);

        pnlPrefs.add(cbShowStormCount, titleConstraints);
        pnlPrefs.add(new NoteLabel("When enabled, displays the current storm count in the prompt pane."), descriptionConstraints);

        pnlPrefs.add(cbPreselectPrevAbOrder, titleConstraints);
        pnlPrefs.add(new NoteLabel("When enabled, preselects the last defined simultaneous ability order in the ordering dialog."), descriptionConstraints);

        pnlPrefs.add(cbpAutoYieldMode, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel("Defines the granularity level of auto-yields (per unique ability or per unique card)."), descriptionConstraints);

        // Deck building options
        pnlPrefs.add(new SectionLabel("Random Deck Generation"), sectionConstraints);

        pnlPrefs.add(cbRemoveSmall, titleConstraints);
        pnlPrefs.add(new NoteLabel("Disables 1/1 and 0/X creatures in generated decks."), descriptionConstraints);

        pnlPrefs.add(cbSingletons, titleConstraints);
        pnlPrefs.add(new NoteLabel("Disables non-land duplicates in generated decks."), descriptionConstraints);

        pnlPrefs.add(cbRemoveArtifacts, titleConstraints);
        pnlPrefs.add(new NoteLabel("Disables artifact cards in generated decks."), descriptionConstraints);

        // Deck building options
        pnlPrefs.add(new SectionLabel("Deck Editor Options"), sectionConstraints);

        pnlPrefs.add(cbFilterLandsByColorId, titleConstraints);
        pnlPrefs.add(new NoteLabel("When using card color filters, filter lands in a way to make it easier to find relevant mana producing lands."), descriptionConstraints);

        // Advanced
        pnlPrefs.add(new SectionLabel("Advanced Settings"), sectionConstraints);

        pnlPrefs.add(cbDevMode, titleConstraints);
        pnlPrefs.add(new NoteLabel("Enables menu with functions for testing during development."), descriptionConstraints);

        pnlPrefs.add(cbWorkshopSyntax, titleConstraints);
        pnlPrefs.add(new NoteLabel("Enables syntax checking of card scripts in the Workshop. Note: functionality still in testing phase!"), descriptionConstraints);

        pnlPrefs.add(cbpGameLogEntryType, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel("Changes how much information is displayed in the game log. Sorted by least to most verbose."), descriptionConstraints);

        pnlPrefs.add(cbpCloseAction, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel("Changes what happens when clicking the X button in the upper right."), descriptionConstraints);

        pnlPrefs.add(cbLoadCardsLazily, titleConstraints);
        pnlPrefs.add(new NoteLabel("If turned on, Forge will load card scripts as they're needed instead of at start up. (Warning: Experimental)"), descriptionConstraints);

        // Graphic Options
        pnlPrefs.add(new SectionLabel("Graphic Options"), sectionConstraints + ", gaptop 2%");

        pnlPrefs.add(cbImageFetcher, titleConstraints);
        pnlPrefs.add(new NoteLabel("Enables live fetching of missing card images from an online resource."), descriptionConstraints);

        pnlPrefs.add(cbDisplayFoil, titleConstraints);
        pnlPrefs.add(new NoteLabel("Displays foil cards with the visual foil overlay effect."), descriptionConstraints);

        pnlPrefs.add(cbRandomFoil, titleConstraints);
        pnlPrefs.add(new NoteLabel("Adds foil effect to random cards."), descriptionConstraints);

        pnlPrefs.add(cbScaleLarger, titleConstraints);
        pnlPrefs.add(new NoteLabel("Allows card pictures to be expanded larger than their original size."), descriptionConstraints);

        pnlPrefs.add(cbRenderBlackCardBorders, titleConstraints);
        pnlPrefs.add(new NoteLabel("Render black borders around card images."), descriptionConstraints);

        pnlPrefs.add(cbLargeCardViewers, titleConstraints);
        pnlPrefs.add(new NoteLabel("Makes all card viewers much larger for use with high resolution images. Will not fit on smaller screens."), descriptionConstraints);

        pnlPrefs.add(cbSmallDeckViewer, titleConstraints);
        pnlPrefs.add(new NoteLabel("Sets the deck viewer window to be 800x600 rather than a proportion of the screen size."), descriptionConstraints);

        pnlPrefs.add(cbRandomArtInPools, titleConstraints);
        pnlPrefs.add(new NoteLabel("Generates cards with random art in generated limited mode card pools."), descriptionConstraints);

        pnlPrefs.add(cbUiForTouchScreen, titleConstraints);
        pnlPrefs.add(new NoteLabel("Increases some UI elements to provide a better experience on touchscreen devices. (Needs restart)"), descriptionConstraints);

        pnlPrefs.add(cbCompactPrompt, titleConstraints);
        pnlPrefs.add(new NoteLabel("Hide header and use smaller font in Prompt pane to make it more compact."), descriptionConstraints);

        /*pnlPrefs.add(cbStackCardView, titleConstraints); TODO: Show this checkbox when setting can support being enabled
        pnlPrefs.add(new NoteLabel("Show cards and abilities on Stack in card view rather than list view."), descriptionConstraints);*/

        pnlPrefs.add(cbHideReminderText, titleConstraints);
        pnlPrefs.add(new NoteLabel("Hide reminder text in Card Detail pane."), descriptionConstraints);

        pnlPrefs.add(cbOpenPacksIndiv, titleConstraints);
        pnlPrefs.add(new NoteLabel("When opening Fat Packs and Booster Boxes, booster packs will be opened and displayed one at a time."), descriptionConstraints);

        pnlPrefs.add(cbTokensInSeparateRow, titleConstraints);
        pnlPrefs.add(new NoteLabel("Displays tokens in a separate row on the battlefield below the non-token creatures."), descriptionConstraints);

        pnlPrefs.add(cbStackCreatures, titleConstraints);
        pnlPrefs.add(new NoteLabel("Stacks identical creatures on the battlefield like lands, artifacts, and enchantments."), descriptionConstraints);

        pnlPrefs.add(cbTimedTargOverlay, titleConstraints);
        pnlPrefs.add(new NoteLabel("Enables throttling-based optimization of targeting overlay to reduce CPU use (only disable if you experience choppiness on older hardware, requires starting a new match)"), descriptionConstraints);

        pnlPrefs.add(cbpCounterDisplayType, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel("Selects the style of the in-game counter display for cards. Text-based is a new tab-like display on the cards. Image-based is the old counter image. Hybrid displays both at once."), descriptionConstraints);

        pnlPrefs.add(cbpCounterDisplayLocation, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel("Determines where to position the text-based counters on the card: close to the top or close to the bottom."), descriptionConstraints);

        pnlPrefs.add(cbpDisplayCurrentCardColors, comboBoxConstraints);
        pnlPrefs.add(new NoteLabel("Displays the breakdown of the current color of cards in the card detail information panel."), descriptionConstraints);

        // Sound options
        pnlPrefs.add(new SectionLabel("Sound Options"), sectionConstraints + ", gaptop 2%");

        pnlPrefs.add(cbEnableSounds, titleConstraints);
        pnlPrefs.add(new NoteLabel("Enable sound effects during the game."), descriptionConstraints);

        pnlPrefs.add(cbEnableMusic, titleConstraints);
        pnlPrefs.add(new NoteLabel("Enable background music during the game."), descriptionConstraints);

        pnlPrefs.add(cbAltSoundSystem, titleConstraints);
        pnlPrefs.add(new NoteLabel("Use the alternate sound system (only use if you have issues with sound not playing or disappearing)."), descriptionConstraints);


        // Keyboard shortcuts
        pnlPrefs.add(new SectionLabel("Keyboard Shortcuts"), sectionConstraints);

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
        return "Preferences";
    }

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
            this.setFont(FSkin.getBoldFont(12));
        }
    }

    /** Consolidates section title label styling in one place. */
    @SuppressWarnings("serial")
    private final class SectionLabel extends SkinnedLabel {
        private SectionLabel(final String txt0) {
            super(txt0);
            this.setBorder(new FSkin.MatteSkinBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
            setHorizontalAlignment(SwingConstants.CENTER);
            this.setFont(FSkin.getBoldFont(16));
            this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }

    /** Consolidates notation label styling in one place. */
    @SuppressWarnings("serial")
    private final class NoteLabel extends SkinnedLabel {
        private NoteLabel(final String txt0) {
            super(txt0);
            this.setFont(FSkin.getItalicFont(12));
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
            this.setFont(FSkin.getFont(14));
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

    /** @return {@link javax.swing.JCheckBox} */
    public final JCheckBox getCbCompactMainMenu() {
        return cbCompactMainMenu;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public final JCheckBox getCbRemoveSmall() {
        return cbRemoveSmall;
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
    public JCheckBox getCbImageFetcher() {
        return cbImageFetcher;
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

    public JCheckBox getCbWorkshopSyntax() {
        return cbWorkshopSyntax;
    }

    public FComboBoxPanel<String> getAiProfilesComboBoxPanel() {
        return cbpAiProfiles;
    }

    public FComboBoxPanel<GameLogEntryType> getGameLogVerbosityComboBoxPanel() {
        return cbpGameLogEntryType;
    }

    public FComboBoxPanel<String> getDisplayColorIdentity() {
        return cbpDisplayCurrentCardColors;
    }

    public FComboBoxPanel<CloseAction> getCloseActionComboBoxPanel() {
        return cbpCloseAction;
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

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbEnforceDeckLegality() {
        return cbEnforceDeckLegality;
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

    public final JCheckBox getCbShowStormCount() {
        return cbShowStormCount;
    }

    public final JCheckBox getCbPreselectPrevAbOrder() {
        return cbPreselectPrevAbOrder;
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
        FLabel lbl = new FLabel.Builder().text("Player Name: ").fontSize(12).fontStyle(Font.BOLD).build();
        p.add(lbl, "aligny top, h 100%, gap 4px 0 0 0");
        p.add(btnPlayerName, "aligny top, h 100%, w 200px!");
        return p;
    }
}
