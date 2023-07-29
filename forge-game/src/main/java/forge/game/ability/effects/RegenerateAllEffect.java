package forge.game.ability.effects;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class RegenerateAllEffect extends RegenerateBaseEffect {

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Regenerate all valid cards.";
    }

    /*
     * (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();
        final String valid = sa.getParamOrDefault("ValidCards", "");

        // create Effect for Regeneration
        createRegenerationEffect(sa, CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), valid, hostCard.getController(), hostCard, sa));
    }

}
