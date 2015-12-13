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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import forge.FThreads;
import forge.GuiBase;
import forge.LobbyPlayer;
import forge.assets.FSkinProp;
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
        public final ConquestEvent event;

        private GameRunner(final ConquestCommander commander0, final ConquestEvent event0) {
            commander = commander0;
            event = event0;
        }

        public final void invokeAndWait() {
            FThreads.assertExecutedByEdt(false); //not supported if on UI thread
            FThreads.invokeInEdtLater(new Runnable() {
                @Override
                public void run() {
                    //commandCenter.startGame(GameRunner.this);
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
            //determine game variants
            Set<GameType> variants = new HashSet<GameType>();
            event.addVariants(variants);

            final RegisteredPlayer humanStart = new RegisteredPlayer(commander.getDeck());
            final RegisteredPlayer aiStart = new RegisteredPlayer(event.getOpponentDeck());

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
            String aiPlayerName = event.getOpponentName();
            if (humanPlayerName.equals(aiPlayerName)) {
                aiPlayerName += " (AI)"; //ensure player names are distinct
            }

            final List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
            humanPlayer = new LobbyPlayerHuman(humanPlayerName);
            humanPlayer.setAvatarCardImageKey(commander.getCard().getImageKey(false));
            starter.add(humanStart.setPlayer(humanPlayer));

            final LobbyPlayer aiPlayer = GamePlayerUtil.createAiPlayer(aiPlayerName, -1);
            aiPlayer.setAvatarCardImageKey(event.getAvatarImageKey());
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

    public void showGameRewards(final GameView game, final IWinLoseView<? extends IButton> view) {
        if (game.isMatchWonBy(humanPlayer)) {
            view.getBtnQuit().setText("Great!");

            //give controller a chance to run remaining logic on a separate thread
            view.showRewards(new Runnable() {
                @Override
                public void run() {
                    awardBooster(view);
                }
            });
        }
        else {
            view.getBtnQuit().setText("OK");
        }
    }

    public void onGameFinished(final GameView game) {
        if (game.isMatchWonBy(humanPlayer)) {
            model.addWin(gameRunner.event);
        }
        else {
            model.addLoss(gameRunner.event);
        }

        FModel.getConquest().save();
        FModel.getConquestPreferences().save();

        gameRunner.finish();
    }

    private void awardBooster(final IWinLoseView<? extends IButton> view) {
        Iterable<PaperCard> cardPool = model.getCurrentPlane().getCardPool().getAllCards();

        ConquestPreferences prefs = FModel.getConquestPreferences();

        BoosterPool commons = new BoosterPool();
        BoosterPool uncommons = new BoosterPool();
        BoosterPool rares = new BoosterPool();
        BoosterPool mythics = new BoosterPool();

        for (PaperCard c : cardPool) {
            switch (c.getRarity()) {
            case Common:
                commons.add(c);
                break;
            case Uncommon:
                uncommons.add(c);
                break;
            case Rare:
            case Special: //lump special cards in with rares for simplicity
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
        int raresPerBooster = prefs.getPrefInt(CQPref.BOOSTER_RARES);
        for (int i = 0; i < raresPerBooster; i++) {
            if (mythics.isEmpty() || Aggregates.randomInt(1, boostersPerMythic) > 1) {
                rares.rewardCard(rewards);
            }
            else {
                mythics.rewardCard(rewards);
            }
        }

        int uncommonsPerBooster = prefs.getPrefInt(CQPref.BOOSTER_UNCOMMONS);
        for (int i = 0; i < uncommonsPerBooster; i++) {
            uncommons.rewardCard(rewards);
        }

        int commonsPerBooster = prefs.getPrefInt(CQPref.BOOSTER_COMMONS);
        for (int i = 0; i < commonsPerBooster; i++) {
            commons.rewardCard(rewards);
        }

        //calculate odds of each rarity
        float commonOdds = commons.getOdds(commonsPerBooster);
        float uncommonOdds = uncommons.getOdds(uncommonsPerBooster);
        float rareOdds = rares.getOdds(raresPerBooster);
        float mythicOdds = mythics.getOdds(raresPerBooster / boostersPerMythic);

        //determine value of each rarity based on the base value of a common
        int commonValue = prefs.getPrefInt(CQPref.CARD_BASE_VALUE);
        int uncommonValue = Math.round(commonValue / (uncommonOdds / commonOdds));
        int rareValue = Math.round(commonValue / (rareOdds / commonOdds));
        int mythicValue = mythics.isEmpty() ? 0 : Math.round(commonValue / (mythicOdds / commonOdds));

        //remove any already unlocked cards from booster, calculating credit to reward instead
        int shards = 0;
        int count = rewards.size();
        for (int i = 0; i < count; i++) {
            PaperCard card = rewards.get(i);
            if (model.hasUnlockedCard(card)) {
                rewards.remove(i);
                i--;
                count--;
                switch (card.getRarity()) {
                case Common:
                    shards += commonValue;
                    break;
                case Uncommon:
                    shards += uncommonValue;
                    break;
                case Rare:
                case Special:
                    shards += rareValue;
                    break;
                case MythicRare:
                    shards += mythicValue;
                    break;
                default:
                    break;
                }
            }
        }

        if (count > 0) {
            BoosterUtils.sort(rewards);
            view.showCards("Booster contained " + count + " new card" + (count != 1 ? "s" : ""), rewards);
            if (shards > 0) {
                view.showMessage("Remaining cards exchanged for " + shards + " AEther shards.", "Received Credits", FSkinProp.ICO_QUEST_COIN);
                model.rewardAEtherShards(shards);
            }
            model.unlockCards(rewards);
        }
        else {
            view.showMessage("Booster contained no cards, so it has been exchanged for " + shards + " AEther shards.", "Received Credits", FSkinProp.ICO_QUEST_COIN);
            model.rewardAEtherShards(shards);
        }
    }

    private class BoosterPool {
        private final List<PaperCard> cards = new ArrayList<PaperCard>();

        private BoosterPool() {
        }

        private boolean isEmpty() {
            return cards.isEmpty();
        }

        public float getOdds(float perBoosterCount) {
            int count = cards.size();
            if (count == 0) { return 0; }
            return (float)perBoosterCount / (float)count;
        }

        private void add(PaperCard c) {
            cards.add(c);
        }

        private void rewardCard(List<PaperCard> rewards) {
            int index = Aggregates.randomInt(0, cards.size() - 1);
            PaperCard c = cards.get(index);
            cards.remove(index);
            rewards.add(c);
        }
    }
}
