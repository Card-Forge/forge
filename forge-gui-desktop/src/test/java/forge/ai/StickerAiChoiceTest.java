package forge.ai;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.sticker.Sticker;
import forge.game.card.sticker.StickerKind;
import forge.game.card.sticker.StickerSheet;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * The choices the AI makes about stickers: what it puts one on, which one it puts there, and
 * whether it takes an effect that sets power and toughness from the stickers in play. The first
 * two are scored together, so the tests here drive whole abilities rather than either choice on
 * its own.
 */
public class StickerAiChoiceTest extends AITest {

    private Card giveSheet(Player p, String sheetName) {
        PaperCard pc = StaticData.instance().getVariantCards().getCard(sheetName);
        assertNotNull(pc, "no such sticker sheet: " + sheetName);
        Card sheet = Card.fromPaperCard(pc, p);
        p.getZone(ZoneType.StickerSheets).add(sheet);
        return sheet;
    }

    /** Resolves a bare PutSticker for the given player, the way a card's own would resolve. */
    private void putSticker(Player p, Card host, String params) {
        SpellAbility put = AbilityFactory.getAbility("DB$ PutSticker | " + params, host);
        put.setActivatingPlayer(p);
        AbilityUtils.resolve(put);
    }

    private Sticker onlySticker(Card c) {
        assertEquals(c.getStickers().size(), 1);
        return c.getStickers().get(0).getSticker();
    }

    /**
     * A power and toughness sticker sets base power and toughness, so the creature to put one on
     * is the one it does the most for - not the best creature on the board.
     */
    @Test
    public void testAiStickersTheCreatureTheStickerDoesMostFor() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(0);
        giveSheet(ai, "Eldrazi Guacamole Tightrope"); // its P/T stickers are 1/4 and 5/3
        ai.setCounters(CounterEnumType.TICKET, 3, ai, false);
        Card rock = addCard("Sol Ring", ai);
        Card bear = addCard("Grizzly Bears", ai); // 2/2
        Card dreadmaw = addCard("Colossal Dreadmaw", ai); // 6/6
        game.getAction().checkStateEffects(true);

        putSticker(ai, rock, "Choices$ Permanent.nonLand+YouOwn | Optional$ True");

        assertTrue(bear.isStickered(), "5/3 is a big gain on a 2/2 and a downgrade on a 6/6");
        assertFalse(dreadmaw.isStickered());
        assertFalse(rock.isStickered());
        Sticker placed = onlySticker(bear);
        assertEquals(placed.getKind(), StickerKind.PT);
        assertEquals(placed.getPower(), 5);
        assertEquals(ai.getCounters(CounterEnumType.TICKET), 0, "three tickets, all three spent");
    }

    /** The only sticker it could afford would shrink the only creature it owns, so it declines. */
    @Test
    public void testAiDeclinesRatherThanShrinkItsCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(0);
        giveSheet(ai, "Eldrazi Guacamole Tightrope");
        ai.setCounters(CounterEnumType.TICKET, 2, ai, false); // enough for the 1/4, not the 5/3
        Card dreadmaw = addCard("Colossal Dreadmaw", ai);
        game.getAction().checkStateEffects(true);

        putSticker(ai, dreadmaw, "Kind$ PT | Choices$ Creature.YouOwn | Optional$ True");

        assertFalse(dreadmaw.isStickered(), "1/4 on a 6/6 is not worth two tickets");
        assertEquals(ai.getCounters(CounterEnumType.TICKET), 2, "and the tickets are still there");
    }

    /** Runs the turn player's begin-combat triggers. */
    private void toBeginCombat(Game game) {
        playUntilPhase(game, PhaseType.COMBAT_BEGIN);
        playUntilStackClear(game);
    }

    /**
     * Ambassador Blorpityblorpboop's base power and toughness become the total on the stickers you
     * control, which sets them rather than adding to them - with no stickers out that is 0/0, and
     * the Ambassador dies. It is a "may", so the AI should pass.
     */
    @Test
    public void testAiKeepsAPowerAndToughnessBiggerThanItsStickers() {
        Game game = initAndCreateGame();
        Player ai = game.getPhaseHandler().getPlayerTurn();
        Card amb = addCard("Ambassador Blorpityblorpboop", ai);
        game.getAction().checkStateEffects(true);
        assertEquals(amb.getNetPower(), 3);

        toBeginCombat(game);
        game.getAction().checkStateEffects(true);

        assertTrue(amb.isInPlay(), "0/0 would have died");
        assertEquals(amb.getNetPower(), 3, "it owns no stickers, so there is nothing to become");
        assertEquals(amb.getNetToughness(), 3);
    }

    /**
     * Finishing Move is removal with a sticker attached, and the sticker is the part that may find
     * nothing to do. The AI's answer for the sticker becomes the answer for the whole spell, so a
     * player with no sheets at all must still be willing to cast it for the damage.
     */
    @Test
    public void testAiCastsASpellWhoseStickerHasNowhereToGo() {
        Game game = initAndCreateGame();
        Player ai = game.getPhaseHandler().getPlayerTurn();
        Player opp = game.getPlayers().stream().filter(p -> p != ai).findFirst().orElseThrow();
        addCard("Colossal Dreadmaw", ai);
        addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);

        Card move = addCardToZone("Finishing Move", ai, ZoneType.Hand);
        SpellAbility spell = move.getFirstSpellAbility();
        spell.setActivatingPlayer(ai);
        assertTrue(StickerSheet.getAvailableStickers(ai).isEmpty(), "no sheets, so no stickers");
        assertTrue(SpellApiToAi.Converter.get(spell).canPlayWithSubs(ai, spell).willingToPlay(),
                "6 damage is worth casting whether or not a sticker comes with it");
    }
}
