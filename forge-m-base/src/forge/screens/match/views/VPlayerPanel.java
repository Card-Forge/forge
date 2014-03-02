package forge.screens.match.views;

import com.badlogic.gdx.graphics.Color;

import forge.Forge.Graphics;
import forge.game.player.RegisteredPlayer;
import forge.toolbox.FContainer;

public class VPlayerPanel extends FContainer {
    private final RegisteredPlayer player;
    private final VPhases phases;
    private final VField field;
    private final VAvatar avatar;

    private boolean flipped;

    public VPlayerPanel(RegisteredPlayer player0) {
        player = player0;
        phases = new VPhases();
        field = new VField();
        avatar = new VAvatar();
    }

    public boolean isFlipped() {
        return flipped;
    }
    public void setFlipped(boolean flipped0) {
        flipped = flipped0;
    }

    @Override
    protected void doLayout(float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void drawBackground(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(flipped ? Color.LIGHT_GRAY : Color.GRAY, 0, 0, w, h);
    }
}
