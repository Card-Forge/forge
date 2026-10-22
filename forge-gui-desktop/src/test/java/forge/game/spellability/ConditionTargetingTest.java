package forge.game.spellability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.player.Player;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/** ConditionTargetValidTargeting$ and ConditionTargetsSingleTarget$ inspect the spell a ChangeTargets ability targets. */
public class ConditionTargetingTest extends AITest {

    private static SpellAbility changeTargets(Card host, String params) {
        SpellAbility sa = AbilityFactory.getAbility(
                "AB$ ChangeTargets | Cost$ 0 | TargetType$ Spell | ValidTgts$ Card | " + params, host);
        sa.setActivatingPlayer(host.getController());
        return sa;
    }

    /** Walking the targeted spell's chain must end when no target is valid, whatever the checking ability chains to. */
    @Test(timeOut = 5000)
    public void aValidTargetingCheckEndsWhenNoTargetIsValid() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        host.setSVar("DBNext", "DB$ Pump | KW$ Flying");
        SpellAbility sa = changeTargets(host,
                "ConditionTargetValidTargeting$ Creature.inZoneBattlefield | SubAbility$ DBNext");
        SpellAbility spell = AbilityFactory.getAbility("DB$ Pump | ValidTgts$ Player | KW$ Flying", host);
        spell.getTargets().add(p);
        sa.getTargets().add(spell);

        assertFalse("the spell targets a player, not a creature", sa.getConditions().areMet(sa));
    }

    /** A spell with two targets does not have "only one target", however many targets the checking ability has. */
    @Test
    public void aSpellWithTwoTargetsIsNotASingleTargetSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        Card other = addCard("Runeclaw Bear", p);
        SpellAbility sa = changeTargets(host, "ConditionTargetsSingleTarget$ True");
        SpellAbility spell = AbilityFactory.getAbility("DB$ Pump | ValidTgts$ Creature | KW$ Flying", host);
        spell.getTargets().add(host);
        spell.getTargets().add(other);
        sa.getTargets().add(spell);

        assertFalse("the spell has two targets", sa.getConditions().areMet(sa));
    }

    /** Control: a spell with exactly one target passes. */
    @Test
    public void aSpellWithOneTargetIsASingleTargetSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        SpellAbility sa = changeTargets(host, "ConditionTargetsSingleTarget$ True");
        SpellAbility spell = AbilityFactory.getAbility("DB$ Pump | ValidTgts$ Creature | KW$ Flying", host);
        spell.getTargets().add(host);
        sa.getTargets().add(spell);

        assertTrue("the spell has one target", sa.getConditions().areMet(sa));
    }
}
