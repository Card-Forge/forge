package forge.game;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.mana.Mana;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;

public class GameSnapshot {
    private final Game origGame;
    private Game newGame = null;

    private final SnapshotEntityMap gameObjectMap = new SnapshotEntityMap();

    public GameSnapshot(Game origGame) {
        this.origGame = origGame;
    }

    public Game getCopiedGame() {
        return origGame;
    }

    public Game makeCopy() {
        return makeCopy(null, true);
    }
    public Game makeCopy(List<RegisteredPlayer> replacementPlayers, boolean includeStack) {
        List<RegisteredPlayer> newPlayers;
        if (replacementPlayers != null) {
            newPlayers = replacementPlayers;
        } else {
            // Create new RegisteredPlayers based off original Match RPs
            newPlayers = Lists.newArrayList(origGame.getMatch().getPlayers());
        }
        GameRules currentRules = origGame.getRules();
        Match newMatch = new Match(currentRules, newPlayers, origGame.getView().getTitle());
        newGame = new Game(newPlayers, currentRules, newMatch);
        assignGameState(origGame, newGame, includeStack, false);
        //System.out.println("Storing game state with timestamp of :" + origGame.getTimestamp());

        return newGame;
    }

    public void restoreGameState(Game currentGame) {
        System.out.println("Restoring game state with timestamp of :" + newGame.getTimestamp());

        assignGameState(newGame, currentGame, true, true);
    }

    public void assignGameState(Game fromGame, Game toGame, boolean includeStack, boolean restore) {
        for (int i = 0; i < fromGame.getPlayers().size(); i++) {
            Player origPlayer = fromGame.getPlayers().get(i);
            Player newPlayer = findBy(toGame, origPlayer);
            assignPlayerState(origPlayer, newPlayer, restore);
        }

        PhaseHandler origPhaseHandler = fromGame.getPhaseHandler();
        Player newPlayerTurn = findBy(toGame, origPhaseHandler.getPlayerTurn());
        toGame.getPhaseHandler().devModeSet(origPhaseHandler.getPhase(), newPlayerTurn, origPhaseHandler.getTurn());
        toGame.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        for (Player p : toGame.getPlayers()) {
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(false);
        }

        copyGameState(fromGame, toGame, restore);

        for (Player p : fromGame.getPlayers()) {
            Player toPlayer = findBy(toGame, p);

            List<Card> commanders = Lists.newArrayList();
            for (final Card c : p.getCommanders()) {
                Card newCommander = findBy(toGame, c);
                commanders.add(newCommander);
                int castTimes = p.getCommanderCast(c);
                for (int i = 0; i < castTimes; i++) {
                    toPlayer.incCommanderCast(c);
                }
            }
            toPlayer.setCommanders(commanders);

            for (Map.Entry<Card, Integer> entry : p.getCommanderDamage()) {
                toPlayer.addCommanderDamage(findBy(toGame, entry.getKey()), entry.getValue());
            }
            ((PlayerZoneBattlefield) toPlayer.getZone(ZoneType.Battlefield)).setTriggers(true);
        }
        toGame.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        for (Card c : toGame.getCardsInGame()) {
            Card origCard = fromGame.findById(c.getId());

            if (origCard == null) {
                // This card doesn't exist in original state
                // What does that mean?
                System.out.println("Missing card " + c);
                continue;
            }

            // Why is this here? This whole area seems wrong
            if (origCard.hasRemembered()) {
                for (Object o : origCard.getRemembered()) {
                    if (o instanceof GameObject) {
                        // Sometimes, a spell can "remember" a token card that's not in any zone
                        // (and thus wouldn't have been copied) - for example Swords to Plowshares
                        // remembering its target for LKI. Skip these to not crash in find().
                        if (o instanceof Card && ((Card)o).getZone() == null) {
                            continue;
                        }
                        // Fix this with something else
                        c.addRemembered(find((GameObject) o));
                    } else {
                        System.err.println(c + " Remembered: " + o + "/" + o.getClass());
                        c.addRemembered(o);
                    }
                }
            }
            // I think this is still wrong, but might be needed for within cost payment?
            for (SpellAbility sa : c.getSpellAbilities()) {
                Player activatingPlayer = sa.getActivatingPlayer();
                if (activatingPlayer != null && activatingPlayer.getGame() != toGame) {
                    sa.setActivatingPlayer(findBy(toGame, activatingPlayer), true);
                }
            }
        }

        // Undo effects first before calculating them below, to avoid them applying twice.
//        for (StaticEffect effect : fromGame.getStaticEffects().getEffects()) {
//            effect.removeMapped(gameObjectMap);
//        }

        if (origPhaseHandler.getCombat() != null) {
            toGame.getPhaseHandler().setCombat(new Combat(origPhaseHandler.getCombat(), gameObjectMap));
        }

        // I think re-assigning this is killing something?
        //toGame.getAction().checkStateEffects(true); //ensure state based effects and triggers are updated
//        toGame.getTriggerHandler().resetActiveTriggers();

        if (includeStack) {
            copyStack(fromGame, toGame);
        }

        if (restore) {
            for (Player p : toGame.getPlayers()) {
                p.updateAllZonesForView();
            }

            System.out.println("RESTORED");
        }

        // TODO update thisTurnCast
    }

