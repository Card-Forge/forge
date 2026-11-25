package forge.ai.simulation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;

/** The loader of the card lists the AI combo logic reads (roles and two card combos). */
public class AiCardListsTest {
    private static Supplier<List<String>> lines(final String... l) {
        return () -> Arrays.asList(l);
    }

    @SafeVarargs
    private static void configure(final Supplier<List<String>> roles, final Supplier<List<String>>... combos) {
        AiCardLists.configure(roles, combos);
    }

    /** Other tests expect the real files, so put them back. */
    @AfterClass(alwaysRun = true)
    public void restoreRealFiles() {
        AiCardLists.configure(() -> FileUtil.readFile(ForgeConstants.AI_CARD_ROLES_FILE),
                () -> FileUtil.readFile(ForgeConstants.COMMANDER_BRACKET_COMBOS_FILE),
                () -> FileUtil.readFile(ForgeConstants.AI_COMMANDER_COMBOS_FILE));
    }

    @Test
    public void rolesAreReadIgnoringCaseAccentsAndSpaces() {
        configure(lines("# a comment", "", "big_target | Griselbrand | Éomer,  Marshal   # trailing comment"));
        assertTrue(AiCardLists.has(AiCardLists.Role.BIG_TARGET, "griselbrand"));
        assertTrue(AiCardLists.has(AiCardLists.Role.BIG_TARGET, "Eomer, Marshal"));
        assertFalse(AiCardLists.has(AiCardLists.Role.BIG_TARGET, "Emrakul"));
        assertFalse(AiCardLists.has(AiCardLists.Role.BIG_TARGET, (String) null));
        assertTrue(AiCardLists.names(AiCardLists.Role.OVERRUN).isEmpty());
    }

    @Test
    public void roleKeyAcceptsDashesAndCase() {
        configure(lines("Big-Target | Griselbrand", "STORM PAYOFF | Tendrils of Agony"));
        assertTrue(AiCardLists.has(AiCardLists.Role.BIG_TARGET, "Griselbrand"));
        assertTrue(AiCardLists.has(AiCardLists.Role.STORM_PAYOFF, "Tendrils of Agony"));
    }

    @Test
    public void unknownRolesAndMalformedLinesAreIgnored() {
        configure(lines("no_such_role | Griselbrand", "big_target", "big_target | Archon of Cruelty"));
        assertEquals(AiCardLists.names(AiCardLists.Role.BIG_TARGET).size(), 1);
        assertTrue(AiCardLists.has(AiCardLists.Role.BIG_TARGET, "Archon of Cruelty"));
    }

    @Test
    public void aRoleCanBeSplitOverSeveralLines() {
        configure(lines("big_target | Griselbrand", "big_target | Archon of Cruelty"));
        assertEquals(AiCardLists.names(AiCardLists.Role.BIG_TARGET).size(), 2);
    }

    @Test
    public void missingFilesGiveEmptyLists() {
        configure(() -> null);
        assertTrue(AiCardLists.names(AiCardLists.Role.BIG_TARGET).isEmpty());
        assertEquals(AiCardLists.pairCount(), 0);

        configure(() -> {
            throw new IllegalStateException("unreadable");
        }, () -> {
            throw new IllegalStateException("unreadable");
        });
        assertTrue(AiCardLists.names(AiCardLists.Role.BIG_TARGET).isEmpty());
        assertEquals(AiCardLists.pairCount(), 0);
    }

    @Test
    public void pairsAreDedupedAcrossFilesInEitherOrder() {
        configure(lines(),
                lines("early_game | Palinchron | Peregrine Drake", "late_game | Thassa's Oracle | Demonic Consultation"),
                lines("early_game | peregrine drake | PALINCHRON", "early_game | Devoted Druid | Melira, Sylvok Outcast"));
        assertEquals(AiCardLists.pairCount(), 3);
    }

    @Test
    public void theFirstFileWinsADuplicate() {
        configure(lines(), lines("early_game | Palinchron | Peregrine Drake"), lines("late_game | Peregrine Drake | Palinchron"));
        final Set<String> deck = AiCardLists.deckKeys(Arrays.asList("Palinchron", "Peregrine Drake"));
        final List<AiCardLists.Pair> found = AiCardLists.combosIn(deck);
        assertEquals(found.size(), 1);
        assertEquals(found.get(0).first, "Palinchron");
    }

    @Test
    public void aPairNeedsBothCardsAndIsFoundOnce() {
        configure(lines(), lines("early_game | Palinchron | Peregrine Drake"));
        assertTrue(AiCardLists.combosIn(AiCardLists.deckKeys(Collections.singletonList("Palinchron"))).isEmpty());
        assertEquals(AiCardLists.combosIn(AiCardLists.deckKeys(Arrays.asList("Peregrine Drake", "Palinchron", "Island"))).size(), 1);
    }

    @Test
    public void malformedPairsAreIgnored() {
        configure(lines(), lines("early_game | Palinchron", "early_game | Same | same", "early_game |  | Peregrine Drake", "garbage"));
        assertEquals(AiCardLists.pairCount(), 0);
    }

    @Test
    public void facesOfADoubleFacedOrSplitCardMatch() {
        configure(lines(), lines("early_game | Fire | Thassa's Oracle"));
        final Set<String> keys = AiCardLists.deckKeys(Arrays.asList("Fire // Ice", "Thassa's Oracle"));
        assertTrue(keys.contains("fire // ice"));
        assertTrue(keys.contains("fire"));
        assertTrue(keys.contains("ice"));
        assertEquals(AiCardLists.combosIn(keys).size(), 1);
    }

    @Test
    public void normalizeIsStable() {
        assertEquals(AiCardLists.normalize("  Éomer,   Marshal "), "eomer, marshal");
        assertEquals(AiCardLists.normalize("Lim-Dûl's Vault"), "lim-dul's vault");
        assertEquals(new HashSet<>(AiCardLists.keysOf("A // B")).size(), 3);
    }

    @Test
    public void reloadReadsTheFilesAgain() {
        final String[] content = {"big_target | Griselbrand"};
        configure(() -> Arrays.asList(content));
        assertTrue(AiCardLists.has(AiCardLists.Role.BIG_TARGET, "Griselbrand"));
        content[0] = "big_target | Archon of Cruelty";
        assertTrue(AiCardLists.has(AiCardLists.Role.BIG_TARGET, "Griselbrand"), "cached until reload");
        AiCardLists.reload();
        assertFalse(AiCardLists.has(AiCardLists.Role.BIG_TARGET, "Griselbrand"));
        assertTrue(AiCardLists.has(AiCardLists.Role.BIG_TARGET, "Archon of Cruelty"));
    }
}
