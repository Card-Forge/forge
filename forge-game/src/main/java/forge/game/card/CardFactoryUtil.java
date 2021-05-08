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
package forge.game.card;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.CardPredicates.Presets;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.AlternativeCost;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityRestriction;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityCantBeCast;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Expressions;
import forge.util.Lang;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;
import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;

/**
 * <p>
 * CardFactoryUtil class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class CardFactoryUtil {

    public static SpellAbility buildBasicLandAbility(final CardState state, byte color) {
        String strcolor = MagicColor.toShortString(color);
        String abString  = "AB$ Mana | Cost$ T | Produced$ " + strcolor +
                " | Secondary$ True | SpellDescription$ Add {" + strcolor + "}.";
        SpellAbility sa = AbilityFactory.getAbility(abString, state);
        sa.setIntrinsic(true); // always intristic
        return sa;
    }

    /**
     * <p>
     * abilityMorphDown.
     * </p>
     *
     * @param sourceCard
     *            a {@link forge.game.card.Card} object.
     * @return a {@link forge.game.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityMorphDown(final CardState cardState) {
        final Spell morphDown = new Spell(cardState.getCard(), new Cost(ManaCost.THREE, false)) {
            private static final long serialVersionUID = -1438810964807867610L;

            @Override
            public void resolve() {
                if (!hostCard.isFaceDown()) {
                    hostCard.setOriginalStateAsFaceDown();
                }
                hostCard.getGame().getAction().moveToPlay(hostCard, this);
            }

            @Override
            public boolean canPlay() {
                if (hostCard.isInPlay()) {
                    return false; // cut short if already on the battlefield, avoids side effects when checking statics
                }
                CardStateName stateBackup = hostCard.getCurrentStateName();
                boolean face = hostCard.isFaceDown();
                hostCard.turnFaceDownNoUpdate();
                boolean success = super.canPlay();
                hostCard.setState(stateBackup, false);
                hostCard.setFaceDown(face);
                return success;
            }
        };

        morphDown.setCardState(cardState);

        morphDown.setDescription("(You may cast this card face down as a 2/2 creature for {3}.)");
        morphDown.setStackDescription("Morph - Creature 2/2");
        morphDown.setCastFaceDown(true);
        morphDown.setBasicSpell(false);

        return morphDown;
    }

    /**
     * <p>
     * abilityMorphUp.
     * </p>
     *
     * @param sourceCard
     *            a {@link forge.game.card.Card} object.
     * @return a {@link forge.game.spellability.AbilityActivated} object.
     */
    public static SpellAbility abilityMorphUp(final CardState cardState, final String costStr, final boolean mega) {
        Cost cost = new Cost(costStr, true);
        String costDesc = cost.toString();
        StringBuilder sbCost = new StringBuilder(mega ? "Megamorph" : "Morph");
        sbCost.append(" ");
        if (!cost.isOnlyManaCost()) {
            sbCost.append("— ");
        }
        // get rid of the ": " at the end
        sbCost.append(costDesc, 0, costDesc.length() - 2);

        StringBuilder sb = new StringBuilder();
        sb.append("ST$ SetState | Cost$ ").append(costStr).append(" | CostDesc$ ").append(sbCost);
        sb.append(" | MorphUp$ True | Secondary$ True | IsPresent$ Card.Self+faceDown");
        if (mega) {
            sb.append(" | Mega$ True");
        }

        sb.append(" | Mode$ TurnFace | SpellDescription$ (Turn this face up any time for its morph cost.)");

        final SpellAbility morphUp = AbilityFactory.getAbility(sb.toString(), cardState);

        // if Cost has X in cost, need to check source for an SVar for this
        if (cost.hasXInAnyCostPart() && cardState.hasSVar("X")) {
            morphUp.setSVar("X", cardState.getSVar("X"));
        }

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(cardState.getName()).append(" - turn this card face up.");
        morphUp.setStackDescription(sbStack.toString());

        return morphUp;
    }

    public static SpellAbility abilityManifestFaceUp(final Card sourceCard, final ManaCost manaCost) {

        String costDesc = manaCost.toString();

        // Cost need to be set later
        StringBuilder sb = new StringBuilder();
        sb.append("ST$ SetState | Cost$ 0 | CostDesc$ Unmanifest ").append(costDesc);
        sb.append(" | ManifestUp$ True | Secondary$ True | PresentDefined$ Self | IsPresent$ Card.faceDown+manifested");
        sb.append(" | Mode$ TurnFace | SpellDescription$ (Turn this face up any time for its mana cost.)");

        final SpellAbility manifestUp = AbilityFactory.getAbility(sb.toString(), sourceCard);
        manifestUp.setPayCosts(new Cost(manaCost, true));

        final StringBuilder sbStack = new StringBuilder();
        sbStack.append(sourceCard.getName()).append(" - turn this card face up.");
        manifestUp.setStackDescription(sbStack.toString());

        return manifestUp;
    }

    public static boolean handleHiddenAgenda(Player player, Card card) {
        SpellAbility sa = new SpellAbility.EmptySa(card);
        sa.putParam("AILogic", card.getSVar("AgendaLogic"));
        Predicate<ICardFace> cpp = Predicates.alwaysTrue();
        //Predicate<Card> pc = Predicates.in(player.getAllCards());
        // TODO This would be better to send in the player's deck, not all cards
        String name = player.getController().chooseCardName(sa, cpp, "Card",
                "Name a card for " + card.getName());
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (card.hasKeyword("Double agenda")) {
            String name2 = player.getController().chooseCardName(sa, cpp, "Card",
                    "Name a second card for " + card.getName());
            if (name2 == null || name2.isEmpty()) {
                return false;
            }
            card.setNamedCard2(name2);
        }

        card.setNamedCard(name);
        card.turnFaceDown();
        card.addMayLookAt(player.getGame().getNextTimestamp(), ImmutableList.of(player));
        card.addSpellAbility(abilityRevealHiddenAgenda(card));
        return true;
    }

    private static SpellAbility abilityRevealHiddenAgenda(final Card sourceCard) {
        String ab = "ST$ SetState | Cost$ 0"
                + " | ConditionDefined$ Self | ConditionPresent$ Card.faceDown+inZoneCommand"
                + " | HiddenAgenda$ True"
                + " | Mode$ TurnFace | SpellDescription$ Reveal this Hidden Agenda at any time.";
        return AbilityFactory.getAbility(ab, sourceCard);
    }

    // does "target" have protection from "card"?
    /**
     * <p>
     * hasProtectionFrom.
     * </p>
     *
     * @param card
     *            a {@link forge.game.card.Card} object.
     * @param target
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean hasProtectionFrom(final Card card, final Card target) {
        if (target == null) {
            return false;
        }

        return target.hasProtectionFrom(card);
    }

    /**
     * <p>
     * isCounterable.
     * </p>
     *
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean isCounterable(final Card c) {
        return !c.hasKeyword("CARDNAME can't be countered.") && c.getCanCounter();
    }

    /**
     * <p>
     * isCounterableBy.
     * </p>
     *
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            the sa
     * @return a boolean.
     */
    public static boolean isCounterableBy(final Card c, final SpellAbility sa) {
        if (!isCounterable(c)) {
            return false;
        }

        for (KeywordInterface k : c.getKeywords()) {
            final String o = k.getOriginal();
            if (o.startsWith("CantBeCounteredBy")) {
                final String[] m = o.split(":");
                if (sa.isValid(m[1].split(","), c.getController(), c, null)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * countOccurrences.
     * </p>
     *
     * @param arg1
     *            a {@link java.lang.String} object.
     * @param arg2
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int countOccurrences(final String arg1, final String arg2) {
        int count = 0;
        int index = 0;
        while ((index = arg1.indexOf(arg2, index)) != -1) {
            ++index;
            ++count;
        }
        return count;
    }

    /**
     * <p>
     * parseMath.
     * </p>
     *
     * @param expression
     *            a {@link java.lang.String} object.
     * @return an array of {@link java.lang.String} objects.
     */
    public static String extractOperators(final String expression) {
        String[] l = expression.split("/");
        return l.length > 1 ? l[1] : null;
    }

    /**
     * <p>
     * Parse player targeted X variables.
     * </p>
     *
     * @param objects
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int objectXCount(final List<?> objects, final String s, final Card source) {
        if (objects.isEmpty()) {
            return 0;
        }

        if (s.startsWith("Valid")) {
            return CardFactoryUtil.handlePaid(Iterables.filter(objects, Card.class), s, source);
        }

        int n = s.startsWith("Amount") ? objects.size() : 0;
        return doXMath(n, extractOperators(s), source);
    }

    /**
     * <p>
     * Parse player targeted X variables.
     * </p>
     *
     * @param players
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int playerXCount(final List<Player> players, final String s, final Card source) {
        if (players.size() == 0) {
            return 0;
        }

        final String[] l = s.split("/");
        final String m = extractOperators(s);

        int n = 0;

        if (l[0].startsWith("TotalCommanderCastFromCommandZone")) {
            int totCast = 0;
            for (Player p : players) {
                totCast += p.getTotalCommanderCast();
            }
            return doXMath(totCast, m, source);
        }

        // methods for getting the highest/lowest playerXCount from a range of players
        if (l[0].startsWith("Highest")) {
            for (final Player player : players) {
                final int current = playerXProperty(player, TextUtil.fastReplace(s, "Highest", ""), source);
                if (current > n) {
                    n = current;
                }
            }

            return doXMath(n, m, source);
        }

        if (l[0].startsWith("Lowest")) {
            n = 99999; // if no players have fewer than 99999 valids, the game is frozen anyway
            for (final Player player : players) {
                final int current = playerXProperty(player, TextUtil.fastReplace(s, "Lowest", ""), source);
                if (current < n) {
                    n = current;
                }
            }
            return doXMath(n, m, source);
        }

        if (l[0].startsWith("TiedForHighestLife")) {
            int maxLife = Integer.MIN_VALUE;
            for (final Player player : players) {
                int highestTotal = playerXProperty(player, "LifeTotal", source);
                if (highestTotal > maxLife) {
                    maxLife = highestTotal;
                }
            }
            int numTied = 0;
            for (final Player player : players) {
                if (player.getLife() == maxLife) {
                    numTied++;
                }
            }
            return doXMath(numTied, m, source);
        }

        if (l[0].startsWith("TiedForLowestLife")) {
            int minLife = Integer.MAX_VALUE;
            for (final Player player : players) {
                int lowestTotal = playerXProperty(player, "LifeTotal", source);
                if (lowestTotal < minLife) {
                    minLife = lowestTotal;
                }
            }
            int numTied = 0;
            for (final Player player : players) {
                if (player.getLife() == minLife) {
                    numTied++;
                }
            }
            return doXMath(numTied, m, source);
        }

        final String[] sq;
        sq = l[0].split("\\.");

        // the number of players passed in
        if (sq[0].equals("Amount")) {
            return doXMath(players.size(), m, source);
        }

        if (sq[0].startsWith("HasProperty")) {
            int totPlayer = 0;
            String property = sq[0].substring(11);
            for (Player p : players) {
                if (p.hasProperty(property, source.getController(), source, null)) {
                    totPlayer++;
                }
            }
            return doXMath(totPlayer, m, source);
        }

        if (sq[0].contains("DamageThisTurn")) {
            int totDmg = 0;
            for (Player p : players) {
                totDmg += p.getAssignedDamage();
            }
            return doXMath(totDmg, m, source);
        } else if (sq[0].contains("LifeLostThisTurn")) {
            int totDmg = 0;
            for (Player p : players) {
                totDmg += p.getLifeLostThisTurn();
            }
            return doXMath(totDmg, m, source);
        }

        if (players.size() > 0) {
            int totCount = 0;
            for (Player p : players) {
                totCount += playerXProperty(p, s, source);
            }
            return totCount;
        }

        return doXMath(n, m, source);
    }

    public static int playerXProperty(final Player player, final String s, final Card source) {
        final String[] l = s.split("/");
        final String m = extractOperators(s);

        final Game game = player.getGame();

        // count valid cards in any specified zone/s
        if (l[0].startsWith("Valid") && !l[0].contains("Valid ")) {
            String[] lparts = l[0].split(" ", 2);
            final List<ZoneType> vZone = ZoneType.listValueOf(lparts[0].split("Valid")[1]);
            String restrictions = TextUtil.fastReplace(l[0], TextUtil.addSuffix(lparts[0]," "), "");
            final String[] rest = restrictions.split(",");
            CardCollection cards = CardLists.getValidCards(game.getCardsIn(vZone), rest, player, source, null);
            return doXMath(cards.size(), m, source);
        }

        // count valid cards on the battlefield
        if (l[0].startsWith("Valid ")) {
            final String restrictions = l[0].substring(6);
            final String[] rest = restrictions.split(",");
            CardCollection cardsonbattlefield = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), rest, player, source, null);
            return doXMath(cardsonbattlefield.size(), m, source);
        }

        final String[] sq = l[0].split("\\.");
        final String value = sq[0];

        if (value.contains("CardsInHand")) {
            return doXMath(player.getCardsIn(ZoneType.Hand).size(), m, source);
        }

        if (value.contains("NumPowerSurgeLands")) {
            return doXMath(player.getNumPowerSurgeLands(), m, source);
        }

        if (value.contains("DomainPlayer")) {
            int n = 0;
            final CardCollectionView someCards = player.getCardsIn(ZoneType.Battlefield);
            final List<String> basic = MagicColor.Constant.BASIC_LANDS;

            for (int i = 0; i < basic.size(); i++) {
                if (!CardLists.getType(someCards, basic.get(i)).isEmpty()) {
                    n++;
                }
            }
            return doXMath(n, m, source);
        }

        if (value.contains("CardsInLibrary")) {
            return doXMath(player.getCardsIn(ZoneType.Library).size(), m, source);
        }

        if (value.contains("CardsInGraveyard")) {
            return doXMath(player.getCardsIn(ZoneType.Graveyard).size(), m, source);
        }
        if (value.contains("LandsInGraveyard")) {
            return doXMath(CardLists.getType(player.getCardsIn(ZoneType.Graveyard), "Land").size(), m, source);
        }

        if (value.contains("CreaturesInPlay")) {
            return doXMath(player.getCreaturesInPlay().size(), m, source);
        }

        if (value.contains("CardsInPlay")) {
            return doXMath(player.getCardsIn(ZoneType.Battlefield).size(), m, source);
        }

        if (value.contains("StartingLife")) {
            return doXMath(player.getStartingLife(), m, source);
        }

        if (value.contains("LifeTotal")) {
            return doXMath(player.getLife(), m, source);
        }

        if (value.contains("LifeLostThisTurn")) {
            return doXMath(player.getLifeLostThisTurn(), m, source);
        }

        if (value.contains("LifeLostLastTurn")) {
            return doXMath(player.getLifeLostLastTurn(), m, source);
        }

        if (value.contains("LifeGainedThisTurn")) {
            return doXMath(player.getLifeGainedThisTurn(), m, source);
        }

        if (value.contains("LifeGainedByTeamThisTurn")) {
            return doXMath(player.getLifeGainedByTeamThisTurn(), m, source);
        }

        if (value.contains("LifeStartedThisTurnWith")) {
            return doXMath(player.getLifeStartedThisTurnWith(), m, source);
        }

        if (value.contains("PoisonCounters")) {
            return doXMath(player.getPoisonCounters(), m, source);
        }

        if (value.contains("TopOfLibraryCMC")) {
            return doXMath(Aggregates.sum(player.getCardsIn(ZoneType.Library, 1), CardPredicates.Accessors.fnGetCmc), m, source);
        }

        if (value.contains("LandsPlayed")) {
            return doXMath(player.getLandsPlayedThisTurn(), m, source);
        }

        if (value.contains("CardsDrawn")) {
            return doXMath(player.getNumDrawnThisTurn(), m, source);
        }

        if (value.contains("CardsDiscardedThisTurn")) {
            return doXMath(player.getNumDiscardedThisTurn(), m, source);
        }

        if (value.contains("TokensCreatedThisTurn")) {
            return doXMath(player.getNumTokensCreatedThisTurn(), m, source);
        }

        if (value.contains("AttackersDeclared")) {
            return doXMath(player.getAttackersDeclaredThisTurn(), m, source);
        }

        if (value.equals("DamageDoneToPlayerBy")) {
            return doXMath(source.getDamageDoneToPlayerBy(player.getName()), m, source);
        }

        if (value.contains("DamageToOppsThisTurn")) {
            int oppDmg = 0;
            for (Player opp : player.getOpponents()) {
                oppDmg += opp.getAssignedDamage();
            }
            return doXMath(oppDmg, m, source);
        }

        if (value.contains("NonCombatDamageDealtThisTurn")) {
            return doXMath(player.getAssignedDamage() - player.getAssignedCombatDamage(), m, source);
        }

        if (value.equals("OpponentsAttackedThisTurn")) {
            return doXMath(player.getAttackedOpponentsThisTurn().size(), m, source);
        }

        return doXMath(0, m, source);
    }

    /**
     * <p>
     * Parse non-mana X variables.
     * </p>
     *
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param expression
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int xCount(final Card c, final String expression) {
        if (StringUtils.isBlank(expression) || c == null) {
            return 0;
        }
        if (StringUtils.isNumeric(expression)) {
            return Integer.parseInt(expression);
        }

        final Player cc = c.getController();
        final Game game = c.getGame();
        final Player activePlayer = game.getPhaseHandler().getPlayerTurn();

        final String[] l = expression.split("/");
        final String m = extractOperators(expression);

        // accept straight numbers
        if (l[0].startsWith("Number$")) {
            final String number = l[0].substring(7);
            if (number.equals("ChosenNumber")) {
                int x = c.getChosenNumber() == null ? 0 : c.getChosenNumber();
                return doXMath(x, m, c);
            }
            return doXMath(Integer.parseInt(number), m, c);
        }

        if (l[0].startsWith("Count$")) {
            l[0] = l[0].substring(6);
        }

        if (l[0].startsWith("SVar$")) {
            return doXMath(xCount(c, c.getSVar(l[0].substring(5))), m, c);
        }

        if (l[0].startsWith("Controller$")) {
            return playerXProperty(cc, l[0].substring(11), c);
        }

        // Manapool
        if (l[0].startsWith("ManaPool")) {
            final String color = l[0].split(":")[1];
            if (color.equals("All")) {
                return cc.getManaPool().totalMana();
            }
            return cc.getManaPool().getAmountOfColor(ManaAtom.fromName(color));
        }

        // count valid cards in any specified zone/s
        if (l[0].startsWith("Valid")) {
            String[] lparts = l[0].split(" ", 2);
            final String[] rest = lparts[1].split(",");

            final CardCollectionView cardsInZones = lparts[0].length() > 5
                ? game.getCardsIn(ZoneType.listValueOf(lparts[0].substring(5)))
                : game.getCardsIn(ZoneType.Battlefield);

            CardCollection cards = CardLists.getValidCards(cardsInZones, rest, cc, c, null);
            return doXMath(cards.size(), m, c);
        }

        if (l[0].startsWith("ImprintedCardManaCost") && !c.getImprintedCards().isEmpty()) {
            return c.getImprintedCards().get(0).getCMC();
        }

        if (l[0].startsWith("GreatestPower")) {
            final String[] lparts = l[0].split("_", 2);
            final String[] rest = lparts[1].split(",");
            final CardCollectionView cardsInZones = lparts[0].length() > 13
                    ? game.getCardsIn(ZoneType.listValueOf(lparts[0].substring(13)))
                    : game.getCardsIn(ZoneType.Battlefield);
            CardCollection list = CardLists.getValidCards(cardsInZones, rest, cc, c, null);
            int highest = 0;
            for (final Card crd : list) {
                if (crd.getNetPower() > highest) {
                    highest = crd.getNetPower();
                }
            }
            return highest;
        }

        if (l[0].startsWith("GreatestToughness_")) {
            final String restriction = l[0].substring(18);
            final String[] rest = restriction.split(",");
            CardCollection list = CardLists.getValidCards(cc.getGame().getCardsIn(ZoneType.Battlefield), rest, cc, c, null);
            int highest = 0;
            for (final Card crd : list) {
                if (crd.getNetToughness() > highest) {
                    highest = crd.getNetToughness();
                }
            }
            return highest;
        }

        if (l[0].startsWith("HighestCMC_")) {
            final String restriction = l[0].substring(11);
            final String[] rest = restriction.split(",");
            CardCollection list = CardLists.getValidCards(cc.getGame().getCardsInGame(), rest, cc, c, null);
            int highest = 0;
            for (final Card crd : list) {
                // dont check for Split card anymore
                if (crd.getCMC() > highest) {
                    highest = crd.getCMC();
                }
            }
            return highest;
        }

        if (l[0].startsWith("MostCardName")) {
        	String[] lparts = l[0].split(" ", 2);
            final String[] rest = lparts[1].split(",");

            final CardCollectionView cardsInZones = lparts[0].length() > 12
                ? game.getCardsIn(ZoneType.listValueOf(lparts[0].substring(12)))
                : game.getCardsIn(ZoneType.Battlefield);

            CardCollection cards = CardLists.getValidCards(cardsInZones, rest, cc, c, null);
            final Map<String, Integer> map = Maps.newHashMap();
            for (final Card card : cards) {
                // Remove Duplicated types
                final String name = card.getName();
                Integer count = map.get(name);
                map.put(name, count == null ? 1 : count + 1);
            }
            int max = 0;
            for (final Entry<String, Integer> entry : map.entrySet()) {
                if (max < entry.getValue()) {
                    max = entry.getValue();
                }
            }
            return max;
        }

        if (l[0].startsWith("DifferentCardNames_")) {
            final List<String> crdname = Lists.newArrayList();
            final String restriction = l[0].substring(19);
            final String[] rest = restriction.split(",");
            CardCollection list = CardLists.getValidCards(cc.getGame().getCardsInGame(), rest, cc, c, null);
            for (final Card card : list) {
                String name = card.getName();
                // CR 201.2b Those objects have different names only if each of them has at least one name and no two objects in that group have a name in common
                if (!crdname.contains(name) && !name.isEmpty()) {
                    crdname.add(name);
                }
            }
            return doXMath(crdname.size(), m, c);
        }

        if (l[0].startsWith("DifferentPower_")) {
            final List<Integer> powers = Lists.newArrayList();
            final String restriction = l[0].substring(15);
            final String[] rest = restriction.split(",");
            CardCollection list = CardLists.getValidCards(cc.getGame().getCardsIn(ZoneType.Battlefield), rest, cc, c, null);
            for (final Card card : list) {
                Integer pow = card.getNetPower();
                if (!powers.contains(pow)) {
                    powers.add(pow);
                }
            }
            return doXMath(powers.size(), m, c);
        }

        if (l[0].startsWith("RememberedSize")) {
            return doXMath(c.getRememberedCount(), m, c);
        }

        if (l[0].startsWith("RememberedNumber")) {
            int num = 0;
            for (final Object o : c.getRemembered()) {
                if (o instanceof Integer) {
                    num += (Integer) o;
                }
            }
            return doXMath(num, m, c);
        }

        if (l[0].startsWith("RememberedWithSharedCardType")) {
            int maxNum = 1;
            for (final Object o : c.getRemembered()) {
                if (o instanceof Card) {
                    int num = 1;
                    Card firstCard = (Card) o;
                    for (final Object p : c.getRemembered()) {
                        if (p instanceof Card) {
                            Card secondCard = (Card) p;
                            if (!firstCard.equals(secondCard) && firstCard.sharesCardTypeWith(secondCard)) {
                                num++;
                            }
                        }
                    }
                    if (num > maxNum) {
                        maxNum = num;
                    }
                }
            }
            return doXMath(maxNum, m, c);
        }

        if (l[0].startsWith("CommanderCastFromCommandZone")) {
            // only used by Opal Palace, and it does add the trigger to the card
            return doXMath(cc.getCommanderCast(c), m, c);
        }

        if (l[0].startsWith("TotalCommanderCastFromCommandZone")) {
            return doXMath(cc.getTotalCommanderCast(), m, c);
        }

        if (l[0].startsWith("MostProminentCreatureType")) {
            String restriction = l[0].split(" ")[1];
            CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), restriction, cc, c, null);
            return doXMath(getMostProminentCreatureTypeSize(list), m, c);
        }

        if (l[0].startsWith("SecondMostProminentColor")) {
            String restriction = l[0].split(" ")[1];
            CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), restriction, cc, c, null);
            int[] colorSize = SortColorsFromList(list);
            return doXMath(colorSize[colorSize.length - 2], m, c);
        }

        if (l[0].startsWith("RolledThisTurn")) {
            return game.getPhaseHandler().getPlanarDiceRolledthisTurn();
        }

        //SacrificedThisTurn <type>
        if (l[0].startsWith("SacrificedThisTurn")) {
            CardCollectionView list = cc.getSacrificedThisTurn();
            if (l[0].contains(" ")) {
                String[] lparts = l[0].split(" ", 2);
                String restrictions = TextUtil.fastReplace(l[0], TextUtil.addSuffix(lparts[0]," "), "");
                final String[] rest = restrictions.split(",");
                list = CardLists.getValidCards(list, rest, cc, c, null);
            }
            return doXMath(list.size(), m, c);
        }

        final String[] sq;
        sq = l[0].split("\\.");

        if (sq[0].contains("xPaid")) {
            return doXMath(c.getXManaCostPaid(), m, c);
        }

        if (sq[0].contains("xColorPaid")) {
            String[] attrs = sq[0].split(" ");
            StringBuilder colors = new StringBuilder();
            for (int i = 1; i < attrs.length; i++) {
                colors.append(attrs[i]);
            }
            return doXMath(c.getXManaCostPaidCount(colors.toString()), m, c);
        }

        if (sq[0].equals("YouCycledThisTurn")) {
            return doXMath(cc.getCycledThisTurn(), m, c);
        }

        if (sq[0].equals("YouDrewThisTurn")) {
            return doXMath(cc.getNumDrawnThisTurn(), m, c);
        }

        if (sq[0].equals("YouSurveilThisTurn")) {
            return doXMath(cc.getSurveilThisTurn(), m, c);
        }

        if (sq[0].equals("YouCastThisGame")) {
            return doXMath(cc.getSpellsCastThisGame(), m, c);
        }

        if (sq[0].equals("StormCount")) {
            return doXMath(game.getStack().getSpellsCastThisTurn().size() - 1, m, c);
        }
        if (sq[0].startsWith("DamageDoneByPlayerThisTurn")) {
            int sum = 0;
            for (Player p : AbilityUtils.getDefinedPlayers(c, sq[1], null)) {
                sum += c.getReceivedDamageByPlayerThisTurn(p);
            }
            return doXMath(sum, m, c);
        }
        if (sq[0].equals("DamageDoneThisTurn")) {
            return doXMath(c.getDamageDoneThisTurn(), m, c);
        }
        if (sq[0].equals("BloodthirstAmount")) {
            return doXMath(cc.getBloodthirstAmount(), m, c);
        }
        if (sq[0].equals("RegeneratedThisTurn")) {
            return doXMath(c.getRegeneratedThisTurn(), m, c);
        }

        if (sq[0].contains("YourStartingLife")) {
            return doXMath(cc.getStartingLife(), m, c);
        }

        if (sq[0].contains("YourLifeTotal")) {
            return doXMath(cc.getLife(), m, c);
        }
        if (sq[0].contains("OppGreatestLifeTotal")) {
            return doXMath(cc.getOpponentsGreatestLifeTotal(), m, c);
        }
        if (sq[0].contains("OppsAtLifeTotal")) {
                final int lifeTotal = xCount(c, sq[1]);
                int number = 0;
                for (final Player opp : cc.getOpponents()) {
                        if (opp.getLife() == lifeTotal) {
                                number++;
                        }
                }
                return doXMath(number, m, c);
        }

        //  Count$TargetedLifeTotal (targeted player's life total)
        if (sq[0].contains("TargetedLifeTotal")) {
            // This doesn't work in some circumstances, since the active SA isn't passed through
            for (final SpellAbility sa : c.getCurrentState().getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player tgtP : saTargeting.getTargets().getTargetPlayers()) {
                        return doXMath(tgtP.getLife(), m, c);
                    }
                }
            }
        }

        if (sq[0].contains("LifeYouLostThisTurn")) {
            return doXMath(cc.getLifeLostThisTurn(), m, c);
        }
        if (sq[0].contains("LifeYouGainedThisTurn")) {
            return doXMath(cc.getLifeGainedThisTurn(), m, c);
        }
        if (sq[0].contains("LifeYourTeamGainedThisTurn")) {
            return doXMath(cc.getLifeGainedByTeamThisTurn(), m, c);
        }
        if (sq[0].contains("LifeYouGainedTimesThisTurn")) {
            return doXMath(cc.getLifeGainedTimesThisTurn(), m, c);
        }
        if (sq[0].contains("LifeOppsLostThisTurn")) {
            return doXMath(cc.getOpponentLostLifeThisTurn(), m, c);
        }
        if (sq[0].equals("TotalDamageDoneByThisTurn")) {
            return doXMath(c.getTotalDamageDoneBy(), m, c);
        }
        if (sq[0].equals("TotalDamageReceivedThisTurn")) {
            return doXMath(c.getTotalDamageRecievedThisTurn(), m, c);
        }

        if (sq[0].startsWith("YourCounters")) {
            // "YourCountersExperience" or "YourCountersPoison"
            String counterType = sq[0].substring(12);
            return doXMath(cc.getCounters(CounterType.getType(counterType)), m, c);
        }

        if (sq[0].contains("YourPoisonCounters")) {
            return doXMath(cc.getPoisonCounters(), m, c);
        }
        if (sq[0].contains("TotalOppPoisonCounters")) {
            return doXMath(cc.getOpponentsTotalPoisonCounters(), m, c);
        }

        if (sq[0].contains("YourDamageThisTurn")) {
            return doXMath(cc.getAssignedDamage(), m, c);
        }
        if (sq[0].contains("TotalOppDamageThisTurn")) {
            return doXMath(cc.getOpponentsAssignedDamage(), m, c);
        }
        if (sq[0].contains("MaxOppDamageThisTurn")) {
            return doXMath(cc.getMaxOpponentAssignedDamage(), m, c);
        }

        // Count$YourTypeDamageThisTurn Type
        if (sq[0].contains("YourTypeDamageThisTurn")) {
            return doXMath(cc.getAssignedDamage(sq[0].split(" ")[1]), m, c);
        }
        if (sq[0].contains("YourDamageSourcesThisTurn")) {
            Iterable<Card> allSrc = cc.getAssignedDamageSources();
            String restriction = sq[0].split(" ")[1];
            CardCollection filtered = CardLists.getValidCards(allSrc, restriction, cc, c, null);
            return doXMath(filtered.size(), m, c);
        }

        if (sq[0].contains("YourLandsPlayed")) {
            return doXMath(cc.getLandsPlayedThisTurn(), m, c);
        }

        // Count$TopOfLibraryCMC
        if (sq[0].contains("TopOfLibraryCMC")) {
            int cmc = cc.getCardsIn(ZoneType.Library).isEmpty() ? 0 :
                cc.getCardsIn(ZoneType.Library).getFirst().getCMC();
            return doXMath(cmc, m, c);
        }

        // Count$EnchantedControllerCreatures
        if (sq[0].contains("EnchantedControllerCreatures")) {
            if (c.getEnchantingCard() != null) {
                return CardLists.count(c.getEnchantingCard().getController().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
            }
            return 0;
        }

        // Count$Chroma.<color name>
        if (sq[0].contains("Chroma")) {
            ZoneType sourceZone = sq[0].contains("ChromaInGrave") ?  ZoneType.Graveyard : ZoneType.Battlefield;
            final CardCollectionView cards;
            if (sq[0].contains("ChromaSource")) { // Runs Chroma for passed in Source card
                cards = new CardCollection(c);
            }
            else {
                cards = cc.getCardsIn(sourceZone);
            }

            int colorOcurrencices = 0;
            byte colorCode = ManaAtom.fromName(sq[1]);
            for (Card c0 : cards) {
                for (ManaCostShard sh : c0.getManaCost()){
                    if ((sh.getColorMask() & colorCode) != 0)
                        colorOcurrencices++;
                }
            }
            return doXMath(colorOcurrencices, m, c);
        }
        // Count$DevotionDual.<color name>.<color name>
        // Count$Devotion.<color name>
        if (sq[0].contains("Devotion")) {
            int colorOcurrencices = 0;
            String colorName = sq[1];
            if (colorName.contains("Chosen")) {
                colorName = MagicColor.toShortString(c.getChosenColor());
            }
            byte colorCode = ManaAtom.fromName(colorName);
            if (sq[0].equals("DevotionDual")) {
                colorCode |= ManaAtom.fromName(sq[2]);
            }
            for (Card c0 : cc.getCardsIn(ZoneType.Battlefield)) {
                for (ManaCostShard sh : c0.getManaCost()) {
                    if ((sh.getColorMask() & colorCode) != 0) {
                        colorOcurrencices++;
                    }
                }
                colorOcurrencices += c0.getAmountOfKeyword("Your devotion to each color and each combination of colors is increased by one.");
            }
            return doXMath(colorOcurrencices, m, c);
        }

        if (sq[0].contains("ColorsCtrl")) {
            final String restriction = l[0].substring(11);
            final String[] rest = restriction.split(",");
            final CardCollection list = CardLists.getValidCards(cc.getCardsIn(ZoneType.Battlefield), rest, cc, c, null);
            byte n = 0;
            for (final Card card : list) {
                n |= card.determineColor().getColor();
            }
            return doXMath(ColorSet.fromMask(n).countColors(), m, c);
        }

        if (sq[0].equals("ColorsColorIdentity")) {
            return doXMath(c.getController().getCommanderColorID().countColors(), m, c);
        }

        if (sq[0].contains("CreatureType")) {
            String[] sqparts = l[0].split(" ", 2);
            final String[] rest = sqparts[1].split(",");

            final CardCollectionView cardsInZones = sqparts[0].length() > 12
                ? game.getCardsIn(ZoneType.listValueOf(sqparts[0].substring(12)))
                : game.getCardsIn(ZoneType.Battlefield);

            CardCollection cards = CardLists.getValidCards(cardsInZones, rest, cc, c, null);
            final Set<String> creatTypes = Sets.newHashSet();

            for (Card card : cards) {
                Iterables.addAll(creatTypes, card.getType().getCreatureTypes());
            }
            int n = creatTypes.contains(CardType.AllCreatureTypes) ? CardType.getAllCreatureTypes().size() : creatTypes.size();
            return doXMath(n, m, c);
        }

        if (sq[0].contains("ExactManaCost")) {
            String[] sqparts = l[0].split(" ", 2);
            final String[] rest = sqparts[1].split(",");

            final CardCollectionView cardsInZones = sqparts[0].length() > 13
                ? game.getCardsIn(ZoneType.listValueOf(sqparts[0].substring(13)))
                : game.getCardsIn(ZoneType.Battlefield);

            CardCollection cards = CardLists.getValidCards(cardsInZones, rest, cc, c, null);
            final Set<String> manaCost = Sets.newHashSet();

            for (Card card : cards) {
                manaCost.add(card.getManaCost().getShortString());
            }

            return doXMath(manaCost.size(), m, c);
        }

        if (sq[0].contains("Hellbent")) {
            return doXMath(Integer.parseInt(sq[cc.hasHellbent() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Metalcraft")) {
            return doXMath(Integer.parseInt(sq[cc.hasMetalcraft() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Delirium")) {
            return doXMath(Integer.parseInt(sq[cc.hasDelirium() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("FatefulHour")) {
            return doXMath(Integer.parseInt(sq[cc.getLife() <= 5 ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Revolt")) {
            return doXMath(Integer.parseInt(sq[cc.hasRevolt() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Landfall")) {
            return doXMath(Integer.parseInt(sq[cc.hasLandfall() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Monarch")) {
            return doXMath(Integer.parseInt(sq[cc.isMonarch() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Blessing")) {
            return doXMath(Integer.parseInt(sq[cc.hasBlessing() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Threshold")) {
            return doXMath(Integer.parseInt(sq[cc.hasThreshold() ? 1 : 2]), m, c);
        }
        if (sq[0].contains("Averna")) {
            String str = "As you cascade, you may put a land card from among the exiled cards onto the " +
                    "battlefield tapped.";
            return cc.getKeywords().getAmount(str);
        }
        if (sq[0].startsWith("Kicked")) {
            return doXMath(Integer.parseInt(sq[c.getKickerMagnitude() > 0 ? 1 : 2]), m, c);
        }
        if (sq[0].startsWith("Escaped")) {
            return doXMath(Integer.parseInt(sq[c.getCastSA() != null && c.getCastSA().isEscape() ? 1 : 2]), m, c);
        }
        if (sq[0].startsWith("AltCost")) {
            return doXMath(Integer.parseInt(sq[c.isOptionalCostPaid(OptionalCost.AltCost) ? 1 : 2]), m, c);
        }

        // Count$wasCastFrom<Zone>.<true>.<false>
        if (sq[0].startsWith("wasCastFrom")) {
            boolean zonesMatch = c.getCastFrom() == ZoneType.smartValueOf(sq[0].substring(11));
            return doXMath(Integer.parseInt(sq[zonesMatch ? 1 : 2]), m, c);
        }

        if (sq[0].startsWith("Devoured")) {
            final String validDevoured = l[0].split(" ")[1];
            CardCollection cl = CardLists.getValidCards(c.getDevouredCards(), validDevoured.split(","), cc, c, null);
            return doXMath(cl.size(), m, c);
        }

        if (sq[0].contains("Party")) {
            CardCollection adventurers = CardLists.getValidCards(cc.getCardsIn(ZoneType.Battlefield),
                    "Creature.Cleric,Creature.Rogue,Creature.Warrior,Creature.Wizard", cc, c, null);

            Set<String> partyTypes = new HashSet<>(Arrays.asList(new String[]{"Cleric", "Rogue", "Warrior", "Wizard"}));
            int partySize = 0;

            HashMap<String, Card> chosenParty = new HashMap<>();
            List<Card> wildcard = Lists.newArrayList();
            HashMap<Card, Set<String>> multityped = new HashMap<>();

            // Figure out how to count each class separately.
            for (Card card : adventurers) {
                Set<String> creatureTypes = card.getType().getCreatureTypes();
                boolean anyType = creatureTypes.contains(CardType.AllCreatureTypes);
                creatureTypes.retainAll(partyTypes);

                if (anyType || creatureTypes.size() == 4) {
                    wildcard.add(card);

                    if (wildcard.size() >= 4) {
                        break;
                    }
                    continue;
                } else if (creatureTypes.size() == 1) {
                    String type = (String)(creatureTypes.toArray()[0]);

                    if (!chosenParty.containsKey(type)) {
                        chosenParty.put(type, card);
                    }
                } else {
                    multityped.put(card, creatureTypes);
                }
            }

            partySize = Math.min(chosenParty.size() + wildcard.size(), 4);

            if (partySize < 4) {
                partyTypes.removeAll(chosenParty.keySet());

                // Here I'm left with just the party types that I haven't selected.
                for(Card multi : multityped.keySet()) {
                    Set<String> types = multityped.get(multi);
                    types.retainAll(partyTypes);

                    for(String type : types) {
                        chosenParty.put(type, multi);
                        partyTypes.remove(type);
                        break;
                    }
                }
            }

            partySize = Math.min(chosenParty.size() + wildcard.size(), 4);

            return doXMath(partySize, m, c);
        }

        if (sq[0].contains("CardPower")) {
            return doXMath(c.getNetPower(), m, c);
        }
        if (sq[0].contains("CardToughness")) {
            return doXMath(c.getNetToughness(), m, c);
        }
        if (sq[0].contains("CardSumPT")) {
            return doXMath((c.getNetPower() + c.getNetToughness()), m, c);
        }

        // Count$SumPower_valid
        if (sq[0].contains("SumPower")) {
            final String[] restrictions = l[0].split("_");
            final String[] rest = restrictions[1].split(",");
            CardCollection filteredCards = CardLists.getValidCards(cc.getGame().getCardsIn(ZoneType.Battlefield), rest, cc, c, null);
            return doXMath(Aggregates.sum(filteredCards, CardPredicates.Accessors.fnGetNetPower), m, c);
        }
        // Count$CardManaCost
        if (sq[0].contains("CardManaCost")) {
            Card ce;
            if (sq[0].contains("Equipped") && c.isEquipping()) {
                ce = c.getEquipping();
            }
            else if (sq[0].contains("Remembered")) {
                ce = (Card) c.getFirstRemembered();
            }
            else {
                ce = c;
            }

            return doXMath(ce == null ? 0 : ce.getCMC(), m, c);
        }
        // Count$SumCMC_valid
        if (sq[0].contains("SumCMC")) {
            ZoneType zone = ZoneType.Battlefield;
            //graveyard support for Inferno Project (may need other zones or multi-zone in future)
            if (sq[0].contains("Graveyard"))
                zone = ZoneType.Graveyard;
            final String[] restrictions = l[0].split("_");
            final String[] rest = restrictions[1].split(",");
            CardCollectionView cardsonbattlefield = game.getCardsIn(zone);
            CardCollection filteredCards = CardLists.getValidCards(cardsonbattlefield, rest, cc, c, null);
            return Aggregates.sum(filteredCards, CardPredicates.Accessors.fnGetCmc);
        }

        if (sq[0].contains("CardNumColors")) {
            return doXMath(CardUtil.getColors(c).countColors(), m, c);
        }
        if (sq[0].contains("CardNumAttacksThisTurn")) {
            return doXMath(c.getDamageHistory().getCreatureAttacksThisTurn(), m, c);
        }
        if (sq[0].contains("ChosenNumber")) {
            Integer i = c.getChosenNumber();
            return doXMath(i == null ? 0 : i, m, c);
        }
        if (sq[0].contains("CardCounters")) {
            // CardCounters.ALL to be used for Kinsbaile Borderguard and anything that cares about all counters
            int count = 0;
            if (sq[1].equals("ALL")) {
                for (Integer i : c.getCounters().values()) {
                    if (i != null && i > 0) {
                        count += i;
                    }
                }
            }
            else {
                count = c.getCounters(CounterType.getType(sq[1]));
            }
            return doXMath(count, m, c);
        }

        // Count$TotalCounters.<counterType>_<valid>
        if (sq[0].contains("TotalCounters")) {
            final String[] restrictions = l[0].split("_");
            final CounterType cType = CounterType.getType(restrictions[1]);
            final String[] validFilter = restrictions[2].split(",");
            CardCollectionView validCards = game.getCardsIn(ZoneType.Battlefield);
            validCards = CardLists.getValidCards(validCards, validFilter, cc, c, null);
            int cCount = 0;
            for (final Card card : validCards) {
                cCount += card.getCounters(cType);
            }
            return doXMath(cCount, m, c);
        }

        if (sq[0].contains("CardControllerTypes")) {
            return doXMath(getCardTypesFromList(cc.getCardsIn(ZoneType.listValueOf(sq[1]))), m, c);
        }

        if (sq[0].contains("CardTypes")) {
            return doXMath(getCardTypesFromList(game.getCardsIn(ZoneType.smartValueOf(sq[1]))), m, c);
        }

        if (sq[0].contains("OppTypesInGrave")) {
            final PlayerCollection opponents = cc.getOpponents();
            CardCollection oppCards = new CardCollection();
            oppCards.addAll(opponents.getCardsIn(ZoneType.Graveyard));
            return doXMath(getCardTypesFromList(oppCards), m, c);
        }

        if (sq[0].contains("BushidoPoint")) {
            return doXMath(c.getKeywordMagnitude(Keyword.BUSHIDO), m, c);
        }
        if (sq[0].contains("TimesKicked")) {
            return doXMath(c.getKickerMagnitude(), m, c);
        }
        if (sq[0].contains("TimesPseudokicked")) {
            return doXMath(c.getPseudoKickerMagnitude(), m, c);
        }
        if (sq[0].contains("TimesMutated")) {
            return doXMath(c.getTimesMutated(), m, c);
        }

        // Count$IfCastInOwnMainPhase.<numMain>.<numNotMain> // 7/10
        if (sq[0].contains("IfCastInOwnMainPhase")) {
            final PhaseHandler cPhase = cc.getGame().getPhaseHandler();
            final boolean isMyMain = cPhase.getPhase().isMain() && cPhase.getPlayerTurn().equals(cc) && c.getCastFrom() != null;
            return doXMath(Integer.parseInt(sq[isMyMain ? 1 : 2]), m, c);
        }

        // Count$ThisTurnEntered <ZoneDestination> [from <ZoneOrigin>] <Valid>
        if (sq[0].contains("ThisTurnEntered")) {
            final String[] workingCopy = l[0].split("_");

            ZoneType destination = ZoneType.smartValueOf(workingCopy[1]);
            final boolean hasFrom = workingCopy[2].equals("from");
            ZoneType origin = hasFrom ? ZoneType.smartValueOf(workingCopy[3]) : null;
            String validFilter = workingCopy[hasFrom ? 4 : 2] ;

            final List<Card> res = CardUtil.getThisTurnEntered(destination, origin, validFilter, c);
            if (origin == null) { // Remove cards on the battlefield that changed controller
                res.removeAll(CardUtil.getThisTurnEntered(destination, destination, validFilter, c));
            }
            return doXMath(res.size(), m, c);
        }

        // Count$LastTurnEntered <ZoneDestination> [from <ZoneOrigin>] <Valid>
        if (sq[0].contains("LastTurnEntered")) {
            final String[] workingCopy = l[0].split("_");

            ZoneType destination = ZoneType.smartValueOf(workingCopy[1]);
            final boolean hasFrom = workingCopy[2].equals("from");
            ZoneType origin = hasFrom ? ZoneType.smartValueOf(workingCopy[3]) : null;
            String validFilter = workingCopy[hasFrom ? 4 : 2] ;

            final List<Card> res = CardUtil.getLastTurnEntered(destination, origin, validFilter, c);
            if (origin == null) { // Remove cards on the battlefield that changed controller
                res.removeAll(CardUtil.getLastTurnEntered(destination, destination, validFilter, c));
            }
            return doXMath(res.size(), m, c);
        }

        // Count$AttackersDeclared
        if (sq[0].contains("AttackersDeclared")) {
            return doXMath(cc.getAttackersDeclaredThisTurn(), m, c);
        }

        // Count$CardAttackedThisTurn_<Valid>
        if (sq[0].contains("CreaturesAttackedThisTurn")) {
            final String[] workingCopy = l[0].split("_");
            final String validFilter = workingCopy[1];
            return doXMath(CardLists.getType(cc.getCreaturesAttackedThisTurn(), validFilter).size(), m, c);
        }

        // Count$ThisTurnCast <Valid>
        // Count$LastTurnCast <Valid>
        if (sq[0].contains("ThisTurnCast") || sq[0].contains("LastTurnCast")) {

            final String[] workingCopy = l[0].split("_");
            final String validFilter = workingCopy[1];

            List<Card> res = Lists.newArrayList();
            if (workingCopy[0].contains("This")) {
                res = CardUtil.getThisTurnCast(validFilter, c);
            }
            else {
                res = CardUtil.getLastTurnCast(validFilter, c);
            }

            final int ret = doXMath(res.size(), m, c);
            return ret;
        }

        // Count$Morbid.<True>.<False>
        if (sq[0].startsWith("Morbid")) {
            final List<Card> res = CardUtil.getThisTurnEntered(ZoneType.Graveyard, ZoneType.Battlefield, "Creature", c);
            return doXMath(Integer.parseInt(sq[res.size() > 0 ? 1 : 2]), m, c);
        }

        // Count$Madness.<True>.<False>
        if (sq[0].startsWith("Madness")) {
            String v = c.isMadness() ? sq[1] : sq[2];
            // TODO move this to AbilityUtils
            return doXMath(StringUtils.isNumeric(v) ? Integer.parseInt(v) : xCount(c, c.getSVar(v)), m, c);
        }

        // Count$Foretold.<True>.<False>
        if (sq[0].startsWith("Foretold")) {
            String v = c.isForetold() ? sq[1] : sq[2];
            // TODO move this to AbilityUtils
            return doXMath(StringUtils.isNumeric(v) ? Integer.parseInt(v) : xCount(c, c.getSVar(v)), m, c);
        }

        // Count$Presence_<Type>.<True>.<False>
        if (sq[0].startsWith("Presence")) {
            final String type = sq[0].split("_")[1];

            if (c.getCastFrom() != null && c.getCastSA() != null) {
                int revealed = AbilityUtils.calculateAmount(c, "Revealed$Valid " + type, c.getCastSA());
                int ctrl = AbilityUtils.calculateAmount(c, "Count$Valid " + type + ".inZoneBattlefield+YouCtrl", c.getCastSA());
                if (revealed + ctrl >= 1) {
                    return doXMath(StringUtils.isNumeric(sq[1]) ? Integer.parseInt(sq[1]) : xCount(c, c.getSVar(sq[1])), m, c);
                }
            }
            return doXMath(StringUtils.isNumeric(sq[2]) ? Integer.parseInt(sq[2]) : xCount(c, c.getSVar(sq[2])), m, c);
        }

        if (sq[0].startsWith("LastStateBattlefield")) {
            final String[] k = l[0].split(" ");
            CardCollection list = new CardCollection(game.getLastStateBattlefield());
            list = CardLists.getValidCards(list, k[1].split(","), cc, c, null);
            return CardFactoryUtil.doXMath(list.size(), m, c);
        }

        if (sq[0].startsWith("LastStateGraveyard")) {
            final String[] k = l[0].split(" ");
            CardCollection list = new CardCollection(game.getLastStateGraveyard());
            list = CardLists.getValidCards(list, k[1].split(","), cc, c, null);
            return CardFactoryUtil.doXMath(list.size(), m, c);
        }

        if (sq[0].equals("YourTurns")) {
            return doXMath(cc.getTurn(), m, c);
        }

        if (sq[0].equals("TotalTurns")) {
            // Sorry for the Singleton use, replace this once this function has game passed into it
            return doXMath(game.getPhaseHandler().getTurn(), m, c);
        }

        if (sq[0].equals("MaxDistinctOnStack")) {
            return game.getStack().getMaxDistinctSources();
        }

        //Count$Random.<Min>.<Max>
        if (sq[0].equals("Random")) {
            int min = StringUtils.isNumeric(sq[1]) ? Integer.parseInt(sq[1]) : xCount(c, c.getSVar(sq[1]));
            int max = StringUtils.isNumeric(sq[2]) ? Integer.parseInt(sq[2]) : xCount(c, c.getSVar(sq[2]));

            return forge.util.MyRandom.getRandom().nextInt(1+max-min) + min;
        }

        // Count$Domain
        if (sq[0].startsWith("Domain")) {
            int n = 0;
            Player neededPlayer = sq[0].equals("DomainActivePlayer") ? activePlayer : cc;
            CardCollection someCards = CardLists.filter(neededPlayer.getCardsIn(ZoneType.Battlefield), Presets.LANDS);
            for (String basic : MagicColor.Constant.BASIC_LANDS) {
                if (!CardLists.getType(someCards, basic).isEmpty()) {
                    n++;
                }
            }
            return doXMath(n, m, c);
        }
        if (sq[0].startsWith("UniqueManaColorsProduced")) {
            boolean untappedOnly = sq[1].contains("ByUntappedSources");
            int uniqueColors = 0;
            CardCollectionView otb = cc.getCardsIn(ZoneType.Battlefield);
            outer: for (byte color : MagicColor.WUBRG) {
                for (Card card : otb) {
                    if (!card.isTapped() || !untappedOnly) {
                        for (SpellAbility ma : card.getManaAbilities()) {
                            if (ma.canProduce(MagicColor.toShortString(color))) {
                                uniqueColors++;
                                continue outer;
                            }
                        }
                    }
                }
            }
            return doXMath(uniqueColors, m, c);
        }
        // Count$Converge
        if (sq[0].contains("Converge")) {
            SpellAbility castSA = c.getCastSA();
            return doXMath(castSA == null ? 0 : castSA.getPayingColors().countColors(), m, c);
        }

        // Count$CardMulticolor.<numMC>.<numNotMC>
        if (sq[0].contains("CardMulticolor")) {
            final boolean isMulti = CardUtil.getColors(c).isMulticolor();
            return doXMath(Integer.parseInt(sq[isMulti ? 1 : 2]), m, c);
        }

        // Complex counting methods
        CardCollectionView someCards = getCardListForXCount(c, cc, sq);

        // 1/10 - Count$MaxCMCYouCtrl
        if (sq[0].contains("MaxCMC")) {
            int mmc = Aggregates.max(someCards, CardPredicates.Accessors.fnGetCmc);
            return doXMath(mmc, m, c);
        }

        return doXMath(someCards.size(), m, c);
    }

    private static CardCollectionView getCardListForXCount(final Card c, final Player cc, final String[] sq) {
        final List<Player> opps = cc.getOpponents();
        CardCollection someCards = new CardCollection();
        final Game game = c.getGame();

        // Generic Zone-based counting
        // Count$QualityAndZones.Subquality

        // build a list of cards in each possible specified zone

        if (sq[0].contains("YouCtrl")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Battlefield));
        }

        if (sq[0].contains("InYourYard")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Graveyard));
        }

        if (sq[0].contains("InYourLibrary")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Library));
        }

        if (sq[0].contains("InYourHand")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Hand));
        }

        if (sq[0].contains("InYourSideboard")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Sideboard));
        }

        if (sq[0].contains("OppCtrl")) {
            for (final Player p : opps) {
                someCards.addAll(p.getZone(ZoneType.Battlefield).getCards());
            }
        }

        if (sq[0].contains("InOppYard")) {
            for (final Player p : opps) {
                someCards.addAll(p.getCardsIn(ZoneType.Graveyard));
            }
        }

        if (sq[0].contains("InOppHand")) {
            for (final Player p : opps) {
                someCards.addAll(p.getCardsIn(ZoneType.Hand));
            }
        }

        if (sq[0].contains("InChosenHand")) {
            if (c.getChosenPlayer() != null) {
                someCards.addAll(c.getChosenPlayer().getCardsIn(ZoneType.Hand));
            }
        }

        if (sq[0].contains("InRememberedHand")) {
            if (c.getRemembered() != null) {
                for (final Object o : c.getRemembered()) {
                    if (o instanceof Player) {
                        Player remPlayer = (Player) o;
                        someCards.addAll(remPlayer.getCardsIn(ZoneType.Hand));
                    }
                }
            }
        }

        if (sq[0].contains("InChosenYard")) {
            if (c.getChosenPlayer() != null) {
                someCards.addAll(c.getChosenPlayer().getCardsIn(ZoneType.Graveyard));
            }
        }

        if (sq[0].contains("OnBattlefield")) {
            someCards.addAll(game.getCardsIn(ZoneType.Battlefield));
        }

        if (sq[0].contains("InAllYards")) {
            someCards.addAll(game.getCardsIn(ZoneType.Graveyard));
        }

        if (sq[0].contains("SpellsOnStack")) {
            someCards.addAll(game.getCardsIn(ZoneType.Stack));
        }

        if (sq[0].contains("InAllHands")) {
            someCards.addAll(game.getCardsIn(ZoneType.Hand));
        }

        //  Count$InTargetedHand (targeted player's cards in hand)
        if (sq[0].contains("InTargetedHand")) {
            for (final SpellAbility sa : c.getCurrentState().getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player tgtP : saTargeting.getTargets().getTargetPlayers()) {
                        someCards.addAll(tgtP.getCardsIn(ZoneType.Hand));
                    }
                }
            }
        }

        //  Count$InTargetedHand (targeted player's cards in hand)
        if (sq[0].contains("InTargetedLibrary")) {
            for (final SpellAbility sa : c.getCurrentState().getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player tgtP : saTargeting.getTargets().getTargetPlayers()) {
                        someCards.addAll(tgtP.getCardsIn(ZoneType.Library));
                    }
                }
            }
        }

        //  Count$InTargetedHand (targeted player's cards in hand)
        if (sq[0].contains("InEnchantedHand")) {
            GameEntity o = c.getEntityAttachedTo();
            Player controller = null;
            if (o instanceof Card) {
                controller = ((Card) o).getController();
            }
            else {
                controller = (Player) o;
            }
            if (controller != null) {
                someCards.addAll(controller.getCardsIn(ZoneType.Hand));
            }
        }
        if (sq[0].contains("InEnchantedYard")) {
            GameEntity o = c.getEntityAttachedTo();
            Player controller = null;
            if (o instanceof Card) {
                controller = ((Card) o).getController();
            }
            else {
                controller = (Player) o;
            }
            if (controller != null) {
                someCards.addAll(controller.getCardsIn(ZoneType.Graveyard));
            }
        }

        // filter lists based on the specified quality

        // "Clerics you control" - Count$TypeYouCtrl.Cleric
        if (sq[0].contains("Type")) {
            someCards = CardLists.filter(someCards, CardPredicates.isType(sq[1]));
        }

        // "Named <CARDNAME> in all graveyards" - Count$NamedAllYards.<CARDNAME>

        if (sq[0].contains("Named")) {
            if (sq[1].equals("CARDNAME")) {
                sq[1] = c.getName();
            }
            someCards = CardLists.filter(someCards, CardPredicates.nameEquals(sq[1]));
        }

        // Refined qualities

        // "Untapped Lands" - Count$UntappedTypeYouCtrl.Land
        // if (sq[0].contains("Untapped")) { someCards = CardLists.filter(someCards, Presets.UNTAPPED); }

        // if (sq[0].contains("Tapped")) { someCards = CardLists.filter(someCards, Presets.TAPPED); }

//        String sq0 = sq[0].toLowerCase();
//        for (String color : MagicColor.Constant.ONLY_COLORS) {
//            if (sq0.contains(color))
//                someCards = someCards.filter(CardListFilter.WHITE);
//        }
        // "White Creatures" - Count$WhiteTypeYouCtrl.Creature
        // if (sq[0].contains("White")) someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.WHITE));
        // if (sq[0].contains("Blue"))  someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.BLUE));
        // if (sq[0].contains("Black")) someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.BLACK));
        // if (sq[0].contains("Red"))   someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.RED));
        // if (sq[0].contains("Green")) someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.GREEN));

        if (sq[0].contains("Multicolor")) {
            someCards = CardLists.filter(someCards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return CardUtil.getColors(c).isMulticolor();
                }
            });
        }

        if (sq[0].contains("Monocolor")) {
            someCards = CardLists.filter(someCards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return CardUtil.getColors(c).isMonoColor();
                }
            });
        }
        return someCards;
    }

    public static int doXMath(final int num, final String operators, final Card c) {
        if (operators == null || operators.equals("none")) {
            return num;
        }

        final String[] s = operators.split("\\.");
        int secondaryNum = 0;

        try {
            if (s.length == 2) {
                secondaryNum = Integer.parseInt(s[1]);
            }
        } catch (final Exception e) {
            secondaryNum = xCount(c, c.getSVar(s[1]));
        }

        if (s[0].contains("Plus")) {
            return num + secondaryNum;
        } else if (s[0].contains("NMinus")) {
            return secondaryNum - num;
        } else if (s[0].contains("Minus")) {
            return num - secondaryNum;
        } else if (s[0].contains("Twice")) {
            return num * 2;
        } else if (s[0].contains("Thrice")) {
            return num * 3;
        } else if (s[0].contains("HalfUp")) {
            return (int) (Math.ceil(num / 2.0));
        } else if (s[0].contains("HalfDown")) {
            return (int) (Math.floor(num / 2.0));
        } else if (s[0].contains("ThirdUp")) {
            return (int) (Math.ceil(num / 3.0));
        } else if (s[0].contains("ThirdDown")) {
            return (int) (Math.floor(num / 3.0));
        } else if (s[0].contains("Negative")) {
            return num * -1;
        } else if (s[0].contains("Times")) {
            return num * secondaryNum;
        } else if (s[0].contains("DivideEvenlyDown")) {
            if (secondaryNum == 0) {
                return 0;
            } else {
                return num / secondaryNum;
            }
        } else if (s[0].contains("Mod")) {
            return num % secondaryNum;
        } else if (s[0].contains("Abs")) {
            return Math.abs(num);
        } else if (s[0].contains("LimitMax")) {
            if (num < secondaryNum) {
                return num;
            } else {
                return secondaryNum;
            }
        } else if (s[0].contains("LimitMin")) {
            if (num > secondaryNum) {
                return num;
            } else {
                return secondaryNum;
            }

        } else {
            return num;
        }
    }

    /**
     * <p>
     * handlePaid.
     * </p>
     *
     * @param paidList
     *            a {@link forge.game.card.CardCollectionView} object.
     * @param string
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int handlePaid(final Iterable<Card> paidList, final String string, final Card source) {
        if (paidList == null) {
            if (string.contains(".")) {
                final String[] splitString = string.split("\\.", 2);
                return doXMath(0, splitString[1], source);
            } else {
                return 0;
            }
        }
        if (string.startsWith("Amount")) {
            int size = Iterables.size(paidList);
            if (string.contains(".")) {
                final String[] splitString = string.split("\\.", 2);
                return doXMath(size, splitString[1], source);
            } else {
                return size;
            }

        }

        if (string.startsWith("DifferentCMC")) {
            final Set<Integer> diffCMC = new HashSet<>();
            for (final Card card : paidList) {
                diffCMC.add(card.getCMC());
            }
            return diffCMC.size();
        }

        if (string.startsWith("SumCMC")) {
            int sumCMC = 0;
            for(Card c : paidList) {
                sumCMC += c.getCMC();
            }
            return sumCMC;
        }

        if (string.startsWith("Valid")) {

            final String[] splitString = string.split("/", 2);
            String valid = splitString[0].substring(6);
            final List<Card> list = CardLists.getValidCardsAsList(paidList, valid, source.getController(), source, null);
            return doXMath(list.size(), splitString.length > 1 ? splitString[1] : null, source);
        }

        String filteredString = string;
        Iterable<Card> filteredList = paidList;
        final String[] filter = filteredString.split("_");

        if (string.startsWith("FilterControlledBy")) {
            final String pString = filter[0].substring(18);
            FCollectionView<Player> controllers = AbilityUtils.getDefinedPlayers(source, pString, null);
            filteredList = CardLists.filterControlledByAsList(filteredList, controllers);
            filteredString = TextUtil.fastReplace(filteredString, pString, "");
            filteredString = TextUtil.fastReplace(filteredString, "FilterControlledBy_", "");
        }

        int tot = 0;
        for (final Card c : filteredList) {
            tot += xCount(c, filteredString);
        }

        return tot;
    }

    /**
     * <p>
     * isMostProminentColor.
     * </p>
     *
     * @param list
     *            a {@link Iterable<Card>} object.
     * @return a boolean.
     */
    public static byte getMostProminentColors(final Iterable<Card> list) {
        int cntColors = MagicColor.WUBRG.length;
        final Integer[] map = new Integer[cntColors];
        for (int i = 0; i < cntColors; i++) {
            map[i] = 0;
        }

        for (final Card crd : list) {
            ColorSet color = CardUtil.getColors(crd);
            for (int i = 0; i < cntColors; i++) {
                if (color.hasAnyColor(MagicColor.WUBRG[i]))
                    map[i]++;
            }
        } // for

        byte mask = 0;
        int nMax = -1;
        for (int i = 0; i < cntColors; i++) {
            if (map[i] > nMax)
                mask = MagicColor.WUBRG[i];
            else if (map[i] == nMax)
                mask |= MagicColor.WUBRG[i];
            else
                continue;
            nMax = map[i];
        }
        return mask;
    }

    /**
     * <p>
     * SortColorsFromList.
     * </p>
     *
     * @param list
     *            a {@link forge.game.card.CardCollection} object.
     * @return a List.
     */
    public static int[] SortColorsFromList(final CardCollection list) {
        int cntColors = MagicColor.WUBRG.length;
        final int[] map = new int[cntColors];
        for (int i = 0; i < cntColors; i++) {
            map[i] = 0;
        }

        for (final Card crd : list) {
            ColorSet color = CardUtil.getColors(crd);
            for (int i = 0; i < cntColors; i++) {
                if (color.hasAnyColor(MagicColor.WUBRG[i]))
                    map[i]++;
            }
        } // for
        Arrays.sort(map);
        return map;
    }

    /**
     * <p>
     * getMostProminentColorsFromList.
     * </p>
     *
     * @param list
     *            a {@link forge.game.card.CardCollectionView} object.
     * @return a boolean.
     */
    public static byte getMostProminentColorsFromList(final CardCollectionView list, final List<String> restrictedToColors) {
        List<Byte> colorRestrictions = Lists.newArrayList();
        for (final String col : restrictedToColors) {
            colorRestrictions.add(MagicColor.fromName(col));
        }
        int cntColors = colorRestrictions.size();
        final Integer[] map = new Integer[cntColors];
        for (int i = 0; i < cntColors; i++) {
            map[i] = 0;
        }

        for (final Card crd : list) {
            ColorSet color = CardUtil.getColors(crd);
            for (int i = 0; i < cntColors; i++) {
                if (color.hasAnyColor(colorRestrictions.get(i))) {
                    map[i]++;
                }
            }
        }

        byte mask = 0;
        int nMax = -1;
        for (int i = 0; i < cntColors; i++) {
            if (map[i] > nMax)
                mask = colorRestrictions.get(i);
            else if (map[i] == nMax)
                mask |= colorRestrictions.get(i);
            else
                continue;
            nMax = map[i];
        }
        return mask;
    }

    /**
     * <p>
     * getMostProminentCreatureType.
     * </p>
     *
     * @param list
     *            a {@link forge.game.card.CardCollection} object.
     * @return an int.
     */
    public static int getMostProminentCreatureTypeSize(final CardCollection list) {
        if (list.isEmpty()) {
            return 0;
        }
        int allCreatureType = 0;

        final Map<String, Integer> map = Maps.newHashMap();
        for (final Card c : list) {
            // Remove Duplicated types
            final Set<String> creatureTypes = c.getType().getCreatureTypes();
            if (creatureTypes.contains(CardType.AllCreatureTypes)) {
                allCreatureType++;
                continue;
            }
            for (String creatureType : creatureTypes) {
                Integer count = map.get(creatureType);
                map.put(creatureType, count == null ? 1 : count + 1);
            }
        }

        int max = 0;
        for (final Entry<String, Integer> entry : map.entrySet()) {
            if (max < entry.getValue()) {
                max = entry.getValue();
            }
        }
        return max + allCreatureType;
    }

    /**
     * <p>
     * sharedKeywords.
     * </p>
     *
     * @param kw
     *            a  String arry.
     * @return a List<String>.
     */
    public static List<String> sharedKeywords(final Iterable<String> kw, final String[] restrictions,
            final Iterable<ZoneType> zones, final Card host) {
        final List<String> filteredkw = Lists.newArrayList();
        final Player p = host.getController();
        CardCollectionView cardlist = p.getGame().getCardsIn(zones);
        final Set<String> landkw = Sets.newHashSet();
        final Set<String> protectionkw = Sets.newHashSet();
        final Set<String> protectionColorkw = Sets.newHashSet();
        final Set<String> hexproofkw = Sets.newHashSet();
        final Set<String> allkw = Sets.newHashSet();

        for (Card c : CardLists.getValidCards(cardlist, restrictions, p, host, null)) {
            for (KeywordInterface inst : c.getKeywords()) {
                final String k = inst.getOriginal();
                if (k.endsWith("walk")) {
                    landkw.add(k);
                } else if (k.startsWith("Protection")) {
                    protectionkw.add(k);
                    for(byte col : MagicColor.WUBRG) {
                        final String colString = "Protection from " + MagicColor.toLongString(col).toLowerCase();
                        if (k.contains(colString)) {
                            protectionColorkw.add(colString);
                        }
                    }
                } else if (k.startsWith("Hexproof")) {
                    hexproofkw.add(k);
                }
                allkw.add(k);
            }
        }
        for (String keyword : kw) {
            if (keyword.equals("Protection")) {
                filteredkw.addAll(protectionkw);
            } else if (keyword.equals("ProtectionColor")) {
                filteredkw.addAll(protectionColorkw);
            } else if (keyword.equals("Landwalk")) {
                filteredkw.addAll(landkw);
            } else if (keyword.equals("Hexproof")) {
                filteredkw.addAll(hexproofkw);
            } else if (allkw.contains(keyword)) {
                filteredkw.add(keyword);
            }
        }
        return filteredkw;
    }

    public static int getCardTypesFromList(final CardCollectionView list) {
        EnumSet<CardType.CoreType> types = EnumSet.noneOf(CardType.CoreType.class);
        for (Card c1 : list) {
            Iterables.addAll(types, c1.getType().getCoreTypes());
        }
        return types.size();
    }

    /**
     * <p>
     * getNeededXDamage.
     * </p>
     *
     * @param ability
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a int.
     */
    public static int getNeededXDamage(final SpellAbility ability) {
        // when targeting a creature, make sure the AI won't overkill on X
        // damage
        final Card target = ability.getTargetCard();
        int neededDamage = -1;

        if ((target != null)) {
            neededDamage = target.getNetToughness() - target.getDamage();
        }

        return neededDamage;
    }

    /**
     * Adds the ability factory abilities.
     *
     * @param card
     *            the card
     */
    public static final void addAbilityFactoryAbilities(final Card card, final Iterable<String> abilities) {
        // **************************************************
        // AbilityFactory cards
        for (String rawAbility : abilities) {
            try {
                final SpellAbility intrinsicAbility = AbilityFactory.getAbility(rawAbility, card);
                card.addSpellAbility(intrinsicAbility);
                intrinsicAbility.setIntrinsic(true);
                intrinsicAbility.setCardState(card.getCurrentState());
            } catch (Exception e) {
                String msg = "CardFactoryUtil:addAbilityFactoryAbilities: crash in raw Ability";
                Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(msg)
                    .withData("Card", card.getName()).withData("Ability", rawAbility).build()
                );

                // rethrow the exception with card Name for the user
                throw new RuntimeException("crash in raw Ability, check card script of " + card.getName(), e);
            }
        }
    }

    /**
     * <p>
     * postFactoryKeywords.
     * </p>
     *
     * @param card
     *            a {@link forge.game.card.Card} object.
     */
    public static void setupKeywordedAbilities(final Card card) {
        // this function should handle any keywords that need to be added after
        // a spell goes through the factory
        // Cards with Cycling abilities
        // -1 means keyword "Cycling" not found

        for (KeywordInterface inst : card.getKeywords()) {
            inst.createTraits(card, true);
        }

        // AltCost
        String altCost = card.getSVar("AltCost");
        if (StringUtils.isNotBlank(altCost)) {
            final SpellAbility sa1 = card.getFirstSpellAbility();
            if (sa1 != null && sa1.isSpell()) {
                card.addSpellAbility(makeAltCostAbility(card, altCost, sa1));
            }
        }
    }

    private static ReplacementEffect createETBReplacement(final CardState card, ReplacementLayer layer,
            final String effect, final boolean optional, final boolean secondary,
            final boolean intrinsic, final String valid, final String zone) {
        SpellAbility repAb = AbilityFactory.getAbility(effect, card);
        return createETBReplacement(card, layer, repAb, optional, secondary, intrinsic, valid, zone);
    }

    private static ReplacementEffect createETBReplacement(final CardState card, ReplacementLayer layer,
            final SpellAbility repAb, final boolean optional, final boolean secondary,
            final boolean intrinsic, final String valid, final String zone) {
        Card host = card.getCard();
        String desc = repAb.getDescription();
        setupETBReplacementAbility(repAb);
        if (!intrinsic) {
            repAb.setIntrinsic(false);
        }

        StringBuilder repEffsb = new StringBuilder();
        repEffsb.append("Event$ Moved | ValidCard$ ").append(valid);
        repEffsb.append(" | Destination$ Battlefield | Description$ ").append(desc);
        if (optional) {
            repEffsb.append(" | Optional$ True");
        }
        if (secondary) {
            repEffsb.append(" | Secondary$ True");
        }

        if (!zone.isEmpty()) {
            repEffsb.append(" | ActiveZones$ ").append(zone);
        }

        ReplacementEffect re = ReplacementHandler.parseReplacement(repEffsb.toString(), host, intrinsic, card);
        re.setLayer(layer);
        re.setOverridingAbility(repAb);

        return re;
    }

    public static ReplacementEffect makeEtbCounter(final String kw, final CardState card, final boolean intrinsic)
    {
        String parse = kw;

        String[] splitkw = parse.split(":");

        String desc = "CARDNAME enters the battlefield with ";
        desc += Lang.nounWithNumeral(splitkw[2], CounterType.getType(splitkw[1]).getName() + " counter");
        desc += " on it.";

        String extraparams = "";
        String amount = splitkw[2];
        if (splitkw.length > 3) {
            if (!splitkw[3].equals("no Condition")) {
                extraparams = splitkw[3];
            }
        }
        if (splitkw.length > 4) {
            desc = !splitkw[4].equals("no desc") ? splitkw[4] : "";
        }
        String abStr = "DB$ PutCounter | Defined$ Self | CounterType$ " + splitkw[1]
                + " | ETB$ True | CounterNum$ " + amount;

        SpellAbility sa = AbilityFactory.getAbility(abStr, card);
        setupETBReplacementAbility(sa);
        if (!intrinsic) {
            sa.setIntrinsic(false);
        }

        String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                + "| Secondary$ True | Description$ " + desc + (!extraparams.equals("") ? " | " + extraparams : "");

        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card.getCard(), intrinsic, card);

        re.setOverridingAbility(sa);

        return re;
    }

    public static void addTriggerAbility(final KeywordInterface inst, final Card card, final boolean intrinsic) {
        String keyword = inst.getOriginal();

        if (keyword.startsWith("Afflict")) {
            final String[] k = keyword.split(":");
            final String n = k[1];

            final String trigStr = "Mode$ AttackerBlocked | ValidCard$ Card.Self | TriggerZones$ Battlefield " +
                    " | ValidBlocker$ Creature | Secondary$ True " +
                    " | TriggerDescription$ Afflict " + n + " (" + inst.getReminderText() + ")";

            final String abStringAfflict = "DB$ LoseLife | Defined$ TriggeredDefendingPlayer" +
                    " | LifeAmount$ " + n;

            final Trigger afflictTrigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic, null);
            afflictTrigger.setOverridingAbility(AbilityFactory.getAbility(abStringAfflict, card));

            inst.addTrigger(afflictTrigger);
        } else if (keyword.startsWith("Afterlife")) {
            final String[] k = keyword.split(":");
            final String name = StringUtils.join(k, " ");

            final StringBuilder sb = new StringBuilder();
            sb.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self ");
            sb.append("| Secondary$ True | TriggerDescription$ ").append(name);
            sb.append(" (").append(inst.getReminderText()).append(")");
            final String effect = "DB$ Token | TokenAmount$ " + k[1] +  " | TokenScript$ wb_1_1_spirit_flying";

            final Trigger trigger = TriggerHandler.parseTrigger(sb.toString(), card, intrinsic);

            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));
            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Annihilator")) {
            final String[] k = keyword.split(":");
            final String n = k[1];

            final String trig = "Mode$ Attacks | ValidCard$ Card.Self | "
                            + "TriggerZones$ Battlefield | Secondary$ True | TriggerDescription$ "
                            + "Annihilator " + n + " (" + inst.getReminderText() + ")";

            final String effect = "DB$ Sacrifice | Defined$ DefendingPlayer | SacValid$ Permanent | Amount$ " + k[1];

            final Trigger trigger = TriggerHandler.parseTrigger(trig, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Ascend")) {
            // Ascend trigger only for Permanent
            if (card.isPermanent()) {
                final String trig = "Mode$ Always | TriggerZones$ Battlefield | Secondary$ True"
                        + " | Static$ True | Blessing$ False | IsPresent$ Permanent.YouCtrl | PresentCompare$ GE10 "
                        + " | TriggerDescription$ Ascend (" + inst.getReminderText() + ")";

                final String effect = "DB$ Ascend | Defined$ You";

                final Trigger trigger = TriggerHandler.parseTrigger(trig, card, intrinsic);
                trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

                inst.addTrigger(trigger);
            } else {
                SpellAbility sa = card.getFirstSpellAbility();
                if (sa != null && sa.isSpell()) {
                    sa.setBlessing(true);
                }
            }
        } else if (keyword.equals("Battle cry")) {
            final String trig = "Mode$ Attacks | ValidCard$ Card.Self | TriggerZones$ Battlefield | Secondary$ True "
                    + " | TriggerDescription$ " + keyword + " (" + inst.getReminderText() + ")";
            String pumpStr = "DB$ PumpAll | ValidCards$ Creature.attacking+Other | NumAtt$ 1";
            SpellAbility sa = AbilityFactory.getAbility(pumpStr, card);

            sa.setIntrinsic(intrinsic);

            final Trigger trigger = TriggerHandler.parseTrigger(trig, card, intrinsic);
            trigger.setOverridingAbility(sa);

            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Bushido")) {
            final String[] k = keyword.split(":");
            final String n = k[1];

            final String trigBlock = "Mode$ Blocks | ValidCard$ Card.Self | Secondary$ True"
                    + " | TriggerDescription$ Bushido "+ n + " (" + inst.getReminderText() + ")";

            final String trigBlocked = "Mode$ AttackerBlocked | ValidCard$ Card.Self | Secondary$ True "
                    + " | TriggerDescription$ Bushido "+ n + " (" + inst.getReminderText() + ")";

            String pumpStr = "DB$ Pump | Defined$ Self | NumAtt$ " + n + " | NumDef$ " + n;

            SpellAbility pump = AbilityFactory.getAbility(pumpStr, card);
            if ("X".equals(n)) {
                pump.setSVar("X", "Count$Valid Creature.attacking");
            }

            final Trigger bushidoTrigger1 = TriggerHandler.parseTrigger(trigBlock, card, intrinsic);
            final Trigger bushidoTrigger2 = TriggerHandler.parseTrigger(trigBlocked, card, intrinsic);

            bushidoTrigger1.setOverridingAbility(pump);
            bushidoTrigger2.setOverridingAbility(pump);

            inst.addTrigger(bushidoTrigger1);
            inst.addTrigger(bushidoTrigger2);
        } else if (keyword.equals("Cascade")) {
            final StringBuilder trigScript = new StringBuilder("Mode$ SpellCast | ValidCard$ Card.Self" +
                    " | Secondary$ True | TriggerDescription$ Cascade - CARDNAME");

            final String abString = "DB$ DigUntil | Defined$ You | Amount$ 1 | Valid$ Card.nonLand+cmcLTCascadeX" +
                    " | FoundDestination$ Exile | RevealedDestination$ Exile | ImprintFound$ True" +
                    " | RememberRevealed$ True";
            SpellAbility dig = AbilityFactory.getAbility(abString, card);
            dig.setSVar("CascadeX", "Count$CardManaCost");

            final String dbLandPut = "DB$ ChangeZone | ConditionCheckSVar$ X | ConditionSVarCompare$ GE1" +
                    " | Hidden$ True | Origin$ Exile | Destination$ Battlefield | ChangeType$ Land.IsRemembered" +
                    " | ChangeNum$ X | Tapped$ True | ForgetChanged$ True" +
                    " | SelectPrompt$ You may select a land to put on the battlefield tapped";
            AbilitySub landPut = (AbilitySub)AbilityFactory.getAbility(dbLandPut, card);
            landPut.setSVar("X", "Count$Averna");
            dig.setSubAbility(landPut);

            final String dbCascadeCast = "DB$ Play | Defined$ Imprinted | WithoutManaCost$ True | Optional$ True | ValidSA$ Spell.cmcLTCascadeX";
            AbilitySub cascadeCast = (AbilitySub)AbilityFactory.getAbility(dbCascadeCast, card);
            cascadeCast.setSVar("CascadeX", "Count$CardManaCost");
            landPut.setSubAbility(cascadeCast);

            final String dbMoveToLib = "DB$ ChangeZoneAll | ChangeType$ Card.IsRemembered,Card.IsImprinted"
                    + " | Origin$ Exile | Destination$ Library | RandomOrder$ True | LibraryPosition$ -1";
            AbilitySub moveToLib = (AbilitySub)AbilityFactory.getAbility(dbMoveToLib, card);
            cascadeCast.setSubAbility(moveToLib);

            final String cascadeCleanup = "DB$ Cleanup | ClearRemembered$ True | ClearImprinted$ True";
            AbilitySub cleanup = (AbilitySub)AbilityFactory.getAbility(cascadeCleanup, card);
            moveToLib.setSubAbility(cleanup);

            dig.setIntrinsic(intrinsic);

            final Trigger cascadeTrigger = TriggerHandler.parseTrigger(trigScript.toString(), card, intrinsic);
            cascadeTrigger.setOverridingAbility(dig);

            inst.addTrigger(cascadeTrigger);
        } else if (keyword.startsWith("Champion")){

            final String[] k = keyword.split(":");
            final String[] valid = k[1].split(",");
            String desc = Lang.joinHomogenous(Lists.newArrayList(valid), null, "or");
            String article = Lang.startsWithVowel(desc) ? "an" : "a";
            if (desc.equals("Creature")) {
                desc = "creature"; //use lowercase for "Champion a creature"
            }

            StringBuilder changeType = new StringBuilder();
            for (String v : valid) {
                if (changeType.length() != 0) {
                    changeType.append(",");
                }
                changeType.append(v).append(".YouCtrl+Other");
            }

            StringBuilder trig = new StringBuilder();
            trig.append("Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self");
            trig.append(" | TriggerDescription$ Champion ").append(article).append(" ").append(desc);
            trig.append(" (").append(Keyword.getInstance("Champion:"+desc).getReminderText()) .append(")");

            StringBuilder trigReturn = new StringBuilder();
            trigReturn.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Any | ValidCard$ Card.Self");
            trigReturn.append(" | Secondary$ True | TriggerDescription$ When this leaves the battlefield, that card returns to the battlefield.");

            StringBuilder ab = new StringBuilder();
            ab.append("DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile | RememberChanged$ True ");
            ab.append(" | Champion$ True | Hidden$ True | Optional$ True | ChangeType$ ").append(changeType);

            StringBuilder subAb = new StringBuilder();
            subAb.append("DB$ Sacrifice | Defined$ Card.Self");
            subAb.append(" | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ EQ0");

            String returnChampion = "DB$ ChangeZone | Defined$ Remembered | Origin$ Exile | Destination$ Battlefield";
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig.toString(), card, intrinsic);
            final Trigger parsedTrigReturn = TriggerHandler.parseTrigger(trigReturn.toString(), card, intrinsic);

            SpellAbility sa = AbilityFactory.getAbility(ab.toString(), card);
            AbilitySub sub = (AbilitySub) AbilityFactory.getAbility(subAb.toString(), card);

            sa.setSubAbility(sub);
            sa.setIntrinsic(intrinsic);

            parsedTrigger.setOverridingAbility(sa);

            SpellAbility saReturn = AbilityFactory.getAbility(returnChampion, card);
            sub = (AbilitySub) AbilityFactory.getAbility("DB$ Cleanup | ClearRemembered$ True", card);
            saReturn.setSubAbility(sub);
            saReturn.setIntrinsic(intrinsic);

            parsedTrigReturn.setOverridingAbility(saReturn);

            inst.addTrigger(parsedTrigger);
            inst.addTrigger(parsedTrigReturn);
        } else if (keyword.equals("Conspire")) {
            final String trigScript = "Mode$ SpellCast | ValidCard$ Card.Self | CheckSVar$ Conspire | Secondary$ True | TriggerDescription$ Copy CARDNAME if its conspire cost was paid";
            final String abString = "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | Amount$ 1 | MayChooseTarget$ True";

            final Trigger conspireTrigger = TriggerHandler.parseTrigger(trigScript, card, intrinsic);
            conspireTrigger.setOverridingAbility(AbilityFactory.getAbility(abString, card));
            conspireTrigger.setSVar("Conspire", "0");
            inst.addTrigger(conspireTrigger);
        } else if (keyword.startsWith("Cumulative upkeep")) {
            final String[] k = keyword.split(":");
            final Cost cost = new Cost(k[1], false);
            String costDesc = cost.toSimpleString();

            if (!cost.isOnlyManaCost()) {
                costDesc = "—" + costDesc;
            }

            String upkeepTrig = "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield " +
                    " | IsPresent$ Card.Self | Secondary$ True | TriggerDescription$ " + k[0] + " " +
                    costDesc + " (" + inst.getReminderText() + ")";

            String effect = "DB$ Sacrifice | SacValid$ Self | CumulativeUpkeep$ " + k[1];

            final Trigger trigger = TriggerHandler.parseTrigger(upkeepTrig, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Demonstrate")) {
            final String trigScript = "Mode$ SpellCast | ValidCard$ Card.Self | TriggerDescription$ Demonstrate (" + inst.getReminderText() + ")";
            final String youCopyStr = "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True | Optional$ True | RememberCopies$ True";
            final String chooseOppStr = "DB$ ChoosePlayer | Defined$ You | Choices$ Player.Opponent | ConditionDefined$ Remembered | ConditionPresent$ Spell";
            final String oppCopyStr = "DB$ CopySpellAbility | Controller$ ChosenPlayer | Defined$ TriggeredSpellAbility | MayChooseTarget$ True | ConditionDefined$ Remembered | ConditionPresent$ Spell";
            final String cleanupStr = "DB$ Cleanup | ClearRemembered$ True | ClearChosenPlayer$ True";

            final Trigger trigger = TriggerHandler.parseTrigger(trigScript, card, intrinsic);
            final SpellAbility youCopy = AbilityFactory.getAbility(youCopyStr, card);
            final AbilitySub chooseOpp = (AbilitySub) AbilityFactory.getAbility(chooseOppStr, card);
            final AbilitySub oppCopy = (AbilitySub) AbilityFactory.getAbility(oppCopyStr, card);
            final AbilitySub cleanup = (AbilitySub) AbilityFactory.getAbility(cleanupStr, card);
            oppCopy.setSubAbility(cleanup);
            chooseOpp.setSubAbility(oppCopy);
            youCopy.setSubAbility(chooseOpp);
            trigger.setOverridingAbility(youCopy);

            inst.addTrigger(trigger);
        } else if (keyword.equals("Dethrone")) {
            final StringBuilder trigScript = new StringBuilder(
                    "Mode$ Attacks | ValidCard$ Card.Self | Attacked$ Player.withMostLife | Secondary$ True | "
                    + "TriggerZones$ Battlefield | TriggerDescription$"
                    + " Dethrone (" + inst.getReminderText() + ")");

            final String abString = "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1";
            final Trigger dethroneTrigger = TriggerHandler.parseTrigger(trigScript.toString(), card, intrinsic);
            dethroneTrigger.setOverridingAbility(AbilityFactory.getAbility(abString, card));

            inst.addTrigger(dethroneTrigger);
        } else if (keyword.startsWith("Echo")) {
            final String[] k = keyword.split(":");
            final String cost = k[1];

            String upkeepTrig = "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield " +
                    " | IsPresent$ Card.Self+cameUnderControlSinceLastUpkeep | Secondary$ True | " +
                    "TriggerDescription$ " + inst.getReminderText();

            String effect = "DB$ Sacrifice | SacValid$ Self | "
                    + "Echo$ " + cost;

            final Trigger trigger = TriggerHandler.parseTrigger(upkeepTrig, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Evoke")) {
            final StringBuilder trigStr = new StringBuilder(
                    "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self+evoked | Secondary$ True | TriggerDescription$ "
                            + "Evoke (" + inst.getReminderText() + ")");

            final String effect = "DB$ Sacrifice";
            final Trigger trigger = TriggerHandler.parseTrigger(trigStr.toString(), card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Evolve")) {
            final String trigStr = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + " ValidCard$ Creature.YouCtrl+Other | EvolveCondition$ True | "
                    + "TriggerZones$ Battlefield | Secondary$ True | "
                    + "TriggerDescription$ Evolve (" + inst.getReminderText()+ ")";
            final String effect = "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | "
                    + "CounterNum$ 1 | Evolve$ True";

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Exalted")) {
            final String trig = "Mode$ Attacks | ValidCard$ Creature.YouCtrl | Alone$ True | "
                    + "TriggerZones$ Battlefield | Secondary$ True | TriggerDescription$ "
                    + "Exalted (" + inst.getReminderText() + ")";

            final String effect = "DB$ Pump | Defined$ TriggeredAttackerLKICopy | NumAtt$ +1 | NumDef$ +1";
            final Trigger trigger = TriggerHandler.parseTrigger(trig, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Exploit")) {
            final String trigStr = "Mode$ ChangesZone | ValidCard$ Card.Self | Origin$ Any | Destination$ Battlefield | Secondary$ True"
                    + " | TriggerDescription$ Exploit (" + inst.getReminderText() + ")";
            final String effect = "DB$ Sacrifice | SacValid$ Creature | Exploit$ True | Optional$ True";

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Extort")) {
            final String extortTrigger = "Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ You | "
                    + "TriggerZones$ Battlefield | Secondary$ True"
                    + " | TriggerDescription$ Extort ("+ inst.getReminderText() +")";

            final String loseLifeStr = "AB$ LoseLife | Cost$ WB | Defined$ Player.Opponent | LifeAmount$ 1";
            final String gainLifeStr = "DB$ GainLife | Defined$ You | LifeAmount$ AFLifeLost";

            SpellAbility loseLifeSA = AbilityFactory.getAbility(loseLifeStr, card);

            AbilitySub gainLifeSA = (AbilitySub) AbilityFactory.getAbility(gainLifeStr, card);
            gainLifeSA.setSVar("AFLifeLost", "Number$0");
            loseLifeSA.setSubAbility(gainLifeSA);
            loseLifeSA.setIntrinsic(intrinsic);

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(extortTrigger, card, intrinsic);
            parsedTrigger.setOverridingAbility(loseLifeSA);
            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("Fabricate")) {
            final String[] k = keyword.split(":");
            final String n = k[1];

            final String name = StringUtils.join(k);

            final String trigStr = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield "
                    + " | ValidCard$ Card.Self | Secondary$ True"
                    + " | TriggerDescription$ Fabricate " + n + " (" + inst.getReminderText() + ")";

            final String choose = "DB$ GenericChoice | AILogic$ " + name;
            final String counter = "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ " + n +
                    " | IsPresent$ Card.StrictlySelf | SpellDescription$ Put "
                    + Lang.nounWithNumeral(n, "+1/+1 counter") + " on it.";
            final String token = "DB$ Token | TokenAmount$ " + n + " | TokenScript$ c_1_1_a_servo | TokenOwner$ You "
                    + " | LegacyImage$ c 1 1 a servo aer | SpellDescription$ Create "
                    + Lang.nounWithNumeral(n, "1/1 colorless Servo artifact creature token") + ".";

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            SpellAbility saChoose = AbilityFactory.getAbility(choose, card);

            List<AbilitySub> list = Lists.newArrayList();
            list.add((AbilitySub)AbilityFactory.getAbility(counter, card));
            list.add((AbilitySub)AbilityFactory.getAbility(token, card));
            saChoose.setAdditionalAbilityList("Choices", list);
            saChoose.setIntrinsic(intrinsic);

            trigger.setOverridingAbility(saChoose);

            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Fading")) {
            String upkeepTrig = "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield | Secondary$ True " +
                    " | TriggerDescription$ At the beginning of your upkeep, remove a fade counter from CARDNAME. If you can't, sacrifice CARDNAME.";

            final String removeCounterStr = "DB$ RemoveCounter | Defined$ Self | CounterType$ FADE | CounterNum$ 1 | RememberRemoved$ True";
            final String sacrificeStr = "DB$ Sacrifice | SacValid$ Self | ConditionCheckSVar$ FadingCheckSVar | ConditionSVarCompare$ EQ0";
            final String cleanupStr = "DB$ Cleanup | ClearRemembered$ True";

            SpellAbility removeCounterSA = AbilityFactory.getAbility(removeCounterStr, card);
            AbilitySub sacrificeSA = (AbilitySub) AbilityFactory.getAbility(sacrificeStr, card);
            sacrificeSA.setSVar("FadingCheckSVar","Count$RememberedSize");
            removeCounterSA.setSubAbility(sacrificeSA);

            AbilitySub cleanupSA = (AbilitySub) AbilityFactory.getAbility(cleanupStr, card);
            sacrificeSA.setSubAbility(cleanupSA);

            final Trigger trigger = TriggerHandler.parseTrigger(upkeepTrig, card, intrinsic);
            removeCounterSA.setIntrinsic(intrinsic);
            trigger.setOverridingAbility(removeCounterSA);

            inst.addTrigger(trigger);
        } else if (keyword.equals("Flanking")) {
            final StringBuilder trigFlanking = new StringBuilder(
                    "Mode$ AttackerBlockedByCreature | ValidCard$ Card.Self | ValidBlocker$ Creature.withoutFlanking " +
                    " | TriggerZones$ Battlefield | Secondary$ True " +
                    " | TriggerDescription$ Flanking (" + inst.getReminderText() + ")");

            final String effect = "DB$ Pump | Defined$ TriggeredBlockerLKICopy | NumAtt$ -1 | NumDef$ -1";

            final Trigger trigger = TriggerHandler.parseTrigger(trigFlanking.toString(), card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Graft")) {
            final StringBuilder sb = new StringBuilder();
            sb.append("DB$ MoveCounter | Source$ Self | Defined$ TriggeredCardLKICopy");
            sb.append(" | CounterType$ P1P1 | CounterNum$ 1");

            if (card.hasSVar("AIGraftPreference")) {
                sb.append(" | AILogic$ ").append(card.getSVar("AIGraftPreference"));
            }

            String trigStr = "Mode$ ChangesZone | ValidCard$ Creature.Other"
                + "| Origin$ Any | Destination$ Battlefield "
                + "| TriggerZones$ Battlefield | OptionalDecider$ You "
                + "| IsPresent$ Card.Self+counters_GE1_P1P1"
                + "| Secondary$ True | TriggerDescription$ "
                + "Whenever another creature enters the battlefield, you "
                + "may move a +1/+1 counter from this creature onto it.";
            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            trigger.setOverridingAbility(AbilityFactory.getAbility(sb.toString(), card));

            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Gravestorm")) {
            String trigStr = "Mode$ SpellCast | ValidCard$ Card.Self | TriggerDescription$ Gravestorm (" + inst.getReminderText() + ")";
            String copyStr = "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | Amount$ GravestormCount | MayChooseTarget$ True";

            SpellAbility copySa = AbilityFactory.getAbility(copyStr, card);
            copySa.setSVar("GravestormCount", "Count$ThisTurnEntered_Graveyard_from_Battlefield_Permanent");

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            trigger.setOverridingAbility(copySa);

            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Haunt")) {
            final String[] k = keyword.split(":");
            final String hauntSVarName = k[1];

            List<Trigger> triggers = Lists.newArrayList();

            final StringBuilder sb = new StringBuilder();
            if (card.isCreature()) {
                sb.append("When ").append(card.getName());
                sb.append(" enters the battlefield or the creature it haunts dies, ");
            } else {
                sb.append("When the creature ").append(card.getName());
                sb.append(" haunts dies, ");
            }

            // use new feature to get the Ability
            sb.append("ABILITY");

            final String hauntDescription = sb.toString();

            // Second, create the trigger that runs when the haunted creature dies
            final StringBuilder sbDies = new StringBuilder();
            sbDies.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | TriggerZones$ Exile |");
            sbDies.append("ValidCard$ Creature.HauntedBy | Execute$ ").append(hauntSVarName);
            sbDies.append(" | TriggerDescription$ ").append(hauntDescription);

            final Trigger hauntedDies = TriggerHandler.parseTrigger(sbDies.toString(), card, intrinsic);

            // Fourth, create a trigger that removes the haunting status if the
            // haunter leaves the exile
            final StringBuilder sbUnExiled = new StringBuilder();
            sbUnExiled.append("Mode$ ChangesZone | Origin$ Exile | Destination$ Any | ");
            sbUnExiled.append("ValidCard$ Card.Self | Static$ True | Secondary$ True | ");
            sbUnExiled.append("TriggerDescription$ Blank");

            final Trigger haunterUnExiled = TriggerHandler.parseTrigger(sbUnExiled.toString(), card,
                    intrinsic);

            final SpellAbility unhaunt = AbilityFactory.getAbility("DB$ Haunt", card);

            haunterUnExiled.setOverridingAbility(unhaunt);

            triggers.add(haunterUnExiled);

            // Trigger for when the haunted creature leaves the battlefield
            final StringBuilder sbHauntRemoved = new StringBuilder();
            sbHauntRemoved.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Any | ");
            sbHauntRemoved.append("ValidCard$ Creature.HauntedBy | Static$ True | Secondary$ True | ");
            sbHauntRemoved.append("TriggerDescription$ Blank");

            final Trigger trigHauntRemoved = TriggerHandler.parseTrigger(sbHauntRemoved.toString(), card,
                    intrinsic);
            trigHauntRemoved.setOverridingAbility(unhaunt);

            triggers.add(trigHauntRemoved);

            // Fifth, add all triggers and abilities to the card.
            if (card.isCreature()) {
                // Third, create the trigger that runs when the haunting creature
                // enters the battlefield
                final StringBuilder sbETB = new StringBuilder();
                sbETB.append("Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Execute$ ");
                sbETB.append(hauntSVarName).append(" | Secondary$ True | TriggerDescription$ ");
                sbETB.append(hauntDescription);

                final Trigger haunterETB = TriggerHandler.parseTrigger(sbETB.toString(), card, intrinsic);

                triggers.add(haunterETB);
            }

            // First, create trigger that runs when the haunter goes to the
            // graveyard
            final StringBuilder sbHaunter = new StringBuilder();
            sbHaunter.append("Mode$ ChangesZone | Origin$ ");
            sbHaunter.append(card.isCreature() ? "Battlefield" : "Stack");
            sbHaunter.append(" | Destination$ Graveyard | ValidCard$ Card.Self");
            sbHaunter.append(" | Static$ True | Secondary$ True | TriggerDescription$ Blank");

            final Trigger haunterDies = TriggerHandler.parseTrigger(sbHaunter.toString(), card, intrinsic);

            final String hauntDiesEffectStr = "DB$ Haunt | ValidTgts$ Creature | TgtPrompt$ Choose target creature to haunt";
            final SpellAbility hauntDiesAbility = AbilityFactory.getAbility(hauntDiesEffectStr, card);

            haunterDies.setOverridingAbility(hauntDiesAbility);

            triggers.add(haunterDies);

            triggers.add(hauntedDies);

            for (final Trigger trigger : triggers) {
                inst.addTrigger(trigger);
            }
        } else if (keyword.equals("Hideaway")) {
            // The exiled card gains ‘Any player who has controlled the permanent that exiled this card may look at this card in the exile zone.’
            // this is currently not possible because the StaticAbility currently has no information about the OriginalHost

            List<Trigger> triggers = Lists.newArrayList();
            StringBuilder sb = new StringBuilder();
            sb.append("Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Secondary$ True ");
            sb.append("| TriggerDescription$ When CARDNAME enters the battlefield, ");
            sb.append("look at the top four cards of your library, exile one face down");
            sb.append(", then put the rest on the bottom of your library.");
            final Trigger hideawayTrigger = TriggerHandler.parseTrigger(sb.toString(), card, intrinsic);

            String hideawayDig = "DB$ Dig | Defined$ You | DigNum$ 4 | DestinationZone$ Exile | ExileFaceDown$ True | RememberChanged$ True";
            String hideawayEffect = "DB$ Effect | StaticAbilities$ STHideawayEffectLookAtCard | ForgetOnMoved$ Exile | RememberObjects$ Remembered | Duration$ Permanent";

            String lookAtCard = "Mode$ Continuous | Affected$ Card.IsRemembered | MayLookAt$ True | EffectZone$ Command | AffectedZone$ Exile | Description$ You may look at the exiled card.";

            SpellAbility digSA = AbilityFactory.getAbility(hideawayDig, card);

            AbilitySub effectSA = (AbilitySub) AbilityFactory.getAbility(hideawayEffect, card);
            effectSA.setSVar("STHideawayEffectLookAtCard", lookAtCard);

            digSA.setSubAbility((AbilitySub)effectSA.copy());

            hideawayTrigger.setOverridingAbility(digSA);

            triggers.add(hideawayTrigger);

            final Trigger gainControlTrigger = TriggerHandler.parseTrigger("Mode$ ChangesController | ValidCard$ Card.Self | Static$ True", card, intrinsic);
            gainControlTrigger.setOverridingAbility((AbilitySub)effectSA.copy());
            triggers.add(gainControlTrigger);

            // when the card with hideaway leaves the battlefield, forget all exiled cards
            final Trigger changeZoneTrigger = TriggerHandler.parseTrigger("Mode$ ChangesZone | ValidCard$ Card.Self | Origin$ Battlefield | Destination$ Any | TriggerZone$ Battlefield | Static$ True", card, intrinsic);
            String cleanupStr = "DB$ Cleanup | ClearRemembered$ True";
            changeZoneTrigger.setOverridingAbility(AbilityFactory.getAbility(cleanupStr, card));
            triggers.add(changeZoneTrigger);

            for (final Trigger trigger : triggers) {
                inst.addTrigger(trigger);
            }
        } else if (keyword.equals("Ingest")) {
            final String trigStr = "Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player | CombatDamage$ True"
                    + "| Secondary$ True | TriggerZones$ Battlefield | TriggerDescription$ Ingest ("
                    + inst.getReminderText() + ")";

            final String abStr = "DB$ Dig | DigNum$ 1 | ChangeNum$ All | DestinationZone$ Exile | Defined$ TriggeredTarget";

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            trigger.setOverridingAbility(AbilityFactory.getAbility(abStr, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Living Weapon")) {
            final StringBuilder sbTrig = new StringBuilder();
            sbTrig.append("Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ");
            sbTrig.append("ValidCard$ Card.Self | Secondary$ True | TriggerDescription$ ");
            sbTrig.append("Living Weapon (").append(inst.getReminderText()).append(")");

            final StringBuilder sbGerm = new StringBuilder();
            sbGerm.append("DB$ Token | TokenAmount$ 1 | TokenScript$ b_0_0_germ |TokenOwner$ You | RememberTokens$ True");

            final SpellAbility saGerm = AbilityFactory.getAbility(sbGerm.toString(), card);

            final String sbAttach = "DB$ Attach | Defined$ Remembered";
            final AbilitySub saAttach = (AbilitySub) AbilityFactory.getAbility(sbAttach, card);
            saGerm.setSubAbility(saAttach);

            final String sbClear = "DB$ Cleanup | ClearRemembered$ True";
            final AbilitySub saClear = (AbilitySub) AbilityFactory.getAbility(sbClear, card);
            saAttach.setSubAbility(saClear);

            final Trigger etbTrigger = TriggerHandler.parseTrigger(sbTrig.toString(), card, intrinsic);

            etbTrigger.setOverridingAbility(saGerm);

            saGerm.setIntrinsic(intrinsic);
            inst.addTrigger(etbTrigger);
        } else if (keyword.startsWith("Madness")) {
            // Set Madness Triggers
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            final String trigStr = "Mode$ Discarded | ValidCard$ Card.Self | IsMadness$ True | Secondary$ True"
                    + " | TriggerDescription$ Play Madness " + ManaCostParser.parse(manacost) + " - " + card.getName();

            final String playMadnessStr = "DB$ Play | Defined$ Self | PlayCost$ " + manacost +
                    " | ConditionDefined$ Self | ConditionPresent$ Card.StrictlySelf+inZoneExile" +
                    " | Optional$ True | RememberPlayed$ True | Madness$ True";

            final String moveToYardStr = "DB$ ChangeZone | Defined$ Self.StrictlySelf | Origin$ Exile" +
                    " | Destination$ Graveyard | TrackDiscarded$ True | ConditionDefined$ Remembered | ConditionPresent$ Card" +
                    " Card | ConditionCompare$ EQ0";

            final String cleanUpStr = "DB$ Cleanup | ClearRemembered$ True";

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            SpellAbility playSA = AbilityFactory.getAbility(playMadnessStr, card);
            AbilitySub moveSA = (AbilitySub)AbilityFactory.getAbility(moveToYardStr, card);
            AbilitySub cleanupSA = (AbilitySub)AbilityFactory.getAbility(cleanUpStr, card);
            moveSA.setSubAbility(cleanupSA);
            playSA.setSubAbility(moveSA);
            playSA.setIntrinsic(intrinsic);

            parsedTrigger.setOverridingAbility(playSA);

            inst.addTrigger(parsedTrigger);
        } else if (keyword.equals("Melee")) {
            final String trigStr = "Mode$ Attacks | ValidCard$ Card.Self | Secondary$ True " +
                    " | TriggerDescription$ Melee (" + inst.getReminderText() + ")";

            final String effect = "DB$ Pump | Defined$ TriggeredAttackerLKICopy | NumAtt$ MeleeX | NumDef$ MeleeX";
            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setSVar("MeleeX", "TriggeredPlayersDefenders$Amount");
            sa.setIntrinsic(intrinsic);
            trigger.setOverridingAbility(sa);

            inst.addTrigger(trigger);
        } else if (keyword.equals("Mentor")) {
            final String trigStr = "Mode$ Attacks | ValidCard$ Card.Self | Secondary$ True " +
                    " | TriggerDescription$ Mentor (" + inst.getReminderText() + ")";

            final String effect = "DB$ PutCounter | CounterType$ P1P1 | CounterNum$ 1"
                    + " | ValidTgts$ Creature.attacking+powerLTX"
                    + " | TgtPrompt$ Select target attacking creature with less power";
            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setSVar("X", "Count$CardPower");
            sa.setIntrinsic(intrinsic);
            trigger.setOverridingAbility(sa);

            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Miracle")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];
            final String abStrReveal = "DB$ Reveal | Defined$ You | RevealDefined$ Self"
                    + " | MiracleCost$ " + manacost;
            final String abStrPlay = "DB$ Play | Defined$ Self | PlayCost$ " + manacost;

            String revealed = "DB$ ImmediateTrigger | TriggerDescription$ CARDNAME - Miracle";

            final String trigStrDrawn = "Mode$ Drawn | ValidCard$ Card.Self | Number$ 1 | Secondary$ True"
                + " | OptionalDecider$ You | Static$ True | TriggerDescription$ CARDNAME - Miracle";

            final Trigger triggerDrawn = TriggerHandler.parseTrigger(trigStrDrawn, card, intrinsic);

            SpellAbility revealSA = AbilityFactory.getAbility(abStrReveal, card);

            AbilitySub immediateTriggerSA = (AbilitySub)AbilityFactory.getAbility(revealed, card);

            immediateTriggerSA.setAdditionalAbility("Execute", (AbilitySub)AbilityFactory.getAbility(abStrPlay, card));

            revealSA.setSubAbility(immediateTriggerSA);

            triggerDrawn.setOverridingAbility(revealSA);

            inst.addTrigger(triggerDrawn);
        } else if (keyword.startsWith("Modular")) {
            final String abStr = "DB$ PutCounter | ValidTgts$ Artifact.Creature | " +
                    "TgtPrompt$ Select target artifact creature | CounterType$ P1P1 | CounterNum$ ModularX";

            String trigStr = "Mode$ ChangesZone | ValidCard$ Card.Self | Origin$ Battlefield | Destination$ Graveyard" +
                    " | OptionalDecider$ TriggeredCardController | TriggerController$ TriggeredCardController" +
                    " | Secondary$ True | TriggerDescription$ When CARDNAME dies, " +
                    "you may put a +1/+1 counter on target artifact creature for each +1/+1 counter on CARDNAME";

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            SpellAbility ab = AbilityFactory.getAbility(abStr, card);

            ab.setSVar("ModularX", "TriggeredCard$CardCounters.P1P1");

            trigger.setOverridingAbility(ab);

            inst.addTrigger(trigger);
        } else if (keyword.equals("Myriad")) {
            final String actualTrigger = "Mode$ Attacks | ValidCard$ Card.Self | Secondary$ True"
                    + " | TriggerDescription$ Myriad (" + inst.getReminderText() + ")";

            final String repeatStr = "DB$ RepeatEach | RepeatPlayers$ OpponentsOtherThanDefendingPlayer | ChangeZoneTable$ True";

            final String copyStr = "DB$ CopyPermanent | Defined$ Self | TokenTapped$ True | Optional$ True | TokenAttacking$ Remembered"
                    + " | ChoosePlayerOrPlaneswalker$ True | ImprintTokens$ True";

            final String delTrigStr = "DB$ DelayedTrigger | Mode$ Phase | Phase$ EndCombat | RememberObjects$ Imprinted"
            + " | TriggerDescription$ Exile the tokens at end of combat.";

            final String exileStr = "DB$ ChangeZone | Defined$ DelayTriggerRemembered | Origin$ Battlefield | Destination$ Exile";

            final String cleanupStr = "DB$ Cleanup | ClearImprinted$ True";

            SpellAbility repeatSA = AbilityFactory.getAbility(repeatStr, card);

            AbilitySub copySA = (AbilitySub) AbilityFactory.getAbility(copyStr, card);
            repeatSA.setAdditionalAbility("RepeatSubAbility", copySA);

            AbilitySub delTrigSA = (AbilitySub) AbilityFactory.getAbility(delTrigStr, card);

            AbilitySub exileSA = (AbilitySub) AbilityFactory.getAbility(exileStr, card);
            delTrigSA.setAdditionalAbility("Execute", exileSA);

            AbilitySub cleanupSA = (AbilitySub) AbilityFactory.getAbility(cleanupStr, card);
            delTrigSA.setSubAbility(cleanupSA);

            repeatSA.setSubAbility(delTrigSA);

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, intrinsic);

            repeatSA.setIntrinsic(intrinsic);

            parsedTrigger.setOverridingAbility(repeatSA);
            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("Partner:")) {
            // Partner With
            final String[] k = keyword.split(":");
            final String trigStr = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield " +
                    "| ValidCard$ Card.Self | Secondary$ True " +
                    "| TriggerDescription$ Partner with " + k[1] + " (" + inst.getReminderText() + ")";
            // replace , for ; in the ChangeZone
            k[1] = k[1].replace(",", ";");

            final String effect = "DB$ ChangeZone | ValidTgts$ Player | TgtPrompt$ Select target player" +
                    " | Origin$ Library | Destination$ Hand | ChangeType$ Card.named" + k[1] +
                    " | ChangeNum$ 1 | Hidden$ True | Chooser$ Targeted | Optional$ Targeted";

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Persist")) {
            final String trigStr = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard " +
                    " | ValidCard$ Card.Self+counters_EQ0_M1M1 | TriggerZones$ Battlefield | Secondary$ True" +
                    " | TriggerDescription$ Persist (" + inst.getReminderText() + ")";
            final String effect = "DB$ ChangeZone | Defined$ TriggeredNewCardLKICopy | Origin$ Graveyard | Destination$ Battlefield | WithCounters$ M1M1_1";

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));
            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Poisonous")) {
            final String[] k = keyword.split(":");
            final String n = k[1];
            final String trigStr = "Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player | CombatDamage$ True | Secondary$ True"
                    + " | TriggerZones$ Battlefield | TriggerDescription$ Poisonous " + n + " (" + inst.getReminderText() + ")";

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            final String effect = "DB$ Poison | Defined$ TriggeredTarget | Num$ " + n;
            parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));
            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("Presence")) {
            final String[] k = keyword.split(":");
            card.addIntrinsicKeyword("Kicker:Reveal<1/" + k[1] + ">:Generic");
        } else if (keyword.equals("Provoke")) {
            final String actualTrigger = "Mode$ Attacks | ValidCard$ Card.Self | OptionalDecider$ You | Secondary$ True"
                    + " | TriggerDescription$ Provoke (" + inst.getReminderText() + ")";
            final String blockStr = "DB$ MustBlock | ValidTgts$ Creature.DefenderCtrl | TgtPrompt$ Select target creature defending player controls";
            final String untapStr = "DB$ Untap | Defined$ Targeted";

            SpellAbility blockSA = AbilityFactory.getAbility(blockStr, card);
            AbilitySub untapSA = (AbilitySub)AbilityFactory.getAbility(untapStr, card);
            blockSA.setSubAbility(untapSA);

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, intrinsic);
            blockSA.setIntrinsic(intrinsic);
            parsedTrigger.setOverridingAbility(blockSA);

            inst.addTrigger(parsedTrigger);
        } else if (keyword.equals("Prowess")) {
            final String trigProwess = "Mode$ SpellCast | ValidCard$ Card.nonCreature"
                    + " | ValidActivatingPlayer$ You | TriggerZones$ Battlefield | TriggerDescription$ "
                    + "Prowess (" + inst.getReminderText() + ")";

            final String effect = "DB$ Pump | Defined$ Self | NumAtt$ +1 | NumDef$ +1";

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(trigProwess, card, intrinsic);
            parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("Rampage")) {
            final String[] k = keyword.split(":");
            final String n = k[1];

            final String trigStr = "Mode$ AttackerBlocked | ValidCard$ Card.Self | TriggerZones$ Battlefield " +
                    " | ValidBlocker$ Creature | MinBlockers$ 1 | Secondary$ True " +
                    " | TriggerDescription$ Rampage " + n + " (" + inst.getReminderText() + ")";

            final String effect = "DB$ Pump | Defined$ TriggeredAttackerLKICopy" +
                    " | NumAtt$ Rampage" + n + " | NumDef$ Rampage" + n;

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setSVar("Rampage" + n, "SVar$RampageCount/Times." + n);

            sa.setSVar("RampageCount", "TriggerCount$NumBlockers/Minus.1");
            sa.setIntrinsic(intrinsic);
            trigger.setOverridingAbility(sa);
            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Renown")) {
            final String[] k = keyword.split(":");

            String renownTrig = "Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player"
                    + " | IsPresent$ Card.Self+IsNotRenowned | CombatDamage$ True | Secondary$ True"
                    + " | TriggerDescription$ Renown " + k[1] +" (" + inst.getReminderText() + ")";

            final String effect = "DB$ PutCounter | Defined$ Self | "
                    + "CounterType$ P1P1 | CounterNum$ " + k[1] + " | Renown$ True";

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(renownTrig, card, intrinsic);
            parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("Recover")) {
            final String recoverCost = keyword.split(":")[1];
            final String changeStr = "DB$ ChangeZone | Defined$ Self"
                        + " | Origin$ Graveyard | Destination$ Hand | UnlessCost$ "
                    + recoverCost + " | UnlessPayer$ You | UnlessSwitched$ True"
                    + " | UnlessResolveSubs$ WhenNotPaid";
            final String exileStr = "DB$ ChangeZone | Defined$ Self | Origin$ Graveyard | Destination$ Exile";

            SpellAbility changeSA = AbilityFactory.getAbility(changeStr, card);
            AbilitySub exileSA = (AbilitySub) AbilityFactory.getAbility(exileStr, card);
            changeSA.setSubAbility(exileSA);

            String trigObject = card.isCreature() ? "Creature.Other+YouOwn" : "Creature.YouOwn";
            String trigArticle = card.isCreature() ? "another" : "a";
            String trigStr = "Mode$ ChangesZone | ValidCard$ " + trigObject
                    + " | Origin$ Battlefield | Destination$ Graveyard | "
                    + "TriggerZones$ Graveyard | Secondary$ True | "
                    + "TriggerDescription$ Recover " + recoverCost + " (When " + trigArticle + " creature is "
                    + "put into your graveyard from the battlefield, you "
                    + "may pay " + recoverCost + ". If you do, return "
                    + "CARDNAME from your graveyard to your hand. Otherwise,"
                    + " exile CARDNAME.)";
            final Trigger myTrigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);
            changeSA.setIntrinsic(intrinsic);
            myTrigger.setOverridingAbility(changeSA);

            inst.addTrigger(myTrigger);
        } else if (keyword.startsWith("Replicate")) {
            final String trigScript = "Mode$ SpellCast | ValidCard$ Card.Self | CheckSVar$ ReplicateAmount | Secondary$ True | TriggerDescription$ Copy CARDNAME for each time you paid its replicate cost";
            final String abString = "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | Amount$ ReplicateAmount | MayChooseTarget$ True";

            final Trigger replicateTrigger = TriggerHandler.parseTrigger(trigScript, card, intrinsic);
            final SpellAbility replicateAbility = AbilityFactory.getAbility(abString, card);
            replicateAbility.setSVar("ReplicateAmount", "0");
            replicateTrigger.setOverridingAbility(replicateAbility);
            replicateTrigger.setSVar("ReplicateAmount", "0");
            inst.addTrigger(replicateTrigger);

        } else if (keyword.startsWith("Ripple")) {
            final String[] k = keyword.split(":");
            final String num = k[1];

            final String actualTrigger = "Mode$ SpellCast | ValidCard$ Card.Self | OptionalDecider$ You | "
                    + " Secondary$ True | TriggerDescription$ Ripple " + num + " - CARDNAME";

            final String abString = "DB$ Dig | NoMove$ True | DigNum$ " + num +
                    " | Reveal$ True | RememberRevealed$ True";

            final String dbCast = "DB$ Play | Valid$ Card.IsRemembered+sameName | " +
                    "ValidZone$ Library | WithoutManaCost$ True | Optional$ True | " +
                    "Amount$ All";

            final String toBottom = "DB$ ChangeZoneAll | ChangeType$ Card.IsRemembered "
                    + "| Origin$ Library | Destination$ Library | LibraryPosition$ -1";

            final String cleanuptxt = "DB$ Cleanup | ClearRemembered$ True";

            SpellAbility sa = AbilityFactory.getAbility(abString, card);
            AbilitySub saCast = (AbilitySub)AbilityFactory.getAbility(dbCast, card);
            AbilitySub saBottom = (AbilitySub)AbilityFactory.getAbility(toBottom, card);
            AbilitySub saCleanup = (AbilitySub)AbilityFactory.getAbility(cleanuptxt, card);

            saBottom.setSubAbility(saCleanup);
            saCast.setSubAbility(saBottom);
            sa.setSubAbility(saCast);

            sa.setIntrinsic(intrinsic);

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, intrinsic);
            parsedTrigger.setOverridingAbility(sa);

            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("Saga")) {
            final String[] k = keyword.split(":");
            final List<String> abs = Arrays.asList(k[2].split(","));
            if (abs.size() != Integer.valueOf(k[1])) {
                throw new RuntimeException("Saga max differ from Ability amount");
            }

            int idx = 0;
            int skipId = 0;
            for(String ab : abs) {
                idx += 1;
                if (idx <= skipId) {
                    continue;
                }

                skipId = idx + abs.subList(idx - 1, abs.size()).lastIndexOf(ab);
                StringBuilder desc = new StringBuilder();
                for (int i = idx; i <= skipId; i++) {
                    if (i != idx) {
                        desc.append(", ");
                    }
                    desc.append(TextUtil.toRoman(i));
                }

                for (int i = idx; i <= skipId; i++) {
                    SpellAbility sa = AbilityFactory.getAbility(card, ab);
                    sa.setChapter(i);

                    StringBuilder trigStr = new StringBuilder("Mode$ CounterAdded | ValidCard$ Card.Self | TriggerZones$ Battlefield");
                    trigStr.append("| CounterType$ LORE | CounterAmount$ EQ").append(i);
                    if (i != idx) {
                        trigStr.append(" | Secondary$ True");
                    }
                    trigStr.append("| TriggerDescription$ ").append(desc).append(" — ").append(sa.getDescription());
                    final Trigger t = TriggerHandler.parseTrigger(trigStr.toString(), card, intrinsic);
                    t.setOverridingAbility(sa);
                    inst.addTrigger(t);
                }
            }
        } else if (keyword.equals("Soulbond")) {
            // Setup ETB trigger for card with Soulbond keyword
            final String actualTriggerSelf = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Card.Self | OptionalDecider$ You | "
                    + "IsPresent$ Creature.Other+YouCtrl+NotPaired | Secondary$ True | "
                    + "TriggerDescription$ When CARDNAME enters the battlefield, "
                    + "you may pair CARDNAME with another unpaired creature you control";
            final String abStringSelf = "DB$ Bond | Defined$ Self | ValidCards$ Creature.Other+YouCtrl+NotPaired";
            final Trigger parsedTriggerSelf = TriggerHandler.parseTrigger(actualTriggerSelf, card, intrinsic);
            parsedTriggerSelf.setOverridingAbility(AbilityFactory.getAbility(abStringSelf, card));

            // Setup ETB trigger for other creatures you control
            final String actualTriggerOther = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | "
                    + "ValidCard$ Creature.Other+YouCtrl | TriggerZones$ Battlefield | OptionalDecider$ You | "
                    + " IsPresent$ Creature.Self+NotPaired | Secondary$ True | "
                    + " TriggerDescription$ When another unpaired creature you control enters the battlefield, "
                    + "you may pair it with CARDNAME";
            final String abStringOther = "DB$ Bond | Defined$ TriggeredCard | ValidCards$ Creature.Self+NotPaired";
            final Trigger parsedTriggerOther = TriggerHandler.parseTrigger(actualTriggerOther, card, intrinsic);
            parsedTriggerOther.setOverridingAbility(AbilityFactory.getAbility(abStringOther, card));

            inst.addTrigger(parsedTriggerSelf);
            inst.addTrigger(parsedTriggerOther);
        } else if (keyword.startsWith("Soulshift")) {
            final String[] k = keyword.split(":");

            final String actualTrigger = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard"
                    + "| Secondary$ True | OptionalDecider$ You | ValidCard$ Card.Self"
                    + "| TriggerController$ TriggeredCardController | TriggerDescription$ " + k[0] + " " + k[1]
                    + " (" + inst.getReminderText() + ")";
            final String effect = "DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand"
                    + "| ValidTgts$ Spirit.YouOwn+cmcLE" + k[1];
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, intrinsic);
            final SpellAbility sp = AbilityFactory.getAbility(effect, card);
            // Soulshift X
            if (k[1].equals("X")) {
                sp.setSVar("X", "Count$LastStateBattlefield " + k[3]);
            }

            parsedTrigger.setOverridingAbility(sp);

            inst.addTrigger(parsedTrigger);
        } else if (keyword.equals("Storm")) {
            final String actualTrigger = "Mode$ SpellCast | ValidCard$ Card.Self | Secondary$ True"
                    + "| TriggerDescription$ Storm (" + inst.getReminderText() + ")";

            String effect = "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | Amount$ StormCount | MayChooseTarget$ True";

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, intrinsic);

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);

            sa.setSVar("StormCount", "TriggerCount$CurrentStormCount/Minus.1");

            parsedTrigger.setOverridingAbility(sa);

            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("Suspend")) {
            //upkeep trigger
            StringBuilder upkeepTrig = new StringBuilder();

            upkeepTrig.append("Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Exile ");
            upkeepTrig.append(" | IsPresent$ Card.Self+suspended | PresentZone$ Exile");
            // Mark this trigger as Secondary, so it's not displayed twice
            upkeepTrig.append(" | Secondary$ True | TriggerDescription$ At the beginning of your upkeep, if this card is suspended, remove a time counter from it");

            final String abRemove = "DB$ RemoveCounter | Defined$ Self | CounterType$ TIME | CounterNum$ 1";

            final Trigger parsedUpkeepTrig = TriggerHandler.parseTrigger(upkeepTrig.toString(), card, intrinsic);
            parsedUpkeepTrig.setOverridingAbility(AbilityFactory.getAbility(abRemove, card));

            //play trigger
            StringBuilder playTrig = new StringBuilder();

            playTrig.append("Mode$ CounterRemoved | TriggerZones$ Exile | ValidCard$ Card.Self | CounterType$ TIME | NewCounterAmount$ 0 | Secondary$ True ");
            playTrig.append(" | TriggerDescription$ When the last time counter is removed from this card, if it's exiled, play it without paying its mana cost if able.  ");
            playTrig.append("If you can't, it remains exiled. If you cast a creature spell this way, it gains haste until you lose control of the spell or the permanent it becomes.");

            String abPlay = "DB$ Play | Defined$ Self | WithoutManaCost$ True | SuspendCast$ True";
            if (card.isPermanent()) {
                abPlay += "| RememberPlayed$ True";
            }

            final SpellAbility saPlay = AbilityFactory.getAbility(abPlay, card);

            if (card.isPermanent()) {
                final String abPump = "DB$ Pump | Defined$ Remembered | KW$ Haste | PumpZone$ Stack "
                        + "| ConditionDefined$ Remembered | ConditionPresent$ Creature | UntilLoseControlOfHost$ True";
                final AbilitySub saPump = (AbilitySub)AbilityFactory.getAbility(abPump, card);

                String dbClean = "DB$ Cleanup | ClearRemembered$ True";
                final AbilitySub saCleanup = (AbilitySub) AbilityFactory.getAbility(dbClean, card);
                saPump.setSubAbility(saCleanup);

                saPlay.setSubAbility(saPump);
            }

            final Trigger parsedPlayTrigger = TriggerHandler.parseTrigger(playTrig.toString(), card, intrinsic);
            parsedPlayTrigger.setOverridingAbility(saPlay);

            inst.addTrigger(parsedUpkeepTrig);
            inst.addTrigger(parsedPlayTrigger);
        } else if (keyword.startsWith("Tribute")) {
            // use hardcoded ability name
            final String abStr = "TrigNotTribute";

            // get Description from Ability
            final String desc = AbilityFactory.getMapParams(card.getSVar(abStr)).get("SpellDescription");
            final String trigStr = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | ValidCard$ Card.Self+notTributed " +
                     " | Execute$ " + abStr + " | TriggerDescription$ " + desc;

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            inst.addTrigger(parsedTrigger);
        } else if (keyword.equals("Undying")) {
            final String trigStr = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard " +
                    " | ValidCard$ Card.Self+counters_EQ0_P1P1 | TriggerZones$ Battlefield | Secondary$ True" +
                    " | TriggerDescription$ Undying (" + inst.getReminderText() + ")";
            final String effect = "DB$ ChangeZone | Defined$ TriggeredNewCardLKICopy | Origin$ Graveyard | Destination$ Battlefield | WithCounters$ P1P1_1";

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);
            parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("UpkeepCost")) {
            final String[] k = keyword.split(":");
            final Cost cost = new Cost(k[1], true);

            final StringBuilder sb = new StringBuilder();
            sb.append("At the beginning of your upkeep, sacrifice CARDNAME unless you ");
            if (cost.isOnlyManaCost()) {
                sb.append("pay ");
            }
            final String costStr = k.length == 3 ? k[2] : cost.toSimpleString();

            sb.append(costStr.substring(0, 1).toLowerCase()).append(costStr.substring(1));
            sb.append(".");

            String upkeepTrig = "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield | " +
                    "TriggerDescription$ " + sb.toString();

            String effect = "DB$ Sacrifice | SacValid$ Self | UnlessPayer$ You | UnlessCost$ " + k[1];

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(upkeepTrig, card, intrinsic);
            parsedTrigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("Vanishing")) {
            // Remove Time counter trigger
            final StringBuilder upkeepTrig = new StringBuilder("Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | " +
                    "TriggerZones$ Battlefield | IsPresent$ Card.Self+counters_GE1_TIME");
            if (keyword.contains(":")) {
                upkeepTrig.append(" | Secondary$ True");
            }
            upkeepTrig.append(" | TriggerDescription$ At the beginning of your upkeep, " +
                    "if CARDNAME has a time counter on it, remove a time counter from it.");

            final String remove = "DB$ RemoveCounter | Defined$ Self" +
                    " | CounterType$ TIME | CounterNum$ 1";
            final Trigger parsedUpkeepTrig = TriggerHandler.parseTrigger(upkeepTrig.toString(), card, intrinsic);
            parsedUpkeepTrig.setOverridingAbility(AbilityFactory.getAbility(remove, card));

            // sacrifice trigger
            final StringBuilder sacTrig = new StringBuilder("Mode$ CounterRemoved | TriggerZones$ Battlefield" +
                    " | ValidCard$ Card.Self | NewCounterAmount$ 0 | CounterType$ TIME");
            if (keyword.contains(":")) {
                sacTrig.append("| Secondary$ True");
            }
            sacTrig.append("| TriggerDescription$ When the last time counter is removed from CARDNAME, sacrifice it.");

            final String sac = "DB$ Sacrifice | SacValid$ Self";
            final Trigger parsedSacTrigger = TriggerHandler.parseTrigger(sacTrig.toString(), card, intrinsic);
            parsedSacTrigger.setOverridingAbility(AbilityFactory.getAbility(sac, card));

            inst.addTrigger(parsedUpkeepTrig);
            inst.addTrigger(parsedSacTrigger);
        } else if (keyword.startsWith("Ward")) {
            final String[] k = keyword.split(":");
            final Cost cost = new Cost(k[1], false);
            String costDesc = cost.toSimpleString();

            String strTrig = "Mode$ BecomesTarget | ValidSource$ Card.OppCtrl | ValidTarget$ Card.Self "
                    + " | Secondary$ True | TriggerZones$ Battlefield | TriggerDescription$ Ward " + costDesc + " ("
                    + inst.getReminderText() + ")";
            String effect = "DB$ Counter | Defined$ TriggeredSourceSA | UnlessCost$ " + k[1]
                    + " | UnlessPayer$ TriggeredSourceSAController";

            final Trigger trigger = TriggerHandler.parseTrigger(strTrig, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("MayFlashSac")) {
            String strTrig = "Mode$ SpellCast | ValidCard$ Card.Self | ValidSA$ Spell.MayPlaySource | Static$ True | Secondary$ True "
                    + " | TriggerDescription$ If you cast it any time a sorcery couldn't have been cast, "
                    + " the controller of the permanent it becomes sacrifices it at the beginning of the next cleanup step.";

            final String strDelay = "DB$ DelayedTrigger | Mode$ Phase | Phase$ Cleanup | TriggerDescription$ At the beginning of the next cleanup step, sacrifice CARDNAME.";
            final String strSac = "DB$ SacrificeAll | Defined$ Self";

            SpellAbility saDelay = AbilityFactory.getAbility(strDelay, card);
            saDelay.setAdditionalAbility("Execute", (AbilitySub) AbilityFactory.getAbility(strSac, card));
            final Trigger trigger = TriggerHandler.parseTrigger(strTrig, card, intrinsic);
            trigger.setOverridingAbility(saDelay);
            inst.addTrigger(trigger);
        }
    }

    public static void addReplacementEffect(final KeywordInterface inst, final CardState card, final boolean intrinsic) {
        Card host = card.getCard();

        String keyword = inst.getOriginal();
        if (keyword.startsWith("Absorb")) {
            final String[] k = keyword.split(":");
            final String n = k[1];

            StringBuilder sb = new StringBuilder();
            sb.append("Event$ DamageDone | ActiveZones$ Battlefield | ValidTarget$ Card.Self");
            sb.append(" | PreventionEffect$ True | Secondary$ True | Description$ Absorb ").append(n);
            sb.append(" (").append(inst.getReminderText()).append(")");
            String repeffstr = sb.toString();

            String abString = "DB$ ReplaceDamage | Amount$ " + n;
            SpellAbility replaceDamage = AbilityFactory.getAbility(abString, card);
            replaceDamage.setIntrinsic(intrinsic);

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);
            re.setOverridingAbility(replaceDamage);
            inst.addReplacement(re);
        } else if (keyword.equals("Aftermath") && card.getStateName().equals(CardStateName.RightSplit)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Event$ Moved | ValidCard$ Card.Self | Origin$ Stack | ExcludeDestination$ Exile ");
            sb.append("| ValidStackSa$ Spell.Aftermath | Description$ Aftermath");

            sb.append(" (");
            sb.append(inst.getReminderText());
            sb.append(")");

            String repeffstr = sb.toString();

            String abExile = "DB$ ChangeZone | Defined$ Self | Origin$ Stack | Destination$ Exile";

            SpellAbility saExile = AbilityFactory.getAbility(abExile, card);

            saExile.setIntrinsic(intrinsic);

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);

            re.setOverridingAbility(saExile);

            inst.addReplacement(re);
        } else if (keyword.startsWith("Amplify")) {
            final String[] ampString = keyword.split(":");
            final String amplifyMagnitude = ampString[1];
            final String ampTypes = ampString[2];
            String[] refinedTypes = ampTypes.split(",");
            final StringBuilder types = new StringBuilder();
            for (int i = 0; i < refinedTypes.length; i++) {
                types.append("Card.").append(refinedTypes[i]).append("+YouCtrl");
                if (i + 1 != refinedTypes.length) {
                    types.append(",");
                }
            }

            // Setup ETB replacement effects
            final String actualRep = "Event$ Moved | Destination$ Battlefield | ValidCard$ Card.Self |"
                    + " | Description$ Amplify " + amplifyMagnitude + " ("
                    + inst.getReminderText() + ")";

            final String abString = "DB$ Reveal | AnyNumber$ True | RevealValid$ "
                    + types.toString() + " | RememberRevealed$ True";

            SpellAbility saReveal = AbilityFactory.getAbility(abString, card);

            final String dbString = "DB$ PutCounter | Defined$ ReplacedCard | CounterType$ P1P1 | "
                    + "CounterNum$ AmpMagnitude | ETB$ True";

            AbilitySub saPut = (AbilitySub) AbilityFactory.getAbility(dbString, card);

            saPut.setSVar("AmpMagnitude", "SVar$Revealed/Times." + amplifyMagnitude);
            saPut.setSVar("Revealed", "Remembered$Amount");

            String dbClean = "DB$ Cleanup | ClearRemembered$ True";
            AbilitySub saCleanup = (AbilitySub) AbilityFactory.getAbility(dbClean, card);

            saPut.setSubAbility(saCleanup);
            setupETBReplacementAbility(saCleanup);

            saReveal.setSubAbility(saPut);

            saReveal.setIntrinsic(intrinsic);

            ReplacementEffect re = ReplacementHandler.parseReplacement(actualRep, host, intrinsic, card);

            re.setOverridingAbility(saReveal);

            inst.addReplacement(re);
        } else if (keyword.startsWith("Bloodthirst")) {
            final String numCounters = keyword.split(":")[1];

            String desc;
            if (numCounters.equals("X")) {
                desc = "Bloodthirst X (This creature enters the battlefield with X +1/+1 counters on it, "
                        + "where X is the damage dealt to your opponents this turn.)";
            } else {
                desc = "Bloodthirst " + numCounters + " (" + inst.getReminderText() + ")";
            }

            final String etbCounter = "etbCounter:P1P1:" + numCounters + ":Bloodthirst$ True:" + desc;

            final ReplacementEffect re = makeEtbCounter(etbCounter, card, intrinsic);
            if (numCounters.equals("X")) {
                re.getOverridingAbility().setSVar("X", "Count$BloodthirstAmount");
            }

            inst.addReplacement(re);
        } else if (keyword.startsWith("Buyback")) {
            final String[] k = keyword.split(":");
            final Cost cost = new Cost(k[1], false);

            StringBuilder sb = new StringBuilder();
            sb.append("Event$ Moved | ValidCard$ Card.Self | Origin$ Stack | Destination$ Graveyard | Fizzle$ False ");
            sb.append("| Secondary$ True | ValidStackSa$ Spell.Buyback | Description$ Buyback");

            sb.append(cost.isOnlyManaCost() ? " " : "—");

            sb.append(cost.toSimpleString());

            if (!cost.isOnlyManaCost()) {
                sb.append(".");
            }

            sb.append(" (");
            sb.append(inst.getReminderText());
            sb.append(")");

            String repeffstr = sb.toString();

            String abReturn = "DB$ ChangeZone | Defined$ Self | Origin$ Stack | Destination$ Hand";

            SpellAbility saReturn = AbilityFactory.getAbility(abReturn, card);

            saReturn.setIntrinsic(intrinsic);

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);

            re.setOverridingAbility(saReturn);

            inst.addReplacement(re);
        } else if (keyword.startsWith("Dredge")) {
            final String dredgeAmount = keyword.split(":")[1];

            final String actualRep = "Event$ Draw | ActiveZones$ Graveyard | ValidPlayer$ You | "
                    + "Secondary$ True | Optional$ True | CheckSVar$ "
                    + "DredgeCheckLib | SVarCompare$ GE" + dredgeAmount
                    + " | AICheckDredge$ True | Description$ CARDNAME - Dredge " + dredgeAmount;

            final String abString = "DB$ Mill | Defined$ You | NumCards$ " + dredgeAmount;

            final String moveToPlay = "DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand | Defined$ Self";

            SpellAbility saMill = AbilityFactory.getAbility(abString, card);
            AbilitySub saMove = (AbilitySub) AbilityFactory.getAbility(moveToPlay, card);
            saMill.setSubAbility(saMove);

            saMill.setIntrinsic(intrinsic);

            ReplacementEffect re = ReplacementHandler.parseReplacement(actualRep, host, intrinsic, card);
            re.setOverridingAbility(saMill);

            re.setSVar("DredgeCheckLib", "Count$ValidLibrary Card.YouOwn");

            inst.addReplacement(re);
        } else if (keyword.startsWith("Devour")) {
            final String[] k = keyword.split(":");
            final String magnitude = k[1];

            String sacrificeStr = "DB$ Sacrifice | Defined$ You | Amount$ DevourSacX | "
                    + "SacValid$ Creature.Other | SacMessage$ another creature (Devour "+ magnitude + ") | "
                    + "RememberSacrificed$ True | Optional$ True | Devour$ True";

            String counterStr = "DB$ PutCounter | ETB$ True | Defined$ Self | CounterType$ P1P1 | CounterNum$ DevourX";
            String cleanupStr = "DB$ Cleanup | ClearRemembered$ True";

            AbilitySub sacrificeSA = (AbilitySub) AbilityFactory.getAbility(sacrificeStr, card);
            sacrificeSA.setSVar("DevourSacX", "Count$Valid Creature.YouCtrl+Other");

            AbilitySub counterSA = (AbilitySub) AbilityFactory.getAbility(counterStr, card);
            counterSA.setSVar("DevourX", "SVar$DevourSize/Times." + magnitude);
            counterSA.setSVar("DevourSize", "Count$RememberedSize");
            sacrificeSA.setSubAbility(counterSA);

            AbilitySub cleanupSA = (AbilitySub) AbilityFactory.getAbility(cleanupStr, card);
            counterSA.setSubAbility(cleanupSA);

            String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Secondary$ True | Description$ Devour " + magnitude + " ("+ inst.getReminderText() + ")";

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);

            setupETBReplacementAbility(cleanupSA);
            re.setOverridingAbility(sacrificeSA);

            inst.addReplacement(re);
        } else if (keyword.startsWith("Fading")) {
            final String[] k = keyword.split(":");
            final String m = k[1];

            StringBuilder sb = new StringBuilder("etbCounter:FADE:");
            sb.append(m).append(":no Condition:");
            sb.append("Fading ");
            sb.append(m);
            sb.append(" (").append(inst.getReminderText()).append(")");

            final ReplacementEffect re = makeEtbCounter(sb.toString(), card, intrinsic);

            inst.addReplacement(re);
        } else if (keyword.startsWith("Flashback")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Event$ Moved | ValidCard$ Card.Self | Origin$ Stack | ExcludeDestination$ Exile ");
            sb.append("| ValidStackSa$ Spell.Flashback | Description$ Flashback");

            if (keyword.contains(":")) {
                final String[] k = keyword.split(":");
                final Cost cost = new Cost(k[1], false);
                sb.append( cost.isOnlyManaCost() ? " " : "—");

                sb.append(cost.toSimpleString());

                if (!cost.isOnlyManaCost()) {
                    sb.append(".");
                }
            }

            sb.append(" (");
            sb.append(inst.getReminderText());
            sb.append(")");

            String repeffstr = sb.toString();

            String abExile = "DB$ ChangeZone | Defined$ Self | Origin$ Stack | Destination$ Exile";

            SpellAbility saExile = AbilityFactory.getAbility(abExile, card);

            if (!intrinsic) {
                saExile.setIntrinsic(false);
            }

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);

            re.setOverridingAbility(saExile);

            inst.addReplacement(re);
        } else if (keyword.startsWith("Graft")) {
            final String[] k = keyword.split(":");
            final String m = k[1];

            StringBuilder sb = new StringBuilder("etbCounter:P1P1:");
            sb.append(m).append(":no Condition:");
            sb.append("Graft ");
            sb.append(m);
            sb.append(" (").append(inst.getReminderText()).append(")");

            final ReplacementEffect re = makeEtbCounter(sb.toString(), card, intrinsic);

            inst.addReplacement(re);
        } else if (keyword.equals("Jump-start")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Event$ Moved | ValidCard$ Card.Self | Origin$ Stack | ExcludeDestination$ Exile ");
            sb.append("| Secondary$ True | ValidStackSa$ Spell.Jumpstart | Description$ Jump-start (");
            sb.append(inst.getReminderText());
            sb.append(")");

            String repeffstr = sb.toString();

            String abExile = "DB$ ChangeZone | Defined$ Self | Origin$ Stack | Destination$ Exile";

            SpellAbility saExile = AbilityFactory.getAbility(abExile, card);

            if (!intrinsic) {
                saExile.setIntrinsic(false);
            }

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);

            re.setOverridingAbility(saExile);

            inst.addReplacement(re);
        } else if (keyword.startsWith("Madness")) {
            // Set Madness Replacement effects
            String repeffstr = "Event$ Discard | ActiveZones$ Hand | ValidCard$ Card.Self | Secondary$ True "
                    + " | Description$ Madness: If you discard this card, discard it into exile.";
            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);
            String sVarMadness = "DB$ Discard | Defined$ ReplacedPlayer | Mode$ Defined | DefinedCards$ ReplacedCard | Madness$ True";

            re.setOverridingAbility(AbilityFactory.getAbility(sVarMadness, card));

            inst.addReplacement(re);
        } else if (keyword.startsWith("Modular")) {
            final String[] k = keyword.split(":");
            final String m = k[1];

            StringBuilder sb = new StringBuilder("etbCounter:P1P1:");
            sb.append(m).append(":no Condition:");
            sb.append("Modular ");
            if (!StringUtils.isNumeric(m)) {
                sb.append("- ");
            }
            sb.append(m);
            sb.append(" (").append(inst.getReminderText()).append(")");

            final ReplacementEffect re = makeEtbCounter(sb.toString(), card, intrinsic);
            if ("Sunburst".equals(m)) {
                re.getOverridingAbility().setSVar("Sunburst", "Count$Converge");
            }
            inst.addReplacement(re);
        } else if (keyword.equals("Rebound")) {
            String repeffstr = "Event$ Moved | ValidLKI$ Card.Self+wasCastFromHand+YouOwn+YouCtrl "
            + " | Origin$ Stack | Destination$ Graveyard | Fizzle$ False "
            + " | Description$ Rebound (" + inst.getReminderText() + ")";

            String abExile = "DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Stack | Destination$ Exile";
            String delTrig = "DB$ DelayedTrigger | Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You " +
            " | OptionalDecider$ You | RememberObjects$ ReplacedCard | TriggerDescription$"
            + " At the beginning of your next upkeep, you may cast " + card.toString() + " without paying its mana cost.";
            // TODO add check for still in exile
            String abPlay = "DB$ Play | Defined$ DelayTriggerRemembered | WithoutManaCost$ True | Optional$ True";

            SpellAbility saExile = AbilityFactory.getAbility(abExile, card);

            final AbilitySub delsub = (AbilitySub) AbilityFactory.getAbility(delTrig, card);

            final AbilitySub saPlay = (AbilitySub) AbilityFactory.getAbility(abPlay, card);

            delsub.setAdditionalAbility("Execute", saPlay);

            saExile.setSubAbility(delsub);

            if (!intrinsic) {
                saExile.setIntrinsic(false);
            }

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);

            re.setOverridingAbility(saExile);

            inst.addReplacement(re);
        } else if (keyword.startsWith("Reflect:")) {
            final String[] k = keyword.split(":");

            final String repeatStr = "DB$ RepeatEach | RepeatPlayers$ Opponent";
            final String payStr = "DB$ ImmediateTrigger | RememberObjects$ Player.IsRemembered | TriggerDescription$ Copy CARDNAME | "
                    + "UnlessPayer$ Player.IsRemembered | UnlessSwitched$ True | UnlessCost$ " + k[1];
            final String copyStr = "DB$ CopyPermanent | Defined$ Self | Controller$ Player.IsRemembered | RemoveKeywords$ Reflect";

            SpellAbility repeatSA = AbilityFactory.getAbility(repeatStr, card);
            AbilitySub paySA = (AbilitySub) AbilityFactory.getAbility(payStr, card);
            AbilitySub copySA = (AbilitySub) AbilityFactory.getAbility(copyStr, card);

            repeatSA.setAdditionalAbility("RepeatSubAbility", paySA);
            paySA.setAdditionalAbility("Execute", copySA);

            ReplacementEffect cardre = createETBReplacement(card, ReplacementLayer.Other, repeatSA, false, true, intrinsic, "Card.Self", "");
            inst.addReplacement(cardre);
        } else if (keyword.startsWith("Riot")) {
            final String choose = "DB$ GenericChoice | AILogic$ Riot | SpellDescription$ Riot";

            final String counter = "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | ETB$ True | CounterNum$ 1" +
                                " | SpellDescription$ Put a +1/+1 counter on it.";
            final String haste = "DB$ Animate | Defined$ Self | Keywords$ Haste | Permanent$ True | SpellDescription$ Haste";

            SpellAbility saChoose = AbilityFactory.getAbility(choose, card);

            List<AbilitySub> list = Lists.newArrayList();
            list.add((AbilitySub)AbilityFactory.getAbility(counter, card));
            list.add((AbilitySub)AbilityFactory.getAbility(haste, card));
            saChoose.setAdditionalAbilityList("Choices", list);

            ReplacementEffect cardre = createETBReplacement(card, ReplacementLayer.Other, saChoose, false, true, intrinsic, "Card.Self", "");

            inst.addReplacement(cardre);
        } else if (keyword.startsWith("Saga")) {
            String sb = "etbCounter:LORE:1:no Condition:no desc";
            final ReplacementEffect re = makeEtbCounter(sb, card, intrinsic);

            inst.addReplacement(re);
        }  else if (keyword.equals("Sunburst")) {
            // Rule 702.43a If this object is entering the battlefield as a creature,
            // ignoring any type-changing effects that would affect it
            CounterType t = CounterType.get(host.isCreature() ? CounterEnumType.P1P1 : CounterEnumType.CHARGE);

            StringBuilder sb = new StringBuilder("etbCounter:");
            sb.append(t).append(":Sunburst:no Condition:");
            sb.append("Sunburst (").append(inst.getReminderText()).append(")");

            final ReplacementEffect re = makeEtbCounter(sb.toString(), card, intrinsic);
            re.getOverridingAbility().setSVar("Sunburst", "Count$Converge");

            inst.addReplacement(re);
        } else if (keyword.equals("Totem armor")) {
            String repeffstr = "Event$ Destroy | ActiveZones$ Battlefield | ValidCard$ Card.EnchantedBy"
                    + " | Secondary$ True | TotemArmor$ True"
                    + " | Description$ Totem armor (" + inst.getReminderText() + ")";

            String abprevDamage = "DB$ DealDamage | Defined$ ReplacedCard | Remove$ All ";
            String abdestroy = "DB$ Destroy | Defined$ Self";

            SpellAbility sa = AbilityFactory.getAbility(abprevDamage, card);

            final AbilitySub dessub = (AbilitySub) AbilityFactory.getAbility(abdestroy, card);

            sa.setSubAbility(dessub);

            if (!intrinsic) {
                sa.setIntrinsic(false);
            }

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);

            re.setOverridingAbility(sa);

            inst.addReplacement(re);
        } else if (keyword.startsWith("Tribute")) {
            final String[] k = keyword.split(":");
            final String tributeAmount = k[1];

            final String effect = "DB$ PutCounter | Defined$ ReplacedCard | Tribute$ True | "
                    + "CounterType$ P1P1 | CounterNum$ " + tributeAmount
                    + " | ETB$ True | SpellDescription$ Tribute " + tributeAmount
                    + " ("+ inst.getReminderText() + ")";

            ReplacementEffect cardre = createETBReplacement(card, ReplacementLayer.Other, effect, false, true, intrinsic, "Card.Self", "");

            inst.addReplacement(cardre);
        } else if (keyword.equals("Unleash")) {
            String effect = "DB$ PutCounter | Defined$ Self | CounterType$ P1P1 | ETB$ True | CounterNum$ 1 | SpellDescription$ Unleash (" + inst.getReminderText() + ")";

            ReplacementEffect cardre = createETBReplacement(card, ReplacementLayer.Other, effect, true, true, intrinsic, "Card.Self", "");

            inst.addReplacement(cardre);
        } else if (keyword.startsWith("Vanishing") && keyword.contains(":")) {
            // Vanishing could be added to a card, but this Effect should only be done when it has amount
            final String[] k = keyword.split(":");
            final String m = k[1];

            StringBuilder sb = new StringBuilder("etbCounter:TIME:");
            sb.append(m).append(":no Condition:");
            sb.append("Vanishing ");
            sb.append(m);
            sb.append(" (").append(inst.getReminderText()).append(")");

            final ReplacementEffect re = makeEtbCounter(sb.toString(), card, intrinsic);

            inst.addReplacement(re);
        } else if (keyword.equals("If CARDNAME would be destroyed, regenerate it.")) {
            String repeffstr = "Event$ Destroy | ActiveZones$ Battlefield | ValidCard$ Card.Self"
                    + " | Secondary$ True | Regeneration$ True | Description$ " + keyword;
            String effect = "DB$ Regeneration | Defined$ ReplacedCard";
            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);
            SpellAbility sa = AbilityFactory.getAbility(effect, card);
            re.setOverridingAbility(sa);

            inst.addReplacement(re);
        }

        // extra part for the Damage Prevention keywords
        if (keyword.startsWith("Prevent all ")) {
            // TODO add intrinsic warning

            boolean isCombat = false;
            boolean from = false;
            boolean to = false;

            if (keyword.equals("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")) {
                isCombat = from = to = true;
            } else if (keyword.equals("Prevent all combat damage that would be dealt by CARDNAME.")) {
                isCombat = from = true;
            } else if (keyword.equals("Prevent all combat damage that would be dealt to CARDNAME.")) {
                isCombat = to = true;
            } else if (keyword.equals("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
                from = to = true;
            } else if (keyword.equals("Prevent all damage that would be dealt by CARDNAME.")) {
                from = true;
            } else if (keyword.equals("Prevent all damage that would be dealt to CARDNAME.")) {
                to = true;
            }

            String rep = "Event$ DamageDone | Prevent$ True";
            if (isCombat) {
                rep += "| IsCombat$ True";
            }
            rep += "| Secondary$ True | TiedToKeyword$ " + keyword + " | Description$ " + keyword;

            if (from) {
                String fromRep = rep + " | ValidSource$ Card.Self";
                ReplacementEffect re = ReplacementHandler.parseReplacement(fromRep, host, intrinsic, card);

                inst.addReplacement(re);
            }
            if (to) {
                String toRep = rep + " | ValidTarget$ Card.Self";
                ReplacementEffect re = ReplacementHandler.parseReplacement(toRep, host, intrinsic, card);

                inst.addReplacement(re);
            }
        }

        else if (keyword.startsWith("If CARDNAME would be put into a graveyard "
                + "from anywhere, reveal CARDNAME and shuffle it into its owner's library instead.")) {

            StringBuilder sb = new StringBuilder("Event$ Moved | Destination$ Graveyard | ValidCard$ Card.Self ");

            // to show it on Nexus
            if (host.isPermanent()) {
                sb.append("| Secondary$ True");
            }
            sb.append("| Description$ ").append(keyword);

            String ab =  "DB$ ChangeZone | Hidden$ True | Origin$ All | Destination$ Library | Defined$ ReplacedCard | Reveal$ True | Shuffle$ True";

            SpellAbility sa = AbilityFactory.getAbility(ab, card);

            if (!intrinsic) {
                sa.setIntrinsic(false);
            }

            ReplacementEffect re = ReplacementHandler.parseReplacement(sb.toString(), host, intrinsic, card);

            re.setOverridingAbility(sa);

            inst.addReplacement(re);
        }

        if (keyword.equals("CARDNAME enters the battlefield tapped.") || keyword.equals("Hideaway")) {
            String effect = "DB$ Tap | Defined$ Self | ETB$ True "
                + " | SpellDescription$ CARDNAME enters the battlefield tapped.";

            final ReplacementEffect re = createETBReplacement(
                card, ReplacementLayer.Other, effect, false, true, intrinsic, "Card.Self", ""
            );

            inst.addReplacement(re);
        }

        if (keyword.startsWith("ETBReplacement")) {
            String[] splitkw = keyword.split(":");
            ReplacementLayer layer = ReplacementLayer.smartValueOf(splitkw[1]);

            final boolean optional = splitkw.length >= 4 && splitkw[3].contains("Optional");

            final String valid = splitkw.length >= 6 ? splitkw[5] : "Card.Self";
            final String zone = splitkw.length >= 5 ? splitkw[4] : "";
            final ReplacementEffect re = createETBReplacement(
                    card, layer, card.getSVar(splitkw[2]), optional, false, intrinsic, valid, zone);

            inst.addReplacement(re);
        } else if (keyword.startsWith("etbCounter")) {
            final ReplacementEffect re = makeEtbCounter(keyword, card, intrinsic);

            inst.addReplacement(re);
        }
    }

    public static void addSpellAbility(final KeywordInterface inst, final CardState card, final boolean intrinsic) {
        String keyword = inst.getOriginal();
        Card host = card.getCard();
        if (keyword.startsWith("Alternative Cost") && !host.isLand()) {
            final String[] kw = keyword.split(":");
            String costStr = kw[1];
            for (SpellAbility sa: host.getBasicSpells()) {
                final SpellAbility newSA = sa.copy();
                newSA.setBasicSpell(false);
                if (costStr.equals("ConvertedManaCost")) {
                    costStr = Integer.toString(host.getCMC());
                }
                final Cost cost = new Cost(costStr, false).add(sa.getPayCosts().copyWithNoMana());
                newSA.putParam("Secondary", "True");
                newSA.setPayCosts(cost);
                newSA.setDescription(sa.getDescription() + " (by paying " + cost.toSimpleString() + " instead of its mana cost)");
                newSA.setIntrinsic(intrinsic);

                inst.addSpellAbility(newSA);

            }
        } else if (keyword.startsWith("Adapt")) {
            final String[] k = keyword.split(":");
            final String magnitude = k[1];
            final String manacost = k[2];
            final String reduceCost = k.length > 3 ? k[3] : null;

            String desc = "Adapt " + magnitude;

            String effect = "AB$ PutCounter | Cost$ " + manacost + " | Adapt$ True | CounterNum$ " + magnitude
                    + " | CounterType$ P1P1 | StackDescription$ SpellDescription";

            if (reduceCost != null) {
                effect += "| ReduceCost$ " + reduceCost;
                desc += ". This ability costs {1} less to activate for each instant and sorcery card in your graveyard.";
            }
            effect += "| SpellDescription$ " + desc + " (" + inst.getReminderText() + ")";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.equals("Aftermath") && card.getStateName().equals(CardStateName.RightSplit)) {
            // Aftermath does modify existing SA, and does not add new one

            // only target RightSplit of it
            final SpellAbility origSA = card.getFirstSpellAbility();
            origSA.setAftermath(true);
            origSA.getRestrictions().setZone(ZoneType.Graveyard);
            // The Exile part is done by the System itself
        } else if (keyword.startsWith("Aura swap")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            final String effect = "AB$ ExchangeZone | Cost$ " + manacost + " | Zone2$ Hand | Type$ Aura "
                    + " | PrecostDesc$ Aura swap | CostDesc$ " + ManaCostParser.parse(manacost)
                    + " | StackDescription$ SpellDescription | SpellDescription$ (" + inst.getReminderText() + ")";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Awaken")) {
            final String[] k = keyword.split(":");
            final String counters = k[1];
            final Cost awakenCost = new Cost(k[2], false);

            final SpellAbility awakenSpell = card.getFirstSpellAbility().copyWithDefinedCost(awakenCost);

            final String awaken = "DB$ PutCounter | CounterType$ P1P1 | CounterNum$ "+ counters + " | "
                    + "ValidTgts$ Land.YouCtrl | TgtPrompt$ Select target land you control | Awaken$ True";
            final String animate = "DB$ Animate | Defined$ Targeted | Power$ 0 | Toughness$ 0 | Types$"
                    + " Creature,Elemental | Permanent$ True | Keywords$ Haste";

            final AbilitySub awakenSub = (AbilitySub) AbilityFactory.getAbility(awaken, card);
            final AbilitySub animateSub = (AbilitySub) AbilityFactory.getAbility(animate, card);

            awakenSub.setSubAbility(animateSub);
            awakenSpell.appendSubAbility(awakenSub);
            String desc = "Awaken " + counters + "—" + awakenCost.toSimpleString() +
                    " (" + inst.getReminderText() + ")";
            awakenSpell.setDescription(desc);
            awakenSpell.setAlternativeCost(AlternativeCost.Awaken);
            awakenSpell.setIntrinsic(intrinsic);
            inst.addSpellAbility(awakenSpell);
        } else if (keyword.startsWith("Bestow")) {
            final String[] params = keyword.split(":");
            final String cost = params[1];

            final StringBuilder sbAttach = new StringBuilder();
            sbAttach.append("SP$ Attach | Cost$ ");
            sbAttach.append(cost);
            sbAttach.append(" | AILogic$ ").append(params.length > 2 ? params[2] : "Pump");
            sbAttach.append(" | Bestow$ True | ValidTgts$ Creature");

            final SpellAbility sa = AbilityFactory.getAbility(sbAttach.toString(), card);
            sa.setDescription("Bestow " + ManaCostParser.parse(cost) +
                    " (" + inst.getReminderText() + ")");
            sa.setStackDescription("Bestow - " + card.getName());
            sa.setAlternativeCost(AlternativeCost.Bestow);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Dash")) {
            final String[] k = keyword.split(":");
            final Cost dashCost = new Cost(k[1], false);

            final SpellAbility newSA = card.getFirstSpellAbility().copyWithDefinedCost(dashCost);

            final StringBuilder desc = new StringBuilder();
            desc.append("Dash ").append(dashCost.toSimpleString()).append(" (");
            desc.append(inst.getReminderText());
            desc.append(")");

            newSA.setDescription(desc.toString());

            final StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" (Dash)");
            newSA.setStackDescription(sb.toString());

            newSA.setAlternativeCost(AlternativeCost.Dash);
            newSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(newSA);
        } else if (keyword.startsWith("Emerge")) {
            final String[] kw = keyword.split(":");
            String costStr = kw[1];
            final SpellAbility sa = card.getFirstSpellAbility();

            final SpellAbility newSA = sa.copyWithDefinedCost(new Cost(costStr, false));

            newSA.getRestrictions().setIsPresent("Creature.YouCtrl+CanBeSacrificedBy");
            newSA.putParam("Secondary", "True");
            newSA.setAlternativeCost(AlternativeCost.Emerge);

            newSA.setDescription(sa.getDescription() + " (Emerge)");
            newSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(newSA);
        } else if (keyword.startsWith("Embalm")) {
            final String[] kw = keyword.split(":");
            String costStr = kw[1];

            String effect = "AB$ CopyPermanent | Cost$ " + costStr + " ExileFromGrave<1/CARDNAME> " +
            "| ActivationZone$ Graveyard | SorcerySpeed$ True | Embalm$ True " +
            "| PrecostDesc$ Embalm | CostDesc$ " + ManaCostParser.parse(costStr) + " | Defined$ Self " +
            "| StackDescription$ Embalm - CARDNAME "+
            "| SpellDescription$ (" + inst.getReminderText() + ")" ;
            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.equals("Epic")) {
            // Epic does modify existing SA, and does not add new one

            // Add the Epic effect as a subAbility
            String dbStr = "DB$ Effect | Triggers$ EpicTrigger | StaticAbilities$ EpicCantBeCast | Duration$ Permanent | Epic$ True";

            final AbilitySub newSA = (AbilitySub) AbilityFactory.getAbility(dbStr, card);

            newSA.setSVar("EpicCantBeCast", "Mode$ CantBeCast | ValidCard$ Card | Caster$ You | EffectZone$ Command | Description$ For the rest of the game, you can't cast spells.");
            newSA.setSVar("EpicTrigger", "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | Execute$ EpicCopy | TriggerDescription$ "
                    + "At the beginning of each of your upkeeps, copy " + card.toString() + " except for its epic ability.");
            newSA.setSVar("EpicCopy", "DB$ CopySpellAbility | Defined$ EffectSource | Epic$ True | MayChooseTarget$ True");

            final SpellAbility origSA = card.getFirstSpellAbility();

            // append to original SA
            origSA.appendSubAbility(newSA);
        } else if (keyword.startsWith("Equip")) {
            if (!keyword.contains(":")) {
                System.err.println("Malformed Equip entry! - Card: " + card.toString());
                return;
            }
            String[] k = keyword.split(":");
            // Get cost string
            String equipCost = k[1];
            String valid = k.length > 2 ? k[2] : "Creature.YouCtrl";
            String vstr = k.length > 3 ? k[3] : "creature";
            // Create attach ability string
            final StringBuilder abilityStr = new StringBuilder();
            abilityStr.append("AB$ Attach | Cost$ ");
            abilityStr.append(equipCost);
            abilityStr.append("| ValidTgts$ ").append(valid);
            abilityStr.append("| TgtPrompt$ Select target ").append(vstr).append(" you control ");
            abilityStr.append("| SorcerySpeed$ True | Equip$ True | AILogic$ Pump | IsPresent$ Equipment.Self+nonCreature ");
            // add AttachAi for some special cards
            if (card.hasSVar("AttachAi")) {
                abilityStr.append("| ").append(card.getSVar("AttachAi"));
            }
            abilityStr.append("| PrecostDesc$ Equip");
            if (k.length > 3) {
                abilityStr.append(" ").append(vstr);
            }
            Cost cost = new Cost(equipCost, true);
            if (!cost.isOnlyManaCost()) { //Something other than a mana cost
                abilityStr.append("—");
            } else {
                abilityStr.append(" ");
            }
            abilityStr.append("| CostDesc$ ").append(cost.toSimpleString()).append(" ");
            abilityStr.append("| SpellDescription$ (").append(inst.getReminderText()).append(")");
            // instantiate attach ability
            final SpellAbility newSA = AbilityFactory.getAbility(abilityStr.toString(), card);
            newSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(newSA);
        } else if (keyword.startsWith("Eternalize")) {
            final String[] kw = keyword.split(":");
            String costStr = kw[1];
            Cost cost = new Cost(costStr, true);

            StringBuilder sb = new StringBuilder();

            sb.append("AB$ CopyPermanent | Cost$ ").append(costStr).append(" ExileFromGrave<1/CARDNAME>")
            .append(" | Defined$ Self | ActivationZone$ Graveyard | SorcerySpeed$ True | Eternalize$ True");

            sb.append(" | PrecostDesc$ Eternalize");
            if (!cost.isOnlyManaCost()) { //Something other than a mana cost
                sb.append("—");
            } else {
                sb.append(" ");
            }
            // don't use SimpleString there because it does has "and" between cost i dont want that
            costStr = cost.toString();
            // but now it has ": " at the end i want to remove
            sb.append("| CostDesc$ ").append(costStr, 0, costStr.length() - 2);
            if (!cost.isOnlyManaCost()) {
                sb.append(".");
            }

            sb.append(" | StackDescription$ Eternalize - CARDNAME ")
            .append("| SpellDescription$ (").append(inst.getReminderText()).append(")");
            final SpellAbility sa = AbilityFactory.getAbility(sb.toString(), card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Evoke")) {
            final String[] k = keyword.split(":");
            final Cost evokedCost = new Cost(k[1], false);
            final SpellAbility sa = card.getFirstSpellAbility();

            final SpellAbility newSA = sa.copyWithDefinedCost(evokedCost);

            final StringBuilder desc = new StringBuilder();
            desc.append("Evoke ").append(evokedCost.toSimpleString()).append(" (");
            desc.append(inst.getReminderText());
            desc.append(")");

            newSA.setDescription(desc.toString());

            final StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" (Evoked)");
            newSA.setStackDescription(sb.toString());
            newSA.setAlternativeCost(AlternativeCost.Evoke);
            newSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(newSA);
        } else if (keyword.startsWith("Foretell")) {

            final SpellAbility foretell = new AbilityStatic(card.getCard(), new Cost(ManaCost.TWO, false), null) {
                @Override
                public boolean canPlay() {
                    if (!getRestrictions().canPlay(getHostCard(), this)) {
                        return false;
                    }

                    Player activator = this.getActivatingPlayer();
                    final Game game = activator.getGame();

                    if (!activator.hasKeyword("Foretell on any player’s turn") && !game.getPhaseHandler().isPlayerTurn(activator)) {
                        return false;
                    }

                    return true;
                }

                @Override
                public boolean isForetelling() {
                    return true;
                }

                @Override
                public void resolve() {
                    final Game game = getHostCard().getGame();
                    final Card c = game.getAction().exile(getHostCard(), this);
                    c.setForetold(true);
                    c.setForetoldThisTurn(true);
                    c.turnFaceDown(true);
                    // look at the exiled card
                    c.addMayLookTemp(getActivatingPlayer());

                    // only done when the card is foretold by the static ability
                    getActivatingPlayer().addForetoldThisTurn();

                    if (!isIntrinsic()) {
                        // because it doesn't work other wise
                        c.setForetoldByEffect(true);
                    }
                    String sb = TextUtil.concatWithSpace(getActivatingPlayer().toString(),"has foretold.");
                    game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                }
            };
            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("Foretell (").append(inst.getReminderText()).append(")");
            foretell.setDescription(sbDesc.toString());
            foretell.putParam("Secondary", "True");

            foretell.setCardState(card);

            foretell.getRestrictions().setZone(ZoneType.Hand);
            foretell.setIntrinsic(intrinsic);
            inst.addSpellAbility(foretell);

        } else if (keyword.startsWith("Fortify")) {
            String[] k = keyword.split(":");
            // Get cost string
            String equipCost = k[1];
            // Create attach ability string
            final StringBuilder abilityStr = new StringBuilder();
            abilityStr.append("AB$ Attach | Cost$ ");
            abilityStr.append(equipCost);
            abilityStr.append(" | ValidTgts$ Land.YouCtrl | TgtPrompt$ Select target land you control ");
            abilityStr.append("| SorcerySpeed$ True | AILogic$ Pump | IsPresent$ Fortification.Self+nonCreature ");
            abilityStr.append("| PrecostDesc$ Fortify");
            Cost cost = new Cost(equipCost, true);
            abilityStr.append(cost.isOnlyManaCost() ? " " : "—");
            abilityStr.append("| CostDesc$ ").append(cost.toSimpleString()).append(" ");
            abilityStr.append("| SpellDescription$ (");
            abilityStr.append(inst.getReminderText()).append(")");

            // instantiate attach ability
            final SpellAbility sa = AbilityFactory.getAbility(abilityStr.toString(), card);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Fuse") && card.getStateName().equals(CardStateName.Original)) {
            final SpellAbility sa = AbilityFactory.buildFusedAbility(card.getCard());
            card.addSpellAbility(sa);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Haunt")) {
            if (!host.isCreature() && intrinsic) {
                final String[] k = keyword.split(":");
                final String hauntSVarName = k[1];

                // no nice way to get the cost
                String abString = TextUtil.concatNoSpace(
                        TextUtil.fastReplace(card.getSVar(hauntSVarName), "DB$", "SP$"),
                        " | Cost$ 0 | StackDescription$ SpellDescription"
                );
                final SpellAbility sa = AbilityFactory.getAbility(abString, card);
                sa.setPayCosts(new Cost(card.getManaCost(), false));
                sa.setIntrinsic(intrinsic);
                inst.addSpellAbility(sa);
            }
        } else if (keyword.startsWith("Level up")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            StringBuilder sb = new StringBuilder();
            sb.append("AB$ PutCounter| Cost$ ").append(manacost);
            sb.append(" | PrecostDesc$ Level Up | CostDesc$ ").append(ManaCostParser.parse(manacost));
            sb.append(" | SorcerySpeed$ True | LevelUp$ True | CounterNum$ 1 | CounterType$ LEVEL");
            if (card.hasSVar("maxLevel")) {
                final String strMaxLevel = card.getSVar("maxLevel");
                sb.append("| MaxLevel$ ").append(strMaxLevel);
            }
            sb.append(" | SpellDescription$ (").append(inst.getReminderText()).append(")");

            final SpellAbility sa = AbilityFactory.getAbility(sb.toString(), card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Monstrosity")) {
            final String[] k = keyword.split(":");
            final String magnitude = k[1];
            final String manacost = k[2];

            final String reduceCost = k.length > 3 ? k[3] : null;

            String desc = "Monstrosity " + magnitude;

            String effect = "AB$ PutCounter | Cost$ " + manacost + " | ConditionPresent$ "
                    + "Card.Self+IsNotMonstrous | Monstrosity$ True | CounterNum$ " + magnitude
                    + " | CounterType$ P1P1 | StackDescription$ SpellDescription";
            if (reduceCost != null) {
                effect += "| ReduceCost$ " + reduceCost;
                desc += ". This ability costs {1} less to activate for each creature card in your graveyard.";
            }

            if (card.hasSVar("MonstrosityAILogic")) {
                effect += "| AILogic$ " + card.getSVar("MonstrosityAILogic");
            }

            effect += "| SpellDescription$ " + desc + " (" + inst.getReminderText() + ")";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);

        } else if (keyword.startsWith("Morph")) {
            final String[] k = keyword.split(":");

            inst.addSpellAbility(abilityMorphDown(card));
            inst.addSpellAbility(abilityMorphUp(card, k[1], false));
        } else if (keyword.startsWith("Megamorph")){
            final String[] k = keyword.split(":");

            inst.addSpellAbility(abilityMorphDown(card));
            inst.addSpellAbility(abilityMorphUp(card, k[1], true));
        } else if (keyword.startsWith("Multikicker")) {
            final String[] n = keyword.split(":");
            final SpellAbility sa = card.getFirstSpellAbility();
            sa.setMultiKickerManaCost(new ManaCost(new ManaCostParser(n[1])));
            if (keyword.endsWith("Generic")) {
                sa.addAnnounceVar("Pseudo-multikicker");
            } else {
                sa.addAnnounceVar("Multikicker");
            }
        } else if (keyword.startsWith("Mutate")) {
            final String[] params = keyword.split(":");
            final String cost = params[1];

            final StringBuilder sbMutate = new StringBuilder();
            sbMutate.append("SP$ Mutate | Cost$ ");
            sbMutate.append(cost);
            sbMutate.append(" | ValidTgts$ Creature.sharesOwnerWith+nonHuman");

            final SpellAbility sa = AbilityFactory.getAbility(sbMutate.toString(), card);
            sa.setDescription("Mutate " + ManaCostParser.parse(cost) +
                    " (" + inst.getReminderText() + ")");
            sa.setStackDescription("Mutate - " + card.getName());
            sa.setAlternativeCost(AlternativeCost.Mutate);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Ninjutsu")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            String desc = "Ninjutsu";
            boolean commander = false;
            if (k.length > 2 && k[2].equals("Commander")) {
                desc = "Commander " + desc;
                commander = true;
            }

            String effect = "AB$ ChangeZone | Cost$ " + manacost +
                    " Return<1/Creature.attacking+unblocked/unblocked attacker> " +
                    "| PrecostDesc$ " + desc + " | CostDesc$ " + ManaCostParser.parse(manacost) +
                    "| ActivationZone$ Hand | Origin$ Hand | Ninjutsu$ True " +
                    "| Destination$ Battlefield | Defined$ Self " +
                    "| SpellDescription$ (" + inst.getReminderText() + ")";

            SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);

            // extra secondary effect for Commander Ninjutsu
            if (commander) {
                effect = "AB$ ChangeZone | Cost$ " + manacost +
                        " Return<1/Creature.attacking+unblocked/unblocked attacker> " +
                        "| PrecostDesc$ " + desc + " | CostDesc$ " + ManaCostParser.parse(manacost) +
                        "| ActivationZone$ Command | Origin$ Command | Ninjutsu$ True " +
                        "| Destination$ Battlefield | Defined$ Self | Secondary$ True " +
                        "| SpellDescription$ (" + inst.getReminderText() + ")";

                sa = AbilityFactory.getAbility(effect, card);
                sa.setIntrinsic(intrinsic);
                inst.addSpellAbility(sa);
            }
        } else if (keyword.startsWith("Outlast")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            // Create outlast ability string
            final StringBuilder abilityStr = new StringBuilder();
            abilityStr.append("AB$ PutCounter | Cost$ ");
            abilityStr.append(manacost);
            abilityStr.append(" T | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1 ");
            abilityStr.append("| SorcerySpeed$ True | PrecostDesc$ Outlast");
            Cost cost = new Cost(manacost, true);
            if (!cost.isOnlyManaCost()) { //Something other than a mana cost
                abilityStr.append("—");
            } else {
                abilityStr.append(" ");
            }
            abilityStr.append("| CostDesc$ ").append(cost.toSimpleString()).append(" ");
            abilityStr.append("| SpellDescription$ (").append(inst.getReminderText()).append(")");

            final SpellAbility sa = AbilityFactory.getAbility(abilityStr.toString(), card);
            sa.setIntrinsic(intrinsic);
            sa.setAlternativeCost(AlternativeCost.Outlast);
            inst.addSpellAbility(sa);

        } else if (keyword.startsWith("Prowl")) {
            final String[] k = keyword.split(":");
            final Cost prowlCost = new Cost(k[1], false);
            final SpellAbility newSA = card.getFirstSpellAbility().copyWithDefinedCost(prowlCost);

            if (host.isInstant() || host.isSorcery()) {
                newSA.putParam("Secondary", "True");
            }
            newSA.putParam("PrecostDesc", "Prowl");
            newSA.putParam("CostDesc", ManaCostParser.parse(k[1]));

            // makes new SpellDescription
            final StringBuilder sb = new StringBuilder();
            sb.append(newSA.getCostDescription());
            sb.append("(").append(inst.getReminderText()).append(")");
            newSA.setDescription(sb.toString());

            newSA.setAlternativeCost(AlternativeCost.Prowl);

            newSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(newSA);
        } else if (keyword.startsWith("Reinforce")) {
            final String[] k = keyword.split(":");
            final String n = k[1];
            final String manacost = k[2];

            StringBuilder sb = new StringBuilder();
            sb.append("AB$ PutCounter | CounterType$ P1P1 | ActivationZone$ Hand");
            sb.append("| ValidTgts$ Creature | TgtPrompt$ Select target creature");
            sb.append("| Cost$ ").append(manacost).append(" Discard<1/CARDNAME>");
            sb.append("| CounterNum$ ").append(n);
            sb.append("| CostDesc$ ").append(ManaCostParser.parse(manacost)); // to hide the Discard from the cost
            sb.append("| PrecostDesc$ Reinforce ").append(n).append("—");
            sb.append("| SpellDescription$ (").append(inst.getReminderText()).append(")");

            final SpellAbility sa = AbilityFactory.getAbility(sb.toString(), card);
            sa.setIntrinsic(intrinsic);

            if (n.equals("X")) {
                sa.setSVar("X", "Count$xPaid");
            }
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Scavenge")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            String effect = "AB$ PutCounter | Cost$ " + manacost + " ExileFromGrave<1/CARDNAME> " +
                    "| ActivationZone$ Graveyard | ValidTgts$ Creature | CounterType$ P1P1 " +
                    "| CounterNum$ ScavengeX | SorcerySpeed$ True " +
                    "| PrecostDesc$ Scavenge | CostDesc$ " + ManaCostParser.parse(manacost) +
                    "| SpellDescription$ (" + inst.getReminderText() + ")";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setSVar("ScavengeX", "Count$CardPower");
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);

        } else if (keyword.startsWith("Encore")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            String effect = "AB$ RepeatEach | Cost$ " + manacost + " ExileFromGrave<1/CARDNAME> " +
                    "| ActivationZone$ Graveyard | ClearRememberedBeforeLoop$ True | RepeatPlayers$ Opponent" +
                    "| PrecostDesc$ Encore | CostDesc$ " + ManaCostParser.parse(manacost) +
                    "| SpellDescription$ (" + inst.getReminderText() + ")";

            final String copyStr = "DB$ CopyPermanent | Defined$ Self | ImprintTokens$ True " +
                    "| AddKeywords$ Haste | RememberTokens$ True | TokenRemembered$ Player.IsRemembered";

            final String pumpStr = "DB$ PumpAll | ValidCards$ Creature.IsRemembered " +
                    "| KW$ HIDDEN CARDNAME attacks specific player each combat if able:Remembered";

            final String pumpcleanStr = "DB$ Cleanup | ForgetDefined$ RememberedCard";

            final String delTrigStr = "DB$ DelayedTrigger | Mode$ Phase | Phase$ End of Turn | RememberObjects$ Imprinted " +
                    "| StackDescription$ None | TriggerDescription$ Sacrifice them at the beginning of the next end step.";

            final String sacStr = "DB$ SacrificeAll | Defined$ DelayTriggerRememberedLKI";

            final String cleanupStr = "DB$ Cleanup | ClearRemembered$ True | ClearImprinted$ True";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);

            AbilitySub copySA = (AbilitySub) AbilityFactory.getAbility(copyStr, card);
            sa.setAdditionalAbility("RepeatSubAbility", copySA);

            AbilitySub pumpSA = (AbilitySub) AbilityFactory.getAbility(pumpStr, card);
            copySA.setSubAbility(pumpSA);

            AbilitySub pumpcleanSA = (AbilitySub) AbilityFactory.getAbility(pumpcleanStr, card);
            pumpSA.setSubAbility(pumpcleanSA);

            AbilitySub delTrigSA = (AbilitySub) AbilityFactory.getAbility(delTrigStr, card);
            sa.setSubAbility(delTrigSA);

            AbilitySub sacSA = (AbilitySub) AbilityFactory.getAbility(sacStr, card);
            delTrigSA.setAdditionalAbility("Execute", sacSA);

            AbilitySub cleanupSA = (AbilitySub) AbilityFactory.getAbility(cleanupStr, card);
            delTrigSA.setSubAbility(cleanupSA);

        } else if (keyword.startsWith("Spectacle")) {
            final String[] k = keyword.split(":");
            final Cost cost = new Cost(k[1], false);
            final SpellAbility newSA = card.getFirstSpellAbility().copyWithDefinedCost(cost);

            newSA.setAlternativeCost(AlternativeCost.Spectacle);

            String desc = "Spectacle " + cost.toSimpleString() + " (" + inst.getReminderText()
                    + ")";
            newSA.setDescription(desc);

            newSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(newSA);

        } else if (keyword.startsWith("Surge")) {
            final String[] k = keyword.split(":");
            final Cost surgeCost = new Cost(k[1], false);
            final SpellAbility newSA = card.getFirstSpellAbility().copyWithDefinedCost(surgeCost);

            newSA.setAlternativeCost(AlternativeCost.Surge);

            String desc = "Surge " + surgeCost.toSimpleString() + " (" + inst.getReminderText()
                    + ")";
            newSA.setDescription(desc);

            newSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(newSA);

        } else if (keyword.startsWith("Suspend") && !keyword.equals("Suspend")) {
            // only add it if suspend has counter and cost
            final String[] k = keyword.split(":");

            // be careful with Suspend ability, it will not hit the stack
            Cost cost = new Cost(k[2], true);
            final SpellAbility suspend = new AbilityStatic(host, cost, null) {
                @Override
                public boolean canPlay() {
                    if (!(this.getRestrictions().canPlay(this.getHostCard(), this))) {
                        return false;
                    }

                    if (StaticAbilityCantBeCast.cantBeCastAbility(this, this.getHostCard(), this.getActivatingPlayer())) {
                        return false;
                    }

                    if (this.getHostCard().isInstant() || this.getHostCard().hasKeyword(Keyword.FLASH)) {
                        return true;
                    }

                    return this.getHostCard().getOwner().canCastSorcery();
                }

                @Override
                public void resolve() {
                    final Game game = this.getHostCard().getGame();
                    final Card c = game.getAction().exile(this.getHostCard(), this);

                    int counters = AbilityUtils.calculateAmount(c, k[1], this);
                    GameEntityCounterTable table = new GameEntityCounterTable();
                    c.addCounter(CounterEnumType.TIME, counters, getActivatingPlayer(), true, table);
                    table.triggerCountersPutAll(game);

                    String sb = TextUtil.concatWithSpace(getActivatingPlayer().toString(),"has suspended", c.getName(), "with", String.valueOf(counters),"time counters on it.");
                    game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                    //reveal suspended card
                    game.getAction().reveal(new CardCollection(c), c.getOwner(), true, c.getName() + " is suspended with " + counters + " time counters in ");
                }
            };
            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("Suspend ").append(k[1]).append("—").append(cost.toSimpleString());
            sbDesc.append(" (").append(inst.getReminderText()).append(")");
            suspend.setDescription(sbDesc.toString());

            String svar = "X"; // emulate "References X" here
            suspend.setSVar(svar, card.getSVar(svar));
            suspend.setCardState(card);

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(card.getName()).append(" suspending for ");
            sbStack.append(Lang.nounWithNumeral(k[1], "turn")).append(".)");
            suspend.setStackDescription(sbStack.toString());

            suspend.getRestrictions().setZone(ZoneType.Hand);
            inst.addSpellAbility(suspend);
        } else if (keyword.startsWith("Transfigure")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];
            final String effect = "AB$ ChangeZone | Cost$ " + manacost + " Sac<1/CARDNAME>"
                    + " | PrecostDesc$ Transfigure | CostDesc$ " + ManaCostParser.parse(manacost)
                    + " | Origin$ Library | Destination$ Battlefield | ChangeType$ Creature.cmcEQTransfigureX"
                    + " | ChangeNum$ 1 | SorcerySpeed$ True | StackDescription$ SpellDescription | SpellDescription$ ("
                    + inst.getReminderText() + ")";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setSVar("TransfigureX", "Count$CardManaCost");
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Transmute")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            final String effect = "AB$ ChangeZone | Cost$ " + manacost + " Discard<1/CARDNAME>"
                    + " | PrecostDesc$ Transmute | CostDesc$ " + ManaCostParser.parse(manacost) + " | ActivationZone$ Hand"
                    + " | Origin$ Library | Destination$ Hand | ChangeType$ Card.cmcEQTransmuteX"
                    + " | ChangeNum$ 1 | SorcerySpeed$ True | StackDescription$ SpellDescription | SpellDescription$ ("
                    + inst.getReminderText() + ")";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setSVar("TransmuteX", "Count$CardManaCost");
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Unearth")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            String effect = "AB$ ChangeZone | Cost$ " + manacost + " | Defined$ Self" +
                    " | Origin$ Graveyard | Destination$ Battlefield | SorcerySpeed$ True" +
                    " | ActivationZone$ Graveyard | Unearth$ True | " +
                    " | PrecostDesc$ Unearth | StackDescription$ " +
                    "Unearth: Return CARDNAME to the battlefield. | SpellDescription$" +
                    " (" + inst.getReminderText() + ")";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);

        } else if (keyword.endsWith(" offering")) {
            final String offeringType = keyword.split(" ")[0];
            final SpellAbility sa = card.getFirstSpellAbility();

            final SpellAbility newSA = sa.copy();

            SpellAbilityRestriction sar = newSA.getRestrictions();
            sar.setIsPresent(offeringType + ".YouCtrl+CanBeSacrificedBy");
            sar.setInstantSpeed(true);

            newSA.putParam("Secondary", "True");
            newSA.setAlternativeCost(AlternativeCost.Offering);
            newSA.setPayCosts(sa.getPayCosts());
            newSA.setDescription(sa.getDescription() + " (" + offeringType + " offering)");
            newSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(newSA);

        } else if (keyword.startsWith("Crew")) {
            final String[] k = keyword.split(":");
            final String power = k[1];

            // tapXType has a special check for withTotalPower, and NEEDS it to be "+withTotalPowerGE"
            // So adding redundant YouCtrl to simplify matters even though its unnecessary
            String effect = "AB$ Animate | Cost$ tapXType<Any/Creature.YouCtrl+withTotalPowerGE" + power +
                    "> | CostDesc$ Crew " + power + " (Tap any number of creatures you control with total power " + power +
                    " or more: | Crew$ True | Secondary$ True | Defined$ Self | Types$ Creature,Artifact | RemoveCardTypes$ True" +
                    " | SpellDescription$ CARDNAME becomes an artifact creature until end of turn.)";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);

        } else if (keyword.startsWith("Cycling")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];
            final Cost cost = new Cost(manacost, true);

            StringBuilder sb = new StringBuilder();
            sb.append("AB$ Draw | Cost$ ");
            sb.append(manacost);
            sb.append(" Discard<1/CARDNAME> | ActivationZone$ Hand | PrecostDesc$ Cycling");
            sb.append(cost.isOnlyManaCost() ? " " : "—");
            sb.append("| CostDesc$ ").append(cost.toSimpleString()).append(" ");
            sb.append("| SpellDescription$ (").append(inst.getReminderText()).append(")");

            SpellAbility sa = AbilityFactory.getAbility(sb.toString(), card);
            sa.setAlternativeCost(AlternativeCost.Cycling);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);

        } else if (keyword.startsWith("TypeCycling")) {
            final String[] k = keyword.split(":");
            final String type = k[1];
            final String manacost = k[2];

            StringBuilder sb = new StringBuilder();
            sb.append("AB$ ChangeZone | Cost$ ").append(manacost);

            String desc = type;
            if (type.equals("Basic")) {
                desc = "Basic land";
            }

            sb.append(" Discard<1/CARDNAME> | ActivationZone$ Hand | PrecostDesc$ ").append(desc).append("cycling ");
            sb.append(" | CostDesc$ ").append(ManaCostParser.parse(manacost));
            sb.append("| Origin$ Library | Destination$ Hand |");
            sb.append("ChangeType$ ").append(type);
            sb.append(" | SpellDescription$ (").append(inst.getReminderText()).append(")");

            SpellAbility sa = AbilityFactory.getAbility(sb.toString(), card);
            sa.setAlternativeCost(AlternativeCost.Cycling);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);

        }
    }

    public static void addStaticAbility(final KeywordInterface inst, final CardState state, final boolean intrinsic) {
        String keyword = inst.getOriginal();
        String effect = null;
        Map<String, String> svars = Maps.newHashMap();

        if (keyword.startsWith("Affinity")) {
            final String[] k = keyword.split(":");
            final String t = k[1];

            String desc = "Artifact".equals(t) ? "artifacts" : CardType.getPluralType(t);
            StringBuilder sb = new StringBuilder();
            sb.append("Mode$ ReduceCost | ValidCard$ Card.Self | Type$ Spell | Amount$ AffinityX | EffectZone$ All");
            sb.append("| Description$ Affinity for ").append(desc);
            sb.append(" (").append(inst.getReminderText()).append(")");
            effect = sb.toString();

            svars.put("AffinityX", "Count$Valid " + t + ".YouCtrl");
        } else if (keyword.equals("Changeling")) {
            effect = "Mode$ Continuous | EffectZone$ All | Affected$ Card.Self" +
                    " | CharacteristicDefining$ True | AddType$ AllCreatureTypes | Secondary$ True" +
                    " | Description$ Changeling (" + inst.getReminderText() + ")";
        } else if (keyword.equals("Cipher")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Mode$ Continuous | EffectZone$ Exile | Affected$ Card.EncodedWithSource");
            sb.append(" | AddTrigger$ CipherTrigger");
            sb.append(" | Description$ Cipher (").append(inst.getReminderText()).append(")");

            effect = sb.toString();

            sb = new StringBuilder();

            sb.append("Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player | Execute$ PlayEncoded");
            sb.append(" | CombatDamage$ True | OptionalDecider$ You | TriggerDescription$ ");
            sb.append("Whenever CARDNAME deals combat damage to a player, its controller may cast a copy of ");
            sb.append(state.getName()).append(" without paying its mana cost.");

            String trig = sb.toString();

            String ab = "DB$ Play | Defined$ OriginalHost | WithoutManaCost$ True | CopyCard$ True";

            svars.put("CipherTrigger", trig);
            svars.put("PlayEncoded", ab);
        } else if (keyword.startsWith("Dash")) {
            effect = "Mode$ Continuous | Affected$ Card.Self+dashed | AddKeyword$ Haste";
        } else if (keyword.equals("Devoid")) {
            effect = "Mode$ Continuous | EffectZone$ All | Affected$ Card.Self" +
                    " | CharacteristicDefining$ True | SetColor$ Colorless | Secondary$ True" +
                    " | Description$ Devoid (" + inst.getReminderText() + ")";
        } else if (keyword.startsWith("Escalate")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];
            final Cost cost = new Cost(manacost, false);

            StringBuilder sb = new StringBuilder("Escalate");
            if (!cost.isOnlyManaCost()) {
                sb.append("—");
            } else {
                sb.append(" ");
            }
            sb.append(cost.toSimpleString());

            effect = "Mode$ RaiseCost | ValidCard$ Card.Self | Type$ Spell | Secondary$ True"
                    + " | Amount$ Escalate | Cost$ "+ manacost +" | EffectZone$ All"
                    + " | Description$ " + sb.toString() + " (" + inst.getReminderText() + ")";
        } else if (keyword.startsWith("Hexproof")) {
            final StringBuilder sbDesc = new StringBuilder("Hexproof");
            final StringBuilder sbValid = new StringBuilder();

            if (!keyword.equals("Hexproof")) {
                final String[] k = keyword.split(":");

                sbDesc.append(" from ").append(k[2]);
                sbValid.append("| ValidSource$ ").append(k[1]);
            }

            effect = "Mode$ CantTarget | Hexproof$ True | ValidCard$ Card.Self | Secondary$ True"
                    + sbValid.toString() + " | Activator$ Opponent | Description$ "
                    + sbDesc.toString() + " (" + inst.getReminderText() + ")";
        } else if (keyword.equals("Shroud")) {
            effect = "Mode$ CantTarget | Shroud$ True | ValidCard$ Card.Self | Secondary$ True"
                    + " | Description$ Shroud (" + inst.getReminderText() + ")";
        } else if (keyword.startsWith("Strive")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            effect = "Mode$ RaiseCost | ValidCard$ Card.Self | Type$ Spell | Amount$ Strive | Cost$ "+ manacost +" | EffectZone$ All" +
                    " | Description$ Strive - " + inst.getReminderText();
        } else if (keyword.equals("Unleash")) {
            effect = "Mode$ Continuous | Affected$ Card.Self+counters_GE1_P1P1 | AddHiddenKeyword$ CARDNAME can't block.";
        } else if (keyword.equals("Undaunted")) {
            effect = "Mode$ ReduceCost | ValidCard$ Card.Self | Type$ Spell | Secondary$ True"
                    + "| Amount$ Undaunted | EffectZone$ All | Description$ Undaunted (" + inst.getReminderText() + ")";
        } else if (keyword.startsWith("CantBeBlockedBy ")) {
            final String[] k = keyword.split(" ", 2);

            effect = "Mode$ CantBlockBy | ValidAttacker$ Creature.Self | ValidBlocker$ " + k[1]
                    + " | Description$ CARDNAME can't be blocked " + getTextForKwCantBeBlockedByType(keyword);
        } else if (keyword.equals("MayFlashSac")) {
            effect = "Mode$ Continuous | EffectZone$ All | Affected$ Card.Self | Secondary$ True | MayPlay$ True"
                + " | MayPlayNotSorcerySpeed$ True | MayPlayWithFlash$ True | MayPlayText$ Sacrifice at the next cleanup step"
                + " | AffectedZone$ Exile,Graveyard,Hand,Library,Stack | Description$ " + inst.getReminderText();
        }

        if (effect != null) {
            StaticAbility st = new StaticAbility(effect, state.getCard(), state);
            st.setIntrinsic(intrinsic);
            for (Map.Entry<String, String> e : svars.entrySet()) {
                st.setSVar(e.getKey(), e.getValue());
            }
            inst.addStaticAbility(st);
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param altCost
     * @param sa
     * @return
     */
    private static SpellAbility makeAltCostAbility(final Card card, final String altCost, final SpellAbility sa) {
        final Map<String, String> params = AbilityFactory.getMapParams(altCost);

        final SpellAbility altCostSA = sa.copy();
        final Cost abCost = new Cost(params.get("Cost"), altCostSA.isAbility());
        altCostSA.setPayCosts(abCost);
        altCostSA.setBasicSpell(false);
        altCostSA.addOptionalCost(OptionalCost.AltCost);

        final SpellAbilityRestriction restriction = new SpellAbilityRestriction();
        restriction.setRestrictions(params);
        if (!params.containsKey("ActivationZone")) {
            restriction.setZone(ZoneType.Hand);
        }
        altCostSA.setRestrictions(restriction);

        String costDescription = TextUtil.fastReplace(params.get("Description"),"CARDNAME", card.getName());
        if (costDescription == null || costDescription.isEmpty()) {
            costDescription = TextUtil.concatWithSpace("You may", abCost.toStringAlt(), "rather than pay", TextUtil.addSuffix(card.getName(), "'s mana cost."));
        }

        altCostSA.setDescription(costDescription);

        if (params.containsKey("StackDescription")) {
            altCostSA.setStackDescription(params.get("StackDescription"));
        }

        return altCostSA;
    }

    private static final Map<String,String> emptyMap = Maps.newTreeMap();
    public static SpellAbility setupETBReplacementAbility(SpellAbility sa) {
        AbilitySub as = new AbilitySub(ApiType.InternalEtbReplacement, sa.getHostCard(), null, emptyMap);
        sa.appendSubAbility(as);
        return as;
        // ETBReplacementMove(sa.getHostCard(), null));
    }

    private static String getTextForKwCantBeBlockedByType(final String keyword) {
        boolean negative = true;
        final List<String> subs = Lists.newArrayList(TextUtil.split(keyword.split(" ", 2)[1], ','));
        final List<List<String>> subsAnd = Lists.newArrayList();
        final List<String> orClauses = Lists.newArrayList();
        for (final String expession : subs) {
            final List<String> parts = Lists.newArrayList(expession.split("[.+]"));
            for (int p = 0; p < parts.size(); p++) {
                final String part = parts.get(p);
                if (part.equalsIgnoreCase("creature")) {
                    parts.remove(p--);
                    continue;
                }
                // based on suppossition that each expression has at least 1 predicate except 'creature'
                negative &= part.contains("non") || part.contains("without");
            }
            subsAnd.add(parts);
        }

        final boolean allNegative = negative;
        final String byClause = allNegative ? "except by " : "by ";

        final Function<Pair<Boolean, String>, String> withToString = new Function<Pair<Boolean, String>, String>() {
            @Override
            public String apply(Pair<Boolean, String> inp) {
                boolean useNon = inp.getKey() == allNegative;
                return (useNon ? "*NO* " : "") + inp.getRight();
            }
        };

        for (final List<String> andOperands : subsAnd) {
            final List<Pair<Boolean, String>> prependedAdjectives = Lists.newArrayList();
            final List<Pair<Boolean, String>> postponedAdjectives = Lists.newArrayList();
            String creatures = null;

            for (String part : andOperands) {
                boolean positive = true;
                if (part.startsWith("non")) {
                    part = part.substring(3);
                    positive = false;
                }
                if (part.startsWith("with")) {
                    positive = !part.startsWith("without");
                    postponedAdjectives.add(Pair.of(positive, part.substring(positive ? 4 : 7)));
                } else if (part.startsWith("powerLEX")) {// Kraken of the Straits
                    postponedAdjectives.add(Pair.of(true, "power less than the number of islands you control"));
                } else if (part.startsWith("power")) {
                    int kwLength = 5;
                    String opName = Expressions.operatorName(part.substring(kwLength, kwLength + 2));
                    String operand = part.substring(kwLength + 2);
                    postponedAdjectives.add(Pair.of(true, "power" + opName + operand));
                } else if (part.startsWith("toughness")) {
                    int kwLength = 9;
                    String operand = part.substring(kwLength + 2);
                    String opName = "";
                    if (part.startsWith("toughnessGE")) {
                        opName = " or greater";
                    } else {
                        opName = "update CardFactoryUtil line 4773";
                    }
                    postponedAdjectives.add(Pair.of(true, "toughness " + operand + opName));
                } else if (CardType.isACreatureType(part)) {
                    if (creatures != null && CardType.isACreatureType(creatures)) { // e.g. Kor Castigator
                        creatures = StringUtils.capitalize(Lang.getPlural(part)) + creatures;
                    } else {
                        creatures = StringUtils.capitalize(Lang.getPlural(part)) + (creatures == null ? "" : " or " + creatures);
                    }
                    // Kor Castigator and other similar creatures with composite subtype Eldrazi Scion in their text
                    creatures = TextUtil.fastReplace(creatures, "Scions or Eldrazis", "Eldrazi Scions");
                } else {
                    prependedAdjectives.add(Pair.of(positive, part.toLowerCase()));
                }
            }

            StringBuilder sbShort = new StringBuilder();
            if (allNegative) {
                boolean isFirst = true;
                for (Pair<Boolean, String> pre : prependedAdjectives) {
                    if (isFirst) isFirst = false;
                    else sbShort.append(" and/or ");

                    boolean useNon = pre.getKey() == allNegative;
                    if (useNon) sbShort.append("non-");
                    sbShort.append(pre.getValue()).append(" ").append(creatures == null ? "creatures" : creatures);
                }
                if (prependedAdjectives.isEmpty())
                    sbShort.append(creatures == null ? "creatures" : creatures);

                if (!postponedAdjectives.isEmpty()) {
                    if (!prependedAdjectives.isEmpty()) {
                        sbShort.append(" and/or creatures");
                    }

                    sbShort.append(" with ");
                    sbShort.append(Lang.joinHomogenous(postponedAdjectives, withToString, allNegative ? "or" : "and"));
                }

            } else {
                for (Pair<Boolean, String> pre : prependedAdjectives) {
                    boolean useNon = pre.getKey() == allNegative;
                    if (useNon) sbShort.append("non-");
                    sbShort.append(pre.getValue()).append(" ");
                }
                sbShort.append(creatures == null ? "creatures" : creatures);

                if (!postponedAdjectives.isEmpty()) {
                    sbShort.append(" with ");
                    sbShort.append(Lang.joinHomogenous(postponedAdjectives, withToString, allNegative ? "or" : "and"));
                }

            }
            orClauses.add(sbShort.toString());
        }
        return byClause + StringUtils.join(orClauses, " or ") + ".";
    }

    public static void setupAdventureAbility(Card card) {
        if (card.getCurrentStateName() != CardStateName.Adventure) {
            return;
        }
        SpellAbility sa = card.getFirstSpellAbility();
        if (sa == null) {
            return;
        }
        sa.setCardState(card.getCurrentState());

        StringBuilder sb = new StringBuilder();
        sb.append("Event$ Moved | ValidCard$ Card.Self | Origin$ Stack | ExcludeDestination$ Exile ");
        sb.append("| ValidStackSa$ Spell.Adventure | Fizzle$ False | Secondary$ True | Description$ Adventure");

        String repeffstr = sb.toString();

        String abExile = "DB$ ChangeZone | Defined$ Self | Origin$ Stack | Destination$ Exile | StackDescription$ None";

        SpellAbility saExile = AbilityFactory.getAbility(abExile, card);

        String abEffect = "DB$ Effect | RememberObjects$ Self | StaticAbilities$ Play | ExileOnMoved$ Exile | Duration$ Permanent | ConditionDefined$ Self | ConditionPresent$ Card.nonCopiedSpell";
        AbilitySub saEffect = (AbilitySub)AbilityFactory.getAbility(abEffect, card);

        StringBuilder sbPlay = new StringBuilder();
        sbPlay.append("Mode$ Continuous | MayPlay$ True | EffectZone$ Command | Affected$ Card.IsRemembered+nonAdventure");
        sbPlay.append(" | AffectedZone$ Exile | Description$ You may cast the card.");
        saEffect.setSVar("Play", sbPlay.toString());

        saExile.setSubAbility(saEffect);

        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, card, true);

        re.setOverridingAbility(saExile);
        card.addReplacementEffect(re);
    }
}
