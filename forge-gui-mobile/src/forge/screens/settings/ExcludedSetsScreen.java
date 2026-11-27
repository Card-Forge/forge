package forge.screens.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.badlogic.gdx.math.Vector2;

import forge.Forge;
import forge.Graphics;
import forge.adventure.util.Config;
import forge.assets.FSkinFont;
import forge.card.CardEdition;
import forge.card.SetExclusions;
import forge.itemmanager.filters.MultiSetSelect;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FGroupList;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextArea;
import forge.toolbox.FTextField;
import forge.util.Utils;

public class ExcludedSetsScreen extends MultiSetSelect {
    private static final float PADDING = Utils.scale(5);
    private static final float SCROLL_BAR_WIDTH = Utils.scale(12);
    private static final float CHEVRON_SPACE = Utils.scale(16);

    private final Header header = new Header();
    private final HelpToggle btnHelp = header.add(new HelpToggle());
    private final FTextArea lblHelp = header.add(new FTextArea(false, Forge.getLocalizer().getMessage("lblExcludedSetsHelp")));
    private final Map<String, FCheckBox> tagBoxes = new LinkedHashMap<>();
    // sets with only their tagged prints excluded, and the note each set that ticked tags cover in part shows
    private final Set<CardEdition> partlySelectedSets = new HashSet<>();
    private final Map<CardEdition, String> partialNotes = new HashMap<>();
    private final FButton btnClear = header.add(new FButton(Forge.getLocalizer().getMessage("lblClearSelection")));
    private final FTextField txtSearch = header.add(new FTextField());
    private final ScrollBar scrollBar = add(new ScrollBar());
    private final Runnable onChanged;
    private boolean helpShown;

    public ExcludedSetsScreen(Runnable onChanged) {
        super(Forge.getLocalizer().getMessage("lblExcludedSets"), editionsOf(FModel.getExcludedEditionCodes()), null);
        this.onChanged = onChanged;
        lblHelp.setFont(FSkinFont.get(12));
        lblHelp.setTextColor(FLabel.getInlineLabelColor());
        txtSearch.setFont(FSkinFont.get(12));
        txtSearch.setGhostText(Forge.getLocalizer().getMessage("lblSearch"));
        txtSearch.setLiveChangeEvents(true);
        txtSearch.setChangedHandler(e -> filterSets());
        Set<String> excludedTags = FModel.getExcludedTags();
        for (String tag : FModel.getEditionTags()) {
            FCheckBox box = header.add(new FCheckBox(Forge.getLocalizer().getMessage("lblExcludeTag", FModel.getTagName(tag)),
                    excludedTags.contains(tag)));
            box.setFont(FSkinFont.get(12));
            List<CardEdition> tagged = editionsOf(FModel.getEditionCodesWithTag(tag));
            List<CardEdition> partlyTagged = editionsOf(FModel.getEditionCodesWithTaggedPrints(tag));
            box.setCommand(e -> {
                Set<CardEdition> couldBePartial = new HashSet<>(partialNotes.keySet());
                updatePartialNotes();
                List<String> tickedTags = getTickedTags();
                List<CardEdition> affected = new ArrayList<>(tagged);
                affected.addAll(partlyTagged);
                for (CardEdition edition : affected) {
                    // ticked when a ticked tag covers it whole, partly ticked when one covers it in part unless the
                    // player had ticked it whole, and unticked otherwise
                    boolean tickedWhole = couldBePartial.contains(edition) && selectedSets.contains(edition);
                    boolean partly = partialNotes.containsKey(edition);
                    boolean ticked = tickedTags.stream().anyMatch(edition::hasTag) || partly && tickedWhole;
                    if (ticked)
                        selectedSets.add(edition);
                    else
                        selectedSets.remove(edition);
                    if (partly && !ticked)
                        partlySelectedSets.add(edition);
                    else
                        partlySelectedSets.remove(edition);
                }
            });
            tagBoxes.put(tag, box);
        }
        btnClear.setCommand(e -> {
            boolean anySelected = !selectedSets.isEmpty() || !partlySelectedSets.isEmpty()
                    || tagBoxes.values().stream().anyMatch(FCheckBox::isSelected);
            if (!anySelected) {
                clearSelection();
                return;
            }
            // the button sits beside the search field, so a stray tap must not wipe a carefully built list
            FOptionPane.showConfirmDialog(Forge.getLocalizer().getMessage("lblConfirmClearExcludedSets"),
                    Forge.getLocalizer().getMessage("lblClearSelection"), Forge.getLocalizer().getMessage("lblYes"),
                    Forge.getLocalizer().getMessage("lblNo"), false, confirmed -> {
                        if (confirmed)
                            clearSelection();
                    });
        });
        updatePartialNotes();
        partlySelectedSets.addAll(editionsOf(FModel.getPartlyExcludedEditionCodes()));
        lstSets.setHeader(header);
        lstSets.setFont(FSkinFont.get(12));
        updateHelpLabel();
    }

