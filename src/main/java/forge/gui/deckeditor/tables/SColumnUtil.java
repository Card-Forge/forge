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
package forge.gui.deckeditor.tables;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.google.common.base.Function;

import forge.Singletons;
import forge.card.CardColor;
import forge.card.CardEdition;
import forge.card.CardManaCost;
import forge.card.CardRarity;
import forge.deck.DeckBase;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.InventoryItemFromSet;

/**
 * A collection of methods pertaining to columns in card catalog and
 * current deck tables, for use in the deck editor.
 * <br><br>
 * <i>(S at beginning of class name denotes a static factory.)</i>
 * 
 */
public final class SColumnUtil {
    /**
     * Each catalog column identified in the XML file is
     * referenced using these names. Its name in the XML
     * should match the name in the enum. Underscores
     * will be replaced with spaces in the display.
     * <br><br>
     * Note: To add a new column, put an enum here, and also add in the XML prefs file.
     */
    public enum ColumnName { /** */
        CAT_QUANTITY, /** */
        CAT_NAME, /** */
        CAT_COST, /** */
        CAT_COLOR, /** */
        CAT_TYPE, /** */
        CAT_POWER, /** */
        CAT_TOUGHNESS, /** */
        CAT_CMC, /** */
        CAT_RARITY, /** */
        CAT_SET, /** */
        CAT_AI, /** */
        CAT_NEW, /** */
        CAT_PURCHASE_PRICE, /** */
        DECK_QUANTITY, /** */
        DECK_NAME, /** */
        DECK_COST, /** */
        DECK_COLOR, /** */
        DECK_TYPE, /** */
        DECK_POWER, /** */
        DECK_TOUGHNESS, /** */
        DECK_CMC, /** */
        DECK_RARITY, /** */
        DECK_SET, /** */
        DECK_AI, /** */
        DECK_NEW, /** */
        DECK_SALE_PRICE, /** */
        DECK_DECKS;
    }

    /** Possible states of data sorting in a column: none, ascending, or descending. */
    public enum SortState { /** */
        NONE, /** */
        ASC, /** */
        DESC
    }

    /** @return List<TableColumnInfo<InventoryItem>> */
    public static List<TableColumnInfo<InventoryItem>> getCatalogDefaultColumns() {
        final List<TableColumnInfo<InventoryItem>> columns = new ArrayList<TableColumnInfo<InventoryItem>>();

        columns.add(SColumnUtil.getColumn(ColumnName.CAT_QUANTITY));
        columns.add(SColumnUtil.getColumn(ColumnName.CAT_NAME));
        columns.add(SColumnUtil.getColumn(ColumnName.CAT_COST));
        columns.add(SColumnUtil.getColumn(ColumnName.CAT_COLOR));
        columns.add(SColumnUtil.getColumn(ColumnName.CAT_TYPE));
        columns.add(SColumnUtil.getColumn(ColumnName.CAT_POWER));
        columns.add(SColumnUtil.getColumn(ColumnName.CAT_TOUGHNESS));
        columns.add(SColumnUtil.getColumn(ColumnName.CAT_CMC));
        columns.add(SColumnUtil.getColumn(ColumnName.CAT_RARITY));
        columns.add(SColumnUtil.getColumn(ColumnName.CAT_SET));
        columns.add(SColumnUtil.getColumn(ColumnName.CAT_AI));

        return columns;
    }

    /** @return List<TableColumnInfo<InventoryItem>> */
    public static List<TableColumnInfo<InventoryItem>> getDeckDefaultColumns() {
        final List<TableColumnInfo<InventoryItem>> columns = new ArrayList<TableColumnInfo<InventoryItem>>();

        columns.add(SColumnUtil.getColumn(ColumnName.DECK_QUANTITY));
        columns.add(SColumnUtil.getColumn(ColumnName.DECK_NAME));
        columns.add(SColumnUtil.getColumn(ColumnName.DECK_COST));
        columns.add(SColumnUtil.getColumn(ColumnName.DECK_COLOR));
        columns.add(SColumnUtil.getColumn(ColumnName.DECK_TYPE));
        columns.add(SColumnUtil.getColumn(ColumnName.DECK_POWER));
        columns.add(SColumnUtil.getColumn(ColumnName.DECK_TOUGHNESS));
        columns.add(SColumnUtil.getColumn(ColumnName.DECK_CMC));
        columns.add(SColumnUtil.getColumn(ColumnName.DECK_RARITY));
        columns.add(SColumnUtil.getColumn(ColumnName.DECK_SET));
        columns.add(SColumnUtil.getColumn(ColumnName.DECK_AI));

        return columns;
    }

