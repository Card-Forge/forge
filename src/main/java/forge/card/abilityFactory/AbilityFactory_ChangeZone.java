package forge.card.abilityFactory;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.CardUtil;
import forge.CombatUtil;
import forge.ComputerUtil;
import forge.Constant;
import forge.GameActionUtil;
import forge.MyRandom;
import forge.Player;
import forge.PlayerZone;

import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.*;
import forge.gui.GuiUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * <p>AbilityFactory_ChangeZone class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public final class AbilityFactory_ChangeZone {

    private AbilityFactory_ChangeZone() {
        throw new AssertionError();
    }

    // Change Zone is going to work much differently than other AFs.
    // *NOTE* Please do not use this as a base for copying and creating your own AF


    /**
     * <p>createAbilityChangeZone.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityChangeZone(final AbilityFactory af) {
        final SpellAbility abChangeZone = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3728332812890211671L;

            @Override
            public boolean canPlayAI() {
                return changeZoneCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                changeZoneResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return changeZoneDescription(af, this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return changeZoneTriggerAI(af, this, mandatory);
            }

        };
        setMiscellaneous(af, abChangeZone);
        return abChangeZone;
    }

    /**
     * <p>createSpellChangeZone.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellChangeZone(final AbilityFactory af) {
        final SpellAbility spChangeZone = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3270484211099902059L;

            @Override
            public boolean canPlayAI() {
                return changeZoneCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                changeZoneResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return changeZoneDescription(af, this);
            }
        };
        setMiscellaneous(af, spChangeZone);
        return spChangeZone;
    }

    /**
     * <p>createDrawbackChangeZone.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackChangeZone(final AbilityFactory af) {
        final SpellAbility dbChangeZone = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 3270484211099902059L;

            @Override
            public void resolve() {
                changeZoneResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return changeZonePlayDrawbackAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return changeZoneDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return changeZoneTriggerAI(af, this, mandatory);
            }
        };
        setMiscellaneous(af, dbChangeZone);
        return dbChangeZone;
    }

    /**
     * <p>isHidden.</p>
     *
     * @param origin a {@link java.lang.String} object.
     * @param hiddenOverride a boolean.
     * @return a boolean.
     */
    public static boolean isHidden(final String origin, final boolean hiddenOverride) {
        return (hiddenOverride || origin.equals("Library") || origin.equals("Hand") || origin.equals("Sideboard"));
    }

    /**
     * <p>isKnown.</p>
     *
     * @param origin a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean isKnown(final String origin) {
        return (origin.equals("Graveyard") || origin.equals("Exile")
                || origin.equals("Battlefield") || origin.equals("Stack"));
    }

    /**
     * <p>setMiscellaneous.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void setMiscellaneous(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        List<Constant.Zone> origin = Constant.Zone.listValueOf(params.get("Origin"));

        Target tgt = af.getAbTgt();

        // Don't set the zone if it targets a player
        if (tgt != null && !tgt.canTgtPlayer()) {
            af.getAbTgt().setZone(origin);
        }

        if (!(sa instanceof Ability_Sub)) {
            if (origin.contains(Zone.Battlefield) || params.get("Destination").equals("Battlefield")) {
                af.getHostCard().setSVar("PlayMain1", "TRUE");
            }
        }
    }

    /**
     * <p>changeZoneCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeZoneCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        String origin = params.get("Origin");

        if (isHidden(origin, params.containsKey("Hidden"))) {
            return changeHiddenOriginCanPlayAI(af, sa);
        } else if (isKnown(origin)) {
            return changeKnownOriginCanPlayAI(af, sa);
        }

        return false;
    }

    /**
     * <p>changeZonePlayDrawbackAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeZonePlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        String origin = params.get("Origin");

        if (isHidden(origin, params.containsKey("Hidden"))) {
            return changeHiddenOriginPlayDrawbackAI(af, sa);
        } else if (isKnown(origin)) {
            return changeKnownOriginPlayDrawbackAI(af, sa);
        }

        return false;
    }

    /**
     * <p>changeZoneTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean changeZoneTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        HashMap<String, String> params = af.getMapParams();
        String origin = params.get("Origin");

        if (isHidden(origin, params.containsKey("Hidden"))) {
            return changeHiddenTriggerAI(af, sa, mandatory);
        } else if (isKnown(origin)) {
            return changeKnownOriginTriggerAI(af, sa, mandatory);
        }

        return false;
    }

    /**
     * <p>changeZoneDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeZoneDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        String origin = params.get("Origin");

        if (isHidden(origin, params.containsKey("Hidden"))) {
            return changeHiddenOriginStackDescription(af, sa);
        } else if (isKnown(origin)) {
            return changeKnownOriginStackDescription(af, sa);
        }

        return "";
    }

    /**
     * <p>changeZoneResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeZoneResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        String origin = params.get("Origin");

        if (isHidden(origin, params.containsKey("Hidden")) && !params.containsKey("Ninjutsu")) {
            changeHiddenOriginResolve(af, sa);
        } else if (isKnown(origin) || params.containsKey("Ninjutsu")) {
            changeKnownOriginResolve(af, sa);
        }
    }

    // *************************************************************************************
    // ************ Hidden Origin (Library/Hand/Sideboard/Non-targetd other) ***************
    // ******* Hidden origin cards are chosen on the resolution of the spell ***************
    // ******* It is possible for these to have Destination of Battlefield *****************
    // ****** Example: Cavern Harpy where you don't choose the card until resolution *******
    // *************************************************************************************

    /**
     * <p>changeHiddenOriginCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeHiddenOriginCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // Fetching should occur fairly often as it helps cast more spells, and have access to more mana
        Cost abCost = af.getAbCost();
        Card source = af.getHostCard();
        HashMap<String, String> params = af.getMapParams();
        Constant.Zone origin = Constant.Zone.smartValueOf(params.get("Origin"));
        String destination = params.get("Destination");

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }
        }

        Random r = MyRandom.random;
        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        ArrayList<Player> pDefined;
        Target tgt = af.getAbTgt();
        if (tgt != null && tgt.canTgtPlayer()) {
            if (af.isCurse()) {
                tgt.addTarget(AllZone.getHumanPlayer());
            } else {
                tgt.addTarget(AllZone.getComputerPlayer());
            }
            pDefined = tgt.getTargetPlayers();
        } else {
            pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("DefinedPlayer"), sa);
        }

        String type = params.get("ChangeType");
        if (type != null) {
            if (type.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                int xPay = ComputerUtil.determineLeftoverMana(sa);
                source.setSVar("PayX", Integer.toString(xPay));
            }
        }

        for (Player p : pDefined) {
            CardList list = p.getCardsIn(origin);

            if (type != null && p.isComputer()) {
                // AI only "knows" about his information
                list = AbilityFactory.filterListByType(list, params.get("ChangeType"), sa);
            }

            if (list.isEmpty()) {
                return false;
            }
        }
        
        //don't use fetching to top of library/graveyard before main2
        if(AllZone.getPhase().isBefore(Constant.Phase.Main2) && !params.containsKey("ActivationPhases") 
                && !destination.equals("Battlefield") && !destination.equals("Hand")) {
            return false;
        }

        chance &= (r.nextFloat() < .8);

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAI_Drawback();
        }

        return chance;
    }

    /**
     * <p>changeHiddenOriginPlayDrawbackAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeHiddenOriginPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // if putting cards from hand to library and parent is drawing cards
        // make sure this will actually do something:
        Target tgt = af.getAbTgt();
        if (tgt != null && tgt.canTgtPlayer()) {
            if (af.isCurse()) {
                tgt.addTarget(AllZone.getHumanPlayer());
            } else {
                tgt.addTarget(AllZone.getComputerPlayer());
            }
        }

        return true;
    }

    /**
     * <p>changeHiddenTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean changeHiddenTriggerAI(final AbilityFactory af, final SpellAbility sa, final boolean mandatory) {
        // Fetching should occur fairly often as it helps cast more spells, and have access to more mana

        Card source = sa.getSourceCard();

        HashMap<String, String> params = af.getMapParams();
        //String destination = params.get("Destination");
        List<Zone> origin = Zone.listValueOf(params.get("Origin"));

        // this works for hidden because the mana is paid first.
        String type = params.get("ChangeType");
        if (type != null && type.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            int xPay = ComputerUtil.determineLeftoverMana(sa);
            source.setSVar("PayX", Integer.toString(xPay));
        }

        ArrayList<Player> pDefined;
        Target tgt = af.getAbTgt();
        if (tgt != null && tgt.canTgtPlayer()) {
            if (af.isCurse()) {
                if (AllZone.getHumanPlayer().canTarget(sa)) {
                    tgt.addTarget(AllZone.getHumanPlayer());
                } else if (mandatory && AllZone.getComputerPlayer().canTarget(sa)) {
                    tgt.addTarget(AllZone.getComputerPlayer());
                }
            } else {
                if (AllZone.getComputerPlayer().canTarget(sa)) {
                    tgt.addTarget(AllZone.getComputerPlayer());
                } else if (mandatory && AllZone.getHumanPlayer().canTarget(sa)) {
                    tgt.addTarget(AllZone.getHumanPlayer());
                }
            }

            pDefined = tgt.getTargetPlayers();

            if (pDefined.isEmpty()) {
                return false;
            }

            if (mandatory) {
                return pDefined.size() > 0;
            }
        } else {
            if (mandatory) {
                return true;
            }
            pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (Player p : pDefined) {
            CardList list = p.getCardsIn(origin);

            // Computer should "know" his deck
            if (p.isComputer()) {
                list = AbilityFactory.filterListByType(list, params.get("ChangeType"), sa);
            }

            if (list.isEmpty()) {
                return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            return subAb.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>changeHiddenOriginStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeHiddenOriginStackDescription(final AbilityFactory af, final SpellAbility sa) {
        // TODO build Stack Description will need expansion as more cards are added
        HashMap<String, String> params = af.getMapParams();

        StringBuilder sb = new StringBuilder();
        Card host = af.getHostCard();

        if (!(sa instanceof Ability_Sub)) {
            sb.append(host.getName()).append(" -");
        }

        sb.append(" ");

        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
        } else {
            String origin = params.get("Origin");
            String destination = params.get("Destination");

            String type = params.containsKey("ChangeType") ? params.get("ChangeType") : "Card";
            int num = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(host, params.get("ChangeNum"), sa) : 1;

            if (origin.equals("Library") && params.containsKey("Defined")) {
                //for now, just handle the Exile from top of library case, but this can be expanded...
                sb.append("Exile the top card of your library");
                if (params.containsKey("ExileFaceDown")) {
                    sb.append(" face down");
                }
                sb.append(".");
            }
            else if (origin.equals("Library")) {
                sb.append("Search your library for ").append(num).append(" ").append(type).append(" and ");

                if (params.get("ChangeNum").equals("1")) {
                    sb.append("put that card ");
                } else {
                    sb.append("put those cards ");
                }

                if (destination.equals("Battlefield")) {
                    sb.append("onto the battlefield");
                    if (params.containsKey("Tapped")) {
                        sb.append(" tapped");
                    }


                    sb.append(".");

                }
                if (destination.equals("Hand")) {
                    sb.append("into your hand.");
                }
                if (destination.equals("Graveyard")) {
                    sb.append("into your graveyard.");
                }

                sb.append(" Then shuffle your library.");
            } else if (origin.equals("Hand")) {
                sb.append("Put ").append(num).append(" ").append(type).append(" card(s) from your hand ");

                if (destination.equals("Battlefield")) {
                    sb.append("onto the battlefield.");
                }
                if (destination.equals("Library")) {
                    int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;

                    if (libraryPos == 0) {
                        sb.append("on top");
                    }
                    if (libraryPos == -1) {
                        sb.append("on bottom");
                    }

                    sb.append(" of your library.");
                }
            } else if (origin.equals("Battlefield")) {
                // TODO Expand on this Description as more cards use it
                // for the non-targeted SAs when you choose what is returned on resolution
                sb.append("Return ").append(num).append(" ").append(type).append(" card(s) ");
                sb.append(" to your ").append(destination);
            }
        }

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>changeHiddenOriginResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeHiddenOriginResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();

        ArrayList<Player> fetchers;

        fetchers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("DefinedPlayer"), sa);
        
        //handle case when Defined is for a Card
        if (fetchers.isEmpty()) {
            fetchers.add(sa.getSourceCard().getController());
        }

        Player chooser = null;
        if (params.containsKey("Chooser")) {
            String choose = params.get("Chooser");
            if (choose.equals("Targeted") && af.getAbTgt().getTargetPlayers() != null) {
                chooser = af.getAbTgt().getTargetPlayers().get(0);
            } else {
                chooser = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), choose, sa).get(0);
            }
        }

        for (Player player : fetchers) {
            Player decider = chooser;
            if (decider == null) {
                decider = player;
            }
            if (decider.isComputer()) {
                changeHiddenOriginResolveAI(af, sa, player);
            } else {
                changeHiddenOriginResolveHuman(af, sa, player);
            }
        }
    }

    /**
     * <p>changeHiddenOriginResolveHuman.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param player a {@link forge.Player} object.
     */
    private static void changeHiddenOriginResolveHuman(final AbilityFactory af, final SpellAbility sa,
            Player player)
    {
        HashMap<String, String> params = af.getMapParams();
        Card card = sa.getSourceCard();
        boolean defined = params.containsKey("Defined");
        
        Target tgt = af.getAbTgt();
        if (tgt != null) {
            ArrayList<Player> players = tgt.getTargetPlayers();
            player = players.get(0);
            if (players.contains(player) && !player.canTarget(sa)) {
                return;
            }
        }

        List<Zone> origin = Zone.listValueOf(params.get("Origin"));
        Zone destination = Zone.smartValueOf(params.get("Destination"));
        // this needs to be zero indexed. Top = 0, Third = 2
        int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;

        if (params.containsKey("OriginChoice")) {
            // Currently only used for Mishra, but may be used by other things
            // Improve how this message reacts for other cards
            List<Zone> alt = Zone.listValueOf(params.get("OriginAlternative"));
            CardList altFetchList = player.getCardsIn(alt);
            altFetchList = AbilityFactory.filterListByType(altFetchList, params.get("ChangeType"), sa);

            StringBuilder sb = new StringBuilder();
            sb.append(params.get("AlternativeMessage")).append(" ");
            sb.append(altFetchList.size()).append(" cards match your searching type in Alternate Zones.");

            if (!GameActionUtil.showYesNoDialog(card, sb.toString())) {
                origin = alt;
            }
        }

        if (params.containsKey("DestinationAlternative")) {

            StringBuilder sb = new StringBuilder();
            sb.append(params.get("AlternativeDestinationMessage"));

            if (!GameActionUtil.showYesNoDialog(card, sb.toString())) {
                destination = Zone.smartValueOf(params.get("DestinationAlternative"));
                libraryPos = params.containsKey("LibraryPositionAlternative") ? Integer.parseInt(params.get("LibraryPositionAlternative")) : 0;
            }
        }

        CardList fetchList;
        if (defined) {
            fetchList = new CardList(AbilityFactory.getDefinedCards(card, params.get("Defined"), sa));
        } else {
            fetchList = player.getCardsIn(origin);
        }
        
        if (origin.contains(Zone.Library) && !defined) {    // Look at whole library before moving onto choosing a card{
            GuiUtils.getChoiceOptional(af.getHostCard().getName() + " - Looking at Library", player.getCardsIn(Zone.Library).toArray());
        }

        // Look at opponents hand before moving onto choosing a card
        if (origin.contains(Zone.Hand) && player.isComputer())
        {
            GuiUtils.getChoiceOptional(af.getHostCard().getName() + " - Looking at Opponent's Hand",
                    player.getCardsIn(Zone.Hand).toArray());
        }

        if (!defined) {
            fetchList = AbilityFactory.filterListByType(fetchList, params.get("ChangeType"), sa);
        }

        PlayerZone destZone = player.getZone(destination);

        int changeNum = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(card, params.get("ChangeNum"), sa) : 1;

        String remember = params.get("RememberChanged");
        String imprint = params.get("Imprint");

        if (params.containsKey("Unimprint")) {
            card.clearImprinted();
        }

        for (int i = 0; i < changeNum; i++) {
            if (fetchList.size() == 0 || destination == null) {
                break;
            }

            Object o;
            if(params.containsKey("AtRandom")) {
                o = CardUtil.getRandom(fetchList.toArray());
            } else if (params.containsKey("Mandatory")) {
                o = GuiUtils.getChoice("Select a card", fetchList.toArray());
            } else if (params.containsKey("Defined")) {
                o = fetchList.get(i);
            } else {
                o = GuiUtils.getChoiceOptional("Select a card", fetchList.toArray());
            }

            if (o != null) {
                Card c = (Card) o;
                fetchList.remove(c);
                Card movedCard = null;

                if (destination.equals(Zone.Library)) {
                    // do not shuffle the library once we have placed a fetched card on top.
                    if (origin.contains(Zone.Library) && i < 1) {
                        player.shuffle();
                    }
                    movedCard = AllZone.getGameAction().moveToLibrary(c, libraryPos);
                } else if (destination.equals(Zone.Battlefield)) {
                    if (params.containsKey("Tapped")) {
                        c.tap();
                    }
                    if (params.containsKey("GainControl")) {
                        c.addController(af.getHostCard());
                    }

                    movedCard = AllZone.getGameAction().moveTo(c.getController().getZone(destination), c);
                } else {
                    movedCard = AllZone.getGameAction().moveTo(destZone, c);
                    if (params.containsKey("ExileFaceDown")) {
                        movedCard.setIsFaceDown(true);
                    }
                }

                if (remember != null) {
                    card.addRemembered(movedCard);
                }
                //for imprinted since this doesn't use Target
                if (imprint != null) {
                    card.addImprinted(movedCard);
                }

            } else {
                StringBuilder sb = new StringBuilder();
                int num = Math.min(fetchList.size(), changeNum - i);
                sb.append("Cancel Search? Up to ").append(num).append(" more cards can change zones.");

                if (i + 1 == changeNum || GameActionUtil.showYesNoDialog(card, sb.toString())) {
                    break;
                }
            }
        }

        if ((origin.contains(Zone.Library) && !destination.equals(Zone.Library) && !defined) || params.containsKey("Shuffle")) {
            player.shuffle();
        }
    }

    /**
     * <p>changeHiddenOriginResolveAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param player a {@link forge.Player} object.
     */
    private static void changeHiddenOriginResolveAI(final AbilityFactory af, final SpellAbility sa, Player player) {
        HashMap<String, String> params = af.getMapParams();
        Target tgt = af.getAbTgt();
        Card card = af.getHostCard();
        boolean defined = params.containsKey("Defined");

        if (tgt != null) {
            if (!tgt.getTargetPlayers().isEmpty()) {
                player = tgt.getTargetPlayers().get(0);
                if (!player.canTarget(sa)) {
                    return;
                }
            }
        }

        List<Zone> origin = Zone.listValueOf(params.get("Origin"));
        
        String type = params.get("ChangeType");
        if (type == null) {
            type = "Card";
        }

        CardList fetchList;
        if (defined) {
            fetchList = new CardList(AbilityFactory.getDefinedCards(card, params.get("Defined"), sa));
        } else {
            fetchList = player.getCardsIn(origin);
            fetchList = AbilityFactory.filterListByType(fetchList, type, sa);
        }

        Zone destination = Zone.smartValueOf(params.get("Destination"));

        PlayerZone destZone = player.getZone(destination);

        CardList fetched = new CardList();

        int changeNum = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(card, params.get("ChangeNum"), sa) : 1;

        String remember = params.get("RememberChanged");
        String imprint = params.get("Imprint");
        
        if (params.containsKey("Unimprint")) {
            card.clearImprinted();
        }

        for (int i = 0; i < changeNum; i++) {
            if (fetchList.size() == 0 || destination == null) {
                break;
            }
            
            // Improve the AI for fetching.
            Card c;
            if(params.containsKey("AtRandom")) {
                c = CardUtil.getRandom(fetchList.toArray());
            } else if (defined) {
                c = fetchList.get(i);
            } else if (type.contains("Basic")) {
                c = basicManaFixing(fetchList);
            } else if (areAllBasics(type)) {
                c = basicManaFixing(fetchList, type);
            } else if (fetchList.getNotType("Creature").size() == 0) {
                c = CardFactoryUtil.AI_getBestCreature(fetchList);     //if only creatures take the best
            } else if (Zone.Battlefield.equals(destination) || Zone.Graveyard.equals(destination)) {
                c = CardFactoryUtil.AI_getMostExpensivePermanent(fetchList, af.getHostCard(), false);
            } else if (Zone.Exile.equals(destination)) {
                // Exiling your own stuff, if Exiling opponents stuff choose best
                if (destZone.getPlayer().isHuman()) {
                    c = CardFactoryUtil.AI_getMostExpensivePermanent(fetchList, af.getHostCard(), false);
                } else {
                    c = CardFactoryUtil.AI_getCheapestPermanent(fetchList, af.getHostCard(), false);
                }
            } else {
                //Don't fetch another tutor with the same name
                if (origin.contains(Zone.Library) && !fetchList.getNotName(card.getName()).isEmpty()) {
                    fetchList = fetchList.getNotName(card.getName());
                }

                fetchList.shuffle();
                c = fetchList.get(0);
            }

            fetched.add(c);
            fetchList.remove(c);
        }

        if (origin.contains(Zone.Library) && !defined) {
            player.shuffle();
        }

        for (Card c : fetched) {
            Card newCard = null;
            if (Zone.Library.equals(destination)) {
                int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;
                AllZone.getGameAction().moveToLibrary(c, libraryPos);
            } else if (Zone.Battlefield.equals(destination)) {
                if (params.containsKey("Tapped")) {
                    c.tap();
                }
                if (params.containsKey("GainControl")) {
                    c.addController(af.getHostCard());
                }
				// Auras without Candidates stay in their current location
            	if (c.isAura()) {
            		SpellAbility saAura = AbilityFactory_Attach.getAttachSpellAbility(c);
            		if (!saAura.getTarget().hasCandidates(false)) {
                        continue;
                    }
            	}

                newCard = AllZone.getGameAction().moveTo(c.getController().getZone(destination), c);
            } else {
                newCard = AllZone.getGameAction().moveTo(destZone, c);
                if (params.containsKey("ExileFaceDown")) {
                    newCard.setIsFaceDown(true);
                }
            }

            if (remember != null) {
                card.addRemembered(newCard);
            }
            //for imprinted since this doesn't use Target
            if (imprint != null) {
                card.addImprinted(newCard);
            }
        }

        if (!Zone.Battlefield.equals(destination) && !"Card".equals(type) && !defined) {
            String picked = af.getHostCard().getName() + " - Computer picked:";
            if (fetched.size() > 0) {
                GuiUtils.getChoice(picked, fetched.toArray());
            } else {
                GuiUtils.getChoice(picked, new String[]{"<Nothing>"});
            }
        }
    } //end changeHiddenOriginResolveAI

    // *********** Utility functions for Hidden ********************
    /**
     * <p>basicManaFixing.</p>
     *
     * @param list a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    private static Card basicManaFixing(final CardList list) {    // Search for a Basic Land
        return basicManaFixing(list, "Plains, Island, Swamp, Mountain, Forest");
    }

    /**
     * <p>basicManaFixing.</p>
     *
     * @param list a {@link forge.CardList} object.
     * @param type a {@link java.lang.String} object.
     * @return a {@link forge.Card} object.
     */
    private static Card basicManaFixing(CardList list, final String type) {    // type = basic land types
        CardList combined = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
        combined.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Hand));

        String[] names = type.split(",");
        ArrayList<String> basics = new ArrayList<String>();

        // what types can I go get?
        for (int i = 0; i < names.length; i++) {
            if (list.getType(names[i]).size() != 0) {
                basics.add(names[i]);
            }
        }

        // Which basic land is least available from hand and play, that I still have in my deck
        int minSize = Integer.MAX_VALUE;
        String minType = null;

        for (int i = 0; i < basics.size(); i++) {
            String b = basics.get(i);
            int num = combined.getType(names[i]).size();
            if (num < minSize) {
                minType = b;
                minSize = num;
            }
        }

        if (minType != null) {
            list = list.getType(minType);
        }

        return list.get(0);
    }

    /**
     * <p>areAllBasics.</p>
     *
     * @param types a {@link java.lang.String} object.
     * @return a boolean.
     */
    private static boolean areAllBasics(final String types) {
        String[] split = types.split(",");
        String[] names = {"Plains", "Island", "Swamp", "Mountain", "Forest"};
        boolean[] bBasic = new boolean[split.length];

        for (String s : names) {
            for (int i = 0; i < split.length; i++) {
                bBasic[i] |= s.equals(split[i]);
            }
        }

        for (int i = 0; i < split.length; i++) {
            if (!bBasic[i]) {
                return false;
            }
        }

        return true;
    }


    // *************************************************************************************
    // **************** Known Origin (Battlefield/Graveyard/Exile) *************************
    // ******* Known origin cards are chosen during casting of the spell (target) **********
    // *************************************************************************************

    /**
     * <p>changeKnownOriginCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeKnownOriginCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // Retrieve either this card, or target Cards in Graveyard
        Cost abCost = af.getAbCost();
        final Card source = af.getHostCard();
        HashMap<String, String> params = af.getMapParams();

        Zone origin = Zone.smartValueOf(params.get("Origin"));
        Zone destination = Zone.smartValueOf(params.get("Destination"));

        float pct = origin.equals(Zone.Battlefield) ? .8f : .667f;

        Random r = MyRandom.random;

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkSacrificeCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getRestrictions().getNumberTurnActivations());

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            if (!changeKnownPreferredTarget(af, sa, false)) {
                return false;
            }
        } else {
            // non-targeted retrieval
            CardList retrieval = knownDetermineDefined(sa, params.get("Defined"), origin);

            if (retrieval == null || retrieval.isEmpty()) {
                return false;
            }

            //if (origin.equals("Graveyard")) {
            // return this card from graveyard: cards like Hammer of Bogardan
            // in general this is cool, but we should add some type of restrictions

            // return this card from battlefield: cards like Blinking Spirit
            // in general this should only be used to protect from Imminent Harm (dying or losing control of)
            if (origin.equals(Zone.Battlefield)) {
                if (AllZone.getStack().size() == 0) {
                    return false;
                }

                Ability_Sub abSub = sa.getSubAbility();
                String subAPI = "";
                if (abSub != null) {
                    subAPI = abSub.getAbilityFactory().getAPI();
                }

                //only use blink or bounce effects
                if (!(destination.equals(Zone.Exile) && (subAPI.equals("DelayedTrigger") || subAPI.equals("ChangeZone")))
                        && !destination.equals(Zone.Hand))
                {
                    return false;
                }

                ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(af);

                for (Card c : retrieval) {
                    if (objects.contains(c)) {
                        pct = 1;
                    }
                }
                if (pct < 1) {
                    return false;
                }
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAI_Drawback();
        }

        return ((r.nextFloat() < pct) && chance);
    }

    /**
     * <p>changeKnownOriginPlayDrawbackAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeKnownOriginPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        if (sa.getTarget() == null) {
            return true;
        }

        return changeKnownPreferredTarget(af, sa, false);
    }

    /**
     * <p>changeKnownPreferredTarget.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean changeKnownPreferredTarget(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory)
    {
        HashMap<String, String> params = af.getMapParams();
        Card source = sa.getSourceCard();
        Zone origin = Zone.smartValueOf(params.get("Origin"));
        Zone destination = Zone.smartValueOf(params.get("Destination"));
        Target tgt = af.getAbTgt();

    	Ability_Sub abSub = sa.getSubAbility();
    	String subAPI = "";
    	String subAffected = "";
    	HashMap<String, String> subParams = null;
    	if (abSub != null) {
    		subAPI = abSub.getAbilityFactory().getAPI();
    		subParams = abSub.getAbilityFactory().getMapParams();
    		if (subParams.containsKey("Defined")) {
                subAffected = subParams.get("Defined");
            }
    	}

        if (tgt != null) {
            tgt.resetTargets();
        }

        CardList list = AllZoneUtil.getCardsIn(origin);
        list = list.getValidCards(tgt.getValidTgts(), AllZone.getComputerPlayer(), source);

        if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            return false;
        }

        // Narrow down the list:
        if (origin.equals(Zone.Battlefield)) {
            // filter out untargetables
            list = list.getTargetableCards(source);
            CardList aiPermanents = list.getController(AllZone.getComputerPlayer());

            // if it's blink or bounce, try to save my about to die stuff
            if ((destination.equals(Zone.Hand)
            		|| (destination.equals(Zone.Exile) && (subAPI.equals("DelayedTrigger")
            				|| (subAPI.equals("ChangeZone") && subAffected.equals("Remembered")))))
            		&& tgt.getMinTargets(sa.getSourceCard(), sa) <= 1)
            {

            	// check stack for something on the stack will kill anything i control
	            if (AllZone.getStack().size() > 0) {
	            	ArrayList<Object> objects = AbilityFactory.predictThreatenedObjects(af);

	                CardList threatenedTargets = new CardList();

	                for (Card c : aiPermanents) {
	                    if (objects.contains(c)) {
                            threatenedTargets.add(c);
                        }
	                }

	                if (!threatenedTargets.isEmpty()) {
		                // Choose "best" of the remaining to save
		                tgt.addTarget(CardFactoryUtil.AI_getBestCreature(threatenedTargets));
		                return true;
	                }
	            }
	            // Save combatants
	            else if (AllZone.getPhase().is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)) {
	                CardList combatants = aiPermanents.getType("Creature");
	                CardListUtil.sortByEvaluateCreature(combatants);

	                for (Card c : combatants) {
	                    if (c.getShield() == 0 && CombatUtil.combatantWouldBeDestroyed(c)) {
	                        tgt.addTarget(c);
	                        return true;
	                    }
	                }
	            }
            }

        } else if (origin.equals(Zone.Graveyard)) {
            // Retrieve from Graveyard to:

        }

        //blink human targets only during combat
        if (origin.equals(Zone.Battlefield) && destination.equals(Zone.Exile)
        		&& (subAPI.equals("DelayedTrigger") || (subAPI.equals("ChangeZone") && subAffected.equals("Remembered")))
        		&& !(AllZone.getPhase().is(Constant.Phase.Combat_Declare_Attackers_InstantAbility) || sa.isAbility()))
        {
            return false;
        }

        // Exile and bounce opponents stuff
        if (destination.equals(Zone.Exile) || origin.equals(Zone.Battlefield)) {
            list = list.getController(AllZone.getHumanPlayer());
        }

        // Only care about combatants during combat
        if (AllZone.getPhase().inCombat()) {
            list.getValidCards("Card.attacking,Card.blocking", null, null);
        }

        if (list.isEmpty()) {
            return false;
        }

        if (!mandatory && list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            return false;
        }

        // target loop
        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            // AI Targeting
            Card choice = null;

            if (!list.isEmpty()) {
                Card mostExpensive = CardFactoryUtil.AI_getMostExpensivePermanent(list, af.getHostCard(), false);
                if (destination.equals(Zone.Battlefield) || origin.equals(Zone.Battlefield)) {
                    if (mostExpensive.isCreature()) {
                        //if a creature is most expensive take the best one
                        if (destination.equals(Zone.Exile)) {
                            // If Exiling things, don't give bonus to Tokens
                            choice = CardFactoryUtil.AI_getBestCreature(list);
                        } else {
                            choice = CardFactoryUtil.AI_getBestCreatureToBounce(list);
                        }
                    } else {
                        choice = mostExpensive;
                    }
                } else {
                    // TODO AI needs more improvement to it's retrieval (reuse some code from spReturn here)
                    list.shuffle();
                    choice = list.get(0);
                }
            }
            if (choice == null) {    // can't find anything left
                if (tgt.getNumTargeted() == 0 || tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            list.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>changeKnownUnpreferredTarget.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean changeKnownUnpreferredTarget(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory)
    {
        if (!mandatory) {
            return false;
        }

        HashMap<String, String> params = af.getMapParams();
        Card source = sa.getSourceCard();
        Zone origin = Zone.smartValueOf(params.get("Origin"));
        Zone destination = Zone.smartValueOf(params.get("Destination"));
        Target tgt = af.getAbTgt();

        CardList list = AllZoneUtil.getCardsIn(origin);
        list = list.getValidCards(tgt.getValidTgts(), AllZone.getComputerPlayer(), source);


        // Narrow down the list:
        if (origin.equals(Zone.Battlefield)) {
            // filter out untargetables
            list = list.getTargetableCards(source);

            // if Destination is hand, either bounce opponents dangerous stuff or save my about to die stuff

            // if Destination is exile, filter out my cards
        } else if (origin.equals(Zone.Graveyard)) {
            // Retrieve from Graveyard to:

        }

        for (Card c : tgt.getTargetCards()) {
            list.remove(c);
        }

        if (list.isEmpty()) {
            return false;
        }

        // target loop
        while (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            // AI Targeting
            Card choice = null;

            if (!list.isEmpty()) {
                if (CardFactoryUtil.AI_getMostExpensivePermanent(list, af.getHostCard(), false).isCreature()
                        && (destination.equals(Zone.Battlefield) || origin.equals(Zone.Battlefield)))
                {
                    //if a creature is most expensive take the best
                    choice = CardFactoryUtil.AI_getBestCreatureToBounce(list);
                } else if (destination.equals(Zone.Battlefield) || origin.equals(Zone.Battlefield)) {
                    choice = CardFactoryUtil.AI_getMostExpensivePermanent(list, af.getHostCard(), false);
                } else {
                    // TODO AI needs more improvement to it's retrieval (reuse some code from spReturn here)
                    list.shuffle();
                    choice = list.get(0);
                }
            }
            if (choice == null) {    // can't find anything left
                if (tgt.getNumTargeted() == 0 || tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            list.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }

    /**
     * <p>changeKnownOriginTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean changeKnownOriginTriggerAI(final AbilityFactory af, final SpellAbility sa,
            final boolean mandatory)
    {
        if (!ComputerUtil.canPayCost(sa)) {
            return false;
        }

        if (sa.getTarget() == null) {
            // Just in case of Defined cases
            // do nothing
        } else if (changeKnownPreferredTarget(af, sa, mandatory)) {
            // do nothing
        } else if (!changeKnownUnpreferredTarget(af, sa, mandatory)) {
            return false;
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            return subAb.doTrigger(mandatory);
        }

        return true;
    }


    /**
     * <p>changeKnownOriginStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeKnownOriginStackDescription(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();

        StringBuilder sb = new StringBuilder();
        Card host = af.getHostCard();

        if (!(sa instanceof Ability_Sub)) {
            sb.append(host.getName()).append(" -");
        }

        sb.append(" ");

        Zone destination = Zone.smartValueOf(params.get("Destination"));
        Zone origin = Zone.smartValueOf(params.get("Origin"));

        StringBuilder sbTargets = new StringBuilder();

        ArrayList<Card> tgts;
        if (af.getAbTgt() != null) {
            tgts = af.getAbTgt().getTargetCards();
        } else {
            // otherwise add self to list and go from there
            tgts = new ArrayList<Card>();
            for (Card c : knownDetermineDefined(sa, params.get("Defined"), origin)) {
                tgts.add(c);
            }
        }

        for (Card c : tgts) {
            sbTargets.append(" ").append(c.getName());
        }

        String targetname = sbTargets.toString();

        String pronoun = tgts.size() > 1 ? " their " : " its ";

        String fromGraveyard = " from the graveyard";

        if (destination.equals(Zone.Battlefield)) {
            sb.append("Put").append(targetname);
            if (origin.equals(Zone.Graveyard)) {
                sb.append(fromGraveyard);
            }

            sb.append(" onto the battlefield");
            if (params.containsKey("Tapped")) {
                sb.append(" tapped");
            }
            if (params.containsKey("GainControl")) {
                sb.append(" under your control");
            }
            sb.append(".");
        }

        if (destination.equals(Zone.Hand)) {
            sb.append("Return").append(targetname);
            if (origin.equals(Zone.Graveyard)) {
                sb.append(fromGraveyard);
            }
            sb.append(" to").append(pronoun).append("owners hand.");
        }

        if (destination.equals(Zone.Library)) {
            if (params.containsKey("Shuffle")) {    // for things like Gaea's Blessing
                sb.append("Shuffle").append(targetname);

                sb.append(" into").append(pronoun).append("owner's library.");
            } else {
                sb.append("Put").append(targetname);
                if (origin.equals(Zone.Graveyard)) {
                    sb.append(fromGraveyard);
                }

                // this needs to be zero indexed. Top = 0, Third = 2, -1 = Bottom
                int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;

                if (libraryPosition == -1) {
                    sb.append(" on the bottom of").append(pronoun).append("owner's library.");
                } else if (libraryPosition == 0) {
                    sb.append(" on top of").append(pronoun).append("owner's library.");
                } else {
                    sb.append(" ").append(libraryPosition + 1).append(" from the top of");
                    sb.append(pronoun).append("owner's library.");
                }
            }
        }

        if (destination.equals(Zone.Exile)) {
            sb.append("Exile").append(targetname);
            if (origin.equals(Zone.Graveyard)) {
                sb.append(fromGraveyard);
            }
            sb.append(".");
        }

        if (destination.equals(Zone.Graveyard)) {
            sb.append("Put").append(targetname);
            sb.append(" from ").append(origin);
            sb.append(" into").append(pronoun).append("owner's graveyard.");
        }

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>changeKnownOriginResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeKnownOriginResolve(final AbilityFactory af, final SpellAbility sa) {
        ArrayList<Card> tgtCards;
        HashMap<String, String> params = af.getMapParams();
        Target tgt = af.getAbTgt();
        Player player = sa.getActivatingPlayer();
        Card hostCard = af.getHostCard();

        Zone destination = Zone.valueOf(params.get("Destination"));
        Zone origin = Zone.valueOf(params.get("Origin"));

        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = new ArrayList<Card>();
            for (Card c : knownDetermineDefined(sa, params.get("Defined"), origin)) {
                tgtCards.add(c);
            }
        }

        String remember = params.get("RememberChanged");
        String imprint = params.get("Imprint");

        if (params.containsKey("Unimprint")) {
            hostCard.clearImprinted();
        }

        if (tgtCards.size() != 0) {
            for (Card tgtC : tgtCards) {
                PlayerZone originZone = AllZone.getZoneOf(tgtC);
                // if Target isn't in the expected Zone, continue
                if (originZone == null || !originZone.is(origin)) {
                    continue;
                }

                if (tgt != null && origin.equals(Zone.Battlefield)) {
                    // check targeting
                    if (!CardFactoryUtil.canTarget(sa.getSourceCard(), tgtC)) {
                        continue;
                    }
                }

                Card movedCard = null;
                Player pl = player;
                if (!destination.equals(Zone.Battlefield)) {
                    pl = tgtC.getOwner();
                }

                if (destination.equals(Zone.Library)) {
                    // library position is zero indexed
                    int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;

                    movedCard = AllZone.getGameAction().moveToLibrary(tgtC, libraryPosition);

                    // for things like Gaea's Blessing
                    if (params.containsKey("Shuffle")) {
                        tgtC.getOwner().shuffle();
                    }
                } else {
                    if (destination.equals(Zone.Battlefield)) {
                        if (params.containsKey("Tapped") || params.containsKey("Ninjutsu")) {
                            tgtC.tap();
                        }
                        if (params.containsKey("GainControl")) {
                            tgtC.addController(af.getHostCard());
                        }
                        // Auras without Candidates stay in their current location
                    	if (tgtC.isAura()) {
                    		SpellAbility saAura = AbilityFactory_Attach.getAttachSpellAbility(tgtC);
                    		if (!saAura.getTarget().hasCandidates(false)) {
                    			continue;
                    		}
                    	}

                    	movedCard = AllZone.getGameAction().moveTo(tgtC.getController().getZone(destination), tgtC);

                        if (params.containsKey("Ninjutsu") || params.containsKey("Attacking")) {
                            AllZone.getCombat().addAttacker(tgtC);
                            AllZone.getCombat().addUnblockedAttacker(tgtC);
                        }
                    } else {
                        movedCard = AllZone.getGameAction().moveTo(pl.getZone(destination), tgtC);
                        if (params.containsKey("ExileFaceDown")) {
                            movedCard.setIsFaceDown(true);
                        }
                    }
                }
                if (remember != null) {
                    hostCard.addRemembered(movedCard);
                }
                if (imprint != null) {
                    hostCard.addImprinted(movedCard);
                }
            }
        }
    }

    // **************************** Known Utility **************************************
    /**
     * <p>knownDetermineDefined.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param defined a {@link java.lang.String} object.
     * @param origin a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    private static CardList knownDetermineDefined(final SpellAbility sa, final String defined, final Zone origin) {
        CardList ret = new CardList();

        ret.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), defined, sa).toArray());
        return ret;
    }

    // *************************************************************************************
    // ************************** ChangeZoneAll ********************************************
    // ************ All is non-targeted and should occur similarly to Hidden ***************
    // ******* Instead of choosing X of type on resolution, all on type go *****************
    // *************************************************************************************
    /**
     * <p>createAbilityChangeZoneAll.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityChangeZoneAll(final AbilityFactory af) {
        final SpellAbility abChangeZone = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3728332812890211671L;

            public boolean canPlayAI() {
                return changeZoneAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                changeZoneAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return changeZoneAllDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return changeZoneAllCanPlayAI(af, this);
            }

        };
        setMiscellaneous(af, abChangeZone);
        return abChangeZone;
    }

    /**
     * <p>createSpellChangeZoneAll.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellChangeZoneAll(final AbilityFactory af) {
        final SpellAbility spChangeZone = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3270484211099902059L;

            public boolean canPlayAI() {
                return changeZoneAllCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                changeZoneAllResolve(af, this);
            }

            @Override
            public String getStackDescription() {
                return changeZoneAllDescription(af, this);
            }
        };
        setMiscellaneous(af, spChangeZone);
        return spChangeZone;
    }

    /**
     * <p>createDrawbackChangeZoneAll.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackChangeZoneAll(final AbilityFactory af) {
        final SpellAbility dbChangeZone = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 3270484211099902059L;

            @Override
            public void resolve() {
                changeZoneAllResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return changeZoneAllPlayDrawbackAI(af, this);
            }

            @Override
            public String getStackDescription() {
                return changeZoneAllDescription(af, this);
            }

            @Override
            public boolean doTrigger(final boolean mandatory) {
                return changeZoneAllCanPlayAI(af, this);
            }
        };
        setMiscellaneous(af, dbChangeZone);
        return dbChangeZone;
    }


    /**
     * <p>changeZoneAllCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeZoneAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        // Change Zone All, can be any type moving from one zone to another
        Cost abCost = af.getAbCost();
        Card source = sa.getSourceCard();
        HashMap<String, String> params = af.getMapParams();
        String destination = params.get("Destination");
        Constant.Zone origin = Constant.Zone.smartValueOf(params.get("Origin"));


        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(abCost, source, 4)) {
                return false;
            }
            
            if (!CostUtil.checkDiscardCost(abCost, source)) {
                return false;
            }

        }

        Random r = MyRandom.random;
        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // TODO targeting with ChangeZoneAll
        // really two types of targeting.
        // Target Player has all their types change zones
        // or target permanent and do something relative to that permanent
        // ex. "Return all Auras attached to target"
        // ex. "Return all blocking/blocked by target creature"

        CardList humanType = AllZone.getHumanPlayer().getCardsIn(origin);
        humanType = AbilityFactory.filterListByType(humanType, params.get("ChangeType"), sa);
        CardList computerType = AllZone.getComputerPlayer().getCardsIn(origin);
        computerType = AbilityFactory.filterListByType(computerType, params.get("ChangeType"), sa);

        // TODO improve restrictions on when the AI would want to use this
        // spBounceAll has some AI we can compare to.
        if (origin.equals(Zone.Hand)) {

        } else if (origin.equals(Zone.Library)) {

        } else if (origin.equals(Zone.Battlefield)) {
            // this statement is assuming the AI is trying to use this spell offensively
            // if the AI is using it defensively, then something else needs to occur
            // if only creatures are affected evaluate both lists and pass only if human creatures are more valuable
            if (humanType.getNotType("Creature").size() == 0 && computerType.getNotType("Creature").size() == 0) {
                if (CardFactoryUtil.evaluateCreatureList(computerType) + 200 >= CardFactoryUtil.evaluateCreatureList(humanType))
                    return false;
            } // otherwise evaluate both lists by CMC and pass only if human permanents are more valuable
            else if (CardFactoryUtil.evaluatePermanentList(computerType) + 3 >= CardFactoryUtil.evaluatePermanentList(humanType))
                return false;

            // Don't cast during main1?
            if (AllZone.getPhase().is(Constant.Phase.Main1, AllZone.getComputerPlayer())) {
                return false;
            }
        } else if (origin.equals(Zone.Graveyard)) {
            Target tgt = af.getAbTgt();
            if (tgt != null) {
                if (AllZone.getHumanPlayer().getCardsIn(Zone.Graveyard).isEmpty()) {
                    return false;
                }
                tgt.resetTargets();
                tgt.addTarget(AllZone.getHumanPlayer());
            }
        } else if (origin.equals(Zone.Exile)) {

        } else if (origin.equals(Zone.Stack)) {
            // time stop can do something like this:
            // Origin$ Stack | Destination$ Exile | SubAbility$ DBSkip
            // DBSKipToPhase | DB$SkipToPhase | Phase$ Cleanup
            // otherwise, this situation doesn't exist
            return false;
        } 
        
        if (destination.equals(Constant.Zone.Battlefield)) {
            if (params.get("GainControl") != null) {
                // Check if the cards are valuable enough
                if (humanType.getNotType("Creature").size() == 0 && computerType.getNotType("Creature").size() == 0) {
                    if (CardFactoryUtil.evaluateCreatureList(computerType) + CardFactoryUtil.evaluateCreatureList(humanType) < 400)
                        return false;
                } // otherwise evaluate both lists by CMC and pass only if human permanents are less valuable
                else if (CardFactoryUtil.evaluatePermanentList(computerType) + CardFactoryUtil.evaluatePermanentList(humanType) < 6)
                    return false;
            } else {
                // don't activate if human gets more back than AI does
                if (humanType.getNotType("Creature").size() == 0 && computerType.getNotType("Creature").size() == 0) {
                    if (CardFactoryUtil.evaluateCreatureList(computerType) <= CardFactoryUtil.evaluateCreatureList(humanType) + 100)
                        return false;
                } // otherwise evaluate both lists by CMC and pass only if human permanents are less valuable
                else if (CardFactoryUtil.evaluatePermanentList(computerType) <= CardFactoryUtil.evaluatePermanentList(humanType) + 2)
                    return false;
            }
        }

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAI_Drawback();
        }

        return ((r.nextFloat() < .8 || sa.isTrigger()) && chance);
    }

    /**
     * <p>changeZoneAllPlayDrawbackAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean changeZoneAllPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa) {
        // if putting cards from hand to library and parent is drawing cards
        // make sure this will actually do something:


        return true;
    }

    /**
     * <p>changeZoneAllDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeZoneAllDescription(final AbilityFactory af, final SpellAbility sa) {
        // TODO build Stack Description will need expansion as more cards are added
        StringBuilder sb = new StringBuilder();
        Card host = af.getHostCard();

        if (!(sa instanceof Ability_Sub)) {
            sb.append(host.getName()).append(" -");
        }

        sb.append(" ");

        String[] desc = sa.getDescription().split(":");

        if (desc.length > 1) {
            sb.append(desc[1]);
        } else {
            sb.append(desc[0]);
        }

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>changeZoneAllResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeZoneAllResolve(final AbilityFactory af, final SpellAbility sa) {
        HashMap<String, String> params = af.getMapParams();
        Zone destination = Zone.smartValueOf(params.get("Destination"));
        List<Zone> origin = Zone.listValueOf(params.get("Origin"));

        CardList cards = null;

        ArrayList<Player> tgtPlayers = null;

        Target tgt = af.getAbTgt();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (params.containsKey("Defined")) {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (tgtPlayers == null || tgtPlayers.isEmpty()) {
            cards = AllZoneUtil.getCardsIn(origin);
        } else {
            cards = tgtPlayers.get(0).getCardsIn(origin);
        }

        cards = AbilityFactory.filterListByType(cards, params.get("ChangeType"), sa);

        if (params.containsKey("ForgetOtherRemembered")) {
            sa.getSourceCard().clearRemembered();
        }

        String remember = params.get("RememberChanged");

        // I don't know if library position is necessary. It's here if it is, just in case
        int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;
        for (Card c : cards) {
            if (destination.equals(Zone.Battlefield)) {
				// Auras without Candidates stay in their current location
            	if (c.isAura()) {
            		SpellAbility saAura = AbilityFactory_Attach.getAttachSpellAbility(c);
            		if (!saAura.getTarget().hasCandidates(false)) {
                        continue;
                    }
            	}

            	if (params.containsKey("Tapped")) {
                    c.tap();
                }
            }

            if (params.containsKey("GainControl")) {
                c.addController(af.getHostCard());
                AllZone.getGameAction().moveToPlay(c, sa.getActivatingPlayer());
            } else {
                Card movedCard = AllZone.getGameAction().moveTo(destination, c, libraryPos);
                if (params.containsKey("ExileFaceDown")) {
                    movedCard.setIsFaceDown(true);
                }
            }

            if (remember != null) {
                sa.getSourceCard().addRemembered(c);
            }
        }

        // if Shuffle parameter exists, and any amount of cards were owned by that player, then shuffle that library
        if (params.containsKey("Shuffle")) {
            if (cards.getOwner(AllZone.getHumanPlayer()).size() > 0) {
                AllZone.getHumanPlayer().shuffle();
            }
            if (cards.getOwner(AllZone.getComputerPlayer()).size() > 0) {
                AllZone.getComputerPlayer().shuffle();
            }
        }
    }


}
