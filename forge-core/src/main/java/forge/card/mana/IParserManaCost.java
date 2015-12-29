package forge.card.mana;

import java.util.Iterator;


/**
 * The Interface ManaParser.
 */
public interface IParserManaCost extends Iterator<ManaCostShard> {
    int getTotalGenericCost();
}
