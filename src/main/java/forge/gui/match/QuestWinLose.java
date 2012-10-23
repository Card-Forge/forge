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

import javax.swing.JOptionPane;

import forge.Card;
import forge.Singletons;
import forge.control.FControl;

import forge.card.BoosterData;
import forge.card.CardEdition;
import forge.card.UnOpenedProduct;
import forge.game.GameEndReason;
import forge.game.GameFormat;
import forge.game.GameLossReason;
import forge.game.GameOutcome;
import forge.game.MatchController;
import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerOutcome;
import forge.game.player.PlayerStatistics;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.ListChooser;
import forge.gui.SOverlayUtils;
import forge.gui.home.quest.CSubmenuChallenges;
import forge.gui.home.quest.CSubmenuDuels;
import forge.gui.toolbox.FSkin;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.quest.bazaar.QuestItemType;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.io.ReadPriceList;
import forge.util.MyRandom;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * <p>
 * QuestWinLose.
 * </p>
 * Processes win/lose presentation for Quest events. This presentation is
 * displayed by WinLoseFrame. Components to be added to pnlCustom in
 * WinLoseFrame should use MigLayout.
 * 
 */
public class QuestWinLose extends ControlWinLose {
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

    private final transient QuestController qData;
    private final transient QuestEvent qEvent;

    /**
     * Instantiates a new quest win lose handler.
     * 
     * @param view0 ViewWinLose object
     * @param match2 
     */
    public QuestWinLose(final ViewWinLose view0, MatchController match2) {
        super(view0, match2);
        this.view = view0;
        qData = Singletons.getModel().getQuest();
        qEvent = qData.getCurrentEvent();
        this.wonMatch = match.isWonBy(Singletons.getControl().getLobby().getQuestPlayer());
        this.isAnte = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE);
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
        QuestController qc = Singletons.getModel().getQuest(); 
        LobbyPlayer questPlayer = Singletons.getControl().getLobby().getQuestPlayer();
        if (isAnte) {
            //do per-game actions
            GameOutcome outcome = match.getLastGameOutcome();
            
            // Ante returns to owners in a draw
            if (!outcome.isDraw()) {
                boolean isHumanWinner = outcome.getWinner().equals(questPlayer);
                final List<CardPrinted> anteCards = new ArrayList<CardPrinted>();
                for( Player p : Singletons.getModel().getGame().getPlayers() ) {
                    if (p.getLobbyPlayer().equals(questPlayer) == isHumanWinner) continue;
                    for(Card c : p.getCardsIn(ZoneType.Ante))
                        anteCards.add(CardDb.instance().getCard(c));
                }
                    
                if (isHumanWinner) {
                    qc.getCards().addAllCards(anteCards);
                    this.anteWon(anteCards);
                } else {
                    for(CardPrinted c : anteCards)
                        qc.getCards().loseCard(c);
                    this.anteLost(anteCards);
                }
            }
        }

