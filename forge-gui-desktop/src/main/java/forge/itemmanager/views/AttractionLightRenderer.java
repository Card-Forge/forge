package forge.itemmanager.views;

import forge.toolbox.CardFaceSymbols;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class AttractionLightRenderer extends ItemCellRenderer {
    private static final int elementWidth = 13;
    private static final int elementGap = 2;
    private static final int padding = 2;

    //Can't check for type params via instanceof, but it doesn't really matter since all we're using is .contains()
    private Set<?> lights;

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

        if (value instanceof Set) {
            this.lights = (Set<?>) value;
            this.setToolTipText(StringUtils.join(this.lights, ", "));
        }
        else {
            this.lights = null;
            this.setToolTipText(null);
        }

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

        if(this.lights == null)
            return;

        final int cellWidth = this.getWidth();

        int x = padding;
        int y = padding + 1;

        final int cntGlyphs = 6;
        final int offsetIfNoSpace = (cellWidth - padding - elementWidth) / (cntGlyphs - 1);
        final int dx = Math.min(elementWidth + elementGap, offsetIfNoSpace);

        CardFaceSymbols.drawManaSymbol(lights.contains(1) ? "AL1ON" : "AL1OFF", g, x, y);
        CardFaceSymbols.drawManaSymbol(lights.contains(2) ? "AL2ON" : "AL2OFF", g, x + (dx), y);
        CardFaceSymbols.drawManaSymbol(lights.contains(3) ? "AL3ON" : "AL3OFF", g, x + (dx * 2), y);
        CardFaceSymbols.drawManaSymbol(lights.contains(4) ? "AL4ON" : "AL4OFF", g, x + (dx * 3), y);
        CardFaceSymbols.drawManaSymbol(lights.contains(5) ? "AL5ON" : "AL5OFF", g, x + (dx * 4), y);
        CardFaceSymbols.drawManaSymbol(lights.contains(6) ? "AL6ON" : "AL6OFF", g, x + (dx * 5), y);
    }
}
