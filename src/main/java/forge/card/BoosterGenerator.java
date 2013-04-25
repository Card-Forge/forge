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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;


import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Singletons;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.IPaperCard;
import forge.item.PrintSheet;
import forge.util.TextUtil;

/**
 * <p>
 * BoosterGenerator class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class BoosterGenerator {
    private final static Map<String, PrintSheet> cachedSheets = new HashMap<String, PrintSheet>();
    private static final synchronized PrintSheet getPrintSheet(String key) {
        if( !cachedSheets.containsKey(key) )
            cachedSheets.put(key, makeSheet(key, CardDb.instance().getAllCards()));
        return cachedSheets.get(key);
    }

    public static final List<CardPrinted> getBoosterPack(SealedProductTemplate booster) {
        List<CardPrinted> result = new ArrayList<CardPrinted>();
        for(Pair<String, Integer> slot : booster.getSlots()) {
            String slotType = slot.getLeft(); // add expansion symbol here?
            int numCards = slot.getRight().intValue();

            String[] sType = TextUtil.splitWithParenthesis(slotType, ' ', '(', ')');
            String sheetKey = sType.length == 1 ? slotType.trim() + " " + booster.getEdition() : slotType.trim(); 

            PrintSheet ps = getPrintSheet(sheetKey);
            result.addAll(ps.random(numCards, true));
        }
        return result;
    }

    // If they request cards from an arbitrary pool, there's no use to cache printsheets.
    public static final List<CardPrinted> getBoosterPack(SealedProductTemplate booster, Iterable<CardPrinted> sourcePool) {
        if(sourcePool == CardDb.instance().getAllCards())
            throw new IllegalArgumentException("Do not use this overload to obtain boosters based on complete cardDb");
        
        List<CardPrinted> result = new ArrayList<CardPrinted>();
        for(Pair<String, Integer> slot : booster.getSlots()) {
            String slotType = slot.getLeft(); // add expansion symbol here?
            int numCards = slot.getRight().intValue();

            PrintSheet ps = makeSheet(slotType, sourcePool);
            result.addAll(ps.random(numCards, true));
        }
        return result;
    }    
    
    @SuppressWarnings("unchecked")
    private static final PrintSheet makeSheet(String sheetKey, Iterable<CardPrinted> src) {
        PrintSheet ps = new PrintSheet(sheetKey);
        String[] sKey = TextUtil.splitWithParenthesis(sheetKey, ' ', '(', ')', 2);
        
        String[] operators = TextUtil.splitWithParenthesis(sKey[0], ':', '(', ')');
        Predicate<CardPrinted> extraPred = buildExtraPredicate(operators);
        String mainCode = operators[0].trim();
        if(mainCode.endsWith("s"))
            mainCode = mainCode.substring(0, mainCode.length()-1);

        Predicate<CardPrinted> setPred = (Predicate<CardPrinted>) (sKey.length > 1 ? IPaperCard.Predicates.printedInSets(sKey[1].split(" ")) : Predicates.alwaysTrue());

        // Pre-defined sheets:
        if (mainCode.startsWith("promo(")) { // get exactly the named cards, nevermind any restrictions
            String list = StringUtils.strip(mainCode.substring(5), "() ");
            String[] cardNames = TextUtil.splitWithParenthesis(list, ',', '"', '"');
            for(String cardName: cardNames) {
                ps.add(CardDb.instance().getCard(cardName));
            }

        } else if( mainCode.equalsIgnoreCase("any") ) { // no restriction on rarity
            Predicate<CardPrinted> predicate = Predicates.and(setPred, extraPred);
            ps.addAll(Iterables.filter(src, predicate));

        } else if( mainCode.equalsIgnoreCase("common") ) {
            Predicate<CardPrinted> predicate = Predicates.and(setPred, IPaperCard.Predicates.Presets.IS_COMMON, extraPred);
            ps.addAll(Iterables.filter(src, predicate));

        } else if ( mainCode.equalsIgnoreCase("uncommon") ) {
            Predicate<CardPrinted> predicate = Predicates.and(setPred, IPaperCard.Predicates.Presets.IS_UNCOMMON, extraPred);
            ps.addAll(Iterables.filter(src, predicate));

        } else if ( mainCode.equalsIgnoreCase("uncommonrare") ) { // for sets like ARN, where U1 cards are considered rare and U3 are uncommon

            Predicate<CardPrinted> predicateRares = Predicates.and(setPred, IPaperCard.Predicates.Presets.IS_RARE, extraPred);
            ps.addAll(Iterables.filter(src, predicateRares));
            
            Predicate<CardPrinted> predicateUncommon = Predicates.and( setPred, IPaperCard.Predicates.Presets.IS_UNCOMMON, extraPred);
            ps.addAll(Iterables.filter(src, predicateUncommon), 3);

        } else if ( mainCode.equalsIgnoreCase("rare") ) {
            // Typical ratio of rares to mythics is 53:15, changing to 35:10 in smaller sets.
            // To achieve the desired 1:8 are all mythics are added once, and all rares added twice per print sheet.

            Predicate<CardPrinted> predicateMythic = Predicates.and( setPred, IPaperCard.Predicates.Presets.IS_MYTHIC_RARE, extraPred);
            ps.addAll(Iterables.filter(src, predicateMythic));
            
            Predicate<CardPrinted> predicateRare = Predicates.and( setPred, IPaperCard.Predicates.Presets.IS_RARE, extraPred);
            ps.addAll(Iterables.filter(src, predicateRare), 2);
            
        } else if ( mainCode.equalsIgnoreCase("rarenotmythic") ) {
            Predicate<CardPrinted> predicateRare = Predicates.and( setPred, IPaperCard.Predicates.Presets.IS_RARE, extraPred);
            ps.addAll(Iterables.filter(src, predicateRare));
            
        } else if ( mainCode.equalsIgnoreCase("mythic") ) {
            Predicate<CardPrinted> predicateMythic = Predicates.and( setPred, IPaperCard.Predicates.Presets.IS_MYTHIC_RARE, extraPred);
            ps.addAll(Iterables.filter(src, predicateMythic));

        } else if ( mainCode.equalsIgnoreCase("basicland") ) {
            Predicate<CardPrinted> predicateLand = Predicates.and( setPred, IPaperCard.Predicates.Presets.IS_BASIC_LAND, extraPred );
            ps.addAll(Iterables.filter(src, predicateLand));

        } else if ( mainCode.equalsIgnoreCase("timeshifted") ) {
            Predicate<CardPrinted> predicate = Predicates.and( setPred, IPaperCard.Predicates.Presets.IS_SPECIAL, extraPred );
            ps.addAll(Iterables.filter(src, predicate));

        } else if ( mainCode.startsWith("Custom(") || mainCode.startsWith("custom(") ) {
            String sheetName = StringUtils.strip(mainCode.substring(6), "()\" ");
            return Singletons.getModel().getPrintSheets().get(sheetName);
        }
        return ps;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param operators
     * @return
     */
    private static Predicate<CardPrinted> buildExtraPredicate(String[] operators) {
        if ( operators.length == 0)
            return Predicates.alwaysTrue();
        
        List<Predicate<CardPrinted>> conditions = new ArrayList<Predicate<CardPrinted>>();
        for(int i = 1; i < operators.length; i++) {
            String operator = operators[i];
            if(StringUtils.isEmpty(operator))
                continue;
            
            boolean invert = operator.charAt(0) == '!';
            if( invert ) operator = operator.substring(1);
            
            Predicate<CardPrinted> toAdd = null;
            if( operator.equals("dfc") ) {
                toAdd = Predicates.compose(CardRulesPredicates.splitType(CardSplitType.Transform), CardPrinted.FN_GET_RULES);
            } else if ( operator.startsWith("name(") ) {
                operator = StringUtils.strip(operator.substring(4), "() ");
                String[] cardNames = TextUtil.splitWithParenthesis(operator, ',', '"', '"');
                toAdd = IPaperCard.Predicates.names(Lists.newArrayList(cardNames));
            }

            if(toAdd == null)
                throw new IllegalArgumentException("Booster generator: operator could not be parsed - " + operator);

            if( invert )
                toAdd = Predicates.not(toAdd);
            conditions.add(toAdd);
        }
        return Predicates.and(conditions);
    }
        

}
