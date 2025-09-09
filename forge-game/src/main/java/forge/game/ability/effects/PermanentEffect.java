package forge.game.ability.effects;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
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

        final Card c = game.getAction().moveToPlay(host, sa, moveParams);
        sa.setHostCard(c);

        // CR 608.3g
        if (sa.isIntrinsic() || c.wasCast()) {
            if (sa.isDash() && c.isInPlay()) {
                registerDelayedTrigger(sa, "Hand", Lists.newArrayList(c));
                // add AI hint
                c.addChangedSVars(Collections.singletonMap("EndOfTurnLeavePlay", "Dash"), c.getGame().getNextTimestamp(), 0);
            }
            if (sa.isBlitz() && c.isInPlay()) {
                registerDelayedTrigger(sa, "Sacrifice", Lists.newArrayList(c));
                c.addChangedSVars(Collections.singletonMap("EndOfTurnLeavePlay", "Blitz"), c.getGame().getNextTimestamp(), 0);
            }
            if (sa.isWarp() && c.isInPlay()) {
                registerDelayedTrigger(sa, "Exile", Lists.newArrayList(c));
                c.addChangedSVars(Collections.singletonMap("EndOfTurnLeavePlay", "Warp"), c.getGame().getNextTimestamp(), 0);
            }
        }

        table.triggerChangesZoneAll(game, sa);
    }
}
