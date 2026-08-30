package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import forge.util.ScreenUtil;

public class Adventure implements Disposable {
    private static Adventure instance;
    private float animationTimeout;
    boolean sceneWasSwapped;
    private SpriteBatch animationBatch, adventureBatch;

    private Adventure() {
        sceneWasSwapped = false;
        animationBatch = new SpriteBatch(30);
        adventureBatch = new SpriteBatch(600);
    }

    public SpriteBatch getAdventureBatch() {
        return adventureBatch;
    }

    public static Adventure getInstance() {
        return instance == null ? instance = new Adventure() : instance;
    }

    void render(float delta) {
        try {
            float transitionTime = 0.12f;
            if (sceneWasSwapped) {
                sceneWasSwapped = false;
                animationTimeout = transitionTime;
                clear();
                return;
            }
            if (animationTimeout >= 0) {
                clear();
                animationBatch.begin();
                animationTimeout -= delta;
                animationBatch.setColor(1, 1, 1, 1);
                animationBatch.draw(ScreenUtil.getInstance().getLastScreenTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                animationBatch.setColor(1, 1, 1, 1 - (1 / transitionTime) * animationTimeout);
                animationBatch.draw(Forge.getAssets().fallback_skins().get("transition"), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                animationBatch.end();
                if (animationTimeout < 0) {
                    Forge.currentScene.render();
                    Forge.storeScreen();
                    clear();
                } else {
                    return;
                }
            }
            if (animationTimeout >= -transitionTime) {
                clear();
                animationBatch.begin();
                animationTimeout -= delta;
                animationBatch.setColor(1, 1, 1, 1);
                animationBatch.draw(ScreenUtil.getInstance().getLastScreenTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                animationBatch.setColor(1, 1, 1, (1 / transitionTime) * (animationTimeout + transitionTime));
                animationBatch.draw(Forge.getAssets().fallback_skins().get("transition"), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                animationBatch.end();
                return;
            }
            Forge.currentScene.render();
            Forge.currentScene.act(delta);
        } catch (IllegalStateException | NullPointerException ie) {
            //silence this..
            //TODO: Don't silence this.
        }
    }

    void clear() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }
    @Override
    public void dispose() {
        if (animationBatch != null)
            animationBatch.dispose();
        if (adventureBatch != null)
            adventureBatch.dispose();
    }
}
