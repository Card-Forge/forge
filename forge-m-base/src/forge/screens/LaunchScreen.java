package forge.screens;

import forge.Forge;
import forge.screens.FScreen;
import forge.screens.match.MatchScreen;
import forge.Forge.Graphics;
import forge.assets.FSkinImage;
import forge.game.Match;
import forge.toolbox.FDisplayObject;

public abstract class LaunchScreen extends FScreen {
	private final StartButton btnStart;

    public LaunchScreen(String headerCaption) {
    	super(true, headerCaption, true);
    	btnStart = add(new StartButton());
    }

    public abstract Match createMatch();

    @Override
    protected final void doLayout(float startY, float width, float height) {
    	float imageWidth = FSkinImage.BTN_START_UP.getSourceWidth();
    	float imageHeight = FSkinImage.BTN_START_UP.getSourceHeight();
    	float padding = imageHeight * 0.1f;

    	btnStart.setBounds((width - imageWidth) / 2, height - imageHeight - padding, imageWidth, imageHeight);

    	doLayoutAboveBtnStart(startY, width, height - imageHeight - 2 * padding);
    }

    protected abstract void doLayoutAboveBtnStart(float startY, float width, float height);

    private class StartButton extends FDisplayObject {
    	private boolean pressed;
    	private boolean creatingMatch;

        /**
         * Instantiates a new FButton.
         */
        public StartButton() {
        }

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

        @Override
        public void draw(Graphics g) {
        	g.drawImage(pressed ? FSkinImage.BTN_START_DOWN : FSkinImage.BTN_START_UP,
        			0, 0, getWidth(), getHeight());
        }
    }
}
