package forge.screens.settings;

import com.badlogic.gdx.utils.Align;
import com.google.common.collect.Iterables;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.gui.FThreads;
import forge.gui.download.CdnUuidCache;
import forge.gui.download.GuiDownloadFilteredCardImages;
import forge.gui.download.ScryfallBulkDataSync;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.itemmanager.SFilterUtil;
import forge.itemmanager.filters.ArchivedFormatSelect;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.*;
import forge.util.TextUtil;
import forge.util.Utils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CardImageBrowserScreen extends FScreen {
    private static final float PADDING      = Utils.scale(5);
    private static final float FIELD_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.75f);
    private static final float STAT_HEIGHT  = Math.round(Utils.AVG_FINGER_HEIGHT * 0.85f);
    private static final float BTN_HEIGHT   = Math.round(Utils.AVG_FINGER_HEIGHT * 0.9f);
    private static final FSkinFont STAT_FONT = FSkinFont.get(16);

    private final FScrollPane       scroller;
    private final FTextField        txtSearch;
    private final FComboBox<Object> cbxFormats;
    private final FLabel            lblTotal;
    private final FLabel            lblDownloaded;
    private final FLabel            lblMissing;
    private final FButton           btnDownload;
    private final FButton           btnSyncBulkData;
    private final FComboBox<String> cbxIndexLang;
    private final FButton           btnSyncBulkDataLang;
    private final FCheckBox         cbPreferLangForUnique;
    private final FProgressBar      bulkSyncProgress;
    private final FButton           btnClearCdnCache;

    private GameFormat   selectedFormat    = null;
    private String       selectedFormatText;
    private boolean      preventHandling   = false;

    /** Generation counter – incremented on every filter change so stale
     *  background results are silently discarded instead of updating labels. */
    private final AtomicInteger generation = new AtomicInteger(0);

    public CardImageBrowserScreen() {
        super(Forge.getLocalizer().getMessage("btnDownloadCardImages"));

        scroller = add(new FScrollPane() {
            @Override
            protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
                float x = PADDING;
                float y = PADDING;
                float w = visibleWidth - 2 * PADDING;

                txtSearch.setBounds(x, y, w, FIELD_HEIGHT);
                y += FIELD_HEIGHT + PADDING;

                cbxFormats.setBounds(x, y, w, FIELD_HEIGHT);
                y += FIELD_HEIGHT + PADDING * 5;

                lblTotal.setBounds(x, y, w, STAT_HEIGHT);
                y += STAT_HEIGHT + PADDING;

                lblDownloaded.setBounds(x, y, w, STAT_HEIGHT);
                y += STAT_HEIGHT + PADDING;

                lblMissing.setBounds(x, y, w, STAT_HEIGHT);
                y += STAT_HEIGHT + PADDING * 5;

                float btnW = Math.min(w * 0.6f, Utils.AVG_FINGER_HEIGHT * 4);
                btnDownload.setBounds(x + (w - btnW) / 2f, y, btnW, BTN_HEIGHT);
                y += BTN_HEIGHT + PADDING;

                btnSyncBulkData.setBounds(x + (w - btnW) / 2f, y, btnW, BTN_HEIGHT);
                y += BTN_HEIGHT + PADDING;

                cbxIndexLang.setBounds(x, y, w, FIELD_HEIGHT);
                y += FIELD_HEIGHT + PADDING;

                btnSyncBulkDataLang.setBounds(x + (w - btnW) / 2f, y, btnW, BTN_HEIGHT);
                y += BTN_HEIGHT + PADDING;

                float checkboxHeight = Math.round(Utils.AVG_FINGER_HEIGHT * 0.6f);
                cbPreferLangForUnique.setBounds(x, y, w, checkboxHeight);
                y += checkboxHeight + PADDING;

                bulkSyncProgress.setBounds(x, y, w, STAT_HEIGHT * 0.8f);
                y += STAT_HEIGHT * 0.8f + PADDING;

                btnClearCdnCache.setBounds(x + (w - btnW) / 2f, y, btnW, BTN_HEIGHT);
                y += BTN_HEIGHT + PADDING;

                return new ScrollBounds(visibleWidth, y);
            }
        });

        // ── Search text field ────────────────────────────────────────────────
        txtSearch = scroller.add(new FTextField());
        txtSearch.setFont(FSkinFont.get(12));
        txtSearch.setGhostText(Forge.getLocalizer().getMessage("lblSearch") + " " +
                Forge.getLocalizer().getMessage("lblCards") + "...");
        // Auto-refresh on every keystroke
        txtSearch.setLiveChangeEvents(true);
        txtSearch.setChangedHandler(e -> scheduleStatsUpdate());

        // ── Format / Sets combo — same options as the deck-browser filter ────
        cbxFormats = scroller.add(new FComboBox<>());
        cbxFormats.setFont(FSkinFont.get(12));
        cbxFormats.addItem(Forge.getLocalizer().getMessage("lblAllSetsFormats"));
        for (GameFormat fmt : FModel.getFormats().getFilterList()) {
            cbxFormats.addItem(fmt);
        }
        cbxFormats.addItem(Forge.getLocalizer().getMessage("lblOtherFormats"));
        cbxFormats.addItem(Forge.getLocalizer().getMessage("lblChooseSets"));
        selectedFormatText = cbxFormats.getText();

        cbxFormats.setChangedHandler(e -> {
            if (preventHandling) { return; }
            int idx = cbxFormats.getSelectedIndex();
            if (idx <= 0) {
                selectedFormat = null;
                if (idx == 0) {
                    selectedFormatText = cbxFormats.getText();
                    scheduleStatsUpdate();
                }
            } else if (idx == cbxFormats.getItemCount() - 2) {
                // "Other Formats" → archived-format picker
                preventHandling = true;
                cbxFormats.setText(selectedFormatText);
                preventHandling = false;
                ArchivedFormatSelect picker = new ArchivedFormatSelect();
                picker.setOnCloseCallBack(() -> {
                    selectedFormat = picker.getSelectedFormat();
                    if (selectedFormat != null) {
                        selectedFormatText = selectedFormat.getName();
                        cbxFormats.setText(selectedFormatText);
                        scheduleStatsUpdate();
                    }
                });
                Forge.openScreen(picker);
            } else if (idx == cbxFormats.getItemCount() - 1) {
                // "Choose Sets" → set picker
                preventHandling = true;
                cbxFormats.setText(selectedFormatText);
                preventHandling = false;
                Forge.openScreen(new MultiSetSelect());
            } else {
                selectedFormat = (GameFormat) cbxFormats.getSelectedItem();
                selectedFormatText = cbxFormats.getText();
                scheduleStatsUpdate();
            }
        });

        // ── Stats labels ─────────────────────────────────────────────────────
        lblTotal      = scroller.add(new FLabel.Builder().text("--").font(STAT_FONT).align(Align.center).build());
        lblDownloaded = scroller.add(new FLabel.Builder().text("--").font(STAT_FONT).align(Align.center).build());
        lblMissing    = scroller.add(new FLabel.Builder().text("--").font(STAT_FONT).align(Align.center).build());

        // ── Download button ──────────────────────────────────────────────────
        btnDownload = scroller.add(new FButton(Forge.getLocalizer().getMessage("btnDownloadCardImages")));
        btnDownload.setCommand(e -> startDownload());

        // ── Bulk data sync buttons ───────────────────────────────────────────
        btnSyncBulkData = scroller.add(new FButton(Forge.getLocalizer().getMessage("btnSyncBulkCardData")));
        btnSyncBulkData.setCommand(e -> startBulkSync(ScryfallBulkDataSync.BULK_TYPE_DEFAULT_CARDS, null, "English"));

        final Map<String, String> cardLangMapping = ForgeConstants.getScryfallCardLanguageMapping();
        cbxIndexLang = scroller.add(new FComboBox<>());
        cbxIndexLang.setFont(FSkinFont.get(12));
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

        cbPreferLangForUnique = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("cbPreferLangForUniqueCards"),
                FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_PREFER_LANG_FOR_UNIQUE_CARDS)));

        btnSyncBulkDataLang = scroller.add(new FButton(Forge.getLocalizer().getMessage("btnSyncBulkCardDataLang")));
        btnSyncBulkDataLang.setCommand(e -> {
            String selectedLangName = cbxIndexLang.getSelectedItem();
            String selectedLangCode = cardLangMapping.get(selectedLangName);
            FModel.getPreferences().setPref(ForgePreferences.FPref.UI_CARD_DOWNLOAD_LANG, selectedLangCode);
            FModel.getPreferences().setPref(ForgePreferences.FPref.UI_PREFER_LANG_FOR_UNIQUE_CARDS,
                    String.valueOf(cbPreferLangForUnique.isSelected()));
            FModel.getPreferences().save();
            applyPreferredLanguageAvailability(selectedLangCode);
            startBulkSync(ScryfallBulkDataSync.BULK_TYPE_ALL_CARDS, new HashSet<>(Arrays.asList("en", selectedLangCode)), selectedLangName);
        });
        bulkSyncProgress = scroller.add(new FProgressBar());

        // ── Clear CDN cache button ─────────────────────────────────────────────
        btnClearCdnCache = scroller.add(new FButton(Forge.getLocalizer().getMessage("btnClearCdnImageCache")));
        btnClearCdnCache.setCommand(e -> clearCdnCache());

        // Run initial stats for "All cards, all sets" as soon as the screen opens
        scheduleStatsUpdate();
    }

    // =========================================================================
    //  Stats computation  (non-blocking, discards stale results)
    // =========================================================================

    private void scheduleStatsUpdate() {
        final int gen = generation.incrementAndGet();

        // Show an in-progress marker immediately (we are on the EDT here)
        lblTotal.setText(Forge.getLocalizer().getMessage("lblTotalCards") + ": ...");
        lblDownloaded.setText(Forge.getLocalizer().getMessage("lblDownloaded") + ": ...");
        lblMissing.setText(Forge.getLocalizer().getMessage("lblMissing") + ": ...");

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

            // Only apply the result if no newer refresh has been triggered
            if (generation.get() != gen) { return; }
            final int fTotal = total, fDownloaded = downloaded, fMissing = missing;
            FThreads.invokeInEdtLater(() -> {
                if (generation.get() != gen) { return; }
                lblTotal.setText(Forge.getLocalizer().getMessage("lblTotalCards") + ": " + fTotal);
                lblDownloaded.setText(Forge.getLocalizer().getMessage("lblDownloaded") + ": " + fDownloaded);
                lblMissing.setText(Forge.getLocalizer().getMessage("lblMissing") + ": " + fMissing);
            });
        });
    }

    // =========================================================================
    //  Download
    // =========================================================================

    private void startDownload() {
        new GuiDownloader(new GuiDownloadFilteredCardImages(buildCurrentFilter()), finished -> {
            if (finished) { scheduleStatsUpdate(); } // re-check counts after download
        }).show();
    }

    // =========================================================================
    //  Bulk data sync: resolve CDN links for every set at once
    // =========================================================================

    /** Opens the screen and immediately starts a bulk sync that's already been confirmed elsewhere (e.g. the first-run prompt). Safe to call from any thread. */
    public static void openAndAutoStartBulkSync() {
        FThreads.invokeInEdtLater(() -> {
            CardImageBrowserScreen screen = new CardImageBrowserScreen();
            Forge.openScreen(screen);
            screen.runBulkSync(ScryfallBulkDataSync.BULK_TYPE_DEFAULT_CARDS, null, "English");
        });
    }

    private void applyPreferredLanguageAvailability(String langCode) {
        boolean preferForUnique = cbPreferLangForUnique.isSelected();
        if (!preferForUnique || langCode == null || langCode.isEmpty() || "en".equalsIgnoreCase(langCode)) {
            FModel.getMagicDb().setPreferredLanguageAvailability(null);
        } else {
            FModel.getMagicDb().setPreferredLanguageAvailability((setCode, cn) -> CdnUuidCache.isAvailableInLanguage(setCode, cn, langCode));
        }
    }

    private void startBulkSync(String bulkDataType, Set<String> allowedLangs, String langLabel) {
        // SOptionPane.showConfirmDialog() blocks its caller while the dialog renders on the EDT,
        // so it must never be called directly from a tap handler (which runs on the EDT itself)
        // -- that throws immediately and the whole method aborts before any UI update happens.
        FThreads.invokeInBackgroundThread(() -> {
            if (!SOptionPane.showConfirmDialog(Forge.getLocalizer().getMessage("lblSyncBulkCardDataConfirm", ScryfallBulkDataSync.approxSizeLabel(bulkDataType)))) {
                return;
            }
            runBulkSync(bulkDataType, allowedLangs, langLabel);
        });
    }

    /** Runs the sync itself; always hops onto its own background thread, so it's safe to call from the EDT or not. */
    private void runBulkSync(String bulkDataType, Set<String> allowedLangs, String langLabel) {
        FThreads.invokeInBackgroundThread(() -> {
            FThreads.invokeInEdtLater(() -> {
                btnDownload.setEnabled(false);
                btnSyncBulkData.setEnabled(false);
                btnSyncBulkDataLang.setEnabled(false);
                btnClearCdnCache.setEnabled(false);
                bulkSyncProgress.reset();
                bulkSyncProgress.setMaximum(100);
                bulkSyncProgress.setShowETA(false);
                bulkSyncProgress.setShowCount(false);
                bulkSyncProgress.setDescription("Starting...");
                bulkSyncProgress.setShowProgressTrail(true);
            });

            int setCount = ScryfallBulkDataSync.sync(bulkDataType, allowedLangs,
                    (message, fraction) -> FThreads.invokeInEdtLater(() -> {
                        bulkSyncProgress.setDescription(message);
                        if (fraction >= 0) {
                            bulkSyncProgress.setShowProgressTrail(false);
                            bulkSyncProgress.setValue((int) Math.round(fraction * 100));
                        } else {
                            bulkSyncProgress.setShowProgressTrail(true);
                        }
                    }),
                    () -> false);
            FThreads.invokeInEdtLater(() -> {
                btnDownload.setEnabled(true);
                btnSyncBulkData.setEnabled(true);
                btnSyncBulkDataLang.setEnabled(true);
                btnClearCdnCache.setEnabled(true);
                bulkSyncProgress.setShowProgressTrail(false);
                if (setCount >= 0) {
                    bulkSyncProgress.setValue(100);
                    bulkSyncProgress.setDescription(Forge.getLocalizer().getMessage("lblBulkCardDataSynced") + " (" + setCount + " sets) - " + langLabel);
                    scheduleStatsUpdate();
                } else {
                    bulkSyncProgress.setDescription("Bulk sync failed -- see log for details.");
                }
            });
        });
    }

    // =========================================================================
    //  CDN image lookup cache: clear
    // =========================================================================

    private void clearCdnCache() {
        // Same EDT restriction as startBulkSync() -- must not call SOptionPane directly from a
        // tap handler.
        FThreads.invokeInBackgroundThread(() -> {
            if (!SOptionPane.showConfirmDialog(Forge.getLocalizer().getMessage("lblClearCdnImageCacheConfirm"))) {
                return;
            }
            CdnUuidCache.clearCache();
            SOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblCdnImageCacheCleared"));
        });
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

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
    //  Layout
    // =========================================================================

    @Override
    protected void doLayout(float startY, float width, float height) {
        scroller.setBounds(0, startY, width, height - startY);
    }

    // =========================================================================
    //  "Choose Sets" inner screen  (mirrors FormatFilter.MultiSetSelect)
    // =========================================================================

    private class MultiSetSelect extends FScreen {
        private final Set<CardEdition>        selectedSets = new HashSet<>();
        private final FGroupList<CardEdition> lstSets      = add(new FGroupList<>());

        private MultiSetSelect() {
            super(Forge.getLocalizer().getMessage("lblChooseSets"));

            lstSets.addGroup("Core Sets");
            lstSets.addGroup("Expansions");
            lstSets.addGroup("Starter Sets");
            lstSets.addGroup("Reprint Sets");
            lstSets.addGroup("Boxed Sets");
            lstSets.addGroup("Collector's Edition");
            lstSets.addGroup("Duel Decks");
            lstSets.addGroup("Promo Sets");
            lstSets.addGroup("Digital Sets");
            lstSets.addGroup("Draft Innovation Sets");
            lstSets.addGroup("Commander Sets");
            lstSets.addGroup("Multiplayer Sets");
            lstSets.addGroup("Other Supplemental Sets");
            lstSets.addGroup("Funny Sets");
            lstSets.addGroup("Custom Sets");

            for (CardEdition set : FModel.getMagicDb().getSortedEditions()) {
                switch (set.getType()) {
                    case CORE:              lstSets.addItem(set,  0); break;
                    case EXPANSION:         lstSets.addItem(set,  1); break;
                    case STARTER:           lstSets.addItem(set,  2); break;
                    case REPRINT:           lstSets.addItem(set,  3); break;
                    case BOXED_SET:         lstSets.addItem(set,  4); break;
                    case COLLECTOR_EDITION: lstSets.addItem(set,  5); break;
                    case DUEL_DECK:         lstSets.addItem(set,  6); break;
                    case PROMO:             lstSets.addItem(set,  7); break;
                    case ONLINE:            lstSets.addItem(set,  8); break;
                    case DRAFT:             lstSets.addItem(set,  9); break;
                    case COMMANDER:         lstSets.addItem(set, 10); break;
                    case MULTIPLAYER:       lstSets.addItem(set, 11); break;
                    case OTHER:             lstSets.addItem(set, 12); break;
                    case FUNNY:             lstSets.addItem(set, 13); break;
                    default:                lstSets.addItem(set, 14); break;
                }
            }
            lstSets.setListItemRenderer(new SetRenderer());
        }

        @Override
        public void onClose(Consumer<Boolean> canCloseCallback) {
            if (!selectedSets.isEmpty()) {
                List<CardEdition> sorted = new ArrayList<>(selectedSets);
                Collections.sort(sorted);
                List<String> codes = new ArrayList<>();
                for (CardEdition ed : sorted) { codes.add(ed.getCode()); }
                selectedFormat     = new GameFormat(null, codes, null);
                selectedFormatText = sorted.size() > 1
                        ? TextUtil.join(codes, ", ")
                        : sorted.get(0).toString();
                cbxFormats.setText(selectedFormatText);
                scheduleStatsUpdate();
            }
            super.onClose(canCloseCallback);
        }

        @Override
        protected void doLayout(float startY, float width, float height) {
            lstSets.setBounds(0, startY, width, height - startY);
        }

        private class SetRenderer extends FList.ListItemRenderer<CardEdition> {
            @Override public float getItemHeight() { return Utils.AVG_FINGER_HEIGHT; }

            @Override
            public boolean tap(Integer index, CardEdition value, float x, float y, int count) {
                if (selectedSets.contains(value)) {
                    if (count == 2) { Forge.back(); } else { selectedSets.remove(value); }
                } else {
                    selectedSets.add(value);
                    if (count == 2) { Forge.back(); }
                }
                return true;
            }

            @Override
            public void drawValue(Graphics g, Integer index, CardEdition value, FSkinFont font,
                                  FSkinColor foreColor, FSkinColor backColor, boolean pressed,
                                  float x, float y, float w, float h) {
                float offset = SettingsScreen.getInsets(w) - FList.PADDING;
                x += offset; y += offset; w -= 2 * offset; h -= 2 * offset;

                float textH = h;
                h *= 0.66f;
                g.drawText(value.toString(), font, foreColor, x, y,
                        w - h - FList.PADDING, textH, false, Align.left, true);
                x += w - h;
                y += (textH - h) / 2;
                FCheckBox.drawCheckBox(g, SettingsScreen.DESC_COLOR, foreColor,
                        selectedSets.contains(value), x, y, h, h);
            }
        }
    }
}
