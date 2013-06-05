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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;


import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Singletons;
import forge.item.CardDb;
import forge.item.PaperCard;
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

    private static final String LAND = "Land";
    public static final String ANY = "Any";
    public static final String COMMON = "Common";
    public static final String UNCOMMON = "Uncommon";
    public static final String UNCOMMON_RARE = "UncommonRare";
    public static final String RARE = "Rare";
    public static final String RARE_MYTHIC = "RareMythic";
    public static final String MYTHIC = "Mythic";
    public static final String BASIC_LAND = "BasicLand";
    public static final String TIME_SHIFTED = "TimeShifted";


    private final static Map<String, PrintSheet> cachedSheets = new TreeMap<String, PrintSheet>(String.CASE_INSENSITIVE_ORDER);
    private static final synchronized PrintSheet getPrintSheet(String key) {
        if( !cachedSheets.containsKey(key) )
            cachedSheets.put(key, makeSheet(key, CardDb.instance().getAllCards()));
        return cachedSheets.get(key);
    }

    public static final List<PaperCard> getBoosterPack(SealedProductTemplate booster) {
        List<PaperCard> result = new ArrayList<PaperCard>();
        for(Pair<String, Integer> slot : booster.getSlots()) {
            String slotType = slot.getLeft(); // add expansion symbol here?
            int numCards = slot.getRight().intValue();

            String[] sType = TextUtil.splitWithParenthesis(slotType, ' ');
            String setCode = sType.length == 1 && booster.getEdition() != null ?  booster.getEdition() : null;
            String sheetKey = Singletons.getModel().getEditions().contains(setCode) ? slotType.trim() + " " + setCode: slotType.trim(); 

            PrintSheet ps = getPrintSheet(sheetKey);
            result.addAll(ps.random(numCards, true));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static final PrintSheet makeSheet(String sheetKey, Iterable<PaperCard> src) {
        PrintSheet ps = new PrintSheet(sheetKey);
        String[] sKey = TextUtil.splitWithParenthesis(sheetKey, ' ', 2);
        Predicate<PaperCard> setPred = (Predicate<PaperCard>) (sKey.length > 1 ? IPaperCard.Predicates.printedInSets(sKey[1].split(" ")) : Predicates.alwaysTrue());

        List<String> operators = new LinkedList<String>(Arrays.asList(TextUtil.splitWithParenthesis(sKey[0], ':')));
        Predicate<PaperCard> extraPred = buildExtraPredicate(operators);

        // source replacement operators - if one is applied setPredicate will be ignored
        Iterator<String> itMod = operators.iterator();
        while(itMod.hasNext()) {
            String mainCode = itMod.next();
            if ( mainCode.regionMatches(true, 0, "fromSheet", 0, 9)) { // custom print sheet
                String sheetName = StringUtils.strip(mainCode.substring(9), "()\" ");
                src = Singletons.getModel().getPrintSheets().get(sheetName).toFlatList();
                setPred = Predicates.alwaysTrue();

            } else if (mainCode.startsWith("promo")) { // get exactly the named cards, that's a tiny inlined print sheet
                String list = StringUtils.strip(mainCode.substring(5), "() ");
                String[] cardNames = TextUtil.splitWithParenthesis(list, ',', '"', '"');
                List<PaperCard> srcList = new ArrayList<PaperCard>();
                for(String cardName: cardNames)
                    srcList.add(CardDb.instance().getCard(cardName));
                src = srcList;
                setPred = Predicates.alwaysTrue();

            } else
                continue;

            itMod.remove();
        }

        // only special operators should remain by now - the ones that could not be turned into one predicate
        String mainCode = operators.isEmpty() ? null : operators.get(0).trim();

        if( null == mainCode || mainCode.equalsIgnoreCase(ANY) ) { // no restriction on rarity
            Predicate<PaperCard> predicate = Predicates.and(setPred, extraPred);
            ps.addAll(Iterables.filter(src, predicate));

        } else if ( mainCode.equalsIgnoreCase(UNCOMMON_RARE) ) { // for sets like ARN, where U1 cards are considered rare and U3 are uncommon
            Predicate<PaperCard> predicateRares = Predicates.and(setPred, IPaperCard.Predicates.Presets.IS_RARE, extraPred);
            ps.addAll(Iterables.filter(src, predicateRares));

            Predicate<PaperCard> predicateUncommon = Predicates.and( setPred, IPaperCard.Predicates.Presets.IS_UNCOMMON, extraPred);
            ps.addAll(Iterables.filter(src, predicateUncommon), 3);

        } else if ( mainCode.equalsIgnoreCase(RARE_MYTHIC) ) {
            // Typical ratio of rares to mythics is 53:15, changing to 35:10 in smaller sets.
            // To achieve the desired 1:8 are all mythics are added once, and all rares added twice per print sheet.

            Predicate<PaperCard> predicateMythic = Predicates.and( setPred, IPaperCard.Predicates.Presets.IS_MYTHIC_RARE, extraPred);
            ps.addAll(Iterables.filter(src, predicateMythic));

            Predicate<PaperCard> predicateRare = Predicates.and( setPred, IPaperCard.Predicates.Presets.IS_RARE, extraPred);
            ps.addAll(Iterables.filter(src, predicateRare), 2);
        } else 
            throw new IllegalArgumentException("Booster generator: operator could not be parsed - " + mainCode);
        return ps;
    }

    /**
     * This method also modifies passed parameter
     */
    private static Predicate<PaperCard> buildExtraPredicate(List<String> operators) {
        List<Predicate<PaperCard>> conditions = new ArrayList<Predicate<PaperCard>>();
        
        Iterator<String> itOp = operators.iterator();
        while(itOp.hasNext()) {
            String operator = itOp.next();
            if(StringUtils.isEmpty(operator)) {
                itOp.remove();
                continue;
            }
            
            if(operator.endsWith("s"))
                operator = operator.substring(0, operator.length()-1);
            
            boolean invert = operator.charAt(0) == '!';
            if( invert ) operator = operator.substring(1);
            
            Predicate<PaperCard> toAdd = null;
            if( operator.equalsIgnoreCase("dfc") ) {                toAdd = Predicates.compose(CardRulesPredicates.splitType(CardSplitType.Transform), PaperCard.FN_GET_RULES);
            } else if ( operator.equalsIgnoreCase(LAND) ) {         toAdd = Predicates.compose(CardRulesPredicates.Presets.IS_LAND, PaperCard.FN_GET_RULES);
            } else if ( operator.equalsIgnoreCase(BASIC_LAND)) {    toAdd = IPaperCard.Predicates.Presets.IS_BASIC_LAND;
            } else if ( operator.equalsIgnoreCase(TIME_SHIFTED)) {  toAdd = IPaperCard.Predicates.Presets.IS_SPECIAL;
            } else if ( operator.equalsIgnoreCase(MYTHIC)) {        toAdd = IPaperCard.Predicates.Presets.IS_MYTHIC_RARE;
            } else if ( operator.equalsIgnoreCase(RARE)) {          toAdd = IPaperCard.Predicates.Presets.IS_RARE;
            } else if ( operator.equalsIgnoreCase(UNCOMMON)) {      toAdd = IPaperCard.Predicates.Presets.IS_UNCOMMON;
            } else if ( operator.equalsIgnoreCase(COMMON)) {        toAdd = IPaperCard.Predicates.Presets.IS_COMMON;
            } else if ( operator.startsWith("name(") ) {
                operator = StringUtils.strip(operator.substring(4), "() ");
                String[] cardNames = TextUtil.splitWithParenthesis(operator, ',', '"', '"');
                toAdd = IPaperCard.Predicates.names(Lists.newArrayList(cardNames));
            }

            if(toAdd == null)
                continue;
            else
                itOp.remove();

            if( invert )
                toAdd = Predicates.not(toAdd);
            conditions.add(toAdd);
        }
        if( conditions.isEmpty() )
            return Predicates.alwaysTrue(); 
        return Predicates.and(conditions);
    }
        

}
