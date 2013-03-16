package forge.card.ability.effects;

import java.util.List;

import forge.Card;
import forge.Command;
import forge.Singletons;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.replacement.ReplacementEffect;
import forge.card.replacement.ReplacementHandler;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

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
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     */

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();

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
            name = sa.getSourceCard().getName() + "'s Effect";
        }

        // Unique Effects shouldn't be duplicated
        if (sa.hasParam("Unique") && Singletons.getModel().getGame().isCardInPlay(name)) {
            return;
        }

        if (sa.hasParam("EffectOwner")) {
            List<Player> effectOwner = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("EffectOwner"), sa);
            ownerEff = effectOwner.get(0);
        }

        final Player controller = sa.hasParam("EffectOwner") ? ownerEff : sa.getActivatingPlayer();
        final Card eff = new Card();
        eff.setName(name);
        eff.addType("Effect"); // Or Emblem
        eff.setToken(true); // Set token to true, so when leaving play it gets nuked
        eff.setOwner(controller);
        eff.setImageFilename(sa.hasParam("Image") ? sa.getParam("Image") : hostCard.getImageFilename());
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
            }
        }

        // Grant triggers
        if (effectTriggers != null) {
            for (final String s : effectTriggers) {
                final String actualTrigger = hostCard.getSVar(s);

                final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, eff, true);
                eff.addTrigger(parsedTrigger);
            }
        }

        // Grant static abilities
        if (effectStaticAbilities != null) {
            for (final String s : effectStaticAbilities) {
                eff.addStaticAbility(hostCard.getSVar(s));
            }
        }

        // Grant replacement effects
        if (effectReplacementEffects != null) {
            for (final String s : effectReplacementEffects) {
                final String actualReplacement = hostCard.getSVar(s);

                final ReplacementEffect parsedReplacement = ReplacementHandler.parseReplacement(actualReplacement, eff);
                eff.addReplacementEffect(parsedReplacement);
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
            for (final Object o : AbilityUtils.getDefinedObjects(hostCard, effectRemembered, sa)) {
                eff.addRemembered(o);
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

        // Remember created effect
        if (sa.hasParam("RememberEffect")) {
            Singletons.getModel().getGame().getCardState(hostCard).addRemembered(eff);
        }

        // Duration
        final String duration = sa.getParam("Duration");
        if ((duration == null) || !duration.equals("Permanent")) {
            final Command endEffect = new Command() {
                private static final long serialVersionUID = -5861759814760561373L;

                @Override
                public void execute() {
                    Singletons.getModel().getGame().getAction().exile(e);
                }
            };

            if ((duration == null) || duration.equals("EndOfTurn")) {
                Singletons.getModel().getGame().getEndOfTurn().addUntil(endEffect);
            }
            else if (duration.equals("UntilHostLeavesPlay")) {
                hostCard.addLeavesPlayCommand(endEffect);
            }
            else if (duration.equals("HostLeavesOrEOT")) {
                Singletons.getModel().getGame().getEndOfTurn().addUntil(endEffect);
                hostCard.addLeavesPlayCommand(endEffect);
            }
            else if (duration.equals("UntilYourNextTurn")) {
                Singletons.getModel().getGame().getCleanup().addUntilYourNextTurn(controller, endEffect);
            }
        }

        // TODO: Add targeting to the effect so it knows who it's dealing with
        Singletons.getModel().getGame().getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        Singletons.getModel().getGame().getAction().moveTo(ZoneType.Command, eff);
        Singletons.getModel().getGame().getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
    }

}
