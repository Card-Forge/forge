package forge.card.abilityFactory;

import java.util.HashMap;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.Command;
import forge.ComputerUtil;
import forge.MyRandom;
import forge.Player;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

/**
 * <p>
 * AbilityFactory_Effect class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_Effect {
    /**
     * <p>
     * createAbilityEffect.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityEffect(final AbilityFactory AF) {

        final SpellAbility abEffect = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = 8869422603616247307L;

            final AbilityFactory af = AF;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return effectStackDescription(af, this);
            }

            public boolean canPlayAI() {
                return effectCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                effectResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return effectDoTriggerAI(af, this, mandatory);
            }

        };
        return abEffect;
    }

    /**
     * <p>
     * createSpellEffect.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellEffect(final AbilityFactory AF) {
        final SpellAbility spEffect = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = 6631124959690157874L;

            final AbilityFactory af = AF;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return effectStackDescription(af, this);
            }

            public boolean canPlayAI() {
                return effectCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                effectResolve(af, this);
            }

        };
        return spEffect;
    }

    /**
     * <p>
     * createDrawbackEffect.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackEffect(final AbilityFactory AF) {
        final SpellAbility dbEffect = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
            private static final long serialVersionUID = 6631124959690157874L;

            final AbilityFactory af = AF;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is
                // happening
                return effectStackDescription(af, this);
            }

            public boolean canPlayAI() {
                return effectCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                effectResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return effectDoTriggerAI(af, this, mandatory);
            }

        };
        return dbEffect;
    }

    /**
     * <p>
     * effectStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    public static String effectStackDescription(final AbilityFactory af, final SpellAbility sa) {
        StringBuilder sb = new StringBuilder();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        }

        sb.append(sa.getDescription());

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * effectCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public static boolean effectCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        Random r = MyRandom.random;

        Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(AllZone.getHumanPlayer());
            } else {
                tgt.addTarget(AllZone.getComputerPlayer());
            }
        }

        return ((r.nextFloat() < .6667));
    }

    /**
     * <p>
     * effectDoTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    public static boolean effectDoTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa) && !mandatory) {
            // payment it's usually
                                                        // not mandatory
            return false;
        }

        // TODO: Add targeting effects

        // check SubAbilities DoTrigger?
        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * effectResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void effectResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card card = af.getHostCard();

        String[] effectAbilities = null;
        String[] effectTriggers = null;
        String[] effectSVars = null;
        String[] effectKeywords = null;
        String[] effectStaticAbilities = null;
        String effectRemembered = null;

        if (params.containsKey("Abilities")) {
            effectAbilities = params.get("Abilities").split(",");
        }

        if (params.containsKey("Triggers")) {
            effectTriggers = params.get("Triggers").split(",");
        }

        if (params.containsKey("StaticAbilities")) {
            effectStaticAbilities = params.get("StaticAbilities").split(",");
        }

        if (params.containsKey("SVars")) {
            effectSVars = params.get("SVars").split(",");
        }

        if (params.containsKey("Keywords")) {
            effectKeywords = params.get("Keywords").split(",");
        }

        if (params.containsKey("RememberCard")) {
            effectRemembered = params.get("RememberCard");
        }

        // Effect eff = new Effect();
        String name = params.get("Name");
        if (name == null) {
            name = sa.getSourceCard().getName() + "'s Effect";
        }

        // Unique Effects shouldn't be duplicated
        if (params.containsKey("Unique") && AllZoneUtil.isCardInPlay(name)) {
            return;
        }

        Player controller = sa.getActivatingPlayer();
        Card eff = new Card();
        eff.setName(name);
        eff.addType("Effect"); // Or Emblem
        eff.setToken(true); // Set token to true, so when leaving play it gets
                            // nuked
        eff.addController(controller);
        eff.setOwner(controller);
        eff.setImageName(card.getImageName());
        eff.setColor(card.getColor());
        eff.setImmutable(true);

        // Effects should be Orange or something probably

        final Card e = eff;

        // Abilities, triggers and SVars work the same as they do for Token
        // Grant abilities
        if (effectAbilities != null) {
            for (String s : effectAbilities) {
                AbilityFactory abFactory = new AbilityFactory();
                String actualAbility = af.getHostCard().getSVar(s);

                SpellAbility grantedAbility = abFactory.getAbility(actualAbility, eff);
                eff.addSpellAbility(grantedAbility);
            }
        }

        // Grant triggers
        if (effectTriggers != null) {
            for (String s : effectTriggers) {
                String actualTrigger = af.getHostCard().getSVar(s);

                Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, eff, true);
                eff.addTrigger(parsedTrigger);
            }
        }

        // Grant static abilities
        if (effectStaticAbilities != null) {
            for (String s : effectStaticAbilities)
                eff.addStaticAbility(af.getHostCard().getSVar(s));
        }

        // Grant SVars
        if (effectSVars != null) {
            for (String s : effectSVars) {
                String actualSVar = af.getHostCard().getSVar(s);
                eff.setSVar(s, actualSVar);
            }
        }

        // Grant Keywords
        if (effectKeywords != null) {
            for (String s : effectKeywords) {
                String actualKeyword = af.getHostCard().getSVar(s);
                eff.addIntrinsicKeyword(actualKeyword);
            }
        }

        // Set Remembered
        if (effectRemembered != null) {
            for (Card c : AbilityFactory.getDefinedCards(card, effectRemembered, sa)) {
                eff.addRemembered(c);
            }
        }

        // Set Chosen Color(s)
        if (!card.getChosenColor().isEmpty()) {
            eff.setChosenColor(card.getChosenColor());
        }

        // Duration
        String duration = params.get("Duration");
        if (duration == null || !duration.equals("Permanent")) {
            final Command endEffect = new Command() {
                private static final long serialVersionUID = -5861759814760561373L;

                public void execute() {
                    AllZone.getGameAction().exile(e);
                }
            };

            if (duration == null || duration.equals("EndOfTurn")) {
                AllZone.getEndOfTurn().addUntil(endEffect);
            }
        }

        // TODO: Add targeting to the effect so it knows who it's dealing with
        AllZone.getTriggerHandler().suppressMode("ChangesZone");
        AllZone.getGameAction().moveToPlay(eff);
        AllZone.getTriggerHandler().clearSuppression("ChangesZone");
    }
}
