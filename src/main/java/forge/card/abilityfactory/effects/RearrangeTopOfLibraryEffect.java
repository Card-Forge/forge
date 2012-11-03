package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class RearrangeTopOfLibraryEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */

    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        int numCards = 0;
        final List<Player> tgtPlayers = getTargetPlayers(sa, params);
        boolean shuffle = false;
        Card host = sa.getAbilityFactory().getHostCard();
    
        numCards = AbilityFactory.calculateAmount(host, params.get("NumCards"), sa);
        shuffle = params.containsKey("MayShuffle");
    
        final StringBuilder ret = new StringBuilder();
        ret.append("Look at the top ");
        ret.append(numCards);
        ret.append(" cards of ");
        for (final Player p : tgtPlayers) {
            ret.append(p.getName());
            ret.append("s");
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
    
            ret.append(" player shuffle his or her library.");
        }
    
        return ret.toString();
    }

    /**
     * <p>
     * rearrangeTopOfLibraryResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        int numCards = 0;
        Card host = sa.getAbilityFactory().getHostCard();
        boolean shuffle = false;

        if (sa.getActivatingPlayer().isHuman()) {
            final Target tgt = sa.getTarget();


            numCards = AbilityFactory.calculateAmount(host, params.get("NumCards"), sa);
            shuffle = params.containsKey("MayShuffle");

            for (final Player p : getTargetPlayers(sa, params)) {
                if ((tgt == null) || p.canBeTargetedBy(sa)) {
                    rearrangeTopOfLibrary(host, p, numCards, shuffle);
                }
            }
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
    private void rearrangeTopOfLibrary(final Card src, final Player player, final int numCards, final boolean mayshuffle) {
        final PlayerZone lib = player.getZone(ZoneType.Library);
        int maxCards = lib.size();
        // If library is smaller than N, only show that many cards
        maxCards = Math.min(maxCards, numCards);
        if (maxCards == 0) {
            return;
        }
        final List<Card> topCards = new ArrayList<Card>();
        // show top n cards:
        for (int j = 0; j < maxCards; j++) {
            topCards.add(lib.get(j));
        }

        List<Card> orderedCards = GuiChoose.getOrderChoices("Select order to Rearrange", "Top of Library", 0, topCards, null, src);
        for (int i = maxCards - 1; i >= 0; i--) {
            Card next = orderedCards.get(i);
            Singletons.getModel().getGame().getAction().moveToLibrary(next, 0);
        }
        if (mayshuffle) {
            if (GameActionUtil.showYesNoDialog(src, "Do you want to shuffle the library?")) {
                player.shuffle();
            }
        }
    }

}