        if (!match.isMatchOver()) {
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

        // TODO: We don't have a enum for difficulty?
        int difficulty = qData.getAchievements().getDifficulty();

        // Win case
        if (this.wonMatch) {
            // Standard event reward credits
            this.awardEventCredits();

            // Challenge reward credits
            if (qEvent instanceof QuestEventChallenge) {
                this.awardChallengeWin();
            }
            // Random rare for winning against a very hard deck
            else if (qEvent.getDifficulty().toLowerCase().equals("very hard")) {
                this.awardRandomRare("You've won a random rare for winning against a very hard deck.");
            }

            // Random rare given at 50% chance (65% with luck upgrade)
            if (this.getLuckyCoinResult()) {
                this.awardRandomRare("You've won a random rare.");
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

        // Unlock new sets?
        if (this.wonMatch && qData.getAchievements().getWin() > 1 && ((qData.getAchievements().getWin() + 1) % 50) == 0) {
            unlockSets();
        }

        // Grant booster on a win, or on a loss in easy mode
        if (this.wonMatch || difficulty == 0) {
            final int outcome = this.wonMatch ? qData.getAchievements().getWin() : qData.getAchievements().getLost();
            if ((outcome % Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.WINS_BOOSTER, qData.getAchievements().getDifficulty())) == 0) {
                this.awardBooster();
            }
        }

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

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(
                new QuestWinLoseCardViewer(antesLost), QuestWinLose.CONSTRAINTS_CARDS);
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

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(
                new QuestWinLoseCardViewer(antesWon), QuestWinLose.CONSTRAINTS_CARDS);
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
        if (qData.getAssets().hasItem(QuestItemType.ZEPPELIN)) {
            qData.getAssets().setItemLevel(QuestItemType.ZEPPELIN, 1);
        }

        if (qEvent instanceof QuestEventChallenge) {
            final int id = ((QuestEventChallenge) qEvent).getId();
            final int size = qData.getAchievements().getCurrentChallenges().size();
            for (int i = 0; i < size; i++) {
                if (qData.getAchievements().getCurrentChallenges().get(i) == id) {
                    qData.getAchievements().getCurrentChallenges().remove(i);
                    break;
                }
            }

            if (!((QuestEventChallenge) qEvent).isRepeatable()) {
                qData.getAchievements().addLockedChallenge(((QuestEventChallenge) qEvent).getId());
            }

            // Increment challenge counter to limit challenges available
            qData.getAchievements().addChallengesPlayed();
        }

        CSubmenuDuels.SINGLETON_INSTANCE.update();
        CSubmenuChallenges.SINGLETON_INSTANCE.update();

        qData.setCurrentEvent(null);
        qData.save();
        Singletons.getModel().getQuestPreferences().save();
        Singletons.getModel().getPreferences().writeMatchPreferences();
        Singletons.getModel().getPreferences().save();

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

        credBase += (int) ((base * multiplier) + (Double.parseDouble(Singletons.getModel().getQuestPreferences()
                .getPreference(QPref.REWARDS_WINS_MULTIPLIER)) * qData.getAchievements().getWin()));

        sb.append(diff + " opponent: " + credBase + " credits.<br>");
        // Gameplay bonuses (for each game win)
        boolean hasNeverLost = true;
        
        LobbyPlayer localHuman = Singletons.getControl().getLobby().getQuestPlayer(); 
        for (final GameOutcome game : match.getPlayedGames()) {
            if (!game.isWinner(localHuman)) {
                hasNeverLost = false;
                continue; // no rewards for losing a game
            }
            // Alternate win
            
//            final PlayerStatistics aiRating = game.getStatistics(computer.getName());
            PlayerStatistics humanRating = null;
            for(Entry<LobbyPlayer, PlayerStatistics> kvRating : game ) {
                if( kvRating.getKey().equals(localHuman)) {
                    humanRating = kvRating.getValue();
                    continue;
                }
                
                final PlayerOutcome outcome = kvRating.getValue().getOutcome();
                final GameLossReason whyAiLost = outcome.lossState;
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
                            winConditionName = outcome.loseConditionSpell;
                            break;
                        default:
                            break;
                        }
                    }
    
                    credGameplay += 50;
                    sb.append(String.format("Alternate win condition: <u>%s</u>! " + "Bonus: %d credits.<br>",
                            winConditionName, 50));
                }
            }
            // Mulligan to zero
            final int cntCardsHumanStartedWith = humanRating.getOpeningHandSize();
            final int mulliganReward = Singletons.getModel().getQuestPreferences()
                    .getPreferenceInt(QPref.REWARDS_MULLIGAN0);

            if (0 == cntCardsHumanStartedWith) {
                credGameplay += mulliganReward;
                sb.append(String
                        .format("Mulliganed to zero and still won! " + "Bonus: %d credits.<br>", mulliganReward));
            }

            // Early turn bonus
            final int winTurn = game.getLastTurnNumber();
            final int turnCredits = this.getCreditsRewardForWinByTurn(winTurn);

