package forge.card.ability.effects;

import java.util.List;

import javax.swing.JOptionPane;

import forge.Card;
import forge.Singletons;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.event.FlipCoinEvent;
import forge.game.player.Player;
import forge.gui.GuiDialog;
import forge.util.MyRandom;

public class FlipCoinEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Player player = host.getController();

        final StringBuilder sb = new StringBuilder();

        sb.append(player).append(" flips a coin.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Player player = host.getController();

        final List<Player> playersToFlip = AbilityUtils.getDefinedPlayers(host, sa.getParam("Flipper"), sa);
        if (playersToFlip.isEmpty()) {
            playersToFlip.add(sa.getActivatingPlayer());
        }

        final List<Player> caller = AbilityUtils.getDefinedPlayers(host, sa.getParam("Caller"), sa);
        if (caller.isEmpty()) {
            caller.add(player);
        }

        final boolean noCall = sa.hasParam("NoCall");
        boolean victory = false;
        if (!noCall) {
            victory = GuiDialog.flipCoin(caller.get(0), host);
        }

        // Run triggers
        // HashMap<String,Object> runParams = new HashMap<String,Object>();
        // runParams.put("Player", player);
        final boolean rememberResult = sa.hasParam("RememberResult");

        for (final Player flipper : playersToFlip) {
            if (noCall) {
                final boolean resultIsHeads = FlipCoinEffect.flipCoinNoCall(sa.getSourceCard(), flipper);
                if (rememberResult) {
                    host.addFlipResult(flipper, resultIsHeads ? "Heads" : "Tails");
                }

                if (resultIsHeads) {
                    if (sa.hasParam("HeadsSubAbility")) {
                        final SpellAbility heads = AbilityFactory.getAbility(host.getSVar(sa.getParam("HeadsSubAbility")), host);
                        heads.setActivatingPlayer(player);
                        ((AbilitySub) heads).setParent(sa);

                        AbilityUtils.resolve(heads, false);
                    }
                } else {
                    if (sa.hasParam("TailsSubAbility")) {
                        final SpellAbility tails = AbilityFactory.getAbility(host.getSVar(sa.getParam("TailsSubAbility")), host);
                        tails.setActivatingPlayer(player);
                        ((AbilitySub) tails).setParent(sa);

                        AbilityUtils.resolve(tails, false);
                    }
                }
            } else {
                if (victory) {
                    if (sa.getParam("RememberWinner") != null) {
                        host.addRemembered(host);
                    }
                    if (sa.hasParam("WinSubAbility")) {
                        final SpellAbility win = AbilityFactory.getAbility(host.getSVar(sa.getParam("WinSubAbility")), host);
                        win.setActivatingPlayer(player);
                        ((AbilitySub) win).setParent(sa);

                        AbilityUtils.resolve(win, false);
                    }
                    // runParams.put("Won","True");
                } else {
                    if (sa.getParam("RememberLoser") != null) {
                        host.addRemembered(host);
                    }
                    if (sa.hasParam("LoseSubAbility")) {
                        final SpellAbility lose = AbilityFactory.getAbility(host.getSVar(sa.getParam("LoseSubAbility")), host);
                        lose.setActivatingPlayer(player);
                        ((AbilitySub) lose).setParent(sa);

                        AbilityUtils.resolve(lose, false);
                    }
                    // runParams.put("Won","False");
                }
            }
        }

        // AllZone.getTriggerHandler().runTrigger("FlipsACoin",runParams);
    }

    /**
     * <p>
     * flipCoinNoCall  Flip a coin without any call.
     * </p>
     * 
     * @param source   the source card.
     * @param flipper  the player flipping the coin.
     * @return a boolean.
     */
    public static boolean flipCoinNoCall(final Card source, final Player flipper) {
        final boolean resultIsHeads = MyRandom.getRandom().nextBoolean();

        Singletons.getModel().getGame().getEvents().post(new FlipCoinEvent());
        final StringBuilder msgTitle = new StringBuilder();
        msgTitle.append(source);
        msgTitle.append(" Flip result:");
        final StringBuilder result = new StringBuilder();
        result.append(flipper.getName());
        result.append("'s flip comes up");
        result.append(resultIsHeads ? " heads." : " tails.");
        JOptionPane.showMessageDialog(null, result, msgTitle.toString(), JOptionPane.PLAIN_MESSAGE);

        return resultIsHeads;
    }

}
