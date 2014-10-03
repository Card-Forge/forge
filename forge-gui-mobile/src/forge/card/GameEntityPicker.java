package forge.card;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import forge.Forge;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.screens.TabPageScreen;
import forge.toolbox.FChoiceList;
import forge.toolbox.FDialog;
import forge.toolbox.FEvent;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextField;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;
import forge.view.CardView;
import forge.view.GameEntityView;

public class GameEntityPicker extends TabPageScreen<GameEntityPicker> {
    private final FOptionPane optionPane;

    public GameEntityPicker(String title, List<GameEntityView> choiceList, List<CardView> revealList, String revealListCaption, FImage revealListImage, boolean isOptional, final Callback<GameEntityView> callback) {
        super(new PickerTab[] {
                new PickerTab(choiceList, "Choices", FSkinImage.DECKLIST, 1),
                new PickerTab(revealList, revealListCaption, revealListImage, 0)
        }, false);

        optionPane = new FOptionPane(null, title, null, this,
                isOptional ? new String[] { "OK", "Cancel" } : new String[] { "OK" }, 0, new Callback<Integer>() {
                    @Override
                    public void run(Integer result) {
                        if (result == 0) {
                            callback.run(((PickerTab)tabPages[0]).list.getSelectedItem());
                        }
                        else {
                            callback.run(null);
                        }
                    }
                }) {
            @Override
            protected boolean padDisplayObject() {
                return false; //let tabs go right up to edges of dialog
            }
        };

        setHeight(Forge.getCurrentScreen().getHeight() - FDialog.TITLE_HEIGHT - 
                3 * FOptionPane.PADDING - FOptionPane.BUTTON_HEIGHT - FOptionPane.GAP_BELOW_BUTTONS);
    }

    public void show() {
        optionPane.show();
    }

    private static class PickerTab extends TabPage<GameEntityPicker> {
        private final FTextField txtSearch;
        private final FChoiceList<GameEntityView> list;

        private PickerTab(final Collection<? extends GameEntityView> items, String caption0, FImage icon0, final int maxChoices) {
            super(caption0 + " (" + items.size() + ")", icon0);
            txtSearch = add(new FTextField());
            txtSearch.setFont(FSkinFont.get(12));
            txtSearch.setGhostText("Search");
            txtSearch.setChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    String pattern = txtSearch.getText().toLowerCase();
                    list.clearSelection();
                    if (pattern.isEmpty()) {
                        list.setListData(items);
                    }
                    else {
                        List<GameEntityView> filteredList = new ArrayList<GameEntityView>();
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
            list = add(new FChoiceList<GameEntityView>(items, maxChoices, maxChoices));
            if (maxChoices > 0) {
                list.addSelectedIndex(0);
            }
        }

        @Override
        protected void doLayout(float width, float height) {
            float x = FOptionPane.PADDING;
            float y = FOptionPane.PADDING;
            float w = width - 2 * x;
            txtSearch.setBounds(x, y, w, txtSearch.getHeight());
            y += txtSearch.getHeight() * 1.25f;
            list.setBounds(x, y, w, height - y);
        }
    }
}
