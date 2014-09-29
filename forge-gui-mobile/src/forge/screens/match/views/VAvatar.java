package forge.screens.match.views;

import com.badlogic.gdx.math.Vector2;

import forge.Graphics;
import forge.assets.FImage;
import forge.match.MatchUtil;
import forge.screens.match.MatchController;
import forge.toolbox.FDisplayObject;
import forge.util.ThreadUtil;
import forge.util.Utils;
import forge.view.PlayerView;

public class VAvatar extends FDisplayObject {
    public static final float WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT;

    private final PlayerView player;
    private final FImage image;

    public VAvatar(PlayerView player0) {
        player = player0;
        image = MatchController.getPlayerAvatar(player);
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public boolean tap(float x, float y, int count) {
        ThreadUtil.invokeInGameThread(new Runnable() { //must invoke in game thread in case a dialog needs to be shown
            @Override
            public void run() {
                MatchUtil.getGameView().selectPlayer(player, null);
            }
        });
        return true;
    }

    public Vector2 getTargetingArrowOrigin() {
        Vector2 origin = new Vector2(screenPos.x, screenPos.y);

        origin.x += WIDTH * 0.75f;
        if (origin.y < MatchController.getView().getHeight() / 2) {
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
        g.drawImage(image, 0, 0, w, h);
    }
}
