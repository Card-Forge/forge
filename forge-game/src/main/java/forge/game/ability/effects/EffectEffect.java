package forge.game.ability.effects;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.ImageKeys;
import forge.card.CardRarity;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;
import forge.util.collect.FCollection;

public class EffectEffect extends SpellAbilityEffect {

    /**
     * <p>
     * effectResolve.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();

        String[] effectAbilities = null;
        String[] effectTriggers = null;
        String[] effectStaticAbilities = null;
        String[] effectReplacementEffects = null;
        FCollection<GameObject> rememberList = null;
        String effectImprinted = null;
        String noteCounterDefined = null;
        final String duration = sa.getParam("Duration");

        if (((duration != null && duration.startsWith("UntilHostLeavesPlay")) || "UntilLoseControlOfHost".equals(duration) || "UntilUntaps".equals(duration))
                && !(hostCard.isInPlay() || hostCard.isInZone(ZoneType.Stack))) {
            return;
        }
        if ("UntilLoseControlOfHost".equals(duration) && hostCard.getController() != sa.getActivatingPlayer()) {
            return;
        }
        if ("UntilUntaps".equals(duration) && !hostCard.isTapped()) {
            return;
        }

        if (sa.hasParam("Abilities")) {
            effectAbilities = sa.getParam("Abilities").split(",");
        }

        if (sa.hasParam("Triggers")) {
            effectTriggers = sa.getParam("Triggers").split(",");
        }

        if (sa.hasParam("StaticAbilities")) {
            effectStaticAbilities = sa.getParam("StaticAbilities").split(",");
        }

        if (sa.hasParam("ReplacementEffects")) {
            effectReplacementEffects = sa.getParam("ReplacementEffects").split(",");
        }

        if (sa.hasParam("RememberSpell")) {
            rememberList = new FCollection<>();
            for (final String rem : sa.getParam("RememberSpell").split(",")) {
                rememberList.addAll(AbilityUtils.getDefinedSpellAbilities(hostCard, rem, sa));
            }
        }

        if (sa.hasParam("RememberObjects")) {
            rememberList = new FCollection<>();
            for (final String rem : sa.getParam("RememberObjects").split(",")) {
                rememberList.addAll(AbilityUtils.getDefinedEntities(hostCard, rem, sa));
            }

            if (sa.hasParam("ForgetCounter")) {
                CounterType cType = CounterType.getType(sa.getParam("ForgetCounter"));
                rememberList = new FCollection<>(CardLists.filter(Iterables.filter(rememberList, Card.class), CardPredicates.hasCounter(cType)));
            }

            // don't create Effect if there is no remembered Objects
            if (rememberList.isEmpty() && (sa.hasParam("ForgetOnMoved") || sa.hasParam("ExileOnMoved") || sa.hasParam("ForgetCounter"))) {
                return;
            }
        }

        if (sa.hasParam("RememberLKI")) {
            rememberList = new FCollection<>();
            for (final String rem : sa.getParam("RememberLKI").split(",")) {
                CardCollection def = AbilityUtils.getDefinedCards(hostCard, rem, sa);
                for (Card c : def) {
                    rememberList.add(CardUtil.getLKICopy(c));
                }
            }

            // don't create Effect if there is no remembered Objects
            if (rememberList.isEmpty() && (sa.hasParam("ForgetOnMoved") || sa.hasParam("ExileOnMoved"))) {
                return;
            }
        }

        if (sa.hasParam("ImprintCards")) {
            effectImprinted = sa.getParam("ImprintCards");
        }

        if (sa.hasParam("NoteCounterDefined")) {
            noteCounterDefined = sa.getParam("NoteCounterDefined");
        }

        String name = sa.getParam("Name");
        if (name == null) {
            name = hostCard + (sa.hasParam("Boon") ? "'s Boon" : "'s Effect");
        }

        PlayerCollection effectOwner = sa.hasParam("EffectOwner") ?
                AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("EffectOwner"), sa) :
                new PlayerCollection(sa.getActivatingPlayer());

        // Unique$ is for effects that should be one per player (e.g. Gollum, Obsessed Stalker)
        if (sa.hasParam("Unique")) {
            for (Player eo : effectOwner.threadSafeIterable()) {
                if (eo.isCardInCommand(name)) {
                    effectOwner.remove(eo);
                }
            }
        }

        if (effectOwner.isEmpty()) {
            return; // return if we don't need to make an effect
        }

        String image;
        if (sa.hasParam("Image")) {
            image = ImageKeys.getTokenKey(sa.getParam("Image"));
        } else if (name.startsWith("Emblem")) { // try to get the image from name
            image = ImageKeys.getTokenKey(
            TextUtil.fastReplace(
                TextUtil.fastReplace(
                    TextUtil.fastReplace(name.toLowerCase(), " - ", "_"),
                        ",", ""),
                    " ", "_").toLowerCase());
        } else { // use host image
            image = hostCard.getImageKey();
        }

        Map<AbilityKey, Object> params = AbilityKey.newMap();
        params.put(AbilityKey.LastStateBattlefield, sa.getLastStateBattlefield());
        params.put(AbilityKey.LastStateGraveyard, sa.getLastStateGraveyard());

        for (Player controller : effectOwner) {
            final Card eff = createEffect(sa, controller, name, image);
            eff.setSetCode(hostCard.getSetCode());
            if (name.startsWith("Emblem")) {
                eff.setRarity(CardRarity.Common);
            } else {
                eff.setRarity(hostCard.getRarity());
            }

            // Abilities and triggers work the same as they do for Token
            // Grant abilities
            if (effectAbilities != null) {
                for (final String s : effectAbilities) {
                    final SpellAbility grantedAbility = AbilityFactory.getAbility(eff, s, sa);
                    eff.addSpellAbility(grantedAbility);
                    grantedAbility.setIntrinsic(true);
                }
            }

            // Grant triggers
            if (effectTriggers != null) {
                for (final String s : effectTriggers) {
                    final Trigger parsedTrigger = TriggerHandler.parseTrigger(AbilityUtils.getSVar(sa, s), eff, true, sa);
                    parsedTrigger.setActiveZone(EnumSet.of(ZoneType.Command));
                    parsedTrigger.setIntrinsic(true);
                    eff.addTrigger(parsedTrigger);
                }
            }

            // Grant static abilities
            if (effectStaticAbilities != null) {
                for (final String s : effectStaticAbilities) {
                    final StaticAbility addedStaticAbility = eff.addStaticAbility(AbilityUtils.getSVar(sa, s));
                    if (addedStaticAbility != null) { //prevent npe casting adventure card spell
                        addedStaticAbility.putParam("EffectZone", "Command");
                        addedStaticAbility.setIntrinsic(true);
                    }
                }
            }

            // Grant replacement effects
            if (effectReplacementEffects != null) {
                for (final String s : effectReplacementEffects) {
                    final String actualReplacement = AbilityUtils.getSVar(sa, s);

                    final ReplacementEffect parsedReplacement = ReplacementHandler.parseReplacement(actualReplacement, eff, true, eff.getCurrentState());
                    parsedReplacement.setActiveZone(EnumSet.of(ZoneType.Command));
                    parsedReplacement.setIntrinsic(true);
                    eff.addReplacementEffect(parsedReplacement);
                }
            }

            // Remember Keywords
            if (sa.hasParam("RememberKeywords")) {
                List<String> effectKeywords = Arrays.asList(sa.getParam("RememberKeywords").split(","));
                if (sa.hasParam("SharedKeywordsZone")) {
                    List<ZoneType> zones = ZoneType.listValueOf(sa.getParam("SharedKeywordsZone"));
                    String[] restrictions = sa.hasParam("SharedRestrictions") ? sa.getParam("SharedRestrictions").split(",")
                            : new String[]{"Card"};
                    effectKeywords = CardFactoryUtil.sharedKeywords(effectKeywords, restrictions, zones, hostCard, sa);
                }
                if (effectKeywords != null) {
                    eff.addRemembered(effectKeywords);
                }
            }

            // Set Remembered
            if (rememberList != null) {
                eff.addRemembered(rememberList);
                if (sa.hasParam("ForgetOnMoved")) {
                    addForgetOnMovedTrigger(eff, sa.getParam("ForgetOnMoved"));
                    if (!"Stack".equals(sa.getParam("ForgetOnMoved")) && !"False".equalsIgnoreCase(sa.getParam("ForgetOnCast"))) {
                        addForgetOnCastTrigger(eff);
                    }
                } else if (sa.hasParam("ForgetOnCast")) {
                    addForgetOnCastTrigger(eff);
                } else if (sa.hasParam("ExileOnMoved")) {
                    addExileOnMovedTrigger(eff, sa.getParam("ExileOnMoved"));
                }
                if (sa.hasParam("ForgetOnPhasedIn")) {
                    addForgetOnPhasedInTrigger(eff);
                }
                if (sa.hasParam("ForgetCounter")) {
                    addForgetCounterTrigger(eff, sa.getParam("ForgetCounter"));
                }
            }

            // Set Imprinted
            if (effectImprinted != null) {
                eff.addImprintedCards(AbilityUtils.getDefinedCards(hostCard, effectImprinted, sa));
            }

            // Note counters on defined
            if (noteCounterDefined != null) {
                for (final Card c : AbilityUtils.getDefinedCards(hostCard, noteCounterDefined, sa)) {
                    CountersNoteEffect.noteCounters(c, eff);
                }
            }

            // Set Chosen Color(s)
            if (hostCard.hasChosenColor()) {
                eff.setChosenColors(Lists.newArrayList(hostCard.getChosenColors()));
            }

            // Set Chosen Cards
            if (hostCard.hasChosenCard()) {
                eff.setChosenCards(hostCard.getChosenCards());
            }

            // Set Chosen Player
            if (hostCard.hasChosenPlayer()) {
                eff.setChosenPlayer(hostCard.getChosenPlayer());
            }

            // Set Chosen Type
            if (hostCard.hasChosenType()) {
                eff.setChosenType(hostCard.getChosenType());
            }
            if (hostCard.hasChosenType2()) {
                eff.setChosenType2(hostCard.getChosenType2());
            }

            // Set Chosen name
            if (!hostCard.getNamedCard().isEmpty()) {
                eff.setNamedCard(hostCard.getNamedCard());
            }

            // chosen number
            if (sa.hasParam("SetChosenNumber")) {
                eff.setChosenNumber(AbilityUtils.calculateAmount(hostCard,
                        sa.getParam("SetChosenNumber"), sa));
            } else if (hostCard.hasChosenNumber()) {
                eff.setChosenNumber(hostCard.getChosenNumber());
            }

            if (sa.hasParam("CopySVar")) {
                eff.setSVar(sa.getParam("CopySVar"), hostCard.getSVar(sa.getParam("CopySVar")));
            }

            // Copy text changes
            if (sa.isIntrinsic()) {
                eff.copyChangedTextFrom(hostCard);
            }

            if (sa.hasParam("AtEOT")) {
                registerDelayedTrigger(sa, sa.getParam("AtEOT"), Lists.newArrayList(hostCard));
            }

            if (duration == null || !duration.equals("Permanent")) {
                final GameCommand endEffect = new GameCommand() {
                    private static final long serialVersionUID = -5861759814760561373L;

                    @Override
                    public void run() {
                        game.getAction().exile(eff, null, null);
                    }
                };

                addUntilCommand(sa, endEffect, controller);
            }

            if (sa.hasParam("ImprintOnHost")) {
                hostCard.addImprintedCard(eff);
            }

            // TODO: Add targeting to the effect so it knows who it's dealing with
            game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
            game.getAction().moveTo(ZoneType.Command, eff, sa, params);
            eff.updateStateForView();
            game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
            //if (effectTriggers != null) {
            //    game.getTriggerHandler().registerActiveTrigger(cmdEffect, false);
            //}
        }
    }

}
