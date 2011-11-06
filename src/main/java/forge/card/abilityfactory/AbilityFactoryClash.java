package forge.card.abilityfactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
import forge.Constant.Zone;
import forge.GameActionUtil;
import forge.Player;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;

/**
 * <p>
 * AbilityFactory_Clash class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactoryClash {

    private AbilityFactoryClash() {
        throw new AssertionError();
    }

    /**
     * <p>
     * getAbilityClash.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getAbilityClash(final AbilityFactory af) {
        final SpellAbility abClash = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
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
                AbilityFactoryClash.clashResolve(af, this);
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
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
                AbilityFactoryClash.clashResolve(af, this);
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility getDrawbackClash(final AbilityFactory af) {
        final SpellAbility dbClash = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
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
            public boolean chkAIDrawback() {
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
                AbilityFactoryClash.clashResolve(af, this);
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param SA
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void clashResolve(final AbilityFactory af, final SpellAbility sa) {
        final AbilityFactory afOutcomes = new AbilityFactory();
        final boolean victory = af.getHostCard().getController().clashWithOpponent(af.getHostCard());

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", af.getHostCard().getController());

        if (victory) {
            if (af.getMapParams().containsKey("WinSubAbility")) {
                final SpellAbility win = afOutcomes.getAbility(
                        af.getHostCard().getSVar(af.getMapParams().get("WinSubAbility")), af.getHostCard());
                win.setActivatingPlayer(af.getHostCard().getController());
                ((AbilitySub) win).setParent(sa);

                AbilityFactory.resolve(win, false);
            }
            runParams.put("Won", "True");
        } else {
            if (af.getMapParams().containsKey("OtherwiseSubAbility")) {
                final SpellAbility otherwise = afOutcomes.getAbility(
                        af.getHostCard().getSVar(af.getMapParams().get("OtherwiseSubAbility")), af.getHostCard());
                otherwise.setActivatingPlayer(af.getHostCard().getController());
                ((AbilitySub) otherwise).setParent(sa);

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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createAbilityFlip(final AbilityFactory af) {
        final SpellAbility abFlip = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
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
                return AbilityFactoryClash.flipGetStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryClash.flipResolve(af, this);
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
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
                return AbilityFactoryClash.flipGetStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryClash.flipResolve(af, this);
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.0.15
     */
    public static SpellAbility createDrawbackFlip(final AbilityFactory af) {
        final SpellAbility dbFlip = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
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
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return true;
            }

            @Override
            public String getStackDescription() {
                return AbilityFactoryClash.flipGetStackDescription(af, this);
            }

            @Override
            public void resolve() {
                AbilityFactoryClash.flipResolve(af, this);
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String flipGetStackDescription(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();
        final Player player = params.containsKey("OpponentCalls") ? host.getController().getOpponent() : host
                .getController();

        final StringBuilder sb = new StringBuilder();

        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }

        sb.append(player).append(" flips a coin.");

        final AbilitySub abSub = sa.getSubAbility();
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void flipResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        final Card host = af.getHostCard();
        final Player player = host.getController();

        final ArrayList<Player> caller = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Caller"), sa);
        if (caller.size() == 0) {
            caller.add(player);
        }

        final AbilityFactory afOutcomes = new AbilityFactory();
        final boolean victory = GameActionUtil.flipACoin(caller.get(0), sa.getSourceCard());

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
                final SpellAbility win = afOutcomes.getAbility(host.getSVar(params.get("WinSubAbility")), host);
                win.setActivatingPlayer(player);
                ((AbilitySub) win).setParent(sa);

                AbilityFactory.resolve(win, false);
            }
            // runParams.put("Won","True");
        } else {
            if (params.get("RememberLoser") != null) {
                host.addRemembered(host);
            }
            if (params.containsKey("LoseSubAbility")) {
                final SpellAbility lose = afOutcomes.getAbility(host.getSVar(params.get("LoseSubAbility")), host);
                lose.setActivatingPlayer(player);
                ((AbilitySub) lose).setParent(sa);

                AbilityFactory.resolve(lose, false);
            }
            // runParams.put("Won","False");
        }

        // AllZone.getTriggerHandler().runTrigger("FlipsACoin",runParams);
    }

    // *************************************************************************
    // ***************************** TwoPiles **********************************
    // *************************************************************************

    /**
     * <p>
     * createAbilityTwoPiles.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.7
     */
    public static SpellAbility createAbilityTwoPiles(final AbilityFactory af) {
        final SpellAbility abTwoPiles = new AbilityActivated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -2700390539969188516L;

            @Override
            public String getStackDescription() {
                return twoPilesStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return twoPilesCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                twoPilesResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return twoPilesTriggerAI(af, this, mandatory);
            }

        };
        return abTwoPiles;
    }

    /**
     * <p>
     * createSpellTwoPiles.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.7
     */
    public static SpellAbility createSpellTwoPiles(final AbilityFactory af) {
        final SpellAbility spTwoPiles = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6521684078773178349L;

            @Override
            public String getStackDescription() {
                return twoPilesStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return twoPilesCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                twoPilesResolve(af, this);
            }

        };
        return spTwoPiles;
    }

    /**
     * <p>
     * createDrawbackTwoPiles.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     * @since 1.1.7
     */
    public static SpellAbility createDrawbackTwoPiles(final AbilityFactory af) {
        final SpellAbility dbTwoPiles = new AbilitySub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 7486255949274716808L;

            @Override
            public String getStackDescription() {
                return twoPilesStackDescription(af, this);
            }

            @Override
            public void resolve() {
                twoPilesResolve(af, this);
            }

            @Override
            public boolean chkAIDrawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return twoPilesTriggerAI(af, this, mandatory);
            }

        };
        return dbTwoPiles;
    }

    private static String twoPilesStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        String valid = "";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        sb.append("Separate all ").append(valid).append(" cards ");

        for (Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("controls into two piles.");

        AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    private static boolean twoPilesCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        return twoPilesTriggerAI(af, sa, false);
    }

    private static boolean twoPilesTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        return false;
    }

    private static void twoPilesResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card card = af.getHostCard();

        String valid = "";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        Player separator = card.getController();
        if (params.containsKey("Separator")) {
            separator = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Separator"), sa).get(0);
        }

        Player chooser = tgtPlayers.get(0);
        if (params.containsKey("Chooser")) {
            chooser = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Chooser"), sa).get(0);
        }

        for (Player p : tgtPlayers) {
            if (tgt == null || p.canTarget(sa)) {
                ArrayList<Card> pile1 = new ArrayList<Card>();
                ArrayList<Card> pile2 = new ArrayList<Card>();
                CardList pool = p.getCardsIn(Zone.Battlefield);
                pool = pool.getValidCards(valid, card.getController(), card);

                //first, separate the cards into piles
                if (separator.isHuman()) {
                    List<Card> l = GuiUtils.getChoicesOptional("Put into pile 1 (multi-select)", pool.toArray());
                    for (Card c : l) {
                        pile1.add(c);
                        pool.remove(c);
                    }
                    for (Card c : pool) {
                        pile2.add(c);
                    }
                } else {
                    // TODO - not implemented
                }

                System.out.println("Pile 1:" + pile1);
                System.out.println("Pile 2:" + pile2);

                //then, the chooser picks a pile
                if (chooser.isHuman()) {
                    Card[] disp = new Card[pile1.size() + pile2.size() + 2];
                    disp[0] = new Card();
                    disp[0].setName("Pile 1");
                    for (int i = 0; i < pile1.size(); i++) {
                        disp[1 + i] = pile1.get(i);
                    }
                    disp[pile1.size() + 1] = new Card();
                    disp[pile1.size() + 1].setName("Pile 2");
                    for (int i = 0; i < pile2.size(); i++) {
                        disp[pile1.size() + i + 2] = pile2.get(i);
                    }

                    //make sure Pile 1 or Pile 2 is clicked on
                    boolean chosen = false;
                    while (!chosen) {
                        Object o = GuiUtils.getChoice("Choose a pile", disp);
                        Card c = (Card) o;
                        if (c.getName().equals("Pile 1")) {
                            chosen = true;
                            for (Card z : pile1) {
                                card.addRemembered(z);
                            }
                        }
                        else if (c.getName().equals("Pile 2")) {
                            chosen = true;
                            for (Card z : pile2) {
                                card.addRemembered(z);
                            }
                        }

                    }
                } else {
                    int cmc1 = 0;
                    int cmc2 = 0;

                    for (Card c : pile1) {
                        cmc1 += CardUtil.getConvertedManaCost(c);
                    }
                    for (Card c : pile2) {
                        cmc2 += CardUtil.getConvertedManaCost(c);
                    }

                    //for now, this assumes that the outcome will be bad
                    //TODO: This should really have a ChooseLogic param to figure this out
                    if (cmc2 > cmc1) {
                        JOptionPane.showMessageDialog(null, "Computer chooses the Pile 1", "",
                                JOptionPane.INFORMATION_MESSAGE);
                        for (Card c : pile1) {
                            card.addRemembered(c);
                        }
                    }
                    else {
                        JOptionPane.showMessageDialog(null, "Computer chooses the Pile 2", "",
                                JOptionPane.INFORMATION_MESSAGE);
                        for (Card c : pile2) {
                            card.addRemembered(c);
                        }
                    }
                }

                //take action on the chosen pile
                if (params.containsKey("ChosenPile")) {
                    final AbilityFactory afPile = new AbilityFactory();
                    final SpellAbility action = afPile.getAbility(card.getSVar(params.get("ChosenPile")), card);
                    action.setActivatingPlayer(sa.getActivatingPlayer());
                    ((AbilitySub) action).setParent(sa);

                    AbilityFactory.resolve(action, false);
                }
            }
        }
    } //end twoPiles resolve

} // end class AbilityFactory_Clash
