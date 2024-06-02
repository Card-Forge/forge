package forge.card;

import forge.card.mana.ManaCost;

import java.util.Set;

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
    Set<Integer> getAttractionLights();

    String getOracleText();
}
