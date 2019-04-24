package forge.ai.ability;

import com.google.common.collect.Maps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.List;
import java.util.Map;

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
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }

    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final Card source = sa.getHostCard();
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

        if (source.getSVar("X").equals("Count$xPaid")) {
            // Handle either Manifest X cards, or Manifest 1 card and give it X P1P1s
            // Set PayX here to maximum value.
            int x = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(x));
            if (x <= 0) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        // Library is empty, no Manifest
        final CardCollectionView library = ai.getCardsIn(ZoneType.Library);
        if (library.isEmpty())
            return false;

        // try not to mill himself with Manifest
        if (library.size() < 5 && !ai.isCardInPlay("Laboratory Maniac")) {
            return false;
        }

        // check to ensure that there are no replacement effects that prevent creatures ETBing from library
        // (e.g. Grafdigger's Cage)
        Card topCopy = CardUtil.getLKICopy(library.getFirst());
        topCopy.turnFaceDownNoUpdate();
        topCopy.setManifested(true);

        final Map<String, Object> repParams = Maps.newHashMap();
        repParams.put("Event", "Moved");
        repParams.put("Affected", topCopy);
        repParams.put("Origin", ZoneType.Library);
        repParams.put("Destination", ZoneType.Battlefield);
        repParams.put("Source", sa.getHostCard());
        List<ReplacementEffect> list = game.getReplacementHandler().getReplacementList(repParams, ReplacementLayer.Other);
        if (!list.isEmpty()) {
            return false;
        }

        // if the AI can see the top card of the library, check it
        final Card topCard = library.getFirst();
        if (topCard.mayPlayerLook(ai)) {
            // try to avoid manifest a non Permanent
            if (!topCard.isPermanent())
                return false;

            // do not manifest a card with X in its cost
            if (topCard.getManaCost().countX() > 0)
                return false;

            // try to avoid manifesting a creature with zero or less thoughness
            if (topCard.isCreature() && topCard.getNetToughness() <= 0)
                return false;

            // card has ETBTrigger or ETBReplacement
            if (topCard.hasETBTrigger(false) || topCard.hasETBReplacement()) {
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
}
