package forge.ai;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * #5634: what the stack shows for a spell or ability must read like the card (no raw script text, no "Exile. ", no
 * "NICKNAMEreveals"). Each row names the card, which of its abilities is looked at, and what the text must and must not contain.
 */
public class StackDescriptionTest extends AITest {

    @Test
    public void sharedDescriptionCleanupPreservesSentenceCase() {
        assertEquals("Haste. It gains haste.",
                SpellAbilityEffect.tidyDescription("haste . It  gains haste .", false));

        // Sub-ability text must retain its continuation-style lowercase.
        assertEquals("then draw a card.",
                SpellAbilityEffect.tidyDescription("then draw a card.", true));
    }

    private static final class Row {
        final String card;
        final int ability;       // index into the card's spell abilities, or -1 for a charm mode
        final String mode;       // name of the charm mode's SVar when ability == -1
        final String[] must;     // regular expressions that must be found
        final String[] mustNot;  // regular expressions that must not be found

        Row(final String card, final int ability, final String mode, final String[] must, final String[] mustNot) {
            this.card = card; this.ability = ability; this.mode = mode; this.must = must; this.mustNot = mustNot;
        }

        @Override public String toString() { return card + (mode == null ? "" : " " + mode) + (ability > 0 ? " #" + ability : ""); }
    }

    private static Row row(final String card, final int ability, final String[] must, final String[] mustNot) {
        return new Row(card, ability, null, must, mustNot);
    }

    private static String[] a(final String... s) { return s; }

    @DataProvider(name = "rows")
    public Object[][] rows() {
        final List<Row> l = new ArrayList<>();
        l.add(row("Assassin's Trophy", 0, a("basic land card", "may search their library", "put it onto the battlefield, then shuffle\\."), a("Basic\\.Land", "Land\\.Basic", "[Pp]layer who cast", "OPTIONAL")));
        l.add(row("All Is Dust", 0, a("all colored permanents"), a("Sacrifice permanents", "nonColorless")));
        l.add(row("Recross the Paths", 0, a("\\sreveals cards", "land card"), a("[a-z]reveals", "CARDNAME")));
        l.add(row("Surgical Extraction", 0, a("graveyard"), a("until end of turn", "CARDNAME")));
        l.add(row("The One Ring", 1, a("puts a burden counter on The One Ring", "draws a card for each burden counter"), a("zero cards", "CARDNAME", "Ring for each")));
        l.add(row("Nahiri, the Harbinger", 3, a("artifact or creature card", "It gains haste\\."), a("Artifact,Creature", "\\. gains haste", "\\s{2,}", "\\s[.,]")));
        l.add(row("Crypt Incursion", 0, a("gains 3 life for each card"), a("gains 0 life")));
        l.add(row("Wildfire", 0, a("4 damage to each creature"), a("damage to Creature")));
        l.add(row("Tear Asunder", 0, a("Exile Darksteel Relic"), a("Exile\\.\\s*Exile", "CARDNAME")));
        l.add(row("Wrath of God", 0, a("Destroy all creatures"), a("Destroy permanents", "OPTIONAL", "HIDDEN")));
        l.add(row("Diabolic Edict", 0, a("sacrifices a creature"), a("OPTIONAL", "Sacrifices ")));
        l.add(row("Sleight of Hand", 0, a("one of them into their hand"), a("zero of them")));
        l.add(new Row("Witherbloom Command", -1, "DBMill", a("mills three cards", "land card"), a("Land\\.YouOwn", "Land\\.You Own", "CARDNAME")));
        l.add(new Row("Witherbloom Command", -1, "DBLoseLife", a("loses 2 life", "gains 2 life"), a("gains 2 life\\. .*gains 2 life")));
        final Object[][] out = new Object[l.size()][];
        for (int i = 0; i < out.length; i++) { out[i] = new Object[] { l.get(i) }; }
        return out;
    }

    /** What no stack text should contain, whatever the card: raw script words, stray spaces and punctuation, a missing amount. */
    static final String[][] LINT = {
            { "CARDNAME|NICKNAME", "raw CARDNAME/NICKNAME" },
            { "[ \\t]{2,}|[ \\t]\\n|\\n[ \\t]", "double space" },
            { "\\s[.,;:!?]", "space before punctuation" },
            { "\\b[A-Z][A-Za-z]+\\.!?[A-Za-z]{3,}", "raw Valid string (Type.restriction)" },
            { "\\b(?:Sacrifice|Destroy|Exile) (?:permanents|things?)\\b", "unspecific noun" },
            { "\\bnull\\b", "null" },
            { "\\[[A-Za-z]+Effect", "effect error marker" },
            { "\\b[a-z]+[A-Z][a-z]+(?:Ctrl|Own|Cast)\\b", "raw script restriction" },
    };

    static String lint(final String text) {
        final StringBuilder sb = new StringBuilder();
        for (final String[] l : LINT) {
            if (Pattern.compile(l[0]).matcher(text).find()) { sb.append(" [").append(l[1]).append("]"); }
        }
        return sb.toString();
    }

    @Test(dataProvider = "rows")
    public void stackTextReadsLikeTheCard(final Row r) {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        // things for the spells to point at
        addCardToZone("Grizzly Bears", opp, ZoneType.Battlefield);
        addCardToZone("Darksteel Relic", opp, ZoneType.Battlefield);
        addCardToZone("Reanimate", opp, ZoneType.Graveyard);
        final boolean permanent = r.card.equals("The One Ring") || r.card.startsWith("Nahiri");
        final Card k = addCardToZone(r.card, ai, permanent ? ZoneType.Battlefield : ZoneType.Hand);

        final SpellAbility sa;
        if (r.ability >= 0) {
            sa = k.getSpellAbilities().get(r.ability);
        } else {
            sa = forge.game.ability.AbilityFactory.getAbility(k, r.mode);
        }
        sa.setActivatingPlayer(ai);
        if (sa.usesTargeting()) {
            if (sa.canTarget(opp)) {
                sa.getTargets().add(opp);
            } else {
                for (final Card c : opp.getCardsIn(java.util.Arrays.asList(ZoneType.Battlefield, ZoneType.Graveyard))) {
                    if (sa.canTarget(c)) { sa.getTargets().add(c); break; }
                }
            }
        }
        final String text = sa.getStackDescription();
        final StringBuilder problems = new StringBuilder();
        problems.append(lint(text));
        for (final String m : r.must) {
            if (!Pattern.compile(m).matcher(text).find()) { problems.append(" [missing /").append(m).append("/]"); }
        }
        for (final String n : r.mustNot) {
            if (Pattern.compile(n).matcher(text).find()) { problems.append(" [unwanted /").append(n).append("/]"); }
        }
        // the composed text, so the grammar can be read in the log
        System.out.println((problems.length() == 0 ? "OK     " : "BUG    ") + r + " -> \"" + text + "\"" + problems);
        assertTrue(r + ": \"" + text + "\"" + problems, problems.length() == 0);
    }
}
