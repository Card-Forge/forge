package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class TwoPilesAi extends SpellAiLogic  {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPlayAI(Player ai, Map<String, String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();
        ZoneType zone = null;

        if (params.containsKey("Zone")) {
            zone = ZoneType.smartValueOf(params.get("Zone"));
        }

        String valid = "";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        ArrayList<Player> tgtPlayers;
        final Player opp = ai.getOpponent();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canTgtPlayer()) {
                tgt.addTarget(opp);
            }
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        final Player p = tgtPlayers.get(0);
        List<Card> pool = new ArrayList<Card>();
        if (params.containsKey("DefinedCards")) {
            pool = new ArrayList<Card>(AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("DefinedCards"), sa));
        } else {
            pool = p.getCardsIn(zone);
        }
        pool = CardLists.getValidCards(pool, valid, card.getController(), card);
        int size = pool.size();
        return size > 2;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, Map<String, String> params, SpellAbility sa, boolean mandatory) {
        return false;
    }
}