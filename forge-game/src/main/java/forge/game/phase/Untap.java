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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

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
    protected final Game game;
    
    public Untap(final Game game0) {
        super(PhaseType.UNTAP);
        game = game0;
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
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean canUntap(final Card c) {

        if (c.hasKeyword("CARDNAME doesn't untap during your untap step.")
                || c.hasKeyword("This card doesn't untap during your next untap step.")
                || c.hasKeyword("This card doesn't untap during your next two untap steps.")) {
            return false;
        }
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

        CardCollection list = new CardCollection(player.getCardsIn(ZoneType.Battlefield));
        for (Card c : list) {
            c.setStartedTheTurnUntapped(c.isUntapped());
        }

        CardCollection bounceList = CardLists.getKeyword(list, "During your next untap step, as you untap your permanents, return CARDNAME to its owner's hand.");
        for (final Card c : bounceList) {
            game.getAction().moveToHand(c);
        }
        list.removeAll((Collection<?>)bounceList);

        final Map<String, Integer> restrictUntap = new HashMap<String, Integer>();
        boolean hasChosen = false;
        for (String kw : player.getKeywords()) {
            if (kw.startsWith("UntapAdjust")) {
                String[] parse = kw.split(":");
                if (!restrictUntap.containsKey(parse[1])
                        || Integer.parseInt(parse[2]) < restrictUntap.get(parse[1])) {
                    restrictUntap.put(parse[1], Integer.parseInt(parse[2]));
                }
            }
            if (kw.startsWith("OnlyUntapChosen") && !hasChosen) {
                List<String> validTypes = Arrays.asList(kw.split(":")[1].split(","));
                List<String> invalidTypes = new ArrayList<String>(CardType.getAllCardTypes());
                invalidTypes.removeAll(validTypes);
                final String chosen = player.getController().chooseSomeType("Card", new SpellAbility.EmptySa(ApiType.ChooseType, null, player), validTypes, invalidTypes);
                list = CardLists.getType(list,chosen);
                hasChosen = true;
            }
        }
        final CardCollection untapList = new CardCollection(list);
        final String[] restrict = restrictUntap.keySet().toArray(new String[restrictUntap.keySet().size()]);
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (!Untap.canUntap(c)) {
                    return false;
                }
                if (c.isValid(restrict, player, null, null)) {
                    return false;
                }
                return true;
            }
        });

        for (final Card c : list) {
            optionalUntap(c);
        }

        // other players untapping during your untap phase
        List<Card> cardsWithKW = CardLists.getKeyword(game.getCardsIn(ZoneType.Battlefield),
                "CARDNAME untaps during each other player's untap step.");
        final FCollection<Player> otherPlayers = new FCollection<Player>(game.getPlayers());
        otherPlayers.remove(player);
        cardsWithKW = CardLists.filter(cardsWithKW, CardPredicates.isControlledByAnyOf(otherPlayers));
        for (final Card cardWithKW : cardsWithKW) {
            cardWithKW.untap();
        }
        // end other players untapping during your untap phase

        CardCollection restrictUntapped = new CardCollection();
        CardCollection cardList = CardLists.filter(untapList, tappedCanUntap);
        cardList = CardLists.getValidCards(cardList, restrict, player, null, null);

        while (!cardList.isEmpty()) {
            Map<String, Integer> remaining = new HashMap<String, Integer>(restrictUntap);
            for (Entry<String, Integer> entry : remaining.entrySet()) {
                if (entry.getValue() == 0) {
                    cardList.removeAll((Collection<?>)CardLists.getValidCards(cardList, entry.getKey(), player, null));
                    restrictUntap.remove(entry.getKey());
                }
            }
            Card chosen = player.getController().chooseSingleEntityForEffect(cardList, new SpellAbility.EmptySa(ApiType.Untap, null, player), 
                    "Select a card to untap\r\n(Selected:" + restrictUntapped + ")\r\n" + "Remaining cards that can untap: " + remaining);
            if (chosen != null) {
                for (Entry<String, Integer> rest : restrictUntap.entrySet()) {
                    if (chosen.isValid(rest.getKey(), player, null, null)) {
                        restrictUntap.put(rest.getKey(), rest.getValue().intValue() - 1);
                    }
                }
                restrictUntapped.add(chosen);
                cardList.remove(chosen);
            }
        }
        for (Card c : restrictUntapped) {
            optionalUntap(c);
        }

        // Remove temporary keywords
        for (final Card c : player.getCardsIn(ZoneType.Battlefield)) {
            c.removeAllExtrinsicKeyword("This card doesn't untap during your next untap step.");
            c.removeAllExtrinsicKeyword("HIDDEN This card doesn't untap during your next untap step.");
            if (c.hasKeyword("This card doesn't untap during your next two untap steps.")) {
                c.removeAllExtrinsicKeyword("HIDDEN This card doesn't untap during your next two untap steps.");
                c.addHiddenExtrinsicKeyword("HIDDEN This card doesn't untap during your next untap step.");
            }
        }
    } // end doUntap

    private static void optionalUntap(final Card c) {
        if (c.hasKeyword("You may choose not to untap CARDNAME during your untap step.")) {
            if (c.isTapped()) {
                String prompt = "Untap " + c.toString() + "?";
                boolean defaultChoice = true;
                if (c.getGainControlTargets().size() > 0) {
                    final Iterable<Card> targets = c.getGainControlTargets();
                    prompt += "\r\n" + c + " is controlling: ";
                    for (final Card target : targets) {
                        prompt += target;
                        if (target.isInPlay()) {
                            defaultChoice = false;
                        }
                    }
                }
                boolean untap = c.getController().getController().chooseBinary(new SpellAbility.EmptySa(c, c.getController()), prompt, BinaryChoiceType.UntapOrLeaveTapped, defaultChoice);
                if (untap) {
                    c.untap();
                }
            }
        } else {
            c.untap();
        }
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
                    if (list.contains(c.getEquipping())) {
                        continue;
                    }
                } else if (c.isFortification() && c.isFortifying()) {
                    if (list.contains(c.getFortifying())) {
                        continue;
                    }
                }
                c.phase();
            }
        }
    }

} //end class Untap
