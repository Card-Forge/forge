package forge.game.ability.effects;

import java.util.*;

import com.google.common.collect.Lists;

import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Lang;

public class MutateEffect extends SpellAbilityEffect {
    
    @Override
    public String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final List<GameObject> targets = getTargets(sa);

        sb.append(" Mutates with ");
        sb.append(Lang.joinHomogenous(targets));
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Card host = sa.getHostCard();
        // There shouldn't be any mutate abilities, but for now.
        if (sa.isSpell()) {
            host.setController(p, 0);
        }

        // 111.11. A copy of a permanent spell becomes a token as it resolves.
        // The token has the characteristics of the spell that became that token.
        // The token is not “created” for the purposes of any replacement effects or triggered abilities that refer to creating a token.
        if (host.isCopiedSpell()) {
            host.setCopiedSpell(false);
            host.setToken(true);
        }

        final List<GameObject> targets = getDefinedOrTargeted(sa, "Defined");
        Card target = (Card)targets.get(0);

        CardCollectionView view = CardCollection.getView(Lists.newArrayList(host, target));
        Card topCard = host.getController().getController().chooseSingleEntityForEffect(
                view,
                sa,
                "Choose which creature to be the top",
                false,
                new HashMap<>()
        );
        final boolean putOnTop = (topCard == host);

        if (putOnTop) {
            host.addMergedCard(target);
            host.addMergedCards(target.getMergedCards());
            target.clearMergedCards();
            target.setMergedToCard(host);
        } else {
            target.addMergedCard(host);
            host.setMergedToCard(target);
        }

        final Card c = p.getGame().getAction().moveToPlay(host, p, sa);
        sa.setHostCard(c);

        p.getGame().getTriggerHandler().runTrigger(TriggerType.Mutates, AbilityKey.mapFromCard(c), false);
    }

}
