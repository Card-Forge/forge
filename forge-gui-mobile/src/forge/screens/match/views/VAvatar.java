package forge.screens.match.views;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Graphics;
import forge.assets.FSkin;
import forge.game.player.Player;
import forge.screens.match.FControl;
import forge.toolbox.FDisplayObject;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class VAvatar extends FDisplayObject {
    public static final float WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT;

    private final Player player;
    private final TextureRegion image;

    public VAvatar(Player player0) {
        player = player0;
        image = FSkin.getAvatars().get(player.getLobbyPlayer().getAvatarIndex());
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public boolean tap(float x, float y, int count) {
        ThreadUtil.invokeInGameThread(new Runnable() { //must invoke in game thread in case a dialog needs to be shown
            @Override
            public void run() {
                FControl.getInputProxy().selectPlayer(player, null);
            }
        });
        return true;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.drawImage(image, 0, 0, w, h);
    }
}
