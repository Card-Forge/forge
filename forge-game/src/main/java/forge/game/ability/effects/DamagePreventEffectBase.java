package forge.game.ability.effects;

import java.util.List;

import forge.GameCommand;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventPlayerStatsChanged;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

public abstract class DamagePreventEffectBase extends SpellAbilityEffect {
    public static void addPreventNextDamage(SpellAbility sa, GameEntity o, int numDam) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final Player player = hostCard.getController();
        final String name = hostCard + "'s Effect";
        final String image = hostCard.getImageKey();
        StringBuilder sb = new StringBuilder("Event$ DamageDone | ActiveZones$ Command | ValidTarget$ ");
        sb.append((o instanceof Card ? "Card.IsRemembered" : "Player.IsRemembered"));
        sb.append(" | PreventionEffect$ NextN | Description$ Prevent the next ").append(numDam).append(" damage.");
        String effect = "DB$ ReplaceDamage | Amount$ ShieldAmount";

        final Card eff = createEffect(sa, player, name, image);
        eff.setSVar("ShieldAmount", "Number$" + numDam);
        eff.setSVar("PreventedDamage", "Number$0");
        eff.addRemembered(o);

        SpellAbility replaceDamage = AbilityFactory.getAbility(effect, eff);
        if (sa.hasParam("PreventionSubAbility")) {
            String subAbString = sa.getSVar(sa.getParam("PreventionSubAbility"));
            if (sa.hasParam("ShieldEffectTarget")) {
                List<GameEntity> effTgts = AbilityUtils.getDefinedEntities(hostCard, sa.getParam("ShieldEffectTarget"), sa);
                String effTgtString = "";
                for (final GameEntity effTgt : effTgts) {
                    if (effTgt instanceof Card) {
                        effTgtString = "CardUID_" + String.valueOf(((Card) effTgt).getId());
                    } else if (effTgt instanceof Player) {
                        effTgtString = "PlayerNamed_" + ((Player) effTgt).getName();
                    }
                }
                subAbString = TextUtil.fastReplace(subAbString, "ShieldEffectTarget", effTgtString);
            }
            AbilitySub subSA = (AbilitySub) AbilityFactory.getAbility(subAbString, eff);
            replaceDamage.setSubAbility(subSA);
            // Add SpellDescription of PreventionSubAbility to effect description
            sb.append(" ").append(subSA.getParam("SpellDescription"));
        }

        String repeffstr = sb.toString();
        ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
        re.setOverridingAbility(replaceDamage);
        eff.addReplacementEffect(re);
        if (o instanceof Card) {
            addForgetOnMovedTrigger(eff, "Battlefield");
        }

        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        game.getAction().moveTo(ZoneType.Command, eff, sa, AbilityKey.newMap());
        eff.updateStateForView();
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        o.getView().updatePreventNextDamage(o);
        if (o instanceof Player) {
            game.fireEvent(new GameEventPlayerStatsChanged((Player) o, false));
        }

        game.getEndOfTurn().addUntil(new GameCommand() {
            private static final long serialVersionUID = 1L;

            @Override
            public void run() {
                game.getAction().exile(eff, null, null);
                o.getView().updatePreventNextDamage(o);
                if (o instanceof Player) {
                    game.fireEvent(new GameEventPlayerStatsChanged((Player) o, false));
                }
            }
        });
    }
}
