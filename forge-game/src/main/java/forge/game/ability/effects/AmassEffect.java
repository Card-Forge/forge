package forge.game.ability.effects;

import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class AmassEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder("Amass ");
        final Card card = sa.getHostCard();
        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);

        sb.append(amount).append(" (Put ");

        sb.append(Lang.nounWithNumeral(amount, "+1/+1 counter"));

        sb.append("on an Army you control. If you don’t control one, create a 0/0 black Zombie Army creature token first.)");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();

        final int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);
        final boolean remember = sa.hasParam("RememberAmass");

        // create army token if needed
        if (CardLists.count(activator.getCardsIn(ZoneType.Battlefield), CardPredicates.isType("Army")) == 0) {
            CardZoneTable triggerList = new CardZoneTable();
            MutableBoolean combatChanged = new MutableBoolean(false);

            makeTokenTable(makeTokenTableInternal(activator, "b_0_0_zombie_army", 1, sa), false, triggerList, combatChanged, sa);

            triggerList.triggerChangesZoneAll(game, sa);
            triggerList.clear();

            game.fireEvent(new GameEventTokenCreated());

            if (combatChanged.isTrue()) {
                game.updateCombatForView();
                game.fireEvent(new GameEventCombatChanged());
            }
        }

        Map<String, Object> params = Maps.newHashMap();
        params.put("CounterType", CounterType.get(CounterEnumType.P1P1));
        params.put("Amount", 1);

        CardCollectionView tgtCards = CardLists.getType(activator.getCardsIn(ZoneType.Battlefield), "Army");
        tgtCards = pc.chooseCardsForEffect(tgtCards, sa, Localizer.getInstance().getMessage("lblChooseAnArmy"), 1, 1, false, params);

        GameEntityCounterTable table = new GameEntityCounterTable();
        for(final Card tgtCard : tgtCards) {
            tgtCard.addCounter(CounterEnumType.P1P1, amount, activator, sa, true, table);
            game.updateLastStateForCard(tgtCard);

            if (remember) {
                card.addRemembered(tgtCard);
            }
        }
        table.triggerCountersPutAll(game);
    }

}
