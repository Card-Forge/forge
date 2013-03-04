package forge.card;

import forge.card.mana.ManaCost;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface ICardCharacteristics {

    public abstract String getName();
    public abstract CardType getType();
    public abstract ManaCost getManaCost();
    public abstract ColorSet getColor();

    public abstract int getIntPower();
    public abstract int getIntToughness();
    public abstract String getPower();
    public abstract String getToughness();
    public abstract int getInitialLoyalty();

    public abstract String getOracleText();
}