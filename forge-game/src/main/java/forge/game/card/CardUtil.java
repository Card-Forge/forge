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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import forge.ImageKeys;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

public final class CardUtil {
    // disable instantiation
    private CardUtil() { }

    public static final List<String> NON_STACKING_LIST = new ArrayList<String>();

    /** List of all keywords that could be modified by text changes.
     *  Mostly this is caused by them having a variable, like a cost.
     */
    public static final ImmutableList<String> modifiableKeywords = ImmutableList.<String>builder().add(
            "Enchant", "Protection", "Cumulative upkeep", "Equip", "Buyback",
            "Cycling", "Echo", "Kicker", "Flashback", "Madness", "Morph",
            "Affinity", "Entwine", "Splice", "Ninjutsu",
            "Transmute", "Replicate", "Recover", "Suspend", "Aura swap",
            "Fortify", "Transfigure", "Champion", "Evoke", "Prowl",
            "Reinforce", "Unearth", "Level up", "Miracle", "Overload",
            "Scavenge", "Bestow", "Outlast", "Dash", "Renown", "Surge").build();
    /** List of keyword endings of keywords that could be modified by text changes. */
    public static final ImmutableList<String> modifiableKeywordEndings = ImmutableList.<String>builder().add(
            "walk", "cycling", "offering").build();

    /**
     * Map of plural type names to the corresponding singular form.
     * So Clerics maps to Cleric, Demons to Demon, etc.
     */
    public static final ImmutableBiMap<String, String> singularTypes = ImmutableBiMap.<String, String>builder()
            .put("Clerics", "Cleric")
            .put("Demons", "Demon")
            .put("Dragons", "Dragon")
            .put("Goblins", "Goblin")
            .put("Gorgons", "Gorgon")
            .build();
    /**
     * Map of singular type names to the corresponding plural form.
     * So Cleric maps to Clerics, Demon to Demons, etc.
     */
    public static final ImmutableBiMap<String, String> pluralTypes = singularTypes.inverse();

    public static final boolean isKeywordModifiable(final String kw) {
        for (final String modKw : modifiableKeywords) {
            if (kw.startsWith(modKw)) {
                return true;
            }
        }
        for (final String end : modifiableKeywordEndings) {
            if (kw.endsWith(end)) {
                return true;
            }
        }
        return false;
    }

    /**
     * If the input is a plural type, return the corresponding singular form.
     * Otherwise, simply return the input.
     * @param type a String.
     * @return the corresponding type.
     */
    public static final String getSingularType(final String type) {
        if (singularTypes.containsKey(type)) {
            return singularTypes.get(type);
        }
        return type;
    }

    /**
     * If the input is a singular type, return the corresponding plural form.
     * Otherwise, simply return the input.
     * @param type a String.
     * @return the corresponding type.
     */
    public static final String getPluralType(final String type) {
        if (pluralTypes.containsKey(type)) {
            return pluralTypes.get(type);
        }
        return type;
    }

    public static ColorSet getColors(final Card c) {
        return c.determineColor();
    }

    public static boolean isStackingKeyword(final String keyword) {
        String kw = new String(keyword);
        if (kw.startsWith("HIDDEN")) {
            kw = kw.substring(7);
        }
        
        return !kw.startsWith("Protection") && !kw.startsWith("CantBeBlockedBy") 
                && !NON_STACKING_LIST.contains(kw);
    }

    public static String getShortColorsString(final Iterable<String> colors) {
        StringBuilder colorDesc = new StringBuilder();
        for (final String col : colors) {
            colorDesc.append(MagicColor.toShortString(col) + " ");
        }
        return colorDesc.toString();
    }

    /**
     * getThisTurnEntered.
     * 
     * @param to    zone going to
     * @param from  zone coming from
     * @param valid a isValid expression
     * @param src   a Card object
     * @return a CardCollection that matches the given criteria
     */
    public static CardCollection getThisTurnEntered(final ZoneType to, final ZoneType from, final String valid, final Card src) {
        return getThisTurnEntered(to, from, valid, src, true);
    }

