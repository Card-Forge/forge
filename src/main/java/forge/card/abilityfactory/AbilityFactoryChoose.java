package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.Player;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;
import forge.gui.ListChooser;
import forge.item.CardDb;
import forge.item.CardPrinted;

/**
 * <p>
 * AbilityFactory_Choose class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactoryChoose {

    private AbilityFactoryChoose() {
        throw new AssertionError();
    }

    // *************************************************************************
    // ************************* ChooseType ************************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityChooseType.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityChooseType(final AbilityFactory af) {

        final SpellAbility abChooseType = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -7734286034988741837L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseTypeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseTypeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseTypeResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseTypeTriggerAI(af, this, mandatory);
            }

        };
        return abChooseType;
    }

    /**
     * <p>
     * createSpellChooseType.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellChooseType(final AbilityFactory af) {
        final SpellAbility spChooseType = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3395765985146644736L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseTypeStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseTypeCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseTypeResolve(af, this);
            }

        };
        return spChooseType;
    }

    /**
     * <p>
     * createDrawbackChooseType.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackChooseType(final AbilityFactory af) {
        final SpellAbility dbChooseType = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 5555184803257696143L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseTypeStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseTypeResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseTypeTriggerAI(af, this, mandatory);
            }

        };
        return dbChooseType;
    }

    /**
     * <p>
     * chooseTypeStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String chooseTypeStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a type.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * chooseTypeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean chooseTypeCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryChoose.chooseTypeTriggerAI(af, sa, false);
    }

    /**
     * <p>
     * chooseTypeTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean chooseTypeTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getComputerPlayer());
        } else {
            final ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams()
                    .get("Defined"), sa);
            for (final Player p : tgtPlayers) {
                if (p.isHuman() && !mandatory) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * chooseTypeResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void chooseTypeResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = af.getHostCard();
        final String type = params.get("Type");
        final ArrayList<String> invalidTypes = new ArrayList<String>();
        if (params.containsKey("InvalidTypes")) {
            invalidTypes.addAll(Arrays.asList(params.get("InvalidTypes").split(",")));
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {

                if (type.equals("Card")) {
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            final Object o = GuiUtils
                                    .getChoice("Choose a card type", CardUtil.getCardTypes().toArray());
                            if (null == o) {
                                return;
                            }
                            final String choice = (String) o;
                            if (CardUtil.isACardType(choice) && !invalidTypes.contains(choice)) {
                                valid = true;
                                card.setChosenType(choice);
                            }
                        } else {
                            // TODO
                            // computer will need to choose a type
                            // based on whether it needs a creature or land,
                            // otherwise, lib search for most common type left
                            // then, reveal chosenType to Human
                        }
                    }
                } else if (type.equals("Creature")) {
                    String chosenType = "";
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            final ArrayList<String> validChoices = CardUtil.getCreatureTypes();
                            for (final String s : invalidTypes) {
                                validChoices.remove(s);
                            }
                            final Object o = GuiUtils.getChoice("Choose a creature type", validChoices.toArray());
                            if (null == o) {
                                return;
                            }
                            final String choice = (String) o;
                            if (CardUtil.isACreatureType(choice) && !invalidTypes.contains(choice)) {
                                valid = true;
                                card.setChosenType(choice);
                            }
                        } else {
                            String chosen = "";
                            if (params.containsKey("AILogic")) {
                                final String logic = params.get("AILogic");
                                if (logic.equals("MostProminentOnBattlefield")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(AllZoneUtil
                                            .getCardsIn(Zone.Battlefield));
                                }
                                if (logic.equals("MostProminentComputerControls")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(AllZone.getComputerPlayer()
                                            .getCardsIn(Zone.Battlefield));
                                }
                                if (logic.equals("MostProminentHumanControls")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(AllZone.getHumanPlayer()
                                            .getCardsIn(Zone.Battlefield));
                                }
                                if (logic.equals("MostProminentInComputerDeck")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(AllZoneUtil.getCardsInGame()
                                            .getController(AllZone.getComputerPlayer()));
                                }
                                if (logic.equals("MostProminentInComputerGraveyard")) {
                                    chosen = CardFactoryUtil.getMostProminentCreatureType(AllZone.getComputerPlayer()
                                            .getCardsIn(Zone.Graveyard));
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
                } else if (type.equals("Basic Land")) {
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            final Object o = GuiUtils.getChoice("Choose a basic land type", CardUtil.getBasicTypes()
                                    .toArray());
                            if (null == o) {
                                return;
                            }
                            final String choice = (String) o;
                            if (CardUtil.isABasicLandType(choice) && !invalidTypes.contains(choice)) {
                                valid = true;
                                card.setChosenType(choice);
                            }
                        } else {
                            // TODO
                            // computer will need to choose a type
                        }
                    }
                } else if (type.equals("Land")) {
                    boolean valid = false;
                    while (!valid) {
                        if (sa.getActivatingPlayer().isHuman()) {
                            final Object o = GuiUtils
                                    .getChoice("Choose a land type", CardUtil.getLandTypes().toArray());
                            if (null == o) {
                                return;
                            }
                            final String choice = (String) o;
                            if (!invalidTypes.contains(choice)) {
                                valid = true;
                                card.setChosenType(choice);
                            }
                        } else {
                            // TODO
                            // computer will need to choose a type
                        }
                    }
                } // end if-else if
            }
        }
    }

    // *************************************************************************
    // ************************* ChooseColor ***********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityChooseColor.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createAbilityChooseColor(final AbilityFactory af) {

        final SpellAbility abChooseColor = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 7069068165774633355L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseColorStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseColorCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseColorResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseColorTriggerAI(af, this, mandatory);
            }

        };
        return abChooseColor;
    }

    /**
     * <p>
     * createSpellChooseColor.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createSpellChooseColor(final AbilityFactory af) {
        final SpellAbility spChooseColor = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -5627273779759130247L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseColorStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseColorCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseColorResolve(af, this);
            }

        };
        return spChooseColor;
    }

    /**
     * <p>
     * createDrawbackChooseColor.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createDrawbackChooseColor(final AbilityFactory af) {
        final SpellAbility dbChooseColor = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 6969618586164278998L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseColorStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseColorResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseColorTriggerAI(af, this, mandatory);
            }

        };
        return dbChooseColor;
    }

    /**
     * <p>
     * chooseColorStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String chooseColorStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a color");
        if (params.containsKey("OrColors")) {
            sb.append(" or colors");
        }
        sb.append(".");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * chooseColorCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean chooseColorCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // Note: if (AILogic == MostProminentAttackers) return isDuringCombat();
        boolean chance = true;

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }
        return chance;
    }

    /**
     * <p>
     * chooseColorTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean chooseColorTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        return false;
    }

    /**
     * <p>
     * chooseColorResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void chooseColorResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = af.getHostCard();

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.getActivatingPlayer().isHuman()) {
                    if (params.containsKey("OrColors")) {
                        final List<String> o = GuiUtils.getChoices("Choose a color or colors",
                                Constant.Color.ONLY_COLORS);
                        card.setChosenColor(new ArrayList<String>(o));
                    } else {
                        final Object o = GuiUtils.getChoice("Choose a color", Constant.Color.ONLY_COLORS);
                        if (null == o) {
                            return;
                        }
                        final String choice = (String) o;
                        final ArrayList<String> tmpColors = new ArrayList<String>();
                        tmpColors.add(choice);
                        card.setChosenColor(tmpColors);
                    }
                } else {
                    String chosen = "";
                    if (params.containsKey("AILogic")) {
                        final String logic = params.get("AILogic");
                        if (logic.equals("MostProminentInHumanDeck")) {
                            chosen = CardFactoryUtil.getMostProminentColor(AllZoneUtil.getCardsInGame().getController(
                                    AllZone.getHumanPlayer()));
                        }
                        if (logic.equals("MostProminentInGame")) {
                            chosen = CardFactoryUtil.getMostProminentColor(AllZoneUtil.getCardsInGame());
                        }
                        if (logic.equals("MostProminentHumanCreatures")) {
                            CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                            if (list.isEmpty()) {
                                list = AllZoneUtil.getCardsInGame().getController(AllZone.getHumanPlayer())
                                        .getType("Creature");
                            }
                            chosen = CardFactoryUtil.getMostProminentColor(list);
                        }
                        if (logic.equals("MostProminentPermanent")) {
                            final CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
                            chosen = CardFactoryUtil.getMostProminentColor(list);
                        }
                        if (logic.equals("MostProminentAttackers")) {
                            chosen = CardFactoryUtil.getMostProminentColor(new CardList(AllZone.getCombat()
                                    .getAttackers()));
                        }
                    }
                    if (chosen.equals("")) {
                        chosen = Constant.Color.GREEN;
                    }
                    GuiUtils.getChoice("Computer picked: ", chosen);
                    final ArrayList<String> colorTemp = new ArrayList<String>();
                    colorTemp.add(chosen);
                    card.setChosenColor(colorTemp);
                }
            }
        }
    }

    // *************************************************************************
    // ************************* ChooseNumber **********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityChooseNumber.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createAbilityChooseNumber(final AbilityFactory af) {

        final SpellAbility abChooseNumber = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -8268155210011368749L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseNumberStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseNumberCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseNumberResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseNumberTriggerAI(af, this, mandatory);
            }

        };
        return abChooseNumber;
    }

    /**
     * <p>
     * createSpellChooseNumber.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createSpellChooseNumber(final AbilityFactory af) {
        final SpellAbility spChooseNumber = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6397887501014311392L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseNumberStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.chooseNumberCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseNumberResolve(af, this);
            }

        };
        return spChooseNumber;
    }

    /**
     * <p>
     * createDrawbackChooseNumber.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createDrawbackChooseNumber(final AbilityFactory af) {
        final SpellAbility dbChooseNumber = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -1339609900364066904L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.chooseNumberStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.chooseNumberResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.chooseNumberTriggerAI(af, this, mandatory);
            }

        };
        return dbChooseNumber;
    }

    /**
     * <p>
     * chooseNumberStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String chooseNumberStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a number.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * chooseNumberCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean chooseNumberCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryChoose.chooseNumberTriggerAI(af, sa, false);
    }

    /**
     * <p>
     * chooseNumberTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean chooseNumberTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        return false;
    }

    /**
     * <p>
     * chooseNumberResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void chooseNumberResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = af.getHostCard();
        final int min = params.containsKey("Min") ? Integer.parseInt(params.get("Min")) : 0;
        final int max = params.containsKey("Max") ? Integer.parseInt(params.get("Max")) : 99;
        final boolean random = params.containsKey("Random");

        final String[] choices = new String[max + 1];
        if (!random) {
            // initialize the array
            for (int i = min; i <= max; i++) {
                choices[i] = "" + i;
            }
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.getActivatingPlayer().isHuman()) {
                    int chosen;
                    if (random) {
                        final Random randomGen = new Random();
                        chosen = randomGen.nextInt(max - min) + min;
                        final String message = "Randomly chosen number: " + chosen;
                        JOptionPane.showMessageDialog(null, message, "" + card, JOptionPane.PLAIN_MESSAGE);
                    } else {
                        final Object o = GuiUtils.getChoice("Choose a number", choices);
                        if (null == o) {
                            return;
                        }
                        chosen = Integer.parseInt((String) o);
                    }
                    card.setChosenNumber(chosen);

                } else {
                    // TODO - not implemented
                }
            }
        }
    }

    // *************************************************************************
    // ************************* ChoosePlayer **********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityChoosePlayer.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createAbilityChoosePlayer(final AbilityFactory af) {
        final SpellAbility abChoosePlayer = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {

            private static final long serialVersionUID = 7502903475594562552L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.choosePlayerStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.choosePlayerCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.choosePlayerResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.choosePlayerTriggerAI(af, this, mandatory);
            }

        };
        return abChoosePlayer;
    }

    /**
     * <p>
     * createSpellChoosePlayer.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createSpellChoosePlayer(final AbilityFactory af) {
        final SpellAbility spChoosePlayer = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {

            private static final long serialVersionUID = -7684507578494661495L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.choosePlayerStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.choosePlayerCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.choosePlayerResolve(af, this);
            }

        };
        return spChoosePlayer;
    }

    /**
     * <p>
     * createDrawbackChoosePlayer.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createDrawbackChoosePlayer(final AbilityFactory af) {
        final SpellAbility dbChoosePlayer = new AbilitySub(af.getHostCard(), af.getAbTgt()) {

            private static final long serialVersionUID = -766158106632103029L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.choosePlayerStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.choosePlayerResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.choosePlayerTriggerAI(af, this, mandatory);
            }

        };
        return dbChoosePlayer;
    }

    /**
     * <p>
     * choosePlayerStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String choosePlayerStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a player.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * choosePlayerCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean choosePlayerCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryChoose.choosePlayerTriggerAI(af, sa, false);
    }

    /**
     * <p>
     * choosePlayerTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean choosePlayerTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        return false;
    }

    /**
     * <p>
     * choosePlayerResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void choosePlayerResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card card = af.getHostCard();

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        final ArrayList<Player> choices = params.containsKey("Choices") ? AbilityFactory.getDefinedPlayers(
                sa.getSourceCard(), params.get("Choices"), sa) : new ArrayList<Player>(AllZone.getPlayersInGame());

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (sa.getActivatingPlayer().isHuman()) {
                    final Object o = GuiUtils.getChoice("Choose a player", choices.toArray());
                    if (null == o) {
                        return;
                    }
                    final Player chosen = (Player) o;
                    card.setChosenPlayer(chosen);

                } else {
                    // TODO - not implemented
                }
            }
        }
    }

    // *************************************************************************
    // ***************************** NameCard **********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityNameCard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createAbilityNameCard(final AbilityFactory af) {
        final SpellAbility abNameCard = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 1748714246609515354L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.nameCardStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.nameCardCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.nameCardResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.nameCardTriggerAI(af, this, mandatory);
            }

        };
        return abNameCard;
    }

    /**
     * <p>
     * createSpellNameCard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createSpellNameCard(final AbilityFactory af) {
        final SpellAbility spNameCard = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 209265128022008897L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.nameCardStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return AbilityFactoryChoose.nameCardCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.nameCardResolve(af, this);
            }

        };
        return spNameCard;
    }

    /**
     * <p>
     * createDrawbackNameCard.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.6
     */
    public static SpellAbility createDrawbackNameCard(final AbilityFactory af) {
        final SpellAbility dbNameCard = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -7647726271751061495L;

            @Override
            public String getStackDescription() {
                return AbilityFactoryChoose.nameCardStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryChoose.nameCardResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return AbilityFactoryChoose.nameCardTriggerAI(af, this, mandatory);
            }

        };
        return dbNameCard;
    }

    /**
     * <p>
     * nameCardStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String nameCardStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("names a card.");

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>
     * nameCardCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean nameCardCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        return AbilityFactoryChoose.choosePlayerTriggerAI(af, sa, false);
    }

    /**
     * <p>
     * nameCardTriggerAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean nameCardTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // TODO - there is no AILogic implemented yet
        return false;
    }

    /**
     * <p>
     * nameCardResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void nameCardResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();

        ArrayList<Player> tgtPlayers;

        final Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        String valid = "Card";
        String validDesc = "card";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
            validDesc = params.get("ValidDesc");
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                boolean ok = false;
                String name = null;
                while (!ok) {
                    if (sa.getActivatingPlayer().isHuman()) {
                        final String message = validDesc.equals("card") ? "Name a card" : "Name a " + validDesc
                                + " card. (Case sensitive)";
                        /*name = JOptionPane.showInputDialog(null, message, host.getName(), JOptionPane.QUESTION_MESSAGE);
                        if (!valid.equals("Card") && !(null == name)) {
                            try {
                                final Card temp = AllZone.getCardFactory().getCard(name, p);
                                ok = temp.isValid(valid, host.getController(), host);
                            } catch (final Exception ignored) {
                                ok = false;
                            }
                        } else {
                            ok = true;
                        }
                        if (ok) {
                            host.setNamedCard(null == name ? "" : name);
                        }*/
                        final List<String> cards = new ArrayList<String>();
                        for (final CardPrinted c : CardDb.instance().getAllUniqueCards()) {
                            cards.add(c.getName());
                        }
                        Collections.sort(cards);

                        // use standard forge's list selection dialog
                        final ListChooser<String> choice = new ListChooser<String>(
                                message, 1, 1, cards);
                        choice.show();
                        // still missing a listener to display the card preview
                        // in the right
                        name = choice.getSelectedValue();
                        if(AllZone.getCardFactory().getCard(name, p).isValid(valid, host.getController(), host)) {
                            host.setNamedCard(choice.getSelectedValue());
                            ok = true;
                        }
                    } else {
                        CardList list = AllZoneUtil.getCardsInGame().getController(AllZone.getHumanPlayer());
                        list = list.filter(new CardListFilter() {
                            @Override
                            public boolean addCard(final Card c) {
                                return !c.isLand();
                            }
                        });
                        if (!list.isEmpty()) {
                            host.setNamedCard(list.get(0).getName());
                        }
                    }
                }
            }
        }
    }

} // end class AbilityFactory_Choose