            if (winTurn == 0) {
                throw new UnsupportedOperationException("QuestWinLose > "
                        + "turn calculation error: Zero turn win");
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
        this.icoTemp = QuestWinLose.getResizedIcon(FSkin.getIcon(FSkin.QuestIcons.ICO_GOLD), 0.5);

        this.lblTemp1 = new TitleLabel("Gameplay Results");

        this.lblTemp2 = new JLabel(sb.toString());
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setFont(FSkin.getFont(14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setIcon(this.icoTemp);
        this.lblTemp2.setIconTextGap(50);

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(this.lblTemp2, QuestWinLose.CONSTRAINTS_TEXT);
    }

    /**
     * Unlock new sets when applicable.
     */
    private void unlockSets() {
        CardEdition unlockedSet = unlockSet();

        if (unlockedSet != null) {

            qData.getFormat().unlockSet(unlockedSet.getCode());

            if (Singletons.getModel().getTournamentPacks().contains(unlockedSet.getCode())) {
                final List<CardPrinted> cardsWon = (new UnOpenedProduct(Singletons.getModel().getTournamentPacks().get(unlockedSet.getCode()))).open();

                // Generate Swing components and attach.
                this.lblTemp1 = new TitleLabel("A starter pack for the *UNLOCKED* " + unlockedSet.getName() + " set!");
                final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

                this.view.getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
                this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);

                qData.getCards().addAllCards(cardsWon);
            }
            else if (Singletons.getModel().getBoosters().contains(unlockedSet.getCode())) {
                for (int i = 0; i < 3; i++) {
                    final List<CardPrinted> cardsWon = (new UnOpenedProduct(Singletons.getModel().getBoosters().get(unlockedSet.getCode()))).open();

                    // Generate Swing components and attach.
                    this.lblTemp1 = new TitleLabel("Booster pack " + (i + 1) + " for the *UNLOCKED* " + unlockedSet.getName() + " set!");
                    final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

                    this.view.getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
                    this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);

                    qData.getCards().addAllCards(cardsWon);
                }
            }
        }
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

        this.view.getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
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

        this.view.getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
    }

    /**
     * <p>
     * awardBooster.
     * </p>
     * Generates and displays booster pack win case.
     * 
     */
    private void awardBooster() {

        List<CardPrinted> cardsWon = null;

        if (qData.getFormat() == null) {
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

            cardsWon = qData.getCards().addCards(selected.getFilterPrinted());

            // Generate Swing components and attach.
            this.lblTemp1 = new TitleLabel("Bonus booster pack from the \"" + selected.getName() + "\" format!");

        } else {
            final List<String> sets = new ArrayList<String>();

            for (BoosterData bd : Singletons.getModel().getBoosters()) {
                if (qData.getFormat().isSetLegal(bd.getEdition())) {
                    sets.add(bd.getEdition());
                }
            }

            List<String> chooseSets = new ArrayList<String>();

            int maxChoices = 1;

            if (this.wonMatch) {
                maxChoices++;
                final int wins = qData.getAchievements().getWin();
                if (wins > 0 && wins % 5 == 0) { maxChoices++; }
                if (wins > 0 && wins % 20 == 0) { maxChoices++; }
                if (wins > 0 && wins % 50 == 0) { maxChoices++; }
            }

            if (sets.size() > maxChoices) {
                if (maxChoices > 1) {
                    Boolean[] choices = new Boolean[sets.size()];
                    for (int i = 0; i < sets.size(); i++) {
                        choices[i] = false;
                    }

                    int toEnable = maxChoices;

                    while (toEnable > 0) {
                        int index = MyRandom.getRandom().nextInt(sets.size());
                        if (!choices[index]) {
                            choices[index] = true;
                            toEnable--;
                        }
                    }

                    for (int i = 0; i < sets.size(); i++) {
                        if (choices[i]) {
                            chooseSets.add(sets.get(i));
                        }
                    }
                } else {
                    chooseSets.add(sets.get(MyRandom.getRandom().nextInt(sets.size())));
                }

            } else {
                chooseSets.addAll(sets);
            }

            final String setPrompt = "Choose bonus booster set:";
            List<CardEdition> chooseEditions = new ArrayList<CardEdition>();
            for (String ed : chooseSets) {
                chooseEditions.add(Singletons.getModel().getEditions().get(ed));
            }
            final CardEdition chooseEd = GuiChoose.one(setPrompt, chooseEditions);

            cardsWon = (new UnOpenedProduct(Singletons.getModel().getBoosters().get(chooseEd.getCode()))).open();
            qData.getCards().addAllCards(cardsWon);
            this.lblTemp1 = new TitleLabel("Bonus " + chooseEd.getName() + " booster pack!");
        }

        if (cardsWon != null) {
            // Generate Swing components and attach.
            final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

            this.view.getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
            this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
        }

    }

