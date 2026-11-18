package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Random;

public class AiConvokeTest extends AITest {

    private SpellAbility inHand(String name, Player ai) {
        SpellAbility sa = addCardToZone(name, ai, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    private boolean aiCasts(Player ai, SpellAbility sa) {
        MyRandom.setRandom(new Random(0));
        List<SpellAbility> plays = ((PlayerControllerAi) ai.getController()).getAi().chooseSpellAbilityToPlay();
        return plays != null && plays.stream().anyMatch(p -> p.getHostCard() == sa.getHostCard());
    }

    @Test
    public void scanSeesConvokeOnlySpellInMainOne() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Raging Goblin", 4, ai);
        SpellAbility stoke = inHand("Stoke the Flames", ai); // {2}{R}{R}

        AssertJUnit.assertTrue(AvailableActions.collectActionable(ai, 5000).contains(stoke.getHostCard().getView()));
    }

    @Test
    public void landsPayBeforeCreatures() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Mountain", 4, ai);
        addCards("Hill Giant", 4, ai);
        SpellAbility stoke = inHand("Stoke the Flames", ai);

        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(stoke, ai, 0, false));
        AssertJUnit.assertTrue(stoke.getTappedForConvoke().isEmpty());
    }

    @Test
    public void aiConvokesWithCreaturesThatCannotAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Savannah Lions", 3, ai); // summoning sick
        SpellAbility soul = inHand("Martyr's Soul", ai); // {2}{W}, PlayMain1

        AssertJUnit.assertTrue(aiCasts(ai, soul));
    }

    @Test
    public void aiKeepsElvesForHeritageDruidBeforeConvoking() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card forest = addCard("Forest", ai);
        Card heritage = addCard("Heritage Druid", ai);
        Card visionary = addCard("Elvish Visionary", ai);
        Card nettle = addCard("Nettle Sentinel", ai);
        SpellAbility chord = inHand("Chord of Calling", ai);
        chord.setXManaCostPaid(1);

        // Heritage Druid plus the two supporting Elves make {G}{G}{G}; the Forest pays {1}.
        // Convoking either supporting Elf first would make Heritage Druid's tap-three-Elves
        // ability unavailable and incorrectly make this otherwise payable Chord fail.
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, chord, null));
        AssertJUnit.assertFalse(game.getStack().isEmpty());
        AssertJUnit.assertNotNull(game.getStack().getSpellMatchingHost(chord.getHostCard()));
        AssertJUnit.assertTrue(chord.getTappedForConvoke().isEmpty());
        AssertJUnit.assertTrue(forest.isTapped());
        AssertJUnit.assertTrue(heritage.isTapped());
        AssertJUnit.assertTrue(visionary.isTapped());
        AssertJUnit.assertTrue(nettle.isTapped());
    }

    @Test
    public void aiRatherAttacksThanConvokesWithItsAttackers() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (Card c : addCards("Savannah Lions", 3, ai)) {
            c.setSickness(false);
        }
        SpellAbility soul = inHand("Martyr's Soul", ai);
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(soul, ai, 0, false));

        AssertJUnit.assertFalse(aiCasts(ai, soul));
    }

    @Test
    public void aiKeepsNextTurnsBlockersInMainOne() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Grizzly Bears", game.getPlayers().get(0));
        addCards("Hill Giant", 2, ai); // summoning sick, one of them blocks the bears
        addCard("Savannah Lions", ai);
        SpellAbility soul = inHand("Martyr's Soul", ai);
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(soul, ai, 0, false));

        AssertJUnit.assertFalse(aiCasts(ai, soul));
    }
}
