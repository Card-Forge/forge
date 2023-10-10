package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class TimeTravelEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Time travel. (For each suspended card you own and each permanent you control with a time counter on it, you may add or remove a time counter.)";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        int num = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa) : 1;

        PlayerController pc = p.getController();

        final CounterEnumType counterType = CounterEnumType.TIME;

        for (int i = 0; i < num; i++) {
            FCollection<Card> list = new FCollection<>();

            // card you own that is suspended
            list.addAll(CardLists.filter(p.getCardsIn(ZoneType.Exile), CardPredicates.hasSuspend()));
            // permanent you control with time counter
            list.addAll(CardLists.filter(p.getCardsIn(ZoneType.Battlefield), CardPredicates.hasCounter(counterType)));

            GameEntityCounterTable table = new GameEntityCounterTable();

            String prompt = Localizer.getInstance().getMessage("lblChooseaCard");
            for (Card c : pc.chooseEntitiesForEffect(list, 0, list.size(), null, sa, prompt, p, null)) {

                Map<String, Object> params = Maps.newHashMap();
                params.put("Target", c);
                params.put("CounterType", counterType);
                prompt = Localizer.getInstance().getMessage("lblWhatToDoWithTargetCounter",  counterType.getName()) + " ";
                boolean putCounter;
                    putCounter = pc.chooseBinary(sa, prompt, BinaryChoiceType.AddOrRemove, params);

                if (putCounter) {
                    c.addCounter(counterType, 1, p, table);
                } else {
                    c.subtractCounter(counterType, 1);
                }
            }
            table.replaceCounterEffect(game, sa, true);
        }
    }

}
