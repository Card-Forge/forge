package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class CloneEffect extends SpellAbilityEffect {
    // TODO update this method

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();
        Card tgtCard = host;

        Card cardToCopy = host;
        if (sa.hasParam("Defined")) {
            List<Card> cloneSources = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            if (!cloneSources.isEmpty()) {
                cardToCopy = cloneSources.get(0);
            }
        } else if (sa.usesTargeting()) {
            cardToCopy = sa.getTargets().getFirstTargetedCard();
        }

        List<Card> cloneTargets = AbilityUtils.getDefinedCards(host, sa.getParam("CloneTarget"), sa);
        if (!cloneTargets.isEmpty()) {
            tgtCard = cloneTargets.get(0);
        }

        sb.append(tgtCard);
        sb.append(" becomes a copy of ").append(cardToCopy).append(".");

        return sb.toString();
    } // end cloneStackDescription()

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        Card tgtCard = host;
        final Game game = activator.getGame();
        final List<String> pumpKeywords = Lists.newArrayList();

        if (sa.hasParam("PumpKeywords")) {
            pumpKeywords.addAll(Arrays.asList(sa.getParam("PumpKeywords").split(" & ")));
        }

        // find cloning source i.e. thing to be copied
        Card cardToCopy = null;
        
        if (sa.hasParam("Choices")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            CardCollection choices = new CardCollection(game.getCardsIn(choiceZone));

            // choices need to be filtered by LastState Battlefield or Graveyard
            // if a Clone enters the field as other cards it could clone,
            // the clone should not be able to clone them
            // but do that only for Replacement Effects
            if (sa.getRootAbility().isReplacementAbility()) {
                if (choiceZone.equals(ZoneType.Battlefield)) {
                    choices.retainAll(sa.getLastStateBattlefield());
                } else if (choiceZone.equals(ZoneType.Graveyard)) {
                    choices.retainAll(sa.getLastStateGraveyard());
                }
            }

            choices = CardLists.getValidCards(choices, sa.getParam("Choices"), activator, host, sa);

            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChooseaCard") + " ";
            cardToCopy = activator.getController().chooseSingleEntityForEffect(choices, sa, title, false, null);
        } else if (sa.hasParam("Defined")) {
            List<Card> cloneSources = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            if (!cloneSources.isEmpty()) {
                cardToCopy = cloneSources.get(0);
            }
        } else if (sa.usesTargeting()) {
            cardToCopy = sa.getTargets().getFirstTargetedCard();
        }
        if (cardToCopy == null) {
            return;
        }

        final boolean optional = sa.hasParam("Optional");
        if (optional && !host.getController().getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantCopy", CardTranslation.getTranslatedName(cardToCopy.getName())))) {
            return;
        }

        // find target of cloning i.e. card becoming a clone
        if (sa.hasParam("CloneTarget")) {
            final List<Card> cloneTargets = AbilityUtils.getDefinedCards(host, sa.getParam("CloneTarget"), sa);
            if (!cloneTargets.isEmpty()) {
                tgtCard = cloneTargets.get(0);
                game.getTriggerHandler().clearActiveTriggers(tgtCard, null);
            } else {
                return;
            }
        } else if (sa.hasParam("Choices") && sa.usesTargeting()) {
            tgtCard = sa.getTargets().getFirstTargetedCard();
            game.getTriggerHandler().clearActiveTriggers(tgtCard, null);
        }

        if (sa.hasParam("CloneZone")) {
            if (!tgtCard.isInZone(ZoneType.smartValueOf(sa.getParam("CloneZone")))) {
                return;
            }
        }

        final Long ts = game.getNextTimestamp();
        tgtCard.addCloneState(CardFactory.getCloneStates(cardToCopy, tgtCard, sa), ts);

        // set ETB tapped of clone
        if (sa.hasParam("IntoPlayTapped")) {
            tgtCard.setTapped(true);
        }

        if (!pumpKeywords.isEmpty()) {
            tgtCard.addChangedCardKeywords(pumpKeywords, Lists.newArrayList(), false, false, ts, 0);
        }

        tgtCard.updateStateForView();

        // when clone is itself, cleanup from old abilities
        if (host.equals(tgtCard) && !sa.hasParam("ImprintRememberedNoCleanup")) {
            tgtCard.clearImprintedCards();
            tgtCard.clearRemembered();
        }

        // check if clone is now an Aura that needs to be attached
        if (tgtCard.isAura() && !tgtCard.isInZone(ZoneType.Battlefield)) {
            AttachEffect.attachAuraOnIndirectEnterBattlefield(tgtCard);
        }

        if (sa.hasParam("Duration")) {
            final Card cloneCard = tgtCard;
            // if clone is temporary, target needs old values back after (keep Death-Mask Duplicant working)
            final Iterable<Card> clonedImprinted = new CardCollection(tgtCard.getImprintedCards());
            final Iterable<Object> clonedRemembered = new FCollection<>(tgtCard.getRemembered());

            final GameCommand unclone = new GameCommand() {
                private static final long serialVersionUID = -78375985476256279L;

                @Override
                public void run() {
                    if (cloneCard.removeCloneState(ts)) {
                        // remove values gained while being cloned
                        cloneCard.clearImprintedCards();
                        cloneCard.clearRemembered();
                        // restore original Remembered and Imprinted, ignore cards from players who lost
                        cloneCard.addImprintedCards(Iterables.filter(clonedImprinted, CardPredicates.ownerLives()));
                        cloneCard.addRemembered(Iterables.filter(clonedRemembered, Player.class));
                        cloneCard.addRemembered(Iterables.filter(Iterables.filter(clonedRemembered, Card.class), CardPredicates.ownerLives()));
                        cloneCard.updateStateForView();
                        game.fireEvent(new GameEventCardStatsChanged(cloneCard));
                    }
                }
            };

            addUntilCommand(sa, unclone);
        }

        // now we can also cleanup in case target was another card
        tgtCard.clearRemembered();
        tgtCard.clearImprintedCards();

        if (sa.hasParam("RememberCloneOrigin")) {
            tgtCard.addRemembered(cardToCopy);
        }

        game.fireEvent(new GameEventCardStatsChanged(tgtCard));
    } // cloneResolve

}
