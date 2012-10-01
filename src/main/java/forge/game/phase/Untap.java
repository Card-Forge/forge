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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.CardPredicates.Presets;
import forge.Counters;
import forge.GameActionUtil;
import forge.GameEntity;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.control.input.Input;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;

import forge.view.ButtonUtil;

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
public class Untap extends Phase implements java.io.Serializable {

    private static final long serialVersionUID = 4515266331266259123L;

    /**
     * <p>
     * Executes any hardcoded triggers that happen "at end of combat".
     * </p>
     */
    @Override
    public void executeAt() {
        this.execute(this.getAt());

        final Player turn = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        Untap.doPhasing(turn);

        Untap.doUntap();
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

        final CardList allp = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
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
     * doUntap.
     * </p>
     */
    private static void doUntap() {
        final Player player = Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn();
        final Predicate<Card> tappedCanUntap = Predicates.and(Presets.TAPPED, Presets.CANUNTAP);
        
        CardList list = player.getCardsIn(ZoneType.Battlefield);

        for (final Card c : list) {
            if (c.getBounceAtUntap() && c.getName().contains("Undiscovered Paradise")) {
                Singletons.getModel().getGameAction().moveToHand(c);
            }
        }

        list = CardListUtil.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (!Untap.canUntap(c)) {
                    return false;
                }
                if (Untap.canOnlyUntapOneLand() && c.isLand()) {
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
                        boolean defaultChoice = true;
                        if (c.getGainControlTargets().size() > 0) {
                            final ArrayList<Card> targets = c.getGainControlTargets();
                            prompt += "\r\n" + c + " is controlling: ";
                            for (final Card target : targets) {
                                prompt += target;
                                if (AllZoneUtil.isCardInPlay(target)) {
                                    defaultChoice = false;
                                }
                            }
                        }
                        if (GameActionUtil.showYesNoDialog(c, prompt, defaultChoice)) {
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
        final CardList opp = player.getOpponent().getCardsIn(ZoneType.Battlefield);
        for (final Card oppCard : opp) {
            if (oppCard.hasKeyword("CARDNAME untaps during each other player's untap step.")) {
                oppCard.untap();
                // end opponent untapping during your untap phase
            }
        }
        
        if (Untap.canOnlyUntapOneLand()) {

            if (Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().isComputer()) {
                // search for lands the computer has and only untap 1
                CardList landList = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer());
 
                landList = CardListUtil.filter(landList, tappedCanUntap);
                if (landList.size() > 0) {
                    landList.get(0).untap();
                }
            } else {
                final Input target = new Input() {
                    private static final long serialVersionUID = 6653677835629939465L;

                    @Override
                    public void showMessage() {
                        CMatchUI.SINGLETON_INSTANCE.showMessage("Select one tapped land to untap");
                        ButtonUtil.enableOnlyCancel();
                    }

                    @Override
                    public void selectButtonCancel() {
                        this.stop();
                    }

                    @Override
                    public void selectCard(final Card c, final PlayerZone zone) {
                        if (c.isLand() && zone.is(ZoneType.Battlefield) && c.isTapped() && Untap.canUntap(c)) {
                            c.untap();
                            this.stop();
                        }
                    } // selectCard()
                }; // Input
                CardList landList = AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer());
                landList = CardListUtil.filter(landList, tappedCanUntap);
                if (landList.size() > 0) {
                    AllZone.getInputControl().setInput(target);
                }
            }
        }
        if (AllZoneUtil.isCardInPlay("Damping Field") || AllZoneUtil.isCardInPlay("Imi Statue")) {
            if (Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().isComputer()) {
                CardList artList = AllZone.getComputerPlayer().getCardsIn(ZoneType.Battlefield);
                artList = CardListUtil.filter(artList, Presets.ARTIFACTS);
                artList = CardListUtil.filter(artList, tappedCanUntap);
                if (artList.size() > 0) {
                    CardFactoryUtil.getBestArtifactAI(artList).untap();
                }
            } else {
                final Input target = new Input() {
                    private static final long serialVersionUID = 5555427219659889707L;

                    @Override
                    public void showMessage() {
                        CMatchUI.SINGLETON_INSTANCE.showMessage("Select one tapped artifact to untap");
                        ButtonUtil.enableOnlyCancel();
                    }

                    @Override
                    public void selectButtonCancel() {
                        this.stop();
                    }

                    @Override
                    public void selectCard(final Card c, final PlayerZone zone) {
                        if (c.isArtifact() && zone.is(ZoneType.Battlefield) && c.getController().isHuman()
                                && Untap.canUntap(c)) {
                            c.untap();
                            this.stop();
                        }
                    } // selectCard()
                }; // Input
                CardList artList = AllZone.getHumanPlayer().getCardsIn(ZoneType.Battlefield);
                artList = CardListUtil.filter(artList, Presets.ARTIFACTS);
                artList = CardListUtil.filter(artList, tappedCanUntap);
                if (artList.size() > 0) {
                    AllZone.getInputControl().setInput(target);
                }
            }
        }
        if ((AllZoneUtil.isCardInPlay("Smoke") || AllZoneUtil.isCardInPlay("Stoic Angel"))) {
            if (Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().isComputer()) {
                CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                creatures = CardListUtil.filter(creatures, tappedCanUntap);
                if (creatures.size() > 0) {
                    creatures.get(0).untap();
                }
            } else {
                final Input target = new Input() {
                    private static final long serialVersionUID = 5555427219659889707L;

                    @Override
                    public void showMessage() {
                        CMatchUI.SINGLETON_INSTANCE.showMessage("Select one creature to untap");
                        ButtonUtil.enableOnlyCancel();
                    }

                    @Override
                    public void selectButtonCancel() {
                        this.stop();
                    }

                    @Override
                    public void selectCard(final Card c, final PlayerZone zone) {
                        if (c.isCreature() && zone.is(ZoneType.Battlefield) && c.getController().isHuman()
                                && Untap.canUntap(c)) {
                            c.untap();
                            this.stop();
                        }
                    } // selectCard()
                }; // Input
                CardList creatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                creatures = CardListUtil.filter(creatures, tappedCanUntap);
                if (creatures.size() > 0) {
                    AllZone.getInputControl().setInput(target);
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
                c.addHiddenExtrinsicKeyword("This card doesn't untap during your next untap step.");
            }
        }
    } // end doUntap

    private static boolean canOnlyUntapOneLand() {
        // Winter Orb was given errata so it no longer matters if it's tapped or
        // not
        if (AllZoneUtil.getCardsIn(ZoneType.Battlefield, "Winter Orb").size() > 0) {
            return true;
        }

        if (Singletons.getModel().getGameState().getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Battlefield, "Mungha Wurm").size() > 0) {
            return true;
        }

        return false;
    }

    private static void doPhasing(final Player turn) {
        // Needs to include phased out cards
        final CardList list = CardListUtil.filter(turn.getCardsIncludePhasingIn(ZoneType.Battlefield), new Predicate<Card>() {

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

} //end class Untap
