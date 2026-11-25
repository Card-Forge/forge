package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Ursine Champion: "gets +3/+3 and becomes a Bear Berserker until end of turn."
 */
public class UrsineChampionTest extends AITest {

    @Test
    public void becomesABearBerserker() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card champion = addCard("Ursine Champion", p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = champion.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get();
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(champion.getType().hasCreatureType("Bear"));
        AssertJUnit.assertTrue(champion.getType().hasCreatureType("Berserker"));
        AssertJUnit.assertFalse(champion.getType().hasCreatureType("Human"));
        AssertJUnit.assertEquals(5, champion.getNetPower());
    }
}
