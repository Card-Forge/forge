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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import forge.FThreads;
import forge.GuiBase;
import forge.ImageKeys;
import forge.LobbyPlayer;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardType;
import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.IButton;
import forge.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.match.HostedMatch;
import forge.model.FModel;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.player.GamePlayerUtil;
import forge.player.LobbyPlayerHuman;
import forge.properties.ForgePreferences.FPref;
import forge.quest.BoosterUtils;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;
import forge.util.storage.IStorage;

public class ConquestController {
    private ConquestData model;
    private transient IStorage<Deck> decks;
    private transient GameRunner gameRunner;
    private LobbyPlayerHuman humanPlayer;

    public ConquestController() {
    }

    public String getName() {
        return model == null ? null : model.getName();
    }

    public ConquestData getModel() {
        return model;
    }

    public IStorage<Deck> getDecks() {
        return decks;
    }

    public void load(final ConquestData model0) {
        model = model0;
        decks = model == null ? null : model.getDeckStorage();
    }

    public void save() {
        if (model != null) {
            model.saveData();
        }
    }

    public void unlockCommander() {
        ConquestPlaneData planeData = model.getCurrentPlaneData();
        List<PaperCard> options = new ArrayList<PaperCard>();
        for (PaperCard pc : model.getCurrentPlane().getCommanders()) {
            if (planeData.getWinsAgainst(pc) > 0 && !planeData.hasCommander(pc)) {
                options.add(pc); //only add commanders that you've beaten and don't already have on your roster
            }
        }

        if (options.isEmpty()) {
            SOptionPane.showMessageDialog("No defeated foes are available to join your cause");
            return;
        }

        Collections.sort(options);

        PaperCard commander = SGuiChoose.oneOrNone("Select a defeated foe to join your cause", options);
        if (commander == null) { return; }

        List<PaperCard> newCards = model.addCommander(commander);
        BoosterUtils.sort(newCards);
        SGuiChoose.reveal(commander.getName() + " brought along " + Lang.nounWithAmount(newCards.size(), "new card"), newCards);
    }

    /*private void playGame(final ConquestCommander commander, final int opponentIndex, final boolean isHumanDefending, final IVCommandCenter commandCenter) {
        gameRunner = new GameRunner(commander, opponentIndex, isHumanDefending, commandCenter);
        gameRunner.invokeAndWait();

        //after game finished
        if (gameRunner.wonGame) {
        }
        gameRunner = null;
    }*/

    public class GameRunner {
        private class Lock {
        }
        private final Lock lock = new Lock();

        public final ConquestCommander commander;
        public final ConquestCommander opponent;
        public final boolean isHumanDefending;
        private final IVCommandCenter commandCenter;
        @SuppressWarnings("unused")
        private boolean wonGame;

