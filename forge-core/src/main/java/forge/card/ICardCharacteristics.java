package forge.card;

import forge.card.mana.ManaCost;

public interface ICardCharacteristics {
    String   getName();
    CardType getType();
    ManaCost getManaCost();
    ColorSet getColor();

    int    getIntPower();
    int    getIntToughness();
    String getPower();
    String getToughness();
    String getInitialLoyalty();
    String getDefense();

    String getOracleText();
}
