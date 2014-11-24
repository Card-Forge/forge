package forge.screens.planarconquest;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.TextRenderer;
import forge.assets.FSkinColor.Colors;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.model.FModel;
import forge.planarconquest.ConquestCommander;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestPlane.Region;
import forge.planarconquest.ConquestPlaneData;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.screens.FScreen;
import forge.toolbox.FCardPanel;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FLabel;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Utils;

public class ConquestMapScreen extends FScreen {
    private static final float PADDING = Utils.scale(5);
    private static final FSkinColor BTN_PRESSED_COLOR = FSkinColor.get(Colors.CLR_THEME2);
    private static final FSkinColor LINE_COLOR = BTN_PRESSED_COLOR.stepColor(-40);
    private static final FSkinColor BACK_COLOR = BTN_PRESSED_COLOR.stepColor(-80);
    private static final FSkinColor FORE_COLOR = FSkinColor.get(Colors.CLR_TEXT);
    private static final FSkinColor BORDER_COLOR = FSkinColor.get(Colors.CLR_BORDERS);
    private static final float LINE_THICKNESS = Utils.scale(1);
    private static final float ARROW_ICON_THICKNESS = Utils.scale(3);
    private static final float REGION_SLIDER_HEIGHT = Math.round(Utils.AVG_FINGER_HEIGHT * 0.7f);
    private static final FSkinFont REGION_NAME_FONT = FSkinFont.get(15);
    private static final FSkinFont UNLOCK_WINS_FONT = FSkinFont.get(18);
    private static final float COMMANDER_ROW_HEIGHT = 2 * Utils.AVG_FINGER_HEIGHT;

    private final RegionDisplay regionDisplay = add(new RegionDisplay());
    private final CommanderRow commanderRow = add(new CommanderRow());

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
        float y = startY;
        regionDisplay.setBounds(0, y, width, width / CardRenderer.CARD_ART_RATIO + REGION_SLIDER_HEIGHT);
        y += regionDisplay.getHeight() + PADDING;
        commanderRow.setBounds(0, y, width, COMMANDER_ROW_HEIGHT);
    }

    private class RegionDisplay extends FContainer {
        private final FLabel btnPrev, btnNext;
        private final TextRenderer textRenderer = new TextRenderer();

        public RegionDisplay() {
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
                textRenderer.drawText(g, region.getName(), REGION_NAME_FONT, FORE_COLOR, REGION_SLIDER_HEIGHT, y, w - 2 * REGION_SLIDER_HEIGHT, REGION_SLIDER_HEIGHT, y, REGION_SLIDER_HEIGHT, false, HAlignment.CENTER, true);
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

    private class CommanderRow extends FContainer {
        private CommanderPanel[] panels = new CommanderPanel[4];

        private CommanderRow() {
            for (int i = 0; i < panels.length; i++) {
                panels[i] = add(new CommanderPanel(i));
            }
        }

        @Override
        protected void doLayout(float width, float height) {
            float panelHeight = height;
            float panelWidth = panelHeight / FCardPanel.ASPECT_RATIO;
            float extraSpace = width - panelWidth * panels.length;
            float gap = extraSpace / (panels.length + 3);
            float dx = panelWidth + gap;
            float x = 2 * gap;

            for (int i = 0; i < panels.length; i++) {
                panels[i].setBounds(x, 0, panelWidth, panelHeight);
                x += dx;
            }
        }

        private class CommanderPanel extends FDisplayObject {
            private final int index;
            private CardView card;

            private CommanderPanel(int index0) {
                index = index0;
            }

            @Override
            public void draw(Graphics g) {
                float w = getWidth();
                float h = getHeight();
                g.drawRect(LINE_THICKNESS, BORDER_COLOR, -LINE_THICKNESS, -LINE_THICKNESS, w + 2 * LINE_THICKNESS, h + 2 * LINE_THICKNESS);

                ConquestPlaneData planeData = model.getCurrentPlaneData();
                if (card == null) {
                    List<ConquestCommander> commanders = planeData.getCommanders();
                    if (index < commanders.size()) {
                        card = Card.getCardForUi(commanders.get(index).getCard()).getView();
                    }
                }
                if (card != null) {
                    CardRenderer.drawCardWithOverlays(g, card, 0, 0, w, h, CardStackPosition.Top);
                }
                else {
                    int winsToUnlock = index * FModel.getConquestPreferences().getPrefInt(CQPref.WINS_TO_UNLOCK_COMMANDER);
                    if (planeData.getWins() < winsToUnlock) {
                        g.setAlphaComposite(0.25f);
                        float imageSize = w * 0.75f;
                        g.drawImage(FSkinImage.LOCK, (w - imageSize) / 2, (h - imageSize) / 2, imageSize, imageSize);
                        g.resetAlphaComposite();
                        float y = (h + imageSize) / 2;
                        g.drawText(String.valueOf(winsToUnlock), UNLOCK_WINS_FONT, FORE_COLOR, 0, y, w, h - y, false, HAlignment.CENTER, true);
                    }
                    else {
                        
                    }
                }
            }
        }
    }
}
