package forge.toolbox;

import com.badlogic.gdx.graphics.Texture;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
import forge.card.CardImageRenderer;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.card.CardStateName;
import forge.game.card.CardView;
import forge.game.zone.ZoneType;
import forge.screens.match.MatchController;
import forge.util.Utils;

public class FCardPanel extends FDisplayObject {
    public static final float ASPECT_RATIO = 3.5f / 2.5f;
    public static final float PADDING = Utils.scale(2);
    public static final float TARGET_ORIGIN_FACTOR_X = 0.15f;
    public static final float TARGET_ORIGIN_FACTOR_Y = 0.5f;

    private CardView card;
    private boolean tapped;
    private boolean highlighted;
    CardTapAnimation tapAnimation;
    CardUnTapAnimation untapAnimation;
    CardDestroyedAnimation destroyedAnimation;
    CardTransformAnimation transformAnimation;

    public FCardPanel() {
        this(null);
    }
    public FCardPanel(CardView card0) {
        card = card0;
        tapAnimation = new CardTapAnimation();
        untapAnimation = new CardUnTapAnimation();
        destroyedAnimation = new CardDestroyedAnimation();
        transformAnimation = new CardTransformAnimation();
    }

    public CardView getCard() {
        return card;
    }
    public void setCard(CardView card0) {
        card = card0;
    }

    public boolean isHighlighted() {
        return highlighted;
    }
    public void setHighlighted(boolean highlighted0) {
        highlighted = highlighted0;
    }

    public boolean isTapped() {
        return tapped;
    }
    public void setTapped(final boolean tapped0) {
        tapped = tapped0;
    }

    protected float getTappedAngle() {
        return -90;
    }

    protected boolean renderedCardContains(float x, float y) {
        float left = PADDING;
        float top = PADDING;
        float w = getWidth() - 2 * PADDING;
        float h = getHeight() - 2 * PADDING;
        if (w == h) { //adjust width if needed to make room for tapping
            w = h / ASPECT_RATIO;
        }

        if (tapped) { //rotate box if tapped
            top += h - w;
            float temp = w;
            w = h;
            h = temp;
        }

        return x >= left && x <= left + w && y >= top && y <= top + h;
    }

    protected float getPadding() {
        return PADDING;
    }

    //allow overriding stack position
    protected CardStackPosition getStackPosition() {
        return CardStackPosition.Top;
    }

