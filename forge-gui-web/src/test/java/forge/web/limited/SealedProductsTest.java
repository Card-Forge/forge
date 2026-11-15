package forge.web.limited;

import forge.gamemodes.limited.SealedCardPoolGenerator;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.web.WebTestSupport;
import forge.web.protocol.ToBrowser.LimitedOptions;
import forge.web.protocol.ToBrowser.SealedBlock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/** Tests the sealed lists the web offers. */
public class SealedProductsTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    // Fails if the web offers a block, or a combo, that desktop's sealed dialog does not
    @Test
    public void sealedListsMatchDesktop() {
        final LimitedOptions lists = OfflinePools.options();
        int offered = 0;
        for (final CardBlock b : FModel.getBlocks()) {
            try {
                if (!SealedCardPoolGenerator.blockCombos(b).isEmpty()) {
                    offered++;
                }
            } catch (final RuntimeException e) {
                // Desktop fails on such a block, so the web leaves it out
            }
        }
        assertEquals(lists.blocks().size(), offered);
        final SealedBlock innistrad = lists.blocks().stream().filter(b -> b.name().equals("Innistrad")).findFirst().orElseThrow();
        final CardBlock desktop = FModel.getBlocks().get("Innistrad");
        assertEquals(innistrad.combos(), SealedCardPoolGenerator.blockCombos(desktop));
        assertEquals(innistrad.packs(), desktop.getCntBoostersSealed());
    }
}
