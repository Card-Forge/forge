package forge.game.ability.effects;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import forge.util.collect.FCollection;

import java.util.Map;

public class AugmentEffect  extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Card source = sa.getHostCard();
        final Game game = source.getGame();

        Card augmentCreature;
        if (sa.hasParam("Choices")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChoose") + " ";

            CardCollection choices = CardLists.getValidCards(game.getCardsIn(choiceZone), sa.getParam("Choices"), activator, source, sa);

            Map<String, Object> params = Maps.newHashMap();
            params.put("Target", Iterables.getFirst(getDefinedCardsOrTargeted(sa, "Defined"), null));

            augmentCreature = activator.getController().chooseSingleEntityForEffect(choices, sa, title, params);
            if (augmentCreature == null) {
                return;
            }
        } else {
            augmentCreature = source;
        }
        if (!augmentCreature.hasKeyword(Keyword.AUGMENT)) {
            return;
        }

        FCollection<Card> targets = new FCollection<>(getDefinedCardsOrTargeted(sa, "Defined"));
        if (targets.isEmpty()) {
            return;
        }
        String title = Localizer.getInstance().getMessage("lblChoose");
        Map<String, Object> params = Maps.newHashMap();
        params.put("Attachments", augmentCreature);
        Card hostCreature = activator.getController().chooseSingleEntityForEffect(targets, sa, title, params);

        if (hostCreature == null || !hostCreature.hasKeyword(Keyword.AUGMENT_HOST)) {
            return;
        }

        final Card gameCard = augmentCreature.getGame().getCardState(augmentCreature, null);
        // gameCard is LKI in that case, the card is not in game anymore
        // or the timestamp did change
        // this should check Self too
        if (gameCard == null || !augmentCreature.equalsWithGameTimestamp(gameCard)) {
            return;
        }

        if (!hostCreature.attachAugment(augmentCreature)) {
            return;
        };
        final long ts = game.getNextTimestamp();
        hostCreature.setAugmentedTimeStamp(ts);
        hostCreature.addCloneState(CardFactory.getAugmentedCloneState(hostCreature, sa), ts);

        // Re-register triggers for target card
        game.getTriggerHandler().clearActiveTriggers(hostCreature, null);
        game.getTriggerHandler().registerActiveTrigger(hostCreature, false);

        game.getAction().moveTo(hostCreature.getOwner().getZone(ZoneType.Merged), augmentCreature, sa);

        hostCreature.updateStateForView();
        augmentCreature.updateStateForView();
    }
}
