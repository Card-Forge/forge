package forge.card;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.ImageKeys;
import forge.StaticData;
import forge.gamesimulationtests.util.CardDatabaseHelper;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Test Case for CardDb forcing No Image for all the cards. Check that
 * everything still applies the same.
 *
 * Note: Run test for the class, being subclass will also run all other tests as
 * regression.
 */
public class CardDbWithNoImageCardDbMockTestCase extends CardDbCardMockTestCase {

    @Override
    @BeforeMethod
    public void setup() {
        super.setup();
    }

    @Override
    protected void initCardImageMocks() {
        imageKeysMock = Mockito.mockStatic(ImageKeys.class);
        imageKeysMock.when(() -> ImageKeys.hasImage(Mockito.any(PaperCard.class), Mockito.anyBoolean()))
                .thenReturn(false);
    }

    /**
     * {@link PaperCard} caches the answer to {@code hasImage}, so this class cannot share
     * the process-wide card database with the sibling classes that expect the opposite
     * answer. PowerMock's per-class classloader used to make that a non-issue.
     */
    @Override
    protected void initializeStaticData() {
        StaticData data = CardDatabaseHelper.createStaticData("CardDbWithNoImageCardDbMockTestCase", false);
        fModelMock.when(FModel::getMagicDb).thenReturn(data);
    }

    @Test
    public void testCardIsReturnedEvenIfThereIsNoImage() {
        PaperCard shivanDragon = this.cardDb.getCard(cardNameShivanDragon);
        assertNotNull(shivanDragon);
        assertFalse(ImageKeys.hasImage(shivanDragon));
        assertFalse(shivanDragon.hasImage());
    }

}
