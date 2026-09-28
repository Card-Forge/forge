package forge.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Graphics;

public class RewardScreen extends FScreen {
    TextureRegion background;
    boolean drawPix;

    public RewardScreen(String headerCaption, TextureRegion bg) {
        super(headerCaption);
        setBackground(bg, false);
    }

    public void setBackground(TextureRegion bg, boolean isLoot) {
        if (bg == null) {
            this.background = null;
            return;
        }
        drawPix = isLoot;
        try {
            if (this.background == null) {
                this.background = new TextureRegion(bg);
            } else {
                this.background.setRegion(bg);
            }
        } catch (Exception ignored) {
            this.background = null;
        }
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
    }

    @Override
    public void draw(Graphics g) {
        if (background != null) {
            if (drawPix) {
                g.drawPix(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            } else
                g.drawImage(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    }
}
