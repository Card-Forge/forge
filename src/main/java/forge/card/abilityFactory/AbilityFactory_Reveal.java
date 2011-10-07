package forge.card.abilityFactory;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
import forge.ComputerUtil;
import forge.Constant;
import forge.Constant.Zone;
import forge.GameActionUtil;
import forge.MyRandom;
import forge.Player;
import forge.PlayerZone;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.*;
import forge.gui.GuiUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * <p>AbilityFactory_Reveal class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactory_Reveal {

    private AbilityFactory_Reveal() {
        throw new AssertionError();
    }

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
            public boolean doTrigger(final boolean mandatory) {
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
            public boolean doTrigger(final boolean mandatory) {
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
    private static String digStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card host = af.getHostCard();
        StringBuilder sb = new StringBuilder();
        int numToDig = AbilityFactory.calculateAmount(af.getHostCard(), params.get("DigNum"), sa);

        if (!(sa instanceof Ability_Sub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }


        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        sb.append(host.getController()).append(" looks at the top ").append(numToDig);
        sb.append(" card");
        if (numToDig != 1) {
            sb.append("s");
        }
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
        if (AbilityFactory.isSorcerySpeed(sa)) {
            chance = .667;    // 66.7% chance for sorcery speed (since it will never activate EOT)
        }
        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        Target tgt = sa.getTarget();
        Player libraryOwner = AllZone.getComputerPlayer();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (!AllZone.getHumanPlayer().canTarget(sa)) {
                return false;
            } else {
                sa.getTarget().addTarget(AllZone.getHumanPlayer());
            }
            libraryOwner = AllZone.getHumanPlayer();
        }

        //return false if nothing to dig into
        if (libraryOwner.getCardsIn(Constant.Zone.Library).isEmpty()) {
            return false;
        }

        if (AbilityFactory.playReusable(sa)) {
            randomReturn = true;
        }

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
    private static boolean digTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

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
        Zone destZone1 = params.containsKey("DestinationZone") ? Zone.smartValueOf(params.get("DestinationZone")) : Zone.Hand;
        Zone destZone2 = params.containsKey("DestinationZone2") ? Zone.smartValueOf(params.get("DestinationZone2")) : Zone.Library;
        
        int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : -1;
        int destZone1ChangeNum = 1;
        boolean mitosis = params.containsKey("Mitosis");
        String changeValid = params.containsKey("ChangeValid") ? params.get("ChangeValid") : "";
        boolean anyNumber = params.containsKey("AnyNumber");

        int libraryPosition2 = params.containsKey("LibraryPosition2") ? Integer.parseInt(params.get("LibraryPosition2")) : -1;
        boolean optional = params.containsKey("Optional");
        boolean noMove = params.containsKey("NoMove");
        boolean changeAll = false;
        ArrayList<String> keywords = new ArrayList<String>();
        if (params.containsKey("Keywords")) {
            keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
        }

        if (params.containsKey("ChangeNum")) {
            if (params.get("ChangeNum").equalsIgnoreCase("All")) {
                changeAll = true;
            } else {
                destZone1ChangeNum = Integer.parseInt(params.get("ChangeNum"));
            }
        }

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (Player p : tgtPlayers) {
            if (tgt == null || p.canTarget(sa)) {

                CardList top = new CardList();
                CardList valid = new CardList();
                CardList rest = new CardList();
                PlayerZone library = p.getZone(Constant.Zone.Library);

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
                                if (!valid.contains(c)) {
                                    rest.add(c);
                                }
                            }
                        } else if (!changeValid.equals("")) {
                            if (changeValid.contains("ChosenType")) {
                                changeValid = changeValid.replace("ChosenType", host.getChosenType());
                            }
                            valid = top.getValidCards(changeValid.split(","), host.getController(), host);
                            for (Card c : top) {
                                if (!valid.contains(c)) {
                                    rest.add(c);
                                }
                            }
                            if (valid.isEmpty()) {
                                valid.add(dummy);
                            }
                        } else {
                            valid = top;
                        }

                        if (changeAll) {
                            for (Card c : valid) {
                                if (c.equals(dummy)) {
                                    continue;
                                }
                                PlayerZone zone = c.getOwner().getZone(destZone1);
                                if (zone.is(Zone.Library)) {
                                    AllZone.getGameAction().moveToLibrary(c, libraryPosition);
                                } else {
                                    AllZone.getGameAction().moveTo(zone, c);
                                }
                                if (params.containsKey("RememberChanged")) {
                                    host.addRemembered(c);
                                }
                            }
                        } else {
                            int j = 0;
                            if (player.isHuman()) {
                                while (j < destZone1ChangeNum || (anyNumber && j < numToDig)) {
                                    //let user get choice
                                    Card chosen = null;
                                    String prompt = "Choose a card to put into the ";
                                    if (destZone1.equals(Zone.Library) && libraryPosition == -1) {
                                        prompt = "Put the rest on the bottom of the ";
                                    }
                                    if (destZone1.equals(Zone.Library) && libraryPosition == 0) {
                                        prompt = "Put the rest on top of the ";
                                    }
                                    if (anyNumber || optional) {
                                        chosen = GuiUtils.getChoiceOptional(prompt + destZone1, valid.toArray());
                                    } else {
                                        chosen = GuiUtils.getChoice(prompt + destZone1, valid.toArray());
                                    }
                                    if (chosen == null || chosen.getName().equals("[No valid cards]")) {
                                        break;
                                    }
                                    valid.remove(chosen);
                                    PlayerZone zone = chosen.getOwner().getZone(destZone1);
                                    if (zone.is(Zone.Library)) {
                                        //System.out.println("Moving to lib position: "+libraryPosition);
                                        AllZone.getGameAction().moveToLibrary(chosen, libraryPosition);
                                    } else {
                                        Card c = AllZone.getGameAction().moveTo(zone, chosen);
                                        if (destZone1.equals(Zone.Battlefield) && !keywords.isEmpty()) {
                                            for (String kw : keywords) {
                                                c.addExtrinsicKeyword(kw);
                                            }
                                        }
                                    }
                                    //AllZone.getGameAction().revealToComputer() - for when this exists
                                    j++;
                                }
                            } //human
                            else { //computer (pick the first cards)
                                int changeNum = Math.min(destZone1ChangeNum, valid.size());
                                if (anyNumber) {
                                    changeNum = valid.size(); //always take all
                                }
                                for (j = 0; j < changeNum; j++) {
                                    Card chosen = valid.get(0);
                                    if (chosen.equals(dummy)) {
                                        break;
                                    }
                                    PlayerZone zone = chosen.getOwner().getZone(destZone1);
                                    if (zone.is(Zone.Library)) {
                                        AllZone.getGameAction().moveToLibrary(chosen, libraryPosition);
                                    } else {
                                        AllZone.getGameAction().moveTo(zone, chosen);
                                    	if (destZone1.equals(Zone.Battlefield) && !keywords.isEmpty()) {
                                            for (String kw : keywords) {
                                                chosen.addExtrinsicKeyword(kw);
                                            }
                                        }
                                    }
                                    if (changeValid.length() > 0) {
                                        GuiUtils.getChoice("Computer picked: ", chosen);
                                    }
                                    valid.remove(chosen);
                                }
                            }
                        }

                        //dump anything not selected from valid back into the rest
                        if (!changeAll) {
                            rest.addAll(valid);
                        }
                        if (rest.contains(dummy)) {
                            rest.remove(dummy);
                        }

                        //now, move the rest to destZone2
                        if (destZone2.equals(Zone.Library)) {
                        	if (player.isHuman()) {
	                            //put them in any order
	                            while (rest.size() > 0) {
	                                Card chosen;
	                                if (rest.size() > 1) {
	                                    String prompt = "Put the rest on top of the library in any order";
	                                    if (libraryPosition2 == -1) {
                                            prompt = "Put the rest on the bottom of the library in any order";
                                        }
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
                                PlayerZone toZone = c.getOwner().getZone(destZone2);
                                c = AllZone.getGameAction().moveTo(toZone, c);
                                if (destZone2.equals(Zone.Battlefield) && !keywords.isEmpty()) {
                                    for (String kw : keywords) {
                                        c.addExtrinsicKeyword(kw);
                                    }
                                }
                            }

                        }
                    }
                } //end if canTarget
            } //end foreach player
        }
    } //end resolve

    //returns a CardList that is a subset of list with cards that share a name with a permanent on the battlefield
    /**
     * <p>sharesNameWithCardOnBattlefield.</p>
     *
     * @param list a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private static CardList sharesNameWithCardOnBattlefield(final CardList list) {
        CardList toReturn = new CardList();
        CardList play = AllZoneUtil.getCardsIn(Zone.Battlefield);
        for (Card c : list) {
            for (Card p : play) {
                if (p.getName().equals(c.getName())) {
                    toReturn.add(c);
                }
            }
        }
        return toReturn;
    }

    //**********************************************************************
    //******************************* DigUntil ***************************
    //**********************************************************************

    /**
     * <p>createAbilityDigUntil.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityDigUntil(final AbilityFactory af) {

        final SpellAbility abDig = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4239474096624403497L;

            @Override
            public String getStackDescription() {
                return digUntilStackDescription(af, this);
            }

            public boolean canPlayAI() {
                return digUntilCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                digUntilResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return digUntilTriggerAI(af, this, mandatory);
            }

        };
        return abDig;
    }

    /**
     * <p>createSpellDigUntil.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellDigUntil(final AbilityFactory af) {
        final SpellAbility spDig = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3389143507816474146L;

            @Override
            public String getStackDescription() {
                return digUntilStackDescription(af, this);
            }

            public boolean canPlayAI() {
                return digUntilCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                digUntilResolve(af, this);
            }

        };
        return spDig;
    }

    /**
     * <p>createDrawbackDigUntil.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackDigUntil(final AbilityFactory af) {
        final SpellAbility dbDig = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -3372788479421357024L;

            @Override
            public String getStackDescription() {
                return digUntilStackDescription(af, this);
            }

            @Override
            public void resolve() {
                digUntilResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return digUntilTriggerAI(af, this, mandatory);
            }

        };
        return dbDig;
    }

    /**
     * <p>digUntilStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String digUntilStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card host = sa.getSourceCard();
        StringBuilder sb = new StringBuilder();

        String desc = "Card";
        if (params.containsKey("ValidDescription")) {
            desc = params.get("ValidDescription");
        }

        int untilAmount = 1;
        if (params.containsKey("Amount")) {
            untilAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Amount"), sa);
        }

        if (!(sa instanceof Ability_Sub)) {
            sb.append(host).append(" - ");
        } else {
            sb.append(" ");
        }

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (Player pl : tgtPlayers) {
            sb.append(pl).append(" ");
        }

        sb.append("reveals cards from his or her library until revealing ");
        sb.append(untilAmount).append(" ").append(desc).append(" card");
        if (untilAmount != 1) {
            sb.append("s");
        }
        sb.append(". Put ");

        String found = params.get("FoundDestination");
        String revealed = params.get("RevealedDestination");
        if (found != null) {

            sb.append(untilAmount > 1 ? "those cards" : "that card");
            sb.append(" ");


            if (found.equals(Constant.Zone.Hand)) {
                sb.append("into his or her hand ");
            }
            
            if (revealed.equals(Constant.Zone.Graveyard)) {
                sb.append("and all other cards into his or her graveyard.");
            }
            if (revealed.equals(Constant.Zone.Exile)) {
                sb.append("and exile all other cards revealed this way.");
            }
        } else {
            if (revealed.equals(Constant.Zone.Hand)) {
                sb.append("all cards revealed this way into his or her hand");
            }
        }



        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>digUntilCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean digUntilCanPlayAI(final AbilityFactory af, final SpellAbility sa) {

        double chance = .4;    // 40 percent chance with instant speed stuff
        if (AbilityFactory.isSorcerySpeed(sa)) {
            chance = .667;    // 66.7% chance for sorcery speed (since it will never activate EOT)
        }
        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        Target tgt = sa.getTarget();
        Player libraryOwner = AllZone.getComputerPlayer();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (!AllZone.getHumanPlayer().canTarget(sa)) {
                return false;
            } else {
                sa.getTarget().addTarget(AllZone.getHumanPlayer());
            }
                libraryOwner = AllZone.getHumanPlayer();
        }

        //return false if nothing to dig into
        if (libraryOwner.getCardsIn(Constant.Zone.Library).isEmpty()) {
            return false;
        }

        if (af.hasSubAbility()) {
            Ability_Sub abSub = sa.getSubAbility();
            if (abSub != null) {
                return randomReturn && abSub.chkAI_Drawback();
            }
        }

        return randomReturn;
    }

    /**
     * <p>digUntilTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean digUntilTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            sa.getTarget().addTarget(AllZone.getComputerPlayer());
        }

        return true;
    }

    /**
     * <p>digUntilResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void digUntilResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card host = sa.getSourceCard();

        String type = "Card";
        if (params.containsKey("Valid")) {
            type = params.get("Valid");
        }

        int untilAmount = 1;
        if (params.containsKey("Amount")) {
            untilAmount = AbilityFactory.calculateAmount(host, params.get("Amount"), sa);
        }

        Integer maxRevealed = null;
        if (params.containsKey("MaxRevealed")) {
            maxRevealed = AbilityFactory.calculateAmount(host, params.get("MaxRevealed"), sa);
        }

        boolean remember = params.containsKey("RememberFound");

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        }
        else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(host, params.get("Defined"), sa);
        }

        Zone foundDest = Zone.smartValueOf(params.get("FoundDestination"));
        int foundLibPos = AbilityFactory.calculateAmount(host, params.get("FoundLibraryPosition"), sa);
        Zone revealedDest = Zone.smartValueOf(params.get("RevealedDestination"));
        int revealedLibPos = AbilityFactory.calculateAmount(host, params.get("RevealedLibraryPosition"), sa);

        for (Player p : tgtPlayers) {
            if (tgt == null || p.canTarget(sa)) {
                CardList found = new CardList();
                CardList revealed = new CardList();

                PlayerZone library = p.getZone(Constant.Zone.Library);

                int maxToDig = maxRevealed != null ? maxRevealed : library.size();

                for (int i = 0; i < maxToDig; i++) {
                    Card c = library.get(i);
                    revealed.add(c);
                    if (c.isValid(type, sa.getActivatingPlayer(), host)) {
                        found.add(c);
                        if (remember) {
                            host.addRemembered(c);
                        }
                        if (found.size() == untilAmount) {
                            break;
                        }
                    }
                }

                GuiUtils.getChoice(p + " revealed: ", revealed.toArray());

                // TODO Allow Human to choose the order
                if (foundDest != null) {
                    Iterator<Card> itr = found.iterator();
                    while (itr.hasNext()) {
                        Card c = itr.next();
                        AllZone.getGameAction().moveTo(foundDest, c, foundLibPos);
                        revealed.remove(c);
                    }
                }

                Iterator<Card> itr = revealed.iterator();
                while (itr.hasNext()) {
                    Card c = itr.next();
                    AllZone.getGameAction().moveTo(revealedDest, c, revealedLibPos);
                }
            } //end foreach player
        }
    } //end resolve

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
            public boolean doTrigger(final boolean mandatory) {
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
            public boolean doTrigger(final boolean mandatory) {
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
    private static String revealHandStackDescription(final AbilityFactory af, final SpellAbility sa) {
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

        sb.append(sa.getActivatingPlayer()).append(" looks at ");

        if (tgtPlayers.size() > 0) {
            for (Player p : tgtPlayers) {
                sb.append(p.toString()).append("'s ");
            }
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
    private static boolean revealHandCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        Cost abCost = sa.getPayCosts();
        Card source = sa.getSourceCard();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }
                
            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }
                
            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }

        }

        boolean bFlag = revealHandTargetAI(af, sa, true, false);

        if (!bFlag)
            return false;

        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(.667, sa.getActivationsThisTurn() + 1);

        if (AbilityFactory.playReusable(sa)) {
            randomReturn = true;
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAI_Drawback();
        }
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
    private static boolean revealHandTargetAI(final AbilityFactory af, final SpellAbility sa,
            final boolean primarySA, final boolean mandatory)
    {
        Target tgt = af.getAbTgt();

        int humanHandSize = AllZone.getHumanPlayer().getCardsIn(Zone.Hand).size();

        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();

            boolean canTgtHuman = AllZone.getHumanPlayer().canTarget(sa);

            if (!canTgtHuman || humanHandSize == 0) {
                return false;
            }
            else {
                tgt.addTarget(AllZone.getHumanPlayer());
            }
        } else {
            //if it's just defined, no big deal
        }

        return true;
    } // revealHandTargetAI()

    /**
     * <p>revealHandTrigger.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean revealHandTrigger(AbilityFactory af, SpellAbility sa, boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (!revealHandTargetAI(af, sa, false, mandatory)) {
            return false;
        }

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
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        }
        else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (Player p : tgtPlayers) {
            if (tgt == null || p.canTarget(sa)) {
                CardList hand = p.getCardsIn(Zone.Hand);
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
            public boolean doTrigger(final boolean mandatory) {
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
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackScry(final AbilityFactory af) {
        final SpellAbility dbScry = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 7763043327497404630L;

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
            public boolean doTrigger(final boolean mandatory) {
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
        if (params.containsKey("ScryNum")) {
            num = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("ScryNum"), sa);
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
    private static boolean scryTargetAI(final AbilityFactory af, final SpellAbility sa) {
        Target tgt = af.getAbTgt();

        if (tgt != null) {    // It doesn't appear that Scry ever targets
            // ability is targeted
            tgt.resetTargets();

            tgt.addTarget(AllZone.getComputerPlayer());
        }

        return true;
    } // scryTargetAI()

    /**
     * <p>scryTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean scryTriggerAI(final AbilityFactory af, final SpellAbility sa) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        return scryTargetAI(af, sa);
    } // scryTargetAI()

    /**
     * <p>scryStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String scryStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        StringBuilder sb = new StringBuilder();

        if (!(sa instanceof Ability_Sub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
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
            sb.append(p.toString()).append(" ");
        }

        int num = 1;
        if (params.containsKey("ScryNum")) {
            num = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("ScryNum"), sa);
        }

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
    private static boolean scryCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        //Card source = sa.getSourceCard();

        double chance = .4;    // 40 percent chance of milling with instant speed stuff
        if (AbilityFactory.isSorcerySpeed(sa)) {
            chance = .667;    // 66.7% chance for sorcery speed (since it will never activate EOT)
        }
        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1);

        if (AbilityFactory.playReusable(sa)) {
            randomReturn = true;
        }

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
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createRearrangeTopOfLibraryAbility(final AbilityFactory af) {
        final SpellAbility rtolAbility = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -548494891203983219L;

            @Override
            public String getStackDescription() {
                return rearrangeTopOfLibraryStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return rearrangeTopOfLibraryTrigger(af, this, mandatory);
            }

            @Override
            public void resolve() {
                rearrangeTopOfLibraryResolve(af, this);
            }

        };

        return rtolAbility;
    }

    /**
     * <p>createRearrangeTopOfLibrarySpell.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createRearrangeTopOfLibrarySpell(final AbilityFactory af) {
        final SpellAbility rtolSpell = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 6977502611509431864L;

            @Override
            public String getStackDescription() {
                return rearrangeTopOfLibraryStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return false;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return rearrangeTopOfLibraryTrigger(af, this, mandatory);
            }

            @Override
            public void resolve() {
                rearrangeTopOfLibraryResolve(af, this);
            }

        };

        return rtolSpell;
    }

    /**
     * <p>createRearrangeTopOfLibraryDrawback.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createRearrangeTopOfLibraryDrawback(final AbilityFactory af) {
        final SpellAbility dbDraw = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -777856059960750319L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is happening
                return rearrangeTopOfLibraryStackDescription(af, this);
            }

            @Override
            public void resolve() {
                rearrangeTopOfLibraryResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return false;
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return rearrangeTopOfLibraryTrigger(af, this, mandatory);
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
        if (tgt != null && !params.containsKey("Defined")) {
            tgtPlayers = tgt.getTargetPlayers();
        }
        else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        numCards = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumCards"), sa);
        shuffle = params.containsKey("MayShuffle");

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
    private static boolean rearrangeTopOfLibraryTrigger(final AbilityFactory af,
            final SpellAbility sa, final boolean mandatory)
    {

        Target tgt = af.getAbTgt();

        if (tgt != null) {
            // ability is targeted
            tgt.resetTargets();

            boolean canTgtHuman = AllZone.getHumanPlayer().canTarget(sa);

            if (!canTgtHuman) {
                return false;
            }
            else {
                tgt.addTarget(AllZone.getHumanPlayer());
            }
        }
        else {
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
            if (tgt != null && !params.containsKey("Defined")) {
                tgtPlayers = tgt.getTargetPlayers();
            }
            else {
                tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
            }

            numCards = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumCards"), sa);
            shuffle = params.containsKey("MayShuffle");

            for (Player p : tgtPlayers) {
                if (tgt == null || p.canTarget(sa)) {
                    rearrangeTopOfLibrary(af.getHostCard(), p, numCards, shuffle);
                }
            }
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
    private static void rearrangeTopOfLibrary(final Card src, final Player player,
            final int numCards, final boolean mayshuffle)
    {
        PlayerZone lib = player.getZone(Constant.Zone.Library);
        int maxCards = lib.size();
        maxCards = Math.min(maxCards, numCards);
        if (maxCards == 0) {
            return;
        }
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
            if (o == null) {
                break;
            }
            Card c1 = (Card) o;
            topCards.remove(c1);
            AllZone.getGameAction().moveToLibrary(c1, i - 1);
        }
        if (mayshuffle) {
            if (GameActionUtil.showYesNoDialog(src, "Do you want to shuffle the library?")) {
                player.shuffle();
            }
        }
    }
    
    //**********************************************************************
    //******************************* Reveal *******************************
    //**********************************************************************

    /**
     * <p>createAbilityReveal.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityReveal(final AbilityFactory af) {
        final SpellAbility abReveal = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -4417404703197532765L;

            @Override
            public String getStackDescription() {
                return revealStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return revealCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                revealResolve(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return revealTrigger(af, this, mandatory);
            }

        };
        return abReveal;
    }

    /**
     * <p>createSpellReveal.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellReveal(final AbilityFactory af) {
        final SpellAbility spReveal = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = -9015033247472453902L;

            @Override
            public String getStackDescription() {
                return revealStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return revealCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                revealResolve(af, this);
            }

        };
        return spReveal;
    }

    /**
     * <p>createDrawbackReveal.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackReveal(final AbilityFactory af) {
        final SpellAbility dbReveal = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -8059731932417441449L;

            @Override
            public String getStackDescription() {
                // when getStackDesc is called, just build exactly what is happening
                return revealStackDescription(af, this);
            }

            @Override
            public void resolve() {
                revealResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                //reuse code from RevealHand
                return revealHandTargetAI(af, this, false, false);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return revealTrigger(af, this, mandatory);
            }

        };
        return dbReveal;
    }

    /**
     * <p>revealStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String revealStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        StringBuilder sb = new StringBuilder();

        if (sa instanceof Ability_Sub) {
            sb.append(" ");
        }
        else {
            sb.append(sa.getSourceCard()).append(" - ");
        }

        ArrayList<Player> tgtPlayers;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        }
        else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtPlayers.size() > 0) {
            sb.append(tgtPlayers.get(0)).append(" reveals a card ");
            if (params.containsKey("Random")) {
                sb.append("at random ");
            }
            sb.append("from his or her hand.");
        } else {
            sb.append("Error - no target players for RevealHand. ");
        }

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>revealCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean revealCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        Cost abCost = sa.getPayCosts();
        Card source = sa.getSourceCard();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }
                
            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }
                
            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }

        }

        //we can reuse this function here...
        boolean bFlag = revealHandTargetAI(af, sa, true, false);

        if (!bFlag)
            return false;

        Random r = MyRandom.random;
        boolean randomReturn = r.nextFloat() <= Math.pow(.667, sa.getActivationsThisTurn() + 1);

        if (AbilityFactory.playReusable(sa)) {
            randomReturn = true;
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAI_Drawback();
        }
        return randomReturn;
    }

    /**
     * <p>revealTrigger.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean revealTrigger(AbilityFactory af, SpellAbility sa, boolean mandatory) {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (!revealHandTargetAI(af, sa, false, mandatory)) {
            return false;
        }

        // check SubAbilities DoTrigger?
        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>revealResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void revealResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Card host = af.getHostCard();

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
                CardList handChoices = p.getCardsIn(Zone.Hand);
                if (handChoices.size() > 0) {
                    Card random = CardUtil.getRandom(handChoices.toArray());
                    if (params.containsKey("RememberRevealed")) {
                        host.addRemembered(random);
                    }
                    GuiUtils.getChoice("Random card", new CardList(random));
                }

            }
        }
    }

} //end class AbilityFactory_Reveal
