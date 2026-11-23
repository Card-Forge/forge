package forge.screens.home.welcome;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import forge.Singletons;
import forge.gamemodes.tutorial.HowToPlay;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.screens.home.CHomeUI;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FRadioButton;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.view.FDialog;
import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
public final class WelcomeWizardDialog extends FDialog {

    public static void maybeShow() {
        if (!FModel.getPreferences().getPrefBoolean(FPref.WELCOME_SHOWN)) {
            new WelcomeWizardDialog().setVisible(true);
        }
    }

    public static void replay() {
        new WelcomeWizardDialog().setVisible(true);
    }

    private final Localizer L = Localizer.getInstance();
    private final ForgePreferences prefs = FModel.getPreferences();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardHost;
    private final List<WizardPage> pages = new ArrayList<>();
    private int currentIndex = 0;

    private FButton btnBack;
    private FButton btnNext;
    private FLabel lblStep;
    private FCheckBox cbDontShowAgain;
    private FComboBox<String> cbLanguage;
    private FComboBox<String> cbCardLanguage;
    private final String initialLanguage;

    private WelcomeWizardDialog() {
        super(true, false, "10");
        setTitle(L.getMessage("lblWelcomeWizardTitle"));
        setSize(640, 560);
        setMinimumSize(new Dimension(560, 460));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // Default FTitleBar separator uses CLR_BORDERS.stepColor(0), which reads lighter than the outer frame
        getTitleBar().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                FSkin.getColor(FSkin.Colors.CLR_BORDERS).getColor()));
        initialLanguage = prefs.getPref(FPref.UI_LANGUAGE);

        cardHost = new JPanel(cardLayout);
        cardHost.setOpaque(false);

        pages.add(buildSplashPage());
        pages.add(buildAiPage());
        pages.add(buildBattlefieldPage());
        pages.add(buildClosingPage());

        for (int i = 0; i < pages.size(); i++) {
            final FScrollPane scroll = new FScrollPane(pages.get(i).panel, false,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setBorder((Border) null);
            scroll.setViewportBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            cardHost.add(scroll, String.valueOf(i));
        }

        final JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.add(cardHost, BorderLayout.CENTER);
        root.add(buildNavBar(), BorderLayout.SOUTH);
        add(root, "push, grow");

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(final WindowEvent e) { onClose(); }
        });

        showPage(0);
    }

    private WizardPage buildSplashPage() {
        final JPanel p = newPagePanel();
        p.add(heading(L.getMessage("lblWelcomeWizardSplashHeading")), "w 100%, wrap, gapbottom 10");
        p.add(new FLabel.Builder()
                .text("<html>" + L.getMessage("lblWelcomeWizardSplashIntro") + "</html>")
                .fontSize(13).fontAlign(SwingConstants.LEFT).build(),
                "w 100%, wrap, gapbottom 20");

        final JPanel langBox = framedBox(new MigLayout("insets 12, fillx, gap 12 10", "[][grow]"));
        cbLanguage = new FComboBox<>(ForgeConstants.getAvailableLanguages().toArray(new String[0]));
        cbLanguage.setSelectedItem(initialLanguage);
        langBox.add(new FLabel.Builder()
                .text(L.getMessage("lblWelcomeWizardLanguagePageTitle") + ":")
                .fontSize(13).fontAlign(SwingConstants.LEFT).build());
        langBox.add(cbLanguage, "w 180!, h 26!, wrap");

        cbCardLanguage = new FComboBox<>(ForgeConstants.getScryfallCardLanguageMapping().keySet().toArray(new String[0]));
        cbCardLanguage.setSelectedItem(cardLanguageName(prefs.getPref(FPref.UI_CARD_DOWNLOAD_LANG)));
        langBox.add(new FLabel.Builder()
                .text(L.getMessage("cbpCardDownloadLang") + ":")
                .fontSize(13).fontAlign(SwingConstants.LEFT).build());
        langBox.add(cbCardLanguage, "w 180!, h 26!, wrap");
        p.add(langBox, "w 100%, wrap");

        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        p.add(spacer, "w 100%, growy, pushy, wrap");

        cbDontShowAgain = new FCheckBox(L.getMessage("lblWelcomeWizardDontShowAgain"));
        cbDontShowAgain.setSelected(true);
        p.add(cbDontShowAgain, "wrap");

        return new WizardPage(p, Collections.<RadioAxis>emptyList());
    }

    private WizardPage buildAiPage() {
        final JPanel p = newPagePanel();
        p.add(heading(L.getMessage("lblWelcomeWizardAiPageTitle")), "w 100%, wrap, gapbottom 5");
        p.add(noteLabel(L.getMessage("lblWelcomeWizardAiPageIntro")), "w 100%, wrap, gapbottom 15");

        final LinkedHashMap<String, Map<FPref, String>> options = new LinkedHashMap<>();
        options.put(describe(L.getMessage("lblWelcomeWizardAiCasual"), L.getMessage("lblWelcomeWizardAiCasualDesc")),
                OnboardingPresets.AI_CASUAL);
        options.put(describe(L.getMessage("lblWelcomeWizardAiExpert"), L.getMessage("lblWelcomeWizardAiExpertDesc")),
                OnboardingPresets.AI_EXPERT);
        final RadioAxis axis = new RadioAxis(options);
        axis.attachTo(p);

        final FLabel preview = previewLabel();
        p.add(previewPanel(preview), "w 100%, wrap, gaptop 15");
        axis.onChange = () -> preview.setText(renderPrefsHtml(axis.currentSelectionValues(prefs)));

        return new WizardPage(p, Collections.singletonList(axis));
    }

    private WizardPage buildBattlefieldPage() {
        final JPanel p = newPagePanel();
        p.add(heading(L.getMessage("lblWelcomeWizardBfPageTitle")), "w 100%, wrap, gapbottom 5");
        p.add(noteLabel(L.getMessage("lblWelcomeWizardBfPageIntro")), "w 100%, wrap, gapbottom 15");

        p.add(subheading(L.getMessage("lblWelcomeWizardBfLayout")), "w 100%, wrap, gapbottom 5");
        final LinkedHashMap<String, Map<FPref, String>> layoutOpts = new LinkedHashMap<>();
        layoutOpts.put(describe(L.getMessage("lblWelcomeWizardBfLayoutDefault"), L.getMessage("lblWelcomeWizardBfLayoutDefaultDesc")),
                OnboardingPresets.LAYOUT_DEFAULT);
        layoutOpts.put(describe(L.getMessage("lblWelcomeWizardBfLayoutCompact"), L.getMessage("lblWelcomeWizardBfLayoutCompactDesc")),
                OnboardingPresets.LAYOUT_COMPACT);
        final RadioAxis layoutAxis = new RadioAxis(layoutOpts);
        layoutAxis.attachTo(p);

        p.add(subheading(L.getMessage("lblWelcomeWizardBfOverlays")), "w 100%, wrap, gaptop 15, gapbottom 5");
        final LinkedHashMap<String, Map<FPref, String>> overlayOpts = new LinkedHashMap<>();
        overlayOpts.put(describe(L.getMessage("lblWelcomeWizardBfOverlaysOn"), L.getMessage("lblWelcomeWizardBfOverlaysOnDesc")),
                OnboardingPresets.OVERLAYS_ON);
        overlayOpts.put(describe(L.getMessage("lblWelcomeWizardBfOverlaysNone"), L.getMessage("lblWelcomeWizardBfOverlaysNoneDesc")),
                OnboardingPresets.OVERLAYS_NONE);
        final RadioAxis overlayAxis = new RadioAxis(overlayOpts);
        overlayAxis.attachTo(p);

        final FLabel preview = previewLabel();
        p.add(previewPanel(preview), "w 100%, wrap, gaptop 15");
        final Runnable refresh = () -> {
            final EnumMap<FPref, String> combined = new EnumMap<>(FPref.class);
            combined.putAll(layoutAxis.currentSelectionValues(prefs));
            combined.putAll(overlayAxis.currentSelectionValues(prefs));
            preview.setText(renderPrefsHtml(combined));
        };
        layoutAxis.onChange = refresh;
        overlayAxis.onChange = refresh;

        return new WizardPage(p, Arrays.asList(layoutAxis, overlayAxis));
    }

    private WizardPage buildClosingPage() {
        final JPanel p = newPagePanel();
        p.add(heading(L.getMessage("lblWelcomeWizardClosingHeading")), "w 100%, wrap, gapbottom 10");
        p.add(new FLabel.Builder()
                .text("<html>" + L.getMessage("lblWelcomeWizardClosingIntro") + "</html>")
                .fontSize(13).fontAlign(SwingConstants.LEFT).build(),
                "w 100%, wrap, gapbottom 15");

        final FButton howToPlay = new FButton(L.getMessage("lblHowToPlay"));
        howToPlay.addActionListener(e -> startTutorial());
        p.add(closingSection("lblWelcomeWizardTutorialHeading", "lblWelcomeWizardTutorialIntro", howToPlay),
                "w 100%, wrap, gapbottom 10");

        final FButton openPrefs = new FButton(L.getMessage("btnWelcomeWizardOpenPreferences"));
        openPrefs.addActionListener(e -> openPreferencesScreen());
        p.add(closingSection("lblWelcomeWizardPrefsHeading", "lblWelcomeWizardPrefsIntro", openPrefs),
                "w 100%, wrap, gapbottom 10");

        final FButton btnWiki = new FButton(L.getMessage("lblWelcomeWizardWikiLink"));
        btnWiki.addActionListener(e -> MenuUtil.openUrlInBrowser(ForgeConstants.GITHUB_FORGE_URL + "wiki"));
        final FButton btnDiscord = new FButton(L.getMessage("lblWelcomeWizardDiscordLink"));
        btnDiscord.addActionListener(e -> MenuUtil.openUrlInBrowser("https://discord.gg/HcPJNyD66a"));
        p.add(closingSection("lblWelcomeWizardHelpHeading", "lblWelcomeWizardClosingMoreInfo", btnWiki, btnDiscord),
                "w 100%, wrap");

        return new WizardPage(p, Collections.<RadioAxis>emptyList());
    }

    private JPanel closingSection(final String headingKey, final String textKey, final FButton... buttons) {
        final JPanel box = framedBox(new MigLayout("insets 12, fillx", "[grow]15[]"));
        box.add(subheading(L.getMessage(headingKey)), "cell 0 0");
        box.add(new FLabel.Builder()
                .text("<html>" + L.getMessage(textKey) + "</html>")
                .fontSize(13).fontAlign(SwingConstants.LEFT).build(),
                "cell 0 1, growx, wmin 0, aligny top");
        final JPanel buttonColumn = new JPanel(new MigLayout("insets 0, gap 0 6, wrap 1"));
        buttonColumn.setOpaque(false);
        for (final FButton b : buttons) {
            buttonColumn.add(b, "w 190!, h 30!");
        }
        box.add(buttonColumn, "cell 1 0, spany 2, aligny center");
        return box;
    }

    private void openPreferencesScreen() {
        closeWizard(() -> {
            Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);
            CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_PREFERENCES);
        });
    }

    private void startTutorial() {
        closeWizard(HowToPlay::chooseAndStart);
    }

    private JPanel buildNavBar() {
        final JPanel bar = new JPanel(new MigLayout("insets 10 20 20 10, gap 10, fillx"));
        bar.setOpaque(false);

        btnBack = new FButton(L.getMessage("lblWelcomeWizardBack"));
        btnBack.addActionListener(e -> onBack());

        btnNext = new FButton(L.getMessage("lblWelcomeWizardNext"));
        btnNext.addActionListener(e -> onNext());

        lblStep = new FLabel.Builder().fontSize(12).fontAlign(SwingConstants.LEFT).build();

        bar.add(new StepDots(), "w 64!, h 10!");
        bar.add(lblStep, "pushx, growx");
        bar.add(btnBack, "w 120!, h 30!, hidemode 3");
        bar.add(btnNext, "w 120!, h 30!");
        return bar;
    }

    /** One dot per page: pages already seen are brighter than pages ahead, and the current page is a wide pill. */
    @SuppressWarnings("serial")
    private final class StepDots extends JComponent {
        @Override
        protected void paintComponent(final Graphics g) {
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final Color text = FSkin.getColor(FSkin.Colors.CLR_TEXT).getColor();
            final int h = 8;
            final int y = (getHeight() - h) / 2;
            int x = 0;
            for (int i = 0; i < pages.size(); i++) {
                final int w = i == currentIndex ? 22 : h;
                final int alpha = i == currentIndex ? 255 : i < currentIndex ? 140 : 60;
                g2.setColor(new Color(text.getRed(), text.getGreen(), text.getBlue(), alpha));
                g2.fillRoundRect(x, y, w, h, h, h);
                x += w + 6;
            }
            g2.dispose();
        }
    }

    private void showPage(final int index) {
        currentIndex = index;
        cardLayout.show(cardHost, String.valueOf(index));
        pages.get(index).onEnter(prefs);
        btnBack.setVisible(index > 0);
        lblStep.setText(L.getMessage("lblWelcomeWizardStep", index + 1, pages.size()));
        btnNext.setText(index == pages.size() - 1
                ? L.getMessage("lblWelcomeWizardFinish")
                : L.getMessage("lblWelcomeWizardNext"));
        // CardLayout swaps repaint children only; force the dialog repaint so FDialog.paint() redraws the rounded outer border
        repaint();
    }

    private void onBack() {
        if (currentIndex > 0) showPage(currentIndex - 1);
    }

    private void onNext() {
        if (!commitCurrentPage()) return;
        if (currentIndex < pages.size() - 1) {
            showPage(currentIndex + 1);
        } else {
            onFinish();
        }
    }

    /** Returns false if the user cancelled the override-warning and we should stay on this page. */
    private boolean commitCurrentPage() {
        final WizardPage page = pages.get(currentIndex);
        if (page.requiresOverrideConfirm()
                && !FOptionPane.showConfirmDialog(
                        L.getMessage("msgWelcomeWizardOverrideCustom"),
                        L.getMessage("lblWelcomeWizardOverrideCustomTitle"))) {
            return false;
        }
        page.applyPending(prefs);
        if (currentIndex == 0) {
            final String selected = (String) cbLanguage.getSelectedItem();
            if (selected != null && !selected.equals(prefs.getPref(FPref.UI_LANGUAGE))) {
                prefs.setPref(FPref.UI_LANGUAGE, selected);
            }
            final String cardLang = ForgeConstants.getScryfallCardLanguageMapping().get((String) cbCardLanguage.getSelectedItem());
            if (cardLang != null) {
                prefs.setPref(FPref.UI_CARD_DOWNLOAD_LANG, cardLang);
            }
        }
        prefs.save();
        return true;
    }

    private void onFinish() {
        closeWizard(null);
    }

    private void onClose() {
        if (!commitCurrentPage()) return;
        closeWizard(null);
    }

    /** Every exit goes through here so a language change always offers the restart it needs. */
    private void closeWizard(final Runnable afterClose) {
        dispose();
        if (!Objects.equals(initialLanguage, prefs.getPref(FPref.UI_LANGUAGE))) {
            // Re-arm the wizard so it shows again in the new language, whatever the splash checkbox says
            prefs.setPref(FPref.WELCOME_SHOWN, "false");
            prefs.save();
            Singletons.getControl().restartForge();
        }
        prefs.setPref(FPref.WELCOME_SHOWN, String.valueOf(cbDontShowAgain.isSelected()));
        prefs.save();
        if (afterClose != null) afterClose.run();
    }

    private static JPanel newPagePanel() {
        final JPanel p = new ScrollableWidthPanel(new MigLayout("insets 20, gap 0, wrap 1, fillx"));
        p.setOpaque(false);
        return p;
    }

    /** Tracks the viewport width so MigLayout never overflows, and its height only while the content fits, so long pages scroll. */
    @SuppressWarnings("serial")
    private static final class ScrollableWidthPanel extends JPanel implements Scrollable {
        ScrollableWidthPanel(final LayoutManager layout) { super(layout); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(final Rectangle r, final int o, final int d) { return 16; }
        @Override public int getScrollableBlockIncrement(final Rectangle r, final int o, final int d) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getPreferredSize().height <= getParent().getHeight();
        }
    }

    private static FLabel heading(final String text) {
        return new FLabel.Builder().text(text).fontSize(20).fontStyle(Font.BOLD)
                .fontAlign(SwingConstants.LEFT).build();
    }

    private static FLabel subheading(final String text) {
        return new FLabel.Builder().text(text).fontSize(15).fontStyle(Font.BOLD)
                .fontAlign(SwingConstants.LEFT).build();
    }

    private static FLabel noteLabel(final String text) {
        return new FLabel.Builder().text("<html>" + text + "</html>").fontSize(12)
                .fontStyle(Font.ITALIC).fontAlign(SwingConstants.LEFT).build();
    }

    private static String describe(final String name, final String desc) {
        return "<html><b>" + name + "</b><br/><font size=\"-1\">" + desc + "</font></html>";
    }

    private FLabel previewLabel() {
        return new FLabel.Builder().text("").fontSize(12).fontAlign(SwingConstants.LEFT).build();
    }

    private JComponent previewPanel(final FLabel previewLabel) {
        final JPanel box = framedBox(new MigLayout("insets 10, fillx"));
        box.add(previewLabel, "growx, wrap");
        return box;
    }

    private static String cardLanguageName(final String code) {
        for (final Map.Entry<String, String> e : ForgeConstants.getScryfallCardLanguageMapping().entrySet()) {
            if (e.getValue().equals(code)) return e.getKey();
        }
        return "English";
    }

    private static JPanel framedBox(final LayoutManager layout) {
        final JPanel box = new JPanel(layout);
        box.setOpaque(false);
        // Hardcoded high-contrast grey: FSkin's CLR_BORDERS reads too dark here and skin-aware variants leave the right edge invisible
        box.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 200), 2));
        return box;
    }

    private String renderPrefsHtml(final Map<FPref, String> map) {
        final StringBuilder sb = new StringBuilder("<html><b>");
        sb.append(L.getMessage("lblWelcomeWizardPreviewHeading"));
        sb.append("</b><br/><br/><table cellpadding='0' cellspacing='2'>");
        for (final Map.Entry<FPref, String> e : map.entrySet()) {
            sb.append("<tr><td><b>").append(friendlyPrefName(e.getKey())).append(":</b></td>");
            sb.append("<td width='20'></td>");
            sb.append("<td>").append(friendlyValue(e.getKey(), e.getValue())).append("</td></tr>");
        }
        sb.append("</table></html>");
        return sb.toString();
    }

    private String friendlyPrefName(final FPref key) {
        switch (key) {
            case UI_OVERLAY_DRAFT_RANKING:         return L.getMessage("lblShowDraftRankingOverlay");
            case UI_ORDER_HAND:                    return L.getMessage("cbOrderHand");
            case YIELD_AUTO_PASS_NO_ACTIONS:       return L.getMessage("lblEnableAutoPass");
            case UI_REMIND_ON_PRIORITY:            return L.getMessage("cbRemindOnPriority");
            case UI_SHOW_STORM_COUNT_IN_PROMPT:    return L.getMessage("cbShowStormCount");
            case UI_DETAILED_SPELLDESC_IN_PROMPT:  return L.getMessage("cbDetailedPaymentDesc");
            case UI_TARGETING_OVERLAY:             return L.getMessage("lblTargetingArcs");
            case UI_GROUP_PERMANENTS:         return L.getMessage("cbpStackGroupPermanents");
            case UI_SHOW_CARD_OVERLAYS:       return L.getMessage("lblShowCardOverlays");
            case UI_SHOW_ACTIONABLE_HIGHLIGHTS: return L.getMessage("cbShowActionableHighlights");
            case UI_SHOW_LINKED_EXILE_CARDS:  return L.getMessage("cbShowLinkedExileCards");
            case UI_UNDO_MODE:                return L.getMessage("cbUndoMode");
            default:                          return key.name();
        }
    }

    private String friendlyValue(final FPref key, final String value) {
        if (key == FPref.UI_TARGETING_OVERLAY) {
            if ("0".equals(value)) return L.getMessage("lblOff");
            if ("1".equals(value)) return L.getMessage("lblCardMouseOver");
            if ("2".equals(value)) return L.getMessage("lblAlwaysOn");
        }
        if (key == FPref.UI_GROUP_PERMANENTS) {
            if ("default".equals(value))         return L.getMessage("lblGroupDefault");
            if ("stack".equals(value))           return L.getMessage("lblGroupStack");
            if ("group_creatures".equals(value)) return L.getMessage("lblGroupCreatures");
            if ("group_all".equals(value))       return L.getMessage("lblGroupAll");
        }
        if ("true".equals(value))  return L.getMessage("lblYes");
        if ("false".equals(value)) return L.getMessage("lblNo");
        return value;
    }

    private static final class WizardPage {
        final JPanel panel;
        final List<RadioAxis> axes;
        boolean entered;

        WizardPage(final JPanel panel, final List<RadioAxis> axes) {
            this.panel = panel;
            this.axes = axes;
        }

        // Only on first entry, so a pick survives Back then Next
        void onEnter(final ForgePreferences prefs) {
            if (entered) return;
            entered = true;
            for (final RadioAxis axis : axes) axis.recomputePreselection(prefs);
        }

        boolean requiresOverrideConfirm() {
            for (final RadioAxis axis : axes) {
                if (axis.isOverridingCustom()) return true;
            }
            return false;
        }

        void applyPending(final ForgePreferences prefs) {
            for (final RadioAxis axis : axes) axis.applyPending(prefs);
        }
    }

    private static final class RadioAxis {
        final LinkedHashMap<String, Map<FPref, String>> options;
        final ButtonGroup group = new ButtonGroup();
        final List<FRadioButton> buttons = new ArrayList<>();
        final List<Map<FPref, String>> presetForButton = new ArrayList<>();
        JPanel ownPanel; // each axis owns its own container so dynamic keep-current insertion is local
        FRadioButton keepCurrentButton; // null when not currently shown
        int preselectedIndex = -1;
        Runnable onChange;

        RadioAxis(final LinkedHashMap<String, Map<FPref, String>> options) {
            this.options = options;
        }

        void attachTo(final JPanel parent) {
            ownPanel = new JPanel(new MigLayout("insets 0, gap 0, wrap 1, fillx"));
            ownPanel.setOpaque(false);
            for (final Map.Entry<String, Map<FPref, String>> e : options.entrySet()) {
                final FRadioButton b = new FRadioButton(e.getKey());
                group.add(b);
                buttons.add(b);
                presetForButton.add(e.getValue());
                wireChangeListener(b);
                ownPanel.add(b, "w 100%, wrap");
            }
            parent.add(ownPanel, "w 100%, wrap");
        }

        private void wireChangeListener(final FRadioButton b) {
            b.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED && onChange != null) onChange.run();
            });
        }

        Set<FPref> prefUnion() {
            final EnumSet<FPref> result = EnumSet.noneOf(FPref.class);
            for (final Map<FPref, String> preset : presetForButton) result.addAll(preset.keySet());
            return result;
        }

        /** What the page's preference state would be if the user committed right now.
         *  For a real preset selection: that preset's map.
         *  For Keep custom settings (or nothing selected): current FPref values for the pref-union. */
        Map<FPref, String> currentSelectionValues(final ForgePreferences prefs) {
            final int idx = selectedPresetIndex();
            if (idx >= 0) return presetForButton.get(idx);
            final Map<FPref, String> result = new EnumMap<>(FPref.class);
            for (final FPref key : prefUnion()) result.put(key, prefs.getPref(key));
            return result;
        }

        void recomputePreselection(final ForgePreferences prefs) {
            int bestIdx = -1;
            int bestSize = -1;
            for (int i = 0; i < presetForButton.size(); i++) {
                final Map<FPref, String> preset = presetForButton.get(i);
                if (matches(preset, prefs) && preset.size() > bestSize) {
                    bestIdx = i;
                    bestSize = preset.size();
                }
            }

            if (bestIdx >= 0) {
                removeKeepCurrentButton();
                buttons.get(bestIdx).setSelected(true);
                preselectedIndex = bestIdx;
            } else {
                addKeepCurrentButton();
                keepCurrentButton.setSelected(true);
                preselectedIndex = -1;
            }
        }

        boolean isOverridingCustom() {
            return preselectedIndex == -1 && (keepCurrentButton == null || !keepCurrentButton.isSelected())
                    && selectedPresetIndex() >= 0;
        }

        void applyPending(final ForgePreferences prefs) {
            final int idx = selectedPresetIndex();
            if (idx < 0 || idx == preselectedIndex) return;
            for (final Map.Entry<FPref, String> e : presetForButton.get(idx).entrySet()) {
                prefs.setPref(e.getKey(), e.getValue());
            }
            // Now saved, so coming back to this page doesn't warn about overriding custom settings again
            preselectedIndex = idx;
        }

        private int selectedPresetIndex() {
            for (int i = 0; i < buttons.size(); i++) {
                if (buttons.get(i).isSelected()) return i;
            }
            return -1;
        }

        private boolean matches(final Map<FPref, String> preset, final ForgePreferences prefs) {
            for (final Map.Entry<FPref, String> e : preset.entrySet()) {
                if (!e.getValue().equals(prefs.getPref(e.getKey()))) return false;
            }
            return true;
        }

        private void addKeepCurrentButton() {
            if (keepCurrentButton != null) return;
            keepCurrentButton = new FRadioButton("<html><b>"
                    + Localizer.getInstance().getMessage("lblWelcomeWizardKeepCurrent") + "</b></html>");
            group.add(keepCurrentButton);
            wireChangeListener(keepCurrentButton);
            ownPanel.add(keepCurrentButton, "w 100%, wrap");
            ownPanel.revalidate();
        }

        private void removeKeepCurrentButton() {
            if (keepCurrentButton == null) return;
            group.remove(keepCurrentButton);
            ownPanel.remove(keepCurrentButton);
            ownPanel.revalidate();
            ownPanel.repaint();
            keepCurrentButton = null;
        }
    }
}
