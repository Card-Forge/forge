package forge.game.mana;

import forge.card.mana.ManaAtom;

public interface IManaConversionMatrix {
    // Conversion matrix ORs byte values to make mana more payable
    // Restrictive matrix ANDs byte values to make mana less payable
    byte[] colorConversionMatrix = new byte[ManaAtom.MANATYPES.length];
    byte[] colorRestrictionMatrix = new byte[ManaAtom.MANATYPES.length];

}
