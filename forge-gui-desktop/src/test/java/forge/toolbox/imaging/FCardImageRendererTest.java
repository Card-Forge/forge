package forge.toolbox.imaging;

import java.awt.Font;
import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/** Isolated checks for the font cache; does not initialize the card database or draw a card. */
public class FCardImageRendererTest {
    @Test
    public void fontArraysAreCreatedForTheRequestedScale() {
        Font font = new Font(Font.SERIF, Font.PLAIN, 20);
        Map<Font, Font[]> at1x = FCardImageRenderer.getFontsForScale(1.0f);
        Map<Font, Font[]> at2x = FCardImageRenderer.getFontsForScale(2.0f);

        Font derived1x = FCardImageRenderer.getFontBySize(1.0f, font, 25);
        Font derived2x = FCardImageRenderer.getFontBySize(2.0f, font, 25);

        AssertJUnit.assertEquals(font.getSize() * 2, at1x.get(font).length);
        AssertJUnit.assertEquals(font.getSize() * 4, at2x.get(font).length);
        AssertJUnit.assertNotSame(at1x.get(font), at2x.get(font));
        AssertJUnit.assertSame(derived1x, at1x.get(font)[25]);
        AssertJUnit.assertSame(derived2x, at2x.get(font)[25]);
    }

    @Test
    public void higherScaleAcceptsSizesBeyondTheOneXArray() {
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
        int size = font.getSize() * 3; // larger than the 1x array, still inside the 2x array

        Font derived = FCardImageRenderer.getFontBySize(2.0f, font, size);
        Map<Font, Font[]> at2x = FCardImageRenderer.getFontsForScale(2.0f);

        AssertJUnit.assertEquals(size, derived.getSize());
        AssertJUnit.assertSame(derived, at2x.get(font)[size]);
    }
}
