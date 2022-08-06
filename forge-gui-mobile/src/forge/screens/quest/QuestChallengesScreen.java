package forge.screens.quest;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.assets.FSkinFont;
import forge.gui.interfaces.IButton;
import forge.model.FModel;
import forge.toolbox.FLabel;

public class QuestChallengesScreen extends QuestLaunchScreen {
    private final FLabel lblInfo = add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblWhichChallenge"))
            .align(Align.center).font(FSkinFont.get(16)).build());

    private final FLabel lblCurrentDeck = add(new FLabel.Builder()
        .text(Forge.getLocalizer().getMessage("lblNoDuelDeck")).align(Align.center).insets(Vector2.Zero)
        .font(FSkinFont.get(12)).build());

    private final FLabel lblNextChallengeInWins = add(new FLabel.Builder()
        .text(Forge.getLocalizer().getMessage("lblNextChallengeNotYet")).align(Align.center).insets(Vector2.Zero)
        .font(FSkinFont.get(12)).build());

    private final QuestEventPanel.Container pnlChallenges = add(new QuestEventPanel.Container());

    public QuestChallengesScreen() {
        super();
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING / 2;
        float w = width - 2 * PADDING;
        lblInfo.setBounds(x, y, w, lblInfo.getAutoSizeBounds().height);
        y += lblInfo.getHeight();
        lblCurrentDeck.setBounds(x, y, w, lblCurrentDeck.getAutoSizeBounds().height);
        y += lblCurrentDeck.getHeight();
        lblNextChallengeInWins.setBounds(x, y, w, lblCurrentDeck.getHeight());
        y += lblCurrentDeck.getHeight() + PADDING / 2;
        pnlChallenges.setBounds(x, y, w, height - y);
    }

    @Override
    protected String getGameType() {
        return "Challenges";
    }

    public IButton getLblNextChallengeInWins() {
        return lblNextChallengeInWins;
    }

    public IButton getLblCurrentDeck() {
        return lblCurrentDeck;
    }

    @Override
    public void onUpdate() {
        pnlChallenges.clear();

        FModel.getQuest().regenerateChallenges();
        for (Object id : FModel.getQuest().getAchievements().getCurrentChallenges()) {
            pnlChallenges.add(new QuestEventPanel(FModel.getQuest().getChallenges().get(id.toString()), pnlChallenges));
        }

        pnlChallenges.revalidate();
    }
}
