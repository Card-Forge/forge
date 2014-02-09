package forge.game.ability.effects;

import com.google.common.collect.Iterables;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MultiplePilesEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        String valid = "";
        String piles = sa.getParam("Piles");
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        sb.append("Separate all ").append(valid).append(" cards ");

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("controls into ").append(piles).append(" piles.");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final ZoneType zone = sa.hasParam("Zone") ? ZoneType.smartValueOf(sa.getParam("Zone")) : ZoneType.Battlefield;
        final boolean randomChosen = sa.hasParam("RandomChosen");
        final int piles = Integer.parseInt(sa.getParam("Piles"));
        final Map<Player, List<List<Card>>> record = new HashMap<Player, List<List<Card>>>();

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        // starting with the activator
        int pSize = tgtPlayers.size();
        Player activator = sa.getActivatingPlayer();
        while (tgtPlayers.contains(activator) && !activator.equals(Iterables.getFirst(tgtPlayers, null))) {
            tgtPlayers.add(pSize - 1, tgtPlayers.remove(0));
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                List<Card> pool;
                if (sa.hasParam("DefinedCards")) {
                    pool = new ArrayList<Card>(AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa));
                } else {
                    pool = p.getCardsIn(zone);
                }
                pool = CardLists.getValidCards(pool, valid, source.getController(), source);

                List<List<Card>> pileList = new ArrayList<List<Card>>();

                for (int i = 1; i < piles; i++) {
                    int size = pool.size();
                    List<Card> pile = p.getController().chooseCardsForEffect(pool, sa, "Choose cards in Pile " + i, 0, size, false);
                    pileList.add(pile);
                    pool.removeAll(pile);
                }

                pileList.add(pool);
                p.getGame().getAction().nofityOfValue(sa, p, pileList.toString(), p);
                record.put(p, pileList);
            }
        }
        if (randomChosen) {
            for (Entry<Player, List<List<Card>>> ev : record.entrySet()) {
                List<Card> chosen = Aggregates.random(ev.getValue());
                for (Card c : chosen) {
                    source.addRemembered(c);
                }
            }
            final SpellAbility action = AbilityFactory.getAbility(source.getSVar(sa.getParam("ChosenPile")), source);
            action.setActivatingPlayer(sa.getActivatingPlayer());
            ((AbilitySub) action).setParent(sa);
            AbilityUtils.resolve(action);
            source.clearRemembered();
        }
    }
}
