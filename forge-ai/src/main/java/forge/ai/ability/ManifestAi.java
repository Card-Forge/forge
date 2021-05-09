package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Created by friarsol on 1/23/15.
 */
public class ManifestAi extends SpellAbilityAi {

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // Manifest doesn't have any "Pay X to manifest X triggers"

        return true;
    }
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }

    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        // Only manifest things on your turn if sorcery speed, or would pump one of my creatures
        if (ph.isPlayerTurn(ai)) {
            if (ph.getPhase().isBefore(PhaseType.MAIN2)
                    && !sa.hasParam("ActivationPhases")
                    && !ComputerUtil.castSpellInMain1(ai, sa)) {
                boolean buff = false;
                for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                    if ("Creature".equals(c.getSVar("BuffedBy"))) {
                        buff = true;
                    }
                }
                if (!buff) {
                    return false;
                }
            } else if (!SpellAbilityAi.isSorcerySpeed(sa)) {
                return false;
            }
        } else {
            // try to ambush attackers
            if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        }

        if (sa.getSVar("X").equals("Count$xPaid")) {
            // Handle either Manifest X cards, or Manifest 1 card and give it X P1P1s
            // Set PayX here to maximum value.
            int x = ComputerUtilCost.getMaxXValue(sa, ai);
            sa.setXManaCostPaid(x);
            if (x <= 0) {
                return false;
            }
        }

        return true;
    }

    static boolean shouldManyfest(final Card card, final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        // check to ensure that there are no replacement effects that prevent creatures ETBing from library
        // (e.g. Grafdigger's Cage)
        Card topCopy = CardUtil.getLKICopy(card);
        topCopy.turnFaceDownNoUpdate();
        topCopy.setManifested(true);

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(topCopy);
        repParams.put(AbilityKey.Origin, card.getZone().getZoneType());
        repParams.put(AbilityKey.Destination, ZoneType.Battlefield);
        repParams.put(AbilityKey.Source, sa.getHostCard());
        List<ReplacementEffect> list = game.getReplacementHandler().getReplacementList(ReplacementType.Moved, repParams, ReplacementLayer.Other);
        if (!list.isEmpty()) {
            return false;
        }

        if (card.mayPlayerLook(ai)) {
            // try to avoid manifest a non Permanent
            if (!card.isPermanent())
                return false;

            // do not manifest a card with X in its cost
            if (card.getManaCost().countX() > 0)
                return false;

            // try to avoid manifesting a creature with zero or less thoughness
            if (card.isCreature() && card.getNetToughness() <= 0)
                return false;

            // card has ETBTrigger or ETBReplacement
            if (card.hasETBTrigger(false) || card.hasETBReplacement()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Card host = sa.getHostCard();
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (sa.hasParam("Choices") || sa.hasParam("ChoiceZone")) {
            ZoneType choiceZone = ZoneType.Hand;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            CardCollection choices = new CardCollection(game.getCardsIn(choiceZone));
            if (sa.hasParam("Choices")) {
                choices = CardLists.getValidCards(choices, sa.getParam("Choices"), ai, host, sa);
            }
            if (choices.isEmpty()) {
                return false;
            }
        } else {
            // Library is empty, no Manifest
            final CardCollectionView library = ai.getCardsIn(ZoneType.Library);
            if (library.isEmpty())
                return false;

            // try not to mill himself with Manifest
            if (library.size() < 5 && !ai.isCardInPlay("Laboratory Maniac")) {
                return false;
            }

            if (!shouldManyfest(library.getFirst(), ai, sa)) {
                return false;
            }
        }
        // Probably should be a little more discerning on playing during OPPs turn
        if (SpellAbilityAi.playReusable(ai, sa)) {
            return true;
        }
        if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            // Add blockers?
            return true;
        }
        if (sa.isAbility()) {
            return true;
        }

        return MyRandom.getRandom().nextFloat() < .8;
    }

    protected Card chooseSingleCard(final Player ai, final SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer) {
        if (Iterables.size(options) <= 1) {
            return Iterables.getFirst(options, null);
        }
        CardCollection filtered = CardLists.filter(options, new Predicate<Card>() {
            @Override
            public boolean apply(Card input) {
                return !shouldManyfest(input, ai, sa);
            }
        });
        if (!filtered.isEmpty()) {
            return ComputerUtilCard.getBestAI(filtered);
        }
        if (isOptional) {
            return null;
        }
        return Iterables.getFirst(options, null);
    }
}
