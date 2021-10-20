package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Localizer;

public class MultiplePilesEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        String piles = sa.getParam("Piles");
        final String valid = sa.getParamOrDefault("ValidCards", "");

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
        final Card source = sa.getHostCard();
        final ZoneType zone = sa.hasParam("Zone") ? ZoneType.smartValueOf(sa.getParam("Zone")) : ZoneType.Battlefield;
        final boolean randomChosen = sa.hasParam("RandomChosen");
        final int piles = Integer.parseInt(sa.getParam("Piles"));
        final Map<Player, List<CardCollectionView>> record = Maps.newHashMap();

        final String valid = sa.getParamOrDefault("ValidCards", "");

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
                CardCollection pool;
                if (sa.hasParam("DefinedCards")) {
                    pool = new CardCollection(AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa));
                } else {
                    pool = new CardCollection(p.getCardsIn(zone));
                }
                pool = CardLists.getValidCards(pool, valid, source.getController(), source, sa);

                List<CardCollectionView> pileList = Lists.newArrayList();

                for (int i = 1; i < piles; i++) {
                    int size = pool.size();
                    CardCollectionView pile = p.getController().chooseCardsForEffect(pool, sa, Localizer.getInstance().getMessage("lblChooseCardsInTargetPile", String.valueOf(i)), 0, size, false, null);
                    pileList.add(pile);
                    pool.removeAll(pile);
                }

                pileList.add(pool);
                p.getGame().getAction().notifyOfValue(sa, p, pileList.toString(), p);
                record.put(p, pileList);
            }
        }
        if (randomChosen) {
            for (Entry<Player, List<CardCollectionView>> ev : record.entrySet()) {
                CardCollectionView chosen = Aggregates.random(ev.getValue());
                for (Card c : chosen) {
                    source.addRemembered(c);
                }
            }

            SpellAbility sub = sa.getAdditionalAbility("ChosenPile");
            if (sub != null) {
                AbilityUtils.resolve(sub);
            }
            source.clearRemembered();
        }
    }
}
