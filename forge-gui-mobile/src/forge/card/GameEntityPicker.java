package forge.card;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import com.google.common.collect.Lists;
import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.player.DelayedReveal;
import forge.game.zone.ZoneType;
import forge.screens.FScreen;
import forge.screens.TabPageScreen;
import forge.screens.match.views.VPlayerPanel;
import forge.toolbox.FChoiceList;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextField;
import forge.util.MessageUtil;
import org.apache.commons.lang3.StringUtils;

public class GameEntityPicker extends TabPageScreen<GameEntityPicker> {
    private final FOptionPane optionPane;

    public GameEntityPicker(String title, Collection<? extends GameEntityView> choiceList, DelayedReveal delayedReveal, boolean isOptional, final Consumer<GameEntityView> callback) {
        this(title, getTabs(choiceList, delayedReveal), isOptional, callback);
    }
    public GameEntityPicker(String title, Collection<? extends GameEntityView> choiceList, Collection<CardView> revealList, String revealListCaption, FImage revealListImage, boolean isOptional, final Consumer<GameEntityView> callback) {
        this(title, new PickerTab[] {
                new PickerTab(choiceList, Forge.getLocalizer().getMessage("lblChoices"), Forge.hdbuttons ? FSkinImage.HDCHOICE : FSkinImage.DECKLIST, 1),
                new PickerTab(revealList, revealListCaption, revealListImage, 0)
        }, isOptional, callback);
    }
    private GameEntityPicker(String title, PickerTab[] pickerTab, boolean isOptional, final Consumer<GameEntityView> callback) {
        super(pickerTab, false);

        setHeight(FOptionPane.getMaxDisplayObjHeight());

        optionPane = new FOptionPane(null, null, title, null, this,
                isOptional ? ImmutableList.of(Forge.getLocalizer().getMessage("lblOK"), Forge.getLocalizer().getMessage("lblCancel")) : ImmutableList.of(Forge.getLocalizer().getMessage("lblOK")), 0, result -> {
                    if (result == 0) {
                        callback.accept(((PickerTab) tabPages.get(0)).list.getSelectedItem());
                    }
                    else {
                        callback.accept(null);
                    }
                }) {
            @Override
            protected boolean padAboveAndBelow() {
                return false; //allow list to go straight up against buttons
            }
        };
    }

    private static PickerTab[] getTabs(Collection<? extends GameEntityView> choiceList, DelayedReveal delayedReveal) {
        List<PickerTab> tabs = Lists.newArrayList();
        tabs.add(new PickerTab(choiceList, Forge.getLocalizer().getMessage("lblChoices"), Forge.hdbuttons ? FSkinImage.HDCHOICE : FSkinImage.DECKLIST, 1));
        for (ZoneType zone : delayedReveal.getZone()) {
            final Collection<CardView> revealList = delayedReveal.getCards().stream().filter(c -> c.getZone() == zone).toList();
            final String revealListCaption = StringUtils.capitalize(MessageUtil.formatMessage("{player's} " + zone.getTranslatedName(), delayedReveal.getOwner(), delayedReveal.getOwner()));
            final FImage revealListImage = VPlayerPanel.iconFromZone(zone);
            tabs.add(new PickerTab(revealList, revealListCaption, revealListImage, 1));
        }
        return tabs.toArray(new PickerTab[0]);
    }

    public void show() {
        optionPane.show();
    }

    @Override
    protected boolean canActivateTabPage() {
        return true; //always allow activating tab pages while this is open
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null;
    }

    private static class PickerTab extends TabPage<GameEntityPicker> {
        private final FTextField txtSearch;
        private final FChoiceList<GameEntityView> list;

        private PickerTab(final Collection<? extends GameEntityView> items, String caption0, FImage icon0, final int maxChoices) {
            super(caption0 + " (" + items.size() + ")", icon0);
            txtSearch = add(new FTextField());
            txtSearch.setFont(FSkinFont.get(12));
            txtSearch.setGhostText(Forge.getLocalizer().getMessage("lblSearch"));
            txtSearch.setChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    String pattern = txtSearch.getText().toLowerCase();
                    list.clearSelection();
                    if (pattern.isEmpty()) {
                        list.setListData(items);
                    }
                    else {
                        List<GameEntityView> filteredList = new ArrayList<>();
                        for (GameEntityView option : items) {
                            if (option.toString().toLowerCase().contains(pattern)) {
                                filteredList.add(option);
                            }
                        }
                        list.setListData(filteredList);
                    }
                    if (!list.isEmpty() && maxChoices > 0) {
                        list.addSelectedIndex(0);
                    }
                    list.setScrollTop(0);
                }
            });
            list = add(new FChoiceList<GameEntityView>(items, maxChoices, maxChoices) {
                @Override
                protected void onItemActivate(Integer index, GameEntityView value) {
                    if (maxChoices > 0) {
                        parentScreen.optionPane.setResult(0);
                    }
                }

                @Override
                public void drawOverlay(Graphics g) {
                    //don't draw border
                }
            });
            if (maxChoices > 0) {
                list.addSelectedIndex(0);
            }
        }

        @Override
        protected void onActivate() {
            if (parentScreen.optionPane != null) {
                parentScreen.optionPane.setButtonEnabled(0, list.getMaxChoices() > 0);
            }
        }

        @Override
        protected void doLayout(float width, float height) {
            float padding = txtSearch.getHeight() * 0.25f;
            float y = padding;
            txtSearch.setBounds(0, y, width, txtSearch.getHeight());
            y += txtSearch.getHeight() + padding;
            list.setBounds(0, y, width, height - y);
        }
    }
}
