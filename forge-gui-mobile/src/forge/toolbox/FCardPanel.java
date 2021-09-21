package forge.toolbox;

import com.badlogic.gdx.graphics.Texture;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkin;
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
    private boolean wasTapped;
    private boolean wasTransformed;
    CardTapAnimation tapAnimation;
    CardUnTapAnimation untapAnimation;
    CardDestroyedAnimation destroyedAnimation;
    CardTransformAnimation transformAnimation;
    CardRollbackAnimation rollbackAnimation;

    public FCardPanel() {
        this(null);
    }
    public FCardPanel(CardView card0) {
        card = card0;
        tapAnimation = new CardTapAnimation();
        untapAnimation = new CardUnTapAnimation();
        destroyedAnimation = new CardDestroyedAnimation();
        transformAnimation = new CardTransformAnimation();
        rollbackAnimation = new CardRollbackAnimation();
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

    private class CardRollbackAnimation extends ForgeAnimation {
        private static final float DURATION = 0.12f;
        private float progress = 0;
        private boolean finished;

        private void drawCard(Graphics g, CardView card, float x, float y, float w, float h, float edgeOffset) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
                wasTransformed = false;
            }
            float mod = percentage;
            float x2 = x + (w - (w*mod))/2;
            float w2 = w*mod;
            if (tapped) {
                g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, getTappedAngle());
            }
            CardRenderer.drawCardWithOverlays(g, card, x2, y, w2, h, getStackPosition());
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
            finished = true;
        }
    }
    private class CardTransformAnimation extends ForgeAnimation {
        private static final float DURATION = 0.12f;
        private float progress = 0;
        private boolean finished;

        private void drawCard(Graphics g, CardView card, float x, float y, float w, float h, float edgeOffset) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
                wasTransformed = true;
            }
            float mod = card.hasAlternateState() ? percentage : 1f;
            float y2 = y + (h - (h*mod))/2;
            float x2 = x + (w - (w*mod))/2;
            float w2 = w*mod;
            float h2 = h*mod;
            if (tapped) {
                g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, getTappedAngle());
            }
            if (card.getCurrentState().getState() == CardStateName.Transformed || card.getCurrentState().getState() == CardStateName.Flipped)
                CardRenderer.drawCardWithOverlays(g, card, x2, y, w2, h, getStackPosition());
            else if (card.getCurrentState().getState() == CardStateName.Meld)
                CardRenderer.drawCardWithOverlays(g, card, x2, y2, w2, h2, getStackPosition());
            else //default
                CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
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
            finished = true;
        }
    }
    private class CardDestroyedAnimation extends ForgeAnimation {
        private static final float DURATION = 0.6f;
        private float progress = 0;
        private boolean finished;
        private Texture splatter = FSkin.splatter;

        private void drawCard(Graphics g, CardView card, float x, float y, float w, float h, float edgeOffset) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            float mod = (w/3f)*percentage;
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
            finished = true;
        }
    }
    private class CardUnTapAnimation extends ForgeAnimation {
        private static final float DURATION = 0.14f;
        private float progress = 0;
        private boolean finished;

        private void drawCard(Graphics g, CardView card, float x, float y, float w, float h, float edgeOffset) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            float angle = -90 + (percentage*90);
            if (wasTapped) {
                g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, angle);
                CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
                g.endTransform();
            } else {
                CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
            }
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            finished = true;
        }
    }

    private class CardTapAnimation extends ForgeAnimation {
        private static final float DURATION = 0.18f;
        private float progress = 0;
        private boolean finished;

        private void drawCard(Graphics g, CardView card, float x, float y, float w, float h, float edgeOffset, float angle) {
            float percentage = progress / DURATION;
            if (percentage > 1) {
                percentage = 1;
                wasTapped = true;
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
            finished = true;
        }
    }

    @Override
    public void draw(Graphics g) {
        if (card == null) { return; }

        boolean isGameFast = MatchController.instance.isGameFast();
        boolean animate = ZoneType.Battlefield.equals(card.getZone()) && Forge.animatedCardTapUntap;

        float padding = getPadding();
        float x = padding;
        float y = padding;
        float w = getWidth() - 2 * padding;
        float h = getHeight() - 2 * padding;
        if (w == h) { //adjust width if needed to make room for tapping
            w = h / ASPECT_RATIO;
        }
        float edgeOffset = w / 2f;

        if (!animate || isGameFast || MatchController.instance.getGameView().isMatchOver()) {
            //don't animate if game is fast or match is over
            if (tapped) {
                g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, getTappedAngle());
            }
            CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
            if (tapped) {
                g.endTransform();
            }
        } else {
            //card destroy animation
            if (card.wasDestroyed()) {
                if (destroyedAnimation != null) {
                    if (destroyedAnimation.progress < 1) {
                        destroyedAnimation.start();
                        destroyedAnimation.drawCard(g, card, x, y, w, h, edgeOffset);
                    } else {
                        if (tapped) {
                            g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, getTappedAngle());
                        }
                        CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
                        if (tapped) {
                            g.endTransform();
                        }
                    }
                }
                return;
            }
            //card transform animation
            if (card.getCurrentState().getState() == CardStateName.Original && wasTransformed) {
                //reset transform
                if (transformAnimation != null)
                    transformAnimation.progress = 0;
                if (rollbackAnimation != null) {
                    if (rollbackAnimation.progress < 1) {
                        rollbackAnimation.start();
                        rollbackAnimation.drawCard(g, card, x, y, w, h, edgeOffset);
                    } else {
                        if (tapped) {
                            g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, getTappedAngle());
                        }
                        CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
                        if (tapped) {
                            g.endTransform();
                        }
                    }
                }
                return;
            }
            if (card.hasAlternateState() && (card.getCurrentState().getState() == CardStateName.Flipped
                    || card.getCurrentState().getState() == CardStateName.Transformed || card.getCurrentState().getState() == CardStateName.Meld) && !wasTransformed) {
                //reset rollback
                if (rollbackAnimation != null)
                    rollbackAnimation.progress = 0;
                if (transformAnimation != null) {
                    if (transformAnimation.progress < 1) {
                        transformAnimation.start();
                        transformAnimation.drawCard(g, card, x, y, w, h, edgeOffset);
                    } else {
                        if (tapped) {
                            g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, getTappedAngle());
                        }
                        CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
                        if (tapped) {
                            g.endTransform();
                        }
                    }
                }
                return;
            }
            //tap-untap animation
            if (tapped) {
                //reset untapAnimation
                if (untapAnimation != null) {
                    untapAnimation.progress = 0;
                }
                //draw tapped
                if (tapAnimation != null) {
                    if (tapAnimation.progress < 1) {
                        tapAnimation.start();
                        tapAnimation.drawCard(g, card, x, y, w, h, w / 2f, getTappedAngle());
                    } else {
                        g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, getTappedAngle());
                        CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
                        g.endTransform();
                    }
                }
            } else {
                //reset tapAnimation
                if (tapAnimation != null) {
                    tapAnimation.progress = 0;
                }
                //draw untapped
                if (untapAnimation != null) {
                    if (untapAnimation.progress < 1) {
                        untapAnimation.start();
                        untapAnimation.drawCard(g, card, x, y, w, h, edgeOffset);
                    } else {
                        wasTapped = false;
                        CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());
                    }
                }
            }
        }
    }

    public String toString() {
        return card == null ? "" : card.toString();
    }
}
