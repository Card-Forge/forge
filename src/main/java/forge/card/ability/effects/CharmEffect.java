package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;
import forge.Card;
import forge.card.ability.AbilityFactory;
import forge.card.ability.SpellAbilityEffect;
import forge.card.ability.ai.CharmAi;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.gui.GuiChoose;

public class CharmEffect extends SpellAbilityEffect {

    public static List<AbilitySub> makePossibleOptions(final SpellAbility sa) {
        final Card source = sa.getSourceCard();

        final String[] saChoices = sa.getParam("Choices").split(",");
        List<AbilitySub> choices = new ArrayList<AbilitySub>();
        for (final String saChoice : saChoices) {
            final String ab = source.getSVar(saChoice);
            choices.add((AbilitySub) AbilityFactory.getAbility(ab, source));
        }
        return choices;
    }

    @Override
    public void resolve(SpellAbility sa) {
        // all chosen modes have been chained as subabilities to this sa.
        // so nothing to do in this resolve
    }


    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        // nothing stack specific for Charm

        return sb.toString();
    }

    public static void makeChoices(SpellAbility sa) {
        //this resets all previous choices
        sa.setSubAbility(null);

        final int num = Integer.parseInt(sa.hasParam("CharmNum") ? sa.getParam("CharmNum") : "1");
        final int min = sa.hasParam("MinCharmNum") ? Integer.parseInt(sa.getParam("MinCharmNum")) : num;
        final List<AbilitySub> choices = makePossibleOptions(sa);

        List<AbilitySub> chosen = null;
        Card source = sa.getSourceCard();
        Player activator = sa.getActivatingPlayer();
        Player chooser = sa.getActivatingPlayer();

        if (sa.hasParam("Chooser")) {
            // Three modal cards require you to choose a player to make the modal choice'
            // Two of these also reference the chosen player during the spell effect
            String choose = sa.getParam("Chooser");
            List<Player> opponents = activator.getOpponents();
            int numOpps = opponents.size();
            if (numOpps == 1) {
                chooser = opponents.get(0);
            } else {
                if (activator.isComputer()) {
                    chooser = CharmAi.determineOpponentChooser((AIPlayer)activator, sa, opponents);
                } else {
                    chooser = GuiChoose.one("Choose an opponent", opponents);
                }
            }
            source.setChosenPlayer(chooser);
        }

        if (chooser.isHuman()) {
            String modeTitle = String.format("%s activated %s - Choose a mode", activator, source);
            chosen = new ArrayList<AbilitySub>();
            for (int i = 0; i < num; i++) {
                AbilitySub a;
                if (i < min) {
                    a = GuiChoose.one(modeTitle, choices);
                } else {
                    a = GuiChoose.oneOrNone(modeTitle, choices);
                }
                if (null == a) {
                    break;
                }

                choices.remove(a);
                chosen.add(a);
            }
        } else {
            chosen = CharmAi.chooseOptionsAi((AIPlayer)chooser, sa.isTrigger(), choices, num, min, !chooser.equals(activator));
        }

        chainAbilities(sa, chosen);
    }

    private static void chainAbilities(SpellAbility sa, List<AbilitySub> chosen) {
        SpellAbility saDeepest = sa;
        while (saDeepest.getSubAbility() != null) {
            saDeepest = saDeepest.getSubAbility();
        }

        for (AbilitySub sub : chosen) {
            saDeepest.setSubAbility(sub);
            sub.setActivatingPlayer(saDeepest.getActivatingPlayer());
            sub.setParent(saDeepest);

            // to chain the next one
            saDeepest = sub;
        }
    }


}
