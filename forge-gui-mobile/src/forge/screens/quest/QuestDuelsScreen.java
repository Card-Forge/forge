package forge.screens.quest;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;
import forge.assets.FSkinFont;
import forge.interfaces.IButton;
import forge.model.FModel;
import forge.quest.QuestEventDuel;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;

import java.util.List;

public class QuestDuelsScreen extends QuestLaunchScreen {

    private final FLabel lblInfo = add(new FLabel.Builder().text("Select your next duel.")
            .align(HAlignment.CENTER).font(FSkinFont.get(16)).build());

    private final FLabel lblCurrentDeck = add(new FLabel.Builder()
        .text("Current deck hasn't been set yet.").align(HAlignment.CENTER).insets(Vector2.Zero)
        .font(FSkinFont.get(12)).build());

    private final FLabel lblNextChallengeInWins = add(new FLabel.Builder()
        .text("Next challenge in wins hasn't been set yet.").align(HAlignment.CENTER).insets(Vector2.Zero)
        .font(FSkinFont.get(12)).build());

    private final QuestEventPanel.Container pnlDuels = add(new QuestEventPanel.Container());

    public QuestDuelsScreen() {
        super();
        pnlDuels.setActivateHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                startMatch();
            }
        });
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
        pnlDuels.setBounds(x, y, w, height - y + PADDING * 2);
    }

    public IButton getLblNextChallengeInWins() {
        return lblNextChallengeInWins;
    }

    public IButton getLblCurrentDeck() {
        return lblCurrentDeck;
    }

    @Override
    protected String getGameType() {
        return "Duels";
    }

    @Override
    public void onUpdate() {

        pnlDuels.clear();

        List<QuestEventDuel> duels = FModel.getQuest().getDuelsManager().generateDuels();

        if (duels != null) {
            for (QuestEventDuel duel : duels) {
                pnlDuels.add(new QuestEventPanel(duel, pnlDuels));
            }
        }

        pnlDuels.revalidate();

    }
}
