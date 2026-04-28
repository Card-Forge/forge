package forge.ai.ability;

import com.google.common.collect.Lists;
import forge.ai.AITest;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class ChoosePlayerAiTest extends AITest {
    @Test
    public void testCurseChoiceUsesPreferredOpponentFromAllPlayers() {
        Game game = initAndCreateThreePlayerGame();
        Player firstOpponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player preferredOpponent = game.getPlayers().get(2);
        firstOpponent.setTeam(0);
        ai.setTeam(1);
        preferredOpponent.setTeam(2);

        firstOpponent.setLife(20, null);
        preferredOpponent.setLife(5, null);

        Card source = addCard("Mountain", ai);
        SpellAbility choosePlayerSa = new SpellAbility.EmptySa(ApiType.ChoosePlayer, source, ai);
        choosePlayerSa.putParam("AILogic", "Curse");

        SpellAbilityAi choosePlayerAi = SpellApiToAi.Converter.get(ApiType.ChoosePlayer);
        Player chosen = choosePlayerAi.chooseSingleEntity(ai, choosePlayerSa,
                Lists.newArrayList(firstOpponent, ai, preferredOpponent), false, null, null);

        AssertJUnit.assertEquals("AI should not blindly choose the first opponent in turn order",
                preferredOpponent, chosen);
    }
}
