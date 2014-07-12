/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package forge.toolbox;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.base.Function;

import forge.FThreads;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.assets.FSkinColor.Colors;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.item.PaperCard;
import forge.screens.match.FControl;
import forge.screens.match.views.VAvatar;
import forge.screens.match.views.VStack;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.util.Callback;
import forge.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A simple class that shows a list of choices in a dialog. Two properties
 * influence the behavior of a list chooser: minSelection and maxSelection.
 * These two give the allowed number of selected items for the dialog to be
 * closed. A negative value for minSelection suggests that the list is revealed
 * and the choice doesn't matter.
 * <ul>
 * <li>If minSelection is 0, there will be a Cancel button.</li>
 * <li>If minSelection is -1, 0 or 1, double-clicking a choice will also close the
 * dialog.</li>
 * <li>If the number of selections is out of bounds, the "OK" button is
 * disabled.</li>
 * <li>The dialog was "committed" if "OK" was clicked or a choice was double
 * clicked.</li>
 * <li>The dialog was "canceled" if "Cancel" or "X" was clicked.</li>
 * <li>If the dialog was canceled, the selection will be empty.</li>
 * <li>
 * </ul>
 * 
 * @param <T>
 *            the generic type
 * @author Forge
 * @version $Id: ListChooser.java 25183 2014-03-14 23:09:45Z drdev $
 */
