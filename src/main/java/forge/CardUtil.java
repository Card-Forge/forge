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
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.card.CardCharacteristics;
import forge.card.MagicColor;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.properties.NewConstants;

public final class CardUtil {
    // disable instantiation
    private CardUtil() { }

    static final Map<String, String> colorMap;
    static {
        colorMap = new HashMap<String, String>();
        colorMap.put(Constant.Color.BLACK.toString(), "B");
        colorMap.put(Constant.Color.BLUE.toString(), "U");
        colorMap.put(Constant.Color.GREEN.toString(), "G");
        colorMap.put(Constant.Color.RED.toString(), "R");
        colorMap.put(Constant.Color.WHITE.toString(), "W");
        colorMap.put(Constant.Color.COLORLESS.toString(), "C");
    }
    
    // returns "G", longColor is Constant.Color.Green and the like
    public static String getShortColor(final String longColor) {
        if (!colorMap.containsKey(longColor)) {
            throw new RuntimeException("CardUtil : getShortColor() invalid argument - " + longColor);
        }
        return colorMap.get(longColor);
    }

    public static List<String> getColors(final Card c) {
        return c.determineColor().toStringList();
    }

    public static boolean isStackingKeyword(final String keyword) {
        String kw = new String(keyword);
        if (kw.startsWith("HIDDEN")) {
            kw = kw.substring(7);
        }
        
        return !kw.startsWith("Protection") && !Constant.Keywords.NON_STACKING_LIST.contains(kw);
    }

