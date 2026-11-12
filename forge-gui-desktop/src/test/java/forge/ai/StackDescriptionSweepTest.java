package forge.ai;

import forge.StaticData;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import org.testng.annotations.Test;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Every spell and ability of every card, as the stack would show it, checked with the same lint as
 * {@link StackDescriptionTest} (#5634). It takes a few minutes, so it only runs on request:
 * {@code mvn test -Dtest=StackDescriptionSweepTest -Dforge.sweep=true}. The findings go to the log and to
 * {@code target/stack-description-sweep.txt}, one line per ability: card, ability, the text and what is wrong with it.
 * Nothing fails: the list is the work to do.
 */
public class StackDescriptionSweepTest extends AITest {
    private static final int BATCH = 400;

    @Test
    public void sweep() throws Exception {
        if (!Boolean.getBoolean("forge.sweep")) {
            System.out.println("StackDescriptionSweepTest skipped (-Dforge.sweep=true to run it)");
            return;
        }
        final List<String> names = new ArrayList<>();
        for (final PaperCard pc : StaticData.instance().getCommonCards().getUniqueCards()) {
            names.add(pc.getName());
        }
        final List<String> findings = new ArrayList<>();
        final List<String> skipped = new ArrayList<>();
        int abilities = 0;
        int built = 0;
        Game game = null;
        Player ai = null;
        Player opp = null;
        for (int i = 0; i < names.size(); i++) {
            if (i % BATCH == 0) {
                game = initAndCreateGame();
                opp = game.getPlayers().get(0);
                ai = game.getPlayers().get(1);
                addCardToZone("Grizzly Bears", opp, ZoneType.Battlefield);
                addCardToZone("Darksteel Relic", opp, ZoneType.Battlefield);
                addCardToZone("Reanimate", opp, ZoneType.Graveyard);
            }
            final Card card;
            try {
                card = addCardToZone(names.get(i), ai, ZoneType.Hand);
            } catch (final RuntimeException e) {
                // a card the test game cannot build (conspiracy, vanguard, ...): listed, not silently dropped
                skipped.add(names.get(i) + " | " + e);
                continue;
            }
            built++;
            for (final SpellAbility sa : card.getAllSpellAbilities()) {
                abilities++;
                try {
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
                    final String problems = StackDescriptionTest.lint(text);
                    if (!problems.isEmpty()) {
                        findings.add(card.getName() + " | " + sa.getApi() + " | \"" + text + "\"" + problems);
                    }
                } catch (final RuntimeException e) {
                    findings.add(card.getName() + " | " + sa.getApi() + " | threw " + e);
                }
            }
        }
        System.out.println("StackDescriptionSweepTest: " + built + " of " + names.size() + " unique cards built, " + skipped.size()
                + " skipped, " + abilities + " abilities, " + findings.size() + " with findings");
        for (final String f : findings) {
            System.out.println("SWEEP  " + f);
        }
        try (PrintWriter w = new PrintWriter("target/stack-description-sweep.txt", "UTF-8")) {
            findings.forEach(w::println);
        }
        try (PrintWriter w = new PrintWriter("target/stack-description-sweep-skipped.txt", "UTF-8")) {
            skipped.forEach(w::println);
        }
    }
}
