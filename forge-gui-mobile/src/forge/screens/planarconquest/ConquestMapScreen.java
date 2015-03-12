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
import forge.util.FCollectionView;

public class ConquestMapScreen extends FScreen {
    private final FList<Region> lstRegions;
    private ConquestData model;

    public ConquestMapScreen() {
        super("", ConquestMenu.getMenu());

        lstRegions = add(new FList<Region>() {
            @Override
            protected void drawBackground(Graphics g) {
                //draw no background
            }
            @Override
            public void drawOverlay(Graphics g) {
                //draw no overlay
            }
            @Override
            protected FSkinColor getItemFillColor(int index) {
                return null;
            }
            @Override
            protected boolean drawLineSeparators() {
                return false;
            }
            @Override
            protected float getPadding() {
                return 0;
            }
        });
        lstRegions.setListItemRenderer(new PlaneRenderer());
    }
    
    @Override
    public void onActivate() {
        update();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        lstRegions.setBounds(0, startY, width, height - startY);
    }

    public void update() {
        model = FModel.getConquest().getModel();
        setHeaderCaption(model.getCurrentPlane().getName());

        FCollectionView<Region> regions = model.getCurrentPlane().getRegions();
        lstRegions.clear();
        for (int i = regions.size() - 1; i >= 0; i--) {
            lstRegions.addItem(regions.get(i));
        }
        lstRegions.revalidate();
        lstRegions.scrollToBottom(); //start at bottom and move up
    }

    private class PlaneRenderer extends ListItemRenderer<ConquestPlane.Region> {
        @Override
        public float getItemHeight() {
            return ConquestMapScreen.this.getWidth() / CardRenderer.CARD_ART_RATIO;
        }

        @Override
        public boolean tap(Integer index, Region region, float x, float y, int count) {
            return false;
        }

        @Override
        public void drawValue(Graphics g, Integer index, Region region,
                FSkinFont font, FSkinColor foreColor, FSkinColor backColor,
                boolean pressed, float x, float y, float w, float h) {

            //draw background art
            FImage art = (FImage)region.getArt();
            if (art != null) {
                g.drawImage(art, x, y, w, h);
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
                    g.fillRect(color1, x, y, w, h);
                }
                else {
                    g.fillGradientRect(color1, color2, false, x, y, w, h);
                }
            }

            //draw path with opponents
            float gridSize = h / rows;
            float iconSize = gridSize * 0.5f;
            float iconOffset = (gridSize - iconSize) / 2;
            float dx = (w - h * cols / rows) / 2; //center grid
            FCollectionView<ConquestOpponent> opponents = region.getOpponents();
            for (int i = 0; i < opponents.size(); i++) {
                GridPosition pos = path[i];
                float x0 = x + gridSize * pos.col + dx + iconOffset;
                float y0 = y + gridSize * pos.row + iconOffset;
                g.drawImage((FImage)opponents.get(i).getMapIcon(), x0, y0, iconSize, iconSize);
            }
        }
    }

    //path through region should look like this:
    //       15
    // 12 13 14
    // 11
    // 10 09 08 07 06
    //             05
    //       02 03 04
    //       01
    private static final int rows = 7;
    private static final int cols = 5;
    private static final GridPosition[] path = {
        new GridPosition(6, 2), //01 is at row 7 col 3
        new GridPosition(5, 2),
        new GridPosition(5, 3),
        new GridPosition(5, 4),
        new GridPosition(4, 4),
        new GridPosition(3, 4),
        new GridPosition(3, 3),
        new GridPosition(3, 2),
        new GridPosition(3, 1),
        new GridPosition(3, 0),
        new GridPosition(2, 0),
        new GridPosition(1, 0),
        new GridPosition(1, 1),
        new GridPosition(1, 2),
        new GridPosition(0, 2)
    };

    private static class GridPosition {
        public final int row, col;
        public GridPosition(int row0, int col0) {
            row = row0;
            col = col0;
        }
    }
}
