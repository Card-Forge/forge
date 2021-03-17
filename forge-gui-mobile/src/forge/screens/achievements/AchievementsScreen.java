package forge.screens.achievements;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FBufferedImage;
import forge.assets.FImage;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.card.CardZoom;
import forge.item.IPaperCard;
import forge.localinstance.achievements.Achievement;
import forge.localinstance.achievements.AchievementCollection;
import forge.menu.FDropDown;
import forge.screens.FScreen;
import forge.toolbox.FComboBox;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.util.Localizer;
import forge.util.Utils;

public class AchievementsScreen extends FScreen {
    private static final float TROPHY_PADDING = 45;
    private static final float PADDING = Utils.scale(5);
    private static final float SELECTED_BORDER_THICKNESS = Utils.scale(1);
    private static final int MIN_SHELVES = 4;
    private static final int TROPHIES_PER_SHELVE = 4;
    private static final FSkinFont NAME_FONT = FSkinFont.get(14);
    private static final FSkinFont DESC_FONT = FSkinFont.get(12);
    private static final FSkinColor TEXT_COLOR = FLabel.DEFAULT_TEXT_COLOR;
    private static final FSkinColor NOT_EARNED_COLOR = TEXT_COLOR.alphaColor(0.5f);
    
    private static AchievementsScreen achievementsScreen; //keep settings screen around so scroll positions maintained

    public static void show() {
        if (achievementsScreen == null) {
            achievementsScreen = new AchievementsScreen();
        }
        Forge.openScreen(achievementsScreen);
    }

    private final FComboBox<AchievementCollection> cbCollections = add(new FComboBox<>());
    private final TrophyCase trophyCase = add(new TrophyCase());