    /**
     * Consider unlocking a new expansion in limited quest format.
     *
     * @return CardEdition, the unlocked edition if any.
     */
    private CardEdition unlockSet() {

        if (qData.getFormat() == null || qData.getFormat().getExcludedSetCodes().isEmpty()) {
            return null;
        }

        List<CardEdition> choices = unlockableSets();

        if (choices == null || choices.size() < 1) {
            return null;
        }

        final int unlockSets = JOptionPane.showConfirmDialog(null,
                "You have now won " + (qData.getAchievements().getWin() + 1) + " matches.\n"
                + "With this achievement, you have the option of unlocking more sets. "
                + "You have " + qData.getAssets().getCredits() + " credits.\n\n"
                + "Do you want to unlock new sets?",
                "CONGRATULATIONS!", JOptionPane.YES_NO_OPTION);

        if (unlockSets == JOptionPane.NO_OPTION) {
            return null;
        }


        final ReadPriceList prices = new ReadPriceList();
        final Map<String, Integer> mapPrices = prices.getPriceList();

        List<Long> unlockPrices = new ArrayList<Long>();
        for (int i = 0; i < choices.size(); i++) {
            if (mapPrices.containsKey(choices.get(i).getName() + " Booster Pack")) {
                unlockPrices.add((long) 20 * mapPrices.get(choices.get(i).getName() + " Booster Pack"));
            }
            else {
                unlockPrices.add((long) 8000);
            }
        }

        final String setPrompt = "You have " + qData.getAssets().getCredits() + " credits. Try to unlock:";
        List<String> options = new ArrayList<String>();
        for (int i = 0; i < choices.size(); i++) {
            options.add(choices.get(i).getName() + " [PRICE: " + unlockPrices.get(i) + " credits]");
        }
        options.add("None, thank you.");
        final String choice = GuiChoose.one(setPrompt, options);
        CardEdition chooseEd = null;
        long price = 0;

        /* Examine choice */
        for (int i = 0; i < options.size(); i++) {
            if (choice.equals(options.get(i))) {
                if (i >= choices.size()) {
                    return null;
                }
                chooseEd = choices.get(i);
                price = unlockPrices.get(i);
                break;
            }
        }

        if (qData.getAssets().getCredits() < price) {
            JOptionPane.showMessageDialog(null, "Unfortunately, you cannot afford that set yet.\n"
                    + "To unlock " + chooseEd.getName() + ", you need " + price + " credits.\n"
                    + "You have only " + qData.getAssets().getCredits() + " credits.",
                    "Failed to unlock " + chooseEd.getName(),
                    JOptionPane.PLAIN_MESSAGE);
            return null;
        }

        qData.getAssets().subtractCredits(price);
        JOptionPane.showMessageDialog(null, "You have successfully unlocked " + chooseEd.getName() + "!",
                chooseEd.getName() + " unlocked!",
                JOptionPane.PLAIN_MESSAGE);

        return chooseEd;
    }

