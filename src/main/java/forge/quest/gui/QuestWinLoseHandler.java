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
import forge.quest.gui.QuestWinLoseCardViewer;
import forge.quest.gui.main.QuestEvent;
import forge.quest.gui.main.QuestChallenge;
import forge.view.swing.WinLoseModeHandler;

/** <p>QuestWinLoseHandler.</p> 
 * Processes win/lose presentation for Quest events. This presentation
 * is displayed by WinLoseFrame. Components to be added to pnlCustom in
 * WinLoseFrame should use MigLayout.
 *
 */
public class QuestWinLoseHandler extends WinLoseModeHandler {
    private boolean wonMatch;
    private ImageIcon icoTemp;
    private JLabel lblTemp1;
    private JLabel lblTemp2;
    int spacer = 50;
    
    private class CommonObjects { 
        public QuestMatchState qMatchState;
        public QuestData qData;
        public QuestEvent qEvent;
    }

    private CommonObjects model;
    
    public QuestWinLoseHandler() {
        super();  
        model = new CommonObjects();
        model.qMatchState = AllZone.getMatchState();
        model.qData = AllZone.getQuestData();
        model.qEvent = AllZone.getQuestEvent();
        wonMatch = model.qMatchState.isMatchWonBy(AllZone.getHumanPlayer().getName());
    }
    
    /**
     * <p>startNextRound.</p>
     * Either continues or restarts a current game.
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    @Override
    public void startNextRound() {
        if (Constant.Quest.fantasyQuest[0]) {
            int extraLife = 0;
            
            if (model.qEvent.getEventType().equals("challenge")) {
                if (model.qData.getInventory().hasItem("Zeppelin")) {
                    extraLife = 3;
                }
            }

            CardList humanList = QuestUtil.getHumanStartingCards(model.qData, model.qEvent);
            CardList computerList = new CardList();


            int humanLife = model.qData.getLife() + extraLife;
            int computerLife = 20;
            if (model.qEvent.getEventType().equals("challenge")) {
                computerLife = ((QuestChallenge)model.qEvent).getAILife();
            }

            AllZone.getGameAction().newGame(Constant.Runtime.HumanDeck[0], Constant.Runtime.ComputerDeck[0],
                    humanList, computerList, humanLife, computerLife, model.qEvent);
        } else {        
            super.startNextRound();
        }
    }
    
    /**
     * <p>populateCustomPanel.</p>
     * Checks conditions of win and fires various reward display methods accordingly.
     *
     * @param boolean indicating if custom panel has contents.
     */
    @Override
    public boolean populateCustomPanel() {
        view.btnRestart.setVisible(false);
        model.qData.getCards().resetNewList();
        
        if(!model.qMatchState.isMatchOver()) {
            view.btnQuit.setText("Quit (15 Credits)");
            return false;
        }
        else {
            view.btnContinue.setVisible(false);
            if(wonMatch) {
                view.btnQuit.setText("Great!");
            }
            else {
                view.btnQuit.setText("OK");
            }
        }
        
        // Win case
        if(wonMatch) {
            // Standard event reward credits
            awardEventCredits();
            
            // Challenge reward credits
            if(model.qEvent.getEventType().equals("challenge")) {
                awardChallengeWin();
            }
                        
            // Random rare given at 50% chance (65% with luck upgrade)
            if (getLuckyCoinResult()) {  
                awardRandomRare("You've won a random rare.");
            }
            
            // Random rare for winning against a very hard deck
            if(model.qData.getDifficultyIndex() == 4) { 
                awardRandomRare("You've won a random rare for winning against a very hard deck.");
            }
            
            // Award jackpot every 80 games won (currently 10 rares)
            int wins = model.qData.getWin();
            if (wins > 0 && wins % 80 == 0) { 
                awardJackpot(); 
            }
        }
        // Lose case
        else {
            penalizeLoss();
        }
        
        // Win or lose, still a chance to win a booster, frequency set in preferences
        int outcome = wonMatch ? model.qData.getWin() : model.qData.getLost();
        if (outcome % QuestPreferences.getWinsForBooster(model.qData.getDifficultyIndex()) == 0) {
            awardBooster(); 
        }

        return true;
    }
    
