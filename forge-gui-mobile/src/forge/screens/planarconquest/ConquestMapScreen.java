package forge.screens.planarconquest;

import java.util.List;

import com.badlogic.gdx.graphics.Color;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.card.CardDetailUtil;
import forge.card.CardRenderer;
import forge.card.CardDetailUtil.DetailColors;
import forge.model.FModel;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestOpponent;
import forge.planarconquest.ConquestPlane;
import forge.planarconquest.ConquestPlane.Region;
import forge.screens.FScreen;
import forge.toolbox.FList;
import forge.toolbox.FList.ListItemRenderer;
import forge.toolbox.FScrollPane;
import forge.util.collect.FCollectionView;

public class ConquestMapScreen extends FScreen {
    private static final int GRID_ROWS = 3;
    private static final int GRID_COLS = 3;
    private static final Color FOG_OF_WAR_COLOR = FSkinColor.alphaColor(Color.BLACK, 0.6f);

    private final PlaneGrid planeGrid;
    private ConquestData model;

    public ConquestMapScreen() {
        super("", ConquestMenu.getMenu());

        planeGrid = add(new PlaneGrid());
    }
    
    @Override
    public void onActivate() {
        update();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        planeGrid.setBounds(0, startY, width, height - startY);
    }

    public void update() {
        model = FModel.getConquest().getModel();
        setHeaderCaption(model.getCurrentPlane().getName());

        planeGrid.revalidate();
        planeGrid.scrollToBottom(); //start at bottom and move up
    }

    private class PlaneGrid extends FScrollPane {

        @Override
        public boolean tap(float x, float y, int count) {
            /*float colWidth = getWidth() / cols;
            float rowHeight = getItemHeight() / rows;
            int row = rows - (int)(y / rowHeight) - 1;
            int col = (int)(x / colWidth);
            int regionIndex = model.getCurrentPlane().getRegions().size() - index - 1;
            if ((regionIndex + row) % 2 == 1) {
                col = cols - col - 1;
            }
            int startIndex = regionIndex * rows * cols;
            int position = startIndex + row * cols + col;
            if (position > model.getProgress()) {
                return false;
            }
            model.setPlaneswalkerPosition(position);*/
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();
            float regionHeight = w / CardRenderer.CARD_ART_RATIO;
            float colWidth = w / GRID_COLS;
            float rowHeight = regionHeight / GRID_ROWS;

            g.startClip(0, 0, w, h);

            float x = 0;
            float y = -getScrollTop();

            //draw top portal row
            if (y + rowHeight > 0) {
                g.fillRect(Color.MAGENTA, 0, y, w, rowHeight);
            }
            y += rowHeight;

            FCollectionView<Region> regions = model.getCurrentPlane().getRegions();
            for (int i = regions.size() - 1; i >= 0; i--) {
                if (y + regionHeight <= 0) {
                    y += regionHeight;
                    continue;
                }
                if (y > h) { break; }

                //draw background art
                Region region = regions.get(i);
                FImage art = (FImage)region.getArt();
                if (art != null) {
                    g.drawImage(art, x, y, w, regionHeight);
                }
                else { //draw fallback background color if needed
                    List<DetailColors> colors = CardDetailUtil.getBorderColors(region.getColorSet());
                    DetailColors dc = colors.get(0);
                    Color color1 = FSkinColor.fromRGB(dc.r, dc.g, dc.b);
                    Color color2 = null;
                    if (colors.size() > 1) {
                        dc = colors.get(1);
                        color2 = FSkinColor.fromRGB(dc.r, dc.g, dc.b);
                    }
                    if (color2 == null) {
                        g.fillRect(color1, x, y, w, regionHeight);
                    }
                    else {
                        g.fillGradientRect(color1, color2, false, x, y, w, regionHeight);
                    }
                }
                
                //draw row lines
                float y0 = y;
                for (int r = 0; r < GRID_ROWS; r++) {
                    g.drawLine(1, Color.BLACK, 0, y0, w, y0);
                    y0 += rowHeight;
                }

                y += regionHeight;
            }

            //draw bottom portal row
            if (y <= h) {
                g.fillRect(Color.MAGENTA, 0, y, w, rowHeight);
                g.drawLine(1, Color.BLACK, 0, y, w, y);
            }

            //draw column lines
            float x0 = x + colWidth;
            for (int c = 1; c < GRID_COLS; c++) {
                g.drawLine(1, Color.BLACK, x0, 0, x0, h);
                x0 += colWidth;
            }

            g.endClip();
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float regionHeight = visibleWidth / CardRenderer.CARD_ART_RATIO;
            float rowHeight = regionHeight / GRID_ROWS;
            float height = model.getCurrentPlane().getRegions().size() * regionHeight;
            height += 2 * rowHeight; //account for portal row at top and bottom
            return new ScrollBounds(visibleWidth, height);
        }
    }
}