    /**
     * Helper function for unlockSet().
     * 
     * @return unmodifiable list, assorted sets that are not currently in the format.
     */
    private List<CardEdition> unlockableSets() {
         if (qData.getFormat() == null || qData.getFormat().getExcludedSetCodes().isEmpty()) {
            return null;
        }

        final int nrChoices = 7;
        List<CardEdition> options = new ArrayList<CardEdition>();

         // Sort current sets by index
         TreeMap<Integer, CardEdition> sortedFormat = new TreeMap<Integer, CardEdition>();
         for (String edCode : qData.getFormat().getAllowedSetCodes()) {
             sortedFormat.put(new Integer(Singletons.getModel().getEditions().get(edCode).getIndex()), Singletons.getModel().getEditions().get(edCode));
         }
         List<CardEdition> currentSets = new ArrayList<CardEdition>(sortedFormat.values());

         // Sort unlockable sets by index
         TreeMap<Integer, CardEdition> sortedExcluded = new TreeMap<Integer, CardEdition>();
         for (String edCode : qData.getFormat().getExcludedSetCodes()) {
             sortedExcluded.put(new Integer(Singletons.getModel().getEditions().get(edCode).getIndex()), Singletons.getModel().getEditions().get(edCode));
         }
         List<CardEdition> excludedSets = new ArrayList<CardEdition>(sortedExcluded.values());

         // Collect 'previous' and 'next' editions
         CardEdition first = currentSets.get(0);
         CardEdition last = currentSets.get(currentSets.size() - 1);
         List<CardEdition> fillers = new ArrayList<CardEdition>();

         for (CardEdition ce : excludedSets) {
             if (first.getIndex() == ce.getIndex() + 1 || last.getIndex() + 1 == ce.getIndex())
             {
                 options.add(ce);
                 // System.out.println("Added adjacent set: " + ce.getName());
             } else if (first.getIndex() == ce.getIndex() + 2 || first.getIndex() == ce.getIndex() + 3
                     || last.getIndex() + 2 == ce.getIndex() || last.getIndex() + 3 == ce.getIndex()
                     || (last.getIndex() > ce.getIndex() && ce.getIndex() > first.getIndex())) {
                 if (MyRandom.getRandom().nextFloat() < 0.6f) {
                     fillers.add(ce);
                     // System.out.println("Added nearby or enclosed set: " + ce.getName());
                 }
             }
         }

         // Look for nearby core sets or block starting sets...
         for (BoosterData bd : Singletons.getModel().getTournamentPacks()) {
             if (qData.getFormat().getExcludedSetCodes().contains(bd.getEdition())
                     && !(fillers.contains(Singletons.getModel().getEditions().get(bd.getEdition())))
                     && !(options.contains(Singletons.getModel().getEditions().get(bd.getEdition())))) {
                 // Set is not yet on any of the lists, see if it is 'close' to any of the sets we currently have
                 CardEdition curEd = Singletons.getModel().getEditions().get(bd.getEdition());
                 int edIdx = curEd.getIndex();
                 for (String cmpCode : qData.getFormat().getAllowedSetCodes()) {
                     int cmpIdx = Singletons.getModel().getEditions().get(cmpCode).getIndex();
                     // Note that we need to check for fillers.contains() again inside this 'for' loop!
                     if (!fillers.contains(curEd) && (cmpIdx == edIdx + 1 || edIdx == cmpIdx + 1)) {
                         fillers.add(curEd);
                         // System.out.println("Added nearby starter/core set " + curEd.getName());
                     }
                     else if (!fillers.contains(curEd) && (cmpIdx == edIdx + 2 || edIdx == cmpIdx + 2) && MyRandom.getRandom().nextFloat() < 0.7f) {
                         fillers.add(curEd);
                         //System.out.println("Added nearby2 starter/core set " + curEd.getName());
                     }
                     else if (!fillers.contains(curEd) && (cmpIdx == edIdx + 3 || edIdx == cmpIdx + 3) && MyRandom.getRandom().nextFloat() < 0.45f) {
                         fillers.add(curEd);
                         // System.out.println("Added nearby3 starter/core set " + curEd.getName());
                     }
                 }
             }
         }

         // Fill in the in-between sets...
         int j = 0;
         // Find the first excluded set between current sets first and current sets last
         while (j < excludedSets.size() && excludedSets.get(j).getIndex() < currentSets.get(0).getIndex()) {
             j++;
         }
         // Consider all sets until current sets last
         while (j < excludedSets.size() && excludedSets.get(j).getIndex() < currentSets.get(currentSets.size() - 1).getIndex()) {
             if (!options.contains(excludedSets.get(j)) && !fillers.contains(excludedSets.get(j))) {
                 // System.out.println("Added in-between set " + excludedSets.get(j).getCode());
                 fillers.add(excludedSets.get(j));
             }
             j++;
         }

         if (fillers.size() + options.size() < nrChoices && excludedSets.size() > fillers.size() + options.size()) {
             if (excludedSets.size() == 1 + fillers.size() + options.size()) {
                 // Only one set exists that isn't on the list yet
                 for (CardEdition ce : excludedSets) {
                     if (!fillers.contains(ce) && !options.contains(ce)) {
                         fillers.add(ce);
                         break;
                     }
                 }
             } else {
                 while (fillers.size() + options.size() < nrChoices) {
                     CardEdition ce = excludedSets.get(MyRandom.getRandom().nextInt(excludedSets.size()));
                     if (!fillers.contains(ce) && !options.contains(ce)) {
                         fillers.add(ce);
                         // System.out.println("Randomly padded with set: " + ce.getName());
                     }
             }
          }
         }

         for (int i = 0; (options.size() < nrChoices) && i < fillers.size(); i++) {
             options.add(fillers.get(i));
             // System.out.println("Padded with: " + fillers.get(i).getName());
         }

         return Collections.unmodifiableList(options);
    }


