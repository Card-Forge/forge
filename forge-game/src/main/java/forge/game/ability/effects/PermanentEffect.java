package forge.game.ability.effects;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.event.GameEventCombatChanged;
import forge.game.spellability.SpellAbility;

public class PermanentEffect extends SpellAbilityEffect {

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.
     * SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        final CardZoneTable table = AbilityKey.addCardZoneTableParams(moveParams, sa);

        if ((sa.isIntrinsic() || host.wasCast()) && sa.isSneak()) {
            host.setTapped(true);
        }

        final Card c = game.getAction().moveToPlay(host, sa, moveParams);
        sa.setHostCard(c);

        // CR 608.3g
        if ((sa.isIntrinsic() || c.wasCast()) && c.isInPlay()) {
            if (sa.isDash()) {
                registerDelayedTrigger(sa, "Hand", Lists.newArrayList(c));
                // add AI hint
                c.addChangedSVars(Collections.singletonMap("EndOfTurnLeavePlay", "Dash"), c.getGame().getNextTimestamp(), 0);
            }
            if (sa.isBlitz()) {
                registerDelayedTrigger(sa, "Sacrifice", Lists.newArrayList(c));
                c.addChangedSVars(Collections.singletonMap("EndOfTurnLeavePlay", "Blitz"), c.getGame().getNextTimestamp(), 0);
            }
            if (sa.isWarp()) {
                registerDelayedTrigger(sa, "Exile", Lists.newArrayList(c));
                c.addChangedSVars(Collections.singletonMap("EndOfTurnLeavePlay", "Warp"), c.getGame().getNextTimestamp(), 0);
            }
            if (sa.isSneak() && c.isCreature()) {
                final Card returned = sa.getPaidList("Returned", true).getFirst();
                final GameEntity defender = game.getCombat().getDefenderByAttacker(returned);
                game.getCombat().addAttacker(c, defender);
                game.getCombat().getBandOfAttacker(c).setBlocked(false);

                game.updateCombatForView();
                game.fireEvent(new GameEventCombatChanged());
            }
        }

        table.triggerChangesZoneAll(game, sa);
    }
}
