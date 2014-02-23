package forge.screens.guantlet;

import forge.FScreen;
import forge.game.Match;
import forge.toolbox.StartButton;

public class GuantletScreen extends FScreen {
	private final StartButton btnStart;

    public GuantletScreen() {
    	super(true, "Guantlet", true);
    	btnStart = add(new StartButton() {
			@Override
			public Match createMatch() {
				return null; //TODO: Start match
			}
    	});
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
    	height = btnStart.updateLayout(width, height); //update height to exclude area taken up by StartButton
    }
}
