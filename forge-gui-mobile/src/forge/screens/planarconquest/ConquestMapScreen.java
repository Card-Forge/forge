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
        setHeaderCaption(model.getCurrentPlane().getName() + " - Map");

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
        }
    }
}
