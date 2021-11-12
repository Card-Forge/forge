package forge.game.ability.effects;


import java.util.List;
import java.util.Map;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.collect.FCollection;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class AttachEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();

        CardCollection attachments;

        final Player p = sa.getActivatingPlayer();

        Player chooser = p;
        if (sa.hasParam("Chooser")) {
            chooser = Iterables.getFirst(AbilityUtils.getDefinedPlayers(source, sa.getParam("Chooser"), sa), null);
        }

        if (sa.hasParam("Object")) {
            attachments = AbilityUtils.getDefinedCards(source, sa.getParam("Object"), sa);
        } else if (sa.hasParam("Choices")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChoose") + " ";

            CardCollection choices = CardLists.getValidCards(game.getCardsIn(choiceZone), sa.getParam("Choices"), p, source, sa);

            Map<String, Object> params = Maps.newHashMap();
            params.put("Target", Iterables.getFirst(getDefinedEntitiesOrTargeted(sa, "Defined"), null));

            Card c = chooser.getController().chooseSingleEntityForEffect(choices, sa, title, params);
            if (c == null) {
                return;
            }
            attachments = new CardCollection(c);
        } else {
            attachments = new CardCollection(source);
        }

        if (attachments.isEmpty()) {
            return;
        }

        GameEntity attachTo;

        if (sa.hasParam("Object") && (sa.hasParam("Choices") || sa.hasParam("PlayerChoices"))) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") :
                    Localizer.getInstance().getMessage("lblChoose") + " ";

            FCollection<GameEntity> choices = new FCollection<>();
            if (sa.hasParam("PlayerChoices")) {
                choices = AbilityUtils.getDefinedEntities(source, sa.getParam("PlayerChoices"), sa);
                for (final Card attachment : attachments) {
                    for (GameEntity g : choices) {
                        if (!g.canBeAttached(attachment)) {
                            choices.remove(g);
                        }
                    }
                }
            } else {
                CardCollection cardChoices = CardLists.getValidCards(game.getCardsIn(choiceZone),
                        sa.getParam("Choices"), p, source, sa);
                // Object + Choices means Attach Aura/Equipment onto new another card it can attach
                // if multiple attachments, all of them need to be able to attach to new card
                for (final Card attachment : attachments) {
                    if (sa.hasParam("Move")) {
                        Card e = attachment.getAttachedTo();
                        if (e != null)
                            cardChoices.remove(e);
                    }
                    cardChoices = CardLists.filter(cardChoices, CardPredicates.canBeAttached(attachment));
                }
                choices.addAll(cardChoices);
            }

            Map<String, Object> params = Maps.newHashMap();
            params.put("Attachments", attachments);

            attachTo = chooser.getController().chooseSingleEntityForEffect(choices, sa, title, params);
        } else {
            FCollection<GameEntity> targets = new FCollection<>(getDefinedEntitiesOrTargeted(sa, "Defined"));
            if (targets.isEmpty()) {
                return;
            }
            String title = Localizer.getInstance().getMessage("lblChoose");
            Map<String, Object> params = Maps.newHashMap();
            params.put("Attachments", attachments);
            attachTo = chooser.getController().chooseSingleEntityForEffect(targets, sa, title, params);
        }

        String attachToName = null;
        if (attachTo == null) {
            return;
        } else if (attachTo instanceof Card) {
            attachToName = CardTranslation.getTranslatedName(((Card)attachTo).getName());
        } else {
            attachToName = attachTo.toString();
        }

        attachments = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, attachments, ZoneType.Battlefield, sa);

        // If Cast Targets will be checked on the Stack
        for (final Card attachment : attachments) {
            String message = Localizer.getInstance().getMessage("lblDoYouWantAttachSourceToTarget", CardTranslation.getTranslatedName(attachment.getName()), attachToName);
            if (sa.hasParam("Optional") && !p.getController().confirmAction(sa, null, message))
            // TODO add params for message
                continue;

            attachment.attachToEntity(attachTo);
            if (sa.hasParam("RememberAttached") && attachment.isAttachedToEntity(attachTo)) {
                source.addRemembered(attachment);
            }
        }

        if (source.isAura() && sa.isSpell()) {
            CardZoneTable table = new CardZoneTable();
            source.setController(sa.getActivatingPlayer(), 0);

            ZoneType previousZone = source.getZone().getZoneType();

            //CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
            //CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();

            Map<AbilityKey, Object> moveParams = Maps.newEnumMap(AbilityKey.class);
            //moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
            //moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);

            // The Spell_Permanent (Auras) version of this AF needs to
            // move the card into play before Attaching
            final Card c = game.getAction().moveToPlay(source, source.getController(), sa, moveParams);

            ZoneType newZone = c.getZone().getZoneType();
            if (newZone != previousZone) {
                table.put(previousZone, newZone, c);
            }
            table.triggerChangesZoneAll(game, sa);
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append(" Attach to ");

        final List<GameObject> targets = getTargets(sa);
        // Should never allow more than one Attachment per card

        sb.append(Lang.joinHomogenous(targets));
        return sb.toString();
    }
}
