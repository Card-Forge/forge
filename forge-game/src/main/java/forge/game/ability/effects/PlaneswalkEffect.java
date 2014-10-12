package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;


public class PlaneswalkEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        Game game = sa.getActivatingPlayer().getGame();

        for(Player p : game.getPlayers()) {
            p.leaveCurrentPlane();
        }
        if (sa.hasParam("Defined")) {
            CardCollectionView destinations = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            sa.getActivatingPlayer().planeswalkTo(destinations);
        }
        else {
            sa.getActivatingPlayer().planeswalk();
        }
    }
}
