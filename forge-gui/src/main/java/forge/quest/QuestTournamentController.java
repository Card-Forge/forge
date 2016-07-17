package forge.quest;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import forge.GuiBase;
import forge.assets.FSkinProp;
import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.interfaces.IGuiGame;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.limited.BoosterDraft;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestDraftUtils.Mode;
import forge.quest.QuestEventDraft.QuestDraftFormat;
import forge.quest.data.QuestAchievements;
import forge.tournament.system.TournamentBracket;
import forge.tournament.system.TournamentPairing;
import forge.tournament.system.TournamentPlayer;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;
import forge.util.storage.IStorage;

public class QuestTournamentController {
    private final IQuestTournamentView view;
    private boolean drafting = false;
    private IGuiGame gui = null;

    public QuestTournamentController(IQuestTournamentView view0) {
        view = view0;

        final QuestAchievements achievements = FModel.getQuest().getAchievements();
        final IStorage<DeckGroup> decks = FModel.getQuest().getDraftDecks();

        if (achievements == null) {
            view.setMode(Mode.EMPTY);
        }
        else if (achievements.getDraftEvents() == null || achievements.getDraftEvents().isEmpty()) {
            achievements.generateDrafts();

            if (achievements.getDraftEvents().isEmpty()) {
                view.setMode(Mode.EMPTY);
            }
            else {
                view.setMode(Mode.SELECT_TOURNAMENT);
            }
        }
        else if (decks == null || !decks.contains(QuestEventDraft.DECK_NAME)) {
            achievements.generateDrafts();
            view.setMode(Mode.SELECT_TOURNAMENT);
        }
        else if (!achievements.getCurrentDraft().isStarted()) {
            view.setMode(Mode.PREPARE_DECK);
        }
        else {
            view.setMode(Mode.TOURNAMENT_ACTIVE);
        }
    }

