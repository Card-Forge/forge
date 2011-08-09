package forge.card.abilityFactory;

import forge.*;
import forge.card.spellability.*;
import forge.gui.GuiUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/**
 * <p>AbilityFactory_Reveal class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_Reveal {

    // *************************************************************************
    // ************************* Dig *******************************************
    // *************************************************************************

    /**
     * <p>createAbilityDig.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityDig(final AbilityFactory af) {

        final SpellAbility abDig = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4239474096624403497L;

            @Override
            public String getStackDescription() {
                return digStackDescription(af, this);
            }

            public boolean canPlayAI() {
                return digCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                digResolve(af, this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return digTriggerAI(af, this, mandatory);
            }

        };
        return abDig;
    }

    /**
     * <p>createSpellDig.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellDig(final AbilityFactory af) {
        final SpellAbility spDig = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3389143507816474146L;

            @Override
            public String getStackDescription() {
                return digStackDescription(af, this);
            }

            public boolean canPlayAI() {
                return digCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                digResolve(af, this);
            }

        };
        return spDig;
    }

    /**
     * <p>createDrawbackDig.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackDig(final AbilityFactory af) {
        final SpellAbility dbDig = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -3372788479421357024L;

            @Override
            public String getStackDescription() {
                return digStackDescription(af, this);
            }

            @Override
            public void resolve() {
                digResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return digTriggerAI(af, this, mandatory);
            }

        };
        return dbDig;
    }

    /**
     * <p>digStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String digStackDescription(AbilityFactory af, SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card host = af.getHostCard();
        StringBuilder sb = new StringBuilder();
        int numToDig = AbilityFactory.calculateAmount(af.getHostCard(), params.get("DigNum"), sa);

        if (!(sa instanceof Ability_Sub))
            sb.append(sa.getSourceCard()).append(" - ");
        else
            sb.append(" ");


        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            tgtPlayers = tgt.getTargetPlayers();
        else
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);

        sb.append(host.getController()).append(" looks at the top ").append(numToDig);
        sb.append(" card");
        if (numToDig != 1) sb.append("s");
        sb.append(" of ");
        if (tgtPlayers.contains(host.getController())) {
            sb.append("his or her ");
        } else {
            for (Player p : tgtPlayers) {
                sb.append(p).append("'s ");
            }
        }
        sb.append("library.");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>digCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean digCanPlayAI(final AbilityFactory af, final SpellAbility sa) {

        double chance = .4;    // 40 percent chance with instant speed stuff
        if (AbilityFactory.isSorcerySpeed(sa))
            chance = .667;    // 66.7% chance for sorcery speed (since it will never activate EOT)
        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        Target tgt = sa.getTarget();
        Player libraryOwner = AllZone.getComputerPlayer();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (!AllZone.getHumanPlayer().canTarget(sa))
                return false;
            else
	            sa.getTarget().addTarget(AllZone.getHumanPlayer());
	            libraryOwner = AllZone.getHumanPlayer();
        }

        //return false if nothing to dig into
        if (AllZoneUtil.getCardsInZone(Constant.Zone.Library, libraryOwner).isEmpty())
            return false;

        if (AbilityFactory.playReusable(sa))
            randomReturn = true;

        if (af.hasSubAbility()) {
            Ability_Sub abSub = sa.getSubAbility();
            if (abSub != null) {
                return randomReturn && abSub.chkAI_Drawback();
            }
        }

        return randomReturn;
    }

    /**
     * <p>digTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean digTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa))
            return false;

        Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getComputerPlayer());
        }

        return true;
    }

    /**
     * <p>digResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void digResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card host = af.getHostCard();
        Player player = sa.getActivatingPlayer();
        int numToDig = AbilityFactory.calculateAmount(af.getHostCard(), params.get("DigNum"), sa);
        String destZone1 = params.containsKey("DestinationZone") ? params.get("DestinationZone") : "Hand";
        int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : -1;
        int destZone1ChangeNum = 1;
        boolean mitosis = params.containsKey("Mitosis");
        String changeValid = params.containsKey("ChangeValid") ? params.get("ChangeValid") : "";
        boolean anyNumber = params.containsKey("AnyNumber");
        String destZone2 = params.containsKey("DestinationZone2") ? params.get("DestinationZone2") : "Library";
        int libraryPosition2 = params.containsKey("LibraryPosition2") ? Integer.parseInt(params.get("LibraryPosition2")) : -1;
        boolean optional = params.containsKey("Optional");
        boolean noMove = params.containsKey("NoMove");
        boolean changeAll = false;
        ArrayList<String> keywords = new ArrayList<String>();
        if (params.containsKey("Keywords")) {
            keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
        }

        if (params.containsKey("ChangeNum")) {
            if (params.get("ChangeNum").equalsIgnoreCase("All")) changeAll = true;
            else destZone1ChangeNum = Integer.parseInt(params.get("ChangeNum"));
        }

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            tgtPlayers = tgt.getTargetPlayers();
        else
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);

        for (Player p : tgtPlayers) {
            if (tgt == null || p.canTarget(sa)) {

                CardList top = new CardList();
                CardList valid = new CardList();
                CardList rest = new CardList();
                PlayerZone library = AllZone.getZone(Constant.Zone.Library, p);

                numToDig = Math.min(numToDig, library.size());
                for (int i = 0; i < numToDig; i++) {
                    top.add(library.get(i));
                }

                if (top.size() > 0) {
                    Card dummy = new Card();
                    dummy.setName("[No valid cards]");

                    if (params.containsKey("Reveal")) {
                        GuiUtils.getChoice("Revealing cards from library", top.toArray());
                        //AllZone.getGameAction().revealToCopmuter(top.toArray()); - for when it exists
                    } else if (player.isHuman()) {
                        //show the user the revealed cards
                        GuiUtils.getChoice("Looking at cards from library", top.toArray());
                    }

                    if (!noMove) {
                        if (mitosis) {
                            valid = sharesNameWithCardOnBattlefield(top);
                            for (Card c : top) {
                                if (!valid.contains(c)) rest.add(c);
                            }
                        } else if (!changeValid.equals("")) {
                            if (changeValid.contains("ChosenType")) {
                                changeValid = changeValid.replace("ChosenType", host.getChosenType());
                            }
                            valid = top.getValidCards(changeValid.split(","), host.getController(), host);
                            for (Card c : top) {
                                if (!valid.contains(c)) rest.add(c);
                            }
                            if (valid.isEmpty()) {
                                valid.add(dummy);
                            }
                        } else {
                            valid = top;
                        }

                        if (changeAll) {
                            for (Card c : valid) {
                                if (c.equals(dummy)) continue;
                                PlayerZone zone = AllZone.getZone(destZone1, c.getOwner());
                                if (zone.is("Library")) {
                                    AllZone.getGameAction().moveToLibrary(c, libraryPosition);
                                } else {
                                    AllZone.getGameAction().moveTo(zone, c);
                                }
                            }
                        } else {
                            int j = 0;
                            if (player.isHuman()) {
                                while (j < destZone1ChangeNum || (anyNumber && j < numToDig)) {
                                    //let user get choice
                                    Card chosen = null;
                                    String prompt = "Choose a card to put into the ";
                                    if (destZone1.equals("Library") && libraryPosition == -1)
                                        prompt = "Put the rest on the bottom of the ";
                                    if (destZone1.equals("Library") && libraryPosition == 0)
                                        prompt = "Put the rest on top of the ";
                                    if (anyNumber || optional) {
                                        chosen = GuiUtils.getChoiceOptional(prompt + destZone1, valid.toArray());
                                    } else {
                                        chosen = GuiUtils.getChoice(prompt + destZone1, valid.toArray());
                                    }
                                    if (chosen == null || chosen.getName().equals("[No valid cards]")) break;
                                    valid.remove(chosen);
                                    PlayerZone zone = AllZone.getZone(destZone1, chosen.getOwner());
                                    if (zone.is("Library")) {
                                        //System.out.println("Moving to lib position: "+libraryPosition);
                                        AllZone.getGameAction().moveToLibrary(chosen, libraryPosition);
                                    } else {
                                        Card c = AllZone.getGameAction().moveTo(zone, chosen);
                                        if (destZone1.equals("Battlefield") && !keywords.isEmpty())
                                            for (String kw : keywords) c.addExtrinsicKeyword(kw);
                                    }
                                    //AllZone.getGameAction().revealToComputer() - for when this exists
                                    j++;
                                }
                            }//human
                            else { //computer (pick the first cards)
                                int changeNum = Math.min(destZone1ChangeNum, valid.size());
                                if (anyNumber) changeNum = valid.size();//always take all
                                for (j = 0; j < changeNum; j++) {
                                    Card chosen = valid.get(0);
                                    if (chosen.equals(dummy)) break;
                                    PlayerZone zone = AllZone.getZone(destZone1, chosen.getOwner());
                                    if (zone.is("Library")) {
                                        AllZone.getGameAction().moveToLibrary(chosen, libraryPosition);
                                    } else {
                                        AllZone.getGameAction().moveTo(zone, chosen);
                                    	if (destZone1.equals("Battlefield") && !keywords.isEmpty())
                                    		for (String kw : keywords) chosen.addExtrinsicKeyword(kw);
                                    }
                                    if (changeValid.length() > 0)
                                        GuiUtils.getChoice("Computer picked: ", chosen);
                                    valid.remove(chosen);
                                }
                            }
                        }

                        //dump anything not selected from valid back into the rest
                        if (!changeAll) rest.addAll(valid);
                        if (rest.contains(dummy)) rest.remove(dummy);

                        //now, move the rest to destZone2
                        if (destZone2.equals("Library")) {
                        	if(player.isHuman()) {
	                            //put them in any order
	                            while (rest.size() > 0) {
	                                Card chosen;
	                                if (rest.size() > 1) {
	                                    String prompt = "Put the rest on top of the library in any order";
	                                    if (libraryPosition2 == -1)
	                                        prompt = "Put the rest on the bottom of the library in any order";
	                                    chosen = GuiUtils.getChoice(prompt, rest.toArray());
	                                } else {
	                                    chosen = rest.get(0);
	                                }
	                                AllZone.getGameAction().moveToLibrary(chosen, libraryPosition2);
	                                rest.remove(chosen);
	                            } 
                            } else { //Computer
                            	for (int i = 0; i < rest.size(); i++) {
                            		AllZone.getGameAction().moveToLibrary(rest.get(i), libraryPosition2);
                            	}
                            }
                        } else {
                            //just move them randomly
                            for (int i = 0; i < rest.size(); i++) {
                                Card c = rest.get(i);
                                PlayerZone toZone = AllZone.getZone(destZone2, c.getOwner());
                                c = AllZone.getGameAction().moveTo(toZone, c);
                                if (destZone2.equals("Battlefield") && !keywords.isEmpty()) {
                                    for (String kw : keywords) c.addExtrinsicKeyword(kw);
                                }
                            }

                        }
                    }
                }//end if canTarget
            }//end foreach player
        }
    }//end resolve

    //returns a CardList that is a subset of list with cards that share a name with a permanent on the battlefield
    /**
     * <p>sharesNameWithCardOnBattlefield.</p>
     *
     * @param list a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private static CardList sharesNameWithCardOnBattlefield(CardList list) {
        CardList toReturn = new CardList();
        CardList play = AllZoneUtil.getCardsInPlay();
        for (Card c : list) {
            for (Card p : play) {
                if (p.getName().equals(c.getName())) toReturn.add(c);
            }
        }
        return toReturn;
    }

    //**********************************************************************
    //******************************* RevealHand ***************************
    //**********************************************************************

    /**
     * <p>createAbilityRevealHand.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityRevealHand(final AbilityFactory af) {
        final SpellAbility abRevealHand = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 2785654059206102004L;

            @Override
            public String getStackDescription() {
                return revealHandStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return revealHandCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                revealHandResolve(af, this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return revealHandTrigger(af, this, mandatory);
            }

        };
        return abRevealHand;
    }

    /**
     * <p>createSpellRevealHand.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellRevealHand(final AbilityFactory af) {
        final SpellAbility spRevealHand = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -668943560971904791L;

            @Override
            public String getStackDescription() {
                return revealHandStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return revealHandCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                revealHandResolve(af, this);
            }

        };
        return spRevealHand;
    }

    /**
     * <p>createDrawbackRevealHand.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackRevealHand(final AbilityFactory af) {
        final SpellAbility dbRevealHand = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -6079668770576878801L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is happening
                return revealHandStackDescription(af, this);
            }

            @Override
            public void resolve() {
                revealHandResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return revealHandTargetAI(af, this, false, false);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return revealHandTrigger(af, this, mandatory);
            }

        };
        return dbRevealHand;
    }

    /**
     * <p>revealHandStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String revealHandStackDescription(AbilityFactory af, SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        StringBuilder sb = new StringBuilder();

        if (!(sa instanceof Ability_Sub))
            sb.append(sa.getSourceCard()).append(" - ");
        else
            sb.append(" ");

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            tgtPlayers = tgt.getTargetPlayers();
        else
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);

        sb.append(sa.getActivatingPlayer()).append(" looks at ");

        if (tgtPlayers.size() > 0) {
            for (Player p : tgtPlayers)
                sb.append(p.toString()).append("'s ");
        } else {
            sb.append("Error - no target players for RevealHand. ");
        }
        sb.append("hand.");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>revealHandCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean revealHandCanPlayAI(final AbilityFactory af, SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        Cost abCost = af.getAbCost();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (abCost.getSacCost()) {
                return false;
            }
            if (abCost.getLifeCost()) {
                if (AllZone.getComputerPlayer().getLife() - abCost.getLifeAmount() < 4)
                    return false;
            }
            if (abCost.getDiscardCost()) return false;

            if (abCost.getSubCounter()) {
                if (abCost.getCounterType().equals(Counters.P1P1)) return false; // Other counters should be used
            }

        }

        boolean bFlag = revealHandTargetAI(af, sa, true, false);

        if (!bFlag)
            return false;

        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(.667, sa.getActivationsThisTurn() + 1);

        if (AbilityFactory.playReusable(sa))
            randomReturn = true;

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
            randomReturn &= subAb.chkAI_Drawback();
        return randomReturn;
    }

    /**
     * <p>revealHandTargetAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param primarySA a boolean.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean revealHandTargetAI(AbilityFactory af, SpellAbility sa, boolean primarySA, boolean mandatory) {
        Target tgt = af.getAbTgt();

        int humanHandSize = AllZoneUtil.getPlayerHand(AllZone.getHumanPlayer()).size();

        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();

            boolean canTgtHuman = AllZone.getHumanPlayer().canTarget(sa);

            if (!canTgtHuman || humanHandSize == 0)
                return false;
            else
                tgt.addTarget(AllZone.getHumanPlayer());
        } else {
            //if it's just defined, no big deal
        }

        return true;
    }// revealHandTargetAI()

    /**
     * <p>revealHandTrigger.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean revealHandTrigger(AbilityFactory af, SpellAbility sa, boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa))    // If there is a cost payment
            return false;

        if (!revealHandTargetAI(af, sa, false, mandatory))
            return false;

        // check SubAbilities DoTrigger?
        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>revealHandResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void revealHandResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            tgtPlayers = tgt.getTargetPlayers();
        else
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);

        for (Player p : tgtPlayers) {
            if (tgt == null || p.canTarget(sa)) {
                CardList hand = AllZoneUtil.getPlayerHand(p);
                if (sa.getActivatingPlayer().isHuman()) {
                    if (hand.size() > 0) {
                        GuiUtils.getChoice(p + "'s hand", hand.toArray());
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append(p).append("'s hand is empty!");
                        javax.swing.JOptionPane.showMessageDialog(null, sb.toString(), p + "'s hand", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    //reveal to Computer (when computer can keep track of seen cards...)
                }

            }
        }
    }

    //**********************************************************************
    //******************************* SCRY *********************************
    //**********************************************************************

    /**
     * <p>createAbilityScry.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityScry(final AbilityFactory af) {
        final SpellAbility abScry = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 2631175859655699419L;

            @Override
            public String getStackDescription() {
                return scryStackDescription(af, this);
            }

            public boolean canPlayAI() {
                return scryCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                scryResolve(af, this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return scryTriggerAI(af, this);
            }

        };
        return abScry;
    }

    /**
     * <p>createSpellScry.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellScry(final AbilityFactory af) {
        final SpellAbility spScry = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6273876397392154403L;

            @Override
            public String getStackDescription() {
                return scryStackDescription(af, this);
            }

            public boolean canPlayAI() {
                return scryCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                scryResolve(af, this);
            }

        };
        return spScry;
    }

    /**
     * <p>createDrawbackScry.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackScry(final AbilityFactory AF) {
        final SpellAbility dbScry = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
            private static final long serialVersionUID = 7763043327497404630L;
            final AbilityFactory af = AF;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is happening
                return scryStackDescription(af, this);
            }

            @Override
            public void resolve() {
                scryResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return scryTargetAI(af, this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return scryTriggerAI(af, this);
            }

        };
        return dbScry;
    }

    /**
     * <p>scryResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void scryResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();

        int num = 1;
        if (params.containsKey("ScryNum"))
            num = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("ScryNum"), sa);

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            tgtPlayers = tgt.getTargetPlayers();
        else
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);

        for (Player p : tgtPlayers) {
            if (tgt == null || p.canTarget(sa)) {
                p.scry(num);
            }
        }
    }

    /**
     * <p>scryTargetAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean scryTargetAI(AbilityFactory af, SpellAbility sa) {
        Target tgt = af.getAbTgt();

        if (tgt != null) {    // It doesn't appear that Scry ever targets
            // ability is targeted
            tgt.resetTargets();

            tgt.addTarget(AllZone.getComputerPlayer());
        }

        return true;
    }// scryTargetAI()

    /**
     * <p>scryTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean scryTriggerAI(AbilityFactory af, SpellAbility sa) {
        if (!ComputerUtil.canPayCost(sa))
            return false;

        return scryTargetAI(af, sa);
    }// scryTargetAI()

    /**
     * <p>scryStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String scryStackDescription(AbilityFactory af, SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        StringBuilder sb = new StringBuilder();

        if (!(sa instanceof Ability_Sub))
            sb.append(sa.getSourceCard().getName()).append(" - ");
        else
            sb.append(" ");

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            tgtPlayers = tgt.getTargetPlayers();
        else
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);

        for (Player p : tgtPlayers)
            sb.append(p.toString()).append(" ");

        int num = 1;
        if (params.containsKey("ScryNum"))
            num = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("ScryNum"), sa);

        sb.append("scrys (").append(num).append(").");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>scryCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean scryCanPlayAI(final AbilityFactory af, SpellAbility sa) {
        Card source = sa.getSourceCard();

        double chance = .4;    // 40 percent chance of milling with instant speed stuff
        if (AbilityFactory.isSorcerySpeed(sa))
            chance = .667;    // 66.7% chance for sorcery speed (since it will never activate EOT)
        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        if (AbilityFactory.playReusable(sa))
            randomReturn = true;

        if (af.hasSubAbility()) {
            Ability_Sub abSub = sa.getSubAbility();
            if (abSub != null) {
                return randomReturn && abSub.chkAI_Drawback();
            }
        }
        return randomReturn;
    }

    //**********************************************************************
    //*********************** REARRANGETOPOFLIBRARY ************************
    //**********************************************************************

    /**
     * <p>createRearrangeTopOfLibraryAbility.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createRearrangeTopOfLibraryAbility(final AbilityFactory AF) {
        final SpellAbility RTOLAbility = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = -548494891203983219L;

            @Override
            public String getStackDescription() {
                return rearrangeTopOfLibraryStackDescription(AF, this);
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return rearrangeTopOfLibraryTrigger(AF, this, mandatory);
            }

            @Override
            public void resolve() {
                rearrangeTopOfLibraryResolve(AF, this);
            }

        };

        return RTOLAbility;
    }

    /**
     * <p>createRearrangeTopOfLibrarySpell.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createRearrangeTopOfLibrarySpell(final AbilityFactory AF) {
        final SpellAbility RTOLSpell = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = 6977502611509431864L;

            @Override
            public String getStackDescription() {
                return rearrangeTopOfLibraryStackDescription(AF, this);
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return rearrangeTopOfLibraryTrigger(AF, this, mandatory);
            }

            @Override
            public void resolve() {
                rearrangeTopOfLibraryResolve(AF, this);
            }

        };

        return RTOLSpell;
    }

    /**
     * <p>createRearrangeTopOfLibraryDrawback.</p>
     *
     * @param AF a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createRearrangeTopOfLibraryDrawback(final AbilityFactory AF) {
        final SpellAbility dbDraw = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
            private static final long serialVersionUID = -777856059960750319L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is happening
                return rearrangeTopOfLibraryStackDescription(AF, this);
            }

            @Override
            public void resolve() {
                rearrangeTopOfLibraryResolve(AF, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return false;
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return rearrangeTopOfLibraryTrigger(AF, this, mandatory);
            }

        };
        return dbDraw;
    }

    /**
     * <p>rearrangeTopOfLibraryStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String rearrangeTopOfLibraryStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        int numCards = 0;
        ArrayList<Player> tgtPlayers;
        boolean shuffle = false;

        Target tgt = af.getAbTgt();
        if (tgt != null && !params.containsKey("Defined"))
            tgtPlayers = tgt.getTargetPlayers();
        else
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);

        numCards = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumCards"), sa);
        shuffle = params.containsKey("MayShuffle") ? true : false;

        StringBuilder ret = new StringBuilder();
        if (!(sa instanceof Ability_Sub)) {
            ret.append(af.getHostCard().getName());
            ret.append(" - ");
        }
        ret.append("Look at the top ");
        ret.append(numCards);
        ret.append(" cards of ");
        for (Player p : tgtPlayers) {
            ret.append(p.getName());
            ret.append("s");
            ret.append(" & ");
        }
        ret.delete(ret.length() - 3, ret.length());

        ret.append(" library. Then put them back in any order.");

        if (shuffle) {
            ret.append("You may have ");
            if (tgtPlayers.size() > 1) {
                ret.append("those");
            } else {
                ret.append("that");
            }

            ret.append(" player shuffle his or her library.");
        }

        return ret.toString();
    }

    /**
     * <p>rearrangeTopOfLibraryTrigger.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean rearrangeTopOfLibraryTrigger(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        
        Target tgt = af.getAbTgt();
        
        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();

            boolean canTgtHuman = AllZone.getHumanPlayer().canTarget(sa);

            if (!canTgtHuman)
                return false;
            else
                tgt.addTarget(AllZone.getHumanPlayer());
        } else {
            //if it's just defined, no big deal
        }

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return false;
    }

    /**
     * <p>rearrangeTopOfLibraryResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void rearrangeTopOfLibraryResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        int numCards = 0;
        ArrayList<Player> tgtPlayers = new ArrayList<Player>();
        boolean shuffle = false;

        if (sa.getActivatingPlayer().isHuman()) {
            Target tgt = af.getAbTgt();
            if (tgt != null && !params.containsKey("Defined"))
                tgtPlayers = tgt.getTargetPlayers();
            else
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);

            numCards = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumCards"), sa);
            shuffle = params.containsKey("MayShuffle") ? true : false;

            for (Player p : tgtPlayers)
                if (tgt == null || p.canTarget(sa))
                    rearrangeTopOfLibrary(af.getHostCard(), p, numCards, shuffle);
        }
    }

    /**
     * use this when Human needs to rearrange the top X cards in a player's library.  You
     * may also specify a shuffle when done
     *
     * @param src      the source card
     * @param player   the player to target
     * @param numCards the number of cards from the top to rearrange
     * @param mayshuffle a boolean.
     */
    private static void rearrangeTopOfLibrary(final Card src, final Player player, final int numCards, boolean mayshuffle) {
        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
        int maxCards = lib.size();
        maxCards = Math.min(maxCards, numCards);
        if (maxCards == 0) return;
        CardList topCards = new CardList();
        //show top n cards:
        for (int j = 0; j < maxCards; j++) {
            topCards.add(lib.get(j));
        }
        for (int i = 1; i <= maxCards; i++) {
            String suffix = "";
            switch (i) {
                case 1:
                    suffix = "st";
                    break;
                case 2:
                    suffix = "nd";
                    break;
                case 3:
                    suffix = "rd";
                    break;
                default:
                    suffix = "th";
            }
            String title = "Put " + i + suffix + " from the top: ";
            Object o = GuiUtils.getChoiceOptional(title, topCards.toArray());
            if (o == null) break;
            Card c_1 = (Card) o;
            topCards.remove(c_1);
            AllZone.getGameAction().moveToLibrary(c_1, i - 1);
        }
        if (mayshuffle) {
            if (GameActionUtil.showYesNoDialog(src, "Do you want to shuffle the library?")) {
                player.shuffle();
            }
        }
    }

}//end class AbilityFactory_Reveal
