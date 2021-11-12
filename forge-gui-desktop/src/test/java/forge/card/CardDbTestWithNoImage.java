package forge.card;

import forge.ImageCache;
import forge.ImageKeys;
import forge.item.PaperCard;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

/**
 * Test Case for CardDb forcing No Image for all the cards.
 * Check that everything still applies the same.
 *
 * Note: Run test for the class, being subclass will also run all
 * other tests as regression.
 */
public class CardDbTestWithNoImage extends CardDbTestCase {

    @Override
    @BeforeMethod
    public void setup(){
        super.setup();
    }

    @Override
    protected void initCardImageMocks() {
        PowerMockito.mockStatic(ImageIO.class);
        PowerMockito.mockStatic(ImageCache.class);
        PowerMockito.mockStatic(ImageKeys.class);
        PowerMockito.when(ImageKeys.hasImage(Mockito.any(PaperCard.class), Mockito.anyBoolean())).thenReturn(false);
    }

    @Test
    public void testCardIsReturnedEvenIfThereIsNoImage(){
        PaperCard shivanDragon = this.cardDb.getCard(cardNameShivanDragon);
        assertNotNull(shivanDragon);
        assertFalse(ImageKeys.hasImage(shivanDragon));
        assertFalse(shivanDragon.hasImage());
    }

}
