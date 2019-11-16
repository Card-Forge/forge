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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import forge.FThreads;
import forge.GuiBase;
import forge.LobbyPlayer;
import forge.card.CardType;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.io.DeckStorage;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.IButton;
import forge.interfaces.IGuiGame;
import forge.interfaces.IWinLoseView;
import forge.item.PaperCard;
import forge.match.HostedMatch;
import forge.model.FModel;
import forge.planarconquest.ConquestPreferences.CQPref;
import forge.player.GamePlayerUtil;
import forge.player.LobbyPlayerHuman;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences.FPref;
import forge.quest.BoosterUtils;
import forge.util.Aggregates;
import forge.util.FileUtil;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;

public class ConquestController {
    private ConquestData model;
    private IStorage<Deck> decks;
    private ConquestBattle activeBattle;
    private LobbyPlayerHuman humanPlayer;

    public ConquestController() {
    }

    public ConquestData getModel() {
        return model;
    }
    public void setModel(final ConquestData model0) {
        model = model0;
        if (model == null) {
            decks = null;
            return;
        }

        File decksDir = new File(model.getDirectory(), "decks");
        FileUtil.ensureDirectoryExists(decksDir);
        DeckStorage storage = new DeckStorage(decksDir, ForgeConstants.CONQUEST_SAVE_DIR);
        decks = new StorageImmediatelySerialized<>(model.getName() + " decks", storage);
    }

    public IStorage<Deck> getDecks() {
        return decks;
    }

