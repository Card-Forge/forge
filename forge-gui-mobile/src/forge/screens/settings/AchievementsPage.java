package forge.screens.settings;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Graphics;
import forge.achievement.Achievement;
import forge.achievement.AchievementCollection;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.screens.TabPageScreen.TabPage;
import forge.toolbox.FComboBox;
import forge.toolbox.FEvent;
import forge.toolbox.FScrollPane;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Utils;

public class AchievementsPage extends TabPage<SettingsScreen> {
    private static final float PADDING = Utils.scaleMin(5);
    private static final int MIN_SHELVES = 4;
    private static final int TROPHIES_PER_SHELVE = 4;

    private final FComboBox<AchievementCollection> cbCollections = add(new FComboBox<AchievementCollection>());
    private final TrophyCase trophyCase = add(new TrophyCase());

    protected AchievementsPage() {
        super("Achievements", FSkinImage.GOLD_TROPHY);

        AchievementCollection.buildComboBox(cbCollections);

        cbCollections.setSelectedIndex(0);
        cbCollections.setAlignment(HAlignment.CENTER);
        cbCollections.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                setAchievements(cbCollections.getSelectedItem());
            }
        });
        setAchievements(cbCollections.getSelectedItem());
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = PADDING;
        float y = PADDING;
        width -= 2 * x;

        cbCollections.setBounds(x, y, width, cbCollections.getHeight());
        y += cbCollections.getHeight() + PADDING;
        trophyCase.setBounds(x, y, width, height - PADDING - y);
    }

    private void setAchievements(AchievementCollection achievements0) {
        trophyCase.achievements = achievements0;
        trophyCase.shelfCount = Math.max(achievements0.getCount() % TROPHIES_PER_SHELVE, MIN_SHELVES);
        trophyCase.revalidate();
    }

    private static class TrophyCase extends FScrollPane {
        private static final Color FORE_COLOR = new Color(239f / 255f, 220f / 255f, 144f / 255f, 1f);

        private AchievementCollection achievements;
        private int shelfCount;
        private float extraWidth = 0;

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float scrollWidth = visibleWidth + extraWidth;
            float scale = scrollWidth / FSkinTexture.BG_TROPHY_CASE_TOP.getWidth();
            float scrollHeight = (FSkinTexture.BG_TROPHY_CASE_TOP.getHeight() +
                    shelfCount * FSkinTexture.BG_TROPHY_CASE_SHELF.getHeight()) * scale;
            return new ScrollBounds(scrollWidth, scrollHeight);
        }

        @Override
        public boolean zoom(float x, float y, float amount) {
            float oldScrollLeft = getScrollLeft();
            float oldScrollTop = getScrollTop();
            float oldScrollWidth = getScrollWidth();
            float oldScrollHeight = getScrollHeight();

            x += oldScrollLeft;
            y += oldScrollTop;

            float zoom = oldScrollWidth / getWidth();
            extraWidth += amount * zoom; //scale amount by current zoom
            if (extraWidth < 0) {
                extraWidth = 0;
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
        protected void drawBackground(Graphics g) {
            float x = -getScrollLeft();
            float y = -getScrollTop();
            float w = getScrollWidth();
            float scale = w / FSkinTexture.BG_TROPHY_CASE_TOP.getWidth();
            float trophyScale = scale / 3f * 1.8f;
            float plateScale = scale / 3f;

            float topHeight = FSkinTexture.BG_TROPHY_CASE_TOP.getHeight() * scale;
            float shelfHeight = FSkinTexture.BG_TROPHY_CASE_SHELF.getHeight() * scale;
            float trophyWidth = FSkinImage.GOLD_TROPHY.getWidth() * trophyScale;
            float trophyHeight = FSkinImage.GOLD_TROPHY.getHeight() * trophyScale;
            float plateWidth = FSkinImage.TROPHY_PLATE.getWidth() * plateScale;
            float plateHeight = FSkinImage.TROPHY_PLATE.getHeight() * plateScale;

            float titleHeight = plateHeight * 0.55f;
            float subTitleHeight = plateHeight * 0.35f;
            FSkinFont titleFont = FSkinFont.forHeight(titleHeight);
            FSkinFont subTitleFont = FSkinFont.forHeight(subTitleHeight);

            float plateY = y + topHeight + shelfHeight - plateHeight;
            float trophyStartY = y + topHeight + (shelfHeight - trophyHeight - 12 * scale) / 2;
            float plateOffset = (trophyWidth - plateWidth) / 2;

            if (y + topHeight > 0) {
                g.drawImage(FSkinTexture.BG_TROPHY_CASE_TOP, x, y, w, topHeight);
            }
            y += topHeight;

            while (true) {
                if (y + shelfHeight > 0) {
                    g.drawImage(FSkinTexture.BG_TROPHY_CASE_SHELF, x, y, w, shelfHeight);
                }
                y += shelfHeight;
                if (y >= getHeight()) {
                    break;
                }
            }

            x += (w - TROPHIES_PER_SHELVE * trophyWidth) / 2;
            y = trophyStartY;

            int trophyCount = 0;
            float startX = x;

            for (Achievement achievement : achievements) {
                if (trophyCount == TROPHIES_PER_SHELVE) {
                    trophyCount = 0;
                    y += shelfHeight;
                    plateY += shelfHeight;
                    x = startX;

                    if (y >= getHeight()) {
                        return;
                    }
                }

                if (plateY + plateHeight > 0) {
                    if (achievement.earnedGold()) {
                        g.drawImage(FSkinImage.GOLD_TROPHY, x, y, trophyWidth, trophyHeight);
                    }
                    else if (achievement.earnedSilver()) {
                        g.drawImage(FSkinImage.SILVER_TROPHY, x, y, trophyWidth, trophyHeight);
                    }
                    else if (achievement.earnedBronze()) {
                        g.drawImage(FSkinImage.BRONZE_TROPHY, x, y, trophyWidth, trophyHeight);
                    }
                    g.drawImage(FSkinImage.TROPHY_PLATE, x + plateOffset, plateY, plateWidth, plateHeight);
    
                    g.drawText(achievement.getDisplayName(), titleFont, FORE_COLOR, x + plateOffset + plateWidth * 0.075f, plateY + plateHeight * 0.05f, plateWidth * 0.85f, titleHeight, false, HAlignment.CENTER, true);
    
                    String subTitle = achievement.getSubTitle();
                    if (subTitle != null) {
                        g.drawText(subTitle, subTitleFont, FORE_COLOR, x + plateOffset + plateWidth * 0.075f, plateY + plateHeight * 0.6f, plateWidth * 0.85f, subTitleHeight, false, HAlignment.CENTER, true);
                    }
                }

                trophyCount++;
                x += trophyWidth;
            }
        }
    }
}
