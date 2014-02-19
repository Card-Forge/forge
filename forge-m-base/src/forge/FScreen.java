package forge;

import com.badlogic.gdx.Screen;

public abstract class FScreen implements Screen {

    protected abstract void doLayout(float width, float height);

    public void draw() {
        //FSkin.drawImage(getSpriteBatch(), background, 0, 0, this.getWidth(), this.getHeight());
    }
}
