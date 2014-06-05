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
package forge.screens.match;

import forge.GuiBase;
import forge.LobbyPlayer;
import forge.Singletons;
import forge.assets.FSkinProp;
import forge.card.CardEdition;
import forge.card.IUnOpenedProduct;
import forge.card.UnOpenedProduct;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameFormat;
import forge.game.GameOutcome;
import forge.game.player.*;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.gui.framework.FScreen;
import forge.item.*;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.*;
import forge.quest.bazaar.QuestItemType;
import forge.quest.data.QuestPreferences;
import forge.quest.data.QuestPreferences.DifficultyPrefs;
import forge.quest.data.QuestPreferences.QPref;
import forge.screens.home.quest.CSubmenuChallenges;
import forge.screens.home.quest.CSubmenuDuels;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinIcon;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.util.MyRandom;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

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
    private transient SkinIcon icoTemp;
    private transient TitleLabel lblTemp1;
    private transient SkinnedLabel lblTemp2;
    private final transient boolean isAnte;

    /** String constraint parameters for title blocks and cardviewer blocks. */
    private static final String CONSTRAINTS_TITLE = "w 95%!, gap 0 0 20px 10px";
    private static final String CONSTRAINTS_TEXT = "w 95%!, h 220px!, gap 0 0 0 20px";
    private static final String CONSTRAINTS_CARDS = "w 95%!, h 330px!, gap 0 0 0 20px";
    private static final String CONSTRAINTS_CARDS_LARGE = "w 95%!, h 600px!, gap 0 0 0 20px";

    private final transient QuestController qData;
    private final transient QuestEvent qEvent;

    /**
     * Instantiates a new quest win lose handler.
     * 
     * @param view0 ViewWinLose object
     * @param match2
     */
    public QuestWinLose(final ViewWinLose view0, Game lastGame) {
        super(view0, lastGame);
        this.view = view0;
        qData = FModel.getQuest();
        qEvent = qData.getCurrentEvent();
        this.wonMatch = lastGame.getMatch().isWonBy(GuiBase.getInterface().getQuestPlayer());
        this.isAnte = FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE);
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
        QuestController qc = FModel.getQuest();

        // After the first game, reset the card shop pool to be able to buy back anted cards
        if (lastGame.getMatch().getPlayedGames().size() == 1) {
            qc.getCards().clearShopList();
            qc.getCards().getShopList();
        }

        final LobbyPlayer questLobbyPlayer = GuiBase.getInterface().getQuestPlayer();
        Player questPlayer = null;
        for (Player p : lastGame.getRegisteredPlayers()) {
            if (p.getLobbyPlayer().equals(questLobbyPlayer)) {
                questPlayer = p;
            }
        }
        if (isAnte) {
            //do per-game actions
            GameOutcome outcome = lastGame.getOutcome();

            // Won/lost cards should already be calculated (even in a draw)
            GameOutcome.AnteResult anteResult = outcome.anteResult.get(questPlayer);
            if (anteResult != null) {
                if (anteResult.wonCards != null)
                    qc.getCards().addAllCards(anteResult.wonCards);
                if (anteResult.lostCards != null)
                    qc.getCards().loseCards(anteResult.lostCards);
                this.anteReport(anteResult.wonCards, anteResult.lostCards, questPlayer.equals(outcome.getWinningPlayer()));
            }
        }

        if (!lastGame.getMatch().isMatchOver()) {
            this.getView().getBtnQuit().setText("Quit (-15 Credits)");
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


        final int wins = qData.getAchievements().getWin();
        // Win case
        if (this.wonMatch) {
            // Standard event reward credits
            this.awardEventCredits();

            // Challenge reward credits
            if (qEvent instanceof QuestEventChallenge) {
                this.awardChallengeWin();
            }

            else {
                awardSpecialReward("Special bonus reward:"); // If any
                // Random rare for winning against a very hard deck
                if (qEvent.getDifficulty() == QuestEventDifficulty.EXPERT) {
                    this.awardRandomRare("You've won a random rare for winning against a very hard deck.");
                }
            }
            
            this.awardWinStreakBonus();

            // Random rare given at 50% chance (65% with luck upgrade)
            if (this.getLuckyCoinResult()) {
                this.awardRandomRare("You've won a random rare.");
            }

            // Award jackpot every 80 games won (currently 10 rares)

            if ((wins > 0) && (((wins + 1) % 80) == 0)) {
                this.awardJackpot();
            }

        }
        // Lose case
        else {
            this.penalizeLoss();
        }

        // Grant booster on a win, or on a loss in easy mode
        if (this.wonMatch || difficulty == 0) {
            final int outcome = this.wonMatch ? wins : qData.getAchievements().getLost();
            int winsPerBooster = FModel.getQuestPreferences().getPrefInt(DifficultyPrefs.WINS_BOOSTER, qData.getAchievements().getDifficulty());
            if (winsPerBooster > 0 && (outcome + 1) % winsPerBooster == 0) {
                this.awardBooster();
            }
        }

        return true;
    }

    private void anteReport(final List<PaperCard> cardsWon, List<PaperCard> cardsLost, boolean hasWon) {
        // Generate Swing components and attach.
        if (cardsWon != null && !cardsWon.isEmpty()) {
            this.getView().getPnlCustom().add(new TitleLabel("Spoils! These cards will be available in your card pool after this ante match:"),
                    QuestWinLose.CONSTRAINTS_TITLE);
            if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
                this.getView().getPnlCustom().add(new QuestWinLoseCardViewer(cardsWon), QuestWinLose.CONSTRAINTS_CARDS_LARGE);
            } else {
                this.getView().getPnlCustom().add(new QuestWinLoseCardViewer(cardsWon), QuestWinLose.CONSTRAINTS_CARDS);
            }
        }
        if (cardsLost != null && !cardsLost.isEmpty()) {
            this.getView().getPnlCustom().add(new TitleLabel("Looted! You lost the following cards in an ante match:"),
                    QuestWinLose.CONSTRAINTS_TITLE);
            if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
                this.getView().getPnlCustom().add(new QuestWinLoseCardViewer(cardsLost), QuestWinLose.CONSTRAINTS_CARDS_LARGE);
            } else {
                this.getView().getPnlCustom().add(new QuestWinLoseCardViewer(cardsLost), QuestWinLose.CONSTRAINTS_CARDS);
            }
        }

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
        final int x = FModel.getQuestPreferences().getPrefInt(QPref.PENALTY_LOSS);

        // Record win/loss in quest data
        if (this.wonMatch) {
            qData.getAchievements().addWin();
        } else {
            qData.getAchievements().addLost();
            qData.getAssets().subtractCredits(x);
        }

        // Reset cards and zeppelin use
        if (qData.getAssets().hasItem(QuestItemType.ZEPPELIN)) {
            qData.getAssets().setItemLevel(QuestItemType.ZEPPELIN, 1);
        }

        if (qEvent instanceof QuestEventChallenge) {
            final String id = ((QuestEventChallenge) qEvent).getId();
            qData.getAchievements().getCurrentChallenges().remove(id);
            qData.getAchievements().addLockedChallenge(id);

            // Increment challenge counter to limit challenges available
            qData.getAchievements().addChallengesPlayed();
        }

        CSubmenuDuels.SINGLETON_INSTANCE.update();
        CSubmenuChallenges.SINGLETON_INSTANCE.update();

        qData.setCurrentEvent(null);
        qData.save();
        FModel.getQuestPreferences().save();
        Singletons.getControl().writeMatchPreferences();

        Singletons.getControl().endCurrentGame();
        Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);

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
        final int base = FModel.getQuestPreferences().getPrefInt(QPref.REWARDS_BASE);
        double multiplier = 1;


        switch(qEvent.getDifficulty()) {
            case EASY: multiplier = 1; break;
            case MEDIUM: multiplier = 1.5; break;
            case HARD: multiplier = 2; break;
            case EXPERT: multiplier = 3; break;
        }

        credBase = (int) (base * multiplier);
        
        sb.append(StringUtils.capitalize(qEvent.getDifficulty().getTitle()));
        sb.append(" opponent: ").append(credBase).append(" credits.<br>");

        int creditsForPreviousWins = (int) ((Double.parseDouble(FModel.getQuestPreferences()
                .getPref(QPref.REWARDS_WINS_MULTIPLIER)) * qData.getAchievements().getWin()));
        credBase += creditsForPreviousWins;
        
        sb.append("Bonus for previous wins: ").append(creditsForPreviousWins).append(
                  creditsForPreviousWins != 1 ? " credits.<br>" : " credit.<br>");
        
        // Gameplay bonuses (for each game win)
        boolean hasNeverLost = true;
        int lifeDifferenceCredits = 0;
        
        LobbyPlayer localHuman = GuiBase.getInterface().getQuestPlayer();
        for (final GameOutcome game : lastGame.getMatch().getPlayedGames()) {
            if (!game.isWinner(localHuman)) {
                hasNeverLost = false;
                continue; // no rewards for losing a game
            }
            // Alternate win

            // final PlayerStatistics aiRating = game.getStatistics(computer.getName());
            PlayerStatistics humanRating = null;
            for (Entry<LobbyPlayer, PlayerStatistics> kvRating : game) {
                if (kvRating.getKey().equals(localHuman)) {
                    humanRating = kvRating.getValue();
                    continue;
                }

                final PlayerOutcome outcome = kvRating.getValue().getOutcome();
                final GameLossReason whyAiLost = outcome.lossState;
                int altReward = this.getCreditsRewardForAltWin(whyAiLost);

                String winConditionName = "Unknown (bug)";
                if (game.getWinCondition() == GameEndReason.WinsGameSpellEffect) {
                    winConditionName = game.getWinSpellEffect();
                    altReward = this.getCreditsRewardForAltWin(null);
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

                if (altReward > 0) {
                    credGameplay += altReward;
                    sb.append(String.format("Alternate win condition: <u>%s</u>! Bonus: %d credits.<br>",
                            winConditionName, altReward));
                }
            }
            // Mulligan to zero
            final int cntCardsHumanStartedWith = humanRating.getOpeningHandSize();
            final int mulliganReward = FModel.getQuestPreferences().getPrefInt(QPref.REWARDS_MULLIGAN0);

            if (0 == cntCardsHumanStartedWith) {
                credGameplay += mulliganReward;
                sb.append(String.format("Mulliganed to zero and still won! Bonus: %d credits.<br>", mulliganReward));
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
            
            if (game.getLifeDelta() >= 50) {
                lifeDifferenceCredits += Math.max(Math.min((game.getLifeDelta() - 46) / 4, 750), 0);
            }
            
        } // End for(game)
        
        if (lifeDifferenceCredits > 0) {
            sb.append(String.format("Life total difference: %d credits.<br>", lifeDifferenceCredits));
        }

        // Undefeated bonus
        if (hasNeverLost) {
            credUndefeated += FModel.getQuestPreferences().getPrefInt(QPref.REWARDS_UNDEFEATED);
            final int reward = FModel.getQuestPreferences().getPrefInt(QPref.REWARDS_UNDEFEATED);
            sb.append(String.format("You have not lost once! Bonus: %d credits.<br>", reward));
        }

        // Estates bonus
        credTotal = credBase + credGameplay + credUndefeated + lifeDifferenceCredits;
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
        this.icoTemp = FSkin.getIcon(FSkinProp.ICO_QUEST_GOLD);

        this.lblTemp1 = new TitleLabel("Gameplay Results");

        this.lblTemp2 = new SkinnedLabel(sb.toString());
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setFont(FSkin.getFont(14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setIcon(this.icoTemp);
        this.lblTemp2.setIconTextGap(50);

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(this.lblTemp2, QuestWinLose.CONSTRAINTS_TEXT);
    }

    /**
     * <p>
     * awardRandomRare.
     * </p>
     * Generates and displays a random rare win case.
     * 
     */
    private void awardRandomRare(final String message) {
        final PaperCard c = qData.getCards().addRandomRare();
        final List<PaperCard> cardsWon = new ArrayList<PaperCard>();
        cardsWon.add(c);

        // Generate Swing components and attach.
        this.lblTemp1 = new TitleLabel(message);

        final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

        this.view.getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
            this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS_LARGE);
        } else {
            this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
        }
    }
    
    /**
     * <p>
     * awardWinStreakBonus.
     * </p>
     * Generates and displays a reward for maintaining a win streak.
     * 
     */
    private void awardWinStreakBonus() {
        
        int currentStreak = (qData.getAchievements().getWinStreakCurrent() + 1) % 50;

        final List<PaperCard> cardsWon = new ArrayList<>();
        List<PaperCard> cardsToAdd;
        String typeWon = "";
        boolean addDraftToken = false;
        
        switch (currentStreak) {
            case 3:
                cardsWon.addAll(qData.getCards().addRandomCommon(1));
                typeWon = "common";
                break;
            case 5:
                cardsWon.addAll(qData.getCards().addRandomUncommon(1));
                typeWon = "uncommon";
                break;
            case 7:
                cardsWon.addAll(qData.getCards().addRandomRareNotMythic(1));
                typeWon = "rare";
                break;
            case 10:
                cardsToAdd = qData.getCards().addRandomMythicRare(1);
                if (cardsToAdd != null) {
                    cardsWon.addAll(cardsToAdd);
                    typeWon = "mythic rare";
                } else {
                    cardsWon.addAll(qData.getCards().addRandomRareNotMythic(5));
                    typeWon = "rare";
                }
                break;
            case 25:
                cardsToAdd = qData.getCards().addRandomMythicRare(5);
                if (cardsToAdd != null) {
                    cardsWon.addAll(cardsToAdd);
                    typeWon = "mythic rare";
                } else {
                    cardsWon.addAll(qData.getCards().addRandomRareNotMythic(15));
                    typeWon = "rare";
                }
                addDraftToken = true;
                break;
            case 0: //The 50th win in the streak is 0, since (50 % 50 == 0)
                cardsToAdd = qData.getCards().addRandomMythicRare(10);
                if (cardsToAdd != null) {
                    cardsWon.addAll(cardsToAdd);
                    typeWon = "mythic rare";
                } else {
                    cardsWon.addAll(qData.getCards().addRandomRareNotMythic(25));
                    typeWon = "rare";
                }
                addDraftToken = true;
                break;
            default:
                return;
        }
        
        if (addDraftToken) {
            TitleLabel title = new TitleLabel("Bonus Draft Token Reward");
            SkinnedLabel contents = new SkinnedLabel("<html><body>For achieving a 25 win streak, you have been awarded a draft token!<br>Use these tokens to generate new tournaments.</body></html>");
            contents.setHorizontalAlignment(SwingConstants.CENTER);
            contents.setFont(FSkin.getFont(14));
            contents.setForeground(Color.white);
            contents.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_COIN));
            contents.setIconTextGap(50);
            this.view.getPnlCustom().add(title, QuestWinLose.CONSTRAINTS_TITLE);
            this.view.getPnlCustom().add(contents, "w 95%!, h 100px!, gap 0 0 0 20px");
            qData.getAchievements().addDraftToken();
        }
        
        if (cardsWon.size() > 0) {
            this.lblTemp1 = new TitleLabel("You have achieved a " + (currentStreak == 0 ? "50" : currentStreak) + " win streak and won " + cardsWon.size() + " " + typeWon + " card" + ((cardsWon.size() != 1) ? "s" : "") + "!");
            final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);
    
            this.view.getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
            if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
                this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS_LARGE);
            } else {
                this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
            }
        }
        
    }

    /**
     * <p>
     * awardJackpot.
     * </p>
     * Generates and displays jackpot win case.
     * 
     */
    private void awardJackpot() {
        final List<PaperCard> cardsWon = qData.getCards().addRandomRare(10);

        // Generate Swing components and attach.
        this.lblTemp1 = new TitleLabel("You just won 10 random rares!");
        final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

        this.view.getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
            this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS_LARGE);
        } else {
            this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
        }
    }

    /**
     * <p>
     * awardBooster.
     * </p>
     * Generates and displays booster pack win case.
     * 
     */
    private void awardBooster() {

        List<PaperCard> cardsWon = null;

        if (qData.getFormat() == null) {
            final List<GameFormat> formats = new ArrayList<GameFormat>();
            String preferredFormat = FModel.getQuestPreferences().getPref(QPref.BOOSTER_FORMAT);

            GameFormat pref = null;
            for (GameFormat f : FModel.getFormats()) {
                formats.add(f);
                if (f.toString().equals(preferredFormat)) {
                    pref = f;
                }
            }

            Collections.sort(formats);

            final GameFormat selected = GuiChoose.getChoices("Choose bonus booster format", 1, 1, formats, pref, null).get(0); //ch.getSelectedValue();
            FModel.getQuestPreferences().setPref(QPref.BOOSTER_FORMAT, selected.toString());

            cardsWon = qData.getCards().generateQuestBooster(selected.getFilterPrinted());
            qData.getCards().addAllCards(cardsWon);

            // Generate Swing components and attach.
            this.lblTemp1 = new TitleLabel("Bonus booster pack from the \"" + selected.getName() + "\" format!");

        } else {
            final List<String> sets = new ArrayList<String>();

            for (SealedProduct.Template bd : FModel.getMagicDb().getBoosters()) {
                if (bd != null && qData.getFormat().isSetLegal(bd.getEdition())) {
                    sets.add(bd.getEdition());
                }
            }

            int maxChoices = 1;
            if (this.wonMatch) {
                maxChoices++;
                final int wins = qData.getAchievements().getWin();
                if ((wins + 1) % 5 == 0) { maxChoices++; }
                if ((wins + 1) % 20 == 0) { maxChoices++; }
                if ((wins + 1) % 50 == 0) { maxChoices++; }
                maxChoices += qData.getAssets().getItemLevel(QuestItemType.MEMBERSHIP_TOKEN);
            }

            List<CardEdition> options = new ArrayList<CardEdition>();
            
            while(!sets.isEmpty() && maxChoices > 0) {
                int ix = MyRandom.getRandom().nextInt(sets.size());
                String set = sets.get(ix);
                sets.remove(ix);
                options.add(FModel.getMagicDb().getEditions().get(set));
                maxChoices--;
            }

            final CardEdition chooseEd = GuiChoose.one("Choose bonus booster set:", options);

            IUnOpenedProduct product = new UnOpenedProduct(FModel.getMagicDb().getBoosters().get(chooseEd.getCode()));
            cardsWon = product.get();
            qData.getCards().addAllCards(cardsWon);
            this.lblTemp1 = new TitleLabel("Bonus " + chooseEd.getName() + " booster pack!");
        }

        if (cardsWon != null) {
            // Generate Swing components and attach.
            final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);

            this.view.getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
            if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
                this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS_LARGE);
            } else {
                this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
            }
        }

    }

    /**
     * <p>
     * awardChallengeWin.
     * </p>
     * Generates and displays win case for challenge event.
     * 
     */
    private void awardChallengeWin() {

        final long questRewardCredits = ((QuestEventChallenge) qEvent).getCreditsReward();

        final StringBuilder sb = new StringBuilder();
        sb.append("<html>Challenge completed.<br><br>");
        sb.append("Challenge bounty: <b>" + questRewardCredits + " credits.</b></html>");

        qData.getAssets().addCredits(questRewardCredits);

        // Generate Swing components and attach.
        this.icoTemp = FSkin.getIcon(FSkinProp.ICO_QUEST_BOX).scale(0.5);
        this.lblTemp1 = new TitleLabel("Challenge Rewards for \"" + ((QuestEventChallenge) qEvent).getTitle() + "\"");

        this.lblTemp2 = new SkinnedLabel(sb.toString());
        this.lblTemp2.setFont(FSkin.getFont(14));
        this.lblTemp2.setForeground(Color.white);
        this.lblTemp2.setHorizontalAlignment(SwingConstants.CENTER);
        this.lblTemp2.setIconTextGap(50);
        this.lblTemp2.setIcon(this.icoTemp);

        this.getView().getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
        this.getView().getPnlCustom().add(this.lblTemp2, QuestWinLose.CONSTRAINTS_TEXT);

        awardSpecialReward(null);
    }

    /**
     * <p>
     * awardSpecialReward.
     * </p>
     * This builds the card reward based on the string data.
     * @param message String, reward text to be displayed, if any
     */
    private void awardSpecialReward(final String message) {
        final List<InventoryItem> itemsWon = ((QuestEvent) qEvent).getCardRewardList();

        if (itemsWon == null || itemsWon.isEmpty()) {
            return;
        }

        final List<PaperCard> cardsWon = new ArrayList<PaperCard>();

        for (InventoryItem ii : itemsWon) {
            if (ii instanceof PaperCard) {
                cardsWon.add((PaperCard) ii);
            } else if (ii instanceof TournamentPack || ii instanceof BoosterPack) {
                List<PaperCard> boosterCards = new ArrayList<PaperCard>();
                SealedProduct booster = null;
                if (ii instanceof BoosterPack) {
                    booster = (BoosterPack) ((BoosterPack) ii).clone();
                    boosterCards.addAll(booster.getCards());
                } else if (ii instanceof TournamentPack) {
                    booster = (TournamentPack) ((TournamentPack) ii).clone();
                    boosterCards.addAll(booster.getCards());
                }
                if (!boosterCards.isEmpty()) {
                    qData.getCards().addAllCards(boosterCards);
                    final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(boosterCards);
                    this.view.getPnlCustom().add(new TitleLabel("Extra " + ii.getName() + "!"), QuestWinLose.CONSTRAINTS_TITLE);
                    if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
                        this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS_LARGE);
                    } else {
                        this.view.getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
                    }
                }
            }
            else if (ii instanceof IQuestRewardCard) {
                final List<PaperCard> cardChoices = ((IQuestRewardCard) ii).getChoices();
                final PaperCard chosenCard = (null == cardChoices ? null : GuiChoose.one("Choose " + ((IQuestRewardCard) ii).getName() + ":", cardChoices));
                if (null != chosenCard) {
                    cardsWon.add(chosenCard);
                }
            }
        }
        if (cardsWon != null && !cardsWon.isEmpty()) {
            final QuestWinLoseCardViewer cv = new QuestWinLoseCardViewer(cardsWon);
            if (message != null) {
                this.lblTemp1 = new TitleLabel(message);
                this.view.getPnlCustom().add(this.lblTemp1, QuestWinLose.CONSTRAINTS_TITLE);
            }
            if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
                this.getView().getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS_LARGE);
            } else {
                this.getView().getPnlCustom().add(cv, QuestWinLose.CONSTRAINTS_CARDS);
            }
            qData.getCards().addAllCards(cardsWon);
        }
    }

    private void penalizeLoss() {
        final int x = FModel.getQuestPreferences().getPrefInt(QPref.PENALTY_LOSS);
        this.icoTemp = FSkin.getIcon(FSkinProp.ICO_QUEST_HEART).scale(0.5);

        this.lblTemp1 = new TitleLabel("Gameplay Results");

        this.lblTemp2 = new SkinnedLabel("You lose! You have lost " + x + " credits.");
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
        QuestPreferences qp = FModel.getQuestPreferences();
        if (null == whyAiLost) {
            // Felidar, Helix Pinnacle, etc.
            return qp.getPrefInt(QPref.REWARDS_ALTERNATIVE);
        }
        switch (whyAiLost) {
        case LifeReachedZero:
            return 0; // nothing special here, ordinary kill
        case Milled:
            return qp.getPrefInt(QPref.REWARDS_MILLED);
        case Poisoned:
            return qp.getPrefInt(QPref.REWARDS_POISON);
        case SpellEffect: // Door to Nothingness, etc.
            return qp.getPrefInt(QPref.REWARDS_ALTERNATIVE);
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
            credits = FModel.getQuestPreferences().getPrefInt(QPref.REWARDS_TURN1);
        } else if (iTurn <= 5) {
            credits = FModel.getQuestPreferences().getPrefInt(QPref.REWARDS_TURN5);
        } else if (iTurn <= 10) {
            credits = FModel.getQuestPreferences().getPrefInt(QPref.REWARDS_TURN10);
        } else if (iTurn <= 15) {
            credits = FModel.getQuestPreferences().getPrefInt(QPref.REWARDS_TURN15);
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
    private class TitleLabel extends SkinnedLabel {
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