    /**
     * getThisTurnEntered.
     * 
     * @param to    zone going to
     * @param from  zone coming from
     * @param valid a isValid expression
     * @param src   a Card object
     * @param checkLatestState  a boolean, true if the latest state of the card as it left the original zone needs to be checked
     * @return a CardCollection that matches the given criteria
     */
    public static CardCollection getThisTurnEntered(final ZoneType to, final ZoneType from, final String valid, final Card src, final boolean checkLatestState) {
        CardCollection res = new CardCollection();
        final Game game = src.getGame();
        if (to != ZoneType.Stack) {
            for (Player p : game.getPlayers()) {
                res.addAll(p.getZone(to).getCardsAddedThisTurn(from, checkLatestState));
            }
        }
        else {
            res.addAll(game.getStackZone().getCardsAddedThisTurn(from, checkLatestState));
        }
        res = CardLists.getValidCards(res, valid, src.getController(), src);
        return res;
    }

    /**
     * getLastTurnEntered.
     * 
     * @param to    zone going to
     * @param from  zone coming from
     * @param valid a isValid expression
     * @param src   a Card object
     * @return a CardCollection that matches the given criteria
     */
    public static CardCollection getLastTurnEntered(final ZoneType to, final ZoneType from, final String valid, final Card src) {
        return getLastTurnEntered(to, from, valid, src, true);
    }

    /**
     * getLastTurnEntered.
     * 
     * @param to    zone going to
     * @param from  zone coming from
     * @param valid a isValid expression
     * @param src   a Card object
     * @return a CardCollection that matches the given criteria
     */
    public static CardCollection getLastTurnEntered(final ZoneType to, final ZoneType from, final String valid, final Card src, final boolean checkLatestState) {
        CardCollection res = new CardCollection();
        final Game game = src.getGame();
        if (to != ZoneType.Stack) {
            for (Player p : game.getPlayers()) {
                res.addAll(p.getZone(to).getCardsAddedLastTurn(from, checkLatestState));
            }
        }
        else {
            res.addAll(game.getStackZone().getCardsAddedLastTurn(from, checkLatestState));
        }
        res = CardLists.getValidCards(res, valid, src.getController(), src);
        return res;
    }

    public static List<Card> getThisTurnCast(final String valid, final Card src) {
        List<Card> res = new ArrayList<Card>();
        final Game game = src.getGame();
        res.addAll(game.getStack().getSpellsCastThisTurn());

        res = CardLists.getValidCardsAsList(res, valid, src.getController(), src);

        return res;
    }

    public static List<Card> getLastTurnCast(final String valid, final Card src) {
        List<Card> res = new ArrayList<Card>();
        final Game game = src.getGame();
        res.addAll(game.getStack().getSpellsCastLastTurn());

        res = CardLists.getValidCardsAsList(res, valid, src.getController(), src);

        return res;
    }

    /**
     * @param in  a Card to copy.
     * @return a copy of C with LastKnownInfo stuff retained.
     */
    public static Card getLKICopy(final Card in) {
        final Card newCopy = new Card(in.getId(), in.getPaperCard(), false, in.getGame());
        newCopy.setSetCode(in.getSetCode());
        newCopy.setOwner(in.getOwner());
        newCopy.setController(in.getController(), 0);
        newCopy.getCurrentState().copyFrom(in, in.getState(in.getCurrentStateName()));
        if (in.isCloned()) {
            newCopy.addAlternateState(CardStateName.Cloner, false);
        }
        newCopy.setType(new CardType(in.getType()));
        newCopy.setToken(in.isToken());
        newCopy.setTriggers(in.getTriggers(), false);
        for (SpellAbility sa : in.getSpellAbilities()) {
            newCopy.addSpellAbility(sa);
            sa.setHostCard(in);
        }

        // lock in the current P/T without bonus from counters
        newCopy.setBasePower(in.getCurrentPower() + in.getTempPowerBoost() + in.getSemiPermanentPowerBoost());
        newCopy.setBaseToughness(in.getCurrentToughness() + in.getTempToughnessBoost() + in.getSemiPermanentToughnessBoost());

        newCopy.setCounters(in.getCounters());
        newCopy.setExtrinsicKeyword(in.getExtrinsicKeyword());

        newCopy.setColor(in.determineColor().getColor());
        newCopy.setReceivedDamageFromThisTurn(in.getReceivedDamageFromThisTurn());
        newCopy.getDamageHistory().setCreatureGotBlockedThisTurn(in.getDamageHistory().getCreatureGotBlockedThisTurn());
        newCopy.setEnchanting(in.getEnchanting());
        newCopy.setEnchantedBy(in.getEnchantedBy(false));
        newCopy.setEquipping(in.getEquipping());
        newCopy.setEquippedBy(in.getEquippedBy(false));
        newCopy.setFortifying(in.getFortifying());
        newCopy.setFortifiedBy(in.getFortifiedBy(false));
        newCopy.setClones(in.getClones());
        newCopy.setHaunting(in.getHaunting());
        for (final Card haunter : in.getHauntedBy()) {
            newCopy.addHauntedBy(haunter);
        }
        for (final Object o : in.getRemembered()) {
            newCopy.addRemembered(o);
        }
        for (final Card o : in.getImprintedCards()) {
            newCopy.addImprintedCard(o);
        }

        newCopy.setChangedCardColors(in.getChangedCardColors());
        newCopy.setChangedCardKeywords(in.getChangedCardKeywords());
        newCopy.setChangedCardTypes(in.getChangedCardTypes());

        return newCopy;
    }

