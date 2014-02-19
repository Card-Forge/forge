package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.scenes.scene2d.Stage;

import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;

public abstract class FScreen implements Screen {
    private static SkinImage background = FSkin.getImage(FSkin.Backgrounds.BG_TEXTURE);

    protected abstract void doLayout(float width, float height);

    public void draw() {
        //FSkin.drawImage(getSpriteBatch(), background, 0, 0, this.getWidth(), this.getHeight());
    }
}
