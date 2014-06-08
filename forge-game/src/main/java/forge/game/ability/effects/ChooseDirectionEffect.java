package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Direction;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;

public class ChooseDirectionEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final List<Player> left = new ArrayList<Player>(game.getPlayers());
        // TODO: We'd better set up turn order UI here
        final String info = "Left (clockwise): " + left + "\r\nRight (anticlockwise):" + Lists.reverse(left);
        sa.getActivatingPlayer().getController().notifyOfValue(sa, source, info);

        boolean chosen = sa.getActivatingPlayer().getController().chooseBinary(sa,
                "Choose a direction", BinaryChoiceType.LeftOrRight);
        source.setChosenDirection(chosen ? Direction.Left : Direction.Right);
    }

}
