package forge.game.ability.effects;

import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventFlipCoin;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.MyRandom;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

public class FlipCoinEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player player = host.getController();
        final List<GameObject> tgts = getTargets(sa);

        final StringBuilder sb = new StringBuilder();

        sb.append(player).append(" flips a coin.");
        if (tgts != null && !tgts.isEmpty()) {
        	sb.append(" Targeting: " + tgts + ".");
        }
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
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

        final boolean rememberResult = sa.hasParam("RememberResult");

        for (final Player flipper : playersToFlip) {
            if (noCall) {
                flipMultiplier = getFilpMultiplier(flipper);
                final boolean resultIsHeads = flipCoinNoCall(sa, flipper, flipMultiplier);
                if (rememberResult) {
                    host.addFlipResult(flipper, resultIsHeads ? "Heads" : "Tails");
                }

                if (resultIsHeads) {
                    AbilitySub sub = sa.getAdditonalAbility("HeadsSubAbility");
                    if (sub != null) {
                        AbilityUtils.resolve(sub);
                    }
                } else {
                    AbilitySub sub = sa.getAdditonalAbility("TailsSubAbility");
                    if (sub != null) {
                        AbilityUtils.resolve(sub);
                    }
                }
            } else {
                if (victory) {
                    if (sa.getParam("RememberWinner") != null) {
                        host.addRemembered(host);
                    }
                    AbilitySub sub = sa.getAdditonalAbility("WinSubAbility");
                    if (sub != null) {
                        AbilityUtils.resolve(sub);
                    }
                    // runParams.put("Won","True");
                } else {
                    if (sa.getParam("RememberLoser") != null) {
                        host.addRemembered(host);
                    }

                    AbilitySub sub = sa.getAdditonalAbility("LoseSubAbility");
                    if (sub != null) {
                        AbilityUtils.resolve(sub);
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
        boolean result = false;
        int numSuccesses = 0;

        do {
            boolean[] results = new boolean[multiplier];
            for (int i = 0; i < multiplier; i++) {
                final boolean resultIsHeads = MyRandom.getRandom().nextBoolean();
                flipper.getGame().fireEvent(new GameEventFlipCoin());
                results[i] = resultIsHeads;
            }
            result = multiplier == 1 ? results[0] : flipper.getController().chooseFlipResult(sa, flipper, results, false);
            if (result) {
                numSuccesses++;
            }
            flipper.getGame().getAction().nofityOfValue(sa, flipper, result ? "heads" : "tails", null);
        } while (sa.hasParam("FlipUntilYouLose") && result != false);
        
        if (sa.hasParam("FlipUntilYouLose")) {
            sa.getAdditonalAbility("LoseSubAbility").setSVar(sa.hasParam("SaveNumFlipsToSVar") ? sa.getParam("SaveNumFlipsToSVar") : "X", "Number$" + numSuccesses);
        }

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
     *            a {@link forge.game.card.Card} object.
     * @param multiplier
     * @return a boolean.
     */
    public static boolean flipCoinCall(final Player caller, final SpellAbility sa, final int multiplier) {
        boolean result = false;
        int numSuccesses = 0;

        do {
            boolean [] results = new boolean [multiplier];
            final boolean choice = caller.getController().chooseBinary(sa, sa.getHostCard().getName() + " - Call coin flip", PlayerController.BinaryChoiceType.HeadsOrTails);
            for (int i = 0; i < multiplier; i++) {
                // Play the Flip A Coin sound
                caller.getGame().fireEvent(new GameEventFlipCoin());
                final boolean flip = MyRandom.getRandom().nextBoolean();
                results[i] = flip == choice;
            }
            result = multiplier == 1 ? results[0] : caller.getController().chooseFlipResult(sa, caller, results, true);
            if (result) {
                numSuccesses++;
            }
            
            caller.getGame().getAction().nofityOfValue(sa, caller, result ? "win" : "lose", null);

            // Run triggers
            Map<String,Object> runParams = Maps.newHashMap();
            runParams.put("Player", caller);
            runParams.put("Result", Boolean.valueOf(result));
            caller.getGame().getTriggerHandler().runTrigger(TriggerType.FlippedCoin, runParams, false);
        } while (sa.hasParam("FlipUntilYouLose") && result != false);
        
        if (sa.hasParam("FlipUntilYouLose")) {
            sa.getAdditonalAbility("LoseSubAbility").setSVar(sa.hasParam("SaveNumFlipsToSVar") ? sa.getParam("SaveNumFlipsToSVar") : "X", "Number$" + numSuccesses);
        }

        return result;
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
