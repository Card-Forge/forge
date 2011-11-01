package forge.quest.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.slightlymagic.maxmtg.Predicate;
import forge.AllZone;
import forge.CardList;
import forge.Constant;
import forge.MyRandom;
import forge.Player;
import forge.SetUtils;
import forge.game.GameEndReason;
import forge.game.GameFormat;
import forge.game.GameLossReason;
import forge.game.GamePlayerRating;
import forge.game.GameSummary;
import forge.gui.GuiUtils;
import forge.gui.ListChooser;
import forge.item.CardPrinted;
import forge.quest.data.QuestData;
import forge.quest.data.QuestMatchState;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestUtil;
import forge.quest.gui.main.QuestChallenge;
import forge.quest.gui.main.QuestEvent;
import forge.view.swing.WinLoseModeHandler;

/**
 * <p>
 * QuestWinLoseHandler.
 * </p>
 * Processes win/lose presentation for Quest events. This presentation is
 * displayed by WinLoseFrame. Components to be added to pnlCustom in
 * WinLoseFrame should use MigLayout.
 * 
 */
public class QuestWinLoseHandler extends WinLoseModeHandler {
    private final boolean wonMatch;
    private ImageIcon icoTemp;
    private JLabel lblTemp1;
    private JLabel lblTemp2;

    /** The spacer. */
    private int spacer = 50;

    private class CommonObjects {
        private QuestMatchState qMatchState;
        private QuestData qData;
        private QuestEvent qEvent;
    }

    private final CommonObjects model;

