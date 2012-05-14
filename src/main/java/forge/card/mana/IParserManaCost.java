package forge.card.mana;

import java.util.Iterator;


/**
 * The Interface ManaParser.
 */
public interface IParserManaCost extends Iterator<ManaCostShard> {

    /**
     * Gets the total colorless cost.
     * 
     * @return the total colorless cost
     */
    int getTotalColorlessCost();
}