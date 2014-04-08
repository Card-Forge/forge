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
package forge.itemmanager.views;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import forge.itemmanager.ColumnDef;
import forge.itemmanager.ItemColumnConfig;
import forge.itemmanager.ItemColumnConfig.SortState;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of methods pertaining to columns in card catalog and
 * current deck tables, for use in the deck editor.
 * <br><br>
 * <i>(S at beginning of class name denotes a static factory.)</i>
 * 
 */
public final class SColumnUtil {
    public static Map<ColumnDef, ItemColumnConfig> getColumns(Iterable<ColumnDef> colDefs) {
        int i = 0;
        final Map<ColumnDef, ItemColumnConfig> columns = new HashMap<ColumnDef, ItemColumnConfig>();
        for (ColumnDef colDef : colDefs) {
            ItemColumnConfig column = new ItemColumnConfig(colDef);
            column.setIndex(i++);
            columns.put(colDef, column);
        }
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getStringColumn() {
        Map<ColumnDef, ItemColumnConfig> columns = new HashMap<ColumnDef, ItemColumnConfig>();
        ItemColumnConfig column = new ItemColumnConfig(ColumnDef.STRING);
        column.setSortPriority(1);
        columns.put(ColumnDef.STRING, column);
        return columns;
    }

    private static Map<ColumnDef, ItemColumnConfig> getCardColumns(ColumnDef quantityColDef, boolean includeFavorite) {
        return getCardColumns(quantityColDef, includeFavorite, false, false, false, false);
    }
    private static Map<ColumnDef, ItemColumnConfig> getCardColumns(ColumnDef quantityColDef, boolean includeFavorite,
            boolean includeOwned, boolean includePrice, boolean includeNew, boolean includeDecks) {
        ArrayList<ColumnDef> colDefs = new ArrayList<ColumnDef>();
        if (includeFavorite) {
            colDefs.add(ColumnDef.FAVORITE);
        }
        if (quantityColDef != null) {
            colDefs.add(quantityColDef);
        }
        if (includeOwned) {
            colDefs.add(ColumnDef.OWNED);
        }
        colDefs.add(ColumnDef.NAME);
        if (includePrice) {
            colDefs.add(ColumnDef.PRICE);
        }
        if (includeNew) {
            colDefs.add(ColumnDef.NEW);
        }
        if (includeDecks) {
            colDefs.add(ColumnDef.DECKS);
        }
        colDefs.add(ColumnDef.COST);
        colDefs.add(ColumnDef.COLOR);
        colDefs.add(ColumnDef.TYPE);
        colDefs.add(ColumnDef.POWER);
        colDefs.add(ColumnDef.TOUGHNESS);
        colDefs.add(ColumnDef.CMC);
        colDefs.add(ColumnDef.RARITY);
        colDefs.add(ColumnDef.SET);
        colDefs.add(ColumnDef.AI);
        colDefs.add(ColumnDef.RANKING);

        Map<ColumnDef, ItemColumnConfig> columns = getColumns(colDefs);
        columns.get(ColumnDef.AI).setVisible(false);
        columns.get(ColumnDef.RANKING).setVisible(false);
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getCatalogDefaultColumns(boolean isInfinite) {
        Map<ColumnDef, ItemColumnConfig> columns = getCardColumns(isInfinite ? null : ColumnDef.QUANTITY, true);
        columns.get(ColumnDef.FAVORITE).setSortPriority(1);
        columns.get(ColumnDef.NAME).setSortPriority(2);
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getDeckEditorDefaultColumns() {
        Map<ColumnDef, ItemColumnConfig> columns = getCardColumns(ColumnDef.DECK_QUANTITY, false);
        columns.get(ColumnDef.CMC).setSortPriority(1);
        columns.get(ColumnDef.TYPE).setSortPriority(2);
        columns.get(ColumnDef.NAME).setSortPriority(3);
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getDeckViewerDefaultColumns() {
        Map<ColumnDef, ItemColumnConfig> columns = getCardColumns(ColumnDef.QUANTITY, false);
        columns.get(ColumnDef.CMC).setSortPriority(1);
        columns.get(ColumnDef.TYPE).setSortPriority(2);
        columns.get(ColumnDef.NAME).setSortPriority(3);
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getDraftPackDefaultColumns() {
        Map<ColumnDef, ItemColumnConfig> columns = getCardColumns(ColumnDef.QUANTITY, false);
        columns.get(ColumnDef.RARITY).setSortPriority(1); //sort rares to top
        columns.get(ColumnDef.RARITY).setSortState(SortState.DESC);
        columns.get(ColumnDef.COLOR).setSortPriority(2);
        columns.get(ColumnDef.NAME).setSortPriority(3);
        columns.get(ColumnDef.RANKING).setVisible(true);
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getSpecialCardPoolDefaultColumns() {
        ArrayList<ColumnDef> colDefs = new ArrayList<ColumnDef>();
        colDefs.add(ColumnDef.FAVORITE);
        colDefs.add(ColumnDef.NAME);
        colDefs.add(ColumnDef.TYPE);
        colDefs.add(ColumnDef.RARITY);
        colDefs.add(ColumnDef.SET);

        Map<ColumnDef, ItemColumnConfig> columns = getColumns(colDefs);
        columns.get(ColumnDef.FAVORITE).setSortPriority(1);
        columns.get(ColumnDef.NAME).setSortPriority(2);
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getSpellShopDefaultColumns() {
        Map<ColumnDef, ItemColumnConfig> columns = getCardColumns(ColumnDef.QUANTITY, false, true, true, false, false);
        columns.get(ColumnDef.OWNED).setSortPriority(1);
        columns.get(ColumnDef.PRICE).setSortPriority(2);
        columns.get(ColumnDef.NAME).setSortPriority(3);
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getQuestInventoryDefaultColumns() {
        Map<ColumnDef, ItemColumnConfig> columns = getCardColumns(ColumnDef.QUANTITY, false, false, true, true, true);
        columns.get(ColumnDef.NEW).setSortPriority(1);
        columns.get(ColumnDef.PRICE).setSortPriority(2);
        columns.get(ColumnDef.NAME).setSortPriority(3);
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getQuestEditorPoolDefaultColumns() {
        Map<ColumnDef, ItemColumnConfig> columns = getCardColumns(ColumnDef.QUANTITY, false, false, false, true, false);
        columns.get(ColumnDef.NEW).setSortPriority(1);
        columns.get(ColumnDef.NAME).setSortPriority(2);
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getQuestDeckEditorDefaultColumns() {
        Map<ColumnDef, ItemColumnConfig> columns = getCardColumns(ColumnDef.DECK_QUANTITY, false, false, false, true, true);
        columns.get(ColumnDef.CMC).setSortPriority(1);
        columns.get(ColumnDef.TYPE).setSortPriority(2);
        columns.get(ColumnDef.NAME).setSortPriority(3);
        return columns;
    }

    public static Map<ColumnDef, ItemColumnConfig> getDecksDefaultColumns(boolean allowEdit, boolean includeFolder) {
        ArrayList<ColumnDef> colDefs = new ArrayList<ColumnDef>();
        colDefs.add(ColumnDef.DECK_FAVORITE);
        if (allowEdit) {
            colDefs.add(ColumnDef.DECK_ACTIONS);
        }
        if (includeFolder) {
            colDefs.add(ColumnDef.DECK_FOLDER);
        }
        colDefs.add(ColumnDef.NAME);
        colDefs.add(ColumnDef.DECK_COLOR);
        colDefs.add(ColumnDef.DECK_FORMAT);
        colDefs.add(ColumnDef.DECK_EDITION);
        colDefs.add(ColumnDef.DECK_MAIN);
        colDefs.add(ColumnDef.DECK_SIDE);

        Map<ColumnDef, ItemColumnConfig> columns = getColumns(colDefs);
        columns.get(ColumnDef.DECK_FAVORITE).setSortPriority(1);
        if (includeFolder) {
            columns.get(ColumnDef.DECK_FOLDER).setSortPriority(2);
            columns.get(ColumnDef.NAME).setSortPriority(3);
        }
        else {
            columns.get(ColumnDef.NAME).setSortPriority(2);
        }
        return columns;
    }

    /**
     * Hides/shows a table column.
     * 
     * @param table JTable
     * @param col0 TableColumnInfo
     */
    public static void toggleColumn(final JTable table, final ItemTableColumn col0) {
        final TableColumnModel colmodel = table.getColumnModel();

        if (col0.isVisible()) {
            col0.setVisible(false);
            colmodel.removeColumn(col0);
        }
        else {
            col0.setVisible(true);
            colmodel.addColumn(col0);

            if (col0.getModelIndex() < colmodel.getColumnCount()) {
                colmodel.moveColumn(colmodel.getColumnIndex(col0.getIdentifier()), col0.getModelIndex());
                Enumeration<TableColumn> cols = colmodel.getColumns();
                int index = 0;
                // If you're getting renderer "can't cast T to U" errors, that's
                // a sign that the model index needs updating.
                while (cols.hasMoreElements()) {
                   cols.nextElement().setModelIndex(index++);
                }
            }
            else {
                col0.setModelIndex(colmodel.getColumnCount());
            }
        }
    }

    /**
     * Convenience method to get a column's index in the view (that is, in the TableColumnModel).
     * 
     * @param table
     * @param def
     * @return int
     */
    public static int getColumnViewIndex(final JTable table, final ColumnDef def) {
        int index = -1;

        try {
            index = table.getColumnModel().getColumnIndex(def);
        }
        catch (final Exception e) { }

        return index;
    }

    /**
     * Convenience method to get a column's index in the model (that is,
     * in the EditorTableModel, NOT the TableColumnModel).
     * 
     * @param table
     * @param def
     * @return int
     */
    public static int getColumnModelIndex(final JTable table, final ColumnDef def) {
        return table.getColumn(def).getModelIndex();
    }
}
