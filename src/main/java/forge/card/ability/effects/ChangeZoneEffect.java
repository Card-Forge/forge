package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.base.Predicates;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.CardPredicates;
import forge.GameEntity;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.ability.ai.ChangeZoneAi;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.Target;
import forge.card.trigger.TriggerType;
import forge.game.Game;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.gui.input.InputSelectCardsFromList;
import forge.util.Aggregates;
import forge.util.Lang;

public class ChangeZoneEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {

        String origin = "";
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }

        if (sa.hasParam("Hidden") || ZoneType.isHidden(origin)) {
            return changeHiddenOriginStackDescription(sa);
        }
        return changeKnownOriginStackDescription(sa);
    }

    /**
     * <p>
     * changeHiddenOriginStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeHiddenOriginStackDescription(final SpellAbility sa) {
        // TODO build Stack Description will need expansion as more cards are
        // added

        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();

        if (!(sa instanceof AbilitySub)) {
            sb.append(" -");
        }

        sb.append(" ");

        // Player whose cards will change zones
        List<Player> fetchers = new ArrayList<Player>();
        if (sa.hasParam("DefinedPlayer")) {
            fetchers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("DefinedPlayer"), sa);
        }
        if (fetchers.isEmpty() && sa.hasParam("ValidTgts") && sa.getTarget() != null) {
            fetchers = sa.getTarget().getTargetPlayers();
        }
        if (fetchers.isEmpty()) {
            fetchers.add(sa.getSourceCard().getController());
        }

        final String fetcherNames = Lang.joinHomogenous(fetchers, Player.Accessors.FN_GET_NAME);

        // Player who chooses the cards to move
        List<Player> choosers = new ArrayList<Player>();
        if (sa.hasParam("Chooser")) {
            choosers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Chooser"), sa);
        }
        if (choosers.isEmpty()) {
            choosers.add(sa.getActivatingPlayer());
        }

        final StringBuilder chooserSB = new StringBuilder();
        for (int i = 0; i < choosers.size(); i++) {
            chooserSB.append(choosers.get(i).getName());
            chooserSB.append((i + 2) == choosers.size() ? " and " : (i + 1) == choosers.size() ? "" : ", ");
        }
        final String chooserNames = chooserSB.toString();

        String fetchPlayer = fetcherNames;
        if (chooserNames.equals(fetcherNames)) {
            fetchPlayer = fetchers.size() > 1 ? "their" : "his/her";
        }

        String origin = "";
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }
        final String destination = sa.getParam("Destination");

        final String type = sa.hasParam("ChangeType") ? sa.getParam("ChangeType") : "Card";
        final int num = sa.hasParam("ChangeNum") ? AbilityUtils.calculateAmount(host,
                sa.getParam("ChangeNum"), sa) : 1;

        if (origin.equals("Library") && sa.hasParam("Defined")) {
            // for now, just handle the Exile from top of library case, but
            // this can be expanded...
            if (destination.equals("Exile")) {
                sb.append("Exile the top card of your library");
                if (sa.hasParam("ExileFaceDown")) {
                    sb.append(" face down");
                }
            } else if (destination.equals("Ante")) {
                sb.append("Add the top card of your library to the ante");
            }
            sb.append(".");
        } else if (origin.equals("Library")) {
            sb.append(chooserNames);
            sb.append(" search").append(choosers.size() > 1 ? " " : "es ");
            sb.append(fetchPlayer);
            sb.append("'s library for ").append(num).append(" ").append(type).append(" and ");

            if (destination.equals("Exile")) {
                if (num == 1) {
                    sb.append("exiles that card ");
                } else {
                    sb.append("exiles those cards ");
                }
            } else {
                if (num == 1) {
                    sb.append("puts that card ");
                } else {
                    sb.append("puts those cards ");
                }

                if (destination.equals("Battlefield")) {
                    sb.append("onto the battlefield");
                    if (sa.hasParam("Tapped")) {
                        sb.append(" tapped");
                    }
                    if (sa.hasParam("GainControl")) {
                        sb.append(" under ").append(chooserNames).append("'s control");
                    }

                    sb.append(".");

                }
                if (destination.equals("Hand")) {
                    sb.append("into its owner's hand.");
                }
                if (destination.equals("Graveyard")) {
                    sb.append("into its owners's graveyard.");
                }
            }
            sb.append(" Then shuffle that library.");
        } else if (origin.equals("Hand")) {
            sb.append(chooserNames);
            if (!chooserNames.equals(fetcherNames)) {
                sb.append(" looks at " + fetcherNames + "'s hand and ");
                sb.append(destination.equals("Exile") ? "exiles " : "puts ");
                sb.append(num).append(" of those ").append(type).append(" card(s)");
            } else {
                sb.append(destination.equals("Exile") ? " exiles " : " puts ");
                sb.append(num).append(" ").append(type).append(" card(s) from");
                sb.append(fetchPlayer).append(" hand");
            }

            if (destination.equals("Battlefield")) {
                sb.append(" onto the battlefield");
                if (sa.hasParam("Tapped")) {
                    sb.append(" tapped");
                }
                if (sa.hasParam("GainControl")) {
                    sb.append(" under ").append(chooserNames).append("'s control");
                }
            }
            if (destination.equals("Library")) {
                final int libraryPos = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : 0;

                if (libraryPos == 0) {
                    sb.append(" on top");
                }
                if (libraryPos == -1) {
                    sb.append(" on the bottom");
                }

                sb.append(" of ").append(fetchPlayer).append("'s library");
            }

            sb.append(".");
        } else if (origin.equals("Battlefield")) {
            // TODO Expand on this Description as more cards use it
            // for the non-targeted SAs when you choose what is returned on
            // resolution
            sb.append("Return ").append(num).append(" ").append(type).append(" card(s) ");
            sb.append(" to your ").append(destination);
        }

        return sb.toString();
    }

    /**
     * <p>
     * changeKnownOriginStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeKnownOriginStackDescription(final SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();

        if (!(sa instanceof AbilitySub)) {
            sb.append(host.getName()).append(" -");
        }

        sb.append(" ");

        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final ZoneType origin = ZoneType.smartValueOf(sa.getParam("Origin"));

        final StringBuilder sbTargets = new StringBuilder();

        List<Card> tgts;
        if (sa.getTarget() != null) {
            tgts = sa.getTarget().getTargetCards();
        } else {
            // otherwise add self to list and go from there
            tgts = new ArrayList<Card>();
            for (final Card c : sa.knownDetermineDefined(sa.getParam("Defined"))) {
                tgts.add(c);
            }
        }

        for (final Card c : tgts) {
            sbTargets.append(" ").append(c);
        }

        final String targetname = sbTargets.toString();

        final String pronoun = tgts.size() > 1 ? " their " : " its ";

        final String fromGraveyard = " from the graveyard";

        if (destination.equals(ZoneType.Battlefield)) {
            sb.append("Put").append(targetname);
            if (origin.equals(ZoneType.Graveyard)) {
                sb.append(fromGraveyard);
            }

            sb.append(" onto the battlefield");
            if (sa.hasParam("Tapped")) {
                sb.append(" tapped");
            }
            if (sa.hasParam("GainControl")) {
                sb.append(" under your control");
            }
            sb.append(".");
        }

        if (destination.equals(ZoneType.Hand)) {
            sb.append("Return").append(targetname);
            if (origin.equals(ZoneType.Graveyard)) {
                sb.append(fromGraveyard);
            }
            sb.append(" to").append(pronoun).append("owners hand.");
        }

        if (destination.equals(ZoneType.Library)) {
            if (sa.hasParam("Shuffle")) { // for things like Gaea's
                                          // Blessing
                sb.append("Shuffle").append(targetname);

                sb.append(" into").append(pronoun).append("owner's library.");
            } else {
                sb.append("Put").append(targetname);
                if (origin.equals(ZoneType.Graveyard)) {
                    sb.append(fromGraveyard);
                }

                // this needs to be zero indexed. Top = 0, Third = 2, -1 =
                // Bottom
                final int libraryPosition = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : 0;

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

        if (destination.equals(ZoneType.Exile)) {
            sb.append("Exile").append(targetname);
            if (origin.equals(ZoneType.Graveyard)) {
                sb.append(fromGraveyard);
            }
            sb.append(".");
        }

        if (destination.equals(ZoneType.Ante)) {
            sb.append("Ante").append(targetname);
            sb.append(".");
        }

        if (destination.equals(ZoneType.Graveyard)) {
            sb.append("Put").append(targetname);
            sb.append(" from ").append(origin);
            sb.append(" into").append(pronoun).append("owner's graveyard.");
        }

        return sb.toString();
    }

    /**
     * <p>
     * changeZoneResolve.
     * </p>
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     */

    @Override
    public void resolve(SpellAbility sa) {
        String origin = "";
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }
        if ((sa.hasParam("Hidden") || ZoneType.isHidden(origin)) && !sa.hasParam("Ninjutsu")) {
            changeHiddenOriginResolve(sa);
        } else {
            //else if (isKnown(origin) || sa.containsKey("Ninjutsu")) {
            // Why is this an elseif and not just an else?
            changeKnownOriginResolve(sa);
        }
    }

    /**
     * <p>
     * changeKnownOriginResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeKnownOriginResolve(final SpellAbility sa) {
        List<Card> tgtCards;
        List<SpellAbility> sas;

        final Target tgt = sa.getTarget();
        final Player player = sa.getActivatingPlayer();
        final Card hostCard = sa.getSourceCard();
        final Game game = player.getGame();

        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final List<ZoneType> origin = ZoneType.listValueOf(sa.getParam("Origin"));

        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = new ArrayList<Card>();
            for (final Card c : AbilityUtils.getDefinedCards(hostCard, sa.getParam("Defined"), sa)) {
                tgtCards.add(c);
            }
        }

        // changing zones for spells on the stack
        if (tgt != null) {
            sas = tgt.getTargetSAs();
        } else {
            sas = AbilityUtils.getDefinedSpellAbilities(hostCard, sa.getParam("Defined"), sa);
        }

        for (final SpellAbility tgtSA : sas) {
            if (!tgtSA.isSpell()) { // Catch any abilities or triggers that slip through somehow
                continue;
            }

            final SpellAbilityStackInstance si = game.getStack().getInstanceFromSpellAbility(tgtSA);
            if (si == null) {
                continue;
            }

            removeFromStack(tgtSA, sa, si, game);
        } // End of change from stack

        final String remember = sa.getParam("RememberChanged");
        final String forget = sa.getParam("ForgetChanged");
        final String imprint = sa.getParam("Imprint");

        if (sa.hasParam("Unimprint")) {
            hostCard.clearImprinted();
        }

        if (sa.hasParam("ForgetOtherRemembered")) {
            hostCard.clearRemembered();
        }

        boolean optional = sa.hasParam("Optional");

        if (tgtCards.size() != 0) {
            for (final Card tgtC : tgtCards) {
                if (tgt != null && tgtC.isInPlay() && !tgtC.canBeTargetedBy(sa)) {
                    continue;
                }
                final String prompt = "Do you want to move " + tgtC + " from " + origin + " to " + destination + "?";
                if (player.isHuman() && optional && !GuiDialog.confirm(hostCard, prompt)) {
                    continue;
                }
                final Zone originZone = game.getZoneOf(tgtC);

                // if Target isn't in the expected Zone, continue

                if (originZone == null || !origin.contains(originZone.getZoneType())) {
                    continue;
                }

                Card movedCard = null;

                if (destination.equals(ZoneType.Library)) {
                    // library position is zero indexed
                    final int libraryPosition = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : 0;

                    movedCard = game.getAction().moveToLibrary(tgtC, libraryPosition);

                    // for things like Gaea's Blessing
                    if (sa.hasParam("Shuffle")) {
                        tgtC.getOwner().shuffle();
                    }
                } else {
                    if (destination.equals(ZoneType.Battlefield)) {
                        if (sa.hasParam("Tapped") || sa.hasParam("Ninjutsu")) {
                            tgtC.setTapped(true);
                        }
                        if (sa.hasParam("GainControl")) {
                            if (sa.hasParam("NewController")) {
                                final Player p = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("NewController"), sa).get(0);
                                tgtC.setController(p, game.getNextTimestamp());
                            } else {
                                tgtC.setController(player, game.getNextTimestamp());
                            }
                        }
                        if (sa.hasParam("AttachedTo")) {
                            List<Card> list = AbilityUtils.getDefinedCards(hostCard, sa.getParam("AttachedTo"), sa);
                            if (list.isEmpty()) {
                                list = game.getCardsIn(ZoneType.Battlefield);
                                list = CardLists.getValidCards(list, sa.getParam("AttachedTo"), tgtC.getController(), tgtC);
                            }
                            if (!list.isEmpty()) {
                                Card attachedTo = null;
                                if (player.isHuman()) {
                                    if (list.size() > 1) {
                                        attachedTo = GuiChoose.one(tgtC + " - Select a card to attach to.", list);
                                    } else {
                                        attachedTo = list.get(0);
                                    }
                                } else { // AI player
                                    attachedTo = ComputerUtilCard.getBestAI(list);
                                }
                                if (tgtC.isAura()) {
                                    if (tgtC.isEnchanting()) {
                                        // If this Card is already Enchanting something, need
                                        // to unenchant it, then clear out the commands
                                        final GameEntity oldEnchanted = tgtC.getEnchanting();
                                        tgtC.removeEnchanting(oldEnchanted);
                                    }
                                    tgtC.enchantEntity(attachedTo);
                                } else { //Equipment
                                    if (tgtC.isEquipping()) {
                                        final Card oldEquiped = tgtC.getEquippingCard();
                                        tgtC.removeEquipping(oldEquiped);
                                    }
                                    tgtC.equipCard(attachedTo);
                                }
                            } else { // When it should enter the battlefield attached to an illegal permanent it fails
                                continue;
                            }
                        }

                        // Auras without Candidates stay in their current
                        // location
                        if (tgtC.isAura()) {
                            final SpellAbility saAura = AttachEffect.getAttachSpellAbility(tgtC);
                            saAura.setActivatingPlayer(sa.getActivatingPlayer());
                            if (!saAura.getTarget().hasCandidates(saAura, false)) {
                                continue;
                            }
                        }

                        movedCard = game.getAction().moveTo(tgtC.getController().getZone(destination), tgtC);

                        if (sa.hasParam("Ninjutsu") || sa.hasParam("Attacking")) {
                            // What should they attack?
                            List<GameEntity> defenders = game.getCombat().getDefenders();
                            if (!defenders.isEmpty()) { 
                                game.getCombat().addAttacker(tgtC, defenders.get(0));
                                game.getCombat().addUnblockedAttacker(tgtC);
                            }
                        }
                        if (sa.hasParam("Tapped") || sa.hasParam("Ninjutsu")) {
                            tgtC.setTapped(true);
                        }
                    } else {
                        movedCard = game.getAction().moveTo(destination, tgtC);
                        // If a card is Exiled from the stack, remove its spells from the stack
                        if (sa.hasParam("Fizzle")) {
                            ArrayList<SpellAbility> spells = tgtC.getSpellAbilities();
                            for (SpellAbility spell : spells) {
                                if (tgtC.isInZone(ZoneType.Exile)) {
                                    final SpellAbilityStackInstance si = game.getStack().getInstanceFromSpellAbility(spell);
                                    game.getStack().remove(si);
                                }
                            }
                        }
                        if (sa.hasParam("ExileFaceDown")) {
                            movedCard.setState(CardCharacteristicName.FaceDown);
                        }
                    }
                }
                if (remember != null) {
                    hostCard.addRemembered(movedCard);
                }
                if (forget != null) {
                    hostCard.getRemembered().remove(movedCard);
                }
                if (imprint != null) {
                    hostCard.addImprinted(movedCard);
                }
            }
        }
    }

    /**
     * <p>
     * changeHiddenOriginResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeHiddenOriginResolve(final SpellAbility sa) {
        List<Player> fetchers;

        if (sa.hasParam("DefinedPlayer")) {
            fetchers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("DefinedPlayer"), sa);
        } else {
            fetchers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Defined"), sa);
        }

        // handle case when Defined is for a Card
        if (fetchers.isEmpty()) {
            fetchers.add(sa.getSourceCard().getController());
        }

        Player chooser = null;
        if (sa.hasParam("Chooser")) {
            final String choose = sa.getParam("Chooser");
            if (choose.equals("Targeted") && (sa.getTarget().getTargetPlayers() != null)) {
                chooser = sa.getTarget().getTargetPlayers().get(0);
            } else {
                chooser = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), choose, sa).get(0);
            }
        }

        for (final Player player : fetchers) {
            Player decider = chooser;
            if (decider == null) {
                decider = player;
            }
            if (decider.isComputer()) {
                ChangeZoneAi.hiddenOriginResolveAI(decider, sa, player);
            } else {
                changeHiddenOriginResolveHuman(decider, sa, player);
            }
        }
    }

    /**
     * <p>
     * changeHiddenOriginResolveHuman.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    private static void changeHiddenOriginResolveHuman(final Player decider, final SpellAbility sa, Player player) {
        final Card card = sa.getSourceCard();
        final List<Card> movedCards = new ArrayList<Card>();
        final boolean defined = sa.hasParam("Defined");
        final boolean optional = sa.hasParam("Optional");
        final Game game = player.getGame();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            final List<Player> players = tgt.getTargetPlayers();
            player = sa.hasParam("DefinedPlayer") ? player : players.get(0);
            if (players.contains(player) && !player.canBeTargetedBy(sa)) {
                return;
            }
        }

        List<ZoneType> origin = new ArrayList<ZoneType>();
        if (sa.hasParam("Origin")) {
            origin = ZoneType.listValueOf(sa.getParam("Origin"));
        }
        ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        // this needs to be zero indexed. Top = 0, Third = 2
        int libraryPos = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : 0;

        if (sa.hasParam("OriginChoice")) {
            // Currently only used for Mishra, but may be used by other things
            // Improve how this message reacts for other cards
            final List<ZoneType> alt = ZoneType.listValueOf(sa.getParam("OriginAlternative"));
            List<Card> altFetchList = player.getCardsIn(alt);
            altFetchList = AbilityUtils.filterListByType(altFetchList, sa.getParam("ChangeType"), sa);

            final StringBuilder sb = new StringBuilder();
            sb.append(sa.getParam("AlternativeMessage")).append(" ");
            sb.append(altFetchList.size()).append(" cards match your searching type in Alternate Zones.");

            if (!GuiDialog.confirm(card, sb.toString())) {
                origin = alt;
            }
        }

        if (sa.hasParam("DestinationAlternative")) {

            final StringBuilder sb = new StringBuilder();
            sb.append(sa.getParam("AlternativeDestinationMessage"));

            if (!GuiDialog.confirm(card, sb.toString())) {
                destination = ZoneType.smartValueOf(sa.getParam("DestinationAlternative"));
                libraryPos = sa.hasParam("LibraryPositionAlternative") ? Integer.parseInt(sa.getParam("LibraryPositionAlternative")) : 0;
            }
        }

        int changeNum = sa.hasParam("ChangeNum") ? AbilityUtils.calculateAmount(card, sa.getParam("ChangeNum"),
                sa) : 1;

        if (optional && !GuiDialog.confirm(card, "Search " + origin + "?")) {
            return;
        }

        List<Card> fetchList;
        if (defined) {
            fetchList = new ArrayList<Card>(AbilityUtils.getDefinedCards(card, sa.getParam("Defined"), sa));
            if (!sa.hasParam("ChangeNum")) {
                changeNum = fetchList.size();
            }
        } else if (!origin.contains(ZoneType.Library) && !origin.contains(ZoneType.Hand)
                && !sa.hasParam("DefinedPlayer")) {
            fetchList = game.getCardsIn(origin);
        } else {
            fetchList = player.getCardsIn(origin);
            if (origin.contains(ZoneType.Library) && decider.hasKeyword("LimitSearchLibrary") 
                    && !sa.hasParam("NoLooking")) {// Aven Mindcensor
                fetchList.removeAll(player.getCardsIn(ZoneType.Library));
                final int fetchNum = Math.min(player.getCardsIn(ZoneType.Library).size(), 4);
                fetchList.addAll(player.getCardsIn(ZoneType.Library, fetchNum));
            }
        }
        
        if (!defined) {
            if (origin.contains(ZoneType.Library) && !defined && !sa.hasParam("NoLooking")) {
                final int fetchNum = Math.min(player.getCardsIn(ZoneType.Library).size(), 4);
                List<Card> shown = !decider.hasKeyword("LimitSearchLibrary") ? player.getCardsIn(ZoneType.Library) : player.getCardsIn(ZoneType.Library, fetchNum);
                // Look at whole library before moving onto choosing a card
                decider.getController().reveal(sa.getSourceCard().getName() + " - Looking at library", shown, ZoneType.Library, player);
            }

            // Look at opponents hand before moving onto choosing a card
            if (origin.contains(ZoneType.Hand) && player.isOpponentOf(decider)) {
                decider.getController().reveal(sa.getSourceCard().getName() + " - Looking at Opponent's Hand", player.getCardsIn(ZoneType.Hand), ZoneType.Hand, player);
            }
            fetchList = AbilityUtils.filterListByType(fetchList, sa.getParam("ChangeType"), sa);
        }

        final String remember = sa.getParam("RememberChanged");
        final boolean champion = sa.hasParam("Champion");
        final String forget = sa.getParam("ForgetChanged");
        final String imprint = sa.getParam("Imprint");
        final String selectPrompt = sa.hasParam("SelectPrompt") ? sa.getParam("SelectPrompt") : "Select a card from " + origin;
        final String totalcmc = sa.getParam("WithTotalCMC");
        int totcmc = AbilityUtils.calculateAmount(card, totalcmc, sa);

        if (sa.hasParam("Unimprint")) {
            card.clearImprinted();
        }

        for (int i = 0; i < changeNum; i++) {
            if (sa.hasParam("DifferentNames")) {
                for (Card c : movedCards) {
                    fetchList = CardLists.filter(fetchList, Predicates.not(CardPredicates.nameEquals(c.getName())));
                }
            }
            if (totalcmc != null) {
                if (totcmc >= 0) {
                    fetchList = CardLists.getValidCards(fetchList, "Card.cmcLE" + Integer.toString(totcmc), card.getController(), card);
                }
            }
            if ((fetchList.size() == 0) || (destination == null)) {
                break;
            }

            // card has to be on battlefield or in own hand
            boolean canUseInputToSelectCard = origin.size() == 1 && ( origin.get(0) == ZoneType.Battlefield || origin.get(0) == ZoneType.Hand && player == decider); 
            
            Card c;
            if (sa.hasParam("AtRandom")) {
                c = Aggregates.random(fetchList);
            } else if (sa.hasParam("Defined")) {
                c = fetchList.get(0);
            } else {
                boolean mustChoose = sa.hasParam("Mandatory");
                if( canUseInputToSelectCard ) {
                    InputSelectCardsFromList inp = new InputSelectCardsFromList(1, 1, fetchList);
                    inp.setCancelAllowed(!mustChoose);
                    inp.setMessage(selectPrompt);
                    Singletons.getControl().getInputQueue().setInputAndWait(inp);
                    c = inp.hasCancelled() ? null : inp.getSelected().get(0);
                }
                else {
                    List<Card> chosen = GuiChoose.getChoices(selectPrompt, mustChoose ? 1 : 0, 1, fetchList);
                    c = chosen.isEmpty() ? null : chosen.get(0);
                }
            }

            if (c != null) {
                fetchList.remove(c);
                Card movedCard = null;

                if (destination.equals(ZoneType.Library)) {
                    // do not shuffle the library once we have placed a fetched
                    // card on top.
                    if (origin.contains(ZoneType.Library) && (i < 1) && !"False".equals(sa.getParam("Shuffle"))) {
                        player.shuffle();
                    }
                    movedCard = game.getAction().moveToLibrary(c, libraryPos);
                } else if (destination.equals(ZoneType.Battlefield)) {
                    if (sa.hasParam("Tapped")) {
                        c.setTapped(true);
                    }
                    if (sa.hasParam("GainControl")) {
                        if (sa.hasParam("NewController")) {
                            final Player p = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("NewController"), sa).get(0);
                            c.setController(p, game.getNextTimestamp());
                        } else {
                            c.setController(sa.getActivatingPlayer(), game.getNextTimestamp());
                        }
                    }

                    if (sa.hasParam("AttachedTo")) {
                        List<Card> list = AbilityUtils.getDefinedCards(sa.getSourceCard(),
                                sa.getParam("AttachedTo"), sa);
                        if (list.isEmpty()) {
                            list = game.getCardsIn(ZoneType.Battlefield);
                            list = CardLists.getValidCards(list, sa.getParam("AttachedTo"), c.getController(), c);
                        }
                        if (!list.isEmpty()) {
                            Card attachedTo = null;
                            if (list.size() > 1) {
                                attachedTo = GuiChoose.one(c + " - Select a card to attach to.", list);
                            } else {
                                attachedTo = list.get(0);
                            }
                            if (c.isAura()) {
                                if (c.isEnchanting()) {
                                    // If this Card is already Enchanting something, need
                                    // to unenchant it, then clear out the commands
                                    final GameEntity oldEnchanted = c.getEnchanting();
                                    c.removeEnchanting(oldEnchanted);
                                }
                                c.enchantEntity(attachedTo);
                            } else { //Equipment
                                if (c.isEquipping()) {
                                    final Card oldEquiped = c.getEquippingCard();
                                    c.removeEquipping(oldEquiped);
                                }
                                c.equipCard(attachedTo);
                            }
                        } else { // When it should enter the battlefield attached to an illegal permanent it fails
                            continue;
                        }
                    }

                    if (sa.hasParam("AttachedToPlayer")) {
                        List<Player> list = AbilityUtils.getDefinedPlayers(card,
                                sa.getParam("AttachedToPlayer"), sa);
                        if (!list.isEmpty()) {
                            Player attachedTo = null;

                            if (list.size() == 1) {
                                attachedTo = list.get(0);
                            } else {
                                if (player.isHuman()) {
                                    attachedTo = GuiChoose.one(c + " - Select a player to attach to.", list);
                                } else { // AI player
                                    // Currently only used by Curse of Misfortunes, so this branch should never get hit
                                    // But just in case it does, just select the first option
                                    attachedTo = list.get(0);
                                }
                            }
                            if (c.isAura()) {
                                if (c.isEnchanting()) {
                                    // If this Card is already Enchanting something, need
                                    // to unenchant it, then clear out the commands
                                    final GameEntity oldEnchanted = c.getEnchanting();
                                    c.removeEnchanting(oldEnchanted);
                                }
                                c.enchantEntity(attachedTo);
                            }
                        } else { // When it should enter the battlefield attached to an illegal permanent it fails
                            continue;
                        }
                    }

                    if (sa.hasParam("Attacking")) {
                        final List<GameEntity> e = c.getController().getGame().getCombat().getDefenders();
                        final GameEntity defender = e.size() == 1 ? e.get(0) : GuiChoose.one("Declare " + c, e);
                        game.getCombat().addAttacker(c, defender);
                    }

                    movedCard = game.getAction().moveTo(c.getController().getZone(destination), c);
                    if (sa.hasParam("Tapped")) {
                        movedCard.setTapped(true);
                    }
                } else if (destination.equals(ZoneType.Exile)) {
                    movedCard = game.getAction().exile(c);
                    if (sa.hasParam("ExileFaceDown")) {
                        movedCard.setState(CardCharacteristicName.FaceDown);
                    }
                } else {
                    movedCard = game.getAction().moveTo(destination, c);
                }
                movedCards.add(movedCard);

                if (champion) {
                    final HashMap<String, Object> runParams = new HashMap<String, Object>();
                    runParams.put("Card", card);
                    runParams.put("Championed", c);
                    game.getTriggerHandler().runTrigger(TriggerType.Championed, runParams, false);
                }
                
                if (remember != null) {
                    card.addRemembered(movedCard);
                }
                if (forget != null) {
                    sa.getSourceCard().getRemembered().remove(movedCard);
                }
                // for imprinted since this doesn't use Target
                if (imprint != null) {
                    card.addImprinted(movedCard);
                }
                if (totalcmc != null) {
                    totcmc -= movedCard.getCMC();
                }
            } else {
                final StringBuilder sb = new StringBuilder();
                final int num = Math.min(fetchList.size(), changeNum - i);
                sb.append("Cancel Search? Up to ").append(num).append(" more cards can change zones.");

                if (((i + 1) == changeNum) || GuiDialog.confirm(card, sb.toString())) {
                    break;
                }
            }
        }
        if (sa.hasParam("Reveal") && !movedCards.isEmpty()) {
            ZoneType zt = null;
            if (!origin.isEmpty()) {
                zt = origin.get(0);
            }
            decider.getController().reveal(card + " - Revealed card: ", movedCards, zt, player);
        }

        if ((origin.contains(ZoneType.Library) && !destination.equals(ZoneType.Library) && !defined)
                || (sa.hasParam("Shuffle") && "True".equals(sa.getParam("Shuffle")))) {
            player.shuffle();
        }
    }

    /**
     * <p>
     * removeFromStack.
     * </p>
     *
     * @param tgtSA
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param srcSA
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param si
     *            a {@link forge.card.spellability.SpellAbilityStackInstance}
     *            object.
     * @param game 
     */
    private static void removeFromStack(final SpellAbility tgtSA, final SpellAbility srcSA, final SpellAbilityStackInstance si, final Game game) {
        game.getStack().remove(si);

        if (srcSA.hasParam("Destination")) {
            final boolean remember = srcSA.hasParam("RememberChanged");
            if (tgtSA.isAbility()) {
                // Shouldn't be able to target Abilities but leaving this in for now
            } else if (tgtSA.isFlashBackAbility())  {
                game.getAction().exile(tgtSA.getSourceCard());
            } else if (srcSA.getParam("Destination").equals("Graveyard")) {
                game.getAction().moveToGraveyard(tgtSA.getSourceCard());
            } else if (srcSA.getParam("Destination").equals("Exile")) {
                game.getAction().exile(tgtSA.getSourceCard());
            } else if (srcSA.getParam("Destination").equals("TopOfLibrary")) {
                game.getAction().moveToLibrary(tgtSA.getSourceCard());
            } else if (srcSA.getParam("Destination").equals("Hand")) {
                game.getAction().moveToHand(tgtSA.getSourceCard());
            } else if (srcSA.getParam("Destination").equals("BottomOfLibrary")) {
                game.getAction().moveToBottomOfLibrary(tgtSA.getSourceCard());
            } else if (srcSA.getParam("Destination").equals("Library")) {
                game.getAction().moveToBottomOfLibrary(tgtSA.getSourceCard());
                if (srcSA.hasParam("Shuffle")) {
                    tgtSA.getSourceCard().getOwner().shuffle();
                }
            } else {
                throw new IllegalArgumentException("AbilityFactory_ChangeZone: Invalid Destination argument for card "
                        + srcSA.getSourceCard().getName());
            }

            if (remember) {
                srcSA.getSourceCard().addRemembered(tgtSA.getSourceCard());
            }

            if (!tgtSA.isAbility()) {
                System.out.println("Moving spell to " + srcSA.getParam("Destination"));
            }
        }
    }

}
