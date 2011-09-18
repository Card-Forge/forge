package forge;

import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.gui.input.Input;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>PhaseUtil class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class PhaseUtil {
    // ******* UNTAP PHASE *****
    /**
     * <p>skipUntap.</p>
     *
     * @param p a {@link forge.Player} object.
     * @return a boolean.
     */
    private static boolean skipUntap(Player p) {
        if (AllZoneUtil.isCardInPlay("Sands of Time") || AllZoneUtil.isCardInPlay("Stasis"))
            return true;

        if (p.skipNextUntap()) {
            p.setSkipNextUntap(false);
            return true;
        }

        return false;
    }

    /**
     * <p>handleUntap.</p>
     */
    public static void handleUntap() {
        Player turn = AllZone.getPhase().getPlayerTurn();

        AllZone.getPhase().turnReset();
        AllZone.getGameInfo().notifyNextTurn();

        AllZone.getCombat().reset();
        AllZone.getCombat().setAttackingPlayer(turn);
        AllZone.getCombat().setDefendingPlayer(turn.getOpponent());

        // For tokens a player starts the game with they don't recover from Sum. Sickness on first turn
        if (turn.getTurn() > 0) {
            CardList list = turn.getCardsIn(Zone.Battlefield);
            for (Card c : list)
                c.setSickness(false);
        }
        turn.incrementTurn();

        AllZone.getGameAction().resetActivationsPerTurn();

        CardList lands = AllZoneUtil.getPlayerLandsInPlay(turn);
        lands = lands.filter(AllZoneUtil.untapped);
        turn.setNumPowerSurgeLands(lands.size());

        // anything before this point happens regardless of whether the Untap phase is skipped

        if (skipUntap(turn)) {
            AllZone.getPhase().setNeedToNextPhase(true);
            return;
        }

        // Phasing would happen here

        doUntap();

        //otherwise land seems to stay tapped when it is really untapped
        AllZone.getHumanPlayer().getZone(Zone.Battlefield).updateObservers();

        AllZone.getPhase().setNeedToNextPhase(true);
    }

    /**
     * <p>doUntap.</p>
     */
    private static void doUntap() {
        Player player = AllZone.getPhase().getPlayerTurn();
        CardList list = player.getCardsIn(Zone.Battlefield);

        for (Card c : list) {
            if (c.getBounceAtUntap() && c.getName().contains("Undiscovered Paradise")) {
                AllZone.getGameAction().moveToHand(c);
            }
        }

        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                if (!canUntap(c)) return false;
                if (canOnlyUntapOneLand() && c.isLand()) return false;
                if ((AllZoneUtil.isCardInPlay("Damping Field") || AllZoneUtil.isCardInPlay("Imi Statue"))
                        && c.isArtifact()) return false;
                if ((AllZoneUtil.isCardInPlay("Smoke") || AllZoneUtil.isCardInPlay("Stoic Angel")
                        || AllZoneUtil.isCardInPlay("Intruder Alarm")) && c.isCreature()) return false;
                return true;
            }
        });

        for (Card c : list) {
            if (c.hasKeyword("You may choose not to untap CARDNAME during your untap step.")) {
                if (c.isTapped()) {
                    if (c.getController().isHuman()) {
                        String prompt = "Untap " + c.getName() + "?";
                        boolean defaultNo = false;
                        if (c.getGainControlTargets().size() > 0) {
                            ArrayList<Card> targets = c.getGainControlTargets();
                            prompt += "\r\n" + c + " is controlling: ";
                            for (Card target : targets) {
                                prompt += target;
                                if(AllZoneUtil.isCardInPlay(target)) defaultNo |= true;
                            }
                        }
                        if (GameActionUtil.showYesNoDialog(c, prompt, defaultNo)) {
                            c.untap();
                        }
                    } else {  //computer
                        //if it is controlling something by staying tapped, leave it tapped
                        //if not, untap it
                    	if (c.getGainControlTargets().size() > 0) {
                            ArrayList<Card> targets = c.getGainControlTargets();
                            boolean untap = true;
                            for (Card target : targets) {
                                if(AllZoneUtil.isCardInPlay(target)) untap |= true;
                            }
                            if(untap) c.untap();
                        }
                    }
                }
            } else if ((c.getCounters(Counters.WIND) > 0) && AllZoneUtil.isCardInPlay("Freyalise's Winds")) {
                //remove a WIND counter instead of untapping
                c.subtractCounter(Counters.WIND, 1);
            } else c.untap();
        }

        //Remove temporary keywords
        list = player.getCardsIn(Zone.Battlefield);
        for (Card c : list) {
            c.removeExtrinsicKeyword("This card doesn't untap during your next untap step.");
            c.removeExtrinsicKeyword("HIDDEN This card doesn't untap during your next untap step.");
        }

        //opponent untapping during your untap phase
        CardList opp = player.getOpponent().getCardsIn(Zone.Battlefield);
        for (Card oppCard : opp)
            if (oppCard.hasKeyword("CARDNAME untaps during each other player's untap step."))
                oppCard.untap();
        //end opponent untapping during your untap phase

        if (canOnlyUntapOneLand()) {
            if (AllZone.getPhase().getPlayerTurn().isComputer()) {
                //search for lands the computer has and only untap 1
                CardList landList = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer());
                landList = landList.filter(AllZoneUtil.tapped);
                if (landList.size() > 0) {
                    landList.get(0).untap();
                }
            } else {
                Input target = new Input() {
                    private static final long serialVersionUID = 6653677835629939465L;

                    public void showMessage() {
                        AllZone.getDisplay().showMessage("Select one tapped land to untap");
                        ButtonUtil.enableOnlyCancel();
                    }

                    public void selectButtonCancel() {
                        stop();
                    }

                    public void selectCard(Card c, PlayerZone zone) {
                        if (c.isLand() && zone.is(Constant.Zone.Battlefield) && c.isTapped()) {
                            c.untap();
                            stop();
                        }
                    }//selectCard()
                };//Input
                CardList landList = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                landList = landList.filter(AllZoneUtil.tapped);
                if (landList.size() > 0) {
                    AllZone.getInputControl().setInput(target);
                }
            }
        }
        if (AllZoneUtil.isCardInPlay("Damping Field") || AllZoneUtil.isCardInPlay("Imi Statue")) {
            if (AllZone.getPhase().getPlayerTurn().isComputer()) {
                CardList artList = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                artList = artList.filter(AllZoneUtil.artifacts);
                artList = artList.filter(AllZoneUtil.tapped);
                if (artList.size() > 0) {
                    CardFactoryUtil.AI_getBestArtifact(artList).untap();
                }
            } else {
                Input target = new Input() {
                    private static final long serialVersionUID = 5555427219659889707L;

                    public void showMessage() {
                        AllZone.getDisplay().showMessage("Select one tapped artifact to untap");
                        ButtonUtil.enableOnlyCancel();
                    }

                    public void selectButtonCancel() {
                        stop();
                    }

                    public void selectCard(Card c, PlayerZone zone) {
                        if (c.isArtifact() && zone.is(Constant.Zone.Battlefield)
                                && c.getController().isHuman()) {
                            c.untap();
                            stop();
                        }
                    }//selectCard()
                };//Input
                CardList artList = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield);
                artList = artList.filter(AllZoneUtil.artifacts);
                artList = artList.filter(AllZoneUtil.tapped);
                if (artList.size() > 0) {
                    AllZone.getInputControl().setInput(target);
                }
            }
        }
        if ((AllZoneUtil.isCardInPlay("Smoke") || AllZoneUtil.isCardInPlay("Stoic Angel"))) {
            if (AllZone.getPhase().getPlayerTurn().isComputer()) {
                CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                creatures = creatures.filter(AllZoneUtil.tapped);
                if (creatures.size() > 0) {
                    creatures.get(0).untap();
                }
            } else {
                Input target = new Input() {
                    private static final long serialVersionUID = 5555427219659889707L;

                    public void showMessage() {
                        AllZone.getDisplay().showMessage("Select one creature to untap");
                        ButtonUtil.enableOnlyCancel();
                    }

                    public void selectButtonCancel() {
                        stop();
                    }

                    public void selectCard(Card c, PlayerZone zone) {
                        if (c.isCreature() && zone.is(Constant.Zone.Battlefield)
                                && c.getController().isHuman()) {
                            c.untap();
                            stop();
                        }
                    }//selectCard()
                };//Input
                CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                creatures = creatures.filter(AllZoneUtil.tapped);
                if (creatures.size() > 0) {
                    AllZone.getInputControl().setInput(target);
                }
            }
        }
    }//end doUntap


    /**
     * <p>canUntap.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean canUntap(Card c) {

        if (c.hasKeyword("CARDNAME doesn't untap during your untap step.")
                || c.hasKeyword("This card doesn't untap during your next untap step.")) return false;

        CardList allp = AllZoneUtil.getCardsIn(Zone.Battlefield);
        for (Card ca : allp) {
            if (ca.hasStartOfKeyword("Permanents don't untap during their controllers' untap steps")) {
                int KeywordPosition = ca.getKeywordPosition("Permanents don't untap during their controllers' untap steps");
                String parse = ca.getKeyword().get(KeywordPosition).toString();
                String k[] = parse.split(":");
                final String restrictions[] = k[1].split(",");
                final Card card = ca;
                if (c.isValidCard(restrictions, card.getController(), card)) return false;
            }
        } // end of Permanents don't untap during their controllers' untap steps

        return true;
    }


    /**
     * <p>canOnlyUntapOneLand.</p>
     *
     * @return a boolean.
     */
    private static boolean canOnlyUntapOneLand() {
        //Winter Orb was given errata so it no longer matters if it's tapped or not
        if (AllZoneUtil.getCardsIn(Zone.Battlefield, "Winter Orb").size() > 0)
            return true;

        if (AllZone.getPhase().getPlayerTurn().getCardsIn(Zone.Battlefield, "Mungha Wurm").size() > 0)
            return true;

        return false;
    }

    // ******* UPKEEP PHASE *****
    /**
     * <p>handleUpkeep.</p>
     */
    public static void handleUpkeep() {
    	Player turn = AllZone.getPhase().getPlayerTurn();
        if (skipUpkeep()) {
            // Slowtrips all say "on the next turn's upkeep" if there is no upkeep next turn, the trigger will never occur.
            turn.clearSlowtripList();
            turn.getOpponent().clearSlowtripList();
            AllZone.getPhase().setNeedToNextPhase(true);
            return;
        }

        AllZone.getUpkeep().executeUntil(turn);
        AllZone.getUpkeep().executeAt();
    }

    /**
     * <p>skipUpkeep.</p>
     *
     * @return a boolean.
     */
    public static boolean skipUpkeep() {
        if (AllZoneUtil.isCardInPlay("Eon Hub"))
            return true;

        Player turn = AllZone.getPhase().getPlayerTurn();

        if (turn.getCardsIn(Zone.Hand).size() == 0 && AllZoneUtil.isCardInPlay("Gibbering Descent", turn))
            return true;

        return false;
    }

    // ******* DRAW PHASE *****
    /**
     * <p>handleDraw.</p>
     */
    public static void handleDraw() {
        Player playerTurn = AllZone.getPhase().getPlayerTurn();

        if (skipDraw(playerTurn)) {
            AllZone.getPhase().setNeedToNextPhase(true);
            return;
        }

        playerTurn.drawCards(1, true);
        GameActionUtil.executeDrawStepEffects();
    }

    /**
     * <p>skipDraw.</p>
     *
     * @param player a {@link forge.Player} object.
     * @return a boolean.
     */
    private static boolean skipDraw(Player player) {
        // starting player skips his draw
        if (AllZone.getPhase().getTurn() == 1) {
            return true;
        }

        CardList list = player.getCardsIn(Zone.Battlefield);

        if (list.containsName("Necropotence") || list.containsName("Yawgmoth's Bargain") || list.containsName("Recycle") ||
                list.containsName("Dragon Appeasement") || list.containsName("Null Profusion") || list.containsName("Colfenor's Plans") ||
                list.containsName("Psychic Possession") || list.containsName("Solitary Confinement") ||
                list.containsName("Symbiotic Deployment"))
            return true;

        return false;
    }

    // ********* Declare Attackers ***********

    /**
     * <p>verifyCombat.</p>
     */
    public static void verifyCombat() {
        AllZone.getCombat().verifyCreaturesInPlay();
        CombatUtil.showCombat();
    }

    /**
     * <p>handleDeclareAttackers.</p>
     */
    public static void handleDeclareAttackers() {
        verifyCombat();
        CardList list = new CardList();
        list.addAll(AllZone.getCombat().getAttackers());

        // TODO move propaganda to happen as the Attacker is Declared
        // Remove illegal Propaganda attacks first only for attacking the Player

        int size = list.size();
        for (int i = 0; i < size; i++) {
            Card c = list.get(i);
            boolean last = (i == size - 1);
            CombatUtil.checkPropagandaEffects(c, last);
        }
    }

    /**
     * <p>handleAttackingTriggers.</p>
     */
    public static void handleAttackingTriggers() {
        CardList list = new CardList();
        list.addAll(AllZone.getCombat().getAttackers());
        AllZone.getStack().freezeStack();
        // Then run other Attacker bonuses
        //check for exalted:
        if (list.size() == 1) {
            Player attackingPlayer = AllZone.getCombat().getAttackingPlayer();

            CardList exalted = attackingPlayer.getCardsIn(Zone.Battlefield);
            exalted = exalted.getKeyword("Exalted");

            if (exalted.size() > 0) CombatUtil.executeExaltedAbility(list.get(0), exalted.size());
            // Make sure exalted effects get applied only once per combat

        }
        
        HashMap<String,Object> runParams = new HashMap<String,Object>();
        runParams.put("Attackers", list);
        runParams.put("AttackingPlayer", AllZone.getCombat().getAttackingPlayer());
        AllZone.getTriggerHandler().runTrigger("AttackersDeclared", runParams);
        
        for (Card c : list)
            CombatUtil.checkDeclareAttackers(c);
        AllZone.getStack().unfreezeStack();
    }

    /**
     * <p>handleDeclareBlockers.</p>
     */
    public static void handleDeclareBlockers() {
        verifyCombat();

        AllZone.getStack().freezeStack();

        AllZone.getCombat().setUnblocked();

        CardList list = new CardList();
        list.addAll(AllZone.getCombat().getAllBlockers());

        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.getCreatureBlockedThisCombat();
            }
        });

        CardList attList = new CardList();
        attList.addAll(AllZone.getCombat().getAttackers());

        CombatUtil.checkDeclareBlockers(list);

        for (Card a : attList) {
            CardList blockList = AllZone.getCombat().getBlockers(a);
            for (Card b : blockList)
                CombatUtil.checkBlockedAttackers(a, b);
        }

        AllZone.getStack().unfreezeStack();
        CombatUtil.showCombat();
    }


    // ***** Combat Utility **********
    // TODO: the below functions should be removed and the code blocks that use them should instead use SA_Restriction
    /**
     * <p>isBeforeAttackersAreDeclared.</p>
     *
     * @return a boolean.
     */
    public static boolean isBeforeAttackersAreDeclared() {
        String phase = AllZone.getPhase().getPhase();
        return phase.equals(Constant.Phase.Untap) || phase.equals(Constant.Phase.Upkeep)
                || phase.equals(Constant.Phase.Draw) || phase.equals(Constant.Phase.Main1)
                || phase.equals(Constant.Phase.Combat_Begin);
    }
}
