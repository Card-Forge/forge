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
    private AvatarAnimation avatarAnimation;

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
        private boolean finished;
        Texture scratch = FSkin.scratch;

        private void drawAvatar(Graphics g, FImage image, float x, float y, float w, float h) {
            float percentage = progress / DURATION;
            if (percentage < 0) {
                percentage = 0;
            } else if (percentage > 1) {
                percentage = 1;
                //animation finished clear avatar red overlay
                player.setAvatarWasDamaged(false);
            }
            float mod = w/2f;
            if (scratch == null) {
                g.setColorRGBA(1, percentage, percentage, g.getfloatAlphaComposite());
                g.drawAvatarImage(image, x, y, w, h, player.getHasLost());
                g.resetColorRGBA(g.getfloatAlphaComposite());
            } else {
                g.drawAvatarImage(image, x, y, w, h, player.getHasLost());
                g.setAlphaComposite(1-(percentage*1));
                g.drawImage(scratch, x-mod/2, y-mod/2, w+mod, h+mod);
                g.resetAlphaComposite();
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

        if (player.getAvatarWasDamaged() && avatarAnimation.progress < 1) {
            avatarAnimation.start();
            avatarAnimation.drawAvatar(g, image, 0, 0, w, h);
        } else {
            avatarAnimation.progress = 0;
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
}
