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

import java.util.Comparator;
import java.util.Map.Entry;

import com.google.common.base.Function;

import forge.item.CardPrinted;

/**
 * <p>
 * TableSorter class.
 * </p>
 * 
 * @param <T>
 *            the generic type
 * @author Forge
 * @version $Id$
 */
@SuppressWarnings("unchecked")
// Comparable needs <type>
public class TableSorter<T> implements Comparator<Entry<T, Integer>> {
    private final boolean ascending;
    private final Function<Entry<T, Integer>, Comparable<?>> field;

    /**
     * <p>
     * Constructor for TableSorter.
     * </p>
     * 
     * @param field
     *            the field
     * @param inAscending
     *            a boolean.
     */
    public TableSorter(final Function<Entry<T, Integer>, Comparable<?>> field, final boolean inAscending) {
        this.field = field;
        this.ascending = inAscending;
    }

    /** The Constant byNameThenSet. */
    public static final TableSorter<CardPrinted> BY_NAME_THEN_SET = new TableSorter<CardPrinted>(
            new Function<Entry<CardPrinted, Integer>, Comparable<?>>() {
                @Override
                public Comparable<?> apply(final Entry<CardPrinted, Integer> from) {
                    return from.getKey();
                }
            }, true);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public final int compare(final Entry<T, Integer> arg0, final Entry<T, Integer> arg1) {
        final Comparable obj1 = this.field.apply(arg0);
        final Comparable obj2 = this.field.apply(arg1);
        if (obj1 == null) {
            return -1;
        }
        if (obj2 == null) {
            return 1;
        }
        //System.out.println(String.format("%s vs %s _______ %s vs %s", arg0, arg1, obj1, obj2));
        return this.ascending ? obj1.compareTo(obj2) : obj2.compareTo(obj1);
    }
}