    /**
     * <p>
     * awardChallengeWin.
     * </p>
     * Generates and displays win case for challenge event.
     * 
     */
    private void awardChallengeWin() {
        final List<CardPrinted> cardsWon = ((QuestEventChallenge) qEvent).getCardRewardList();
        final long questRewardCredits = ((QuestEventChallenge) qEvent).getCreditsReward();

        final StringBuilder sb = new StringBuilder();
        sb.append("<html>Challenge completed.<br><br>");
        sb.append("Challenge bounty: <b>" + questRewardCredits + " credits.</b></html>");

        qData.getAssets().addCredits(questRewardCredits);

        // Generate Swing components and attach.
        this.icoTemp = QuestWinLose.getResizedIcon(FSkin.getIcon(FSkin.QuestIcons.ICO_BOX), 0.5);
        this.lblTemp1 = new TitleLabel("Challenge Rewards for \"" + ((QuestEventChallenge) qEvent).getTitle() + "\"");

        this.lblTemp2 = new JLabel(sb.toString());
        this.lblTemp2.setFont(FSkin.getFont(14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setIconTextGap(50);
        this.lblTemp2.setIcon(this.icoTemp);

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(this.lblTemp2, QuestWinLose.CONSTRAINTS_TEXT);

        if (cardsWon != null) {
            final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);
            this.getView().getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
            qData.getCards().addAllCards(cardsWon);
        }
    }

    private void penalizeLoss() {
        final int x = Singletons.getModel().getQuestPreferences().getPreferenceInt(QPref.PENALTY_LOSS);
        this.icoTemp = QuestWinLose.getResizedIcon(FSkin.getIcon(FSkin.QuestIcons.ICO_HEART), 0.5);

        this.lblTemp1 = new TitleLabel("Gameplay Results");

        this.lblTemp2 = new JLabel("You lose! You have lost " + x + " credits.");
        this.lblTemp2.setFont(FSkin.getFont(14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setIconTextGap(50);
        this.lblTemp2.setIcon(this.icoTemp);

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(this.lblTemp2, QuestWinLose.CONSTRAINTS_TEXT);
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
        QuestPreferences qp = Singletons.getModel().getQuestPreferences(); 
        if ( null == whyAiLost) // Felidar, Helix Pinnacle, etc.
            return qp.getPreferenceInt(QPref.REWARDS_UNDEFEATED);
        switch (whyAiLost) {
        case LifeReachedZero:
            return 0; // nothing special here, ordinary kill
        case Milled:
            return qp.getPreferenceInt(QPref.REWARDS_MILLED);
        case Poisoned:
            return qp.getPreferenceInt(QPref.REWARDS_POISON);
        case SpellEffect: // Door to Nothingness, etc.
            return qp.getPreferenceInt(QPref.REWARDS_UNDEFEATED);
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
     * <p>
     * getResizedIcon.
     * </p>
     * 
     * @param icon
     *            ImageIcon
     * @param scale
     *            Double
     * @return {@link javax.swing.ImageIcon} object
     */
    public static ImageIcon getResizedIcon(final ImageIcon icon, final double scale) {
        final int w = (int) (icon.getIconWidth() * scale);
        final int h = (int) (icon.getIconHeight() * scale);

        return new ImageIcon(icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH));
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
