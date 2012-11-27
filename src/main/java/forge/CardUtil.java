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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;



import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.card.CardCharacteristics;
import forge.card.CardManaCost;
import forge.card.EditionInfo;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.mana.ManaCost;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputPayManaCostUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiDisplayUtil;
import forge.item.CardPrinted;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.MyRandom;


/**
 * <p>
 * CardUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class CardUtil {
    /** Constant <code>RANDOM</code>. */
    public static final Random RANDOM = MyRandom.getRandom();

    /**
     * Do not instantiate.
     */
    private CardUtil() {
        // This space intentionally left blank.
    }

    /**
     * <p>
     * getRandom.
     * </p>
     * 
     * @param o
     *            an array of {@link forge.Card} objects.
     * @return a {@link forge.Card} object.
     */
    public static <T> T getRandom(final List<T> o) {
        if (o == null) {
            throw new IllegalArgumentException("CardUtil : getRandom(T) recieved null instead of array.");
        }
        int len = o.size();
        switch(len) {
            case 0: throw new IllegalArgumentException("CardUtil : getRandom(T) recieved an empty array.");
            case 1: return o.get(0);
            default: return o.get(CardUtil.RANDOM.nextInt(len));
        }
    }

    // returns "G", longColor is Constant.Color.Green and the like
    /**
     * <p>
     * getShortColor.
     * </p>
     * 
     * @param longColor
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getShortColor(final String longColor) {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(Constant.Color.BLACK.toString(), "B");
        map.put(Constant.Color.BLUE.toString(), "U");
        map.put(Constant.Color.GREEN.toString(), "G");
        map.put(Constant.Color.RED.toString(), "R");
        map.put(Constant.Color.WHITE.toString(), "W");

        final Object o = map.get(longColor);
        if (o == null) {
            throw new RuntimeException("CardUtil : getShortColor() invalid argument - " + longColor);
        }

        return (String) o;
    }

    /**
     * <p>
     * getColors.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static List<String> getColors(final Card c) {
        return c.determineColor().toStringList();
    }

    /**
     * <p>
     * getOnlyColors.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<String> getOnlyColors(final Card c) {
        final CardManaCost m = c.getManaCost();
        final byte colorProfile = m.getColorProfile();

        final Set<String> colors = new HashSet<String>();
        if ((colorProfile & forge.card.CardColor.WHITE) > 0) {
            colors.add(Constant.Color.WHITE);
        }
        if ((colorProfile & forge.card.CardColor.BLACK) > 0) {
            colors.add(Constant.Color.BLACK);
        }
        if ((colorProfile & forge.card.CardColor.BLUE) > 0) {
            colors.add(Constant.Color.BLUE);
        }
        if ((colorProfile & forge.card.CardColor.RED) > 0) {
            colors.add(Constant.Color.RED);
        }
        if ((colorProfile & forge.card.CardColor.GREEN) > 0) {
            colors.add(Constant.Color.GREEN);
        }

        for (final String kw : c.getKeyword()) {
            if (kw.startsWith(c.getName() + " is ") || kw.startsWith("CARDNAME is ")) {
                for (final String color : Constant.Color.COLORS) {
                    if (kw.endsWith(color + ".")) {
                        colors.add(color);
                    }
                }
            }
        }
        return new ArrayList<String>(colors);
    }

    // probably should put this somewhere else, but not sure where
    /**
     * <p>
     * getConvertedManaCost.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a int.
     */
    public static int getConvertedManaCost(final Card c) {
        if (c.isToken() && !c.isCopiedToken()) {
            return 0;
        }

        int xPaid = 0;

        // 2012-07-22 - If a card is on the stack, count the xManaCost in with it's CMC
        if (Singletons.getModel().getGame().getCardsIn(ZoneType.Stack).contains(c) && c.getManaCost() != null) {
            xPaid = c.getXManaCostPaid() * c.getManaCost().countX();
        }
        return c.getManaCost().getCMC() + xPaid;
    }

    /**
     * <p>
     * getConvertedManaCost.
     * </p>
     * 
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public static int getConvertedManaCost(final String manaCost) {
        if (manaCost.equals("")) {
            return 0;
        }

        final ManaCost cost = new ManaCost(manaCost);
        return cost.getConvertedManaCost();
    }

    /**
     * <p>
     * isACardType.
     * </p>
     * 
     * @param cardType
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isACardType(final String cardType) {
        return CardUtil.getAllCardTypes().contains(cardType);
    }

    /**
     * <p>
     * getAllCardTypes.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<String> getAllCardTypes() {
        final ArrayList<String> types = new ArrayList<String>();

        // types.addAll(getCardTypes());
        types.addAll(Constant.CardTypes.CARD_TYPES);

        // not currently used by Forge
        types.add("Plane");
        types.add("Scheme");
        types.add("Vanguard");

        return types;
    }


    /**
     * <p>
     * getBasicTypes.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     * @since 1.1.3
     */
    public static ArrayList<String> getBasicTypes() {
        final ArrayList<String> types = new ArrayList<String>();

        types.addAll(Constant.CardTypes.BASIC_TYPES);

        return types;
    }

    /**
     * Gets the land types.
     * 
     * @return the land types
     */
    public static ArrayList<String> getLandTypes() {
        final ArrayList<String> types = new ArrayList<String>();

        types.addAll(Constant.CardTypes.BASIC_TYPES);
        types.addAll(Constant.CardTypes.LAND_TYPES);

        return types;
    }

    /**
     * <p>
     * getCreatureTypes.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     * @since 1.1.6
     */
    public static ArrayList<String> getCreatureTypes() {
        final ArrayList<String> types = new ArrayList<String>();

        types.addAll(Constant.CardTypes.CREATURE_TYPES);

        return types;
    }

    /**
     * <p>
     * isASuperType.
     * </p>
     * 
     * @param cardType
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */

    public static boolean isASuperType(final String cardType) {
        return (Constant.CardTypes.SUPER_TYPES.contains(cardType));
    }

    /**
     * <p>
     * isASubType.
     * </p>
     * 
     * @param cardType
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isASubType(final String cardType) {
        return (!CardUtil.isASuperType(cardType) && !CardUtil.isACardType(cardType));
    }

    /**
     * <p>
     * isACreatureType.
     * </p>
     * 
     * @param cardType
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isACreatureType(final String cardType) {
        return (Constant.CardTypes.CREATURE_TYPES.contains(cardType));
    }

    /**
     * <p>
     * isALandType.
     * </p>
     * 
     * @param cardType
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isALandType(final String cardType) {
        return (Constant.CardTypes.LAND_TYPES.contains(cardType));
    }

    /**
     * Checks if is a planeswalker type.
     * 
     * @param cardType
     *            the card type
     * @return true, if is a planeswalker type
     */
    public static boolean isAPlaneswalkerType(final String cardType) {
        return (Constant.CardTypes.WALKER_TYPES.contains(cardType));
    }

    /**
     * <p>
     * isABasicLandType.
     * </p>
     * 
     * @param cardType
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isABasicLandType(final String cardType) {
        return (Constant.CardTypes.BASIC_TYPES.contains(cardType));
    }

    // this function checks, if duplicates of a keyword are not necessary (like
    // flying, trample, etc.)
    /**
     * <p>
     * isNonStackingKeyword.
     * </p>
     * 
     * @param keyword
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isNonStackingKeyword(final String keyword) {
        String kw = new String(keyword);
        if (kw.startsWith("HIDDEN")) {
            kw = kw.substring(7);
        }
        if (kw.startsWith("Protection")) {
            return true;
        }
        return Constant.Keywords.NON_STACKING_LIST.contains(kw);
    }

    /**
     * <p>
     * isStackingKeyword.
     * </p>
     * 
     * @param keyword
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isStackingKeyword(final String keyword) {
        return !CardUtil.isNonStackingKeyword(keyword);
    }

    /**
     * Builds the ideal filename.
     * 
     * @param cardName
     *            the card name
     * @param artIndex
     *            the art index
     * @param artIndexMax
     *            the art index max
     * @return the string
     */
    public static String buildIdealFilename(final String cardName, final int artIndex, final int artIndexMax) {
        final String nn = artIndexMax > 1 ? Integer.toString(artIndex + 1) : "";
        final String mwsCardName = GuiDisplayUtil.cleanStringMWS(cardName);
        // 3 letter set code with MWS filename format
        return String.format("%s%s.full.jpg", mwsCardName, nn);
    }

    /**
     * <p>
     * buildFilename.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a {@link java.lang.String} object.
     */
    public static String buildFilename(final Card card) {
        final boolean token = card.isToken() && !card.isCopiedToken();

        final String set = card.getCurSetCode();
        final Predicate<EditionInfo> findSetInfo = new Predicate<EditionInfo>() {
            @Override
            public boolean apply(final EditionInfo subject) {
                return subject.getCode().equals(set);
            }
        };
        EditionInfo neededSet = null;
        if (!card.getSets().isEmpty()) {
            neededSet = Iterables.find(card.getSets(), findSetInfo);
        }
        final int cntPictures = neededSet == null ? 1 : neededSet.getPicCount();
        return CardUtil
                .buildFilename(card.getName(), card.getCurSetCode(), card.getRandomPicture(), cntPictures, token);
    }

    /**
     * buildFilename for lightweight card. Searches for a matching file on disk,
     * 
     * @param card
     *            the card
     * @return the string
     */
    public static String buildFilename(final CardPrinted card) {
        final int maxIndex = card.getCard().getEditionInfo(card.getEdition()).getCopiesCount();
        return CardUtil.buildFilename(card.getName(), card.getEdition(), card.getArtIndex(), maxIndex, false);
    }

    /**
     * Builds the filename.
     * 
     * @param card
     *            the card
     * @param nameToUse
     *            the name to use
     * @return the string
     */
    public static String buildFilename(final CardPrinted card, final String nameToUse) {
        final int maxIndex = card.getCard().getEditionInfo(card.getEdition()).getCopiesCount();
        return CardUtil.buildFilename(nameToUse, card.getEdition(), card.getArtIndex(), maxIndex, false);
    }

    private static String buildFilename(final String cardName, final String setName, final int artIndex,
            final int artIndexMax, final boolean isToken) {
        final File path = ForgeProps.getFile(isToken ? NewConstants.IMAGE_TOKEN : NewConstants.IMAGE_BASE);
        final String nn = artIndexMax > 1 ? Integer.toString(artIndex + 1) : "";
        final String cleanCardName = GuiDisplayUtil.cleanString(cardName);

        File f = null;
        if (StringUtils.isNotBlank(setName)) {
            final String mwsCardName = GuiDisplayUtil.cleanStringMWS(cardName);

            // First, try 3 letter set code with MWS filename format
            final String mwsSet3 = String.format("%s/%s%s.full", setName, mwsCardName, nn);
            f = new File(path, mwsSet3 + ".jpg");
            if (f.exists()) {
                return mwsSet3;
            }

            // Second, try 2 letter set code with MWS filename format
            final String mwsSet2 = String.format("%s/%s%s.full", Singletons.getModel().getEditions().getCode2ByCode(setName), mwsCardName, nn);
            f = new File(path, mwsSet2 + ".jpg");
            if (f.exists()) {
                return mwsSet2;
            }

            // Third, try 3 letter set code with Forge filename format
            final String forgeSet3 = String.format("%s/%s%s", setName, cleanCardName, nn);
            f = new File(path, forgeSet3 + ".jpg");
            if (f.exists()) {
                return forgeSet3;
            }
        }

        // Last, give up with set images, go with the old picture type
        final String forgePlain = String.format("%s%s", cleanCardName, nn);

        f = new File(path, forgePlain + ".jpg");
        if (f.exists()) {
            return forgePlain;
        }

        // give up with art index
        f = new File(path, cleanCardName + ".jpg");
        if (f.exists()) {
            return cleanCardName;
        }

        // if still no file, download if option enabled?
        return "none";
    }

    /**
     * <p>
     * getShortColorsString.
     * </p>
     * 
     * @param colors
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getShortColorsString(final ArrayList<String> colors) {
        String colorDesc = "";
        for (final String col : colors) {
            if (col.equalsIgnoreCase("White")) {
                colorDesc += "W";
            } else if (col.equalsIgnoreCase("Blue")) {
                colorDesc += "U";
            } else if (col.equalsIgnoreCase("Black")) {
                colorDesc += "B";
            } else if (col.equalsIgnoreCase("Red")) {
                colorDesc += "R";
            } else if (col.equalsIgnoreCase("Green")) {
                colorDesc += "G";
            } else if (col.equalsIgnoreCase("Colorless")) {
                colorDesc = "C";
            }
        }
        return colorDesc;
    }

    /**
     * Compute the canonicalized ASCII form of a card name.
     * 
     * @param cardName
     *            the name to transform (but not side effect)
     * 
     * @return the name in ASCII characters
     */
    public static String canonicalizeCardName(final String cardName) {
        String result = cardName;
        result = result.replace("\u00ae", "(R)"); // Ultimate Nightmare ...
        result = result.replace("\u00c6", "AE");
        result = result.replace("\u00e0", "a");
        result = result.replace("\u00e1", "a");
        result = result.replace("\u00e2", "a");
        result = result.replace("\u00e9", "e");
        result = result.replace("\u00ed", "i");
        result = result.replace("\u00f6", "o");
        result = result.replace("\u00fa", "u");
        result = result.replace("\u00fb", "u");
        result = result.replace("\u2012", "-");
        result = result.replace("\u2013", "-");
        result = result.replace("\u2014", "-");
        result = result.replace("\u2015", "-");
        result = result.replace("\u2018", "'");
        result = result.replace("\u2019", "'");
        result = result.replace("\u221e", "Infinity"); // Mox Lo...

        return result;
    }

    /**
     * getThisTurnEntered.
     * 
     * @param to
     *            zone going to
     * @param from
     *            zone coming from
     * @param valid
     *            a isValid expression
     * @param src
     *            a Card object
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

    /**
     * getThisTurnCast.
     * 
     * @param valid
     *            a String object
     * @param src
     *            a Card object
     * @return a List<Card> that matches the given criteria
     */
    public static List<Card> getThisTurnCast(final String valid, final Card src) {
        List<Card> res = new ArrayList<Card>();

        res.addAll(Singletons.getModel().getGame().getStack().getCardsCastThisTurn());

        res = CardLists.getValidCards(res, valid, src.getController(), src);

        return res;
    }

    /**
     * getLastTurnCast.
     * 
     * @param valid
     *            a String object
     * @param src
     *            a Card object
     * @return a List<Card> that matches the given criteria
     */
    public static List<Card> getLastTurnCast(final String valid, final Card src) {
        List<Card> res = new ArrayList<Card>();

        res.addAll(Singletons.getModel().getGame().getStack().getCardsCastLastTurn());

        res = CardLists.getValidCards(res, valid, src.getController(), src);

        return res;
    }

    /**
     * getLKICopy.
     * 
     * @param in
     *            a Card to copy.
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
        }

        newCopy.setControllerObjects(in.getControllerObjects());
        newCopy.setCounters(in.getCounters());
        newCopy.setExtrinsicKeyword(in.getExtrinsicKeyword());
        newCopy.setColor(in.getColor());
        newCopy.setReceivedDamageFromThisTurn(in.getReceivedDamageFromThisTurn());
        newCopy.getDamageHistory().setCreatureGotBlockedThisTurn(in.getDamageHistory().getCreatureGotBlockedThisTurn());
        newCopy.setEnchanting(in.getEnchanting());
        newCopy.setEnchantedBy(in.getEnchantedBy());
        newCopy.setEquipping(in.getEquipping());
        newCopy.setEquippedBy(in.getEquippedBy());
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

    /**
     * Gets the radiance.
     * 
     * @param source
     *            the source
     * @param origin
     *            the origin
     * @param valid
     *            the valid
     * @return the radiance
     */
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

    /**
     * Gets the convokable colors.
     * 
     * @param cardToConvoke
     *            the card to convoke
     * @param cost
     *            the cost
     * @return the convokable colors
     */
    public static ArrayList<String> getConvokableColors(final Card cardToConvoke, final ManaCost cost) {
        final ArrayList<String> usableColors = new ArrayList<String>();

        if (cost.getColorlessManaAmount() > 0) {
            usableColors.add("colorless");
        }
        for (final CardColor col : cardToConvoke.getColor()) {
            for (final String strCol : col.toStringList()) {
                if (strCol.equals("colorless")) {
                    continue;
                }
                if (cost.toString().contains(InputPayManaCostUtil.getShortColorString(strCol))) {
                    usableColors.add(strCol.toString());
                }
            }
        }

        return usableColors;
    }

    /**
     * Gets the face down characteristic.
     * 
     * @return the face down characteristic
     */
    public static CardCharacteristics getFaceDownCharacteristic() {
        final ArrayList<String> types = new ArrayList<String>();
        types.add("Creature");

        final CardCharacteristics ret = new CardCharacteristics();
        ret.setBaseAttack(2);
        ret.setBaseDefense(2);

        ret.setName("");
        ret.setType(types);

        ret.setImageName(NewConstants.MORPH_IMAGE_FILE_NAME);

        return ret;
    }

    private static List<String> getColorsOfCards(final int maxChoices, final List<Card> cards, final List<String> colors) {
        for (final Card card : cards) {
            // For each card, go through all the colors and if the card is that
            // color, add
            for (final String col : Constant.Color.ONLY_COLORS) {
                if (card.isColor(col) && !colors.contains(col)) {
                    colors.add(col);
                    if (colors.size() == maxChoices) {
                        break;
                    }
                }
            }
        }
        return colors;
    }

    // add Colors and
    /**
     * <p>
     * reflectableMana.
     * </p>
     * 
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param colors
     *            a {@link java.util.ArrayList} object.
     * @param parents
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static List<String> getReflectableManaColors(final SpellAbility abMana, final SpellAbility sa,
            List<String> colors, final List<Card> parents) {
        // Here's the problem with reflectable Mana. If more than one is out,
        // they need to Reflect each other,
        // so we basically need to have a recursive list that send the parents
        // so we don't infinite recurse.
        final Card card = abMana.getSourceCard();

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
            cards = AbilityFactory.getDefinedCards(card, validCard.replace("Defined.", ""), abMana);
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
            colors = getColorsOfCards(maxChoices, cards, colors);
        } else if (reflectProperty.equals("Produced")) {
            final String producedColors = (String) abMana.getTriggeringObject("Produced");
            for (final String col : Constant.Color.ONLY_COLORS) {
                final String s = InputPayManaCostUtil.getShortColorString(col);
                if (producedColors.contains(s) && !colors.contains(col)) {
                    colors.add(col);
                }
            }
            if ((maxChoices == 6) && producedColors.contains("1") && !colors.contains(Constant.Color.COLORLESS)) {
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

                if (ab.getManaPart().isReflectedMana()) {
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

    public static List<String> canProduce(final int maxChoices, final AbilityManaPart ab,
            final List<String> colors) {
        for (final String col : Constant.Color.ONLY_COLORS) {
            final String s = InputPayManaCostUtil.getShortColorString(col);
            if (ab.canProduce(s) && !colors.contains(col)) {
                colors.add(col);
            }
        }

        if ((maxChoices == 6) && ab.canProduce("1") && !colors.contains(Constant.Color.COLORLESS)) {
            colors.add(Constant.Color.COLORLESS);
        }

        return colors;
    }


} // end class CardUtil
