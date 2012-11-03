package forge.card.abilityfactory.effects;

import java.util.ArrayList;

import forge.Card;
import forge.Command;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.replacement.ReplacementEffect;
import forge.card.replacement.ReplacementHandler;
import forge.card.spellability.SpellAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.card.trigger.TriggerType;
import forge.game.player.Player;

public class EffectEffect extends SpellEffect {

    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        return sa.getDescription();
    }

    /**
     * <p>
     * effectResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card hostCard = sa.getAbilityFactory().getHostCard();

        String[] effectAbilities = null;
        String[] effectTriggers = null;
        String[] effectSVars = null;
        String[] effectKeywords = null;
        String[] effectStaticAbilities = null;
        String[] effectReplacementEffects = null;
        String effectRemembered = null;
        String effectImprinted = null;
        Player ownerEff = null;

        if (params.containsKey("Abilities")) {
            effectAbilities = params.get("Abilities").split(",");
        }

        if (params.containsKey("Triggers")) {
            effectTriggers = params.get("Triggers").split(",");
        }

        if (params.containsKey("StaticAbilities")) {
            effectStaticAbilities = params.get("StaticAbilities").split(",");
        }

        if (params.containsKey("ReplacementEffects")) {
            effectReplacementEffects = params.get("ReplacementEffects").split(",");
        }

        if (params.containsKey("SVars")) {
            effectSVars = params.get("SVars").split(",");
        }

        if (params.containsKey("Keywords")) {
            effectKeywords = params.get("Keywords").split(",");
        }

        if (params.containsKey("RememberObjects")) {
            effectRemembered = params.get("RememberObjects");
        }

        if (params.containsKey("ImprintCards")) {
            effectImprinted = params.get("ImprintCards");
        }

        // Effect eff = new Effect();
        String name = params.get("Name");
        if (name == null) {
            name = sa.getSourceCard().getName() + "'s Effect";
        }

        // Unique Effects shouldn't be duplicated
        if (params.containsKey("Unique") && Singletons.getModel().getGame().isCardInPlay(name)) {
            return;
        }

        if (params.containsKey("EffectOwner")) {
            ArrayList<Player> effectOwner;
            effectOwner = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("EffectOwner"), sa);
            ownerEff = effectOwner.get(0);
        }

        final Player controller = params.containsKey("EffectOwner") ? ownerEff : sa.getActivatingPlayer();
        final Card eff = new Card();
        eff.setName(name);
        eff.addType("Effect"); // Or Emblem
        eff.setToken(true); // Set token to true, so when leaving play it gets
                            // nuked
        eff.addController(controller);
        eff.setOwner(controller);
        eff.setImageName(hostCard.getImageName());
        eff.setColor(hostCard.getColor());
        eff.setImmutable(true);
        eff.setEffectSource(hostCard);
        if (params.containsKey("Image")) {
            eff.setImageName(params.get("Image"));
        }

        // Effects should be Orange or something probably

        final Card e = eff;

        // Abilities, triggers and SVars work the same as they do for Token
        // Grant abilities
        if (effectAbilities != null) {
            for (final String s : effectAbilities) {
                final AbilityFactory abFactory = new AbilityFactory();
                final String actualAbility = hostCard.getSVar(s);

                final SpellAbility grantedAbility = abFactory.getAbility(actualAbility, eff);
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
            for (final Object o : AbilityFactory.getDefinedObjects(hostCard, effectRemembered, sa)) {
                eff.addRemembered(o);
            }
        }

        // Set Imprinted
        if (effectImprinted != null) {
            for (final Card c : AbilityFactory.getDefinedCards(hostCard, effectImprinted, sa)) {
                eff.addImprinted(c);
            }
        }

        // Set Chosen Color(s)
        if (!hostCard.getChosenColor().isEmpty()) {
            eff.setChosenColor(hostCard.getChosenColor());
        }

        // Remember created effect
        if (params.containsKey("RememberEffect")) {
            Singletons.getModel().getGame().getCardState(hostCard).addRemembered(eff);
        }

        // Duration
        final String duration = params.get("Duration");
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
        Singletons.getModel().getGame().getAction().moveToPlay(eff);
        Singletons.getModel().getGame().getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
    }

} 