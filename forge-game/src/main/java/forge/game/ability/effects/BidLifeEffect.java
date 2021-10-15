package forge.game.ability.effects;

import com.google.common.collect.Iterables;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class BidLifeEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Bid Life";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final FCollection<Player> bidPlayers = new FCollection<>();
        final int startBidding;
        if (sa.hasParam("StartBidding")) {
            String start = sa.getParam("StartBidding");
            if ("Any".equals(start)) {
                startBidding = activator.getController().announceRequirements(sa, Localizer.getInstance().getMessage("lblChooseStartingBid"));
            } else {
                startBidding = AbilityUtils.calculateAmount(host, start, sa);
            }
        } else {
            startBidding = 0;
        }
        
        if (sa.hasParam("OtherBidder")) {
            bidPlayers.add(activator);
            bidPlayers.addAll(AbilityUtils.getDefinedPlayers(host, sa.getParam("OtherBidder"), sa));
        } else {
            bidPlayers.addAll(activator.getGame().getPlayersInTurnOrder());
            int pSize = bidPlayers.size();
            // start with the activator
            while (bidPlayers.contains(activator) && !activator.equals(Iterables.getFirst(bidPlayers, null))) {
                bidPlayers.add(pSize - 1, bidPlayers.remove(0));
            }
        }

        boolean willBid = true;
        Player winner = activator;
        int bid = startBidding;
        while (willBid) {
            willBid = false;
            for (final Player p : bidPlayers) {
                final boolean result = p.getController().confirmBidAction(sa, PlayerActionConfirmMode.BidLife,
                        Localizer.getInstance().getMessage("lblDoYouWantTopBid") + bid, bid, winner);
                willBid |= result;
                if (result) { // a different choose number
                    bid += p.getController().chooseNumber(sa, Localizer.getInstance().getMessage("lblBidLife") + ":", 1, 9);
                    winner = p;
                    host.getGame().getAction().notifyOfValue(sa, p,  Localizer.getInstance().getMessage("lblTopBidWithValueLife", String.valueOf(bid)), p);
                }
            }
        }
        
        host.setChosenNumber(bid);
        host.addRemembered(winner);
        final SpellAbility action = sa.getAdditionalAbility("BidSubAbility");
        if (action != null) {
            AbilityUtils.resolve(action);
        }
        host.clearRemembered();
    }
}
