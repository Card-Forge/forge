package forge.ai;

import java.util.ArrayList;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;


public class ComputerUtilAbility {
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

    public static ArrayList<SpellAbility> getSpellAbilities(final CardCollectionView l, final Player player) {
        final ArrayList<SpellAbility> spellAbilities = new ArrayList<SpellAbility>();
        for (final Card c : l) {
            for (final SpellAbility sa : c.getSpellAbilities()) {
                spellAbilities.add(sa);
            }
            if (c.isFaceDown() && c.isInZone(ZoneType.Exile) && c.mayPlay(player) != null) {
                for (final SpellAbility sa : c.getState(CardStateName.Original).getSpellAbilities()) {
                    spellAbilities.add(sa);
                }
            }
        }
        return spellAbilities;
    }

    public static ArrayList<SpellAbility> getOriginalAndAltCostAbilities(final ArrayList<SpellAbility> originList, final Player player) {
        final ArrayList<SpellAbility> newAbilities = new ArrayList<SpellAbility>();
        for (SpellAbility sa : originList) {
            sa.setActivatingPlayer(player);
            //add alternative costs as additional spell abilities
            newAbilities.add(sa);
            newAbilities.addAll(GameActionUtil.getAlternativeCosts(sa, player));
        }
    
        final ArrayList<SpellAbility> result = new ArrayList<SpellAbility>();
        for (SpellAbility sa : newAbilities) {
            sa.setActivatingPlayer(player);
            result.addAll(GameActionUtil.getOptionalCosts(sa));
        }
        return result;
    }
}
