package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

import java.util.List;

public class AdvanceCrankEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        if(tgtPlayers.isEmpty())
            return "";

        sb.append(Lang.joinHomogenous(tgtPlayers));
        sb.append(" advances their CRANK! counter to the next sprocket and cranks any number of that sprocket's contraptions");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        getDefinedPlayersOrTargeted(sa).forEach(Player::advanceCrankCounter);
    }
}
