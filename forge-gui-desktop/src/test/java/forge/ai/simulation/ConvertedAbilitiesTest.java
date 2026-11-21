package forge.ai.simulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.testng.AssertJUnit;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.ai.SpellApiToAi;
import forge.ai.ability.NothingAi;
import forge.ai.ability.TargetOnlyAi;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.localinstance.properties.ForgeConstants;

/**
 * Every ability the scripts spell {@code TargetOnly}, {@code Bookkeeping}, {@code Nothing} or {@code EndOfTurn} (they
 * were Pumps): each one builds, is the effect it names, has the AI that decides it, and has the sub-abilities it
 * names. Asking the AI about each one raises no exception.
 * <p>
 * Off by default (it loads about 400 cards): run it with {@code mvn test -Dforge.test.converted=true
 * -Dtest=ConvertedAbilitiesTest}, for example after a change to the scripts or to TargetOnlyAi / NothingAi.
 */
public class ConvertedAbilitiesTest extends FlatMcTestBase {
    private static final Pattern STEP = Pattern.compile("^(?:AB|SP|DB)\\$ (TargetOnly|Bookkeeping|Nothing|EndOfTurn)\\b.*");
    private static final Pattern ANY_STEP = Pattern.compile("(?:AB|SP|DB)\\$ (?:TargetOnly|Bookkeeping|Nothing|EndOfTurn)\\b");

    /** What a Bookkeeping step may carry besides the parameters every ability may carry. */
    private static final Set<String> BOOKKEEPING = new HashSet<>(Arrays.asList("RememberObjects", "ForgetObjects",
            "ImprintCards", "ForgetImprinted", "NoteCards", "NoteCardsFor", "ClearNotedCardsFor", "NoteNumber",
            "RememberAll", "RememberZone"));
    private static final Set<String> LEFTOVER_FROM_PUMP = new HashSet<>(Arrays.asList("PumpZone", "RememberPumped"));

    @BeforeMethod
    public void setUp() {
        if (!Boolean.getBoolean("forge.test.converted")) {
            throw new SkipException(
                    "Enable with -Dforge.test.converted=true");
        }
        setUpGame();
    }