    public static String getShortColorsString(final ArrayList<String> colors) {
        StringBuilder colorDesc = new StringBuilder();
        for (final String col : colors) {
            colorDesc.append(getShortColor(col));
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
     * @return a List<Card> that matches the given criteria
     */
    public static List<Card> getThisTurnEntered(final ZoneType to, final ZoneType from, final String valid,
            final Card src) {
        List<Card> res = new ArrayList<Card>();
        if (to != ZoneType.Stack) {
            for (Player p : Singletons.getModel().getGame().getPlayers()) {
                res.addAll(p.getZone(to).getCardsAddedThisTurn(from));
            }
        } else {
            res.addAll(Singletons.getModel().getGame().getStackZone().getCardsAddedThisTurn(from));
        }

        res = CardLists.getValidCards(res, valid, src.getController(), src);

        return res;
    }

    public static List<Card> getThisTurnCast(final String valid, final Card src) {
        List<Card> res = new ArrayList<Card>();

        res.addAll(Singletons.getModel().getGame().getStack().getCardsCastThisTurn());

        res = CardLists.getValidCards(res, valid, src.getController(), src);

        return res;
    }

    public static List<Card> getLastTurnCast(final String valid, final Card src) {
        List<Card> res = new ArrayList<Card>();

        res.addAll(Singletons.getModel().getGame().getStack().getCardsCastLastTurn());

        res = CardLists.getValidCards(res, valid, src.getController(), src);

        return res;
    }

    /**
     * @param in  a Card to copy.
     * @return a copy of C with LastKnownInfo stuff retained.
     */
    public static Card getLKICopy(final Card in) {
        if (in.isToken()) {
            return in;
        }

        final Card newCopy = new Card();
        newCopy.setUniqueNumber(in.getUniqueNumber());
        newCopy.setCurSetCode(in.getCurSetCode());
        newCopy.setOwner(in.getOwner());
        newCopy.setFlipCard(in.isFlipCard());
        newCopy.setDoubleFaced(in.isDoubleFaced());
        newCopy.getCharacteristics().copy(in.getState(in.getCurState()));
        newCopy.setBaseAttack(in.getNetAttack());
        newCopy.setBaseDefense(in.getNetDefense());
        newCopy.setType(new ArrayList<String>(in.getType()));
        newCopy.setTriggers(in.getTriggers());
        for (SpellAbility sa : in.getManaAbility()) {
            newCopy.addSpellAbility(sa);
            sa.setSourceCard(in);
        }

        newCopy.setControllerObjects(in.getControllerObjects());
        newCopy.setCounters(in.getCounters());
        newCopy.setExtrinsicKeyword(in.getExtrinsicKeyword());
        newCopy.setColor(in.getColor());
        newCopy.setReceivedDamageFromThisTurn(in.getReceivedDamageFromThisTurn());
        newCopy.getDamageHistory().setCreatureGotBlockedThisTurn(in.getDamageHistory().getCreatureGotBlockedThisTurn());
        newCopy.setEnchanting(in.getEnchanting());
        newCopy.setEnchantedBy(new ArrayList<Card> (in.getEnchantedBy()));
        newCopy.setEquipping(new ArrayList<Card> (in.getEquipping()));
        newCopy.setEquippedBy(new ArrayList<Card> (in.getEquippedBy()));
        newCopy.setClones(in.getClones());
        newCopy.setHaunting(in.getHaunting());
        for (final Card haunter : in.getHauntedBy()) {
            newCopy.addHauntedBy(haunter);
        }
        for (final Object o : in.getRemembered()) {
            newCopy.addRemembered(o);
        }
        for (final Card o : in.getImprinted()) {
            newCopy.addImprinted(o);
        }

        return newCopy;
    }

    public static List<Card> getRadiance(final Card source, final Card origin, final String[] valid) {
        final List<Card> res = new ArrayList<Card>();

        for (final CardColor col : origin.getColor()) {
            for (final String strCol : col.toStringList()) {
                if (strCol.equalsIgnoreCase("Colorless")) {
                    continue;
                }
                for (final Card c : Singletons.getModel().getGame().getColoredCardsInPlay(strCol)) {
                    if (!res.contains(c) && c.isValid(valid, source.getController(), source) && !c.equals(origin)) {
                        res.add(c);
                    }
                }
            }
        }

        return res;
    }

    public static CardCharacteristics getFaceDownCharacteristic() {
        final ArrayList<String> types = new ArrayList<String>();
        types.add("Creature");

        final CardCharacteristics ret = new CardCharacteristics();
        ret.setBaseAttack(2);
        ret.setBaseDefense(2);

        ret.setName("");
        ret.setType(types);

        ret.setImageFilename(NewConstants.CACHE_MORPH_IMAGE_FILE);

        return ret;
    }

    public static Set<String> getReflectableManaColors(final SpellAbility abMana, final SpellAbility sa,
            Set<String> colors, final List<Card> parents) {
        // Here's the problem with reflectable Mana. If more than one is out,
        // they need to Reflect each other,
        // so we basically need to have a recursive list that send the parents
        // so we don't infinite recurse.
        final Card card = abMana.getSourceCard();
        
        if (abMana.getApi() != ApiType.ManaReflected) {
            return colors;
        }

        if (!parents.contains(card)) {
            parents.add(card);
        }

        final String colorOrType = sa.getParam("ColorOrType"); // currently Color
                                                              // or
        // Type, Type is colors
        // + colorless
        final String validCard = sa.getParam("Valid");
        final String reflectProperty = sa.getParam("ReflectProperty"); // Produce
        // (Reflecting Pool) or Is (Meteor Crater)

        int maxChoices = 5; // Color is the default colorOrType
        if (colorOrType.equals("Type")) {
            maxChoices++;
        }

        List<Card> cards = null;

        // Reuse AF_Defined in a slightly different way
        if (validCard.startsWith("Defined.")) {
            cards = AbilityUtils.getDefinedCards(card, validCard.replace("Defined.", ""), abMana);
        } else {
            cards = CardLists.getValidCards(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), validCard, abMana.getActivatingPlayer(), card);
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
                for (final String col : Constant.Color.ONLY_COLORS) {
                    if (card1.isColor(col)) {
                        colors.add(col);
                        if (colors.size() == maxChoices) {
                            break;
                        }
                    }
                }
            }
        } else if (reflectProperty.equals("Produced")) {
            final String producedColors = (String) abMana.getTriggeringObject("Produced");
            for (final String col : Constant.Color.ONLY_COLORS) {
                final String s = MagicColor.toShortString(col);
                if (producedColors.contains(s)) {
                    colors.add(col);
                }
            }
            if (maxChoices == 6 && producedColors.contains("1")) {
                colors.add(Constant.Color.COLORLESS);
            }
        } else if (reflectProperty.equals("Produce")) {
            final ArrayList<SpellAbility> abilities = new ArrayList<SpellAbility>();
            for (final Card c : cards) {
                abilities.addAll(c.getManaAbility());
            }
            // currently reflected mana will ignore other reflected mana
            // abilities

            final ArrayList<SpellAbility> reflectAbilities = new ArrayList<SpellAbility>();

            for (final SpellAbility ab : abilities) {
                if (maxChoices == colors.size()) {
                    break;
                }

                if (ab.getApi() == ApiType.ManaReflected) {
                    if (!parents.contains(ab.getSourceCard())) {
                        // Recursion!
                        reflectAbilities.add(ab);
                        parents.add(ab.getSourceCard());
                    }
                    continue;
                }
                colors = canProduce(maxChoices, ab.getManaPart(), colors);
                if (!parents.contains(ab.getSourceCard())) {
                    parents.add(ab.getSourceCard());
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
        for (final String col : Constant.Color.ONLY_COLORS) {
            final String s = MagicColor.toShortString(col);
            if (ab.canProduce(s)) {
                colors.add(col);
            }
        }

        if (maxChoices == 6 && ab.canProduce("1")) {
            colors.add(Constant.Color.COLORLESS);
        }

        return colors;
    }
}
