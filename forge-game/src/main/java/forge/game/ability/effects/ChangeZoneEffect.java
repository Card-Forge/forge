package forge.game.ability.effects;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.card.CardType;
import forge.game.*;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.*;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.*;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class ChangeZoneEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.isHidden()) {
            return changeHiddenOriginStackDescription(sa);
        }
        return changeKnownOriginStackDescription(sa);
    }

    /**
     * <p>
     * changeHiddenOriginStackDescription.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeHiddenOriginStackDescription(final SpellAbility sa) {
        // TODO build Stack Description will need expansion as more cards are added

        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();

        if (sa.hasParam("Optional")) { // TODO make boolean and handle verb reconjugation throughout
            sb.append("(OPTIONAL) ");
        }

        // Player whose cards will change zones
        List<Player> fetchers = null;
        if (sa.hasParam("DefinedPlayer")) {
            fetchers = AbilityUtils.getDefinedPlayers(host, sa.getParam("DefinedPlayer"), sa);
        }
        if (fetchers == null && sa.usesTargeting()) {
            fetchers = Lists.newArrayList(sa.getTargets().getTargetPlayers());
        }
        if (fetchers == null) {
            fetchers = Lists.newArrayList(host.getController());
        }

        final String fetcherNames = Lang.joinHomogenous(fetchers, Player.Accessors.FN_GET_NAME);

        // Player who chooses the cards to move
        List<Player> choosers = Lists.newArrayList();
        if (sa.hasParam("Chooser")) {
            choosers = AbilityUtils.getDefinedPlayers(host, sa.getParam("Chooser"), sa);
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
            fetchPlayer = "their";
        }

        String origin = "";
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }
        final String destination = sa.getParam("Destination");

        String type = "card";
        boolean defined = false;
        if (sa.hasParam("ChangeTypeDesc")) {
            type = sa.getParam("ChangeTypeDesc");
            if (type.contains("{")) {
                final StringBuilder typesb = new StringBuilder();
                SpellAbilityEffect.tokenizeString(sa, typesb, type);
                type = typesb.toString();
            }
        } else if (sa.usesTargeting() || sa.hasParam("Defined")) {
            List<Card> tgts = getDefinedCardsOrTargeted(sa, "Defined");
            type = Lang.joinHomogenous(tgts);
            defined = true;
        } else if (sa.hasParam("ChangeType") && !sa.getParam("ChangeType").equals("Card")) {
            final String ct = sa.getParam("ChangeType");
            type = CardType.CoreType.isValidEnum(ct) ? ct.toLowerCase() : ct;
        }
        final String cardTag = type.contains("card") ? "" : " card";

        final int num = sa.hasParam("ChangeNum") ? AbilityUtils.calculateAmount(host,
                sa.getParam("ChangeNum"), sa) : 1;
        boolean tapped = sa.hasParam("Tapped");
        boolean attacking = sa.hasParam("Attacking");
        if (sa.isNinjutsu()) {
            tapped = true;
            attacking = true;
        }

        if (origin.equals("Library") && sa.hasParam("Defined")) {
            // for now, just handle the Exile from top of library case, but
            // this can be expanded...
            if (destination.equals("Exile")) {
                sb.append("Exile the top card of your library");
                if (sa.hasParam("ExileFaceDown")) {
                    sb.append(" face down");
                }
                sb.append(".");
            } else if (destination.equals("Ante")) {
                sb.append("Add the top card of your library to the ante.");
            }
        } else if (origin.equals("Library")) {
            final boolean originAlt = sa.hasParam("OriginAlternative");
            sb.append(chooserNames).append(" search").append(choosers.size() > 1 ? " " : "es ");
            sb.append(fetchPlayer).append(fetchPlayer.equals(chooserNames) ? "'s " : " ").append("library");
            if (originAlt) {
                sb.append(sa.getParam("OriginAlternative").contains("Hand") ? ", hand, and/or graveyard for " :
                                " and/or graveyard for ");
            } else {
                sb.append(" for ");
            }
            sb.append(Lang.nounWithNumeralExceptOne(num, type + cardTag)).append(", ");
            if (!sa.hasParam("NoReveal") && ZoneType.smartValueOf(destination) != null && ZoneType.smartValueOf(destination).isHidden()) {
                if (choosers.size() == 1) {
                    sb.append(num > 1 ? "reveals them, " : "reveals it, ");
                } else {
                    sb.append(num > 1 ? "reveal them, " : "reveal it, ");
                }
            }

            if (destination.equals("Exile")) {
                if (num == 1) {
                    sb.append("exiles it");
                } else {
                    sb.append("exiles them");
                }
            } else {
                if (num == 1) {
                    sb.append("puts it ");
                } else {
                    sb.append("puts them ");
                }

                if (destination.equals("Battlefield")) {
                    sb.append("onto the battlefield");
                    if (tapped) {
                        sb.append(" tapped").append(attacking ? " and" : "");
                    }
                    sb.append(attacking ? " attacking" : "");
                    if (sa.hasParam("GainControl")) {
                        sb.append(" under ").append(chooserNames).append("'s control");
                    }
                }
                if (destination.equals("Hand")) {
                    if (num == 1) {
                        sb.append("into their hand");
                    } else {
                        sb.append("into their owner's hand");
                    }
                }
                if (destination.equals("Graveyard")) {
                    if (num == 1) {
                        sb.append("into its owner's graveyard");
                    } else {
                        sb.append("into their owner's graveyard");
                    }
                }
            }
            sb.append(", then shuffles").append(originAlt ? " if they searched their library." : ".");
        } else if (origin.equals("Sideboard")) {
            sb.append(chooserNames);
            //currently Reveal is always True in ChangeZone
            if (sa.hasParam("Reveal")) {
                sb.append(" may reveal ").append(num).append(" ").append(type).append(" from outside the game and put ");
                if (num == 1) {
                    sb.append("it ");
                } else {
                    sb.append("them ");
                }
                sb.append("into their ").append(destination.toLowerCase()).append(".");
            } else {
                if (sa.hasParam("Mandatory")) {
                    sb.append(" puts ");
                } else {
                    sb.append(" may put ");
                }
                sb.append(num).append(" ").append(type).append(" from outside the game into their ");
                sb.append(destination.toLowerCase()).append(".");
            }
        } else if (origin.equals("Hand")) {
            sb.append(chooserNames);
            if (!chooserNames.equals(fetcherNames)) {
                sb.append(" looks at ").append(fetcherNames).append("'s hand and ");
                sb.append(destination.equals("Exile") ? "exiles " : "puts ");
                sb.append(num).append(" of those ").append(type).append(" card(s)");
            } else {
                sb.append(destination.equals("Exile") ? " exiles " : " puts ");
                if (defined) {
                    sb.append(type);
                } else if (StringUtils.containsIgnoreCase(type, "Card")) {
                    sb.append(Lang.nounWithNumeralExceptOne(num, type));
                } else {
                    sb.append(Lang.nounWithNumeralExceptOne(num, type + " card"));
                }
                sb.append(" from ").append(fetchPlayer).append(" hand");
            }

            if (destination.equals("Battlefield")) {
                sb.append(" onto the battlefield");
                if (tapped) {
                    sb.append(" tapped").append(attacking ? " and" : "");
                }
                sb.append(attacking ? " attacking" : "");
                if (sa.hasParam("GainControl")) {
                    sb.append(" under ").append(chooserNames).append("'s control");
                }
            }
            if (destination.equals("Library")) {
                final int libraryPos = sa.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(host, sa.getParam("LibraryPosition"), sa) : 0;

                if (libraryPos == 0) {
                    sb.append(" on top");
                }
                if (libraryPos == -1) {
                    sb.append(" on the bottom");
                }

                sb.append(" of ").append(fetchPlayer);
                if (!fetchPlayer.equals("their")) {
                    sb.append("'s");
                }
                sb.append(" library");
            }

            sb.append(".");
        } else if (origin.equals("Battlefield")) {
            // TODO Expand on this Description as more cards use it
            // for the non-targeted SAs when you choose what is returned on resolution
            sb.append("Return ").append(num).append(" ").append(type).append(" card(s) ");
            sb.append(" to your ").append(destination).append(".");
        } else if (origin.equals("Graveyard")) {
            // for non-targeted SAs when you choose what is moved on resolution
            // this will need expansion as more cards use it
            final boolean changeNumDesc = sa.hasParam("ChangeNumDesc");
            final boolean mandatory = sa.hasParam("Mandatory");
            String changed;
            if (changeNumDesc) {
                changed = sa.getParam("ChangeNumDesc") + " " + type + cardTag;
            } else if (!mandatory) {
                changed = Lang.nounWithNumeral(num, type + cardTag);
            } else {
                changed = Lang.nounWithNumeralExceptOne(num, type + cardTag);
            }
            final boolean toField = destination.equals("Battlefield");
            final boolean toHand = destination.equals("Hand");
            sb.append(chooserNames).append(" returns ").append(mandatory || changeNumDesc ? "" : "up to ");
            sb.append(changed);
            // so far, it seems non-targeted only lets you return from your own graveyard
            sb.append(" from their graveyard").append(choosers.size() > 1 ? "s" : "");
            sb.append(toField ? " to the " : toHand ? " to their " : " into their ").append(destination.toLowerCase());
            if (sa.hasParam("WithCountersType")) {
                final CounterType cType = CounterType.getType(sa.getParam("WithCountersType"));
                if (cType != null) {
                    sb.append(" with an additional ").append(cType.getName()).append(" counter on it");
                } else {
                    sb.append(" [ChangeZoneEffect WithCountersType error]");
                }
            }
            sb.append(".");
        } else if (origin.equals("Exile")) {
            // for non-targeted, moved cards are chosen on resolution – will need expansion as more cards use it
            sb.append(chooserNames).append(" puts ").append(Lang.nounWithNumeralExceptOne(num, type + cardTag));
            sb.append(" into their ").append(destination.toLowerCase()).append(".");
        }

        return sb.toString();
    }

    /**
     * <p>
     * changeKnownOriginStackDescription.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeKnownOriginStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();
        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        ZoneType origin = null;
        if (sa.hasParam("Origin")) {
            origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);
        }

        final StringBuilder sbTargets = new StringBuilder();
        Iterable<Card> tgts;
        if (sa.usesTargeting()) {
            tgts = getCardsfromTargets(sa);
        } else { // otherwise add self to list and go from there
            tgts = sa.knownDetermineDefined(sa.getParam("Defined"));
        }

        sbTargets.append(" ").append(sa.getParamOrDefault("DefinedDesc", Lang.joinHomogenous(tgts)));

        final String targetname = sbTargets.toString();

        final String pronoun = Iterables.size(tgts) > 1 ? " their " : " its ";

        final String fromGraveyard = " from the graveyard";

        if (destination.equals(ZoneType.Battlefield)) {
            final boolean attacking = sa.hasParam("Attacking");
            if (ZoneType.Graveyard.equals(origin)) {
                sb.append("Return").append(targetname).append(fromGraveyard).append(" to the battlefield");
            } else {
                sb.append("Put").append(targetname).append(" onto the battlefield");
            }
            if (sa.hasParam("Tapped")) {
                sb.append(" tapped").append(attacking ? " and" : "");
            }
            sb.append(attacking ? " attacking" : "");
            if (sa.hasParam("GainControl")) {
                sb.append(" under your control");
            }
            sb.append(".");
        }

        if (destination.equals(ZoneType.Hand)) {
            if (ZoneType.Graveyard.equals(origin)) {
                sb.append("Return").append(targetname).append(fromGraveyard).append(" to");
            } else if (ZoneType.Battlefield.equals(origin)) {
                sb.append("Return").append(targetname).append(" to");
            } else {
                sb.append("Put").append(targetname).append(" in");
            }
            sb.append(pronoun).append("owner's hand.");
        }

        if (destination.equals(ZoneType.Library)) {
            if (sa.hasParam("Shuffle")) { // for things like Gaea's Blessing
                sb.append("Shuffle").append(targetname);

                sb.append(" into").append(pronoun).append("owner's library.");
            } else {
                sb.append("Put").append(targetname);
                if (ZoneType.Graveyard.equals(origin)) {
                    sb.append(fromGraveyard);
                }

                // this needs to be zero indexed. Top = 0, Third = 2, -1 = Bottom
                final int libraryPosition = sa.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(host, sa.getParam("LibraryPosition"), sa) : 0;

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
            if (ZoneType.Graveyard.equals(origin)) {
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
            if (origin != null) {
                sb.append(" from ").append(origin);
            }
            sb.append(" into").append(pronoun).append("owner's graveyard.");
        }

        return sb.toString();
    }

    /**
     * <p>
     * changeZoneResolve.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */

    @Override
    public void resolve(SpellAbility sa) {
        //if host is not on the battlefield don't apply
        if ("UntilHostLeavesPlay".equals(sa.getParam("Duration")) && !sa.getHostCard().isInPlay()) {
            return;
        }

        if (sa.isHidden() && !sa.isNinjutsu()) {
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
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    private void changeKnownOriginResolve(final SpellAbility sa) {
        CardCollectionView tgtCards = getTargetCards(sa);
        final Player player = sa.getActivatingPlayer();
        final Card hostCard = sa.getHostCard();
        final Game game = player.getGame();
        final CardCollection commandCards = new CardCollection();

        ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final List<ZoneType> origin = Lists.newArrayList();
        if (sa.hasParam("Origin")) {
            origin.addAll(ZoneType.listValueOf(sa.getParam("Origin")));
        }

        boolean altDest = false;
        if (sa.hasParam("DestinationAlternative")) {
            final StringBuilder sb = new StringBuilder();
            sb.append(sa.getParam("AlternativeDestinationMessage"));
            Player alterDecider = player;
            if (sa.hasParam("AlternativeDecider")) {
                PlayerCollection deciders = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("AlternativeDecider"), sa);
                alterDecider = deciders.isEmpty() ? null : deciders.get(0);
            }
            if (alterDecider != null && !alterDecider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneToAltDestination, sb.toString(), null)) {
                destination = ZoneType.smartValueOf(sa.getParam("DestinationAlternative"));
                altDest = true;
            }
        }

        final CardZoneTable triggerList = new CardZoneTable();
        GameEntityCounterTable counterTable = new GameEntityCounterTable();
        // changing zones for spells on the stack
        for (final SpellAbility tgtSA : getTargetSpells(sa)) {
            if (!tgtSA.isSpell()) { // Catch any abilities or triggers that slip through somehow
                continue;
            }

            final SpellAbilityStackInstance si = game.getStack().getInstanceMatchingSpellAbilityID(tgtSA);
            if (si == null) {
                continue;
            }

            removeFromStack(tgtSA, sa, si, game, triggerList, counterTable);
        } // End of change from stack

        final String remember = sa.getParam("RememberChanged");
        final String forget = sa.getParam("ForgetChanged");
        final String imprint = sa.getParam("Imprint");

        if (sa.hasParam("Unimprint")) {
            hostCard.clearImprintedCards();
        }

        if (sa.hasParam("ForgetOtherRemembered")) {
            hostCard.clearRemembered();
        }

        final boolean optional = sa.hasParam("Optional");
        final boolean shuffle = sa.hasParam("Shuffle") && "True".equals(sa.getParam("Shuffle"));
        boolean combatChanged = false;

        Player chooser = player;
        if (sa.hasParam("Chooser")) {
            chooser = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("Chooser"), sa).get(0);
        }

        CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
        CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();

        // CR 401.4
        if (destination.equals(ZoneType.Library) && !shuffle && tgtCards.size() > 1) {
            if (sa.hasParam("RandomOrder")) {
                final CardCollection random = new CardCollection(tgtCards);
                CardLists.shuffle(random);
                tgtCards = random;
            } else if (sa.hasParam("Chooser")) {
                tgtCards = chooser.getController().orderMoveToZoneList(tgtCards, destination, sa);
            } else {
                tgtCards = GameActionUtil.orderCardsByTheirOwners(game, tgtCards, destination, sa);
            }
        }

        for (final Card tgtC : tgtCards) {
            final Card gameCard = game.getCardState(tgtC, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtC.equalsWithTimestamp(gameCard) || gameCard.isPhasedOut()) {
                continue;
            }

            if (sa.hasParam("RememberLKI")) {
                hostCard.addRemembered(CardUtil.getLKICopy(gameCard));
            }

            final String prompt = TextUtil.concatWithSpace(Localizer.getInstance().getMessage("lblDoYouWantMoveTargetFromOriToDest", CardTranslation.getTranslatedName(gameCard.getName()), Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME), destination.getTranslatedName()));
            if (optional && !chooser.getController().confirmAction(sa, null, prompt, null))
                continue;

            final Zone originZone = game.getZoneOf(gameCard);

            // if Target isn't in the expected Zone, continue
            if (originZone == null || (!origin.isEmpty() && !origin.contains(originZone.getZoneType()))) {
                continue;
            }

            Card movedCard = null;

            if (destination.equals(ZoneType.Library)) {
                // library position is zero indexed
                int libraryPosition = 0;
                if (altDest) {
                    libraryPosition = sa.hasParam("LibraryPositionAlternative") ? Integer.parseInt(sa.getParam("LibraryPositionAlternative")) : 0;
                } else {
                    libraryPosition = sa.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(hostCard, sa.getParam("LibraryPosition"), sa) : 0;
                }

                // If a card is moved to library from the stack, remove its spells from the stack
                if (sa.hasParam("Fizzle")) {
                    // TODO only AI still targets as card, try to remove it
                    if (gameCard.isInZone(ZoneType.Exile) || gameCard.isInZone(ZoneType.Hand) || gameCard.isInZone(ZoneType.Stack)) {
                        // This only fizzles spells, not anything else.
                        game.getStack().remove(gameCard);
                    }
                }

                movedCard = game.getAction().moveToLibrary(gameCard, libraryPosition, sa);
            } else if (destination.equals(ZoneType.Battlefield)) {
                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
                moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);
                moveParams.put(AbilityKey.SimultaneousETB, tgtCards);
                if (sa.isReplacementAbility()) {
                    ReplacementEffect re = sa.getReplacementEffect();
                    moveParams.put(AbilityKey.ReplacementEffect, re);
                    if (ReplacementType.Moved.equals(re.getMode()) && sa.getReplacingObject(AbilityKey.CardLKI) != null) {
                        moveParams.put(AbilityKey.CardLKI, sa.getReplacingObject(AbilityKey.CardLKI));
                    }
                }

                if (sa.hasParam("Tapped") || sa.isNinjutsu()) {
                    gameCard.setTapped(true);
                }
                if (sa.hasParam("Transformed")) {
                    if (gameCard.isTransformable()) {
                        // need LKI before Animate does apply
                        if (!moveParams.containsKey(AbilityKey.CardLKI)) {
                            moveParams.put(AbilityKey.CardLKI, CardUtil.getLKICopy(gameCard));
                        }
                        gameCard.changeCardState("Transform", null, sa);
                    } else {
                        // If it can't Transform, don't change zones.
                        continue;
                    }
                }
                if (sa.hasParam("WithCountersType")) {
                    CounterType cType = CounterType.getType(sa.getParam("WithCountersType"));
                    int cAmount = AbilityUtils.calculateAmount(hostCard, sa.getParamOrDefault("WithCountersAmount", "1"), sa);
                    gameCard.addEtbCounter(cType, cAmount, player);
                }
                if (sa.hasParam("GainControl")) {
                    final String g = sa.getParam("GainControl");
                    Player newController = g.equals("True") ? player :
                        AbilityUtils.getDefinedPlayers(hostCard, g, sa).get(0);
                    if (newController != null) {
                        if (newController != gameCard.getController()) {
                            gameCard.runChangeControllerCommands();
                        }
                        gameCard.setController(newController, game.getNextTimestamp());
                    }
                }
                if (sa.hasParam("AttachedTo")) {
                    CardCollection list = AbilityUtils.getDefinedCards(hostCard, sa.getParam("AttachedTo"), sa);
                    if (list.isEmpty()) {
                        list = CardLists.getValidCards(lastStateBattlefield, sa.getParam("AttachedTo"), hostCard.getController(), hostCard, sa);
                    }

                    // only valid choices are when they could be attached
                    // TODO for multiple Auras entering attached this way, need to use LKI info
                    if (!list.isEmpty()) {
                        list = CardLists.filter(list, CardPredicates.canBeAttached(gameCard, sa));
                    }
                    if (!list.isEmpty()) {
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("Attach", gameCard);
                        Card attachedTo = player.getController().chooseSingleEntityForEffect(list, sa, Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", gameCard.toString()), params);

                        // TODO can't attach later or moveToPlay would attach indirectly
                        // bypass canBeAttached to skip Protection checks when trying to attach multiple auras that would grant protection
                        gameCard.attachToEntity(game.getCardState(attachedTo), sa, true);
                    } else if (gameCard.isAura()) { // When it should enter the battlefield attached to an illegal permanent it fails
                        continue;
                    }
                }

                if (sa.hasParam("AttachedToPlayer")) {
                    FCollectionView<Player> list = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("AttachedToPlayer"), sa);
                    if (!list.isEmpty()) {
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("Attach", gameCard);
                        Player attachedTo = player.getController().chooseSingleEntityForEffect(list, sa, Localizer.getInstance().getMessage("lblSelectAPlayerAttachSourceTo", gameCard.toString()), params);
                        gameCard.attachToEntity(attachedTo, sa);
                    }
                    else { // When it should enter the battlefield attached to an illegal player it fails
                        continue;
                    }
                }

                if (sa.hasAdditionalAbility("AnimateSubAbility")) {
                    // need LKI before Animate does apply
                    if (!moveParams.containsKey(AbilityKey.CardLKI)) {
                        moveParams.put(AbilityKey.CardLKI, CardUtil.getLKICopy(gameCard));
                    }

                    final SpellAbility animate = sa.getAdditionalAbility("AnimateSubAbility");
                    hostCard.addRemembered(gameCard);
                    AbilityUtils.resolve(animate);
                    hostCard.removeRemembered(gameCard);
                    animate.setSVar("unanimateTimestamp", String.valueOf(game.getTimestamp()));
                }

                // need to be facedown before it hits the battlefield in case of Replacement Effects or Trigger
                if (sa.hasParam("FaceDown")) {
                    gameCard.turnFaceDown(true);
                    CardFactoryUtil.setFaceDownState(gameCard, sa);
                }

                movedCard = game.getAction().moveTo(gameCard.getController().getZone(destination), gameCard, sa, moveParams);
                // below stuff only if it changed zones
                if (movedCard.getZone().equals(originZone)) {
                    continue;
                }
                if (sa.hasParam("Unearth") && movedCard.isInPlay()) {
                    movedCard.setUnearthed(true);
                    movedCard.addChangedCardKeywords(Lists.newArrayList("Haste"), null, false,
                            game.getNextTimestamp(), 0, true);
                    registerDelayedTrigger(sa, "Exile", Lists.newArrayList(movedCard));
                    addLeaveBattlefieldReplacement(movedCard, sa, "Exile");
                }
                if (sa.hasParam("LeaveBattlefield")) {
                    addLeaveBattlefieldReplacement(movedCard, sa, sa.getParam("LeaveBattlefield"));
                }
                if (addToCombat(movedCard, movedCard.getController(), sa, "Attacking", "Blocking")) {
                    combatChanged = true;
                }
                if (sa.isNinjutsu()) {
                    // Ninjutsu need to get the Defender of the Returned Creature
                    final Card returned = sa.getPaidList("Returned", true).getFirst();
                    final GameEntity defender = game.getCombat().getDefenderByAttacker(returned);
                    game.getCombat().addAttacker(movedCard, defender);
                    game.getCombat().getBandOfAttacker(movedCard).setBlocked(false);
                    combatChanged = true;
                }

                if (sa.hasParam("AttachAfter") && movedCard.isAttachment()) {
                    CardCollection list = AbilityUtils.getDefinedCards(hostCard, sa.getParam("AttachAfter"), sa);
                    if (list.isEmpty()) {
                        list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("AttachAfter"), hostCard.getController(), hostCard, sa);
                    }
                    if (!list.isEmpty()) {
                        String title = Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", CardTranslation.getTranslatedName(gameCard.getName()));
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("Attach", gameCard);
                        Card attachedTo = chooser.getController().chooseSingleEntityForEffect(list, sa, title, params);
                        movedCard.attachToEntity(attachedTo, sa);
                    }
                }
            } else {
                // might set before card is moved only for nontoken
                if (destination.equals(ZoneType.Exile)) {
                    handleExiledWith(gameCard, sa);
                }

                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
                moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);
                movedCard = game.getAction().moveTo(destination, gameCard, sa, moveParams);

                if (ZoneType.Hand.equals(destination) && ZoneType.Command.equals(originZone.getZoneType())) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(movedCard.getName()).append(" has moved from Command Zone to ").append(player).append("'s hand.");
                    game.getGameLog().add(GameLogEntryType.ZONE_CHANGE, sb.toString());
                    commandCards.add(movedCard); //add to list to reveal the commandzone cards
                }

                // If a card is Exiled from the stack, remove its spells from the stack
                if (sa.hasParam("Fizzle")) {
                    if (gameCard.isInZone(ZoneType.Exile) || gameCard.isInZone(ZoneType.Hand)
                            || gameCard.isInZone(ZoneType.Stack) || gameCard.isInZone(ZoneType.Command)) {
                        // This only fizzles spells, not anything else.
                        game.getStack().remove(gameCard);
                    }
                }

                if (sa.hasParam("WithCountersType")) {
                    CounterType cType = CounterType.getType(sa.getParam("WithCountersType"));
                    int cAmount = AbilityUtils.calculateAmount(hostCard, sa.getParamOrDefault("WithCountersAmount", "1"), sa);
                    movedCard.addCounter(cType, cAmount, player, counterTable);
                }

                if (sa.hasParam("ExileFaceDown") || sa.hasParam("FaceDown")) {
                    movedCard.turnFaceDown(true);
                }
                if (sa.hasParam("Foretold")) {
                    movedCard.setForetold(true);
                    movedCard.setForetoldThisTurn(true);
                    if (sa.hasParam("ForetoldCost")) {
                        movedCard.setForetoldCostByEffect(true);
                    }
                    // look at the exiled card
                    movedCard.addMayLookTemp(player);
                }

                if (sa.hasParam("TrackDiscarded")) {
                    movedCard.setDiscarded(true);
                }
            }
            if (!movedCard.getZone().equals(originZone)) {
                Card meld = null;
                triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), movedCard);

                if (gameCard.getMeldedWith() != null) {
                    meld = game.getCardState(gameCard.getMeldedWith(), null);
                    if (meld != null) {
                        triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), meld);
                    }
                    if (sa.hasParam("WithCountersType")) {
                        CounterType cType = CounterType.getType(sa.getParam("WithCountersType"));
                        int cAmount = AbilityUtils.calculateAmount(hostCard, sa.getParamOrDefault("WithCountersAmount", "1"), sa);
                        meld.addCounter(cType, cAmount, player, counterTable);
                    }
                }
                if (gameCard.hasMergedCard()) {
                    for (final Card c : gameCard.getMergedCards()) {
                        if (c == gameCard) continue;
                        triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), c);
                        if (sa.hasParam("WithCountersType")) {
                            CounterType cType = CounterType.getType(sa.getParam("WithCountersType"));
                            int cAmount = AbilityUtils.calculateAmount(hostCard, sa.getParamOrDefault("WithCountersAmount", "1"), sa);
                            c.addCounter(cType, cAmount, player, counterTable);
                        }
                    }
                }

                if (sa.hasParam("RememberToEffectSource")) {
                    if (hostCard.isImmutable() && hostCard.getEffectSource() != null) {
                        hostCard.getEffectSource().addRemembered(movedCard);
                    }
                }
                if (remember != null) {
                    hostCard.addRemembered(movedCard);
                    // addRememberedFromCardState ?
                    if (meld != null) {
                        hostCard.addRemembered(meld);
                    }
                    if (gameCard.hasMergedCard()) {
                        for (final Card c : gameCard.getMergedCards()) {
                            if (c == gameCard) continue;
                            hostCard.addRemembered(c);
                        }
                    }
                }
                if (forget != null) {
                    hostCard.removeRemembered(movedCard);
                }
                if (imprint != null) {
                    hostCard.addImprintedCard(movedCard);
                    if (gameCard.hasMergedCard()) {
                        for (final Card c : gameCard.getMergedCards()) {
                            if (c == gameCard) continue;
                            hostCard.addImprintedCard(c);
                        }
                        // For Duplicant
                        if (sa.hasParam("ImprintLast")) {
                            Card lastCard = null;
                            for (final Card c : movedCard.getOwner().getCardsIn(destination)) {
                                if (hostCard.hasImprintedCard(c)) {
                                    hostCard.removeImprintedCard(c);
                                    lastCard = c;
                                }
                            }
                            hostCard.addImprintedCard(lastCard);
                        }
                    }
                }
            }
        }

        if (combatChanged) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }

        //reveal command cards that changes zone from command zone to player's hand
        if (!commandCards.isEmpty()) {
            game.getAction().reveal(commandCards, player, true, "Revealed cards in ");
        }

        triggerList.triggerChangesZoneAll(game, sa);
        counterTable.replaceCounterEffect(game, sa, true);

        if (sa.hasParam("AtEOT") && !triggerList.isEmpty()) {
            registerDelayedTrigger(sa, sa.getParam("AtEOT"), triggerList.allCards());
        }
        if ("UntilHostLeavesPlay".equals(sa.getParam("Duration"))) {
            addUntilCommand(sa, untilHostLeavesPlayCommand(triggerList, sa));
        }

        // might set after card is moved again if something has changed
        if (destination.equals(ZoneType.Exile)) {
            handleExiledWith(triggerList.allCards(), sa);
        }

        // for things like Gaea's Blessing
        if (destination.equals(ZoneType.Library) && shuffle) {
            PlayerCollection pl = new PlayerCollection();
            // use defined controller. it does need to work even without Targets.
            if (sa.hasParam("TargetsWithDefinedController")) {
                pl.addAll(AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("TargetsWithDefinedController"), sa));
            } else {
                for (final Card tgtC : tgtCards) {
                    // FCollection already does use set.
                    pl.add(tgtC.getOwner());
                }
            }

            for (final Player p : pl) {
                p.shuffle(sa);
            }
        }
    }

    /**
     * <p>
     * changeHiddenOriginResolve.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    private void changeHiddenOriginResolve(final SpellAbility sa) {
        List<Player> fetchers;

        if (sa.hasParam("DefinedPlayer")) {
            fetchers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("DefinedPlayer"), sa);
        } else {
            fetchers = Lists.newArrayList(sa.getActivatingPlayer());
        }

        Player chooser = null;
        if (sa.hasParam("Chooser")) {
            final String choose = sa.getParam("Chooser");
            if (choose.equals("Targeted") && sa.getTargets().isTargetingAnyPlayer()) {
                chooser = sa.getTargets().getFirstTargetedPlayer();
            } else {
                final FCollectionView<Player> choosers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Chooser"), sa);
                if (!choosers.isEmpty()) {
                    chooser = sa.getActivatingPlayer().getController().chooseSingleEntityForEffect(choosers, null, sa, Localizer.getInstance().getMessage("lblChooser") + ":", false, null, null);
                }
            }
        }

        changeZonePlayerInvariant(chooser, sa, fetchers);
    }

    private void changeZonePlayerInvariant(Player chooser, SpellAbility sa, List<Player> fetchers) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final boolean defined = sa.hasParam("Defined");
        final String changeType = sa.getParamOrDefault("ChangeType", "");
        boolean mandatory = sa.hasParam("Mandatory");
        Map<Player, HiddenOriginChoices> HiddenOriginChoicesMap = Maps.newHashMap();

        for (Player player : fetchers) {
            Player decider = chooser;
            if (decider == null) {
                decider = player;
            }

            if (sa.usesTargeting()) {
                final List<Player> players = Lists.newArrayList(sa.getTargets().getTargetPlayers());
                player = sa.hasParam("DefinedPlayer") ? player : players.get(0);
                if (players.contains(player) && !player.canBeTargetedBy(sa)) {
                    return;
                }
            }

            List<ZoneType> origin = Lists.newArrayList();
            if (sa.hasParam("Origin")) {
                origin = ZoneType.listValueOf(sa.getParam("Origin"));
            }
            ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));

            if (sa.hasParam("OriginAlternative")) {
                // Currently only used for Mishra, but may be used by other things
                // Improve how this message reacts for other cards
                final List<ZoneType> alt = ZoneType.listValueOf(sa.getParam("OriginAlternative"));
                CardCollectionView altFetchList = AbilityUtils.filterListByType(player.getCardsIn(alt), sa.getParam("ChangeType"), sa);

                final StringBuilder sb = new StringBuilder();
                sb.append(sa.getParam("AlternativeMessage")).append(" ");
                sb.append(altFetchList.size()).append(" " + Localizer.getInstance().getMessage("lblCardMatchSearchingTypeInAlternateZones"));

                if (!decider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneFromAltSource, sb.toString(), null)) {
                    origin.clear();
                }
                while (!alt.isEmpty() && origin.size() + alt.size() != 1) {
                    ZoneType z = alt.get(0);
                    String prompt = Localizer.getInstance().getMessage("lblSearchPlayerZoneConfirm", "{player's}", z.getTranslatedName().toLowerCase());
                    prompt = MessageUtil.formatMessage(prompt , decider, player);
                    if (decider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneFromAltSource, prompt, null)) {
                        origin.add(z);
                    }
                    alt.remove(0);
                }
                if (origin.isEmpty()) {
                    origin = alt;
                }
                for (ZoneType z : origin) {
                    // all cards that use this currently only search 1 card, no extra logic needed
                    if (z.isKnown() && Iterables.any(altFetchList, CardPredicates.inZone(z))) {
                        mandatory = true;
                    }
                }
            }

            // this needs to be zero indexed. Top = 0, Third = 2
            int libraryPos = sa.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(source, sa.getParam("LibraryPosition"), sa) : 0;

            if (sa.hasParam("DestinationAlternative")) {
                final StringBuilder sb = new StringBuilder();
                sb.append(sa.getParam("AlternativeDestinationMessage"));

                if (!decider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneToAltDestination, sb.toString(), null)) {
                    destination = ZoneType.smartValueOf(sa.getParam("DestinationAlternative"));
                    libraryPos = sa.hasParam("LibraryPositionAlternative") ? Integer.parseInt(sa.getParam("LibraryPositionAlternative")) : 0;
                }
            }

            int changeNum = sa.hasParam("ChangeNum") ? AbilityUtils.calculateAmount(source, sa.getParam("ChangeNum"), sa) : 1;

            if (sa.hasParam("Optional")) {
                String prompt;
                if (sa.hasParam("OptionalPrompt")) {
                    prompt = sa.getParam("OptionalPrompt");
                } else {
                    if (defined) {
                        prompt = Localizer.getInstance().getMessage("lblPutThatCardFromPlayerOriginToDestination", "{player's}", Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME).toLowerCase(), destination.getTranslatedName().toLowerCase());
                    } else {
                        prompt = Localizer.getInstance().getMessage("lblSearchPlayerZoneConfirm", "{player's}", Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME).toLowerCase());
                    }
                }
                String message = MessageUtil.formatMessage(prompt , decider, player);
                if (!decider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneGeneral, message, null)) {
                    return;
                }
            }

            // for Wish cards, if the player is controlled by someone else
            // they can't fetch from the outside the game/sideboard
            if (player.isControlled()) {
                origin.remove(ZoneType.Sideboard);
            }

            CardCollection fetchList;
            boolean shuffleMandatory = true;
            boolean searchedLibrary = false;
            if (defined) {
                fetchList = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
                if (!sa.hasParam("ChangeNum")) {
                    changeNum = fetchList.size();
                }
            }
            else if (!origin.contains(ZoneType.Library) && !origin.contains(ZoneType.Hand)
                    && !sa.hasParam("DefinedPlayer")) {
                fetchList = new CardCollection(player.getGame().getCardsIn(origin));
            }
            else {
                fetchList = new CardCollection(player.getCardsIn(origin));
                if (origin.contains(ZoneType.Library) && !sa.hasParam("NoLooking")) {
                    searchedLibrary = true;

                    if (decider.hasKeyword("LimitSearchLibrary")) { // Aven Mindcensor
                        fetchList.removeAll(player.getCardsIn(ZoneType.Library));
                        final int fetchNum = Math.min(player.getCardsIn(ZoneType.Library).size(), 4);
                        if (fetchNum == 0) {
                            searchedLibrary = false;
                        } else {
                            fetchList.addAll(player.getCardsIn(ZoneType.Library, fetchNum));
                        }
                    }
                    if (!decider.canSearchLibraryWith(sa, player)) {
                        fetchList.removeAll(player.getCardsIn(ZoneType.Library));
                        // "if you do/sb does, shuffle" is not mandatory (usually a triggered ability), should has this param.
                        // "then shuffle" is mandatory
                        shuffleMandatory = !sa.hasParam("ShuffleNonMandatory");
                        searchedLibrary = false;
                    }
                }
            }

            //determine list of all cards to reveal to player in addition to those that can be chosen
            DelayedReveal delayedReveal = null;
            if (!defined && !sa.hasParam("AlreadyRevealed")) {
                if (origin.contains(ZoneType.Library) && searchedLibrary) {
                    final int fetchNum = Math.min(player.getCardsIn(ZoneType.Library).size(), 4);
                    CardCollectionView shown = !decider.hasKeyword("LimitSearchLibrary") ? player.getCardsIn(ZoneType.Library) : player.getCardsIn(ZoneType.Library, fetchNum);
                    // Look at whole library before moving onto choosing a card
                    delayedReveal = new DelayedReveal(shown, ZoneType.Library, PlayerView.get(player), CardTranslation.getTranslatedName(source.getName()) + " - " + Localizer.getInstance().getMessage("lblLookingCardIn") + " ");
                }
                else if (origin.contains(ZoneType.Hand) && player.isOpponentOf(decider)) {
                    delayedReveal = new DelayedReveal(player.getCardsIn(ZoneType.Hand), ZoneType.Hand, PlayerView.get(player), CardTranslation.getTranslatedName(source.getName()) + " - " + Localizer.getInstance().getMessage("lblLookingCardIn") + " ");
                }
            }

            Long controlTimestamp = null;
            if (!searchedLibrary && sa.hasParam("Searched")) {
                searchedLibrary = true;
            }
            if (searchedLibrary) {
                if (decider.equals(player)) {
                    Map.Entry<Long, Player> searchControlPlayer = player.getControlledWhileSearching();
                    if (searchControlPlayer != null) {
                        controlTimestamp = searchControlPlayer.getKey();
                        player.addController(controlTimestamp, searchControlPlayer.getValue());
                    }

                    decider.incLibrarySearched();
                    // should only count the number of searching player's own library
                    // Panglacial Wurm
                    CardCollection canCastWhileSearching = CardLists.getKeyword(fetchList,
                            "While you're searching your library, you may cast CARDNAME from your library.");
                    decider.getController().tempShowCards(canCastWhileSearching);
                    for (final Card tgtCard : canCastWhileSearching) {
                        List<SpellAbility> sas = AbilityUtils.getBasicSpellsFromPlayEffect(tgtCard, decider);
                        if (sas.isEmpty()) {
                            continue;
                        }
                        SpellAbility tgtSA = decider.getController().getAbilityToPlay(tgtCard, sas);
                        if (!decider.getController().confirmAction(tgtSA, null, Localizer.getInstance().getMessage("lblDoYouWantPlayCard", CardTranslation.getTranslatedName(tgtCard.getName())), null)) {
                            continue;
                        }
                        tgtSA.setSVar("IsCastFromPlayEffect", "True");
                        // if played, that card cannot be found
                        if (decider.getController().playSaFromPlayEffect(tgtSA)) {
                            fetchList.remove(tgtCard);
                        }
                        //some kind of reset here?
                    }
                    decider.getController().endTempShowCards();
                }
                final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(decider);
                runParams.put(AbilityKey.Target, Lists.newArrayList(player));
                decider.getGame().getTriggerHandler().runTrigger(TriggerType.SearchedLibrary, runParams, false);
            }
            if (searchedLibrary && sa.hasParam("Searched")) {
                searchedLibrary = false;
            }

            if (!defined && !changeType.equals("") && !changeType.startsWith("EACH")) {
                fetchList = (CardCollection)AbilityUtils.filterListByType(fetchList, sa.getParam("ChangeType"), sa);
            }

            if (sa.hasParam("NoShuffle") || "False".equals(sa.getParam("Shuffle"))) {
                shuffleMandatory = false;
            }

            if (sa.hasParam("Unimprint")) {
                source.clearImprintedCards();
            }
            if (sa.hasParam("ForgetOtherRemembered")) {
                source.clearRemembered();
            }

            String selectPrompt = sa.hasParam("SelectPrompt") ? sa.getParam("SelectPrompt") : MessageUtil.formatMessage(Localizer.getInstance().getMessage("lblSelectCardFromPlayerZone", "{player's}", Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME).toLowerCase()), decider, player);
            final String totalcmc = sa.getParam("WithTotalCMC");
            final String totalpower = sa.getParam("WithTotalPower");
            int totcmc = AbilityUtils.calculateAmount(source, totalcmc, sa);
            int totpower = AbilityUtils.calculateAmount(source, totalpower, sa);

            fetchList.sort();

            CardCollection chosenCards = new CardCollection();
            if (changeType.startsWith("EACH")) {
                String[] eachTypes = changeType.substring(5).split(" & ");
                for (String thisType : eachTypes) {
                    for (int i = 0; i < changeNum && destination != null; i++) {
                        CardCollection thisList = (CardCollection) AbilityUtils.filterListByType(fetchList, thisType, sa);
                        if (!chosenCards.isEmpty()) {
                            thisList.removeAll(chosenCards);
                        }
                        Card c = decider.getController().chooseSingleCardForZoneChange(destination, origin, sa,
                                thisList, delayedReveal, selectPrompt, !mandatory, decider);
                        if (c == null) {
                            continue;
                        }
                        chosenCards.add(c);
                    }
                }
            } else if (changeNum > 1 && allowMultiSelect(decider, sa)) {
                // only multi-select if player can select more than one
                List<Card> selectedCards;
                if (!sa.hasParam("SelectPrompt")) {
                    // new default messaging for multi select
                    if (fetchList.size() > changeNum) {
                        //Select up to %changeNum cards from %players %origin
                        selectPrompt = MessageUtil.formatMessage(Localizer.getInstance().getMessage("lblSelectUpToNumCardFromPlayerZone", String.valueOf(changeNum), "{player's}", Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME).toLowerCase()), decider, player);
                    } else {
                        selectPrompt = MessageUtil.formatMessage(Localizer.getInstance().getMessage("lblSelectCardsFromPlayerZone", "{player's}", Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME).toLowerCase()), decider, player);
                    }
                }
                // ensure that selection is within maximum allowed changeNum
                do {
                    selectedCards = decider.getController().chooseCardsForZoneChange(destination, origin, sa, fetchList, 0, changeNum, delayedReveal, selectPrompt, decider);
                } while (selectedCards != null && selectedCards.size() > changeNum);
                if (selectedCards != null) {
                    chosenCards.addAll(selectedCards);
                }
                // maybe prompt the user if they selected fewer than the maximum possible?
            } else {
                // one at a time
                for (int i = 0; i < changeNum && destination != null; i++) {
                    if (sa.hasParam("DifferentNames")) {
                        for (Card c : chosenCards) {
                            fetchList = CardLists.filter(fetchList, Predicates.not(CardPredicates.sharesNameWith(c)));
                        }
                    }
                    if (sa.hasParam("DifferentCMC")) {
                        for (Card c : chosenCards) {
                            fetchList = CardLists.filter(fetchList, Predicates.not(CardPredicates.sharesCMCWith(c)));
                        }
                    }
                    if (sa.hasParam("DifferentPower")) {
                        for (Card c : chosenCards) {
                            fetchList = CardLists.filter(fetchList, Predicates.not(Predicates.compose(Predicates.equalTo(c.getNetPower()), CardPredicates.Accessors.fnGetNetPower)));
                        }
                    }
                    if (sa.hasParam("ShareLandType")) {
                        // After the first card is chosen, check if the land type is shared
                        for (final Card c : chosenCards) {
                            fetchList = CardLists.filter(fetchList, CardPredicates.sharesLandTypeWith(c));
                        }
                    }
                    if (totalcmc != null) {
                        if (totcmc >= 0) {
                            fetchList = CardLists.getValidCards(fetchList, "Card.cmcLE" + totcmc, source.getController(), source, sa);
                        }
                    }
                    if (totalpower != null) {
                        if (totpower >= 0) {
                            fetchList = CardLists.getValidCards(fetchList, "Card.powerLE" + totpower, source.getController(), source, sa);
                        }
                    }

                    // If we're choosing multiple cards, only need to show the reveal dialog the first time through.
                    boolean shouldReveal = (i == 0);
                    Card c = null;
                    if (sa.hasParam("AtRandom")) {
                        if (shouldReveal && delayedReveal != null) {
                            decider.getController().reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
                        }
                        c = Aggregates.random(fetchList);
                    } else if (defined && !sa.hasParam("ChooseFromDefined")) {
                        c = Iterables.getFirst(fetchList, null);
                    } else {
                        String title = selectPrompt;
                        if (changeNum > 1) { //indicate progress if multiple cards being chosen
                            title += " (" + (i + 1) + " / " + changeNum + ")";
                        }
                        c = decider.getController().chooseSingleCardForZoneChange(destination, origin, sa, fetchList, shouldReveal ? delayedReveal : null, title, !mandatory, decider);
                    }

                    if (c == null) {
                        final int num = Math.min(fetchList.size(), changeNum - i);
                        String message = Localizer.getInstance().getMessage("lblCancelSearchUpToSelectNumCards", String.valueOf(num));

                        if (fetchList.isEmpty() || sa.hasParam("SkipCancelPrompt") ||
                                decider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneGeneral, message, null)) {
                            break;
                        }
                        i--;
                        continue;
                    }

                    fetchList.remove(c);
                    if (delayedReveal != null) {
                        delayedReveal.remove(CardView.get(c));
                    }
                    chosenCards.add(c);

                    if (totalcmc != null) {
                        totcmc -= c.getCMC();
                    }
                    if (totalpower != null) {
                        totpower -= c.getCurrentPower();
                    }
                }
            }

            if (sa.hasParam("ShuffleChangedPile")) {
                CardLists.shuffle(chosenCards);
            }
            // do not shuffle the library once we have placed a fetched card on top.
            if (origin.contains(ZoneType.Library) && destination == ZoneType.Library && shuffleMandatory) {
                player.shuffle(sa);
            }

            if (sa.hasParam("Reorder")) {
                chosenCards = new CardCollection(decider.getController().orderMoveToZoneList(chosenCards, destination, sa));
            }

            // remove Controlled While Searching
            if (controlTimestamp != null) {
                player.removeController(controlTimestamp);
            }

            HiddenOriginChoices choices = new HiddenOriginChoices();
            choices.searchedLibrary = searchedLibrary;
            choices.shuffleMandatory = shuffleMandatory;
            choices.chosenCards = chosenCards;
            choices.libraryPos = libraryPos;
            choices.origin = origin;
            choices.destination = destination;
            HiddenOriginChoicesMap.put(player, choices);
        }

        final boolean remember = sa.hasParam("RememberChanged");
        final boolean forget = sa.hasParam("ForgetChanged");
        final boolean champion = sa.hasParam("Champion");
        final boolean imprint = sa.hasParam("Imprint");

        boolean combatChanged = false;
        final CardZoneTable triggerList = new CardZoneTable();

        CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
        CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();

        for (Player player : HiddenOriginChoicesMap.keySet()) {
            boolean searchedLibrary = HiddenOriginChoicesMap.get(player).searchedLibrary;
            boolean shuffleMandatory = HiddenOriginChoicesMap.get(player).shuffleMandatory;
            CardCollection chosenCards = HiddenOriginChoicesMap.get(player).chosenCards;
            int libraryPos = HiddenOriginChoicesMap.get(player).libraryPos;
            List<ZoneType> origin = HiddenOriginChoicesMap.get(player).origin;
            ZoneType destination = HiddenOriginChoicesMap.get(player).destination;
            CardCollection movedCards = new CardCollection();
            Player decider = ObjectUtils.firstNonNull(chooser, player);

            for (final Card c : chosenCards) {
                Card movedCard = null;
                final Zone originZone = game.getZoneOf(c);
                Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                moveParams.put(AbilityKey.FoundSearchingLibrary, searchedLibrary);
                moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
                moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);
                if (destination.equals(ZoneType.Library)) {
                    movedCard = game.getAction().moveToLibrary(c, libraryPos, sa, moveParams);
                }
                else if (destination.equals(ZoneType.Battlefield)) {
                    moveParams.put(AbilityKey.SimultaneousETB, chosenCards);
                    if (sa.hasParam("Tapped")) {
                        c.setTapped(true);
                    }
                    if (sa.hasAdditionalAbility("AnimateSubAbility")) {
                        // need LKI before Animate does apply
                        moveParams.put(AbilityKey.CardLKI, CardUtil.getLKICopy(c));

                        final SpellAbility animate = sa.getAdditionalAbility("AnimateSubAbility");
                        source.addRemembered(c);
                        AbilityUtils.resolve(animate);
                        source.removeRemembered(c);
                        animate.setSVar("unanimateTimestamp", String.valueOf(game.getTimestamp()));
                    }
                    if (sa.hasParam("GainControl")) {
                        final String g = sa.getParam("GainControl");
                        Player newController = g.equals("True") ? sa.getActivatingPlayer() :
                                AbilityUtils.getDefinedPlayers(source, g, sa).get(0);
                        if (newController != c.getController()) {
                            c.runChangeControllerCommands();
                        }
                        c.setController(newController, game.getNextTimestamp());
                    }

                    if (sa.hasParam("WithCountersType")) {
                        CounterType cType = CounterType.getType(sa.getParam("WithCountersType"));
                        int cAmount = AbilityUtils.calculateAmount(source, sa.getParamOrDefault("WithCountersAmount", "1"), sa);
                        c.addEtbCounter(cType, cAmount, player);
                    }
                    if (sa.hasParam("Transformed")) {
                        if (c.isTransformable()) {
                            // need LKI before Animate does apply
                            if (!moveParams.containsKey(AbilityKey.CardLKI)) {
                                moveParams.put(AbilityKey.CardLKI, CardUtil.getLKICopy(c));
                            }
                            c.changeCardState("Transform", null, sa);
                        } else {
                            // If it can't Transform, don't change zones.
                            continue;
                        }
                    }

                    if (sa.hasParam("AttachedTo") && c.isAttachment()) {
                        CardCollection list = AbilityUtils.getDefinedCards(source, sa.getParam("AttachedTo"), sa);
                        if (list.isEmpty()) {
                            list = CardLists.getValidCards(lastStateBattlefield, sa.getParam("AttachedTo"), source.getController(), source, sa);
                        }
                        // only valid choices are when they could be attached
                        // TODO for multiple Auras entering attached this way, need to use LKI info
                        if (!list.isEmpty()) {
                            list = CardLists.filter(list, CardPredicates.canBeAttached(c, sa));
                        }
                        if (!list.isEmpty()) {
                            String title = Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", CardTranslation.getTranslatedName(c.getName()));
                            Map<String, Object> params = Maps.newHashMap();
                            params.put("Attach", c);
                            Card attachedTo = decider.getController().chooseSingleEntityForEffect(list, sa, title, params);

                            // TODO can't attach later or moveToPlay would attach indirectly
                            // bypass canBeAttached to skip Protection checks when trying to attach multiple auras that would grant protection
                            c.attachToEntity(game.getCardState(attachedTo), sa, true);
                        }
                        else if (c.isAura()) { // When it should enter the battlefield attached to an illegal permanent it fails
                            continue;
                        }
                    }

                    if (sa.hasParam("AttachedToPlayer")) {
                        FCollectionView<Player> list = AbilityUtils.getDefinedPlayers(source, sa.getParam("AttachedToPlayer"), sa);
                        if (!list.isEmpty()) {
                            String title =  Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", CardTranslation.getTranslatedName(c.getName()));
                            Map<String, Object> params = Maps.newHashMap();
                            params.put("Attach", c);
                            Player attachedTo = player.getController().chooseSingleEntityForEffect(list, sa, title, params);
                            c.attachToEntity(attachedTo, sa);
                        }
                        else { // When it should enter the battlefield attached to an illegal permanent it fails
                            continue;
                        }
                    }

                    if (addToCombat(c, c.getController(), sa, "Attacking", "Blocking")) {
                        combatChanged = true;
                    }

                    // need to be facedown before it hits the battlefield in case of Replacement Effects or Trigger
                    if (sa.hasParam("FaceDown")) {
                        c.turnFaceDown(true);
                        CardFactoryUtil.setFaceDownState(c, sa);
                    }
                    movedCard = game.getAction().moveToPlay(c, c.getController(), sa, moveParams);

                    if (sa.hasParam("AttachAfter") && movedCard.isAttachment() && movedCard.isInPlay()) {
                        CardCollection list = AbilityUtils.getDefinedCards(source, sa.getParam("AttachAfter"), sa);
                        if (list.isEmpty()) {
                            list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("AttachAfter"), c.getController(), c, sa);
                        }
                        if (!list.isEmpty()) {
                            String title = Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", CardTranslation.getTranslatedName(c.getName()));
                            Map<String, Object> params = Maps.newHashMap();
                            params.put("Attach", movedCard);
                            Card attachedTo = decider.getController().chooseSingleEntityForEffect(list, sa, title, params);
                            movedCard.attachToEntity(attachedTo, sa);
                        }
                    }
                }
                else if (destination.equals(ZoneType.Exile)) {
                    movedCard = game.getAction().exile(c, sa, moveParams);

                    handleExiledWith(movedCard, sa);

                    if (sa.hasParam("ExileFaceDown")) {
                        movedCard.turnFaceDown(true);
                    }

                    if (sa.hasParam("Foretold")) {
                        movedCard.setForetold(true);
                        movedCard.setForetoldThisTurn(true);
                        if (sa.hasParam("ForetoldCost")) {
                            movedCard.setForetoldCostByEffect(true);
                        }
                        // look at the exiled card
                        movedCard.addMayLookTemp(sa.getActivatingPlayer());
                    }
                }
                else {
                    movedCard = game.getAction().moveTo(destination, c, 0, sa, moveParams);
                }

                movedCards.add(movedCard);

                if (originZone != null) {
                    triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), movedCard);

                    if (c.getMeldedWith() != null) {
                        Card meld = game.getCardState(c.getMeldedWith(), null);
                        if (meld != null) {
                            triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), meld);
                            if (destination.equals(ZoneType.Exile)) {
                                handleExiledWith(meld, sa);
                            }
                        }
                    }
                    if (c.hasMergedCard()) {
                        for (final Card card : c.getMergedCards()) {
                            if (card == c) continue;
                            triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), card);
                            if (destination.equals(ZoneType.Exile)) {
                                handleExiledWith(c, sa);
                            }
                        }
                    }
                }

                if (champion) {
                    final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(source);
                    runParams.put(AbilityKey.Championed, c);
                    game.getTriggerHandler().runTrigger(TriggerType.Championed, runParams, false);
                }

                if (remember) {
                    source.addRemembered(movedCard);
                    // addRememberedFromCardState ?
                    if (c.getMeldedWith() != null) {
                        Card meld = game.getCardState(c.getMeldedWith(), null);
                        if (meld != null) {
                            source.addRemembered(meld);
                        }
                    }
                    if (c.hasMergedCard()) {
                        for (final Card card : c.getMergedCards()) {
                            if (card == c) continue;
                            source.addRemembered(card);
                        }
                    }
                }
                if (sa.hasParam("RememberLKI")) {
                    source.addRemembered(CardUtil.getLKICopy(c));
                }
                if (forget) {
                    source.removeRemembered(movedCard);
                }
                // for imprinted since this doesn't use Target
                if (imprint) {
                    source.addImprintedCard(movedCard);
                    if (c.hasMergedCard()) {
                        for (final Card card : c.getMergedCards()) {
                            if (card == c) continue;
                            source.addImprintedCard(card);
                        }
                    }
                }
                if (ZoneType.Exile.equals(destination) && sa.hasParam("WithCountersType")) {
                    CounterType cType = CounterType.getType(sa.getParam("WithCountersType"));
                    int cAmount = AbilityUtils.calculateAmount(sa.getOriginalHost(), sa.getParamOrDefault("WithCountersAmount", "1"), sa);
                    GameEntityCounterTable table = new GameEntityCounterTable();
                    movedCard.addCounter(cType, cAmount, player, table);
                    table.replaceCounterEffect(game, sa, true);
                }
            }

            if (((!ZoneType.Battlefield.equals(destination) && !changeType.equals("") && !defined && !changeType.equals("Card"))
                    || (sa.hasParam("Reveal") && !movedCards.isEmpty())) && !sa.hasParam("NoReveal")) {
                game.getAction().reveal(movedCards, player);
            }

            if ((origin.contains(ZoneType.Library) && !destination.equals(ZoneType.Library) && !defined && shuffleMandatory)
                    || (sa.hasParam("Shuffle") && "True".equals(sa.getParam("Shuffle")))) {
                player.shuffle(sa);
            }

            if (sa.hasParam("AtEOT") && !movedCards.isEmpty()) {
                registerDelayedTrigger(sa, sa.getParam("AtEOT"), movedCards);
            }

        }
        if (combatChanged) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
        triggerList.triggerChangesZoneAll(game, sa);

        if ("UntilHostLeavesPlay".equals(sa.getParam("Duration"))) {
            addUntilCommand(sa, untilHostLeavesPlayCommand(triggerList, sa));
        }
    }

    private static class HiddenOriginChoices {
        boolean shuffleMandatory;
        boolean searchedLibrary;
        CardCollection chosenCards;
        int libraryPos;
        List<ZoneType> origin;
        ZoneType destination;
    }

    private static boolean allowMultiSelect(Player decider, SpellAbility sa) {
        return decider.getController().isGuiPlayer()        // limit mass selection to human players for now
                && !sa.hasParam("Mandatory")                // only handle optional decisions, for now
                && !sa.hasParam("ShareLandType")
                && !sa.hasParam("DifferentNames")
                && !sa.hasParam("DifferentPower")
                && !sa.hasParam("DifferentCMC")
                && !sa.hasParam("AtRandom")
                && (!sa.hasParam("Defined") || sa.hasParam("ChooseFromDefined"))
                && !sa.hasParam("WithTotalCMC")
                && !sa.hasParam("WithTotalPower");
    }

    /**
     * <p>
     * removeFromStack.
     * </p>
     *
     * @param tgtSA
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param srcSA
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param si
     *            a {@link forge.game.spellability.SpellAbilityStackInstance}
     *            object.
     * @param game
     */
    private void removeFromStack(final SpellAbility tgtSA, final SpellAbility srcSA, final SpellAbilityStackInstance si, final Game game, CardZoneTable triggerList, GameEntityCounterTable counterTable) {
        final Card tgtHost = tgtSA.getHostCard();
        final Zone originZone = tgtHost.getZone();
        game.getStack().remove(si);

        Map<AbilityKey,Object> params = AbilityKey.newMap();
        params.put(AbilityKey.StackSa, tgtSA);
        params.put(AbilityKey.StackSi, si);

        Card movedCard = null;
        if (srcSA.hasParam("Destination")) {
            final boolean remember = srcSA.hasParam("RememberChanged");
            final boolean imprint = srcSA.hasParam("Imprint");
            if (tgtSA.isAbility()) {
                // Shouldn't be able to target Abilities but leaving this in for now
            } else if (srcSA.getParam("Destination").equals("Graveyard")) {
                movedCard = game.getAction().moveToGraveyard(tgtHost, srcSA, params);
            } else if (srcSA.getParam("Destination").equals("Exile")) {
                movedCard = game.getAction().exile(tgtHost, srcSA, params);
                handleExiledWith(movedCard, srcSA);
            } else if (srcSA.getParam("Destination").equals("TopOfLibrary")) {
                movedCard = game.getAction().moveToLibrary(tgtHost, srcSA, params);
            } else if (srcSA.getParam("Destination").equals("Hand")) {
                movedCard = game.getAction().moveToHand(tgtHost, srcSA, params);
            } else if (srcSA.getParam("Destination").equals("BottomOfLibrary")) {
                movedCard = game.getAction().moveToBottomOfLibrary(tgtHost, srcSA, params);
            } else if (srcSA.getParam("Destination").equals("Library")) {
                final int libraryPos = srcSA.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(tgtHost, srcSA.getParam("LibraryPosition"), srcSA) : 0;
                movedCard = game.getAction().moveToLibrary(tgtHost, libraryPos, srcSA, params);
                if (srcSA.hasParam("Shuffle") && "True".equals(srcSA.getParam("Shuffle"))) {
                    tgtHost.getOwner().shuffle(srcSA);
                }
            } else {
                throw new IllegalArgumentException("AbilityFactory_ChangeZone: Invalid Destination argument for card "
                        + srcSA.getHostCard().getName());
            }

            if (srcSA.hasParam("WithCountersType")) {
                Player placer = srcSA.getActivatingPlayer();
                if (srcSA.hasParam("WithCountersPlacer")) {
                    placer = AbilityUtils.getDefinedPlayers(srcSA.getHostCard(), srcSA.getParam("WithCountersPlacer"), srcSA).get(0);
                }
                CounterType cType = CounterType.getType(srcSA.getParam("WithCountersType"));
                int cAmount = AbilityUtils.calculateAmount(srcSA.getHostCard(), srcSA.getParamOrDefault("WithCountersAmount", "1"), srcSA);
                movedCard.addCounter(cType, cAmount, placer, counterTable);
            }

            if (remember) {
                srcSA.getHostCard().addRemembered(tgtHost);
                // TODO or remember moved?
            }
            if (imprint) {
                srcSA.getHostCard().addImprintedCard(tgtHost);
            }

            if (!tgtSA.isAbility()) {
                System.out.println("Moving spell to " + srcSA.getParam("Destination"));
            }
            if (originZone != null && movedCard != null) {
                triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), movedCard);
            }
        }
    }
}
