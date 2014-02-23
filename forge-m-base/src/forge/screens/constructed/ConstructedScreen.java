package forge.screens.constructed;

import forge.FScreen;
import forge.toolbox.StartButton;

public class ConstructedScreen extends FScreen {
	private final StartButton btnStart;

    public ConstructedScreen() {
    	super(true, "Constructed", true);
    	btnStart = add(new StartButton() {
			@Override
			public void start() {
				//TODO: Start match
			}
    	});
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
    	height = btnStart.updateLayout(width, height); //update height to exclude area taken up by StartButton
    }
}
