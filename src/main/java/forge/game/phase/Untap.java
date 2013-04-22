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
package forge.game.phase;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CounterType;
import forge.FThreads;
import forge.GameEntity;
import forge.Singletons;
import forge.control.input.InputSelectCards;
import forge.control.input.InputSelectCardsFromList;
import forge.game.GameState;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;

/**
 * <p>
 * Untap class.
 * Handles "until next untap", "until your next untap" and "at beginning of untap"
 * commands from cards.
 * </p>
 * 
 * @author Forge
 * @version $Id: Untap 12482 2011-12-06 11:14:11Z Sloth $
 */
public class Untap extends Phase {
    private static final long serialVersionUID = 4515266331266259123L;

    public Untap(final GameState game0) {
        super(game0);
    }
    
    /**
     * <p>
     * Executes any hardcoded triggers that happen "at end of combat".
     * </p>
     */
    @Override
    public void executeAt() {
        this.execute(this.at);

        final Player turn = game.getPhaseHandler().getPlayerTurn();
        Untap.doPhasing(turn);

        doUntap();
    }

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
                || c.hasKeyword("This card doesn't untap during your next untap step.")
                || c.hasKeyword("This card doesn't untap during your next two untap steps.")) {
            return false;
        }

        for (final Card ca : Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield)) {
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


    public static final Predicate<Card> CANUNTAP = new Predicate<Card>() {
        @Override
        public boolean apply(Card c) {
            return Untap.canUntap(c);
        }
    };
    
    /**
     * <p>
     * doUntap.
     * </p>
     */
    private void doUntap() {
        final Player player = game.getPhaseHandler().getPlayerTurn();
        final Predicate<Card> tappedCanUntap = Predicates.and(Presets.TAPPED, CANUNTAP);

        List<Card> list = new ArrayList<Card>(player.getCardsIn(ZoneType.Battlefield));

        List<Card> bounceList = CardLists.getKeyword(list, "During your next untap step, as you untap your permanents, return CARDNAME to its owner's hand.");
        for (final Card c : bounceList) {
            game.getAction().moveToHand(c);
        }

        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (!Untap.canUntap(c)) {
                    return false;
                }
                if (canOnlyUntapOneLand() && c.isLand()) {
                    return false;
                }
                if (c.isArtifact() && (game.isCardInPlay("Damping Field") || game.isCardInPlay("Imi Statue"))) {
                    return false;
                }
                if (c.isCreature() && (game.isCardInPlay("Smoke") || game.isCardInPlay("Stoic Angel") || game.isCardInPlay("Intruder Alarm"))) {
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
                        boolean defaultChoice = true;
                        if (c.getGainControlTargets().size() > 0) {
                            final List<Card> targets = c.getGainControlTargets();
                            prompt += "\r\n" + c + " is controlling: ";
                            for (final Card target : targets) {
                                prompt += target;
                                if (target.isInPlay()) {
                                    defaultChoice = false;
                                }
                            }
                        }
                        if (GuiDialog.confirm(c, prompt, defaultChoice)) {
                            c.untap();
                        }
                    } else { // computer
                        // if it is controlling something by staying tapped,
                        // leave it tapped
                        // if not, untap it
                        if (c.getGainControlTargets().size() > 0) {
                            final List<Card> targets = c.getGainControlTargets();
                            boolean untap = true;
                            for (final Card target : targets) {
                                if (target.isInPlay()) {
                                    untap |= true;
                                }
                            }
                            if (untap) {
                                c.untap();
                            }
                        }
                    }
                }
            } else if ((c.getCounters(CounterType.WIND) > 0) && game.isCardInPlay("Freyalise's Winds")) {
                // remove a WIND counter instead of untapping
                c.subtractCounter(CounterType.WIND, 1);
            } else {
                c.untap();
            }
        }

        // other players untapping during your untap phase
        final List<Card> cardsWithKW = CardLists.getKeyword(game.getCardsIn(ZoneType.Battlefield),
                "CARDNAME untaps during each other player's untap step.");
        final List<Player> otherPlayers = player.getOpponents();
        otherPlayers.addAll(player.getAllies());
        CardLists.filter(cardsWithKW, CardPredicates.isControlledByAnyOf(otherPlayers));
        for (final Card cardWithKW : cardsWithKW) {
            cardWithKW.untap();
        }
        // end other players untapping during your untap phase

        if (canOnlyUntapOneLand()) {
            final List<Card> landList = CardLists.filter(player.getLandsInPlay(), tappedCanUntap);
            
            if (!landList.isEmpty()) {
                if (player.isComputer()) {
                    // search for lands the computer has and only untap 1
                    landList.get(0).untap();
                } else {
                    final InputSelectCards target = new InputSelectCardsFromList(1,1, landList);
                    target.setMessage("Select one tapped land to untap");
                    FThreads.setInputAndWait(target);
                    if( !target.hasCancelled() && !target.getSelected().isEmpty())
                        target.getSelected().get(0).untap();
                }
            }
        }
        if (game.isCardInPlay("Damping Field") || game.isCardInPlay("Imi Statue")) {
            final Player turnOwner = game.getPhaseHandler().getPlayerTurn();
            final List<Card> artList = CardLists.filter(turnOwner.getCardsIn(ZoneType.Battlefield), Presets.ARTIFACTS, tappedCanUntap);
            
            if (!artList.isEmpty()) {
                if (turnOwner.isComputer()) {
                    ComputerUtilCard.getBestArtifactAI(artList).untap();
                } else {
                    final InputSelectCards target = new InputSelectCardsFromList(1,1, artList);
                    target.setMessage("Select one tapped artifact to untap");
                    FThreads.setInputAndWait(target);
                    if( !target.hasCancelled() && !target.getSelected().isEmpty())
                        target.getSelected().get(0).untap();
                }
            }
        }
        if ((game.isCardInPlay("Smoke") || game.isCardInPlay("Stoic Angel"))) {
            final List<Card> creatures = CardLists.filter(player.getCreaturesInPlay(), tappedCanUntap);
            if (!creatures.isEmpty()) {
                if (player.isComputer()) {
                    ComputerUtilCard.getBestCreatureAI(creatures).untap();
                } else {
                    final InputSelectCards target = new InputSelectCardsFromList(1, 1, creatures);
                    target.setMessage("Select one creature to untap");
                    FThreads.setInputAndWait(target);
                    if( !target.hasCancelled() && !target.getSelected().isEmpty())
                        target.getSelected().get(0).untap();
                }
            }
        }

        // Remove temporary keywords
        list = player.getCardsIn(ZoneType.Battlefield);
        for (final Card c : list) {
            c.removeAllExtrinsicKeyword("This card doesn't untap during your next untap step.");
            c.removeAllExtrinsicKeyword("HIDDEN This card doesn't untap during your next untap step.");
            if (c.hasKeyword("This card doesn't untap during your next two untap steps.")) {
                c.removeAllExtrinsicKeyword("HIDDEN This card doesn't untap during your next two untap steps.");
                c.addHiddenExtrinsicKeyword("HIDDEN This card doesn't untap during your next untap step.");
            }
        }
        game.getStack().chooseOrderOfSimultaneousStackEntryAll();
    } // end doUntap

    private boolean canOnlyUntapOneLand() {
        // Winter Orb was given errata so it no longer matters if it's tapped or
        // not
        if (game.isCardInPlay("Winter Orb") || game.getPhaseHandler().getPlayerTurn().isCardInPlay("Mungha Wurm")) {
            return true;
        }

        return false;
    }

    private static void doPhasing(final Player turn) {
        // Needs to include phased out cards
        final List<Card> list = CardLists.filter(turn.getCardsIncludePhasingIn(ZoneType.Battlefield), new Predicate<Card>() {

            @Override
            public boolean apply(final Card c) {
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

                    if ((ent instanceof Card) && list.contains(ent)) {
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

} //end class Untap
