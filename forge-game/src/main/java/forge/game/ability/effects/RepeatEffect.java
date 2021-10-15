package forge.game.ability.effects;

import forge.game.Game;
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

        if (repeat != null && !repeat.getHostCard().equals(source)) {
            // TODO: for some reason, the host card of the original additional SA is set to the cloned card when
            // the ability is copied (e.g. Clone Legion + Swarm Intelligence). Couldn't figure out why this happens,
            // so this hack is necessary for now to work around this issue.
            System.out.println("Warning: RepeatSubAbility had the wrong host set (potentially after cloning the root SA), attempting to correct...");
            repeat.setHostCard(source);
        }

        Integer maxRepeat = null;
        if (sa.hasParam("MaxRepeat")) {
            maxRepeat = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("MaxRepeat"), sa);
            if (maxRepeat.intValue() == 0) return; // do nothing if maxRepeat is 0. the next loop will execute at least once
        }

        //execute repeat ability at least once
        int count = 0;
        do {
            AbilityUtils.resolve(repeat);
            count++;
            if (maxRepeat != null && maxRepeat <= count) {
                // TODO Replace Infinite Loop Break with a game draw. Here are the scenarios that can cause this:
                // Helm of Obedience vs Graveyard to Library replacement effect

                if (source.getName().equals("Helm of Obedience")) {
                StringBuilder infLoop = new StringBuilder(sa.getHostCard().toString());
                    infLoop.append(" - To avoid an infinite loop, this repeat has been broken ");
                    infLoop.append(" and the game will now continue in the current state, ending the loop early. ");
                    infLoop.append("Once Draws are available this probably should change to a Draw.");
                    System.out.println(infLoop.toString());
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

        if (sa.hasParam("RepeatPresent")) {
            final String repeatPresent = sa.getParam("RepeatPresent");

            String repeatCompare = "GE1";
            if (sa.hasParam("RepeatCompare")) {
                repeatCompare = sa.getParam("RepeatCompare");
            }

            CardCollectionView list;
            if (sa.hasParam("RepeatDefined")) {
                list = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("RepeatDefined"), sa);
            } else {
                list = game.getCardsIn(ZoneType.Battlefield);
            }
            list = CardLists.getValidCards(list, repeatPresent.split(","), sa.getActivatingPlayer(), sa.getHostCard(), sa);

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
                    : sa.getActivatingPlayer();
            return decider.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantRepeatProcessAgain"));
        }

        return true;
    }
}
