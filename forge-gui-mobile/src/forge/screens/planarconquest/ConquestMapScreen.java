package forge.screens.planarconquest;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.assets.TextRenderer;
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
import forge.toolbox.FLabel;
import forge.util.Utils;

public class ConquestMapScreen extends FScreen {
    private static final Color BACK_COLOR = FSkinColor.fromRGB(1, 2, 2);
    private static final float BORDER_THICKNESS = Utils.scale(1);
    private static final FSkinFont REGION_NAME_FONT = FSkinFont.get(15);
    private static final FSkinFont UNLOCK_WINS_FONT = FSkinFont.get(18);
    private static final float COMMANDER_ROW_PADDING = Utils.scale(16);
    private static final float COMMANDER_GAP = Utils.scale(12);

    private final RegionDisplay regionDisplay = add(new RegionDisplay());
    private final CommanderRow commanderRow = add(new CommanderRow());
    private final FLabel lblCurrentPlane = add(new FLabel.Builder().font(FSkinFont.get(16)).align(HAlignment.CENTER).build());
    private final FLabel btnEndDay = add(new FLabel.ButtonBuilder().font(FSkinFont.get(14)).build());

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
        setHeaderCaption(model.getName());
        lblCurrentPlane.setText("Plane - " + model.getCurrentPlane().getName());
        btnEndDay.setText("End Day " + model.getDay());
    }

    @Override
    public void drawBackground(Graphics g) {
        FImage background = FSkinTexture.BG_PLANAR_MAP;
        float w = getWidth();
        float h = w * background.getHeight() / background.getWidth();
        g.fillRect(BACK_COLOR, 0, 0, w, getHeight());
        g.drawImage(background, 0, getHeight() - h, w, h); //retain proportions, remaining area will be covered by back color
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        btnEndDay.setSize(width / 2, btnEndDay.getAutoSizeBounds().height);
        btnEndDay.setPosition((width - btnEndDay.getWidth()) / 2, height - btnEndDay.getHeight());

        lblCurrentPlane.setSize(btnEndDay.getWidth(), lblCurrentPlane.getAutoSizeBounds().height);
        lblCurrentPlane.setPosition(btnEndDay.getLeft(), btnEndDay.getTop() - lblCurrentPlane.getHeight());

        float numCommanders = commanderRow.panels.length;
        float commanderWidth = (width - (numCommanders + 3) * COMMANDER_GAP) / numCommanders;
        float commanderRowHeight = commanderWidth * FCardPanel.ASPECT_RATIO + 2 * COMMANDER_ROW_PADDING;
        commanderRow.setBounds(0, lblCurrentPlane.getTop() - commanderRowHeight, width, commanderRowHeight);

        regionDisplay.setBounds(0, startY, width, commanderRow.getTop() - startY);
    }

    private class RegionDisplay extends FDisplayObject {
        private final TextRenderer textRenderer = new TextRenderer();

        public RegionDisplay() {
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
        public void draw(Graphics g) {
            if (model == null) { return; }
            Region region = model.getCurrentRegion();

            float x = COMMANDER_GAP / 2;
            float y = COMMANDER_GAP;
            float w = getWidth() - 2 * x;
            float h = getHeight() - y;
            float regionFrameThickness = 15f / 443f * w;
            float regionFrameBaseHeight = 25f / 317f * h;

            g.startClip(x + regionFrameThickness, y + regionFrameThickness, w - 2 * regionFrameThickness, h - regionFrameThickness - regionFrameBaseHeight);
            g.drawImage((FImage)region.getArt(), x, y, w, h); //TODO: Maximize in frame while retaining proportions
            g.endClip();

            g.drawImage(FSkinTexture.BG_MONITOR, x, y, w, h);

            y += h - regionFrameBaseHeight;
            h = regionFrameBaseHeight * 0.9f;
            textRenderer.drawText(g, region.getName(), REGION_NAME_FONT, Color.BLACK, x, y, w, h, y, h, false, HAlignment.CENTER, true);
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
            float panelHeight = height - 2 * COMMANDER_ROW_PADDING;
            float panelWidth = panelHeight / FCardPanel.ASPECT_RATIO;
            float dx = panelWidth + COMMANDER_GAP;
            float x = 2 * COMMANDER_GAP;
            float y = COMMANDER_ROW_PADDING;

            for (int i = 0; i < panels.length; i++) {
                panels[i].setBounds(x, y, panelWidth, panelHeight);
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
                g.drawRect(BORDER_THICKNESS, Color.WHITE, -BORDER_THICKNESS, -BORDER_THICKNESS, w + 2 * BORDER_THICKNESS, h + 2 * BORDER_THICKNESS);

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
                    CQPref unlockPref;
                    switch (index) {
                    case 1:
                        unlockPref = CQPref.WINS_TO_UNLOCK_COMMANDER_2;
                        break;
                    case 2:
                        unlockPref = CQPref.WINS_TO_UNLOCK_COMMANDER_3;
                        break;
                    default:
                        unlockPref = CQPref.WINS_TO_UNLOCK_COMMANDER_4;
                        break;
                    }
                    int winsToUnlock = FModel.getConquestPreferences().getPrefInt(unlockPref);
                    if (planeData.getWins() < winsToUnlock) {
                        g.setAlphaComposite(0.25f);
                        float imageSize = w * 0.75f;
                        g.drawImage(FSkinImage.LOCK, (w - imageSize) / 2, (h - imageSize) / 2, imageSize, imageSize);
                        g.resetAlphaComposite();
                        float y = (h + imageSize) / 2;
                        g.drawText(String.valueOf(winsToUnlock), UNLOCK_WINS_FONT, Color.WHITE, 0, y, w, h - y, false, HAlignment.CENTER, true);
                    }
                    else {
                        
                    }
                }
            }
        }
    }
}