    @Override
    public void draw(Graphics g) {
        if (card == null) { return; }
        boolean animate = Forge.animatedCardTapUntap;
        float padding = getPadding();
        float x = padding;
        float y = padding;
        float w = getWidth() - 2 * padding;
        float h = getHeight() - 2 * padding;
        if (w == h) { //adjust width if needed to make room for tapping
            w = h / ASPECT_RATIO;
        }
        float edgeOffset = w / 2f;

        if (!ZoneType.Battlefield.equals(card.getZone())) {
            rotateTransform(g, x, y, w, h, edgeOffset, false);
            return;
        }

        if (!animate || MatchController.instance.isGameFast() || MatchController.instance.getGameView().isMatchOver()) {
            //don't animate if game is fast or match is over
            rotateTransform(g, x, y, w, h, edgeOffset, false);
            card.updateNeedsTapAnimation(false);
            card.updateNeedsUntapAnimation(false);
            card.updateNeedsTransformAnimation(false);
        } else {
            //card destroy animation
            if (card.wasDestroyed()) {
                if (destroyedAnimation.progress < 1) {
                    destroyedAnimation.start();
                    destroyedAnimation.drawCard(g, card, x, y, w, h, edgeOffset);
                } else {
                    rotateTransform(g, x, y, w, h, edgeOffset, animate);
                }
                return;
            }
            //tap-untap animation
            if (card.needsTapAnimation()) {
                //draw tapped
                if (tapAnimation.progress < 1) {
                    tapAnimation.start();
                    tapAnimation.drawCard(g, card, x, y, w, h, w / 2f, getTappedAngle());
                } else {
                    rotateTransform(g, x, y, w, h, edgeOffset, animate);
                }
            } else if (card.needsUntapAnimation()) {
                //draw untapped
                if (untapAnimation.progress < 1) {
                    untapAnimation.start();
                    untapAnimation.drawCard(g, card, x, y, w, h, edgeOffset);
                } else {
                    rotateTransform(g, x, y, w, h, edgeOffset, animate);
                }
            } else {
                rotateTransform(g, x, y, w, h, edgeOffset, animate);
            }
        }
    }
    private void rotateTransform(Graphics g, float x, float y, float w, float h, float edgeOffset, boolean animate) {
        if (tapped) {
            g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, getTappedAngle());
        }
        if (card.needsTransformAnimation() && animate) {
            transformAnimation.start();
            transformAnimation.drawCard(g, card, x, y, w, h);
        } else {
            CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
        }
        if (tapped) {
            g.endTransform();
        }
    }
    private class CardDestroyedAnimation extends ForgeAnimation {
        private static final float DURATION = 0.6f;
        private float progress = 0;
        private Texture splatter = FSkin.splatter;
        private void drawCard(Graphics g, CardView card, float x, float y, float w, float h, float edgeOffset) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
                progress = 0;
            }
            float mod = w*percentage;
            float oldAlpha = g.getfloatAlphaComposite();
            if (tapped) {
                g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, getTappedAngle());
            }
            CardRenderer.drawCardWithOverlays(g, card, x-mod/2, y-mod/2, w+mod, h+mod, getStackPosition());
            if (splatter != null) {
                g.setAlphaComposite(0.6f);
                g.drawCardImage(splatter, null,x-mod/2, y-mod/2, w+mod, h+mod, true, false);
                g.setAlphaComposite(oldAlpha);
            }
            if (tapped) {
                g.endTransform();
            }
        }
        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }
        @Override
        protected void onEnd(boolean endingAll) {
        }
    }
    private class CardTransformAnimation extends ForgeAnimation {
        private float DURATION = 0.18f;
        private float progress = 0;

        private void drawCard(Graphics g, CardView card, float x, float y, float w, float h) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
                progress = 0;
            }
            float mod = percentage;
            float y2 = y + (h - (h*mod))/2;
            float x2 = x + (w - (w*mod))/2;
            float w2 = w*mod;
            float h2 = h*mod;
            float gap = (h/2) - (percentage*(h/2));
            if (card.getCurrentState().getState() == CardStateName.Original) {
                DURATION = 0.16f;
                //rollback
                CardRenderer.drawCardWithOverlays(g, card, x2, y, w2, h, getStackPosition());
            } else {
                //transform
                if (card.getCurrentState().getState() == CardStateName.Transformed || card.getCurrentState().getState() == CardStateName.Flipped) {
                    DURATION = 0.16f;
                    CardRenderer.drawCardWithOverlays(g, card, x2, y, w2, h, getStackPosition());
                } else if (card.getCurrentState().getState() == CardStateName.Meld) {
                    if (CardRenderer.getMeldCardParts(card.getCurrentState().getImageKey(), false) == CardImageRenderer.forgeArt) {
                        DURATION = 0.18f;
                        CardRenderer.drawCardWithOverlays(g, card, x2, y2, w2, h2, getStackPosition());
                    } else {
                        //Meld Animation merging
                        DURATION = 0.25f;
                        //top card
                        g.drawImage(CardRenderer.getMeldCardParts(card.getCurrentState().getImageKey(), false), x, y-gap, w, h/2);
                        //bottom card
                        g.drawImage(CardRenderer.getMeldCardParts(card.getCurrentState().getImageKey(), true), x, y+h/2+gap, w, h/2);
                    }
                }
            }
        }
        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }
        @Override
        protected void onEnd(boolean endingAll) {
            card.updateNeedsTransformAnimation(false);
        }
    }
    private class CardUnTapAnimation extends ForgeAnimation {
        private static final float DURATION = 0.18f;
        private float progress = 0;
        private void drawCard(Graphics g, CardView card, float x, float y, float w, float h, float edgeOffset) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
                progress = 0;
            }
            float angle = -90 + (percentage*90);
            g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, angle);
            CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
            g.endTransform();
        }
        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }
        @Override
        protected void onEnd(boolean endingAll) {
            card.updateNeedsUntapAnimation(false);
        }
    }
    private class CardTapAnimation extends ForgeAnimation {
        private static final float DURATION = 0.18f;
        private float progress = 0;
        private void drawCard(Graphics g, CardView card, float x, float y, float w, float h, float edgeOffset, float angle) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
                progress = 0;
            }
            g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, percentage*angle);
            CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
            g.endTransform();
        }
        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }
        @Override
        protected void onEnd(boolean endingAll) {
            card.updateNeedsTapAnimation(false);
        }
    }
    public String toString() {
        return card == null ? "" : card.toString();
    }
}
