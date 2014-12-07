/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.planarconquest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.eventbus.Subscribe;

import forge.FThreads;
import forge.LobbyPlayer;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.Match;
import forge.game.event.GameEvent;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.IButton;
import forge.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.match.MatchUtil;
import forge.model.FModel;
import forge.planarconquest.ConquestPlaneData.RegionData;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.player.GamePlayerUtil;
import forge.properties.ForgePreferences.FPref;
import forge.quest.BoosterUtils;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;
import forge.util.storage.IStorage;

public class ConquestController {
    private static final int STARTING_LIFE = 30;

    private ConquestData model;
    private CardPool cardPool;
    private transient IStorage<Deck> decks;
    private transient GameRunner gameRunner;

    public ConquestController() {
    }

    public String getName() {
        return model == null ? null : model.getName();
    }

    public ConquestData getModel() {
        return model;
    }

    public CardPool getCardPool() {
        return cardPool;
    }

    public IStorage<Deck> getDecks() {
        return decks;
    }

    public void load(final ConquestData model0) {
        model = model0;
        cardPool = model == null ? null : model.getCollection();
        decks = model == null ? null : model.getDeckStorage();
    }

    public void save() {
        if (model != null) {
            model.saveData();
        }
    }

    @Subscribe
    public void receiveGameEvent(GameEvent ev) { // Receives events only during planar conquest games

    }

