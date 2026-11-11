package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilAbility;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;

/** 
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentNoncreatureAi extends PermanentAi {

    /**
     * The rest of the logic not covered by the canPlayAI template is defined
     * here
     */
    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        AiAbilityDecision decision = super.checkApiLogic(ai, sa);
        if (!decision.willingToPlay()) {
            return decision;
        }

        final Card host = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final Game game = ai.getGame();

        if (hatesActivatedAbilitiesNobodyUses(ai, host)) {
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactGame); // Stony Silence against no artifact ability (#6105)
        }

        // Check for valid targets before casting
        if (host.hasSVar("OblivionRing")) {
            // TODO: only the "may" case wouldn't fail checkETBEffects - replace with NeedsToPlay SVar?
            SpellAbility effectExile = AbilityFactory.getAbility(host.getSVar("TrigExile"), host);
            final ZoneType origin = ZoneType.listValueOf(effectExile.getParamOrDefault("Origin", "Battlefield")).get(0);
            effectExile.setActivatingPlayer(ai);
            CardCollection targets = CardLists.getTargetableCards(game.getCardsIn(origin), effectExile);
            if (sourceName.equals("Suspension Field")
                    || sourceName.equals("Detention Sphere")) {
                // existing "exile until leaves" enchantments only target opponent's permanents
                targets = CardLists.filterControlledBy(targets, ai.getOpponents());
            }
            if (targets.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }
        return decision;
    }

    /**
     * A permanent whose only job is to stop or tax activated abilities (Stony Silence, Suppression Field) when no
     * opposing permanent it reaches has one that is not a mana ability.
     */
    private static boolean hatesActivatedAbilitiesNobodyUses(final Player ai, final Card host) {
        boolean hates = false;
        for (final StaticAbility st : host.getStaticAbilities()) {
            final String mode = st.getParamOrDefault("Mode", "");
            final boolean stops = "CantBeActivated".equals(mode) && st.getParamOrDefault("ValidSA", "").startsWith("Activated");
            final boolean taxes = "RaiseCost".equals(mode) && st.getParamOrDefault("ValidSpell", "").startsWith("Activated");
            if (!stops && !taxes) {
                continue;
            }
            hates = true;
            final String valid = st.getParamOrDefault("ValidCard", "Card");
            for (final Card c : ai.getOpponents().getCardsIn(ZoneType.Battlefield)) {
                if (!c.isValid(valid, host.getController(), host, st)) {
                    continue;
                }
                for (final SpellAbility ab : c.getSpellAbilities()) {
                    if (ab.isActivatedAbility() && !ab.isManaAbility()) {
                        return false;
                    }
                }
            }
        }
        return hates;
    }
}
