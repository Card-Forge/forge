package forge;

import forge.card.CardPrinted;
import forge.error.ErrorViewer;
import forge.game.GameEndReason;
import forge.game.GameLossReason;
import forge.game.GamePlayerRating;
import forge.game.GameSummary;
import forge.game.PlayerIndex;
import forge.gui.CardListViewer;
import forge.gui.GuiUtils;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.properties.NewConstants.LANG.Gui_WinLose.WINLOSE_TEXT;
import forge.quest.data.QuestData;
import forge.quest.data.QuestMatchState;
import forge.quest.data.QuestPreferences;
import forge.quest.gui.QuestFrame;
import forge.view.swing.OldGuiNewGame;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Gui_WinLose class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Gui_WinLose extends JFrame implements NewConstants {
    /** Constant <code>serialVersionUID=-5800412940994975483L</code> */
    private static final long serialVersionUID = -5800412940994975483L;

    private JLabel titleLabel = new JLabel();
    private JButton continueButton = new JButton();
    private JButton restartButton = new JButton();
    private JButton quitButton = new JButton();
    private JLabel statsLabel = new JLabel();
    private JPanel jPanel2 = new JPanel();
    @SuppressWarnings("unused")
    // titledBorder1
    private TitledBorder titledBorder1;
    @SuppressWarnings("unused")
    // border1
    private Border border1;

    private class WinLoseModel {
        public QuestMatchState match;
        public QuestData quest;
        public Quest_Assignment qa;
    }

    private WinLoseModel model;

    /**
     * <p>Constructor for Gui_WinLose.</p>
     */
    public Gui_WinLose(final QuestMatchState matchState, final QuestData quest, final Quest_Assignment qa) {
        model = new WinLoseModel();
        model.match = matchState;
        model.quest = quest;
        model.qa = qa;

        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }

        setup();

        Dimension screen = this.getToolkit().getScreenSize();
        setBounds(screen.width / 3, 100 /*position*/, 215, 370 /*size*/);
        setVisible(true);
    }

    /**
     * <p>setup.</p>
     */
    private void setup() {
        Phase.setGameBegins(0);

        if (model.match.isMatchOver()) {
//      editDeckButton.setEnabled(false);
            continueButton.setEnabled(false);
            quitButton.grabFocus();
        }

        restartButton.setEnabled(true);

        //show Wins and Loses
        int humanWins = model.match.getGamesCountWonByHuman();
        int humanLosses = model.match.getGamesCountLostByHuman();
        statsLabel.setText(ForgeProps.getLocalized(WINLOSE_TEXT.WON) + humanWins
                + ForgeProps.getLocalized(WINLOSE_TEXT.LOST) + humanLosses);

        //show "You Won" or "You Lost"
        if (model.match.hasHumanWonLastGame()) {
            titleLabel.setText(ForgeProps.getLocalized(WINLOSE_TEXT.WIN));
        } else {
            titleLabel.setText(ForgeProps.getLocalized(WINLOSE_TEXT.LOSE));
        }
    }//setup();

    /**
     * <p>jbInit.</p>
     *
     * @throws java.lang.Exception if any.
     */
    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder("");
        border1 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titleLabel.setFont(new java.awt.Font("Dialog", 0, 26));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText(ForgeProps.getLocalized(WINLOSE_TEXT.WIN));
        this.getContentPane().setLayout(new MigLayout("fill"));
        continueButton.setText(ForgeProps.getLocalized(WINLOSE_TEXT.CONTINUE));
        continueButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { continueButton_actionPerformed(e); }
        });
        restartButton.setText(ForgeProps.getLocalized(WINLOSE_TEXT.RESTART));
        restartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { restartButton_actionPerformed(e); }
        });
        quitButton.setText(ForgeProps.getLocalized(WINLOSE_TEXT.QUIT));
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) { quitButton_actionPerformed(e); }
        });
        statsLabel.setFont(new java.awt.Font("Dialog", 0, 16));
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        jPanel2.setBorder(BorderFactory.createLineBorder(Color.black));
        jPanel2.setLayout(new MigLayout("align center"));
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { this_windowClosing(e); }
        });
        this.getContentPane().add(titleLabel, "align center, grow, wrap");
        this.getContentPane().add(statsLabel, "align center, grow, wrap");
        this.getContentPane().add(jPanel2, "grow");
        jPanel2.add(continueButton, "sg buttons, w 80%, h 20%, wrap");
        jPanel2.add(quitButton, "sg buttons, wrap");
        jPanel2.add(restartButton, "sg buttons");

    }

    /**
     * <p>continueButton_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    void continueButton_actionPerformed(ActionEvent e) {
        // issue 147 - keep battlefield up following win/loss
        JFrame frame = (JFrame) AllZone.getDisplay();
        frame.dispose();

        //open up "Game" screen
        PrepareForNextRound();
        AllZone.getDisplay().setVisible(true);
        frame.setEnabled(true);
        dispose();
    }
    
    void PrepareForNextRound()
    {
        if (Constant.Quest.fantasyQuest[0]) {
            int extraLife = 0;
            if (model.qa != null) {
                forge.quest.data.QuestUtil.setupQuest(model.qa);
                if (model.quest.getInventory().hasItem("Zeppelin"))
                    extraLife = 3;
            }
            //AllZone.getGameAction().newGame(Constant.Runtime.HumanDeck[0], Constant.Runtime.ComputerDeck[0], humanList, computerList, humanLife, computerLife);
            CardList humanList = forge.quest.data.QuestUtil.getHumanPlantAndPet(model.quest, model.qa);
            CardList computerList = new CardList();


            int humanLife = model.quest.getLife() + extraLife;
            int computerLife = 20;
            if ( model.qa != null )
                computerLife = model.qa.getComputerLife();

            AllZone.getGameAction().newGame(Constant.Runtime.HumanDeck[0], Constant.Runtime.ComputerDeck[0], humanList, computerList, humanLife, computerLife, model.qa);
        } else {
            AllZone.getGameAction().newGame(Constant.Runtime.HumanDeck[0], Constant.Runtime.ComputerDeck[0]);
        }

    }

    /**
     * <p>restartButton_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    void restartButton_actionPerformed(ActionEvent e) {
        // issue 147 - keep battlefield up following win/loss
        JFrame frame = (JFrame) AllZone.getDisplay();
        frame.dispose();
        
        model.match.reset();
        PrepareForNextRound();
        AllZone.getDisplay().setVisible(true);
        frame.setEnabled(true);
        dispose();
    }

    /**
     * <p>getWinText.</p>
     *
     * @param creds a long.
     * @param matchState a {@link forge.quest.data.QuestMatchState} object.
     * @param q a {@link forge.quest.data.QuestData} object.
     * @return a {@link java.lang.String} object.
     */
    private String getCreditsAwardedText(final long creds, final QuestMatchState matchState, final forge.quest.data.QuestData q) {
        // TODO use q.qdPrefs to write bonus credits in prefs file
        StringBuilder sb = new StringBuilder("<html>");

        boolean hasNeverLost = true;
        for (GameSummary game : matchState.getGamesPlayed()) {

            if (game.isAIWinner()) {
                hasNeverLost = true;
                continue; // no rewards for losing a game
            }

            GamePlayerRating aiRating = game.getPlayerRating(PlayerIndex.AI);
            GamePlayerRating humanRating = game.getPlayerRating(PlayerIndex.HUMAN);
            GameLossReason whyAiLost = aiRating.getLossReason();

            int rewardAltWinCondition = q.getRewards().getCreditsRewardForAltWin(whyAiLost);
            if (rewardAltWinCondition > 0) {
                String winConditionName = "Unknown (bug)";
                if (game.getWinCondition() == GameEndReason.WinsGameSpellEffect) {
                    winConditionName = game.getWinSpellEffect();
                } else {
                    switch(whyAiLost) {
                        case Poisoned: winConditionName = "Poison"; break;
                        case Milled: winConditionName = "Milled"; break;
                        default: break;
                    }
                }

                String bonus = String.format("Alternate win condition: <u>%s</u>! Bonus: <b>+%d credits</b>.<br>",
                        winConditionName, rewardAltWinCondition);
                sb.append(bonus);
            }

            int winTurn = game.getTurnGameEnded();
            int turnCredits = q.getRewards().getCreditsRewardForWinByTurn(winTurn);

            if (winTurn == 0) { // this case should never happen - anyway, no reward if we got here
            } else if (winTurn == 1) { sb.append("Won in one turn!");
            } else if (winTurn <= 5) { sb.append("Won by turn 5!");
            } else if (winTurn <= 10) { sb.append("Won by turn 10!");
            } else if (winTurn <= 15) { sb.append("Won by turn 15!");
            }

            if (turnCredits > 0) {
                sb.append(String.format(" Bonus: <b>+%d credits</b>.<br>", turnCredits));
            }

            int cntCardsHumanStartedWith = humanRating.getOpeningHandSize();
            if (0 == cntCardsHumanStartedWith) {
                int reward = QuestPreferences.getMatchMullToZero();
                sb.append(String.format("Mulliganed to zero and still won! Bonus: <b>%d credits</b>.<br>", reward));
            }
        }


        if (hasNeverLost) {
            int reward = QuestPreferences.getMatchRewardNoLosses();
            sb.append(String.format("You have not lost once! Bonus: <b>+%d credits</b>.<br>", reward));
        }

        int estatesLevel = q.getInventory().getItemLevel("Estates");

        if (estatesLevel == 1) { sb.append("Estates bonus: <b>10%</b>.<br>");
        } else if (estatesLevel == 2) { sb.append("Estates bonus: <b>15%</b>.<br>");
        } else if (estatesLevel == 3) { sb.append("Estates bonus: <b>20%</b>.<br>");
        }
        
        sb.append(String.format("<br>You have earned <b>%d credits</b> in total.", creds));
        sb.append("</html>");
        return sb.toString();
    }

    /**
     * <p>getIcon.</p>
     *
     * @param fileName a {@link java.lang.String} object.
     * @return a {@link javax.swing.ImageIcon} object.
     */
    private ImageIcon getIcon(String fileName) {
        File base = ForgeProps.getFile(IMAGE_ICON);
        File file = new File(base, fileName);
        ImageIcon icon = new ImageIcon(file.toString());
        return icon;
    }

    /**
     * <p>quitButton_actionPerformed.</p>
     *
     * @param e a {@link java.awt.event.ActionEvent} object.
     */
    void quitButton_actionPerformed(ActionEvent e) {
        // issue 147 - keep battlefield up following win/loss
        JFrame frame = (JFrame) AllZone.getDisplay();
        frame.dispose();
        frame.setEnabled(true);
        
        //are we on a quest?
        if (AllZone.getQuestData() == null) {
            new OldGuiNewGame();
        } else { //Quest

            boolean wonMatch = false;
            if (model.match.isMatchWonByHuman()) {
                model.quest.addWin();
                wonMatch = true;
            } else {
                model.quest.addLost();
            }

            //System.out.println("QuestData cardpoolsize:" + AllZone.getQuestData().getCardpool().size());
            model.quest.getCards().clearShopList();


            if (model.quest.getAvailableQuests() != null) {
                model.quest.clearAvailableQuests();
            }

            model.quest.getCards().resetNewList();
            giveQuestRewards(wonMatch);

            model.match.reset();
            AllZone.setQuestAssignment(null);

            model.quest.saveData();

            new QuestFrame();
        }//else - on quest

        dispose();

        //clear Image caches, so the program doesn't get slower and slower
        //not needed with soft values - will shrink as needed
//        ImageUtil.rotatedCache.clear();
//        ImageCache.cache.clear();
    }

    /**
     * <p>this_windowClosing.</p>
     *
     * @param e a {@link java.awt.event.WindowEvent} object.
     */
    void this_windowClosing(WindowEvent e) {
        quitButton_actionPerformed(null);
    }

    protected void giveBooster()
    {
        String[] boosterTypes = {"Legacy", "Extended", "Standard"};
        String boosterType = GuiUtils.getChoice("Choose prize booster format", boosterTypes);
        List<String> setsToGive = null;
        if (boosterTypes[2].equals(boosterType)) { // T2
            setsToGive = new ArrayList<String>();
            setsToGive.addAll(Arrays.asList(new String[]{"M12","NPH","MBS","M11","ROE","WWK","ZEN"}));
        }
        if (boosterTypes[1].equals(boosterType)) { // Ext
            setsToGive = new ArrayList<String>();
            setsToGive.addAll(Arrays.asList(new String[]{"M12","NPH","MBS","M11","ROE","WWK","ZEN","M10","ARB","CFX","ALA","MOR","SHM","EVE","LRW"}));
        }

        ArrayList<CardPrinted> cardsWon = model.quest.getCards().addCards(setsToGive);
        ImageIcon icon = getIcon("BookIcon.png");
        CardListViewer c = new CardListViewer("Booster", "You have won the following new cards", cardsWon, icon);
        c.show();
        
    }

    protected void giveQuestRewards(final boolean wonMatch) {
        // Award a random booster, as frequent as set in difficulty setup
        if (model.quest.getRewards().willGiveBooster(wonMatch)) {
            giveBooster();
        }

        // Award credits
        if (wonMatch) {
            long creds = model.quest.getRewards().getCreditsToAdd(model.match);
            model.quest.addCredits(creds);

            String s = getCreditsAwardedText(creds, model.match, model.quest);
            String fileName = "GoldIcon.png";
            ImageIcon icon = getIcon(fileName);

            JOptionPane.showMessageDialog(null, s, "", JOptionPane.INFORMATION_MESSAGE, icon);
        }

        // Award 10 rares for 80 games won
        if (wonMatch) {
            int wins = model.quest.getWin();
            if (wins > 0 && wins % 80 == 0) // at every 80 wins, give 10 random rares
            {
                List<CardPrinted> randomRares = model.quest.getCards().addRandomRare(10);
                
                ImageIcon icon = getIcon("BoxIcon.png");
                String title = "You just won 10 random rares!";
                CardListViewer c = new CardListViewer("Random rares", title, randomRares, icon);
                c.show();
            }
        }

        // Rewards from QuestAssignment
        if (wonMatch && model.qa != null) {
            model.quest.addQuestsPlayed();

            ArrayList<CardPrinted> questRewardCards = model.qa.getCardRewardList();
            long questRewardCredits = model.qa.getCreditsReward();

            StringBuilder sb = new StringBuilder();
            sb.append("Quest Completed - \r\n");

            if (questRewardCards != null) {
                sb.append("You won the following cards:\r\n\r\n");
                for (CardPrinted cardName : questRewardCards) {
                    sb.append(cardName.getName());
                    sb.append("\r\n");
                }
                model.quest.getCards().addAllCards(questRewardCards);
                sb.append("\r\n");
            }
            sb.append("Quest Bounty: ");
            sb.append(questRewardCredits);

            model.quest.addCredits(questRewardCredits);

            String fileName = "BoxIcon.png";
            ImageIcon icon = getIcon(fileName);
            String title = "Quest Rewards for " + model.qa.getName();
            JOptionPane.showMessageDialog(null, sb.toString(), title, JOptionPane.INFORMATION_MESSAGE, icon);
        }
        /*
        else if(quest.getDifficultyIndex() == 4) {
            Card c = AllZone.getCardFactory().getCard(quest.addRandomRare(), AllZone.getHumanPlayer());
            c.setCurSetCode(c.getMostRecentSet());
            fileName = CardUtil.buildFilename(c) +".jpg";
            icon = getCardIcon(fileName);

            JOptionPane.showMessageDialog(null, "", "You have won a random rare for winning against a very hard deck.", JOptionPane.INFORMATION_MESSAGE, icon);
        }*/

        // Loss match penalty
        if (!wonMatch) {
            model.quest.subtractCredits(15);
            String fileName = "HeartIcon.png";
            ImageIcon icon = getIcon(fileName);
            String text = "You lose! You have lost 15 credits.";
            JOptionPane.showMessageDialog(null, text, "Awwww", JOptionPane.INFORMATION_MESSAGE, icon);
        }

        // Random rare given at 50% chance (65% with luck upgrade)
        if (wonMatch && model.quest.getRewards().getLuckyCoinResult()) {
            CardPrinted card = model.quest.getCards().addRandomRare();
            ArrayList<CardPrinted> rares = new ArrayList<CardPrinted>();
            rares.add(card);

            String title = "You have won a random rare.";
            ImageIcon icon = getIcon("BoxIcon.png");

            CardListViewer c = new CardListViewer("Random rares", title, rares, icon);
            c.show();
        }
    }
}
