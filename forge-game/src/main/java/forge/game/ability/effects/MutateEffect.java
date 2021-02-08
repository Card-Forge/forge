package forge.game.ability.effects;

import java.util.*;

import com.google.common.collect.Lists;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

public class MutateEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
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
        final Card target = (Card)targets.get(0);

        CardCollectionView view = CardCollection.getView(Lists.newArrayList(host, target));
        final Card topCard = host.getController().getController().chooseSingleEntityForEffect(
                view,
                sa,
                "Choose which creature to be on top",
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


        // First remove current mutated states
        if (target.getMutatedTimestamp() != -1) {
            target.removeCloneState(target.getMutatedTimestamp());
            target.setMutatedTimestamp(-1);
        }
        // Now add all abilities from bottom cards
        final Long ts = game.getNextTimestamp();
        if (topCard.getCurrentStateName() != CardStateName.FaceDown) {
            final CardCloneStates mutatedStates = CardFactory.getMutatedCloneStates(topCard, sa);
            topCard.addCloneState(mutatedStates, ts);
            topCard.setMutatedTimestamp(ts);
        }
        if (topCard == target) {
            // Re-register triggers for target card
            game.getTriggerHandler().clearActiveTriggers(target, null);
            game.getTriggerHandler().registerActiveTrigger(target, false);
        }

        game.getAction().moveToPlay(host, p, sa);

        if (topCard == host) {
            CardFactory.migrateTopCard(host, target);
        } else {
            host.setTapped(target.isTapped());
            host.setFlipped(target.isFlipped());
        }
        topCard.setTimesMutated(topCard.getTimesMutated() + 1);

        game.getTriggerHandler().runTrigger(TriggerType.Mutates, AbilityKey.mapFromCard(topCard), false);
    }

}