    /**
     * <p>actionOnQuit.</p>
     * When "quit" button is pressed, this method adjusts quest data as appropriate and saves.
     *
     */
    @Override
    public void actionOnQuit() {
        // Record win/loss in quest data
        if (wonMatch) {
            model.qData.addWin();
        } else {
            model.qData.addLost(); 
            model.qData.subtractCredits(15);
        }

        //System.out.println("model.qData cardpoolsize:" + AllZone.getQuestData().getCardpool().size());
        model.qData.getCards().clearShopList();

        if (model.qData.getAvailableChallenges() != null) {
            model.qData.clearAvailableChallenges();
        }

        model.qData.getCards().resetNewList();

        model.qMatchState.reset();
        AllZone.setQuestEvent(null);

        model.qData.saveData();

        new QuestFrame();
    }
    
    /**
     * <p>awardEventCredits.</p>
     * Generates and displays standard rewards for gameplay and skill level.
     *
     */
    private void awardEventCredits() {  
        // TODO use q.qdPrefs to write bonus credits in prefs file
        StringBuilder sb = new StringBuilder("<html>");
        
        int credTotal = 0;
        int credBase = 0;
        int credGameplay = 0;
        int credUndefeated = 0;
        int credEstates = 0;

        // Basic win bonus
        int base = QuestPreferences.getMatchRewardBase();
        double multiplier = 1;
        
        String diff = AllZone.getQuestEvent().getDifficulty();
        diff = diff.substring(0, 1).toUpperCase() + diff.substring(1);
        
        if(diff.equalsIgnoreCase("medium")) {
            multiplier = 1.5;
        }
        else if(diff.equalsIgnoreCase("hard")) {
            multiplier = 2;
        }
        else if(diff.equalsIgnoreCase("very hard")) {
            multiplier = 2.5;
        }
        else if(diff.equalsIgnoreCase("expert")) {
            multiplier = 3;
        }
        
        credBase += (int) (base*multiplier + 
                (QuestPreferences.getMatchRewardTotalWins() * model.qData.getWin()));
        
        sb.append(diff + " opponent: " + credBase + " credits.<br>");
        
        // Gameplay bonuses (for each game win)
        boolean hasNeverLost = true;
        
        Player computer = AllZone.getComputerPlayer();
        for (GameSummary game : model.qMatchState.getGamesPlayed()) {

            if (game.isWinner(computer.getName())) {
                hasNeverLost = false;
                continue; // no rewards for losing a game
            }

            // Alternate win
            GamePlayerRating aiRating = game.getPlayerRating(computer.getName());
            GamePlayerRating humanRating = game.getPlayerRating(AllZone.getHumanPlayer().getName());
            GameLossReason whyAiLost = aiRating.getLossReason();
            int altReward = getCreditsRewardForAltWin(whyAiLost);
            
            if (altReward > 0) {
                String winConditionName = "Unknown (bug)";
                if (game.getWinCondition() == GameEndReason.WinsGameSpellEffect) {
                    winConditionName = game.getWinSpellEffect();
                } else {
                    switch(whyAiLost) {
                        case Poisoned: winConditionName = "Poison"; break;
                        case Milled: winConditionName = "Milled"; break;
                        case SpellEffect: winConditionName = aiRating.getLossSpellName(); break;
                        default: break;
                    }
                }

                credGameplay += 50;
                sb.append(String.format("Alternate win condition: <u>%s</u>! " +
                		"Bonus: %d credits.<br>",
                        winConditionName, 50));
            }
    
            // Mulligan to zero
            int cntCardsHumanStartedWith = humanRating.getOpeningHandSize();
            int mulliganReward = QuestPreferences.getMatchMullToZero();
            
            if (0 == cntCardsHumanStartedWith) {
                credGameplay += mulliganReward;
                sb.append(String.format("Mulliganed to zero and still won! " +
                		"Bonus: %d credits.<br>", mulliganReward));
            }
            
            // Early turn bonus
            int winTurn = game.getTurnGameEnded();
            int turnCredits = getCreditsRewardForWinByTurn(winTurn);

            if (winTurn == 0) { System.err.println("QuestWinLoseHandler > " +
            		"turn calculation error: Zero turn win");
            } else if (winTurn == 1) { sb.append("Won in one turn!");
            } else if (winTurn <= 5) { sb.append("Won by turn 5!");
            } else if (winTurn <= 10) { sb.append("Won by turn 10!");
            } else if (winTurn <= 15) { sb.append("Won by turn 15!");
            }

            if (turnCredits > 0) {
                credGameplay += turnCredits;
                sb.append(String.format(" Bonus: %d credits.<br>", turnCredits));
            }
        } // End for(game)
        
        // Undefeated bonus
        if (hasNeverLost) {
            credUndefeated += QuestPreferences.getMatchRewardNoLosses();
            int reward = QuestPreferences.getMatchRewardNoLosses();
            sb.append(String.format("You have not lost once! " +
            		"Bonus: %d credits.<br>", reward));
        }
        
        // Estates bonus
        credTotal = credBase + credGameplay + credUndefeated;
        switch(model.qData.getInventory().getItemLevel("Estates")) {
            case 1: 
                credEstates = (int)0.1*credTotal; 
                sb.append("Estates bonus: 10%.<br>");
                break;
            
            case 2: 
                credEstates = (int)0.15*credTotal; 
                sb.append("Estates bonus: 15%.<br>");
                break;
                
            case 3: 
                credEstates = (int)0.2*credTotal; 
                sb.append("Estates bonus: 20%.<br>");
                break;
                
            default: break;
        }
        credTotal += credEstates;
    
        // Final output
        String congrats = "<br><h3>";
        if(credTotal < 100) {
            congrats += "You've earned";
        } else if(credTotal < 250) {
            congrats += "Could be worse: ";
        } else if(credTotal < 500) {
            congrats += "A respectable";
        } else if(credTotal < 750) {
            congrats += "An impressive";
        } else {
            congrats += "Spectacular match!";
        }

        sb.append(String.format("%s <b>%d credits</b> in total.</h3>", congrats, credTotal));
        sb.append("</body></html>");
        model.qData.addCredits(credTotal);
        
        // Generate Swing components and attach.
        icoTemp = GuiUtils.getResizedIcon("GoldIcon.png",0.5);
        
        lblTemp1 = new TitleLabel("Gameplay Results");
        
        lblTemp2 = new JLabel(sb.toString());
        lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        lblTemp2.setFont(AllZone.getSkin().font2.deriveFont(Font.PLAIN,14));
        lblTemp2.setForeground(Color.white);
        lblTemp2.setIcon(icoTemp);
        lblTemp2.setIconTextGap(50);

        
        view.pnlCustom.add(lblTemp1,"align center, width 95%!");
        view.pnlCustom.add(lblTemp2,"align center, width 95%!, gaptop 10");
    }
    
