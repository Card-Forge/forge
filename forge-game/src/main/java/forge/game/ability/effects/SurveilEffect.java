package forge.game.ability.effects;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;
import forge.util.Localizer;

public class SurveilEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append(Lang.joinHomogenous(getTargetPlayers(sa)));

        int num = 1;
        if (sa.hasParam("Amount")) {
            num = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa);
        }

        sb.append(" surveils (").append(num).append(").");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        int num = 1;
        if (sa.hasParam("Amount")) {
            num = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa);
        }

        boolean isOptional = sa.hasParam("Optional");

        CardZoneTable table = new CardZoneTable(sa.getLastStateBattlefield(), sa.getLastStateGraveyard());
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());
        moveParams.put(AbilityKey.InternalTriggerTable, table);

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            if (isOptional && !p.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantSurveil"), null)) {
                continue;
            }

            p.surveil(num, sa, moveParams);
        }
        table.triggerChangesZoneAll(sa.getHostCard().getGame(), sa);
    }

}
