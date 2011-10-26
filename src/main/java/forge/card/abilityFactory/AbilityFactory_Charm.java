package forge.card.abilityFactory;

import java.util.ArrayList;

import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;

// Charm specific params:
// Choices - a ","-delimited list of SVars containing ability choices

/**
 * <p>
 * AbilityFactory_Charm class.
 * </p>
 * 
 * @author Forge
 */
public final class AbilityFactory_Charm {

    private AbilityFactory_Charm() {
        throw new AssertionError();
    }

    /**
     * <p>
     * createAbilityCharm.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityCharm(final AbilityFactory af) {
        final SpellAbility abCharm = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4038591081733095021L;

            @Override
            public boolean canPlayAI() {
                return charmCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                charmResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return charmStackDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return charmCanPlayAI(af, this);
            }
        }; // Ability_Activated

        return abCharm;
    }

    /**
     * <p>
     * createSpellCharm.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellCharm(final AbilityFactory af) {
        final SpellAbility spCharm = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -7297235470289087240L;

            @Override
            public boolean canPlayAI() {
                return charmCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                charmResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return charmStackDescription(af, this);
            }
        };

        return spCharm;
    }

    private static String charmStackDescription(final AbilityFactory af, final SpellAbility sa) {
        StringBuilder sb = new StringBuilder();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }
        // end standard begin

        // nothing stack specific for Charm

        // begin standard post
        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private static boolean charmCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // TODO - enable Charms for the AI
        return false;
    }

    private static void charmResolve(final AbilityFactory af, final SpellAbility sa) {
        // nothing to do. Ability_Subs are set up in
        // GameAction.playSpellAbility(),
        // and that Ability_Sub.resolve() is called from AbilityFactory
    }

    /**
     * Sets the up charm s as.
     * 
     * @param sa
     *            the new up charm s as
     */
    public static void setupCharmSAs(final SpellAbility sa) {
        // make Charm choices
        if (sa.isCharm()) {
            ArrayList<SpellAbility> choices = new ArrayList<SpellAbility>();
            choices.addAll(sa.getCharmChoices());
            for (int i = 0; i < choices.size(); i++) {
                if (!sa.canPlay()) {
                    choices.remove(sa);
                }
            }
            for (int i = 0; i < sa.getCharmNumber(); i++) {
                Object o;
                if (i < sa.getMinCharmNumber()) {
                    o = GuiUtils.getChoice("Choose a mode", choices.toArray());
                } else {
                    o = GuiUtils.getChoiceOptional("Choose a mode", choices.toArray());
                }
                if (null == o) {
                    break;
                }
                Ability_Sub chosen = (Ability_Sub) o;
                sa.addCharmChoice(chosen);
                choices.remove(chosen);

                // walk down the SpellAbility tree and add to the child
                // Ability_Sub
                SpellAbility child = sa;
                while (child.getSubAbility() != null) {
                    child = child.getSubAbility();
                }
                child.setSubAbility(chosen);
                if (chosen.getActivatingPlayer() == null) {
                    chosen.setActivatingPlayer(child.getActivatingPlayer());
                }
                chosen.setParent(child);
            }
        }
    }

} // end class AbilityFactory_Charm
