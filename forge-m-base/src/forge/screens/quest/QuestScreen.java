package forge.screens.quest;

import forge.FScreen;
import forge.game.Match;
import forge.toolbox.StartButton;

public class QuestScreen extends FScreen {
	private final StartButton btnStart;

    public QuestScreen() {
    	super(true, "Quest", true);
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
