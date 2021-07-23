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
package forge.card;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;

import forge.card.mana.IParserManaCost;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.util.TextUtil;

import static forge.card.MagicColor.Constant.*;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

/**
 * A collection of methods containing full
 * meta and gameplay properties of a card.
 *
 * @author Forge
 * @version $Id: CardRules.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardRules implements ICardCharacteristics {
    private String normalizedName;
    private CardSplitType splitType;
    private ICardFace mainPart;
    private ICardFace otherPart;
    private CardAiHints aiHints;
    private ColorSet colorIdentity;
    private ColorSet deckbuildingColors;
    private String meldWith;
    private String partnerWith;

    private CardRules(ICardFace[] faces, CardSplitType altMode, CardAiHints cah) {
        splitType = altMode;
        mainPart = faces[0];
        otherPart = faces[1];
        aiHints = cah;
        meldWith = "";
        partnerWith = "";

        //calculate color identity
        byte colMask = calculateColorIdentity(mainPart);

        if (otherPart != null) {
            colMask |= calculateColorIdentity(otherPart);
        }
        colorIdentity = ColorSet.fromMask(colMask);
    }

    void reinitializeFromRules(CardRules newRules) {
        if(!newRules.getName().equals(this.getName()))
            throw new UnsupportedOperationException("You cannot rename the card using the same CardRules object");

        splitType = newRules.splitType;
        mainPart = newRules.mainPart;
        otherPart = newRules.otherPart;
        aiHints = newRules.aiHints;
        colorIdentity = newRules.colorIdentity;
        meldWith = newRules.meldWith;
        partnerWith = newRules.partnerWith;
    }

    private static byte calculateColorIdentity(final ICardFace face) {
        byte res = face.getColor().getColor();
        boolean isReminder = false;
        boolean isSymbol = false;
        String oracleText = face.getOracleText();
        // CR 903.4 colors defined by its characteristic-defining abilities
        for (String staticAbility : face.getStaticAbilities()) {
            if (staticAbility.contains("CharacteristicDefining$ True") && staticAbility.contains("SetColor$ All")) {
                res |= MagicColor.ALL_COLORS;
            }
        }
        int len = oracleText.length();
        for(int i = 0; i < len; i++) {
            char c = oracleText.charAt(i); // This is to avoid needless allocations performed by toCharArray()
            switch(c) {
                case('('): isReminder = i > 0; break; // if oracle has only reminder, consider it valid rules (basic and true lands need this)
                case(')'): isReminder = false; break;
                case('{'): isSymbol = true; break;
                case('}'): isSymbol = false; break;
                default:
                    if(isSymbol && !isReminder) {
                        switch(c) {
                            case('W'): res |= MagicColor.WHITE; break;
                            case('U'): res |= MagicColor.BLUE; break;
                            case('B'): res |= MagicColor.BLACK; break;
                            case('R'): res |= MagicColor.RED; break;
                            case('G'): res |= MagicColor.GREEN; break;
                        }
                    }
                    break;
            }
        }
        return res;
    }

    public boolean isVariant() {
        CardType t = getType();
        return t.isVanguard() || t.isScheme() || t.isPlane() || t.isPhenomenon() || t.isConspiracy() || t.isDungeon();
    }

    public CardSplitType getSplitType() {
        return splitType;
    }

    public ICardFace getMainPart() {
        return mainPart;
    }

    public ICardFace getOtherPart() {
        return otherPart;
    }

    public String getName() {
        switch(splitType.getAggregationMethod()) {
            case COMBINE:
                return mainPart.getName() + " // " + otherPart.getName();
            default:
                return mainPart.getName();
        }
    }

    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String filename) { normalizedName = filename; }

    public CardAiHints getAiHints() {
        return aiHints;
    }

    @Override
    public CardType getType() {
        switch(splitType.getAggregationMethod()) {
            case COMBINE: // no cards currently have different types
                return CardType.combine(mainPart.getType(), otherPart.getType());
            default:
                return mainPart.getType();
        }
    }

    @Override
    public ManaCost getManaCost() {
        switch(splitType.getAggregationMethod()) {
        case COMBINE:
            return ManaCost.combine(mainPart.getManaCost(), otherPart.getManaCost());
        default:
            return mainPart.getManaCost();
        }
    }

    @Override
    public ColorSet getColor() {
        switch(splitType.getAggregationMethod()) {
        case COMBINE:
            return ColorSet.fromMask(mainPart.getColor().getColor() | otherPart.getColor().getColor());
        default:
            return mainPart.getColor();
        }
    }

    private static boolean canCastFace(final ICardFace face, final byte colorCode) {
        if (face.getManaCost().isNoCost()) {
            //if card face has no cost, assume castable only by mana of its defined color
            return face.getColor().hasNoColorsExcept(colorCode);
        }
        return face.getManaCost().canBePaidWithAvaliable(colorCode);
    }

    public boolean canCastWithAvailable(byte colorCode) {
        switch(splitType.getAggregationMethod()) {
        case COMBINE:
            return canCastFace(mainPart, colorCode) || canCastFace(otherPart, colorCode);
        default:
            return canCastFace(mainPart, colorCode);
        }
    }

    @Override public int getIntPower() { return mainPart.getIntPower(); }
    @Override public int getIntToughness() { return mainPart.getIntToughness(); }
    @Override public String getPower() { return mainPart.getPower(); }
    @Override public String getToughness() { return mainPart.getToughness(); }
    @Override public String getInitialLoyalty() { return mainPart.getInitialLoyalty(); }

    @Override
    public String getOracleText() {
        switch(splitType.getAggregationMethod()) {
        case COMBINE:
            return mainPart.getOracleText() + "\r\n\r\n" + otherPart.getOracleText();
        default:
            return mainPart.getOracleText();
        }
    }

    public boolean canBeCommander() {
        if (mainPart.getOracleText().contains("can be your commander")) {
            return true;
        }
        CardType type = mainPart.getType();
        boolean creature = type.isCreature();
        for (String staticAbility : mainPart.getStaticAbilities()) { // Check for Grist
            if (staticAbility.contains("CharacteristicDefining$ True") && staticAbility.contains("AddType$ Creature")) {
                creature = true;
            }
        }
        if (type.isLegendary() && creature) {
            return true;
        }
        return false;
    }

    public boolean canBePartnerCommander() {
        return canBeCommander() && (hasKeyword("Partner") || !this.partnerWith.isEmpty());
    }

    public boolean canBeOathbreaker() {
        CardType type = mainPart.getType();
        return type.isPlaneswalker();
    }

    public boolean canBeSignatureSpell() {
        CardType type = mainPart.getType();
        return type.isInstant() || type.isSorcery();
    }

    public boolean canBeBrawlCommander() {
        CardType type = mainPart.getType();
        if (!type.isLegendary()) {
            return false;
        }
        if (type.isCreature() || type.isPlaneswalker()) {
            return true;
        }

        // Grist is checked above, but new cards might work this way
        for (String staticAbility : mainPart.getStaticAbilities()) {
            if (staticAbility.contains("CharacteristicDefining$ True") && staticAbility.contains("AddType$ Creature")) {
                return true;
            }
        }
        return false;
    }

    public boolean canBeTinyLeadersCommander() {
        CardType type = mainPart.getType();
        if (!type.isLegendary()) {
            return false;
        }
        if (type.isCreature() || type.isPlaneswalker()) {
            return true;
        }

        // Grist is checked above, but new cards might work this way
        for (String staticAbility : mainPart.getStaticAbilities()) {
            if (staticAbility.contains("CharacteristicDefining$ True") && staticAbility.contains("AddType$ Creature")) {
                return true;
            }
        }
        return false;
    }

    public String getMeldWith() {
        return meldWith;
    }

    public String getPartnerWith() {
        return partnerWith;
    }

    // vanguard card fields, they don't use sides.
    private int deltaHand;
    private int deltaLife;

    public int getHand() { return deltaHand; }
    public int getLife() { return deltaLife; }
    public void setVanguardProperties(String pt) {
        final int slashPos = pt == null ? -1 : pt.indexOf('/');
        if (slashPos == -1) {
            throw new RuntimeException("Vanguard '" + this.getName() + "' has bad hand/life stats");
        }
        this.deltaHand = Integer.parseInt(TextUtil.fastReplace(pt.substring(0, slashPos), "+", ""));
        this.deltaLife = Integer.parseInt(TextUtil.fastReplace(pt.substring(slashPos+1), "+", ""));
    }

    // Downloadable image
    private String dlUrl;
    private String dlUrlOtherSide;

    @Deprecated
    public String getPictureUrl(boolean backface ) { return backface ? dlUrlOtherSide : dlUrl; }

    @Deprecated
    public void setDlUrls(String[] dlUrls) { this.dlUrl = dlUrls[0]; this.dlUrlOtherSide = dlUrls[1]; }

    public ColorSet getColorIdentity() {
        return colorIdentity;
    }

    /** Instantiates class, reads a card. For batch operations better create you own reader instance. */
    public static CardRules fromScript(Iterable<String> script) {
        Reader crr = new Reader();
        for(String line : script) {
            crr.parseLine(line);
        }
        return crr.getCard();
    }

    // Reads cardname.txt
    public static class Reader {
        // fields to build
        private CardFace[] faces = new CardFace[] { null, null };
        private String[] pictureUrl = new String[] { null, null };
        private int curFace = 0;
        private CardSplitType altMode = CardSplitType.None;
        private String meldWith = "";
        private String partnerWith = "";
        private String handLife = null;
        private String normalizedName = "";

        // fields to build CardAiHints
        private boolean removedFromAIDecks = false;
        private boolean removedFromRandomDecks = false;
        private boolean removedFromNonCommanderDecks = false;
        private DeckHints hints = null;
        private DeckHints needs = null;
        private DeckHints has = null;

        /**
         * Reset all fields to parse next card (to avoid allocating new CardRulesReader N times)
         */
        public final void reset() {
            this.curFace = 0;
            this.faces[0] = null;
            this.faces[1] = null;
            this.pictureUrl[0] = null;
            this.pictureUrl[1] = null;

            this.handLife = null;
            this.altMode = CardSplitType.None;

            this.removedFromAIDecks = false;
            this.removedFromRandomDecks = false;
            this.removedFromNonCommanderDecks = false;
            this.needs = null;
            this.hints = null;
            this.has = null;
            this.meldWith = "";
            this.partnerWith = "";
            this.normalizedName = "";
        }

        /**
         * Gets the card.
         *
         * @return the card
         */
        public final CardRules getCard() {
            CardAiHints cah = new CardAiHints(removedFromAIDecks, removedFromRandomDecks, removedFromNonCommanderDecks, hints, needs, has);
            faces[0].assignMissingFields();
            if (null != faces[1]) faces[1].assignMissingFields();
            final CardRules result = new CardRules(faces, altMode, cah);

            result.setNormalizedName(this.normalizedName);
            result.meldWith = this.meldWith;
            result.partnerWith = this.partnerWith;
            result.setDlUrls(pictureUrl);
            if (StringUtils.isNotBlank(handLife))
                result.setVanguardProperties(handLife);
            return result;
        }

        public final CardRules readCard(final Iterable<String> script, String filename) {
            this.reset();
            for (String line : script) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                this.parseLine(line);
            }
            this.normalizedName = filename;
            return this.getCard();
        }

        public final CardRules readCard(final Iterable<String> script) {
            return readCard(script, null);
        }

        /**
         * Parses the line.
         *
         * @param line
         *            the line
         */
        public final void parseLine(final String line) {
            int colonPos = line.indexOf(':');
            String key = colonPos > 0 ? line.substring(0, colonPos) : line;
            String value = colonPos > 0 ? line.substring(1+colonPos).trim() : null;

            switch(key.charAt(0)) {
                case 'A':
                    if ("A".equals(key)) {
                        this.faces[curFace].addAbility(value);
                    } else if ("AI".equals(key)) {
                        colonPos = value.indexOf(':');
                        String variable = colonPos > 0 ? value.substring(0, colonPos) : value;
                        value = colonPos > 0 ? value.substring(1+colonPos) : null;

                        if ( "RemoveDeck".equals(variable) ) {
                            this.removedFromAIDecks |= "All".equalsIgnoreCase(value);
                            this.removedFromRandomDecks |= "Random".equalsIgnoreCase(value);
                            this.removedFromNonCommanderDecks |= "NonCommander".equalsIgnoreCase(value);
                        }
                    } else if ("AlternateMode".equals(key)) {
                        this.altMode = CardSplitType.smartValueOf(value);
                    } else if ("ALTERNATE".equals(key)) {
                        this.curFace = 1;
                    } else if ("AltName".equals(key)) {
                        this.faces[curFace].setAltName(value);
                    }
                break;

                case 'C':
                    if ("Colors".equals(key)) {
                        // This is forge.card.CardColor not forge.CardColor.
                        // Why do we have two classes with the same name?
                        ColorSet newCol = ColorSet.fromNames(value.split(","));
                        this.faces[this.curFace].setColor(newCol);
                    }
                    break;

                case 'D':
                    if ("DeckHints".equals(key)) {
                        hints = new DeckHints(value);
                    } else if ("DeckNeeds".equals(key)) {
                        needs = new DeckHints(value);
                    } else if ("DeckHas".equals(key)) {
                        has = new DeckHints(value);
                    }
                    break;

                case 'H':
                    if ("HandLifeModifier".equals(key)) {
                        handLife = value;
                    }
                    break;

                case 'K':
                    if ("K".equals(key)) {
                        this.faces[this.curFace].addKeyword(value);
                        if (value.startsWith("Partner:")) {
                            this.partnerWith = value.split(":")[1];
                        }
                    }
                    break;

                case 'L':
                    if ("Loyalty".equals(key)) {
                        this.faces[this.curFace].setInitialLoyalty(value);
                    }
                    break;

                case 'M':
                    if ("ManaCost".equals(key)) {
                        this.faces[this.curFace].setManaCost("no cost".equals(value) ? ManaCost.NO_COST
                                : new ManaCost(new ManaCostParser(value)));
                    } else if ("MeldPair".equals(key)) {
                        this.meldWith = value;
                    }
                    break;

                case 'N':
                    if ("Name".equals(key)) {
                        this.faces[this.curFace] = new CardFace(value);
                    }
                    break;

                case 'O':
                    if ("Oracle".equals(key)) {
                        this.faces[this.curFace].setOracleText(value);
                    }
                    break;

                case 'P':
                    if ("PT".equals(key)) {
                        this.faces[this.curFace].setPtText(value);
                    }
                    break;

                case 'R':
                    if ("R".equals(key)) {
                        this.faces[this.curFace].addReplacementEffect(value);
                    }
                    break;

                case 'S':
                    if ("S".equals(key)) {
                        this.faces[this.curFace].addStaticAbility(value);
                    } else if ("SVar".equals(key)) {
                        if (null == value) throw new IllegalArgumentException("SVar has no variable name");

                        colonPos = value.indexOf(':');
                        String variable = colonPos > 0 ? value.substring(0, colonPos) : value;
                        value = colonPos > 0 ? value.substring(1+colonPos) : null;

                        if ("Picture".equals(variable)) {
                            this.pictureUrl[this.curFace] = value;
                        } else
                            this.faces[curFace].addSVar(variable, value);
                    } else if ("SetInfo".equals(key)) {
                        // deprecated
                    }
                    break;

                case 'T':
                    if ("T".equals(key)) {
                        this.faces[this.curFace].addTrigger(value);
                    } else if ("Types".equals(key)) {
                        this.faces[this.curFace].setType(CardType.parse(value, false));
                    } else if ("Text".equals(key) && !"no text".equals(value) && StringUtils.isNotBlank(value)) {
                        this.faces[this.curFace].setNonAbilityText(value);
                    }
                    break;
            }
        }

        /**
         * The Class ParserCardnameTxtManaCost.
         */
        private static class ManaCostParser implements IParserManaCost {
            private final StringTokenizer st;
            private int genericCost;

            public ManaCostParser(final String cost) {
                st = new StringTokenizer(cost, " ");
                this.genericCost = 0;
            }

            @Override
            public final int getTotalGenericCost() {
                if (this.hasNext()) {
                    throw new RuntimeException("Generic cost should be obtained after iteration is complete");
                }
                return this.genericCost;
            }

            /*
             * (non-Javadoc)
             *
             * @see java.util.Iterator#hasNext()
             */
            @Override
            public final boolean hasNext() {
                return st.hasMoreTokens();
            }

            /*
             * (non-Javadoc)
             *
             * @see java.util.Iterator#next()
             */
            @Override
            public final ManaCostShard next() {
                final String unparsed = st.nextToken();
                if (StringUtils.isNumeric(unparsed)) {
                    this.genericCost += Integer.parseInt(unparsed);
                    return null;
                }

                return ManaCostShard.parseNonGeneric(unparsed);
            }

            /*
             * (non-Javadoc)
             *
             * @see java.util.Iterator#remove()
             */
            @Override
            public void remove() {
            } // unsupported
        }
    }

    public static CardRules getUnsupportedCardNamed(String name) {
        CardAiHints cah = new CardAiHints(true, true, true, null, null, null);
        CardFace[] faces = { new CardFace(name), null};
        faces[0].setColor(ColorSet.fromMask(0));
        faces[0].setType(CardType.parse("", false));
        faces[0].setOracleText("This card is not supported by Forge. Whenever you start a game with this card, it will be bugged.");
        faces[0].setNonAbilityText("This card is not supported by Forge.\nWhenever you start a game with this card, it will be bugged.");
        faces[0].assignMissingFields();
        final CardRules result = new CardRules(faces, CardSplitType.None, cah);

        return result;
    }

    public boolean hasKeyword(final String k) {
        return Iterables.contains(mainPart.getKeywords(), k);
    }

    public Integer getKeywordMagnitude(final String k) {
        for (final String inst : mainPart.getKeywords()) {
            final String[] parts = inst.split(":");
            if (parts[0].equals(k) && StringUtils.isNumeric(parts[1])) {
                return Integer.valueOf(parts[1]);
            }
        }
        return null;
    }

    public ColorSet getDeckbuildingColors() {
        if (deckbuildingColors == null) {
            byte colors = 0;
            if (mainPart.getType().isLand()) {
                colors = getColorIdentity().getColor();
                for (int i = 0; i < 5; i++) {
                    if (containsIgnoreCase(mainPart.getOracleText(), BASIC_LANDS.get(i))) {
                        colors |= 1 << i;
                    }
                }
            } else {
                colors = getColor().getColor();
                if (getOtherPart() != null) {
                    colors |= getOtherPart().getManaCost().getColorProfile();
                }
            }
            deckbuildingColors = ColorSet.fromMask(colors);
        }
        return deckbuildingColors;
    }
}
