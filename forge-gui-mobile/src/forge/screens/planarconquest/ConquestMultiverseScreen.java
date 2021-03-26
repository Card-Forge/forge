package forge.screens.planarconquest;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.card.CardAvatarImage;
import forge.card.CardFaceSymbols;
import forge.card.CardImageRenderer;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.card.ColorSet;
import forge.gamemodes.planarconquest.ConquestAwardPool;
import forge.gamemodes.planarconquest.ConquestBattle;
import forge.gamemodes.planarconquest.ConquestChaosBattle;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestEvent;
import forge.gamemodes.planarconquest.ConquestEvent.ChaosWheelOutcome;
import forge.gamemodes.planarconquest.ConquestEvent.ConquestEventRecord;
import forge.gamemodes.planarconquest.ConquestLocation;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.ConquestPlaneData;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gamemodes.planarconquest.ConquestRegion;
import forge.gamemodes.planarconquest.ConquestReward;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.gui.FThreads;
import forge.gui.card.CardDetailUtil;
import forge.gui.card.CardDetailUtil.DetailColors;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FButton;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.Callback;
import forge.util.Localizer;
import forge.util.Utils;
import forge.util.collect.FCollectionView;

public class ConquestMultiverseScreen extends FScreen {
    private static final Color FOG_OF_WAR_COLOR = FSkinColor.alphaColor(Color.BLACK, 0.65f);
    private static final Color UNCONQUERED_COLOR = FSkinColor.alphaColor(Color.BLACK, 0.1f);
    public static final Color LOCATION_BAR_COLOR = FSkinColor.alphaColor(Color.WHITE, 0.15f);
    public static final Color LOCATION_BAR_TEXT_COLOR = FSkinColor.fromRGB(200, 207, 208);
    private static final FSkinFont PLANE_NAME_FONT = FSkinFont.get(16);
    private static final FSkinFont EVENT_NAME_FONT = FSkinFont.get(14);
    private static final FSkinFont AVATAR_NAME_FONT = FSkinFont.get(14);
    private static final float BATTLE_BAR_HEIGHT = Utils.AVG_FINGER_HEIGHT * 2;
    private static final float PADDING = FList.PADDING;
    private static final float LOCATION_BAR_HEIGHT = CardRenderer.getCardListItemHeight(false);

    private final PlaneGrid planeGrid;
    private final LocationBar locationBar;
    private final BattleBar battleBar;
    private ConquestData model;
    private ConquestBattle activeBattle;

    public ConquestMultiverseScreen() {
        super("", ConquestMenu.getMenu());

        planeGrid = add(new PlaneGrid());
        locationBar = add(new LocationBar());
        battleBar = add(new BattleBar());
    }

    @Override
    public void onActivate() {
        if (activeBattle == null) {
            update();
        }
        else if (activeBattle.isFinished()) {
            //when returning to this screen from launched battle, award prizes if it was conquered
            if (activeBattle.wasConquered()) {
                if (activeBattle instanceof ConquestChaosBattle) {
                    ConquestChaosBattle chaosBattle = (ConquestChaosBattle)activeBattle;
                    awardBoosters(chaosBattle.getAwardPool(), 3);
                }
                else {
                    ConquestLocation loc = activeBattle.getLocation();
                    ConquestEventRecord record = model.getCurrentPlaneData().getEventRecord(loc);
                    if (record.getWins(activeBattle.getTier()) == 1 && record.getHighestConqueredTier() == activeBattle.getTier()) {
                        //if first time conquering event at the selected tier, show animation of new badge being positioned on location
                        model.rewardPlaneswalkEmblems(FModel.getConquestPreferences().getPrefInt(CQPref.PLANESWALK_CONQUER_EMBLEMS));
                        model.saveData();
                        planeGrid.animateBadgeIntoPosition(loc, activeBattle.getTier());
                    }
                    else {
                        //just spin Chaos Wheel immediately if event tier was previously conquered
                        spinChaosWheel();
                    }
                }
            }
            activeBattle = null;
        }
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        battleBar.setBounds(0, height - BATTLE_BAR_HEIGHT, width, BATTLE_BAR_HEIGHT);
        locationBar.setBounds(0, height - BATTLE_BAR_HEIGHT - LOCATION_BAR_HEIGHT, width, LOCATION_BAR_HEIGHT);
        planeGrid.setBounds(0, startY, width, height - BATTLE_BAR_HEIGHT - LOCATION_BAR_HEIGHT - startY);
        planeGrid.scrollPlaneswalkerIntoView();
    }

