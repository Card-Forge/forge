package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicates;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardUtil;
import forge.GameActionUtil;
import forge.GameEntity;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.abilityfactory.ai.ChangeZoneAi;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class ChangeZoneEffect extends SpellEffect {
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {

        String origin = "";
        if (params.containsKey("Origin")) {
            origin = params.get("Origin");
        }
    
        if (ZoneType.isHidden(origin, params.containsKey("Hidden"))) {
            return changeHiddenOriginStackDescription(params, sa);
        } else if (ZoneType.isKnown(origin)) {
            return changeKnownOriginStackDescription(params, sa);
        }
    
        return "";
    }

    /**
     * <p>
     * changeHiddenOriginStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeHiddenOriginStackDescription(final Map<String,String> params, final SpellAbility sa) {
        // TODO build Stack Description will need expansion as more cards are
        // added
    
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(host.getName()).append(" -");
        }
    
        sb.append(" ");
    
        if (params.containsKey("StackDescription")) {
            String stackDesc = params.get("StackDescription");
            if (stackDesc.equals("None")) {
                // Intentionally blank to avoid double spaces, otherwise: sb.append("");
            } else if (stackDesc.equals("SpellDescription")) {
                sb.append(params.get("SpellDescription"));
            } else {
                sb.append(stackDesc);
            }
        } else {
            String origin = "";
            if (params.containsKey("Origin")) {
                origin = params.get("Origin");
            }
            final String destination = params.get("Destination");
    
            final String type = params.containsKey("ChangeType") ? params.get("ChangeType") : "Card";
            final int num = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(host,
                    params.get("ChangeNum"), sa) : 1;
    
            if (origin.equals("Library") && params.containsKey("Defined")) {
                // for now, just handle the Exile from top of library case, but
                // this can be expanded...
                if (destination.equals("Exile")) {
                    sb.append("Exile the top card of your library");
                    if (params.containsKey("ExileFaceDown")) {
                        sb.append(" face down");
                    }
                } else if (destination.equals("Ante")) {
                    sb.append("Add the top card of your library to the ante");
                }
                sb.append(".");
            } else if (origin.equals("Library")) {
                sb.append("Search your library for ").append(num).append(" ").append(type).append(" and ");
    
                if (num == 1) {
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
                    final int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params
                            .get("LibraryPosition")) : 0;
    
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
                // for the non-targeted SAs when you choose what is returned on
                // resolution
                sb.append("Return ").append(num).append(" ").append(type).append(" card(s) ");
                sb.append(" to your ").append(destination);
            }
        }
    
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    /**
     * <p>
     * changeKnownOriginStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeKnownOriginStackDescription(final Map<String,String> params, final SpellAbility sa) {
    
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(host.getName()).append(" -");
        }
    
        sb.append(" ");
    
        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final ZoneType origin = ZoneType.smartValueOf(params.get("Origin"));
    
        final StringBuilder sbTargets = new StringBuilder();
    
        ArrayList<Card> tgts;
        if (sa.getTarget() != null) {
            tgts = sa.getTarget().getTargetCards();
        } else {
            // otherwise add self to list and go from there
            tgts = new ArrayList<Card>();
            for (final Card c : sa.knownDetermineDefined(params.get("Defined"))) {
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
            if (params.containsKey("Tapped")) {
                sb.append(" tapped");
            }
            if (params.containsKey("GainControl")) {
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
            if (params.containsKey("Shuffle")) { // for things like Gaea's
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
                final int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params
                        .get("LibraryPosition")) : 0;
    
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
    
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    /**
     * <p>
     * changeZoneResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        String origin = "";
        if (params.containsKey("Origin")) {
            origin = params.get("Origin");
        }
    
        if (ZoneType.isHidden(origin, params.containsKey("Hidden")) && !params.containsKey("Ninjutsu")) {
            changeHiddenOriginResolve(params, sa);
        } else {
            //else if (isKnown(origin) || params.containsKey("Ninjutsu")) {
            // Why is this an elseif and not just an else?
            changeKnownOriginResolve(params, sa);
        }
    }

    /**
     * <p>
     * changeKnownOriginResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeKnownOriginResolve(final Map<String,String> params, final SpellAbility sa) {
        ArrayList<Card> tgtCards;
        ArrayList<SpellAbility> sas;
    
        final Target tgt = sa.getTarget();
        final Player player = sa.getActivatingPlayer();
        final Card hostCard = sa.getSourceCard();
    
        final ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        final List<ZoneType> origin = ZoneType.listValueOf(params.get("Origin"));
    
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = new ArrayList<Card>();
            for(ZoneType o : origin) {
                for (final Card c : sa.knownDetermineDefined(params.get("Defined"))) {
                    tgtCards.add(c);
                }
            }
        }
    
        // changing zones for spells on the stack
        if (tgt != null) {
            sas = tgt.getTargetSAs();
        } else {
            sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        for (final SpellAbility tgtSA : sas) {
            if (!tgtSA.isSpell()) { // Catch any abilities or triggers that slip through somehow
                continue;
            }
    
            final SpellAbilityStackInstance si = Singletons.getModel().getGame().getStack().getInstanceFromSpellAbility(tgtSA);
            if (si == null) {
                continue;
            }
    
            removeFromStack(tgtSA, sa, si);
        } // End of change from stack
    
        final String remember = params.get("RememberChanged");
        final String imprint = params.get("Imprint");
    
        if (params.containsKey("Unimprint")) {
            hostCard.clearImprinted();
        }
    
        if (params.containsKey("ForgetOtherRemembered")) {
            hostCard.clearRemembered();
        }
    
        boolean optional = params.containsKey("Optional");
    
        if (tgtCards.size() != 0) {
            for (final Card tgtC : tgtCards) {
                final StringBuilder sb = new StringBuilder();
                sb.append("Do you want to move " + tgtC + " from " + origin + " to " + destination + "?");
                if (player.isHuman() && optional
                        && !GameActionUtil.showYesNoDialog(hostCard, sb.toString())) {
                    continue;
                }
                final Zone originZone = Singletons.getModel().getGame().getZoneOf(tgtC);
    
                // if Target isn't in the expected Zone, continue
    
                if (originZone == null || !origin.contains(originZone.getZoneType())) {
                    continue;
                }
                
                Card movedCard = null;
    
                if (destination.equals(ZoneType.Library)) {
                    // library position is zero indexed
                    final int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params
                            .get("LibraryPosition")) : 0;
    
                    movedCard = Singletons.getModel().getGame().getAction().moveToLibrary(tgtC, libraryPosition);
    
                    // for things like Gaea's Blessing
                    if (params.containsKey("Shuffle")) {
                        tgtC.getOwner().shuffle();
                    }
                } else {
                    if (destination.equals(ZoneType.Battlefield)) {
                        if (params.containsKey("Tapped") || params.containsKey("Ninjutsu")) {
                            tgtC.setTapped(true);
                        }
                        if (params.containsKey("GainControl")) {
                            tgtC.addController(sa.getSourceCard());
                        }
                        if (params.containsKey("AttachedTo")) {
                            final ArrayList<Card> list = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                                    params.get("AttachedTo"), sa);
                            if (!list.isEmpty()) {
                                final Card attachedTo = list.get(0);
                                if (tgtC.isEnchanting()) {
                                    // If this Card is already Enchanting
                                    // something
                                    // Need to unenchant it, then clear out the
                                    // commands
                                    final GameEntity oldEnchanted = tgtC.getEnchanting();
                                    tgtC.removeEnchanting(oldEnchanted);
                                    tgtC.clearEnchantCommand();
                                    tgtC.clearUnEnchantCommand();
                                }
                                tgtC.enchantEntity(attachedTo);
                            }
                        }
                        // Auras without Candidates stay in their current
                        // location
                        if (tgtC.isAura()) {
                            final SpellAbility saAura = AttachEffect.getAttachSpellAbility(tgtC);
                            if (!saAura.getTarget().hasCandidates(saAura, false)) {
                                continue;
                            }
                        }
    
                        movedCard = Singletons.getModel().getGame().getAction()
                                .moveTo(tgtC.getController().getZone(destination), tgtC);
    
                        if (params.containsKey("Ninjutsu") || params.containsKey("Attacking")) {
                            Singletons.getModel().getGame().getCombat().addAttacker(tgtC);
                            Singletons.getModel().getGame().getCombat().addUnblockedAttacker(tgtC);
                        }
                        if (params.containsKey("Tapped") || params.containsKey("Ninjutsu")) {
                            tgtC.setTapped(true);
                        }
                    } else {
                        movedCard = Singletons.getModel().getGame().getAction().moveTo(destination, tgtC);
                        // If a card is Exiled from the stack, remove its spells from the stack
                        if (params.containsKey("Fizzle")) {
                            ArrayList<SpellAbility> spells = tgtC.getSpellAbilities();
                            for (SpellAbility spell : spells) {
                                if (tgtC.isInZone(ZoneType.Exile)) {
                                    final SpellAbilityStackInstance si = Singletons.getModel().getGame().getStack().getInstanceFromSpellAbility(spell);
                                    Singletons.getModel().getGame().getStack().remove(si);
                                }
                            }
                        }
                        if (params.containsKey("ExileFaceDown")) {
                            movedCard.setState(CardCharacteristicName.FaceDown);
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

    /**
     * <p>
     * changeHiddenOriginResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void changeHiddenOriginResolve(final Map<String,String> params, final SpellAbility sa) {
        ArrayList<Player> fetchers;
    
        if (params.containsKey("DefinedPlayer")) {
            fetchers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("DefinedPlayer"), sa);
        } else {
            fetchers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        // handle case when Defined is for a Card
        if (fetchers.isEmpty()) {
            fetchers.add(sa.getSourceCard().getController());
        }
    
        Player chooser = null;
        if (params.containsKey("Chooser")) {
            final String choose = params.get("Chooser");
            if (choose.equals("Targeted") && (sa.getTarget().getTargetPlayers() != null)) {
                chooser = sa.getTarget().getTargetPlayers().get(0);
            } else {
                chooser = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), choose, sa).get(0);
            }
        }
    
        for (final Player player : fetchers) {
            Player decider = chooser;
            if (decider == null) {
                decider = player;
            }
            if (decider.isComputer()) {
                ChangeZoneAi.hiddenOriginResolveAI(decider, params, sa, player);
            } else {
                changeHiddenOriginResolveHuman(params, sa, player);
            }
        }
    }

    /**
     * <p>
     * changeHiddenOriginResolveHuman.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    private static void changeHiddenOriginResolveHuman(final Map<String,String> params, final SpellAbility sa, Player player) {
        final Card card = sa.getSourceCard();
        final List<Card> movedCards = new ArrayList<Card>();
        final boolean defined = params.containsKey("Defined");
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            final ArrayList<Player> players = tgt.getTargetPlayers();
            player = players.get(0);
            if (players.contains(player) && !player.canBeTargetedBy(sa)) {
                return;
            }
        }
    
        List<ZoneType> origin = new ArrayList<ZoneType>();
        if (params.containsKey("Origin")) {
            origin = ZoneType.listValueOf(params.get("Origin"));
        }
        ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        // this needs to be zero indexed. Top = 0, Third = 2
        int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;
    
        if (params.containsKey("OriginChoice")) {
            // Currently only used for Mishra, but may be used by other things
            // Improve how this message reacts for other cards
            final List<ZoneType> alt = ZoneType.listValueOf(params.get("OriginAlternative"));
            List<Card> altFetchList = player.getCardsIn(alt);
            altFetchList = AbilityFactory.filterListByType(altFetchList, params.get("ChangeType"), sa);
    
            final StringBuilder sb = new StringBuilder();
            sb.append(params.get("AlternativeMessage")).append(" ");
            sb.append(altFetchList.size()).append(" cards match your searching type in Alternate Zones.");
    
            if (!GameActionUtil.showYesNoDialog(card, sb.toString())) {
                origin = alt;
            }
        }
    
        if (params.containsKey("DestinationAlternative")) {
    
            final StringBuilder sb = new StringBuilder();
            sb.append(params.get("AlternativeDestinationMessage"));
    
            if (!GameActionUtil.showYesNoDialog(card, sb.toString())) {
                destination = ZoneType.smartValueOf(params.get("DestinationAlternative"));
                libraryPos = params.containsKey("LibraryPositionAlternative") ? Integer.parseInt(params
                        .get("LibraryPositionAlternative")) : 0;
            }
        }
    
        int changeNum = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(card, params.get("ChangeNum"),
                sa) : 1;
    
        List<Card> fetchList;
        if (defined) {
            fetchList = new ArrayList<Card>(AbilityFactory.getDefinedCards(card, params.get("Defined"), sa));
            if (!params.containsKey("ChangeNum")) {
                changeNum = fetchList.size();
            }
        } else if (!origin.contains(ZoneType.Library) && !origin.contains(ZoneType.Hand)
                && !params.containsKey("DefinedPlayer")) {
            fetchList = Singletons.getModel().getGame().getCardsIn(origin);
        } else {
            fetchList = player.getCardsIn(origin);
        }
    
        if (!defined) {
            if (origin.contains(ZoneType.Library) && !defined && !params.containsKey("NoLooking")) { 
                // Look at whole library before moving onto choosing a card
                GuiChoose.oneOrNone(sa.getSourceCard().getName() + " - Looking at Library",
                        player.getCardsIn(ZoneType.Library));
            }
    
            // Look at opponents hand before moving onto choosing a card
            if (origin.contains(ZoneType.Hand) && player.isComputer()) {
                GuiChoose.oneOrNone(sa.getSourceCard().getName() + " - Looking at Opponent's Hand", player
                        .getCardsIn(ZoneType.Hand));
            }
            fetchList = AbilityFactory.filterListByType(fetchList, params.get("ChangeType"), sa);
        }
    
        final String remember = params.get("RememberChanged");
        final String forget = params.get("ForgetChanged");
        final String imprint = params.get("Imprint");
        final String selectPrompt = params.containsKey("SelectPrompt") ? params.get("SelectPrompt") : "Select a card";
    
        if (params.containsKey("Unimprint")) {
            card.clearImprinted();
        }
    
        for (int i = 0; i < changeNum; i++) {
            if (params.containsKey("DifferentNames")) {
                for (Card c : movedCards) {
                    fetchList = CardLists.filter(fetchList, Predicates.not(CardPredicates.nameEquals(c.getName())));
                }
            }
            if ((fetchList.size() == 0) || (destination == null)) {
                break;
            }
    
            Object o;
            if (params.containsKey("AtRandom")) {
                o = CardUtil.getRandom(fetchList);
            } else if (params.containsKey("Mandatory")) {
                o = GuiChoose.one(selectPrompt, fetchList);
            } else if (params.containsKey("Defined")) {
                o = fetchList.get(0);
            } else {
                o = GuiChoose.oneOrNone(selectPrompt, fetchList);
            }
    
            if (o != null) {
                final Card c = (Card) o;
                fetchList.remove(c);
                Card movedCard = null;
    
                if (destination.equals(ZoneType.Library)) {
                    // do not shuffle the library once we have placed a fetched
                    // card on top.
                    if (origin.contains(ZoneType.Library) && (i < 1) && "False".equals(params.get("Shuffle"))) {
                        player.shuffle();
                    }
                    movedCard = Singletons.getModel().getGame().getAction().moveToLibrary(c, libraryPos);
                } else if (destination.equals(ZoneType.Battlefield)) {
                    if (params.containsKey("Tapped")) {
                        c.setTapped(true);
                    }
                    if (params.containsKey("GainControl")) {
                        c.addController(sa.getSourceCard());
                    }
    
                    if (params.containsKey("AttachedTo")) {
                        final ArrayList<Card> list = AbilityFactory.getDefinedCards(sa.getSourceCard(),
                                params.get("AttachedTo"), sa);
                        if (!list.isEmpty()) {
                            final Card attachedTo = list.get(0);
                            if (c.isEnchanting()) {
                                // If this Card is already Enchanting something
                                // Need to unenchant it, then clear out the
                                // commands
                                final GameEntity oldEnchanted = c.getEnchanting();
                                oldEnchanted.removeEnchantedBy(c);
                                c.removeEnchanting(oldEnchanted);
                                c.clearEnchantCommand();
                                c.clearUnEnchantCommand();
                            }
                            c.enchantEntity(attachedTo);
                        }
                    }
    
                    if (params.containsKey("Attacking")) {
                        Singletons.getModel().getGame().getCombat().addAttacker(c);
                    }
    
                    movedCard = Singletons.getModel().getGame().getAction().moveTo(c.getController().getZone(destination), c);
                    if (params.containsKey("Tapped")) {
                        movedCard.setTapped(true);
                    }
                } else if (destination.equals(ZoneType.Exile)) {
                    movedCard = Singletons.getModel().getGame().getAction().exile(c);
                    if (params.containsKey("ExileFaceDown")) {
                        movedCard.setState(CardCharacteristicName.FaceDown);
                    }
                } else {
                    movedCard = Singletons.getModel().getGame().getAction().moveTo(destination, c);
                }
                movedCards.add(movedCard);
    
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
    
            } else {
                final StringBuilder sb = new StringBuilder();
                final int num = Math.min(fetchList.size(), changeNum - i);
                sb.append("Cancel Search? Up to ").append(num).append(" more cards can change zones.");
    
                if (((i + 1) == changeNum) || GameActionUtil.showYesNoDialog(card, sb.toString())) {
                    break;
                }
            }
        }
        if (params.containsKey("Reveal") && !movedCards.isEmpty()) {
            GuiChoose.one(card + " - Revealed card: ", movedCards.toArray());
        }
    
        if ((origin.contains(ZoneType.Library) && !destination.equals(ZoneType.Library) && !defined)
                || (params.containsKey("Shuffle") && "True".equals(params.get("Shuffle")))) {
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
     */
    private static void removeFromStack(final SpellAbility tgtSA, final SpellAbility srcSA, final SpellAbilityStackInstance si) {
        Singletons.getModel().getGame().getStack().remove(si);
    
        final AbilityFactory af = srcSA.getAbilityFactory();
        final Map<String, String> params = af.getMapParams();
    
        if (params.containsKey("Destination")) {
            if (tgtSA.isAbility()) {
                // Shouldn't be able to target Abilities but leaving this in for now
            } else if (tgtSA.isFlashBackAbility())  {
                Singletons.getModel().getGame().getAction().exile(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("Graveyard")) {
                Singletons.getModel().getGame().getAction().moveToGraveyard(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("Exile")) {
                Singletons.getModel().getGame().getAction().exile(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("TopOfLibrary")) {
                Singletons.getModel().getGame().getAction().moveToLibrary(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("Hand")) {
                Singletons.getModel().getGame().getAction().moveToHand(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("BottomOfLibrary")) {
                Singletons.getModel().getGame().getAction().moveToBottomOfLibrary(tgtSA.getSourceCard());
            } else if (params.get("Destination").equals("ShuffleIntoLibrary")) {
                Singletons.getModel().getGame().getAction().moveToBottomOfLibrary(tgtSA.getSourceCard());
                tgtSA.getSourceCard().getController().shuffle();
            } else {
                throw new IllegalArgumentException("AbilityFactory_ChangeZone: Invalid Destination argument for card "
                        + srcSA.getSourceCard().getName());
            }
    
            if (!tgtSA.isAbility()) {
                System.out.println("Moving spell to " + params.get("Destination"));
            }
        }
    }

}