package forge.game.ability;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertEquals;

/** A CopySpell replacement sees the number of copies of one spell, however many spells one effect copies. */
public class CopySpellAmountTest extends AITest {

    private static SpellAbility putOnStack(Game game, Player p, Card host) {
        SpellAbility ability = AbilityFactory.getAbility(
                "AB$ DealDamage | Cost$ 0 | ValidTgts$ Any | NumDmg$ 1", host);
        ability.setActivatingPlayer(p);
        ability.getTargets().add(p);
        game.getStack().add(ability);
        return game.getStack().peekAbility();
    }

    /** Doubling each spell's copy count turns one copy of each of two spells into four copies, not into six. */
    @Test
    public void theReplacementIsAppliedToEachSpellOnItsOwn() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        host.setSVar("Double", "DB$ ReplaceEffect | VarName$ Amount | VarValue$ X");
        host.setSVar("X", "ReplaceCount$Amount/Twice");
        ReplacementEffect re = ReplacementHandler.parseReplacement(
                "Event$ CopySpell | ActiveZones$ Battlefield | ValidPlayer$ You | ValidSpell$ Activated"
                + " | ReplaceWith$ Double | Description$ Doubles the copies.", host, true);
        host.addChangedCardTraits(null, null, Lists.newArrayList(re), null, null, game.getNextTimestamp(), 0);

        SpellAbility first = putOnStack(game, p, host);
        SpellAbility second = putOnStack(game, p, host);
        int before = game.getStack().size();

        SpellAbility sa = AbilityFactory.getAbility(
                "DB$ CopySpellAbility | Controller$ You | ValidTgts$ Card | TargetType$ Activated", host);
        sa.setActivatingPlayer(p);
        sa.getTargets().add(first);
        sa.getTargets().add(second);
        AbilityUtils.resolve(sa);

        assertEquals("each of the two spells is copied twice", before + 4, game.getStack().size());
    }
}
