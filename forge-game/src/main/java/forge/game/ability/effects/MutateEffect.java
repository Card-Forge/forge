package forge.game.ability.effects;

import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class MutateEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = host.getOwner();
        final Game game = host.getGame();
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
                Localizer.getInstance().getMessage("lblChooseCreatureToBeTop"),
                false,
                new HashMap<>()
        );
        final boolean putOnTop = (topCard == host);

        // There shouldn't be any mutate abilities, but for now.
        if (sa.isSpell()) {
            host.setController(p, 0);
        }

        final boolean wasFaceDown = target.isFaceDown();

        host.setMergedToCard(target);
        // If first time mutate, add target first.
        if (!target.hasMergedCard()) {
            target.addMergedCard(target);
        }
        if (putOnTop) {
            target.addMergedCardToTop(host);
        } else {
            target.addMergedCard(host);
        }

        // First remove current mutated states
        if (target.getMutatedTimestamp() != -1) {
            target.removeCloneState(target.getMutatedTimestamp());
        }
        // Now add all abilities from bottom cards
        final Long ts = game.getNextTimestamp();
        target.setMutatedTimestamp(ts);

        target.addCloneState(CardFactory.getMutatedCloneStates(target, sa), ts);

        // currently used by Tezzeret, Cruel Machinist and Yedora, Grave Gardener
        // when mutating onto the FaceDown, their effect should end, then 721.2e would stop trigger
        if (wasFaceDown && !target.isFaceDown()) {
            target.runFaceupCommands();
        }

        // Re-register triggers for target card
        game.getTriggerHandler().clearActiveTriggers(target, null);
        game.getTriggerHandler().registerActiveTrigger(target, false);

        game.getAction().moveTo(p.getZone(ZoneType.Merged), host, sa);

        host.setTapped(target.isTapped());
        host.setFlipped(target.isFlipped());
        target.setTimesMutated(target.getTimesMutated() + 1);
        target.updateStateForView();
        target.updateTokenView();
        if (host.isCommander()) {
            host.getOwner().updateMergedCommanderInfo(target, host);
            target.updateCommanderView();
        }

        game.getTriggerHandler().runTrigger(TriggerType.Mutates, AbilityKey.mapFromCard(target), false);
    }

}
