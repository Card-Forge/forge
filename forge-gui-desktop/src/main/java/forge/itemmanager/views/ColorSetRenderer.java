package forge.itemmanager.views;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JTable;

import forge.card.ColorSet;
import forge.card.mana.ManaCostShard;
import forge.toolbox.CardFaceSymbols;

public class ColorSetRenderer extends ItemCellRenderer {
    private static final long serialVersionUID = 1770527102334163549L;

    private static final int elemtWidth = 13;
    private static final int elemtGap = 0;
    private static final int padding0 = 2;

    private ColorSet cs;

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

        if (value instanceof ColorSet) {
            this.cs = (ColorSet) value;
        }
        else {
            this.cs = ColorSet.NO_COLORS;
        }
        this.setToolTipText(cs.toString());
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

        final int cellWidth = this.getWidth();

        int x = padding0;
        int y = padding0 + 1;

        final int cntGlyphs = cs.countColors();
        final int offsetIfNoSpace = cntGlyphs > 1 ? (cellWidth - padding0 - elemtWidth) / (cntGlyphs - 1) : elemtWidth + elemtGap;
        final int dx = Math.min(elemtWidth + elemtGap, offsetIfNoSpace);

        for (final ManaCostShard s : cs.getOrderedShards()) {
            CardFaceSymbols.drawManaSymbol(s.getImageKey(), g, x, y);
            x += dx;
        }
    }
}