public class ListChooser<T> extends FContainer {
    public static final FSkinColor ITEM_COLOR = FSkinColor.get(Colors.CLR_ZEBRA);
    public static final FSkinColor ALT_ITEM_COLOR = ITEM_COLOR.getContrastColor(-20);
    public static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);
    public static final FSkinColor BORDER_COLOR = FList.FORE_COLOR;
    public static final float DEFAULT_ITEM_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.75f;

    // Data and number of choices for the list
    private int minChoices, maxChoices;

    // Flag: was the dialog already shown?
    private boolean called;

    // initialized before; listeners may be added to it
    private FTextField txtSearch;
    private ChoiceList lstChoices;
    private FOptionPane optionPane;
    private final Function<T, String> display;
    private final Callback<List<T>> callback;

    public ListChooser(final String title, final int minChoices0, final int maxChoices0, final Collection<T> list, final Function<T, String> display0, final Callback<List<T>> callback0) {
        FThreads.assertExecutedByEdt(true);
        minChoices = minChoices0;
        maxChoices = maxChoices0;
        if (list.size() > 25) { //only show search field if more than 25 items
            txtSearch = add(new FTextField());
            txtSearch.setFont(FSkinFont.get(12));
            txtSearch.setGhostText("Search");
            txtSearch.setChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    String pattern = txtSearch.getText().toLowerCase();
                    lstChoices.selectedIndices.clear();
                    if (pattern.isEmpty()) {
                        lstChoices.setListData(list);
                    }
                    else {
                        List<T> filteredList = new ArrayList<T>();
                        for (T option : list) {
                            if (getChoiceText(option).toLowerCase().contains(pattern)) {
                                filteredList.add(option);
                            }
                        }
                        lstChoices.setListData(filteredList);
                    }
                    if (!lstChoices.isEmpty() && maxChoices > 0) {
                        lstChoices.selectedIndices.add(0);
                    }
                    lstChoices.setScrollTop(0);
                    onSelectionChange();
                }
            });
        }
        lstChoices = add(new ChoiceList(list));
        display = display0;
        callback = callback0;

        String[] options;
        if (minChoices == 0) {
            options = new String[] {"OK","Cancel"};
        }
        else {
            options = new String[] {"OK"};
        }

        setHeight(Math.min(lstChoices.getListItemRenderer().getItemHeight() * list.size(), FOptionPane.getMaxDisplayObjHeight()));

        optionPane = new FOptionPane(null, title, null, this, options, 0, new Callback<Integer>() {
            @Override
            public void run(Integer result) {
                called = false;
                if (result == 0) {
                    List<T> choices = new ArrayList<T>();
                    for (int i : lstChoices.selectedIndices) {
                        choices.add(lstChoices.getItemAt(i));
                    }
                    callback.run(choices);
                }
                else if (minChoices > 0) {
                    show(); //show if user tries to cancel when input is mandatory
                }
                else {
                    callback.run(new ArrayList<T>());
                }
            }
        });
    }

    public void show() {
        show(null);
    }

    /**
     * Shows the dialog and returns after the dialog was closed.
     * 
     * @param index0 index to select when shown
     * @return a boolean.
     */
    public void show(final T item) {
        if (called) {
            throw new IllegalStateException("Already shown");
        }
        called = true;
        lstChoices.selectedIndices.clear();
        if (item == null) {
            if (maxChoices == 1) { //select first item only if single-select
                lstChoices.selectedIndices.add(0);
            }
        }
        else {
            lstChoices.selectedIndices.add(lstChoices.getIndexOf(item));
        }
        onSelectionChange();
        optionPane.show();
    }

    private void onSelectionChange() {
        final int num = lstChoices.selectedIndices.size();
        optionPane.setButtonEnabled(0, (num >= minChoices) && (num <= maxChoices || maxChoices == -1));
    }

    private String getChoiceText(T choice) {
        if (display == null) {
            return choice.toString();
        }
        return display.apply(choice);
    }

    @Override
    protected void doLayout(float width, float height) {
        float y = 0;
        if (txtSearch != null) {
            txtSearch.setBounds(0, 0, width, txtSearch.getHeight());
            y += txtSearch.getHeight() * 1.25f;
        }
        lstChoices.setBounds(0, y, width, height - y);
    }

    private abstract class ItemRenderer {
        public abstract FSkinFont getDefaultFont();
        public abstract float getItemHeight();
        public abstract boolean tap(T value, float x, float y, int count);
        public abstract boolean longPress(T value, float x, float y);
        public abstract void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h);
    }
    private class DefaultItemRenderer extends ItemRenderer {
        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(12);
        }

        @Override
        public float getItemHeight() {
            return DEFAULT_ITEM_HEIGHT;
        }

        @Override
        public boolean tap(T value, float x, float y, int count) {
            return false;
        }

        @Override
        public boolean longPress(T value, float x, float y) {
            return false;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            g.drawText(getChoiceText(value), font, foreColor, x, y, w, h, false, HAlignment.LEFT, true);
        }
    }
    //special renderer for cards
    private class PaperCardItemRenderer extends ItemRenderer {
        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(14);
        }

        @Override
        public float getItemHeight() {
            return CardRenderer.getCardListItemHeight();
        }

        @Override
        public boolean tap(T value, float x, float y, int count) {
            return CardRenderer.cardListItemTap((PaperCard)value, x, y, count);
        }

        @Override
        public boolean longPress(T value, float x, float y) {
            CardZoom.show((PaperCard)value);
            return true;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            CardRenderer.drawCardListItem(g, font, foreColor, (PaperCard)value, 0, x, y, w, h);
        }
    }
    //special renderer for cards
    private class CardItemRenderer extends ItemRenderer {
        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(14);
        }

        @Override
        public float getItemHeight() {
            return CardRenderer.getCardListItemHeight();
        }

        @Override
        public boolean tap(T value, float x, float y, int count) {
            return CardRenderer.cardListItemTap((Card)value, x, y, count);
        }

        @Override
        public boolean longPress(T value, float x, float y) {
            CardZoom.show((Card)value);
            return true;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            CardRenderer.drawCardListItem(g, font, foreColor, (Card)value, 0, x, y, w, h);
        }
    }
    //special renderer for SpellAbilities
    private class SpellAbilityItemRenderer extends ItemRenderer {
        private final TextRenderer textRenderer = new TextRenderer(true);

        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(14);
        }

        @Override
        public float getItemHeight() {
            return VStack.CARD_HEIGHT + 2 * FList.PADDING;
        }

        @Override
        public boolean tap(T value, float x, float y, int count) {
            if (x <= VStack.CARD_WIDTH + 2 * FList.PADDING) {
                CardZoom.show(((SpellAbility)value).getHostCard());
                return true;
            }
            return false;
        }

        @Override
        public boolean longPress(T value, float x, float y) {
            CardZoom.show(((SpellAbility)value).getHostCard());
            return true;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            SpellAbility spellAbility = (SpellAbility)value;
            CardRenderer.drawCardWithOverlays(g, spellAbility.getHostCard(), x, y, VStack.CARD_WIDTH, VStack.CARD_HEIGHT);

            float dx = VStack.CARD_WIDTH + FList.PADDING;
            x += dx;
            w -= dx;
            textRenderer.drawText(g, spellAbility.toString(), font, foreColor, x, y, w, h, y, h, true, HAlignment.LEFT, true);
        }
    }
    private class PlayerItemRenderer extends ItemRenderer {
        @Override
        public FSkinFont getDefaultFont() {
            return FSkinFont.get(18);
        }

        @Override
        public float getItemHeight() {
            return VAvatar.HEIGHT;
        }

        @Override
        public boolean tap(T value, float x, float y, int count) {
            return false;
        }

        @Override
        public boolean longPress(T value, float x, float y) {
            return false;
        }

        @Override
        public void drawValue(Graphics g, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
            Player player = (Player)value;
            g.drawImage(FControl.getPlayerAvatar(player), x - FList.PADDING, y - FList.PADDING, VAvatar.WIDTH, VAvatar.HEIGHT);
            x += VAvatar.WIDTH;
            w -= VAvatar.WIDTH;
            g.drawText(player.getName() + " (" + player.getLife() + ")", font, foreColor, x, y, w, h, false, HAlignment.LEFT, true);
        }
    }

    private class ChoiceList extends FList<T> {
        private List<Integer> selectedIndices = new ArrayList<Integer>();

        private ChoiceList(Collection<T> items) {
            super(items);

            //determine renderer from item type
            final ItemRenderer renderer;
            T item = items.iterator().next();
            if (item instanceof PaperCard) {
                renderer = new PaperCardItemRenderer();
            }
            else if (item instanceof Card) {
                renderer = new CardItemRenderer();
            }
            else if (item instanceof SpellAbility) {
                renderer = new SpellAbilityItemRenderer();
            }
            else if (item instanceof Player) {
                renderer = new PlayerItemRenderer();
            }
            else {
                renderer = new DefaultItemRenderer();
            }
            setListItemRenderer(new ListItemRenderer<T>() {
                private Integer prevTapIndex = -1;

                @Override
                public float getItemHeight() {
                    return renderer.getItemHeight();
                }

                @Override
                public boolean tap(Integer index, T value, float x, float y, int count) {
                    if (maxChoices > 1) {
                        if (selectedIndices.contains(index)) {
                            selectedIndices.remove(index);
                            onSelectionChange();
                        }
                        else if (selectedIndices.size() < maxChoices) {
                            selectedIndices.add(index);
                            Collections.sort(selectedIndices); //ensure selected indices are sorted
                            onSelectionChange();
                        }
                    }
                    else if (maxChoices > 0 && !selectedIndices.contains(index)) {
                        selectedIndices.clear();
                        selectedIndices.add(index);
                        onSelectionChange();
                    }
                    if (renderer.tap(value, x, y, count)) {
                        prevTapIndex = index;
                        return true; //don't activate if renderer handles tap
                    }
                    if (count == 2 && index == prevTapIndex && optionPane.isButtonEnabled(0)) {
                        optionPane.setResult(0);
                    }
                    prevTapIndex = index;
                    return true;
                }

                @Override
                public boolean showMenu(Integer index, T value, FDisplayObject owner, float x, float y) {
                    return renderer.longPress(value, x, y);
                }

                @Override
                public void drawValue(Graphics g, Integer index, T value, FSkinFont font, FSkinColor foreColor, boolean pressed, float x, float y, float w, float h) {
                    if (maxChoices > 1) {
                        if (pressed) { //if multi-select mode, draw SEL_COLOR when pressed
                            g.fillRect(SEL_COLOR, x - FList.PADDING, y - FList.PADDING, w + 2 * FList.PADDING, h + 2 * FList.PADDING);
                        }
                        //draw checkbox, with it checked based on whether item is selected
                        float checkBoxSize = h / 2;
                        float padding = checkBoxSize / 2;
                        w -= checkBoxSize + padding;
                        FCheckBox.drawCheckBox(g, selectedIndices.contains(index), x + w, y + padding, checkBoxSize, checkBoxSize);
                        w -= padding;
                    }
                    renderer.drawValue(g, value, font, foreColor, pressed, x, y, w, h);
                }
            });
            setFont(renderer.getDefaultFont());
        }

        @Override
        protected void drawBackground(Graphics g) {
            //draw no background
        }

        @Override
        public void drawOverlay(Graphics g) {
            g.drawRect(1.5f, BORDER_COLOR, 0, 0, getWidth(), getHeight());
        }

        @Override
        protected FSkinColor getItemFillColor(int index) {
            if (maxChoices == 1 && selectedIndices.contains(index)) {
                return SEL_COLOR; //don't show SEL_COLOR if in multi-select mode
            }
            if (index % 2 == 1) {
                return ALT_ITEM_COLOR;
            }
            return ITEM_COLOR;
        }

        @Override
        protected boolean drawLineSeparators() {
            return false;
        }
    }
}
