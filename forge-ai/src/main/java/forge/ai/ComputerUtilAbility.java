package forge.ai;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import java.util.Iterator;

public class ComputerUtilAbility {
    public static CardCollection getAvailableLandsToPlay(final Game game, final Player player) {
        if (!game.getStack().isEmpty() || !game.getPhaseHandler().getPhase().isMain()) {
            return null;
        }
        final CardCollection hand = new CardCollection(player.getCardsIn(ZoneType.Hand));
        hand.addAll(player.getCardsIn(ZoneType.Exile));
        CardCollection landList = CardLists.filter(hand, Presets.LANDS);

        //filter out cards that can't be played
        landList = CardLists.filter(landList, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (!c.getSVar("NeedsToPlay").isEmpty()) {
                    final String needsToPlay = c.getSVar("NeedsToPlay");
                    CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), needsToPlay.split(","), c.getController(), c, null);
                    if (list.isEmpty()) {
                        return false;
                    }
                }
                return player.canPlayLand(c);
            }
        });

        final CardCollection landsNotInHand = new CardCollection(player.getCardsIn(ZoneType.Graveyard));
        landsNotInHand.addAll(game.getCardsIn(ZoneType.Exile));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            landsNotInHand.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        for (final Card crd : landsNotInHand) {
            if (!(crd.isLand() || (crd.isFaceDown() && crd.getState(CardStateName.Original).getType().isLand()))) {
                continue;
            }
            if (!crd.mayPlay(player).isEmpty()) {
                landList.add(crd);
            }
        }
        if (landList.isEmpty()) {
            return null;
        }
        return landList;
    }

    public static CardCollection getAvailableCards(final Game game, final Player player) {
        CardCollection all = new CardCollection(player.getCardsIn(ZoneType.Hand));

        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        all.addAll(player.getCardsIn(ZoneType.Command));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            all.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        for(Player p : game.getPlayers()) {
            all.addAll(p.getCardsIn(ZoneType.Exile));
            all.addAll(p.getCardsIn(ZoneType.Battlefield));
        }
        return all;
    }

    public static List<SpellAbility> getSpellAbilities(final CardCollectionView l, final Player player) {
        final List<SpellAbility> spellAbilities = new ArrayList<SpellAbility>();
        for (final Card c : l) {
            for (final SpellAbility sa : c.getSpellAbilities()) {
                spellAbilities.add(sa);
            }
            if (c.isFaceDown() && c.isInZone(ZoneType.Exile) && !c.mayPlay(player).isEmpty()) {
                for (final SpellAbility sa : c.getState(CardStateName.Original).getSpellAbilities()) {
                    spellAbilities.add(sa);
                }
            }
        }
        return spellAbilities;
    }

    public static List<SpellAbility> getOriginalAndAltCostAbilities(final List<SpellAbility> originList, final Player player) {
        final List<SpellAbility> newAbilities = new ArrayList<SpellAbility>();
        for (SpellAbility sa : originList) {
            sa.setActivatingPlayer(player);
            //add alternative costs as additional spell abilities
            newAbilities.add(sa);
            newAbilities.addAll(GameActionUtil.getAlternativeCosts(sa, player));
        }
    
        final List<SpellAbility> result = new ArrayList<SpellAbility>();
        for (SpellAbility sa : newAbilities) {
            sa.setActivatingPlayer(player);
            result.addAll(GameActionUtil.getOptionalCosts(sa));
        }
        return result;
    }

    public static SpellAbility getTopSpellAbilityOnStack(Game game, SpellAbility sa) {
        Iterator<SpellAbilityStackInstance> it = game.getStack().iterator();

        if (!it.hasNext()) {
            return null;
        }

        SpellAbility tgtSA = it.next().getSpellAbility(true);
        // Grab the topmost spellability that isn't this SA and use that for comparisons
        if (sa.equals(tgtSA) && game.getStack().size() > 1) {
            if (!it.hasNext()) {
                return null;
            }
            tgtSA = it.next().getSpellAbility(true);
        }
        return tgtSA;
    }

    public static Card getAbilitySource(SpellAbility sa) {
        return sa.getOriginalHost() != null ? sa.getOriginalHost() : sa.getHostCard();
    }

    public static String getAbilitySourceName(SpellAbility sa) {
        return sa.getOriginalHost() != null ? sa.getOriginalHost().getName() : sa.getHostCard() != null ? sa.getHostCard().getName() : "";
    }
}
