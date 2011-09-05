package forge.gui.deckeditor;

import java.awt.Component;
import java.awt.Graphics;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import arcane.ui.util.ManaSymbols;

import forge.card.CardManaCostShard;
import forge.card.CardManaCost;

/** 
 * Displays mana cost as symbols.
 */
public class ManaCostRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1770527102334163549L;

    private CardManaCost value;

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value,
            final boolean isSelected, final boolean hasFocus, final int row, final int column)
    {
        this.value = (CardManaCost) value;
        setToolTipText(this.value.toString());
        return super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
    }

    @Override
    public void paint(final Graphics g) {
        super.paint(g);

        final int elemtWidth = 13;
        final int elemtGap = 0;
        final int padding = 1;

        float xpos = padding;

        int genericManaCost = value.getGenericCost();
        boolean hasGeneric = genericManaCost > 0 || value.isPureGeneric();
        List<CardManaCostShard> shards = value.getShards();

        int cellWidth = getWidth();
        int cntGlyphs = hasGeneric ? shards.size() + 1 : shards.size();
        float offsetIfNoSpace = cntGlyphs > 1 ? (cellWidth - padding - elemtWidth) / (cntGlyphs - 1f) : elemtWidth + elemtGap;
        float offset = Math.min(elemtWidth + elemtGap, offsetIfNoSpace);

        if (hasGeneric) {
            String sGeneric = Integer.toString(genericManaCost);
            ManaSymbols.drawSymbol(sGeneric, g, (int) xpos, 1);
            xpos += offset;
        }

        for (CardManaCostShard s : shards) {
            ManaSymbols.drawSymbol(s.imageKey, g, (int) xpos, 1);
            xpos += offset;
        }
    }

}