    public void endTournamentAndAwardPrizes() {
        final QuestEventDraft draft = FModel.getQuest().getAchievements().getCurrentDraft();

        if (!draft.isStarted()) {
            final boolean shouldQuit = SOptionPane.showOptionDialog("If you leave now, this tournament will be forever gone."
                    + "\nYou will keep the cards you drafted, but will receive no other prizes."
                    + "\n\nWould you still like to quit the tournament?", "Really Quit?", SOptionPane.WARNING_ICON, ImmutableList.of("Yes", "No"), 1) == 0;
            if (!shouldQuit) {
                return;
            }
        }
        else {
            if (draft.playerHasMatchesLeft()) {
                final boolean shouldQuit = SOptionPane.showOptionDialog("You have matches left to play!\nLeaving the tournament early will forfeit your potential future winnings."
                        + "\nYou will still receive winnings as if you conceded your next match and you will keep the cards you drafted."
                        + "\n\nWould you still like to quit the tournament?", "Really Quit?", SOptionPane.WARNING_ICON, ImmutableList.of("Yes", "No"), 1) == 0;
                if (!shouldQuit) {
                    return;
                }
            }

            final String placement = draft.getPlacementString();

            final QuestEventDraft.QuestDraftPrizes prizes = draft.collectPrizes();

            if (prizes.hasCredits()) {
                SOptionPane.showMessageDialog("For placing " + placement + ", you have been awarded " + QuestUtil.formatCredits(prizes.credits) + " credits!", "Credits Awarded", FSkinProp.ICO_QUEST_GOLD);
            }

            if (prizes.hasIndividualCards()) {
                GuiBase.getInterface().showCardList("Tournament Reward", "For participating in the tournament, you have been awarded the following promotional card:", prizes.individualCards);
            }

            if (prizes.hasBoosterPacks()) {
                final String packPlural = (prizes.boosterPacks.size() == 1) ? "" : "s";

                SOptionPane.showMessageDialog("For placing " + placement + ", you have been awarded " + prizes.boosterPacks.size() + " booster pack" + packPlural + "!", "Booster Pack" + packPlural + " Awarded", FSkinProp.ICO_QUEST_BOX);

                if (FModel.getPreferences().getPrefBoolean(FPref.UI_OPEN_PACKS_INDIV) && prizes.boosterPacks.size() > 1) {
                    boolean skipTheRest = false;
                    final List<PaperCard> remainingCards = new ArrayList<>();
                    final int totalPacks = prizes.boosterPacks.size();
                    int currentPack = 0;

                    while (!prizes.boosterPacks.isEmpty()) {

                        final BoosterPack pack = prizes.boosterPacks.remove(0);
                        currentPack++;

                        if (skipTheRest) {
                            remainingCards.addAll(pack.getCards());
                            continue;
                        }

                        skipTheRest = GuiBase.getInterface().showBoxedProduct(pack.getName(), "You have found the following cards inside (Booster Pack " + currentPack + " of " + totalPacks + "):", pack.getCards());
                    }

                    if (skipTheRest && !remainingCards.isEmpty()) {
                        GuiBase.getInterface().showCardList("Tournament Reward", "You have found the following cards inside:", remainingCards);
                    }
                }
                else {
                    final List<PaperCard> cards = new ArrayList<>();

                    while (!prizes.boosterPacks.isEmpty()) {
                        final BoosterPack pack = prizes.boosterPacks.remove(0);
                        cards.addAll(pack.getCards());
                    }

                    GuiBase.getInterface().showCardList("Tournament Reward", "You have found the following cards inside:", cards);
                }
            }

            if (prizes.selectRareFromSets()) {
                SOptionPane.showMessageDialog("For placing " + placement  + ", you may select a rare or mythic rare card from the drafted block.", "Rare Awarded", FSkinProp.ICO_QUEST_STAKES);

                final PaperCard card = GuiBase.getInterface().chooseCard("Select a Card", "Select a card to keep:", prizes.selectRareCards);
                prizes.addSelectedCard(card);

                SOptionPane.showMessageDialog("'" + card.getName() + "' has been added to your collection!", "Card Added", FSkinProp.ICO_QUEST_STAKES);
            }

            if (draft.getPlayerPlacement() == 1) {
                SOptionPane.showMessageDialog("For placing " + placement + ", you have been awarded a token!\nUse tokens to create new drafts to play.", "Bonus Token", FSkinProp.ICO_QUEST_NOTES);
                FModel.getQuest().getAchievements().addDraftToken();
            }

        }

        final boolean saveDraft = SOptionPane.showOptionDialog("Would you like to save this draft to the regular draft mode?", "Save Draft?", SOptionPane.QUESTION_ICON, ImmutableList.of("Yes", "No"), 0) == 0;
        if (saveDraft) {
            draft.saveToRegularDraft();
        }
        draft.addToQuestDecks();

        update();
        view.populate();
    }

    public void spendToken() {
        final QuestAchievements achievements = FModel.getQuest().getAchievements();
        if (achievements != null) {

            List<QuestDraftFormat> formats = QuestEventDraft.getAvailableFormats(FModel.getQuest());

            if (formats.isEmpty()) {
                SOptionPane.showErrorDialog(
                        "You do not have any draft-able sets unlocked!\n" +
                        "Come back later when you've unlocked more sets.",
                        "No Available Drafts");
                return;
            }

            final QuestDraftFormat format = SGuiChoose.oneOrNone("Choose Draft Format", formats);
            if (format != null) {
                achievements.spendDraftToken(format);

                update();
                view.populate();
            }
        }
    }

    public void update() {
        if (FModel.getQuest().getAchievements() == null) {
            view.setMode(Mode.EMPTY);
            return;
        }

        final QuestAchievements achievements = FModel.getQuest().getAchievements();
        achievements.generateDrafts();

        if (FModel.getQuest().getAchievements().getDraftEvents().isEmpty()) {
            view.setMode(Mode.EMPTY);
            updatePlacementLabelsText();
            return;
        }

        if ((FModel.getQuest().getDraftDecks() == null
                || !FModel.getQuest().getDraftDecks().contains(QuestEventDraft.DECK_NAME)
                || FModel.getQuest().getAchievements().getCurrentDraftIndex() == -1)) {
            view.setMode(Mode.SELECT_TOURNAMENT);
        }
        else if (!FModel.getQuest().getAchievements().getCurrentDraft().isStarted()) {
            view.setMode(Mode.PREPARE_DECK);
        }
        else {
            view.setMode(Mode.TOURNAMENT_ACTIVE);
        }

        QuestDraftUtils.update(gui);

        switch (view.getMode()) {
        case SELECT_TOURNAMENT:
            updateSelectTournament();
            break;
        case PREPARE_DECK:
            updatePrepareDeck();
            break;
        case TOURNAMENT_ACTIVE:
            updateTournamentActive();
            break;
        default:
            break;
        }
    }