    public void startBattle(ConquestBattle battle) {
        if (activeBattle != null) { return; }

        final Set<GameType> variants = battle.getVariants();
        final ConquestCommander commander = model.getSelectedCommander(); 
        final RegisteredPlayer humanStart = new RegisteredPlayer(commander.getDeck());
        final RegisteredPlayer aiStart = new RegisteredPlayer(battle.getOpponentDeck());

        if (variants.contains(GameType.Commander)) { //add 10 starting life for both players if playing a Commander game
            humanStart.setStartingLife(humanStart.getStartingLife() + 10);
            aiStart.setStartingLife(aiStart.getStartingLife() + 10);
            humanStart.assignCommander();
            aiStart.assignCommander();
        } else { // Add commander as just a normal card in the deck
            CardPool main = humanStart.getDeck().getOrCreate(DeckSection.Main);
            main.add(humanStart.getDeck().getOrCreate(DeckSection.Commander).toFlatList());}
        if (variants.contains(GameType.Vanguard)) { //add opponent vanguard to player deck
            CardPool avatarPool = humanStart.getDeck().getOrCreate(DeckSection.Avatar);
            avatarPool.clear();
            avatarPool.add(aiStart.getDeck().getOrCreate(DeckSection.Avatar).get(0));
            humanStart.assignVanguardAvatar();
            aiStart.assignVanguardAvatar();
        }
        if (variants.contains(GameType.Planeswalker)) { //account for Planeswalker format
            humanStart.setPlaneswalker(model.getPlaneswalker());
            aiStart.setPlaneswalker(battle.getPlaneswalker());
        }
        if (variants.contains(GameType.Planechase)) { //generate planar decks if planechase variant being applied
            List<PaperCard> planes = generatePlanarPool();
            humanStart.setPlanes(planes);
            aiStart.setPlanes(planes);
        }

        //String humanPlayerName = commander.getPlayerName(); // TODO: this currently makes it impossible to manually pay mana from pool (see VManaPool#tap)
        String humanPlayerName = GamePlayerUtil.getGuiPlayer().getName();

        String aiPlayerName = battle.getOpponentName();
        if (humanPlayerName.equals(aiPlayerName)) {
            aiPlayerName += " (AI)"; //ensure player names are distinct
        }

        final List<RegisteredPlayer> starter = new ArrayList<>();
        humanPlayer = new LobbyPlayerHuman(humanPlayerName);
        humanPlayer.setAvatarCardImageKey(commander.getCard().getImageKey(false));
        starter.add(humanStart.setPlayer(humanPlayer));

        final IGuiGame gui = GuiBase.getInterface().getNewGuiGame();
        final LobbyPlayer aiPlayer = GamePlayerUtil.createAiPlayer(aiPlayerName, -1, -1);
        battle.setOpponentAvatar(aiPlayer, gui);
        starter.add(aiStart.setPlayer(aiPlayer));

        final boolean useRandomFoil = FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL);
        for (final RegisteredPlayer rp : starter) {
            rp.setRandomFoil(useRandomFoil);
        }
        final GameRules rules = new GameRules(GameType.PlanarConquest);
        rules.setGamesPerMatch(battle.gamesPerMatch());
        rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
        rules.setCanCloneUseTargetsImage(FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE));
        final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
        FThreads.invokeInEdtNowOrLater(new Runnable(){
            @Override
            public void run() {
                hostedMatch.startMatch(rules, variants, starter, humanStart, gui);
            }
        });
        activeBattle = battle;
    }

    private List<PaperCard> generatePlanarPool() {
        String planeName = model.getCurrentPlane().getName();
        List<PaperCard> pool = new ArrayList<>();
        List<PaperCard> otherPlanes = new ArrayList<>();
        List<PaperCard> phenomenons = new ArrayList<>();

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

    public void showGameOutcome(final GameView game, final IWinLoseView<? extends IButton> view) {
        activeBattle.showGameOutcome(model, game, humanPlayer, view);
    }

    public void finishEvent(final IWinLoseView<? extends IButton> view) {
        if (activeBattle == null) { return; }

        activeBattle.onFinished(model, view);
        activeBattle = null;
    }

    public List<ConquestReward> awardBooster(ConquestAwardPool pool) {
        ConquestPreferences prefs = FModel.getConquestPreferences();
        List<PaperCard> rewards = new ArrayList<>();
        int boostersPerMythic = prefs.getPrefInt(CQPref.BOOSTERS_PER_MYTHIC);
        int raresPerBooster = prefs.getPrefInt(CQPref.BOOSTER_RARES);
        for (int i = 0; i < raresPerBooster; i++) {
            if (!pool.mythics.isEmpty() && Aggregates.randomInt(1, boostersPerMythic) == 1) 
                rewards.add(Aggregates.removeRandom(pool.mythics));
            else if(!pool.rares.isEmpty()) 
                rewards.add(Aggregates.removeRandom(pool.rares));
        }

        int uncommonsPerBooster = prefs.getPrefInt(CQPref.BOOSTER_UNCOMMONS);
        for (int i = 0; i < uncommonsPerBooster && !pool.uncommons.isEmpty(); i++)
            rewards.add(Aggregates.removeRandom(pool.uncommons));

        int commonsPerBooster = prefs.getPrefInt(CQPref.BOOSTER_COMMONS);
        for (int i = 0; i < commonsPerBooster && !pool.commons.isEmpty(); i++)
            rewards.add(Aggregates.removeRandom(pool.commons));

        BoosterUtils.sort(rewards);

        //remove any already unlocked cards from booster, calculating credit to reward instead
        //also build list of all rewards including replacement shards for each duplicate card
        //build this list in reverse order so commons appear first
        int shards = 0;
        final List<ConquestReward> allRewards = new ArrayList<>();
        for (int i = rewards.size() - 1; i >= 0; i--) {
            int replacementShards = 0;
            PaperCard card = rewards.get(i);
            if (model.hasUnlockedCard(card)) {
                rewards.remove(i);
                replacementShards = ConquestUtil.getShardValue(card, CQPref.AETHER_BASE_DUPLICATE_VALUE);
                shards += replacementShards;
            }
            allRewards.add(new ConquestReward(card, replacementShards));
        }

        model.unlockCards(rewards);
        model.rewardAEtherShards(shards);
        return allRewards;
    }
}
