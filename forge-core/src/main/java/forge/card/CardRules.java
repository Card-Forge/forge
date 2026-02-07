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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.card.mana.ManaCost;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static forge.card.MagicColor.Constant.BASIC_LANDS;
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
    private Map<CardStateName, ICardFace> specializedParts = Maps.newHashMap();
    private List<ICardFace> allFaces;

    private CardAiHints aiHints;
    private ColorSet colorIdentity;
    private ColorSet deckbuildingColors;
    private String meldWith;
    private String partnerWith;
    private String partnerType;
    private boolean addsWildCardColor;
    private int setColorID;
    private boolean custom;
    private boolean unsupported;
    private String path;

    public CardRules(ICardFace[] faces, CardSplitType altMode, CardAiHints cah) {
        splitType = altMode;
        mainPart = faces[0];
        otherPart = faces[1];

        if (CardSplitType.Specialize.equals(splitType)) {
            specializedParts.put(CardStateName.SpecializeW, faces[2]);
            specializedParts.put(CardStateName.SpecializeU, faces[3]);
            specializedParts.put(CardStateName.SpecializeB, faces[4]);
            specializedParts.put(CardStateName.SpecializeR, faces[5]);
            specializedParts.put(CardStateName.SpecializeG, faces[6]);
        }

        // Android doesn't support toUnmodifiableList
        allFaces = Arrays.stream(faces).filter(Objects::nonNull).collect(Collectors.toList());

        aiHints = cah;
        meldWith = "";
        partnerWith = "";
        partnerType = "";
        addsWildCardColor = false;
        setColorID = 0;


        //calculate color identity
        byte colMask = calculateColorIdentity(mainPart);

        if (otherPart != null) {
            colMask |= calculateColorIdentity(otherPart);
        }
        colorIdentity = ColorSet.fromMask(colMask);
    }

    void reinitializeFromRules(CardRules newRules) {
        if (!newRules.getName().equals(this.getName()))
            throw new UnsupportedOperationException("You cannot rename the card using the same CardRules object");

        splitType = newRules.splitType;
        mainPart = newRules.mainPart;
        otherPart = newRules.otherPart;
        specializedParts = Maps.newHashMap(newRules.specializedParts);
        allFaces = newRules.allFaces;
        aiHints = newRules.aiHints;
        colorIdentity = newRules.colorIdentity;
        meldWith = newRules.meldWith;
        partnerWith = newRules.partnerWith;
        addsWildCardColor = newRules.addsWildCardColor;
        setColorID = newRules.setColorID;
        tokens = newRules.tokens;
    }

    private static byte calculateColorIdentity(final ICardFace face) {
        byte res = face.getColor().getColor();
        boolean isReminder = false;
        boolean isSymbol = false;
        String oracleText = face.getOracleText();
        // CR 903.4 colors defined by its characteristic-defining abilities
        for (String staticAbility : face.getStaticAbilities()) {
            if (staticAbility.contains("CharacteristicDefining$ True") && staticAbility.contains("SetColor$ All")) {
                return MagicColor.ALL_COLORS;
            }
        }
        // no need to check oracle if it is already all colors
        if (res == MagicColor.ALL_COLORS) {
            return res;
        }
        int len = oracleText.length();
        for (int i = 0; i < len; i++) {
            char c = oracleText.charAt(i); // This is to avoid needless allocations performed by toCharArray()
            switch (c) {
                case('('): isReminder = i > 0; break; // if oracle has only reminder, consider it valid rules (basic and true lands need this)
                case(')'): isReminder = false; break;
                case('{'): isSymbol = true; break;
                case('}'): isSymbol = false; break;
                default:
                    if (isSymbol && !isReminder) {
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
        return t.isVanguard() || t.isScheme() || t.isPlane() || t.isPhenomenon()
                || t.isConspiracy() || t.isDungeon() || t.isAttraction() || t.isContraption();
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

    public Map<CardStateName, ICardFace> getSpecializeParts() {
        return specializedParts;
    }

    public List<ICardFace> getAllFaces() {
        return allFaces;
    }

    public boolean isTransformable() {
        return CardSplitType.Transform == getSplitType() || CardSplitType.Modal == getSplitType();
    }

    public ICardFace getWSpecialize() {
        return specializedParts.get(CardStateName.SpecializeW);
    }
    public ICardFace getUSpecialize() {
        return specializedParts.get(CardStateName.SpecializeU);
    }
    public ICardFace getBSpecialize() {
        return specializedParts.get(CardStateName.SpecializeB);
    }
    public ICardFace getRSpecialize() {
        return specializedParts.get(CardStateName.SpecializeR);
    }
    public ICardFace getGSpecialize() {
        return specializedParts.get(CardStateName.SpecializeG);
    }

    public String getName() {
        switch (splitType.getAggregationMethod()) {
            case COMBINE:
                return mainPart.getName() + " // " + otherPart.getName();
            default:
                return mainPart.getName();
        }
    }

    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String filename) { normalizedName = filename; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public CardAiHints getAiHints() {
        return aiHints;
    }

    public boolean isCustom() { return custom; }
    public void setCustom() { custom = true; }

    public boolean isUnsupported() { return unsupported; }

    @Override
    public CardType getType() {
        switch (splitType.getAggregationMethod()) {
            case COMBINE: // no cards currently have different types
                return CardType.combine(mainPart.getType(), otherPart.getType());
            default:
                return mainPart.getType();
        }
    }

    @Override
    public ManaCost getManaCost() {
        switch (splitType.getAggregationMethod()) {
            case COMBINE:
                return ManaCost.combine(mainPart.getManaCost(), otherPart.getManaCost());
            default:
                return mainPart.getManaCost();
        }
    }

    @Override
    public ColorSet getColor() {
        switch (splitType.getAggregationMethod()) {
            case COMBINE:
                return ColorSet.combine(mainPart.getColor(), otherPart.getColor());
            default:
                return mainPart.getColor();
        }
    }

    private static boolean canCastFace(final ICardFace face, final byte colorCode) {
        if (face.getManaCost().isNoCost()) {
            //if card face has no cost, assume castable only by mana of its defined color
            return face.getColor().hasNoColorsExcept(colorCode);
        }
        return face.getManaCost().canBePaidWithAvailable(colorCode);
    }

    public boolean canCastWithAvailable(byte colorCode) {
        switch (splitType.getAggregationMethod()) {
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
    public String getDefense() {
        return mainPart.getDefense();
    }

    @Override public Set<Integer> getAttractionLights() { return mainPart.getAttractionLights(); }

    @Override
    public String getOracleText() {
        switch (splitType.getAggregationMethod()) {
            case COMBINE:
                return mainPart.getOracleText() + "\r\n\r\n" + otherPart.getOracleText();
            default:
                return mainPart.getOracleText();
        }
    }

    public boolean isEnterableDungeon() {
        if (mainPart.getOracleText().contains("You can't enter this dungeon unless")) {
            return false;
        }
        return getType().isDungeon();
    }

    public boolean hasPrintedPT() {
        return getPower() != null || getToughness() != null;
    }

    public boolean canBeCommander() {
        if (mainPart.getOracleText().contains(" is your commander, choose a color before the game begins.")) {
            addsWildCardColor = true;
        }
        if (mainPart.getOracleText().contains("can be your commander") || canBeBackground()) {
            return true;
        }
        CardType type = mainPart.getType();
        if (!type.isLegendary()) {
            return false;
        }
        if (canBeCreature() || type.isVehicle() || (
                type.isSpacecraft() && getPower() != null)) {
            // Spacecraft need printed PT
            return true;
        }
        return false;
    }

    public boolean canBePartnerCommanders(CardRules b) {
        if (!(canBePartnerCommander() && b.canBePartnerCommander())) {
            return false;
        }
        if (hasKeyword("Partner") && b.hasKeyword("Partner")) {
            return true; // normal partner commander
        }
        if (getName().equals(b.getPartnerWith()) && b.getName().equals(getPartnerWith())) {
            return true; // paired partner commander
        }

        if (!this.partnerType.isEmpty() && this.partnerType.equals(b.partnerType)) {
            return true;
        }

        if (hasKeyword("Choose a Background") && b.canBeBackground()
                || b.hasKeyword("Choose a Background") && canBeBackground()) {
            return true; // commander with background
        }
        if (isDoctor() && b.hasKeyword("Doctor's companion")
                || hasKeyword("Doctor's companion") && b.isDoctor()) {
            return true; // Doctor Who partner commander
        }
        return false;
    }

    public boolean canBePartnerCommander() {
        if (canBeBackground()) {
            return true;
        }
        if (!canBeCommander()) {
            return false;
        }
        return hasKeyword("Partner") || !this.partnerWith.isEmpty() || !this.partnerType.isEmpty() ||
                hasKeyword("Choose a Background") || hasKeyword("Doctor's companion") || isDoctor();
    }

    public boolean canBeBackground() {
        return mainPart.getType().hasSubtype("Background");
    }

    public boolean isDoctor() {
        Set<String> subtypes = new HashSet<>();
        for (String type : mainPart.getType().getSubtypes()) {
            subtypes.add(type);
        }

        return subtypes.size() == 2 &&
                subtypes.contains("Time Lord") &&
                subtypes.contains("Doctor");
    }

    public boolean canBeOathbreaker() {
        CardType type = mainPart.getType();
        if (mainPart.getOracleText().contains("can be your commander")) {
            return true;
        }
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
        if (canBeCreature() || type.isPlaneswalker()) {
            return true;
        }
        return false;
    }

    public boolean canBeTinyLeadersCommander() {
        CardType type = mainPart.getType();
        if (!type.isLegendary()) {
            return false;
        }
        if (canBeCreature() || type.isPlaneswalker()) {
            return true;
        }
        return false;
    }

    public boolean canBeCreature() {
        CardType type = mainPart.getType();
        if (type.isCreature()) {
            return true;
        }
        for (String staticAbility : mainPart.getStaticAbilities()) { // Check for Grist
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

    public boolean getAddsWildCardColor() {
        return addsWildCardColor;
    }

    public int getSetColorID() {
        //Could someday generalize this to support other kinds of markings.
        return setColorID;
    }

    // vanguard card fields, they don't use sides.
    private int deltaHand;
    private int deltaLife;

    private List<String> tokens = Collections.emptyList();

    public List<String> getTokens() {
        return tokens;
    }

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

    private Set<String> supportedFunctionalVariants;
    public boolean hasFunctionalVariants() {
        return this.supportedFunctionalVariants != null;
    }
    public Set<String> getSupportedFunctionalVariants() {
        return this.supportedFunctionalVariants;
    }

    public String getDisplayNameForVariant(String variantName) {
        if(supportedFunctionalVariants == null || !supportedFunctionalVariants.contains(variantName))
            return getName();

        ICardFace mainFace = Objects.requireNonNullElse(mainPart.getFunctionalVariant(variantName), mainPart);
        String mainPartName = mainFace.getDisplayName();

        if(splitType.getAggregationMethod() == CardSplitType.FaceSelectionMethod.COMBINE) {
            ICardFace otherFace = Objects.requireNonNullElse(otherPart.getFunctionalVariant(variantName), otherPart);
            String otherPartName = otherFace.getDisplayName();
            return mainPartName + " // " + otherPartName;
        }
        else
            return mainPartName;
    }

    /* package */ String findOrCreateVariantForFlavorName(String flavorName, String suggestedVariantName) {
        Objects.requireNonNull(flavorName);
        String[] nameParts = flavorName.trim().split("\\s*//\\s*");
        flavorName = String.join(" // ", nameParts); //Normalize this just in case.
        if(otherPart != null && nameParts.length < 2)
            throw new IllegalArgumentException("Tried to assign a single flavor name to a multi-faced card. Use ' // ' as a separator in the flavorName parameter.");
        if(supportedFunctionalVariants == null)
            supportedFunctionalVariants = new HashSet<>();
        for(String variantName : this.supportedFunctionalVariants) {
            if(getDisplayNameForVariant(variantName).equals(flavorName))
                return variantName;
        }
        String variantName = suggestedVariantName != null ? suggestedVariantName : "FlavorName" + flavorName.hashCode();
        if(supportedFunctionalVariants.contains(variantName))
            variantName = variantName + flavorName.hashCode();

        CardFace variantMain = ((CardFace) mainPart).getOrCreateFunctionalVariant(variantName);
        variantMain.setFlavorName(nameParts[0]);
        ((CardFace) mainPart).assignMissingFieldsToVariant(variantMain);

        if(otherPart != null) {
            CardFace variantOther = ((CardFace) otherPart).getOrCreateFunctionalVariant(variantName);
            variantOther.setFlavorName(nameParts[1]);
            ((CardFace) otherPart).assignMissingFieldsToVariant(variantOther);
        }

        supportedFunctionalVariants.add(variantName);

        return variantName;
    }

    public ColorSet getColorIdentity() {
        return colorIdentity;
    }

    /** Instantiates class, reads a card. For batch operations better create you own reader instance. */
    public static CardRules fromScript(Iterable<String> script) {
        Reader crr = new Reader();
        for (String line : script) {
            crr.parseLine(line);
        }
        return crr.getCard();
    }

    // Reads cardname.txt
    public static class Reader {
        // fields to build
        private CardFace[] faces = new CardFace[] { null, null, null, null, null, null, null };
        private int curFace = 0;
        private CardSplitType altMode = CardSplitType.None;
        private String meldWith = "";
        private String partnerWith = "";
        private String partnerType = "";
        private boolean addsWildCardColor = false;
        private int setColorID = 0;
        private String handLife = null;
        private String normalizedName = "";
        private Set<String> supportedFunctionalVariants = null;

        private List<String> tokens = Lists.newArrayList();

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
            this.setColorID = 0;
            this.curFace = 0;
            this.faces[0] = null;
            this.faces[1] = null;
            this.faces[2] = null;
            this.faces[3] = null;
            this.faces[4] = null;
            this.faces[5] = null;
            this.faces[6] = null;

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
            this.partnerType = "";
            this.addsWildCardColor = false;
            this.normalizedName = "";
            this.supportedFunctionalVariants = null;
            this.tokens = Lists.newArrayList();
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
            if (null != faces[2]) faces[2].assignMissingFields();
            if (null != faces[3]) faces[3].assignMissingFields();
            if (null != faces[4]) faces[4].assignMissingFields();
            if (null != faces[5]) faces[5].assignMissingFields();
            if (null != faces[6]) faces[6].assignMissingFields();
            final CardRules result = new CardRules(faces, altMode, cah);

            result.setNormalizedName(this.normalizedName);
            result.meldWith = this.meldWith;
            result.partnerWith = this.partnerWith;
            result.partnerType = this.partnerType;
            result.addsWildCardColor = this.addsWildCardColor;
            result.setColorID = this.setColorID;
            if (!tokens.isEmpty()) {
                result.tokens = tokens;
            }
            if (StringUtils.isNotBlank(handLife))
                result.setVanguardProperties(handLife);
            result.supportedFunctionalVariants = this.supportedFunctionalVariants;
            return result;
        }

        public final CardRules readCard(final Iterable<String> script, String filename) {
            this.reset();
            for (String line : script) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                this.parseLine(line, this.faces[curFace]);
            }
            this.normalizedName = filename;
            return this.getCard();
        }

        public final CardRules readCard(final Iterable<String> script) {
            return readCard(script, null);
        }

        /**
         * Parses a single line of a card script.
         *
         * @param line Line of text to parse.
         */
        public final void parseLine(final String line) {
            this.parseLine(line, this.faces[curFace]);
        }

        private void parseLine(final String line, CardFace face) {
            int colonPos = line.indexOf(':');
            String key = colonPos > 0 ? line.substring(0, colonPos) : line;
            String value = colonPos > 0 ? line.substring(1+colonPos).trim() : null;

            if (value != null) {
                int tokIdx = value.indexOf("TokenScript$");
                if (tokIdx > 0) {
                    String tokenParam = value.substring(tokIdx + 12).trim();
                    int endIdx = tokenParam.indexOf("|");
                    if (endIdx > 0) {
                        tokenParam = tokenParam.substring(0, endIdx).trim();
                    }
                    this.tokens.addAll(Arrays.asList(tokenParam.split(",")));
                }
            }

            switch (key.charAt(0)) {
                case 'A':
                    if ("A".equals(key)) {
                        face.addAbility(value);
                    } else if ("AI".equals(key)) {
                        colonPos = value.indexOf(':');
                        String variable = colonPos > 0 ? value.substring(0, colonPos) : value;
                        value = colonPos > 0 ? value.substring(1+colonPos) : null;

                        if ("RemoveDeck".equals(variable)) {
                            this.removedFromAIDecks |= "All".equalsIgnoreCase(value);
                            this.removedFromRandomDecks |= "Random".equalsIgnoreCase(value);
                            this.removedFromNonCommanderDecks |= "NonCommander".equalsIgnoreCase(value);
                        }
                    } else if ("AlternateMode".equals(key)) {
                        this.altMode = CardSplitType.smartValueOf(value);
                    } else if ("ALTERNATE".equals(key)) {
                        this.curFace = 1;
                    }
                    break;

                case 'C':
                    if ("Colors".equals(key)) {
                        ColorSet newCol = ColorSet.fromNames(value.split(","));
                        face.setColor(newCol);
                    }
                    break;

                case 'D':
                    if ("DeckHints".equals(key)) {
                        hints = new DeckHints(value);
                    } else if ("DeckNeeds".equals(key)) {
                        needs = new DeckHints(value);
                    } else if ("DeckHas".equals(key)) {
                        has = new DeckHints(value);
                    } else if ("Defense".equals(key)) {
                        face.setDefense(value);
                    } else if ("Draft".equals(key)) {
                        face.addDraftAction(value);
                    }
                    break;

                case 'F':
                    if("FlavorName".equals(key)) {
                        face.setFlavorName(value);
                    }

                case 'H':
                    if ("HandLifeModifier".equals(key)) {
                        handLife = value;
                    }
                    break;

                case 'K':
                    if ("K".equals(key)) {
                        face.addKeyword(value);
                        if (value.startsWith("Partner with:")) {
                            this.partnerWith = value.split(":")[1];
                        }
                        if (value.startsWith("Partner:")) {
                            this.partnerType = value.split(":")[1];
                        }
                    }
                    break;

                case 'L':
                    if ("Loyalty".equals(key)) {
                        face.setInitialLoyalty(value);
                    }
                    if ("Lights".equals(key)) {
                        face.setAttractionLights(value);
                    }
                    break;

                case 'M':
                    if ("ManaCost".equals(key)) {
                        face.setManaCost("no cost".equals(value) ? ManaCost.NO_COST : new ManaCost(value));
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
                        face.setOracleText(value);
                    }
                    break;

                case 'P':
                    if ("PT".equals(key)) {
                        face.setPtText(value);
                    }
                    break;

                case 'R':
                    if ("R".equals(key)) {
                        face.addReplacementEffect(value);
                    }
                    break;

                case 'S':
                    if ("S".equals(key)) {
                        face.addStaticAbility(value);
                    } else if (key.startsWith("SPECIALIZE")) {
                        if (value.equals("WHITE")) {
                            this.curFace = 2;
                        } else if (value.equals("BLUE")) {
                            this.curFace = 3;
                        } else if (value.equals("BLACK")) {
                            this.curFace = 4;
                        } else if (value.equals("RED")) {
                            this.curFace = 5;
                        } else if (value.equals("GREEN")) {
                            this.curFace = 6;
                        }
                    } else if ("SVar".equals(key)) {
                        if (null == value) throw new IllegalArgumentException("SVar has no variable name");

                        colonPos = value.indexOf(':');
                        String variable = colonPos > 0 ? value.substring(0, colonPos) : value;
                        value = colonPos > 0 ? value.substring(1+colonPos) : null;

                        face.addSVar(variable, value);
                    } else if (key.startsWith("SETCOLORID")) {
                        this.setColorID = Integer.parseInt(value);
                    }
                    break;

                case 'T':
                    if ("T".equals(key)) {
                        face.addTrigger(value);
                    } else if ("Types".equals(key)) {
                        face.setType(CardType.parse(value, false));
                    } else if ("Text".equals(key) && StringUtils.isNotBlank(value)) {
                        face.setNonAbilityText(value);
                    }
                    break;

                case 'V':
                    if("Variant".equals(key)) {
                        if (value == null) value = "";
                        colonPos = value.indexOf(':');
                        if(colonPos <= 0) throw new IllegalArgumentException("Missing variant name");
                        String variantName = value.substring(0, colonPos);
                        CardFace varFace = face.getOrCreateFunctionalVariant(variantName);
                        String variantLine = value.substring(1 + colonPos);
                        this.parseLine(variantLine, varFace);
                        if(this.supportedFunctionalVariants == null)
                            this.supportedFunctionalVariants = new HashSet<>();
                        this.supportedFunctionalVariants.add(variantName);
                    }
                    break;
            }
        }
    }

    public static CardRules getUnsupportedCardNamed(String name) {
        CardAiHints cah = new CardAiHints(true, true, true, null, null, null);
        CardFace[] faces = { new CardFace(name), null, null, null, null, null, null};
        faces[0].setColor(ColorSet.fromMask(0));
        faces[0].setType(CardType.parse("", false));
        faces[0].setOracleText("This card is not supported by Forge. Whenever you start a game with this card, it will be bugged.");
        faces[0].setNonAbilityText("This card is not supported by Forge.\nWhenever you start a game with this card, it will be bugged.");
        faces[0].assignMissingFields();
        final CardRules result = new CardRules(faces, CardSplitType.None, cah);

        result.unsupported = true;

        return result;
    }

    public boolean hasKeyword(final String k) {
        return Iterables.contains(mainPart.getKeywords(), k);
    }

    public boolean hasStartOfKeyword(final String k) {
        return hasStartOfKeyword(k, mainPart);
    }
    public boolean hasStartOfKeyword(final String k, ICardFace cf) {
        for (final String inst : cf.getKeywords()) {
            if (inst.startsWith(k)) {
                return true;
            }
        }
        return false;
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