    public void assignPlayerState(Player origPlayer, Player newPlayer, boolean restore) {
        if (restore) {
            origPlayer.dangerouslySetController(newPlayer.getController());
        }
        newPlayer.setLife(origPlayer.getLife(), null);
        newPlayer.setLifeLostLastTurn(origPlayer.getLifeLostLastTurn());
        newPlayer.setLifeLostThisTurn(origPlayer.getLifeLostThisTurn());
        newPlayer.setLifeGainedThisTurn(origPlayer.getLifeGainedThisTurn());
        newPlayer.setLifeStartedThisTurnWith(origPlayer.getLifeStartedThisTurnWith());
        newPlayer.setDamageReceivedThisTurn(origPlayer.getDamageReceivedThisTurn());
        newPlayer.setActivateLoyaltyAbilityThisTurn(origPlayer.getActivateLoyaltyAbilityThisTurn());
        newPlayer.setLandsPlayedThisTurn(origPlayer.getLandsPlayedThisTurn());
        newPlayer.setCounters(Maps.newHashMap(origPlayer.getCounters()));
        newPlayer.setBlessing(origPlayer.hasBlessing());
        newPlayer.setRevolt(origPlayer.hasRevolt());
        newPlayer.setLibrarySearched(origPlayer.getLibrarySearched());
        newPlayer.setSpellsCastLastTurn(origPlayer.getSpellsCastLastTurn());
        for (int j = 0; j < origPlayer.getSpellsCastThisTurn(); j++) {
            newPlayer.addSpellCastThisTurn();
        }
        newPlayer.setMaxHandSize(origPlayer.getMaxHandSize());
        newPlayer.setUnlimitedHandSize(origPlayer.isUnlimitedHandSize());
        // TODO creatureAttackedThisTurn
        for (Mana m : origPlayer.getManaPool()) {
            // TODO Mana pool isn't being restored properly?
            newPlayer.getManaPool().addMana(m, false);
        }
        newPlayer.setCommanders(origPlayer.getCommanders()); // will be fixed up below
    }

    private void copyStack(Game fromGame, Game toGame) {
        for (SpellAbilityStackInstance origEntry : fromGame.getStack()) {
            SpellAbility origSa = origEntry.getSpellAbility();
            Card origHostCard = origSa.getHostCard();
            Card newCard = findBy(toGame, origHostCard);

            if (newCard == null) {
                // IF this card isn't in future world, it's likely a copy
                newCard = createCardCopy(toGame, findBy(toGame, origHostCard.getOwner()), origHostCard);
            }

            SpellAbility newSa = null;
            if (origSa.isSpell()) {
                newSa = findSAInCard(origSa, newCard);
            }
            if (newSa != null) {
                newSa.setActivatingPlayer(findBy(toGame, origSa.getActivatingPlayer()), true);
                if (origSa.usesTargeting()) {
                    for (GameObject o : origSa.getTargets()) {
                        if (o instanceof Card) {
                            newSa.getTargets().add(findBy(toGame, (Card) o));
                        } else if (o instanceof Player) {
                            newSa.getTargets().add(findBy(toGame, (Player) o));
                        } else {
                            System.out.println("Failed to restore target " + o + " for " + origSa);
                        }
                    }
                }
                toGame.getStack().add(newSa);
            }
        }
    }