    /**
     * <p>awardRandomRare.</p>
     * Generates and displays a random rare win case.
     *
     */
    private void awardRandomRare(String message) {
        CardPrinted c = model.qData.getCards().addRandomRare();
        List<CardPrinted> cardsWon = new ArrayList<CardPrinted>();
        cardsWon.add(c);
        
        // Generate Swing components and attach.
        lblTemp1 = new TitleLabel(message);

        QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);
        
        view.pnlCustom.add(lblTemp1,"align center, width 95%!, " +
        		"gaptop " + spacer + ", gapbottom 10");
        view.pnlCustom.add(cv,"align center, width 95%!");   
    }
    
    /**
     * <p>awardJackpot.</p>
     * Generates and displays jackpot win case.
     *
     */
    private void awardJackpot() {            
        List<CardPrinted> cardsWon = model.qData.getCards().addRandomRare(10);

        // Generate Swing components and attach.        
        lblTemp1 = new TitleLabel("You just won 10 random rares!");
        QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);
        
        view.pnlCustom.add(lblTemp1,"align center, width 95%!, " +
                "gaptop " + spacer + ", gapbottom 10");
        view.pnlCustom.add(cv,"align center, width 95%!");  
    }
    
    /**
     * <p>awardBooster.</p>
     * Generates and displays booster pack win case.
     *
     */
    private void awardBooster() {
        ListChooser<GameFormat> ch = new ListChooser<GameFormat>("Choose bonus booster format", 1, SetUtils.getFormats());
        ch.show();
        GameFormat selected = ch.getSelectedValue();
        
        List<CardPrinted> cardsWon = model.qData.getCards().addCards(selected.getFilterPrinted());
        
        // Generate Swing components and attach.        
        lblTemp1 = new TitleLabel("Bonus booster pack from the \""+selected.getName()+"\" format!");
        QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);
        
        view.pnlCustom.add(lblTemp1,"align center, width 95%!, " +
                "gaptop " + spacer + ", gapbottom 10");
        view.pnlCustom.add(cv,"align center, width 95%!");  
    }
    
    /**
     * <p>awardChallengeWin.</p>
     * Generates and displays win case for challenge event.
     *
     */
    private void awardChallengeWin() {          
        if(!((QuestChallenge)model.qEvent).getRepeatable()) {
            model.qData.addCompletedChallenge(((QuestChallenge)model.qEvent).getId());
        }
        
        // Note: challenge only registers as "played" if it's won.
        // This doesn't seem right, but it's easy to fix.  Doublestrike 01-10-11
        model.qData.addChallengesPlayed();

        List<CardPrinted> cardsWon = ((QuestChallenge)model.qEvent).getCardRewardList();
        long questRewardCredits = ((QuestChallenge)model.qEvent).getCreditsReward();

        StringBuilder sb = new StringBuilder();
        sb.append("<html>Challenge completed.<br><br>");
        sb.append("Challenge bounty: <b>" + questRewardCredits + " credits.</b></html>");

        model.qData.addCredits(questRewardCredits);
        
        // Generate Swing components and attach.
        icoTemp = GuiUtils.getResizedIcon("BoxIcon.png",0.5);
        lblTemp1 = new TitleLabel("Challenge Rewards for \"" + 
                ((QuestChallenge)model.qEvent).getTitle() + "\"");
        
        lblTemp2 = new JLabel(sb.toString());
        lblTemp2.setFont(AllZone.getSkin().font2.deriveFont(Font.PLAIN,14));
        lblTemp2.setForeground(Color.white);
        lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        lblTemp2.setIconTextGap(50);
        lblTemp2.setIcon(icoTemp);

        view.pnlCustom.add(lblTemp1,"align center, width 95%!, " +
                "gaptop " + spacer);
        view.pnlCustom.add(lblTemp2,"align center, width 95%!, height 80!, gapbottom 10");
        
        if (cardsWon != null) {
            QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);
            view.pnlCustom.add(cv,"align center, width 95%!");                        
            model.qData.getCards().addAllCards(cardsWon);
        }
    } 
    
    private void penalizeLoss() {  
        icoTemp = GuiUtils.getResizedIcon("HeartIcon.png",0.5);
        
        lblTemp1 = new TitleLabel("Gameplay Results");
        
        lblTemp2 = new JLabel("You lose! You have lost 15 credits.");
        lblTemp2.setFont(AllZone.getSkin().font2.deriveFont(Font.PLAIN,14));
        lblTemp2.setForeground(Color.white);
        lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        lblTemp2.setIconTextGap(50);
        lblTemp2.setIcon(icoTemp);

        view.pnlCustom.add(lblTemp1,"align center, width 95%!");
        view.pnlCustom.add(lblTemp2,"align center, width 95%!, height 80!");
    }
    
    /**
     * <p>getLuckyCoinResult.</p>
     * A chance check, for rewards like random rares.
     *
     * @return boolean
     */
    private boolean getLuckyCoinResult() {
        boolean hasCoin = model.qData.getInventory().getItemLevel("Lucky Coin") >= 1;

        return MyRandom.random.nextFloat() <= (hasCoin ? 0.65f : 0.5f);
    }
    
    /**
     * <p>getCreditsRewardForAltWin.</p>
     * Retrieves credits for win under special conditions.
     *
     * @param GameLossReason why AI lost
     * @return int
     */
    private int getCreditsRewardForAltWin(final GameLossReason whyAiLost) {
        switch (whyAiLost) {
            case LifeReachedZero:   return 0; // nothing special here, ordinary kill
            case Milled:            return QuestPreferences.getMatchRewardMilledWinBonus();
            case Poisoned:          return QuestPreferences.getMatchRewardPoisonWinBonus();
            case DidNotLoseYet:     return QuestPreferences.getMatchRewardAltWinBonus(); // Felidar, Helix Pinnacle, etc.
            case SpellEffect:       return QuestPreferences.getMatchRewardAltWinBonus(); // Door to Nothingness, etc.
            default:                return 0;
        }
    }

    /**
     * <p>getCreditsRewardForWinByTurn.</p>
     * Retrieves credits for win on or under turn count.
     *
     * @param int turn count
     * @return int credits won
     */
    private int getCreditsRewardForWinByTurn(final int iTurn) {
        int credits = 0;
        
        if (iTurn == 1) { 
            credits = QuestPreferences.getMatchRewardWinFirst();
        } 
        else if (iTurn <= 5) { 
            credits = QuestPreferences.getMatchRewardWinByFifth();
        } 
        else if (iTurn <= 10) { 
            credits = QuestPreferences.getMatchRewardWinByTen();
        } 
        else if (iTurn <= 15) { 
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
        TitleLabel(String msg) {
            super(msg);
            this.setFont(AllZone.getSkin().font2.deriveFont(Font.ITALIC,16));
            this.setPreferredSize(new Dimension(200,40));
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setForeground(Color.white);
            this.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.white));
        }
    }
    
}