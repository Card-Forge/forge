package forge.screens.match.views;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Forge.Graphics;
import forge.assets.FSkin;
import forge.toolbox.FDisplayObject;
import forge.utils.Utils;

public class VAvatar extends FDisplayObject {
    public static final float WIDTH = Utils.AVG_FINGER_WIDTH;
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT;

    private final TextureRegion image;

    public VAvatar(int avatarIndex) {
        image = FSkin.getAvatars().get(avatarIndex);
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.drawImage(image, 0, 0, w, h);
    }
}
