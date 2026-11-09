package forge.screens.match.views;

import forge.Graphics;
import forge.assets.FSkinTexture;
import forge.screens.match.CardFlightOverlay;
import forge.screens.match.MatchScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;

/**
 * Root of the secondary (bottom) display of a dual-screen device during a match:
 * the local player's panel (battlefield, hand and zone tabs) above the prompt buttons.
 */
public class VBottomScreen extends FContainer {
    private final VPlayerPanel playerPanel;
    private final VPrompt prompt;

    public VBottomScreen(VPlayerPanel playerPanel0, VPrompt prompt0) {
        playerPanel = add(playerPanel0);
        prompt = add(prompt0);
    }

    public VPlayerPanel getPlayerPanel() {
        return playerPanel;
    }

    public VPrompt getPrompt() {
        return prompt;
    }

    /** True if the object is drawn somewhere inside this screen. */
    public boolean isAncestorOf(FDisplayObject object) {
        return object != null && contains(this, object);
    }

    private static boolean contains(FContainer container, FDisplayObject object) {
        for (FDisplayObject child : container.getChildren()) {
            if (child == object || (child instanceof FContainer && contains((FContainer) child, object))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doLayout(float width, float height) {
        float promptHeight = VPrompt.HEIGHT;
        playerPanel.setBounds(0, 0, width, height - promptHeight);
        prompt.setBounds(0, height - promptHeight, width, promptHeight);
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth(), h = getHeight();
        FSkinTexture bg = FSkinTexture.BG_MATCH;
        float bgW = h * bg.getWidth() / bg.getHeight();
        float bgH = h;
        if (bgW < w) { //cover the whole screen
            bgH = w * bgH / bgW;
            bgW = w;
        }
        g.drawImage(bg, (w - bgW) / 2, (h - bgH) / 2, bgW, bgH);
    }

    @Override
    protected void drawOverlay(Graphics g) {
        float y = playerPanel.getBottom() - 0.5f;
        g.drawLine(1, MatchScreen.getBorderColor(), 0, y, getWidth(), y);
        //animations of the local player's cards happen here; the top screen draws everyone else's
        CardFlightOverlay.draw(g, playerPanel.getPlayer(), getHeight(), playerPanel.getPlayer(), false);
    }
}
