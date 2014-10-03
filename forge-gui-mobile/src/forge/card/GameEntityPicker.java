package forge.card;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.screens.TabPageScreen;
import forge.toolbox.FChoiceList;
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
                new PickerTab(choiceList, "Choices", FSkinImage.DECKLIST),
                new PickerTab(revealList, revealListCaption, revealListImage)
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

        setHeight(Math.min(((PickerTab)tabPages[0]).list.getListItemRenderer().getItemHeight() * Math.max(choiceList.size(), revealList.size()), FOptionPane.getMaxDisplayObjHeight() + FOptionPane.PADDING)); //add PADDING to account for the lack of top padding above tabs
    }

    public void show() {
        optionPane.show();
    }

    private static class PickerTab extends TabPage<GameEntityPicker> {
        private final FTextField txtSearch;
        private final FChoiceList<GameEntityView> list;

        private PickerTab(Collection<? extends GameEntityView> items, String caption0, FImage icon0) {
            super(caption0 + " (" + items.size() + ")", icon0);
            txtSearch = add(new FTextField());
            txtSearch.setFont(FSkinFont.get(12));
            txtSearch.setGhostText("Search");
            txtSearch.setChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    /*String pattern = txtSearch.getText().toLowerCase();
                    list.clearSelection();
                    if (pattern.isEmpty()) {
                        lstChoices.setListData(list);
                    }
                    else {
                        List<T> filteredList = new ArrayList<T>();
                        for (T option : list) {
                            if (lstChoices.getChoiceText(option).toLowerCase().contains(pattern)) {
                                filteredList.add(option);
                            }
                        }
                        lstChoices.setListData(filteredList);
                    }
                    if (!lstChoices.isEmpty() && maxChoices > 0) {
                        lstChoices.addSelectedIndex(0);
                    }
                    lstChoices.setScrollTop(0);*/
                }
            });
            list = add(new FChoiceList<GameEntityView>(items));
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
