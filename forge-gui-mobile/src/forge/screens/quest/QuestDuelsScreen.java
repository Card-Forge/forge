package forge.screens.quest;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.interfaces.IButton;
import forge.interfaces.ICheckBox;
import forge.interfaces.IComboBox;
import forge.model.FModel;
import forge.quest.QuestUtil;
import forge.screens.LaunchScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;

public class QuestDuelsScreen extends LaunchScreen {
    private final FComboBox<String> cbxPet = add(new FComboBox<String>());
    private final FCheckBox cbCharm = add(new FCheckBox("Use Charm of Vigor"));
    private final FCheckBox cbPlant = add(new FCheckBox("Summon Plant"));
    private final FLabel lblZep = add(new FLabel.Builder().text("Launch Zeppelin").font(FSkinFont.get(14)).build());

    private final FLabel lblInfo = add(new FLabel.Builder().text("Select your next duel.")
            .font(FSkinFont.get(16)).build());

    private final FLabel lblCurrentDeck = add(new FLabel.Builder()
        .text("Current deck hasn't been set yet.")
        .font(FSkinFont.get(12)).build());

    private final FLabel lblNextChallengeInWins = add(new FLabel.Builder()
        .text("Next challenge in wins hasn't been set yet.")
        .font(FSkinFont.get(12)).build());

    private final FLabel btnRandomOpponent = add(new FLabel.ButtonBuilder().text("Random Duel").font(FSkinFont.get(16)).build());

    public QuestDuelsScreen() {
        super("Quest Duels", QuestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        QuestUtil.updateQuestView(QuestMenu.getMenu());
        setHeaderCaption(FModel.getQuest().getName() + " - Duels\n(" + FModel.getQuest().getRank() + ")");
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        // TODO Auto-generated method stub
        return false;
    }

    public IButton getBtnRandomOpponent() {
        return btnRandomOpponent;
    }

    public IButton getLblNextChallengeInWins() {
        return lblNextChallengeInWins;
    }

    public IButton getLblCurrentDeck() {
        return lblCurrentDeck;
    }

    public IComboBox<String> getCbxPet() {
        return cbxPet;
    }

    public ICheckBox getCbPlant() {
        return cbPlant;
    }

    public ICheckBox getCbCharm() {
        return cbCharm;
    }

    public IButton getLblZep() {
        return lblZep;
    }
}