    private void toggleHelp() {
        helpShown = !helpShown;
        updateHelpLabel();
        lstSets.revalidate();
    }

    private void clearSelection() {
        tagBoxes.values().forEach(box -> box.setSelected(false));
        selectedSets.clear();
        partlySelectedSets.clear();
        partialNotes.clear();
    }

    private void updateHelpLabel() {
        btnHelp.setText(Forge.getLocalizer().getMessage(helpShown ? "lblHideHelp" : "lblShowHelp"));
    }

    private List<String> getTickedTags() {
        List<String> ticked = new ArrayList<>();
        tagBoxes.forEach((tag, box) -> {
            if (box.isSelected())
                ticked.add(tag);
        });
        return ticked;
    }

    private void updatePartialNotes() {
        partialNotes.clear();
        List<String> tickedTags = getTickedTags();
        for (CardEdition edition : FModel.getMagicDb().getEditions()) {
            String note = FModel.getPartialExclusionNote(edition, tickedTags);
            if (note != null)
                partialNotes.put(edition, note);
        }
    }

    // a set that ticked tags cover in part cycles from partly ticked to ticked to unticked
    @Override
    protected void toggleSet(CardEdition set) {
        if (!partialNotes.containsKey(set))
            super.toggleSet(set);
        else if (partlySelectedSets.remove(set))
            selectedSets.add(set);
        else if (!selectedSets.remove(set))
            partlySelectedSets.add(set);
    }

    @Override
    protected boolean isPartlySelected(CardEdition set) {
        return partlySelectedSets.contains(set) && !selectedSets.contains(set);
    }

    @Override
    protected String getNoteText(CardEdition set) {
        return isPartlySelected(set) ? partialNotes.get(set) : null;
    }

    private static List<CardEdition> editionsOf(Collection<String> codes) {
        List<CardEdition> editions = new ArrayList<>();
        for (String code : codes) {
            CardEdition edition = FModel.getMagicDb().getEditions().get(code);
            if (edition != null)
                editions.add(edition);
        }
        return editions;
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        // Adventure has no home screen behind it, so there the picker takes the whole window
        return Forge.isMobileAdventureMode ? null : super.getLandscapeBackdropScreen();
    }

    @Override
    protected String getTagText(CardEdition set) {
        return set.getTags().stream().map(FModel::getTagName).collect(Collectors.joining(", "));
    }

    private void filterSets() {
        String pattern = txtSearch.getText().trim().toLowerCase();
        lstSets.setItemFilter(pattern.isEmpty() ? null : set -> set.toString().toLowerCase().contains(pattern)
                || getTagText(set).toLowerCase().contains(pattern));
    }

    @Override
    public void onClose(Consumer<Boolean> canCloseCallback) {
        List<String> codes = new ArrayList<>();
        for (CardEdition edition : selectedSets)
            codes.add(edition.getCode());
        List<String> partlyCodes = new ArrayList<>();
        for (CardEdition edition : partlySelectedSets)
            if (isPartlySelected(edition))
                partlyCodes.add(edition.getCode());
        Set<String> tags = SetExclusions.newCodeSet();
        tags.addAll(getTickedTags());
        if (FModel.setExcludedEditions(codes, partlyCodes, tags)) {
            Config.onExclusionChanged();
            if (onChanged != null)
                onChanged.run();
        }
        super.onClose(canCloseCallback);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        lstSets.setBounds(0, startY, width - SCROLL_BAR_WIDTH, height - startY);
        scrollBar.setBounds(width - SCROLL_BAR_WIDTH, startY, SCROLL_BAR_WIDTH, height - startY);
    }

    // the help, categories, Clear Selection and search scroll with the set list, so a portrait screen keeps room for it
    private class Header extends FGroupList.Header {
        @Override
        public float getPreferredHeight(float width) {
            return layout(width, false);
        }

