package forge.itemmanager.views;

import forge.assets.CardFaceSymbols;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.ColorSet;
import forge.card.mana.ManaCostShard;
import forge.itemmanager.filters.ItemFilter;

import com.badlogic.gdx.math.Vector2;

public class ColorSetRenderer extends ItemCellRenderer {
    private static final int ICON_SIZE = 13;
    private static final int PADDING = 2;

    @Override
    public void draw(forge.Forge.Graphics g, Object value, FSkinFont font, FSkinColor foreColor, Vector2 loc, float itemWidth, float itemHeight) {
        final float cellWidth = itemWidth;

        float x = loc.x + PADDING;
        float y = loc.y + PADDING + 1;

        ColorSet cs;
        if (value instanceof ColorSet) {
            cs = (ColorSet) value;
        }
        else {
            cs = ColorSet.getNullColor();
        }
        final int cntGlyphs = cs.countColors();
        final float offsetIfNoSpace = cntGlyphs > 1 ? (cellWidth - PADDING - ICON_SIZE) / (cntGlyphs - 1) : ICON_SIZE;
        final float dx = Math.min(ICON_SIZE, offsetIfNoSpace);

        // Display colorless mana before colored mana
        if (cntGlyphs == 0) {
            CardFaceSymbols.drawSymbol(ManaCostShard.X.getImageKey(), g, x, y, ICON_SIZE);
            x += dx;
        }

        if (cs.hasWhite()) { CardFaceSymbols.drawSymbol(ManaCostShard.WHITE.getImageKey(), g, x, y, ICON_SIZE); x += dx; }
        if (cs.hasBlue()) { CardFaceSymbols.drawSymbol(ManaCostShard.BLUE.getImageKey(), g, x, y, ICON_SIZE); x += dx; }
        if (cs.hasBlack()) { CardFaceSymbols.drawSymbol(ManaCostShard.BLACK.getImageKey(), g, x, y, ICON_SIZE); x += dx; }
        if (cs.hasRed()) { CardFaceSymbols.drawSymbol(ManaCostShard.RED.getImageKey(), g, x, y, ICON_SIZE); x += dx; }
        if (cs.hasGreen()) { CardFaceSymbols.drawSymbol(ManaCostShard.GREEN.getImageKey(), g, x, y, ICON_SIZE); x += dx; }

        loc.x = ItemFilter.PADDING;
        loc.y += itemHeight / 2;
    }
}