    public void copyGameState(Game fromGame, Game toGame, boolean restore) {
        toGame.setAge(fromGame.getAge());
        toGame.dangerouslySetTimestamp(fromGame.getTimestamp());

        // TODO countersAddedThisTurn

        // I think this is slightly wrong. I Don't think we need player maps
        if (fromGame.getStartingPlayer() != null) {
            toGame.setStartingPlayer(findBy(toGame, fromGame.getStartingPlayer()));
        }
        if (fromGame.getMonarch() != null) {
            toGame.setMonarch(findBy(toGame, fromGame.getMonarch()));
        }
        if (fromGame.getMonarchBeginTurn() != null) {
            toGame.setMonarchBeginTurn(findBy(toGame, fromGame.getMonarchBeginTurn()));
        }
        if (fromGame.getHasInitiative() != null) {
            toGame.setHasInitiative(findBy(toGame, fromGame.getHasInitiative()));
        }
        if (fromGame.getDayTime() != null) {
            toGame.setDayTime(fromGame.getDayTime());
        }

        for(Card fromCard : fromGame.getCardsInGame()) {
            Card newCard = toGame.findById(fromCard.getId());
            Player toPlayer = findBy(toGame, fromCard.getController());
            ZoneType fromType = fromCard.getZone().getZoneType();

            if (newCard == null) {
                // Storing a game uses this path...
                newCard = createCardCopy(toGame, toPlayer, fromCard);
            } else {
                ZoneType type = newCard.getZone().getZoneType();
                if (type != fromType) {
                    if (type.equals(ZoneType.Stack)) {
                        toGame.getStackZone().remove(newCard);
                    } else {
                        toPlayer.getZone(type).remove(newCard);
                    }
                }
            }

            if (fromType.equals(ZoneType.Stack)) {
                toGame.getStackZone().add(newCard);
            } else {
                toPlayer.getZone(fromType).add(newCard);
            }

            // TODO: This is a bit of a mess. We should probably have a method to copy a card's state.
            newCard.setGameTimestamp(fromCard.getGameTimestamp());
            newCard.setLayerTimestamp(fromCard.getLayerTimestamp());
            newCard.setZone(fromCard.getZone());
            newCard.setTapped(fromCard.isTapped());
            newCard.setFaceDown(fromCard.isFaceDown());
            newCard.setManifested(fromCard.isManifested());
            newCard.setSickness(fromCard.hasSickness());
            newCard.setState(fromCard.getCurrentStateName(), false);
        }

        // This loop happens later to make sure all cards are in the correct zone first
        for (Card newCard : toGame.getCardsIn(ZoneType.Battlefield)) {
            Card fromCard = fromGame.findById(newCard.getId());

            if (fromCard.isAttachedToEntity()) {
                Card fromAttachedTo = fromCard.getAttachedTo();
                Card newAttachedTo = fromAttachedTo == null ? null : toGame.findById(fromAttachedTo.getId());
                if (newAttachedTo != null) {
                    newCard.setEntityAttachedTo(newAttachedTo);
                    newAttachedTo.addAttachedCard(newCard);
                }
            }
            if (fromCard.getCloneOrigin() != null) {
                newCard.setCloneOrigin(toGame.findById(fromCard.getCloneOrigin().getId()));
            }
            if (newCard.getHaunting() != null) {
                newCard.setHaunting(toGame.findById(fromCard.getHaunting().getId()));
            }
            if (newCard.getEffectSource() != null) {
                newCard.setEffectSource(toGame.findById(fromCard.getEffectSource().getId()));
            }
            if (newCard.isPaired()) {
                newCard.setPairedWith(toGame.findById(fromCard.getPairedWith().getId()));
            }
            if (newCard.getCopiedPermanent() != null) {
                newCard.setCopiedPermanent(toGame.findById(fromCard.getCopiedPermanent().getId()));
            }
            // TODO: Verify that the above relationships are preserved bi-directionally or not.
        }
    }

    private Card createCardCopy(Game newGame, Player newOwner, Card c) {
        Card newCard = new CardCopyService(c, newGame).copyCard(false, newOwner);
        newCard.dangerouslySetGame(newGame);
        return newCard;
    }

