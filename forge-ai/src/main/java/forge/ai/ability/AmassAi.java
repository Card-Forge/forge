package forge.ai.ability;

import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.card.token.TokenInfo;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class AmassAi extends SpellAbilityAi {
    @Override
    protected boolean checkApiLogic(Player ai, final SpellAbility sa) {
        CardCollection aiArmies = CardLists.getType(ai.getCardsIn(ZoneType.Battlefield), "Army");
        Card host = sa.getHostCard();
        final Game game = ai.getGame();

        if (!aiArmies.isEmpty()) {
            return Iterables.any(aiArmies, CardPredicates.canReceiveCounters(CounterEnumType.P1P1));
        }
        final String type = sa.getParam("Type");
        StringBuilder sb = new StringBuilder("b_0_0_");
        sb.append(sa.getOriginalParam("Type").toLowerCase()).append("_army");
        final String tokenScript = sb.toString();
        final int amount = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Num", "1"), sa);

        Card token = TokenInfo.getProtoType(tokenScript, sa, ai, false);

        if (token == null) {
            return false;
        }

        token.setController(ai, 0);
        token.setLastKnownZone(ai.getZone(ZoneType.Battlefield));
        token.setCreatureTypes(Lists.newArrayList(type, "Army"));
        token.setName(type + " Army Token");
        token.setTokenSpawningAbility(sa);

        boolean result = true;

        // need to check what the cards would be on the battlefield
        // do not attach yet, that would cause Events
        CardCollection preList = new CardCollection(token);
        game.getAction().checkStaticAbilities(false, Sets.newHashSet(token), preList);

        if (token.canReceiveCounters(CounterEnumType.P1P1)) {
            token.setCounters(CounterEnumType.P1P1, amount);
        }

        if (token.isCreature() && token.getNetToughness() < 1) {
            result = false;
        }

        //reset static abilities
        game.getAction().checkStaticAbilities(false);

        return result;
    }

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        // TODO: Special check for instant speed logic? Something like Lazotep Plating.
        /*
        boolean isInstant = sa.getRestrictions().isInstantSpeed();
        CardCollection aiArmies = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.isType("Army"));

        if (isInstant) {

        }
        */

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return mandatory || checkApiLogic(ai, sa);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        Iterable<Card> better = CardLists.filter(options, CardPredicates.canReceiveCounters(CounterEnumType.P1P1));
        if (Iterables.isEmpty(better)) {
            better = options;
        }
        return ComputerUtilCard.getBestAI(better);
    }
}
