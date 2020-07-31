package forge.screens.planarconquest;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.ImageKeys;
import forge.animation.ForgeAnimation;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.ImageCache;
import forge.card.CardRenderer;
import forge.card.CardZoom;
import forge.card.CardRenderer.CardStackPosition;
import forge.item.PaperCard;
import forge.planarconquest.ConquestReward;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDialog;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.util.Utils;
import forge.util.Localizer;

public class ConquestRewardDialog extends FScrollPane {
    private static final float PADDING = Utils.scale(5);

    public static void show(String title, PaperCard card, Runnable callback0) {
        List<ConquestReward> rewards = new ArrayList<>(1);
        rewards.add(new ConquestReward(card, 0));
        show(title, rewards, callback0);
    }
    public static void show(String title, Iterable<ConquestReward> rewards, Runnable callback0) {
        ConquestRewardDialog revealer = new ConquestRewardDialog(title, rewards, callback0);
        revealer.dialog.show();
    }

    private final RevealDialog dialog;
    private final List<CardRevealer> cardRevealers = new ArrayList<>();
    private final CardRevealAnimation animation;
    private final Runnable callback;

    private int columnCount;
    private float totalZoomAmount;
    private CardRevealer focalCard;

    private ConquestRewardDialog(String title, Iterable<ConquestReward> rewards, Runnable callback0) {
        dialog = new RevealDialog(title);
        callback = callback0;

        for (ConquestReward reward : rewards) {
            cardRevealers.add(this.add(new CardRevealer(reward)));
        }

        //determine starting column count based on card count
        int cardCount = cardRevealers.size();
        if (cardCount == 1) {
            columnCount = 1;
        }
        else if (cardCount < 5) {
            if (Forge.extrawide.equals("default"))
                columnCount = 2;
            else {
                if (cardCount == 4)
                    columnCount = 4;
                else
                    columnCount = 3;
            }
        }
        else {
            if (Forge.extrawide.equals("extrawide"))
                columnCount = 5;
            else if (Forge.extrawide.equals("wide"))
                columnCount = 4;
            else
                columnCount = 3;
        }

        animation = new CardRevealAnimation();
    }

    @Override
    protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
        float x = PADDING;
        float y = PADDING;
        float cardWidth = (visibleWidth - (columnCount + 1) * PADDING) / columnCount;
        float cardHeight = cardWidth * FCardPanel.ASPECT_RATIO;

        //ensure card height doesn't exceed max allowed height
        float maxHeight = visibleHeight - 2 * PADDING;
        if (cardHeight > maxHeight) {
            cardHeight = maxHeight;
            float newCardWidth = cardHeight / FCardPanel.ASPECT_RATIO;
            x += (cardWidth - newCardWidth) * columnCount / 2;
            cardWidth = newCardWidth;
        }

        float startX = x;
        int cardCount = cardRevealers.size();
        cardRevealers.get(0).setBounds(x, y, cardWidth, cardHeight);
        for (int i = 1; i < cardCount; i++) {
            if (i % columnCount == 0) {
                x = startX;
                y += cardHeight + PADDING;
            }
            else {
                x += cardWidth + PADDING;
            }
            cardRevealers.get(i).setBounds(x, y, cardWidth, cardHeight);
        }
        return new ScrollBounds(visibleWidth, y + cardHeight + PADDING);
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

            add(ConquestRewardDialog.this);

