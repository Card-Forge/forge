package forge.game.ability.effects;

import com.google.common.collect.Lists;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.List;

public class ChooseEntityEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return (sa.hasParam("StackDescription") ? sa.getParam("StackDescription") :
                sa.getParamOrDefault("SpellDescription", "Write a Stack/SpellDescription!"));
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        List<GameEntity> choices = Lists.newArrayList();
        String cardsDef = sa.getParam("CardChoices");
        String playersDef = sa.getParam("PlayerChoices");
        CardCollection cards = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), cardsDef, activator,
                host, sa);
        choices.addAll(cards);
        PlayerCollection players = AbilityUtils.getDefinedPlayers(host, playersDef, sa);
        choices.addAll(players);

        Object chosen = null;
        if (sa.hasParam("Random")) { // currently we only choose at random for this
            chosen = Aggregates.random(choices);
        }
        if (chosen == null) {
            System.err.println("Error: ChooseEntityEffect.java unable to choose an entity");
            return;
        }

        if (sa.hasParam("RememberChosen")) {
            host.addRemembered(chosen);
        }
    }
}
