/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
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
package forge;

import java.util.ArrayList;
import java.util.HashMap;

import forge.Constant.Zone;
import forge.card.cardfactory.CardFactoryUtil;
import forge.gui.input.Input;
import forge.view.match.ViewField.PhaseLabel;
import forge.view.match.ViewTopLevel;

/**
 * <p>
 * PhaseUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class PhaseUtil {
    // ******* UNTAP PHASE *****
    /**
     * <p>
     * skipUntap.
     * </p>
     * 
     * @param p
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    private static boolean skipUntap(final Player p) {
        if (AllZoneUtil.isCardInPlay("Sands of Time") || AllZoneUtil.isCardInPlay("Stasis")) {
            return true;
        }

        if (p.skipNextUntap()) {
            p.setSkipNextUntap(false);
            return true;
        }

        return false;
    }

    /**
     * <p>
     * handleUntap.
     * </p>
     */
    public static void handleUntap() {
        final Player turn = AllZone.getPhase().getPlayerTurn();

        AllZone.getPhase().turnReset();
        AllZone.getGameInfo().notifyNextTurn();

        AllZone.getCombat().reset();
        AllZone.getCombat().setAttackingPlayer(turn);
        AllZone.getCombat().setDefendingPlayer(turn.getOpponent());

        // For tokens a player starts the game with they don't recover from Sum.
        // Sickness on first turn
        if (turn.getTurn() > 0) {
            final CardList list = turn.getCardsIncludePhasingIn(Zone.Battlefield);
            for (final Card c : list) {
                c.setSickness(false);
            }
        }
        turn.incrementTurn();

        AllZone.getGameAction().resetActivationsPerTurn();

        final CardList lands = AllZoneUtil.getPlayerLandsInPlay(turn).filter(CardListFilter.UNTAPPED);
        turn.setNumPowerSurgeLands(lands.size());

        // anything before this point happens regardless of whether the Untap
        // phase is skipped

        if (PhaseUtil.skipUntap(turn)) {
            AllZone.getPhase().setNeedToNextPhase(true);
            return;
        }

        // Phasing would happen here
        PhaseUtil.doPhasing(turn);

        PhaseUtil.doUntap();

        // otherwise land seems to stay tapped when it is really untapped
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();

        AllZone.getPhase().setNeedToNextPhase(true);
    }

    private static void doPhasing(final Player turn) {
        // Needs to include phased out cards
        final CardList list = turn.getCardsIncludePhasingIn(Constant.Zone.Battlefield).filter(new CardListFilter() {

            @Override
            public boolean addCard(final Card c) {
                return ((c.isPhasedOut() && c.isDirectlyPhasedOut()) || c.hasKeyword("Phasing"));
            }
        });

        // If c has things attached to it, they phase out simultaneously, and
        // will phase back in with it
        // If c is attached to something, it will phase out on its own, and try
        // to attach back to that thing when it comes back
        for (final Card c : list) {
            if (c.isPhasedOut()) {
                c.phase();
            } else if (c.hasKeyword("Phasing")) {
                // 702.23g If an object would simultaneously phase out directly
                // and indirectly, it just phases out indirectly.
                if (c.isAura()) {
                    final GameEntity ent = c.getEnchanting();

                    if ((ent instanceof Card) && list.contains((Card) ent)) {
                        continue;
                    }
                } else if (c.isEquipment() && c.isEquipping()) {
                    if (list.contains(c.getEquippingCard())) {
                        continue;
                    }
                }
                // TODO: Fortification
                c.phase();
            }
        }
    }

    /**
     * <p>
     * doUntap.
     * </p>
     */
    private static void doUntap() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList list = player.getCardsIn(Zone.Battlefield);

        for (final Card c : list) {
            if (c.getBounceAtUntap() && c.getName().contains("Undiscovered Paradise")) {
                AllZone.getGameAction().moveToHand(c);
            }
        }

        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                if (!PhaseUtil.canUntap(c)) {
                    return false;
                }
                if (PhaseUtil.canOnlyUntapOneLand() && c.isLand()) {
                    return false;
                }
                if ((AllZoneUtil.isCardInPlay("Damping Field") || AllZoneUtil.isCardInPlay("Imi Statue"))
                        && c.isArtifact()) {
                    return false;
                }
                if ((AllZoneUtil.isCardInPlay("Smoke") || AllZoneUtil.isCardInPlay("Stoic Angel") || AllZoneUtil
                        .isCardInPlay("Intruder Alarm")) && c.isCreature()) {
                    return false;
                }
                return true;
            }
        });

        for (final Card c : list) {
            if (c.hasKeyword("You may choose not to untap CARDNAME during your untap step.")) {
                if (c.isTapped()) {
                    if (c.getController().isHuman()) {
                        String prompt = "Untap " + c.getName() + "?";
                        boolean defaultNo = false;
                        if (c.getGainControlTargets().size() > 0) {
                            final ArrayList<Card> targets = c.getGainControlTargets();
                            prompt += "\r\n" + c + " is controlling: ";
                            for (final Card target : targets) {
                                prompt += target;
                                if (AllZoneUtil.isCardInPlay(target)) {
                                    defaultNo |= true;
                                }
                            }
                        }
                        if (GameActionUtil.showYesNoDialog(c, prompt, defaultNo)) {
                            c.untap();
                        }
                    } else { // computer
                        // if it is controlling something by staying tapped,
                        // leave it tapped
                        // if not, untap it
                        if (c.getGainControlTargets().size() > 0) {
                            final ArrayList<Card> targets = c.getGainControlTargets();
                            boolean untap = true;
                            for (final Card target : targets) {
                                if (AllZoneUtil.isCardInPlay(target)) {
                                    untap |= true;
                                }
                            }
                            if (untap) {
                                c.untap();
                            }
                        }
                    }
                }
            } else if ((c.getCounters(Counters.WIND) > 0) && AllZoneUtil.isCardInPlay("Freyalise's Winds")) {
                // remove a WIND counter instead of untapping
                c.subtractCounter(Counters.WIND, 1);
            } else {
                c.untap();
            }
        }

        // opponent untapping during your untap phase
        final CardList opp = player.getOpponent().getCardsIn(Zone.Battlefield);
        for (final Card oppCard : opp) {
            if (oppCard.hasKeyword("CARDNAME untaps during each other player's untap step.")) {
                oppCard.untap();
                // end opponent untapping during your untap phase
            }
        }

        if (PhaseUtil.canOnlyUntapOneLand()) {
            if (AllZone.getPhase().getPlayerTurn().isComputer()) {
                // search for lands the computer has and only untap 1
                CardList landList = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer());
                landList = landList.filter(CardListFilter.TAPPED).filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return PhaseUtil.canUntap(c);
                    }
                });
                if (landList.size() > 0) {
                    landList.get(0).untap();
                }
            } else {
                final Input target = new Input() {
                    private static final long serialVersionUID = 6653677835629939465L;

                    @Override
                    public void showMessage() {
                        AllZone.getDisplay().showMessage("Select one tapped land to untap");
                        ButtonUtil.enableOnlyCancel();
                    }

                    @Override
                    public void selectButtonCancel() {
                        this.stop();
                    }

                    @Override
                    public void selectCard(final Card c, final PlayerZone zone) {
                        if (c.isLand() && zone.is(Constant.Zone.Battlefield) && c.isTapped() && PhaseUtil.canUntap(c)) {
                            c.untap();
                            this.stop();
                        }
                    } // selectCard()
                }; // Input
                CardList landList = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                landList = landList.filter(CardListFilter.TAPPED).filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return PhaseUtil.canUntap(c);
                    }
                });
                if (landList.size() > 0) {
                    AllZone.getInputControl().setInput(target);
                }
            }
        }
        if (AllZoneUtil.isCardInPlay("Damping Field") || AllZoneUtil.isCardInPlay("Imi Statue")) {
            if (AllZone.getPhase().getPlayerTurn().isComputer()) {
                CardList artList = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                artList = artList.filter(CardListFilter.ARTIFACTS);
                artList = artList.filter(CardListFilter.TAPPED).filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return PhaseUtil.canUntap(c);
                    }
                });
                if (artList.size() > 0) {
                    CardFactoryUtil.getBestArtifactAI(artList).untap();
                }
            } else {
                final Input target = new Input() {
                    private static final long serialVersionUID = 5555427219659889707L;

                    @Override
                    public void showMessage() {
                        AllZone.getDisplay().showMessage("Select one tapped artifact to untap");
                        ButtonUtil.enableOnlyCancel();
                    }

                    @Override
                    public void selectButtonCancel() {
                        this.stop();
                    }

                    @Override
                    public void selectCard(final Card c, final PlayerZone zone) {
                        if (c.isArtifact() && zone.is(Constant.Zone.Battlefield) && c.getController().isHuman()
                                && PhaseUtil.canUntap(c)) {
                            c.untap();
                            this.stop();
                        }
                    } // selectCard()
                }; // Input
                CardList artList = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                artList = artList.filter(CardListFilter.ARTIFACTS);
                artList = artList.filter(CardListFilter.TAPPED).filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return PhaseUtil.canUntap(c);
                    }
                });
                if (artList.size() > 0) {
                    AllZone.getInputControl().setInput(target);
                }
            }
        }
        if ((AllZoneUtil.isCardInPlay("Smoke") || AllZoneUtil.isCardInPlay("Stoic Angel"))) {
            if (AllZone.getPhase().getPlayerTurn().isComputer()) {
                CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                creatures = creatures.filter(CardListFilter.TAPPED).filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return PhaseUtil.canUntap(c);
                    }
                });
                if (creatures.size() > 0) {
                    creatures.get(0).untap();
                }
            } else {
                final Input target = new Input() {
                    private static final long serialVersionUID = 5555427219659889707L;

                    @Override
                    public void showMessage() {
                        AllZone.getDisplay().showMessage("Select one creature to untap");
                        ButtonUtil.enableOnlyCancel();
                    }

                    @Override
                    public void selectButtonCancel() {
                        this.stop();
                    }

                    @Override
                    public void selectCard(final Card c, final PlayerZone zone) {
                        if (c.isCreature() && zone.is(Constant.Zone.Battlefield) && c.getController().isHuman()
                                && PhaseUtil.canUntap(c)) {
                            c.untap();
                            this.stop();
                        }
                    } // selectCard()
                }; // Input
                CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                creatures = creatures.filter(CardListFilter.TAPPED).filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return PhaseUtil.canUntap(c);
                    }
                });
                if (creatures.size() > 0) {
                    AllZone.getInputControl().setInput(target);
                }
            }
        }

        // Remove temporary keywords
        list = player.getCardsIn(Zone.Battlefield);
        for (final Card c : list) {
            c.removeAllExtrinsicKeyword("This card doesn't untap during your next untap step.");
            c.removeAllExtrinsicKeyword("HIDDEN This card doesn't untap during your next untap step.");
        }
    } // end doUntap

    /**
     * <p>
     * canUntap.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canUntap(final Card c) {

        if (c.hasKeyword("CARDNAME doesn't untap during your untap step.")
                || c.hasKeyword("This card doesn't untap during your next untap step.")) {
            return false;
        }

        final CardList allp = AllZoneUtil.getCardsIn(Zone.Battlefield);
        for (final Card ca : allp) {
            if (ca.hasStartOfKeyword("Permanents don't untap during their controllers' untap steps")) {
                final int keywordPosition = ca
                        .getKeywordPosition("Permanents don't untap during their controllers' untap steps");
                final String parse = ca.getKeyword().get(keywordPosition).toString();
                final String[] k = parse.split(":");
                final String[] restrictions = k[1].split(",");
                final Card card = ca;
                if (c.isValid(restrictions, card.getController(), card)) {
                    return false;
                }
            }
        } // end of Permanents don't untap during their controllers' untap steps

        return true;
    }

    /**
     * <p>
     * canOnlyUntapOneLand.
     * </p>
     * 
     * @return a boolean.
     */
    private static boolean canOnlyUntapOneLand() {
        // Winter Orb was given errata so it no longer matters if it's tapped or
        // not
        if (AllZoneUtil.getCardsIn(Zone.Battlefield, "Winter Orb").size() > 0) {
            return true;
        }

        if (AllZone.getPhase().getPlayerTurn().getCardsIn(Zone.Battlefield, "Mungha Wurm").size() > 0) {
            return true;
        }

        return false;
    }

    // ******* UPKEEP PHASE *****
    /**
     * <p>
     * handleUpkeep.
     * </p>
     */
    public static void handleUpkeep() {
        final Player turn = AllZone.getPhase().getPlayerTurn();

        if (PhaseUtil.skipUpkeep()) {
            // Slowtrips all say "on the next turn's upkeep" if there is no
            // upkeep next turn, the trigger will never occur.
            turn.clearSlowtripList();
            turn.getOpponent().clearSlowtripList();
            AllZone.getPhase().setNeedToNextPhase(true);
            return;
        }

        AllZone.getUpkeep().executeUntil(turn);
        AllZone.getUpkeep().executeAt();
    }

    /**
     * <p>
     * skipUpkeep.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean skipUpkeep() {
        if (AllZoneUtil.isCardInPlay("Eon Hub")) {
            return true;
        }

        final Player turn = AllZone.getPhase().getPlayerTurn();

        if ((turn.getCardsIn(Zone.Hand).size() == 0) && AllZoneUtil.isCardInPlay("Gibbering Descent", turn)) {
            return true;
        }

        return false;
    }

    // ******* DRAW PHASE *****
    /**
     * <p>
     * handleDraw.
     * </p>
     */
    public static void handleDraw() {
        final Player playerTurn = AllZone.getPhase().getPlayerTurn();

        if (PhaseUtil.skipDraw(playerTurn)) {
            AllZone.getPhase().setNeedToNextPhase(true);
            return;
        }

        playerTurn.drawCards(1, true);
    }

    /**
     * <p>
     * skipDraw.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    private static boolean skipDraw(final Player player) {
        // starting player skips his draw
        if (AllZone.getPhase().getTurn() == 1) {
            return true;
        }

        final CardList list = player.getCardsIn(Zone.Battlefield);

        if (list.containsName("Necropotence") || list.containsName("Yawgmoth's Bargain")
                || list.containsName("Recycle") || list.containsName("Dragon Appeasement")
                || list.containsName("Null Profusion") || list.containsName("Colfenor's Plans")
                || list.containsName("Psychic Possession") || list.containsName("Solitary Confinement")
                || list.containsName("Symbiotic Deployment")) {
            return true;
        }

        return false;
    }

    // ********* Declare Attackers ***********

    /**
     * <p>
     * verifyCombat.
     * </p>
     */
    public static void verifyCombat() {
        AllZone.getCombat().verifyCreaturesInPlay();
        CombatUtil.showCombat();
    }

    /**
     * <p>
     * handleDeclareAttackers.
     * </p>
     */
    public static void handleDeclareAttackers() {
        PhaseUtil.verifyCombat();
        final CardList list = new CardList();
        list.addAll(AllZone.getCombat().getAttackers());

        // TODO move propaganda to happen as the Attacker is Declared
        // Remove illegal Propaganda attacks first only for attacking the Player

        final int size = list.size();
        for (int i = 0; i < size; i++) {
            final Card c = list.get(i);
            final boolean last = (i == (size - 1));
            CombatUtil.checkPropagandaEffects(c, last);
        }
    }

    /**
     * <p>
     * handleAttackingTriggers.
     * </p>
     */
    public static void handleAttackingTriggers() {
        final CardList list = new CardList();
        list.addAll(AllZone.getCombat().getAttackers());
        AllZone.getStack().freezeStack();
        // Then run other Attacker bonuses
        // check for exalted:
        if (list.size() == 1) {
            final Player attackingPlayer = AllZone.getCombat().getAttackingPlayer();

            CardList exalted = attackingPlayer.getCardsIn(Zone.Battlefield);
            exalted = exalted.getKeyword("Exalted");

            if (exalted.size() > 0) {
                CombatUtil.executeExaltedAbility(list.get(0), exalted.size());
                // Make sure exalted effects get applied only once per combat
            }

        }
        
        AllZone.getGameLog().add("Combat", CombatUtil.getCombatAttackForLog(), 1);

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Attackers", list);
        runParams.put("AttackingPlayer", AllZone.getCombat().getAttackingPlayer());
        AllZone.getTriggerHandler().runTrigger("AttackersDeclared", runParams);

        for (final Card c : list) {
            CombatUtil.checkDeclareAttackers(c);
        }
        AllZone.getStack().unfreezeStack();
    }

    /**
     * <p>
     * handleDeclareBlockers.
     * </p>
     */
    public static void handleDeclareBlockers() {
        PhaseUtil.verifyCombat();

        AllZone.getStack().freezeStack();

        AllZone.getCombat().setUnblocked();

        CardList list = new CardList();
        list.addAll(AllZone.getCombat().getAllBlockers());

        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return !c.getCreatureBlockedThisCombat();
            }
        });

        final CardList attList = new CardList();
        attList.addAll(AllZone.getCombat().getAttackers());

        CombatUtil.checkDeclareBlockers(list);

        for (final Card a : attList) {
            final CardList blockList = AllZone.getCombat().getBlockers(a);
            for (final Card b : blockList) {
                CombatUtil.checkBlockedAttackers(a, b);
            }
        }

        AllZone.getStack().unfreezeStack();

        AllZone.getGameLog().add("Combat", CombatUtil.getCombatBlockForLog(), 1);
        CombatUtil.showCombat();
    }

    // ***** Combat Utility **********
    // TODO: the below functions should be removed and the code blocks that use
    // them should instead use SA_Restriction
    /**
     * <p>
     * isBeforeAttackersAreDeclared.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean isBeforeAttackersAreDeclared() {
        final String phase = AllZone.getPhase().getPhase();
        return phase.equals(Constant.Phase.UNTAP) || phase.equals(Constant.Phase.UPKEEP)
                || phase.equals(Constant.Phase.DRAW) || phase.equals(Constant.Phase.MAIN1)
                || phase.equals(Constant.Phase.COMBAT_BEGIN);
    }

    /**
     * Retrieves and visually activates phase label for appropriate phase and
     * player.
     * 
     * @param s
     *            &emsp; Phase state
     */
    public static void visuallyActivatePhase(final String s) {
        PhaseLabel lbl = null;
        final Player p = AllZone.getPhase().getPlayerTurn();
        final ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();

        int i; // Index of field; computer is 0, human is 1
        if (p.isComputer()) {
            i = 0;
        } else {
            i = 1;
        }

        if (s.equals(Constant.Phase.UPKEEP)) {
            lbl = t.getFieldControllers().get(i).getView().getLblUpkeep();
        } else if (s.equals(Constant.Phase.DRAW)) {
            lbl = t.getFieldControllers().get(i).getView().getLblDraw();
        } else if (s.equals(Constant.Phase.MAIN1)) {
            lbl = t.getFieldControllers().get(i).getView().getLblMain1();
        } else if (s.equals(Constant.Phase.COMBAT_BEGIN)) {
            lbl = t.getFieldControllers().get(i).getView().getLblBeginCombat();
        } else if (s.equals(Constant.Phase.COMBAT_DECLARE_ATTACKERS)) {
            lbl = t.getFieldControllers().get(i).getView().getLblDeclareAttackers();
        } else if (s.equals(Constant.Phase.COMBAT_DECLARE_BLOCKERS)) {
            lbl = t.getFieldControllers().get(i).getView().getLblDeclareBlockers();
        } else if (s.equals(Constant.Phase.COMBAT_DAMAGE)) {
            lbl = t.getFieldControllers().get(i).getView().getLblCombatDamage();
        } else if (s.equals(Constant.Phase.COMBAT_FIRST_STRIKE_DAMAGE)) {
            lbl = t.getFieldControllers().get(i).getView().getLblFirstStrike();
        } else if (s.equals(Constant.Phase.COMBAT_END)) {
            lbl = t.getFieldControllers().get(i).getView().getLblEndCombat();
        } else if (s.equals(Constant.Phase.MAIN2)) {
            lbl = t.getFieldControllers().get(i).getView().getLblMain2();
        } else if (s.equals(Constant.Phase.END_OF_TURN)) {
            lbl = t.getFieldControllers().get(i).getView().getLblEndTurn();
        } else if (s.equals(Constant.Phase.CLEANUP)) {
            lbl = t.getFieldControllers().get(i).getView().getLblCleanup();
        } else {
            return;
        }

        t.getController().resetAllPhaseButtons();
        lbl.setActive(true);
    }
}
