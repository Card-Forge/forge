package forge.screens.home.settings;

import com.google.common.collect.Iterables;
import forge.card.CardEdition;
import forge.download.GuiDownloader;
import forge.game.GameFormat;
import forge.gui.FThreads;
import forge.gui.SOverlayUtils;
import forge.gui.download.CdnUuidCache;
import forge.gui.download.GuiDownloadFilteredCardImages;
import forge.gui.download.ScryfallBulkDataSync;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.itemmanager.SFilterUtil;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.*;
import forge.toolbox.FCheckBoxTree.FTreeNodeData;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Desktop equivalent of the mobile {@code CardImageBrowserScreen}: a Scryfall-syntax search box,
 * a format/"Choose Sets" filter, live total/downloaded/missing stats, and Download / Clear CDN
 * Cache buttons, all driving the shared {@link GuiDownloadFilteredCardImages}.
 */
public class DialogDownloadCardImages {
    private static final Localizer localizer = Localizer.getInstance();

    private final FTextField txtSearch = new FTextField.Builder()
            .ghostText(localizer.getMessage("lblSearch") + " " + localizer.getMessage("lblCards") + "...")
            .build();
    private final FComboBox<Object> cbxFormats = new FComboBox<>();
    private final FLabel lblTotal      = new FLabel.Builder().text("--").fontSize(14).fontAlign(SwingConstants.CENTER).build();
    private final FLabel lblDownloaded = new FLabel.Builder().text("--").fontSize(14).fontAlign(SwingConstants.CENTER).build();
    private final FLabel lblMissing    = new FLabel.Builder().text("--").fontSize(14).fontAlign(SwingConstants.CENTER).build();
    private final FProgressBar bulkSyncProgress = new FProgressBar();

    private final Timer searchDebounce = new Timer(200, e -> scheduleStatsUpdate());
    private final AtomicInteger generation = new AtomicInteger(0);

    private GameFormat selectedFormat = null;
    private FPanel mainPanel;
    private FButton btnDownload;
    private FButton btnSyncBulkData;
    private FComboBox<String> cbxIndexLang;
    private FButton btnSyncBulkDataLang;
    private FCheckBox cbPreferLangForUnique;
    private FButton btnClearCdnCache;

    public void show() {
        searchDebounce.setRepeats(false);
        buildMainPanel();
        showMainOverlay();
        scheduleStatsUpdate();
    }

    // =========================================================================
    //  Main dialog
    // =========================================================================

    private void buildMainPanel() {
        txtSearch.addChangeListener(new FTextField.ChangeListener() {
            @Override public void textChanged() { searchDebounce.restart(); }
        });

        cbxFormats.addItem(localizer.getMessage("lblAllSetsFormats"));
        for (GameFormat fmt : FModel.getFormats().getFilterList()) {
            cbxFormats.addItem(fmt);
        }
        cbxFormats.addItem(localizer.getMessage("lblChooseSets"));

        cbxFormats.addActionListener(e -> {
            int idx = cbxFormats.getSelectedIndex();
            if (idx < 0) { return; }
            if (idx == cbxFormats.getItemCount() - 1) {
                openChooseSets();
                return;
            }
            selectedFormat = idx == 0 ? null : (GameFormat) cbxFormats.getSelectedItem();
            scheduleStatsUpdate();
        });

        btnDownload = new FButton(localizer.getMessage("btnDownloadCardImages"));
        btnDownload.addActionListener(e -> startDownload());

        btnSyncBulkData = new FButton(localizer.getMessage("btnSyncBulkCardData"));
        btnSyncBulkData.addActionListener(e -> startBulkSync(ScryfallBulkDataSync.BULK_TYPE_DEFAULT_CARDS, null, "English"));

        final Map<String, String> cardLangMapping = ForgeConstants.getScryfallCardLanguageMapping();
        cbxIndexLang = new FComboBox<>();
        for (Map.Entry<String, String> entry : cardLangMapping.entrySet()) {
            if (!"en".equalsIgnoreCase(entry.getValue())) {
                cbxIndexLang.addItem(entry.getKey());
            }
        }
        final String savedLangCode = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_CARD_DOWNLOAD_LANG);
        cardLangMapping.entrySet().stream()
                .filter(entry -> entry.getValue().equals(savedLangCode))
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(cbxIndexLang::setSelectedItem);

