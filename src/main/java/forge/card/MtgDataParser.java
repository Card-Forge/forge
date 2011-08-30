package forge.card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.FileUtil;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

public final class MtgDataParser implements Iterator<CardRules> {

    private Iterator<String> it;
    private final List<String> mtgDataLines; 
    public MtgDataParser() {
        mtgDataLines = FileUtil.readFile(ForgeProps.getFile(NewConstants.MTG_DATA));
        it = mtgDataLines.iterator();
        skipSetList();
    }
    
    private static List<String> setsToSkipPrefixes = new ArrayList<String>();
    private static List<String> unSets = new ArrayList<String>(); // take only lands from there
    static {
        setsToSkipPrefixes.add("VG"); // Vanguard
        setsToSkipPrefixes.add("ME"); // Mtgo master's editions
        setsToSkipPrefixes.add("FV"); // From the vaults

        // Duel decks... or... should I keep them?
        setsToSkipPrefixes.add("DVD");
        setsToSkipPrefixes.add("EVT");
        setsToSkipPrefixes.add("EVG");
        setsToSkipPrefixes.add("GVL");
        setsToSkipPrefixes.add("JVC");
        setsToSkipPrefixes.add("DDG");
        setsToSkipPrefixes.add("PVC");

        // Archenemy - we cannot play it now anyway
        setsToSkipPrefixes.add("ARC");

        // Planechase - this too
        setsToSkipPrefixes.add("HOP");

        // Reprints
        setsToSkipPrefixes.add("BRB");
        setsToSkipPrefixes.add("BTD");
        setsToSkipPrefixes.add("DKM");
        //setsToSkipPrefixes.add("ATH"); // No need to skip it really.
                                         // On gatherer's opinion this cards was releases twice in original set

        // Promo sets - all cards have been issued in other sets
        setsToSkipPrefixes.add("SDC");
        setsToSkipPrefixes.add("ASTRAL");

        // Premium decks
        setsToSkipPrefixes.add("H09");
        setsToSkipPrefixes.add("H10");

        // Un-sets are weird, but lands from there are valuable
        unSets.add("UNH");
        unSets.add("UGL");
    }

    private boolean weHaveNext;
    private void skipSetList() {
        String nextLine = it.next();
        while (nextLine.length() > 0 && it.hasNext()) {
            nextLine = it.next();
        }
        weHaveNext = it.hasNext();
    }

    @Override
    public boolean hasNext() { return weHaveNext; }

    private static final String[] emptyArray = new String[0]; // list.toArray() needs this =(

    @Override
    public CardRules next() {
        if (!it.hasNext()) { weHaveNext = false; return null; }
        String name = it.next();

        if (!it.hasNext()) { weHaveNext = false; return null; }
        String manaCost = it.next();
        CardType type = null;
        if (manaCost.startsWith("{")) {
            if (!it.hasNext()) { weHaveNext = false; return null; }
            type = CardType.parse(it.next());
        } else { // Land?
            type = CardType.parse(manaCost);
            manaCost = null;
        }
        String ptOrLoyalty = null;
        if (type.isCreature() || type.isPlaneswalker()) {
            if (!it.hasNext()) { weHaveNext = false; return null; }
            ptOrLoyalty = it.next();
        }

        List<String> strs = new ArrayList<String>();
        if (!it.hasNext()) { weHaveNext = false; return null; }
        String nextLine = it.next();
        while (StringUtils.isNotBlank(nextLine) && it.hasNext()) {
            strs.add(nextLine);
            nextLine = it.next();
        }
        // feel free to return null after this line

        String setsLine = strs.remove(strs.size() - 1);
        boolean isBasicLand = type.isLand() && type.isBasic();
        Map<String, CardInSet> sets = getValidEditions(setsLine, isBasicLand);

        if (sets.isEmpty()) { return null; } // that was a bad card - it won't be added by invoker

        return new CardRules(name, type, manaCost, ptOrLoyalty, strs.toArray(emptyArray), sets,
                // TODO: fix last two parameters
                false, false);
    }

    private Map<String, CardInSet> getValidEditions(final String sets, final boolean isBasicLand) {
        String[] setsData = sets.split(", ");
        Map<String, CardInSet> result = new HashMap<String, CardInSet>();
        for (int iSet = 0; iSet < setsData.length; iSet++) {
            int spacePos = setsData[iSet].indexOf(' ');
            String setCode = setsData[iSet].substring(0, spacePos);
            boolean shouldSkip = false;
            for (String s : setsToSkipPrefixes) { if (setCode.startsWith(s)) { shouldSkip = true; break; } }
            for (String s : unSets) { if (setCode.startsWith(s) && !isBasicLand) { shouldSkip = true; break; } }
            if (shouldSkip) { continue; }
            result.put(setCode, parseCardInSet(setsData[iSet], spacePos));
        }
        return result;
    }

    public static CardInSet parseCardInSet(final String unparsed, final int spaceAt) {
        char rarity = unparsed.charAt(spaceAt + 1);
        CardRarity rating;
        switch (rarity) {
            case 'L': rating = CardRarity.BasicLand; break;
            case 'C': rating = CardRarity.Common; break;
            case 'U': rating = CardRarity.Uncommon; break;
            case 'R': rating = CardRarity.Rare; break;
            case 'M': rating = CardRarity.MythicRare; break;
            case 'S': rating = CardRarity.Special; break;
            default: rating = CardRarity.MythicRare; break;
        }

        int number = 1;
        int bracketAt = unparsed.indexOf('(', spaceAt);
        if (-1 != bracketAt) {
            String sN = unparsed.substring(bracketAt + 2, unparsed.indexOf(')', bracketAt));
            number = Integer.parseInt(sN);
        }
        return new CardInSet(rating, number);
    }

    @Override public void remove() { }


}
