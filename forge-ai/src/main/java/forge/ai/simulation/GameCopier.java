package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.LobbyPlayer;
import forge.ai.LobbyPlayerAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.GameObjectMap;
import forge.game.GameRules;
import forge.game.Match;
import forge.game.StaticEffect;
import forge.game.card.Card;
import forge.game.card.CardCloneStates;
import forge.game.card.CardFactory;
import forge.game.card.CounterType;
import forge.game.card.token.TokenInfo;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.staticability.StaticAbility;
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
    
    public Game getOriginalGame() {
        return origGame;
    }
    
    public Game getCopiedGame() {
        return gameObjectMap.getGame();
    }
    
    public Game makeCopy() {
        return makeCopy(null);
    }
    public Game makeCopy(PhaseType advanceToPhase) {
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
            newPlayer.setCounters(Maps.newHashMap(origPlayer.getCounters()));
            newPlayer.setLifeLostLastTurn(origPlayer.getLifeLostLastTurn());
            newPlayer.setLifeLostThisTurn(origPlayer.getLifeLostThisTurn());
            newPlayer.getManaPool().add(origPlayer.getManaPool());
            newPlayer.setCommanders(origPlayer.getCommanders()); // will be fixed up below
            playerMap.put(origPlayer, newPlayer);
        }

        PhaseHandler origPhaseHandler = origGame.getPhaseHandler();
        Player newPlayerTurn = playerMap.get(origPhaseHandler.getPlayerTurn());
        newGame.getPhaseHandler().devModeSet(origPhaseHandler.getPhase(), newPlayerTurn, origPhaseHandler.getTurn());
        newGame.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        for (Player p : newGame.getPlayers()) {
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(false);
        }
        
        copyGameState(newGame);

        for (Player p : newGame.getPlayers()) {
            List<Card> commanders = Lists.newArrayList();
            for (final Card c : p.getCommanders()) {
                commanders.add(gameObjectMap.map(c));
            }
            p.setCommanders(commanders);
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(true);
        }
        newGame.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        for (Card c : newGame.getCardsInGame()) {
            Card origCard = (Card) reverseFind(c);
            if (origCard.hasRemembered()) {
                for (Object o : origCard.getRemembered()) {
                    if (o instanceof GameObject) {
                        c.addRemembered(find((GameObject)o));
                    } else {
                        System.err.println(c + " Remembered: " + o + "/" + o.getClass());
                        c.addRemembered(o);
                    }
                    
                }
            }
            for (SpellAbility sa : c.getSpellAbilities()) {
                Player activatingPlayer = sa.getActivatingPlayer();
                if (activatingPlayer != null && activatingPlayer.getGame() != newGame) {
                    sa.setActivatingPlayer(gameObjectMap.map(activatingPlayer));
                }
            }
        }

        // Undo effects first before calculating them below, to avoid them applying twice.
        for (StaticEffect effect : origGame.getStaticEffects().getEffects()) {
            effect.removeMapped(gameObjectMap);
        }

        newGame.getAction().checkStateEffects(true); //ensure state based effects and triggers are updated
        newGame.getTriggerHandler().resetActiveTriggers();

        if (GameSimulator.COPY_STACK)
            copyStack(origGame, newGame, gameObjectMap);

        if (origPhaseHandler.getCombat() != null) {
            newGame.getPhaseHandler().setCombat(new Combat(origPhaseHandler.getCombat(), gameObjectMap));
        }

        if (advanceToPhase != null) {
            newGame.getPhaseHandler().devAdvanceToPhase(advanceToPhase);
        }
        
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
                    for (GameObject o : origSa.getTargets()) {
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
            otherCard.setState(card.getCurrentStateName(), false);
            if (card.isAttachedToEntity()) {
                GameEntity ge = gameObjectMap.map(card.getEntityAttachedTo());
                otherCard.setEntityAttachedTo(ge);
                ge.addAttachedCard(otherCard);
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
            // TODO: Verify that the above relationships are preserved bi-directionally or not.
        }
    }
    
    private static final boolean USE_FROM_PAPER_CARD = true;
    private Card createCardCopy(Game newGame, Player newOwner, Card c) {
        if (c.isToken() && !c.isImmutable()) {
            Card result = new TokenInfo(c).makeOneToken(newOwner);
            CardFactory.copyCopiableCharacteristics(c, result);
            return result;
        }
        if (USE_FROM_PAPER_CARD && !c.isImmutable() && c.getPaperCard() != null) {
            Card newCard = Card.fromPaperCard(c.getPaperCard(), newOwner);
            newCard.setCommander(c.isCommander());
            return newCard;
        }

        // TODO: The above is very expensive and accounts for the vast majority of GameCopier execution time.
        // The issue is that it requires parsing the original card from scratch from the paper card. We should
        // improve the copier to accurately copy the card from its actual state, so that the paper card shouldn't
        // be needed. Once the below code accurately copies the card, remove the USE_FROM_PAPER_CARD code path.
        Card newCard = new Card(newGame.nextCardId(), c.getPaperCard(), newGame);
        newCard.setOwner(newOwner);
        newCard.setName(c.getName());
        newCard.setCommander(c.isCommander());
        for (String type : c.getType()) {
            newCard.addType(type);
        }
        for (StaticAbility stAb : c.getStaticAbilities()) {
            newCard.addStaticAbility(stAb.copy(newCard, true));
        }
        for (SpellAbility sa : c.getSpellAbilities()) {
            SpellAbility saCopy = sa.copy(newCard, true);
            if (saCopy != null) {
                newCard.addSpellAbility(saCopy);
            } else {
                System.err.println(sa.toString());
            }
        }

        return newCard;
    }

    private void addCard(Game newGame, ZoneType zone, Card c) {
        final Player owner = playerMap.get(c.getOwner());
        final Card newCard = createCardCopy(newGame, owner, c);
        cardMap.put(c, newCard);

        Player zoneOwner = owner;
        if (zone == ZoneType.Battlefield) {
            // TODO: Controllers' list with timestamps should be copied.
            zoneOwner = playerMap.get(c.getController());
            newCard.setController(zoneOwner, 0);
            
            int setPower = c.getSetPower();
            int setToughness = c.getSetToughness();
            if (setPower != Integer.MAX_VALUE || setToughness != Integer.MAX_VALUE)  {
                // TODO: Copy the full list with timestamps.
                newCard.addNewPT(setPower, setToughness, newGame.getNextTimestamp());
            }
            newCard.setPTBoost(c.getPTBoostTable());
            newCard.setDamage(c.getDamage());

            newCard.setChangedCardColors(c.getChangedCardColorsMap());
            newCard.setChangedCardColorsCharacterDefining(c.getChangedCardColorsCharacterDefiningMap());

            newCard.setChangedCardTypes(c.getChangedCardTypesMap());
            newCard.setChangedCardTypesCharacterDefining(c.getChangedCardTypesCharacterDefiningMap());
            newCard.setChangedCardKeywords(c.getChangedCardKeywords());
            newCard.setChangedCardNames(c.getChangedCardNames());

            // TODO: Is this correct? Does it not duplicate keywords from enchantments and such?
            //for (KeywordInterface kw : c.getHiddenExtrinsicKeywords())
            //    newCard.addHiddenExtrinsicKeyword(kw);
            if (c.isTapped()) {
                newCard.setTapped(true);
            }
            if (c.isFaceDown()) {
                newCard.turnFaceDown(true);
                if (c.isManifested()) {
                    newCard.setManifested(true);
                }
            }
            if (c.isMonstrous()) {
                newCard.setMonstrous(true);
            }
            if (c.isRenowned()) {
                newCard.setRenowned(true);
            }
            if (c.isPlaneswalker()) {
                for (SpellAbility sa : c.getAllSpellAbilities()) {
                    int active = sa.getActivationsThisTurn();
                    if (sa.isPwAbility() && active > 0) {
                        SpellAbility newSa = findSAInCard(sa, newCard);
                        if (newSa != null) {
                            for (int i = 0; i < active; i++) {
                                newCard.addAbilityActivated(newSa);
                            }
                        }
                    }
                }
            }

            newCard.setFlipped(c.isFlipped());
            for (Map.Entry<Long, CardCloneStates> e : c.getCloneStates().entrySet()) {
                newCard.addCloneState(e.getValue().copy(newCard, true), e.getKey());
            }

            Map<CounterType, Integer> counters = c.getCounters();
            if (!counters.isEmpty()) {
                newCard.setCounters(Maps.newHashMap(counters));
            }
            if (c.getChosenPlayer() != null) {
                newCard.setChosenPlayer(playerMap.get(c.getChosenPlayer()));
            }
            if (!c.getChosenType().isEmpty()) {
                newCard.setChosenType(c.getChosenType());
            }
            if (!c.getChosenType2().isEmpty()) {
                newCard.setChosenType2(c.getChosenType2());
            }
            if (c.getChosenColors() != null) {
                newCard.setChosenColors(Lists.newArrayList(c.getChosenColors()));
            }
            if (!c.getNamedCard().isEmpty()) {
                newCard.setNamedCard(c.getNamedCard());
            }
            if (!c.getNamedCard2().isEmpty()) {
                newCard.setNamedCard2(c.getNamedCard());
            }
            newCard.setSVars(c.getSVars());
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