    /** Should be called after column preferences has run, which has created a new column list.  */
    public static void attachSortAndDisplayFunctions() {
        SColumnUtil.getColumn(ColumnName.CAT_QUANTITY).setSortAndDisplayFunctions(
                SColumnUtil.FN_QTY_COMPARE, SColumnUtil.FN_QTY_GET);
        SColumnUtil.getColumn(ColumnName.CAT_NAME).setSortAndDisplayFunctions(
                SColumnUtil.FN_NAME_COMPARE, SColumnUtil.FN_NAME_GET);
        SColumnUtil.getColumn(ColumnName.CAT_COST).setSortAndDisplayFunctions(
                SColumnUtil.FN_COST_COMPARE, SColumnUtil.FN_COST_GET);
        SColumnUtil.getColumn(ColumnName.CAT_COLOR).setSortAndDisplayFunctions(
                SColumnUtil.FN_COLOR_COMPARE, SColumnUtil.FN_COLOR_GET);
        SColumnUtil.getColumn(ColumnName.CAT_TYPE).setSortAndDisplayFunctions(
                SColumnUtil.FN_TYPE_COMPARE, SColumnUtil.FN_TYPE_GET);
        SColumnUtil.getColumn(ColumnName.CAT_POWER).setSortAndDisplayFunctions(
                SColumnUtil.FN_POWER_COMPARE, SColumnUtil.FN_POWER_GET);
        SColumnUtil.getColumn(ColumnName.CAT_TOUGHNESS).setSortAndDisplayFunctions(
                SColumnUtil.FN_TOUGHNESS_COMPARE, SColumnUtil.FN_TOUGHNESS_GET);
        SColumnUtil.getColumn(ColumnName.CAT_CMC).setSortAndDisplayFunctions(
                SColumnUtil.FN_CMC_COMPARE, SColumnUtil.FN_CMC_GET);
        SColumnUtil.getColumn(ColumnName.CAT_RARITY).setSortAndDisplayFunctions(
                SColumnUtil.FN_RARITY_COMPARE, SColumnUtil.FN_RARITY_GET);
        SColumnUtil.getColumn(ColumnName.CAT_SET).setSortAndDisplayFunctions(
                SColumnUtil.FN_SET_COMPARE, SColumnUtil.FN_SET_GET);
        SColumnUtil.getColumn(ColumnName.CAT_AI).setSortAndDisplayFunctions(
                SColumnUtil.FN_AI_STATUS_COMPARE, SColumnUtil.FN_AI_STATUS_GET);

        SColumnUtil.getColumn(ColumnName.DECK_QUANTITY).setSortAndDisplayFunctions(
                SColumnUtil.FN_QTY_COMPARE, SColumnUtil.FN_QTY_GET);
        SColumnUtil.getColumn(ColumnName.DECK_NAME).setSortAndDisplayFunctions(
                SColumnUtil.FN_NAME_COMPARE, SColumnUtil.FN_NAME_GET);
        SColumnUtil.getColumn(ColumnName.DECK_COST).setSortAndDisplayFunctions(
                SColumnUtil.FN_COST_COMPARE, SColumnUtil.FN_COST_GET);
        SColumnUtil.getColumn(ColumnName.DECK_COLOR).setSortAndDisplayFunctions(
                SColumnUtil.FN_COLOR_COMPARE, SColumnUtil.FN_COLOR_GET);
        SColumnUtil.getColumn(ColumnName.DECK_TYPE).setSortAndDisplayFunctions(
                SColumnUtil.FN_TYPE_COMPARE, SColumnUtil.FN_TYPE_GET);
        SColumnUtil.getColumn(ColumnName.DECK_POWER).setSortAndDisplayFunctions(
                SColumnUtil.FN_POWER_COMPARE, SColumnUtil.FN_POWER_GET);
        SColumnUtil.getColumn(ColumnName.DECK_TOUGHNESS).setSortAndDisplayFunctions(
                SColumnUtil.FN_TOUGHNESS_COMPARE, SColumnUtil.FN_TOUGHNESS_GET);
        SColumnUtil.getColumn(ColumnName.DECK_CMC).setSortAndDisplayFunctions(
                SColumnUtil.FN_CMC_COMPARE, SColumnUtil.FN_CMC_GET);
        SColumnUtil.getColumn(ColumnName.DECK_RARITY).setSortAndDisplayFunctions(
                SColumnUtil.FN_RARITY_COMPARE, SColumnUtil.FN_RARITY_GET);
        SColumnUtil.getColumn(ColumnName.DECK_SET).setSortAndDisplayFunctions(
                SColumnUtil.FN_SET_COMPARE, SColumnUtil.FN_SET_GET);
        SColumnUtil.getColumn(ColumnName.DECK_AI).setSortAndDisplayFunctions(
                SColumnUtil.FN_AI_STATUS_COMPARE, SColumnUtil.FN_AI_STATUS_GET);

        SColumnUtil.getColumn(ColumnName.CAT_COST).setCellRenderer(new ManaCostRenderer());
        SColumnUtil.getColumn(ColumnName.CAT_POWER).setCellRenderer(new IntegerRenderer());
        SColumnUtil.getColumn(ColumnName.CAT_TOUGHNESS).setCellRenderer(new IntegerRenderer());
        SColumnUtil.getColumn(ColumnName.CAT_CMC).setCellRenderer(new IntegerRenderer());

        SColumnUtil.getColumn(ColumnName.DECK_COST).setCellRenderer(new ManaCostRenderer());
        SColumnUtil.getColumn(ColumnName.DECK_POWER).setCellRenderer(new IntegerRenderer());
        SColumnUtil.getColumn(ColumnName.DECK_TOUGHNESS).setCellRenderer(new IntegerRenderer());
        SColumnUtil.getColumn(ColumnName.DECK_CMC).setCellRenderer(new IntegerRenderer());
    }

