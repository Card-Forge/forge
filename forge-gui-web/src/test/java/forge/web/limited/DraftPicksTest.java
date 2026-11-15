package forge.web.limited;

import forge.card.DraftOptions.DoublePick;
import forge.gamemodes.limited.BoosterDraft;
import forge.web.WebTestSupport;
import forge.web.protocol.FromBrowser.DraftStart;
import forge.web.protocol.ToBrowser.DraftBlockOption;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/** Tests the pick rule of an offline draft. */
public class DraftPicksTest {
    private static final String BLOCK = "Commander Legends";

    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    // Fails if a set is not offered with its own pick rule, such as the two picks in every pack of Commander Legends, a chosen pick rule does not replace the set's own, or a draft with no choice loses the set's own
    @Test
    public void aSetsOwnRuleIsOfferedAndAChosenOneReplacesIt() {
        final DraftBlockOption block = OfflinePools.options().draftBlocks().stream().filter(b -> b.name().equals(BLOCK)).findFirst().orElseThrow();
        assertEquals(block.picks(), DoublePick.ALWAYS.name());
        final BoosterDraft chosen = OfflinePools.draft(new DraftStart("Block", BLOCK, null, null, null, null, DoublePick.NEVER.name())).get();
        assertEquals(chosen.getDoublePick(), DoublePick.NEVER);
        final BoosterDraft own = OfflinePools.draft(new DraftStart("Block", BLOCK, null, null, null, null, null)).get();
        assertEquals(own.getDoublePick(), DoublePick.ALWAYS);
    }
}
