package forge.screens.home.welcome;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.Rectangle;
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
    private FCheckBox cbDontShowAgain;
    private FComboBox<String> cbLanguage;
    private final String initialLanguage;

    private WelcomeWizardDialog() {
        super(true, false, "10");
        setTitle(L.getMessage("lblWelcomeWizardTitle"));
        setSize(640, 520);
        setMinimumSize(new Dimension(560, 460));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // Default FTitleBar separator uses CLR_BORDERS.stepColor(0), which reads lighter than the outer frame.
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
        add(root, "w 100%!, h 100%!");

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

        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        p.add(spacer, "w 100%, growy, pushy, wrap");

        cbLanguage = new FComboBox<>(ForgeConstants.getAvailableLanguages().toArray(new String[0]));
        cbLanguage.setSelectedItem(initialLanguage);
        p.add(new FLabel.Builder()
                .text(L.getMessage("lblWelcomeWizardLanguagePageTitle") + ":")
                .fontSize(13).fontAlign(SwingConstants.LEFT).build(),
                "split 2, gapbottom 15");
        p.add(cbLanguage, "w 160!, h 26!, wrap, gapbottom 15");

        cbDontShowAgain = new FCheckBox(L.getMessage("lblWelcomeWizardDontShowAgain"));
        cbDontShowAgain.setSelected(true);
        p.add(cbDontShowAgain, "w 100%, wrap");

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
        final RadioAxis axis = new RadioAxis(options, true);
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
        final RadioAxis layoutAxis = new RadioAxis(layoutOpts, true);
        layoutAxis.attachTo(p);

        p.add(subheading(L.getMessage("lblWelcomeWizardBfOverlays")), "w 100%, wrap, gaptop 15, gapbottom 5");
        final LinkedHashMap<String, Map<FPref, String>> overlayOpts = new LinkedHashMap<>();
        overlayOpts.put(describe(L.getMessage("lblWelcomeWizardBfOverlaysOn"), L.getMessage("lblWelcomeWizardBfOverlaysOnDesc")),
                OnboardingPresets.OVERLAYS_ON);
        overlayOpts.put(describe(L.getMessage("lblWelcomeWizardBfOverlaysNone"), L.getMessage("lblWelcomeWizardBfOverlaysNoneDesc")),
                OnboardingPresets.OVERLAYS_NONE);
        final RadioAxis overlayAxis = new RadioAxis(overlayOpts, true);
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

        final FButton openPrefs = new FButton(L.getMessage("btnWelcomeWizardOpenPreferences"));
        openPrefs.addActionListener(e -> openPreferencesScreen());
        p.add(openPrefs, "w 240!, h 30!, wrap, gapbottom 20");

        p.add(new FLabel.Builder()
                .text("<html>" + L.getMessage("lblWelcomeWizardClosingMoreInfo") + "</html>")
                .fontSize(13).fontAlign(SwingConstants.LEFT).build(),
                "w 100%, wrap, gapbottom 10");

        final FButton btnWiki = new FButton(L.getMessage("lblWelcomeWizardWikiLink"));
        btnWiki.addActionListener(e -> MenuUtil.openUrlInBrowser(ForgeConstants.GITHUB_FORGE_URL + "wiki"));
        final FButton btnDiscord = new FButton(L.getMessage("lblWelcomeWizardDiscordLink"));
        btnDiscord.addActionListener(e -> MenuUtil.openUrlInBrowser("https://discord.gg/HcPJNyD66a"));

        final JPanel linkRow = new JPanel(new MigLayout("insets 0, gap 15"));
        linkRow.setOpaque(false);
        linkRow.add(btnWiki, "w 240!, h 30!");
        linkRow.add(btnDiscord, "w 240!, h 30!");
        p.add(linkRow, "w 100%, wrap");

        return new WizardPage(p, Collections.<RadioAxis>emptyList());
    }

    private void openPreferencesScreen() {
        // Dispose before switching screens so the prefs screen comes to the foreground.
        writeWelcomeShownFromCheckbox();
        prefs.save();
        dispose();
        Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);
        CHomeUI.SINGLETON_INSTANCE.itemClick(EDocID.HOME_PREFERENCES);
    }

    private JPanel buildNavBar() {
        final JPanel bar = new JPanel(new MigLayout("insets 10 10 20 10, gap 10, ax right"));
        bar.setOpaque(false);

        btnBack = new FButton(L.getMessage("lblWelcomeWizardBack"));
        btnBack.addActionListener(e -> onBack());

        btnNext = new FButton(L.getMessage("lblWelcomeWizardNext"));
        btnNext.addActionListener(e -> onNext());

        bar.add(btnBack, "w 120!, h 30!");
        bar.add(btnNext, "w 120!, h 30!");
        return bar;
    }

    private void showPage(final int index) {
        currentIndex = index;
        cardLayout.show(cardHost, String.valueOf(index));
        pages.get(index).onEnter(prefs);
        btnBack.setEnabled(index > 0);
        btnNext.setText(index == pages.size() - 1
                ? L.getMessage("lblWelcomeWizardFinish")
                : L.getMessage("lblWelcomeWizardNext"));
        // CardLayout swaps repaint children only; force the dialog repaint so FDialog.paint() redraws the rounded outer border.
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
        }
        prefs.save();
        return true;
    }

    private void onFinish() {
        final boolean languageChanged = !Objects.equals(initialLanguage, prefs.getPref(FPref.UI_LANGUAGE));
        if (languageChanged && FOptionPane.showConfirmDialog(
                L.getMessage("msgWelcomeWizardRestart"),
                L.getMessage("lblWelcomeWizardRestartTitle"),
                L.getMessage("lblYes"),
                L.getMessage("lblWelcomeWizardLater"))) {
            // Re-arm the wizard for next launch so the user sees it in the new language regardless of the splash checkbox.
            prefs.setPref(FPref.WELCOME_SHOWN, "false");
            prefs.save();
            dispose();
            Singletons.getControl().restartForge();
        } else {
            writeWelcomeShownFromCheckbox();
            prefs.save();
            dispose();
        }
    }

    private void onClose() {
        if (!commitCurrentPage()) return;
        writeWelcomeShownFromCheckbox();
        prefs.save();
        dispose();
    }

    private void writeWelcomeShownFromCheckbox() {
        prefs.setPref(FPref.WELCOME_SHOWN, String.valueOf(cbDontShowAgain.isSelected()));
    }

    private static JPanel newPagePanel() {
        final JPanel p = new ScrollableWidthPanel(new MigLayout("insets 20, gap 0, wrap 1, fillx"));
        p.setOpaque(false);
        return p;
    }

    /** JPanel that tracks the viewport width (so MigLayout doesn't overflow horizontally),
     *  and tracks the viewport height when its preferred height fits (so MigLayout's
     *  growy/pushy spacers can fill the page). When content exceeds the viewport, falls
     *  back to preferred height so the scrollpane shows a vertical scrollbar. */
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
        final JPanel box = new JPanel(new MigLayout("insets 10, fillx"));
        box.setOpaque(false);
        // Hardcoded high-contrast grey: FSkin's CLR_BORDERS reads too dark here and skin-aware variants leave the right edge invisible.
        box.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 200), 2));
        box.add(previewLabel, "growx, wrap");
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

        WizardPage(final JPanel panel, final List<RadioAxis> axes) {
            this.panel = panel;
            this.axes = axes;
        }

        void onEnter(final ForgePreferences prefs) {
            for (final RadioAxis axis : axes) axis.recomputePreselection(prefs);
        }

        boolean requiresOverrideConfirm() {
            for (final RadioAxis axis : axes) {
                if (axis.isOverridingCustom()) return true;
            }
            return false;
        }

        /** Returns true if any FPref was written. */
        boolean applyPending(final ForgePreferences prefs) {
            boolean wrote = false;
            for (final RadioAxis axis : axes) {
                if (axis.applyPending(prefs)) wrote = true;
            }
            return wrote;
        }
    }

    private static final class RadioAxis {
        final LinkedHashMap<String, Map<FPref, String>> options;
        final boolean offerKeepCurrent;
        final ButtonGroup group = new ButtonGroup();
        final List<FRadioButton> buttons = new ArrayList<>();
        final List<Map<FPref, String>> presetForButton = new ArrayList<>();
        JPanel ownPanel; // each axis owns its own container so dynamic keep-current insertion is local
        FRadioButton keepCurrentButton; // null when not currently shown
        int preselectedIndex = -1;
        Runnable onChange; // fired when the user picks a different option

        RadioAxis(final LinkedHashMap<String, Map<FPref, String>> options,
                  final boolean offerKeepCurrent) {
            this.options = options;
            this.offerKeepCurrent = offerKeepCurrent;
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
            } else if (offerKeepCurrent) {
                addKeepCurrentButton();
                keepCurrentButton.setSelected(true);
                preselectedIndex = -1;
            } else if (!buttons.isEmpty()) {
                buttons.get(0).setSelected(true);
                preselectedIndex = 0;
            }
        }

        boolean isOverridingCustom() {
            return preselectedIndex == -1 && (keepCurrentButton == null || !keepCurrentButton.isSelected())
                    && selectedPresetIndex() >= 0;
        }

        boolean applyPending(final ForgePreferences prefs) {
            final int idx = selectedPresetIndex();
            if (idx < 0 || idx == preselectedIndex) return false;
            for (final Map.Entry<FPref, String> e : presetForButton.get(idx).entrySet()) {
                prefs.setPref(e.getKey(), e.getValue());
            }
            return true;
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
            if (keepCurrentButton != null || ownPanel == null) return;
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
            if (ownPanel != null) {
                ownPanel.remove(keepCurrentButton);
                ownPanel.revalidate();
                ownPanel.repaint();
            }
            keepCurrentButton = null;
        }
    }
}
