package forge.screens;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import forge.Forge.Graphics;
import forge.toolbox.FContainer;
import forge.toolbox.FProgressBar;

public class SplashScreen extends FContainer {
    private TextureRegion background;
    private final FProgressBar progressBar;

    public SplashScreen() {
        progressBar = add(new FProgressBar());
    }

    public FProgressBar getProgressBar() {
        return progressBar;
    }

    public void setBackground(TextureRegion background0) {
        background = background0;
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    @Override
    protected void drawBackground(Graphics g) {
        if (background == null) { return; }

        g.fillRect(FProgressBar.BACK_COLOR, 0, 0, getWidth(), getHeight());
 
        float x, y, w, h;
        float backgroundRatio = background.getRegionWidth() / background.getRegionHeight();
        float screenRatio = getWidth() / getHeight();
        if (backgroundRatio > screenRatio) {
            x = 0;
            w = getWidth();
            h = getWidth() * backgroundRatio;
            y = (getHeight() - h) / 2;
        }
        else {
            y = 0;
            h = getHeight();
            w = getHeight() / backgroundRatio;
            x = (getWidth() - w) / 2;
        }
        g.drawImage(background, x, y, w, h);
    }
}
