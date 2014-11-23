package forge.screens.planarconquest;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.assets.FSkinColor.Colors;
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
    private static final FSkinColor BTN_PRESSED_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    private static final FSkinColor LINE_COLOR = BTN_PRESSED_COLOR.stepColor(-40);
    private static final FSkinColor BACK_COLOR = BTN_PRESSED_COLOR.stepColor(-80);
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final float LINE_THICKNESS = Utils.scale(1);
    private static final float ARROW_ICON_THICKNESS = Utils.scale(3);
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
            btnPrev = add(new FLabel.Builder().icon(new ArrowIcon(REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT, false)).pressedColor(BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    model.incrementRegion(-1);
                }
            }).build());
            btnNext = add(new FLabel.Builder().icon(new ArrowIcon(REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT, true)).pressedColor(BTN_PRESSED_COLOR).align(HAlignment.CENTER).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    model.incrementRegion(1);
                }
            }).build());
            btnPrev.setSize(REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT);
            btnNext.setSize(REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT);
        }

        private class ArrowIcon implements FImage {
            private final float width, height;
            private final boolean flip;

            public ArrowIcon(float width0, float height0, boolean flip0) {
                width = width0;
                height = height0;
                flip = flip0;
            }

            @Override
            public float getWidth() {
                return width;
            }

            @Override
            public float getHeight() {
                return height;
            }

            @Override
            public void draw(Graphics g, float x, float y, float w, float h) {
                float xMid = x + w * 0.4f; 
                float yMid = y + h / 2;
                float offsetX = h / 8;
                float offsetY = w / 4;

                if (flip) {
                    xMid = x + w * 0.6f; 
                    offsetX = -offsetX;
                }

                g.drawLine(ARROW_ICON_THICKNESS, FORE_COLOR, xMid + offsetX, yMid - offsetY, xMid - offsetX, yMid + 1);
                g.drawLine(ARROW_ICON_THICKNESS, FORE_COLOR, xMid - offsetX, yMid - 1, xMid + offsetX, yMid + offsetY);
            }
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
                textRenderer.drawText(g, region.getName(), FONT, FORE_COLOR, REGION_SLIDER_HEIGHT, y, w - 2 * REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT, y, REGION_SLIDER_HEIGHT, false, HAlignment.CENTER, true);
            }
        }

        @Override
        public void drawOverlay(Graphics g) {
            float w = getWidth();
            float y2 = getHeight();
            float y1 = y2 - REGION_SLIDER_HEIGHT;
            g.drawLine(LINE_THICKNESS, LINE_COLOR, 0, y1, w, y1);
            g.drawLine(LINE_THICKNESS, LINE_COLOR, 0, y2, w, y2);
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