    public void endDay(final IVCommandCenter commandCenter) {
        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                //prompt user if any commander hasn't taken an action
                final List<ConquestCommander> commanders = model.getCurrentPlaneData().getCommanders();
                for (ConquestCommander commander : commanders) {
                    if (commander.getCurrentDayAction() == null) {
                        if (!SOptionPane.showConfirmDialog(commander.getName() + " has not taken an action today. End day anyway?", "Action Not Taken", "End Day", "Cancel")) {
                            return;
                        }
                    }
                }
                //perform all commander actions
                for (ConquestCommander commander : commanders) {
                    switch (commander.getCurrentDayAction()) {
                    case Attack1:
                        playGame(commander, 0, false, commandCenter);
                        break;
                    case Attack2:
                        playGame(commander, 1, false, commandCenter);
                        break;
                    case Attack3:
                        playGame(commander, 2, false, commandCenter);
                        break;
                    case Defend:
                        playGame(commander, Aggregates.randomInt(0, 2), true, commandCenter); //defend against random opponent
                        break;
                    case Recruit:
                        if (!recruit(commander)) { return; }
                        break;
                    case Study:
                        if (!study(commander)) { return; }
                        break;
                    case Undeploy:
                        model.getCurrentPlaneData().getRegionData(commander.getDeployedRegion()).setDeployedCommander(null);
                        break;
                    default: //remaining actions don't need to do anything more
                        break;
                    }
                }
                //increment day and reset actions, then update UI for new day
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        model.incrementDay();
                        for (ConquestCommander commander : commanders) {
                            commander.setCurrentDayAction(null);
                        }
                        commandCenter.updateCurrentDay();
                    }
                });
            }
        });
    }

    private void playGame(final ConquestCommander commander, final int opponentIndex, final boolean isHumanDefending, final IVCommandCenter commandCenter) {
        gameRunner = new GameRunner(commander, opponentIndex, isHumanDefending, commandCenter);
        gameRunner.invokeAndWait();

        //after game finished
        if (gameRunner.wonGame) {
            RegionData regionData = model.getCurrentPlaneData().getRegionData(commander.getDeployedRegion());
            regionData.replaceOpponent(opponentIndex);
        }
        gameRunner = null;
    }

    public class GameRunner {
        private class Lock {
        }
        private final Lock lock = new Lock();

        public final ConquestCommander commander;
        public final ConquestCommander opponent;
        public final boolean isHumanDefending;
        private final IVCommandCenter commandCenter;
        private boolean wonGame;

        private GameRunner(final ConquestCommander commander0, final int opponentIndex, final boolean isHumanDefending0, final IVCommandCenter commandCenter0) {
            commander = commander0;
            RegionData regionData = model.getCurrentPlaneData().getRegionData(commander.getDeployedRegion());
            opponent = regionData.getOpponent(opponentIndex);
            isHumanDefending = isHumanDefending0;
            commandCenter = commandCenter0;
        }

        public final void invokeAndWait() {
            FThreads.assertExecutedByEdt(false); //not supported if on UI thread
            FThreads.invokeInEdtLater(new Runnable() {
                @Override
                public void run() {
                    commandCenter.startGame(GameRunner.this);
                }
            });
            try {
                synchronized(lock) {
                    lock.wait();
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void finishStartingGame() {
            RegisteredPlayer humanStart = new RegisteredPlayer(commander.getDeck());
            RegisteredPlayer aiStart = new RegisteredPlayer(opponent.getDeck());

            humanStart.setStartingLife(STARTING_LIFE + (isHumanDefending ? FModel.getConquestPreferences().getPrefInt(CQPref.DEFEND_BONUS_LIFE) : 0));
            aiStart.setStartingLife(STARTING_LIFE);

            List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
            starter.add(humanStart.setPlayer(getConquestPlayer()));

            LobbyPlayer aiPlayer = GamePlayerUtil.createAiPlayer();
            starter.add(aiStart.setPlayer(aiPlayer));

            boolean useRandomFoil = FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL);
            for(RegisteredPlayer rp : starter) {
                rp.setRandomFoil(useRandomFoil);
            }
            GameRules rules = new GameRules(GameType.PlanarConquest);
            rules.setGamesPerMatch(1); //only play one game at a time
            rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
            rules.canCloneUseTargetsImage = FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE);
            final Match mc = new Match(rules, starter);
            FThreads.invokeInEdtNowOrLater(new Runnable(){
                @Override
                public void run() {
                    MatchUtil.startGame(mc);
                }
            });
        }

        private void finish() {
            synchronized(lock) { //release game lock once game finished
                lock.notify();
            }
        }
    }

    private boolean recruit(ConquestCommander commander) {
        boolean bonusCard = Aggregates.randomInt(1, 100) <= FModel.getConquestPreferences().getPrefInt(CQPref.RECRUIT_BONUS_CARD_ODDS);
        return awardNewCards(commander.getDeployedRegion().getCardPool().getAllCards(),
                commander.getName() + " recruited", "new creature", null, null,
                CardRulesPredicates.Presets.IS_CREATURE, bonusCard ? 2 : 1);
    }

    private boolean study(ConquestCommander commander) {
        boolean bonusCard = Aggregates.randomInt(1, 100) <= FModel.getConquestPreferences().getPrefInt(CQPref.STUDY_BONUS_CARD_ODDS);
        return awardNewCards(commander.getDeployedRegion().getCardPool().getAllCards(),
                commander.getName() + " unlocked", "new spell", null, null,
                CardRulesPredicates.Presets.IS_NON_CREATURE_SPELL, bonusCard ? 2 : 1);
    }

    private LobbyPlayer getConquestPlayer() {
        return GamePlayerUtil.getGuiPlayer(); //TODO: Should this be a separate player?
    }

    public void showGameRewards(final GameView game, final IWinLoseView<? extends IButton> view) {
        if (game.isMatchWonBy(getConquestPlayer())) {
            view.getBtnQuit().setText("Great!");

            //give controller a chance to run remaining logic on a separate thread
            view.showRewards(new Runnable() {
                @Override
                public void run() {
                    awardWinStreakBonus(view);

                    String[] options = {"Booster", "Card"};
                    if (SOptionPane.showOptionDialog("Choose one \u2014\n\n" +
                            "\u2022 Open a random booster pack\n" +
                            "\u2022 Choose a card from your opponent's deck",
                            "Spoils of Victory", null, options) == 0) {
                        awardBooster(view);
                    }
                    else {
                        List<PaperCard> cards = new ArrayList<PaperCard>();
                        for (Entry<PaperCard, Integer> entry : gameRunner.opponent.getDeck().getMain()) {
                            PaperCard c = entry.getKey();
                            if (!c.getRules().getType().isBasicLand() && !getModel().getCollection().contains(c)) {
                                cards.add(c);
                            }
                        }
                        if (cards.isEmpty()) { //if there are no new cards, show all cards
                            for (Entry<PaperCard, Integer> entry : gameRunner.opponent.getDeck().getMain()) {
                                PaperCard c = entry.getKey();
                                if (!c.getRules().getType().isBasicLand()) {
                                    cards.add(c);
                                }
                            }
                        }
                        BoosterUtils.sort(cards);
                        PaperCard card = SGuiChoose.one("Choose a card from your opponent's deck", cards);
                        model.getCollection().add(card);
                    }
                }
            });
        }
        else {
            view.getBtnQuit().setText("OK");
        }
    }

    public void onGameFinished(final GameView game) {
        if (game.isMatchWonBy(getConquestPlayer())) {
            model.addWin();
            gameRunner.wonGame = true;
        }
        else {
            model.addLoss();
        }

        FModel.getConquest().save();
        FModel.getConquestPreferences().save();

        gameRunner.finish();
    }

    private void awardWinStreakBonus(final IWinLoseView<? extends IButton> view) {
        int currentStreak = model.getCurrentPlaneData().getWinStreakCurrent() + 1;
        int mod = currentStreak % 10;
        int count = (currentStreak - 1) / 10 + 1; //so on 13th win you get 2 commons, etc.

        CardRarity rarity = null;
        String typeWon = "";

        switch (mod) {
            case 3:
                rarity = CardRarity.Common;
                count = 1;
                typeWon = "common";
                break;
            case 5:
                rarity = CardRarity.Uncommon;
                count = 1;
                typeWon = "uncommon";
                break;
            case 7:
                rarity = CardRarity.Rare;
                count = 1;
                typeWon = "rare";
                break;
            case 0: //0 is multiple of 10 win
                rarity = CardRarity.MythicRare;
                count = 1;
                typeWon = "mythic rare";
                break;
            default:
                return;
        }

        awardNewCards(model.getCurrentPlane().getCardPool().getAllCards(), currentStreak + " game win streak - unlocked", typeWon, rarity, view, null, count);
    }

    private boolean awardNewCards(Iterable<PaperCard> cardPool, String messagePrefix, String messageSuffix, CardRarity rarity, final IWinLoseView<? extends IButton> view, Predicate<CardRules> pred, int count) {
        List<PaperCard> commons = new ArrayList<PaperCard>();
        List<PaperCard> uncommons = new ArrayList<PaperCard>();
        List<PaperCard> rares = new ArrayList<PaperCard>();
        List<PaperCard> mythics = new ArrayList<PaperCard>();
        int newCardCount = 0;
        for (PaperCard c : cardPool) {
            if ((pred == null || pred.apply(c.getRules())) && !model.getCollection().contains(c)) {
                switch (c.getRarity()) {
                case Common:
                    commons.add(c);
                    break;
                case Uncommon:
                    uncommons.add(c);
                    break;
                case Rare:
                    rares.add(c);
                    break;
                case MythicRare:
                    mythics.add(c);
                    break;
                default:
                    break;
                }
            }
        }

        newCardCount = commons.size() + uncommons.size() + rares.size() + mythics.size();
        if (newCardCount == 0) {
            return false;
        }

        List<PaperCard> rewardPool = null;
        if (rarity != null) {
            switch (rarity) {
            case Common:
                rewardPool = commons;
                if (rewardPool.isEmpty()) {
                    rewardPool = uncommons;
                    messageSuffix = messageSuffix.replace("common", "uncommon");
                    if (rewardPool.isEmpty()) {
                        rewardPool = rares;
                        messageSuffix = messageSuffix.replace("uncommon", "rare");
                        if (rewardPool.isEmpty()) {
                            rewardPool = mythics;
                            messageSuffix = messageSuffix.replace("rare", "mythic rare");
                        }
                    }
                }
                break;
            case Uncommon:
                rewardPool = uncommons;
                if (rewardPool.isEmpty()) {
                    rewardPool = commons;
                    messageSuffix = messageSuffix.replace("uncommon", "common");
                    if (rewardPool.isEmpty()) {
                        rewardPool = rares;
                        messageSuffix = messageSuffix.replace("common", "rare");
                        if (rewardPool.isEmpty()) {
                            rewardPool = mythics;
                            messageSuffix = messageSuffix.replace("rare", "mythic rare");
                        }
                    }
                }
                break;
            case Rare:
                rewardPool = rares;
                if (rewardPool.isEmpty()) {
                    rewardPool = uncommons;
                    messageSuffix = messageSuffix.replace("rare", "uncommon");
                    if (rewardPool.isEmpty()) {
                        rewardPool = commons;
                        messageSuffix = messageSuffix.replace("uncommon", "common");
                        if (rewardPool.isEmpty()) {
                            rewardPool = mythics;
                            messageSuffix = messageSuffix.replace("common", "mythic rare");
                        }
                    }
                }
                break;
            case MythicRare:
                rewardPool = mythics;
                if (rewardPool.isEmpty()) {
                    rewardPool = rares;
                    messageSuffix = messageSuffix.replace("mythic rare", "rare");
                    if (rewardPool.isEmpty()) {
                        rewardPool = uncommons;
                        messageSuffix = messageSuffix.replace("rare", "uncommon");
                        if (rewardPool.isEmpty()) {
                            rewardPool = commons;
                            messageSuffix = messageSuffix.replace("uncommon", "common");
                        }
                    }
                }
                break;
            default:
                return false;
            }
            newCardCount = rewardPool.size();
        }
        List<PaperCard> rewards = new ArrayList<PaperCard>();
        for (int i = 0; i < count; i++) {
            //determine which rarity card to get based on pack ratios if no rarity passed in
            if (rarity == null) {
                int value = Aggregates.randomInt(1, 15);
                if (value <= 8) { //reward common about 50% of the time
                    rewardPool = commons;
                    if (rewardPool.isEmpty()) {
                        rewardPool = uncommons;
                        if (rewardPool.isEmpty()) {
                            rewardPool = rares;
                            if (rewardPool.isEmpty()) {
                                rewardPool = mythics;
                            }
                        }
                    }
                }
                else if (value <= 12) { //reward uncommon about 25% of the time
                    rewardPool = uncommons;
                    if (rewardPool.isEmpty()) {
                        rewardPool = commons;
                        if (rewardPool.isEmpty()) {
                            rewardPool = rares;
                            if (rewardPool.isEmpty()) {
                                rewardPool = mythics;
                            }
                        }
                    }
                }
                else if (value <= 14) { //reward rare about 12.5% of the time
                    rewardPool = rares;
                    if (rewardPool.isEmpty()) {
                        rewardPool = uncommons;
                        if (rewardPool.isEmpty()) {
                            rewardPool = commons;
                            if (rewardPool.isEmpty()) {
                                rewardPool = mythics;
                            }
                        }
                    }
                }
                else { //reward mythic about 6.75% of the time
                    rewardPool = mythics;
                    if (rewardPool.isEmpty()) {
                        rewardPool = rares;
                        if (rewardPool.isEmpty()) {
                            rewardPool = uncommons;
                            if (rewardPool.isEmpty()) {
                                rewardPool = commons;
                            }
                        }
                    }
                }
            }

            int index = Aggregates.randomInt(0, rewardPool.size() - 1);
            rewards.add(rewardPool.remove(index));

            if (--newCardCount == 0) {
                break; //break out if no new cards remain
            }
        }

        model.getCollection().add(rewards);

        String message = messagePrefix + " " + Lang.nounWithAmount(rewards.size(), messageSuffix);
        if (view == null) {
            SGuiChoose.reveal(message, rewards);
        }
        else {
            view.showCards(message, rewards);
        }
        return true;
    }

    private void awardBooster(final IWinLoseView<? extends IButton> view) {
        Iterable<PaperCard> cardPool = gameRunner.commander.getDeployedRegion().getCardPool().getAllCards();

        int newCommonCount = 0;
        int newUncommonCount = 0;
        int newRareCount = 0;
        int newMythicCount = 0;
        List<PaperCard> commons = new ArrayList<PaperCard>();
        List<PaperCard> uncommons = new ArrayList<PaperCard>();
        List<PaperCard> rares = new ArrayList<PaperCard>();
        List<PaperCard> mythics = new ArrayList<PaperCard>();

        for (PaperCard c : cardPool) {
            switch (c.getRarity()) {
            case Common:
                commons.add(c);
                break;
            case Uncommon:
                uncommons.add(c);
                break;
            case Rare:
                rares.add(c);
                break;
            case MythicRare:
                mythics.add(c);
                break;
            default:
                break;
            }
        }

        ConquestPreferences prefs = FModel.getConquestPreferences();

        int boostersPerMythic = prefs.getPrefInt(CQPref.BOOSTERS_PER_MYTHIC);
        int count = prefs.getPrefInt(CQPref.BOOSTER_RARES);
        for (int i = 0; i < count; i++) {
            if (Aggregates.randomInt(1, boostersPerMythic) == 1) {
                
            }
            else {
                
            }
        }

        int uncommonCount = prefs.getPrefInt(CQPref.BOOSTER_UNCOMMONS);

        count = prefs.getPrefInt(CQPref.BOOSTER_COMMONS);
        int commonReroll = prefs.getPrefInt(CQPref.BOOSTER_COMMON_REROLL);
        int uncommonReroll = prefs.getPrefInt(CQPref.BOOSTER_COMMON_REROLL);
        int rareReroll = prefs.getPrefInt(CQPref.BOOSTER_COMMON_REROLL);
        int mythicReroll = prefs.getPrefInt(CQPref.BOOSTER_MYTHIC_REROLL);
        
    }

    private class BoosterPool {
        private final int rerollChance;
        private int newCount = 0;
        private final List<PaperCard> cards = new ArrayList<PaperCard>();

        private BoosterPool(int rerollChance0) {
            rerollChance = rerollChance0;
        }

        private boolean isEmpty() {
            return cards.isEmpty();
        }

        private void add(PaperCard c) {
            if (!model.getCollection().contains(c)) {
                newCount++;
            }
            cards.add(c);
        }

        private PaperCard rewardCard() {
            PaperCard c;
            while (true) {
                int index = Aggregates.randomInt(0, cards.size() - 1);
                c = cards.get(index);

                //break out if new, if no new remaining, or if reroll chance fails
                if (newCount == 0 || !model.getCollection().contains(c) ||
                        Aggregates.randomInt(1, 100) > rerollChance) {
                    break;
                }
            }
            cards.remove(c);
            return c;
        }
    }
}
