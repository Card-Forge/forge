package forge.game.spellability;

import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.util.Localizer;
import forge.util.Lang;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Collections;

public class StackItemViewTest {
    @BeforeClass
    public void initializeLocalizer() {
        Localizer.getInstance().initialize(
                "en-US",
                Path.of("..", "forge-gui", "res", "languages").toAbsolutePath().toString());
        Lang.createInstance("en-US");
    }

    @Test
    public void constructorInitializesViewWhileTrackerIsFrozen() {
        GameRules rules = new GameRules(GameType.Constructed);
        Game game = new Game(Collections.emptyList(), rules, new Match(rules, Collections.emptyList(), "Test"));
        Card card = new Card(1, game);
        Player player = new Player("Test player", game, 1);
        SpellAbility ability = new AbilityActivated(card, Cost.Zero, null) {
            @Override
            public void resolve() {
            }
        };
        ability.setStackDescription("Test ability");
        ability.setActivatingPlayer(player);

        game.getTracker().freeze();
        SpellAbilityStackInstance instance = new SpellAbilityStackInstance(ability);
        game.getTracker().clearDelayed();
        game.getTracker().unfreeze();

        Assert.assertSame(instance.getView().getSourceCard(), card.getView());
        Assert.assertSame(instance.getView().getActivatingPlayer(), player.getView());
        Assert.assertTrue(instance.getView().isAbility());
        Assert.assertEquals(instance.getView().getText(), "Test ability");
        Assert.assertSame(instance.getView().getTracker(), game.getTracker());

        Player newPlayer = new Player("New player", game, 2);
        game.getTracker().freeze();
        instance.setActivatingPlayer(newPlayer);

        Assert.assertSame(instance.getView().getActivatingPlayer(), player.getView());

        game.getTracker().unfreeze();

        Assert.assertSame(instance.getView().getActivatingPlayer(), newPlayer.getView());
    }
}