    private static Set<String> convertedCardNames() {
        final Set<String> names = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(Path.of(ForgeConstants.CARD_DATA_DIR))) {
            paths.filter(p -> p.toString().endsWith(".txt")).forEach(p -> {
                try {
                    final List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                    boolean has = false;
                    for (final String l : lines) {
                        has |= ANY_STEP.matcher(l).find();
                    }
                    if (has && !lines.isEmpty() && lines.get(0).startsWith("Name:")) {
                        names.add(lines.get(0).substring(5).trim());
                    }
                } catch (final IOException e) {
                    throw new AssertionError("Could not read " + p, e);
                }
            });
        } catch (final IOException e) {
            throw new AssertionError("Could not read " + ForgeConstants.CARD_DATA_DIR, e);
        }
        return names;
    }

    private static void check(final Card card, final String where, final SpellAbility sa, final List<String> bad) {
        final String api = sa.getApi() == null ? null : sa.getApi().name();
        final String logic = where + " (" + api + ")";
        final Class<?> ai = SpellApiToAi.Converter.get(sa) == null ? null : SpellApiToAi.Converter.get(sa).getClass();
        if (sa.getApi() == ApiType.TargetOnly) {
            if (ai != TargetOnlyAi.class) {
                bad.add(logic + ": AI is " + ai);
            }
        } else if (sa.getApi() == ApiType.Nothing || sa.getApi() == ApiType.Bookkeeping
                || sa.getApi() == ApiType.EndOfTurn) {
            if (ai != NothingAi.class) {
                bad.add(logic + ": AI is " + ai);
            }
        }
        for (final String leftover : LEFTOVER_FROM_PUMP) {
            if (sa.hasParam(leftover)) {
                bad.add(logic + ": still has " + leftover);
            }
        }
        if (sa.getApi() == ApiType.EndOfTurn && !sa.hasParam("AtEOT")) {
            bad.add(logic + ": no AtEOT");
        }
        if (sa.hasParam("SubAbility") && sa.getSubAbility() == null) {
            bad.add(logic + ": SubAbility$ " + sa.getParam("SubAbility") + " is not built");
        }
        if (sa.getApi() == ApiType.Bookkeeping && !sa.getMapParams().keySet().stream().anyMatch(BOOKKEEPING::contains)) {
            bad.add(logic + ": Bookkeeping with nothing to keep");
        }
    }

    @Test
    public void everyConvertedAbilityBuildsAndHasItsAi() {
        final Set<String> names = convertedCardNames();
        AssertJUnit.assertTrue("cards with a converted ability: " + names.size(), names.size() > 300);
        final List<String> bad = new ArrayList<>();
        int checked = 0;
        int missing = 0;
        for (final String name : names) {
            final Card card;
            try {
                card = put(name, ai, ZoneType.Hand);
            } catch (final RuntimeException e) {
                missing++;
                continue;
            }
            for (final Map.Entry<String, String> e : card.getSVars().entrySet()) {
                if (!STEP.matcher(e.getValue()).matches()) {
                    continue;
                }
                try {
                    final SpellAbility sa = AbilityFactory.getAbility(card, e.getKey());
                    check(card, name + " / " + e.getKey(), sa, bad);
                    checked++;
                } catch (final RuntimeException ex) {
                    bad.add(name + " / " + e.getKey() + ": " + ex);
                }
            }
            for (final SpellAbility sa : card.getSpellAbilities()) {
                if (sa.getApi() == ApiType.TargetOnly || sa.getApi() == ApiType.Nothing
                        || sa.getApi() == ApiType.Bookkeeping || sa.getApi() == ApiType.EndOfTurn) {
                    check(card, name + " / " + sa.getDescription(), sa, bad);
                    checked++;
                }
            }
        }
        AssertJUnit.assertTrue("cards not in the test database: " + missing + " of " + names.size(),
                missing * 10 < names.size());
        AssertJUnit.assertTrue("abilities checked: " + checked, checked > 400);
        if (!bad.isEmpty()) {
            AssertJUnit.fail(bad.size() + " converted abilities are wrong:\n"
                    + String.join("\n", bad.subList(0, Math.min(25, bad.size()))));
        }
    }

    @Test
    public void askingTheAiAboutAnyConvertedAbilityRaisesNothing() {
        at(PhaseType.MAIN1, ai);
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        put("Grizzly Bears", opp, ZoneType.Battlefield);
        final List<String> bad = new ArrayList<>();
        for (final String name : convertedCardNames()) {
            final Card card;
            try {
                card = put(name, ai, ZoneType.Hand);
            } catch (final RuntimeException e) {
                continue;
            }
            final List<SpellAbility> all = new ArrayList<>();
            for (final Map.Entry<String, String> e : card.getSVars().entrySet()) {
                if (STEP.matcher(e.getValue()).matches()) {
                    try {
                        all.add(AbilityFactory.getAbility(card, e.getKey()));
                    } catch (final RuntimeException ex) {
                        // reported by the other test
                    }
                }
            }
            for (final SpellAbility sa : card.getSpellAbilities()) {
                if (sa.getApi() == ApiType.TargetOnly || sa.getApi() == ApiType.Nothing
                        || sa.getApi() == ApiType.Bookkeeping || sa.getApi() == ApiType.EndOfTurn) {
                    all.add(sa);
                }
            }
            for (final SpellAbility sa : all) {
                sa.setActivatingPlayer(ai);
                try {
                    SpellApiToAi.Converter.get(sa).chkDrawback(ai, sa);
                    SpellApiToAi.Converter.get(sa).doTrigger(ai, sa, true);
                    sa.resetTargets();
                } catch (final RuntimeException ex) {
                    bad.add(name + " / " + sa.getApi() + ": " + ex);
                }
            }
        }
        if (!bad.isEmpty()) {
            AssertJUnit.fail(bad.size() + " abilities raised an exception in the AI:\n"
                    + String.join("\n", bad.subList(0, Math.min(25, bad.size()))));
        }
    }
}
