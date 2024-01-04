package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;

public class ChangeCombatantsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        // should update when adding effects for defined blocker
        sb.append("Reselect the defender of ");
        sb.append(Lang.joinHomogenous(getTargetCards(sa)));

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        boolean isCombatChanged = false;
        final boolean isOptional = sa.hasParam("Optional");
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        // TODO: may expand this effect for defined blocker (False Orders, General Jarkeld, Sorrow's Path, Ydwen Efreet)
        for (final Card c : getTargetCards(sa)) {
            String cardString = CardTranslation.getTranslatedName(c.getName()) + " (" + c.getId() + ")";
            if (isOptional && !activator.getController().confirmAction(sa, null,
                    Localizer.getInstance().getMessage("lblChangeCombatantOption", cardString), null)) {
                continue;
            }

            final GameEntity originalDefender = game.getCombat().getDefenderByAttacker(c);

            if (addToCombat(c, sa, "Attacking", "Blocking")) {
                isCombatChanged = true;
                GameEntity defender = game.getCombat().getDefenderByAttacker(c);

                // retarget triggers to the new defender (e.g. Ulamog, Ceaseless Hunger + Portal Mage)
                for (SpellAbilityStackInstance si : game.getStack()) {
                    if (si.isTrigger() && c.equals(si.getSourceCard())
                            && si.getTriggeringObject(AbilityKey.Attacker) != null) {
                        si.addTriggeringObject(AbilityKey.OriginalDefender, originalDefender);
                        if (defender instanceof Player) {
                            si.updateTriggeringObject(AbilityKey.DefendingPlayer, defender);
                        } else if (defender instanceof Card) {
                            si.updateTriggeringObject(AbilityKey.DefendingPlayer, ((Card)defender).getController());
                        }
                    }
                }
            }
        }

        if (isCombatChanged) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    }
}
