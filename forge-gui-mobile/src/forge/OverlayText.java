package forge;

import com.github.tommyettinger.textra.TypingLabel;
import forge.adventure.scene.GameScene;
import forge.adventure.scene.TileMapScene;
import org.apache.commons.lang3.StringUtils;

import static forge.adventure.util.Controls.newTypingLabel;

/**
 * Class for showing overlay text above rendered Scene
 */

public class OverlayText {
    private float alpha;
    private boolean render;
    private TypingLabel label;
    private static OverlayText instance;

    public static OverlayText getInstance() {
        return instance == null ? instance = new OverlayText() : instance;
    }

    private OverlayText() {
        label = newTypingLabel("");
    }

    public void update(String text) {
        alpha = 0f;
        render = !StringUtils.isEmpty(text);
        label.restart();
        label.setText(text);
        label.setPosition(Forge.getScreenWidth() / 2f - label.getWidth() / 2f, Forge.getScreenHeight() / 2f - label.getHeight() / 2f);
    }

    void render(float delta) {
        if (!render)
            return;
        //TODO: Add detection check to be used on other needed scenes..
        if (Forge.currentScene instanceof TileMapScene) {
            update("");
            return;
        }
        // render only on GameScenes
        if (Forge.currentScene instanceof GameScene) {
            alpha = Math.min(alpha + delta * 0.75f, 1f);
            float oldAlpha = Forge.getGraphics().getfloatAlphaComposite();
            Forge.getGraphics().setAlphaComposite(alpha);
            Forge.getGraphics().begin(Forge.getScreenWidth(), Forge.getScreenHeight());
            Forge.getGraphics().getBatch().draw(Forge.getAssets().getBackropTexture(), 0, 0, Forge.getScreenWidth(), Forge.getScreenHeight());
            Forge.getGraphics().setAlphaComposite(oldAlpha);
            label.draw(Forge.getGraphics().getBatch(), 1f);
            label.act(delta);
            Forge.getGraphics().end();
        }
    }

}