package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.github.tommyettinger.textra.TypingLabel;
import forge.adventure.scene.TileMapScene;
import org.apache.commons.lang3.StringUtils;

import static forge.adventure.util.Controls.newTypingLabel;

/**
 * Class for showing overlay text above rendered Scene
 */

public class OverlayText implements Disposable {
    private float alpha;
    private boolean render;
    private TypingLabel label;
    private SpriteBatch batch;
    private OrthographicCamera cam;

    public OverlayText() {
        label = newTypingLabel("");
        batch = new SpriteBatch();
        cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int screenWidth, int screenHeight) {
        cam = new OrthographicCamera(screenWidth, screenHeight);
        cam.translate(screenWidth / 2f, screenHeight / 2f);
        cam.update();
        batch.setProjectionMatrix(cam.combined);
    }

    public void update(String text) {
        alpha = 0f;
        render = !StringUtils.isEmpty(text);
        label.restart();
        label.setText(text);
        //label.setPosition(Gdx.graphics.getWidth() - label.getPrefWidth(), label.getPrefHeight());
        label.setPosition(Gdx.graphics.getWidth() / 2f - label.getWidth() / 2f, Gdx.graphics.getHeight() / 2f - label.getHeight() / 2f);
    }

    public void render(float delta) {
        if (!render)
            return;
        //TODO: Add detection check to be used on other needed scenes..
        if (Forge.currentScene instanceof TileMapScene) {
            update("");
            return;
        }
        batch.begin();
        alpha = Math.min(alpha + delta * 0.75f, 1f);
        batch.setColor(1, 1, 1, alpha);
        batch.draw(Forge.getGraphics().getBackropTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        //batch.draw(Forge.getGraphics().getBackropTexture(), label.getX(), label.getPrefHeight() / 2, label.getPrefWidth(), label.getPrefHeight());
        batch.setColor(1, 1, 1, 1);
        label.draw(batch, 1f);
        label.act(delta);
        batch.end();
    }

    public void dispose() {
        if (batch != null)
            batch.dispose();
    }

}