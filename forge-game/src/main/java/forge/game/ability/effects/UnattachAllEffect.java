package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;

public class UnattachAllEffect extends SpellAbilityEffect {
    private static void handleUnattachment(final GameEntity o, final Card cardToUnattach) {
        if (cardToUnattach.isAttachment() && o.hasCardAttachment(cardToUnattach)) {
            cardToUnattach.unattachFromEntity(cardToUnattach.getEntityAttachedTo());
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

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Unattach all valid Equipment and Auras from ");
        sb.append(Lang.joinHomogenous(getTargetEntities(sa)));
        return sb.toString();
    }

    @Override
    public void resolve(final SpellAbility sa) {
        Card source = sa.getHostCard();
        final Game game = sa.getActivatingPlayer().getGame();

        // If Cast Targets will be checked on the Stack
        for (final GameEntity ge : getTargetEntities(sa)) {
            String valid = sa.getParam("UnattachValid");
            CardCollectionView unattachList = game.getCardsIn(ZoneType.Battlefield);
            unattachList = CardLists.getValidCards(unattachList, valid, source.getController(), source, sa);
            for (final Card c : unattachList) {
                handleUnattachment(ge, c);
            }
        }
    }
}
