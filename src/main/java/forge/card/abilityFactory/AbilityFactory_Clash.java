package forge.card.abilityFactory;

import forge.AllZone;
import forge.Card;
import forge.GameActionUtil;
import forge.Player;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>AbilityFactory_Clash class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_Clash {

    /**
     * <p>getAbilityClash.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getAbilityClash(final AbilityFactory AF) {
        final SpellAbility abClash = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
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
            public boolean doTrigger(boolean mandatory) {
                return true;
            }

            @Override
            public String getStackDescription() {
                return AF.getHostCard().getName() + " - Clash with an opponent.";
            }

            @Override
            public void resolve() {
                clashResolve(AF, this);
            }
        };

        return abClash;
    }

    /**
     * <p>getSpellClash.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getSpellClash(final AbilityFactory AF) {
        final SpellAbility spClash = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
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
            public boolean doTrigger(boolean mandatory) {
                return true;
            }

            @Override
            public String getStackDescription() {
                return AF.getHostCard().getName() + " - Clash with an opponent.";
            }

            @Override
            public void resolve() {
                clashResolve(AF, this);
            }
        };

        return spClash;
    }

    /**
     * <p>getDrawbackClash.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getDrawbackClash(final AbilityFactory AF) {
        final SpellAbility dbClash = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
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
            public boolean doTrigger(boolean mandatory) {
                return true;
            }

            @Override
            public String getStackDescription() {
                return AF.getHostCard().getName() + " - Clash with an opponent.";
            }

            @Override
            public void resolve() {
                clashResolve(AF, this);
            }
        };

        return dbClash;
    }

    /**
     * <p>clashResolve.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param SA a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void clashResolve(final AbilityFactory AF, final SpellAbility SA) {
        AbilityFactory AF_Outcomes = new AbilityFactory();
        boolean victory = AF.getHostCard().getController().clashWithOpponent(AF.getHostCard());

        //Run triggers
        HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", AF.getHostCard().getController());

        if (victory) {
            if (AF.getMapParams().containsKey("WinSubAbility")) {
                SpellAbility win = AF_Outcomes.getAbility(AF.getHostCard().getSVar(AF.getMapParams().get("WinSubAbility")), AF.getHostCard());
                win.setActivatingPlayer(AF.getHostCard().getController());
                ((Ability_Sub) win).setParent(SA);

                AbilityFactory.resolve(win, false);
            }
            runParams.put("Won", "True");
        } else {
            if (AF.getMapParams().containsKey("OtherwiseSubAbility")) {
                SpellAbility otherwise = AF_Outcomes.getAbility(AF.getHostCard().getSVar(AF.getMapParams().get("OtherwiseSubAbility")), AF.getHostCard());
                otherwise.setActivatingPlayer(AF.getHostCard().getController());
                ((Ability_Sub) otherwise).setParent(SA);

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
     * <p>createAbilityFlip.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
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
            public boolean doTrigger(boolean mandatory) {
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
     * <p>createSpellFlip.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
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
            public boolean doTrigger(boolean mandatory) {
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
     * <p>createDrawbackFlip.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
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
            public boolean doTrigger(boolean mandatory) {
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
     * <p>flipGetStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String flipGetStackDescription(AbilityFactory af, SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card host = af.getHostCard();
        Player player = params.containsKey("OpponentCalls") ? host.getController().getOpponent() : host.getController();

        StringBuilder sb = new StringBuilder();

        if (!(sa instanceof Ability_Sub))
            sb.append(sa.getSourceCard()).append(" - ");
        else
            sb.append(" ");

        sb.append(player).append(" flips a coin.");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>flipResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void flipResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card host = af.getHostCard();
        Player player = host.getController();
    	
    	ArrayList<Player> caller = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Caller"), sa);
		if(caller.size() == 0) caller.add(player);

    	AbilityFactory AF_Outcomes = new AbilityFactory();
    	boolean victory = GameActionUtil.flipACoin(caller.get(0), sa.getSourceCard());

        //Run triggers
        //HashMap<String,Object> runParams = new HashMap<String,Object>();
        //runParams.put("Player", player);
    	if (params.get("RememberAll") != null){
    	    host.addRemembered(host);
    	}

        if (victory) {
            if (params.get("RememberWinner") != null){
                host.addRemembered(host);
            }
            if (params.containsKey("WinSubAbility")) {
                SpellAbility win = AF_Outcomes.getAbility(host.getSVar(params.get("WinSubAbility")), host);
                win.setActivatingPlayer(player);
                ((Ability_Sub) win).setParent(sa);

    			AbilityFactory.resolve(win, false);
    		}
    		//runParams.put("Won","True");
    	}
    	else {
    	    if (params.get("RememberLoser") != null){
                host.addRemembered(host);
            }
    		if(params.containsKey("LoseSubAbility")) {
    			SpellAbility lose = AF_Outcomes.getAbility(host.getSVar(params.get("LoseSubAbility")), host);
    			lose.setActivatingPlayer(player);
    			((Ability_Sub)lose).setParent(sa);

    			AbilityFactory.resolve(lose, false);
    		}
    		//runParams.put("Won","False");
    	}

        //AllZone.getTriggerHandler().runTrigger("FlipsACoin",runParams);
    }

}//end class AbilityFactory_Clash
