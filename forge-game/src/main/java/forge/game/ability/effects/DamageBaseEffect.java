package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

abstract public class DamageBaseEffect extends SpellAbilityEffect {

    static void replaceDying(final SpellAbility sa) {
        if (sa.hasParam("ReplaceDyingDefined")) {

            if (sa.hasParam("ReplaceDyingCondition")) {
                // currently there is only one with Kicker
                final String condition = sa.getParam("ReplaceDyingCondition");
                if ("Kicked".equals(condition)) {
                    if (!sa.isKicked()) {
                        return;
                    }
                }
            }

            final Card host = sa.getHostCard();
            final Player controller = sa.getActivatingPlayer();
            final Game game = host.getGame();
            String zone = sa.getParamOrDefault("ReplaceDyingZone", "Exile");
            CardCollection cards = AbilityUtils.getDefinedCards(host, sa.getParam("ReplaceDyingDefined"), sa);
            // no cards, no need for Effect
            if (cards.isEmpty()) {
                return;
            }

            // build an Effect with that infomation
            String name = host.getName() + "'s Effect";

            final Card eff = createEffect(host, controller, name, host.getImageKey());
            eff.addRemembered(cards);

            String repeffstr = "Event$ Moved | ValidCard$ Card.IsRemembered " +
            "| Origin$ Battlefield | Destination$ Graveyard " +
            "| Description$ If the creature would die this turn, exile it instead.";
            String effect = "DB$ ChangeZone | Defined$ ReplacedCard | Origin$ Battlefield | Destination$ " + zone;

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
            re.setLayer(ReplacementLayer.Other);

            re.setOverridingAbility(AbilityFactory.getAbility(effect, eff));
            eff.addReplacementEffect(re);

            // Add forgot trigger
            addForgetOnMovedTrigger(eff, "Battlefield");

            // Copy text changes
            if (sa.isIntrinsic()) {
                eff.copyChangedTextFrom(host);
            }

            final GameCommand endEffect = new GameCommand() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void run() {
                    game.getAction().exile(eff, null);
                }
            };

            game.getEndOfTurn().addUntil(endEffect);

            eff.updateStateForView();

            // TODO: Add targeting to the effect so it knows who it's dealing with
            game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
            game.getAction().moveTo(ZoneType.Command, eff, sa);
            game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        }
    }
}
