package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

public class ScryEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        for (final Player p : tgtPlayers) {
            sb.append(p.toString()).append(" ");
        }

        int num = 1;
        if (sa.hasParam("ScryNum")) {
            num = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("ScryNum"), sa);
        }

        sb.append("scrys (").append(num).append(").");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {

        int num = 1;
        if (sa.hasParam("ScryNum")) {
            num = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("ScryNum"), sa);
        }

        final Target tgt = sa.getTarget();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                scry(p, num);
            }
        }
    }
    
    /**
     * <p>
     * scry.
     * </p>
     * 
     * @param numScry
     *            a int.
     */
    public final void scry(Player p, int numScry) {
        final List<Card> topN = new ArrayList<Card>();
        final PlayerZone library = p.getZone(ZoneType.Library);
        numScry = Math.min(numScry, library.size());
        for (int i = 0; i < numScry; i++) {
            topN.add(library.get(i));
        }

        ImmutablePair<List<Card>, List<Card>> lists = p.getController().arrangeForScry(topN);
        List<Card> toTop = lists.getLeft();
        List<Card> toBottom = lists.getRight();
        
        if ( null != toBottom) {
            for(Card c : toBottom) {
                p.getGame().getAction().moveToBottomOfLibrary(c);
            }
        }

        if ( null != toTop ) {
            Collections.reverse(toTop); // the last card in list will become topmost in library, have to revert thus.
            for(Card c : toTop) {
                p.getGame().getAction().moveToLibrary(c);
            }
        }
    }

}