            initButton(0, Localizer.getInstance().getMessage("lblOK"), new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    hide();
                    if (callback != null) {
                        callback.run();
                    }
                }
            });
            initButton(1, Localizer.getInstance().getMessage("lblSkip"), new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    animation.skip();
                }
            });

            //disable both buttons initially
            setButtonEnabled(0, false);
            setButtonEnabled(1, false);
        }

        @Override
        protected float layoutAndGetHeight(float width, float maxHeight) {
            ConquestRewardDialog.this.setBounds(0, 0, width, maxHeight);
            return maxHeight;
        }

        @Override
        public boolean fling(float velocityX, float velocityY) {
            return false; //disable ability to hide dialog since it's animated
        }

        @Override
        protected void onRevealFinished() {
            animation.start(); //start animation when dialog finished opening
            setButtonEnabled(1, true); //enable Skip button
        }
    }

    private class CardRevealAnimation extends ForgeAnimation {
        private static final float DURATION_PER_CARD = 0.7f;

        private float progress = -0.25f; //delay start of animation slightly
        private int currentIndex;

        private CardRevealAnimation() {
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            if (progress <= 0) { return true; }

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
                //ensure current card in view
                if (getScrollHeight() > getHeight() && index < cardCount) {
                    CardRevealer currentCard = cardRevealers.get(index);
                    scrollIntoView(currentCard, currentCard.getHeight() / (columnCount * PADDING) / 2);
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
            int cardCount = cardRevealers.size();
            for (int i = currentIndex; i < cardCount; i++) {
                cardRevealers.get(i).progress = 1;
            }
            currentIndex = cardCount;
            animation.stop();
            scrollToBottom();
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

    private class CardRevealer extends FLabel {
        private static final float DUPLICATE_ALPHA_COMPOSITE = 0.35f;
        private static final float FLIP_DURATION = 0.85f;
        private static final float FADE_DUPLICATE_DURATION = 0.1f; //give a brief interlude before the next flip

        private final ConquestReward reward;
        private float progress;

        private CardRevealer(ConquestReward reward0) {
            super(new FLabel.Builder().iconScaleWithFont(true).iconScaleFactor(1));

            reward = reward0;
            if (reward.isDuplicate()) {
                setFont(FSkinFont.get(20));
                setIcon(FSkinImage.AETHER_SHARD);
                setAlignment(Align.center);
                setText(String.valueOf(reward.getReplacementShards()));
            }
        }

        private boolean showCardZoom() {
            int index = -1;
            List<PaperCard> cards = new ArrayList<>();
            for (int i = 0; i < animation.currentIndex; i++) {
                CardRevealer revealer = cardRevealers.get(i);
                if (revealer == this) {
                    index = i;
                }
                cards.add(revealer.reward.getCard());
            }
            if (index == -1) { return false; } //don't show zoom for unrevealed cards

            CardZoom.show(cards, index, null);
            return true;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            return showCardZoom();
        }

        @Override
        public boolean longPress(float x, float y) {
            return showCardZoom();
        }

        @Override
        public void draw(Graphics g) {
            float w = getWidth();
            float h = getHeight();

            if (progress >= FLIP_DURATION) {
                float fadeProgress = (progress - FLIP_DURATION) / FADE_DUPLICATE_DURATION;
                if (reward.isDuplicate()) {
                    float alphaComposite = DUPLICATE_ALPHA_COMPOSITE;
                    if (fadeProgress < 1) {
                        alphaComposite += (1 - fadeProgress) * (1 - DUPLICATE_ALPHA_COMPOSITE);
                    }
                    g.setAlphaComposite(alphaComposite);
                }
                CardRenderer.drawCard(g, reward.getCard(), 0, 0, w, h, CardStackPosition.Top);
                if (reward.isDuplicate()) {
                    g.resetAlphaComposite();
                    if (fadeProgress >= 1) {
                        drawContent(g, 0, 0, w, h);
                    }
                }
            }
            else {
                float halfDuration = FLIP_DURATION / 2;
                if (progress >= halfDuration) {
                    float flipWidth = w * (progress - halfDuration) / halfDuration;
                    CardRenderer.drawCard(g, reward.getCard(), (w - flipWidth) / 2, 0, flipWidth, h, CardStackPosition.Top);
                }
                else {
                    Texture cardBack = ImageCache.getImage(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD), true);
                    if (cardBack != null) {
                        float flipWidth = w * (halfDuration - progress) / halfDuration;
                        g.drawImage(cardBack, (w - flipWidth) / 2, 0, flipWidth, h);
                    }
                }
            }
        }
    }
}
