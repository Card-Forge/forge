package forge.toolbox;

import forge.Forge;
import forge.Forge.Graphics;
import forge.assets.FSkinImage;
import forge.game.Match;
import forge.screens.match.MatchScreen;

public abstract class StartButton extends FDisplayObject {
	private boolean pressed;
	private boolean creatingMatch;

    /**
     * Instantiates a new FButton.
     */
    public StartButton() {
    }

    public abstract Match createMatch();

    @Override
    public final boolean touchDown(float x, float y) {
        pressed = true;
        return true;
    }

    @Override
    public final boolean touchUp(float x, float y) {
    	pressed = false;
        return true;
    }

    @Override
    public final boolean tap(float x, float y, int count) {
        if (count == 1 && !creatingMatch) {
        	creatingMatch = true; //ensure user doesn't create multiple matches by tapping multiple times
        	Match match = createMatch();
        	if (match != null) {
        		Forge.openScreen(new MatchScreen(match));
        	}
        }
        return true;
    }

    public float updateLayout(float parentWidth, float parentHeight) {
    	float width = FSkinImage.BTN_START_UP.getSourceWidth();
    	float height = FSkinImage.BTN_START_UP.getSourceHeight();
    	float padding = height * 0.1f;
    	setBounds((parentWidth - width) / 2, parentHeight - height - padding, width, height);
    	return parentHeight - height - 2 * padding; //indicate to caller how much space is taken up by StartButton
    }

    @Override
    public void draw(Graphics g) {
    	g.drawImage(pressed ? FSkinImage.BTN_START_DOWN : FSkinImage.BTN_START_UP,
    			0, 0, getWidth(), getHeight());
    }
}
