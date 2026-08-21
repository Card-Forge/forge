// REFORGE COMMANDER EXTENSION: Helm infinite-loop draw + default maxRepeat cap (loop safety net).
package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Expressions;
import forge.util.Localizer;

public class RepeatEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Repeat something. Somebody should really write a better StackDescription!";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        Card source = sa.getHostCard();

        // setup subability to repeat
        SpellAbility repeat = sa.getAdditionalAbility("RepeatSubAbility");

        Integer maxRepeat = null;
        if (sa.hasParam("MaxRepeat")) {
            maxRepeat = AbilityUtils.calculateAmount(source, sa.getParam("MaxRepeat"), sa);
            if (maxRepeat == 0) return; // do nothing if maxRepeat is 0. the next loop will execute at least once
        } else {
            // Uncounted "repeat until condition" loops get a hang-safety net.
            // ponytail: arbitrary cap like the stack limit; raise if a real card needs more.
            maxRepeat = 1000;
        }

        //execute repeat ability at least once
        int count = 0;
        do {
            AbilityUtils.resolve(repeat);
            count++;
            if (maxRepeat <= count) {
                // Helm of Obedience vs Graveyard-to-Library replacement effect:
                // the repeat can never terminate on its own, so declare a draw
                // instead of continuing in a half-resolved state. Other cards
                // hitting their (explicit or default) cap just stop — MaxRepeat
                // is also used as a legitimate counted-loop bound.
                if (source.getName().equals("Helm of Obedience")
                        && checkRepeatConditions(sa)) {
                    final Game game = sa.getActivatingPlayer().getGame();
                    for (final Player p : game.getPlayers()) {
                        p.loopDraw();
                    }
                    game.setGameOver(GameEndReason.Draw);
                }
                break;
            }
        } while (checkRepeatConditions(sa));
    }

    /**
     * <p>
     * checkRepeatConditions.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    private static boolean checkRepeatConditions(final SpellAbility sa) {
        //boolean doAgain = false;
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        if (game.isGameOver()) {
            return false;
        }

        if (sa.hasParam("RepeatPresent")) {
            final String repeatPresent = sa.getParam("RepeatPresent");
            String repeatCompare = sa.getParamOrDefault("RepeatCompare", "GE1");

            CardCollectionView list;
            if (sa.hasParam("RepeatDefined")) {
                list = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("RepeatDefined"), sa);
            } else {
                list = game.getCardsIn(ZoneType.Battlefield);
            }
            list = CardLists.getValidCards(list, repeatPresent, activator, sa.getHostCard(), sa);

            final String rightString = repeatCompare.substring(2);
            int right = AbilityUtils.calculateAmount(sa.getHostCard(), rightString, sa);

            final int left = list.size();

            if (!Expressions.compare(left, repeatCompare, right)) {
                return false;
            }
        }

        if (sa.hasParam("RepeatCheckSVar")) {
            String sVarOperator = "GE";
            String sVarOperand = "1";
            if (sa.hasParam("RepeatSVarCompare")) {
                sVarOperator = sa.getParam("RepeatSVarCompare").substring(0, 2);
                sVarOperand = sa.getParam("RepeatSVarCompare").substring(2);
            }
            final int svarValue = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("RepeatCheckSVar"), sa);
            final int operandValue = AbilityUtils.calculateAmount(sa.getHostCard(), sVarOperand, sa);

            if (!Expressions.compare(svarValue, sVarOperator, operandValue)) {
                return false;
            }
        }

        if (sa.hasParam("RepeatOptional")) {
            Player decider = sa.hasParam("RepeatOptionalDecider")
                    ? AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("RepeatOptionalDecider"), sa).get(0)
                    : activator;
            return decider.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantRepeatProcessAgain"), null);
        }

        return true;
    }
}