    /**
     * Hides/shows a table column.
     * 
     * @param col0 TableColumnInfo<InventoryItem>
     * @param <TItem> extends InventoryItem
     * @param <TModel> extends DeckBase
     */
    @SuppressWarnings("unchecked")
    public static <TItem extends InventoryItem, TModel extends DeckBase>
        void toggleColumn(final TableColumnInfo<InventoryItem> col0) {

        final ACEditorBase<TItem, TModel> ed = (ACEditorBase<TItem, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        final JTable tbl = (col0.getEnumValue().substring(0, 4).equals("DECK"))
            ? ed.getTableDeck().getTable()
            : ed.getTableCatalog().getTable();

        final TableColumnModel colmodel = tbl.getColumnModel();

        if (col0.isShowing()) {
            col0.setShowing(false);
            colmodel.removeColumn(col0);
        }
        else {
            col0.setShowing(true);
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
     * Retrieve a custom column (uses identical method in SEditorIO).
     * 
     * @param id0 &emsp; {@link forge.gui.deckeditor.SEditorUtil.CatalogColumnName}
     * @return TableColumnInfo<InventoryItem>
     */
    public static TableColumnInfo<InventoryItem> getColumn(final ColumnName id0) {
        return SEditorIO.getColumn(id0);
    }

    /**
     * Convenience method to get a column's index in the view (that is,
     * in the TableColumnModel).
     * 
     * @param id0 &emsp; {@link forge.gui.deckeditor.SEditorUtil.CatalogColumnName}
     * @return int
     * @param <TItem> extends InventoryItem
     * @param <TModel> extends InventoryItem
     */
    @SuppressWarnings("unchecked")
    public static <TItem extends InventoryItem, TModel extends DeckBase>
                                    int getColumnViewIndex(final ColumnName id0) {

        final ACEditorBase<TItem, TModel> ed = (ACEditorBase<TItem, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        final JTable tbl = (id0.toString().substring(0, 4).equals("DECK"))
                ? ed.getTableDeck().getTable()
                : ed.getTableCatalog().getTable();

        int index = -1;

        try {
            index = tbl.getColumnModel().getColumnIndex(SColumnUtil.getColumn(id0).getIdentifier());
        }
        catch (final Exception e) { }

        return index;
    }

    /**
     * Convenience method to get a column's index in the model (that is,
     * in the TableModel, NOT the TableColumnModel).
     * 
     * @param id0 &emsp; {@link forge.gui.deckeditor.SEditorUtil.CatalogColumnName}
     * @return int
     * @param <TItem> extends InventoryItem
     * @param <TModel> extends InventoryItem
     */
    @SuppressWarnings("unchecked")
    public static <TItem extends InventoryItem, TModel extends DeckBase>
                                    int getColumnModelIndex(final ColumnName id0) {

        final ACEditorBase<TItem, TModel> ed = (ACEditorBase<TItem, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        final JTable tbl = (id0.toString().substring(0, 4).equals("DECK"))
                ? ed.getTableDeck().getTable()
                : ed.getTableCatalog().getTable();

        return tbl.getColumn(SColumnUtil.getColumn(id0).getIdentifier()).getModelIndex();
    }

    //========== Display functions

    private static final Pattern AE_FINDER = Pattern.compile("AE", Pattern.LITERAL);

    private static CardManaCost toManaCost(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getManaCost() : CardManaCost.EMPTY;
    }

    private static CardColor toColor(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getColor() : CardColor.getNullColor();
    }

    private static Integer toPower(final InventoryItem i) {
        Integer result = -1;
        if (i instanceof CardPrinted) {
            result = ((CardPrinted) i).getCard().getIntPower();
            if (result == null) {
                result = Integer.valueOf(((CardPrinted) i).getCard().getLoyalty());
                if (result == null) { result = -1; }
            }
        }
        return result;
    }

    private static Integer toToughness(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getIntToughness() : -1;
    }

    private static Integer toCMC(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getManaCost().getCMC() : -1;
    }

    private static CardRarity toRarity(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getRarity() : CardRarity.Unknown;
    }

    private static CardEdition toSetCmp(final InventoryItem i) {
        return i instanceof InventoryItemFromSet ? Singletons.getModel().getEditions()
                .get(((InventoryItemFromSet) i).getEdition()) : CardEdition.UNKNOWN;
    }

    private static String toSetStr(final InventoryItem i) {
        return i instanceof InventoryItemFromSet ? ((InventoryItemFromSet) i).getEdition() : "n/a";
    }

    private static Integer toAiCmp(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getAiStatusComparable() : Integer.valueOf(-1);
    }

    private static String toAiStr(final InventoryItem i) {
        return i instanceof CardPrinted ? ((CardPrinted) i).getCard().getAiStatus() : "n/a";
    }

    //==========

    /** Lamda sort fnQtyCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_QTY_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return from.getValue();
        }
    };

    /** Lamda sort fnQtyGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_QTY_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return from.getValue();
        }
    };

    /** Lamda sort fnNameCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_NAME_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return from.getKey().getName();
        }
    };

    /** Lamda sort fnNameGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_NAME_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            final String name = from.getKey().getName();
            return name.contains("AE") ? SColumnUtil.AE_FINDER.matcher(name).replaceAll("\u00C6") : name;
        }
    };

    /** Lamda sort fnCostCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_COST_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toManaCost(from.getKey());
        }
    };

    /** Lamda sort fnCostGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_COST_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toManaCost(from.getKey());
        }
    };

    /** Lamda sort fnColorCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_COLOR_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toColor(from.getKey());
        }
    };

    /** Lamda sort fnColorGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_COLOR_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toColor(from.getKey());
        }
    };

    /** Lamda sort fnTypeCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_TYPE_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return from.getKey().getType();
        }
    };

    /** Lamda sort fnTypeGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_TYPE_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return from.getKey().getType();
        }
    };

    /** Lamda sort fnPowerCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_POWER_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toPower(from.getKey());
        }
    };

    /** Lamda sort fnPowerGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_POWER_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toPower(from.getKey());
        }
    };

    /** Lamda sort fnToughnessCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_TOUGHNESS_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toToughness(from.getKey());
        }
    };

    /** Lamda sort fnToughnessGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_TOUGHNESS_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toToughness(from.getKey());
        }
    };

    /** Lamda sort fnCMCCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_CMC_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toCMC(from.getKey());
        }
    };

    /** Lamda sort fnCMCGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_CMC_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toCMC(from.getKey());
        }
    };

    /** Lamda sort fnRarityCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_RARITY_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toRarity(from.getKey());
        }
    };

    /** Lamda sort fnRarityGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_RARITY_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toRarity(from.getKey());
        }
    };

    /** Lamda sort fnSetCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_SET_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toSetCmp(from.getKey());
        }
    };

    /** Lamda sort fnSetGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_SET_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toSetStr(from.getKey());
        }
    };

    /** Lamda sort fnAiStatusCompare. */
    @SuppressWarnings("rawtypes")
    private static final Function<Entry<InventoryItem, Integer>, Comparable> FN_AI_STATUS_COMPARE = new Function<Entry<InventoryItem, Integer>, Comparable>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toAiCmp(from.getKey());
        }
    };

    /** Lamda sort fnAiStatusGet. */
    private static final Function<Entry<InventoryItem, Integer>, Object> FN_AI_STATUS_GET = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return SColumnUtil.toAiStr(from.getKey());
        }
    };
}
