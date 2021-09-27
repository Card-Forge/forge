package forge.screens.match.views;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
import forge.game.card.CounterEnumType;
import forge.game.player.PlayerView;
import forge.screens.match.MatchController;
import forge.toolbox.FDisplayObject;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class VAvatar extends FDisplayObject {
    public static final float WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT;

    private final PlayerView player;
    private final FImage image;
    private final AvatarAnimation avatarAnimation;
    private static final FSkinFont LIFE_FONT = FSkinFont.get(18);
    private static final FSkinFont LIFE_FONT_ALT = FSkinFont.get(22);

    public VAvatar(PlayerView player0) {
        player = player0;
        image = MatchController.getPlayerAvatar(player);
        setSize(WIDTH, HEIGHT);
        avatarAnimation = new AvatarAnimation();
    }

    public VAvatar(PlayerView player0, float size) {
        player = player0;
        image = MatchController.getPlayerAvatar(player);
        setSize(size, size);
        avatarAnimation = new AvatarAnimation();
    }
    private class AvatarAnimation extends ForgeAnimation {
        private static final float DURATION = 0.6f;
        private float progress = 0;
        Texture splatter = FSkin.splatter;

        private void drawAvatar(Graphics g, FImage image, float x, float y, float w, float h) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
            }
            float mod = w/2f;
            int amount = player.getAvatarLifeDifference();
            float oldAlpha = g.getfloatAlphaComposite();
            float fade = 1-(percentage*1);
            if (amount > 0) {
                g.drawAvatarImage(image, x, y, w, h, player.getHasLost());
                drawPlayerIndicator(g, w, h, percentage);
                g.setAlphaComposite(fade);
                g.drawRect(w / 12f, Color.WHITE, 0, 0, w, h);
                g.drawOutlinedText("+"+amount, Forge.altZoneTabs ? LIFE_FONT_ALT : LIFE_FONT, Color.WHITE, Color.SKY, 0, (getHeight()/2)*fade, getWidth(), getHeight(), false, Align.center, true);
                g.setAlphaComposite(oldAlpha);
            } else if (amount < 0) {
                if (splatter == null) {
                    g.setColorRGBA(1, percentage, percentage, oldAlpha);
                    g.drawAvatarImage(image, x, y, w, h, player.getHasLost());
                    g.resetColorRGBA(oldAlpha);
                } else {
                    g.drawAvatarImage(image, x, y, w, h, player.getHasLost());
                    g.setAlphaComposite(fade);
                    g.drawImage(splatter, x-mod/2, y-mod/2, w+mod, h+mod);
                    g.setAlphaComposite(oldAlpha);
                }
                drawPlayerIndicator(g, w, h, percentage);
                g.setAlphaComposite(fade);
                g.drawOutlinedText(String.valueOf(amount), Forge.altZoneTabs ? LIFE_FONT_ALT : LIFE_FONT, Color.RED, Color.ORANGE, 0, (getHeight()/2)*fade, getWidth(), getHeight(), false, Align.center, true);
                g.setAlphaComposite(oldAlpha);
            }
        }

        @Override
        protected boolean advance(float dt) {
            progress += dt;
            return progress < DURATION;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            progress = 0;
            player.setAvatarLifeDifference(0);
        }
    }
    @Override
    public boolean tap(float x, float y, int count) {
        ThreadUtil.invokeInGameThread(new Runnable() { //must invoke in game thread in case a dialog needs to be shown
            @Override
            public void run() {
                MatchController.instance.getGameController().selectPlayer(player, null);
            }
        });
        return true;
    }

    public Vector2 getTargetingArrowOrigin() {
        return getTargetingArrowOrigin(2);
    }
    public Vector2 getTargetingArrowOrigin(int numplayers) {
        Vector2 origin = new Vector2(screenPos.x, screenPos.y);

        float modx = numplayers > 2 ? 0.25f : 0.75f;

        origin.x += WIDTH * modx;
        if (origin.y < MatchController.getView().getHeight() / numplayers) {
            origin.y += HEIGHT * 0.75f; //target bottom right corner if on top half of screen
        }
        else {
            origin.y += HEIGHT * 0.25f; //target top right corner if on bottom half of screen
        }

        return origin;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();

        if (avatarAnimation != null && !MatchController.instance.getGameView().isMatchOver()) {
            if (player.wasAvatarLifeChanged()) {
                avatarAnimation.start();
                avatarAnimation.drawAvatar(g, image, 0, 0, w, h);
            } else {
                g.drawAvatarImage(image, 0, 0, w, h, player.getHasLost());
                drawPlayerIndicator(g, w, h, 1);
            }
        } else {
            g.drawAvatarImage(image, 0, 0, w, h, player.getHasLost());
        }

        if (Forge.altPlayerLayout && !Forge.altZoneTabs && Forge.isLandscapeMode())
            return;

        //display XP in lower right corner of avatar
        int xp = player.getCounters(CounterEnumType.EXPERIENCE);
        if (xp > 0) {
            //use font and padding from phase indicator so text lines up
            FSkinFont font = VPhaseIndicator.BASE_FONT;
            float xpHeight = font.getCapHeight();
            g.drawOutlinedText(xp + " XP", font, Color.WHITE, Color.BLACK, 0, h - xpHeight - VPhaseIndicator.PADDING_Y, w - VPhaseIndicator.PADDING_X, h, false, Align.right, false);
        }
    }
    private void drawPlayerIndicator(Graphics g, float w, float h, float alphaModifier) {
        float oldAlpha = g.getfloatAlphaComposite();
        boolean displayPriority = true;
        for (PlayerView playerView : MatchController.instance.getGameView().getPlayers()) {
            if (playerView.isAI()) {
                //only display priority indicator if there's no AI player
                displayPriority = false;
                break;
            }
        }
        //turn indicator
        if (player == MatchController.instance.getGameView().getPlayerTurn()) {
            float alpha = displayPriority ? 1f : 0.8f;
            if (alphaModifier < 1)
                alpha = alphaModifier;
            g.setAlphaComposite(alpha);
            g.drawRect(w / 16f, Color.CYAN, 0, 0, w, h);
            g.setAlphaComposite(oldAlpha);
        }
        //priority indicator
        if (displayPriority && player.getHasPriority() && alphaModifier == 1) {
            g.setAlphaComposite(0.6f);
            g.drawRect(w / 16f, Color.LIME, 0, 0, w, h);
            g.setAlphaComposite(oldAlpha);
        }
    }
}
