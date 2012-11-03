package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class UntapEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        // when getStackDesc is called, just build exactly what is happening
        final StringBuilder sb = new StringBuilder();
    
        sb.append("Untap ");
    
        if (params.containsKey("UntapUpTo")) {
            sb.append("up to ").append(params.get("Amount")).append(" ");
            sb.append(params.get("UntapType")).append("s");
        } else {
            List<Card> tgtCards = getTargetCards(sa, params);
            sb.append(StringUtils.join(tgtCards, ", "));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Target tgt = sa.getTarget();
    
        if (params.containsKey("UntapUpTo")) {
            untapChooseUpTo(sa, params);
        } else {
            
            final List<Card> tgtCards = getTargetCards(sa, params);
    
            for (final Card tgtC : tgtCards) {
                if (tgtC.isInPlay() && ((tgt == null) || tgtC.canBeTargetedBy(sa))) {
                    tgtC.untap();
                }
            }
        }
    }

    /**
     * <p>
     * untapChooseUpTo.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param params
     *            a {@link java.util.HashMap} object.
     */
    private void untapChooseUpTo(final SpellAbility sa, final Map<String, String> params) {
        final int num = Integer.parseInt(params.get("Amount"));
        final String valid = params.get("UntapType");
    
        final ArrayList<Player> definedPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                params.get("Defined"), sa);
    
        for (final Player p : definedPlayers) {
            if (p.isHuman()) {
                Singletons.getModel().getMatch().getInput().setInput(CardFactoryUtil.inputUntapUpToNType(num, valid));
            } else {
                List<Card> list = p.getCardsIn(ZoneType.Battlefield);
                list = CardLists.getType(list, valid);
                list = CardLists.filter(list, Presets.TAPPED);
    
                int count = 0;
                while ((list.size() != 0) && (count < num)) {
                    for (int i = 0; (i < list.size()) && (count < num); i++) {
    
                        final Card c = CardFactoryUtil.getBestLandAI(list);
                        c.untap();
                        list.remove(c);
                        count++;
                    }
                }
            }
        }
    }

}