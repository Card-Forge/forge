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

import java.awt.Component;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JTable;

import org.apache.commons.lang3.StringUtils;

import forge.deck.Deck;
import forge.deck.DeckProxy;

/**
 * NAME column renderer for {@link forge.itemmanager.DeckManager}: shows deck display name and
 * uses the deck file comment as the cell tooltip when present.
 */
@SuppressWarnings("serial")
public final class DeckNameCommentRenderer extends ItemCellRenderer {

    private static final int TOOLTIP_WRAP_WIDTH = 72;

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value,
            final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        final JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        final DeckProxy proxy = deckProxyFromRow(table, row);
        if (proxy == null) {
            lbl.setToolTipText(null);
            return lbl;
        }
        final Deck deck = proxy.getDeck();
        final String comment = deck != null ? deck.getComment() : null;
        final String trimmed = StringUtils.trimToNull(comment);
        if (trimmed != null) {
            lbl.setToolTipText(toHtmlWrappedTooltip(trimmed));
        } else {
            lbl.setToolTipText(null);
        }
        return lbl;
    }

    private static DeckProxy deckProxyFromRow(final JTable table, final int row) {
        if (!(table.getModel() instanceof ItemListView.ItemTableModel)) {
            return null;
        }
        final ItemListView.ItemTableModel tm = (ItemListView.ItemTableModel) table.getModel();
        final Entry<?, Integer> entry = tm.rowToItem(row);
        if (entry != null && entry.getKey() instanceof DeckProxy) {
            return (DeckProxy) entry.getKey();
        }
        return null;
    }

    private static String toHtmlWrappedTooltip(final String comment) {
        final String[] lines = comment.split("\r\n|\n|\r", -1);
        final StringBuilder out = new StringBuilder("<html>");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append("<br/>");
            }
            appendWrappedEscapedLine(out, lines[i]);
        }
        out.append("</html>");
        return out.toString();
    }

    private static void appendWrappedEscapedLine(final StringBuilder out, final String line) {
        final String escaped = escapeHtml(line);
        if (escaped.isEmpty()) {
            return;
        }
        String remaining = escaped;
        boolean first = true;
        while (remaining.length() > TOOLTIP_WRAP_WIDTH) {
            int breakAt = remaining.lastIndexOf(' ', TOOLTIP_WRAP_WIDTH);
            if (breakAt <= 0) {
                breakAt = TOOLTIP_WRAP_WIDTH;
            }
            if (!first) {
                out.append("<br/>");
            }
            out.append(remaining, 0, breakAt);
            if (breakAt < remaining.length() && remaining.charAt(breakAt) == ' ') {
                remaining = remaining.substring(breakAt + 1);
            } else {
                remaining = remaining.substring(breakAt);
            }
            first = false;
        }
        if (!remaining.isEmpty()) {
            if (!first) {
                out.append("<br/>");
            }
            out.append(remaining);
        }
    }

    private static String escapeHtml(final String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
