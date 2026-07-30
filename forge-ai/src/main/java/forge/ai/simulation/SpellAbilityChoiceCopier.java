package forge.ai.simulation;

import forge.game.GameActionUtil;
import forge.game.GameObject;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class SpellAbilityChoiceCopier {
    private SpellAbilityChoiceCopier() {
    }

    static SpellAbility copyCastChoices(SpellAbility source, SpellAbility destination, Player player) {
        if (source.hasParam("WithoutManaCost") && !destination.hasParam("WithoutManaCost")) {
            destination = destination.copyWithNoManaCost(player);
        }

        if (source.getOptionalCosts().iterator().hasNext()) {
            List<OptionalCostValue> costs = GameActionUtil.getOptionalCostValues(destination);
            costs.removeIf(cost -> !source.isOptionalCostPaid(cost.getType()));
            destination = GameActionUtil.addOptionalCosts(destination, costs);
        }

        copyKeywordChoices(source, destination);
        destination.setCastFromPlayEffect(source.isCastFromPlayEffect());
        destination.setXManaCostPaid(source.getXManaCostPaid());
        return copyChosenModes(source, destination) ? destination : null;
    }

    static void copyTargets(SpellAbility source, SpellAbility destination,
            Function<GameObject, GameObject> gameObjectMapper) {
        if (source == destination) {
            return;
        }
        for (SpellAbility sourceAbility = source, destinationAbility = destination;
                sourceAbility != null && destinationAbility != null;
                sourceAbility = sourceAbility.getSubAbility(),
                        destinationAbility = destinationAbility.getSubAbility()) {
            if (!sourceAbility.usesTargeting()) {
                continue;
            }
            for (GameObject sourceTarget : sourceAbility.getTargets()) {
                GameObject destinationTarget = gameObjectMapper.apply(sourceTarget);
                destinationAbility.getTargets().add(destinationTarget);
                if (sourceAbility.isDividedAsYouChoose()) {
                    destinationAbility.addDividedAllocation(destinationTarget, sourceAbility.getDividedValue(sourceTarget));
                }
            }
        }
    }

    private static void copyKeywordChoices(SpellAbility source, SpellAbility destination) {
        if (source.getOptionalKeywords().isEmpty()) {
            return;
        }
        for (KeywordInterface sourceKeyword : source.getHostCard().getKeywords()) {
            if (!source.hasOptionalKeywordAmount(sourceKeyword)) {
                continue;
            }
            for (KeywordInterface destinationKeyword : destination.getHostCard().getKeywords()) {
                if (sourceKeyword.getKeyword() == destinationKeyword.getKeyword()
                        && Objects.equals(sourceKeyword.getOriginal(), destinationKeyword.getOriginal())) {
                    destination.setOptionalKeywordAmount(destinationKeyword, source.getOptionalKeywordAmount(sourceKeyword));
                    break;
                }
            }
        }
    }

    private static boolean copyChosenModes(SpellAbility source, SpellAbility destination) {
        if (source.getChosenList() == null) {
            return true;
        }

        List<AbilitySub> choices = destination.getAdditionalAbilityList("Choices");
        List<AbilitySub> chosen = new ArrayList<>();
        for (AbilitySub sourceMode : source.getChosenList()) {
            AbilitySub destinationMode = choices.stream()
                    .filter(choice -> Objects.equals(
                            choice.getParam("SpellDescription"),
                            sourceMode.getParam("SpellDescription")))
                    .findFirst().orElse(null);
            if (destinationMode == null) {
                return false;
            }
            chosen.add(destinationMode);
        }
        destination.setChosenList(chosen);
        return true;
    }
}
