package forge.player;

import com.google.common.collect.ImmutableMap;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.card.CardState;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.item.PaperCard;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each test loads a real card script from {@code res/cardsfolder} and checks the one behavior its
 * line used to get wrong: a parameter that nothing reads, or a trigger that fires for the wrong card.
 * <p>
 * The tests drive the code that reads the line (the trigger's {@code performTest}, the triggering
 * objects, {@code AbilityUtils.calculateAmount}, {@code CardFactory.getCloneStates}) rather than
 * resolving the whole ability, so no card database, stack or UI is needed.
 */
public class CardScriptFixesShould {

    // Without the card database no creature type is known, and CardType drops every subtype it
    // does not know, which would make a "copied Elf" check pass for the wrong reason.
    @BeforeClass
    public void knowTheCreatureTypes() {
        CardType.Constant.CREATURE_TYPES.addAll(List.of("Elf", "Warrior", "Human", "Mercenary", "Villain", "Zombie"));
    }

    @Test
    public void notFireToothClawAndTailForAnotherSchemeSetInMotion() throws IOException {
        assertSetInMotionOnlyForItself("t/tooth_claw_and_tail.txt");
    }

    @Test
    public void notFireYourWillIsNotYourOwnForAnotherSchemeSetInMotion() throws IOException {
        assertSetInMotionOnlyForItself("y/your_will_is_not_your_own.txt");
    }

    private static void assertSetInMotionOnlyForItself(final String file) throws IOException {
        // Without ValidCard$ Card.Self the trigger fires for any scheme set in motion while it is
        // face up in the command zone, not just for the scheme that has it.
        final HumanControlledGame g = new HumanControlledGame();
        final Card scheme = fromScript(g, g.human, file);
        final Card other = g.card(g.human, "Other Scheme", "Types:Scheme");

        final Trigger trigger = trigger(scheme, TriggerType.SetInMotion);

        assertThat(trigger.performTest(Map.of(AbilityKey.Scheme, scheme))).as("set in motion itself").isTrue();
        assertThat(trigger.performTest(Map.of(AbilityKey.Scheme, other))).as("another scheme set in motion").isFalse();
    }

    @Test
    public void readTheCounterCountOnMountKeraliaItself() throws IOException {
        // PlaneswalkedFrom sets only Cards as a triggering object, so TriggeredCard$ in the damage
        // amount was always 0 and the eruption dealt nothing.
        final HumanControlledGame g = new HumanControlledGame();
        final Card plane = fromScript(g, g.human, "m/mount_keralia.txt");
        plane.addCounterInternal(CounterEnumType.PRESSURE, 3, g.human, false, null, null);

        final SpellAbility eruption = AbilityFactory.getAbility(plane, "Eruption");
        trigger(plane, TriggerType.PlaneswalkedFrom).setTriggeringObjects(eruption, Map.of(AbilityKey.Cards, new CardCollection(plane)));

        assertThat(AbilityUtils.calculateAmount(plane, eruption.getParam("NumDmg"), eruption)).isEqualTo(3);
    }

    @Test
    public void redirectTheSpellThatCapturedByTheConsulateTriggeredOn() throws IOException {
        // Mode$ SpellCast sets the spell ability as TriggeredSpellAbility; TriggeredSourceSA is set
        // by Becomes-targeted triggers only, so ChangeTargets had no spell to redirect.
        final HumanControlledGame g = new HumanControlledGame();
        final Card aura = fromScript(g, g.human, "c/captured_by_the_consulate.txt");
        final Card caster = g.card(g.human, "Caster", "Types:Instant");
        final SpellAbility cast = AbilityFactory.getAbility("SP$ Pump | Cost$ 0 | Defined$ Self", caster);
        cast.setActivatingPlayer(g.human);

        final SpellAbility change = AbilityFactory.getAbility(aura, "TrigChangeTarget");
        trigger(aura, TriggerType.SpellCast).setTriggeringObjects(change, ImmutableMap.<AbilityKey, Object>builder()
                .put(AbilityKey.SpellAbility, cast)
                .put(AbilityKey.Activator, g.human)
                .put(AbilityKey.CurrentStormCount, 1)
                .put(AbilityKey.CurrentCastSpells, new CardCollection())
                .build());

        assertThat(AbilityUtils.getDefinedSpellAbilities(aura, change.getParam("Defined"), change)).containsExactly(cast);
    }

    @Test
    public void giveOlagBothBlueAndBlack() throws IOException {
        // CardFactory splits AddColors$ on commas; "Blue & Black" named no color at all.
        final HumanControlledGame g = new HumanControlledGame();
        // The file is a two-faced card; take its real Clone line onto a plain host.
        final Card ludevic = g.card(g.human, "Ludevic", "Types:Creature Human",
                svarLine("l/ludevic_necrogenius_olag_ludevics_hubris.txt", "Copy"));
        final Card exiled = g.card(g.human, "Exiled", "Types:Creature Elf", "PT:2/2");

        final CardState olag = clonedState(exiled, ludevic, "Copy");

        assertThat(olag.getColor().hasAnyColor(MagicColor.BLUE)).as("blue").isTrue();
        assertThat(olag.getColor().hasAnyColor(MagicColor.BLACK)).as("black").isTrue();
    }

    @Test
    public void dropTheCopiedCreatureTypesForTaskmaster() throws IOException {
        // Taskmaster's line asked for RemoveCreatureTypes$, which Clone never reads; the param
        // that removes the copied subtypes is RemoveSubTypes$ (next to RemoveCardTypes$).
        final HumanControlledGame g = new HumanControlledGame();
        final Card taskmaster = fromScript(g, g.human, "t/taskmaster_mercenary_mimic.txt");
        final Card elf = g.card(g.human, "Elf", "Types:Creature Elf Warrior", "PT:2/2");

        final CardState copy = clonedState(elf, taskmaster, "TrigClone");

        assertThat(copy.getType().hasSubtype("Elf")).as("copied Elf").isFalse();
        assertThat(copy.getType().hasSubtype("Warrior")).as("copied Warrior").isFalse();
        assertThat(copy.getType().hasSubtype("Mercenary")).as("added Mercenary, types " + copy.getType()).isTrue();
    }

    private static CardState clonedState(final Card toCopy, final Card host, final String cloneSVar) {
        final SpellAbility clone = AbilityFactory.getAbility(host, cloneSVar);
        return CardFactory.getCloneStates(toCopy, host, clone).get(CardStateName.Original);
    }

    private static String svarLine(final String file, final String svar) throws IOException {
        return Files.readAllLines(Path.of("res", "cardsfolder", file)).stream()
                .filter(l -> l.startsWith("SVar:" + svar + ":")).findFirst().orElseThrow();
    }

    private static Trigger trigger(final Card card, final TriggerType mode) {
        return card.getTriggers().stream().filter(t -> t.getMode() == mode).findFirst().orElseThrow();
    }

    /** Builds a card from its real script file. Tests run with forge-gui as the working directory. */
    private static Card fromScript(final HumanControlledGame g, final Player owner, final String file) throws IOException {
        final List<String> script = Files.readAllLines(Path.of("res", "cardsfolder", file));
        final PaperCard paperCard = new PaperCard(CardRules.fromScript(script), "", CardRarity.Common);
        return CardFactory.getCard(paperCard, owner, g.game);
    }
}
