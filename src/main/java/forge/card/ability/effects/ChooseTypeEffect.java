package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.Card;
import forge.Constant;
import forge.card.CardType;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;

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
        final Card card = sa.getSourceCard();
        final String type = sa.getParam("Type");
        final List<String> invalidTypes = sa.hasParam("InvalidTypes") ? Arrays.asList(sa.getParam("InvalidTypes").split(",")) : new ArrayList<String>();

        final List<String> validTypes = new ArrayList<String>();
        if (sa.hasParam("ValidTypes")) {
            validTypes.addAll(Arrays.asList(sa.getParam("ValidTypes").split(",")));
        }

        final Target tgt = sa.getTarget();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (type.equals("Card")) {
                    if (validTypes.isEmpty()) validTypes.addAll(Constant.CardTypes.CARD_TYPES);
                } else if (type.equals("Creature")) {
                    if (validTypes.isEmpty()) validTypes.addAll(CardType.getCreatureTypes());
                } else if (type.equals("Basic Land")) {
                    if (validTypes.isEmpty()) validTypes.addAll(CardType.getBasicTypes());
                } else if (type.equals("Land")) {
                    if (validTypes.isEmpty()) validTypes.addAll(CardType.getLandTypes());
                } // end if-else if

                if( !validTypes.isEmpty()) {
                    for (final String s : invalidTypes) {
                        validTypes.remove(s);
                    }
                    String choice = p.getController().chooseSomeType(type, sa.getParam("AILogic"), validTypes, invalidTypes);
                    card.setChosenType(choice);
                }

            }
        }
    }

}
