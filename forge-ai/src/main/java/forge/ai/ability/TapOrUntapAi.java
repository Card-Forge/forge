package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class TapOrUntapAi extends TapAiBase {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();

        if (!sa.usesTargeting()) {
            // assume we are looking to tap human's stuff
            // TODO - check for things with untap abilities, and don't tap those.

            boolean bFlag = false;
            for (final Card c : AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa)) {
                bFlag |= c.isUntapped();
            }

            if (!bFlag) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else {
            sa.resetTargets();
            // Pestermite with Splinter Twin or Kiki-Jiki: untap the tapped engine, which copies the Pestermite again
            final Card engine = untapEngine(ai, sa);
            if (engine != null) {
                sa.getTargets().add(engine);
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            if (!tapPrefTargeting(ai, source, sa, false)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }


    /** A "{T}: create a hasty copy" ability: Splinter Twin on a creature (it grants one), Kiki-Jiki, Zealous Conscripts' like. */
    private static boolean isCopyEngine(final Card c) {
        if ("True".equals(c.getSVar("UntapMe"))) {
            return true;
        }
        for (final SpellAbility a : c.getSpellAbilities()) {
            if (a.getApi() == ApiType.CopyPermanent && a.hasParam("AtEOT")
                    && a.getParamOrDefault("AddKeywords", "").contains("Haste")
                    && a.getPayCosts() != null && a.getPayCosts().hasTapCost()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The tapped permanent of the AI that the tap-or-untap effect should untap: the one that makes a hasty copy for a
     * tap. Each copy of a Pestermite or Deceiver Exarch then untaps it again, and that is the whole combo. Null when
     * there is none (the effect then taps what an opponent needs).
     */
    public static Card untapEngine(final Player ai, final SpellAbility sa) {
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isTapped() && isCopyEngine(c) && sa.canTarget(c)) {
                return c;
            }
        }
        return null;
    }
}
