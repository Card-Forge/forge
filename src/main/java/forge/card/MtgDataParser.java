package forge.card;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public final class MtgDataParser implements Iterator<CardRules> {

    private Iterator<String> it;
    public MtgDataParser(final Iterable<String> data) {
        it = data.iterator();
        skipSetList();
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
        String[] sets = strs.remove(strs.size() - 1).split(", ");

        return new CardRules(name, type, manaCost, ptOrLoyalty, strs.toArray(emptyArray), sets);
    }

    @Override public void remove() { } 


}
