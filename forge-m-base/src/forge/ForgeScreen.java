package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.scenes.scene2d.Stage;

import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinImage;

public abstract class ForgeScreen extends Stage {
    private static SkinImage background = FSkin.getImage(FSkin.Backgrounds.BG_TEXTURE);

    @Override
    public void setViewport(float width, float height) {
        super.setViewport(width, height);
        doLayout(width, height);
    }

    protected abstract void doLayout(float width, float height);

    @Override
    public void draw() {
        //draw background image
        Gdx.gl.glClearColor(0, 1, 0, 0.5f);
        Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        getSpriteBatch().begin();
        FSkin.drawImage(getSpriteBatch(), background, 0, 0, this.getWidth(), this.getHeight());
        getSpriteBatch().end();
        super.draw();
    }
}
