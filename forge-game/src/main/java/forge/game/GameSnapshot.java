package forge.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import forge.game.card.Card;
import forge.game.card.CardCloneStates;
import forge.game.card.CardCollection;
import forge.game.card.CardCopyService;
import forge.game.combat.Combat;
import forge.game.event.GameEventSnapshotRestored;
import forge.game.mana.Mana;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameSnapshot {
    private final Game origGame;
    private Game newGame = null;
    private boolean restore = false;

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
        // Clear deck-construction cards — we'll populate zones from the original game's state
        for (Player p : newGame.getPlayers()) {
            for (ZoneType zt : ZoneType.values()) {
                PlayerZone zone = p.getZone(zt);
                if (zone != null) {
                    zone.removeAllCards(true);
                }
            }
        }
        restore = false;
        assignGameState(origGame, newGame, includeStack);
        //System.out.println("Storing game state with timestamp of :" + origGame.getTimestamp());

        return newGame;
    }

    public void restoreGameState(Game currentGame) {
        System.out.println("Restoring game state with timestamp of :" + newGame.getTimestamp());
        restore = true;

        currentGame.fireEvent(new GameEventSnapshotRestored(true));
        assignGameState(newGame, currentGame, true);
        currentGame.fireEvent(new GameEventSnapshotRestored(false));
    }

    public void assignGameState(Game fromGame, Game toGame, boolean includeStack) {
        for (int i = 0; i < fromGame.getPlayers().size(); i++) {
            Player origPlayer = fromGame.getPlayers().get(i);
            Player newPlayer = findBy(toGame, origPlayer);
            assignPlayerState(origPlayer, newPlayer);
        }

        PhaseHandler origPhaseHandler = fromGame.getPhaseHandler();
        Player newPlayerTurn = findBy(toGame, origPhaseHandler.getPlayerTurn());
        toGame.getPhaseHandler().devModeSet(origPhaseHandler.getPhase(), newPlayerTurn, origPhaseHandler.getTurn());
        toGame.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        for (Player p : toGame.getPlayers()) {
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(false);
        }

        copyGameState(fromGame, toGame);

        for (Player p : fromGame.getPlayers()) {
            Player toPlayer = findBy(toGame, p);
            p.copyCommandersToSnapshot(toPlayer, c -> findBy(toGame, c));
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

            // Clear and re-populate remembered objects to avoid duplicates on restore
            c.clearRemembered();
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
                    sa.setActivatingPlayer(findBy(toGame, activatingPlayer));
                }
            }
        }

        // Undo effects first before calculating them below, to avoid them applying twice.
        for (StaticEffect effect : fromGame.getStaticEffects().getEffects()) {
            effect.removeMapped(gameObjectMap);
        }

        if (origPhaseHandler.getCombat() != null) {
            Combat combat = new Combat(origPhaseHandler.getCombat(), gameObjectMap);
            toGame.getPhaseHandler().setCombat(combat);
            //System.out.println(origPhaseHandler.getCombat().toString());
        }

        toGame.getAction().checkStateEffects(true); //ensure state based effects and triggers are updated
        toGame.getTriggerHandler().resetActiveTriggers();

        if (includeStack) {
            copyStack(fromGame, toGame);
        }

        if (restore) {
            for (Player p : toGame.getPlayers()) {
                p.updateAllZonesForView();
            }

            Combat combat = toGame.getPhaseHandler().getCombat();
            if (combat != null) {
                //System.out.println(combat.toString());
                toGame.updateCombatForView();
            }
            //System.out.println("RESTORED");
        }

        // Copy MagicStack turn-tracking lists
        toGame.getStack().setSpellsCastThisTurn(Lists.newArrayList(fromGame.getStack().getSpellsCastThisTurn()));
        toGame.getStack().setSpellsCastLastTurn(Lists.newArrayList(fromGame.getStack().getSpellsCastLastTurn()));
        toGame.getStack().setAbilityActivatedThisTurn(Lists.newArrayList(fromGame.getStack().getAbilityActivatedThisTurn()));
    }

    public void assignPlayerState(Player origPlayer, Player newPlayer) {
        if (restore) {
            // Player controller of the original player isn't associated with the GUI at this point?
            origPlayer.dangerouslySetController(newPlayer.getController());
        }
        newPlayer.setLife(origPlayer.getLife(), null);
        newPlayer.setLifeLostLastTurn(origPlayer.getLifeLostLastTurn());
        newPlayer.setLifeLostThisTurn(origPlayer.getLifeLostThisTurn());
        newPlayer.setLifeGainedThisTurn(origPlayer.getLifeGainedThisTurn());
        newPlayer.setLifeStartedThisTurnWith(origPlayer.getLifeStartedThisTurnWith());
        newPlayer.setDamageReceivedThisTurn(origPlayer.getDamageReceivedThisTurn());
        newPlayer.setLandsPlayedThisTurn(origPlayer.getLandsPlayedThisTurn());
        newPlayer.setCounters(Maps.newHashMap(origPlayer.getCounters()));
        newPlayer.setBlessing(origPlayer.hasBlessing(), null);
        newPlayer.setLibrarySearched(origPlayer.getLibrarySearched());
        newPlayer.setSpellsCastLastTurn(origPlayer.getSpellsCastLastTurn());
        newPlayer.setCommitedCrimeThisTurn(origPlayer.getCommittedCrimeThisTurn());
        newPlayer.setExpentThisTurn(origPlayer.getExpentThisTurn());
        for (int j = 0; j < origPlayer.getSpellsCastThisTurn(); j++) {
            newPlayer.addSpellCastThisTurn();
        }
        newPlayer.setMaxHandSize(origPlayer.getMaxHandSize());
        newPlayer.setUnlimitedHandSize(origPlayer.isUnlimitedHandSize());
        newPlayer.setCrankCounter(origPlayer.getCrankCounter());
        newPlayer.setSpeed(origPlayer.getSpeed());
        newPlayer.setDescended(origPlayer.getDescended());
        newPlayer.setNumDrawnThisTurn(origPlayer.getNumDrawnThisTurn());
        newPlayer.setNumDrawnLastTurn(origPlayer.getNumDrawnLastTurn());
        newPlayer.setNumCardsInHandStartedThisTurnWith(origPlayer.getNumCardsInHandStartedThisTurnWith());
        newPlayer.setNumTokenCreatedThisTurn(origPlayer.getNumTokenCreatedThisTurn());
        newPlayer.setNumForetoldThisTurn(origPlayer.getNumForetoldThisTurn());
        newPlayer.setNumExploredThisTurn(origPlayer.getNumExploredThisTurn());
        newPlayer.setInvestigatedThisTurn(origPlayer.getInvestigateNumThisTurn());
        newPlayer.setSurveilThisTurn(origPlayer.getSurveilThisTurn());
        newPlayer.setNumRollsThisTurn(origPlayer.getNumRollsThisTurn());
        newPlayer.setVenturedThisTurn(origPlayer.getVenturedThisTurn());
        newPlayer.setAttractionsVisitedThisTurn(origPlayer.getAttractionsVisitedThisTurn());
        newPlayer.setNumRingTemptedYou(origPlayer.getNumRingTemptedYou());
        newPlayer.setTappedLandForManaThisTurn(origPlayer.hasTappedLandForManaThisTurn());
        newPlayer.setNumPowerSurgeLands(origPlayer.getNumPowerSurgeLands());

        // Per-turn tracking — counters and flags
        newPlayer.setLifeGainedTimesThisTurn(origPlayer.getLifeGainedTimesThisTurn());
        newPlayer.setLifeGainedByTeamThisTurn(origPlayer.getLifeGainedByTeamThisTurn());
        newPlayer.setNumDrawnThisDrawStep(origPlayer.numDrawnThisDrawStep());
        newPlayer.setLandsPlayedLastTurn(origPlayer.getLandsPlayedLastTurn());
        newPlayer.setSpellsCastThisGame(origPlayer.getSpellsCastThisGame());
        newPlayer.setNumFlipsThisTurn(origPlayer.getNumFlipsThisTurn());
        newPlayer.setSimultaneousDamage(origPlayer.getSimultaneousDamage());
        newPlayer.setLastTurnNr(origPlayer.getLastTurnNr());
        newPlayer.setTriedToDrawFromEmptyLibrary(origPlayer.getTriedToDrawFromEmptyLibrary());
        newPlayer.setDevotionMod(origPlayer.getDevotionMod());
        newPlayer.setNumManaShards(origPlayer.getNumManaShards());
        newPlayer.setNamedCard(origPlayer.getNamedCard());
        newPlayer.setBeenDealtCombatDamageSinceLastTurn(origPlayer.hasBeenDealtCombatDamageSinceLastTurn());

        // Per-turn tracking — lists
        newPlayer.setDiceRollsThisTurn(origPlayer.getDiceRollsThisTurn());
        newPlayer.setDiscardedThisTurn(origPlayer.getDiscardedThisTurn());
        newPlayer.setSacrificedThisTurn(origPlayer.getSacrificedThisTurn());
        newPlayer.setSpellsCastSinceBeginningOfLastTurn(origPlayer.getSpellsCastSinceBeginningOfLastTurn());
        newPlayer.setCompletedDungeons(origPlayer.getCompletedDungeons());
        newPlayer.setLostOwnership(new CardCollection(origPlayer.getLostOwnership()));
        newPlayer.setGainedOwnership(new CardCollection(origPlayer.getGainedOwnership()));
        newPlayer.setElementalBendThisTurn(origPlayer.getElementalBendThisTurn());

        // Combat tracking
        newPlayer.setAttackedThisTurn(origPlayer.getAttackedThisTurn());
        newPlayer.setAttackedPlayersMyLastTurn(origPlayer.getAttackedPlayersMyLastTurn());
        newPlayer.setAttackedPlayersThisCombat(origPlayer.getAttackedPlayersMyCombat());

        // Card references
        if (origPlayer.getLastDrawnCard() != null) {
            newPlayer.setLastDrawnCard(findBy(newPlayer.getGame(), origPlayer.getLastDrawnCard()));
        }

        // Planar/scheme state
        newPlayer.setCurrentPlanes(new CardCollection(origPlayer.getCurrentPlanes()));
        newPlayer.setPlaneswalkedToThisTurn(new CardCollection(origPlayer.getPlaneswalkedToThisTurn()));
        newPlayer.setActiveScheme(origPlayer.getActiveScheme());

        // Copy mana pool
        copyManaPool(origPlayer, newPlayer);
    }

    private void copyManaPool(Player fromPlayer, Player toPlayer) {
        Game toGame = toPlayer.getGame();
        toPlayer.getManaPool().resetPool();
        for (Mana m : fromPlayer.getManaPool()) {
            toPlayer.getManaPool().addMana(copyMana(m, toGame, toPlayer), false);
        }
        toPlayer.updateManaForView();
    }

    private Mana copyMana(Mana m, Game toGame, Player toPlayer) {
        Card fromCard = m.getSourceCard();
        Card toCard = findBy(toGame, fromCard);
        // Are we copying over mana abilities properly?
        if (toCard == null) {
            return m;
        }
        Mana newMana = new Mana(m.getColor(), toCard, m.getManaAbility(), toPlayer);
        newMana.getManaAbility().setSourceCard(toCard);
        return newMana;
    }

    private void copyStack(Game fromGame, Game toGame) {
        // Try to match the StackInstance ID. If we don't find it, generate a new stack instance that matches
        // If we do find it, we may need to alter the existing stack instance
        // If we find it and we're restoring, we dont need to do anything

        Map<Integer, SpellAbilityStackInstance> stackIds = new HashMap<>();
        for (SpellAbilityStackInstance toEntry : toGame.getStack()) {
            stackIds.put(toEntry.getId(), toEntry);
        }

        for (SpellAbilityStackInstance origEntry : fromGame.getStack()) {
            int id = origEntry.getId();
            SpellAbilityStackInstance instance = stackIds.getOrDefault(id, null);

            if (instance != null) {
                if (!restore) {
                    System.out.println("Might need to alter " + origEntry.getSpellAbility() + " on stack");
                }

                continue;
            }

            System.out.println("Adding " + origEntry.getSpellAbility() + " to stack");

            SpellAbility origSa = origEntry.getSpellAbility();
            Card origHostCard = origSa.getHostCard();
            Card newCard = findBy(toGame, origHostCard);

            if (newCard == null) {
                // IF this card isn't in future world, it's likely a copy
                newCard = createCardCopy(toGame, findBy(toGame, origHostCard.getOwner()), origHostCard);
            }

            // FInd newEntry from origEntrys

            SpellAbility newSa = null;
            if (origSa.isSpell()) {
                newSa = findSAInCard(origSa, newCard);
            }

            // Is the SA on the stack?
            if (newSa != null) {
                newSa.setActivatingPlayer(findBy(toGame, origSa.getActivatingPlayer()));
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
                toGame.getStack().add(newSa, id);
            }
        }
    }

    public void copyGameState(Game fromGame, Game toGame) {
        toGame.setAge(fromGame.getAge());
        toGame.dangerouslySetTimestamp(fromGame.getTimestamp());

        // Remove cards that were created after the snapshot (e.g., tokens, copies)
        if (restore) {
            // Remove cards created after the snapshot (e.g., tokens)
            List<Card> toRemove = Lists.newArrayList();
            for (Card currentCard : toGame.getCardsInGame()) {
                if (fromGame.findById(currentCard.getId()) == null) {
                    toRemove.add(currentCard);
                }
            }
            for (Card card : toRemove) {
                ZoneType zone = card.getZone().getZoneType();
                if (zone == ZoneType.Stack) {
                    toGame.getStackZone().remove(card);
                } else {
                    card.getOwner().getZone(zone).remove(card);
                }
            }

            // Note: we do NOT clear ordered zones here — that would leave them temporarily
            // empty, causing race conditions with the UI thread (e.g., hand drag operations).
            // Instead, we reorder them after all cards are placed (see below).
        }

        // TODO countersAddedThisTurn

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

        List<UnorderedEntities> unorderedEntities = Lists.newArrayList();

        for(Card fromCard : fromGame.getCardsInGame()) {
            Card newCard = toGame.findById(fromCard.getId());
            Player toPlayer = findBy(toGame, fromCard.getController());
            ZoneType fromType = fromCard.getZone().getZoneType();
            int zonePosition = 0;
            if (ZoneType.ORDERED_ZONES.contains(fromType)) {
                // If the card is in an ordered zone, we need to find its position in the zone
                // and set it in the new game.
                zonePosition = fromCard.getZone().getCards().indexOf(fromCard);
            }

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

            if (zonePosition == 0) {
                setCardInCopiedGame(toGame, toPlayer, fromCard, newCard, fromType, zonePosition);
            } else {
                // stash this info
                unorderedEntities.add(new UnorderedEntities(toPlayer, fromCard, newCard, fromType, zonePosition));
            }
        }

        Collections.sort(unorderedEntities);
        for(UnorderedEntities ue : unorderedEntities) {
            setCardInCopiedGame(toGame, ue.toPlayer, ue.fromCard, ue.newCard, ue.fromType, ue.zonePosition);
        }

        // Reorder ordered zones to match snapshot order. We use setCardOrder which
        // atomically replaces the card list, avoiding a window where the zone is empty
        // (which would cause race conditions with the UI thread).
        if (restore) {
            for (Player fromPlayer : fromGame.getPlayers()) {
                Player toPlayer = findBy(toGame, fromPlayer);
                for (ZoneType zt : ZoneType.ORDERED_ZONES) {
                    PlayerZone fromZone = fromPlayer.getZone(zt);
                    PlayerZone toZone = toPlayer == null ? null : toPlayer.getZone(zt);
                    if (fromZone == null || toZone == null) continue;
                    List<Card> correctOrder = Lists.newArrayList();
                    for (Card fromCard : fromZone.getCards()) {
                        Card toCard = toGame.findById(fromCard.getId());
                        if (toCard != null) {
                            correctOrder.add(toCard);
                        }
                    }
                    toZone.setCardOrder(correctOrder);
                }
            }
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
            if (fromCard.getHaunting() != null) {
                newCard.setHaunting(toGame.findById(fromCard.getHaunting().getId()));
            }
            if (fromCard.getEffectSource() != null) {
                newCard.setEffectSource(toGame.findById(fromCard.getEffectSource().getId()));
            }
            if (fromCard.isPaired()) {
                newCard.setPairedWith(toGame.findById(fromCard.getPairedWith().getId()));
            }
            if (fromCard.getCopiedPermanent() != null) {
                newCard.setCopiedPermanent(toGame.findById(fromCard.getCopiedPermanent().getId()));
            }
            if (fromCard.getCrewedByThisTurn() != null) {
                newCard.setCrewedByThisTurn(fromCard.getCrewedByThisTurn());
            }
            if (fromCard.getSaddledByThisTurn() != null) {
                newCard.setSaddledByThisTurn(fromCard.getSaddledByThisTurn());
            }
        }
    }

    private Card createCardCopy(Game newGame, Player newOwner, Card c) {
        Card newCard = new CardCopyService(c, newGame).copyCard(false, newOwner);
        newCard.dangerouslySetGame(newGame);
        return newCard;
    }

    private void setCardInCopiedGame(Game toGame, Player toPlayer, Card fromCard, Card newCard, ZoneType fromType, int zonePosition) {
        // On restore, skip zone add if card is already in the correct zone (avoids duplicates).
        // On store, always add — the card was just created and must be placed in the snapshot's zone.
        boolean alreadyInZone = restore && newCard.getZone() != null && newCard.getZone().getZoneType() == fromType;
        if (!alreadyInZone) {
            if (fromType.equals(ZoneType.Stack)) {
                toGame.getStackZone().add(newCard);
                newCard.setZone(toGame.getStackZone());
            } else {
                toPlayer.getZone(fromType).add(newCard);
                newCard.setZone(toPlayer.getZone(fromType));
            }
        }

        newCard.setGameTimestamp(fromCard.getGameTimestamp());
        newCard.setLayerTimestamp(fromCard.getLayerTimestamp());
        newCard.setSickness(fromCard.hasSickness());
        newCard.setState(fromCard.getCurrentStateName(), false);
        newCard.setForetold(fromCard.isForetold());
        newCard.setForetoldCostByEffect(fromCard.isForetoldCostByEffect());

        if (fromType == ZoneType.Battlefield) {
            // Controller
            Player controller = findBy(toGame, fromCard.getController());
            newCard.setController(controller, 0);

            if (fromCard.isBattle()) {
                newCard.setProtectingPlayer(findBy(toGame, fromCard.getProtectingPlayer()));
            }

            newCard.setCameUnderControlSinceLastUpkeep(fromCard.cameUnderControlSinceLastUpkeep());

            // P/T tables
            newCard.setPTTable(fromCard.getSetPTTable());
            newCard.setPTCharacterDefiningTable(fromCard.getSetPTCharacterDefiningTable());
            newCard.setPTBoost(fromCard.getPTBoostTable());

            // Damage
            newCard.setDamage(fromCard.getDamage());
            newCard.setDamageReceivedThisTurn(fromCard.getDamageReceivedThisTurn());

            // Copy copiable characteristics
            newCard.copyFrom(fromCard);

            // Hidden extrinsic keywords
            for (Table.Cell<Long, Long, List<String>> kw : fromCard.getHiddenExtrinsicKeywordsTable().cellSet()) {
                newCard.addHiddenExtrinsicKeywords(kw.getRowKey(), kw.getColumnKey(), kw.getValue());
            }
            newCard.updateKeywordsCache();

            // Tapped state
            newCard.setTapped(fromCard.isTapped());

            // Face-down handling
            if (fromCard.isFaceDown()) {
                newCard.turnFaceDown(true);
                if (fromCard.isManifested()) {
                    newCard.setManifested(fromCard.getManifestedSA());
                }
                if (fromCard.isCloaked()) {
                    newCard.setCloaked(fromCard.getCloakedSA());
                }
            }

            // Status flags
            newCard.setMonstrous(fromCard.isMonstrous());
            newCard.setRenowned(fromCard.isRenowned());
            newCard.setSolved(fromCard.isSolved());
            newCard.setSaddled(fromCard.isSaddled());
            newCard.setSuspected(fromCard.isSuspected());

            // Ability activation tracking — reset first, then copy from snapshot
            newCard.resetActivationsPerTurn();
            for (SpellAbility sa : fromCard.getAllSpellAbilities()) {
                int active = sa.getActivationsThisTurn();
                if (active > 0) {
                    SpellAbility newSa = findSAInCard(sa, newCard);
                    if (newSa != null) {
                        for (int i = 0; i < active; i++) {
                            newCard.addAbilityActivated(newSa);
                        }
                    }
                }
            }

            // Flipped and clone states — clear before copying
            newCard.setFlipped(fromCard.isFlipped());
            newCard.removeCloneStates();
            for (Map.Entry<Long, CardCloneStates> e : fromCard.getCloneStates().entrySet()) {
                newCard.addCloneState(e.getValue().copy(newCard, true), e.getKey());
            }

            // Counters — always set (empty map clears counters added after snapshot)
            newCard.setCounters(Maps.newHashMap(fromCard.getCounters()));

            // Chosen attributes
            if (fromCard.hasChosenPlayer()) newCard.setChosenPlayer(findBy(toGame, fromCard.getChosenPlayer()));
            if (fromCard.hasChosenType()) newCard.setChosenType(fromCard.getChosenType());
            if (fromCard.hasChosenType2()) newCard.setChosenType2(fromCard.getChosenType2());
            if (fromCard.hasChosenColor()) newCard.setChosenColors(Lists.newArrayList(fromCard.getChosenColors()));
            if (fromCard.hasNamedCard()) newCard.setNamedCards(Lists.newArrayList(fromCard.getNamedCards()));

            // Misc
            newCard.setSprocket(fromCard.getSprocket());
            newCard.setSVars(fromCard.getSVars());
            newCard.copyChangedSVarsFrom(fromCard);
        } else {
            // Non-battlefield zones: simple face-down/manifest handling
            newCard.setTapped(fromCard.isTapped());
            newCard.setFaceDown(fromCard.isFaceDown());
            newCard.setManifested(fromCard.getManifestedSA());
        }
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

    private record UnorderedEntities(
        Player toPlayer, Card fromCard, Card newCard, ZoneType fromType, int zonePosition
    ) implements Comparable<UnorderedEntities> {
        @Override
        public int compareTo(UnorderedEntities o) {
            return Integer.compare(this.zonePosition, o.zonePosition);
        }
    }

    public class SnapshotEntityMap implements IEntityMap {
        @Override
        public Game getGame() {
            if (restore) {
                return origGame;
            }
            return newGame;
        }

        @Override
        public GameObject map(GameObject o) {
            if (o instanceof Player) {
                return findBy(getGame(), (Player) o);
            } else if (o instanceof Card) {
                return findBy(getGame(), (Card) o);
            }
            return null;
        }

        @Override
        public Card map(final Card c) {
            return findBy(getGame(), c);
        }

        @Override
        public Player map(final Player p) {
            return findBy(getGame(), p);
        }
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

