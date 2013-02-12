package forge.card.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardLists;
import forge.GameEntity;
import forge.Singletons;
import forge.card.ability.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class UnattachAllEffect extends SpellEffect {

    private void handleUnattachment(final GameEntity o, final Card cardToUnattach) {

        if (o instanceof Card) {
            final Card c = (Card) o;
            if (cardToUnattach.isAura()) {
                //final boolean gainControl = "GainControl".equals(af.getMapParams().get("AILogic"));
                //AbilityFactoryAttach.handleUnattachAura(cardToUnattach, c, gainControl);
            } else if (cardToUnattach.isEquipment()) {
                if (cardToUnattach.isEquipping() && c.getEquippedBy().contains(cardToUnattach)) {
                    cardToUnattach.unEquipCard(cardToUnattach.getEquipping().get(0));
                }
                //TODO - unfortify would also be handled here
            }
        } else if (o instanceof Player) {
            final Player p = (Player) o;
            if (cardToUnattach.isAura() && p.getEnchantedBy().contains(cardToUnattach)) {
                //AbilityFactoryAttach.handleUnattachAura(cardToUnattach, p, false);
            }
        }
    }

    /* this isn't modifed to handled unattach yet, but should be for things like Remove Enchantments, etc.
    private static void handleUnattachAura(final Card card, final GameEntity tgt, final boolean gainControl) {
        if (card.isEnchanting()) {
            // If this Card is already Enchanting something
            // Need to unenchant it, then clear out the commands
            final GameEntity oldEnchanted = card.getEnchanting();
            card.removeEnchanting(oldEnchanted);
            card.clearEnchantCommand();
            card.clearUnEnchantCommand();
            card.clearTriggers(); // not sure if cleartriggers is needed?
        }

        if (gainControl) {
            // Handle GainControl Auras
            final Player[] pl = new Player[1];

            if (tgt instanceof Card) {
                pl[0] = ((Card) tgt).getController();
            } else {
                pl[0] = (Player) tgt;
            }

            final Command onEnchant = new Command() {
                private static final long serialVersionUID = -2519887209491512000L;

                @Override
                public void execute() {
                    final Card crd = card.getEnchantingCard();
                    if (crd == null) {
                        return;
                    }

                    pl[0] = crd.getController();

                    crd.addController(card);

                } // execute()
            }; // Command

            final Command onUnEnchant = new Command() {
                private static final long serialVersionUID = 3426441132121179288L;

                @Override
                public void execute() {
                    final Card crd = card.getEnchantingCard();
                    if (crd == null) {
                        return;
                    }

                    if (AllZoneUtil.isCardInPlay(crd)) {
                        crd.removeController(card);
                    }

                } // execute()
            }; // Command

            final Command onChangesControl = new Command() {
                private static final long serialVersionUID = -65903786170234039L;

                @Override
                public void execute() {
                    final Card crd = card.getEnchantingCard();
                    if (crd == null) {
                        return;
                    }
                    crd.removeController(card); // This looks odd, but will
                                                // simply refresh controller
                    crd.addController(card);
                } // execute()
            }; // Command

            // Add Enchant Commands for Control changers
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addChangeControllerCommand(onChangesControl);
        }

        final Command onLeavesPlay = new Command() {
            private static final long serialVersionUID = -639204333673364477L;

            @Override
            public void execute() {
                final GameEntity entity = card.getEnchanting();
                if (entity == null) {
                    return;
                }

                card.unEnchantEntity(entity);
            }
        }; // Command

        card.addLeavesPlayCommand(onLeavesPlay);
        card.enchantEntity(tgt);
    }
    */

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Unattach all valid Equipment and Auras from ");
        final List<Object> targets = getTargetObjects(sa);
        sb.append(StringUtils.join(targets, " "));
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getSourceCard();
        final List<Object> targets = getTargetObjects(sa);

        // If Cast Targets will be checked on the Stack
        for (final Object o : targets) {
            if (!(o instanceof GameEntity)) {
                continue;
            }

            String valid = sa.getParam("UnattachValid");
            List<Card> unattachList = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            unattachList = CardLists.getValidCards(unattachList, valid.split(","), source.getController(), source);
            for (final Card c : unattachList) {
                handleUnattachment((GameEntity) o, c);
            }
        }
    }
}
