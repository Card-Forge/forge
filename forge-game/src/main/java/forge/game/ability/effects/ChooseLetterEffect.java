package forge.game.ability.effects;

import com.google.common.collect.Lists;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.*;

public class ChooseLetterEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        List<String> letters = Lists.newArrayList("A", "B", "C", "D", "E", "É", "F", "G", "H", "I", "J", "K", "L", "M", "N",
                "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");
        List<String> consonants = Lists.newArrayList("B", "C", "D", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q",
                "R", "S", "T", "V", "W", "X", "Y", "Z");
        List<String> choices = sa.hasParam("Consonant") ? consonants : letters;
        if (sa.hasParam("Exclude")) choices.removeAll(Arrays.asList(sa.getParam("Exclude").split(",")));
        int num = AbilityUtils.calculateAmount(card, sa.getParamOrDefault("Num", "1"), sa);
        final List<String> chosen = card.getController().getController().chooseLetter(num, sa, choices);
        card.setChosenLetters(chosen);
    }
}
