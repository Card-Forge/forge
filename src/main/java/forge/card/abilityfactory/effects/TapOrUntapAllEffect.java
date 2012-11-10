package forge.card.abilityfactory.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TapOrUntapAllEffect extends SpellEffect {
    
    private enum TapOrUntap {
        TAP, UNTAP
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();
        sb.append("Tap or untap ");

        if (sa.hasParam("ValidMessage")) {
            sb.append(sa.getParam("ValidMessage"));
        }
        else {
            final List<Card> tgtCards = getTargetCards(sa);
            sb.append(StringUtils.join(tgtCards, ", "));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        List<Card> validCards = getTargetCards(sa);

        if (sa.hasParam("ValidCards")) {
            validCards = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            validCards = AbilityFactory.filterListByType(validCards, sa.getParam("ValidCards"), sa);
        }

        // Default to tapping for AI
        TapOrUntap toTap = TapOrUntap.TAP;
        if (sa.getActivatingPlayer().isHuman()) {
            StringBuilder sb = new StringBuilder("Tap or Untap ");
            if (sa.hasParam("ValidMessage")) {
                sb.append(sa.getParam("ValidMessage"));
            } else{
                sb.append("Permanents");
            }
            sb.append("?");
            
            final String[] tapOrUntap = new String[] { "Tap", "Untap" };
            final Object z = GuiChoose.one(sb.toString(), tapOrUntap);
            toTap = (z.equals("Tap")) ? TapOrUntap.TAP : TapOrUntap.UNTAP;
        }

        for (final Card cad : validCards) {
            if (cad.isInPlay()) {
                if (toTap.equals(TapOrUntap.TAP)) {
                    cad.tap();
                } else {
                    cad.untap();
                }
            }
        }
    }

}
