package forge.screens.planarconquest;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.FThreads;
import forge.Forge;
import forge.Graphics;
import forge.achievement.PlaneswalkerAchievements;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.TextRenderer;
import forge.card.CardRarity;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardZoom.ActivateHandler;
import forge.deck.FDeckViewer;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.model.FModel;
import forge.planarconquest.ConquestAction;
import forge.planarconquest.ConquestCommander;
import forge.planarconquest.ConquestData;
import forge.planarconquest.ConquestController.GameRunner;
import forge.planarconquest.ConquestPlane;
import forge.planarconquest.ConquestPlane.Region;
import forge.planarconquest.ConquestPlaneData;
import forge.planarconquest.ConquestPlaneData.RegionData;
import forge.planarconquest.ConquestPlaneMap;
import forge.planarconquest.ConquestPlaneMap.HexagonTile;
import forge.planarconquest.ConquestPlaneMap.IPlaneMapRenderer;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.planarconquest.IVCommandCenter;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.screens.match.TargetingOverlay;
import forge.toolbox.FCardPanel;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FScrollPane;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Utils;

public class CommandCenterScreen extends FScreen implements IVCommandCenter {
    private static final float BORDER_THICKNESS = Utils.scale(1);
    private static final FSkinFont REGION_STATS_FONT = FSkinFont.get(12);
    private static final FSkinFont REGION_NAME_FONT = FSkinFont.get(15);
    private static final FSkinFont PLANE_NAME_FONT = FSkinFont.get(16);
    private static final FSkinFont UNLOCK_WINS_FONT = FSkinFont.get(18);
    private static final float COMMANDER_ROW_PADDING = Utils.scale(16);
    private static final float COMMANDER_GAP = Utils.scale(12);
    private static final float REGION_FRAME_THICKNESS_MULTIPLIER = 15f / 443f;
    private static final float REGION_FRAME_BASE_HEIGHT_MULTIPLIER = 25f / 317f;

    private final PlaneMap map = add(new PlaneMap());
    private final RegionDisplay regionDisplay = add(new RegionDisplay());
    private final CommanderRow commanderRow = add(new CommanderRow());
    private final FLabel btnEndDay = add(new FLabel.ButtonBuilder().font(FSkinFont.get(14)).build());

    private ConquestData model;
    private String unlockRatio, winRatio;

