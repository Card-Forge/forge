package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Localizer;

public class HeistEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());
        final Card source = sa.getHostCard();
        final Player player = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa).get(0);
        final Game game = player.getGame();
        final Player target = getTargetPlayers(sa).get(0);
        final CardZoneTable triggerList = new CardZoneTable();
        final int num = AbilityUtils.calculateAmount(source, sa.getParamOrDefault("Num", "1"), sa);
        CardCollection heisted = new CardCollection();

        for (int i = 0; i < num; i++) {
            List<Card> choices = Aggregates.random(CardLists.getNotType(target.getCardsIn(ZoneType.Library), 
                "Land"), 3);
            if (choices.isEmpty()) continue; //nothing to heist
            Card chosenCard = player.getController().chooseSingleCardForZoneChange(ZoneType.Exile, 
                new ArrayList<ZoneType>(Arrays.asList(ZoneType.Exile)), sa, new CardCollection(choices), 
                null, Localizer.getInstance().getMessage("lblChooseCardHeist"), false, 
                player);
            if (!chosenCard.canExiledBy(sa, true)) {
                continue;
            }
            Card exiled = game.getAction().moveTo(ZoneType.Exile, chosenCard, sa, moveParams);
            handleExiledWith(exiled, sa);
            heisted.add(exiled);
            if (chosenCard != null) triggerList.put(ZoneType.Library, exiled.getZone().getZoneType(), exiled);
        }

        if (!heisted.isEmpty()) {
            final Card eff = createEffect(sa, player, source + "'s Heist Effect", source.getImageKey());
            eff.addRemembered(heisted);
            String mayPlay = "Mode$ Continuous | MayPlay$ True | MayPlayIgnoreType$ True | EffectZone$ Command | " +
            "Affected$ Card.IsRemembered | AffectedZone$ Exile | Description$ You may play the heisted card for as " +
            "long as it remains exiled, and mana of any type can be spent to cast it.";
            eff.addStaticAbility(mayPlay);
            addForgetOnMovedTrigger(eff, "Exile");
            addForgetOnCastTrigger(eff, "Card.IsRemembered");
            game.getAction().moveToCommand(eff, sa);
        }

        triggerList.triggerChangesZoneAll(game, sa);
    }
}
