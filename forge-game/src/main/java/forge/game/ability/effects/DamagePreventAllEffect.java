package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class DamagePreventAllEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = sa.getActivatingPlayer().getGame();
        final int numDam = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa);

        String players = "";

        if (sa.hasParam("ValidPlayers")) {
            players = sa.getParam("ValidPlayers");
        }

        if (sa.hasParam("ValidCards")) {
            CardCollectionView list = game.getCardsIn(ZoneType.Battlefield);
            list = AbilityUtils.filterListByType(list, sa.getParam("ValidCards"), sa);
            for (final Card c : list) {
                c.addPreventNextDamage(numDam);
            }
        }

        if (!players.equals("")) {
            for (final Player p : game.getPlayers()) {
                if (p.isValid(players, source.getController(), source, sa)) {
                    p.addPreventNextDamage(numDam);
                }
            }
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        String desc = sa.getDescription();

        if (desc.contains(":")) {
            desc = desc.split(":")[1];
        }
        sb.append(desc);

        return sb.toString();
    }
}
