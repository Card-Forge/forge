package forge.itemmanager.views;

import forge.card.ColorSet;
import forge.card.mana.ManaCostShard;
import forge.toolbox.CardFaceSymbols;

import javax.swing.*;

import java.awt.*;

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
            this.cs = ColorSet.getNullColor();
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

        // Display colorless mana before colored mana
        if (cntGlyphs == 0) {
            CardFaceSymbols.drawSymbol(ManaCostShard.X.getImageKey(), g, x, y);
            x += dx;
        }

        if (cs.hasWhite()) { CardFaceSymbols.drawSymbol(ManaCostShard.WHITE.getImageKey(), g, x, y); x += dx; }
        if (cs.hasBlue()) { CardFaceSymbols.drawSymbol(ManaCostShard.BLUE.getImageKey(), g, x, y); x += dx; }
        if (cs.hasBlack()) { CardFaceSymbols.drawSymbol(ManaCostShard.BLACK.getImageKey(), g, x, y); x += dx; }
        if (cs.hasRed()) { CardFaceSymbols.drawSymbol(ManaCostShard.RED.getImageKey(), g, x, y); x += dx; }
        if (cs.hasGreen()) { CardFaceSymbols.drawSymbol(ManaCostShard.GREEN.getImageKey(), g, x, y); x += dx; }
    }
}