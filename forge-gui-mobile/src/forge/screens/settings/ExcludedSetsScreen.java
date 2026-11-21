package forge.screens.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextArea;
import forge.toolbox.FTextField;
import forge.util.Utils;

public class ExcludedSetsScreen extends MultiSetSelect {
    private static final float PADDING = Utils.scale(5);
    private static final float SCROLL_BAR_WIDTH = Utils.scale(12);

    private final FTextArea lblHelp = add(new FTextArea(false, Forge.getLocalizer().getMessage("lblExcludedSetsHelp")));
    private final Map<String, FCheckBox> tagBoxes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final FButton btnClear = add(new FButton(Forge.getLocalizer().getMessage("lblClearSelection")));
    private final FTextField txtSearch = add(new FTextField());
    private final ScrollBar scrollBar = add(new ScrollBar());
    private final Runnable onChanged;

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
            FCheckBox box = add(new FCheckBox(Forge.getLocalizer().getMessage("lblExcludeTag", FModel.getTagName(tag)),
                    excludedTags.contains(tag)));
            List<CardEdition> tagged = editionsOf(FModel.getEditionCodesWithTag(tag));
            box.setCommand(e -> {
                if (box.isSelected())
                    selectedSets.addAll(tagged);
                else
                    tagged.forEach(selectedSets::remove);
            });
            tagBoxes.put(tag, box);
        }
        btnClear.setCommand(e -> {
            tagBoxes.values().forEach(box -> box.setSelected(false));
            selectedSets.clear();
        });
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
        Set<String> tags = SetExclusions.newCodeSet();
        tagBoxes.forEach((tag, box) -> {
            if (box.isSelected())
                tags.add(tag);
        });
        if (FModel.setExcludedEditions(codes, tags)) {
            Config.onExclusionChanged();
            if (onChanged != null)
                onChanged.run();
        }
        super.onClose(canCloseCallback);
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float rowHeight = Utils.AVG_FINGER_HEIGHT;
        float y = startY + PADDING;
        float helpHeight = lblHelp.getPreferredHeight(width - 2 * PADDING);
        lblHelp.setBounds(PADDING, y, width - 2 * PADDING, helpHeight);
        y += helpHeight + PADDING;
        for (FCheckBox box : tagBoxes.values()) {
            box.setBounds(PADDING, y, width - 2 * PADDING, rowHeight);
            y += rowHeight;
        }
        btnClear.setBounds(PADDING, y, width - 2 * PADDING, rowHeight);
        y += rowHeight + PADDING;
        txtSearch.setBounds(PADDING, y, width - 2 * PADDING, txtSearch.getHeight());
        y += txtSearch.getHeight() + PADDING;
        lstSets.setBounds(0, y, width - SCROLL_BAR_WIDTH, height - y);
        scrollBar.setBounds(width - SCROLL_BAR_WIDTH, y, SCROLL_BAR_WIDTH, height - y);
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