    private AchievementsScreen() {
        super(Localizer.getInstance().getMessage("lblAchievements"));

        AchievementCollection.buildComboBox(cbCollections);

        cbCollections.setSelectedIndex(0);
        cbCollections.setAlignment(Align.center);
        cbCollections.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                setAchievements(cbCollections.getSelectedItem());
            }
        });
        setAchievements(cbCollections.getSelectedItem());
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        width -= 2 * x;

        cbCollections.setBounds(x, y, width, cbCollections.getHeight());
        y += cbCollections.getHeight() + PADDING;
        trophyCase.setBounds(x, y, width, height - PADDING - y);
    }

    @Override
    protected float doLandscapeLayout(float width, float height) {
        //don't show header in landscape mode
        getHeader().setBounds(0, 0, 0, 0);
        doLayout(0, width, height);
        return 0;
    }

    private void setAchievements(AchievementCollection achievements0) {
        trophyCase.achievements = achievements0;
        trophyCase.selectedAchievement = null;
        trophyCase.shelfCount = (int)Math.ceil((double)achievements0.getCount() / (double)TROPHIES_PER_SHELVE);
        if (trophyCase.shelfCount < MIN_SHELVES) {
            trophyCase.shelfCount = MIN_SHELVES;
        }
        for (Achievement achievement : achievements0) {
            //call getImage() and getTexture() here so images are loaded before the first draw
            ((FBufferedImage)achievement.getImage()).getTexture();
        }
        trophyCase.revalidate();
    }

    private static class TrophyCase extends FScrollPane {
        private static final Color FORE_COLOR = new Color(239f / 255f, 220f / 255f, 144f / 255f, 1f);

        private AchievementCollection achievements;
        private int shelfCount;
        private float extraWidth = 0;
        private Achievement selectedAchievement;

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float scrollWidth = visibleWidth + extraWidth;
            float scale = scrollWidth / FSkinImage.TROPHY_CASE_TOP.getWidth();
            float scrollHeight = (FSkinImage.TROPHY_CASE_TOP.getHeight() +
                    shelfCount * FSkinImage.TROPHY_SHELF.getHeight()) * scale;
            return new ScrollBounds(scrollWidth, scrollHeight);
        }

        private Achievement getAchievementAt(float x0, float y0) {
            float w = getScrollWidth();
            float scale = w / FSkinImage.TROPHY_CASE_TOP.getWidth();

            float shelfHeight = FSkinImage.TROPHY_SHELF.getHeight() * scale;
            float trophyWidth = (FSkinImage.COMMON_TROPHY.getWidth() + TROPHY_PADDING) * scale;
            float trophyHeight = FSkinImage.COMMON_TROPHY.getHeight() * scale;
            float x = -getScrollLeft() + (w - TROPHIES_PER_SHELVE * trophyWidth) / 2;
            float y = -getScrollTop() + FSkinImage.TROPHY_CASE_TOP.getHeight() * scale + (shelfHeight - trophyHeight - 37 * scale) / 2;

            int trophyCount = 0;
            float startX = x;

            for (Achievement achievement : achievements) {
                if (trophyCount == TROPHIES_PER_SHELVE) {
                    trophyCount = 0;
                    x = startX;
                    y += shelfHeight;

                    if (y >= getHeight()) {
                        return null;
                    }
                }

                if (x <= x0 && x0 < x + trophyWidth && y <= y0 && y0 < y + shelfHeight) {
                    return achievement;
                }

                trophyCount++;
                x += trophyWidth;
            }
            return null;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            Achievement achievement = getAchievementAt(x, y);
            if (count > 1 && showCard(achievement)) {
                return true;
            }
            if (achievement == selectedAchievement) {
                achievement = null; //unselect if selected achievement tapped again
            }
            selectedAchievement = achievement;
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            selectedAchievement = getAchievementAt(x, y);
            showCard(selectedAchievement);
            return true;
        }

        private boolean showCard(Achievement achievement) {
            if (achievement != null) {
                IPaperCard pc = achievement.getPaperCard();
                if (pc != null) {
                    CardZoom.show(pc);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean zoom(float x, float y, float amount) {
            selectedAchievement = null; //unselect when zooming

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
            float scale = w / FSkinImage.TROPHY_CASE_TOP.getWidth();

            float topHeight = FSkinImage.TROPHY_CASE_TOP.getHeight() * scale;
            float shelfHeight = FSkinImage.TROPHY_SHELF.getHeight() * scale;
            float trophyOffset = TROPHY_PADDING * scale / 2;
            float trophyImageWidth = FSkinImage.COMMON_TROPHY.getWidth() * scale;
            float trophyWidth = trophyImageWidth + TROPHY_PADDING * scale;
            float trophyHeight = FSkinImage.COMMON_TROPHY.getHeight() * scale;
            float plateWidth = FSkinImage.TROPHY_PLATE.getWidth() * scale;
            float plateHeight = FSkinImage.TROPHY_PLATE.getHeight() * scale;

            float titleHeight = plateHeight * 0.55f;
            float subTitleHeight = plateHeight * 0.35f;
            FSkinFont titleFont = FSkinFont.forHeight(titleHeight);
            FSkinFont subTitleFont = FSkinFont.forHeight(subTitleHeight);

            float plateY = y + topHeight + shelfHeight - plateHeight;
            float trophyStartY = plateY - trophyHeight;
            float plateOffset = (trophyWidth - plateWidth) / 2;

            if (y + topHeight > 0) {
                g.drawImage(FSkinImage.TROPHY_CASE_TOP, x, y, w, topHeight);
            }
            y += topHeight;

            for (int i = 0; i < shelfCount; i++) {
                if (y + shelfHeight > 0) {
                    g.drawImage(FSkinImage.TROPHY_SHELF, x, y, w, shelfHeight);
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
            Rectangle selectRect = null;

            for (Achievement achievement : achievements) {
                if (trophyCount == TROPHIES_PER_SHELVE) {
                    trophyCount = 0;
                    y += shelfHeight;
                    plateY += shelfHeight;
                    x = startX;

                    if (y >= getHeight()) {
                        break;
                    }
                }

                if (plateY + plateHeight > 0) {
                    g.drawImage((FImage)achievement.getImage(), x + trophyOffset, y, trophyImageWidth, trophyHeight);
                    g.drawImage(FSkinImage.TROPHY_PLATE, x + plateOffset, plateY, plateWidth, plateHeight);
                    g.drawText(achievement.getDisplayName(), titleFont, FORE_COLOR, x + plateOffset + plateWidth * 0.075f, plateY + plateHeight * 0.05f, plateWidth * 0.85f, titleHeight, false, Align.center, true);
    
                    String subTitle = achievement.getSubTitle(false);
                    if (subTitle != null) {
                        g.drawText(subTitle, subTitleFont, FORE_COLOR, x + plateOffset + plateWidth * 0.075f, plateY + plateHeight * 0.6f, plateWidth * 0.85f, subTitleHeight, false, Align.center, true);
                    }

                    if (achievement == selectedAchievement) {
                        float selY = y - shelfHeight + trophyHeight + plateHeight;
                        g.drawRect(SELECTED_BORDER_THICKNESS, Color.GREEN, x, selY, trophyWidth, shelfHeight);
                        selectRect = new Rectangle(x, selY, trophyWidth, shelfHeight);
                    }
                }

                trophyCount++;
                x += trophyWidth;
            }

            //draw tooltip for selected achievement if needed
            if (selectRect != null) {
                String subTitle = selectedAchievement.getSubTitle(true);
                String sharedDesc = selectedAchievement.getSharedDesc();
                String mythicDesc = selectedAchievement.getMythicDesc();
                String rareDesc = selectedAchievement.getRareDesc();
                String uncommonDesc = selectedAchievement.getUncommonDesc();
                String commonDesc = selectedAchievement.getCommonDesc();

                w = getWidth() - 2 * PADDING;
                float h = NAME_FONT.getLineHeight() + 2.5f * PADDING;
                if (subTitle != null) {
                    h += DESC_FONT.getLineHeight();
                }
                if (sharedDesc != null) {
                    h += DESC_FONT.getLineHeight();
                }
                if (mythicDesc != null) {
                    h += DESC_FONT.getLineHeight();
                }
                if (rareDesc != null) {
                    h += DESC_FONT.getLineHeight();
                }
                if (uncommonDesc != null) {
                    h += DESC_FONT.getLineHeight();
                }
                if (commonDesc != null) {
                    h += DESC_FONT.getLineHeight();
                }

                x = PADDING;
                y = selectRect.y + selectRect.height + PADDING;
                if (y + h > getHeight()) {
                    if (selectRect.y - PADDING > h) {
                        y = selectRect.y - h - PADDING;
                    }
                    else {
                        y = getHeight() - h;
                    }
                }

                g.drawImage(FSkinTexture.BG_TEXTURE, x, y, w, h);
                g.fillRect(FScreen.TEXTURE_OVERLAY_COLOR, x, y, w, h);
                g.drawRect(SELECTED_BORDER_THICKNESS, FDropDown.BORDER_COLOR, x, y, w, h);

                x += PADDING;
                y += PADDING;
                w -= 2 * PADDING;
                h -= 2 * PADDING;
                g.drawText(selectedAchievement.getDisplayName(), NAME_FONT, TEXT_COLOR, x, y, w, h, false, Align.left, false);
                y += NAME_FONT.getLineHeight();
                if (subTitle != null) {
                    g.drawText(subTitle, DESC_FONT, TEXT_COLOR, x, y, w, h, false, Align.left, false);
                    y += DESC_FONT.getLineHeight();
                }
                y += PADDING;
                if (sharedDesc != null) {
                    g.drawText(selectedAchievement.isSpecial() ? sharedDesc : sharedDesc + "...", DESC_FONT, TEXT_COLOR,
                            x, y, w, h, false, Align.left, false);
                    y += DESC_FONT.getLineHeight();
                }
                if (mythicDesc != null) {
                    g.drawText(selectedAchievement.isSpecial() ? mythicDesc : "(Mythic) " + mythicDesc, DESC_FONT, //handle flavor text here too
                            selectedAchievement.earnedMythic() ? TEXT_COLOR : NOT_EARNED_COLOR,
                            x, y, w, h, false, Align.left, false);
                    y += DESC_FONT.getLineHeight();
                }
                if (rareDesc != null) {
                    g.drawText("(Rare) " + rareDesc, DESC_FONT,
                            selectedAchievement.earnedRare() ? TEXT_COLOR : NOT_EARNED_COLOR,
                            x, y, w, h, false, Align.left, false);
                    y += DESC_FONT.getLineHeight();
                }
                if (uncommonDesc != null) {
                    g.drawText("(Uncommon) " + uncommonDesc, DESC_FONT,
                            selectedAchievement.earnedUncommon() ? TEXT_COLOR : NOT_EARNED_COLOR,
                            x, y, w, h, false, Align.left, false);
                    y += DESC_FONT.getLineHeight();
                }
                if (commonDesc != null) {
                    g.drawText("(Common) " + commonDesc, DESC_FONT,
                            selectedAchievement.earnedCommon() ? TEXT_COLOR : NOT_EARNED_COLOR,
                            x, y, w, h, false, Align.left, false);
                }
            }
        }
    }
}
