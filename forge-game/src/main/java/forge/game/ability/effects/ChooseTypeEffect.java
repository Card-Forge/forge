package forge.game.ability.effects;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;

import forge.card.CardType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import com.google.common.collect.Lists;

public class ChooseTypeEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a type.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final String type = sa.getParam("Type");
        int amount = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Amount", "1"), sa);

        final List<String> validTypes = Lists.newArrayList();
        if (sa.hasParam("ValidTypes")) {
            validTypes.addAll(Arrays.asList(sa.getParam("ValidTypes").split(",")));
        } else {
            switch (type) {
            case "Card":
                validTypes.addAll(CardType.getAllCardTypes());
                break;
            case "Creature":
                validTypes.addAll(CardType.getAllCreatureTypes());
                break;
            case "Basic Land":
                validTypes.addAll(CardType.getBasicTypes());
                break;
            case "Land":
                validTypes.addAll(CardType.getAllLandTypes());
                break;
            }
        }

        if (sa.hasParam("InvalidTypes")) {
            for (final String s : sa.getParam("InvalidTypes").split(",")) {
                validTypes.remove(s);
            }
        }

        if (!validTypes.isEmpty()) {
            for (final Player p : getTargetPlayers(sa)) {
                if (!sa.usesTargeting() || p.canBeTargetedBy(sa)) {
                    List<String> choices = Lists.newArrayList();
                    for (int i = 0; i < amount; i++) {
                        // the only one with multiple amount currently cares about the order
                        choices.addAll(p.getController().chooseSomeType(type, sa, 1, 1, validTypes));
                        validTypes.removeAll(choices);
                    }
                    card.setChosenType(choices, sa);
                }
            }
        }
        else {
            throw new InvalidParameterException(sa.getHostCard() + "'s ability resulted in no types to choose from");
        }
    }
}
