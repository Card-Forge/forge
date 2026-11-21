package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import forge.adventure.scene.HudScene;
import forge.gui.error.BugReporter;
//import forge.util.ScreenUtil;

public class Adventure {
    public static Adventure instance;
    //private float transitionTimeout;
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
            // TODO: Use better transistion. Fixes Android slow screen updates on switching scenes.
            /*if (renderTransitionScreen) {
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
            }*/
            // Adventure UIScene
            Forge.currentScene.render();
            Forge.currentScene.act(delta);
            if (Forge.currentScene instanceof HudScene hudScene)
                FrameRate.getInstance().sampleAdventure(hudScene.getBatch(), Forge.showFPS);
        } catch (IllegalStateException | NullPointerException ie) {
            // Swallowed rather than propagated so a bad frame doesn't take the whole app down, but
            // not silently: this is the failure users describe as "fades to black and returns to
            // the title with no error" (#11555), and scene input runs from act(), so a scene that
            // can't be entered dies here without a trace. Once per distinct failure, since a
            // broken scene throws every frame.
            logOnce(ie);
        }
    }

    private String lastLoggedFailure;

    private void logOnce(RuntimeException ex) {
        StackTraceElement[] trace = ex.getStackTrace();
        String key = ex.getClass().getName() + ": " + ex.getMessage() + (trace.length > 0 ? " @ " + trace[0] : "");
        if (key.equals(lastLoggedFailure))
            return;
        lastLoggedFailure = key;
        System.err.println("Adventure scene " + (Forge.currentScene == null ? "null" : Forge.currentScene.getClass().getSimpleName()) + " failed:");
        // With Sentry on, reportException prints the trace itself and sends it; without, it would
        // open the crash dialog mid-frame, so just print.
        if (BugReporter.isSentryEnabled())
            BugReporter.reportException(ex);
        else
            ex.printStackTrace();
    }

    void clearScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }
}
