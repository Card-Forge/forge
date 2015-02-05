package forge.ai.simulation;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import forge.LobbyPlayer;
import forge.ai.LobbyPlayerAi;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.GameObjectMap;
import forge.game.GameRules;
import forge.game.Match;
import forge.game.StaticEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class GameCopier {
    private static final ZoneType[] ZONES = new ZoneType[] {
        ZoneType.Battlefield,
        ZoneType.Hand,
        ZoneType.Graveyard,
        ZoneType.Library,
        ZoneType.Exile,
    };

    private Game origGame;
    private BiMap<Player, Player> playerMap = HashBiMap.create();
    private BiMap<Card, Card> cardMap = HashBiMap.create();
    private CopiedGameObjectMap gameObjectMap;

    public GameCopier(Game origGame) {
        this.origGame = origGame;
    }
    
    public Game makeCopy() {
        List<RegisteredPlayer> origPlayers = origGame.getMatch().getPlayers();
        List<RegisteredPlayer> newPlayers = new ArrayList<>();
        for (RegisteredPlayer p : origPlayers) {
            newPlayers.add(clonePlayer(p));
        }
        GameRules currentRules = origGame.getRules();
        Match newMatch = new Match(currentRules, newPlayers);
        Game newGame = new Game(newPlayers, currentRules, newMatch);
        for (int i = 0; i < origGame.getPlayers().size(); i++) {
            Player origPlayer = origGame.getPlayers().get(i);
            Player newPlayer = newGame.getPlayers().get(i);
            newPlayer.setLife(origPlayer.getLife(), null);
            newPlayer.setActivateLoyaltyAbilityThisTurn(origPlayer.getActivateLoyaltyAbilityThisTurn());
            newPlayer.setPoisonCounters(origPlayer.getPoisonCounters(), null);
            newPlayer.setLifeLostLastTurn(origPlayer.getLifeLostLastTurn());
            newPlayer.setLifeLostThisTurn(origPlayer.getLifeLostThisTurn());
            newPlayer.setPreventNextDamage(origPlayer.getPreventNextDamage());
            playerMap.put(origPlayer, newPlayer);
        }

        Player newPlayerTurn = playerMap.get(origGame.getPhaseHandler().getPlayerTurn());
        newGame.getPhaseHandler().devModeSet(origGame.getPhaseHandler().getPhase(), newPlayerTurn);
        newGame.getTriggerHandler().suppressMode(TriggerType.ChangesZone);

        copyGameState(newGame);
        
        newGame.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        
        // Undo effects first before calculating them below, to avoid them applying twice.
        gameObjectMap = new CopiedGameObjectMap(newGame);
        for (StaticEffect effect : origGame.getStaticEffects().getEffects()) {
            effect.removeMapped(gameObjectMap);
        }

        newGame.getAction().checkStateEffects(true); //ensure state based effects and triggers are updated
        
        newGame.getTriggerHandler().resetActiveTriggers();

        return newGame;
    }

    private RegisteredPlayer clonePlayer(RegisteredPlayer p) {
        RegisteredPlayer clone = new RegisteredPlayer(p.getDeck());
        LobbyPlayer lp = p.getPlayer();
        if (!(lp instanceof LobbyPlayerAi))
            lp = new LobbyPlayerAi(p.getPlayer().getName());
        clone.setPlayer(lp);
        return clone;
    }

    private void copyGameState(Game newGame) {
        for (ZoneType zone : ZONES) {
            for (Card card : origGame.getCardsIn(zone)) {
                addCard(newGame, zone, card);
            }
        }
        for (Card card : origGame.getCardsIn(ZoneType.Battlefield)) {
            Card otherCard = cardMap.get(card);
            otherCard.setTimestamp(card.getTimestamp());
            otherCard.setSickness(card.hasSickness());
            if (card.isEnchanting()) {
                otherCard.setEnchanting(cardMap.get(card.getEnchanting()));
            }
            if (card.isEquipping()) {
                otherCard.equipCard(cardMap.get(card.getEquipping()));
            }
            if (card.isFortifying()) {
                otherCard.setFortifying(cardMap.get(card.getFortifying()));
            }
            if (card.getCloneOrigin() != null) {
                otherCard.setCloneOrigin(cardMap.get(card.getCloneOrigin()));
            }
            if (card.getHaunting() != null) {
                otherCard.setHaunting(cardMap.get(card.getHaunting()));
            }
            if (card.getEffectSource() != null) {
                otherCard.setEffectSource(cardMap.get(card.getEffectSource()));
            }
            if (card.isPaired()) {
                otherCard.setPairedWith(cardMap.get(card.getPairedWith()));
            }
            otherCard.setCommander(card.isCommander());
            // TODO: Verify that the above relationships are preserved bi-directionally or not.
        }
    }

    @SuppressWarnings("unchecked")
    private void addCard(Game newGame, ZoneType zone, Card c) {
        Player owner = playerMap.get(c.getOwner());
        Card newCard = null;
        if (c.isToken()) {
            String tokenStr = new CardFactory.TokenInfo(c).toString();
            // TODO: Use a version of the API that doesn't return a list (i.e. these shouldn't be affected
            // by doubling season, etc).
            newCard = CardFactory.makeToken(CardFactory.TokenInfo.fromString(tokenStr), owner).get(0);
        } else {
            newCard = Card.fromPaperCard(c.getPaperCard(), owner);
        }
        cardMap.put(c, newCard);

        Player zoneOwner = owner;
        if (zone == ZoneType.Battlefield) {
            // TODO: Controllers' list with timestamps should be copied.
            zoneOwner = playerMap.get(c.getController());
            newCard.setController(zoneOwner, 0);
            
            newCard.addTempPowerBoost(c.getTempPowerBoost());
            newCard.setSemiPermanentPowerBoost(c.getSemiPermanentPowerBoost());
            newCard.addTempToughnessBoost(c.getTempToughnessBoost());
            newCard.setSemiPermanentToughnessBoost(c.getSemiPermanentToughnessBoost());
            
            newCard.setChangedCardTypes(c.getChangedCardTypes());
            newCard.setChangedCardKeywords(c.getChangedCardKeywords());
            // TODO: Is this correct? Does it not duplicate keywords from enchantments and such?
            for (String kw : c.getHiddenExtrinsicKeywords())
                newCard.addHiddenExtrinsicKeyword(kw);
            newCard.setExtrinsicKeyword((ArrayList<String>) c.getExtrinsicKeyword().clone());
            if (c.isTapped()) {
                newCard.setTapped(true);
            }
            if (c.isFaceDown()) {
                newCard.setState(CardStateName.FaceDown, true);
                if (c.isManifested()) {
                    newCard.setManifested(true);
                    // TODO: Should be able to copy other abilities...
                    newCard.addSpellAbility(CardFactoryUtil.abilityManifestFaceUp(newCard, newCard.getManaCost()));
                }
            }
            
            Map<CounterType, Integer> counters = c.getCounters();
            if (!counters.isEmpty()) {
                for(Entry<CounterType, Integer> kv : counters.entrySet()) {
                    String str = kv.getKey().toString();
                    int count = kv.getValue();
                    newCard.addCounter(CounterType.valueOf(str), count, false);
                }
            }
            // TODO: Other chosen things...
            if (c.getChosenPlayer() != null) {
                newCard.setChosenPlayer(playerMap.get(c.getChosenPlayer()));
            }
            // TODO: FIXME
            if (c.hasRemembered()) {
                for (Object o : c.getRemembered()) {
                    System.out.println("Remembered: " + o +  o.getClass());
                    //newCard.addRemembered(o);
                }
            }
        }

        zoneOwner.getZone(zone).add(newCard);
    }

    private class CopiedGameObjectMap extends GameObjectMap {
        private Game copiedGame;

        public CopiedGameObjectMap(Game copiedGame) {
            this.copiedGame = copiedGame;
        }

        @Override
        public Game getGame() {
            return copiedGame;
        }

        @Override
        public GameObject map(GameObject o) {
            return find(o);
        }
    }
 
    public GameObject find(GameObject o) {
        GameObject result = cardMap.get(o);
        if (result != null)
            return result;
        // TODO: Have only one GameObject map?
        return playerMap.get(o);
    }
    public GameObject reverseFind(GameObject o) {
        GameObject result = cardMap.inverse().get(o);
        if (result != null)
            return result;
        // TODO: Have only one GameObject map?
        return playerMap.inverse().get(o);
    }
}