    private void updateSelectTournament() {
        view.getLblCredits().setText("Credits: " + QuestUtil.formatCredits(FModel.getQuest().getAssets().getCredits()));

        final QuestAchievements achievements = FModel.getQuest().getAchievements();
        achievements.generateDrafts();

        view.updateEventList(achievements.getDraftEvents());

        updatePlacementLabelsText();
    }

    private void updatePlacementLabelsText() {
        final QuestAchievements achievements = FModel.getQuest().getAchievements();

        if (view.getMode() == Mode.EMPTY) {
            view.updateEventList(null);
        }

        view.getLblFirst().setText("1st Place: " + achievements.getWinsForPlace(1) + " time" + (achievements.getWinsForPlace(1) == 1 ? "" : "s"));
        view.getLblSecond().setText("2nd Place: " + achievements.getWinsForPlace(2) + " time" + (achievements.getWinsForPlace(2) == 1 ? "" : "s"));
        view.getLblThird().setText("3rd Place: " + achievements.getWinsForPlace(3) + " time" + (achievements.getWinsForPlace(3) == 1 ? "" : "s"));
        view.getLblFourth().setText("4th Place: " + achievements.getWinsForPlace(4) + " time" + (achievements.getWinsForPlace(4) == 1 ? "" : "s"));

        view.getBtnSpendToken().setText("Spend Token (" + achievements.getDraftTokens() + ")");
        view.getBtnSpendToken().setEnabled(achievements.getDraftTokens() > 0);
    }

    private void updatePrepareDeck() {
    }

