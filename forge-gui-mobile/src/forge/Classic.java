package forge;

import forge.gui.error.BugReporter;
import forge.screens.match.arena.ArenaCurtain;
import forge.toolbox.FContainer;
import forge.toolbox.FOverlay;

public class Classic {
    private static Classic instance;
    private Classic() {
    }

    public static Classic getInstance() {
        return instance == null ? instance = new Classic() : instance;
    }

    void render(FContainer screen) {
        if (screen == null) // shouldn't be null
            return;
        try {
            Forge.getGraphics().setProjectionMatrix(Forge.camera.combined);
            Forge.getGraphics().begin(Forge.getScreenWidth(), Forge.getScreenHeight());
            drawScene(screen);
            //update here
            if (Forge.needsUpdate) {
                if (Forge.getAssets().manager().update(16))
                    Forge.needsUpdate = false;
            }
            //Arena look: a picture of how things looked a moment ago, fading off over the new frame
            ArenaCurtain.draw(Forge.getGraphics());
            //sample batch
            FrameRate.getInstance().sampleClassic(Forge.showFPS);
            Forge.getGraphics().end();
        } catch (Exception e) {
            //check if sentry is enabled, if not it will call the gui interface but here we end the graphics so we only send it via sentry.
            if (BugReporter.isSentryEnabled())
                BugReporter.reportException(e);
            else
                e.printStackTrace();
            Forge.getGraphics().end();
        }
    }

    /**
     * Draws a whole frame of the given screen and its overlays (and, if asked, the Arena curtain) into whatever is
     * bound, typically an offscreen framebuffer for a picture of the window. Must not be called while a frame is being
     * drawn.
     */
    public void renderScene(FContainer screen, boolean withCurtain) {
        if (screen == null)
            return;
        Graphics g = Forge.getGraphics();
        try {
            g.setProjectionMatrix(Forge.camera.combined);
            g.begin(Forge.getScreenWidth(), Forge.getScreenHeight());
            drawScene(screen);
            if (withCurtain)
                ArenaCurtain.draw(g);
        } finally {
            g.end();
        }
    }

    private void drawScene(FContainer screen) {
        screen.screenPos.setSize(Forge.getScreenWidth(), Forge.getScreenHeight());
        if (screen.getRotate180()) {
            Forge.getGraphics().startRotateTransform(Forge.getScreenWidth() / 2f, Forge.getScreenHeight() / 2f, 180);
        }
        screen.draw(Forge.getGraphics());
        if (screen.getRotate180()) {
            Forge.getGraphics().endTransform();
        }
        for (FOverlay overlay : FOverlay.getOverlays()) {
            if (overlay.isVisibleOnScreen(Forge.getCurrentScreen())) {
                overlay.screenPos.setSize(Forge.getScreenWidth(), Forge.getScreenHeight());
                overlay.setSize(Forge.getScreenWidth(), Forge.getScreenHeight()); //update overlay sizes as they're rendered
                if (overlay.getRotate180()) {
                    Forge.getGraphics().startRotateTransform(Forge.getScreenWidth() / 2f, Forge.getScreenHeight() / 2f, 180);
                }
                overlay.draw(Forge.getGraphics());
                if (overlay.getRotate180()) {
                    Forge.getGraphics().endTransform();
                }
            }
        }
    }
}
