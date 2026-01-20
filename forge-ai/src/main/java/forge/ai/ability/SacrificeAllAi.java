package forge.ai.ability;

import forge.ai.*;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.OptionalInt;
import java.util.function.Function;

public class SacrificeAllAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final String logic = sa.getParamOrDefault("AILogic", "");

        if (logic.equals("HellionEruption")) {
            if (ai.getCreaturesInPlay().size() < 5 || ai.getCreaturesInPlay().size() * 150 < ComputerUtilCard.evaluateCreatureList(ai.getCreaturesInPlay())) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else if (logic.equals("MadSarkhanDragon")) {
            return SpecialCardAi.SarkhanTheMad.considerMakeDragon(ai, sa);
        } else if (logic.equals("ShapeAnew")) {
            return considerShapeAnew(ai, sa);
        }

        AiAbilityDecision decision = DestroyAllAi.doMassRemovalLogic(ai, sa);
        if (!decision.willingToPlay()) {
            return decision;
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        //TODO: Add checks for bad outcome
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);

    }

    private AiAbilityDecision considerShapeAnew(Player ai, SpellAbility sa) {
        Function<CardCollectionView, OptionalInt> minArtifactCmc = cards ->
            cards.stream().filter(Card::isArtifact).mapToInt(Card::getCMC).min();

        int minToSacrifice = minArtifactCmc.apply(ai.getCardsIn(ZoneType.Battlefield)).orElse(100);
        int minToGain = minArtifactCmc.apply(ai.getCardsIn(ZoneType.Library)).orElse(0);
        if (minToGain > minToSacrifice + sa.getHostCard().getCMC()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantAfford);
        }
    }
}
