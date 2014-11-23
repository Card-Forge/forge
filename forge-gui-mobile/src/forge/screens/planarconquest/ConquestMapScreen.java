package forge.screens.planarconquest;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.card.CardRenderer;
import forge.model.FModel;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane.Region;
import forge.screens.FScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Utils;

public class ConquestMapScreen extends FScreen {
    private static final FSkinColor BTN_PRESSED_COLOR = TEXTURE_OVERLAY_COLOR.alphaColor(1f);
    private static final FSkinColor LINE_COLOR = BTN_PRESSED_COLOR.stepColor(-40);
    private static final FSkinColor BACK_COLOR = BTN_PRESSED_COLOR.stepColor(-80);
    private static final float LINE_THICKNESS = Utils.scale(1);
    private static final float REGION_SLIDER_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.7f);
    private static final FSkinFont FONT = FSkinFont.get(15);

    private final RegionArt regionArt = add(new RegionArt());

    private ConquestData model;

    public ConquestMapScreen() {
        super("", ConquestMenu.getMenu());
    }

    @Override
    public final void onActivate() {
        update();
    }

    public void update() {
        model = FModel.getConquest().getModel();
        setHeaderCaption(model.getName() + " - " + model.getCurrentPlane());
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        regionArt.setBounds(0, startY, width, width / CardRenderer.CARD_ART_RATIO + REGION_SLIDER_HEIGHT);
    }

    private class RegionArt extends FContainer {
        private final FLabel btnPrev, btnNext;
        private final TextRenderer textRenderer = new TextRenderer();

        public RegionArt() {
            btnPrev = add(new FLabel.Builder().icon(new BackIcon(REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT)).pressedColor(BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    model.incrementRegion(-1);
                }
            }).build());
            btnNext = add(new FLabel.Builder().icon(new BackIcon(REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT)).pressedColor(BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    model.incrementRegion(1);
                }
            }).build());
            btnPrev.setSize(REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT);
            btnNext.setSize(REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT);
        }

        @Override
        public boolean fling(float velocityX, float velocityY) {
            //switch to next/previous region when flung left or right
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                if (velocityX < 0) {
                    model.incrementRegion(1);
                }
                else {
                    model.incrementRegion(-1);
                }
                return true;
            }
            return false;
        }

        @Override
        public void drawBackground(Graphics g) {
            float w = getWidth();
            float h = getHeight();
            float y = h - REGION_SLIDER_HEIGHT;
            g.fillRect(BACK_COLOR, 0, y, w, REGION_SLIDER_HEIGHT);
            if (model != null) {
                Region region = model.getCurrentRegion();
                g.drawImage((FImage)region.getArt(), 0, 0, w, y);
                textRenderer.drawText(g, region.getName(), FONT, FLabel.DEFAULT_TEXT_COLOR, REGION_SLIDER_HEIGHT, y, w - 2 * REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT, y, REGION_SLIDER_HEIGHT, false, HAlignment.CENTER, true);
            }
        }

        @Override
        protected void doLayout(float width, float height) {
            float y = height - REGION_SLIDER_HEIGHT;
            float size = REGION_SLIDER_HEIGHT;
            btnPrev.setBounds(0, y, size, size);
            btnNext.setBounds(width - REGION_SLIDER_HEIGHT, y, size, size);
        }
    }
}
