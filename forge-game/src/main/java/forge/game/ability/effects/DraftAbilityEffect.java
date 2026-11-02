package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;
import forge.util.MyRandom;

import java.util.*;

public class DraftAbilityEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player player = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa).get(0);

        final StringBuilder sb = new StringBuilder();

        sb.append(player).append(" drafts a ability for ").append(source.getDisplayName());

        return sb.toString();
    }


    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player player = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa).get(0);
        List<String> abilities = Arrays.asList(sa.getParam("Abilities").split(","));

        Collections.shuffle(abilities, MyRandom.getRandom());
        Map<SpellAbility, String> draftOptions = new HashMap<>();
        for (String sVar : abilities.subList(0, 3)) {
            draftOptions.put(AbilityFactory.getAbility(source, sVar), sVar);
        }
        final String title = Localizer.getInstance().getMessage("lblChooseRoom");
        SpellAbility chosen = player.getController().chooseSingleSpellForEffect(List.copyOf(draftOptions.keySet()), sa, title, null);
        source.setSVar(sa.getParamOrDefault("ResultSVar", "ChosenAbility"),source.getSVar(draftOptions.get(chosen)));
    }
}
