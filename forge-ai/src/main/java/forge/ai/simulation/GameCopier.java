package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

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
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityRestriction;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;

public class GameCopier {
    private static final ZoneType[] ZONES = new ZoneType[] {
        ZoneType.Battlefield,
        ZoneType.Hand,
        ZoneType.Graveyard,
        ZoneType.Library,
        ZoneType.Exile,
        ZoneType.Stack,
        ZoneType.Command,
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
        Match newMatch = new Match(currentRules, newPlayers, origGame.getView().getTitle());
        Game newGame = new Game(newPlayers, currentRules, newMatch);
        for (int i = 0; i < origGame.getPlayers().size(); i++) {
            Player origPlayer = origGame.getPlayers().get(i);
            Player newPlayer = newGame.getPlayers().get(i);
            newPlayer.setLife(origPlayer.getLife(), null);
            newPlayer.setActivateLoyaltyAbilityThisTurn(origPlayer.getActivateLoyaltyAbilityThisTurn());
            for (int j = 0; j < origPlayer.getSpellsCastThisTurn(); j++)
                newPlayer.addSpellCastThisTurn();
            for (int j = 0; j < origPlayer.getLandsPlayedThisTurn(); j++)
                newPlayer.addLandPlayedThisTurn();
            newPlayer.setPoisonCounters(origPlayer.getPoisonCounters(), null);
            newPlayer.setLifeLostLastTurn(origPlayer.getLifeLostLastTurn());
            newPlayer.setLifeLostThisTurn(origPlayer.getLifeLostThisTurn());
            newPlayer.setPreventNextDamage(origPlayer.getPreventNextDamage());
            newPlayer.setCommander(origPlayer.getCommander()); // will be fixed up below
            playerMap.put(origPlayer, newPlayer);
        }

        Player newPlayerTurn = playerMap.get(origGame.getPhaseHandler().getPlayerTurn());
        newGame.getPhaseHandler().devModeSet(origGame.getPhaseHandler().getPhase(), newPlayerTurn);
        newGame.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        for (Player p : newGame.getPlayers()) {
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(false);
        }
        
        copyGameState(newGame);

        for (Player p : newGame.getPlayers()) {
            p.setCommander(gameObjectMap.map(p.getCommander()));
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(true);
        }
        newGame.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        
        // Undo effects first before calculating them below, to avoid them applying twice.
        for (StaticEffect effect : origGame.getStaticEffects().getEffects()) {
            effect.removeMapped(gameObjectMap);
        }

        newGame.getAction().checkStateEffects(true); //ensure state based effects and triggers are updated
        
        newGame.getTriggerHandler().resetActiveTriggers();

        if (GameSimulator.COPY_STACK)
            copyStack(origGame, newGame, gameObjectMap);

        return newGame;
    }

    private static void copyStack(Game origGame, Game newGame, GameObjectMap map) {
        for (SpellAbilityStackInstance origEntry : origGame.getStack()) {
            SpellAbility origSa = origEntry.getSpellAbility(false);
            Card origHostCard = origSa.getHostCard();
            Card newCard = map.map(origHostCard);
            SpellAbility newSa = null;
            if (origSa.isSpell()) {
                for (SpellAbility sa : newCard.getAllSpellAbilities()) {
                    if (sa.getDescription().equals(origSa.getDescription())) {
                        newSa = sa;
                        break;
                    }
                }
            }
            if (newSa != null) {
                newSa.setActivatingPlayer(map.map(origSa.getActivatingPlayer()));
                if (origSa.usesTargeting()) {
                    for (GameObject o : origSa.getTargets().getTargets()) {
                        newSa.getTargets().add(map.map(o));
                    }
                }
                newGame.getStack().add(newSa);
            }
        } 
    }

    private RegisteredPlayer clonePlayer(RegisteredPlayer p) {
        RegisteredPlayer clone = new RegisteredPlayer(p.getDeck());
        LobbyPlayer lp = p.getPlayer();
        if (!(lp instanceof LobbyPlayerAi)) {
            lp = new LobbyPlayerAi(p.getPlayer().getName(), null);
        }
        clone.setPlayer(lp);
        return clone;
    }

