package forge.game.ability.effects;

import forge.card.CardType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        final List<String> invalidTypes = sa.hasParam("InvalidTypes") ? Arrays.asList(sa.getParam("InvalidTypes").split(",")) : new ArrayList<String>();

        final List<String> validTypes = new ArrayList<String>();
        if (sa.hasParam("ValidTypes")) {
            validTypes.addAll(Arrays.asList(sa.getParam("ValidTypes").split(",")));
        }

        if (type.equals("Card")) {
            if (validTypes.isEmpty()) validTypes.addAll(CardType.getAllCardTypes());
        } else if (type.equals("Creature")) {
            if (validTypes.isEmpty()) validTypes.addAll(CardType.getAllCreatureTypes());
        } else if (type.equals("Basic Land")) {
            if (validTypes.isEmpty()) validTypes.addAll(CardType.getBasicTypes());
        } else if (type.equals("Land")) {
            if (validTypes.isEmpty()) validTypes.addAll(CardType.getAllLandTypes());
        } // end if-else if

        for (final String s : invalidTypes) {
            validTypes.remove(s);
        }

        
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if( !validTypes.isEmpty()) {
            for (final Player p : tgtPlayers) {
                if ((tgt == null) || p.canBeTargetedBy(sa)) {
                    String choice = p.getController().chooseSomeType(type, sa, validTypes, invalidTypes);
                    card.setChosenType(choice);
                }
            }
        } else 
            throw new InvalidParameterException(sa.getHostCard() + "'s ability resulted in no types to choose from");
    }

}
