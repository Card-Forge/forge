package forge.game.ability.effects;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventFlipCoin;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityFlipCoinMod;
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

        final List<Player> playersToFlip = AbilityUtils.getDefinedPlayers(host, sa.getParam("Flipper"), sa);
        //final List<Player> caller = AbilityUtils.getDefinedPlayers(host, sa.getParam("Caller"), sa);

        final boolean noCall = sa.hasParam("NoCall");
        final boolean forEachPlayer = sa.hasParam("ForEachPlayer");
        String varName = sa.getParamOrDefault("SaveNumFlipsToSVar", "X");
        int amount = 1;
        if (sa.hasParam("Amount")) {
            amount = AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa);
        }

        for (final Player flipper : playersToFlip) {
            if (noCall) {
                int countHeads = flipCoins(flipper, sa, amount);
                int countTails = Math.abs(countHeads - amount);
                if (countHeads > 0) {
                    if (sa.hasParam("RememberResult")) {
                        host.addFlipResult(flipper, "Heads");
                    }
                    SpellAbility sub = sa.getAdditionalAbility("HeadsSubAbility");
                    if (sub != null) {
                        if (sa.hasParam("Amount")) {
                            sub.setSVar(varName, "Number$" + countHeads);
                        }
                        AbilityUtils.resolve(sub);
                    }
                }
                if (countTails > 0) {
                    if (sa.hasParam("RememberResult")) {
                        host.addFlipResult(flipper, "Tails");
                    }
                    SpellAbility sub = sa.getAdditionalAbility("TailsSubAbility");
                    if (sub != null) {
                        if (sa.hasParam("Amount")) {
                            sub.setSVar(varName, "Number$" + countTails);
                        }
                        AbilityUtils.resolve(sub);
                    }
                }
            } else if (forEachPlayer) {
                int countWins = 0;
                int countLosses = 0;
                PlayerCollection wonFor = new PlayerCollection();
                PlayerCollection lostFor = new PlayerCollection();

                for (final Player p : AbilityUtils.getDefinedPlayers(host, sa.getParam("ForEachPlayer"), sa)) {
                    final String info = " (" + p.getName() +")";
                    final int win = flipCoins(flipper, sa, 1, info);

                    if (win > 0) {
                        countWins++;
                        wonFor.add(p);
                    } else {
                        countLosses++;
                        lostFor.add(p);
                    }
                }
                if (countWins > 0) {
                    SpellAbility sub = sa.getAdditionalAbility("WinSubAbility");
                    if (sub != null) {
                        List<Object> tempRemembered = Lists.newArrayList(host.getRemembered());
                        host.removeRemembered(tempRemembered);
                        host.addRemembered(wonFor);
                        sub.setSVar("Wins", "Number$" + countWins);
                        AbilityUtils.resolve(sub);
                        host.removeRemembered(wonFor);
                        host.addRemembered(tempRemembered);
                    }
                }
                if (countLosses > 0) {
                    SpellAbility sub = sa.getAdditionalAbility("LoseSubAbility");
                    if (sub != null) {
                        List<Object> tempRemembered = Lists.newArrayList(host.getRemembered());
                        host.removeRemembered(tempRemembered);
                        host.addRemembered(lostFor);
                        sub.setSVar("Losses", "Number$" + countLosses);
                        AbilityUtils.resolve(sub);
                        host.removeRemembered(lostFor);
                        host.addRemembered(tempRemembered);
                    }
                }
            } else {
                int countWins = flipCoins(flipper, sa, amount);
                int countLosses = Math.abs(countWins - amount);
                if (countWins > 0) {
                    if (sa.hasParam("RememberWinner")) {
                        host.addRemembered(flipper);
                    }
                    SpellAbility sub = sa.getAdditionalAbility("WinSubAbility");
                    if (sub != null) {
                        sub.setSVar("Wins", "Number$" + countWins);
                        AbilityUtils.resolve(sub);
                    }
                }
                if (countLosses > 0) {
                    if (sa.hasParam("RememberLoser")) {
                        host.addRemembered(flipper);
                    }
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
            }
        }
    }

    public static int flipCoins(final Player flipper, final SpellAbility sa, final int amount) {
        return flipCoins(flipper, sa, amount, "");
    }
    public static int flipCoins(final Player flipper, final SpellAbility sa, final int amount, final String info) {
        int multiplier = getFlipMultiplier(flipper);
        int result = 0;
        boolean won = false;
        do {
            Boolean fixedResult = StaticAbilityFlipCoinMod.fixedResult(flipper);
            for (int i = 0; i < amount; i++) {
                won = flipCoin(flipper, sa, multiplier, fixedResult, info);
                if (won) {
                    result++;
                }
            }
            // until is sequential
        }
        while (sa.hasParam("FlipUntilYouLose") && won);
        return result;
    }

    /**
     * <p>
     * flipCoinCall.
     * </p>
     *
     * @param flipper
     * @param sa
     * @param multiplier
     * @return a boolean.
     */
    private static boolean flipCoin(final Player flipper, final SpellAbility sa, int multiplier, final Boolean fixedResult, final String info) {
        Set<Boolean> flipResults = new HashSet<>();
        boolean noCall = sa.hasParam("NoCall");
        boolean choice = true;
        if (fixedResult != null) {
            flipResults.add(fixedResult);
        } else {
            // no reason to ask if result is fixed anyway
            if (!noCall) {
                choice = flipper.getController().chooseBinary(sa, sa.getHostCard().getName() + " - " + Localizer.getInstance().getMessage("lblCallCoinFlip") + info, PlayerController.BinaryChoiceType.HeadsOrTails);
            }

            for (int i = 0; i < multiplier; i++) {
                flipResults.add(MyRandom.getRandom().nextBoolean());
            }
        }

        boolean result = flipResults.size() == 1 ? flipResults.iterator().next() : flipper.getController().chooseFlipResult(sa, flipper, BOTH_CHOICES, true);
        boolean wonOrHeads = result == choice;

        String outcome;
        if (noCall) {
            outcome = wonOrHeads ? Localizer.getInstance().getMessage("lblHeads") : Localizer.getInstance().getMessage("lblTails");
        } else {
            outcome = wonOrHeads ? Localizer.getInstance().getMessage("lblWin") : Localizer.getInstance().getMessage("lblLose");
        }
        // Play the Flip A Coin sound
        flipper.getGame().fireEvent(new GameEventFlipCoin());
        flipper.getGame().getAction().notifyOfValue(sa, flipper, outcome, null);

        flipper.flip();

        if (!noCall || fixedResult != null) {
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(flipper);
            runParams.put(AbilityKey.Result, wonOrHeads);
            flipper.getGame().getTriggerHandler().runTrigger(TriggerType.FlippedCoin, runParams, false);
        }

        return wonOrHeads;
    }

    public static int getFlipMultiplier(final Player flipper) {
        String str = "If you would flip a coin, instead flip two coins and ignore one.";
        return 1 << flipper.getAmountOfKeyword(str);
    }
}
