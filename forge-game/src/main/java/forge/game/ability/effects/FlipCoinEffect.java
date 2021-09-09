package forge.game.ability.effects;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventFlipCoin;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.util.Localizer;
import forge.util.MyRandom;

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
        	sb.append(" Targeting: ").append(tgts).append(".");
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
        String varName = sa.hasParam("SaveNumFlipsToSVar") ? sa.getParam("SaveNumFlipsToSVar") : "X";
        boolean victory = false;
        int amount = 1;
        if (sa.hasParam("Amount")) {
            amount = AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa);
        }

        if ((!noCall) && (amount == 1)) {
            flipMultiplier = getFlipMultiplier(caller.get(0));
            victory = flipCoinCall(caller.get(0), sa, flipMultiplier, varName);
        }

        final boolean rememberResult = sa.hasParam("RememberResult");

        for (final Player flipper : playersToFlip) {
            if (noCall) {
                flipMultiplier = getFlipMultiplier(flipper);

                int countHeads = 0;
                int countTails = 0;

                for (int i = 0; i < amount; ++i) {
                    final boolean resultIsHeads = flipCoinNoCall(sa, flipper, flipMultiplier, varName);

                    if (resultIsHeads) {
                        countHeads++;
                    } else {
                        countTails++;
                    }

                    if (rememberResult) {
                        host.addFlipResult(flipper, resultIsHeads ? "Heads" : "Tails");
                    }
                }
                if (countHeads > 0) {
                    SpellAbility sub = sa.getAdditionalAbility("HeadsSubAbility");
                    if (sub != null) {
                        if (sa.hasParam("Amount")) {
                            sub.setSVar(varName, "Number$" + countHeads);
                        }
                        AbilityUtils.resolve(sub);
                    }
                }
                if (countTails > 0) {
                    SpellAbility sub = sa.getAdditionalAbility("TailsSubAbility");
                    if (sub != null) {
                        if (sa.hasParam("Amount")) {
                            sub.setSVar(varName, "Number$" + countTails);
                        }
                        AbilityUtils.resolve(sub);
                    }
                }
            } else if (amount > 1) {
                flipMultiplier = getFlipMultiplier(flipper);

                int countWins = 0;
                int countLosses = 0;

                for (int i = 0; i < amount; ++i) {
                    final boolean win = flipCoinCall(caller.get(0), sa, flipMultiplier, varName);

                    if (win) {
                        countWins++;
                    } else {
                        countLosses++;
                    }
                }
                if (countWins > 0) {
                    SpellAbility sub = sa.getAdditionalAbility("WinSubAbility");
                    if (sub != null) {
                        sub.setSVar("Wins", "Number$" + countWins);
                        AbilityUtils.resolve(sub);
                    }
                }
                if (countLosses > 0) {
                    SpellAbility sub = sa.getAdditionalAbility("LoseSubAbility");
                    if (sub != null) {
                        sub.setSVar("Losses", "Number$" + countLosses);
                        AbilityUtils.resolve(sub);
                    }
                }
                if (sa.hasParam("RememberNumber")) {
                    String toRemember = sa.getParam("RememberNumber");
                    if (toRemember.startsWith("Win")) {
                        host.addRemembered(countWins);
                    } else if (toRemember.startsWith("Loss")) {
                        host.addRemembered(countLosses);
                    }
                }
            } else {
                if (victory) {
                    if (sa.getParam("RememberWinner") != null) {
                        host.addRemembered(flipper);
                    }

                    if (sa.hasAdditionalAbility("WinSubAbility")) {
                        AbilityUtils.resolve(sa.getAdditionalAbility("WinSubAbility"));
                    }
                    // runParams.put("Won","True");
                } else {
                    if (sa.getParam("RememberLoser") != null) {
                        host.addRemembered(flipper);
                    }

                    if (sa.hasAdditionalAbility("LoseSubAbility")) {
                        AbilityUtils.resolve(sa.getAdditionalAbility("LoseSubAbility"));
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
    public boolean flipCoinNoCall(final SpellAbility sa, final Player flipper, final int multiplier, final String varName) {
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
            flipper.getGame().getAction().notifyOfValue(sa, flipper, result ? Localizer.getInstance().getMessage("lblHeads") : Localizer.getInstance().getMessage("lblTails"), null);
        } while (sa.hasParam("FlipUntilYouLose") && result != false);
        
        if (sa.hasParam("FlipUntilYouLose") && sa.hasAdditionalAbility("LoseSubAbility")) {
            sa.getAdditionalAbility("LoseSubAbility").setSVar(varName, "Number$" + numSuccesses);
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
        String varName = sa.hasParam("SaveNumFlipsToSVar") ? sa.getParam("SaveNumFlipsToSVar") : "X";
        return flipCoinCall(caller, sa, multiplier, varName);
    }
    public static boolean flipCoinCall(final Player caller, final SpellAbility sa, final int multiplier, final String varName) {
        boolean wonFlip = false;
        int numSuccesses = 0;

        do {
            Set<Boolean> flipResults = new HashSet<>();
            final boolean choice = caller.getController().chooseBinary(sa, sa.getHostCard().getName() + " - " + Localizer.getInstance().getMessage("lblCallCoinFlip"), PlayerController.BinaryChoiceType.HeadsOrTails);
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

            caller.getGame().getAction().notifyOfValue(sa, caller, wonFlip ? Localizer.getInstance().getMessage("lblWin") : Localizer.getInstance().getMessage("lblLose"), null);

            // Run triggers
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Player, caller);
            runParams.put(AbilityKey.Result, wonFlip);
            caller.getGame().getTriggerHandler().runTrigger(TriggerType.FlippedCoin, runParams, false);
        } while (sa.hasParam("FlipUntilYouLose") && wonFlip);
        
        if (sa.hasParam("FlipUntilYouLose") && sa.hasAdditionalAbility("LoseSubAbility")) {
            sa.getAdditionalAbility("LoseSubAbility").setSVar(varName, "Number$" + numSuccesses);
        }

        return wonFlip;
    }

    public static int getFlipMultiplier(final Player flipper) {
        String str = "If you would flip a coin, instead flip two coins and ignore one.";
        return 1 + flipper.getKeywords().getAmount(str);
    }
}
