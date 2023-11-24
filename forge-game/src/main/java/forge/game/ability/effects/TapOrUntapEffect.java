package forge.game.ability.effects;

import com.google.common.collect.Maps;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.Map;

public class TapOrUntapEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();

        sb.append("Tap or untap ");

        sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();
        PlayerController pc = activator.getController();

        CardCollection tapped = new CardCollection();
        final Map<Player, CardCollection> untapMap = Maps.newHashMap();
        for (final Card tgtC : getTargetCards(sa)) {
            if (!tgtC.isInPlay()) {
                continue;
            }
            if (tgtC.isPhasedOut()) {
                continue;
            }

            // If the effected card is controlled by the same controller of the SA, default to untap.
            boolean tap = pc.chooseBinary(sa, Localizer.getInstance().getMessage("lblTapOrUntapTarget", CardTranslation.getTranslatedName(tgtC.getName())), PlayerController.BinaryChoiceType.TapOrUntap,
                    !tgtC.getController().equals(activator) );

            Player tapper = activator;
            if (sa.hasParam("Tapper")) {
                tapper = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Tapper"), sa).getFirst();
            }
            if (tap) {
                if (tgtC.tap(true, sa, tapper)) tapped.add(tgtC);
            } else if (tgtC.untap(true)) {
                untapMap.computeIfAbsent(tapper, i -> new CardCollection()).add(tgtC);
            }
        }
        if (!untapMap.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Map, untapMap);
            activator.getGame().getTriggerHandler().runTrigger(TriggerType.UntapAll, runParams, false);
        }
        if (!tapped.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cards, tapped);
            activator.getGame().getTriggerHandler().runTrigger(TriggerType.TapAll, runParams, false);
        }
    }

}
