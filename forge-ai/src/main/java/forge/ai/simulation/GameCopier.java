package forge.ai.simulation;

import com.google.common.collect.*;
import forge.LobbyPlayer;
import forge.ai.AIOption;
import forge.ai.LobbyPlayerAi;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardType;
import forge.game.*;
import forge.game.ability.effects.DetachedCardEffect;
import forge.game.card.Card;
import forge.game.card.CardCloneStates;
import forge.game.card.CardCopyService;
import forge.game.card.CounterType;
import forge.game.card.token.TokenInfo;
import forge.game.combat.Combat;
import forge.game.mana.Mana;
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
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private GameSnapshot snapshot = null;

    public GameCopier(Game origGame) {
        this.origGame = origGame;
        if (origGame.EXPERIMENTAL_RESTORE_SNAPSHOT) {
            this.snapshot = new GameSnapshot(origGame);
        }
    }

    public Game getOriginalGame() {
        return origGame;
    }

    public Game getCopiedGame() {
        return gameObjectMap.getGame();
    }

    public Game makeCopy() {
        return makeCopy(null, null);
    }
    public Game makeCopy(PhaseType advanceToPhase, Player aiPlayer) {
        if (origGame.EXPERIMENTAL_RESTORE_SNAPSHOT) {
            // How do we advance to phase when using restores?
            return snapshot.makeCopy();
        }

        List<RegisteredPlayer> origPlayers = origGame.getMatch().getPlayers();
        List<RegisteredPlayer> newPlayers = new ArrayList<>();
        for (RegisteredPlayer p : origPlayers) {
            newPlayers.add(clonePlayer(p));
        }

        GameRules currentRules = origGame.getRules();
        Match newMatch = new Match(currentRules, newPlayers, origGame.getView().getTitle());
        Game newGame = new Game(newPlayers, currentRules, newMatch);
        newGame.dangerouslySetTimestamp(origGame.getTimestamp());

        for (int i = 0; i < origGame.getPlayers().size(); i++) {
            Player origPlayer = origGame.getPlayers().get(i);
            Player newPlayer = newGame.getPlayer(origPlayer.getId());
            newPlayer.setLife(origPlayer.getLife(), null);
            newPlayer.setLifeLostLastTurn(origPlayer.getLifeLostLastTurn());
            newPlayer.setLifeLostThisTurn(origPlayer.getLifeLostThisTurn());
            newPlayer.setLifeGainedThisTurn(origPlayer.getLifeGainedThisTurn());
            newPlayer.setCommitedCrimeThisTurn(origPlayer.getCommittedCrimeThisTurn());
            newPlayer.setLifeStartedThisTurnWith(origPlayer.getLifeStartedThisTurnWith());
            newPlayer.setDamageReceivedThisTurn(origPlayer.getDamageReceivedThisTurn());
            newPlayer.setLandsPlayedThisTurn(origPlayer.getLandsPlayedThisTurn());
            newPlayer.setCounters(Maps.newHashMap(origPlayer.getCounters()));
            newPlayer.setSpeed(origPlayer.getSpeed());
            newPlayer.setBlessing(origPlayer.hasBlessing(), null);
            newPlayer.setDescended(origPlayer.getDescended());
            newPlayer.setLibrarySearched(origPlayer.getLibrarySearched());
            newPlayer.setSpellsCastLastTurn(origPlayer.getSpellsCastLastTurn());
            for (int j = 0; j < origPlayer.getSpellsCastThisTurn(); j++) {
                newPlayer.addSpellCastThisTurn();
            }
            newPlayer.setMaxHandSize(origPlayer.getMaxHandSize());
            newPlayer.setUnlimitedHandSize(origPlayer.isUnlimitedHandSize());
            newPlayer.setCrankCounter(origPlayer.getCrankCounter());
            // TODO creatureAttackedThisTurn
            for (Mana m : origPlayer.getManaPool()) {
                newPlayer.getManaPool().addMana(m, false);
            }
            playerMap.put(origPlayer, newPlayer);
        }

        PhaseHandler origPhaseHandler = origGame.getPhaseHandler();
        Player newPlayerTurn = playerMap.get(origPhaseHandler.getPlayerTurn());
        newGame.getPhaseHandler().devModeSet(origPhaseHandler.getPhase(), newPlayerTurn, origPhaseHandler.getTurn());
        newGame.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        for (Player p : newGame.getPlayers()) {
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(false);
        }

        copyGameState(newGame, aiPlayer);

        for (Player origPlayer : playerMap.keySet()) {
            Player newPlayer = playerMap.get(origPlayer);
            origPlayer.copyCommandersToSnapshot(newPlayer, gameObjectMap::map);
            ((PlayerZoneBattlefield) newPlayer.getZone(ZoneType.Battlefield)).setTriggers(true);
        }
        newGame.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        for (Card c : newGame.getCardsInGame()) {
            Card origCard = (Card) reverseFind(c);
            if (origCard.hasRemembered()) {
                for (Object o : origCard.getRemembered()) {
                    if (o instanceof GameObject) {
                        // Sometimes, a spell can "remember" a token card that's not in any zone
                        // (and thus wouldn't have been copied) - for example Swords to Plowshares
                        // remembering its target for LKI. Skip these to not crash in find().
                        if (o instanceof Card && ((Card)o).getZone() == null) {
                           continue;
                        }
                        c.addRemembered(find((GameObject) o));
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

        if (origPhaseHandler.getCombat() != null) {
            newGame.getPhaseHandler().setCombat(new Combat(origPhaseHandler.getCombat(), gameObjectMap));
        }

        newGame.getAction().checkStateEffects(true); //ensure state based effects and triggers are updated
        newGame.getTriggerHandler().resetActiveTriggers();

        if (GameSimulator.COPY_STACK)
            copyStack(origGame, newGame, gameObjectMap);

        // TODO update thisTurnCast

        if (advanceToPhase != null) {
            newGame.getPhaseHandler().devAdvanceToPhase(advanceToPhase, () -> GameSimulator.resolveStack(newGame, aiPlayer.getWeakestOpponent()));
        }

        return newGame;
    }

    private static void copyStack(Game origGame, Game newGame, IEntityMap map) {
        for (SpellAbilityStackInstance origEntry : origGame.getStack()) {
            SpellAbility origSa = origEntry.getSpellAbility();
            Card origHostCard = origSa.getHostCard();
            Card newCard = map.map(origHostCard);
            SpellAbility newSa = null;
            if (origSa.isSpell()) {
                newSa = findSAInCard(origSa, newCard);
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
            // TODO should probably also override them if they're normal AI
            lp = new LobbyPlayerAi(p.getPlayer().getName(), Sets.newHashSet(AIOption.USE_SIMULATION));
        }
        clone.setPlayer(lp);
        return clone;
    }

    private void copyGameState(Game newGame, Player aiPlayer) {
        newGame.EXPERIMENTAL_RESTORE_SNAPSHOT = origGame.EXPERIMENTAL_RESTORE_SNAPSHOT;
        newGame.AI_TIMEOUT = origGame.AI_TIMEOUT;
        newGame.AI_CAN_USE_TIMEOUT = origGame.AI_CAN_USE_TIMEOUT;
        newGame.setAge(origGame.getAge());

        // TODO countersAddedThisTurn

        if (origGame.getStartingPlayer() != null) {
            newGame.setStartingPlayer(playerMap.get(origGame.getStartingPlayer()));
        }
        if (origGame.getMonarch() != null) {
            newGame.setMonarch(playerMap.get(origGame.getMonarch()));
        }
        if (origGame.getMonarchBeginTurn() != null) {
            newGame.setMonarchBeginTurn(playerMap.get(origGame.getMonarchBeginTurn()));
        }
        if (origGame.getHasInitiative() != null) {
            newGame.setHasInitiative(playerMap.get(origGame.getHasInitiative()));
        }
        if (origGame.getDayTime() != null) {
            newGame.setDayTime(origGame.getDayTime());
        }

        for (ZoneType zone : ZONES) {
            for (Card card : origGame.getCardsIn(zone)) {
                addCard(newGame, zone, card, aiPlayer);
            }
            // TODO CardsAddedThisTurn is now messed up
        }
        gameObjectMap = new CopiedGameObjectMap(newGame);

        for (Card card : origGame.getCardsIn(ZoneType.Battlefield)) {
            Card otherCard = cardMap.get(card);
            otherCard.setGameTimestamp(card.getGameTimestamp());
            otherCard.setLayerTimestamp(card.getLayerTimestamp());
            otherCard.setSickness(card.hasSickness());
            otherCard.setState(card.getCurrentStateName(), false);
            if (card.isAttachedToEntity()) {
                GameEntity ge = gameObjectMap.map(card.getEntityAttachedTo());
                otherCard.setEntityAttachedTo(ge);
                ge.addAttachedCard(otherCard);
            }
            if (card.getCrewedByThisTurn() != null) {
                otherCard.setCrewedByThisTurn(card.getCrewedByThisTurn());
            }
            if (card.getCloneOrigin() != null) {
                otherCard.setCloneOrigin(cardMap.get(card.getCloneOrigin()));
            }
            if (card.getHaunting() != null) {
                otherCard.setHaunting(cardMap.get(card.getHaunting()));
            }
            if (card.getSaddledByThisTurn() != null) {
                otherCard.setSaddledByThisTurn(card.getSaddledByThisTurn());
            }
            if (card.getEffectSource() != null) {
                otherCard.setEffectSource(cardMap.get(card.getEffectSource()));
            }
            if (card.isPaired()) {
                otherCard.setPairedWith(cardMap.get(card.getPairedWith()));
            }
            if (card.getCopiedPermanent() != null) {
                // TODO would it be safe to simply reuse the prototype?
                otherCard.setCopiedPermanent(new CardCopyService(card.getCopiedPermanent()).copyCard(false));
            }
            // TODO: Verify that the above relationships are preserved bi-directionally or not.
        }
    }

    private static PaperCard hidden_info_card = new PaperCard(CardRules.fromScript(Lists.newArrayList("Name:hidden", "Types:Artifact", "Oracle:")), "", CardRarity.Common);
    private static final boolean PRUNE_HIDDEN_INFO = false;
    private static final boolean USE_FROM_PAPER_CARD = true;
    private Card createCardCopy(Game newGame, Player newOwner, Card c, Player aiPlayer) {
        if (c.isToken() && !c.isImmutable()) {
            Card result = new TokenInfo(c).makeOneToken(newOwner);
            new CardCopyService(c).copyCopiableCharacteristics(result, null, null);
            return result;
        }
        if (USE_FROM_PAPER_CARD && !c.isImmutable() && c.getPaperCard() != null) {
            Card newCard;
            if (PRUNE_HIDDEN_INFO && !c.getView().canBeShownTo(aiPlayer.getView())) {
                // TODO also check REVEALED_CARDS memory
                newCard = new Card(newGame.nextCardId(), hidden_info_card, newGame);
                newCard.setOwner(newOwner);
            } else {
                newCard = Card.fromPaperCard(c.getPaperCard(), newOwner);
            }
            newCard.setCommander(c.isCommander());
            return newCard;
        }

        // TODO: The above is very expensive and accounts for the vast majority of GameCopier execution time.
        // The issue is that it requires parsing the original card from scratch from the paper card. We should
        // improve the copier to accurately copy the card from its actual state, so that the paper card shouldn't
        // be needed. Once the below code accurately copies the card, remove the USE_FROM_PAPER_CARD code path.
        Card newCard;
        if (c instanceof DetachedCardEffect)
            newCard = new DetachedCardEffect((DetachedCardEffect) c, newGame, true);
        else
            newCard = new Card(newGame.nextCardId(), c.getPaperCard(), newGame);
        newCard.setOwner(newOwner);
        newCard.setName(c.getName());
        newCard.setCommander(c.isCommander());
        newCard.setType(new CardType(c.getType()));
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

    private void addCard(Game newGame, ZoneType zone, Card c, Player aiPlayer) {
        final Player owner = playerMap.get(c.getOwner());
        final Card newCard = createCardCopy(newGame, owner, c, aiPlayer);
        cardMap.put(c, newCard);

        // TODO ExiledWith

        Player zoneOwner = owner;
        // everything the CreatureEvaluator checks must be set here
        if (zone == ZoneType.Battlefield) {
            // TODO: Controllers' list with timestamps should be copied.
            zoneOwner = playerMap.get(c.getController());
            newCard.setController(zoneOwner, 0);

            if (c.isBattle()) {
                newCard.setProtectingPlayer(playerMap.get(c.getProtectingPlayer()));
            }

            newCard.setCameUnderControlSinceLastUpkeep(c.cameUnderControlSinceLastUpkeep());

            newCard.setPTTable(c.getSetPTTable());
            newCard.setPTCharacterDefiningTable(c.getSetPTCharacterDefiningTable());

            newCard.setPTBoost(c.getPTBoostTable());
            // TODO copy by map
            newCard.setDamage(c.getDamage());
            newCard.setDamageReceivedThisTurn(c.getDamageReceivedThisTurn());

            newCard.copyFrom(c);

            for (Table.Cell<Long, Long, List<String>> kw : c.getHiddenExtrinsicKeywordsTable().cellSet()) {
                newCard.addHiddenExtrinsicKeywords(kw.getRowKey(), kw.getColumnKey(), kw.getValue());
            }
            newCard.updateKeywordsCache();

            if (c.isTapped()) {
                newCard.setTapped(true);
            }
            if (c.isFaceDown()) {
                newCard.turnFaceDown(true);
                if (c.isManifested()) {
                    newCard.setManifested(c.getManifestedSA());
                }
                if (c.isCloaked()) {
                    newCard.setCloaked(c.getCloakedSA());
                }
            }
            if (c.isMonstrous()) {
                newCard.setMonstrous(true);
            }
            if (c.isRenowned()) {
                newCard.setRenowned(true);
            }
            if (c.isSolved()) {
                newCard.setSolved(true);
            }
            if (c.isSaddled()) {
                newCard.setSaddled(true);
            }
            if (c.isSuspected()) {
                newCard.setSuspected(true);
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
            if (c.hasChosenPlayer()) {
                newCard.setChosenPlayer(playerMap.get(c.getChosenPlayer()));
            }
            if (c.hasChosenType()) {
                newCard.setChosenType(c.getChosenType());
            }
            if (c.hasChosenType2()) {
                newCard.setChosenType2(c.getChosenType2());
            }
            if (c.hasChosenColor()) {
                newCard.setChosenColors(Lists.newArrayList(c.getChosenColors()));
            }
            if (c.hasNamedCard()) {
                newCard.setNamedCards(Lists.newArrayList(c.getNamedCards()));
            }

            newCard.setSprocket(c.getSprocket());

            newCard.setSVars(c.getSVars());
            newCard.copyChangedSVarsFrom(c);
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

    private class CopiedGameObjectMap implements IEntityMap {
        private final Game copiedGame;

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
        if (origGame.EXPERIMENTAL_RESTORE_SNAPSHOT) {
            return snapshot.find(o);
        }

        GameObject result = null;
        if (o instanceof Card) {
            result = cardMap.get(o);
            if (result != null) {
                return result;
            } else {
                System.out.println("Couldn't map " + o + "/" + System.identityHashCode(o));
            }
        } else if (o instanceof Player) {
            result = playerMap.get(o);
            if (result != null)
                return result;
        }
        if (o != null)
            throw new RuntimeException("Couldn't map " + o + "/" + System.identityHashCode(o));
        return result;
    }
    public GameObject reverseFind(GameObject o) {
        if (origGame.EXPERIMENTAL_RESTORE_SNAPSHOT) {
            return snapshot.reverseFind(o);
        }

        GameObject result = cardMap.inverse().get(o);
        if (result != null)
            return result;
        // TODO: Have only one GameObject map?
        return playerMap.inverse().get(o);
    }
}
