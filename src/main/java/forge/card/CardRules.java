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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.google.common.collect.Lists;

import forge.card.mana.ManaCost;





/**
 * A collection of methods containing full
 * meta and gameplay properties of a card.
 * 
 * @author Forge
 * @version $Id: CardRules.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardRules implements ICardCharacteristics {

    private final CardSplitType splitType;
    private final ICardCharacteristics mainPart;
    private final ICardCharacteristics otherPart;
    
    private CardAiHints aiHints;

    Map<String, CardInSet> setsPrinted = null;
    private Iterable<String> forgeScript;



    public CardRules(ICardCharacteristics[] faces, CardSplitType altMode, CardAiHints cah, Iterable<String> script) {
        splitType = altMode;
        mainPart = faces[0];
        otherPart = faces[1];
        aiHints = cah;
        forgeScript = Lists.newArrayList(script);
    }


    /**
     * Gets the latest set printed.
     * 
     * @return the latest set printed
     */
    public String getLatestSetPrinted() {
        String lastSet = null;
        // TODO: Make a true release-date based sorting
        for (final String cs : this.setsPrinted.keySet()) {
            lastSet = cs;
        }
        return lastSet;
    }



    /**
     * Gets the rarity from latest set.
     * 
     * @return the rarity from latest set
     */
    public CardRarity getRarityFromLatestSet() {
        final CardInSet cis = this.setsPrinted.get(this.getLatestSetPrinted());
        return cis.getRarity();
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public Iterable<String> getForgeScript() {
        return forgeScript;
    }



    public boolean isTraditional() {
        return !(getType().isVanguard() || getType().isScheme() || getType().isPlane() || getType().isPhenomenon());
    }


    /**
     * @return the splitType
     */
    public CardSplitType getSplitType() {
        return splitType;
    }

    public ICardCharacteristics getMainPart() {
        // TODO Auto-generated method stub
        return mainPart;
    }


    public ICardCharacteristics getOtherPart() {
        return otherPart;
    }


    public String getName() {
        switch(splitType.getAggregationMethod()) {
            case AGGREGATE:
                return mainPart.getName() + " // " + otherPart.getName();
            default:
                return mainPart.getName();
        }
    }

    public CardAiHints getAiHints() {
        return aiHints;
    }

    @Override
    public CardType getType() {
        switch(splitType.getAggregationMethod()) {
            case AGGREGATE: // no cards currently have different types
                return CardType.combine(mainPart.getType(), otherPart.getType());
            default:
                return mainPart.getType();
        }
    }


    @Override
    public ManaCost getManaCost() {
        switch(splitType.getAggregationMethod()) {
        case AGGREGATE:
            return ManaCost.combine(mainPart.getManaCost(), otherPart.getManaCost());
        default:
            return mainPart.getManaCost();
        }
    }


    @Override
    public ColorSet getColor() {
        switch(splitType.getAggregationMethod()) {
        case AGGREGATE:
            return ColorSet.fromMask(mainPart.getColor().getColor() | otherPart.getColor().getColor()); 
        default:
            return mainPart.getColor();
        }
    }


    @Override     public int getIntPower() { return mainPart.getIntPower(); }
    @Override     public int getIntToughness() { return mainPart.getIntToughness(); }
    @Override     public String getPower() { return mainPart.getPower(); }
    @Override     public String getToughness() { return mainPart.getToughness(); }
    @Override     public int getInitialLoyalty() { return mainPart.getInitialLoyalty(); }


    @Override
    public String getOracleText() {
        switch(splitType.getAggregationMethod()) {
        case AGGREGATE:
            return mainPart.getOracleText() + "\r\n\r\n" + otherPart.getOracleText(); 
        default:
            return mainPart.getOracleText();
        }
    }


    @Override
    public Iterable<String> getKeywords() {
        switch(splitType.getAggregationMethod()) {
        case AGGREGATE:
            List<String> res = new ArrayList<String>();
            for(String ka : mainPart.getKeywords())
                res.add(ka);
            for(String kb : otherPart.getKeywords())
                res.add(kb);
            return res; 
        default:
            return mainPart.getKeywords();
        }
    }

    
    public Iterable<Entry<String, CardInSet>> getSetsPrinted() { return mainPart.getSetsPrinted(); }
    public CardInSet getEditionInfo(final String setCode) { return mainPart.getEditionInfo(setCode); }


    // vanguard card fields, they don't use sides.
    private int deltaHand;
    private int deltaLife;

    public int getHand() { return deltaHand; }
    public int getLife() { return deltaLife; }
    public void setVanguardProperties(String pt) {
        final int slashPos = pt == null ? -1 : pt.indexOf('/');
        if (slashPos == -1) {
            throw new RuntimeException(String.format("Vanguard '%s' has bad hand/life stats", this.getName()));
        }
        this.deltaHand = Integer.parseInt(pt.substring(0, pt.indexOf('/')).replace("+", ""));
        this.deltaLife = Integer.parseInt(pt.substring(pt.indexOf('/') + 1).replace("+", ""));
    }

    // Downloadable image
    private String dlUrl;
    private String dlUrlOtherSide;
    public String getPictureUrl() { return dlUrl; }
    public String getPictureOtherSideUrl() { return dlUrlOtherSide; }
    public void setDlUrls(String[] dlUrls) { this.dlUrl = dlUrls[0]; this.dlUrlOtherSide = dlUrls[1]; }



}
