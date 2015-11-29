package forge.screens.planarconquest;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinImage;
import forge.card.CardDetailUtil;
import forge.card.CardRenderer;
import forge.card.CardDetailUtil.DetailColors;
import forge.model.FModel;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestLocation;
import forge.planarconquest.ConquestPlane;
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
        private MoveAnimation activeMoveAnimation;

        @Override
        public boolean tap(float x, float y, int count) {
            ConquestLocation loc = getLocation(x, y);
            if (loc.isTraversable()) {
                model.setCurrentLocation(loc);
            }
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

            ConquestPlane plane = model.getCurrentPlane();
            FCollectionView<Region> regions = plane.getRegions();
            int regionCount = regions.size();
            ConquestPlaneData planeData = model.getCurrentPlaneData();
            ConquestLocation currentLocation = model.getCurrentLocation();

            g.startClip(0, 0, w, h);

            Color color;
            float x0, y0;
            float x = 0;
            float y = -getScrollTop();
            float colLineStartY = 0;
            float colLineEndY = h;

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
                            for (ConquestLocation loc : ConquestLocation.getNeighbors(plane, i, r, c)) {
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
                y0 = y;
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
            x0 = x + colWidth;
            for (int c = 1; c < cols; c++) {
                g.drawLine(1, Color.BLACK, x0, colLineStartY, x0, colLineEndY);
                x0 += colWidth;
            }

            //draw planeswalker token
            FImage token = (FImage)model.getPlaneswalkerToken();
            float tokenHeight = rowHeight * 0.85f;
            float tokenWidth = tokenHeight * token.getWidth() / token.getHeight();
            Vector2 pos = activeMoveAnimation == null ? getPosition(currentLocation) : activeMoveAnimation.pos;
            x0 = pos.x - tokenWidth / 2;
            y0 = pos.y - tokenHeight / 2 - getScrollTop();
            g.drawImage(token, x0, y0, tokenWidth, tokenHeight);

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

        private ConquestLocation getLocation(float x, float y) {
            y += getScrollTop();

            float w = getWidth();
            float h = getScrollHeight();
            float regionHeight = w / CardRenderer.CARD_ART_RATIO;
            float colWidth = w / Region.COLS_PER_REGION;
            float rowHeight = regionHeight / Region.ROWS_PER_REGION;

            int row;
            int rowIndex = (int)((h - y) / rowHeight) - 1; //flip axis since locations go bottom to top
            ConquestPlane plane = model.getCurrentPlane();
            int regionCount = plane.getRegions().size();
            int regionIndex = rowIndex / Region.ROWS_PER_REGION;
            if (rowIndex < 0) {
                regionIndex = -1;
                row = 0;
            }
            else if (regionIndex >= regionCount) {
                regionIndex = regionCount;
                row = 0;
            }
            else {
                row = rowIndex % Region.ROWS_PER_REGION;
            }

            int col = (int)(x / colWidth);
            if (col < 0) {
                col = 0;
            }
            else if (col > Region.COLS_PER_REGION - 1) {
                col = Region.COLS_PER_REGION - 1;
            }

            return new ConquestLocation(plane, regionIndex, row, col);
        }

        private Vector2 getPosition(ConquestLocation loc) {
            float w = getWidth();
            float h = getScrollHeight();
            float regionHeight = w / CardRenderer.CARD_ART_RATIO;
            float colWidth = w / Region.COLS_PER_REGION;
            float rowHeight = regionHeight / Region.ROWS_PER_REGION;

            float x = loc.getCol() * colWidth + colWidth / 2;
            float y;
            if (loc.getRegionIndex() == -1) {
                y = h - rowHeight / 2;
            }
            else {
                y = h - (loc.getRegionIndex() * regionHeight + loc.getRow() * rowHeight + 3 * rowHeight / 2);
            }

            return new Vector2(x, y);
        }

        private class MoveAnimation extends ForgeAnimation {
            private final List<ConquestLocation> path;
            private final Vector2 pos;
            private int pathIndex;

            private MoveAnimation(List<ConquestLocation> path0) {
                path = path0;
                pos = getPosition(path.get(0));
            }

            @Override
            protected boolean advance(float dt) {
                return false;
            }

            @Override
            protected void onEnd(boolean endingAll) {
                activeMoveAnimation = null;
            }
        }
    }
}
