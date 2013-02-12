package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class TwoPilesAi extends SpellAiLogic  {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Card card = sa.getSourceCard();
        ZoneType zone = null;

        if (sa.hasParam("Zone")) {
            zone = ZoneType.smartValueOf(sa.getParam("Zone"));
        }

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        List<Player> tgtPlayers;
        final Player opp = ai.getOpponent();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canTgtPlayer()) {
                tgt.addTarget(opp);
            }
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }

        final Player p = tgtPlayers.get(0);
        List<Card> pool = new ArrayList<Card>();
        if (sa.hasParam("DefinedCards")) {
            pool = new ArrayList<Card>(AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("DefinedCards"), sa));
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
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        return false;
    }
}
