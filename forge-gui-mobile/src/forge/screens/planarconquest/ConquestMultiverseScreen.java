package forge.screens.planarconquest;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import forge.Forge;
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
import forge.planarconquest.ConquestRecord;
import forge.screens.FScreen;
import forge.toolbox.FScrollPane;
import forge.util.collect.FCollectionView;

public class ConquestMultiverseScreen extends FScreen {
    private static final Color FOG_OF_WAR_COLOR = FSkinColor.alphaColor(Color.BLACK, 0.75f);
    private static final Color UNCONQUERED_COLOR = FSkinColor.alphaColor(Color.BLACK, 0.1f);

    private final PlaneGrid planeGrid;
    private ConquestData model;

    public ConquestMultiverseScreen() {
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
        setHeaderCaption(model.getName() + "\nPlane - " + model.getCurrentPlane().getName());

        planeGrid.revalidate();
        planeGrid.scrollToBottom(); //start at bottom and move up
    }

    private void launchEvent() {
        Forge.openScreen(new ConquestEventScreen(model.getCurrentLocation().getEvent()));
    }

    private class PlaneGrid extends FScrollPane {
        private MoveAnimation activeMoveAnimation;

        @Override
        public boolean tap(float x, float y, int count) {
            if (activeMoveAnimation == null) {
                //start move animation if a path can be found to tapped location
                ConquestLocation loc = getLocation(x, y);
                if (model.getCurrentLocation().equals(loc)) {
                    launchEvent();
                }
                else {
                    List<ConquestLocation> path = model.getPath(loc);
                    if (path != null) {
                        activeMoveAnimation = new MoveAnimation(path);
                        activeMoveAnimation.start();
                    }
                }
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
            float eventIconSize = Math.min(colWidth, rowHeight) / 3;
            float eventIconOffset = Math.round(eventIconSize * 0.1f);

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
                        x0 = x + c * colWidth;
                        y0 = y + (rows - r - 1) * rowHeight;

                        ConquestRecord eventRecord = planeData.getEventRecord(i, r, c);
                        if (eventRecord != null && eventRecord.getWins() > 0) {
                            //draw badge in upper-right corner of conquered squares
                            FSkinImage badge;
                            switch (eventRecord.getTier()) {
                            case 1:
                                badge = FSkinImage.PW_BADGE_COMMON;
                                break;
                            case 2:
                                badge = FSkinImage.PW_BADGE_UNCOMMON;
                                break;
                            case 3:
                                badge = FSkinImage.PW_BADGE_RARE;
                                break;
                            default:
                                badge = FSkinImage.PW_BADGE_MYTHIC;
                                break;
                            }
                            //shift slightly right to account for transparent edge of icon
                            g.drawImage(badge, Math.round(x0 + colWidth - eventIconOffset - eventIconSize * 0.9f), Math.round(y0 + eventIconOffset), eventIconSize, eventIconSize);
                        }
                        else {
                            //draw fog of war by default if area hasn't been conquered
                            color = FOG_OF_WAR_COLOR;

                            //if any bordering grid square has been conquered, instead show unconquered color
                            if (i == 0 && r == 0 && c == Region.START_COL) {
                                color = UNCONQUERED_COLOR; //show unconquered color for starting square of plane
                            }
                            else {
                                for (ConquestLocation loc : ConquestLocation.getNeighbors(plane, i, r, c)) {
                                    if (planeData.hasConquered(loc)) {
                                        color = UNCONQUERED_COLOR;
                                        break;
                                    }
                                }
                            }

                            g.fillRect(color, x0, y0, colWidth, rowHeight);
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
            float height = model.getCurrentPlane().getRegions().size() * regionHeight;
            return new ScrollBounds(visibleWidth, height);
        }

        private ConquestLocation getLocation(float x, float y) {
            y += getScrollTop();

            float w = getWidth();
            float h = getScrollHeight();
            float regionHeight = w / CardRenderer.CARD_ART_RATIO;
            float colWidth = w / Region.COLS_PER_REGION;
            float rowHeight = regionHeight / Region.ROWS_PER_REGION;

            int rowIndex = (int)((h - y) / rowHeight); //flip axis since locations go bottom to top
            ConquestPlane plane = model.getCurrentPlane();
            int regionIndex = rowIndex / Region.ROWS_PER_REGION;
            int row = rowIndex % Region.ROWS_PER_REGION;

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
            float y = h - (loc.getRegionIndex() * regionHeight + loc.getRow() * rowHeight + rowHeight / 2);

            return new Vector2(x, y);
        }

        private class MoveAnimation extends ForgeAnimation {
            private static final float DURATION_PER_SEGMENT = 0.5f;

            private final List<ConquestLocation> path;
            private final float duration;
            private Vector2 pos;
            private float progress;

            private MoveAnimation(List<ConquestLocation> path0) {
                path = path0;
                pos = getPosition(path.get(0));
                duration = (path.size() - 1) * DURATION_PER_SEGMENT;
            }

            @Override
            protected boolean advance(float dt) {
                progress += dt;
                if (progress >= duration) {
                    //we've reached our destination, so stop animation
                    pos = getPosition(path.get(path.size() - 1));
                    return false;
                }

                int currentSegment = (int)(progress / DURATION_PER_SEGMENT);
                float r = (progress - currentSegment * DURATION_PER_SEGMENT) / DURATION_PER_SEGMENT;
                Vector2 p1 = getPosition(path.get(currentSegment));
                Vector2 p2 = getPosition(path.get(currentSegment + 1));
                pos = new Vector2((1.0f - r) * p1.x + r * p2.x, (1.0f - r) * p1.y + r * p2.y);
                return true;
            }

            @Override
            protected void onEnd(boolean endingAll) {
                model.setCurrentLocation(path.get(path.size() - 1));
                model.saveData(); //save new location
                activeMoveAnimation = null;
                launchEvent();
            }
        }
    }
}
