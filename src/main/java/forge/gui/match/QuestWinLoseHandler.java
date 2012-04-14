/** Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui.match;

import forge.AllZone;
import forge.CardList;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;

import forge.game.GameEndReason;
import forge.game.GameFormat;
import forge.game.GameLossReason;
import forge.game.GameNew;
import forge.game.GamePlayerRating;
import forge.game.GameSummary;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.gui.ListChooser;
import forge.gui.SOverlayUtils;
import forge.gui.home.quest.CSubmenuChallenges;
import forge.gui.home.quest.CSubmenuDuels;
import forge.gui.toolbox.FSkin;
import forge.item.CardPrinted;
import forge.model.FMatchState;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.QuestMode;
import forge.quest.QuestUtil;
import forge.quest.bazaar.QuestItemType;
import forge.quest.data.QuestAssets;
import forge.quest.data.QuestPreferences.QPref;
import forge.util.MyRandom;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * <p>
 * QuestWinLoseHandler.
 * </p>
 * Processes win/lose presentation for Quest events. This presentation is
 * displayed by WinLoseFrame. Components to be added to pnlCustom in
 * WinLoseFrame should use MigLayout.
 * 
 */
public class QuestWinLoseHandler extends ControlWinLose {
    private final transient boolean wonMatch;
    private final transient ViewWinLose view;
    private transient ImageIcon icoTemp;
    private transient JLabel lblTemp1;
    private transient JLabel lblTemp2;
    private final transient boolean isAnte;

    /** String constraint parameters for title blocks and cardviewer blocks. */
    private static final String CONSTRAINTS_TITLE = "w 95%!, gap 0 0 20px 10px";
    private static final String CONSTRAINTS_TEXT = "w 95%!,, h 180px!, gap 0 0 0 20px";
    private static final String CONSTRAINTS_CARDS = "w 95%!, h 330px!, gap 0 0 0 20px";

    private final transient FMatchState matchState;
    private final transient QuestController qData;
    private final transient QuestEvent qEvent;

