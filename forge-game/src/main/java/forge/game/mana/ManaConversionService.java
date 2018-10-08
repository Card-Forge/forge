package forge.game.mana;

import forge.card.mana.ManaAtom;

public class ManaConversionService {
    static byte[] identityMatrix = { ManaAtom.WHITE, ManaAtom.BLUE, ManaAtom.BLACK, ManaAtom.RED, ManaAtom.GREEN, ManaAtom.COLORLESS };

    IManaConversionMatrix matrix;

    public ManaConversionService(IManaConversionMatrix mtrx) {
        matrix = mtrx;
    }

    public void adjustColorReplacement(byte originalColor, byte replacementColor, boolean additive) {
        // Fix the index without hardcodes
        int rowIdx = ManaAtom.getIndexOfFirstManaType(originalColor);
        rowIdx = rowIdx < 0 ? identityMatrix.length - 1 : rowIdx;
        if (additive) {
            matrix.colorConversionMatrix[rowIdx] |= replacementColor;
        }
        else {
            matrix.colorRestrictionMatrix[rowIdx] &= replacementColor;
        }
    }

    public void restoreColorReplacements() {
        // By default each color can only be paid by itself ( {G} -> {G}, {C} -> {C}
        for (int i = 0; i < matrix.colorConversionMatrix.length; i++) {
            matrix.colorConversionMatrix[i] = identityMatrix[i];
        }
        // By default all mana types are unrestricted
        for (int i = 0; i < matrix.colorRestrictionMatrix.length; i++) {
            matrix.colorRestrictionMatrix[i] = ManaAtom.ALL_MANA_TYPES;
        }
    }
}