        private GameRunner(final ConquestCommander commander0, final int opponentIndex, final boolean isHumanDefending0, final IVCommandCenter commandCenter0) {
            commander = commander0;
            opponent = null; //model.getCurrentPlaneData().getOpponent(opponentIndex);
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
            ConquestPreferences prefs = FModel.getConquestPreferences();

            //determine game variants
            Set<GameType> variants = new HashSet<GameType>();
            int rng = Aggregates.randomInt(1, 100);
            int commanderThreshold = prefs.getPrefInt(CQPref.PERCENT_COMMANDER);
            int planechaseThreshold = commanderThreshold + prefs.getPrefInt(CQPref.PERCENT_PLANECHASE);
            int doubleVariantThreshold = planechaseThreshold + prefs.getPrefInt(CQPref.PERCENT_DOUBLE_VARIANT);
            if (rng <= commanderThreshold) {
                variants.add(GameType.Commander);
            }
            else if (rng <= planechaseThreshold) {
                variants.add(GameType.Planechase);
            }
            else if (rng <= doubleVariantThreshold) {
                variants.add(GameType.Commander);
                variants.add(GameType.Planechase);
            }

            final RegisteredPlayer humanStart = new RegisteredPlayer(commander.getDeck());
            final RegisteredPlayer aiStart = new RegisteredPlayer(opponent.getDeck());

            if (isHumanDefending) { //give human a small life bonus if defending
                humanStart.setStartingLife(humanStart.getStartingLife() + prefs.getPrefInt(CQPref.DEFEND_BONUS_LIFE));
            }
            if (variants.contains(GameType.Commander)) { //add 10 starting life for both players if playing a Commander game
                humanStart.setStartingLife(humanStart.getStartingLife() + 10);
                aiStart.setStartingLife(aiStart.getStartingLife() + 10);
                humanStart.assignCommander();
                aiStart.assignCommander();
            }
            if (variants.contains(GameType.Planechase)) { //generate planar decks if planechase variant being applied
                humanStart.setPlanes(generatePlanarPool());
                aiStart.setPlanes(generatePlanarPool());
            }

            String humanPlayerName = commander.getPlayerName();
            String aiPlayerName = opponent.getPlayerName();
            if (humanPlayerName.equals(aiPlayerName)) {
                aiPlayerName += " (AI)"; //ensure player names are distinct
            }

            final List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
            humanPlayer = new LobbyPlayerHuman(humanPlayerName);
            humanPlayer.setAvatarCardImageKey(ImageKeys.getImageKey(commander.getCard(), false));
            starter.add(humanStart.setPlayer(humanPlayer));

            final LobbyPlayer aiPlayer = GamePlayerUtil.createAiPlayer(aiPlayerName, -1);
            aiPlayer.setAvatarCardImageKey(ImageKeys.getImageKey(opponent.getCard(), false));
            starter.add(aiStart.setPlayer(aiPlayer));

            final boolean useRandomFoil = FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL);
            for (final RegisteredPlayer rp : starter) {
                rp.setRandomFoil(useRandomFoil);
            }
            final GameRules rules = new GameRules(GameType.PlanarConquest);
            rules.setGamesPerMatch(1); //only play one game at a time
            rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
            rules.setCanCloneUseTargetsImage(FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE));
            final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
            FThreads.invokeInEdtNowOrLater(new Runnable(){
                @Override
                public void run() {
                    hostedMatch.startMatch(rules, null, starter, humanStart, GuiBase.getInterface().getNewGuiGame());
                }
            });
        }

        private void finish() {
            synchronized(lock) { //release game lock once game finished
                lock.notify();
            }
        }

        private List<PaperCard> generatePlanarPool() {
            String planeName = model.getCurrentPlane().getName();
            List<PaperCard> pool = new ArrayList<PaperCard>();
            List<PaperCard> otherPlanes = new ArrayList<PaperCard>();
            List<PaperCard> phenomenons = new ArrayList<PaperCard>();

            for (PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
                CardType type = c.getRules().getType();
                if (type.isPlane()) {
                    if (type.hasSubtype(planeName)) {
                        pool.add(c); //always include card in pool if it matches the current plane
                    }
                    else {
                        otherPlanes.add(c);
                    }
                }
                else if (type.isPhenomenon()) {
                    phenomenons.add(c);
                }
            }

            //add between 0 and 2 phenomenons (where 2 is the most supported)
            int numPhenomenons = Aggregates.randomInt(0, 2);
            for (int i = 0; i < numPhenomenons; i++) {
                pool.add(Aggregates.removeRandom(phenomenons));
            }

            //add enough additional plane cards to reach a minimum 10 card deck
            while (pool.size() < 10) {
                pool.add(Aggregates.removeRandom(otherPlanes));
            }
            return pool;
        }
    }

    /*private boolean recruit(ConquestCommander commander) {
        boolean bonusCard = Aggregates.randomInt(1, 100) <= FModel.getConquestPreferences().getPrefInt(CQPref.RECRUIT_BONUS_CARD_ODDS);
        return awardNewCards(model.getCurrentPlane().getCardPool().getAllCards(),
                commander.getName() + " recruited", "new creature", null, null,
                CardRulesPredicates.Presets.IS_CREATURE, bonusCard ? 2 : 1);
    }

    private boolean study(ConquestCommander commander) {
        boolean bonusCard = Aggregates.randomInt(1, 100) <= FModel.getConquestPreferences().getPrefInt(CQPref.STUDY_BONUS_CARD_ODDS);
        return awardNewCards(model.getCurrentPlane().getCardPool().getAllCards(),
                commander.getName() + " unlocked", "new spell", null, null,
                CardRulesPredicates.Presets.IS_NON_CREATURE_SPELL, bonusCard ? 2 : 1);
    }*/

    public void showGameRewards(final GameView game, final IWinLoseView<? extends IButton> view) {
        if (game.isMatchWonBy(humanPlayer)) {
            view.getBtnQuit().setText("Great!");

            //give controller a chance to run remaining logic on a separate thread
            view.showRewards(new Runnable() {
                @Override
                public void run() {
                    awardWinStreakBonus(view);

                    final List<String> options = ImmutableList.of("Booster", "Card");
                    if (SOptionPane.showOptionDialog("Choose one \u2014\n\n" +
                            "\u2022 Open a random booster pack\n" +
                            "\u2022 Choose a card from your opponent's deck",
                            "Spoils of Victory", null, options) == 0) {
                        awardBooster(view);
                    }
                    else {
                        if (!awardOpponentsCard(view)) {
                            SOptionPane.showMessageDialog("Opponent had no new cards, so you can open a random booster pack instead.");
                            awardBooster(view);
                        }
                    }
                }
            });
        }
        else {
            view.getBtnQuit().setText("OK");
        }
    }

    public void onGameFinished(final GameView game) {
        if (game.isMatchWonBy(humanPlayer)) {
            model.addWin(gameRunner.opponent);
            gameRunner.wonGame = true;
        }
        else {
            model.addLoss(gameRunner.opponent);
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

        model.getCollection().addAll(rewards);
        model.getNewCards().addAll(rewards);

        String message = messagePrefix;
        if (messageSuffix != null) {
            message += " " + Lang.nounWithAmount(rewards.size(), messageSuffix);
        }
        if (view == null) {
            SGuiChoose.reveal(message, rewards);
        }
        else {
            view.showCards(message, rewards);
        }
        return true;
    }

    private void awardBooster(final IWinLoseView<? extends IButton> view) {
        Iterable<PaperCard> cardPool = model.getCurrentPlane().getCardPool().getAllCards();

        ConquestPreferences prefs = FModel.getConquestPreferences();

        BoosterPool commons = new BoosterPool(prefs.getPrefInt(CQPref.BOOSTER_COMMON_REROLL));
        BoosterPool uncommons = new BoosterPool(prefs.getPrefInt(CQPref.BOOSTER_UNCOMMON_REROLL));
        BoosterPool rares = new BoosterPool(prefs.getPrefInt(CQPref.BOOSTER_RARE_REROLL));
        BoosterPool mythics = new BoosterPool(prefs.getPrefInt(CQPref.BOOSTER_MYTHIC_REROLL));

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

        List<PaperCard> rewards = new ArrayList<PaperCard>();
        int boostersPerMythic = prefs.getPrefInt(CQPref.BOOSTERS_PER_MYTHIC);
        int count = prefs.getPrefInt(CQPref.BOOSTER_RARES);
        for (int i = 0; i < count; i++) {
            if (mythics.isEmpty() || Aggregates.randomInt(1, boostersPerMythic) > 1) {
                rares.rewardCard(rewards);
            }
            else {
                mythics.rewardCard(rewards);
            }
        }

        count = prefs.getPrefInt(CQPref.BOOSTER_UNCOMMONS);
        for (int i = 0; i < count; i++) {
            uncommons.rewardCard(rewards);
        }

        count = prefs.getPrefInt(CQPref.BOOSTER_COMMONS);
        for (int i = 0; i < count; i++) {
            commons.rewardCard(rewards);
        }

        count = rewards.size();
        if (count == 0) {
            //if no new cards in booster, pretend it contained a random card
            awardNewCards(cardPool, "Booster contained ", "new card", null, view, null, 1);
            return;
        }

        BoosterUtils.sort(rewards);
        model.getCollection().addAll(rewards);
        model.getNewCards().addAll(rewards);
        view.showCards("Booster contained " + count + " new card" + (count != 1 ? "s" : ""), rewards);
    }

    private boolean awardOpponentsCard(IWinLoseView<? extends IButton> view) {
        List<PaperCard> cards = new ArrayList<PaperCard>();
        for (Entry<PaperCard, Integer> entry : gameRunner.opponent.getDeck().getMain()) {
            PaperCard c = entry.getKey();
            if (!c.getRules().getType().isBasicLand() && !getModel().getCollection().contains(c)) {
                cards.add(c);
            }
        }

        if (cards.isEmpty()) { return false; }

        BoosterUtils.sort(cards);
        PaperCard card = SGuiChoose.one("Choose a card from your opponent's deck", cards);
        model.getCollection().add(card);
        model.getNewCards().add(card);
        return true;
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

        private void rewardCard(List<PaperCard> rewards) {
            if (newCount == 0) {
                return;
            }

            PaperCard c;
            while (true) {
                int index = Aggregates.randomInt(0, cards.size() - 1);
                c = cards.get(index);

                if (!model.getCollection().contains(c)) {
                    newCount--;
                    cards.remove(c);
                    rewards.add(c);
                    return; //return if new
                }

                if (Aggregates.randomInt(1, 100) > rerollChance) {
                    return; //return if reroll chance fails
                }
            }
        }
    }
}
