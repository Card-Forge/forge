package forge.game.ability.effects;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;


public class VillainousChoiceEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final List<SpellAbility> abilities = Lists.newArrayList(sa.getAdditionalAbilityList("Choices"));
        final int amount = extractAmount(sa);
        String prompt = sa.getParamOrDefault("ChoicePrompt", "Villainous Choice by " + sa.getActivatingPlayer());
        Card source = sa.getHostCard();

        for (Player p : getDefinedPlayersOrTargeted(sa)) {
            int choiceAmount = p.getAdditionalVillainousChoices() + 1;
            source.addRemembered(p);

            List<SpellAbility> saToRemove = Lists.newArrayList();

            for (SpellAbility saChoice : abilities) {
                if (saChoice.getRestrictions() != null && !saChoice.getRestrictions().checkOtherRestrictions(sa.getHostCard(), saChoice, sa.getActivatingPlayer())) {
                    saToRemove.add(saChoice);
                }
            }
            abilities.removeAll(saToRemove);

            // For the AI chooseSAForEffect really should take the least good ability. Currently it just takes the first
            List<SpellAbility> chosenSAs = Lists.newArrayList();
            for(int i = 0; i < choiceAmount; i++) {
                // This is a loop because you can choose the same abilities multiple times
                 chosenSAs.addAll(p.getController().chooseSpellAbilitiesForEffect(abilities, sa, prompt, amount, ImmutableMap.of()));
            }

            for (SpellAbility chosenSA : chosenSAs) {
                AbilityUtils.resolve(chosenSA);
            }
            source.removeRemembered(p);
        }
    }
}