    public void update() {
        model = FModel.getConquest().getModel();
        setHeaderCaption(model.getName());

        planeGrid.revalidate();
        planeGrid.scrollPlaneswalkerIntoView();

        battleBar.update();
    }

    private void spinChaosWheel() {
        ConquestChaosWheel.spin(new Callback<ChaosWheelOutcome>() {
            @Override
            public void run(ChaosWheelOutcome outcome) {
                switch (outcome) {
                case BOOSTER:
                    awardBoosters(model.getCurrentPlane().getAwardPool(), 1);
                    break;
                case DOUBLE_BOOSTER:
                    awardBoosters(model.getCurrentPlane().getAwardPool(), 2);
                    break;
                case SHARDS:
                    awardShards(FModel.getConquestPreferences().getPrefInt(CQPref.AETHER_WHEEL_SHARDS), false);
                    break;
                case DOUBLE_SHARDS:
                    awardShards(2 * FModel.getConquestPreferences().getPrefInt(CQPref.AETHER_WHEEL_SHARDS), false);
                    break;
                case PLANESWALK:
                    if (model.getUnlockedPlaneCount() == model.getAccessiblePlaneCount()) {
                        FOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblAllPlanesUnlockedNotify"), Localizer.getInstance().getMessage("lblAllPlanesUnlocked"), EMBLEM_IMAGE, new Callback<Integer>() {
                            @Override
                            public void run(Integer result) {
                                launchChaosBattle();
                            }
                        });
                    } else {
                        awardBonusPlaneswalkEmblems(FModel.getConquestPreferences().getPrefInt(CQPref.PLANESWALK_WHEEL_EMBLEMS));
                    }
                    break;
                case CHAOS:
                    launchChaosBattle();
                    break;
                }
            }
        });
    }

    private void awardBoosters(ConquestAwardPool pool, int totalCount) {
        AwardBoosterHelper helper = new AwardBoosterHelper(pool, totalCount);
        helper.run();
    }

    private class AwardBoosterHelper implements Runnable {
        private final ConquestAwardPool pool;
        private final int totalCount;
        private final int shardsBefore;
        private int number;

        private AwardBoosterHelper(ConquestAwardPool pool0, int totalCount0) {
            pool = pool0;
            number = 1;
            totalCount = totalCount0;
            shardsBefore = model.getAEtherShards();
        }

        @Override
        public void run() {
            if (number > totalCount) {
                //show total shards received from all boosters once all boosters shown
                final int shardsReceived = model.getAEtherShards() - shardsBefore;
                if (shardsReceived > 0) {
                    awardShards(shardsReceived, true);
                }
                model.saveData(); //save data once all cards and shard awarded
                return;
            }

            String title = Localizer.getInstance().getMessage("lblReceivedBoosterPack");
            if (totalCount > 1) {
                title = Localizer.getInstance().getMessage("lblReceivedBoosterPackNOfTotal", String.valueOf(number), String.valueOf(totalCount));
            }
            number++;
            List<ConquestReward> rewards = FModel.getConquest().awardBooster(pool);
            ConquestRewardDialog.show(title, rewards, this);
        }
    }

    private static final FImage SHARD_IMAGE = new FImage() {
        final float size = Forge.getScreenWidth() * 0.6f;
        @Override
        public float getWidth() {
            return size;
        }
        @Override
        public float getHeight() {
            return size;
        }
        @Override
        public void draw(Graphics g, float x, float y, float w, float h) {
            FSkinImage.AETHER_SHARD.draw(g, x, y, w, h);
        }
    };

    private void awardShards(int shards, boolean fromDuplicateCards) {
        String message = Localizer.getInstance().getMessage("lblReceivedAetherShards");
        if (fromDuplicateCards) { //if from duplicate cards, shards already added to model
            message = Localizer.getInstance().getMessage("lblReceivedAetherShardsForDuplicateCards");
        }
        else {
            model.rewardAEtherShards(shards);
            model.saveData();
        }
        FOptionPane.showMessageDialog(String.valueOf(shards), FSkinFont.get(32), message, SHARD_IMAGE);
    }

    private static final FImage EMBLEM_IMAGE = new FImage() {
        final float size = Forge.getScreenWidth() * 0.6f;
        @Override
        public float getWidth() {
            return size;
        }
        @Override
        public float getHeight() {
            return size;
        }
        @Override
        public void draw(Graphics g, float x, float y, float w, float h) {
            FSkinImage.PW_BADGE_COMMON.draw(g, x, y, w, h);
        }
    };

    private void awardBonusPlaneswalkEmblems(int emblems) {
        String message = Localizer.getInstance().getMessage("lblReceivedBonusPlaneswalkEmblems");
        model.rewardPlaneswalkEmblems(emblems);
        model.saveData();
        FOptionPane.showMessageDialog(String.valueOf(emblems), FSkinFont.get(32), message, EMBLEM_IMAGE);
    }

    private void launchEvent() {
        LoadingOverlay.show(Localizer.getInstance().getMessage("lblStartingBattle"), new Runnable() {
            @Override
            public void run() {
                ConquestLocation loc = model.getCurrentLocation();
                activeBattle = loc.getEvent().createBattle(loc, 0);
                FModel.getConquest().startBattle(activeBattle);
            }
        });
    }

    private void launchChaosBattle() {
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override
            public void run() {
                LoadingOverlay.show(Localizer.getInstance().getMessage("lblChaosApproaching"), new Runnable() {
                    @Override
                    public void run() {
                        activeBattle = new ConquestChaosBattle();
                        FModel.getConquest().startBattle(activeBattle);
                    }
                });
            }
        });
    }

    @Override
    public void buildTouchListeners(float screenX, float screenY, List<FDisplayObject> listeners) {
        //prevent user touch actions while an animation is in progress
        if (planeGrid.activeMoveAnimation == null && planeGrid.activeBadgeAnimation == null) {
            super.buildTouchListeners(screenX, screenY, listeners);
        }
    }

    private class PlaneGrid extends FScrollPane {
        private MoveAnimation activeMoveAnimation;
        private BadgeAnimation activeBadgeAnimation;

        @Override
        public boolean tap(float x, float y, int count) {
            //start move animation if a path can be found to tapped location
            ConquestLocation loc = getLocation(x, y);
            if (!model.getCurrentLocation().equals(loc)) {
                List<ConquestLocation> path = model.getPath(loc);
                if (path != null) {
                    activeMoveAnimation = new MoveAnimation(path);
                    activeMoveAnimation.start();
                }
            }
            return true;
        }

        private void animateBadgeIntoPosition(ConquestLocation loc, int tier) {
            activeBadgeAnimation = new BadgeAnimation(loc, tier);
            activeBadgeAnimation.start();
        }

        @Override
        public void draw(Graphics g) {
            ConquestPlane plane = model.getCurrentPlane();

            float w = getWidth();
            float h = getHeight();
            float regionHeight = w / CardRenderer.CARD_ART_RATIO;
            int cols = plane.getCols();
            int rows = plane.getRowsPerRegion();
            float colWidth = w / cols;
            float rowHeight = regionHeight / rows;
            float eventIconSize = Math.min(colWidth, rowHeight) / 3;
            float eventIconOffset = Math.round(eventIconSize * 0.1f);

            FCollectionView<ConquestRegion> regions = plane.getRegions();
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
                ConquestRegion region = regions.get(i);
                FImage art = (FImage)region.getArt();
                if (art != null && art != CardImageRenderer.forgeArt) {
                    g.drawImage(art, x, y, w, regionHeight);
                }
                else { //draw fallback background color if needed
                    List<DetailColors> backColors = CardDetailUtil.getBorderColors(region.getColorSet());
                    CardImageRenderer.fillColorBackground(g, backColors, x, y, w, regionHeight);
                }

                //draw event icon and overlay based on event record for each event in the region
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        x0 = x + c * colWidth;
                        y0 = y + (rows - r - 1) * rowHeight;

                        ConquestEventRecord eventRecord = planeData.getEventRecord(i, r, c);
                        if (eventRecord != null && eventRecord.hasConquered()) {
                            //draw badge in upper-right corner of conquered squares
                            //shift slightly right to account for transparent edge of icon
                            x0 = Math.round(x0 + colWidth - eventIconOffset - eventIconSize * 0.9f);
                            y0 = Math.round(y0 + eventIconOffset);

                            int tier = eventRecord.getHighestConqueredTier();
                            if (activeBadgeAnimation != null && activeBadgeAnimation.location.isAt(i, r, c)) {
                                activeBadgeAnimation.end.set(x0, y0, eventIconSize, eventIconSize);
                                tier--; //show previous tier while animation active
                            }
                            switch (tier) {
                            case 0:
                                FSkinImage.PW_BADGE_COMMON.draw(g, x0, y0, eventIconSize, eventIconSize);
                                break;
                            case 1:
                                FSkinImage.PW_BADGE_UNCOMMON.draw(g, x0, y0, eventIconSize, eventIconSize);
                                break;
                            case 2:
                                FSkinImage.PW_BADGE_RARE.draw(g, x0, y0, eventIconSize, eventIconSize);
                                break;
                            case 3:
                                FSkinImage.PW_BADGE_MYTHIC.draw(g, x0, y0, eventIconSize, eventIconSize);
                                break;
                            }
                        }
                        else {
                            //draw fog of war by default if area hasn't been conquered
                            color = FOG_OF_WAR_COLOR;

                            //if any bordering grid square has been conquered, instead show unconquered color
                            if (i == 0 && r == 0 && c == 0) {
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

            //draw badge animation on top of everything else
            if (activeBadgeAnimation != null) {
                activeBadgeAnimation.drawBadge(g);
            }

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

            ConquestPlane plane = model.getCurrentPlane();
            int rowsPerRegion = plane.getRowsPerRegion();
            int cols = plane.getCols();

            float w = getWidth();
            float h = getScrollHeight();
            float regionHeight = w / CardRenderer.CARD_ART_RATIO;
            float colWidth = w / cols;
            float rowHeight = regionHeight / rowsPerRegion;

            int rowIndex = (int)((h - y) / rowHeight); //flip axis since locations go bottom to top
            int regionIndex = rowIndex / rowsPerRegion;
            int row = rowIndex % rowsPerRegion;

            int col = (int)(x / colWidth);
            if (col < 0) {
                col = 0;
            }
            else if (col > cols - 1) {
                col = cols - 1;
            }

            return new ConquestLocation(plane, regionIndex, row, col);
        }

        private Vector2 getPosition(ConquestLocation loc) {
            float w = getWidth();
            float h = getScrollHeight();
            float regionHeight = w / CardRenderer.CARD_ART_RATIO;
            float colWidth = w / loc.getPlane().getCols();
            float rowHeight = regionHeight / loc.getPlane().getRowsPerRegion();

            float x = loc.getCol() * colWidth + colWidth / 2;
            float y = h - (loc.getRegionIndex() * regionHeight + loc.getRow() * rowHeight + rowHeight / 2);

            return new Vector2(x, y);
        }

        private void scrollPlaneswalkerIntoView() {
            float w = getWidth();
            float regionHeight = w / CardRenderer.CARD_ART_RATIO;
            float rowHeight = regionHeight / model.getCurrentPlane().getRowsPerRegion();
            float margin = rowHeight * 1.5f;  //use rowHeight * 1.5f margin so an extra row is kept visible above and below your current location
            float maxMargin = getHeight() / 2; //prevent flicker if rows too tall to fit 3 on screen at once
            if (margin > maxMargin) {
                margin = maxMargin;
            }

            Vector2 pos = activeMoveAnimation == null ? getPosition(model.getCurrentLocation()) : activeMoveAnimation.pos;
            scrollIntoView(pos.x, pos.y - getScrollTop(), 0, 0, margin);
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
                    scrollPlaneswalkerIntoView();
                    return false;
                }

                int currentSegment = (int)(progress / DURATION_PER_SEGMENT);
                float r = (progress - currentSegment * DURATION_PER_SEGMENT) / DURATION_PER_SEGMENT;
                Vector2 p1 = getPosition(path.get(currentSegment));
                Vector2 p2 = getPosition(path.get(currentSegment + 1));
                pos = new Vector2((1.0f - r) * p1.x + r * p2.x, (1.0f - r) * p1.y + r * p2.y);
                scrollPlaneswalkerIntoView();
                return true;
            }

            @Override
            protected void onEnd(boolean endingAll) {
                String secretArea = model.getCurrentLocation().getEvent().getTemporaryUnlock();
                if (secretArea != null) {
                    ConquestUtil.setPlaneTemporarilyAccessible(secretArea, false);
                }
                model.setCurrentLocation(path.get(path.size() - 1));
                model.saveData(); //save new location
                activeMoveAnimation = null;

                battleBar.update();
            }
        }

        private class BadgeAnimation extends ForgeAnimation {
            private static final float DURATION = 0.75f;

            private final ConquestLocation location;
            private final FSkinImage badge;
            private final Rectangle start, end;
            private float progress = -0.5f; //delay animation by a half second

            private BadgeAnimation(ConquestLocation location0, int tier) {
                location = location0;

                switch (tier) {
                case 0:
                    badge = FSkinImage.PW_BADGE_COMMON;
                    break;
                case 1:
                    badge = FSkinImage.PW_BADGE_UNCOMMON;
                    break;
                case 2:
                    badge = FSkinImage.PW_BADGE_RARE;
                    break;
                default:
                    badge = FSkinImage.PW_BADGE_MYTHIC;
                    break;
                }

                float gridWidth = PlaneGrid.this.getWidth();
                float startSize = gridWidth * 0.75f;
                start = new Rectangle((gridWidth - startSize) / 2, (PlaneGrid.this.getHeight() - startSize) / 2, startSize, startSize);
                end = new Rectangle();
            }

            private void drawBadge(Graphics g) {
                float percentage = progress / DURATION;
                if (percentage < 0) {
                    percentage = 0;
                }
                else if (percentage > 1) {
                    percentage = 1;
                }
                Rectangle pos = Utils.getTransitionPosition(start, end, percentage);
                g.drawImage(badge, pos.x, pos.y, pos.width, pos.height);
            }

            @Override
            protected boolean advance(float dt) {
                progress += dt;
                return progress < DURATION + 0.5f; //delay ending animation for a half second
            }

            @Override
            protected void onEnd(boolean endingAll) {
                activeBadgeAnimation = null;
                if (!endingAll) {
                    spinChaosWheel(); //spin Chaos Wheel after badge positioned
                }
            }
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        g.startClip(0, locationBar.getTop(), w, h - locationBar.getTop());

        FImage background = FSkinTexture.BG_SPACE;
        float backgroundHeight = w * background.getHeight() / background.getWidth();
        g.drawImage(background, 0, h - backgroundHeight, w, backgroundHeight);

        g.endClip();
    }
 
    private class LocationBar extends FDisplayObject {
        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();

            //draw background
            g.fillRect(LOCATION_BAR_COLOR, 0, 0, w, h);

            //draw plane name, region name, and region colors on first line
            ConquestLocation loc = model.getCurrentLocation();
            ConquestRegion region = loc.getRegion();
            ColorSet regionColors = region.getColorSet();
            float x = PADDING;
            float y = PADDING;
            float symbolSize = CardRenderer.MANA_SYMBOL_SIZE;
            float availableNameWidth = w - CardFaceSymbols.getWidth(regionColors, symbolSize) - 3 * PADDING;
            g.drawText(region.toString(), PLANE_NAME_FONT, LOCATION_BAR_TEXT_COLOR, x, y, availableNameWidth, symbolSize, false, Align.left, true);
            x += availableNameWidth + PADDING;
            CardFaceSymbols.drawColorSet(g, regionColors, x, y, symbolSize);

            //draw event details on second line
            ConquestEvent event = loc.getEvent();
            String details = event.getName();
            if (!event.getVariants().isEmpty()) {
                details += " (" + StringUtils.join(event.getVariants(), ", ") + ")";
            }
            x = PADDING;
            y += symbolSize;
            g.drawText(details, EVENT_NAME_FONT, LOCATION_BAR_TEXT_COLOR, x, y, w - 2 * PADDING, h - y, false, Align.center, true);

            //draw top and bottom borders
            g.drawLine(1, Color.BLACK, 0, 0, w, 0);
        }
    }

    private class BattleBar extends FContainer {
        private final AvatarDisplay playerAvatar, opponentAvatar;
        private final FButton btnBattle;
        private ConquestEvent event;

        private BattleBar() {
            playerAvatar = add(new AvatarDisplay(false));
            opponentAvatar = add(new AvatarDisplay(true));
            btnBattle = add(new FButton(Localizer.getInstance().getMessage("lblBattle"), new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    launchEvent();
                }
            }));
            btnBattle.setFont(FSkinFont.get(20));
        }

        private void update() {
            event = model.getCurrentLocation().getEvent();
            playerAvatar.setCard(model.getSelectedCommander().getCard());
            opponentAvatar.setCard(event.getAvatarCard());
        }

        @Override
        protected void doLayout(float width, float height) {
            float labelHeight = AVATAR_NAME_FONT.getLineHeight() * 1.1f;
            float avatarSize = height - labelHeight - 2 * PADDING;

            playerAvatar.setBounds(PADDING, height - avatarSize - PADDING, avatarSize, avatarSize);
            opponentAvatar.setBounds(width - avatarSize - PADDING, PADDING, avatarSize, avatarSize);

            float buttonWidth = width - 2 * avatarSize - 4 * PADDING;
            float buttonHeight = height - 2 * labelHeight - 4 * PADDING;
            btnBattle.setBounds((width - buttonWidth) / 2, (height - buttonHeight) / 2, buttonWidth, buttonHeight);
        }

        @Override
        protected void drawOverlay(Graphics g) {
            float labelWidth = opponentAvatar.getLeft() - 2 * PADDING;
            float labelHeight = playerAvatar.getTop();

            if (playerAvatar.card != null) {
                g.drawText(playerAvatar.card.getName(), AVATAR_NAME_FONT, Color.WHITE, PADDING, 0, labelWidth, labelHeight, false, Align.left, true);
            }
            if (opponentAvatar.card != null) {
                g.drawText(opponentAvatar.card.getName(), AVATAR_NAME_FONT, Color.WHITE, getWidth() - labelWidth - PADDING, getHeight() - labelHeight, labelWidth, labelHeight, false, Align.right, true);
            }
        }

        private class AvatarDisplay extends FDisplayObject {
            private final boolean forOpponent;
            private PaperCard card;
            private FImage image;

            private AvatarDisplay(boolean forOpponent0) {
                forOpponent = forOpponent0;
                image = FSkinImage.UNKNOWN;
            }

            public void setCard(PaperCard card0) {
                if (card == card0) { return; }
                card = card0;
                image = card != null ? new CardAvatarImage(card) : FSkinImage.UNKNOWN;
            }

            @Override
            public void draw(Graphics g) {
                if (image != null) {
                    image.draw(g, 0, 0, getWidth(), getHeight());
                }
            }

            @Override
            public boolean tap(float x, float y, int count) {
                if (card != null) {
                    if (forOpponent) {
                        CardZoom.show(card);
                    }
                    else {
                        ConquestMenu.selectCommander();
                    }
                }
                return true;
            }

            @Override
            public boolean longPress(float x, float y) {
                if (card != null) {
                    CardZoom.show(card);
                }
                return true;
            }
        }
    }
}
