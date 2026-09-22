package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import forge.adventure.scene.HudScene;
import forge.util.ScreenUtil;

public class Adventure {
    public static Adventure instance;
    private float transitionTimeout;
    boolean sceneWasSwapped;
    public boolean renderTransitionScreen = true;

    private Adventure() {
        sceneWasSwapped = false;
    }

    public static Adventure getInstance() {
        return instance == null ? instance = new Adventure() : instance;
    }

    void render(float delta) {
        try {
            if (renderTransitionScreen) {
                Forge.getGraphics().getBatch().setProjectionMatrix(Forge.camera.combined);
                // Transition Overlay
                float transitionTime = 0.12f;
                if (sceneWasSwapped) {
                    sceneWasSwapped = false;
                    transitionTimeout = transitionTime;
                    clearScreen();
                    return;
                }
                if (transitionTimeout >= 0) {
                    clearScreen();
                    Forge.getGraphics().getBatch().begin();
                    transitionTimeout -= delta;
                    Forge.getGraphics().getBatch().setColor(1, 1, 1, 1);
                    Forge.getGraphics().getBatch().draw(ScreenUtil.getInstance().getLastScreenTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    Forge.getGraphics().getBatch().setColor(1, 1, 1, 1 - (1 / transitionTime) * transitionTimeout);
                    Forge.getGraphics().getBatch().draw(Forge.getAssets().fallback_skins().get("transition"), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    FrameRate.getInstance().sampleAdventure(Forge.getGraphics().getBatch(), Forge.showFPS);
                    Forge.getGraphics().getBatch().end();
                    if (transitionTimeout < 0) {
                        Forge.currentScene.render();
                        Forge.storeScreen();
                        clearScreen();
                    } else {
                        return;
                    }
                }
                if (transitionTimeout >= -transitionTime) {
                    clearScreen();
                    Forge.getGraphics().getBatch().begin();
                    transitionTimeout -= delta;
                    Forge.getGraphics().getBatch().setColor(1, 1, 1, 1);
                    Forge.getGraphics().getBatch().draw(ScreenUtil.getInstance().getLastScreenTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    Forge.getGraphics().getBatch().setColor(1, 1, 1, (1 / transitionTime) * (transitionTimeout + transitionTime));
                    Forge.getGraphics().getBatch().draw(Forge.getAssets().fallback_skins().get("transition"), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    FrameRate.getInstance().sampleAdventure(Forge.getGraphics().getBatch(), Forge.showFPS);
                    Forge.getGraphics().getBatch().end();
                    return;
                }
            }
            // Adventure UIScene
            Forge.currentScene.render();
            Forge.currentScene.act(delta);
            if (Forge.currentScene instanceof HudScene hudScene)
                FrameRate.getInstance().sampleAdventure(hudScene.getBatch(), Forge.showFPS);
        } catch (IllegalStateException | NullPointerException ie) {
            //silence this..
            //TODO: Don't silence this.
        }
    }

    void clearScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }
}
