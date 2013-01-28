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
import forge.CardPredicates.Presets;
import forge.CardPredicates;
import forge.CounterType;
import forge.GameEntity;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.control.input.Input;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;
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
        this.execute(this.getAt());

        final Player turn = game.getPhaseHandler().getPlayerTurn();
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

    /**
     * <p>
     * doUntap.
     * </p>
     */
    private static void doUntap() {
        final Player player = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
        final Predicate<Card> tappedCanUntap = Predicates.and(Presets.TAPPED, Presets.CANUNTAP);

        List<Card> list = new ArrayList<Card>(player.getCardsIn(ZoneType.Battlefield));

        List<Card> bounceList = CardLists.getKeyword(list, "During your next untap step, as you untap your permanents, return CARDNAME to its owner's hand.");
        for (final Card c : bounceList) {
            Singletons.getModel().getGame().getAction().moveToHand(c);
        }

        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (!Untap.canUntap(c)) {
                    return false;
                }
                if (Untap.canOnlyUntapOneLand() && c.isLand()) {
                    return false;
                }
                if ((Singletons.getModel().getGame().isCardInPlay("Damping Field") || Singletons.getModel().getGame().isCardInPlay("Imi Statue"))
                        && c.isArtifact()) {
                    return false;
                }
                if ((Singletons.getModel().getGame().isCardInPlay("Smoke") || Singletons.getModel().getGame().isCardInPlay("Stoic Angel") || Singletons.getModel().getGame().isCardInPlay("Intruder Alarm")) && c.isCreature()) {
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
                            final ArrayList<Card> targets = c.getGainControlTargets();
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
            } else if ((c.getCounters(CounterType.WIND) > 0) && Singletons.getModel().getGame().isCardInPlay("Freyalise's Winds")) {
                // remove a WIND counter instead of untapping
                c.subtractCounter(CounterType.WIND, 1);
            } else {
                c.untap();
            }
        }

        // other players untapping during your untap phase
        final List<Card> cardsWithKW = CardLists.getKeyword(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield),
                "CARDNAME untaps during each other player's untap step.");
        final List<Player> otherPlayers = player.getOpponents();
        otherPlayers.addAll(player.getAllies());
        CardLists.filter(cardsWithKW, CardPredicates.isControlledByAnyOf(otherPlayers));
        for (final Card cardWithKW : cardsWithKW) {
            cardWithKW.untap();
        }
        // end other players untapping during your untap phase

        if (Untap.canOnlyUntapOneLand()) {
            if (player.isComputer()) {
                // search for lands the computer has and only untap 1
                List<Card> landList = player.getLandsInPlay();

                landList = CardLists.filter(landList, tappedCanUntap);
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
                    public void selectCard(final Card c) {
                        Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                        if (c.isLand() && zone.is(ZoneType.Battlefield) && c.isTapped() && Untap.canUntap(c)) {
                            c.untap();
                            this.stop();
                        }
                    } // selectCard()
                }; // Input
                List<Card> landList = player.getLandsInPlay();
                landList = CardLists.filter(landList, tappedCanUntap);
                if (landList.size() > 0) {
                    Singletons.getModel().getMatch().getInput().setInput(target);
                }
            }
        }
        if (Singletons.getModel().getGame().isCardInPlay("Damping Field") || Singletons.getModel().getGame().isCardInPlay("Imi Statue")) {
            final Player turnOwner = Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn();
            if (turnOwner.isComputer()) {
                List<Card> artList = new ArrayList<Card>(turnOwner.getCardsIn(ZoneType.Battlefield));
                artList = CardLists.filter(artList, Presets.ARTIFACTS);
                artList = CardLists.filter(artList, tappedCanUntap);
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
                    public void selectCard(final Card c) {
                        Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                        if (c.isArtifact() && zone.is(ZoneType.Battlefield) && c.getController().isHuman()
                                && Untap.canUntap(c)) {
                            c.untap();
                            this.stop();
                        }
                    } // selectCard()
                }; // Input
                List<Card> artList = new ArrayList<Card>(turnOwner.getCardsIn(ZoneType.Battlefield));
                artList = CardLists.filter(artList, Presets.ARTIFACTS);
                artList = CardLists.filter(artList, tappedCanUntap);
                if (artList.size() > 0) {
                    Singletons.getModel().getMatch().getInput().setInput(target);
                }
            }
        }
        if ((Singletons.getModel().getGame().isCardInPlay("Smoke") || Singletons.getModel().getGame().isCardInPlay("Stoic Angel"))) {
            if (player.isComputer()) {
                List<Card> creatures = player.getCreaturesInPlay();
                creatures = CardLists.filter(creatures, tappedCanUntap);
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
                    public void selectCard(final Card c) {
                        Zone zone = Singletons.getModel().getGame().getZoneOf(c);
                        if (c.isCreature() && zone.is(ZoneType.Battlefield) && c.getController().isHuman()
                                && Untap.canUntap(c)) {
                            c.untap();
                            this.stop();
                        }
                    } // selectCard()
                }; // Input
                final List<Card> creatures = CardLists.filter(player.getCreaturesInPlay(), tappedCanUntap);
                if (creatures.size() > 0) {
                    Singletons.getModel().getMatch().getInput().setInput(target);
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
    } // end doUntap

    private static boolean canOnlyUntapOneLand() {
        // Winter Orb was given errata so it no longer matters if it's tapped or
        // not
        if (Singletons.getModel().getGame().isCardInPlay("Winter Orb")
                || Singletons.getModel().getGame().getPhaseHandler().getPlayerTurn().isCardInPlay("Mungha Wurm")) {
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
