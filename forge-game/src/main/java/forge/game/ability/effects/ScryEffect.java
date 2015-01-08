package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.event.GameEventScry;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
            num = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("ScryNum"), sa);
        }

        sb.append("scrys (").append(num).append(").");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        int num = 1;
        if (sa.hasParam("ScryNum")) {
            num = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("ScryNum"), sa);
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                scry(p, num);
            }
        }
    }

    private static void scry(final Player p, final int numScry) {
        final CardCollection topN = new CardCollection();
        final PlayerZone library = p.getZone(ZoneType.Library);
        final int actualNumScry = Math.min(numScry, library.size());

        if (actualNumScry == 0) { return; }

        for (int i = 0; i < actualNumScry; i++) {
            topN.add(library.get(i));
        }

        final ImmutablePair<CardCollection, CardCollection> lists = p.getController().arrangeForScry(topN);
        final CardCollection toTop = lists.getLeft();
        final CardCollection toBottom = lists.getRight();

        int numToBottom = 0;
        int numToTop = 0;
        
        if (toBottom != null) {
            for(Card c : toBottom) {
                p.getGame().getAction().moveToBottomOfLibrary(c);
                numToBottom++;
            }
        }

        if (toTop != null) {
            Collections.reverse(toTop); // the last card in list will become topmost in library, have to revert thus.
            for(Card c : toTop) {
                p.getGame().getAction().moveToLibrary(c);
                numToTop++;
            }
        }

        p.getGame().fireEvent(new GameEventScry(p, numToTop, numToBottom));

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", p);
        p.getGame().getTriggerHandler().runTrigger(TriggerType.Scry, runParams, false);
    }
}
