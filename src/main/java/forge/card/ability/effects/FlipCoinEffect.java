package forge.card.ability.effects;

import java.util.HashMap;
import java.util.List;
import forge.Card;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.TriggerType;
import forge.game.event.GameEventFlipCoin;
import forge.game.player.Player;
import forge.gui.GuiChoose;
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

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Player player = host.getController();
        int flipMultiplier = 1; // For multiple copies of Krark's Thumb

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
            flipMultiplier = getFilpMultiplier(caller.get(0));
            victory = flipCoinCall(caller.get(0), host, flipMultiplier);
        }

        // Run triggers
        HashMap<String,Object> runParams = new HashMap<String,Object>();
        runParams.put("Player", caller.get(0));
        runParams.put("Result", (Boolean) victory);
        player.getGame().getTriggerHandler().runTrigger(TriggerType.FlippedCoin, runParams, false);

        final boolean rememberResult = sa.hasParam("RememberResult");

        for (final Player flipper : playersToFlip) {
            if (noCall) {
                flipMultiplier = getFilpMultiplier(flipper);
                final boolean resultIsHeads = FlipCoinEffect.flipCoinNoCall(sa.getSourceCard(), flipper, flipMultiplier);
                if (rememberResult) {
                    host.addFlipResult(flipper, resultIsHeads ? "Heads" : "Tails");
                }

                if (resultIsHeads) {
                    if (sa.hasParam("HeadsSubAbility")) {
                        final SpellAbility heads = AbilityFactory.getAbility(host.getSVar(sa.getParam("HeadsSubAbility")), host);
                        heads.setActivatingPlayer(player);
                        ((AbilitySub) heads).setParent(sa);

                        AbilityUtils.resolve(heads);
                    }
                } else {
                    if (sa.hasParam("TailsSubAbility")) {
                        final SpellAbility tails = AbilityFactory.getAbility(host.getSVar(sa.getParam("TailsSubAbility")), host);
                        tails.setActivatingPlayer(player);
                        ((AbilitySub) tails).setParent(sa);

                        AbilityUtils.resolve(tails);
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

                        AbilityUtils.resolve(win);
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

                        AbilityUtils.resolve(lose);
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
     * @param multiplier
     * @return a boolean.
     */
    public static boolean flipCoinNoCall(final Card source, final Player flipper, final int multiplier) {
        String[] results = new String[multiplier];
        String result;
        for (int i = 0; i < multiplier; i++) {
            final boolean resultIsHeads = MyRandom.getRandom().nextBoolean();
            flipper.getGame().fireEvent(new GameEventFlipCoin());
            results[i] = resultIsHeads ? " heads." : " tails.";
        }
        if (multiplier == 1) {
            result = results[0];
        } else {
            result = flipper.getController().chooseFilpResult(source, flipper, results, false);
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(flipper.getName());
        sb.append("'s flip comes up");
        sb.append(result);
        GuiDialog.message(sb.toString(), source + " Flip result:");
        return result.equals(" heads.");
    }

    /**
     * <p>
     * flipCoinCall.
     * </p>
     * 
     * @param caller
     *            a {@link forge.game.player.Player} object.
     * @param source
     *            a {@link forge.Card} object.
     * @param multiplier
     * @return a boolean.
     */
    public static boolean flipCoinCall(final Player caller, final Card source, final int multiplier) {
        String choice;
        final String[] choices = { "heads", "tails" };
        String[] results = new String[multiplier];
        for (int i = 0; i < multiplier; i++) {
            // Play the Flip A Coin sound
            caller.getGame().fireEvent(new GameEventFlipCoin());
            final boolean flip = MyRandom.getRandom().nextBoolean();
            if (caller.isHuman()) {
                choice = GuiChoose.one(source.getName() + " - Call coin flip", choices);
            } else {
                choice = choices[MyRandom.getRandom().nextInt(2)];
            }
            final boolean winFlip = flip == choice.equals(choices[0]);
            final String winMsg = winFlip ? " wins flip." : " loses flip.";
            results[i] = winMsg;
        }
        String result;
        if (multiplier == 1) {
            result = results[0];
        } else {
            result = caller.getController().chooseFilpResult(source, caller, results, true);
        }
        
        GuiDialog.message(source.getName() + " - " + caller + result, source.getName());
        
        return result.equals(" wins flip.");
    }

    public static int getFilpMultiplier(final Player flipper) {
        int i = 0;
        for (String kw : flipper.getKeywords()) {
            if (kw.startsWith("If you would flip a coin")) {
                i++;
            }
        }
        return 1 << i;
    }

}
