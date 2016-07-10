package forge.screens.quest;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.math.Vector2;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.FDeckEditor;
import forge.deck.FDeckEditor.EditorType;
import forge.limited.BoosterDraft;
import forge.model.FModel;
import forge.quest.IQuestTournamentView;
import forge.quest.QuestEventDraft;
import forge.quest.QuestTournamentController;
import forge.quest.QuestDraftUtils.Mode;
import forge.quest.data.QuestEventDraftContainer;
import forge.screens.limited.DraftingProcessScreen;
import forge.toolbox.FLabel;
import forge.util.Utils;

public class QuestTournamentsScreen extends QuestLaunchScreen implements IQuestTournamentView {
    private final FLabel lblCredits = add(new FLabel.Builder().icon(FSkinImage.QUEST_COINSTACK)
            .iconScaleFactor(0.75f).font(FSkinFont.get(16)).build());

    private final FLabel lblTokens = add(new FLabel.Builder()
            .align(HAlignment.RIGHT).font(FSkinFont.get(16)).build());

    private final FLabel lblInfo = add(new FLabel.Builder().text("Select a tournament to join:")
            .align(HAlignment.CENTER).font(FSkinFont.get(16)).build());

    private final FLabel lblNoTournaments = add(new FLabel.Builder()
            .align(HAlignment.CENTER).text("There are no tournaments available at this time.").insets(Vector2.Zero)
            .font(FSkinFont.get(12)).build());

    private final QuestEventPanel.Container pnlTournaments = add(new QuestEventPanel.Container());

    private final FLabel btnLeaveTournament = add(new FLabel.ButtonBuilder().text("Leave Tournament").build());
    private final FLabel btnSpendToken = add(new FLabel.ButtonBuilder().text("Spend Token").build());

    private static final FSkinFont RESULTS_FONT = FSkinFont.get(15);
    private static final Vector2 RESULTS_INSETS = new Vector2(2 * PADDING, 0);
    private final FLabel lblFirst = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();
    private final FLabel lblSecond = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();
    private final FLabel lblThird = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();
    private final FLabel lblFourth = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();

    private final QuestTournamentController controller;
    private Mode mode = Mode.SELECT_TOURNAMENT;

    public QuestTournamentsScreen() {
        super();
        controller = new QuestTournamentController(this);
        btnLeaveTournament.setVisible(false);
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float gap = Utils.scale(2);
        float y = startY + gap; //move credits label down a couple pixels so it looks better

        float halfWidth = width / 2;
        lblCredits.setBounds(0, y, halfWidth, lblCredits.getAutoSizeBounds().height);
        lblTokens.setBounds(halfWidth, y, halfWidth - gap, lblCredits.getHeight());
        y += lblCredits.getHeight() + gap;

        TextBounds buttonBounds = btnSpendToken.getAutoSizeBounds();
        float buttonWidth = buttonBounds.width + 2 * gap;
        float x = width - gap - buttonWidth;
        btnSpendToken.setBounds(x, y, buttonWidth, buttonBounds.height + 2 * gap);
        btnLeaveTournament.setBounds(gap, y, x - 3 * gap, btnSpendToken.getHeight());
        y += btnSpendToken.getHeight() + gap;

        x = PADDING;
        float w = width - 2 * PADDING;
        lblInfo.setBounds(x, y, w, lblInfo.getAutoSizeBounds().height);
        y += lblInfo.getHeight() + gap;
        lblNoTournaments.setBounds(x, y, w, lblNoTournaments.getAutoSizeBounds().height);
        pnlTournaments.setBounds(x, y, w, height - y);
    }

    @Override
    protected String getGameType() {
        return "Tournaments";
    }

    @Override
    public void onUpdate() {
        controller.update();
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public void setMode(Mode mode0) {
        mode = mode0;
    }

    @Override
    public void populate() {
        //not needed
    }

    @Override
    public void updateEventList(QuestEventDraftContainer events) {
        pnlTournaments.clear();

        if (events != null) {
            for (QuestEventDraft event : events) {
                pnlTournaments.add(new QuestEventPanel(event, pnlTournaments));
            }
        }

        pnlTournaments.revalidate();

        boolean hasTournaments = pnlTournaments.getChildCount() > 0;
        pnlTournaments.setVisible(hasTournaments);
        lblNoTournaments.setVisible(!hasTournaments);
    }

    @Override
    public void updateTournamentBoxLabel(String playerID, int iconID, int box, boolean first) {

    }

    @Override
    public void startDraft(BoosterDraft draft) {
        Forge.openScreen(new DraftingProcessScreen(draft));
    }

    @Override
    public void editDeck(boolean isExistingDeck) {
        DeckGroup deckGroup = FModel.getQuest().getDraftDecks().get(QuestEventDraft.DECK_NAME);
        if (deckGroup != null) {
            Deck deck = deckGroup.getHumanDeck();
            if (deck != null) {
                Forge.openScreen(new FDeckEditor(EditorType.QuestDraft, deck, isExistingDeck));
            }
        }
    }

    @Override
    public FLabel getLblCredits() {
        return lblCredits;
    }

    @Override
    public FLabel getLblFirst() {
        return lblFirst;
    }

    @Override
    public FLabel getLblSecond() {
        return lblSecond;
    }

    @Override
    public FLabel getLblThird() {
        return lblThird;
    }

    @Override
    public FLabel getLblFourth() {
        return lblFourth;
    }

    @Override
    public FLabel getLblTokens() {
        return lblTokens;
    }

    @Override
    public FLabel getBtnSpendToken() {
        return btnSpendToken;
    }

    @Override
    public FLabel getBtnLeaveTournament() {
        return btnLeaveTournament;
    }
}
