package forge.game.ability.effects;

import forge.card.MagicColor;
import forge.deck.DeckRecognizer;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChooseColorEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a color");
        if (sa.hasParam("OrColors")) {
            sb.append(" or colors");
        }
        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();

        List<String> colorChoices = new ArrayList<>(MagicColor.Constant.ONLY_COLORS);
        if (sa.hasParam("Choices")) {
            String[] restrictedChoices = sa.getParam("Choices").split(",");
            colorChoices = Arrays.asList(restrictedChoices);
        }
        if (sa.hasParam("Exclude")) {
            for (String s : sa.getParam("Exclude").split(",")) {
                colorChoices.remove(s);
            }
        }

        for (Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                p = getNewChooser(sa, sa.getActivatingPlayer(), p);
            }
            List<String> chosenColors = new ArrayList<>();
            int cntMin = sa.hasParam("TwoColors") ? 2 : 1;
            int cntMax = sa.hasParam("TwoColors") ? 2 : sa.hasParam("OrColors") ? colorChoices.size() : 1;
            String prompt = null;
            if (cntMax == 1) {
                prompt = Localizer.getInstance().getMessage("lblChooseAColor");
            } else {
                if (cntMax > cntMin) {
                    if (cntMax >= MagicColor.NUMBER_OR_COLORS) {
                        prompt = Localizer.getInstance().getMessage("lblAtLastChooseNumColors", Lang.getNumeral(cntMin));
                    } else {
                        prompt = Localizer.getInstance().getMessage("lblChooseSpecifiedRangeColors", Lang.getNumeral(cntMin), Lang.getNumeral(cntMax));
                    }
                } else {
                    prompt = Localizer.getInstance().getMessage("lblChooseNColors", Lang.getNumeral(cntMax));
                }
            }
            Player noNotify = p;
            if (sa.hasParam("Random")) {
                String choice;
                for (int i=0; i<cntMin; i++) {
                    choice = Aggregates.random(colorChoices);
                    colorChoices.remove(choice);
                    chosenColors.add(choice);
                }
                noNotify = null;
            } else {
                chosenColors = p.getController().chooseColors(prompt, sa, cntMin, cntMax, colorChoices);
            }
            if (chosenColors.isEmpty()) {
                return;
            }
            card.setChosenColors(chosenColors);
            chosenColors = chosenColors.stream().map(DeckRecognizer::getLocalisedMagicColorName).collect(Collectors.toList());
            p.getGame().getAction().notifyOfValue(sa, p, Lang.joinHomogenous(chosenColors), noNotify);
        }
    }
}
