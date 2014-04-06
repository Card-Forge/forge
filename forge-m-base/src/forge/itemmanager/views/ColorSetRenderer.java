package forge.itemmanager.views;

import forge.assets.CardFaceSymbols;
import forge.card.ColorSet;
import forge.card.mana.ManaCostShard;

import com.badlogic.gdx.math.Vector2;

public class ColorSetRenderer extends ItemCellRenderer {
    private static final int elemtWidth = 13;
    private static final int elemtGap = 0;
    private static final int padding0 = 2;

    @Override
    public void draw(forge.Forge.Graphics g, Object value, Vector2 loc, float itemWidth, float itemHeight) {
        final float cellWidth = itemWidth;

        float x = loc.x + padding0;
        float y = loc.y + padding0 + 1;

        ColorSet cs;
        if (value instanceof ColorSet) {
            cs = (ColorSet) value;
        }
        else {
            cs = ColorSet.getNullColor();
        }
        final int cntGlyphs = cs.countColors();
        final float offsetIfNoSpace = cntGlyphs > 1 ? (cellWidth - padding0 - elemtWidth) / (cntGlyphs - 1) : elemtWidth + elemtGap;
        final float dx = Math.min(elemtWidth + elemtGap, offsetIfNoSpace);

        // Display colorless mana before colored mana
        if (cntGlyphs == 0) {
            CardFaceSymbols.drawSymbol(ManaCostShard.X.getImageKey(), g, x, y, elemtWidth);
            x += dx;
        }

        if (cs.hasWhite()) { CardFaceSymbols.drawSymbol(ManaCostShard.WHITE.getImageKey(), g, x, y, elemtWidth); x += dx; }
        if (cs.hasBlue()) { CardFaceSymbols.drawSymbol(ManaCostShard.BLUE.getImageKey(), g, x, y, elemtWidth); x += dx; }
        if (cs.hasBlack()) { CardFaceSymbols.drawSymbol(ManaCostShard.BLACK.getImageKey(), g, x, y, elemtWidth); x += dx; }
        if (cs.hasRed()) { CardFaceSymbols.drawSymbol(ManaCostShard.RED.getImageKey(), g, x, y, elemtWidth); x += dx; }
        if (cs.hasGreen()) { CardFaceSymbols.drawSymbol(ManaCostShard.GREEN.getImageKey(), g, x, y, elemtWidth); x += dx; }
    }
}