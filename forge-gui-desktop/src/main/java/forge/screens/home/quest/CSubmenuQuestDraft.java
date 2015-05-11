package forge.screens.home.quest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import com.google.common.collect.ImmutableList;

import forge.GuiBase;
import forge.Singletons;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.gui.BoxedProductCardListViewer;
import forge.gui.CardListChooser;
import forge.gui.CardListViewer;
import forge.gui.GuiChoose;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.interfaces.IGuiGame;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.itemmanager.DeckManager;
import forge.limited.BoosterDraft;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestDraftUtils;
import forge.quest.QuestEventDraft;
import forge.quest.QuestUtil;
import forge.quest.data.QuestAchievements;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorQuestDraftingProcess;
import forge.screens.deckeditor.controllers.CEditorQuestLimited;
import forge.screens.deckeditor.views.VCurrentDeck;
import forge.screens.home.quest.VSubmenuQuestDraft.Mode;
import forge.screens.home.sanctioned.CSubmenuDraft;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.JXButtonPanel;

/**
 * Controls the quest draft submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuQuestDraft implements ICDoc {
    SINGLETON_INSTANCE;

    private static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("#,###");

    private boolean drafting = false;
    private IGuiGame gui = null;

    @Override
    public void register() {
    }

    @SuppressWarnings("serial")
    @Override
    public void initialize() {

        final VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;

        view.getBtnStartDraft().addActionListener(selectTournamentStart);
        view.getBtnStartTournament().addActionListener(prepareDeckStart);
        view.getBtnStartMatch().addActionListener(nextMatchStart);

        view.getBtnStartMatchSmall().setCommand(new UiCommand() {
            @Override public void run() {
                CSubmenuQuestDraft.this.startNextMatch();
            }
        });
        view.getBtnSpendToken().setCommand(new UiCommand() {
            @Override public void run() {
                CSubmenuQuestDraft.this.spendToken();
            }
        });
        view.getBtnEditDeck().setCommand(new UiCommand() {
            @Override public void run() {
                CSubmenuQuestDraft.this.editDeck();
            }
        });
        view.getBtnLeaveTournament().setCommand(new UiCommand() {
            @Override public void run() {
                CSubmenuQuestDraft.this.endTournamentAndAwardPrizes();
            }
        });

        final QuestAchievements achievements = FModel.getQuest().getAchievements();
        FModel.getQuest().getDraftDecks();

        if (achievements == null) {

            view.setMode(Mode.EMPTY);

        } else if (achievements.getDraftEvents() == null || achievements.getDraftEvents().isEmpty()) {

            achievements.generateDrafts();

            if (achievements.getDraftEvents().isEmpty()) {
                view.setMode(Mode.EMPTY);
            } else {
                view.setMode(Mode.SELECT_TOURNAMENT);
            }

        } else if (FModel.getQuest().getDraftDecks() == null || !FModel.getQuest().getDraftDecks().contains(QuestEventDraft.DECK_NAME)) {

            achievements.generateDrafts();
            view.setMode(Mode.SELECT_TOURNAMENT);

        } else if (!achievements.getCurrentDraft().isStarted()) {

            view.setMode(Mode.PREPARE_DECK);

        } else {

            view.setMode(Mode.TOURNAMENT_ACTIVE);

        }

    }

    private void endTournamentAndAwardPrizes() {
        final QuestEventDraft draft = FModel.getQuest().getAchievements().getCurrentDraft();

        if (!draft.isStarted()) {

            final boolean shouldQuit = FOptionPane.showOptionDialog("If you leave now, this tournament will be forever gone."
                    + "\nYou will keep the cards you drafted, but will receive no other prizes."
                    + "\n\nWould you still like to quit the tournament?", "Really Quit?", FSkin.getImage(FSkinProp.ICO_WARNING).scale(2.0), ImmutableList.of("Yes", "No"), 1) == 0;
            if (!shouldQuit) {
                return;
            }

        } else {

            if (draft.playerHasMatchesLeft()) {
                final boolean shouldQuit = FOptionPane.showOptionDialog("You have matches left to play!\nLeaving the tournament early will forfeit your potential future winnings."
                        + "\nYou will still receive winnings as if you conceded your next match and you will keep the cards you drafted."
                        + "\n\nWould you still like to quit the tournament?", "Really Quit?", FSkin.getImage(FSkinProp.ICO_WARNING).scale(2.0), ImmutableList.of("Yes", "No"), 1) == 0;
                if (!shouldQuit) {
                    return;
                }
            }

            final String placement = draft.getPlacementString();

            final QuestEventDraft.QuestDraftPrizes prizes = draft.collectPrizes();

            if (prizes.hasCredits()) {
                FOptionPane.showMessageDialog("For placing " + placement + ", you have been awarded " + prizes.credits + " credits!", "Credits Awarded", FSkin.getImage(FSkinProp.ICO_QUEST_GOLD));
            }

            if (prizes.hasIndividualCards()) {
                final CardListViewer c = new CardListViewer("Tournament Reward", "For participating in the tournament, you have been awarded the following promotional card:", prizes.individualCards);
                c.setVisible(true);
                c.dispose();
            }

            if (prizes.hasBoosterPacks()) {

                final String packPlural = (prizes.boosterPacks.size() == 1) ? "" : "s";

                FOptionPane.showMessageDialog("For placing " + placement + ", you have been awarded " + prizes.boosterPacks.size() + " booster pack" + packPlural + "!", "Booster Pack" + packPlural + " Awarded", FSkin.getImage(FSkinProp.ICO_QUEST_BOX));

                if (FModel.getPreferences().getPrefBoolean(FPref.UI_OPEN_PACKS_INDIV) && prizes.boosterPacks.size() > 1) {

                    boolean skipTheRest = false;
                    final List<PaperCard> remainingCards = new ArrayList<>();
                    final int totalPacks = prizes.boosterPacks.size();
                    int currentPack = 0;

                    while (prizes.boosterPacks.size() > 0) {

                        final BoosterPack pack = prizes.boosterPacks.remove(0);
                        currentPack++;

                        if (skipTheRest) {
                            remainingCards.addAll(pack.getCards());
                            continue;
                        }

                        final BoxedProductCardListViewer c = new BoxedProductCardListViewer(pack.getName(), "You have found the following cards inside (Booster Pack " + currentPack + " of " + totalPacks + "):", pack.getCards());
                        c.setVisible(true);
                        c.dispose();
                        skipTheRest = c.skipTheRest();

                    }

                    if (skipTheRest && !remainingCards.isEmpty()) {
                        final CardListViewer c = new CardListViewer("Tournament Reward", "You have found the following cards inside:", remainingCards);
                        c.setVisible(true);
                        c.dispose();
                    }

                } else {

                    final List<PaperCard> cards = new ArrayList<>();

                    while (prizes.boosterPacks.size() > 0) {
                        final BoosterPack pack = prizes.boosterPacks.remove(0);
                        cards.addAll(pack.getCards());
                    }

                    final CardListViewer c = new CardListViewer("Tournament Reward", "You have found the following cards inside:", cards);
                    c.setVisible(true);
                    c.dispose();

                }

            }

            if (prizes.selectRareFromSets()) {

                FOptionPane.showMessageDialog("For placing " + placement  + ", you may select a rare or mythic rare card from the drafted block.", "Rare Awarded", FSkin.getImage(FSkinProp.ICO_QUEST_STAKES));

                final CardListChooser cardListChooser = new CardListChooser("Select a Card", "Select a card to keep:", prizes.selectRareCards);
                cardListChooser.setVisible(true);
                cardListChooser.dispose();
                prizes.addSelectedCard(cardListChooser.getSelectedCard());

                FOptionPane.showMessageDialog("'" + cardListChooser.getSelectedCard().getName() + "' has been added to your collection!", "Card Added", FSkin.getImage(FSkinProp.ICO_QUEST_STAKES));

            }

            if (draft.getPlayerPlacement() == 1) {
                FOptionPane.showMessageDialog("For placing " + placement + ", you have been awarded a token!\nUse tokens to create new drafts to play.", "Bonus Token", FSkin.getImage(FSkinProp.ICO_QUEST_NOTES));
                FModel.getQuest().getAchievements().addDraftToken();
            }

        }

        final boolean saveDraft = FOptionPane.showOptionDialog("Would you like to save this draft to the regular draft mode?", "Save Draft?", FSkin.getImage(FSkinProp.ICO_QUESTION).scale(2.0), ImmutableList.of("Yes", "No"), 0) == 0;

        if (saveDraft) {
            draft.saveToRegularDraft();
            CSubmenuDraft.SINGLETON_INSTANCE.update();
        }

        draft.addToQuestDecks();

        final VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;
        CSubmenuQuestDraft.SINGLETON_INSTANCE.update();
        view.populate();

    }



    private final ActionListener selectTournamentStart = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
            CSubmenuQuestDraft.this.startDraft();
        }
    };

    private final ActionListener prepareDeckStart = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
            CSubmenuQuestDraft.this.startTournament();
        }
    };

    private final ActionListener nextMatchStart = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
            CSubmenuQuestDraft.this.startNextMatch();
        }
    };

    private final KeyAdapter startOnEnter = new KeyAdapter() {
        @Override
        public void keyPressed(final KeyEvent e) {
            if (KeyEvent.VK_ENTER == e.getKeyChar()) {
                VSubmenuQuestDraft.SINGLETON_INSTANCE.getBtnStartDraft().doClick();
            }
        }
    };

    private final MouseAdapter startOnDblClick = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (MouseEvent.BUTTON1 == e.getButton() && 2 == e.getClickCount()) {
                VSubmenuQuestDraft.SINGLETON_INSTANCE.getBtnStartDraft().doClick();
            }
        }
    };

    private void spendToken() {

        final QuestAchievements achievements = FModel.getQuest().getAchievements();

        if (achievements != null) {

            final CardBlock block = GuiChoose.oneOrNone("Choose Draft Format", QuestEventDraft.getAvailableBlocks(FModel.getQuest()));

            if (block != null) {

                achievements.spendDraftToken(block);

                update();
                VSubmenuQuestDraft.SINGLETON_INSTANCE.populate();

            }

        }

    }

    @Override
    public void update() {

        final VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;

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
        } else if (!FModel.getQuest().getAchievements().getCurrentDraft().isStarted()) {
            view.setMode(Mode.PREPARE_DECK);
        } else {
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

        final VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;

        view.getLblCredits().setText("Available Credits: " + NUMBER_FORMATTER.format(FModel.getQuest().getAssets().getCredits()));

        final QuestAchievements achievements = FModel.getQuest().getAchievements();
        achievements.generateDrafts();

        view.getPnlTournaments().removeAll();
        final JXButtonPanel grpPanel = new JXButtonPanel();

        boolean firstPanel = true;

        for (final QuestEventDraft draft : FModel.getQuest().getAchievements().getDraftEvents()) {

            final PnlDraftEvent draftPanel = new PnlDraftEvent(draft);
            final JRadioButton button = draftPanel.getRadioButton();

            if (firstPanel) {
                button.setSelected(true);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() { button.requestFocusInWindow(); }
                });
                firstPanel = false;
            }

            grpPanel.add(draftPanel, button, "w 100%!, h 135px!, gapy 15px");

            button.addKeyListener(startOnEnter);
            button.addMouseListener(startOnDblClick);

        }

        view.getPnlTournaments().add(grpPanel, "w 100%!");

        updatePlacementLabelsText();

    }

    private void updatePlacementLabelsText() {

        final VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;
        final QuestAchievements achievements = FModel.getQuest().getAchievements();

        if (view.getMode().equals(Mode.EMPTY)) {
            view.getPnlTournaments().removeAll();
        }

        view.getLblFirst().setText("1st Place: " + achievements.getWinsForPlace(1) + " time" + (achievements.getWinsForPlace(1) == 1 ? "" : "s"));
        view.getLblSecond().setText("2nd Place: " + achievements.getWinsForPlace(2) + " time" + (achievements.getWinsForPlace(2) == 1 ? "" : "s"));
        view.getLblThird().setText("3rd Place: " + achievements.getWinsForPlace(3) + " time" + (achievements.getWinsForPlace(3) == 1 ? "" : "s"));
        view.getLblFourth().setText("4th Place: " + achievements.getWinsForPlace(4) + " time" + (achievements.getWinsForPlace(4) == 1 ? "" : "s"));

        view.getLblTokens().setText("Tokens: " + achievements.getDraftTokens());
        view.getBtnSpendToken().setEnabled(achievements.getDraftTokens() > 0);

    }

    private void updatePrepareDeck() {
    }

    private void updateTournamentActive() {

        final VSubmenuQuestDraft view = VSubmenuQuestDraft.SINGLETON_INSTANCE;

        if (FModel.getQuest().getAchievements().getCurrentDraft() == null) {
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

            SkinImage icon = FSkin.getAvatars().get(iconID);

            if (icon == null) {
                icon = FSkin.getAvatars().get(0);
            }

            if (first) {
                view.getLblsMatchups()[box].setPlayerOne(playerID, icon);
            } else {
                view.getLblsMatchups()[box].setPlayerTwo(playerID, icon);
            }

        }

        if (FModel.getQuest().getAchievements().getCurrentDraft().playerHasMatchesLeft()) {
            view.getBtnLeaveTournament().setText("Leave Tournament");
        } else {
            view.getBtnLeaveTournament().setText("Collect Prizes");
        }

    }

    public void setCompletedDraft(final DeckGroup finishedDraft) {

        QuestDraftUtils.completeDraft(finishedDraft);

        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_QUEST_TOURNAMENT);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorQuestLimited(FModel.getQuest(), CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture()));

        drafting = false;

        VSubmenuQuestDraft.SINGLETON_INSTANCE.setMode(Mode.PREPARE_DECK);
        VSubmenuQuestDraft.SINGLETON_INSTANCE.populate();

    }

    private void editDeck() {
        final CDetailPicture cDetailPicture = CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture();
        VCurrentDeck.SINGLETON_INSTANCE.setItemManager(new DeckManager(GameType.Draft, cDetailPicture));
        Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_QUEST_TOURNAMENT);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(new CEditorQuestLimited(FModel.getQuest(), cDetailPicture));
        FModel.getQuest().save();
    }

    private void startDraft() {

        if (drafting) {
            return;
        }

        final QuestEventDraft draftEvent = QuestUtil.getDraftEvent();

        final long creditsAvailable = FModel.getQuest().getAssets().getCredits();
        if (draftEvent.canEnter()) {
            FOptionPane.showMessageDialog("You need " + NUMBER_FORMATTER.format(draftEvent.getEntryFee() - creditsAvailable) + " more credits to enter this tournament.", "Not Enough Credits", FSkin.getImage(FSkinProp.ICO_WARNING).scale(2.0));
            return;
        }

        final boolean okayToEnter = FOptionPane.showOptionDialog("This tournament costs " + draftEvent.getEntryFee() + " credits to enter.\nAre you sure you wish to enter?", "Enter Draft Tournament?", FSkin.getImage(FSkinProp.ICO_QUEST_GOLD), ImmutableList.of("Yes", "No"), 1) == 0;

        if (!okayToEnter) {
            return;
        }

        drafting = true;

        final BoosterDraft draft = draftEvent.enter();

        final CEditorQuestDraftingProcess draftController = new CEditorQuestDraftingProcess(CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());
        draftController.showGui(draft);

        draftController.setDraftQuest(CSubmenuQuestDraft.this);

        Singletons.getControl().setCurrentScreen(FScreen.DRAFTING_PROCESS);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(draftController);

    }

    private void startTournament() {

        FModel.getQuest().save();

        final String message = GameType.QuestDraft.getDeckFormat().getDeckConformanceProblem(FModel.getQuest().getAssets().getDraftDeckStorage().get(QuestEventDraft.DECK_NAME).getHumanDeck());
        if (message != null && FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            FOptionPane.showMessageDialog(message, "Deck Invalid");
            return;
        }

        FModel.getQuest().getAchievements().getCurrentDraft().start();

        VSubmenuQuestDraft.SINGLETON_INSTANCE.setMode(Mode.TOURNAMENT_ACTIVE);
        VSubmenuQuestDraft.SINGLETON_INSTANCE.populate();

        update();

    }

    private void startNextMatch() {

        final String message = QuestDraftUtils.getDeckLegality();

        if (message != null) {
            FOptionPane.showMessageDialog(message, "Deck Invalid");
            return;
        }

        gui = GuiBase.getInterface().getNewGuiGame();
        QuestDraftUtils.startNextMatch(gui);

    }

}
