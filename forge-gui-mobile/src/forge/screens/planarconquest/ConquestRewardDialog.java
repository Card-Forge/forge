package forge.screens.planarconquest;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.item.PaperCard;
import forge.planarconquest.ConquestReward;
import forge.toolbox.FDialog;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.util.Utils;

public class ConquestRewardDialog extends FScrollPane {
    public static void show(String title, PaperCard card) {
        List<ConquestReward> rewards = new ArrayList<ConquestReward>(1);
        rewards.add(new ConquestReward(card, 0));
        show(title, rewards);
    }
    public static void show(String title, Iterable<ConquestReward> rewards) {
        ConquestRewardDialog revealer = new ConquestRewardDialog(title, rewards);
        revealer.dialog.show();
    }

    private final RevealDialog dialog;
    private final List<CardRevealer> cardRevealers = new ArrayList<CardRevealer>();
    private final CardRevealAnimation animation;

    private int columnCount;
    private float totalZoomAmount;
    private CardRevealer focalCard;

    private ConquestRewardDialog(String title, Iterable<ConquestReward> rewards) {
        dialog = new RevealDialog(title);

        for (ConquestReward reward : rewards) {
            cardRevealers.add(this.add(new CardRevealer(reward)));
        }

        //determine starting column count based on card count
        int cardCount = cardRevealers.size();
        if (cardCount == 1) {
            columnCount = 1;
        }
        else if (cardCount < 5) {
            columnCount = 2;
        }
        else {
            columnCount = 3;
        }

        animation = new CardRevealAnimation();
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        float y = 0;
        return new ScrollBounds(visibleWidth, y);
    }

    @Override
    public boolean zoom(float x, float y, float amount) {
        totalZoomAmount += amount;

        float columnZoomAmount = 2 * Utils.AVG_FINGER_WIDTH;
        while (totalZoomAmount >= columnZoomAmount) {
            setColumnCount(columnCount - 1);
            totalZoomAmount -= columnZoomAmount;
        }
        while (totalZoomAmount <= -columnZoomAmount) {
            setColumnCount(columnCount + 1);
            totalZoomAmount += columnZoomAmount;
        }
        return true;
    }

    public void setColumnCount(int columnCount0) {
        setColumnCount(columnCount0, false);
    }
    private void setColumnCount(int columnCount0, boolean forSetup) {
        if (columnCount0 < 1) {
            columnCount0 = 1;
        }
        else if (columnCount0 > cardRevealers.size()) {
            columnCount0 = cardRevealers.size();
        }
        if (columnCount == columnCount0) { return; }
        columnCount = columnCount0;

        //determine card to retain scroll position of following column count change
        CardRevealer focalCard0 = getFocalCard();
        if (focalCard0 == null) {
            revalidate();
            return;
        }

        float offsetTop = focalCard0.getTop() - getScrollTop();
        revalidate();
        setScrollTop(focalCard0.getTop() - offsetTop);
        focalCard = focalCard0; //cache focal item so consecutive column count changes use the same card
    }

    private CardRevealer getFocalCard() {
        if (focalCard != null) { //use cached focalCard if one
            return focalCard;
        }

        //if not item hovered, use first fully visible card as focal point
        final float visibleTop = getScrollTop();
        for (CardRevealer card : cardRevealers) {
            if (card.getTop() >= visibleTop) {
                return card;
            }
        }
        return cardRevealers.get(0);
    }

    private class RevealDialog extends FDialog {
        private RevealDialog(String title) {
            super(title, 2);
            initButton(0, "OK", new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    hide();
                }
            });
            setButtonEnabled(0, false); //disable OK button
            initButton(1, "Skip", new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    animation.skip();
                }
            });
        }

        @Override
        protected float layoutAndGetHeight(float width, float maxHeight) {
            ConquestRewardDialog.this.setBounds(0, 0, width, maxHeight);
            return maxHeight;
        }
    }

    private class CardRevealAnimation extends ForgeAnimation {
        private static final float DURATION_PER_CARD = 0.5f;

        private float progress;
        private int currentIndex;

        private CardRevealAnimation() {
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;

            int index = (int)(progress / DURATION_PER_CARD);
            int cardCount = cardRevealers.size();
            if (index > cardCount) {
                index = cardCount;
            }
            if (index > currentIndex) {
                //finish reveal of previous cards
                for (int i = currentIndex; i < index; i++) {
                    cardRevealers.get(i).progress = 1;
                }
            }

            currentIndex = index;
            if (currentIndex == cardCount) {
                return false; //end animation when all cards have been revealed
            }

            //update progress of current card
            cardRevealers.get(currentIndex).progress = (progress - currentIndex * DURATION_PER_CARD) / DURATION_PER_CARD;
            return true;
        }

        //skip remainder of animation
        private void skip() {
            currentIndex = cardRevealers.size();
            for (int i = currentIndex; i < currentIndex; i++) {
                cardRevealers.get(i).progress = 1;
            }
            animation.stop();
        }

        @Override
        protected void onEnd(boolean endingAll) {
            //enable OK button and disable Skip button when animation ends
            if (currentIndex == cardRevealers.size()) {
                dialog.setButtonEnabled(0, true);
                dialog.setButtonEnabled(1, false);
            }
        }
    }

    private static class CardRevealer extends FLabel {
        private static final float DUPLICATE_ALPHA_COMPOSITE = 0.35f;

        private final ConquestReward reward;
        private float progress;

        private CardRevealer(ConquestReward reward0) {
            super(new FLabel.Builder().iconScaleFactor(1f));

            reward = reward0;
            if (reward.isDuplicate()) {
                setFont(FSkinFont.get(20));
                setIcon(FSkinImage.QUEST_COIN);
                setAlignment(HAlignment.CENTER);
            }
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();

            if (reward.isDuplicate()) {
                g.setAlphaComposite(DUPLICATE_ALPHA_COMPOSITE);
            }
            CardRenderer.drawCard(g, reward.getCard(), 0, 0, w, h, CardStackPosition.Top);
            if (reward.isDuplicate()) {
                g.resetAlphaComposite();
                drawContent(g, 0, 0, w, h);
            }
        }
    }
}
