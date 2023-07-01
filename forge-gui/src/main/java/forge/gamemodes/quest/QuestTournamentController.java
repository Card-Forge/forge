package forge.gamemodes.quest;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.quest.QuestDraftUtils.Mode;
import forge.gamemodes.quest.QuestEventDraft.QuestDraftFormat;
import forge.gamemodes.quest.data.QuestAchievements;
import forge.gamemodes.quest.data.QuestPreferences;
import forge.gamemodes.tournament.system.TournamentBracket;
import forge.gamemodes.tournament.system.TournamentPairing;
import forge.gamemodes.tournament.system.TournamentPlayer;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.gui.util.SGuiChoose;
import forge.gui.util.SOptionPane;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.ThreadUtil;
import forge.util.storage.IStorage;

public class QuestTournamentController {
    private final IQuestTournamentView view;
    private final Localizer localizer = Localizer.getInstance();
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
            final boolean shouldQuit = SOptionPane.showOptionDialog(localizer.getMessage("lblLeaveTournamentDraftWarning1"), localizer.getMessage("lblReallyQuit"), SOptionPane.WARNING_ICON, ImmutableList.of(localizer.getMessage("lblYes"), localizer.getMessage("lblNo")), 1) == 0;
            if (!shouldQuit) {
                return;
            }
        }
        else {
            if (draft.playerHasMatchesLeft()) {
                final boolean shouldQuit = SOptionPane.showOptionDialog(localizer.getMessage("lblLeaveTournamentDraftWarning2"), localizer.getMessage("lblReallyQuit"), SOptionPane.WARNING_ICON, ImmutableList.of(localizer.getMessage("lblYes"), localizer.getMessage("lblNo")), 1) == 0;
                if (!shouldQuit) {
                    return;
                }
            }

            final String placement = draft.getPlacementString();

            final QuestEventDraft.QuestDraftPrizes prizes = draft.collectPrizes();

            if (prizes.hasCredits()) {
                SOptionPane.showMessageDialog(localizer.getMessage("lblForPlacing") + placement + localizer.getMessage("lblHaveBeAward") + QuestUtil.formatCredits(prizes.credits) + " " + localizer.getMessage("lblCredits") + "!", localizer.getMessage("lblCreditsAwarded"), FSkinProp.ICO_QUEST_GOLD);
            }

            if (prizes.hasIndividualCards()) {
                GuiBase.getInterface().showCardList(localizer.getMessage("lblTournamentReward"), localizer.getMessage("lblParticipateingTournamentReward"), prizes.individualCards);
            }

            if (prizes.hasBoosterPacks()) {
                final String packPlural = (prizes.boosterPacks.size() == 1) ? "" : "s";

                SOptionPane.showMessageDialog(localizer.getMessage("lblForPlacing") + placement + localizer.getMessage("lblHaveBeAward") + prizes.boosterPacks.size() + " " + localizer.getMessage("lblBoosterPack") + packPlural + "!", localizer.getMessage("lblBoosterPack") + packPlural + " " + localizer.getMessage("lblAwarded"), FSkinProp.ICO_QUEST_BOX);

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

                        skipTheRest = GuiBase.getInterface().showBoxedProduct(pack.getName(), localizer.getMessage("lblFoundCards") + " (" + localizer.getMessage("lblBoosterPack") + " " + currentPack + " / " + totalPacks + "):", pack.getCards());
                    }

                    if (skipTheRest && !remainingCards.isEmpty()) {
                        GuiBase.getInterface().showCardList(localizer.getMessage("lblTournamentReward"), localizer.getMessage("lblFoundCards") + ":", remainingCards);
                    }
                }
                else {
                    final List<PaperCard> cards = new ArrayList<>();

                    while (!prizes.boosterPacks.isEmpty()) {
                        final BoosterPack pack = prizes.boosterPacks.remove(0);
                        cards.addAll(pack.getCards());
                    }

                    GuiBase.getInterface().showCardList(localizer.getMessage("lblTournamentReward"), localizer.getMessage("lblFoundCards") + ":", cards);
                }
            }

            if (prizes.selectRareFromSets()) {
                SOptionPane.showMessageDialog(localizer.getMessage("lblForPlacing") + placement + localizer.getMessage("lblSelectRareAwarded"), localizer.getMessage("lblRareAwarded"), FSkinProp.ICO_QUEST_STAKES);

                final PaperCard card = GuiBase.getInterface().chooseCard(localizer.getMessage("lblSelectACard"), localizer.getMessage("lblSelectKeepCard"), prizes.selectRareCards);
                prizes.addSelectedCard(card);

                SOptionPane.showMessageDialog("'" + card.getName() + "' " + localizer.getMessage("lblAddToCollection"), localizer.getMessage("lblCardAdded"), FSkinProp.ICO_QUEST_STAKES);
            }

            if (draft.getPlayerPlacement() == 1) {
                SOptionPane.showMessageDialog(localizer.getMessage("lblForPlacing") + placement + localizer.getMessage("lblHaveBeAwardToken"), localizer.getMessage("lblBonusToken"), FSkinProp.ICO_QUEST_NOTES);
                FModel.getQuest().getAchievements().addDraftToken();
            }

        }

        final boolean saveDraft = SOptionPane.showOptionDialog(localizer.getMessage("lblWouldLikeSaveDraft"), localizer.getMessage("lblSaveDraft") + "?", SOptionPane.QUESTION_ICON, ImmutableList.of(localizer.getMessage("lblYes"), localizer.getMessage("lblNo")), 0) == 0;
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
                SOptionPane.showErrorDialog(localizer.getMessage("lblNoAvailableDraftsMessage"),localizer.getMessage("lblNoAvailableDrafts"));
                return;
            }

            final QuestDraftFormat format = SGuiChoose.oneOrNone(localizer.getMessage("lblChooseDraftFormat"), formats);
            if (format != null) {
                QuestEventDraft evt = QuestEventDraft.getDraftOrNull(FModel.getQuest(), format);
                if (evt != null) {
                    String fee = TextUtil.concatNoSpace(localizer.getMessage("lblEntryFeeOfDraftTournament"), String.valueOf(evt.getEntryFee()), localizer.getMessage("lblWouldLikeCreateTournament"));
                    if (SOptionPane.showConfirmDialog(fee, localizer.getMessage("lblCreatingDraftTournament"))) {
                        achievements.spendDraftToken(format);
    
                        update();
                        view.populate();
                    }
                } else {
                    SOptionPane.showErrorDialog(localizer.getMessage("lblUnexpectedCreatingDraftTournament") + format.getName() + localizer.getMessage("lblPleaseReportBug"));
                    System.err.println("Error creating booster draft tournament (QuestEventDraft object was null): " + format.getName());
                }
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
        view.getLblCredits().setText(localizer.getMessage("lblCredits") + ": " + QuestUtil.formatCredits(FModel.getQuest().getAssets().getCredits()));

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

        view.getLblFirst().setText(localizer.getMessage("lbl1stPlace") + achievements.getWinsForPlace(1) + localizer.getMessage("lblTime") + (achievements.getWinsForPlace(1) == 1 ? "" : "s"));
        view.getLblSecond().setText(localizer.getMessage("lbl2ndPlace") + achievements.getWinsForPlace(2) + localizer.getMessage("lblTime") + (achievements.getWinsForPlace(2) == 1 ? "" : "s"));
        view.getLblThird().setText(localizer.getMessage("lbl3rdPlace") + achievements.getWinsForPlace(3) + localizer.getMessage("lblTime") + (achievements.getWinsForPlace(3) == 1 ? "" : "s"));
        view.getLblFourth().setText(localizer.getMessage("lbl4thPlace") + achievements.getWinsForPlace(4) + localizer.getMessage("lblTime") + (achievements.getWinsForPlace(4) == 1 ? "" : "s"));

        view.getBtnSpendToken().setText(localizer.getMessage("btnSpendToken") + " (" + achievements.getDraftTokens() + ")");
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
            view.getBtnLeaveTournament().setText(localizer.getMessage("btnLeaveTournament"));
        }
        else {
            view.getBtnLeaveTournament().setText(localizer.getMessage("lblCollectPrizes"));
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
            view.getBtnLeaveTournament().setText(localizer.getMessage("btnLeaveTournament"));
        } else {
            view.getBtnLeaveTournament().setText(localizer.getMessage("lblCollectPrizes"));
        }
    }

    public void setCompletedDraft(final DeckGroup finishedDraft) {
        QuestDraftUtils.completeDraft(finishedDraft);

        view.editDeck(false);

        drafting = false;

        view.setMode(Mode.PREPARE_DECK);
        view.populate();
    }

    public void startDraft() {
        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                if (drafting) {
                    SOptionPane.showErrorDialog(localizer.getMessage("lblCurrentlyInDraft"));
                    return;
                }

                final QuestEventDraft draftEvent = QuestUtil.getDraftEvent();

                final long creditsAvailable = FModel.getQuest().getAssets().getCredits();
                if (draftEvent.canEnter()) {
                    SOptionPane.showMessageDialog(localizer.getMessage("lblYouNeed") + QuestUtil.formatCredits(draftEvent.getEntryFee() - creditsAvailable) + " " + localizer.getMessage("lblMoreCredits"), localizer.getMessage("lblNotEnoughCredits"), SOptionPane.WARNING_ICON);
                    return;
                }

                final boolean okayToEnter = SOptionPane.showOptionDialog(localizer.getMessage("lblTournamentCosts") + QuestUtil.formatCredits(draftEvent.getEntryFee()) + localizer.getMessage("lblSureEnterTournament"), localizer.getMessage("lblEnterDraftTournament"), FSkinProp.ICO_QUEST_GOLD, ImmutableList.of(localizer.getMessage("lblYes"), localizer.getMessage("lblNo")), 1) == 0;

                if (!okayToEnter) {
                    return;
                }

                drafting = true;

                final BoosterDraft draft = draftEvent.enter();
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        view.startDraft(draft);
                    }
                });
            }
        });
    }

    public boolean cancelDraft() {
        if (SOptionPane.showConfirmDialog(localizer.getMessage("lblLeaveDraftConfirm"), localizer.getMessage("lblLeaveDraft") + "?", localizer.getMessage("lblLeave"), localizer.getMessage("lblCancel"), false)) {
            drafting = false;

            QuestController quest = FModel.getQuest();
            QuestEventDraft draft = quest.getAchievements().getCurrentDraft();
            quest.getAssets().addCredits(draft.getEntryFee());
            quest.getAchievements().deleteDraft(draft);
            quest.save();

            return true;
        }
        return false;
    }

    public void startTournament() {
        FModel.getQuest().save();

        final String message = GameType.QuestDraft.getDeckFormat().getDeckConformanceProblem(FModel.getQuest().getAssets().getDraftDeckStorage().get(QuestEventDraft.DECK_NAME).getHumanDeck());
        if (message != null && FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            SOptionPane.showMessageDialog(localizer.getMessage("lblDeck") + " " + message, localizer.getMessage("lblDeckInvalid"));
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
            SOptionPane.showMessageDialog(message, localizer.getMessage("lblDeckInvalid"));
            return;
        }

        if (QuestDraftUtils.matchInProgress) {
            SOptionPane.showErrorDialog(localizer.getMessage("lblAlreadyMatchPleaseWait"));
            return;
        }

        if (FModel.getQuestPreferences().getPrefInt(QuestPreferences.QPref.SIMULATE_AI_VS_AI_RESULTS) == 1 || GuiBase.getInterface().isLibgdxPort()) {
            if (!QuestDraftUtils.injectRandomMatchOutcome(false)) {
                gui = GuiBase.getInterface().getNewGuiGame();
                QuestDraftUtils.startNextMatch(gui);
            } else {
                view.populate();
                update();
            }
        } else {
            gui = GuiBase.getInterface().getNewGuiGame();
            QuestDraftUtils.startNextMatch(gui);
        }
    }
}
