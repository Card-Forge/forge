package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class StorageLandAiTest extends AITest {
    private static final CounterType STORAGE = CounterType.getType("STORAGE");

    @Test
    public void fallenEmpiresStorageLandsAreAvailableToAiDecks() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final String[] storageLands = {
                "Bottomless Vault", "Dwarven Hold", "Hollow Trees", "Icatian Store", "Sand Silos"
        };

        for (String cardName : storageLands) {
            final Card land = addCard(cardName, ai);

            AssertJUnit.assertFalse(cardName, land.getRules().getAiHints().getRemAIDecks());
            AssertJUnit.assertEquals(cardName, "StorageLand", land.getSVar("AIUntapPreference"));
            AssertJUnit.assertEquals(cardName, "True", land.getManaAbilities().getFirst()
                    .getParam("AINoRecursiveCheck"));
        }
    }

    @Test
    public void storageLandUntapsWhenStoredManaEnablesAHandCard() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card silos = addCard("Sand Silos", ai);
        silos.setTapped(true);
        addCard("Island", ai);
        addCard("Island", ai);
        addCardToZone("Air Elemental", ai, ZoneType.Hand);

        silos.setCounters(STORAGE, 2);
        AssertJUnit.assertFalse(chooseToUntap(ai, silos));

        silos.setCounters(STORAGE, 3);
        AssertJUnit.assertTrue(chooseToUntap(ai, silos));
    }

    private static boolean chooseToUntap(Player ai, Card land) {
        return ((PlayerControllerAi) ai.getController()).chooseBinary(
                new SpellAbility.EmptySa(land, ai), "", BinaryChoiceType.UntapOrLeaveTapped, true);
    }
}
