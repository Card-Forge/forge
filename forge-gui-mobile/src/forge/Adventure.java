package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import forge.adventure.scene.HudScene;
import forge.gui.error.BugReporter;
import forge.util.ScreenUtil;

public class Adventure implements Disposable {
    public static Adventure instance;
    private float transitionTimeout;
    boolean sceneWasSwapped;
    private SpriteBatch transitionBatch, uiBatch;
    public boolean renderTransitionScreen = true;
    private boolean isDisposed = false;

    private Adventure() {
        sceneWasSwapped = false;
        transitionBatch = new SpriteBatch(Forge.LOW_SPRITES_CAP);
        // adventureBatch is used on UIScene so every scene passed will use this shared batch
        // instead of creating new SpriteBatch each with default 1000 capacity (14 scenes currently)
        uiBatch = new SpriteBatch(Forge.HIGH_SPRITES_CAP);
    }

    public SpriteBatch getUiBatch() {
        return uiBatch;
    }

    public static Adventure getInstance() {
        return instance == null ? instance = new Adventure() : instance;
    }

    void render(float delta) {
        try {
            if (renderTransitionScreen) {
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
                    transitionBatch.begin();
                    transitionTimeout -= delta;
                    transitionBatch.setColor(1, 1, 1, 1);
                    transitionBatch.draw(ScreenUtil.getInstance().getLastScreenTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    transitionBatch.setColor(1, 1, 1, 1 - (1 / transitionTime) * transitionTimeout);
                    transitionBatch.draw(Forge.getAssets().fallback_skins().get("transition"), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    FrameRate.getInstance().sampleAdventure(transitionBatch, Forge.showFPS);
                    transitionBatch.end();
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
                    transitionBatch.begin();
                    transitionTimeout -= delta;
                    transitionBatch.setColor(1, 1, 1, 1);
                    transitionBatch.draw(ScreenUtil.getInstance().getLastScreenTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    transitionBatch.setColor(1, 1, 1, (1 / transitionTime) * (transitionTimeout + transitionTime));
                    transitionBatch.draw(Forge.getAssets().fallback_skins().get("transition"), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    FrameRate.getInstance().sampleAdventure(transitionBatch, Forge.showFPS);
                    transitionBatch.end();
                    return;
                }
            }
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
    @Override
    public void dispose() {
        if (isDisposed) {
            return;
        }
        isDisposed = true;
        Forge.safeDispose(transitionBatch, uiBatch);
    }
}
