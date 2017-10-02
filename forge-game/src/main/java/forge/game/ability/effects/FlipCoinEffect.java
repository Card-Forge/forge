package forge.game.ability.effects;

import com.google.common.collect.Maps;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlipCoinEffect extends SpellAbilityEffect {

    public static boolean[] BOTH_CHOICES = new boolean[] {false, true};

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
                    AbilitySub sub = sa.getAdditionalAbility("HeadsSubAbility");
                    if (sub != null) {
                        AbilityUtils.resolve(sub);
                    }
                } else {
                    AbilitySub sub = sa.getAdditionalAbility("TailsSubAbility");
                    if (sub != null) {
                        AbilityUtils.resolve(sub);
                    }
                }
            } else {
                if (victory) {
                    if (sa.getParam("RememberWinner") != null) {
                        host.addRemembered(host);
                    }
                    AbilitySub sub = sa.getAdditionalAbility("WinSubAbility");
                    if (sub != null) {
                        AbilityUtils.resolve(sub);
                    }
                    // runParams.put("Won","True");
                } else {
                    if (sa.getParam("RememberLoser") != null) {
                        host.addRemembered(host);
                    }

                    AbilitySub sub = sa.getAdditionalAbility("LoseSubAbility");
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
     * @param sa   the source card.
     * @param flipper  the player flipping the coin.
     * @param multiplier
     * @return a boolean.
     */
    public boolean flipCoinNoCall(final SpellAbility sa, final Player flipper, final int multiplier) {
        boolean result = false;
        int numSuccesses = 0;

        do {
            Set<Boolean> flipResults = new HashSet<>();
            for (int i = 0; i < multiplier; i++) {
                flipResults.add(MyRandom.getRandom().nextBoolean());
            }
            flipper.getGame().fireEvent(new GameEventFlipCoin());
            result = flipResults.size() == 1 ? flipResults.iterator().next() : flipper.getController().chooseFlipResult(sa, flipper, BOTH_CHOICES, false);
            if (result) {
                numSuccesses++;
            }
            flipper.getGame().getAction().nofityOfValue(sa, flipper, result ? "heads" : "tails", null);
        } while (sa.hasParam("FlipUntilYouLose") && result != false);
        
        if (sa.hasParam("FlipUntilYouLose")) {
            sa.getAdditionalAbility("LoseSubAbility").setSVar(sa.hasParam("SaveNumFlipsToSVar") ? sa.getParam("SaveNumFlipsToSVar") : "X", "Number$" + numSuccesses);
        }

        return result;
    }

    /**
     * <p>
     * flipCoinCall.
     * </p>
     * 
     * @param caller
     * @param sa
     * @param multiplier
     * @return a boolean.
     */
    public static boolean flipCoinCall(final Player caller, final SpellAbility sa, final int multiplier) {
        boolean wonFlip = false;
        int numSuccesses = 0;

        do {
            Set<Boolean> flipResults = new HashSet<>();
            final boolean choice = caller.getController().chooseBinary(sa, sa.getHostCard().getName() + " - Call coin flip", PlayerController.BinaryChoiceType.HeadsOrTails);
            for (int i = 0; i < multiplier; i++) {
                flipResults.add(MyRandom.getRandom().nextBoolean());
            }
            // Play the Flip A Coin sound
            caller.getGame().fireEvent(new GameEventFlipCoin());
            boolean result = flipResults.size() == 1 ? flipResults.iterator().next() : caller.getController().chooseFlipResult(sa, caller, BOTH_CHOICES, true);
            wonFlip = result == choice;

            if (wonFlip) {
                numSuccesses++;
            }

            caller.getGame().getAction().nofityOfValue(sa, caller, wonFlip ? "win" : "lose", null);

            // Run triggers
            Map<String,Object> runParams = Maps.newHashMap();
            runParams.put("Player", caller);
            runParams.put("Result", Boolean.valueOf(wonFlip));
            caller.getGame().getTriggerHandler().runTrigger(TriggerType.FlippedCoin, runParams, false);
        } while (sa.hasParam("FlipUntilYouLose") && wonFlip);
        
        if (sa.hasParam("FlipUntilYouLose")) {
            sa.getAdditionalAbility("LoseSubAbility").setSVar(sa.hasParam("SaveNumFlipsToSVar") ? sa.getParam("SaveNumFlipsToSVar") : "X", "Number$" + numSuccesses);
        }

        return wonFlip;
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
