package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.Card;
import forge.GameActionUtil;
import forge.Player;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * AbilityFactory_Clash class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactory_Clash {

    private AbilityFactory_Clash() {
        throw new AssertionError();
    }

    /**
     * <p>
     * getAbilityClash.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getAbilityClash(final AbilityFactory af) {
        final SpellAbility abClash = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -8019637116128196248L;

            @Override
            public boolean canPlayAI() {
                return true;
            }

            @Override
            public boolean canPlay() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public String getStackDescription() {
                return af.getHostCard().getName() + " - Clash with an opponent.";
            }

            @Override
            public void resolve() {
                clashResolve(af, this);
            }
        };

        return abClash;
    }

    /**
     * <p>
     * getSpellClash.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getSpellClash(final AbilityFactory af) {
        final SpellAbility spClash = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4991665176268317172L;

            @Override
            public boolean canPlayAI() {
                return true;
            }

            @Override
            public boolean canPlay() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public String getStackDescription() {
                return af.getHostCard().getName() + " - Clash with an opponent.";
            }

            @Override
            public void resolve() {
                clashResolve(af, this);
            }
        };

        return spClash;
    }

    /**
     * <p>
     * getDrawbackClash.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getDrawbackClash(final AbilityFactory af) {
        final SpellAbility dbClash = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -3850086157052881360L;

            @Override
            public boolean canPlayAI() {
                return true;
            }

            @Override
            public boolean canPlay() {
                return true;
            }

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public String getStackDescription() {
                return af.getHostCard().getName() + " - Clash with an opponent.";
            }

            @Override
            public void resolve() {
                clashResolve(af, this);
            }
        };

        return dbClash;
    }

    /**
     * <p>
     * clashResolve.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param SA
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void clashResolve(final AbilityFactory af, final SpellAbility sa) {
        AbilityFactory afOutcomes = new AbilityFactory();
        boolean victory = af.getHostCard().getController().clashWithOpponent(af.getHostCard());

        // Run triggers
        HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", af.getHostCard().getController());

        if (victory) {
            if (af.getMapParams().containsKey("WinSubAbility")) {
                SpellAbility win = afOutcomes.getAbility(
                        af.getHostCard().getSVar(af.getMapParams().get("WinSubAbility")), af.getHostCard());
                win.setActivatingPlayer(af.getHostCard().getController());
                ((Ability_Sub) win).setParent(sa);

                AbilityFactory.resolve(win, false);
            }
            runParams.put("Won", "True");
        } else {
            if (af.getMapParams().containsKey("OtherwiseSubAbility")) {
                SpellAbility otherwise = afOutcomes.getAbility(
                        af.getHostCard().getSVar(af.getMapParams().get("OtherwiseSubAbility")), af.getHostCard());
                otherwise.setActivatingPlayer(af.getHostCard().getController());
                ((Ability_Sub) otherwise).setParent(sa);

                AbilityFactory.resolve(otherwise, false);
            }
            runParams.put("Won", "False");
        }

        AllZone.getTriggerHandler().runTrigger("Clashed", runParams);
    }

    // *************************************************************************
    // ************************* FlipACoin *************************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityFlip.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createAbilityFlip(final AbilityFactory af) {
        final SpellAbility abFlip = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -8293336773930687488L;

            @Override
            public boolean canPlayAI() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public String getStackDescription() {
                return flipGetStackDescription(af, this);
            }

            @Override
            public void resolve() {
                flipResolve(af, this);
            }
        };

        return abFlip;
    }

    /**
     * <p>
     * createSpellFlip.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createSpellFlip(final AbilityFactory af) {
        final SpellAbility spFlip = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4402144245527547151L;

            @Override
            public boolean canPlayAI() {
                return true;
            }

            @Override
            public boolean canPlay() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public String getStackDescription() {
                return flipGetStackDescription(af, this);
            }

            @Override
            public void resolve() {
                flipResolve(af, this);
            }
        };

        return spFlip;
    }

    /**
     * <p>
     * createDrawbackFlip.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createDrawbackFlip(final AbilityFactory af) {
        final SpellAbility dbFlip = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 8581978154811461324L;

            @Override
            public boolean canPlayAI() {
                return true;
            }

            @Override
            public boolean canPlay() {
                return true;
            }

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public String getStackDescription() {
                return flipGetStackDescription(af, this);
            }

            @Override
            public void resolve() {
                flipResolve(af, this);
            }
        };

        return dbFlip;
    }

    /**
     * <p>
     * flipGetStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String flipGetStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card host = af.getHostCard();
        Player player = params.containsKey("OpponentCalls") ? host.getController().getOpponent() : host.getController();

        StringBuilder sb = new StringBuilder();

        if (!(sa instanceof Ability_Sub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        sb.append(player).append(" flips a coin.");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * flipResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void flipResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card host = af.getHostCard();
        Player player = host.getController();

        ArrayList<Player> caller = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Caller"), sa);
        if (caller.size() == 0) {
            caller.add(player);
        }

        AbilityFactory afOutcomes = new AbilityFactory();
        boolean victory = GameActionUtil.flipACoin(caller.get(0), sa.getSourceCard());

        // Run triggers
        // HashMap<String,Object> runParams = new HashMap<String,Object>();
        // runParams.put("Player", player);
        if (params.get("RememberAll") != null) {
            host.addRemembered(host);
        }

        if (victory) {
            if (params.get("RememberWinner") != null) {
                host.addRemembered(host);
            }
            if (params.containsKey("WinSubAbility")) {
                SpellAbility win = afOutcomes.getAbility(host.getSVar(params.get("WinSubAbility")), host);
                win.setActivatingPlayer(player);
                ((Ability_Sub) win).setParent(sa);

                AbilityFactory.resolve(win, false);
            }
            // runParams.put("Won","True");
        } else {
            if (params.get("RememberLoser") != null) {
                host.addRemembered(host);
            }
            if (params.containsKey("LoseSubAbility")) {
                SpellAbility lose = afOutcomes.getAbility(host.getSVar(params.get("LoseSubAbility")), host);
                lose.setActivatingPlayer(player);
                ((Ability_Sub) lose).setParent(sa);

                AbilityFactory.resolve(lose, false);
            }
            // runParams.put("Won","False");
        }

        // AllZone.getTriggerHandler().runTrigger("FlipsACoin",runParams);
    }

} // end class AbilityFactory_Clash
