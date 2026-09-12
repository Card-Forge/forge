package forge.game.spellability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Flashback and its siblings say "from your graveyard", so only the owner may use them.
 * A grant from another card is still what lets anyone else cast from there.
 */
public class CastFromOwnZoneTest extends AITest {

    @Test
    public void onlyTheOwnerMayFlashback() {
        Game game = initAndCreateGame();
        Player other = game.getPlayers().get(0);
        Player owner = game.getPlayers().get(1);

        Card geistflame = addCardToZone("Geistflame", owner, ZoneType.Graveyard);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, other);

        assertFalse("its owner may flash it back",
                geistflame.getAllPossibleAbilities(owner, true).isEmpty());
        assertTrue("the other player may not",
                geistflame.getAllPossibleAbilities(other, true).isEmpty());
        assertFalse("but a card that grants it - Arcane Heist, Wrexial - still can",
                AbilityUtils.getSpellsFromPlayEffect(geistflame, other,
                        geistflame.getCurrentStateName(), true, null).isEmpty());

        // Shaman's Trance is the grant that arrives as a keyword rather than a MayPlay, so it has
        // to be honoured here - the graveyard it names is still not the caster's own.
        addCards("Mountain", 8, other);
        resolveFromHand(game, "Shaman's Trance", other);
        assertFalse("Shaman's Trance still reaches another player's graveyard",
                geistflame.getAllPossibleAbilities(other, true).isEmpty());
    }

    /** Foretell is the other zone a spell can be restricted to, so the owner must keep it. */
    @Test
    public void onlyTheOwnerMayCastAForetoldCard() {
        Game game = initAndCreateGame();
        Player other = game.getPlayers().get(0);
        Player owner = game.getPlayers().get(1);

        Card saw = addCardToZone("Saw It Coming", owner, ZoneType.Exile);
        saw.setForetold(true);
        saw.turnFaceDown(true);
        addCards("Island", 8, owner);
        // foretell is barred on the turn the card was exiled, so let that turn end
        playUntilNextTurn(game);
        playUntilNextTurn(game);

        assertFalse("its owner may cast it out of exile",
                saw.getAllPossibleAbilities(owner, false).isEmpty());
        assertTrue("the other player may not",
                saw.getAllPossibleAbilities(other, false).isEmpty());
    }

    private void resolveFromHand(Game game, String name, Player p) {
        SpellAbility sa = addCardToZone(name, p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
    }
}
