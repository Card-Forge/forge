package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Iterables;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;

public class RearrangeTopOfLibraryEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */

    @Override
    protected String getStackDescription(SpellAbility sa) {
        int numCards = 0;
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        boolean shuffle = false;
        Card host = sa.getHostCard();

        numCards = AbilityUtils.calculateAmount(host, sa.getParam("NumCards"), sa);
        shuffle = sa.hasParam("MayShuffle");

        final StringBuilder ret = new StringBuilder();
        ret.append("Look at the top ");
        ret.append(numCards);
        ret.append(" cards of ");
        for (final Player p : tgtPlayers) {
            ret.append(Lang.getInstance().getPossesive(p.getName()));
            ret.append(" & ");
        }
        ret.delete(ret.length() - 3, ret.length());

        ret.append(" library. Then put them back in any order.");

        if (shuffle) {
            ret.append("You may have ");
            if (tgtPlayers.size() > 1) {
                ret.append("those");
            } else {
                ret.append("that");
            }

            ret.append(" player shuffle their library.");
        }

        return ret.toString();
    }

    /**
     * <p>
     * rearrangeTopOfLibraryResolve.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     */

    @Override
    public void resolve(SpellAbility sa) {
        Card host = sa.getHostCard();
        int numCards = AbilityUtils.calculateAmount(host, sa.getParam("NumCards"), sa);
        boolean shuffle = sa.hasParam("MayShuffle");

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            rearrangeTopOfLibrary(host, p, numCards, shuffle, sa);
        }
    }

    /**
     * use this when Human needs to rearrange the top X cards in a player's
     * library. You may also specify a shuffle when done
     *
     * @param src
     *            the source card
     * @param player
     *            the player to target
     * @param numCards
     *            the number of cards from the top to rearrange
     * @param mayshuffle
     *            a boolean.
     */
    private static void rearrangeTopOfLibrary(final Card src,
            final Player player, final int numCards, final boolean mayshuffle,
            final SpellAbility sa) {
        final Player activator = sa.hasParam("RearrangePlayer") ? Iterables.getFirst(AbilityUtils.getDefinedPlayers(src, sa.getParam("RearrangePlayer"), sa), null)
                : sa.getActivatingPlayer();
        if (activator == null) {
            return;
        }

        CardCollection topCards  = player.getTopXCardsFromLibrary(numCards);

        CardCollectionView orderedCards = activator.getController().orderMoveToZoneList(topCards, ZoneType.Library, sa);
        for (Card next : orderedCards) {
            player.getGame().getAction().moveToLibrary(next, 0, sa);
        }
        if (mayshuffle && activator.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoyouWantShuffleTheLibrary"), null)) {
            player.shuffle(sa);
        }
    }

}