    /**
     * Instantiates a new quest win lose handler.
     * 
     * @param view0 ViewWinLose object
     */
    public QuestWinLoseHandler(final ViewWinLose view0) {
        super(view0);
        this.view = view0;
        matchState = Singletons.getModel().getMatchState();
        qData = AllZone.getQuest();
        qEvent = qData.getCurrentEvent();
        this.wonMatch = matchState.isMatchWonBy(AllZone.getHumanPlayer().getName());
        this.isAnte = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE);
    }

    /**
     * <p>
     * startNextRound.
     * </p>
     * Either continues or restarts a current game.
     * 
     */
    @Override
    public final void startNextRound() {
        Singletons.getModel().savePrefs();
        SOverlayUtils.hideOverlay();
        Singletons.getModel().getQuestPreferences().save();

        QuestAssets qa = qData.getAssets();
        if (qData.getMode() == QuestMode.Fantasy) {
            int extraLife = 0;

            if (qEvent instanceof QuestEventChallenge) {
                if (qa.hasItem(QuestItemType.ZEPPELIN)) {
                    extraLife = 3;
                }
            }

            final CardList humanList = QuestUtil.getHumanStartingCards(qData, qEvent);
            final CardList computerList = QuestUtil.getComputerStartingCards(qEvent);

            final int humanLife = qa.getLife(qData.getMode()) + extraLife;
            int computerLife = 20;
            if (qEvent instanceof QuestEventChallenge) {
                computerLife = ((QuestEventChallenge) qEvent).getAILife();
            }

            GameNew.newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0],
                    humanList, computerList, humanLife, computerLife, qEvent.getIconFilename());
        } else {
            super.startNextRound();
        }
    }

    /**
     * <p>
     * populateCustomPanel.
     * </p>
     * Checks conditions of win and fires various reward display methods
     * accordingly.
     * 
     * @return true, if successful
     */
    @Override
    public final boolean populateCustomPanel() {
        this.getView().getBtnRestart().setVisible(false);
        qData.getCards().resetNewList();

        //do per-game actions
        if (matchState.hasWonLastGame(AllZone.getHumanPlayer().getName())) {
            if (isAnte) {
                final CardList antes = AllZone.getComputerPlayer().getCardsIn(ZoneType.Ante);
                final List<CardPrinted> antesPrinted = Singletons.getModel().getMatchState().addAnteWon(antes);
                this.anteWon(antesPrinted);

            }
        } else {
            if (isAnte) {
                final CardList antes = AllZone.getHumanPlayer().getCardsIn(ZoneType.Ante);
                final List<CardPrinted> antesPrinted = Singletons.getModel().getMatchState().addAnteLost(antes);
                for (final CardPrinted ante : antesPrinted) {
                    //the last param here (should) determine if this is added to the Card Shop
                    AllZone.getQuest().getCards().sellCard(ante, 0, false);
                }
                this.anteLost(antesPrinted);
            }
        }

        if (!matchState.isMatchOver()) {
            this.getView().getBtnQuit().setText("Quit (15 Credits)");
            return isAnte;
        } else {
            this.getView().getBtnContinue().setVisible(false);
            if (this.wonMatch) {
                this.getView().getBtnQuit().setText("Great!");
            } else {
                this.getView().getBtnQuit().setText("OK");
            }
        }

        // Win case
        if (this.wonMatch) {
            // Standard event reward credits
            this.awardEventCredits();

            // Challenge reward credits
            if (qEvent instanceof QuestEventChallenge) {
                this.awardChallengeWin();
            }

            // Random rare given at 50% chance (65% with luck upgrade)
            if (this.getLuckyCoinResult()) {
                this.awardRandomRare("You've won a random rare.");
            }

            // Random rare for winning against a very hard deck
            if (qData.getAchievements().getDifficulty() == 4) {
                this.awardRandomRare("You've won a random rare for winning against a very hard deck.");
            }

            // Award jackpot every 80 games won (currently 10 rares)
            final int wins = qData.getAchievements().getWin();
            if ((wins > 0) && ((wins % 80) == 0)) {
                this.awardJackpot();
            }
        }
        // Lose case
        else {
            this.penalizeLoss();
        }

        // Win or lose, still a chance to win a booster, frequency set in
        // preferences
        final int outcome = this.wonMatch ? qData.getAchievements().getWin() : qData.getAchievements().getLost();
        if ((outcome % Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.WINS_BOOSTER, qData.getAchievements().getDifficulty())) == 0) {
            this.awardBooster();
        }

        // Add any antes won this match (regardless of Match Win/Lose to Card Pool
        // Note: Antes lost have already been remove from decks.
        Singletons.getModel().getMatchState().addAnteWonToCardPool();

        return true;
    }

    /**
     * <p>
     * anteLost.
     * </p>
     * Displays cards lost to ante this game.
     * 
     */
    private void anteLost(final List<CardPrinted> antesLost) {
        // Generate Swing components and attach.
        this.lblTemp1 = new TitleLabel("Ante Lost: You lost the following cards in Ante:");

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLoseHandler.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(
                new QuestWinLoseCardViewer(antesLost), QuestWinLoseHandler.CONSTRAINTS_CARDS);
    }

    /**
     * <p>
     * anteWon.
     * </p>
     * Displays cards won in ante this game (which will be added to your Card Pool).
     * 
     */
    private void anteWon(final List<CardPrinted> antesWon) {
        final StringBuilder str = new StringBuilder();
        str.append("Ante Won: These cards will be available in your card pool after this match.");
        // Generate Swing components and attach.
        this.lblTemp1 = new TitleLabel(str.toString());

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLoseHandler.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(
                new QuestWinLoseCardViewer(antesWon), QuestWinLoseHandler.CONSTRAINTS_CARDS);
    }

    /**
     * <p>
     * actionOnQuit.
     * </p>
     * When "quit" button is pressed, this method adjusts quest data as
     * appropriate and saves.
     * 
     */
    @Override
    public final void actionOnQuit() {
        final int x = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.PENALTY_LOSS);

        // Record win/loss in quest data
        if (this.wonMatch) {
            qData.getAchievements().addWin();
        } else {
            qData.getAchievements().addLost();
            qData.getAssets().subtractCredits(x);
        }

        // Reset cards and zeppelin use
        qData.getCards().clearShopList();
        qData.getAssets().setItemLevel(QuestItemType.ZEPPELIN, 1);

        if (qEvent instanceof QuestEventChallenge && !((QuestEventChallenge) qEvent).isRepeatable()) {
            qData.getAchievements().addCompletedChallenge(((QuestEventChallenge) qEvent).getId());
        }

        if (qData.getAvailableChallenges() != null) {
            qData.clearAvailableChallenges();
        }

        matchState.reset();
        CSubmenuDuels.SINGLETON_INSTANCE.update();
        CSubmenuChallenges.SINGLETON_INSTANCE.update();

        qData.setCurrentEvent(null);
        qData.save();
        Singletons.getModel().getQuestPreferences().save();
        Singletons.getModel().savePrefs();

        Singletons.getControl().changeState(FControl.HOME_SCREEN);

        SOverlayUtils.hideOverlay();
    }

    /**
     * <p>
     * awardEventCredits.
     * </p>
     * Generates and displays standard rewards for gameplay and skill level.
     * 
     */
    private void awardEventCredits() {
        // TODO use q.qdPrefs to write bonus credits in prefs file
        final StringBuilder sb = new StringBuilder("<html>");

        int credTotal = 0;
        int credBase = 0;
        int credGameplay = 0;
        int credUndefeated = 0;
        int credEstates = 0;

        // Basic win bonus
        final int base = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_BASE);
        double multiplier = 1;

        String diff = qEvent.getDifficulty();
        diff = diff.substring(0, 1).toUpperCase() + diff.substring(1);

        if (diff.equalsIgnoreCase("medium")) {
            multiplier = 1.5;
        } else if (diff.equalsIgnoreCase("hard")) {
            multiplier = 2;
        } else if (diff.equalsIgnoreCase("very hard")) {
            multiplier = 2.5;
        } else if (diff.equalsIgnoreCase("expert")) {
            multiplier = 3;
        }

        credBase += (int) ((base * multiplier)
                + (Double.parseDouble(Singletons.getModel().getQuestPreferences().getPreference(QPref.REWARDS_WINS_MULTIPLIER))
                        * qData.getAchievements().getWin()));

        sb.append(diff + " opponent: " + credBase + " credits.<br>");
        // Gameplay bonuses (for each game win)
        boolean hasNeverLost = true;
        final Player computer = AllZone.getComputerPlayer();
        for (final GameSummary game : matchState.getGamesPlayed()) {
            if (game.isWinner(computer.getName())) {
                hasNeverLost = false;
                continue; // no rewards for losing a game
            }
            // Alternate win
            final GamePlayerRating aiRating = game.getPlayerRating(computer.getName());
            final GamePlayerRating humanRating = game.getPlayerRating(AllZone.getHumanPlayer().getName());
            final GameLossReason whyAiLost = aiRating.getLossReason();
            final int altReward = this.getCreditsRewardForAltWin(whyAiLost);

            if (altReward > 0) {
                String winConditionName = "Unknown (bug)";
                if (game.getWinCondition() == GameEndReason.WinsGameSpellEffect) {
                    winConditionName = game.getWinSpellEffect();
                } else {
                    switch (whyAiLost) {
                    case Poisoned:
                        winConditionName = "Poison";
                        break;
                    case Milled:
                        winConditionName = "Milled";
                        break;
                    case SpellEffect:
                        winConditionName = aiRating.getLossSpellName();
                        break;
                    default:
                        break;
                    }
                }

                credGameplay += 50;
                sb.append(String.format("Alternate win condition: <u>%s</u>! " + "Bonus: %d credits.<br>",
                        winConditionName, 50));
            }
            // Mulligan to zero
            final int cntCardsHumanStartedWith = humanRating.getOpeningHandSize();
            final int mulliganReward = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_MULLIGAN0);

            if (0 == cntCardsHumanStartedWith) {
                credGameplay += mulliganReward;
                sb.append(String
                        .format("Mulliganed to zero and still won! " + "Bonus: %d credits.<br>", mulliganReward));
            }

            // Early turn bonus
            final int winTurn = game.getTurnGameEnded();
            final int turnCredits = this.getCreditsRewardForWinByTurn(winTurn);

            if (winTurn == 0) {
                throw new UnsupportedOperationException("QuestWinLoseHandler > " + "turn calculation error: Zero turn win");
            } else if (winTurn == 1) {
                sb.append("Won in one turn!");
            } else if (winTurn <= 5) {
                sb.append("Won by turn 5!");
            } else if (winTurn <= 10) {
                sb.append("Won by turn 10!");
            } else if (winTurn <= 15) {
                sb.append("Won by turn 15!");
            }

            if (turnCredits > 0) {
                credGameplay += turnCredits;
                sb.append(String.format(" Bonus: %d credits.<br>", turnCredits));
            }
        } // End for(game)

        // Undefeated bonus
        if (hasNeverLost) {
            credUndefeated += Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_UNDEFEATED);
            final int reward = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_UNDEFEATED);
            sb.append(String.format("You have not lost once! " + "Bonus: %d credits.<br>", reward));
        }

        // Estates bonus
        credTotal = credBase + credGameplay + credUndefeated;
        double estateValue = 0;
        switch (qData.getAssets().getItemLevel(QuestItemType.ESTATES)) {
        case 1:
            estateValue = .1;
            break;

        case 2:
            estateValue = .15;
            break;

        case 3:
            estateValue = .2;
            break;

        default:
            break;
        }
        if (estateValue > 0) {
            credEstates = (int) (estateValue * credTotal);
            sb.append("Estates bonus: ").append((int) (100 * estateValue)).append("%.<br>");
            credTotal += credEstates;
        }

        // Final output
        String congrats = "<br><h3>";
        if (credTotal < 100) {
            congrats += "You've earned";
        } else if (credTotal < 250) {
            congrats += "Could be worse: ";
        } else if (credTotal < 500) {
            congrats += "A respectable";
        } else if (credTotal < 750) {
            congrats += "An impressive";
        } else {
            congrats += "Spectacular match!";
        }

        sb.append(String.format("%s <b>%d credits</b> in total.</h3>", congrats, credTotal));
        sb.append("</body></html>");
        qData.getAssets().addCredits(credTotal);

        // Generate Swing components and attach.
        this.icoTemp = GuiUtils.getResizedIcon(FSkin.getIcon(FSkin.QuestIcons.ICO_GOLD), 0.5);

        this.lblTemp1 = new TitleLabel("Gameplay Results");

        this.lblTemp2 = new JLabel(sb.toString());
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setFont(FSkin.getFont(14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setIcon(this.icoTemp);
        this.lblTemp2.setIconTextGap(50);

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLoseHandler.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(this.lblTemp2, QuestWinLoseHandler.CONSTRAINTS_TEXT);
    }

    /**
     * <p>
     * awardRandomRare.
     * </p>
     * Generates and displays a random rare win case.
     * 
     */
    private void awardRandomRare(final String message) {
        final CardPrinted c = qData.getCards().addRandomRare();
        final List<CardPrinted> cardsWon = new ArrayList<CardPrinted>();
        cardsWon.add(c);

        // Generate Swing components and attach.
        this.lblTemp1 = new TitleLabel(message);

        final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

        this.view.getPnlCustom().add(this.lblTemp1, QuestWinLoseHandler.CONSTRAINTS_TITLE);
        this.view.getPnlCustom().add(cv, QuestWinLoseHandler.CONSTRAINTS_CARDS);
    }

    /**
     * <p>
     * awardJackpot.
     * </p>
     * Generates and displays jackpot win case.
     * 
     */
    private void awardJackpot() {
        final List<CardPrinted> cardsWon = qData.getCards().addRandomRare(10);

        // Generate Swing components and attach.
        this.lblTemp1 = new TitleLabel("You just won 10 random rares!");
        final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

        this.view.getPnlCustom().add(this.lblTemp1, QuestWinLoseHandler.CONSTRAINTS_TITLE);
        this.view.getPnlCustom().add(cv, QuestWinLoseHandler.CONSTRAINTS_CARDS);
    }

    /**
     * <p>
     * awardBooster.
     * </p>
     * Generates and displays booster pack win case.
     * 
     */
    private void awardBooster() {
        final List<GameFormat> formats = new ArrayList<GameFormat>();
        String prefferedFormat = Singletons.getModel().getQuestPreferences().getPreference(QPref.BOOSTER_FORMAT);

        int index = 0, i = 0;
        for (GameFormat f : Singletons.getModel().getFormats()) {
            formats.add(f);
            if (f.toString().equals(prefferedFormat)) {
                index = i;
            }
            i++;
        }

        final ListChooser<GameFormat> ch = new ListChooser<GameFormat>("Choose bonus booster format", 1, formats);
        ch.show(index);

        final GameFormat selected = ch.getSelectedValue();
        Singletons.getModel().getQuestPreferences().setPreference(QPref.BOOSTER_FORMAT, selected.toString());

        final List<CardPrinted> cardsWon = qData.getCards().addCards(selected.getFilterPrinted());

        // Generate Swing components and attach.
        this.lblTemp1 = new TitleLabel("Bonus booster pack from the \"" + selected.getName() + "\" format!");
        final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

        this.view.getPnlCustom().add(this.lblTemp1, QuestWinLoseHandler.CONSTRAINTS_TITLE);
        this.view.getPnlCustom().add(cv, QuestWinLoseHandler.CONSTRAINTS_CARDS);
    }

    /**
     * <p>
     * awardChallengeWin.
     * </p>
     * Generates and displays win case for challenge event.
     * 
     */
    private void awardChallengeWin() {
        // This method should perhaps be called addChallengesWon() since it's actually
        // used for "wins before next challenge"
        qData.getAchievements().addChallengesPlayed();

        final List<CardPrinted> cardsWon = ((QuestEventChallenge) qEvent).getCardRewardList();
        final long questRewardCredits = ((QuestEventChallenge) qEvent).getCreditsReward();

        final StringBuilder sb = new StringBuilder();
        sb.append("<html>Challenge completed.<br><br>");
        sb.append("Challenge bounty: <b>" + questRewardCredits + " credits.</b></html>");

        qData.getAssets().addCredits(questRewardCredits);

        // Generate Swing components and attach.
        this.icoTemp = GuiUtils.getResizedIcon(FSkin.getIcon(FSkin.QuestIcons.ICO_BOX), 0.5);
        this.lblTemp1 = new TitleLabel("Challenge Rewards for \"" + ((QuestEventChallenge) qEvent).getTitle()
                + "\"");

        this.lblTemp2 = new JLabel(sb.toString());
        this.lblTemp2.setFont(FSkin.getFont(14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setIconTextGap(50);
        this.lblTemp2.setIcon(this.icoTemp);

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLoseHandler.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(this.lblTemp2, QuestWinLoseHandler.CONSTRAINTS_TEXT);

        if (cardsWon != null) {
            final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);
            this.getView().getPnlCustom().add(cv, QuestWinLoseHandler.CONSTRAINTS_CARDS);
            qData.getCards().addAllCards(cardsWon);
        }
    }

    private void penalizeLoss() {
        final int x = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.PENALTY_LOSS);
        this.icoTemp = GuiUtils.getResizedIcon(FSkin.getIcon(FSkin.QuestIcons.ICO_HEART), 0.5);

        this.lblTemp1 = new TitleLabel("Gameplay Results");

        this.lblTemp2 = new JLabel("You lose! You have lost " + x + " credits.");
        this.lblTemp2.setFont(FSkin.getFont(14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setIconTextGap(50);
        this.lblTemp2.setIcon(this.icoTemp);

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLoseHandler.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(this.lblTemp2, QuestWinLoseHandler.CONSTRAINTS_TEXT);
    }

    /**
     * <p>
     * getLuckyCoinResult.
     * </p>
     * A chance check, for rewards like random rares.
     * 
     * @return boolean
     */
    private boolean getLuckyCoinResult() {
        final boolean hasCoin = qData.getAssets().getItemLevel(QuestItemType.LUCKY_COIN) >= 1;

        return MyRandom.getRandom().nextFloat() <= (hasCoin ? 0.65f : 0.5f);
    }

    /**
     * <p>
     * getCreditsRewardForAltWin.
     * </p>
     * Retrieves credits for win under special conditions.
     * 
     * @param GameLossReason
     *            why AI lost
     * @return int
     */
    private int getCreditsRewardForAltWin(final GameLossReason whyAiLost) {
        switch (whyAiLost) {
        case LifeReachedZero:
            return 0; // nothing special here, ordinary kill
        case Milled:
            return Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_MILLED);
        case Poisoned:
            return Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_POISON);
        case DidNotLoseYet: // Felidar, Helix Pinnacle, etc.
            return Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_UNDEFEATED);
        case SpellEffect: // Door to Nothingness, etc.
            return Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_UNDEFEATED);
        default:
            return 0;
        }
    }

    /**
     * <p>
     * getCreditsRewardForWinByTurn.
     * </p>
     * Retrieves credits for win on or under turn count.
     * 
     * @param int turn count
     * @return int credits won
     */
    private int getCreditsRewardForWinByTurn(final int iTurn) {
        int credits;

        if (iTurn == 1) {
            credits = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_TURN1);
        } else if (iTurn <= 5) {
            credits = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_TURN5);
        } else if (iTurn <= 10) {
            credits = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_TURN10);
        } else if (iTurn <= 15) {
            credits = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.REWARDS_TURN15);
        } else {
            credits = 0;
        }

        return credits;
    }

    /**
     * JLabel header between reward sections.
     * 
     */
    @SuppressWarnings("serial")
    private class TitleLabel extends JLabel {
        TitleLabel(final String msg) {
            super(msg);
            this.setFont(FSkin.getFont(16));
            this.setPreferredSize(new Dimension(200, 40));
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setForeground(Color.white);
            this.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.white));
        }
    }
}
