package forge.screens.planarconquest;

import java.util.List;

import com.badlogic.gdx.graphics.Color;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.card.CardDetailUtil;
import forge.card.CardRenderer;
import forge.card.CardDetailUtil.DetailColors;
import forge.model.FModel;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestLocation;
import forge.planarconquest.ConquestPlane.Region;
import forge.planarconquest.ConquestPlaneData;
import forge.screens.FScreen;
import forge.toolbox.FScrollPane;
import forge.util.collect.FCollectionView;

public class ConquestMapScreen extends FScreen {
    private static final Color FOG_OF_WAR_COLOR = FSkinColor.alphaColor(Color.BLACK, 0.6f);
    private static final Color UNCONQUERED_COLOR = FSkinColor.alphaColor(Color.BLACK, 0.2f);

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
            int cols = Region.COLS_PER_REGION;
            int rows = Region.ROWS_PER_REGION;
            float colWidth = w / cols;
            float rowHeight = regionHeight / rows;
            ConquestPlaneData planeData = model.getCurrentPlaneData();
            ConquestLocation currentLocation = model.getCurrentLocation();

            g.startClip(0, 0, w, h);

            Color color;
            float x = 0;
            float y = -getScrollTop();
            float colLineStartY = 0;
            float colLineEndY = h;
            FCollectionView<Region> regions = model.getCurrentPlane().getRegions();
            int regionCount = regions.size();

            //draw top portal row
            if (y + rowHeight > 0) {
                g.drawImage(FSkinImage.PLANAR_PORTAL, 0, y, w, rowHeight);
                if (planeData.getBossResult() == 0) { //draw overlay if boss hasn't been beaten yet
                    if (planeData.getEventResult(regionCount - 1, rows - 1, Region.PORTAL_COL) > 0) {
                        color = UNCONQUERED_COLOR;
                    }
                    else {
                        color = FOG_OF_WAR_COLOR;
                    }
                    g.fillRect(color, 0, y, w, rowHeight);
                }
                colLineStartY = y + rowHeight;
            }
            y += rowHeight;

            for (int i = regionCount - 1; i >= 0; i--) {
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

                //draw event icon and overlay based on event record for each event in the region
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (planeData.getEventResult(i, r, c) == 0) {
                            //draw fog of war by default if area hasn't been conquered
                            color = FOG_OF_WAR_COLOR;

                            //if any bordering grid square has been conquered, instead show unconquered color
                            for (ConquestLocation loc : currentLocation.getNeighbors(i, r, c)) {
                                if (planeData.getEventResult(loc.getRegionIndex(), loc.getRow(), loc.getCol()) > 0) {
                                    color = UNCONQUERED_COLOR;
                                    break;
                                }
                            }

                            g.fillRect(color, x + c * colWidth, y + (rows - r - 1) * rowHeight, colWidth, rowHeight);
                        }
                    }
                }

                //draw row lines
                float y0 = y;
                for (int r = 0; r < rows; r++) {
                    g.drawLine(1, Color.BLACK, 0, y0, w, y0);
                    y0 += rowHeight;
                }

                y += regionHeight;
            }

            //draw bottom portal row
            if (y <= h) {
                g.drawImage(FSkinImage.PLANAR_PORTAL, 0, y, w, rowHeight);
                g.drawLine(1, Color.BLACK, 0, y, w, y);
                colLineEndY = y;
            }

            //draw column lines
            float x0 = x + colWidth;
            for (int c = 1; c < cols; c++) {
                g.drawLine(1, Color.BLACK, x0, colLineStartY, x0, colLineEndY);
                x0 += colWidth;
            }

            g.endClip();
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float regionHeight = visibleWidth / CardRenderer.CARD_ART_RATIO;
            float rowHeight = regionHeight / Region.ROWS_PER_REGION;
            float height = model.getCurrentPlane().getRegions().size() * regionHeight;
            height += 2 * rowHeight; //account for portal row at top and bottom
            return new ScrollBounds(visibleWidth, height);
        }
    }
}
