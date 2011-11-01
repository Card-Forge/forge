package forge.gui.deckeditor;

import java.awt.Component;
import java.awt.Graphics;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import arcane.ui.util.ManaSymbols;
import forge.card.CardManaCost;
import forge.card.CardManaCostShard;

/**
 * Displays mana cost as symbols.
 */
public class ManaCostRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1770527102334163549L;

    private CardManaCost value;

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent
     * (javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
     */
    @Override
    public final Component getTableCellRendererComponent(final JTable table, final Object value,
            final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        this.value = (CardManaCost) value;
        this.setToolTipText(this.value.toString());
        return super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#paint(java.awt.Graphics)
     */
    @Override
    public final void paint(final Graphics g) {
        super.paint(g);

        final int elemtWidth = 13;
        final int elemtGap = 0;
        final int padding = 1;

        float xpos = padding;

        final int genericManaCost = this.value.getGenericCost();
        final boolean hasGeneric = (genericManaCost > 0) || this.value.isPureGeneric();
        final List<CardManaCostShard> shards = this.value.getShards();

        final int cellWidth = this.getWidth();
        final int cntGlyphs = hasGeneric ? shards.size() + 1 : shards.size();
        final float offsetIfNoSpace = cntGlyphs > 1 ? (cellWidth - padding - elemtWidth) / (cntGlyphs - 1f)
                : elemtWidth + elemtGap;
        final float offset = Math.min(elemtWidth + elemtGap, offsetIfNoSpace);

        if (hasGeneric) {
            final String sGeneric = Integer.toString(genericManaCost);
            ManaSymbols.drawSymbol(sGeneric, g, (int) xpos, 1);
            xpos += offset;
        }

        for (final CardManaCostShard s : shards) {
            ManaSymbols.drawSymbol(s.getImageKey(), g, (int) xpos, 1);
            xpos += offset;
        }
    }

}
