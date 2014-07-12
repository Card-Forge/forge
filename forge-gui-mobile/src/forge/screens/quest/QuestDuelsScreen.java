package forge.screens.quest;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.math.Vector2;

import forge.assets.FSkinFont;
import forge.interfaces.IButton;
import forge.interfaces.ICheckBox;
import forge.interfaces.IComboBox;
import forge.model.FModel;
import forge.quest.QuestEventDuel;
import forge.quest.QuestUtil;
import forge.screens.LaunchScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;

public class QuestDuelsScreen extends LaunchScreen {
    private static final float PADDING = FOptionPane.PADDING;

    private final FComboBox<String> cbxPet = add(new FComboBox<String>());
    private final FCheckBox cbCharm = add(new FCheckBox("Use Charm of Vigor"));
    private final FCheckBox cbPlant = add(new FCheckBox("Summon Plant"));
    private final FLabel lblZep = add(new FLabel.Builder().text("Launch Zeppelin").font(FSkinFont.get(14)).build());

    private final FLabel lblInfo = add(new FLabel.Builder().text("Select your next duel.")
            .align(HAlignment.CENTER).font(FSkinFont.get(16)).build());

    private final FLabel lblCurrentDeck = add(new FLabel.Builder()
        .text("Current deck hasn't been set yet.").align(HAlignment.CENTER).insets(Vector2.Zero)
        .font(FSkinFont.get(12)).build());

    private final FLabel lblNextChallengeInWins = add(new FLabel.Builder()
        .text("Next challenge in wins hasn't been set yet.").align(HAlignment.CENTER).insets(Vector2.Zero)
        .font(FSkinFont.get(12)).build());

    private final QuestEventPanel.Container pnlDuels = add(new QuestEventPanel.Container());

    private final FLabel btnRandomOpponent = add(new FLabel.ButtonBuilder().text("Random Duel").font(FSkinFont.get(16)).build());

    public QuestDuelsScreen() {
        super("Quest Duels", QuestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        QuestUtil.updateQuestView(QuestMenu.getMenu());
        setHeaderCaption(FModel.getQuest().getName() + " - Duels\n(" + FModel.getQuest().getRank() + ")");
        if (pnlDuels.getChildCount() == 0) {
            update(); //update if duels list hasn't been populated yet
        }
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
        pnlDuels.setBounds(x, y, w, height - y);
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
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

    public void update() {
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