    public CommandCenterScreen() {
        super("", ConquestMenu.getMenu());

        btnEndDay.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FModel.getConquest().endDay(CommandCenterScreen.this);
            }
        });
    }

    @Override
    public final void onActivate() {
        update();
    }

    public void update() {
        model = FModel.getConquest().getModel();
        setHeaderCaption(model.getName());
        map.revalidate();
        updateCurrentDay();
    }

    @Override
    public void updateCurrentDay() {
        ConquestPlane plane = model.getCurrentPlane();
        ConquestPlaneData planeData = model.getCurrentPlaneData();

        btnEndDay.setText("End Day " + model.getDay());

        //update unlock and win ratios for plane
        unlockRatio = planeData.getUnlockedCount() + "/" + plane.getCardPool().size();
        winRatio = planeData.getWins() + "-" + planeData.getLosses();

        commanderRow.selectedIndex = 0; //select first commander at the beginning of each day
        ConquestCommander commander = commanderRow.panels[0].commander;
        if (commander != null && commander.getDeployedRegion() != null) {
            model.setCurrentRegion(commander.getDeployedRegion());
        }
        regionDisplay.onRegionChanged(); //simulate region change when day changes to ensure everything is updated, even if region didn't changed
    }

    public ConquestCommander getSelectedCommander() {
        return model.getCurrentPlaneData().getCommanders().get(commanderRow.selectedIndex);
    }

    @Override
    public boolean setSelectedCommander(ConquestCommander commander) {
        int newIndex = model.getCurrentPlaneData().getCommanders().indexOf(commander);
        boolean changed = commanderRow.selectedIndex != newIndex; 
        commanderRow.selectedIndex = newIndex;
        if (commander.getDeployedRegion() != null) {
            if (model.setCurrentRegion(commander.getDeployedRegion())) {
                regionDisplay.onRegionChanged();
                changed = true; //if region changed, always return true
            }
        }
        return changed;
    }

    @Override
    public void startGame(final GameRunner gameRunner) {
        LoadingOverlay.show("Loading new game...", new Runnable() {
            @Override
            public void run() {
                gameRunner.finishStartingGame();
            }
        });
    }

    @Override
    protected void drawOverlay(Graphics g) {
        ConquestCommander commander = regionDisplay.deployedCommander.commander;
        if (commander == null || commander.getCurrentDayAction() == null) { return; }

        int idx;
        switch (commander.getCurrentDayAction()) {
        case Attack1:
            TargetingOverlay.drawArrow(g, regionDisplay.deployedCommander, regionDisplay.opponents[0], true);
            break;
        case Attack2:
            TargetingOverlay.drawArrow(g, regionDisplay.deployedCommander, regionDisplay.opponents[1], true);
            break;
        case Attack3:
            TargetingOverlay.drawArrow(g, regionDisplay.deployedCommander, regionDisplay.opponents[2], true);
            break;
        case Deploy:
            idx = model.getCurrentPlaneData().getCommanders().indexOf(regionDisplay.deployedCommander.commander);
            if (idx != -1) {
                TargetingOverlay.drawArrow(g, commanderRow.panels[idx], regionDisplay.deployedCommander, false);
            }
            break;
        case Undeploy:
            idx = model.getCurrentPlaneData().getCommanders().indexOf(regionDisplay.deployedCommander.commander);
            if (idx != -1) {
                TargetingOverlay.drawArrow(g, regionDisplay.deployedCommander, commanderRow.panels[idx], false);
            }
            break;
        default:
            break;
        }
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        //btnEndDay.setSize(width / 2, btnEndDay.getAutoSizeBounds().height);
        //btnEndDay.setPosition((width - btnEndDay.getWidth()) / 2, height - btnEndDay.getHeight());

        //float numCommanders = commanderRow.panels.length;
        //float commanderWidth = (width - (numCommanders + 3) * COMMANDER_GAP) / numCommanders;
        //float commanderRowHeight = commanderWidth * FCardPanel.ASPECT_RATIO + 2 * COMMANDER_ROW_PADDING;
        //commanderRow.setBounds(0, btnEndDay.getTop() - PLANE_NAME_FONT.getLineHeight() - 2 * FLabel.DEFAULT_INSETS - commanderRowHeight, width, commanderRowHeight);

        map.setBounds(0, startY, width, height - startY);
    }

    private class PlaneMap extends FScrollPane implements IPlaneMapRenderer {
        private float zoom = 1;
        private FImage walker = (FImage)PlaneswalkerAchievements.getTrophyImage("Ajani Goldmane");
        private Graphics g;
        private ConquestPlaneMap mapData;

        public PlaneMap() {
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            if (model == null) {
                mapData = null;
                return new ScrollBounds(visibleWidth, visibleHeight);
            }
            mapData = model.getCurrentPlaneData().getMap();
            mapData.setTileWidth(Math.round(visibleWidth / 4 * zoom));
            return new ScrollBounds(mapData.getWidth(), mapData.getHeight());
        }

        @Override
        public boolean zoom(float x, float y, float amount) {
            float oldScrollLeft = getScrollLeft();
            float oldScrollTop = getScrollTop();
            float oldScrollWidth = getScrollWidth();
            float oldScrollHeight = getScrollHeight();

            x += oldScrollLeft;
            y += oldScrollTop;

            if (amount > 0) {
                zoom *= 1.1f;
                if (zoom > 4f) {
                    zoom = 4f;
                }
            }
            else {
                zoom /= 1.1f;
                if (zoom < 0.25f) {
                    zoom = 0.25f;
                }
            }
            revalidate(); //apply change in height to all scroll panes

            //adjust scroll positions to keep x, y in the same spot
            float newScrollWidth = getScrollWidth();
            float xAfter = x * newScrollWidth / oldScrollWidth;
            setScrollLeft(oldScrollLeft + xAfter - x);

            float newScrollHeight = getScrollHeight();
            float yAfter = y * newScrollHeight / oldScrollHeight;
            setScrollTop(oldScrollTop + yAfter - y);
            return true;
        }

        @Override
        protected void drawBackground(Graphics g0) {
            if (mapData == null) { return; }
            g = g0;
            mapData.draw(this, Math.round(getScrollLeft()), Math.round(getScrollTop()), Math.round(getWidth()), Math.round(getHeight()));
            g = null;
        }

        @Override
        public void draw(HexagonTile tile, int x, int y, int w, int h) {
            g.drawImage(FSkinImage.HEXAGON_TILE, x, y, w, h);

            if (x < getWidth() / 2 && x + w > getWidth() / 2 && y < getHeight() / 2 && y + h > getHeight() / 2) {
                float pedestalWidth = w * 0.8f;
                float pedestalHeight = pedestalWidth * walker.getHeight() / walker.getWidth();
                g.drawImage(walker, x + (w - pedestalWidth) / 2, y + h / 2 - pedestalHeight * 0.8f, pedestalWidth, pedestalHeight);
            }
        }
    }

    private class RegionDisplay extends FContainer {
        private final TextRenderer textRenderer = new TextRenderer();
        private final CommanderPanel[] opponents = new CommanderPanel[3];
        private final CommanderPanel deployedCommander;
        private final ActionPanel[] actions = new ActionPanel[4];
        private String unlockRatio, winRatio;
        private RegionData data;

        private RegionDisplay() {
            opponents[0] = add(new CommanderPanel(-1)); //use negative indices to represent cards in region
            opponents[1] = add(new CommanderPanel(-2));
            opponents[2] = add(new CommanderPanel(-3));
            deployedCommander = add(new CommanderPanel(-4));
            actions[0] = add(new ActionPanel(ConquestAction.Undeploy));
            actions[1] = add(new ActionPanel(ConquestAction.Defend));
            actions[2] = add(new ActionPanel(ConquestAction.Recruit));
            actions[3] = add(new ActionPanel(ConquestAction.Study));
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
                onRegionChanged();
                return true;
            }
            return false;
        }

        private void onRegionChanged() {
            data = model.getCurrentPlaneData().getRegionData(model.getCurrentRegion());
            for (int i = 0; i < opponents.length; i++) {
                opponents[i].setCommander(data.getOpponent(i));
            }
            deployedCommander.setCommander(data.getDeployedCommander());
            unlockRatio = data.getUnlockedCount() + "/" + data.getRegion().getCardPool().size();
            winRatio = data.getWins() + "-" + data.getLosses();
        }

        @Override
        protected void doLayout(float width, float height) {
            /*float x = COMMANDER_GAP / 2;
            float y = COMMANDER_GAP;
            float w = width - 2 * x;
            float h = height - y;
            float regionFrameThickness = w * REGION_FRAME_THICKNESS_MULTIPLIER;
            float regionFrameBaseHeight = h * REGION_FRAME_BASE_HEIGHT_MULTIPLIER;
            x += regionFrameThickness;
            y += regionFrameThickness;
            w -= 2 * regionFrameThickness;
            h -= regionFrameThickness + regionFrameBaseHeight;

            float padding = COMMANDER_GAP;
            float panelHeight = (h - 5 * padding) / 2;
            float panelWidth = panelHeight / FCardPanel.ASPECT_RATIO;

            opponents[0].setBounds(x + padding, y + padding,  panelWidth, panelHeight);
            opponents[1].setBounds(x + (w - panelWidth) / 2, y + padding,  panelWidth, panelHeight);
            opponents[2].setBounds(x + w - padding - panelWidth, y + padding,  panelWidth, panelHeight);
            deployedCommander.setBounds(x + (w - panelWidth) / 2, y + h - padding - panelHeight,  panelWidth, panelHeight);

            float actionPanelSize = (w - panelWidth - 6 * padding) / 4;
            x += padding;
            y += h - padding - actionPanelSize;
            actions[0].setBounds(x, y, actionPanelSize, actionPanelSize);
            x += actionPanelSize + padding;
            actions[1].setBounds(x, y, actionPanelSize, actionPanelSize);
            x = deployedCommander.getRight() + padding;
            actions[2].setBounds(x, y, actionPanelSize, actionPanelSize);
            x += actionPanelSize + padding;
            actions[3].setBounds(x, y, actionPanelSize, actionPanelSize);*/
        }

        @Override
        public void drawBackground(Graphics g) {
            if (model == null) { return; }
            Region region = model.getCurrentRegion();

            float x = COMMANDER_GAP / 2;
            float y = COMMANDER_GAP;
            float w = getWidth() - 2 * x;
            float h = getHeight() - y;
            float regionFrameThickness = w * REGION_FRAME_THICKNESS_MULTIPLIER;
            float regionFrameBaseHeight = h * REGION_FRAME_BASE_HEIGHT_MULTIPLIER;

            g.startClip(x + regionFrameThickness, y + regionFrameThickness, w - 2 * regionFrameThickness, h - regionFrameThickness - regionFrameBaseHeight);
            g.drawImage((FImage)region.getArt(), x, y, w, h); //TODO: Maximize in frame while retaining proportions
            g.endClip();

            y += h - regionFrameBaseHeight;
            h = regionFrameBaseHeight * 0.9f;
            g.drawText(unlockRatio, REGION_STATS_FONT, Color.BLACK, 2 * x, y, w, h, false, HAlignment.LEFT, true);
            textRenderer.drawText(g, region.getName(), REGION_NAME_FONT, Color.BLACK, x, y, w, h, y, h, false, HAlignment.CENTER, true);
            g.drawText(winRatio, REGION_STATS_FONT, Color.BLACK, x, y, w - x, h, false, HAlignment.RIGHT, true);
        }
    }

    private void activate(CommanderPanel panel) {
        int index = panel.index;
        if (index >= 0) { //commander row panel
            if (panel.card != null) {
                commanderRow.selectedIndex = index;
                ConquestCommander commander = commanderRow.panels[commanderRow.selectedIndex].commander;
                if (commander.getDeployedRegion() != null) {
                    if (model.setCurrentRegion(commander.getDeployedRegion())) {
                        regionDisplay.onRegionChanged();
                    }
                    else if (commander.getCurrentDayAction() == null) {
                        //if already on commander's region, change action to undeploy if no action currently set
                        commander.setCurrentDayAction(ConquestAction.Undeploy);
                    }
                    else if (commander.getCurrentDayAction() == ConquestAction.Undeploy) {
                        commander.setCurrentDayAction(null);
                    }
                }
            }
            else if (model.getCurrentPlaneData().getWins() >= panel.getWinsToUnlock()) {
                FThreads.invokeInBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        FModel.getConquest().unlockCommander();
                    }
                });
            }
        }
        else if (index >= -regionDisplay.opponents.length) { //opponent panel
            ConquestCommander commander = regionDisplay.deployedCommander.commander;
            if (commander != null && commander.getCurrentDayAction() != ConquestAction.Deploy) {
                ConquestAction action;
                switch (index) {
                case -1:
                    action = ConquestAction.Attack1;
                    break;
                case -2:
                    action = ConquestAction.Attack2;
                    break;
                default:
                    action = ConquestAction.Attack3;
                    break;
                }
                if (commander.getCurrentDayAction() == action) {
                    action = null; //toggle off set to attack this opponent
                }
                commander.setCurrentDayAction(action);
            }
        }
        else { //deploy panel - toggle whether selected commander is deployed to current region
            ConquestCommander commander = regionDisplay.deployedCommander.commander;
            if (commander == null) {
                commander = commanderRow.panels[commanderRow.selectedIndex].commander;
                if (commander.getCurrentDayAction() == null && commander.getDeployedRegion() == null) {
                    regionDisplay.deployedCommander.setCommander(commander);
                    regionDisplay.data.setDeployedCommander(commander);
                    commander.setCurrentDayAction(ConquestAction.Deploy);
                }
            }
            else if (commander.getCurrentDayAction() == ConquestAction.Deploy) {
                regionDisplay.deployedCommander.setCommander(null);
                regionDisplay.data.setDeployedCommander(null);
                commander.setCurrentDayAction(null);
            }
            else {
                commander.setCurrentDayAction(null);
            }
        }
    }

    private class CommanderRow extends FContainer {
        private int selectedIndex;
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
    }

    private class CommanderPanel extends FDisplayObject {
        private final int index;
        private CardView card;
        private ConquestCommander commander;

        private CommanderPanel(int index0) {
            index = index0;
        }

        private void setCommander(ConquestCommander commander0) {
            if (commander == commander0) { return; }
            commander = commander0;
            card = commander != null ? Card.getCardForUi(commander.getCard()).getView() : null;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (count == 2 && index >= 0 && commander != null) {
                Forge.openScreen(new ConquestDeckEditor(commander));
                return true;
            }
            activate(this);
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            if (card == null) { return false; }

            if (index >= 0) {
                FDeckViewer.show(commander.getDeck());
            }
            else if (index >= -regionDisplay.opponents.length) {
                List<CardView> cards = new ArrayList<CardView>();
                for (CommanderPanel panel : regionDisplay.opponents) {
                    if (panel.card == null) {
                        break;
                    }
                    cards.add(panel.card);
                }
                CardZoom.show(cards, -index - 1, new ActivateHandler() {
                    @Override
                    public String getActivateAction(int idx) {
                        if (regionDisplay.deployedCommander.card == null) {
                            return null;
                        }
                        return "play against opponent";
                    }

                    @Override
                    public void activate(int idx) {
                        CommandCenterScreen.this.activate(regionDisplay.opponents[idx]);
                    }
                });
            }
            else {
                CardZoom.show(card);
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();

            float borderThickness = BORDER_THICKNESS;
            Color color = Color.WHITE;

            ConquestPlaneData planeData = model.getCurrentPlaneData();
            if (commander == null) {
                if (index >= 0) {
                    List<ConquestCommander> commanders = planeData.getCommanders();
                    if (index < commanders.size()) {
                        setCommander(commanders.get(index));
                    }
                }
            }
            if (card != null) {
                boolean needAlpha;
                if (index >= 0) {
                    //fade out card in commander row if its deployed and not in the process of being undeployed
                    needAlpha = commander.getDeployedRegion() != null && commander.getCurrentDayAction() != ConquestAction.Undeploy;
                }
                else if (index >= -regionDisplay.opponents.length) {
                    //fade out opponent if another opponent is being attacked
                    if (regionDisplay.deployedCommander.commander == null || regionDisplay.deployedCommander.commander.getCurrentDayAction() == null) {
                        needAlpha = false;
                    }
                    else {
                        switch (regionDisplay.deployedCommander.commander.getCurrentDayAction()) {
                        case Attack1:
                            needAlpha = (index != -1);
                            break;
                        case Attack2:
                            needAlpha = (index != -2);
                            break;
                        case Attack3:
                            needAlpha = (index != -3);
                            break;
                        default:
                            needAlpha = false;
                            break;
                        }
                    }
                }
                else {
                    //fade out card in deployed commander spot if its being undeployed
                    needAlpha = commander.getCurrentDayAction() == ConquestAction.Undeploy;
                }
                if (needAlpha) {
                    g.setAlphaComposite(0.7f); //use alpha composite if commander deployed
                }
                CardRenderer.drawCardWithOverlays(g, card, 0, 0, w, h, CardStackPosition.Top);

                if (commander.getCurrentDayAction() != null) {
                    float padding = w * CardRenderer.PADDING_MULTIPLIER; //adjust for card border
                    w -= 2 * padding;
                    h -= 2 * padding;
                    float actionIconSize = w / 2;
                    float x = (padding + (w / 4)) - actionIconSize / 2;
                    float y = (padding + h) - (h / 8) - actionIconSize / 2;
                    g.drawImage(FSkin.getImages().get(commander.getCurrentDayAction().getIcon()), x, y, actionIconSize, actionIconSize);
                }

                if (needAlpha) {
                    g.resetAlphaComposite();
                }

                if (index == commanderRow.selectedIndex) {
                    borderThickness *= 2;
                    color = Color.GREEN;
                }
            }
            else if (index > 0) {
                int winsToUnlock = getWinsToUnlock();
                if (planeData.getWins() < winsToUnlock) {
                    g.setAlphaComposite(0.25f);
                    float imageSize = w * 0.75f;
                    g.drawImage(FSkinImage.LOCK, (w - imageSize) / 2, (h - imageSize) / 2, imageSize, imageSize);
                    g.resetAlphaComposite();
                    float y = (h + imageSize) / 2;
                    g.drawText(String.valueOf(winsToUnlock), UNLOCK_WINS_FONT, Color.WHITE, 0, y, w, h - y, false, HAlignment.CENTER, true);
                }
                else {
                    borderThickness *= 2; //double border thickness and make it gold to indicate it's been unlocked
                    color = CardRenderer.getRarityColor(CardRarity.Rare);
                }
            }

            g.drawRect(borderThickness, color, -borderThickness, -borderThickness, getWidth() + 2 * borderThickness, getHeight() + 2 * borderThickness);
        }

        private int getWinsToUnlock() {
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
            return FModel.getConquestPreferences().getPrefInt(unlockPref);
        }
    }

    private class ActionPanel extends FDisplayObject {
        private final ConquestAction action;

        public ActionPanel(ConquestAction action0) {
            action = action0;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            ConquestCommander commander = regionDisplay.deployedCommander.commander;
            if (commander == null || commander.getCurrentDayAction() == ConquestAction.Deploy) {
                return false; //don't draw if commander just deployed or not deployed in current region
            }

            if (action == commander.getCurrentDayAction()) {
                commander.setCurrentDayAction(null);
            }
            else {
                commander.setCurrentDayAction(action);
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            ConquestCommander commander = regionDisplay.deployedCommander.commander;
            if (commander == null || commander.getCurrentDayAction() == ConquestAction.Deploy) {
                return; //don't draw if commander just deployed or not deployed in current region
            }

            float w = getWidth();
            float h = getHeight();

            boolean needAlpha = commander.getCurrentDayAction() != action && commander.getCurrentDayAction() != null;
            if (needAlpha) {
                g.setAlphaComposite(0.4f); //use alpha composite if another action was selected
            }

            float iconSize = w * 0.8f;
            g.drawImage(FSkin.getImages().get(action.getIcon()), (w - iconSize) / 2, (h - iconSize) / 2, iconSize, iconSize);

            if (needAlpha) {
                g.resetAlphaComposite();
                return; //don't draw border if another action was selected
            }

            float borderThickness = BORDER_THICKNESS;
            Color color = Color.WHITE;
            g.drawRect(borderThickness, color, -borderThickness, -borderThickness, w + 2 * borderThickness, h + 2 * borderThickness);
        }
    }
}
