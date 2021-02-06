package forge.game.ability.effects;

import java.util.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Lang;

public class MutateEffect extends SpellAbilityEffect {

    private void migrateTopCard(final Card host, final Card target) {
        // Copy all status from target card and migrate all counters
        // Also update all reference of target card to new top card

        // TODO: find out all necessary status that should be copied
        host.setTapped(target.isTapped());
        host.setSickness(target.isFirstTurnControlled());
        host.setFlipped(target.isFlipped());
        host.setDamage(target.getDamage());
        host.setMonstrous(target.isMonstrous());
        host.setRenowned(target.isRenowned());

        // Migrate counters
        Map<CounterType, Integer> counters = target.getCounters();
        if (!counters.isEmpty()) {
            host.setCounters(Maps.newHashMap(counters));
        }
        target.clearCounters();

        // Migrate attached cards
        CardCollectionView attached = target.getAttachedCards();
        for (final Card c : attached) {
            c.setEntityAttachedTo(host);
        }
        target.setAttachedCards(null);
        host.setAttachedCards(attached);
        
        // TODO: move all remembered, imprinted objects to new top card
        //       and possibly many other needs to be migrated.
    }
    
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
        
        // Now the top card always have all abilities from bottom cards
        final Long ts = game.getNextTimestamp();
        if (topCard == target) {
            final CardCloneStates cloneStates = CardFactory.getCloneStates(target, target, sa);
            final CardState targetState = cloneStates.get(target.getCurrentStateName());
            final CardState newState = host.getCurrentState();
            targetState.addAbilitiesFrom(newState, false);
            target.addCloneState(cloneStates, ts);
            // Re-register triggers for target card
            game.getTriggerHandler().clearActiveTriggers(target, null);
            game.getTriggerHandler().registerActiveTrigger(target, false);
        } else {
            final CardCloneStates cloneStates = CardFactory.getCloneStates(host, host, sa);
            final CardState newState = cloneStates.get(host.getCurrentStateName());
            final CardState targetState = target.getCurrentState();
            newState.addAbilitiesFrom(targetState, false);
            host.addCloneState(cloneStates, ts);
        }

        game.getAction().moveToPlay(host, p, sa);

        if (topCard == host) {
            migrateTopCard(host, target);
        } else {
            host.setTapped(target.isTapped());
            host.setFlipped(target.isFlipped());
        }

        game.getTriggerHandler().runTrigger(TriggerType.Mutates, AbilityKey.mapFromCard(topCard), false);
    }

}
