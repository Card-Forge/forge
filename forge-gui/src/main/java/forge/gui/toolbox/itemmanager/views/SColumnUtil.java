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
package forge.gui.toolbox.itemmanager.views;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import forge.gui.toolbox.itemmanager.views.ItemColumn.ColumnDef;

/**
 * A collection of methods pertaining to columns in card catalog and
 * current deck tables, for use in the deck editor.
 * <br><br>
 * <i>(S at beginning of class name denotes a static factory.)</i>
 * 
 */
public final class SColumnUtil {
    public static Map<ColumnDef, ItemColumn> getColumns(ColumnDef... colDefs) {
        int i = 0;
        final Map<ColumnDef, ItemColumn> columns = new HashMap<ColumnDef, ItemColumn>();
        for (ColumnDef colDef : colDefs) {
            ItemColumn column = new ItemColumn(colDef);
            column.setIndex(i++);
            columns.put(colDef, column);
        }
        return columns;
    }

    public static Map<ColumnDef, ItemColumn> getCatalogDefaultColumns() {
        Map<ColumnDef, ItemColumn> columns = getColumns(
                ColumnDef.FAVORITE,
                ColumnDef.QUANTITY,
                ColumnDef.NAME,
                ColumnDef.COST,
                ColumnDef.COLOR,
                ColumnDef.TYPE,
                ColumnDef.POWER,
                ColumnDef.TOUGHNESS,
                ColumnDef.CMC,
                ColumnDef.RARITY,
                ColumnDef.SET,
                ColumnDef.AI,
                ColumnDef.RANKING);
        columns.get(ColumnDef.FAVORITE).setSortPriority(1);
        columns.get(ColumnDef.NAME).setSortPriority(2);
        columns.get(ColumnDef.AI).hide();
        columns.get(ColumnDef.RANKING).hide();
        return columns;
    }

    public static Map<ColumnDef, ItemColumn> getDeckDefaultColumns() {
        Map<ColumnDef, ItemColumn> columns = getColumns(
                ColumnDef.DECK_QUANTITY,
                ColumnDef.NAME,
                ColumnDef.COST,
                ColumnDef.COLOR,
                ColumnDef.TYPE,
                ColumnDef.POWER,
                ColumnDef.TOUGHNESS,
                ColumnDef.CMC,
                ColumnDef.RARITY,
                ColumnDef.SET,
                ColumnDef.AI,
                ColumnDef.RANKING);
        columns.get(ColumnDef.CMC).setSortPriority(1);
        columns.get(ColumnDef.TYPE).setSortPriority(2);
        columns.get(ColumnDef.NAME).setSortPriority(3);
        columns.get(ColumnDef.AI).hide();
        columns.get(ColumnDef.RANKING).hide();
        return columns;
    }

    /**
     * Hides/shows a table column.
     * 
     * @param table JTable
     * @param col0 TableColumnInfo
     */
    public static void toggleColumn(final JTable table, final ItemColumn col0) {
        final TableColumnModel colmodel = table.getColumnModel();

        if (!col0.isHidden()) {
            col0.hide();
            colmodel.removeColumn(col0);
        }
        else {
            col0.show();
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