    private void updateTournamentActive() {
        if (FModel.getQuest().getAchievements().getCurrentDraft() == null) {
            return;
        }

        if (QuestDraftUtils.TOURNAMENT_TOGGLE) {
            updateTournamentActiveForBracket();
            return;
        }

        for (int i = 0; i < 15; i++) {
            String playerID = FModel.getQuest().getAchievements().getCurrentDraft().getStandings()[i];

            int iconID = 0;

            switch (playerID) {
            case QuestEventDraft.HUMAN:
                playerID = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
                if (FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",").length > 0) {
                    iconID = Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0]);
                }
                break;
            case QuestEventDraft.UNDETERMINED:
                playerID = "Undetermined";
                iconID = GuiBase.getInterface().getAvatarCount() - 1;
                break;
            default:
                iconID = FModel.getQuest().getAchievements().getCurrentDraft().getAIIcons()[Integer.parseInt(playerID) - 1];
                playerID = FModel.getQuest().getAchievements().getCurrentDraft().getAINames()[Integer.parseInt(playerID) - 1];
                break;
            }

            final boolean first = i % 2 == 0;
            final int box = i / 2;
            view.updateTournamentBoxLabel(playerID, iconID, box, first);
        }

        if (FModel.getQuest().getAchievements().getCurrentDraft().playerHasMatchesLeft()) {
            view.getBtnLeaveTournament().setText("Leave Tournament");
        }
        else {
            view.getBtnLeaveTournament().setText("Collect Prizes");
        }
    }

    private void updateTournamentActiveForBracket() {
        QuestEventDraft draft = FModel.getQuest().getAchievements().getCurrentDraft();
        TournamentBracket bracket = draft.getBracket();

        if (bracket == null) {
            return;
        }

        // Combine finished pairings with active round pairings
        List<TournamentPairing> allPairings = Lists.newArrayList();
        allPairings.addAll(bracket.getCompletedPairings());
        allPairings.addAll(bracket.getActivePairings());

        int count = 0;
        int playerCount = 0;
        int lastWinner = 0;
        for (TournamentPairing tp : allPairings) {
            boolean first = true;
            String playerID = "Undetermined";
            int iconID = 0;
            for(TournamentPlayer player : tp.getPairedPlayers()) {
                if (player.getIndex() == -1) {
                    playerID = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
                    if (FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",").length > 0) {
                        iconID = Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0]);
                    }
                }
                else{
                    playerID = player.getPlayer().getName();
                    iconID = player.getIndex();
                }
                view.updateTournamentBoxLabel(playerID, iconID, count, first);

                if (tp.getWinner() != null && tp.getWinner().equals(player)) {
                    // Temporarily fill in winner box
                    lastWinner = playerCount;
                    view.updateTournamentBoxLabel(player.getPlayer().getName(), player.getIndex(), playerCount/4 + 4, count%2 == 0);
                }
                first = false;
                playerCount++;
            }
            count++;
        }
        if (!bracket.isTournamentOver()) {
            for (int i = lastWinner/2+9 ; i < 15; i++) {
                String playerID = "Undetermined";
                int iconID = GuiBase.getInterface().getAvatarCount() - 1;
                view.updateTournamentBoxLabel(playerID, iconID,  i / 2, i%2 == 0);
            }
        }

        if (draft.playerHasMatchesLeft()) {
            view.getBtnLeaveTournament().setText("Leave Tournament");
        }
        else {
            view.getBtnLeaveTournament().setText("Collect Prizes");
        }
    }

    public void setCompletedDraft(final DeckGroup finishedDraft) {
        QuestDraftUtils.completeDraft(finishedDraft);

        view.editDeck(false);

        drafting = false;

        view.setMode(Mode.PREPARE_DECK);
        view.populate();
    }

    public void editDeck() {
        view.editDeck(true);
        FModel.getQuest().save();
    }

    public void startDraft() {
        if (drafting) {
            SOptionPane.showErrorDialog("You are currently in a draft.\n" +
                    "You should leave or finish that draft before starting another.");
            return;
        }

        final QuestEventDraft draftEvent = QuestUtil.getDraftEvent();

        final long creditsAvailable = FModel.getQuest().getAssets().getCredits();
        if (draftEvent.canEnter()) {
            SOptionPane.showMessageDialog("You need " + QuestUtil.formatCredits(draftEvent.getEntryFee() - creditsAvailable) + " more credits to enter this tournament.", "Not Enough Credits", SOptionPane.WARNING_ICON);
            return;
        }

        final boolean okayToEnter = SOptionPane.showOptionDialog("This tournament costs " + QuestUtil.formatCredits(draftEvent.getEntryFee()) + " credits to enter.\nAre you sure you wish to enter?", "Enter Draft Tournament?", FSkinProp.ICO_QUEST_GOLD, ImmutableList.of("Yes", "No"), 1) == 0;

        if (!okayToEnter) {
            return;
        }

        drafting = true;

        final BoosterDraft draft = draftEvent.enter();
        view.startDraft(draft);
    }

    public void startTournament() {
        FModel.getQuest().save();

        final String message = GameType.QuestDraft.getDeckFormat().getDeckConformanceProblem(FModel.getQuest().getAssets().getDraftDeckStorage().get(QuestEventDraft.DECK_NAME).getHumanDeck());
        if (message != null && FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            SOptionPane.showMessageDialog(message, "Deck Invalid");
            return;
        }

        FModel.getQuest().getAchievements().getCurrentDraft().start();

        view.setMode(Mode.TOURNAMENT_ACTIVE);
        view.populate();

        update();
    }

    public void startNextMatch() {
        final String message = QuestDraftUtils.getDeckLegality();

        if (message != null) {
            SOptionPane.showMessageDialog(message, "Deck Invalid");
            return;
        }

        if (QuestDraftUtils.matchInProgress) {
            SOptionPane.showErrorDialog("There is already a match in progress.\n" +
                    "Please wait for the current round to end before attempting to continue.");
            return;
        }

        gui = GuiBase.getInterface().getNewGuiGame();
        QuestDraftUtils.startNextMatch(gui);
    }
}