    private void copyGameState(Game newGame) {
        newGame.setAge(origGame.getAge());
        for (ZoneType zone : ZONES) {
            for (Card card : origGame.getCardsIn(zone)) {
                addCard(newGame, zone, card);
            }
        }
        gameObjectMap = new CopiedGameObjectMap(newGame);
        for (Card card : origGame.getCardsIn(ZoneType.Battlefield)) {
            Card otherCard = cardMap.get(card);
            otherCard.setTimestamp(card.getTimestamp());
            otherCard.setSickness(card.hasSickness());
            if (card.isEnchanting()) {
                otherCard.setEnchanting(gameObjectMap.map(card.getEnchanting()));
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
            
            int setPower = c.getSetPower();
            int setToughness = c.getSetToughness();
            if (setPower != -1 || setToughness != -1)  {
                // TODO: Copy the full list with timestamps.
                newCard.addNewPT(setPower, setToughness, newGame.getNextTimestamp());
            }
            newCard.addTempPowerBoost(c.getTempPowerBoost());
            newCard.setSemiPermanentPowerBoost(c.getSemiPermanentPowerBoost());
            newCard.addTempToughnessBoost(c.getTempToughnessBoost());
            newCard.setSemiPermanentToughnessBoost(c.getSemiPermanentToughnessBoost());
            
            newCard.setChangedCardTypes(c.getChangedCardTypes());
            newCard.setChangedCardKeywords(c.getChangedCardKeywords());
            // TODO: Is this correct? Does it not duplicate keywords from enchantments and such?
            for (String kw : c.getHiddenExtrinsicKeywords())
                newCard.addHiddenExtrinsicKeyword(kw);
            newCard.setExtrinsicKeyword(Lists.newArrayList(c.getExtrinsicKeyword()));
            if (c.isTapped()) {
                newCard.setTapped(true);
            }
            if (c.isFaceDown()) {
                boolean isCreature = newCard.isCreature();
                newCard.setState(CardStateName.FaceDown, true);
                if (c.isManifested()) {
                    newCard.setManifested(true);
                    // TODO: Should be able to copy other abilities...
                    if (isCreature) {
                        newCard.addSpellAbility(CardFactoryUtil.abilityManifestFaceUp(newCard, newCard.getManaCost()));
                    }
                }
            }
            if (c.isMonstrous()) {
                newCard.setMonstrous(true);
                newCard.setMonstrosityNum(c.getMonstrosityNum());
            }
            if (c.isRenowned()) {
                newCard.setRenowned(true);
            }
            if (c.isPlaneswalker()) {
                for (SpellAbility sa : c.getAllSpellAbilities()) {
                    SpellAbilityRestriction restrict = sa.getRestrictions();
                    if (restrict.isPwAbility() && restrict.getNumberTurnActivations() > 0) {
                        SpellAbility newSa = findSAInCard(sa, newCard);
                        if (newSa != null) {
                            for (int i = 0; i < restrict.getNumberTurnActivations(); i++) {
                                newSa.getRestrictions().abilityActivated();
                            }
                        }
                    }
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
            if (c.getChosenPlayer() != null) {
                newCard.setChosenPlayer(playerMap.get(c.getChosenPlayer()));
            }
            if (!c.getChosenType().isEmpty()) {
                newCard.setChosenType(c.getChosenType());
            }
            if (c.getChosenColors() != null) {
                newCard.setChosenColors(Lists.newArrayList(c.getChosenColors()));
            }
            if (!c.getNamedCard().isEmpty()) {
                newCard.setNamedCard(c.getNamedCard());
            }

            // TODO: FIXME
            if (c.hasRemembered()) {
                for (Object o : c.getRemembered()) {
                    System.out.println("Remembered: " + o +  o.getClass());
                    //newCard.addRemembered(o);
                }
            }
        }

        if (zone == ZoneType.Stack) {
            newGame.getStackZone().add(newCard);
        } else {
            zoneOwner.getZone(zone).add(newCard);
        }
    }
    
    private static SpellAbility findSAInCard(SpellAbility sa, Card c) {
        String saDesc = sa.getDescription();
        for (SpellAbility cardSa : c.getAllSpellAbilities()) {
            if (saDesc.equals(cardSa.getDescription())) {
                return cardSa;
            }
        }
        return null;
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
        result = playerMap.get(o);
        if (result != null)
            return result;
        if (o != null)
            throw new RuntimeException("Couldn't map " + o + "/" + System.identityHashCode(o));
        return null;
    }
    public GameObject reverseFind(GameObject o) {
        GameObject result = cardMap.inverse().get(o);
        if (result != null)
            return result;
        // TODO: Have only one GameObject map?
        return playerMap.inverse().get(o);
    }
}
