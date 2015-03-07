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

import forge.FThreads;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FOptionPane;
import forge.util.Callback;

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
    private ChoiceList lstChoices;
    private FOptionPane optionPane;
    private final Collection<T> list;
    private final Function<T, String> display;
    private final Callback<List<T>> callback;

    public ListChooser(final String title, final int minChoices, final int maxChoices, final Collection<T> list0, final Function<T, String> display0, final Callback<List<T>> callback0) {
        FThreads.assertExecutedByEdt(true);
        list = list0;
        if (list.size() > 25) { //only show search field if more than 25 items
            txtSearch = add(new FTextField());
            txtSearch.setFont(FSkinFont.get(12));
            txtSearch.setGhostText("Search");
            txtSearch.setChangedHandler(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    String pattern = txtSearch.getText().toLowerCase();
                    lstChoices.clearSelection();
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
                    lstChoices.setScrollTop(0);
                }
            });
        }
        lstChoices = add(new ChoiceList(list, minChoices, maxChoices));
        display = display0;
        callback = callback0;

        String[] options;
        if (minChoices == 0) {
            options = new String[] {"OK", "Cancel"};
        }
        else {
            options = new String[] {"OK"};
        }

        updateHeight();

        optionPane = new FOptionPane(null, title, null, this, options, 0, new Callback<Integer>() {
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
                    callback.run(new ArrayList<T>());
                }
            }
        });
    }

    private void updateHeight() {
        boolean needRevalidate = getHeight() > 0; //needs to revalidate if already has height
        setHeight(Math.min(lstChoices.getListItemRenderer().getItemHeight() * list.size(), FOptionPane.getMaxDisplayObjHeight()));
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
            float padding = txtSearch.getHeight() * 0.25f;
            y += padding;
            txtSearch.setBounds(0, y, width, txtSearch.getHeight());
            y += txtSearch.getHeight() + padding;
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
