package forge.game.mana;

import forge.ai.simulation.SimulationTest;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class ManaRefundServiceTest extends SimulationTest {

    /**
     * Tests that mana is refunded to the player stored in the Mana object.
     */
    @Test
    public void testManaRefundsToManaPlayer() {
        Game game = initAndCreateGame();
        Player caster = game.getPlayers().get(1);
        Player manaOwner = game.getPlayers().get(0);

        Card land = addCard("Island", manaOwner);
        Mana mana = new Mana((byte) ManaAtom.BLUE, land, null, manaOwner);

        Card spell = addCardToZone("Bear Cub", caster, ZoneType.Hand);
        SpellAbility castSpell = spell.getFirstSpellAbility();
        castSpell.setActivatingPlayer(caster);
        castSpell.getPayingMana().add(mana);

        new ManaRefundService(castSpell).refundManaPaid();

        AssertJUnit.assertEquals(1, manaOwner.getManaPool().totalMana());
        AssertJUnit.assertEquals(0, caster.getManaPool().totalMana());
    }
}
