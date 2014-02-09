package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

public class TwoPilesAi extends SpellAbilityAi  {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card card = sa.getHostCard();
        ZoneType zone = null;

        if (sa.hasParam("Zone")) {
            zone = ZoneType.smartValueOf(sa.getParam("Zone"));
        }

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        
        final Player opp = ai.getOpponent();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (tgt.canTgtPlayer()) {
                sa.getTargets().add(opp);
            }
        }
        
        List<Player> tgtPlayers = getTargetPlayers(sa);
        
        final Player p = tgtPlayers.get(0);
        List<Card> pool = new ArrayList<Card>();
        if (sa.hasParam("DefinedCards")) {
            pool = new ArrayList<Card>(AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("DefinedCards"), sa));
        } else {
            pool = p.getCardsIn(zone);
        }
        pool = CardLists.getValidCards(pool, valid, card.getController(), card);
        int size = pool.size();
        return size > 2;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return false;
    }
}
