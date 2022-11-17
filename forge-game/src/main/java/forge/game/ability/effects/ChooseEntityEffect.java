package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class ChooseEntityEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.hasParam("StackDescription") ? sa.getParam("StackDescription") :
                sa.getParamOrDefault("SpellDescription", "Write a Stack/SpellDescription!");
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        FCollection<GameEntity> choices = new FCollection<>();
        if (sa.hasParam("CardChoices")) {
            choices.addAll(CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("CardChoices"),
                    activator, host, sa));
        }
        if (sa.hasParam("PlayerChoices")) {
            choices.addAll(AbilityUtils.getDefinedPlayers(host, sa.getParam("PlayerChoices"), sa));
        }

        FCollection<GameEntity> chosen = new FCollection<>();
        int n = sa.hasParam("ChoiceAmount") ? AbilityUtils.calculateAmount(host, sa.getParam("ChoiceAmount"), sa) : 1;
        if (sa.hasParam("Random")) {
            for (int i = 0; i < n; i++) {
            chosen.add(Aggregates.random(choices));
            choices.remove(chosen);
        } else {
            final String prompt = sa.hasParam("ChoicePrompt") ? sa.getParam("ChoicePrompt") :
                    Localizer.getInstance().getMessage("lblChooseEntity");
            chosen.addAll(activator.getController().chooseEntitiesForEffect(choices, n, n, null, sa, prompt,
                    null, null));
        }
        if (chosen == null) {
            System.err.println("Error: ChooseEntityEffect.java unable to choose an entity");
            return;
        }

        if (sa.hasParam("RememberChosen")) {
            for (GameEntity ge : chosen) {
                host.addRemembered(ge);
            }
        }
    }
}