        @Override
        protected void doLayout(float width, float height) {
            layout(width, true);
        }

        // landscape shows the help, two columns of categories, and the search box beside Clear Selection
        private float layout(float width, boolean apply) {
            boolean landscape = Forge.isLandscapeMode();
            float rowHeight = Utils.AVG_FINGER_HEIGHT;
            float innerWidth = width - 2 * PADDING;
            float y = PADDING;
            btnHelp.setVisible(!landscape);
            lblHelp.setVisible(landscape || helpShown);
            if (!landscape) {
                float h = btnHelp.getAutoSizeBounds().height;
                if (apply)
                    btnHelp.setBounds(PADDING, y, innerWidth, h);
                y += h + PADDING;
            }
            if (lblHelp.isVisible()) {
                float h = lblHelp.getPreferredHeight(innerWidth);
                if (apply)
                    lblHelp.setBounds(PADDING, y, innerWidth, h);
                y += h + PADDING;
            }
            // portrait keeps one column so long category names fit
            int columns = landscape ? 2 : 1;
            float columnWidth = innerWidth / columns;
            int i = 0;
            for (FCheckBox box : tagBoxes.values()) {
                if (apply)
                    box.setBounds(PADDING + (i % columns) * columnWidth, y + (i / columns) * rowHeight, columnWidth, rowHeight);
                i++;
            }
            y += (float) Math.ceil(i / (double) columns) * rowHeight;
            float searchHeight = txtSearch.getHeight();
            if (landscape) {
                float halfWidth = (innerWidth - PADDING) / 2;
                if (apply) {
                    txtSearch.setBounds(PADDING, y, halfWidth, searchHeight);
                    btnClear.setBounds(PADDING + halfWidth + PADDING, y, halfWidth, searchHeight);
                }
                y += searchHeight + PADDING;
            } else {
                if (apply) {
                    txtSearch.setBounds(PADDING, y, innerWidth, searchHeight);
                    btnClear.setBounds(PADDING, y + searchHeight + PADDING, innerWidth, searchHeight);
                }
                y += 2 * (searchHeight + PADDING);
            }
            return y;
        }
    }

    // a chevron, pointing right while the help is hidden and down while it shows, marks the label as expandable
    private class HelpToggle extends FLabel {
        private HelpToggle() {
            super(new FLabel.Builder().font(FSkinFont.get(12)).textColor(FLabel.getInlineLabelColor())
                    .insets(new Vector2(CHEVRON_SPACE, 0)).command(e -> toggleHelp()));
        }

        @Override
        public void draw(Graphics g) {
            super.draw(g);
            float size = Utils.scale(8);
            float x = (CHEVRON_SPACE - size) / 2;
            float y = getHeight() / 2;
            if (helpShown)
                g.fillTriangle(FLabel.getInlineLabelColor(), x, y - size / 4, x + size, y - size / 4, x + size / 2, y + size / 4);
            else
                g.fillTriangle(FLabel.getInlineLabelColor(), x, y - size / 2, x, y + size / 2, x + size / 2, y);
        }
    }

    private class ScrollBar extends FDisplayObject {
        private float getThumbHeight() {
            return Math.max(getHeight() * lstSets.getHeight() / lstSets.getScrollHeight(), Utils.AVG_FINGER_HEIGHT / 2);
        }

        private void scrollTo(float y) {
            float thumbHeight = getThumbHeight();
            float fraction = Math.min(1, Math.max(0, (y - thumbHeight / 2) / (getHeight() - thumbHeight)));
            lstSets.setScrollTop(fraction * lstSets.getMaxScrollTop());
        }

        @Override
        public boolean press(float x, float y) {
            scrollTo(y);
            return true;
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
            scrollTo(y);
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float maxScrollTop = lstSets.getMaxScrollTop();
            if (maxScrollTop <= 0)
                return;
            float w = getWidth() / 2;
            float x = w / 2;
            float thumbHeight = getThumbHeight();
            float thumbTop = (getHeight() - thumbHeight) * lstSets.getScrollTop() / maxScrollTop;
            g.fillRoundRect(FScrollPane.getIndicatorColor().alphaColor(0.2f), x, 0, w, getHeight(), w / 2);
            g.fillRoundRect(FScrollPane.getIndicatorColor(), x, thumbTop, w, thumbHeight, w / 2);
        }
    }
}
