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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import forge.FThreads;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.item.InventoryItem;
import forge.itemmanager.filters.AdvancedSearchFilter;
import forge.itemmanager.filters.ItemFilter;
import forge.itemmanager.filters.ListLabelFilter;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;
import forge.util.Localizer;
import forge.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
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
    // Data and number of choices for the list

    // Flag: was the dialog already shown?
    private boolean called;

    // initialized before; listeners may be added to it
    private FTextField txtSearch;
    private FLabel btnSearch;
    private ChoiceList lstChoices;
    private FOptionPane optionPane;
    private final Collection<T> list;
    private final Function<T, String> display;
    private final Callback<List<T>> callback;
    private AdvancedSearchFilter<? extends InventoryItem> advancedSearchFilter;

    public ListChooser(final String title, final int minChoices, final int maxChoices, final Collection<T> list0, final Function<T, String> display0, final Callback<List<T>> callback0) {
        FThreads.assertExecutedByEdt(true);
        list = list0;
        lstChoices = add(new ChoiceList(list, minChoices, maxChoices));
        display = display0;
        callback = callback0;

        //only show search field if more than 25 items and vertical layout
        if (list.size() > 25 && !lstChoices.getListItemRenderer().layoutHorizontal()) {
            txtSearch = add(new FTextField());
            txtSearch.setFont(FSkinFont.get(12));
            txtSearch.setGhostText(Localizer.getInstance().getMessage("lblSearch"));
            txtSearch.setChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    applyFilters();
                }
            });

            advancedSearchFilter = lstChoices.getListItemRenderer().getAdvancedSearchFilter(this);
            if (advancedSearchFilter != null) {
                btnSearch = add(new FLabel.ButtonBuilder()
                    .icon(Forge.hdbuttons ? FSkinImage.HDSEARCH : FSkinImage.SEARCH).iconScaleFactor(0.9f).command(new FEventHandler() {
                        @Override
                        public void handleEvent(FEvent e) {
                            FPopupMenu menu = new FPopupMenu() {
                                @Override
                                protected void buildMenu() {
                                    addItem(new FMenuItem(Localizer.getInstance().getMessage("lblAdvancedSearch"), Forge.hdbuttons ? FSkinImage.HDSEARCH : FSkinImage.SEARCH, new FEventHandler() {
                                        @Override
                                        public void handleEvent(FEvent e) {
                                            advancedSearchFilter.edit();
                                        }
                                    }));
                                    addItem(new FMenuItem(Localizer.getInstance().getMessage("lblResetFilters"), Forge.hdbuttons ? FSkinImage.HDDELETE : FSkinImage.DELETE, new FEventHandler() {
                                        @Override
                                        public void handleEvent(FEvent e) {
                                            resetFilters();
                                        }
                                    }));
                                }
                            };
                            menu.show(btnSearch, 0, btnSearch.getHeight());
                        }
                    }).build());
                add(advancedSearchFilter.getWidget());
            }
        }

        final List<String> options;
        if (minChoices == 0) {
            options = ImmutableList.of(Localizer.getInstance().getMessage("lblOK"), Localizer.getInstance().getMessage("lblCancel"));
        } else {
            options = ImmutableList.of(Localizer.getInstance().getMessage("lblOK"));
        }

        updateHeight();

        optionPane = new FOptionPane(null, null, title, null, this, options, 0, new Callback<Integer>() {
            @Override
            public void run(Integer result) {
                called = false;
                if (result == 0) {
                    callback.run(lstChoices.getSelectedItems());
                }
                else if (minChoices > 0) {
                    show(); //show if user tries to cancel when input is mandatory
                }
                else {
                    callback.run(new ArrayList<>());
                }
            }
        }) {
            @Override
            protected boolean padAboveAndBelow() {
                return false; //allow list to go straight up against buttons
            }
        };
    }

    public void resetFilters() {
        txtSearch.setText("");
        if (advancedSearchFilter != null) {
            advancedSearchFilter.reset();
            ItemFilter<? extends InventoryItem>.Widget widget = advancedSearchFilter.getWidget();
            if (widget.isVisible()) {
                widget.setVisible(false);
                revalidate();
            }
        }
        applyFilters();
    }

    @SuppressWarnings("unchecked")
    public void applyFilters() {
        lstChoices.clearSelection();

        List<Predicate<? super T>> predicates = new ArrayList<>();

        final String pattern = txtSearch.getText().toLowerCase();
        if (!pattern.isEmpty()) {
            predicates.add(new Predicate<T>() {
                @Override
                public boolean apply(T input) {
                    return lstChoices.getChoiceText(input).toLowerCase().contains(pattern);
                }
            });
        }
        if (advancedSearchFilter != null && !advancedSearchFilter.isEmpty()) {
            predicates.add((Predicate<? super T>)advancedSearchFilter.getPredicate());
        }

        if (predicates.isEmpty()) {
            lstChoices.setListData(list);
        }
        else {
            lstChoices.setListData(Iterables.filter(list, Predicates.and(predicates)));
        }

        if (!lstChoices.isEmpty() && lstChoices.getMaxChoices() > 0) {
            lstChoices.addSelectedIndex(0);
        }
        lstChoices.setScrollTop(0);
    }

    private void updateHeight() {
        boolean needRevalidate = getHeight() > 0; //needs to revalidate if already has height
        if (lstChoices.getListItemRenderer().layoutHorizontal()) {
            setHeight(Utils.AVG_FINGER_HEIGHT);
        }
        else {
            setHeight(Math.min(lstChoices.getListItemRenderer().getItemHeight() * list.size(), FOptionPane.getMaxDisplayObjHeight()));
        }
        if (needRevalidate) {
            optionPane.revalidate();
        }
    }

    public void show() {
        show(null, false);
    }

    /**
     * Shows the dialog and returns after the dialog was closed.
     *
     * @param index0 index to select when shown
     * @return a boolean.
     */
    public void show(final T item, final boolean selectMax) {
        if (called) {
            throw new IllegalStateException("Already shown");
        }
        called = true;
        if (item == null) {
            if (selectMax) {
                lstChoices.clearSelection();
                int max = Math.min(lstChoices.getMaxChoices(), list.size());
                for (int i = 0; i < max; i++) {
                    lstChoices.addSelectedIndex(i);
                }
            }
            else if (lstChoices.getMaxChoices() == 1) { //select first item only if single-select
                lstChoices.setSelectedIndex(0);
            }
            else {
                lstChoices.clearSelection();
            }
        }
        else {
            lstChoices.setSelectedItem(item);
        }
        optionPane.show();
    }

    @Override
    protected void doLayout(float width, float height) {
        float y = 0;
        if (txtSearch != null) {
            float fieldWidth = width;
            float fieldHeight = txtSearch.getHeight();
            float padding = fieldHeight * 0.25f;
            y += padding;
            if (btnSearch != null) {
                float buttonWidth = fieldHeight;
                btnSearch.setBounds(width - buttonWidth, y, buttonWidth, fieldHeight);
                fieldWidth -= buttonWidth + ItemFilter.PADDING;
            }
            txtSearch.setBounds(0, y, fieldWidth, fieldHeight);

            if (advancedSearchFilter != null && advancedSearchFilter.getWidget().isVisible()) {
                padding = ItemFilter.PADDING;
                y += fieldHeight + padding;
                fieldHeight = FTextField.getDefaultHeight(ListLabelFilter.LABEL_FONT);
                advancedSearchFilter.getWidget().setBounds(0, y, width, fieldHeight);
            }
            y += fieldHeight + padding;
        }
        lstChoices.setBounds(0, y, width, height - y);
    }

    private class ChoiceList extends FChoiceList<T> {
        private ChoiceList(Collection<T> items, int minChoices0, int maxChoices0) {
            super(items, minChoices0, maxChoices0);
        }

        @Override
        protected String getChoiceText(T choice) {
            if (display == null) {
                return choice.toString();
            }
            return display.apply(choice);
        }

        @Override
        protected void onSelectionChange() {
            final int num = getSelectionCount();
            optionPane.setButtonEnabled(0, (num >= minChoices) && (num <= maxChoices || maxChoices == -1));
        }

        @Override
        protected void onItemActivate(Integer index, T value) {
            if (optionPane.isButtonEnabled(0)) {
                optionPane.setResult(0);
            }
        }

        @Override
        protected void onCompactModeChange() {
            updateHeight(); //update height and scroll bounds based on compact mode change
        }

        @Override
        public void drawOverlay(Graphics g) {
            //don't draw border
        }
    }
}
