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
            victory = flipCoinCall(caller.get(0), sa, flipMultiplier);
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
                final boolean resultIsHeads = flipCoinNoCall(sa, flipper, flipMultiplier);
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
    public boolean flipCoinNoCall(final SpellAbility sa, final Player flipper, final int multiplier) {
        boolean[] results = new boolean[multiplier];
        for (int i = 0; i < multiplier; i++) {
            final boolean resultIsHeads = MyRandom.getRandom().nextBoolean();
            flipper.getGame().fireEvent(new GameEventFlipCoin());
            results[i] = resultIsHeads;
        }
        boolean result = multiplier == 1 ? results[0] : flipper.getController().chooseFilpResult(sa, flipper, results, false);
        
        flipper.getGame().getAction().nofityOfValue(sa, flipper, result ? "heads" : "tails", null);
        return result;
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
    public boolean flipCoinCall(final Player caller, final SpellAbility sa, final int multiplier) {
        boolean [] results = new boolean [multiplier];
        for (int i = 0; i < multiplier; i++) {
            final boolean choice = caller.getController().chooseBinary(sa, sa.getSourceCard().getName() + " - Call coin flip", true);
            // Play the Flip A Coin sound
            caller.getGame().fireEvent(new GameEventFlipCoin());
            final boolean flip = MyRandom.getRandom().nextBoolean();
            results[i] = flip == choice;
        }
        boolean result = multiplier == 1 ? results[0] : caller.getController().chooseFilpResult(sa, caller, results, true);
        
        caller.getGame().getAction().nofityOfValue(sa, caller, result ? "win" : "lose", null);
        return result;
    }

    public int getFilpMultiplier(final Player flipper) {
        int i = 0;
        for (String kw : flipper.getKeywords()) {
            if (kw.startsWith("If you would flip a coin")) {
                i++;
            }
        }
        return 1 << i;
    }

}
