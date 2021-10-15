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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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

        doPhasing(game.getPhaseHandler().getPlayerTurn());
        doDayTime(game.getPhaseHandler().getPreviousPlayerTurn());

        game.getAction().checkStaticAbilities();

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
                || c.hasKeyword("This card doesn't untap during your next two untap steps.")
                || c.hasKeyword("This card doesn't untap.")) {
            return false;
        }
        //exerted need current player turn
        final Player playerTurn = c.getGame().getPhaseHandler().getPlayerTurn();

        return !c.isExertedBy(playerTurn);
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
            game.getAction().moveToHand(c, null);
        }
        list.removeAll(bounceList);

        final Map<String, Integer> restrictUntap = Maps.newHashMap();
        boolean hasChosen = false;
        for (KeywordInterface ki : player.getKeywords()) {
            String kw = ki.getOriginal();
            if (kw.startsWith("UntapAdjust")) {
                String[] parse = kw.split(":");
                if (!restrictUntap.containsKey(parse[1])
                        || Integer.parseInt(parse[2]) < restrictUntap.get(parse[1])) {
                    restrictUntap.put(parse[1], Integer.parseInt(parse[2]));
                }
            }
            if (kw.startsWith("OnlyUntapChosen") && !hasChosen) {
                List<String> validTypes = Arrays.asList(kw.split(":")[1].split(","));
                List<String> invalidTypes = Lists.newArrayList(CardType.getAllCardTypes());
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
                return !c.isValid(restrict, player, null, null);
            }
        });

        for (final Card c : list) {
            optionalUntap(c);
        }

        // other players untapping during your untap phase
        List<Card> cardsWithKW = CardLists.getKeyword(game.getCardsIn(ZoneType.Battlefield),
                "CARDNAME untaps during each other player's untap step.");
        cardsWithKW = CardLists.getNotKeyword(cardsWithKW, "This card doesn't untap.");
        cardsWithKW = CardLists.filterControlledBy(cardsWithKW, player.getAllOtherPlayers());

        List<Card> cardsWithKW2 = CardLists.getKeyword(game.getCardsIn(ZoneType.Battlefield),
                "CARDNAME untaps during each opponent's untap step.");
        cardsWithKW2 = CardLists.getNotKeyword(cardsWithKW2, "This card doesn't untap.");
        cardsWithKW2 = CardLists.filterControlledBy(cardsWithKW2, player.getOpponents());

        cardsWithKW.addAll(cardsWithKW2);
        for (final Card cardWithKW : cardsWithKW) {
            if (cardWithKW.isExertedBy(player)) {
                continue;
            }
            cardWithKW.untap(true);
        }
        // end other players untapping during your untap phase

        CardCollection restrictUntapped = new CardCollection();
        CardCollection cardList = CardLists.filter(untapList, tappedCanUntap);
        cardList = CardLists.getValidCards(cardList, restrict, player, null, null);

        while (!cardList.isEmpty()) {
            Map<String, Integer> remaining = Maps.newHashMap(restrictUntap);
            for (Entry<String, Integer> entry : remaining.entrySet()) {
                if (entry.getValue() == 0) {
                    cardList.removeAll(CardLists.getValidCards(cardList, entry.getKey(), player, null, null));
                    restrictUntap.remove(entry.getKey());
                }
            }
            Card chosen = player.getController().chooseSingleEntityForEffect(cardList, new SpellAbility.EmptySa(ApiType.Untap, null, player), 
                    "Select a card to untap\r\n(Selected:" + restrictUntapped + ")\r\n" + "Remaining cards that can untap: " + remaining, null);
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
        // TODO Replace with Static Abilities
        for (final Card c : player.getCardsIn(ZoneType.Battlefield)) {
            c.removeHiddenExtrinsicKeyword("This card doesn't untap during your next untap step.");
            if (c.hasKeyword("This card doesn't untap during your next two untap steps.")) {
                c.removeHiddenExtrinsicKeyword("This card doesn't untap during your next two untap steps.");
                c.addHiddenExtrinsicKeywords(game.getNextTimestamp(), 0, Lists.newArrayList("This card doesn't untap during your next untap step."));
            }
        }
        
        // remove exerted flags from all things in play
        // even if they are not creatures
        for (final Card c : game.getCardsInGame()) {
            c.removeExertedBy(player);
        }
        
    } // end doUntap

    private static void optionalUntap(final Card c) {
        if (c.hasKeyword("You may choose not to untap CARDNAME during your untap step.")) {
            if (c.isTapped()) {
                StringBuilder prompt = new StringBuilder("Untap " + c.toString() + "?");
                boolean defaultChoice = true;
                if (c.getGainControlTargets().size() > 0) {
                    final Iterable<Card> targets = c.getGainControlTargets();
                    prompt.append("\r\n").append(c).append(" is controlling: ");
                    for (final Card target : targets) {
                        prompt.append(target);
                        if (target.isInPlay()) {
                            defaultChoice = false;
                        }
                    }
                }
                boolean untap = c.getController().getController().chooseBinary(new SpellAbility.EmptySa(c, c.getController()), prompt.toString(), BinaryChoiceType.UntapOrLeaveTapped, defaultChoice);
                if (untap) {
                    c.untap(true);
                }
            }
        } else {
            c.untap(true);
        }
    }

    private static void doPhasing(final Player turn) {
        // Needs to include phased out cards
        final List<Card> list = CardLists.filter(turn.getCardsIncludePhasingIn(ZoneType.Battlefield), new Predicate<Card>() {

            @Override
            public boolean apply(final Card c) {
                return ((c.isPhasedOut() && c.isDirectlyPhasedOut()) || c.hasKeyword(Keyword.PHASING));
            }
        });

        // If c has things attached to it, they phase out simultaneously, and
        // will phase back in with it
        // If c is attached to something, it will phase out on its own, and try
        // to attach back to that thing when it comes back
        for (final Card c : list) {
            if (c.isPhasedOut()) {
                c.phase(true);
            } else if (c.hasKeyword(Keyword.PHASING)) {
                // 702.23g If an object would simultaneously phase out directly
                // and indirectly, it just phases out indirectly.
                if (c.isAttachment()) {
                    final Card ent = c.getAttachedTo();
                    if (ent != null && list.contains(ent)) {
                        continue;
                    }
                }
                c.phase(true);
            }
        }
    }

    private static void doDayTime(final Player previous) {
        if (previous == null) {
            return;
        }
        final Game game = previous.getGame();
        List<Card> casted = game.getStack().getSpellsCastLastTurn();

        if (game.isDay() && !Iterables.any(casted, CardPredicates.isController(previous))) {
            game.setDayTime(true);
        } else if (game.isNight() && Iterables.size(Iterables.filter(casted, CardPredicates.isController(previous))) > 1) {
            game.setDayTime(false);
        }
    }
} //end class Untap