    public static CardCollection getRadiance(final Card source, final Card origin, final String[] valid) {
        final CardCollection res = new CardCollection();

        final Game game = source.getGame();
        ColorSet cs = CardUtil.getColors(origin);
        for (byte color : MagicColor.WUBRG) {
            if(!cs.hasAnyColor(color)) 
                continue;
            
            for(final Card c : game.getColoredCardsInPlay(MagicColor.toLongString(color))) {
                if (!res.contains(c) && c.isValid(valid, source.getController(), source, null) && !c.equals(origin)) {
                    res.add(c);
                }
            }
        }
        return res;
    }

    public static CardState getFaceDownCharacteristic(Card c) {
        final CardType type = new CardType();
        type.add("Creature");

        final CardState ret = new CardState(c.getView().createAlternateState(CardStateName.FaceDown), c);
        ret.setBasePower(2);
        ret.setBaseToughness(2);

        ret.setName("");
        ret.setType(type);

        ret.setImageKey(ImageKeys.getTokenKey(ImageKeys.MORPH_IMAGE));
        return ret;
    }

    // a nice entry point with minimum parameters
    public static Set<String> getReflectableManaColors(final SpellAbility sa) {
        return getReflectableManaColors(sa, sa, new HashSet<String>(), new CardCollection());
    }
    
