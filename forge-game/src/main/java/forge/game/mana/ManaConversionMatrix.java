package forge.game.mana;

import forge.card.mana.ManaAtom;

public class ManaConversionMatrix {
    static byte[] identityMatrix = { ManaAtom.WHITE, ManaAtom.BLUE, ManaAtom.BLACK, ManaAtom.RED, ManaAtom.GREEN, ManaAtom.COLORLESS };

    // Conversion matrix ORs byte values to make mana more payable
    // Restrictive matrix ANDs byte values to make mana less payable
    protected byte[] colorConversionMatrix = new byte[ManaAtom.MANATYPES.length];
    protected byte[] colorRestrictionMatrix = new byte[ManaAtom.MANATYPES.length];

    protected boolean snowForColor = false;

    public boolean isSnowForColor() {
        return snowForColor;
    }

    public void setSnowForColor(boolean value) {
        snowForColor = value;
    }

    public byte getPossibleColorUses(byte color) {
        // Take the current conversion value, AND with restrictions to get mana usage
        int rowIdx = ManaAtom.getIndexOfFirstManaType(color);
        int matrixIdx = rowIdx < 0 ? identityMatrix.length - 1 : rowIdx;

        byte colorUse = colorConversionMatrix[matrixIdx];
        colorUse &= colorRestrictionMatrix[matrixIdx];
        return colorUse;
    }

    public void adjustColorReplacement(byte originalColor, byte replacementColor, boolean additive) {
        // Fix the index without hardcodes
        int rowIdx = ManaAtom.getIndexOfFirstManaType(originalColor);
        rowIdx = rowIdx < 0 ? identityMatrix.length - 1 : rowIdx;
        if (additive) {
            colorConversionMatrix[rowIdx] |= replacementColor;
        } else {
            colorRestrictionMatrix[rowIdx] &= replacementColor;
        }
    }

    public void applyCardMatrix(ManaConversionMatrix extraMatrix) {
        for (int i = 0; i < colorConversionMatrix.length; i++) {
            colorConversionMatrix[i] |= extraMatrix.colorConversionMatrix[i];
        }

        for (int i = 0; i < colorRestrictionMatrix.length; i++) {
            colorRestrictionMatrix[i] &= extraMatrix.colorRestrictionMatrix[i];
        }
        setSnowForColor(extraMatrix.isSnowForColor());
    }

    public void restoreColorReplacements() {
        // By default each color can only be paid by itself ( {G} -> {G}, {C} -> {C}
        for (int i = 0; i < colorConversionMatrix.length; i++) {
            colorConversionMatrix[i] = identityMatrix[i];
        }
        // By default all mana types are unrestricted
        for (int i = 0; i < colorRestrictionMatrix.length; i++) {
            colorRestrictionMatrix[i] = ManaAtom.ALL_MANA_TYPES;
        }
        snowForColor = false;
    }
}