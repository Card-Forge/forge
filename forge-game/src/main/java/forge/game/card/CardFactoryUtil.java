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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import forge.GameCommand;
import forge.game.event.GameEventCardForetold;
import forge.game.trigger.TriggerType;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.CardTypeView;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.AlternativeCost;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityRestriction;
import forge.game.spellability.SpellPermanent;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityCantBeCast;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.TextUtil;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;

/**
 * <p>
 * CardFactoryUtil class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class CardFactoryUtil {

    /**
     * <p>
     * abilityMorphDown.
     * </p>
     *
     * @return a {@link forge.game.spellability.SpellAbility} object.
     */
    public static SpellAbility abilityMorphDown(final CardState cardState, final boolean intrinsic) {
        final Spell morphDown = new Spell(cardState.getCard(), new Cost(ManaCost.THREE, false)) {
            private static final long serialVersionUID = -1438810964807867610L;

            @Override
            public void resolve() {
                if (!hostCard.isFaceDown()) {
                    hostCard.setOriginalStateAsFaceDown();
                }
                final Game game = hostCard.getGame();

                CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
                CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();

                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
                moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);

                hostCard.getGame().getAction().moveToPlay(hostCard, this, moveParams);
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

        morphDown.setIntrinsic(intrinsic);

        return morphDown;
    }

    /**
     * <p>
     * abilityMorphUp.
     * </p>
     *
     * @return a {@link forge.game.spellability.AbilityActivated} object.
     */
    public static SpellAbility abilityMorphUp(final CardState cardState, final String costStr, final boolean mega, final boolean intrinsic) {
        Cost cost = new Cost(costStr, true);
        StringBuilder sbCost = new StringBuilder(mega ? "Megamorph" : "Morph");
        sbCost.append(" ");
        if (!cost.isOnlyManaCost()) {
            sbCost.append("— ");
        }
        sbCost.append(cost.toString());

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

        morphUp.setIntrinsic(intrinsic);

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
        card.setNamedCard(name);

        if (card.hasKeyword("Double agenda")) {
            String name2 = player.getController().chooseCardName(sa, cpp, "Card.!NamedCard",
                    "Name a second card for " + card.getName());
            if (name2 == null || name2.isEmpty()) {
                return false;
            }
            card.setNamedCard2(name2);
        }

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
        if (c.hasKeyword("CARDNAME can't be countered.") || c.hasKeyword("This spell can't be countered.")) {
            return false;
        }

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(c);
        List<ReplacementEffect> list = c.getGame().getReplacementHandler().getReplacementList(ReplacementType.Counter, repParams, ReplacementLayer.CantHappen);
        return list.isEmpty();
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

        for (String o : c.getHiddenExtrinsicKeywords()) {
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
            ColorSet color = crd.getColor();
            for (int i = 0; i < cntColors; i++) {
                if (color.hasAnyColor(MagicColor.WUBRG[i]))
                    map[i]++;
            }
        }

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
            ColorSet color = crd.getColor();
            for (int i = 0; i < cntColors; i++) {
                if (color.hasAnyColor(MagicColor.WUBRG[i]))
                    map[i]++;
            }
        }
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
            ColorSet color = crd.getColor();
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
     * getMostProminentCreatureTypeSize.
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
            CardTypeView type = c.getType();
            if (type.hasAllCreatureTypes() && Iterables.isEmpty(type.getExcludedCreatureSubTypes())) {
                allCreatureType++;
                continue;
            }
            // if something has all creature types, but some are excluded, the count might be messed up

            for (String creatureType : type.getCreatureTypes()) {
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
     * getMostProminentCreatureType.
     * </p>
     *
     * @param list
     *            a {@link forge.game.card.CardCollection} object.
     * @return a string.
     */
    public static Iterable<String> getMostProminentCreatureType(final CardCollectionView list) {
        if (list.isEmpty()) {
            return ImmutableList.of();
        }

        final Map<String, Integer> map = Maps.newHashMap();
        for (final Card c : list) {
            // Remove Duplicated types
            for (String creatureType : c.getType().getCreatureTypes()) {
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
        if (max == 0) {
            return ImmutableList.of();
        }
        List<String> result = Lists.newArrayList();
        for (final Entry<String, Integer> entry : map.entrySet()) {
            if (max == entry.getValue()) {
                result.add(entry.getKey());
            }
        }

        return result;
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
            final Iterable<ZoneType> zones, final Card host, CardTraitBase ctb) {
        final List<String> filteredkw = Lists.newArrayList();
        Player p = null;
        if (ctb instanceof SpellAbility) {
            p = ((SpellAbility)ctb).getActivatingPlayer();
        }
        if (p == null) {
            p = host.getController();
        }
        CardCollectionView cardlist = p.getGame().getCardsIn(zones);
        final Set<String> landkw = Sets.newHashSet();
        final Set<String> protectionkw = Sets.newHashSet();
        final Set<String> protectionColorkw = Sets.newHashSet();
        final Set<String> hexproofkw = Sets.newHashSet();
        final Set<String> tramplekw = Sets.newHashSet();
        final Set<String> allkw = Sets.newHashSet();

        for (Card c : CardLists.getValidCards(cardlist, restrictions, p, host, ctb)) {
            for (KeywordInterface inst : c.getKeywords()) {
                final String k = inst.getOriginal();
                if (k.endsWith("walk")) {
                    landkw.add(k);
                } else if (k.startsWith("Protection")) {
                    protectionkw.add(k);
                    for (byte col : MagicColor.WUBRG) {
                        final String colString = "Protection from " + MagicColor.toLongString(col).toLowerCase();
                        if (k.contains(colString)) {
                            protectionColorkw.add(colString);
                        }
                    }
                } else if (k.startsWith("Hexproof")) {
                    hexproofkw.add(k);
                } else if (k.startsWith("Trample")) {
                    tramplekw.add(k);
                } else {
                    allkw.add(k.toLowerCase());
                }
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
            } else if (keyword.equals("Trample")) {
                filteredkw.addAll(tramplekw);
            } else if (allkw.contains(keyword.toLowerCase())) {
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

                Breadcrumb bread = new Breadcrumb(msg);
                bread.setData("Card", card.getName());
                bread.setData("Ability", rawAbility);
                Sentry.addBreadcrumb(bread, card);

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
        if (!intrinsic) {
            repAb.setIntrinsic(false);
        }

        StringBuilder repEffsb = new StringBuilder();
        repEffsb.append("Event$ Moved | ValidCard$ ").append(valid);
        repEffsb.append(" | Destination$ Battlefield | ReplacementResult$ Updated | Description$ ").append(desc);
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

    public static String getProtectionValid(final String kw, final boolean damage) {
        String validSource = "";

        if (kw.startsWith("Protection:")) {
            final String[] kws = kw.split(":");
            String characteristic = kws[1];
            if (characteristic.startsWith("Player")) {
                validSource = "ControlledBy " + characteristic;
            } else {
                if (damage && (characteristic.endsWith("White") || characteristic.endsWith("Blue")
                    || characteristic.endsWith("Black") || characteristic.endsWith("Red")
                    || characteristic.endsWith("Green") || characteristic.endsWith("Colorless")
                    || characteristic.endsWith("MonoColor") || characteristic.endsWith("MultiColor"))) {
                    characteristic += "Source";
                }
                return characteristic;
            }
        } else if (kw.startsWith("Protection from ")) {
            String protectType = kw.substring("Protection from ".length());
            if (protectType.equals("white")) {
                validSource = "White" + (damage ? "Source" : "");
            } else if (protectType.equals("blue")) {
                validSource = "Blue" + (damage ? "Source" : "");
            } else if (protectType.equals("black")) {
                validSource = "Black" + (damage ? "Source" : "");
            } else if (protectType.equals("red")) {
                validSource = "Red" + (damage ? "Source" : "");
            } else if (protectType.equals("green")) {
                validSource = "Green" + (damage ? "Source" : "");
            } else if (protectType.equals("colorless")) {
                validSource = "Colorless" + (damage ? "Source" : "");
            } else if (protectType.equals("all colors")) {
                validSource = "nonColorless" + (damage ? "Source" : "");
            } else if (protectType.equals("everything")) {
                return "";
            } else {
                validSource = CardType.getSingularType(protectType);
            }
        }
        if (validSource.isEmpty()) {
            return validSource;
        }
        return "Card." + validSource + ",Emblem." + validSource;
    }

    public static ReplacementEffect makeEtbCounter(final String kw, final CardState card, final boolean intrinsic) {
        String parse = kw;

        String[] splitkw = parse.split(":");

        String desc = "CARDNAME enters the battlefield with ";
        desc += Lang.nounWithNumeralExceptOne(splitkw[2], CounterType.getType(splitkw[1]).getName().toLowerCase() + " counter");
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
        if (splitkw[1].startsWith("EACH")) {
            abStr = abStr.replace("CounterType$ EACH ", "CounterTypes$ ");
        }

        SpellAbility sa = AbilityFactory.getAbility(abStr, card);
        if (!intrinsic) {
            sa.setIntrinsic(false);
        }

        String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield "
                + "| Secondary$ True | ReplacementResult$ Updated | Description$ " + desc + (!extraparams.equals("") ? " | " + extraparams : "");

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

            final String effect = "DB$ Sacrifice | Defined$ TriggeredDefendingPlayer | SacValid$ Permanent | Amount$ " + k[1];

            final Trigger trigger = TriggerHandler.parseTrigger(trig, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Ascend")) {
            // Ascend trigger only for Permanent
            if (card.isPermanent() || card.isPlane()) {
                final String trig = "Mode$ Always | TriggerZones$ " + (card.isPlane() ? "Command" : "Battlefield")
                        + " | Secondary$ True | Static$ True | Blessing$ False | IsPresent$ Permanent.YouCtrl | PresentCompare$ GE10"
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
        } else if (keyword.startsWith("Backup")) {
            final String[] k = keyword.split(":");
            String magnitude = k[1];
            final String backupVar = card.getSVar(k[2]);

            String descStr = "Backup " + magnitude;

            final String trigStr = "Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | " +
                    "Secondary$ True | TriggerDescription$ " + descStr;

            final String putCounter = "DB$ PutCounter | ValidTgts$ Creature | CounterNum$ " + magnitude
                    + " | CounterType$ P1P1 | Backup$ True";

            final String addAbility = backupVar + " | ConditionDefined$ Targeted | ConditionPresent$ Card.Other | " +
                    "Defined$ Targeted";

            SpellAbility sa = AbilityFactory.getAbility(putCounter, card);
            AbilitySub backupSub = (AbilitySub) AbilityFactory.getAbility(addAbility, card);
            sa.setSubAbility(backupSub);

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);
            sa.setIntrinsic(intrinsic);
            trigger.setOverridingAbility(sa);

            inst.addTrigger(trigger);
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

            final Trigger bushidoTrigger1 = TriggerHandler.parseTrigger(trigBlock, card, intrinsic);
            final Trigger bushidoTrigger2 = TriggerHandler.parseTrigger(trigBlocked, card, intrinsic);

            bushidoTrigger1.setOverridingAbility(pump);
            bushidoTrigger2.setOverridingAbility(pump);

            inst.addTrigger(bushidoTrigger1);
            inst.addTrigger(bushidoTrigger2);
        } else if (keyword.equals("Cascade")) {
            final StringBuilder trigScript = new StringBuilder("Mode$ SpellCast | ValidCard$ Card.Self | TriggerZones$ Stack" +
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
        } else if (keyword.startsWith("Champion")) {
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
            trig.append("Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self");
            trig.append(" | TriggerDescription$ Champion ").append(article).append(" ").append(desc);
            trig.append(" (").append(Keyword.getInstance("Champion:" + desc).getReminderText()).append(")");

            StringBuilder trigReturn = new StringBuilder();
            trigReturn.append("Mode$ ChangesZone | Origin$ Battlefield | ValidCard$ Card.Self");
            trigReturn.append(" | Secondary$ True | TriggerDescription$ When this permanent leaves the battlefield, return the exiled card to the battlefield under its owner's control.");

            StringBuilder ab = new StringBuilder();
            ab.append("DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile | RememberChanged$ True ");
            ab.append(" | Champion$ True | Hidden$ True | Optional$ True | ChangeType$ ").append(changeType);

            StringBuilder subAb = new StringBuilder();
            subAb.append("DB$ Sacrifice | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ EQ0");

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
        } else if (keyword.startsWith("Casualty")) {
            final String trigScript = "Mode$ SpellCast | ValidCard$ Card.Self | TriggerZones$ Stack | " +
                    "CheckSVar$ CasualtyPaid | Secondary$ True";
            String abString = "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True";
            String[] k = keyword.split(":");
            if (k.length > 2) {
                abString = abString + " | " + k[2];
            }

            final Trigger casualtyTrigger = TriggerHandler.parseTrigger(trigScript, card, intrinsic);
            casualtyTrigger.setOverridingAbility(AbilityFactory.getAbility(abString, card));
            casualtyTrigger.setSVar("Casualty", "0");
            casualtyTrigger.setSVar("CasualtyPaid", "0");

            inst.addTrigger(casualtyTrigger);
        } else if (keyword.equals("Conspire")) {
            final String trigScript = "Mode$ SpellCast | ValidCard$ Card.Self | CheckSVar$ Conspire | TriggerZones$ Stack | Secondary$ True | TriggerDescription$ Copy CARDNAME if its conspire cost was paid";
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
        } else if (keyword.equals("Daybound")) {
            // Set Day when it's Neither
            final String setDayTrig = "Mode$ Always | TriggerZones$ Battlefield | Static$ True | DayTime$ Neither | Secondary$ True | TriggerDescription$ Any time a player controls a permanent with daybound, if it's neither day nor night, it becomes day.";
            String setDayEff = "DB$ DayTime | Value$ Day";

            Trigger trigger = TriggerHandler.parseTrigger(setDayTrig, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(setDayEff, card));

            inst.addTrigger(trigger);

            final String transformTrig = "Mode$ Always | TriggerZones$ Battlefield | Static$ True | DayTime$ Night | IsPresent$ Card.Self+FrontSide | Secondary$ True | TriggerDescription$ As it becomes night, if this permanent is front face up, transform it.";
            String transformEff = "DB$ SetState | Mode$ Transform | Daybound$ True";

            trigger = TriggerHandler.parseTrigger(transformTrig, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(transformEff, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Decayed")) {
            final String attackTrig = "Mode$ Attacks | ValidCard$ Card.Self | Secondary$ True | TriggerDescription$ " +
                    "When a creature with decayed attacks, sacrifice it at end of combat.";

            final String delayTrigStg = "DB$ DelayedTrigger | Mode$ Phase | Phase$ EndCombat | ValidPlayer$ Player | " +
                    "TriggerDescription$ At end of combat, sacrifice CARDNAME.";

            final String trigSacStg = "DB$ SacrificeAll | Defined$ Self | Controller$ You";

            SpellAbility delayTrigSA = AbilityFactory.getAbility(delayTrigStg, card);

            AbilitySub sacSA = (AbilitySub) AbilityFactory.getAbility(trigSacStg, card);
            delayTrigSA.setAdditionalAbility("Execute", sacSA);

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(attackTrig, card, intrinsic);

            delayTrigSA.setIntrinsic(intrinsic);

            parsedTrigger.setOverridingAbility(delayTrigSA);
            inst.addTrigger(parsedTrigger);
        } else if (keyword.equals("Demonstrate")) {
            final String trigScript = "Mode$ SpellCast | ValidCard$ Card.Self | TriggerZones$ Stack | TriggerDescription$ Demonstrate (" + inst.getReminderText() + ")";
            final String youCopyStr = "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility | MayChooseTarget$ True | Optional$ True | RememberCopies$ True | IgnoreFreeze$ True";
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
        } else if (keyword.equals("Double team")) {
            final String trigString = "Mode$ Attacks | ValidCard$ Card.Self+nonToken | TriggerZones$ Battlefield" +
                    " | Secondary$ True | TriggerDescription$ Double team (" + inst.getReminderText() + ")";
            final String makeString = "DB$ MakeCard | DefinedName$ Self | Zone$ Hand | RememberMade$ True | Conjure$ True";
            final String forgetString = "DB$ Effect | Duration$ Permanent | RememberObjects$ Remembered | ImprintCards$ TriggeredAttacker | StaticAbilities$ RemoveDoubleTeamMade";       
            final String madeforgetmadeString = "Mode$ Continuous | EffectZone$ Command | Affected$ Card.IsRemembered,Card.IsImprinted | RemoveKeyword$ Double team | AffectedZone$ Battlefield,Hand,Graveyard,Exile,Stack,Library,Command | Description$ Both cards perpetually lose double team.";
            final String CleanupString = "DB$ Cleanup | ClearRemembered$ True | ClearImprinted$ True";
            final Trigger trigger = TriggerHandler.parseTrigger(trigString, card, intrinsic);
            final SpellAbility youMake = AbilityFactory.getAbility(makeString, card);
            final AbilitySub forget = (AbilitySub) AbilityFactory.getAbility(forgetString, card);
            final AbilitySub Cleanup = (AbilitySub) AbilityFactory.getAbility(CleanupString, card);
            forget.setSVar("RemoveDoubleTeamMade",madeforgetmadeString);
            youMake.setSubAbility(forget);
            forget.setSubAbility(Cleanup);
            trigger.setOverridingAbility(youMake);
            
            inst.addTrigger(trigger); 
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
                    "Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self+evoked | Secondary$ True | TriggerDescription$ "
                            + "Evoke (" + inst.getReminderText() + ")");

            final String effect = "DB$ Sacrifice";
            final Trigger trigger = TriggerHandler.parseTrigger(trigStr.toString(), card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Evolve")) {
            final String trigStr = "Mode$ ChangesZone | Destination$ Battlefield | "
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
            final String trigStr = "Mode$ ChangesZone | ValidCard$ Card.Self | Destination$ Battlefield | Secondary$ True"
                    + " | TriggerDescription$ Exploit (" + inst.getReminderText() + ")";
            final String effect = "DB$ Sacrifice | SacValid$ Creature | SacMessage$ creature | Exploit$ True | Optional$ True";

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Extort")) {
            final String extortTrigger = "Mode$ SpellCast | ValidCard$ Card | ValidActivatingPlayer$ You | "
                    + "TriggerZones$ Battlefield | Secondary$ True"
                    + " | TriggerDescription$ Extort (" + inst.getReminderText() + ")";

            final String loseLifeStr = "AB$ LoseLife | Cost$ WB | Defined$ Player.Opponent | LifeAmount$ 1";
            final String gainLifeStr = "DB$ GainLife | Defined$ You | LifeAmount$ AFLifeLost";

            SpellAbility loseLifeSA = AbilityFactory.getAbility(loseLifeStr, card);

            AbilitySub gainLifeSA = (AbilitySub) AbilityFactory.getAbility(gainLifeStr, card);
            loseLifeSA.setSVar("AFLifeLost", "Number$0");
            loseLifeSA.setSubAbility(gainLifeSA);
            loseLifeSA.setIntrinsic(intrinsic);

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(extortTrigger, card, intrinsic);
            parsedTrigger.setOverridingAbility(loseLifeSA);
            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("Fabricate")) {
            final String[] k = keyword.split(":");
            final String n = k[1];

            final String name = StringUtils.join(k);

            final String trigStr = "Mode$ ChangesZone | Destination$ Battlefield "
                    + " | ValidCard$ Card.Self | Secondary$ True"
                    + " | TriggerDescription$ Fabricate " + n + " (" + inst.getReminderText() + ")";

            final String token = "DB$ Token | TokenAmount$ " + n + " | TokenScript$ c_1_1_a_servo"
                    + " | UnlessCost$ AddCounter<" + n + "/P1P1> | UnlessPayer$ You | UnlessAI$ " + name
                    + " | SpellDescription$ Fabricate - Create "
                    + Lang.nounWithNumeral(n, "1/1 colorless Servo artifact creature token") + ".";

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            SpellAbility saChoose = AbilityFactory.getAbility(token, card);
            saChoose.setIntrinsic(intrinsic);

            trigger.setOverridingAbility(saChoose);

            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Fading")) {
            String upkeepTrig = "Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Battlefield | Secondary$ True" +
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
        } else if (keyword.equals("For Mirrodin")) {
            final StringBuilder sbTrig = new StringBuilder();
            sbTrig.append("Mode$ ChangesZone | Destination$ Battlefield | ");
            sbTrig.append("ValidCard$ Card.Self | TriggerDescription$ ");
            sbTrig.append("For Mirrodin! (").append(inst.getReminderText()).append(")");

            final String sbRebel = "DB$ Token | TokenScript$ r_2_2_rebel | TokenOwner$ You | RememberTokens$ True";
            final SpellAbility saRebel= AbilityFactory.getAbility(sbRebel, card);

            final String sbAttach = "DB$ Attach | Defined$ Remembered";
            final AbilitySub saAttach = (AbilitySub) AbilityFactory.getAbility(sbAttach, card);
            saRebel.setSubAbility(saAttach);

            final String sbClear = "DB$ Cleanup | ClearRemembered$ True";
            final AbilitySub saClear = (AbilitySub) AbilityFactory.getAbility(sbClear, card);
            saAttach.setSubAbility(saClear);

            final Trigger etbTrigger = TriggerHandler.parseTrigger(sbTrig.toString(), card, intrinsic);

            etbTrigger.setOverridingAbility(saRebel);

            saRebel.setIntrinsic(intrinsic);
            inst.addTrigger(etbTrigger);
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
            String trigStr = "Mode$ SpellCast | ValidCard$ Card.Self | TriggerZones$ Stack | TriggerDescription$ Gravestorm (" + inst.getReminderText() + ")";
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
            sbDies.append("Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | TriggerZones$ Exile");
            sbDies.append(" | ValidCard$ Creature.HauntedBy | Execute$ ").append(hauntSVarName);
            sbDies.append(" | TriggerDescription$ ").append(hauntDescription);

            final Trigger hauntedDies = TriggerHandler.parseTrigger(sbDies.toString(), card, intrinsic);

            // Fourth, create a trigger that removes the haunting status if the
            // haunter leaves the exile
            final StringBuilder sbUnExiled = new StringBuilder();
            sbUnExiled.append("Mode$ ChangesZone | Origin$ Exile | ");
            sbUnExiled.append("ValidCard$ Card.Self | Static$ True | Secondary$ True | ");
            sbUnExiled.append("TriggerDescription$ Blank");

            final Trigger haunterUnExiled = TriggerHandler.parseTrigger(sbUnExiled.toString(), card,
                    intrinsic);

            final SpellAbility unhaunt = AbilityFactory.getAbility("DB$ Haunt", card);

            haunterUnExiled.setOverridingAbility(unhaunt);

            triggers.add(haunterUnExiled);

            // Trigger for when the haunted creature leaves the battlefield
            final StringBuilder sbHauntRemoved = new StringBuilder();
            sbHauntRemoved.append("Mode$ ChangesZone | Origin$ Battlefield | ");
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

            // First, create trigger that runs when the haunter goes to the graveyard
            final StringBuilder sbHaunter = new StringBuilder();
            sbHaunter.append("Mode$ ChangesZone | Origin$ ");
            sbHaunter.append(card.isCreature() ? "Battlefield" : "Stack | Fizzle$ False");
            sbHaunter.append(" | Destination$ Graveyard | ValidCard$ Card.Self");
            sbHaunter.append(" | Secondary$ True | TriggerDescription$ Haunt (").append(inst.getReminderText()).append(")");

            final Trigger haunterDies = TriggerHandler.parseTrigger(sbHaunter.toString(), card, intrinsic);

            final String hauntDiesEffectStr = "DB$ Haunt | ValidTgts$ Creature | TgtPrompt$ Choose target creature to haunt";
            final SpellAbility hauntDiesAbility = AbilityFactory.getAbility(hauntDiesEffectStr, card);

            haunterDies.setOverridingAbility(hauntDiesAbility);

            triggers.add(haunterDies);

            triggers.add(hauntedDies);

            for (final Trigger trigger : triggers) {
                inst.addTrigger(trigger);
            }
        } else if (keyword.startsWith("Hideaway")) {
            final String[] k = keyword.split(":");
            String n = k[1];

            // The exiled card gains ‘Any player who has controlled the permanent that exiled this card may look at this card in the exile zone.’
            // this is currently not possible because the StaticAbility currently has no information about the OriginalHost

            List<Trigger> triggers = Lists.newArrayList();
            StringBuilder sb = new StringBuilder();
            sb.append("Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self | Secondary$ True | ");
            sb.append("TriggerDescription$ Hideaway ").append(n).append(" (").append(inst.getReminderText()).append(")");
            final Trigger hideawayTrigger = TriggerHandler.parseTrigger(sb.toString(), card, intrinsic);

            String hideawayDig = "DB$ Dig | Defined$ You | DigNum$ " + n + " | DestinationZone$ Exile | ExileFaceDown$ True | RememberChanged$ True | RestRandomOrder$ True";
            String hideawayEffect = "DB$ Effect | StaticAbilities$ STHideawayEffectLookAtCard | ForgetOnMoved$ Exile | RememberObjects$ Remembered | Duration$ Permanent";
            String cleanupStr = "DB$ Cleanup | ClearRemembered$ True";
            
            String lookAtCard = "Mode$ Continuous | Affected$ Card.IsRemembered | MayLookAt$ EffectSourceController | EffectZone$ Command | AffectedZone$ Exile | Description$ Any player who has controlled the permanent that exiled this card may look at this card in the exile zone.";

            SpellAbility digSA = AbilityFactory.getAbility(hideawayDig, card);

            AbilitySub effectSA = (AbilitySub) AbilityFactory.getAbility(hideawayEffect, card);
            effectSA.setSVar("STHideawayEffectLookAtCard", lookAtCard);

            AbilitySub cleanSA = (AbilitySub) AbilityFactory.getAbility(cleanupStr, card);

            digSA.setSubAbility(effectSA);
            effectSA.setSubAbility(cleanSA);

            hideawayTrigger.setOverridingAbility(digSA);

            triggers.add(hideawayTrigger);

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
            sbTrig.append("Mode$ ChangesZone | Destination$ Battlefield | ");
            sbTrig.append("ValidCard$ Card.Self | Secondary$ True | TriggerDescription$ ");
            sbTrig.append("Living Weapon (").append(inst.getReminderText()).append(")");

            final StringBuilder sbGerm = new StringBuilder();
            sbGerm.append("DB$ Token | TokenAmount$ 1 | TokenScript$ b_0_0_phyrexian_germ |TokenOwner$ You | RememberTokens$ True");

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
            final String abStrPlay = "DB$ Play | Defined$ Self | Optional$ True | PlayCost$ " + manacost;

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
                    "TgtPrompt$ Select target artifact creature | CounterType$ P1P1 | CounterNum$ ModularX | Modular$ True";

            String trigStr = "Mode$ ChangesZone | ValidCard$ Card.Self | Origin$ Battlefield | Destination$ Graveyard" +
                    " | OptionalDecider$ TriggeredCardController" +
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

            final String copyStr = "DB$ CopyPermanent | Defined$ Self | TokenTapped$ True | Optional$ True | TokenAttacking$ Remembered"
                    + "| ForEach$ OppNonDefendingPlayer | ChoosePlayerOrPlaneswalker$ True | AtEOT$ ExileCombat | CleanupForEach$ True";

            final SpellAbility copySA = AbilityFactory.getAbility(copyStr, card);
            copySA.setIntrinsic(intrinsic);

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, intrinsic);
            parsedTrigger.setOverridingAbility(copySA);

            inst.addTrigger(parsedTrigger);
        } else if (keyword.equals("Nightbound")) {
            // Set Night when it's Neither
            final String setDayTrig = "Mode$ Always | TriggerZones$ Battlefield | Static$ True | DayTime$ Neither | IsPresent$ Card.Daybound | PresentCompare$ EQ0 | Secondary$ True | TriggerDescription$ Any time a player controls a permanent with nightbound, if it's neither day nor night and there are no permanents with daybound on the battlefield, it becomes night.";
            String setDayEff = "DB$ DayTime | Value$ Night";

            Trigger trigger = TriggerHandler.parseTrigger(setDayTrig, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(setDayEff, card));

            inst.addTrigger(trigger);

            final String transformTrig = "Mode$ Always | TriggerZones$ Battlefield | Static$ True | DayTime$ Day | IsPresent$ Card.Self+BackSide | Secondary$ True | TriggerDescription$ As it becomes day, if this permanent is back face up, transform it";
            String transformEff = "DB$ SetState | Mode$ Transform | Nightbound$ True";

            trigger = TriggerHandler.parseTrigger(transformTrig, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(transformEff, card));

            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Partner:")) {
            // Partner With
            final String[] k = keyword.split(":");
            final String trigStr = "Mode$ ChangesZone | Destination$ Battlefield " +
                    "| ValidCard$ Card.Self | Secondary$ True " +
                    "| TriggerDescription$ Partner with " + k[1] + " (" + inst.getReminderText() + ")";
            // replace , for ; in the ChangeZone
            k[1] = k[1].replace(",", ";");

            final String effect = "DB$ ChangeZone | ValidTgts$ Player | TgtPrompt$ Select target player" +
                    " | Origin$ Library | Destination$ Hand | ChangeType$ Card.named" + k[1] +
                    " | Hidden$ True | Chooser$ Targeted | Optional$ Targeted";

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);
            trigger.setOverridingAbility(AbilityFactory.getAbility(effect, card));

            inst.addTrigger(trigger);
        } else if (keyword.equals("Persist")) {
            final String trigStr = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard " +
                    " | ValidCard$ Card.Self+counters_EQ0_M1M1 | TriggerZones$ Battlefield | Secondary$ True" +
                    " | TriggerDescription$ Persist (" + inst.getReminderText() + ")";
            final String effect = "DB$ ChangeZone | Defined$ TriggeredNewCardLKICopy | Origin$ Graveyard | Destination$ Battlefield | WithCountersType$ M1M1";

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
        } else if (keyword.equals("Provoke")) {
            final String actualTrigger = "Mode$ Attacks | ValidCard$ Card.Self | OptionalDecider$ You | Secondary$ True"
                    + " | TriggerDescription$ Provoke (" + inst.getReminderText() + ")";
            final String blockStr = "DB$ MustBlock | Duration$ UntilEndOfCombat | ValidTgts$ Creature.ControlledBy TriggeredDefendingPlayer | TgtPrompt$ Select target creature defending player controls";
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
                    " | ValidBlocker$ Creature | Secondary$ True " +
                    " | TriggerDescription$ Rampage " + n + " (" + inst.getReminderText() + ")";

            final String effect = "DB$ Pump | Defined$ TriggeredAttackerLKICopy" +
                    " | NumAtt$ Rampage" + n + " | NumDef$ Rampage" + n;

            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setSVar("Rampage" + n, "SVar$RampageCount/Times." + n);

            sa.setSVar("RampageCount", "Count$Valid Creature.blockingTriggeredAttacker/Minus.1");
            sa.setIntrinsic(intrinsic);
            trigger.setOverridingAbility(sa);
            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Ravenous")) {
            final String ravenousTrig = "Mode$ ChangesZone | ValidCard$ Card.Self | Origin$ Any | " +
                    "Destination$ Battlefield | CheckSVar$ Count$xPaid | SVarCompare$ GE5 | Secondary$ True | " +
                    "TriggerDescription$ If X is 5 or more, draw a card when it enters.";

            final String drawStr = "DB$ Draw";

            final Trigger trigger = TriggerHandler.parseTrigger(ravenousTrig, card, intrinsic);
            SpellAbility sa = AbilityFactory.getAbility(drawStr, card);

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
                    + recoverCost + " | UnlessPayer$ You | UnlessSwitched$ True | UnlessResolveSubs$ WhenNotPaid";
            final String exileStr = "DB$ ChangeZone | Defined$ Self | Origin$ Graveyard | Destination$ Exile";

            SpellAbility changeSA = AbilityFactory.getAbility(changeStr, card);
            AbilitySub exileSA = (AbilitySub) AbilityFactory.getAbility(exileStr, card);
            changeSA.setSubAbility(exileSA);

            final Cost cost = new Cost(recoverCost, false);
            String costDesc = cost.toSimpleString();
            if (!cost.isOnlyManaCost()) {
                costDesc = "—" + costDesc;
            }

            String trigObject = card.isCreature() ? "Creature.Other+YouOwn" : "Creature.YouOwn";
            String trigArticle = card.isCreature() ? "another" : "a";
            String trigStr = "Mode$ ChangesZone | ValidCard$ " + trigObject
                    + " | Origin$ Battlefield | Destination$ Graveyard | TriggerZones$ Graveyard | Secondary$ True | "
                    + "TriggerDescription$ Recover " + costDesc + " (When " + trigArticle + " creature is "
                    + "put into your graveyard from the battlefield, you "
                    + "may pay " + costDesc + ". If you do, return "
                    + "CARDNAME from your graveyard to your hand. Otherwise, exile CARDNAME.)";
            final Trigger myTrigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);
            changeSA.setIntrinsic(intrinsic);
            myTrigger.setOverridingAbility(changeSA);

            inst.addTrigger(myTrigger);
        } else if (keyword.startsWith("Replicate")) {
            final String trigScript = "Mode$ SpellCast | ValidCard$ Card.Self | CheckSVar$ ReplicateAmount | Secondary$ True | TriggerDescription$ Copy CARDNAME for each time you paid its replicate cost.";
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

            final String abString = "DB$ PeekAndReveal | PeekAmount$ " + num + " | RememberRevealed$ True";

            final String dbCast = "DB$ Play | Valid$ Card.IsRemembered+sameName | ValidSA$ Spell | " +
                    "ValidZone$ Library | WithoutManaCost$ True | Optional$ True | Amount$ All";

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
        } else if (keyword.startsWith("Saga") || keyword.startsWith("Read ahead")) {
            final String[] k = keyword.split(":");
            final List<String> abs = Arrays.asList(k[2].split(","));
            if (abs.size() != Integer.valueOf(k[1])) {
                throw new RuntimeException("Saga max differ from Ability amount");
            }

            int idx = 0;
            int skipId = 0;
            for (String ab : abs) {
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
                    sa.setLastChapter(i == abs.size());

                    StringBuilder trigStr = new StringBuilder("Mode$ CounterAdded | ValidCard$ Card.Self | TriggerZones$ Battlefield");
                    trigStr.append("| Chapter$ ").append(i).append(" | CounterType$ LORE | CounterAmount$ EQ").append(i);
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
            final String actualTriggerSelf = "Mode$ ChangesZone | Destination$ Battlefield | "
                    + "ValidCard$ Card.Self | "
                    + "IsPresent$ Creature.Other+YouCtrl+NotPaired | Secondary$ True | "
                    + "TriggerDescription$ When CARDNAME enters the battlefield, "
                    + "you may pair CARDNAME with another unpaired creature you control";
            final String abStringSelf = "DB$ Bond | Defined$ TriggeredCardLKICopy | ValidCards$ Creature.Other+YouCtrl+NotPaired";
            final Trigger parsedTriggerSelf = TriggerHandler.parseTrigger(actualTriggerSelf, card, intrinsic);
            parsedTriggerSelf.setOverridingAbility(AbilityFactory.getAbility(abStringSelf, card));

            // Setup ETB trigger for other creatures you control
            final String actualTriggerOther = "Mode$ ChangesZone | Destination$ Battlefield | "
                    + "ValidCard$ Creature.Other+YouCtrl | TriggerZones$ Battlefield | "
                    + " IsPresent$ Creature.Self+NotPaired | Secondary$ True | "
                    + " TriggerDescription$ When another unpaired creature you control enters the battlefield, "
                    + "you may pair it with CARDNAME";
            final String abStringOther = "DB$ Bond | Defined$ TriggeredCardLKICopy | ValidCards$ Creature.Self+NotPaired";
            final Trigger parsedTriggerOther = TriggerHandler.parseTrigger(actualTriggerOther, card, intrinsic);
            parsedTriggerOther.setOverridingAbility(AbilityFactory.getAbility(abStringOther, card));

            inst.addTrigger(parsedTriggerSelf);
            inst.addTrigger(parsedTriggerOther);
        } else if (keyword.startsWith("Soulshift")) {
            final String[] k = keyword.split(":");

            final String actualTrigger = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard"
                    + "| Secondary$ True | OptionalDecider$ You | ValidCard$ Card.Self"
                    + "| TriggerDescription$ " + k[0] + " " + k[1] + " (" + inst.getReminderText() + ")";
            final String effect = "DB$ ChangeZone | Origin$ Graveyard | Destination$ Hand"
                    + "| ValidTgts$ Spirit.YouOwn+cmcLE" + k[1];
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, card, intrinsic);
            final SpellAbility sp = AbilityFactory.getAbility(effect, card);

            parsedTrigger.setOverridingAbility(sp);

            inst.addTrigger(parsedTrigger);
        } else if (keyword.startsWith("Squad")) {
            final String trigScript = "Mode$ ChangesZone | Origin$ Any | Destination$ Battlefield | " +
                    "ValidCard$ Card.Self+wasCast | CheckSVar$ SquadAmount | Secondary$ True | " +
                    "TriggerDescription$ When this creature enters the battlefield, create that many tokens that " +
                    "are copies of it.";
            final String abString = "DB$ CopyPermanent | Defined$ TriggeredCard | NumCopies$ SquadAmount";

            final Trigger squadTrigger = TriggerHandler.parseTrigger(trigScript, card, intrinsic);
            final SpellAbility squadAbility = AbilityFactory.getAbility(abString, card);
            squadAbility.setSVar("SquadAmount", "0");
            squadTrigger.setOverridingAbility(squadAbility);
            squadTrigger.setSVar("SquadAmount", "0");
            inst.addTrigger(squadTrigger);
        } else if (keyword.equals("Storm")) {
            final String actualTrigger = "Mode$ SpellCast | ValidCard$ Card.Self | TriggerZones$ Stack | Secondary$ True"
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

            upkeepTrig.append("Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$ Exile");
            upkeepTrig.append(" | IsPresent$ Card.Self+suspended | PresentZone$ Exile");
            // Mark this trigger as Secondary, so it's not displayed twice
            upkeepTrig.append(" | Secondary$ True | TriggerDescription$ At the beginning of your upkeep, if this card is suspended, remove a time counter from it");

            final String abRemove = "DB$ RemoveCounter | Defined$ Self | CounterType$ TIME | CounterNum$ 1";

            final Trigger parsedUpkeepTrig = TriggerHandler.parseTrigger(upkeepTrig.toString(), card, intrinsic);
            parsedUpkeepTrig.setOverridingAbility(AbilityFactory.getAbility(abRemove, card));

            //play trigger
            StringBuilder playTrig = new StringBuilder();

            playTrig.append("Mode$ CounterRemoved | TriggerZones$ Exile | ValidCard$ Card.Self | CounterType$ TIME | NewCounterAmount$ 0 | Secondary$ True");
            playTrig.append(" | TriggerDescription$ When the last time counter is removed from this card, if it's exiled, play it without paying its mana cost if able.  ");
            playTrig.append("If you can't, it remains exiled. If you cast a creature spell this way, it gains haste until you lose control of the spell or the permanent it becomes.");

            String abPlay = "DB$ Play | Defined$ Self | WithoutManaCost$ True";
            if (card.isPermanent()) {
                abPlay += "| RememberPlayed$ True";
            }

            final SpellAbility saPlay = AbilityFactory.getAbility(abPlay, card);

            if (card.isPermanent()) {
                final String abPump = "DB$ Pump | Defined$ Remembered | KW$ Haste | PumpZone$ Stack "
                        + "| ConditionDefined$ Remembered | ConditionPresent$ Creature | Duration$ UntilLoseControlOfHost";
                final AbilitySub saPump = (AbilitySub) AbilityFactory.getAbility(abPump, card);

                String dbClean = "DB$ Cleanup | ClearRemembered$ True";
                final AbilitySub saCleanup = (AbilitySub) AbilityFactory.getAbility(dbClean, card);
                saPump.setSubAbility(saCleanup);

                saPlay.setSubAbility(saPump);
            }

            final Trigger parsedPlayTrigger = TriggerHandler.parseTrigger(playTrig.toString(), card, intrinsic);
            parsedPlayTrigger.setOverridingAbility(saPlay);

            inst.addTrigger(parsedUpkeepTrig);
            inst.addTrigger(parsedPlayTrigger);
        } else if (keyword.equals("Training")) {
            final String trigStr = "Mode$ Attacks | ValidCard$ Card.Self | Secondary$ True | " +
                    "IsPresent$ Creature.attacking+Other+powerGTX | NoResolvingCheck$ True | TriggerDescription$ Training (" +
                    inst.getReminderText() + ")";

            final String effect = "DB$ PutCounter | CounterType$ P1P1 | CounterNum$ 1 | Defined$ Self | Training$ True";
            final Trigger trigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            SpellAbility sa = AbilityFactory.getAbility(effect, card);
            trigger.setSVar("X", "Count$CardPower");
            sa.setIntrinsic(intrinsic);
            trigger.setOverridingAbility(sa);

            inst.addTrigger(trigger);
        } else if (keyword.startsWith("Tribute")) {
            // use hardcoded ability name
            final String abStr = "TrigNotTribute";

            // get Description from Ability
            final String desc = AbilityFactory.getMapParams(card.getSVar(abStr)).get("SpellDescription");
            final String trigStr = "Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self+notTributed " +
                     " | Execute$ " + abStr + " | TriggerDescription$ " + desc;

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(trigStr, card, intrinsic);

            inst.addTrigger(parsedTrigger);
        } else if (keyword.equals("Undying")) {
            final String trigStr = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard " +
                    " | ValidCard$ Card.Self+counters_EQ0_P1P1 | TriggerZones$ Battlefield | Secondary$ True" +
                    " | TriggerDescription$ Undying (" + inst.getReminderText() + ")";
            final String effect = "DB$ ChangeZone | Defined$ TriggeredNewCardLKICopy | Origin$ Graveyard | Destination$ Battlefield | WithCountersType$ P1P1";

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

            String effect = "DB$ SacrificeAll | Defined$ Self | Controller$ You | UnlessPayer$ You | UnlessCost$ " + k[1];

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

            final String remove = "DB$ RemoveCounter | Defined$ Self | CounterType$ TIME | CounterNum$ 1";
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
        } else if (keyword.startsWith("Dungeon")) {
            final List<String> abs = Arrays.asList(keyword.substring("Dungeon:".length()).split(","));
            final Map<String, SpellAbility> saMap = new LinkedHashMap<>();

            for (String ab : abs) {
                saMap.put(ab, AbilityFactory.getAbility(card, ab));
            }
            for (SpellAbility sa : saMap.values()) {
                String roomName = sa.getParam("RoomName");
                StringBuilder trigStr = new StringBuilder("Mode$ RoomEntered | TriggerZones$ Command");
                trigStr.append(" | ValidCard$ Card.Self | ValidRoom$ ").append(roomName);
                trigStr.append(" | TriggerDescription$ ").append(roomName).append(" — ").append(sa.getDescription());
                if (sa.hasParam("NextRoom")) {
                    boolean first = true;
                    StringBuilder nextRoomParam = new StringBuilder();
                    trigStr.append("  (Leads to: ");
                    for (String nextRoomSVar : sa.getParam("NextRoom").split(",")) {
                        if (!first) {
                            trigStr.append(", ");
                            nextRoomParam.append(",");
                        }
                        String nextRoomName = saMap.get(nextRoomSVar).getParam("RoomName");
                        trigStr.append(nextRoomName);
                        nextRoomParam.append(nextRoomName);
                        first = false;
                    }
                    trigStr.append(")");
                    sa.putParam("NextRoomName", nextRoomParam.toString());
                }

                // Need to set intrinsic to false here, else the first room won't get triggered
                final Trigger t = TriggerHandler.parseTrigger(trigStr.toString(), card, false);
                t.setOverridingAbility(sa);
                inst.addTrigger(t);
            }
        } else if (keyword.startsWith("Ward")) {
            final String[] k = keyword.split(":");
            final Cost cost = new Cost(k[1], false);
            String costDesc = cost.toSimpleString();

            String strTrig = "Mode$ BecomesTarget | ValidSource$ SpellAbility.OppCtrl | ValidTarget$ Card.Self "
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

            final String strDelay = "DB$ DelayedTrigger | Mode$ Phase | Phase$ Cleanup | RememberObjects$ Self | TriggerDescription$ At the beginning of the next cleanup step, sacrifice CARDNAME.";
            final String strSac = "DB$ SacrificeAll | Defined$ DelayTriggerRememberedLKI";

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
                    + " | ReplacementResult$ Updated | Description$ Amplify " + amplifyMagnitude + " ("
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
        } else if (keyword.equals("Compleated")) {
            String sb = "etbCounter:LOYALTY:PhySpent:CheckSVar$ PhySpent | SVarCompare$ LT0:This planeswalker" +
                    " enters with two fewer loyalty counters for each Phyrexian mana symbol life was paid for";
            final ReplacementEffect re = makeEtbCounter(sb, card, intrinsic);
            card.setSVar("PhySpent", "Count$EachPhyrexianPaidWithLife/Negative");

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
        } else if (keyword.equals("Daybound")) {
            final String actualRep = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | DayTime$ Night | Secondary$ True | Layer$ Transform | ReplacementResult$ Updated | Description$ If it is night, this permanent enters the battlefield transformed.";
            final String abTransform = "DB$ SetState | Defined$ ReplacedCard | Mode$ Transform | ETB$ True | Daybound$ True";

            ReplacementEffect re = ReplacementHandler.parseReplacement(actualRep, host, intrinsic, card);

            SpellAbility saTransform = AbilityFactory.getAbility(abTransform, card);
            re.setOverridingAbility(saTransform);

            inst.addReplacement(re);
        } else if (keyword.startsWith("Devour")) {
            final String[] k = keyword.split(":");
            final String magnitude = k[1];
            String valid = "Creature";
            final String[] s = k[0].split(" ");
            if (s.length > 1) {
                valid = s[1].substring(0, 1).toUpperCase() + s[1].substring(1);
            }

            String sacrificeStr = "DB$ Sacrifice | Defined$ You | Amount$ DevourSacX | SacValid$ " + valid +
                    ".Other | SacMessage$ another " + valid.toLowerCase() + " (Devour " + magnitude +
                    ") | RememberSacrificed$ True | Optional$ True | Devour$ True";

            String counterStr = "DB$ PutCounter | ETB$ True | Defined$ Self | CounterType$ P1P1 | CounterNum$ DevourX";
            String cleanupStr = "DB$ Cleanup | ClearRemembered$ True";

            AbilitySub sacrificeSA = (AbilitySub) AbilityFactory.getAbility(sacrificeStr, card);
            sacrificeSA.setSVar("DevourSacX", "Count$Valid " + valid + ".YouCtrl+Other");

            AbilitySub counterSA = (AbilitySub) AbilityFactory.getAbility(counterStr, card);
            counterSA.setSVar("DevourX", "Count$RememberedSize/Times." + magnitude);
            sacrificeSA.setSubAbility(counterSA);

            AbilitySub cleanupSA = (AbilitySub) AbilityFactory.getAbility(cleanupStr, card);
            counterSA.setSubAbility(cleanupSA);

            String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Secondary$ True | ReplacementResult$ Updated | Description$ Devour " + magnitude + " ("+ inst.getReminderText() + ")";

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);

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
            sb.append("| ValidStackSa$ Spell.Flashback+castKeyword | Description$ Flashback");

            if (keyword.contains(":")) { // K:Flashback:Cost:ExtraParams:ExtraDescription
                final String[] k = keyword.split(":");
                final Cost cost = new Cost(k[1], false);
                sb.append(cost.isOnlyManaCost() ? " " : "—").append(cost.toSimpleString());
                sb.append(cost.isOnlyManaCost() ? "" : ".");

                String extraDesc =  k.length > 3 ? k[3] : "";
                if (!extraDesc.isEmpty()) { // extra params added in GameActionUtil, desc added here
                    sb.append(cost.isOnlyManaCost() ? ". " : " ").append(extraDesc);
                }
            }

            sb.append(" (").append(inst.getReminderText()).append(")");

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
        } else if (keyword.equals("Ravenous")) {
            String repeffStr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | " +
                    " | ReplacementResult$ Updated | Description$ Ravenous (" + inst.getReminderText() + ")";

            String counterStr = "DB$ PutCounter | CounterType$ P1P1 | ETB$ True | CounterNum$ X";
            SpellAbility countersSA = AbilityFactory.getAbility(counterStr, card);

            if (!intrinsic) {
                countersSA.setIntrinsic(false);
            }

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffStr, host, intrinsic, card);

            re.setOverridingAbility(countersSA);
            countersSA.setSVar("X", "Count$xPaid");

            inst.addReplacement(re);
        } else if (keyword.startsWith("Read ahead")) {
            final String[] k = keyword.split(":");
            String repeffstr = "Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | Secondary$ True | ReplacementResult$ Updated | Description$ Choose a chapter and start with that many lore counters.";

            String effStr = "DB$ PutCounter | Defined$ Self | CounterType$ LORE | ETB$ True | UpTo$ True | UpToMin$ 1 | ReadAhead$ True | CounterNum$ " + k[1];

            SpellAbility saCounter = AbilityFactory.getAbility(effStr, card);

            if (!intrinsic) {
                saCounter.setIntrinsic(false);
            }

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, host, intrinsic, card);

            re.setOverridingAbility(saCounter);

            inst.addReplacement(re); 
        } else if (keyword.equals("Rebound")) {
            String repeffstr = "Event$ Moved | ValidLKI$ Card.Self+wasCastFromHand+YouOwn+YouCtrl "
            + " | Origin$ Stack | Destination$ Graveyard | Fizzle$ False "
            + " | Description$ Rebound (" + inst.getReminderText() + ")";

            String abExile = "DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Stack | Destination$ Exile | RememberChanged$ True";
            String delTrig = "DB$ DelayedTrigger | Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You " +
            " | OptionalDecider$ You | RememberObjects$ Remembered | TriggerDescription$"
            + " At the beginning of your next upkeep, you may cast " + card.toString() + " without paying its mana cost.";
            String abPlay = "DB$ Play | Defined$ DelayTriggerRememberedLKI | WithoutManaCost$ True | Optional$ True";

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
            final String copyStr = "DB$ CopyPermanent | Defined$ Self | Controller$ Player.IsTriggerRemembered | RemoveKeywords$ Reflect";

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
            final String haste = "DB$ Animate | Defined$ Self | Keywords$ Haste | Duration$ Permanent | SpellDescription$ Haste";

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
                    + " (" + inst.getReminderText() + ")";

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
            rep += "| Secondary$ True | Description$ " + keyword;

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
        else if (keyword.startsWith("Protection")) {
            String validSource = getProtectionValid(keyword, true);

            String rep = "Event$ DamageDone | Prevent$ True | ActiveZones$ Battlefield | ValidTarget$ Card.Self";
            if (!validSource.isEmpty()) {
                rep += " | ValidSource$ " + validSource;
            }
            rep += " | Secondary$ True | Description$ " + keyword;

            ReplacementEffect re = ReplacementHandler.parseReplacement(rep, host, intrinsic, card);
            inst.addReplacement(re);
        }

        if (keyword.equals("CARDNAME enters the battlefield tapped.")) {
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
            for (SpellAbility sa : host.getBasicSpells()) {
                if (costStr.equals("ConvertedManaCost")) {
                    costStr = Integer.toString(host.getCMC());
                }
                final Cost cost = new Cost(costStr, false).add(sa.getPayCosts().copyWithNoMana());
                final SpellAbility newSA = sa.copyWithDefinedCost(cost);
                newSA.setBasicSpell(false);
                newSA.putParam("Secondary", "True");
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
            final String animate = "DB$ Animate | Defined$ ParentTarget | Power$ 0 | Toughness$ 0 | Types$"
                    + " Creature,Elemental | Duration$ Permanent | Keywords$ Haste";

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
        } else if (keyword.startsWith("Blitz")) {
            final String[] k = keyword.split(":");
            final Cost blitzCost = new Cost(k[1], false);

            final SpellAbility newSA = card.getFirstSpellAbility().copyWithManaCostReplaced(host.getController(), blitzCost);

            if (k.length > 2) {
                newSA.getMapParams().put("ValidAfterStack", k[2]);
            }

            final StringBuilder desc = new StringBuilder();
            desc.append("Blitz ").append(blitzCost.toSimpleString()).append(" (");
            desc.append(inst.getReminderText());
            desc.append(")");

            newSA.setDescription(desc.toString());

            final StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" (Blitz)");
            newSA.setStackDescription(sb.toString());

            newSA.setAlternativeCost(AlternativeCost.Blitz);
            newSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(newSA);
        } else if (keyword.startsWith("Class")) {
            final String[] k = keyword.split(":");
            final int level = Integer.valueOf(k[1]);

            final StringBuilder sbClass = new StringBuilder();
            sbClass.append("AB$ ClassLevelUp | Cost$ ").append(k[2]);
            sbClass.append(" | ClassLevel$ EQ").append(level - 1);
            sbClass.append(" | SorcerySpeed$ True");
            sbClass.append(" | StackDescription$ SpellDescription | SpellDescription$ Level ").append(level);

            final SpellAbility sa = AbilityFactory.getAbility(sbClass.toString(), card);
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
        } else if (keyword.startsWith("Disturb")) {
            final String[] k = keyword.split(":");
            final Cost disturbCost = new Cost(k[1], true);

            SpellAbility newSA;
            if (host.getAlternateState().getType().hasSubtype("Aura")) {
                newSA = host.getAlternateState().getFirstAbility().copyWithDefinedCost(disturbCost);
            } else {
                newSA = new SpellPermanent(host, host.getAlternateState(), disturbCost);
            }
            newSA.setCardState(host.getAlternateState());

            StringBuilder sbCost = new StringBuilder("Disturb");
            if (!disturbCost.isOnlyManaCost()) { //Something other than a mana cost
                sbCost.append("—");
            } else {
                sbCost.append(" ");
            }

            newSA.putParam("PrecostDesc", sbCost.toString());
            newSA.putParam("CostDesc", disturbCost.toString());

            // makes new SpellDescription
            final StringBuilder desc = new StringBuilder();
            desc.append(newSA.getCostDescription());
            desc.append("(").append(inst.getReminderText()).append(")");
            newSA.setDescription(desc.toString());
            newSA.putParam("AfterDescription", "(Disturbed)");

            newSA.setAlternativeCost(AlternativeCost.Disturb);
            newSA.getRestrictions().setZone(ZoneType.Graveyard);
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
            String valid = k.length > 2 && !k[2].isEmpty() ? k[2] : "Creature.YouCtrl";
            String vstr = k.length > 3 && !k[3].isEmpty() ? k[3] : "creature";
            String extra = k.length > 4 ? k[4] : "";
            boolean altCost = extra.contains("AlternateCost");
            String extraDesc = k.length > 5 ? k[5] : "";
            // Create attach ability string
            final StringBuilder abilityStr = new StringBuilder();
            abilityStr.append("AB$ Attach | Cost$ ");
            abilityStr.append(equipCost);
            abilityStr.append("| ValidTgts$ ").append(valid);
            abilityStr.append(" | TgtPrompt$ Select target ").append(vstr).append(" you control ");
            // the if the Equipment can really attach should be part of the Attach Effect
            abilityStr.append("| SorcerySpeed$ True | Equip$ True | AILogic$ Pump");
            // add AttachAi for some special cards
            if (card.hasSVar("AttachAi")) {
                abilityStr.append("| ").append(card.getSVar("AttachAi"));
            }
            abilityStr.append("| PrecostDesc$ Equip");
            if (k.length > 3 && !k[3].isEmpty()) {
                abilityStr.append(" ").append(vstr);
            }
            Cost cost = new Cost(equipCost, true);
            if (!cost.isOnlyManaCost() || (altCost && extra.contains("<"))) { //Something other than a mana cost
                abilityStr.append("—");
            } else {
                abilityStr.append(" ");
            }
            if (!altCost) {
                abilityStr.append("| CostDesc$ ").append(cost.toSimpleString()).append(" ");
            }
            abilityStr.append("| SpellDescription$ ");
            if (!extraDesc.isEmpty()) {
                abilityStr.append(". ").append(extraDesc).append(". ");
            }
            if (!altCost) {
                abilityStr.append("(").append(inst.getReminderText()).append(")");
            }
            if (!extra.isEmpty()) {
                abilityStr.append(" | ").append(extra);
            }
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
            boolean onlyMana = evokedCost.isOnlyManaCost();
            desc.append("Evoke").append(onlyMana ? " " : "—").append(evokedCost.toSimpleString());
            desc.append(onlyMana ? "" : ".").append(" (").append(inst.getReminderText()).append(")");

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

                    if (!activator.hasKeyword("Foretell on any player's turn") && !game.getPhaseHandler().isPlayerTurn(activator)) {
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
                    final Card c = game.getAction().exile(new CardCollection(getHostCard()), this, null).get(0);
                    c.setForetold(true);
                    game.getTriggerHandler().runTrigger(TriggerType.IsForetold, AbilityKey.mapFromCard(c), false);
                    c.setForetoldThisTurn(true);
                    c.turnFaceDown(true);
                    // look at the exiled card
                    c.addMayLookTemp(getActivatingPlayer());

                    // only done when the card is foretold by the static ability
                    getActivatingPlayer().addForetoldThisTurn();

                    if (!isIntrinsic()) {
                        // because it doesn't work other wise
                        c.setForetoldCostByEffect(true);
                    }
                    String sb = TextUtil.concatWithSpace(getActivatingPlayer().toString(),"has foretold.");
                    game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, sb);
                    game.fireEvent(new GameEventCardForetold(getActivatingPlayer()));
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
            sb.append("AB$ PutCounter | Cost$ ").append(manacost).append(" | PrecostDesc$ Level up | CostDesc$ ");
            sb.append(ManaCostParser.parse(manacost)).append(" | SorcerySpeed$ True | LevelUp$ True | Secondary$ True");
            sb.append("| CounterType$ LEVEL | StackDescription$ {p:You} levels up {c:Self}.");
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

            inst.addSpellAbility(abilityMorphDown(card, intrinsic));
            inst.addSpellAbility(abilityMorphUp(card, k[1], false, intrinsic));
        } else if (keyword.startsWith("Megamorph")) {
            final String[] k = keyword.split(":");

            inst.addSpellAbility(abilityMorphDown(card, intrinsic));
            inst.addSpellAbility(abilityMorphUp(card, k[1], true, intrinsic));
        } else if (keyword.startsWith("More Than Meets the Eye")) {
            final String[] n = keyword.split(":");
            final Cost convertCost = new Cost(n[1], false);

            final SpellAbility sa = new SpellPermanent(host, host.getAlternateState(), convertCost);
            sa.setDescription(host.getAlternateState().getName() + " (" + inst.getReminderText() + ")");
            sa.setCardState(host.getAlternateState());
            sa.setAlternativeCost(AlternativeCost.MTMtE);

            sa.putParam("PrecostDesc", n[0] + " ");
            sa.putParam("CostDesc", convertCost.toString());
            sa.putParam("AfterDescription", "(Converted)");
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
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
        } else if (keyword.startsWith("Prototype")) {
            final String[] k = keyword.split(":");
            if (k.length < 4) {
                System.err.println("Malformed Prototype entry! - Card: " + card.toString());
                return;
            }

            final Cost protoCost = new Cost(k[1], false);
            final SpellAbility newSA = card.getFirstSpellAbility().copyWithDefinedCost(protoCost);
            newSA.putParam("SetManaCost", k[1]);
            newSA.putParam("SetColorByManaCost", "True");
            newSA.putParam("SetPower", k[2]);
            newSA.putParam("SetToughness", k[3]);
            newSA.putParam("Prototype", "True");

            // need to store them for additional copies
            newSA.getOriginalMapParams().putAll(newSA.getMapParams());

            // only makes description for prompt
            newSA.setDescription(k[0] + " " + ManaCostParser.parse(k[1]) + " [" + k[2] + "/" + k[3] + "]");

            newSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(newSA);
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
        } else if (keyword.startsWith("Reconfigure")) {
            if (!keyword.contains(":")) {
                System.err.println("Malformed Reconfigure entry! - Card: " + card.toString());
                return;
            }
            String[] k = keyword.split(":");
            String bothStr = "| Cost$ " + k[1] + " | SorcerySpeed$ True | Reconfigure$ True | PrecostDesc$ Reconfigure ";
            final StringBuilder attachStr = new StringBuilder();
            attachStr.append("AB$ Attach | ValidTgts$ Creature.YouCtrl+Other | TgtPrompt$ Select target creature you ");
            attachStr.append("control | AILogic$ Pump | Secondary$ True | SpellDescription$ Attach ").append(bothStr);
            final StringBuilder unattachStr = new StringBuilder();
            unattachStr.append("AB$ Unattach | Defined$ Self | SpellDescription$ Unattach | Secondary$ True | IsPresent$ Card.Self+AttachedTo Creature");
            unattachStr.append(bothStr);
            // instantiate attach ability
            SpellAbility attachSA = AbilityFactory.getAbility(attachStr.toString(), card);
            attachSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(attachSA);
            // instantiate unattach ability
            SpellAbility unattachSA = AbilityFactory.getAbility(unattachStr.toString(), card);
            unattachSA.setIntrinsic(intrinsic);
            inst.addSpellAbility(unattachSA);
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
            sa.setSVar("ScavengeX", "Exiled$CardPower");
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Encore")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            final String effect = "AB$ CopyPermanent | Cost$ " + manacost + " ExileFromGrave<1/CARDNAME> | ActivationZone$ Graveyard" +
                    "| SorcerySpeed$ True | Defined$ Self | PumpKeywords$ Haste | RememberTokens$ True | ForEach$ Opponent" +
                    "| AtEOT$ Sacrifice | PrecostDesc$ Encore | CostDesc$ " + ManaCostParser.parse(manacost) +
                    "| SpellDescription$ (" + inst.getReminderText() + ")";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);

            final String repeatStr = "DB$ RepeatEach | DefinedCards$ Remembered | UseImprinted$ True";
            final AbilitySub repeatSub = (AbilitySub) AbilityFactory.getAbility(repeatStr, card);
            sa.setSubAbility(repeatSub);

            final String effectStr = "DB$ Effect | RememberObjects$ Imprinted,ImprintedRemembered | ExileOnMoved$ Battlefield | StaticAbilities$ AttackChosen";
            final AbilitySub effectSub = (AbilitySub) AbilityFactory.getAbility(effectStr, card);
            repeatSub.setAdditionalAbility("RepeatSubAbility", effectSub);

            final String attackStaticStr = "Mode$ MustAttack | ValidCreature$ Card.IsRemembered | MustAttack$ RememberedPlayer" +
                    " | Description$ This token copy attacks that opponent this turn if able.";
            effectSub.setSVar("AttackChosen", attackStaticStr);

            final String cleanStr = "DB$ Cleanup | Defined$ Imprinted | ForgetDefined$ Remembered";
            final AbilitySub cleanSub = (AbilitySub) AbilityFactory.getAbility(cleanStr, card);
            effectSub.setSubAbility(cleanSub);

            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Specialize")) {
            final String[] k = keyword.split(":");
            final String cost = k[1];
            String flavor = k.length > 2 && !k[2].isEmpty() ? k[2] + " – " : "";
            String condition = k.length > 3 && !k[3].isEmpty() ? ". " + k[3] : "";
            String extra = k.length > 4 && !k[4].isEmpty() ? k[4] + " | " : "";

            final String effect = "AB$ SetState | Cost$ " + cost + " ChooseColor<1> Discard<1/Card.ChosenColor;" +
                    "Card.AssociatedWithChosenColor/card of the chosen color or its associated basic land type> | " +
                    "Mode$ Specialize | SorcerySpeed$ True | " + extra + "PrecostDesc$ " + flavor + "Specialize | " +
                    "CostDesc$ " + ManaCostParser.parse(cost) + condition + " | SpellDescription$ (" +
                    inst.getReminderText() + ")";

            final SpellAbility sa = AbilityFactory.getAbility(effect, card);
            sa.setIntrinsic(intrinsic);
            inst.addSpellAbility(sa);
        } else if (keyword.startsWith("Spectacle")) {
            final String[] k = keyword.split(":");
            final Cost cost = new Cost(k[1], false);
            final SpellAbility newSA = card.getFirstSpellAbility().copyWithDefinedCost(cost);

            newSA.setAlternativeCost(AlternativeCost.Spectacle);

            String desc = "Spectacle " + cost.toSimpleString() + " (" + inst.getReminderText() + ")";
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

                    if (this.getHostCard().getGame().getStack().isSplitSecondOnStack()) {
                        return false;
                    }

                    if (StaticAbilityCantBeCast.cantBeCastAbility(this, this.getHostCard(), this.getActivatingPlayer())) {
                        return false;
                    }

                    return this.getHostCard().getFirstSpellAbility().canCastTiming(this.getHostCard(), this.getActivatingPlayer());
                }

                @Override
                public void resolve() {
                    final Game game = this.getHostCard().getGame();
                    final Card c = game.getAction().exile(new CardCollection(getHostCard()), this, null).get(0);

                    int counters = AbilityUtils.calculateAmount(c, k[1], this);
                    GameEntityCounterTable table = new GameEntityCounterTable();
                    c.addCounter(CounterEnumType.TIME, counters, getActivatingPlayer(), table);
                    table.replaceCounterEffect(game, this, false); // this is a special Action, not an Effect

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
                    + " | SorcerySpeed$ True | StackDescription$ SpellDescription | SpellDescription$ ("
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
                    + " | SorcerySpeed$ True | StackDescription$ SpellDescription | SpellDescription$ ("
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
                    " | PrecostDesc$ Unearth | CostDesc$ " + ManaCostParser.parse(manacost) + " | StackDescription$ " +
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
            String effect = "AB$ Animate | Cost$ tapXType<Any/Creature.Other+withTotalPowerGE" + power + "> | " +
                    "CostDesc$ Crew " + power + " (Tap any number of creatures you control with total power " + power +
                    " or more: | Crew$ True | Secondary$ True | Defined$ Self | Types$ Artifact,Creature | " +
                    "SpellDescription$ CARDNAME becomes an artifact creature until end of turn.)";

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
            } else if (type.equals("Land.Artifact")) {
                desc = "Artifact land";
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

        if (keyword.startsWith("Affinity")) {
            final String[] k = keyword.split(":");
            final String t = k[1];
            String d = "";
            if (k.length > 2) {
                final StringBuilder s = new StringBuilder();
                s.append(k[2]).append("s");
                d = s.toString();
            }

            String desc = "Artifact".equals(t) ? "artifacts" : CardType.getPluralType(t);
            if (!d.isEmpty()) {
                desc = d;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Mode$ ReduceCost | ValidCard$ Card.Self | Type$ Spell | Amount$ AffinityX | EffectZone$ All");
            sb.append("| Description$ Affinity for ").append(desc);
            sb.append(" (").append(inst.getReminderText()).append(")");
            String effect = sb.toString();

            StaticAbility st = StaticAbility.create(effect, state.getCard(), state, intrinsic);

            StringBuilder sb2 = new StringBuilder();
            sb2.append("Count$Valid ").append(t).append(t.contains(".") ? "+" : ".").append("YouCtrl");
            st.setSVar("AffinityX", sb2.toString());
            inst.addStaticAbility(st);
        } else if (keyword.startsWith("Blitz")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];
            final Cost cost = new Cost(manacost, false);

            StringBuilder sb = new StringBuilder("Blitz");
            if (!cost.isOnlyManaCost()) {
                sb.append("—");
            } else {
                sb.append(" ");
            }
            sb.append(cost.toSimpleString());
            String effect = "Mode$ Continuous | Affected$ Card.Self+blitzed+castKeyword | AddKeyword$ Haste | AddTrigger$ Dies"
                    + " | Secondary$ True | Description$ " + sb.toString() + " (" + inst.getReminderText() + ")";
            String trig = "Mode$ ChangesZone | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self" +
                    " | Execute$ TrigDraw | Secondary$ True | TriggerDescription$ When this creature dies, draw a card.";
            String ab = "DB$ Draw | NumCards$ 1";

            StaticAbility st = StaticAbility.create(effect, state.getCard(), state, intrinsic);

            st.setSVar("Dies", trig);
            st.setSVar("TrigDraw", ab);

            inst.addStaticAbility(st);
        } else if (keyword.equals("Changeling")) {
            String effect = "Mode$ Continuous | EffectZone$ All | Affected$ Card.Self" +
                    " | CharacteristicDefining$ True | AddAllCreatureTypes$ True | Secondary$ True" +
                    " | Description$ Changeling (" + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Cipher")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Mode$ Continuous | EffectZone$ Exile | Affected$ Card.EncodedWithSource");
            sb.append(" | AddTrigger$ CipherTrigger");
            sb.append(" | Description$ Cipher (").append(inst.getReminderText()).append(")");

            String effect = sb.toString();

            sb = new StringBuilder();

            sb.append("Mode$ DamageDone | ValidSource$ Card.Self | ValidTarget$ Player | Execute$ PlayEncoded");
            sb.append(" | CombatDamage$ True | OptionalDecider$ You | TriggerDescription$ ");
            sb.append("Whenever CARDNAME deals combat damage to a player, its controller may cast a copy of ");
            sb.append(state.getName()).append(" without paying its mana cost.");

            String trig = sb.toString();

            String ab = "DB$ Play | Defined$ OriginalHost | WithoutManaCost$ True | CopyCard$ True";

            StaticAbility st = StaticAbility.create(effect, state.getCard(), state, intrinsic);

            st.setSVar("CipherTrigger", trig);
            st.setSVar("PlayEncoded", ab);

            inst.addStaticAbility(st);
        } else if (keyword.startsWith("Class")) {
            final String[] k = keyword.split(":");
            final String level = k[1];
            final String params = k[3];

            // get Description from CardTraits
            StringBuilder desc = new StringBuilder();
            boolean descAdded = false;
            Map<String, String> mapParams = AbilityFactory.getMapParams(params);
            if (mapParams.containsKey("AddTrigger")) {
                for (String s : mapParams.get("AddTrigger").split(" & ")) {
                    if (descAdded) {
                        desc.append("\r\n");
                    }
                    desc.append(AbilityFactory.getMapParams(state.getSVar(s)).get("TriggerDescription"));
                    descAdded = true;
                }
            }
            if (mapParams.containsKey("AddStaticAbility")) {
                for (String s : mapParams.get("AddStaticAbility").split(" & ")) {
                    if (descAdded) {
                        desc.append("\r\n");
                    }
                    desc.append(AbilityFactory.getMapParams(state.getSVar(s)).get("Description"));
                    descAdded = true;
                }
            }

            String effect = "Mode$ Continuous | Affected$ Card.Self | ClassLevel$ " + level + " | " + params;
            if (descAdded) {
                effect += " | Description$ " + desc.toString();
            }

            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.startsWith("Dash")) {
            String effect = "Mode$ Continuous | Affected$ Card.Self+dashed+castKeyword | AddKeyword$ Haste";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Daybound")) {
            String effect = "Mode$ CantTransform | ValidCard$ Creature.Self | ExceptCause$ SpellAbility.Daybound | Secondary$ True | Description$ This permanent can't be transformed except by its daybound ability.";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Decayed")) {
            String effect = "Mode$ CantBlockBy | ValidBlocker$ Creature.Self | Secondary$ True | Description$ CARDNAME can't block.";
            StaticAbility st = StaticAbility.create(effect, state.getCard(), state, intrinsic);
            inst.addStaticAbility(st);
        } else if (keyword.equals("Defender")) {
            String effect = "Mode$ CantAttack | ValidCard$ Card.Self | DefenderKeyword$ True | Secondary$ True";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Devoid")) {
            String effect = "Mode$ Continuous | EffectZone$ All | Affected$ Card.Self" +
                    " | CharacteristicDefining$ True | SetColor$ Colorless | Secondary$ True" +
                    " | Description$ Devoid (" + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
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

            String effect = "Mode$ RaiseCost | ValidCard$ Card.Self | Type$ Spell | Secondary$ True"
                    + " | Amount$ Escalate | Cost$ "+ manacost +" | EffectZone$ All"
                    + " | Description$ " + sb.toString() + " (" + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Enlist")) {
            String effect = "Mode$ OptionalAttackCost | ValidCard$ Card.Self | Cost$ Enlist<1/CARDNAME/creature> | Secondary$ True" +
                    "| Trigger$ TrigEnlist | Description$ Enlist ( " + inst.getReminderText() + ")";
            StaticAbility st = StaticAbility.create(effect, state.getCard(), state, intrinsic);
            st.setSVar("TrigEnlist", "DB$ Pump | NumAtt$ TriggerRemembered$CardPower" +
            " | SpellDescription$ When you do, add its power to this creature's until end of turn.");
            inst.addStaticAbility(st);
        } else if (keyword.equals("Fear")) {
            String effect = "Mode$ CantBlockBy | ValidAttacker$ Creature.Self | ValidBlocker$ Creature.nonArtifact+nonBlack | Secondary$ True" +
                    " | Description$ Fear ( " + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Flying")) {
            String effect = "Mode$ CantBlockBy | ValidAttacker$ Creature.Self | ValidBlocker$ Creature.withoutFlying+withoutReach | Secondary$ True" +
                    " | Description$ Flying ( " + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.startsWith("Hexproof")) {
            final StringBuilder sbDesc = new StringBuilder("Hexproof");
            final StringBuilder sbValid = new StringBuilder();

            if (!keyword.equals("Hexproof")) {
                final String[] k = keyword.split(":");

                sbDesc.append(" from ").append(k[2]);
                sbValid.append("| ValidSource$ ").append(k[1]);
            }

            String effect = "Mode$ CantTarget | Hexproof$ True | ValidCard$ Card.Self | Secondary$ True"
                    + sbValid.toString() + " | Activator$ Opponent | Description$ "
                    + sbDesc.toString() + " (" + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Horsemanship")) {
            String effect = "Mode$ CantBlockBy | ValidAttacker$ Creature.Self | ValidBlocker$ Creature.withoutHorsemanship | Secondary$ True " +
                    " | Description$ Horsemanship ( " + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Intimidate")) {
            String effect = "Mode$ CantBlockBy | ValidAttacker$ Creature.Self | ValidBlocker$ Creature.nonArtifact+notSharesColorWith | Secondary$ True " +
                    " | Description$ Intimidate ( " + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Living metal")) {
            String effect = "Mode$ Continuous | Affected$ Card.Self | AddType$ Creature | Condition$ PlayerTurn | Secondary$ True";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Nightbound")) {
            String effect = "Mode$ CantTransform | ValidCard$ Creature.Self | ExceptCause$ SpellAbility.Nightbound | Secondary$ True | Description$ This permanent can't be transformed except by its nightbound ability.";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.startsWith("Protection")) {
            String valid = getProtectionValid(keyword, false);

            // Block
            String effect = "Mode$ CantBlockBy | ValidAttacker$ Creature.Self | Secondary$ True ";
            String desc = "Protection ( " + inst.getReminderText() + ")";
            if (!valid.isEmpty()) {
                effect += "| ValidBlocker$ " + valid;
            }
            effect += " | Description$ " + desc;
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));

            // Target
            effect = "Mode$ CantTarget | Protection$ True | ValidCard$ Card.Self | Secondary$ True ";
            if (!valid.isEmpty()) {
                effect += "| ValidSource$ " + valid;
            }
            effect += " | Description$ " + desc;
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));

            // Attach
            effect = "Mode$ CantAttach | Protection$ True | Target$ Card.Self | Secondary$ True ";
            if (!valid.isEmpty()) {
                effect += "| ValidCard$ " + valid;
            }
            // This effect doesn't remove something
            if (keyword.startsWith("Protection:")) {
                final String[] kws = keyword.split(":");
                if (kws.length > 3) {
                    effect += "| Exceptions$ " + kws[3];
                }
                if (kws.length > 4) {
                    effect += " | ExceptionSBA$ True";
                }
            }
            effect += " | Description$ " + desc;
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.startsWith("Read ahead")) {
            String effect = "Mode$ DisableTriggers | ValidCard$ Card.Self+ThisTurnEntered | ValidTrigger$ Triggered.ChapterNotLore | Secondary$ True" +
                    " | Description$ Chapter abilities of this Saga can't trigger the turn it entered the battlefield unless it has exactly the number of lore counters on it specified in the chapter symbol of that ability.";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Shroud")) {
            String effect = "Mode$ CantTarget | Shroud$ True | ValidCard$ Card.Self | Secondary$ True"
                    + " | Description$ Shroud (" + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Skulk")) {
            String effect = "Mode$ CantBlockBy | ValidAttacker$ Creature.Self | ValidBlocker$ Creature.powerGTX | Secondary$ True " +
                    " | Description$ Skulk ( " + inst.getReminderText() + ")";
            StaticAbility st = StaticAbility.create(effect, state.getCard(), state, intrinsic);
            st.setSVar("X", "Count$CardPower");
            inst.addStaticAbility(st);
        } else if (keyword.startsWith("Strive")) {
            final String[] k = keyword.split(":");
            final String manacost = k[1];

            String effect = "Mode$ RaiseCost | ValidCard$ Card.Self | Type$ Spell | Amount$ Strive | Cost$ "+ manacost +" | EffectZone$ All" +
                    " | Description$ Strive - " + inst.getReminderText();
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Unleash")) {
            String effect = "Mode$ CantBlockBy | ValidBlocker$ Creature.Self+counters_GE1_P1P1 | Secondary$ True | Description$ CARDNAME can't block.";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Undaunted")) {
            String effect = "Mode$ ReduceCost | ValidCard$ Card.Self | Type$ Spell | Secondary$ True"
                    + "| Amount$ Undaunted | EffectZone$ All | Description$ Undaunted (" + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("Vigilance")) {
            String effect = "Mode$ AttackVigilance | ValidCard$ Card.Self | Secondary$ True | Description$ Vigilance (" + inst.getReminderText() + ")";
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
        } else if (keyword.equals("MayFlashSac")) {
            String effect = "Mode$ Continuous | EffectZone$ All | Affected$ Card.Self | Secondary$ True | MayPlay$ True"
                + " | MayPlayNotSorcerySpeed$ True | MayPlayWithFlash$ True | MayPlayText$ Sacrifice at the next cleanup step"
                + " | AffectedZone$ Exile,Graveyard,Hand,Library,Stack | Description$ " + inst.getReminderText();
            inst.addStaticAbility(StaticAbility.create(effect, state.getCard(), state, intrinsic));
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

        final Cost abCost = new Cost(params.get("Cost"), sa.isAbility());
        final SpellAbility altCostSA = sa.copyWithDefinedCost(abCost);
        altCostSA.setBasicSpell(false);
        altCostSA.addOptionalCost(OptionalCost.AltCost);

        final SpellAbilityRestriction restriction = new SpellAbilityRestriction();
        restriction.setRestrictions(params);
        if (!params.containsKey("ActivationZone")) {
            restriction.setZone(ZoneType.Hand);
        }
        altCostSA.setRestrictions(restriction);

        String costDescription = TextUtil.fastReplace(params.get("Description"), "CARDNAME", card.getName());
        if (costDescription == null || costDescription.isEmpty()) {
            costDescription = TextUtil.concatWithSpace("You may", abCost.toStringAlt(), "rather than pay", TextUtil.addSuffix(card.getName(), "'s mana cost."));
        }

        altCostSA.setDescription(costDescription);

        if (params.containsKey("StackDescription")) {
            altCostSA.setStackDescription(params.get("StackDescription"));
        }

        if (params.containsKey("Announce")) {
            altCostSA.addAnnounceVar(params.get("Announce"));
        }

        if (params.containsKey("ManaRestriction")) {
            altCostSA.putParam("ManaRestriction", params.get("ManaRestriction"));
        }

        return altCostSA;
    }

    public static void setupSiegeAbilities(Card card) {
        StringBuilder chooseSB = new StringBuilder();
        chooseSB.append("Event$ Moved | ValidCard$ Card.Self | Destination$ Battlefield | ReplacementResult$ Updated");
        chooseSB.append(" | Description$ (As a Siege enters the battlefield, choose an opponent to protect it. You and others can attack it. When it's defeated, exile it, then cast it transformed.)");
        String chooseProtector = "DB$ ChoosePlayer | Defined$ You | Choices$ Opponent | Protect$ True | ChoiceTitle$ Choose an opponent to protect this battle";

        ReplacementEffect re = ReplacementHandler.parseReplacement(chooseSB.toString(), card, true);
        re.setOverridingAbility(AbilityFactory.getAbility(chooseProtector, card));
        card.addReplacementEffect(re);

        // Defeated trigger
        StringBuilder triggerDefeated = new StringBuilder();
        triggerDefeated.append("Mode$ CounterRemovedOnce | ValidCard$ Card.Self | Secondary$ True | CounterType$ DEFENSE | Remaining$ 0 | TriggerZones$ Battlefield | ");
        triggerDefeated.append(" TriggerDescription$ When CARDNAME is defeated, exile it, then cast it transformed.");

        String castExileBattle = "DB$ ChangeZone | Defined$ Self | Origin$ Battlefield | Destination$ Exile | RememberChanged$ True";
        // note full rules text:
        // When the last defense counter is removed from this permanent, exile it, then you may cast it transformed
        // without paying its mana cost.
        String castDefeatedBattle = "DB$ Play | Defined$ Remembered | WithoutManaCost$ True | Optional$ True | " +
                "CastTransformed$ True";

        Trigger defeatedTrigger = TriggerHandler.parseTrigger(triggerDefeated.toString(), card, true);
        SpellAbility exileAbility = AbilityFactory.getAbility(castExileBattle, card);
        AbilitySub castAbility = (AbilitySub)AbilityFactory.getAbility(castDefeatedBattle, card);

        exileAbility.setSubAbility(castAbility);
        defeatedTrigger.setOverridingAbility(exileAbility);
        card.addTrigger(defeatedTrigger);
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

        String abEffect = "DB$ Effect | RememberObjects$ Self | StaticAbilities$ Play | ForgetOnMoved$ Exile | Duration$ Permanent | ConditionDefined$ Self | ConditionPresent$ Card.nonCopiedSpell";
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

    public static void setFaceDownState(Card c, SpellAbility sa) {
        final Card source = sa.getHostCard();
        CardState faceDown = c.getFaceDownState();

        // set New Pt doesn't work because this values need to be copyable for clone effects
        if (sa.hasParam("FaceDownPower")) {
            faceDown.setBasePower(AbilityUtils.calculateAmount(
                    source, sa.getParam("FaceDownPower"), sa));
        }
        if (sa.hasParam("FaceDownToughness")) {
            faceDown.setBaseToughness(AbilityUtils.calculateAmount(
                    source, sa.getParam("FaceDownToughness"), sa));
        }

        if (sa.hasParam("FaceDownSetType")) {
            faceDown.setType(new CardType(Arrays.asList(sa.getParam("FaceDownSetType").split(" & ")), false));
        }

        if (sa.hasParam("FaceDownPower") || sa.hasParam("FaceDownToughness")
                || sa.hasParam("FaceDownSetType")) {
            final GameCommand unanimate = new GameCommand() {
                private static final long serialVersionUID = 8853789549297846163L;

                @Override
                public void run() {
                    c.clearStates(CardStateName.FaceDown, true);
                }
            };

            c.addFaceupCommand(unanimate);
        }
    }
}