    private static Set<String> getReflectableManaColors(final SpellAbility abMana, final SpellAbility sa,
            Set<String> colors, final CardCollection parents) {
        // Here's the problem with reflectable Mana. If more than one is out,
        // they need to Reflect each other,
        // so we basically need to have a recursive list that send the parents
        // so we don't infinite recurse.
        final Card card = abMana.getHostCard();
        
        if (abMana.getApi() != ApiType.ManaReflected) {
            return colors;
        }

        if (!parents.contains(card)) {
            parents.add(card);
        }

        final String colorOrType = sa.getParam("ColorOrType"); 
        // currently Color or Type, Type is colors + colorless
        final String validCard = sa.getParam("Valid");
        final String reflectProperty = sa.getParam("ReflectProperty");
        // Produce (Reflecting Pool) or Is (Meteor Crater)

        int maxChoices = 5; // Color is the default colorOrType
        if (colorOrType.equals("Type")) {
            maxChoices++;
        }

        CardCollection cards = null;

        // Reuse AF_Defined in a slightly different way
        if (validCard.startsWith("Defined.")) {
            cards = AbilityUtils.getDefinedCards(card, validCard.replace("Defined.", ""), abMana);
        } else {
            if (sa.getActivatingPlayer() == null) {
                sa.setActivatingPlayer(sa.getHostCard().getController());
            }
            final Game game = sa.getActivatingPlayer().getGame();
            cards = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), validCard, abMana.getActivatingPlayer(), card);
        }

        // remove anything cards that is already in parents
        for (final Card p : parents) {
            if (cards.contains(p)) {
                cards.remove(p);
            }
        }

        if ((cards.size() == 0) && !reflectProperty.equals("Produced")) {
            return colors;
        }

        if (reflectProperty.equals("Is")) { // Meteor Crater
            for (final Card card1 : cards) {
                // For each card, go through all the colors and if the card is that color, add
                for (final String col : MagicColor.Constant.ONLY_COLORS) {
                    if (card1.isOfColor(col)) {
                        colors.add(col);
                        if (colors.size() == maxChoices) {
                            break;
                        }
                    }
                }
            }
        } else if (reflectProperty.equals("Produced")) {
            // Why is this name so similar to the one below?
            final String producedColors = abMana instanceof AbilitySub ? (String) abMana.getRootAbility().getTriggeringObject("Produced") : (String) abMana.getTriggeringObject("Produced");
            for (final String col : MagicColor.Constant.ONLY_COLORS) {
                final String s = MagicColor.toShortString(col);
                if (producedColors.contains(s)) {
                    colors.add(col);
                }
            }
            // TODO Sol Remove production of "1" Generic Mana
            if (maxChoices == 6 && (producedColors.contains("1") || producedColors.contains("C"))) {
                colors.add(MagicColor.Constant.COLORLESS);
            }
        } else if (reflectProperty.equals("Produce")) {
            final FCollection<SpellAbility> abilities = new FCollection<SpellAbility>();
            for (final Card c : cards) {
                abilities.addAll(c.getManaAbilities());
            }

            final List<SpellAbility> reflectAbilities = new ArrayList<SpellAbility>();

            for (final SpellAbility ab : abilities) {
                if (maxChoices == colors.size()) {
                    break;
                }

                if (ab.getApi() == ApiType.ManaReflected) {
                    if (!parents.contains(ab.getHostCard())) {
                        // Recursion! Set Activator to controller for appropriate valid comparison
                        ab.setActivatingPlayer(ab.getHostCard().getController());
                        reflectAbilities.add(ab);
                        parents.add(ab.getHostCard());
                    }
                    continue;
                }
                colors = canProduce(maxChoices, ab.getManaPart(), colors);
                if (!parents.contains(ab.getHostCard())) {
                    parents.add(ab.getHostCard());
                }
            }

            for (final SpellAbility ab : reflectAbilities) {
                if (maxChoices == colors.size()) {
                    break;
                }

                colors = CardUtil.getReflectableManaColors(ab, sa, colors, parents);
            }
        }
        return colors;
    }
    
    public static Set<String> canProduce(final int maxChoices, final AbilityManaPart ab,
            final Set<String> colors) {
        if (ab == null) {
            return colors;
        }
        for (final String col : MagicColor.Constant.ONLY_COLORS) {
            final String s = MagicColor.toShortString(col);
            if (ab.canProduce(s)) {
                colors.add(col);
            }
        }

        // TODO Sol Remove production of "1" Generic Mana
        if (maxChoices == 6 && (ab.canProduce("1") || ab.canProduce("C"))) {
            colors.add(MagicColor.Constant.COLORLESS);
        }

        return colors;
    }

    // these have been copied over from CardFactoryUtil as they need two extra
    // parameters for target selection.
    // however, due to the changes necessary for SA_Requirements this is much
    // different than the original
    public static List<Card> getValidCardsToTarget(TargetRestrictions tgt, SpellAbility ability) {
        final Game game = ability.getActivatingPlayer().getGame();
        final List<ZoneType> zone = tgt.getZone();

        final boolean canTgtStack = zone.contains(ZoneType.Stack);
        List<Card> validCards = CardLists.getValidCards(game.getCardsIn(zone), tgt.getValidTgts(), ability.getActivatingPlayer(), ability.getHostCard(), ability);
        List<Card> choices = CardLists.getTargetableCards(validCards, ability);
        if (canTgtStack) {
            // Since getTargetableCards doesn't have additional checks if one of the Zones is stack
            // Remove the activating card from targeting itself if its on the Stack
            Card activatingCard = ability.getHostCard();
            if (activatingCard.isInZone(ZoneType.Stack)) {
                choices.remove(ability.getHostCard());
            }
        }
        List<GameObject> targetedObjects = ability.getUniqueTargets();

        // Remove cards already targeted
        final List<Card> targeted = Lists.newArrayList(ability.getTargets().getTargetCards());
        for (final Card c : targeted) {
            if (choices.contains(c)) {
                choices.remove(c);
            }
        }

        // Remove cards exceeding total CMC
        if (ability.hasParam("MaxTotalTargetCMC")) {
            int totalCMCTargeted = 0;
            for (final Card c : targeted) {
                totalCMCTargeted += c.getCMC(); 
            }

            final List<Card> choicesCopy = Lists.newArrayList(choices);
            for (final Card c : choicesCopy) {
                if (c.getCMC() > tgt.getMaxTotalCMC(c, ability) - totalCMCTargeted) {
                    choices.remove(c);
                }
            }
        }

        // If all cards (including subability targets) must have the same controller
        if (tgt.isSameController() && !targetedObjects.isEmpty()) {
            final List<Card> list = new ArrayList<Card>();
            for (final Object o : targetedObjects) {
                if (o instanceof Card) {
                    list.add((Card) o);
                }
            }
            if (!list.isEmpty()) {
                final Card card = list.get(0);
                choices = CardLists.filter(choices, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return c.sharesControllerWith(card);
                    }
                });
            }
        }

        return choices;
    }
}
