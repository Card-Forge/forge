package forge.ai.ability;

import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/** 
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentNoncreatureAi extends PermanentAi {

    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if ("PithingNeedle".equals(aiLogic)) {
            // Make sure theres something in play worth Needlings.
            // Planeswalker or equipment or something

            CardCollection oppPerms = CardLists.getValidCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), "Card.OppCtrl+hasNonManaActivatedAbility", ai, sa.getHostCard(), sa);
            if (oppPerms.isEmpty()) {
                return false;
            }

            Card card = ComputerUtilCard.getBestPlaneswalkerAI(oppPerms);
            if (card != null) {
                return true;
            }

            // 5 percent chance to cast per opposing card with a non mana ability
            return MyRandom.getRandom().nextFloat() <= .05 * oppPerms.size();
        }

        return super.checkAiLogic(ai, sa, aiLogic);
    }

    /**
     * The rest of the logic not covered by the canPlayAI template is defined
     * here
     */
    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!super.checkApiLogic(ai, sa))
            return false;

        final Card host = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final Game game = ai.getGame();

        // Check for valid targets before casting
        if (host.hasSVar("OblivionRing")) {
            SpellAbility effectExile = AbilityFactory.getAbility(host.getSVar("TrigExile"), host);
            final ZoneType origin = ZoneType.listValueOf(effectExile.getParamOrDefault("Origin", "Battlefield")).get(0);
            effectExile.setActivatingPlayer(ai, true);
            CardCollection targets = CardLists.getTargetableCards(game.getCardsIn(origin), effectExile);
            if (sourceName.equals("Suspension Field") 
                    || sourceName.equals("Detention Sphere")) {
                // existing "exile until leaves" enchantments only target opponent's permanents
                // TODO: consider replacing the condition with host.hasSVar("OblivionRing")
                targets = CardLists.filterControlledBy(targets, ai.getOpponents());
            }
            // AiPlayDecision.AnotherTime
            return !targets.isEmpty();
        }
        return true;
    }
}