    /**
     * Instantiates a new quest win lose handler.
     */
    public QuestWinLoseHandler() {
        super();
        this.model = new CommonObjects();
        this.model.qMatchState = AllZone.getMatchState();
        this.model.qData = AllZone.getQuestData();
        this.model.qEvent = AllZone.getQuestEvent();
        this.wonMatch = this.model.qMatchState.isMatchWonBy(AllZone.getHumanPlayer().getName());
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
        if (Constant.Quest.FANTASY_QUEST[0]) {
            int extraLife = 0;

            if (this.model.qEvent.getEventType().equals("challenge")) {
                if (this.model.qData.getInventory().hasItem("Zeppelin")) {
                    extraLife = 3;
                }
            }

            final CardList humanList = QuestUtil.getHumanStartingCards(this.model.qData, this.model.qEvent);
            final CardList computerList = QuestUtil.getComputerStartingCards(this.model.qData, this.model.qEvent);

            final int humanLife = this.model.qData.getLife() + extraLife;
            int computerLife = 20;
            if (this.model.qEvent.getEventType().equals("challenge")) {
                computerLife = ((QuestChallenge) this.model.qEvent).getAILife();
            }

            AllZone.getGameAction().newGame(Constant.Runtime.HUMAN_DECK[0], Constant.Runtime.COMPUTER_DECK[0],
                    humanList, computerList, humanLife, computerLife, this.model.qEvent);
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
        this.view.btnRestart.setVisible(false);
        this.model.qData.getCards().resetNewList();

        if (!this.model.qMatchState.isMatchOver()) {
            this.view.btnQuit.setText("Quit (15 Credits)");
            return false;
        } else {
            this.view.btnContinue.setVisible(false);
            if (this.wonMatch) {
                this.view.btnQuit.setText("Great!");
            } else {
                this.view.btnQuit.setText("OK");
            }
        }

        // Win case
        if (this.wonMatch) {
            // Standard event reward credits
            this.awardEventCredits();

            // Challenge reward credits
            if (this.model.qEvent.getEventType().equals("challenge")) {
                this.awardChallengeWin();
            }

            // Random rare given at 50% chance (65% with luck upgrade)
            if (this.getLuckyCoinResult()) {
                this.awardRandomRare("You've won a random rare.");
            }

            // Random rare for winning against a very hard deck
            if (this.model.qData.getDifficultyIndex() == 4) {
                this.awardRandomRare("You've won a random rare for winning against a very hard deck.");
            }

            // Award jackpot every 80 games won (currently 10 rares)
            final int wins = this.model.qData.getWin();
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
        final int outcome = this.wonMatch ? this.model.qData.getWin() : this.model.qData.getLost();
        if ((outcome % QuestPreferences.getWinsForBooster(this.model.qData.getDifficultyIndex())) == 0) {
            this.awardBooster();
        }

        return true;
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
        // Record win/loss in quest data
        if (this.wonMatch) {
            this.model.qData.addWin();
        } else {
            this.model.qData.addLost();
            this.model.qData.subtractCredits(15);
        }

        this.model.qData.getCards().clearShopList();

        if (this.model.qData.getAvailableChallenges() != null) {
            this.model.qData.clearAvailableChallenges();
        }

        this.model.qMatchState.reset();
        AllZone.setQuestEvent(null);

        this.model.qData.saveData();

        new QuestFrame();
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
        final int base = QuestPreferences.getMatchRewardBase();
        double multiplier = 1;

        String diff = AllZone.getQuestEvent().getDifficulty();
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
        credBase += (int) ((base * multiplier) + (QuestPreferences.getMatchRewardTotalWins() * this.model.qData
                .getWin()));
        sb.append(diff + " opponent: " + credBase + " credits.<br>");
        // Gameplay bonuses (for each game win)
        boolean hasNeverLost = true;
        final Player computer = AllZone.getComputerPlayer();
        for (final GameSummary game : this.model.qMatchState.getGamesPlayed()) {
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
            final int mulliganReward = QuestPreferences.getMatchMullToZero();

            if (0 == cntCardsHumanStartedWith) {
                credGameplay += mulliganReward;
                sb.append(String
                        .format("Mulliganed to zero and still won! " + "Bonus: %d credits.<br>", mulliganReward));
            }

            // Early turn bonus
            final int winTurn = game.getTurnGameEnded();
            final int turnCredits = this.getCreditsRewardForWinByTurn(winTurn);

            if (winTurn == 0) {
                System.err.println("QuestWinLoseHandler > " + "turn calculation error: Zero turn win");
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
            credUndefeated += QuestPreferences.getMatchRewardNoLosses();
            final int reward = QuestPreferences.getMatchRewardNoLosses();
            sb.append(String.format("You have not lost once! " + "Bonus: %d credits.<br>", reward));
        }

        // Estates bonus
        credTotal = credBase + credGameplay + credUndefeated;
        switch (this.model.qData.getInventory().getItemLevel("Estates")) {
        case 1:
            credEstates = (int) 0.1 * credTotal;
            sb.append("Estates bonus: 10%.<br>");
            break;

        case 2:
            credEstates = (int) 0.15 * credTotal;
            sb.append("Estates bonus: 15%.<br>");
            break;

        case 3:
            credEstates = (int) 0.2 * credTotal;
            sb.append("Estates bonus: 20%.<br>");
            break;

        default:
            break;
        }
        credTotal += credEstates;

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
        this.model.qData.addCredits(credTotal);

        // Generate Swing components and attach.
        this.icoTemp = GuiUtils.getResizedIcon("GoldIcon.png", 0.5);

        this.lblTemp1 = new TitleLabel("Gameplay Results");

        this.lblTemp2 = new JLabel(sb.toString());
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setFont(AllZone.getSkin().getFont2().deriveFont(Font.PLAIN, 14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setIcon(this.icoTemp);
        this.lblTemp2.setIconTextGap(50);

        this.view.pnlCustom.add(this.lblTemp1, "align center, width 95%!");
        this.view.pnlCustom.add(this.lblTemp2, "align center, width 95%!, gaptop 10");
    }

    /**
     * <p>
     * awardRandomRare.
     * </p>
     * Generates and displays a random rare win case.
     * 
     */
    private void awardRandomRare(final String message) {
        final CardPrinted c = this.model.qData.getCards().addRandomRare();
        final List<CardPrinted> cardsWon = new ArrayList<CardPrinted>();
        cardsWon.add(c);

        // Generate Swing components and attach.
        this.lblTemp1 = new TitleLabel(message);

        final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

        this.view.pnlCustom.add(this.lblTemp1, "align center, width 95%!, " + "gaptop " + this.spacer
                + ", gapbottom 10");
        this.view.pnlCustom.add(cv, "align center, width 95%!");
    }

    /**
     * <p>
     * awardJackpot.
     * </p>
     * Generates and displays jackpot win case.
     * 
     */
    private void awardJackpot() {
        final List<CardPrinted> cardsWon = this.model.qData.getCards().addRandomRare(10);

        // Generate Swing components and attach.
        this.lblTemp1 = new TitleLabel("You just won 10 random rares!");
        final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

        this.view.pnlCustom.add(this.lblTemp1, "align center, width 95%!, " + "gaptop " + this.spacer
                + ", gapbottom 10");
        this.view.pnlCustom.add(cv, "align center, width 95%!");
    }

    /**
     * <p>
     * awardBooster.
     * </p>
     * Generates and displays booster pack win case.
     * 
     */
    private void awardBooster() {
        final ListChooser<GameFormat> ch = new ListChooser<GameFormat>("Choose bonus booster format", 1,
                SetUtils.getFormats());
        ch.show();
        final GameFormat selected = ch.getSelectedValue();

        final List<CardPrinted> cardsWon = this.model.qData.getCards().addCards(
                Predicate.and(selected.getFilterPrinted(), CardPrinted.Predicates.Presets.NON_ALTERNATE));

        // Generate Swing components and attach.
        this.lblTemp1 = new TitleLabel("Bonus booster pack from the \"" + selected.getName() + "\" format!");
        final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

        this.view.pnlCustom.add(this.lblTemp1, "align center, width 95%!, " + "gaptop " + this.spacer
                + ", gapbottom 10");
        this.view.pnlCustom.add(cv, "align center, width 95%!");
    }

    /**
     * <p>
     * awardChallengeWin.
     * </p>
     * Generates and displays win case for challenge event.
     * 
     */
    private void awardChallengeWin() {
        if (!((QuestChallenge) this.model.qEvent).getRepeatable()) {
            this.model.qData.addCompletedChallenge(((QuestChallenge) this.model.qEvent).getId());
        }

        // Note: challenge only registers as "played" if it's won.
        // This doesn't seem right, but it's easy to fix. Doublestrike 01-10-11
        this.model.qData.addChallengesPlayed();

        final List<CardPrinted> cardsWon = ((QuestChallenge) this.model.qEvent).getCardRewardList();
        final long questRewardCredits = ((QuestChallenge) this.model.qEvent).getCreditsReward();

        final StringBuilder sb = new StringBuilder();
        sb.append("<html>Challenge completed.<br><br>");
        sb.append("Challenge bounty: <b>" + questRewardCredits + " credits.</b></html>");

        this.model.qData.addCredits(questRewardCredits);

        // Generate Swing components and attach.
        this.icoTemp = GuiUtils.getResizedIcon("BoxIcon.png", 0.5);
        this.lblTemp1 = new TitleLabel("Challenge Rewards for \"" + ((QuestChallenge) this.model.qEvent).getTitle()
                + "\"");

        this.lblTemp2 = new JLabel(sb.toString());
        this.lblTemp2.setFont(AllZone.getSkin().getFont2().deriveFont(Font.PLAIN, 14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setIconTextGap(50);
        this.lblTemp2.setIcon(this.icoTemp);

        this.view.pnlCustom.add(this.lblTemp1, "align center, width 95%!, " + "gaptop " + this.spacer);
        this.view.pnlCustom.add(this.lblTemp2, "align center, width 95%!, height 80!, gapbottom 10");

        if (cardsWon != null) {
            final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);
            this.view.pnlCustom.add(cv, "align center, width 95%!");
            this.model.qData.getCards().addAllCards(cardsWon);
        }
    }

    private void penalizeLoss() {
        this.icoTemp = GuiUtils.getResizedIcon("HeartIcon.png", 0.5);

        this.lblTemp1 = new TitleLabel("Gameplay Results");

        this.lblTemp2 = new JLabel("You lose! You have lost 15 credits.");
        this.lblTemp2.setFont(AllZone.getSkin().getFont2().deriveFont(Font.PLAIN, 14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setIconTextGap(50);
        this.lblTemp2.setIcon(this.icoTemp);

        this.view.pnlCustom.add(this.lblTemp1, "align center, width 95%!");
        this.view.pnlCustom.add(this.lblTemp2, "align center, width 95%!, height 80!");
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
        final boolean hasCoin = this.model.qData.getInventory().getItemLevel("Lucky Coin") >= 1;

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
            return QuestPreferences.getMatchRewardMilledWinBonus();
        case Poisoned:
            return QuestPreferences.getMatchRewardPoisonWinBonus();
        case DidNotLoseYet:
            return QuestPreferences.getMatchRewardAltWinBonus(); // Felidar,
                                                                 // Helix
                                                                 // Pinnacle,
                                                                 // etc.
        case SpellEffect:
            return QuestPreferences.getMatchRewardAltWinBonus(); // Door to
                                                                 // Nothingness,
                                                                 // etc.
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
        int credits = 0;

        if (iTurn == 1) {
            credits = QuestPreferences.getMatchRewardWinFirst();
        } else if (iTurn <= 5) {
            credits = QuestPreferences.getMatchRewardWinByFifth();
        } else if (iTurn <= 10) {
            credits = QuestPreferences.getMatchRewardWinByTen();
        } else if (iTurn <= 15) {
            credits = QuestPreferences.getMatchRewardWinByFifteen();
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
            this.setFont(AllZone.getSkin().getFont2().deriveFont(Font.ITALIC, 16));
            this.setPreferredSize(new Dimension(200, 40));
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setForeground(Color.white);
            this.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.white));
        }
    }
}
