package forge.card.abilityFactory;


import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardUtil;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Player;

import forge.card.cardFactory.CardFactoryUtil;
import forge.gui.GuiUtils;

import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JOptionPane;

/**
 * <p>AbilityFactory_Choose class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_Choose {

    private AbilityFactory_Choose() {
        throw new AssertionError();
    }

    // *************************************************************************
    // ************************* ChooseType ************************************
    // *************************************************************************

    /**
     * <p>createAbilityChooseType.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityChooseType(final AbilityFactory af) {

        final SpellAbility abChooseType = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -7734286034988741837L;

            @Override
            public String getStackDescription() {
                return chooseTypeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return chooseTypeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                chooseTypeResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return chooseTypeTriggerAI(af, this, mandatory);
            }

        };
        return abChooseType;
    }

    /**
     * <p>createSpellChooseType.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellChooseType(final AbilityFactory af) {
        final SpellAbility spChooseType = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3395765985146644736L;

            @Override
            public String getStackDescription() {
                return chooseTypeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return chooseTypeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                chooseTypeResolve(af, this);
            }

        };
        return spChooseType;
    }

    /**
     * <p>createDrawbackChooseType.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackChooseType(final AbilityFactory af) {
        final SpellAbility dbChooseType = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 5555184803257696143L;

            @Override
            public String getStackDescription() {
                return chooseTypeStackDescription(af, this);
            }

            @Override
            public void resolve() {
                chooseTypeResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return chooseTypeTriggerAI(af, this, mandatory);
            }

        };
        return dbChooseType;
    }

    /**
     * <p>chooseTypeStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String chooseTypeStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        StringBuilder sb = new StringBuilder();

        if (!(sa instanceof Ability_Sub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        }
        else {
            sb.append(" ");
        }

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        }
        else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a type.");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>chooseTypeCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean chooseTypeCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        return chooseTypeTriggerAI(af, sa, false);
    }

    /**
     * <p>chooseTypeTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean chooseTypeTriggerAI(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory)
    {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getComputerPlayer());
        } else {
            ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(
                    sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
            for (Player p : tgtPlayers) {
                if (p.isHuman() && !mandatory) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * <p>chooseTypeResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void chooseTypeResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card card = af.getHostCard();
        String type = params.get("Type");
        ArrayList<String> invalidTypes = new ArrayList<String>();
        if (params.containsKey("InvalidTypes")) {
            invalidTypes.addAll(Arrays.asList(params.get("InvalidTypes").split(",")));
        }

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        }
        else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (Player p : tgtPlayers) {
            if (tgt == null || p.canTarget(sa)) {

                if (type.equals("Card")) {
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            Object o = GuiUtils.getChoice("Choose a card type", CardUtil.getCardTypes().toArray());
                            if (null == o) {
                                return;
                            }
                            String choice = (String) o;
                            if (CardUtil.isACardType(choice) && !invalidTypes.contains(choice)) {
                                valid = true;
                                card.setChosenType(choice);
                            }
                        } else {
                            //TODO
                            //computer will need to choose a type
                            //based on whether it needs a creature or land,
                            //otherwise, lib search for most common type left
                            //then, reveal chosenType to Human
                        }
                    }
                } else if (type.equals("Creature")) {
                    String chosenType = "";
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            chosenType = JOptionPane.showInputDialog(null, "Choose a creature type:", card.getName(),
                                    JOptionPane.QUESTION_MESSAGE);
                        }
                        else {
                            String chosen = "";
                            if (params.containsKey("AILogic")) {
                                String logic = params.get("AILogic");
                                if (logic.equals("MostProminentOnBattlefield")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(AllZoneUtil.getCardsIn(Zone.Battlefield));
                                }
                                if (logic.equals("MostProminentComputerControls")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(
                                            AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield));
                                }
                                if (logic.equals("MostProminentHumanControls")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(
                                            AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield));
                                }
                                if (logic.equals("MostProminentInComputerDeck")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(
                                            AllZoneUtil.getCardsInGame().getController(AllZone.getComputerPlayer()));
                                }
                            }
                            if (!CardUtil.isACreatureType(chosen) || invalidTypes.contains(chosen)) {
                                chosen = "Sliver";
                            }
                            GuiUtils.getChoice("Computer picked: ", chosen);
                            chosenType = chosen;
                        }
                        if (CardUtil.isACreatureType(chosenType) && !invalidTypes.contains(chosenType)) {
                            valid = true;
                            card.setChosenType(chosenType);
                        }
                    }
                }
                else if (type.equals("Basic Land")) {
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            Object o = GuiUtils.getChoice("Choose a basic land type",
                                    CardUtil.getBasicTypes().toArray());
                            if (null == o) {
                                return;
                            }
                            String choice = (String) o;
                            if (CardUtil.isABasicLandType(choice) && !invalidTypes.contains(choice)) {
                                valid = true;
                                card.setChosenType(choice);
                            }
                        } else {
                            //TODO
                            //computer will need to choose a type
                        }
                    }
                } //end if-else if
            }
        }
    }

    // *************************************************************************
    // ************************* ChooseColor ***********************************
    // *************************************************************************

    /**
     * <p>createAbilityChooseColor.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createAbilityChooseColor(final AbilityFactory af) {

        final SpellAbility abChooseColor = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 7069068165774633355L;

            @Override
            public String getStackDescription() {
                return chooseColorStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return chooseColorCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                chooseColorResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return chooseColorTriggerAI(af, this, mandatory);
            }

        };
        return abChooseColor;
    }

    /**
     * <p>createSpellChooseColor.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createSpellChooseColor(final AbilityFactory af) {
        final SpellAbility spChooseColor = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -5627273779759130247L;

            @Override
            public String getStackDescription() {
                return chooseColorStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return chooseColorCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                chooseColorResolve(af, this);
            }

        };
        return spChooseColor;
    }

    /**
     * <p>createDrawbackChooseColor.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createDrawbackChooseColor(final AbilityFactory af) {
        final SpellAbility dbChooseColor = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 6969618586164278998L;

            @Override
            public String getStackDescription() {
                return chooseColorStackDescription(af, this);
            }

            @Override
            public void resolve() {
                chooseColorResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return chooseColorTriggerAI(af, this, mandatory);
            }

        };
        return dbChooseColor;
    }

    /**
     * <p>chooseColorStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String chooseColorStackDescription(final AbilityFactory af, final SpellAbility sa) {
        StringBuilder sb = new StringBuilder();

        if (!(sa instanceof Ability_Sub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        }
        else {
            sb.append(" ");
        }

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        }
        else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        }

        for (Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a color.");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>chooseColorCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean chooseColorCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        return chooseColorTriggerAI(af, sa, false);
    }

    /**
     * <p>chooseColorTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean chooseColorTriggerAI(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory)
    {
        return false;
        /*if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getComputerPlayer());
        } else {
            ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                    af.getMapParams().get("Defined"), sa);
            for (Player p : tgtPlayers) {
                if (p.isHuman() && !mandatory) {
                    return false;
                }
            }
        }
        return true;*/
    }

    /**
     * <p>chooseColorResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void chooseColorResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card card = af.getHostCard();

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        }
        else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (Player p : tgtPlayers) {
            if (tgt == null || p.canTarget(sa)) {
                if (sa.getActivatingPlayer().isHuman()) {
                    Object o = GuiUtils.getChoice("Choose a color", Constant.Color.onlyColors);
                    if (null == o) {
                        return;
                    }
                    String choice = (String) o;
                    card.setChosenColor(choice);
                } else {
                    //TODO - needs improvement
                    card.setChosenColor(Constant.Color.Black);
                    String message = "Computer chooses " + Constant.Color.Black;
                    JOptionPane.showMessageDialog(null, message, "" + card, JOptionPane.PLAIN_MESSAGE);
                }
            }
        }
    }

} //end class AbilityFactory_Choose
