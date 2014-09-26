package forge.game.ability.effects;

import forge.GameCommand;
import forge.ImageKeys;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

import java.util.List;

public class EffectEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getDescription();
    }

    /**
     * <p>
     * effectResolve.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     */

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();

        String[] effectAbilities = null;
        String[] effectTriggers = null;
        String[] effectSVars = null;
        String[] effectKeywords = null;
        String[] effectStaticAbilities = null;
        String[] effectReplacementEffects = null;
        String effectRemembered = null;
        String effectImprinted = null;
        Player ownerEff = null;

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

        if (sa.hasParam("SVars")) {
            effectSVars = sa.getParam("SVars").split(",");
        }

        if (sa.hasParam("Keywords")) {
            effectKeywords = sa.getParam("Keywords").split(",");
        }

        if (sa.hasParam("RememberObjects")) {
            effectRemembered = sa.getParam("RememberObjects");
        }

        if (sa.hasParam("ImprintCards")) {
            effectImprinted = sa.getParam("ImprintCards");
        }

        // Effect eff = new Effect();
        String name = sa.getParam("Name");
        if (name == null) {
            name = sa.getHostCard().getName() + "'s Effect";
        }

        // Unique Effects shouldn't be duplicated
        if (sa.hasParam("Unique") && game.isCardInCommand(name)) {
            return;
        }

        if (sa.hasParam("EffectOwner")) {
            List<Player> effectOwner = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("EffectOwner"), sa);
            ownerEff = effectOwner.get(0);
        }

        final Player controller = sa.hasParam("EffectOwner") ? ownerEff : sa.getActivatingPlayer();
        final Card eff = new Card(controller.getGame().nextCardId());
        eff.setName(name);
        eff.addType("Effect"); // Or Emblem
        eff.setToken(true); // Set token to true, so when leaving play it gets nuked
        eff.setOwner(controller);
        eff.setImageKey(sa.hasParam("Image") ? ImageKeys.getTokenKey(sa.getParam("Image")) : hostCard.getImageKey());
        eff.setColor(hostCard.getColor());
        eff.setImmutable(true);
        eff.setEffectSource(hostCard);

        // Effects should be Orange or something probably

        final Card e = eff;

        // Abilities, triggers and SVars work the same as they do for Token
        // Grant abilities
        if (effectAbilities != null) {
            for (final String s : effectAbilities) {
                final String actualAbility = hostCard.getSVar(s);

                final SpellAbility grantedAbility = AbilityFactory.getAbility(actualAbility, eff);
                eff.addSpellAbility(grantedAbility);
                grantedAbility.setIntrinsic(true);
            }
        }

        // Grant triggers
        if (effectTriggers != null) {
            for (final String s : effectTriggers) {
                final String actualTrigger = hostCard.getSVar(s);

                final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, eff, true);
                final Trigger addedTrigger = eff.addTrigger(parsedTrigger);
                addedTrigger.setIntrinsic(true);
            }
        }

        // Grant static abilities
        if (effectStaticAbilities != null) {
            for (final String s : effectStaticAbilities) {
                final StaticAbility addedStaticAbility = eff.addStaticAbility(hostCard.getSVar(s));
                addedStaticAbility.setIntrinsic(true);
            }
        }

        // Grant replacement effects
        if (effectReplacementEffects != null) {
            for (final String s : effectReplacementEffects) {
                final String actualReplacement = hostCard.getSVar(s);

                final ReplacementEffect parsedReplacement = ReplacementHandler.parseReplacement(actualReplacement, eff, true);
                final ReplacementEffect addedReplacement = eff.addReplacementEffect(parsedReplacement);
                addedReplacement.setIntrinsic(true);
            }
        }

        // Grant SVars
        if (effectSVars != null) {
            for (final String s : effectSVars) {
                final String actualSVar = hostCard.getSVar(s);
                eff.setSVar(s, actualSVar);
            }
        }

        // Grant Keywords
        if (effectKeywords != null) {
            for (final String s : effectKeywords) {
                final String actualKeyword = hostCard.getSVar(s);
                eff.addIntrinsicKeyword(actualKeyword);
            }
        }

        // Set Remembered
        if (effectRemembered != null) {
            for (final String rem : effectRemembered.split(",")) {
                for (final Object o : AbilityUtils.getDefinedObjects(hostCard, rem, sa)) {
                    eff.addRemembered(o);
                }
            }
        }

        // Set Imprinted
        if (effectImprinted != null) {
            for (final Card c : AbilityUtils.getDefinedCards(hostCard, effectImprinted, sa)) {
                eff.addImprinted(c);
            }
        }

        // Set Chosen Color(s)
        if (!hostCard.getChosenColor().isEmpty()) {
            eff.setChosenColor(hostCard.getChosenColor());
        }

        // Set Chosen name
        if (!hostCard.getNamedCard().isEmpty()) {
            eff.setNamedCard(hostCard.getNamedCard());
        }

        // Copy text changes
        if (sa.isIntrinsic()) {
            eff.copyChangedTextFrom(hostCard);
        }

        // Duration
        final String duration = sa.getParam("Duration");
        if ((duration == null) || !duration.equals("Permanent")) {
            final GameCommand endEffect = new GameCommand() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void run() {
                    game.getAction().exile(e);
                }
            };

            if ((duration == null) || duration.equals("EndOfTurn")) {
                game.getEndOfTurn().addUntil(endEffect);
            }
            else if (duration.equals("UntilHostLeavesPlay")) {
                hostCard.addLeavesPlayCommand(endEffect);
            }
            else if (duration.equals("HostLeavesOrEOT")) {
                game.getEndOfTurn().addUntil(endEffect);
                hostCard.addLeavesPlayCommand(endEffect);
            }
            else if (duration.equals("UntilYourNextTurn")) {
                game.getCleanup().addUntil(controller, endEffect);
            }
            else if (duration.equals("UntilEndOfCombat")) {
                game.getEndOfCombat().addUntil(endEffect);
            }
        }

        // TODO: Add targeting to the effect so it knows who it's dealing with
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        Card cmdEffect = game.getAction().moveTo(ZoneType.Command, eff);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        if (effectTriggers != null) {
            game.getTriggerHandler().registerActiveTrigger(cmdEffect, false);
        }
    }

}