        cbPreferLangForUnique = new FCheckBox(localizer.getMessage("cbPreferLangForUniqueCards"),
                FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_PREFER_LANG_FOR_UNIQUE_CARDS));
        cbPreferLangForUnique.setToolTipText(localizer.getMessage("nlPreferLangForUniqueCards"));

        btnSyncBulkDataLang = new FButton(localizer.getMessage("btnSyncBulkCardDataLang"));
        btnSyncBulkDataLang.addActionListener(e -> {
            String selectedLangName = (String) cbxIndexLang.getSelectedItem();
            String selectedLangCode = cardLangMapping.get(selectedLangName);
            FModel.getPreferences().setPref(ForgePreferences.FPref.UI_CARD_DOWNLOAD_LANG, selectedLangCode);
            FModel.getPreferences().setPref(ForgePreferences.FPref.UI_PREFER_LANG_FOR_UNIQUE_CARDS,
                    String.valueOf(cbPreferLangForUnique.isSelected()));
            FModel.getPreferences().save();
            applyPreferredLanguageAvailability(selectedLangCode);
            startBulkSync(ScryfallBulkDataSync.BULK_TYPE_ALL_CARDS,
                    new java.util.HashSet<>(java.util.Arrays.asList("en", selectedLangCode)), selectedLangName);
        });

        btnClearCdnCache = new FButton(localizer.getMessage("btnClearCdnImageCache"));
        btnClearCdnCache.addActionListener(e -> clearCdnCache());

        final FButton btnClose = new FButton(localizer.getMessage("lblClose"));
        btnClose.addActionListener(e -> SOverlayUtils.hideOverlay());

        mainPanel = new FPanel(new MigLayout("insets 15, gap 5, wrap, center"));
        mainPanel.setOpaque(false);
        mainPanel.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));

        mainPanel.add(new FLabel.Builder().text(localizer.getMessage("btnDownloadCardImages"))
                .fontSize(18).fontStyle(Font.BOLD).fontAlign(SwingConstants.CENTER).build(), "center, w 480!, gaptop 5");
        mainPanel.add(txtSearch, "w 480!, h 28!, gaptop 10");
        mainPanel.add(cbxFormats, "w 480!, h 28!, gaptop 8");
        mainPanel.add(lblTotal, "w 480!, gaptop 15, center");
        mainPanel.add(lblDownloaded, "w 480!, center");
        mainPanel.add(lblMissing, "w 480!, center, gapbottom 10");
        mainPanel.add(btnDownload, "w 460!, h 32!, center, gaptop 10");
        mainPanel.add(btnSyncBulkData, "w 460!, h 32!, center, gaptop 8");
        mainPanel.add(cbxIndexLang, "w 300!, h 32!, split 2, gaptop 8");
        mainPanel.add(btnSyncBulkDataLang, "w 154!, h 32!");
        mainPanel.add(cbPreferLangForUnique, "w 460!, center, gaptop 2");
        mainPanel.add(bulkSyncProgress, "w 460!, h 26!, center, gaptop 6");
        mainPanel.add(btnClearCdnCache, "w 460!, h 32!, center, gaptop 8");
        mainPanel.add(btnClose, "w 460!, h 32!, center, gaptop 8, gapbottom 15");
    }

    private void showMainOverlay() {
        final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        overlay.removeAll();
        overlay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
        overlay.add(mainPanel);
        SOverlayUtils.showOverlay();
    }

    // =========================================================================
    //  Stats computation (non-blocking, discards stale results)
    // =========================================================================

    private void scheduleStatsUpdate() {
        final int gen = generation.incrementAndGet();

        lblTotal.setText(localizer.getMessage("lblTotalCards") + ": ...");
        lblDownloaded.setText(localizer.getMessage("lblDownloaded") + ": ...");
        lblMissing.setText(localizer.getMessage("lblMissing") + ": ...");

        final Predicate<PaperCard> combined = buildCurrentFilter();

        FThreads.invokeInBackgroundThread(() -> {
            int total = 0, downloaded = 0, missing = 0;
            for (PaperCard card : Iterables.concat(
                    FModel.getMagicDb().getCommonCards().getAllCards(),
                    FModel.getMagicDb().getVariantCards().getAllCards())) {
                if (combined.test(card)) {
                    total++;
                    if (card.hasImage()) { downloaded++; } else { missing++; }
                }
            }

            if (generation.get() != gen) { return; }
            final int fTotal = total, fDownloaded = downloaded, fMissing = missing;
            FThreads.invokeInEdtLater(() -> {
                if (generation.get() != gen) { return; }
                lblTotal.setText(localizer.getMessage("lblTotalCards") + ": " + fTotal);
                lblDownloaded.setText(localizer.getMessage("lblDownloaded") + ": " + fDownloaded);
                lblMissing.setText(localizer.getMessage("lblMissing") + ": " + fMissing);
            });
        });
    }

    /** Builds the combined card predicate from the current search field and format selection. */
    private Predicate<PaperCard> buildCurrentFilter() {
        Predicate<PaperCard> textPred = SFilterUtil.buildTextFilter(
                txtSearch.getText(), false, true, true, true, false);
        Predicate<PaperCard> fmtPred = selectedFormat == null        ? x -> true
                : selectedFormat.getName() == null ? selectedFormat.getFilterPrinted()
                : selectedFormat.getFilterRules();
        return textPred.and(fmtPred);
    }

    // =========================================================================
    //  Download / cache actions
    // =========================================================================

    private void startDownload() {
        new GuiDownloader(new GuiDownloadFilteredCardImages(buildCurrentFilter())).show();
    }

    private void clearCdnCache() {
        if (!SOptionPane.showConfirmDialog(localizer.getMessage("lblClearCdnImageCacheConfirm"))) {
            return;
        }
        CdnUuidCache.clearCache();
        SOptionPane.showMessageDialog(localizer.getMessage("lblCdnImageCacheCleared"));
    }

    /** Called after {@link #show()} to open straight into a confirmed bulk sync (e.g. the first-run prompt). */
    public void showAndAutoStartBulkSync() {
        show();
        runBulkSync(ScryfallBulkDataSync.BULK_TYPE_DEFAULT_CARDS, null, "English");
    }

    private void applyPreferredLanguageAvailability(String langCode) {
        boolean preferForUnique = cbPreferLangForUnique.isSelected();
        if (!preferForUnique || langCode == null || langCode.isEmpty() || "en".equalsIgnoreCase(langCode)) {
            FModel.getMagicDb().setPreferredLanguageAvailability(null);
        } else {
            FModel.getMagicDb().setPreferredLanguageAvailability((setCode, cn) -> CdnUuidCache.isAvailableInLanguage(setCode, cn, langCode));
        }
    }

    /** Resolves CDN links for every set at once from an online bulk index, instead of one set at a time. */
    private void startBulkSync(String bulkDataType, java.util.Set<String> allowedLangs, String langLabel) {
        if (!SOptionPane.showConfirmDialog(localizer.getMessage("lblSyncBulkCardDataConfirm", ScryfallBulkDataSync.approxSizeLabel(bulkDataType)))) {
            return;
        }
        runBulkSync(bulkDataType, allowedLangs, langLabel);
    }

    private void runBulkSync(String bulkDataType, java.util.Set<String> allowedLangs, String langLabel) {
        btnDownload.setEnabled(false);
        btnSyncBulkData.setEnabled(false);
        btnSyncBulkDataLang.setEnabled(false);
        btnClearCdnCache.setEnabled(false);

        bulkSyncProgress.reset();
        bulkSyncProgress.setMaximum(100);
        bulkSyncProgress.setShowETA(false);
        bulkSyncProgress.setShowCount(false);
        bulkSyncProgress.setIndeterminate(true);
        bulkSyncProgress.setDescription("Starting...");

        FThreads.invokeInBackgroundThread(() -> {
            int setCount = ScryfallBulkDataSync.sync(bulkDataType, allowedLangs,
                    (message, fraction) -> FThreads.invokeInEdtLater(() -> {
                        bulkSyncProgress.setDescription(message);
                        if (fraction >= 0) {
                            bulkSyncProgress.setIndeterminate(false);
                            bulkSyncProgress.setValue((int) Math.round(fraction * 100));
                        } else {
                            bulkSyncProgress.setIndeterminate(true);
                        }
                    }),
                    () -> false);
            FThreads.invokeInEdtLater(() -> {
                btnDownload.setEnabled(true);
                btnSyncBulkData.setEnabled(true);
                btnSyncBulkDataLang.setEnabled(true);
                btnClearCdnCache.setEnabled(true);
                bulkSyncProgress.setIndeterminate(false);
                if (setCount >= 0) {
                    bulkSyncProgress.setValue(100);
                    bulkSyncProgress.setDescription(localizer.getMessage("lblBulkCardDataSynced") + " (" + setCount + " sets) - " + langLabel);
                    scheduleStatsUpdate();
                } else {
                    bulkSyncProgress.setDescription("Bulk sync failed -- see log for details.");
                }
            });
        });
    }

    // =========================================================================
    //  "Choose Sets" picker (mirrors DialogChooseSets, minus its randomizer/format extras)
    // =========================================================================

    private void openChooseSets() {
        final FCheckBoxTree checkBoxTree = new FCheckBoxTree();
        checkBoxTree.setOpaque(false);

        final TreeMap<FTreeNodeData, List<FTreeNodeData>> treeData = new TreeMap<>();
        for (Map.Entry<CardEdition.Type, List<CardEdition>> entry : FModel.getMagicDb().getEditionsTypeMap().entrySet()) {
            if (entry.getValue().isEmpty()) { continue; }
            final List<FTreeNodeData> nodes = new ArrayList<>();
            for (CardEdition ce : entry.getValue()) {
                nodes.add(new FTreeNodeData(ce, ce.getName(), ce.getCode()));
            }
            treeData.put(new FTreeNodeData(entry.getKey()), nodes);
        }
        checkBoxTree.setTreeData(treeData);

        final FPanel panel = new FPanel(new MigLayout("insets 10, gap 5, wrap, center"));
        panel.setOpaque(false);
        panel.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));
        panel.add(new FLabel.Builder().text(localizer.getMessage("lblChooseSets"))
                .fontSize(18).fontStyle(Font.BOLD).fontAlign(SwingConstants.CENTER).build(), "center, w 500!, gaptop 5");
        panel.add(new FScrollPane(checkBoxTree, true), "w 500!, h 400!, gaptop 10");

        final FButton btnOk = new FButton(localizer.getMessage("lblOK"));
        final FButton btnCancel = new FButton(localizer.getMessage("lblCancel"));
        btnOk.addActionListener(e -> {
            final List<String> codes = new ArrayList<>();
            for (Object v : checkBoxTree.getCheckedValues(true)) {
                codes.add(((CardEdition) v).getCode());
            }
            showMainOverlay();
            if (!codes.isEmpty()) {
                selectedFormat = new GameFormat(null, codes, null);
                scheduleStatsUpdate();
            }
        });
        btnCancel.addActionListener(e -> showMainOverlay());

        final JPanel southPanel = new JPanel(new MigLayout("insets 10, gap 30, ax center"));
        southPanel.setOpaque(false);
        southPanel.add(btnOk, "w 200!, h 30!");
        southPanel.add(btnCancel, "w 200!, h 30!");
        panel.add(southPanel, "gaptop 10, gapbottom 5");

        final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
        overlay.removeAll();
        overlay.setLayout(new MigLayout("insets 0, gap 0, wrap, ax center, ay center"));
        overlay.add(panel);
        SOverlayUtils.showOverlay();
    }
}