    private void addCard(Game toGame, ZoneType zone, Card c) {
        // Can i delete this?
        final Player owner = findBy(toGame, c.getOwner());
        final Card newCard = createCardCopy(toGame, owner, c);

        // TODO ExiledWith

        Player zoneOwner = owner;
        // everything the CreatureEvaluator checks must be set here
        if (zone == ZoneType.Battlefield) {
            zoneOwner = findBy(toGame, c.getController());
            // TODO: Controllers' list with timestamps should be copied.
            newCard.setController(zoneOwner, 0);

            if (c.isBattle()) {
                newCard.setProtectingPlayer(findBy(toGame, c.getProtectingPlayer()));
            }

            newCard.setCameUnderControlSinceLastUpkeep(c.cameUnderControlSinceLastUpkeep());

            newCard.setPTTable(c.getSetPTTable());
            newCard.setPTCharacterDefiningTable(c.getSetPTCharacterDefiningTable());

            newCard.setPTBoost(c.getPTBoostTable());
            // TODO copy by map
            newCard.setDamage(c.getDamage());
            newCard.setDamageReceivedThisTurn(c.getDamageReceivedThisTurn());

            newCard.setChangedCardColors(c.getChangedCardColorsTable());
            newCard.setChangedCardColorsCharacterDefining(c.getChangedCardColorsCharacterDefiningTable());

            newCard.setChangedCardTypes(c.getChangedCardTypesTable());
            newCard.setChangedCardTypesCharacterDefining(c.getChangedCardTypesCharacterDefiningTable());
            newCard.setChangedCardKeywords(c.getChangedCardKeywords());
            newCard.setChangedCardNames(c.getChangedCardNames());

            for (Table.Cell<Long, Long, List<String>> kw : c.getHiddenExtrinsicKeywordsTable().cellSet()) {
                newCard.addHiddenExtrinsicKeywords(kw.getRowKey(), kw.getColumnKey(), kw.getValue());
            }
            newCard.updateKeywordsCache(newCard.getCurrentState());

            // Is any of this really needed?
            newCard.setTapped(c.isTapped());

            if (c.isFaceDown()) {
                newCard.turnFaceDown(c.isFaceDown());
            }
            newCard.setManifested(c.isManifested());

            newCard.setMonstrous(c.isMonstrous());
            newCard.setRenowned(c.isRenowned());
            if (c.isPlaneswalker()) {
                // Why is this limited to planeswalkers?
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
                newCard.setChosenPlayer(findBy(toGame, c.getChosenPlayer()));
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
            newCard.setSVars(c.getSVars());
            newCard.copyChangedSVarsFrom(c);
        }

        if (zone == ZoneType.Stack) {
            toGame.getStackZone().add(newCard);
        } else {
            zoneOwner.getZone(zone).add(newCard);
        }
        // Update view?
    }

    private static SpellAbility findSAInCard(SpellAbility sa, Card c) {
        String saDesc = sa.getDescription();
        for (SpellAbility cardSa : c.getAllSpellAbilities()) {
            if (saDesc.equals(cardSa.getDescription())) {
                return cardSa;
            }
        }

        Map<String, String> origMap = sa.getOriginalMapParams();
        for (SpellAbility cardSa : c.getAllSpellAbilities()) {
            if (origMap.equals(cardSa.getOriginalMapParams())) {
                return cardSa;
            }
        }


        return null;
    }

    public class SnapshotEntityMap implements IEntityMap {
        @Override
        public Game getGame() {
            return newGame;
        }

        @Override
        public GameObject map(GameObject o) {
            if (o instanceof Player) {
                return findBy(newGame, (Player) o);
            } else if (o instanceof Card) {
                return findBy(newGame, (Card) o);
            }
            return null;
        }

        @Override
        public Card map(final Card c) {
            return findBy(newGame, c);
        }

        @Override
        public Player map(final Player p) {
            return findBy(newGame, p);
        }
    }

    private GameEntity findBy(Game toGame, GameEntity fromEntity) {
        if (fromEntity instanceof Card) {
            return toGame.findById(fromEntity.getId());
        } else if (fromEntity instanceof Player) {
            return toGame.getPlayer(fromEntity.getId());
        }

        return null;
    }

    private Card findBy(Game toGame, Card fromCard) {
        return toGame.findById(fromCard.getId());
    }

    private Player findBy(Game toGame, Player fromPlayer) {
        return toGame.getPlayer(fromPlayer.getId());
    }

    public GameObject find(GameObject o) {
        // Is this finding the object in the new game?
        if (o instanceof Card) {
            return findBy(newGame, (Card) o);
        } else if (o instanceof Player) {
            return findBy(newGame, (Player) o);
        }

        return null;
    }
    public GameObject reverseFind(GameObject o) {
        // Is this finding the object in the orig game?
        if (o instanceof Card) {
            return findBy(origGame, (Card) o);
        } else if (o instanceof Player) {
            return findBy(origGame, (Player) o);
        }

        return null;
    }
}

