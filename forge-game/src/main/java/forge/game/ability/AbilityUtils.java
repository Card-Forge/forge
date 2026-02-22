package forge.game.ability;

import com.google.common.base.Function;
import com.google.common.collect.*;
import com.google.common.math.IntMath;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.CardTypeView;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.*;
import forge.game.ability.AbilityFactory.AbilityRecordType;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.cost.CostAdjustment;
import forge.game.cost.IndividualCostPaymentInstance;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.KeywordWithCostAndType;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.*;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.*;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AbilityUtils {
    private final static ImmutableList<String> cmpList = ImmutableList.of("LT", "LE", "EQ", "GE", "GT", "NE");

    // should the three getDefined functions be merged into one? Or better to
    // have separate?
    // If we only have one, each function needs to Cast the Object to the
    // appropriate type when using
    // But then we only need update one function at a time once the casting is
    // everywhere.
    // Probably will move to One function solution sometime in the future
    public static CardCollection getDefinedCards(final Card hostCard, final String def, CardTraitBase sa) {
        CardCollection cards = new CardCollection();
        String changedDef = (def == null) ? "Self" : applyAbilityTextChangeEffects(def, sa); // default to Self
        final String[] incR = changedDef.split("\\.", 2);
        sa = adjustTriggerContext(incR, sa);
        String defined = incR[0];
        final Game game = hostCard.getGame();

        Card c = null;
        Player player = null;
        if (sa instanceof SpellAbility) {
            player = ((SpellAbility)sa).getActivatingPlayer();
        }
        if (player == null) {
            player = hostCard.getController();
        }

        if (defined.equals("Self")) {
            c = hostCard;
        } else if (defined.equals("CorrectedSelf")) {
            c = game.getCardState(hostCard);
        } else if (defined.equals("OriginalHost")) {
            if (sa instanceof SpellAbility) {
                c = ((SpellAbility)sa).getRootAbility().getOriginalHost();
            } else {
                c = sa.getOriginalHost();
            }
        } else if (defined.equals("EffectSource")) {
            if (hostCard.isImmutable()) {
                c = findEffectRoot(hostCard);
            }
        } else if (defined.equals("Equipped")) {
            c = hostCard.getEquipping();
        } else if (defined.startsWith("AttachedTo ")) {
            String v = defined.split(" ")[1];
            for (GameEntity ge : getDefinedEntities(hostCard, v, sa)) {
                for (Card att : ge.getAttachedCards()) {
                    // TODO handle phased out inside attachedCards
                    if (ge instanceof Card && ((Card) ge).isLKI()) {
                        att = game.getCardState(att);
                    }
                    cards.add(att);
                }
            }
        } else if (defined.startsWith("AttachedBy ")) {
            String v = defined.split(" ")[1];
            for (Card attachment : getDefinedCards(hostCard, v, sa)) {
                Card attached = attachment.getAttachedTo();
                if (attached != null) {
                    cards.add(attached);
                }
            }
        } else if (defined.equals("Enchanted")) {
            c = hostCard.getEnchantingCard();
        } else if (defined.equals("TopOfGraveyard")) {
            final CardCollectionView grave = player.getCardsIn(ZoneType.Graveyard);

            if (grave.size() > 0) {
                c = grave.getLast();
            } else {
                // we don't want this to fall through and return the "Self"
                return cards;
            }
        } else if (defined.endsWith("OfLibrary")) {
            final CardCollectionView lib = player.getCardsIn(ZoneType.Library);
            int libSize = lib.size();
            if (libSize > 0) { // TopOfLibrary or BottomOfLibrary
                if (defined.startsWith("TopThird")) {
                    int third = defined.contains("RoundedDown") ? (int) Math.floor(libSize / 3.0)
                            : (int) Math.ceil(libSize / 3.0);
                    cards = player.getTopXCardsFromLibrary(third);
                } else if (defined.startsWith("Top_")) {
                    String[] parts = defined.split("_");
                    cards = player.getTopXCardsFromLibrary(AbilityUtils.calculateAmount(hostCard, parts[1], sa));
                } else {
                    c = lib.get(defined.startsWith("Top") ? 0 : libSize - 1);
                }
            } else {
                // we don't want this to fall through and return the "Self"
                return cards;
            }
        } else if ((defined.equals("Targeted") || defined.equals("TargetedCard")) && sa instanceof SpellAbility) {
            for (TargetChoices tc : ((SpellAbility)sa).getAllTargetChoices()) {
                for (Card tgt : tc.getTargetCards()) {
                    cards.add(game.getChangeZoneLKIInfo(tgt));
                }
            }
        } else if (defined.equals("TargetedSource") && sa instanceof SpellAbility) {
            for (TargetChoices tc : ((SpellAbility)sa).getAllTargetChoices()) {
                for (SpellAbility s : tc.getTargetSpells()) {
                    cards.add(s.getHostCard());
                }
            }
        } else if (defined.equals("ThisTargetedCard") && sa instanceof SpellAbility) { // do not add parent targeted
            if (((SpellAbility)sa).getTargets() != null) {
                ((SpellAbility)sa).getTargets().getTargetCards().forEach(cards::add);
            }
        } else if (defined.equals("ParentTarget") && sa instanceof SpellAbility) {
            final SpellAbility parent = ((SpellAbility)sa).getParentTargetingCard();
            if (parent != null) {
                parent.getTargets().getTargetCards().forEach(cards::add);
            }
        }  else if (defined.startsWith("Triggered") && sa instanceof SpellAbility) {
            final SpellAbility root = ((SpellAbility)sa).getRootAbility();
            if (defined.contains("LKICopy")) { //Triggered*LKICopy
                int lkiPosition = defined.indexOf("LKICopy");
                AbilityKey type = AbilityKey.fromString(defined.substring(9, lkiPosition));
                final Object crd = root.getTriggeringObject(type);
                if (crd instanceof Card) {
                    c = (Card) crd;
                } else if (crd instanceof Iterable) {
                    cards.addAll(IterableUtil.filter((Iterable<?>) crd, Card.class));
                }
            }
            else if (defined.contains("HostCard")) { //Triggered*HostCard
                int hcPosition = defined.indexOf("HostCard");
                AbilityKey type = AbilityKey.fromString(defined.substring(9, hcPosition));
                final Object o = root.getTriggeringObject(type);
                if (o instanceof SpellAbility) {
                    c = ((SpellAbility) o).getHostCard();
                }
            } else {
                AbilityKey type = AbilityKey.fromString(defined.substring(9));
                final Object crd = root.getTriggeringObject(type);
                if (crd instanceof Card) {
                    c = game.getCardState((Card) crd);
                } else if (crd instanceof Iterable) {
                    for (Card gameCard : IterableUtil.filter((Iterable<?>) crd, Card.class)) {
                        if (gameCard.isLKI()) {
                            gameCard = game.getCardState(gameCard);
                        }
                        cards.add(gameCard);
                    }
                }
            }
        } else if (defined.startsWith("Replaced") && sa instanceof SpellAbility) {
            final SpellAbility root = ((SpellAbility)sa).getRootAbility();
            AbilityKey type = AbilityKey.fromString(defined.substring(8));
            final Object crd = root.getReplacingObject(type);

            if (crd instanceof Card) {
                c = (Card) crd;
            } else if (crd instanceof Iterable<?>) {
                cards.addAll(IterableUtil.filter((Iterable<?>) crd, Card.class));
            }
        } else if (defined.equals("Remembered") || defined.equals("RememberedCard")) {
            if (!hostCard.hasRemembered()) {
                final Card newCard = game.getCardState(hostCard);
                for (final Object o : newCard.getRemembered()) {
                    if (o instanceof Card) {
                        cards.add(game.getCardState((Card) o));
                    }
                }
            }
            // game.getCardState(Card c) is not working for LKI
            for (final Object o : hostCard.getRemembered()) {
                if (o instanceof Card) {
                    cards.addAll(addRememberedFromCardState(game, (Card)o));
                }
            }
        } else if (defined.equals("RememberedLKI")) {
            for (final Object o : hostCard.getRemembered()) {
                if (o instanceof Card) {
                    cards.add((Card) o);
                }
            }
        } else if (defined.equals("DirectRemembered")) {
            if (!hostCard.hasRemembered()) {
                final Card newCard = game.getCardState(hostCard);
                for (final Object o : newCard.getRemembered()) {
                    if (o instanceof Card) {
                        cards.add((Card) o);
                    }
                }
            }

            for (final Object o : hostCard.getRemembered()) {
                if (o instanceof Card) {
                    cards.add((Card) o);
                }
            }
        } else if (defined.equals("DelayTriggerRememberedLKI")) {
            for (Object o : sa.getTriggerRemembered()) {
                if (o instanceof Card) {
                    cards.add((Card)o);
                }
            }
        } else if (defined.equals("DelayTriggerRemembered")) {
            for (Object o : sa.getTriggerRemembered()) {
                if (o instanceof Card) {
                    cards.addAll(addRememberedFromCardState(game, (Card)o));
                }
            }
        } else if (defined.equals("RememberedFirst")) {
            Object o = hostCard.getFirstRemembered();
            if (o instanceof Card) {
                cards.add(game.getCardState((Card) o));
            }
        } else if (defined.equals("RememberedLast")) {
            Object o = Iterables.getLast(hostCard.getRemembered(), null);
            if (o instanceof Card) {
                cards.add(game.getCardState((Card) o));
            }
        } else if (defined.equals("ImprintedLKI")) {
            for (final Card imprint : hostCard.getImprintedCards()) {
                cards.add(imprint);
            }
        } else if (defined.equals("Imprinted")) {
            for (final Card imprint : hostCard.getImprintedCards()) {
                cards.add(game.getCardState(imprint));
            }
        } else if (defined.equals("ChosenCard")) {
            for (final Card chosen : hostCard.getChosenCards()) {
                cards.add(game.getCardState(chosen));
            }
        } else if (defined.startsWith("CardUID_")) {
            String idString = defined.substring(8);
            for (final Card cardByID : game.getCardsInGame()) {
                if (cardByID.getId() == Integer.parseInt(idString)) {
                    cards.add(game.getCardState(cardByID));
                }
            }
        } else if (defined.startsWith("Valid")) {
            Iterable<Card> candidates;
            String validDefined;
            if (defined.startsWith("Valid ")) {
                candidates = game.getCardsIn(ZoneType.Battlefield);
                validDefined = changedDef.substring("Valid ".length());
            } else if (defined.startsWith("ValidAll ")) {
                candidates = game.getCardsInGame();
                validDefined = changedDef.substring("ValidAll ".length());
            } else {
                String[] s = changedDef.split(" ", 2);
                String zone = s[0].substring("Valid".length());
                candidates = game.getCardsIn(ZoneType.smartValueOf(zone));
                validDefined = s[1];
            }
            cards.addAll(CardLists.getValidCards(candidates, validDefined, player, hostCard, sa));
            return cards;
        } else if (defined.startsWith("ExiledWith")) {
            cards.addAll(hostCard.getExiledCards());
        } else if (defined.equals("Convoked")) {
            cards.addAll(hostCard.getConvoked());
        } else {
            CardCollection list = getPaidCards(sa, incR[0]);
            if (list != null) {
                cards.addAll(list);
            }
        }

        if (c != null) {
            cards.add(c);
        }

        if (incR.length > 1 && !cards.isEmpty()) {
            String[] valids = incR[1].split(",");
            // need to add valids onto all of them
            for (int i = 0; i < valids.length; i++) {
                valids[i] = "Card." + valids[i];
            }
            cards = CardLists.getValidCards(cards, valids, player, hostCard, sa);
        }

        return cards;
    }

    private static CardCollection addRememberedFromCardState(Game game, Card c) {
        CardCollection coll = new CardCollection();
        Card newState = game.getCardState(c);
        if (c.getMeldedWith() != null) {
            // When remembering a card that flickers, also remember it's meld pair
            coll.add(game.getCardState(c.getMeldedWith()));
        }
        coll.add(newState);
        return coll;
    }

    private static Card findEffectRoot(Card startCard) {
        Card cc = startCard.getEffectSource();
        if (cc != null) {
            if (cc.isImmutable()) {
                return findEffectRoot(cc);
            }
            return cc;
        }
        return null; //If this happens there is a card in the game that is not in any zone
    }

    // Utility functions used by the AFs
    /**
     * <p>
     * calculateAmount.
     * </p>
     *
     * @param card
     *            a {@link forge.game.card.Card} object.
     * @param amount
     *            a {@link java.lang.String} object.
     * @param ability
     *            a {@link forge.game.CardTraitBase} object.
     * @return a int.
     */
    public static int calculateAmount(final Card card, String amount, final CardTraitBase ability) {
        return calculateAmount(card, amount, ability, false);
    }
    public static int calculateAmount(final Card card, String amount, CardTraitBase ability, boolean maxto) {
        // return empty strings and constants
        if (StringUtils.isBlank(amount)) { return 0; }
        if (card == null) { return 0; }

        Player player = null;
        if (ability instanceof SpellAbility) {
            player = ((SpellAbility)ability).getActivatingPlayer();
        }
        if (player == null) {
            player = card.getController();
        }

        final Game game = card.getGame();

        // Strip and save sign for calculations
        final boolean startsWithPlus = amount.charAt(0) == '+';
        final boolean startsWithMinus = amount.charAt(0) == '-';
        if (startsWithPlus || startsWithMinus) { amount = amount.substring(1); }
        int multiplier = startsWithMinus ? -1 : 1;

        // return result soon for plain numbers
        if (StringUtils.isNumeric(amount)) {
            int val = Integer.parseInt(amount);
            if (maxto) {
                val = Math.max(val, 0);
            }
            return val * multiplier;
        }

        // Try to fetch variable, try ability first, then card.
        String svarval = null;
        if (amount.indexOf('$') > 0) { // when there is a dollar sign, it's not a reference, it's a raw value!
            svarval = amount;
        }
        else if (ability != null) {
            svarval = ability.getSVar(amount);
        }
        if (StringUtils.isBlank(svarval)) {
            if ((ability != null) && (ability instanceof SpellAbility) && !(ability instanceof SpellPermanent)) {
                System.err.printf("SVar '%s' not found in ability, fallback to Card (%s). Ability is (%s)%n", amount, card.getName(), ability);
            }
            svarval = card.getSVar(amount);
        }

        if (StringUtils.isBlank(svarval)) {
            // cost hasn't been paid yet
            if (amount.startsWith("Cost")) {
                return 0;
            }
            // Nothing to do here if value is missing or blank
            System.err.printf("SVar '%s' not defined in Card (%s)%n", amount, card.getName());
            return 0;
        }

        // Handle numeric constant coming in svar value
        if (StringUtils.isNumeric(svarval)) {
            int val = Integer.parseInt(svarval);
            if (maxto) {
                val = Math.max(val, 0);
            }
            return val * multiplier;
        }

        // Parse Object$Property string
        final String[] calcX = svarval.split("\\$", 2);

        // Incorrect parses mean zero.
        if (calcX.length == 1 || calcX[1].equals("none")) {
            return 0;
        }

        // modify amount string for text changes
        calcX[1] = applyAbilityTextChangeEffects(calcX[1], ability);

        ability = adjustTriggerContext(calcX, ability);

        Integer val = null;
        if (calcX[0].startsWith("Count")) {
            val = xCount(card, calcX[1], ability);
        } else if (calcX[0].startsWith("Number")) {
            val = xCount(card, svarval, ability);
        } else if (calcX[0].startsWith("SVar")) {
            final String[] l = calcX[1].split("/");
            final String m = CardFactoryUtil.extractOperators(calcX[1]);
            val = doXMath(calculateAmount(card, l[0], ability), m, card, ability);
        } else if (calcX[0].startsWith("PlayerCount")) {
            final String hType = calcX[0].substring(11);
            final FCollection<Player> players = new FCollection<>();
            if (hType.equals("Players") || hType.isEmpty()) {
                players.addAll(game.getPlayers());
                val = playerXCount(players, calcX[1], card, ability);
            } else if (hType.equals("YourTeam")) {
                players.addAll(player.getYourTeam());
                val = playerXCount(players, calcX[1], card, ability);
            } else if (hType.equals("Opponents")) {
                players.addAll(player.getOpponents());
                val = playerXCount(players, calcX[1], card, ability);
            } else if (hType.equals("RegisteredOpponents")) {
                players.addAll(game.getRegisteredPlayers().filter(PlayerPredicates.isOpponentOf(player)));
                val = playerXCount(players, calcX[1], card, ability);
            } else if (hType.equals("Other")) {
                players.addAll(player.getAllOtherPlayers());
                val = playerXCount(players, calcX[1], card, ability);
            } else if (hType.startsWith("Remembered")) {
                addPlayer(card.getRemembered(), hType, players);
                val = playerXCount(players, calcX[1], card, ability);
            } else if (hType.equals("NonActive")) {
                players.addAll(game.getPlayers());
                players.remove(game.getPhaseHandler().getPlayerTurn());
                val = playerXCount(players, calcX[1], card, ability);
            } else if (hType.equals("HasLost")) {
                players.addAll(game.getLostPlayers());
                val = playerXCount(players, calcX[1], card, ability);
            } else if (hType.startsWith("PropertyYou")) {
                players.add(player);
                val = playerXCount(players, calcX[1], card, ability);
            } else if (hType.startsWith("Property")) {
                String defined = hType.split("Property")[1];
                for (Player p : game.getPlayersInTurnOrder()) {
                    if (p.hasProperty(defined, player, ability.getHostCard(), ability)) {
                        players.add(p);
                    }
                }
                val = playerXCount(players, calcX[1], card, ability);
            } else if (hType.startsWith("Defined")) {
                String defined = hType.split("Defined")[1];
                val = playerXCount(getDefinedPlayers(card, defined, ability), calcX[1], card, ability);
            } else {
                val = 0;
            }
        } else if (calcX[0].equals("OriginalHost")) {
            val = xCount(ability.getOriginalHost(), calcX[1], ability);
        } else if (calcX[0].equals("DungeonsCompleted")) {
            val = handlePaid(player.getCompletedDungeons(), calcX[1], card, ability);
        } else if (calcX[0].startsWith("ExiledWith")) {
            val = handlePaid(card.getExiledCards(), calcX[1], card, ability);
        } else if (calcX[0].startsWith("Convoked")) {
            val = handlePaid(card.getConvoked(), calcX[1], card, ability);
        } else if (calcX[0].startsWith("Emerged")) {
            val = handlePaid(card.getEmerged(), calcX[1], card, ability);
        } else if (calcX[0].startsWith("Crewed")) {
            val = handlePaid(card.getCrewedByThisTurn(), calcX[1], card, ability);
        } else if (calcX[0].startsWith("ChosenCard")) {
            val = handlePaid(card.getChosenCards(), calcX[1], card, ability);
        } else if (calcX[0].startsWith("Remembered")) {
            // Add whole Remembered list to handlePaid
            final CardCollection list = new CardCollection();
            Card newCard = card;
            if (!card.hasRemembered()) {
                newCard = game.getCardState(card);
            }

            if (calcX[0].endsWith("LKI")) { // last known information
                for (final Object o : newCard.getRemembered()) {
                    if (o instanceof Card) {
                        list.add((Card) o);
                    }
                }
            }
            else {
                for (final Object o : newCard.getRemembered()) {
                    if (o instanceof Card) {
                        list.add(game.getCardState((Card) o));
                    }
                }
            }

            val = handlePaid(list, calcX[1], card, ability);
        }
        else if (calcX[0].startsWith("Imprinted")) {
            // Add whole Imprinted list to handlePaid
            final CardCollection list = new CardCollection();
            Card newCard = card;
            if (card.getImprintedCards().isEmpty()) {
                newCard = game.getCardState(card);
            }

            if (calcX[0].endsWith("LKI")) { // last known information
                list.addAll(newCard.getImprintedCards());
            }
            else {
                for (final Card c : newCard.getImprintedCards()) {
                    list.add(game.getCardState(c));
                }
            }

            val = handlePaid(list, calcX[1], card, ability);
        }
        else if (calcX[0].matches("Enchanted") || calcX[0].matches("Equipped")) {
            // Add whole Enchanted list to handlePaid
            final CardCollection list = new CardCollection();
            if (card.isEnchanting()) {
                Object o = card.getEntityAttachedTo();
                if (o instanceof Card) {
                    list.add(game.getCardState((Card) o));
                }
            }
            val = handlePaid(list, calcX[1], card, ability);
        }

        // All the following only work for SpellAbilities
        else if (ability instanceof SpellAbility sa) {
            // Player attribute counting
            if (calcX[0].startsWith("TargetedPlayer")) {
                final List<Player> players = new ArrayList<>();
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (null != saTargeting) {
                    saTargeting.getTargets().getTargetPlayers().forEach(players::add);
                }
                val = playerXCount(players, calcX[1], card, ability);
            }
            else if (calcX[0].startsWith("ThisTargetedPlayer")) {
                final List<Player> players = new ArrayList<>();
                sa.getTargets().getTargetPlayers().forEach(players::add);
                val = playerXCount(players, calcX[1], card, ability);
            }
            else if (calcX[0].startsWith("TargetedObjects")) {
                List<GameObject> objects = new ArrayList<>();
                // Make list of all targeted objects starting with the root SpellAbility
                SpellAbility loopSA = sa.getRootAbility();
                while (loopSA != null) {
                    if (loopSA.usesTargeting()) {
                        objects.addAll(loopSA.getTargets());
                    }
                    loopSA = loopSA.getSubAbility();
                }
                if (calcX[0].endsWith("Distinct")) {
                    objects = new ArrayList<>(new HashSet<>(objects));
                }
                val = objectXCount(objects, calcX[1], card, ability);
            }
            else if (calcX[0].startsWith("TargetedController")) {
                final PlayerCollection players = new PlayerCollection();
                final CardCollection list = getDefinedCards(card, "Targeted", sa);
                final List<SpellAbility> sas = getDefinedSpellAbilities(card, "Targeted", sa);

                for (final Card c : list) {
                    players.add(c.getController());
                }
                for (final SpellAbility s : sas) {
                    players.add(s.getHostCard().getController());
                }
                val = playerXCount(players, calcX[1], card, ability);
            }
            else if (calcX[0].startsWith("TargetedByTarget")) {
                final CardCollection tgtList = new CardCollection();
                final List<SpellAbility> saList = getDefinedSpellAbilities(card, "Targeted", sa);

                for (final SpellAbility s : saList) {
                    tgtList.addAll(getDefinedCards(s.getHostCard(), "Targeted", s));
                }
                val = handlePaid(tgtList, calcX[1], card, ability);
            }
            else if (calcX[0].startsWith("TriggeredPlayers") || calcX[0].equals("TriggeredCardController")) {
                String key = calcX[0];
                if (calcX[0].startsWith("TriggeredPlayers")) {
                    key = "Triggered" + key.substring(16);
                }
                val = playerXCount(getDefinedPlayers(card, key, sa), calcX[1], card, ability);
            }
            else if (calcX[0].startsWith("TriggeredPlayer") || calcX[0].startsWith("TriggeredTarget")
                    || calcX[0].startsWith("TriggeredDefendingPlayer") || calcX[0].startsWith("TriggeredActivator")) {
                final SpellAbility root = sa.getRootAbility();
                Object o = root.getTriggeringObject(AbilityKey.fromString(calcX[0].substring(9)));
                val = o instanceof Player ? playerXProperty((Player) o, calcX[1], card, ability) : 0;
            }
            else if (calcX[0].equals("TriggeredSpellAbility") || calcX[0].equals("SpellTargeted")) {
                final SpellAbility sat = Iterables.getFirst(getDefinedSpellAbilities(card, calcX[0], sa), null);
                val = sat == null ? 0 : xCount(sat.getHostCard(), calcX[1], sat);
            }
            else if (calcX[0].startsWith("TriggerCount")) {
                // TriggerCount is similar to a regular Count, but just
                // pulls Integer Values from Trigger objects
                final SpellAbility root = sa.getRootAbility();
                final String[] l = calcX[1].split("/");
                final String m = CardFactoryUtil.extractOperators(calcX[1]);
                final Object to = root.getTriggeringObject(AbilityKey.fromString(l[0]));
                Integer count = null;
                if (to instanceof Iterable<?>) {
                    @SuppressWarnings("unchecked")
                    Iterable<Integer> numbers = (Iterable<Integer>) to;
                    if (calcX[0].endsWith("Max")) {
                        count = Aggregates.max(numbers);
                    } else {
                        count = Aggregates.sum(numbers);
                    }
                } else {
                    count = (Integer) to;
                }

                val = doXMath(Objects.requireNonNullElse(count, 0), m, card, ability);
            }
            else if (calcX[0].startsWith("ReplaceCount")) {
                // ReplaceCount is similar to a regular Count, but just
                // pulls Integer Values from Replacement objects
                final SpellAbility root = sa.getRootAbility();
                final String[] l = calcX[1].split("/");
                final String m = CardFactoryUtil.extractOperators(calcX[1]);
                final Integer count = (Integer) root.getReplacingObject(AbilityKey.fromString(l[0]));

                val = doXMath(Objects.requireNonNullElse(count, 0), m, card, ability);
            } else { // these ones only for handling lists
                Iterable<Card> list = null;
                if (calcX[0].startsWith("Targeted")) {
                    list = sa.findTargetedCards();
                }
                else if (calcX[0].startsWith("AllTargeted")) {
                    CardCollection all = new CardCollection();
                    SpellAbility loopSA = sa.getRootAbility();
                    while (loopSA != null) {
                        if (loopSA.usesTargeting()) {
                            all.addAll(loopSA.findTargetedCards());
                        }
                        loopSA = loopSA.getSubAbility();
                    }
                    list = all;
                }
                else if (calcX[0].startsWith("ParentTargeted")) {
                    SpellAbility parent = sa.getParentTargetingCard();
                    if (parent != null) {
                        list = parent.findTargetedCards();
                    }
                }
                else if (calcX[0].startsWith("TriggerRemembered")) {
                    list = IterableUtil.filter(sa.getTriggerRemembered(), Card.class);
                }
                else if (calcX[0].startsWith("TriggerObjects")) {
                    final SpellAbility root = sa.getRootAbility();
                    list = IterableUtil.filter((Iterable<?>) root.getTriggeringObjects().getOrDefault(
                            (AbilityKey.fromString(calcX[0].substring(14))), new CardCollection()), Card.class);
                }
                // CardTriggered<AbilityKey> used to bypass AbilityKeys that could also be Player above
                else if (calcX[0].startsWith("Triggered") || (calcX[0].startsWith("CardTriggered"))) {
                    final SpellAbility root = sa.getRootAbility();
                    final int s = calcX[0].startsWith("Triggered") ? 9 : 13;
                    list = new CardCollection((Card) root.getTriggeringObject(AbilityKey.fromString(calcX[0].substring(s))));
                }
                else if (calcX[0].startsWith("Replaced")) {
                    final SpellAbility root = sa.getRootAbility();
                    list = new CardCollection((Card) root.getReplacingObject(AbilityKey.fromString(calcX[0].substring(8))));
                }
                else {
                    list = getPaidCards(sa, calcX[0]);
                }
                if (list != null) {
                    // there could be null inside!
                    list = IterableUtil.filter(list, Card.class);
                    val = handlePaid(list, calcX[1], card, ability);
                }
            }
        }

        if (val != null) {
            if (maxto) {
                val = Math.max(val, 0);
            }
            return val * multiplier;
        }
        return 0;
    }

    /**
     * <p>
     * getDefinedObjects.
     * </p>
     *
     * @param card
     *            a {@link forge.game.card.Card} object.
     * @param def
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static FCollection<GameObject> getDefinedObjects(final Card card, final String def, final CardTraitBase sa) {
        final FCollection<GameObject> objects = new FCollection<>();
        final String defined = (def == null) ? "Self" : def;

        objects.addAll(getDefinedPlayers(card, defined, sa));
        objects.addAll(getDefinedCards(card, defined, sa));
        objects.addAll(getDefinedSpellAbilities(card, defined, sa));
        return objects;
    }

    public static FCollection<GameEntity> getDefinedEntities(final Card card, final String def, final CardTraitBase sa) {
        final FCollection<GameEntity> objects = new FCollection<>();
        final String defined = (def == null) ? "Self" : def;

        objects.addAll(getDefinedPlayers(card, defined, sa));
        objects.addAll(getDefinedCards(card, defined, sa));
        return objects;
    }

    public static List<GameEntity> getDefinedEntities(final Card card, final String[] def, final CardTraitBase sa) {
        final List<GameEntity> objects = new ArrayList<>();
        for (String d : def) {
            objects.addAll(getDefinedEntities(card, d, sa));
        }
        return objects;
    }

    /**
     * Filter list by type.
     *
     * @param list
     *            a CardList
     * @param type
     *            a card type
     * @param sa
     *            a SpellAbility
     * @return a {@link forge.game.card.CardCollectionView} object.
     */
    public static CardCollectionView filterListByType(final CardCollectionView list, String type, final SpellAbility sa) {
        if (type == null) {
            return list;
        }

        // Filter List Can send a different Source card in for things like
        // Mishra and Lobotomy

        Card source = sa.getHostCard();
        final Object o;
        if (type.startsWith("Triggered")) {
            if (type.contains("Card")) {
                o = sa.getTriggeringObject(AbilityKey.Card);
            }
            else if (type.contains("Object")) {
                o = sa.getTriggeringObject(AbilityKey.Object);
            }
            else if (type.contains("Attacker")) {
                o = sa.getTriggeringObject(AbilityKey.Attacker);
            }
            else if (type.contains("Blocker")) {
                o = sa.getTriggeringObject(AbilityKey.Blocker);
            }
            else {
                o = sa.getTriggeringObject(AbilityKey.Card);
            }

            if (!(o instanceof Card)) {
                return new CardCollection();
            }

            if (type.equals("Triggered") || type.equals("TriggeredCard") || type.equals("TriggeredObject")
                || type.equals("TriggeredAttacker") || type.equals("TriggeredBlocker")) {
                type = "Card.Self";
            }

            source = (Card) (o);
            if (type.contains("TriggeredCard")) {
                type = TextUtil.fastReplace(type, "TriggeredCard", "Card");
            }
            else if (type.contains("TriggeredObject")) {
                type = TextUtil.fastReplace(type, "TriggeredObject", "Card");
            }
            else if (type.contains("TriggeredAttacker")) {
                type = TextUtil.fastReplace(type, "TriggeredAttacker", "Card");
            }
            else if (type.contains("TriggeredBlocker")) {
                type = TextUtil.fastReplace(type, "TriggeredBlocker", "Card");
            }
            else {
                type = TextUtil.fastReplace(type, "Triggered", "Card");
            }
        }
        else if (type.startsWith("Targeted")) {
            source = null;
            CardCollectionView tgts = sa.findTargetedCards();
            if (!tgts.isEmpty()) {
                source = tgts.get(0);
            }
            if (source == null) {
                return new CardCollection();
            }

            if (type.startsWith("TargetedCard")) {
                type = TextUtil.fastReplace(type, "TargetedCard", "Card");
            }
            else {
                type = TextUtil.fastReplace(type, "Targeted", "Card");
            }
        }
        else if (type.startsWith("Remembered")) {
            boolean hasRememberedCard = false;
            for (final Object object : source.getRemembered()) {
                if (object instanceof Card) {
                    hasRememberedCard = true;
                    source = (Card) object;
                    type = TextUtil.fastReplace(type, "Remembered", "Card");

                    break;
                }
            }

            if (!hasRememberedCard) {
                return new CardCollection();
            }
        }
        else if (type.startsWith("Imprinted")) {
            type = TextUtil.fastReplace(type, "Imprinted", "Card");
        }
        else if (type.equals("Card.AttachedBy")) {
            source = source.getEnchantingCard();
            type = TextUtil.fastReplace(type, "Card.AttachedBy", "Card.Self");
        }

        String valid = type;

        for (String t : cmpList) {
            int index = valid.indexOf(t);
            if (index >= 0) {
                char reference = valid.charAt(index + 2); // take whatever goes after EQ
                if (Character.isLetter(reference)) {
                    String varName = valid.substring(index).split(",")[0].split(t)[1].split("\\+")[0];
                    if (!sa.getSVar(varName).isEmpty() || source.hasSVar(varName)) {
                        valid = TextUtil.fastReplace(valid, TextUtil.concatNoSpace(t, varName),
                                TextUtil.concatNoSpace(t, Integer.toString(calculateAmount(source, varName, sa))));
                    }
                }
            }
        }
        if (sa.hasParam("AbilityCount")) { // replace specific string other than "EQ" cases
            String var = sa.getParam("AbilityCount");
            valid = TextUtil.fastReplace(valid, var, Integer.toString(calculateAmount(source, var, sa)));
        }
        return CardLists.getValidCards(list, valid, sa.getActivatingPlayer(), source, sa);
    }

    /**
     * <p>
     * getDefinedPlayers.
     * </p>
     *
     * @param card
     *            a {@link forge.game.card.Card} object.
     * @param def
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    @SuppressWarnings("unchecked")
    public static PlayerCollection getDefinedPlayers(final Card card, final String def, CardTraitBase sa) {
        final PlayerCollection players = new PlayerCollection();
        final Player player = sa instanceof SpellAbility ? ((SpellAbility)sa).getActivatingPlayer() : card.getController();
        final Game game = card == null ? null : card.getGame();
        String changedDef = (def == null) ? "You" : applyAbilityTextChangeEffects(def, sa); // default to Self
        final String[] incR = changedDef.split("\\.", 2);
        sa = adjustTriggerContext(incR, sa);
        String defined = incR[0];

        if (defined.equals("Self") || defined.equals("TargetedCard") || defined.equals("ThisTargetedCard")
                || defined.equals("Convoked")
                || defined.startsWith("Valid") || getPaidCards(sa, incR[0]) != null || defined.equals("TargetedSource")
                || defined.startsWith("CardUID_")) {
            // defined syntax indicates cards only, so don't include any players
        } else if (defined.equals("TargetedOrController")) {
            players.addAll(getDefinedPlayers(card, "Targeted", sa));
            players.addAll(getDefinedPlayers(card, "TargetedController", sa));
        } else if ((defined.equals("Targeted") || defined.equals("TargetedPlayer")) && sa instanceof SpellAbility) {
            for (TargetChoices tc : ((SpellAbility)sa).getAllTargetChoices()) {
                players.addAll(tc.getTargetPlayers());
            }
        } else if (defined.startsWith("PlayerUID_")) {
            int id = Integer.parseInt(defined.split("PlayerUID_")[1]);
            for (Player p : game.getRegisteredPlayers()) {
                if (p.getId() == id) {
                    players.add(p);
                }
            }
        } else if (defined.equals("ParentTarget") && sa instanceof SpellAbility) {
            final SpellAbility parent = ((SpellAbility)sa).getParentTargetingPlayer();
            if (parent != null) {
                players.addAll(parent.getTargets().getTargetPlayers());
            }
        } else if (defined.equals("ThisTargetedPlayer") && sa instanceof SpellAbility) { // do not add parent targeted
            if (((SpellAbility)sa).getTargets() != null) {
                ((SpellAbility)sa).getTargets().getTargetPlayers().forEach(players::add);
            }
        } else if (defined.equals("TargetedController")) {
            for (final Card c : getDefinedCards(card, "Targeted", sa)) {
                players.add(c.getController());
            }
            for (final SpellAbility s : getDefinedSpellAbilities(card, "Targeted", sa)) {
                players.add(s.getActivatingPlayer());
            }
        } else if (defined.equals("TargetedOwner")) {
            for (final Card c : getDefinedCards(card, "Targeted", sa)) {
                players.add(c.getOwner());
            }
            for (final SpellAbility s : getDefinedSpellAbilities(card, "Targeted", sa)) {
                players.add(s.getHostCard().getOwner());
            }
        } else if (defined.equals("TargetedAndYou") && sa instanceof SpellAbility) {
            final SpellAbility saTargeting = ((SpellAbility)sa).getSATargetingPlayer();
            if (saTargeting != null) {
                players.addAll(saTargeting.getTargets().getTargetPlayers());
                players.add(((SpellAbility)sa).getActivatingPlayer());
            }
        } else if (defined.equals("ThisTargetedController")) {
            for (final Card c : getDefinedCards(card, "ThisTargetedCard", sa)) {
                players.add(c.getController());
            }
            for (final SpellAbility s : getDefinedSpellAbilities(card, "ThisTargeted", sa)) {
                players.add(s.getActivatingPlayer());
            }
        } else if (defined.equals("ThisTargetedOwner")) {
            for (final Card c : getDefinedCards(card, "ThisTargetedCard", sa)) {
                players.add(c.getOwner());
            }
        } else if (defined.equals("ParentTargetedController")) {
            for (final Card c : getDefinedCards(card, "ParentTarget", sa)) {
                players.add(c.getController());
            }
            for (final SpellAbility s : getDefinedSpellAbilities(card, "Targeted", sa)) {
                players.add(s.getActivatingPlayer());
            }
        } else if (defined.startsWith("Remembered")) {
            addPlayer(card.getRemembered(), defined, players);
        } else if (defined.startsWith("Imprinted")) {
            addPlayer(card.getImprintedCards(), defined, players);
        } else if (defined.startsWith("EffectSource")) {
            Card root = findEffectRoot(card);
            if (root != null) {
                addPlayer(Lists.newArrayList(root), defined, players);
            }
        } else if (defined.startsWith("OriginalHost")) {
            Card originalHost = sa.getOriginalHost();
            if (originalHost != null) {
                addPlayer(Lists.newArrayList(originalHost), defined, players);
            }
        } else if (defined.startsWith("DelayTriggerRemembered") && sa instanceof SpellAbility) {
            addPlayer(sa.getTriggerRemembered(), defined, players);
        } else if (defined.startsWith("Triggered") && sa instanceof SpellAbility) {
            String defParsed = defined.endsWith("AndYou") ? defined.substring(0, defined.indexOf("AndYou")) : defined;
            if (defined.endsWith("AndYou")) {
                players.add(((SpellAbility)sa).getActivatingPlayer());
            }
            final SpellAbility root = ((SpellAbility)sa).getRootAbility();
            Object o = null;
            if (defParsed.endsWith("Controller")) {
                final boolean orCont = defParsed.endsWith("OrController") || defParsed.endsWith("OriginalController");
                String triggeringType = defParsed.substring(9);
                if (!triggeringType.equals("OriginalController")) { //certain triggering objects we don't want to trim
                    triggeringType = triggeringType.substring(0, triggeringType.length() - (orCont ? 12 : 10));
                }
                final Object c = root.getTriggeringObject(AbilityKey.fromString(triggeringType));
                if (orCont && c instanceof Player) {
                    o = c;
                } else if (c instanceof Card) {
                    o = ((Card) c).getController();
                } else if (c instanceof SpellAbility) {
                    o = ((SpellAbility) c).getActivatingPlayer();
                } else if (c instanceof Iterable<?>) { // For merged permanent
                    if (orCont) {
                        addPlayer(IterableUtil.filter((Iterable<Object>)c, Player.class), "", players);
                    }
                    addPlayer(IterableUtil.filter((Iterable<Object>)c, Card.class), "Controller", players);
                }
            } else if (defParsed.endsWith("Opponent")) {
                String triggeringType = defParsed.substring(9);
                triggeringType = triggeringType.substring(0, triggeringType.length() - 8);
                final Object c = root.getTriggeringObject(AbilityKey.fromString(triggeringType));
                if (c instanceof Card) {
                    o = ((Card) c).getController().getOpponents();
                }
                if (c instanceof SpellAbility) {
                    o = ((SpellAbility) c).getActivatingPlayer().getOpponents();
                }
                // For merged permanent
                if (c instanceof CardCollection) {
                    o = ((CardCollection) c).get(0).getController().getOpponents();
                }
            } else if (defParsed.endsWith("Owner")) {
                String triggeringType = defParsed.substring(9);
                triggeringType = triggeringType.substring(0, triggeringType.length() - 5);
                final Object c = root.getTriggeringObject(AbilityKey.fromString(triggeringType));
                if (c instanceof Card) {
                    o = ((Card) c).getOwner();
                }
                // For merged permanent
                if (c instanceof CardCollection) {
                    o = ((CardCollection) c).get(0).getOwner();
                }
            }
            else {
                String triggeringType = defParsed.substring(9);
                o = root.getTriggeringObject(AbilityKey.fromString(triggeringType));
            }
            if (o != null) {
                if (o instanceof Player) {
                    players.add((Player) o);
                }
                if (o instanceof Iterable) {
                    players.addAll(IterableUtil.filter((Iterable<?>)o, Player.class));
                }
            }
        } else if (defined.startsWith("OppNon")) {
            players.addAll(player.getOpponents());
            players.removeAll(getDefinedPlayers(card, defined.substring(6), sa));
        } else if (defined.startsWith("Replaced") && sa instanceof SpellAbility) {
            final SpellAbility root = ((SpellAbility)sa).getRootAbility();
            Object o = null;
            if (defined.endsWith("Controller")) {
                String replacingType = defined.substring(8);
                replacingType = replacingType.substring(0, replacingType.length() - 10);
                final Object c = root.getReplacingObject(AbilityKey.fromString(replacingType));
                if (c instanceof Card) {
                    o = ((Card) c).getController();
                }
                if (c instanceof SpellAbility) {
                    o = ((SpellAbility) c).getHostCard().getController();
                }
            } else if (defined.endsWith("Owner")) {
                String replacingType = defined.substring(8);
                replacingType = replacingType.substring(0, replacingType.length() - 5);
                final Object c = root.getReplacingObject(AbilityKey.fromString(replacingType));
                if (c instanceof Card) {
                    o = ((Card) c).getOwner();
                }
            } else {
                final String replacingType = defined.substring(8);
                o = root.getReplacingObject(AbilityKey.fromString(replacingType));
            }
            if (o instanceof Player) {
                players.add((Player) o);
            }
        } else if (defined.startsWith("Non")) {
            players.addAll(game.getPlayersInTurnOrder());
            players.removeAll(getDefinedPlayers(card, defined.substring(3), sa));
        } else if (defined.equals("Registered")) {
            players.addAll(game.getRegisteredPlayers());
        } else if (defined.equals("EnchantedPlayer")) {
            final Object o = sa.getHostCard().getEntityAttachedTo();
            if (o instanceof Player) {
                players.add((Player) o);
            }
        } else if (defined.startsWith("Enchanted")) {
            if (card.isAttachedToEntity()) {
                addPlayer(Lists.newArrayList(card.getEntityAttachedTo()), defined, players);
            }
        } else if (defined.startsWith("Equipped")) {
            if (card.isEquipping()) {
                addPlayer(Lists.newArrayList(card.getEquipping()), defined, players);
            }
        } else if (defined.equals("AttackingPlayer")) {
            if (game.getPhaseHandler().inCombat()) {
                players.add(game.getCombat().getAttackingPlayer());
            }
        } else if (defined.equals("DefendingPlayer")) {
            players.add(game.getCombat().getDefendingPlayerRelatedTo(card));
        } else if (defined.equals("ChoosingPlayer")) {
            players.add(((SpellAbility) sa).getRootAbility().getChoosingPlayer());
        } else if (defined.equals("ChosenPlayer")) {
            final Player p = card.getChosenPlayer();
            if (p != null) {
                players.add(p);
            }
        } else if (defined.equals("Promised")) {
            final Player p = card.getPromisedGift();
            if (p != null) {
                players.add(p);
            }
        } else if (defined.startsWith("ChosenCard")) {
            addPlayer(card.getChosenCards(), defined, players);
        } else if (defined.equals("SourceController")) {
            players.add(sa.getHostCard().getController());
        } else if (defined.equals("CardController")) {
            players.add(card.getController());
        } else if (defined.equals("CardOwner")) {
            players.add(card.getOwner());
        } else if (defined.startsWith("PlayerNamed_")) {
            for (Player p : game.getPlayersInTurnOrder()) {
                if (p.getName().equals(defined.substring(12))) {
                    players.add(p);
                }
            }
        } else if (defined.startsWith("Flipped")) {
            for (Player p : game.getPlayersInTurnOrder()) {
                if (null != sa.getHostCard().getFlipResult(p)) {
                    if (sa.getHostCard().getFlipResult(p).equals(defined.substring(7))) {
                        players.add(p);
                    }
                }
            }
        } else if (defined.equals("Caster")) {
            if (sa.getHostCard().wasCast()) {
                players.add((sa.getHostCard().getCastSA().getActivatingPlayer()));
            }
        } else if (defined.equals("Exiler")) {
            players.add(card.getExiledBy());
        } else if (defined.equals("ActivePlayer")) {
            players.add(game.getPhaseHandler().getPlayerTurn());
        } else if (defined.equals("You")) {
            players.add(player);
        } else if (defined.equals("Opponent")) {
            players.addAll(player.getOpponents());
        } else if (defined.startsWith("NextPlayerToYour")) {
            Direction dir = defined.substring(16).equals("Left") ? Direction.Left : Direction.Right;
            players.add(game.getNextPlayerAfter(player, dir));
        } else if (defined.startsWith("NextOpponentToYour")) {
            Direction dir = defined.substring(18).equals("Left") ? Direction.Left : Direction.Right;
            Player next = game.getNextPlayerAfter(player, dir);
            while (!next.isOpponentOf(player)) {
                next = game.getNextPlayerAfter(next, dir);
            }
            players.add(next);
        } else {
            // will be filtered below
            players.addAll(game.getPlayersInTurnOrder());
        }

        if (incR.length > 1 && !players.isEmpty()) {
            String[] valids = incR[1].split(",");
            // need to add valids onto all of them
            for (int i = 0; i < valids.length; i++) {
                valids[i] = "Player." + valids[i];
            }
            return players.filter(PlayerPredicates.restriction(valids, player, card, sa));
        }
        return players;
    }

    /**
     * <p>
     * getDefinedSpellAbilities.
     * </p>
     *
     * @param card
     *            a {@link forge.game.card.Card} object.
     * @param def
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static FCollection<SpellAbility> getDefinedSpellAbilities(final Card card, final String def, CardTraitBase sa) {
        final FCollection<SpellAbility> sas = new FCollection<>();
        final String changedDef = (def == null) ? "Self" : applyAbilityTextChangeEffects(def, sa); // default to Self
        final Player player = sa instanceof SpellAbility ? ((SpellAbility)sa).getActivatingPlayer() : card.getController();
        final Game game = card.getGame();
        final String[] incR = changedDef.split("\\.", 2);
        sa = adjustTriggerContext(incR, sa);
        String defined = incR[0];

        SpellAbility s = null;

        // TODO - this probably needs to be fleshed out a bit, but the basics work
        if (defined.equals("Self") && sa instanceof SpellAbility) {
            s = (SpellAbility)sa;
        } else if (defined.equals("Parent") && sa instanceof SpellAbility) {
            s = ((SpellAbility)sa).getRootAbility();
        } else if (defined.equals("Remembered")) {
            for (final Object o : card.getRemembered()) {
                if (o instanceof Card) {
                    final Card rem = (Card) o;
                    sas.addAll(game.getCardState(rem).getSpellAbilities());
                } else if (o instanceof SpellAbility) {
                    sas.add((SpellAbility) o);
                }
            }
        } else if (defined.equals("Imprinted")) {
            for (final Card imp : card.getImprintedCards()) {
                sas.addAll(imp.getSpellAbilities());
            }
        } else if (defined.equals("EffectSource")) {
            if (card.getEffectSourceAbility() != null) {
                sas.add(card.getEffectSourceAbility().getRootAbility());
            }
        } else if (defined.equals("SourceFirstSpell")) {
            SpellAbility spell = game.getStack().getSpellMatchingHost(card);
            if (spell != null) {
                sas.add(spell);
            }
        } else if (defined.startsWith("Triggered") && sa instanceof SpellAbility) {
            final SpellAbility root = ((SpellAbility)sa).getRootAbility();

            final String triggeringType = defined.substring(9);
            final Object o = root.getTriggeringObject(AbilityKey.fromString(triggeringType));
            if (o instanceof SpellAbility) {
                s = (SpellAbility) o;
            }
        } else if (defined.endsWith("Targeted") && sa instanceof SpellAbility) {
            final List<TargetChoices> targets = defined.startsWith("This") ? Arrays.asList(((SpellAbility)sa).getTargets()) : ((SpellAbility)sa).getAllTargetChoices();
            for (TargetChoices tc : targets) {
                for (SpellAbility targetSpell : tc.getTargetSpells()) {
                    SpellAbilityStackInstance stackInstance = game.getStack().getInstanceMatchingSpellAbilityID(targetSpell);
                    if (stackInstance != null) {
                        SpellAbility instanceSA = stackInstance.getSpellAbility();
                        if (instanceSA != null) {
                            sas.add(instanceSA);
                        }
                    } else {
                        sas.add(targetSpell);
                    }
                }
            }
        } else if (defined.startsWith("ValidStack")) {
            String[] valid = changedDef.split(" ", 2)[1].split(",");
            for (SpellAbilityStackInstance stackInstance : game.getStack()) {
                SpellAbility instanceSA = stackInstance.getSpellAbility();
                if (instanceSA != null && instanceSA.isValid(valid, player, card, sa)) {
                    sas.add(instanceSA);
                }
            }
        }

        if (s != null) {
            sas.add(s);
        }

        return sas;
    }


    /////////////////////////////////////////////////////////////////////////////////////
    //
    // BELOW ARE resolve() METHOD AND ITS DEPENDANTS, CONSIDER MOVING TO DEDICATED CLASS
    //
    /////////////////////////////////////////////////////////////////////////////////////
    public static void resolve(final SpellAbility sa) {
        if (sa == null) {
            return;
        }

        Player pl = sa.getActivatingPlayer();
        final Game game = pl.getGame();

        if (sa.isTrigger() && !sa.getTrigger().isStatic() && sa.getParent() == null) {
            // when trigger cost are paid before the effect does resolve, need to clean the trigger
            game.getTriggerHandler().resetActiveTriggers();
        }

        resolvePreAbilities(sa, game);

        // count times ability resolves this turn
        if (!sa.isWrapper() && sa.isAbility()) {
            final Card host = sa.getHostCard();
            if (host != null) {
                host.addAbilityResolved(sa);
            }
        }

        final ApiType api = sa.getApi();
        if (api == null) {
            sa.resolve();
            if (sa.getSubAbility() != null) {
                resolve(sa.getSubAbility());
            }
            return;
        }
        resolveApiAbility(sa, game);
    }

    private static void resolvePreAbilities(final SpellAbility sa, final Game game) {
        Player controller = sa.getActivatingPlayer();
        Card source = sa.getHostCard();

        if (!sa.isSpell() || source.isPermanent()) {
            return;
        }

        // do blessing there before condition checks
        if (source.hasKeyword(Keyword.ASCEND) && controller.getZone(ZoneType.Battlefield).size() >= 10) {
            controller.setBlessing(true, source.getSetCode());
        }

        if (source.hasKeyword(Keyword.GIFT) && sa.isGiftPromised()) {
            game.getAction().checkStaticAbilities();
            // Is AdditionalAbility available from anything here?
            AbilitySub giftAbility = (AbilitySub) sa.getAdditionalAbility("GiftAbility");
            if (giftAbility != null) {
                giftAbility.setActivatingPlayer(controller);
                resolveApiAbility(giftAbility, game);
            }
        }
    }

    private static void resolveSubAbilities(final SpellAbility sa, final Game game) {
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub == null || sa.isWrapper()) {
            return;
        }

        // Needed - Equip an untapped creature with Sword of the Paruns then cast Deadshot on it. Should deal 2 more damage.
        game.getAction().checkStaticAbilities(); // this will refresh continuous abilities for players and permanents.
        if (sa.isReplacementAbility()) {
            // register all LTB trigger from last state battlefield
            for (Card lki : sa.getRootAbility().getLastStateBattlefield()) {
                game.getTriggerHandler().registerActiveLTBTrigger(lki);
            }
            game.getTriggerHandler().collectTriggerForWaiting();
        } else {
            game.getTriggerHandler().resetActiveTriggers();
        }
        resolveApiAbility(abSub, game);
    }

    private static void resolveApiAbility(final SpellAbility sa, final Game game) {
        final Card card = sa.getHostCard();

        String msg = "AbilityUtils:resolveApiAbility: try to resolve API ability";
        Breadcrumb bread = new Breadcrumb(msg);
        bread.setData("Api", sa.getApi().toString());
        bread.setData("Card", card.getName());
        bread.setData("SA", sa.toString());
        Sentry.addBreadcrumb(bread);

        if (!sa.isWrapper() && sa.isKeyword(Keyword.GIFT)) {
            game.getTriggerHandler().runTrigger(TriggerType.GiveGift, AbilityKey.mapFromPlayer(sa.getActivatingPlayer()), false);
        }

        // check conditions
        if (sa.metConditions()) {
            if (sa.isWrapper() || StringUtils.isBlank(sa.getParam("UnlessCost"))) {
                sa.resolve();
            } else {
                handleUnlessCost(sa, game);
                return;
            }
        }
        resolveSubAbilities(sa, game);
    }

    private static void handleUnlessCost(final SpellAbility sa, final Game game) {
        final Card source = sa.getHostCard();

        // The player who has the chance to cancel the ability
        final String pays = sa.getParamOrDefault("UnlessPayer", "TargetedController");
        final FCollectionView<Player> allPayers = getDefinedPlayers(source, pays, sa);
        final String  resolveSubs = sa.getParam("UnlessResolveSubs"); // no value means 'Always'
        final boolean execSubsWhenPaid = "WhenPaid".equals(resolveSubs) || StringUtils.isBlank(resolveSubs);
        final boolean execSubsWhenNotPaid = "WhenNotPaid".equals(resolveSubs) || StringUtils.isBlank(resolveSubs);
        final boolean isSwitched = sa.hasParam("UnlessSwitched");

        String unlessCost = sa.getParam("UnlessCost").trim();
        Cost cost = calculateUnlessCost(sa, unlessCost, true);
        if (cost == null) {
            sa.resolve();
            resolveSubAbilities(sa, game);
            return;
        }

        boolean alreadyPaid = false;
        for (Player payer : allPayers) {
            if (!payer.isInGame()) {
                // CR 800.4f
                continue;
            }
            if (unlessCost.equals("LifeTotalHalfUp")) {
                String halfup = Integer.toString(Math.max(0,(int) Math.ceil(payer.getLife() / 2.0)));
                cost = new Cost("PayLife<" + halfup + ">", true);
            }
            alreadyPaid |= payer.getController().payCostToPreventEffect(cost, sa, alreadyPaid, allPayers);
        }

        if (alreadyPaid == isSwitched) {
            sa.resolve();
        }

        if (alreadyPaid && execSubsWhenPaid || !alreadyPaid && execSubsWhenNotPaid) { // switched refers only to main ability!
            resolveSubAbilities(sa, game);
        }
    }

    public static Cost calculateUnlessCost(SpellAbility sa, String unlessCost, boolean beforePayment) {
        final Card source = sa.getHostCard();
        Cost cost;
        if (unlessCost.equals("ChosenNumber")) {
            cost = new Cost(new ManaCost(String.valueOf(source.getChosenNumber())), true);
        }
        else if (unlessCost.startsWith("DefinedCost")) {
            CardCollection definedCards = getDefinedCards(source, unlessCost.split("_")[1], sa);
            if (definedCards.isEmpty()) {
                return null;
            }
            Card card = definedCards.getFirst();
            ManaCostBeingPaid newCost = new ManaCostBeingPaid(card.getManaCost());
            // Check if there's a third underscore for cost modifying
            if (unlessCost.split("_").length == 3) {
                String modifier = unlessCost.split("_")[2];
                if (modifier.startsWith("Minus")) {
                    int max = Integer.parseInt(modifier.substring(5));
                    if (sa.hasParam("UnlessUpTo") && beforePayment) { // Flash
                        max = sa.getActivatingPlayer().getController().chooseNumberForCostReduction(sa, 0, max);
                    }
                    newCost.decreaseGenericMana(max);
                } else {
                    newCost.increaseGenericMana(Integer.parseInt(modifier.substring(4)));
                }
            }
            cost = new Cost(newCost.toManaCost(), true);
        }
        else if (unlessCost.startsWith("DefinedSACost")) {
            FCollection<SpellAbility> definedSAs = getDefinedSpellAbilities(source, unlessCost.split("_")[1], sa);
            if (definedSAs.isEmpty()) {
                return null;
            }
            Card host = definedSAs.getFirst().getHostCard();
            if (host.getManaCost() == null) {
                cost = new Cost(ManaCost.ZERO, true);
            } else {
                int xCount = host.getManaCost().countX();
                int xPaid = host.getXManaCostPaid() * xCount;
                ManaCostBeingPaid toPay = new ManaCostBeingPaid(host.getManaCost());
                toPay.decreaseShard(ManaCostShard.X, xCount);
                toPay.increaseGenericMana(xPaid);
                cost = new Cost(toPay.toManaCost(), true);
            }
        }
        else if (!StringUtils.isBlank(sa.getSVar(unlessCost)) && !unlessCost.equals("X")) {
            // check for non-X costs (stored in SVars
            int xCost = calculateAmount(source, TextUtil.fastReplace(sa.getParam("UnlessCost"),
                    " ", ""), sa);
            //Check for XColor
            ManaCostBeingPaid toPay = new ManaCostBeingPaid(ManaCost.ZERO);
            byte xColor = ManaAtom.fromName(sa.getParamOrDefault("UnlessColor", "1"));
            toPay.increaseShard(ManaCostShard.valueOf(xColor), xCost);
            cost = new Cost(toPay.toManaCost(), true);
        }
        else {
            cost = new Cost(unlessCost, true);
        }
        cost = CostAdjustment.adjust(cost, sa, true);
        return cost;
    }

    /**
     * <p>
     * handleRemembering.
     * </p>
     *
     * @param sa
     *            a SpellAbility object.
     */
    public static void handleRemembering(final SpellAbility sa) {
        Card host = sa.getHostCard();

        if (sa.hasParam("RememberTargets") && sa.usesTargeting()) {
            if (sa.hasParam("ForgetOtherTargets")) {
                host.clearRemembered();
            }
            host.addRemembered(sa.getTargets());
            if (sa.hasParam("IncludeAllComponentCards")) {
                for (Card c : sa.getTargets().getTargetCards()) {
                    host.addRemembered(c.getAllComponentCards(false));
                }
            }
        }

        if (sa.hasParam("RememberCostMana")) {
            host.clearRemembered();
            ManaCostBeingPaid activationMana = new ManaCostBeingPaid(sa.getPayCosts().getTotalMana());
            if (sa.getXManaCostPaid() != null) {
                activationMana.setXManaCostPaid(sa.getXManaCostPaid(), null);
            }
            int activationShards = activationMana.getConvertedManaCost();
            List<Mana> payingMana = sa.getPayingMana();
            // even if the cost was raised, we only care about mana from activation part
            // let's just assume the first shards spent are that for easy handling
            List<Mana> activationPaid = payingMana.subList(0, activationShards);
            StringBuilder sb = new StringBuilder();
            int nMana = 0;
            for (Mana m : activationPaid) {
                if (nMana > 0) {
                    sb.append(" ");
                }
                sb.append(m.toString());
                nMana++;
            }
            host.addRemembered(sb.toString());
        }
    }

    /**
     * <p>
     * Parse non-mana X variables.
     * </p>
     *
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param ctb
     *            a {@link forge.game.CardTraitBase} object.
     * @return a int.
     */
    public static int xCount(Card c, final String s, final CardTraitBase ctb) {
        final String s2 = applyAbilityTextChangeEffects(s, ctb);
        final String[] l = s2.split("/");
        final String expr = CardFactoryUtil.extractOperators(s2);

        Player player = null;
        if (ctb != null) {
            if (ctb instanceof SpellAbility) {
                player = ((SpellAbility)ctb).getActivatingPlayer();
            }
            if (player == null) {
                player = ctb.getHostCard().getController();
            }
        }

        // accept straight numbers
        if (l[0].startsWith("Number$")) {
            final String number = l[0].substring(7);
            return doXMath(Integer.parseInt(number), expr, c, ctb);
        }

        if (l[0].startsWith("Count$")) {
            l[0] = l[0].substring(6);
        }

        if (l[0].startsWith("SVar$")) {
            String n = l[0].substring(5);
            String v = ctb == null ? c.getSVar(n) : ctb.getSVar(n);
            return doXMath(xCount(c, v, ctb), expr, c, ctb);
        }

        final String[] sq;
        sq = l[0].split("\\.");
        String[] paidparts = l[0].split("\\$", 2);
        Iterable<Card> someCards = null;
        final Game game = c.getGame();

        if (ctb != null) {
            // Count$Compare <int comparator value>.<True>.<False>
            if (sq[0].startsWith("Compare")) {
                final String[] compString = sq[0].split(" ");
                final int lhs = calculateAmount(c, compString[1], ctb);
                final int rhs =  calculateAmount(c, compString[2].substring(2), ctb);
                boolean v = Expressions.compare(lhs, compString[2], rhs);
                return doXMath(calculateAmount(c, sq[v ? 1 : 2], ctb), expr, c, ctb);
            }

            // Count$IsPrime <SVar>.<True>.<False>
            if (sq[0].startsWith("IsPrime")) {
                final String[] compString = sq[0].split(" ");
                final int lhs = calculateAmount(c, compString[1], ctb);
                boolean v = IntMath.isPrime(lhs);
                return doXMath(calculateAmount(c, sq[v ? 1 : 2], ctb), expr, c, ctb);
            }

            SpellAbility sa = null;
            if (ctb instanceof SpellAbility) {
                sa = (SpellAbility) ctb;
            } else if (sq[0].contains("xPaid") && ctb instanceof TriggerReplacementBase) {
                // try avoid fallback
                sa = ((TriggerReplacementBase) ctb).getOverridingAbility();
            }

            if (sa != null) {
                // special logic for xPaid in SpellAbility
                if (sq[0].contains("xPaid")) {
                    SpellAbility root = sa.getRootAbility();

                    // 107.3i If an object gains an ability, the value of X within that ability is the value defined by that ability,
                    // or 0 if that ability doesn't define a value of X. This is an exception to rule 107.3h. This may occur with ability-adding effects, text-changing effects, or copy effects.
                    if (root.getXManaCostPaid() != null) {
                        return doXMath(root.getXManaCostPaid(), expr, c, ctb);
                    }

                    // If the chosen creature has X in its mana cost, that X is considered to be 0.
                    // The value of X in Altered Ego’s last ability will be whatever value was chosen for X while casting Altered Ego.
                    if (sa.isCopiedTrait() && !sa.getHostCard().equals(c)) {
                        return doXMath(0, expr, c, ctb);
                    }

                    if (root.isTrigger()) {
                        Trigger t = root.getTrigger();

                        // ImmediateTrigger should check for the Ability which created the trigger
                        if (t.getSpawningAbility() != null) {
                            root = t.getSpawningAbility().getRootAbility();
                            return doXMath(root.getXManaCostPaid() == null ? 0 : root.getXManaCostPaid(), expr, c, ctb);
                        }

                        // 107.3k If an object’s enters-the-battlefield triggered ability or replacement effect refers to X,
                        // and the spell that became that object as it resolved had a value of X chosen for any of its costs,
                        // the value of X for that ability is the same as the value of X for that spell, although the value of X for that permanent is 0.
                        if (TriggerType.ChangesZone.equals(t.getMode()) && ZoneType.Battlefield.name().equals(t.getParam("Destination"))) {
                           int x = isUnlinkedFromCastSA(ctb, c) ? 0 : c.getXManaCostPaid();
                           return doXMath(x, expr, c, ctb);
                        } else if (TriggerType.SpellCast.equals(t.getMode())) {
                            // Cast Trigger like Hydroid Krasis
                            SpellAbility castSA = (SpellAbility) root.getTriggeringObject(AbilityKey.SpellAbility);
                            if (castSA == null || castSA.getXManaCostPaid() == null) {
                                return doXMath(0, expr, c, ctb);
                            }
                            return doXMath(castSA.getXManaCostPaid(), expr, c, ctb);
                        } else if (TriggerType.Cycled.equals(t.getMode())) {
                            SpellAbility cycleSA = (SpellAbility) sa.getTriggeringObject(AbilityKey.Cause);
                            if (cycleSA == null || cycleSA.getXManaCostPaid() == null) {
                                return doXMath(0, expr, c, ctb);
                            }
                            return doXMath(cycleSA.getXManaCostPaid(), expr, c, ctb);
                        } else if (TriggerType.TurnFaceUp.equals(t.getMode())) {
                            SpellAbility turnupSA = (SpellAbility) sa.getTriggeringObject(AbilityKey.Cause);
                            if (turnupSA == null || turnupSA.getXManaCostPaid() == null) {
                                return doXMath(0, expr, c, ctb);
                            }
                            return doXMath(turnupSA.getXManaCostPaid(), expr, c, ctb);
                        }
                    }

                    if (root.isReplacementAbility() && sa.hasParam("ETB")) {
                        int x = isUnlinkedFromCastSA(ctb, c) ? 0 : c.getXManaCostPaid();
                        return doXMath(x, expr, c, ctb);
                    }

                    return doXMath(0, expr, c, ctb);
                }

                // Count$Kicked.<numHB>.<numNotHB>
                if (sq[0].startsWith("Kicked")) {
                    boolean kicked = sa.isKicked() || (!isUnlinkedFromCastSA(ctb, c) && c.getKickerMagnitude() > 0);
                    return doXMath(calculateAmount(c, sq[kicked ? 1 : 2], ctb), expr, c, ctb);
                }

                if (sq[0].startsWith("OptionalGenericCostPaid")) {
                    return doXMath(calculateAmount(c, sq[sa.isOptionalCostPaid(OptionalCost.Generic) ? 1 : 2], ctb), expr, c, ctb);
                }

                if (sq[0].startsWith("Bargain")) {
                    return doXMath(calculateAmount(c, sq[sa.isBargained() ? 1 : 2], ctb), expr, c, ctb);
                }

                if (sq[0].startsWith("Freerunning")) {
                    return doXMath(calculateAmount(c, sq[sa.isFreerunning() ? 1 : 2], ctb), expr, c, ctb);
                }

                // Count$Madness.<True>.<False>
                if (sq[0].startsWith("Madness")) {
                    return doXMath(calculateAmount(c, sq[sa.isMadness() ? 1 : 2], ctb), expr, c, ctb);
                }

                //Count$HasNumChosenColors.<DefinedCards related to spellability>
                if (sq[0].contains("HasNumChosenColors")) {
                    int sum = 0;
                    for (Card card : getDefinedCards(c, sq[1], sa)) {
                        sum += card.getColor().getSharedColors(ColorSet.fromNames(c.getChosenColors())).countColors();
                    }
                    return sum;
                }
                if (sq[0].startsWith("TriggerRememberAmount")) {
                    int count = 0;
                    for (final Object o : sa.getTriggerRemembered()) {
                        if (o instanceof Integer) {
                            count += (Integer) o;
                        }
                    }
                    return count;
                }
                // Count$TriggeredManaCostDevotion.<Color>
                if (sq[0].startsWith("TriggeredManaCostDevotion")) {
                    final SpellAbility root = sa.getRootAbility();
                    Card triggeringObject = (Card) root.getTriggeringObject(AbilityKey.Card);
                    int count = 0;
                    byte colorCode = ManaAtom.fromName(sq[1]);
                    for (ManaCostShard sh : triggeringObject.getManaCost()) {
                        if (sh.isColor(colorCode)) {
                            count++;
                        }
                    }
                    return count;
                }
                // Count$TriggeredPayingMana.<Color1>.<Color2>
                if (sq[0].startsWith("TriggeredPayingMana")) {
                    final SpellAbility root = sa.getRootAbility();
                    String mana = (String) root.getTriggeringObject(AbilityKey.PayingMana);
                    int count = 0;
                    Matcher mat = Pattern.compile(StringUtils.join(sq, "|", 1, sq.length)).matcher(mana);
                    while (mat.find()) {
                        count++;
                    }
                    return count;
                }
                // Count$ManaProduced
                if (sq[0].startsWith("AmountManaProduced")) {
                    final SpellAbility root = sa.getRootAbility();
                    int amount = 0;
                    if (root != null) {
                        for (AbilityManaPart amp : root.getAllManaParts()) {
                            amount = amount + amp.getLastManaProduced().size();
                        }
                    }
                    return doXMath(amount, expr, c, ctb);
                }
                // Count$NumTimesChoseMode
                if (sq[0].startsWith("NumTimesChoseMode")) {
                    int amount = 0;
                    SpellAbility tail = sa.getTailAbility();
                    if (tail.hasSVar("CharmOrder")) {
                        amount = tail.getSVarInt("CharmOrder");
                    }
                    return doXMath(amount, expr, c, ctb);
                }
                // Count$ManaColorsPaid
                if (sq[0].equals("ManaColorsPaid")) {
                    final SpellAbility root = sa.getRootAbility();
                    return doXMath(root == null ? 0 : root.getPayingColors().countColors(), expr, c, ctb);
                }

                // Count$Adamant.<Color>.<True>.<False>
                if (sq[0].startsWith("Adamant")) {
                    final String payingMana = StringUtils.join(sa.getRootAbility().getPayingMana());
                    final int num = sq[0].length() > 7 ? Integer.parseInt(sq[0].split("_")[1]) : 3;
                    final boolean adamant = StringUtils.countMatches(payingMana, MagicColor.toShortString(sq[1])) >= num;
                    return doXMath(calculateAmount(c,sq[adamant ? 2 : 3], ctb), expr, c, ctb);
                }

                if (sq[0].startsWith("LastStateBattlefield")) {
                    final String[] k = paidparts[0].split(" ");
                    // this is only for spells that were cast
                    if (sq[0].contains("WithFallback")) {
                        if (!sa.getHostCard().wasCast()) {
                            return doXMath(0, expr, c, ctb);
                        }
                        someCards = sa.getHostCard().getCastSA().getLastStateBattlefield();
                    } else {
                        someCards = sa.getLastStateBattlefield();
                    }
                    if (someCards == null || Iterables.isEmpty(someCards)) {
                        // LastState is Empty
                        if (sq[0].contains("WithFallback")) {
                            someCards = game.getCardsIn(ZoneType.Battlefield);
                        } else {
                            return doXMath(0, expr, c, ctb);
                        }
                    }
                    someCards = CardLists.getValidCards(someCards, k[1], player, c, sa);
                }

                if (sq[0].startsWith("LastStateGraveyard")) {
                    final String[] k = l[0].split(" ");
                    CardCollectionView list;
                    // this is only for spells that were cast
                    if (sq[0].contains("WithFallback")) {
                        if (!sa.getHostCard().wasCast()) {
                            return doXMath(0, expr, c, ctb);
                        }
                        list = sa.getHostCard().getCastSA().getLastStateGraveyard();
                    } else {
                        list = sa.getLastStateGraveyard();
                    }
                    if (sa.getLastStateGraveyard() == null || list.isEmpty()) {
                        // LastState is Empty
                        if (sq[0].contains("WithFallback")) {
                            list = game.getCardsIn(ZoneType.Graveyard);
                        } else {
                            return doXMath(0, expr, c, ctb);
                        }
                    }
                    list = CardLists.getValidCards(list, k[1], player, c, sa);
                    return doXMath(list.size(), expr, c, ctb);
                }

                if (sq[0].equals("ActivatedThisGame")) {
                    return doXMath(sa.getActivationsThisGame(), expr, c, ctb);
                }

                if (sq[0].equals("ResolvedThisTurn")) {
                    return doXMath(sa.getResolvedThisTurn(), expr, c, ctb);
                }

                if (sq[0].startsWith("TotalManaSpent ")) {
                    final String[] k = sq[0].split(" ");
                    int v = 0;
                    if (sa.getRootAbility().getPayingMana() != null) {
                        for (Mana m : sa.getRootAbility().getPayingMana()) {
                            Card source = m.getSourceCard();
                            if (source != null) {
                                if (source.isValid(k[1].split(","), player, c, sa)) {
                                    v += 1;
                                }
                            }
                        }
                    }
                    return doXMath(v, expr, c, ctb);
                }
                
                // Count$FromNamedAbility[abilityName].<True>.<False>
                if (sq[0].startsWith("FromNamedAbility")) {
                    String abilityNamed = sq[0].substring(16);
                    SpellAbility trigSA = sa.getHostCard().getCastSA();
                    boolean fromNamedAbility = trigSA != null && trigSA.getName().equals(abilityNamed);
                    return doXMath(calculateAmount(c, sq[fromNamedAbility ? 1 : 2], ctb), expr, c, ctb);
                }
            } else {
                // fallback if ctb isn't a spellability
                if (sq[0].startsWith("LastStateBattlefield")) {
                    final String[] k = l[0].split(" ");
                    CardCollectionView list = game.getLastStateBattlefield();
                    list = CardLists.getValidCards(list, k[1], player, c, ctb);
                    return doXMath(list.size(), expr, c, ctb);
                }

                if (sq[0].startsWith("LastStateGraveyard")) {
                    final String[] k = l[0].split(" ");
                    CardCollectionView list = game.getLastStateGraveyard();
                    list = CardLists.getValidCards(list, k[1], player, c, ctb);
                    return doXMath(list.size(), expr, c, ctb);
                }

                if (sq[0].startsWith("xPaid")) {
                    return doXMath(c.getXManaCostPaid(), expr, c, ctb);
                }

            } // end SpellAbility

            if (sq[0].equals("CastTotalManaSpent")) {
                return doXMath(c.getCastSA() != null ? c.getCastSA().getTotalManaSpent() : 0, expr, c, ctb);
            }
            if (sq[0].startsWith("CastTotalManaSpent ")) {
                final String[] k = sq[0].split(" ");
                int v = 0;
                if (c.getCastSA() != null) {
                    for (Mana m : c.getCastSA().getPayingMana()) {
                        Card source = m.getSourceCard();
                        if (source != null) {
                            if (source.isValid(k[1].split(","), player, c, ctb)) {
                                v += 1;
                            }
                        }
                    }
                }
                return doXMath(v, expr, c, ctb);
            }

            if (sq[0].equals("hasOptionalKeywordAmount")) {
                return doXMath(c.getCastSA() != null && c.getCastSA().hasOptionalKeywordAmount(ctb.getKeyword()) ? 1 : 0, expr, c, ctb);
            }
            if (sq[0].equals("OptionalKeywordAmount")) {
                return doXMath(c.getCastSA() != null ? c.getCastSA().getOptionalKeywordAmount(ctb.getKeyword()) : 0, expr, c, ctb);
            }

            // Count$DevotionDual.<color name>.<color name>
            // Count$Devotion.<color name>
            if (sq[0].contains("Devotion")) {
                int colorOccurrences = 0;
                String colorName = sq[1];
                if (colorName.contains("Chosen")) {
                    colorName = MagicColor.toShortString(c.getChosenColor());
                }
                byte colorCode = ManaAtom.fromName(colorName);
                if (sq[0].equals("DevotionDual")) {
                    colorCode |= ManaAtom.fromName(sq[2]);
                }
                for (Card c0 : player.getCardsIn(ZoneType.Battlefield)) {
                    for (ManaCostShard sh : c0.getManaCost()) {
                        if (sh.isColor(colorCode)) {
                            colorOccurrences++;
                        }
                    }
                }
                colorOccurrences += player.getDevotionMod();
                return doXMath(colorOccurrences, expr, c, ctb);
            }
        } // end ctb != null

        //Count$SearchedLibrary.<DefinedPlayer>
        if (sq[0].contains("SearchedLibrary")) {
            int sum = 0;
            for (Player p : getDefinedPlayers(c, sq[1], ctb)) {
                sum += p.getLibrarySearched();
            }
            return doXMath(sum, expr, c, ctb);
        }

        // count valid cards in any specified zone/s
        if (sq[0].startsWith("Valid")) {
            String[] lparts = paidparts[0].split(" ", 2);

            CardCollectionView cardsInZones = null;
            if (lparts[0].contains("All")) {
                cardsInZones = game.getCardsInGame();
            } else if (lparts[0].endsWith("Self")) {
                cardsInZones = new CardCollection(c);
            } else {
                final List<ZoneType> zones = ZoneType.listValueOf(lparts[0].length() > 5 ? lparts[0].substring(5) : "Battlefield");
                boolean usedLastState = false;
                if (ctb instanceof SpellAbility && zones.size() == 1) {
                    SpellAbility sa = (SpellAbility) ctb;
                    if (sa.isReplacementAbility()) {
                        if (zones.get(0).equals(ZoneType.Battlefield)) {
                            cardsInZones = sa.getRootAbility().getLastStateBattlefield();
                            usedLastState = true;
                        } else if (zones.get(0).equals(ZoneType.Graveyard)) {
                            cardsInZones = sa.getRootAbility().getLastStateGraveyard();
                            usedLastState = true;
                        }
                    }
                }
                if (!usedLastState) {
                    cardsInZones = game.getCardsIn(zones);
                }
            }

            someCards = CardLists.getValidCards(cardsInZones, lparts[1], player, c, ctb);
        }

        if (sq[0].startsWith("RememberedSize")) {
            return doXMath(c.getRememberedCount(), expr, c, ctb);
        }
        if (sq[0].startsWith("ChosenSize")) {
            return doXMath(c.getChosenCards().size(), expr, c, ctb);
        }
        if (sq[0].startsWith("ImprintedSize")) {
            return doXMath(c.getImprintedCards().size(), expr, c, ctb);
        }

        if (sq[0].startsWith("RememberedNumber")) {
            int num = 0;
            for (final Object o : c.getRemembered()) {
                if (o instanceof Integer) {
                    num += (Integer) o;
                }
            }
            return doXMath(num, expr, c, ctb);
        }

        if (sq[0].startsWith("RememberedWithSharedCardType")) {
            int maxNum = 1;
            for (final Object o : c.getRemembered()) {
                if (o instanceof Card) {
                    int num = 1;
                    Card firstCard = (Card) o;
                    for (final Object p : c.getRemembered()) {
                        if (p instanceof Card) {
                            Card secondCard = (Card) p;
                            if (!firstCard.equals(secondCard) && firstCard.sharesCardTypeWith(secondCard)) {
                                num++;
                            }
                        }
                    }
                    if (num > maxNum) {
                        maxNum = num;
                    }
                }
            }
            return doXMath(maxNum, expr, c, ctb);
        }

        // might get called from editor
        if (game != null) {
            // CR 608.2h
            // we'll want to avoid grabbing LKI for params that can handle internal information
            // e.g. the remembering on Xenagos, the Reveler
            c = game.getChangeZoneLKIInfo(c);
        }

        ////////////////////
        // card info

        // Count$CardMulticolor.<numMC>.<numNotMC>
        if (sq[0].contains("CardMulticolor")) {
            final boolean isMulti = c.getColor().isMulticolor();
            return doXMath(Integer.parseInt(sq[isMulti ? 1 : 2]), expr, c, ctb);
        }

        if (sq[0].equals("ColorsColorIdentity")) {
            return doXMath(c.getController().getCommanderColorID().countColors(), expr, c, ctb);
        }

        // Count$Foretold.<True>.<False>
        if (sq[0].startsWith("Foretold")) {
            return doXMath(calculateAmount(c, sq[c.isForetold() ? 1 : 2], ctb), expr, c, ctb);
        }

        if (sq[0].startsWith("Kicked")) { // fallback for not spellAbility
            return doXMath(calculateAmount(c, sq[!isUnlinkedFromCastSA(ctb, c) && c.getKickerMagnitude() > 0 ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].startsWith("PromisedGift")) {
            return doXMath(calculateAmount(c, sq[c.getCastSA() != null && c.getCastSA().isGiftPromised() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].startsWith("Escaped")) {
            return doXMath(calculateAmount(c, sq[c.getCastSA() != null && c.getCastSA().isEscape() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].startsWith("Emerged")) {
            return doXMath(calculateAmount(c, sq[!isUnlinkedFromCastSA(ctb, c) && c.getCastSA() != null && c.getCastSA().isEmerge() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].startsWith("AltCost")) {
            return doXMath(calculateAmount(c, sq[c.isOptionalCostPaid(OptionalCost.AltCost) ? 1 : 2], ctb), expr, c, ctb);
        }

        if (sq[0].equals("CardPower")) {
            return doXMath(c.getNetPower(), expr, c, ctb);
        }
        if (sq[0].equals("CardBasePower")) {
            return doXMath(c.getCurrentPower(), expr, c, ctb);
        }
        if (sq[0].equals("CardToughness")) {
            return doXMath(c.getNetToughness(), expr, c, ctb);
        }
        if (sq[0].equals("CardSumPT")) {
            return doXMath(c.getNetPower() + c.getNetToughness(), expr, c, ctb);
        }

        if (sq[0].equals("CardNumNotedTypes")) {
            return doXMath(c.getNumNotedTypes(), expr, c, ctb);
        }

        if (sq[0].equals("CardNumColors")) {
            return doXMath(c.getColor().countColors(), expr, c, ctb);
        }

        if (sq[0].equals("CardNumAttacksThisTurn")) {
            return doXMath(c.getDamageHistory().getCreatureAttacksThisTurn(), expr, c, ctb);
        }
        if (sq[0].equals("CardNumAttacksThisGame")) {
            return doXMath(c.getDamageHistory().getAttacksThisGame(), expr, c, ctb);
        }

        if (sq[0].equals("CrewSize")) {
            return doXMath(c.getCrewedByThisTurn() == null ? 0 : c.getCrewedByThisTurn().size(), expr, c, ctb);
        }

        if (sq[0].equals("Intensity")) {
            return doXMath(c.getIntensity(true), expr, c, ctb);
        }

        if (sq[0].startsWith("CardCounters")) {
            // CardCounters.ALL to be used for Kinsbaile Borderguard and anything that cares about all counters
            int count = 0;
            if (sq[1].equals("ALL")) count = c.getNumAllCounters();
            else count = c.getCounters(CounterType.getType(sq[1]));
            return doXMath(count, expr, c, ctb);
        }

        if (sq[0].contains("TotalValue")) {
            return doXMath(c.getKeywordMagnitude(Keyword.smartValueOf(l[0].split(" ")[1])), expr, c, ctb);
        }
        if (sq[0].contains("TimesKicked")) {
            return doXMath(isUnlinkedFromCastSA(ctb, c) ? 0 : c.getKickerMagnitude(), expr, c, ctb);
        }
        if (sq[0].contains("TimesMutated")) {
            return doXMath(c.getTimesMutated(), expr, c, ctb);
        }

        if (sq[0].equals("RegeneratedThisTurn")) {
            return doXMath(c.getRegeneratedThisTurn(), expr, c, ctb);
        }

        if (sq[0].contains("Converge")) {
            SpellAbility castSA = c.getCastSA();
            return doXMath(castSA == null ? 0 : castSA.getPayingColors().countColors(), expr, c, ctb);
        }

        if (sq[0].startsWith("EachPhyrexianPaidWithLife")) {
            SpellAbility castSA = c.getCastSA();
            if (castSA == null) {
                return 0;
            }
            return doXMath(castSA.getSpendPhyrexianMana(), expr, c, ctb);
        }

        if (sq[0].startsWith("EachSpentToCast")) {
            SpellAbility castSA = c.getCastSA();
            if (castSA == null) {
                return 0;
            }
            final List<Mana> paidMana = castSA.getPayingMana();
            final String type = sq[1];
            int count = 0;
            for (Mana m : paidMana) {
                if (m.toString().equals(type)) {
                    count++;
                }
            }
            return doXMath(count, expr, c, ctb);
        }

        // Count$wasCastFrom<Zone>.<true>.<false>
        if (sq[0].startsWith("wasCastFrom")) {
            boolean your = sq[0].contains("Your");
            boolean byYou = sq[0].contains("ByYou");
            String strZone = sq[0].substring(11);
            if (your) {
                strZone = strZone.substring(4);
            }
            if (byYou) {
                strZone = strZone.substring(0, strZone.indexOf("ByYou", 0));
            }
            boolean zonesMatch = c.getCastFrom() != null && c.getCastFrom().getZoneType() == ZoneType.smartValueOf(strZone)
                    && (!byYou || player.equals(c.getCastSA().getActivatingPlayer()))
                    && (!your || c.getCastFrom().getPlayer().equals(player));
            return doXMath(calculateAmount(c, sq[zonesMatch ? 1 : 2], ctb), expr, c, ctb);
        }

        // Count$Presence_<Type>.<True>.<False>
        if (sq[0].startsWith("Presence")) {
            final String type = sq[0].split("_")[1];
            boolean found = false;
            if (c.getCastFrom() != null && c.getCastSA() != null) {
                int revealed = calculateAmount(c, "Revealed$Valid " + type, c.getCastSA());
                int ctrl = calculateAmount(c, "Count$LastStateBattlefield " + type + ".YouCtrl", c.getCastSA());
                if (revealed + ctrl >= 1) {
                    found = true;
                }
            }
            return doXMath(calculateAmount(c, sq[found ? 1 : 2], ctb), expr, c, ctb);
        }

        if (sq[0].startsWith("Devoured")) {
            final String validDevoured = sq[0].split(" ")[1];
            CardCollection cl = CardLists.getValidCards(c.getDevouredCards(), validDevoured, player, c, ctb);
            return doXMath(cl.size(), expr, c, ctb);
        }

        if (sq[0].contains("ChosenNumber")) {
            Integer i = c.getChosenNumber();
            return doXMath(i == null ? 0 : i, expr, c, ctb);
        }

        // Count$IfCastInOwnMainPhase.<numMain>.<numNotMain>
        if (sq[0].endsWith("InOwnMainPhase")) {
            final PhaseHandler cPhase = game.getPhaseHandler();
            final boolean isMyMain = cPhase.getPhase().isMain() && cPhase.isPlayerTurn(player) &&
                    (!sq[0].startsWith("IfCast") || c.wasCast());
            return doXMath(Integer.parseInt(sq[isMyMain ? 1 : 2]), expr, c, ctb);
        }

        // Count$FinishedUpkeepsThisTurn
        if (sq[0].startsWith("FinishedUpkeepsThisTurn")) {
            return doXMath(game.getPhaseHandler().getNumUpkeep() - (game.getPhaseHandler().is(PhaseType.UPKEEP) ? 1 : 0), expr, c, ctb);
        }

        // Count$FinishedEndOfTurnsThisTurn
        if (sq[0].startsWith("FinishedEndOfTurnsThisTurn")) {
            return doXMath(game.getPhaseHandler().getNumEndOfTurn() - (game.getPhaseHandler().is(PhaseType.END_OF_TURN) ? 1 : 0), expr, c, ctb);
        }

        // Count$AttachedTo <restriction>
        if (sq[0].startsWith("AttachedTo")) {
            final String[] k = l[0].split(" ");
            int sum = CardLists.getValidCardCount(c.getAttachedCards(), k[1], player, c, ctb);
            return doXMath(sum, expr, c, ctb);
        }

        // Count$CardManaCost
        if (sq[0].startsWith("CardManaCost")) {
            int cmc = c.getCMC();

            if (sq[0].contains("LKI") && !c.isInZone(ZoneType.Stack) && c.getManaCost() != null) {
                if (ctb instanceof SpellAbility && ((SpellAbility) ctb).getXManaCostPaid() != null) {
                    cmc += ((SpellAbility) ctb).getXManaCostPaid() * c.getManaCost().countX();
                } else {
                    cmc += c.getXManaCostPaid() * c.getManaCost().countX();
                }
            }

            return doXMath(cmc, expr, c, ctb);
        }

        // Count$EnchantedControllerCreatures
        if (sq[0].equals("EnchantedControllerCreatures")) { // maybe refactor into a Valid with ControlledBy
            int v = 0;
            if (c.getEnchantingCard() != null) {
                v = CardLists.count(c.getEnchantingCard().getController().getCardsIn(ZoneType.Battlefield), CardPredicates.CREATURES);
            }
            return doXMath(v, expr, c, ctb);
        }

        ////////////////////////
        // player info
        if (sq[0].equals("Hellbent")) {
            return doXMath(calculateAmount(c, sq[player.hasHellbent() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("Metalcraft")) {
            return doXMath(calculateAmount(c, sq[player.hasMetalcraft() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("Delirium")) {
            return doXMath(calculateAmount(c, sq[player.hasDelirium() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("FatefulHour")) {
            return doXMath(calculateAmount(c, sq[player.getLife() <= 5 ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("Revolt")) {
            return doXMath(calculateAmount(c, sq[player.hasRevolt() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("Landfall")) {
            return doXMath(calculateAmount(c, sq[player.hasLandfall() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("Monarch")) {
            return doXMath(calculateAmount(c, sq[player.isMonarch() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("Initiative")) {
            return doXMath(calculateAmount(c, sq[player.hasInitiative() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("StartingPlayer")) {
            return doXMath(calculateAmount(c, sq[player.isStartingPlayer() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("Blessing")) {
            return doXMath(calculateAmount(c, sq[player.hasBlessing() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("Threshold")) {
            return doXMath(calculateAmount(c, sq[player.hasThreshold() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("CommittedCrimeThisTurn")) {
            return doXMath(calculateAmount(c, sq[player.getCommittedCrimeThisTurn() > 0 ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("ExtraTurn")) {
            return doXMath(calculateAmount(c, sq[game.getPhaseHandler().getPlayerTurn().isExtraTurn() ? 1 : 2], ctb), expr, c, ctb);
        }
        if (sq[0].equals("YourStartingLife")) {
            return doXMath(player.getStartingLife(), expr, c, ctb);
        }

        if (sq[0].equals("YourLifeTotal")) {
            return doXMath(player.getLife(), expr, c, ctb);
        }
        if (sq[0].equals("OppGreatestLifeTotal")) {
            return doXMath(player.getOpponentsGreatestLifeTotal(), expr, c, ctb);
        }

        if (sq[0].equals("YouDrewThisTurn")) {
            return doXMath(player.getNumDrawnThisTurn(), expr, c, ctb);
        }
        if (sq[0].equals("YouDrewLastTurn")) {
            return doXMath(player.getNumDrawnLastTurn(), expr, c, ctb);
        }

        if (sq[0].equals("YouFlipThisTurn")) {
            return doXMath(player.getNumFlipsThisTurn(), expr, c, ctb);
        }

        if (sq[0].equals("YouRollThisTurn")) {
            return doXMath(player.getNumRollsThisTurn(), expr, c, ctb);
        }
        if (sq[0].startsWith("YouRolledThisTurn")) {
            int n = calculateAmount(c, sq[0].substring(17), ctb);
            return doXMath(Collections.frequency(player.getDiceRollsThisTurn(), n), expr, c, ctb);
        }

        if (sq[0].equals("YouSurveilThisTurn")) {
            return doXMath(player.getSurveilThisTurn(), expr, c, ctb);
        }

        if (sq[0].equals("YouDescendedThisTurn")) {
            return doXMath(player.getDescended(), expr, c, ctb);
        }

        if (sq[0].equals("YouCastThisGame")) {
            return doXMath(player.getSpellsCastThisGame(), expr, c, ctb);
        }

        if (sq[0].equals("YourSpeed")) {
            return doXMath(player.getSpeed(), expr, c, ctb);
        }
        if (sq[0].equals("MaxSpeed")) {
            return doXMath(calculateAmount(c, sq[player.maxSpeed() ? 1 : 2], ctb), expr, c, ctb);
        }

        if (sq[0].equals("AllFourBend")) {
            return doXMath(calculateAmount(c, sq[player.hasAllElementBend() ? 1 : 2], ctb), expr, c, ctb);
        }

        if (sq[0].equals("Night")) {
            return doXMath(calculateAmount(c, sq[game.isNight() ? 1 : 2], ctb), expr, c, ctb);
        }

        if (sq[0].equals("NumPiledGuessedSA")) {
            return doXMath(game.getNumPiledGuessedSA(), expr, c, ctb);
        }

        if (sq[0].startsWith("CommanderCastFromCommandZone")) {
            // only used by Opal Palace, and it does add the trigger to the card
            return doXMath(player.getCommanderCast(c), expr, c, ctb);
        }
        if (l[0].startsWith("TotalCommanderCastFromCommandZone")) {
            return doXMath(player.getTotalCommanderCast(), expr, c, ctb);
        }

        if (sq[0].contains("LifeYouLostThisTurn")) {
            return doXMath(player.getLifeLostThisTurn(), expr, c, ctb);
        }
        if (sq[0].contains("LifeYouGainedThisTurn")) {
            return doXMath(player.getLifeGainedThisTurn(), expr, c, ctb);
        }
        if (sq[0].contains("LifeYourTeamGainedThisTurn")) {
            return doXMath(player.getLifeGainedByTeamThisTurn(), expr, c, ctb);
        }
        if (sq[0].contains("LifeYouGainedTimesThisTurn")) {
            return doXMath(player.getLifeGainedTimesThisTurn(), expr, c, ctb);
        }
        if (sq[0].contains("LifeOppsLostThisTurn")) {
            return doXMath(player.getOpponentLostLifeThisTurn(), expr, c, ctb);
        }
        if (sq[0].equals("BloodthirstAmount")) {
            return doXMath(player.getBloodthirstAmount(), expr, c, ctb);
        }

        if (sq[0].startsWith("YourCounters")) {
            // "YourCountersExperience" or "YourCountersPoison"
            String counterType = sq[0].substring(12);
            return doXMath(player.getCounters(CounterType.getType(counterType)), expr, c, ctb);
        }

        if (sq[0].contains("TotalOppPoisonCounters")) {
            return doXMath(player.getOpponentsTotalPoisonCounters(), expr, c, ctb);
        }

        if (sq[0].equals("TotalDamageDoneByThisTurn")) {
            return doXMath(c.getTotalDamageDoneBy(), expr, c, ctb);
        }
        if (sq[0].equals("TotalDamageReceivedThisTurn")) {
            return doXMath(c.getAssignedDamage(), expr, c, ctb);
        }
        if (sq[0].equals("ExcessDamageReceivedThisTurn")) {
            return doXMath(c.getExcessDamageThisTurn(), expr, c, ctb);
        }

        if (sq[0].equals("MaxOppDamageThisTurn")) {
            return doXMath(player.getMaxOpponentAssignedDamage(), expr, c, ctb);
        }

        if (sq[0].equals("MaxCombatDamageThisTurn")) {
            return doXMath(player.getMaxAssignedCombatDamage(), expr, c, ctb);
        }

        if (sq[0].contains("TotalDamageThisTurn")) {
            String[] props = l[0].split(" ");
            int sum = 0;
            for (Pair<Integer, Boolean> p : c.getDamageReceivedThisTurn()) {
                if (game.getDamageLKI(p).getLeft().isValid(props[1], player, c, ctb)) {
                    sum += p.getLeft();
                }
            }
            return doXMath(sum, expr, c, ctb);
        }

        if (sq[0].equals("SingleMaxDamageThisTurn")) {
            int sum = game.getSingleMaxDamageDoneThisTurn();
            return doXMath(sum, expr, c, ctb);
        }

        if (sq[0].contains("DamageThisTurn")) {
            String[] props = l[0].split(" ");
            Boolean isCombat = null;
            if (sq[0].contains("CombatDamage")) {
                isCombat = !sq[0].contains("Non");
            }
            int num;
            List<Integer> dmgInstances = game.getDamageDoneThisTurn(isCombat, false, props[1], props[2], c, player, ctb);
            if (!dmgInstances.isEmpty() && sq[0].contains("Max")) {
                num = Collections.max(dmgInstances);
            } else if (sq[0].startsWith("Num")) {
                num = dmgInstances.size();
            } else {
                num = Aggregates.sum(dmgInstances);
            }
            return doXMath(num, expr, c, ctb);
        }

        if (sq[0].equals("YourTurns")) {
            return doXMath(player.getTurn(), expr, c, ctb);
        }

        if (sq[0].equals("NotedNumber")) {
            return doXMath(player.getNotedNumberForName(c.getName()), expr, c, ctb);
        }

        if (sq[0].equals("DraftNotesHighest")) {
            // Just in case you are playing this card in a deck without draft notes
            String note = player.getDraftNotes().getOrDefault(sq[1],  "0");
            int highest = 0;
            for (String n : note.split(",")) {
                int num = Integer.parseInt(n);
                if (num > highest) {
                    highest = num;
                }
            }

            return doXMath(highest, expr, c, ctb);
            // Other draft notes include: Names, Colors, Players, Creature Type.
            // But these aren't really things you count so they'll show up in properties most likely
        }

        if (sq[0].equals("DraftNotesCount")) {
            // Just in case you are playing this card in a deck without draft notes
            String note = player.getDraftNotes().getOrDefault(sq[1],  null);

            if (note == null) {
                return 0;
            }
            int highest = note.split(";").length;

            return doXMath(highest, expr, c, ctb);
            // Other draft notes include: Names, Colors, Players, Creature Type.
            // But these aren't really things you count so they'll show up in properties most likely
        }

        //Count$TypesSharedWith [defined]
        if (sq[0].startsWith("TypesSharedWith")) {
            Set<CardType.CoreType> thisTypes = Sets.newHashSet(c.getType().getCoreTypes());
            Set<CardType.CoreType> matches = new HashSet<>();
            for (Card c1 : getDefinedCards(ctb.getHostCard(), l[0].split(" ", 2)[1], ctb)) {
                for (CardType.CoreType type : Sets.newHashSet(c1.getType().getCoreTypes())) {
                    if (thisTypes.contains(type)) {
                        matches.add(type);
                    }
                }
            }
            return matches.size();
        }

        // Count$TopOfLibraryCMC
        if (sq[0].equals("TopOfLibraryCMC")) {
            int cmc = player.getCardsIn(ZoneType.Library).isEmpty() ? 0 :
                player.getCardsIn(ZoneType.Library).getFirst().getCMC();
            return doXMath(cmc, expr, c, ctb);
        }

        // Count$AttackersDeclared
        if (sq[0].startsWith("AttackersDeclared")) {
            List<Card> attackers = player.getCreaturesAttackedThisTurn();
            List<Card> differentAttackers = new ArrayList<>();
            for (Card attacker : attackers) {
                boolean add = true;
                for (Card different : differentAttackers) {
                    if (different.equalsWithGameTimestamp(attacker)) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    differentAttackers.add(attacker);
                }
            }
            return doXMath(differentAttackers.size(), expr, c, ctb);
        }

        // Count$CardAttackedThisTurn <Valid>
        if (sq[0].startsWith("CreaturesAttackedThisTurn")) {
            final String[] workingCopy = l[0].split(" ", 2);
            final String validFilter = workingCopy[1];
            return doXMath(CardLists.getValidCardCount(player.getCreaturesAttackedThisTurn(), validFilter, player, c, ctb), expr, c, ctb);
        }

        // Count$LeftBattlefieldThisTurn <Valid>
        if (sq[0].startsWith("LeftBattlefieldThisTurn")) {
            final String[] workingCopy = l[0].split(" ", 2);
            final String validFilter = workingCopy[1];
            return doXMath(CardLists.getValidCardCount(game.getLeftBattlefieldThisTurn(), validFilter, player, c, ctb), expr, c, ctb);
        }
        if (sq[0].startsWith("LeftGraveyardThisTurn")) {
            final String[] workingCopy = l[0].split(" ", 2);
            final String validFilter = workingCopy[1];
            return doXMath(CardLists.getValidCardCount(game.getLeftGraveyardThisTurn(), validFilter, player, c, ctb), expr, c, ctb);
        }

        if (sq[0].equals("UnlockedDoors")) {
            return doXMath(player.getUnlockedDoors().size(), expr, c, ctb);
        }
        // Counts the distinct names of unlocked doors. Used for the "Promising Stairs"
        if (sq[0].equals("DistinctUnlockedDoors")) {
            return doXMath(Sets.newHashSet(player.getUnlockedDoors()).size(), expr, c, ctb);
        }

        // Manapool
        if (sq[0].startsWith("ManaPool")) {
            final String color = l[0].split(":")[1];
            int v = 0;
            if (color.equals("All")) {
                v = player.getManaPool().totalMana();
            } else {
                v = player.getManaPool().getAmountOfColor(ManaAtom.fromName(color));
            }
            return doXMath(v, expr, c, ctb);
        }

        // Count$Domain
        if (sq[0].startsWith("Domain")) {
            int n = 0;
            Player neededPlayer = sq[0].equals("DomainActivePlayer") ? game.getPhaseHandler().getPlayerTurn() : player;
            CardCollection lands = neededPlayer.getLandsInPlay();
            for (String basic : MagicColor.Constant.BASIC_LANDS) {
                if (!CardLists.getType(lands, basic).isEmpty()) {
                    n++;
                }
            }
            return doXMath(n, expr, c, ctb);
        }

        if (sq[0].contains("AbilityYouCtrl")) {
            CardCollection all = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), "Creature", player, c, ctb);
            int count = 0;
            for (String ab : sq[0].substring(15).split(",")) {
                CardCollection found = CardLists.getValidCards(all, "Creature.with" + ab, player, c, ctb);
                if (!found.isEmpty()) {
                    count++;
                }
            }
            return doXMath(count, expr, c, ctb);
        }

        if (sq[0].contains("Party")) {
            Set<String> chosenParty = Sets.newHashSet();
            int wildcard = 0;
            ListMultimap<String, Card> multityped = MultimapBuilder.hashKeys().arrayListValues().build();
            List<Card> chosenMulti = Lists.newArrayList();

            // Figure out how to count each class separately.
            for (Card card : player.getCardsIn(ZoneType.Battlefield)) {
                if (!card.isCreature()) {
                    continue;
                }
                CardTypeView type = card.getType();
                Set<String> creatureTypes;

                // extra logic for "all creature types" cards
                if (type.hasAllCreatureTypes()) {
                    // one of the party types could be excluded, so check each of them separate
                    creatureTypes = CardType.Constant.PARTY_TYPES.stream().filter(p -> type.hasCreatureType(p)).collect(Collectors.toSet());
                } else { // shortcut for others 
                    creatureTypes = type.getCreatureTypes();
                    creatureTypes.retainAll(CardType.Constant.PARTY_TYPES);
                }

                switch (creatureTypes.size()) {
                case 0:
                    continue;
                case 4:
                    wildcard++;
                    break;
                case 1:
                    chosenParty.addAll(creatureTypes);
                    break;
                default:
                    for (String t : creatureTypes) {
                        multityped.put(t, card);
                    }
                }

                // found enough
                if (chosenParty.size() + wildcard >= 4) {
                    break;
                }
            }

            if (chosenParty.size() + wildcard < 4) {
                multityped.keySet().removeAll(chosenParty);

                // sort by amount of members
                Multimaps.asMap(multityped).entrySet().stream()
                    .sorted(Map.Entry.<String, List<Card>>comparingByValue(Comparator.<List<Card>>comparingInt(Collection::size)))
                    .forEach(e -> {
                        e.getValue().removeAll(chosenMulti);
                        if (e.getValue().size() > 0) {
                            chosenParty.add(e.getKey());
                            chosenMulti.add(e.getValue().get(0));
                        }
                    });
            }

            return doXMath(Math.min(chosenParty.size() + wildcard, 4), expr, c, ctb);
        }

        // TODO make AI part to understand Sunburst better so this isn't needed
        if (sq[0].startsWith("UniqueManaColorsProduced")) {
            boolean untappedOnly = sq[1].contains("ByUntappedSources");
            int uniqueColors = 0;
            CardCollectionView otb = player.getCardsIn(ZoneType.Battlefield);
            outer: for (byte color : MagicColor.WUBRG) {
                for (Card card : otb) {
                    if (!card.isTapped() || !untappedOnly) {
                        for (SpellAbility ma : card.getManaAbilities()) {
                            if (ma.canProduce(MagicColor.toShortString(color))) {
                                uniqueColors++;
                                continue outer;
                            }
                        }
                    }
                }
            }
            return doXMath(uniqueColors, expr, c, ctb);
        }

        // TODO change into checking SpellAbility
        if (sq[0].contains("xColorPaid")) {
            String[] attrs = sq[0].split(" ");
            StringBuilder colors = new StringBuilder();
            for (int i = 1; i < attrs.length; i++) {
                colors.append(attrs[i]);
            }
            return doXMath(c.getXManaCostPaidCount(colors.toString()), expr, c, ctb);
        }

        // Count$UrzaLands.<numHB>.<numNotHB>
        if (sq[0].startsWith("UrzaLands")) {
            return doXMath(calculateAmount(c, sq[player.hasUrzaLands() ? 1 : 2], ctb), expr, c, ctb);
        }

        /////////////////
        //game info
        // Count$Morbid.<True>.<False>
        if (sq[0].startsWith("Morbid")) {
            final List<Card> res = CardUtil.getThisTurnEntered(ZoneType.Graveyard, ZoneType.Battlefield, "Creature", c, ctb, player);
            return doXMath(calculateAmount(c, sq[res.size() > 0 ? 1 : 2], ctb), expr, c, ctb);
        }
        // Count$Void.<True>.<False>
        if (sq[0].startsWith("Void")) {
            return doXMath(calculateAmount(c, sq[game.isVoid() ? 1 : 2], ctb), expr, c, ctb);
        }

        // Count$Chroma.<color name>
        if (sq[0].startsWith("Chroma")) {
            final CardCollectionView cards;
            if (sq[0].contains("ChromaSource")) { // Runs Chroma for passed in Source card
                cards = new CardCollection(c);
            } else {
                ZoneType sourceZone = sq[0].contains("ChromaInGrave") ?  ZoneType.Graveyard : ZoneType.Battlefield;
                cards = player.getCardsIn(sourceZone);
            }

            byte colorCode;
            if (sq.length > 1) {
                colorCode = ManaAtom.fromName(sq[1]);
            } else {
                colorCode = ManaAtom.ALL_MANA_COLORS;
            }

            return doXMath(CardLists.getTotalChroma(cards, colorCode), expr, c, ctb);
        }

        if (l[0].contains("ExactManaCost")) {
            String[] sqparts = l[0].split(" ", 2);
            final String[] rest = sqparts[1].split(",");

            final CardCollectionView cardsInZones = sqparts[0].length() > 13
                ? game.getCardsIn(ZoneType.listValueOf(sqparts[0].substring(13)))
                : game.getCardsIn(ZoneType.Battlefield);

            CardCollection cards = CardLists.getValidCards(cardsInZones, rest, player, c, ctb);
            final Set<String> manaCost = Sets.newHashSet();

            for (Card card : cards) {
                manaCost.add(card.getManaCost().getShortString());
            }
            manaCost.remove(ManaCost.NO_COST.getShortString());

            return doXMath(manaCost.size(), expr, c, ctb);
        }

        if (sq[0].equals("StormCount")) {
            return doXMath(game.getStack().getSpellsCastThisTurn().size() - 1, expr, c, ctb);
        }

        if (sq[0].equals("FinalChapterNr")) {
            return doXMath(c.getFinalChapterNr(), expr, c, ctb);
        }

        if (sq[0].startsWith("PlanarDiceSpecialActionThisTurn")) {
            return game.getPhaseHandler().getPlanarDiceSpecialActionThisTurn();
        }

        if (sq[0].equals("TotalTurns")) {
            return doXMath(game.getPhaseHandler().getTurn(), expr, c, ctb);
        }

        if (sq[0].equals("MaxDistinctOnStack")) {
            return doXMath(game.getStack().getMaxDistinctSources(), expr, c, ctb);
        }

        if (sq[0].equals("MaxSameStoredRolls")) {
            int max = 0;
            List<Integer> rolls = c.getStoredRolls();
            if (rolls != null) {
                int lastNum = 0;
                for (Integer roll : rolls) {
                    if (roll.equals(lastNum)) {
                        continue; // no need to count instances of the same roll multiple times
                    }
                    int tally = Collections.frequency(rolls, roll);
                    if (tally > max) {
                        max = tally;
                    }
                    lastNum = roll;
                }
            }
            return doXMath(max, expr, c, ctb);
        }

        //Count$Random.<Min>.<Max>
        if (sq[0].equals("Random")) {
            int min = calculateAmount(c, sq[1], ctb);
            int max = calculateAmount(c, sq[2], ctb);

            return MyRandom.getRandom().nextInt(1+max-min) + min;
        }

        // Count$ThisTurnCast <Valid>
        // Count$LastTurnCast <Valid>
        // Count$CastSinceBeginningOfYourLastTurn_<Valid>
        if (sq[0].startsWith("ThisTurnCast") || sq[0].startsWith("LastTurnCast") 
            || sq[0].startsWith("CastSince")) {
            final String[] workingCopy = paidparts[0].split("_");
            final String validFilter = workingCopy[1];

            if (workingCopy[0].contains("This")) {
                someCards = CardUtil.getThisTurnCast(validFilter, c, ctb, player);
            } else if (workingCopy[0].contains("SinceBeginningOfYourLastTurn")) {
                someCards = CardUtil.getCastSinceBeginningOfYourLastTurn(validFilter, c, ctb, player);
            } else {
                someCards = CardUtil.getLastTurnCast(validFilter, c, ctb, player);
            }
        }
        if (sq[0].startsWith("ThisTurnActivated")) {
            final String[] workingCopy = paidparts[0].split("_");
            final String validFilter = workingCopy[1];
            // use objectXCount ?
            int activated = CardUtil.getThisTurnActivated(validFilter, c, ctb, player).size();
            for (IndividualCostPaymentInstance i : game.costPaymentStack) {
                if (i.getPayment().getAbility().isValid(validFilter, player, c, ctb)) {
                    activated++;
                }
            }
            return activated;
        }

        // Count$ThisTurnEntered <ZoneDestination> [from <ZoneOrigin>] <Valid>
        if (sq[0].startsWith("ThisTurnEntered") || sq[0].startsWith("LastTurnEntered")) {
            final String[] workingCopy = paidparts[0].split("_", 5);
            ZoneType destination = ZoneType.smartValueOf(workingCopy[1]);
            final boolean hasFrom = workingCopy[2].equals("from");
            ZoneType origin = hasFrom ? ZoneType.smartValueOf(workingCopy[3]) : null;
            String validFilter = workingCopy[hasFrom ? 4 : 2];

            if (sq[0].startsWith("This")) {
                someCards = CardUtil.getThisTurnEntered(destination, origin, validFilter, c, ctb, player);
            } else {
                someCards = CardUtil.getLastTurnEntered(destination, origin, validFilter, c, ctb, player);
            }
        }

        if (sq[0].startsWith("CountersAddedThisTurn")) {
            final String[] parts = l[0].split(" ");
            CounterType cType = CounterType.getType(parts[1]);

            return doXMath(game.getCounterAddedThisTurn(cType, parts[2], parts[3], c, player, ctb), expr, c, ctb);
        }
        if (sq[0].startsWith("CountersRemovedThisTurn")) {
            final String[] parts = l[0].split(" ");
            CounterType cType = CounterType.getType(parts[1]);

            return doXMath(game.getCounterRemovedThisTurn(cType, parts[2], c, player, ctb), expr, c, ctb);
        }

        if (sq[0].startsWith("MostCardName")) {
            String[] lparts = l[0].split(" ", 2);
            final String[] rest = lparts[1].split(",");

            final CardCollectionView cardsInZones = lparts[0].length() > 12
                ? game.getCardsIn(ZoneType.listValueOf(lparts[0].substring(12)))
                : game.getCardsIn(ZoneType.Battlefield);

            CardCollection cards = CardLists.getValidCards(cardsInZones, rest, player, c, ctb);
            final Map<String, Integer> map = Maps.newHashMap();
            for (final Card card : cards) {
                // Remove Duplicated types
                final String name = card.getName();
                Integer count = map.get(name);
                map.put(name, count == null ? 1 : count + 1);
            }
            int max = 0;
            for (final Entry<String, Integer> entry : map.entrySet()) {
                if (max < entry.getValue()) {
                    max = entry.getValue();
                }
            }
            return max;
        }

        if (sq[0].startsWith("MostProminentCreatureType")) {
            String restriction = l[0].split(" ")[1];
            CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), restriction, player, c, ctb);
            return doXMath(CardFactoryUtil.getMostProminentCreatureTypeSize(list), expr, c, ctb);
        }

        if (sq[0].startsWith("SecondMostProminentColor")) {
            String restriction = l[0].split(" ")[1];
            CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), restriction, player, c, ctb);
            int[] colorSize = CardFactoryUtil.SortColorsFromList(list);
            return doXMath(colorSize[colorSize.length - 2], expr, c, ctb);
        }

        // TODO move below to handlePaid
        if (sq[0].startsWith("DifferentCounterKinds_")) {
            final Set<CounterType> kinds = Sets.newHashSet();
            final String rest = l[0].substring(22);
            CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), rest, player, c, ctb);
            for (final Card card : list) {
                kinds.addAll(card.getCounters().keySet());
            }
            return doXMath(kinds.size(), expr, c, ctb);
        }

        // Complex counting methods
        Integer num = null;
        if (someCards == null) {
            someCards = getCardListForXCount(c, player, sq, ctb);
        } else if (paidparts.length > 1) {
            num = handlePaid(someCards, paidparts[1], c, ctb);
        }
        if (num == null) {
            num = Iterables.size(someCards);
        }

        return doXMath(num, expr, c, ctb);
    }

    public static final void applyManaColorConversion(ManaConversionMatrix matrix, String conversion) {
        for (String pair : conversion.split(" ")) {
            // Check if conversion is additive or restrictive and how to split
            boolean additive = pair.contains("->");
            String[] sides = pair.split(additive ? "->" : "<-");

            byte replacedColor = ManaAtom.fromConversion(sides[1]);
            if (sides[0].equals("AnyColor") || sides[0].equals("AnyType")) {
                for (byte c : (sides[0].equals("AnyColor") ? MagicColor.WUBRG : MagicColor.WUBRGC)) {
                    matrix.adjustColorReplacement(c, replacedColor, additive);
                }
            } else if (sides[0].startsWith("non")) {
                byte originalColor = ManaAtom.fromConversion(sides[0]);
                for (byte b : ManaAtom.MANATYPES) {
                    if ((originalColor & b) != 0) {
                        matrix.adjustColorReplacement(b, replacedColor, additive);
                    }
                }
            } else {
                matrix.adjustColorReplacement(ManaAtom.fromConversion(sides[0]), replacedColor, additive);
            }
        }
    }

    public static final List<SpellAbility> getBasicSpellsFromPlayEffect(final Card tgtCard, final Player controller) {
        return getSpellsFromPlayEffect(tgtCard, controller, CardStateName.Original, false);
    }
    public static final List<SpellAbility> getSpellsFromPlayEffect(final Card tgtCard, final Player controller, CardStateName state, boolean withAltCost) {
        List<SpellAbility> sas = new ArrayList<>();
        List<SpellAbility> list = new ArrayList<>();
        collectSpellsForPlayEffect(list, tgtCard.getState(tgtCard.getCurrentStateName()), controller, withAltCost);
        CardState original = tgtCard.getState(state);

        if (tgtCard.isFaceDown()) {
            collectSpellsForPlayEffect(list, original, controller, withAltCost);
        } else {
            if (state == CardStateName.Backside && !tgtCard.isModal() && tgtCard.isPermanent() && !tgtCard.isAura()) {
                // casting defeated battle
                Spell sp = new SpellPermanent(tgtCard, original);
                sp.setCardState(original);
                list.add(sp);
            }
            if (tgtCard.isModal() && tgtCard.hasState(CardStateName.Backside)) {
                collectSpellsForPlayEffect(list, tgtCard.getState(CardStateName.Backside), controller, withAltCost);
            }
        }

        for (SpellAbility s : list) {
            if (s.isLandAbility()) {
                s.setActivatingPlayer(controller);
                // CR 305.3
                if (controller.getGame().getPhaseHandler().isPlayerTurn(controller) && controller.canPlayLand(tgtCard, true, s)) {
                    sas.add(s);
                }
            } else {
                final Spell newSA = (Spell) s.copy(controller);
                newSA.getRestrictions().setZone(null);
                newSA.setCastFromPlayEffect(true);
                // extra timing restrictions still apply
                if (newSA.canPlay()) {
                    sas.add(newSA);
                }
            }
        }
        return sas;
    }

    private static void collectSpellsForPlayEffect(final List<SpellAbility> result, final CardState state, final Player controller, final boolean withAltCost) {
        if (state.getType().isLand()) {
            result.add(state.getFirstSpellAbility());
        }
        final Iterable<SpellAbility> spells = state.getSpellAbilities();
        for (SpellAbility sa : spells) {
            if (!sa.isSpell()) {
                continue;
            }
            if (!withAltCost && !sa.isBasicSpell()) {
                continue;
            }
            result.add(sa);
            if (withAltCost) {
                result.addAll(GameActionUtil.getAlternativeCosts(sa, controller, true));
            }
        }
    }

    public static final String applyAbilityTextChangeEffects(final String def, final CardTraitBase ability) {
        if (ability == null || !ability.isIntrinsic() || ability.hasParam("LockInText")) {
            return def;
        }
        return applyTextChangeEffects(def, ability.getHostCard(), false);
    }

    public static final String applyKeywordTextChangeEffects(final String kw, final Card card) {
        if (!CardUtil.isKeywordModifiable(kw)) {
            return kw;
        }
        return applyTextChangeEffects(kw, card, false);
    }

    public static final String applyDescriptionTextChangeEffects(final String def, final CardTraitBase ability) {
        if (ability == null || !ability.isIntrinsic() || ability.hasParam("LockInText")) {
            return def;
        }
        return applyTextChangeEffects(def, ability.getHostCard(), true);
    }

    /**
     * Apply description-based text changes of a {@link Card} to a String. No
     * checks are made on traits being intrinsic.
     *
     * @param def a String.
     * @param card a {@link Card}.
     * @return a new String, taking text changes into account.
     */
    public static final String applyDescriptionTextChangeEffects(final String def, final Card card) {
        return applyTextChangeEffects(def, card, true);
    }
    private static String applyTextChangeEffects(final String def, final Card card, final boolean isDescriptive) {
        return applyTextChangeEffects(def, isDescriptive, card.getChangedTextColorWords(), card.getChangedTextTypeWords());
    }

    public static final String applyTextChangeEffects(final String def, final boolean isDescriptive,
            Map<String,String> colorMap, Map<String,String> typeMap) {
        if (StringUtils.isEmpty(def)) {
            return def;
        }

        String replaced = def;
        for (final Entry<String, String> e : colorMap.entrySet()) {
            final String key = e.getKey();
            if (key.equals("Any")) {
                for (final byte c : MagicColor.WUBRG) {
                    final String colorLowerCase = MagicColor.toLongString(c).toLowerCase(),
                            colorCaptCase = StringUtils.capitalize(MagicColor.toLongString(c));
                    // Color should not replace itself.
                    if (e.getValue().equalsIgnoreCase(colorLowerCase)) {
                        continue;
                    }
                    replaced = getReplacedText(replaced, colorLowerCase, e.getValue().toLowerCase(), isDescriptive);
                    replaced = getReplacedText(replaced, colorCaptCase, e.getValue(), isDescriptive);
                }
            } else {
                replaced = getReplacedText(replaced, key.toLowerCase(), e.getValue().toLowerCase(), isDescriptive);
                replaced = getReplacedText(replaced, key, e.getValue(), isDescriptive);
            }
        }
        for (final Entry<String, String> e : typeMap.entrySet()) {
            final String key = e.getKey();
            if (isDescriptive) {
                replaced = getReplacedText(replaced, CardType.getPluralType(key), CardType.getPluralType(e.getValue()), isDescriptive);
            }
            replaced = getReplacedText(replaced, key, e.getValue(), isDescriptive);
        }

        return replaced;
    }

    private static String getReplacedText(final String text, final String originalWord, String newWord, final boolean isDescriptive) {
        if (isDescriptive) {
            newWord = "<strike>" + originalWord + "</strike> " + newWord;
        }
        // use word boundaries and keep negations - java only supports bounded maximum length in negative lookbehind
        return text.replaceAll((isDescriptive ? "(?<!>)" : "") + "(?<!named.{0,100})\\b(non)?" + originalWord, "$1" + newWord);
    }

    public static final String getSVar(final CardTraitBase ability, final String sVarName) {
        String val = ability.getSVar(sVarName);
        if (!ability.isIntrinsic() || StringUtils.isEmpty(val)) {
            return val;
        }
        return applyAbilityTextChangeEffects(val, ability);
    }

    private static void addPlayer(Iterable<?> objects, final String def, FCollection<Player> players) {
        addPlayer(objects, def, players, false);
    }

    private static void addPlayer(Iterable<?> objects, final String def, FCollection<Player> players, boolean skipRemembered) {
        for (Object o : objects) {
            if (o instanceof Player) {
                final Player p = (Player) o;
                if (def.endsWith("Opponents")) {
                    players.addAll(p.getOpponents());
                } else {
                    players.add(p);
                }
            } else if (o instanceof Card) {
                final Card c = (Card) o;
                if (def.endsWith("Controller")) {
                    players.add(c.getController());
                } else if (def.endsWith("Owner")) {
                    players.add(c.getOwner());
                } else if (def.endsWith("Remembered") && !skipRemembered) {
                    //fixme recursive call to skip so it will not cause StackOverflow, ie Riveteers Overlook
                    addPlayer(c.getRemembered(), def, players, true);
                }
            } else if (o instanceof SpellAbility) {
                final SpellAbility c = (SpellAbility) o;
                if (def.endsWith("Controller")) {
                    players.add(c.getHostCard().getController());
                }
            }
        }
    }

    public static SpellAbility addSpliceEffects(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player player = sa.getActivatingPlayer();

        if (!sa.isSpell() || source.isCopiedSpell()) {
            return sa;
        }

        final CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
        if (hand.isEmpty()) {
            return sa;
        }

        final CardCollection splices = CardLists.filter(hand, input -> {
            for (final KeywordInterface inst : input.getKeywords(Keyword.SPLICE)) {
                if (inst instanceof KeywordWithCostAndType splice) {
                    if (source.isValid(splice.getValidType().split(","), player, input, sa)) {
                        return true;
                    }
                }
            }
            return false;
        });

        splices.remove(source);

        if (splices.isEmpty()) {
            return sa;
        }

        final List<Card> choosen = player.getController().chooseCardsForSplice(sa, splices);
        if (choosen.isEmpty()) {
            return sa;
        }

        final SpellAbility newSA = sa.copy();
        for (final Card c : choosen) {
            addSpliceEffect(newSA, c);
        }
        return newSA;
    }

    public static void addSpliceEffect(final SpellAbility sa, final Card c) {
        Cost spliceCost = null;
        // This Function thinks that Splice exist only once on the card
        for (final KeywordInterface inst : c.getKeywords(Keyword.SPLICE)) {
            if (inst instanceof KeywordWithCostAndType splice) {
                spliceCost = splice.getCost();
                break;
            }
        }

        if (spliceCost == null)
            return;

        SpellAbility firstSpell = c.getFirstSpellAbility();
        Map<String, String> params = Maps.newHashMap(firstSpell.getMapParams());
        ApiType api = AbilityRecordType.getRecordType(params).getApiTypeOf(params);
        AbilitySub subAbility = (AbilitySub) AbilityFactory.getAbility(AbilityRecordType.SubAbility, api, params, null, c.getCurrentState(), c.getCurrentState());

        subAbility.setActivatingPlayer(sa.getActivatingPlayer());
        subAbility.setHostCard(sa.getHostCard());

        //add the spliced ability to the end of the chain
        sa.appendSubAbility(subAbility);

        // update master SpellAbility
        sa.setBasicSpell(false);
        sa.getPayCosts().add(spliceCost);
        sa.setDescription(sa.getDescription() + " (Splicing " + c + " onto it)");
        sa.addSplicedCards(c);
    }

    public static int doXMath(final int num, final String operators, final Card c, CardTraitBase ctb) {
        if (operators == null || operators.equals("none")) {
            return num;
        }

        final String[] s = operators.split("\\.");
        int secondaryNum = 0;

        try {
            if (s.length == 2) {
                secondaryNum = Integer.parseInt(s[1]);
            }
        } catch (final Exception e) {
            secondaryNum = calculateAmount(c, s[1], ctb);
        }

        if (s[0].contains("Plus")) {
            return num + secondaryNum;
        } else if (s[0].contains("NMinus")) {
            return secondaryNum - num;
        } else if (s[0].contains("Minus")) {
            return num - secondaryNum;
        } else if (s[0].contains("Twice")) {
            return num * 2;
        } else if (s[0].contains("Thrice")) {
            return num * 3;
        } else if (s[0].contains("HalfUp")) {
            return (int) Math.ceil(num / 2.0);
        } else if (s[0].contains("HalfDown")) {
            return (int) Math.floor(num / 2.0);
        } else if (s[0].contains("ThirdUp")) {
            return (int) Math.ceil(num / 3.0);
        } else if (s[0].contains("ThirdDown")) {
            return (int) Math.floor(num / 3.0);
        } else if (s[0].contains("Negative")) {
            return num * -1;
        } else if (s[0].contains("Times")) {
            return num * secondaryNum;
        } else if (s[0].contains("Pow")) {
            return (int) Math.pow(num, secondaryNum);
        } else if (s[0].contains("DivideEvenlyUp")) {
            if (secondaryNum == 0) {
                return 0;
            }
            return num / secondaryNum + (num % secondaryNum == 0 ? 0 : 1);
        } else if (s[0].contains("DivideEvenlyDown")) {
            if (secondaryNum == 0) {
                return 0;
            }
            return num / secondaryNum;
        } else if (s[0].contains("Mod")) {
            return num % secondaryNum;
        } else if (s[0].contains("Abs")) {
            return Math.abs(num);
        } else if (s[0].contains("LimitMax")) {
            if (num < secondaryNum) {
                return num;
            }
            return secondaryNum;
        } else if (s[0].contains("LimitMin")) {
            if (num > secondaryNum) {
                return num;
            }
            return secondaryNum;
        } else {
            return num;
        }
    }

    /**
     * <p>
     * Parse player targeted X variables.
     * </p>
     *
     * @param players
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int playerXCount(final List<Player> players, final String s, final Card source, CardTraitBase ctb) {
        if (players.isEmpty()) {
            return 0;
        }

        final String[] l = s.split("/");
        final String m = CardFactoryUtil.extractOperators(s);
        final Player controller = ctb instanceof SpellAbility ? ((SpellAbility)ctb).getActivatingPlayer() : source.getController();

        int n = 0;

        if (l[0].startsWith("TotalCommanderCastFromCommandZone")) {
            int totCast = 0;
            for (Player p : players) {
                totCast += p.getTotalCommanderCast();
            }
            return doXMath(totCast, m, source, ctb);
        }

        // methods for getting the highest/lowest playerXCount from a range of players
        if (l[0].startsWith("Highest")) {
            for (final Player player : players) {
                final int current = playerXProperty(player, TextUtil.fastReplace(s, "Highest", ""), source, ctb);
                if (current > n) {
                    n = current;
                }
            }

            return doXMath(n, m, source, ctb);
        }

        if (l[0].startsWith("Lowest")) {
            n = 99999; // if no players have fewer than 99999 valids, the game is frozen anyway
            for (final Player player : players) {
                final int current = playerXProperty(player, TextUtil.fastReplace(s, "Lowest", ""), source, ctb);
                if (current < n) {
                    n = current;
                }
            }
            return doXMath(n, m, source, ctb);
        }

        if (l[0].startsWith("TiedForHighestLife")) {
            int maxLife = Integer.MIN_VALUE;
            for (final Player player : players) {
                int highestTotal = playerXProperty(player, "LifeTotal", source, ctb);
                if (highestTotal > maxLife) {
                    maxLife = highestTotal;
                }
            }
            int numTied = 0;
            for (final Player player : players) {
                if (player.getLife() == maxLife) {
                    numTied++;
                }
            }
            return doXMath(numTied, m, source, ctb);
        }

        if (l[0].startsWith("TiedForLowestLife")) {
            int minLife = Integer.MAX_VALUE;
            for (final Player player : players) {
                int lowestTotal = playerXProperty(player, "LifeTotal", source, ctb);
                if (lowestTotal < minLife) {
                    minLife = lowestTotal;
                }
            }
            int numTied = 0;
            for (final Player player : players) {
                if (player.getLife() == minLife) {
                    numTied++;
                }
            }
            return doXMath(numTied, m, source, ctb);
        }

        // the number of players passed in
        if (l[0].equals("Amount")) {
            return doXMath(players.size(), m, source, ctb);
        }

        if (l[0].startsWith("HasProperty")) {
            int totPlayer = 0;
            String property = l[0].substring(11);
            for (Player p : players) {
                if (p.hasProperty(property, controller, source, ctb)) {
                    totPlayer++;
                }
            }
            return doXMath(totPlayer, m, source, ctb);
        }

        if (l[0].startsWith("Condition")) {
            int totPlayer = 0;
            String[] parts = l[0].split(" ", 2);
            boolean def = parts[0].equals("Condition");
            String comparator = def ? "GE" : parts[0].substring(9, 11);
            String calc = def ? "1" : parts[0].substring(11);
            Integer y = null;
            if (!ctb.getSVar(calc).contains("RelativePlayerUID")) {
                y = calculateAmount(source, calc, ctb);
            }
            for (Player p : players) {
                if (y == null) {
                    calc = ctb.getSVar(calc).replaceAll("RelativePlayerUID", String.valueOf(p.getId()));
                    y = calculateAmount(source, calc, ctb);
                }
                int x = playerXProperty(p, parts[1], source, ctb);
                if (Expressions.compare(x, comparator, y)) {
                    totPlayer++;
                }
            }
            return doXMath(totPlayer, m, source, ctb);
        }

        if (l[0].contains("DamageThisTurn")) {
            int totDmg = 0;
            for (Player p : players) {
                totDmg += p.getAssignedDamage();
            }
            return doXMath(totDmg, m, source, ctb);
        }

        if (players.size() > 0) {
            int totCount = 0;
            for (Player p : players) {
                totCount += playerXProperty(p, s, source, ctb);
            }
            return totCount;
        }

        return doXMath(n, m, source, ctb);
    }

    public static int playerXProperty(final Player player, final String s, final Card source, CardTraitBase ctb) {
        final String[] l = s.split("/");
        final String m = CardFactoryUtil.extractOperators(s);

        final Game game = player.getGame();

        // count valid cards on the battlefield
        if (l[0].startsWith("Valid ")) {
            final String restrictions = l[0].substring(6);
            int num = CardLists.getValidCardCount(game.getCardsIn(ZoneType.Battlefield), restrictions, player, source, ctb);
            return doXMath(num, m, source, ctb);
        }

        // count valid cards in any specified zone/s
        if (l[0].startsWith("Valid")) {
            String[] lparts = l[0].split(" ", 2);
            final List<ZoneType> vZone = ZoneType.listValueOf(lparts[0].split("Valid")[1]);
            String restrictions = TextUtil.fastReplace(l[0], TextUtil.addSuffix(lparts[0]," "), "");
            int num = CardLists.getValidCardCount(game.getCardsIn(vZone), restrictions, player, source, ctb);
            return doXMath(num, m, source, ctb);
        }

        if (l[0].startsWith("ThisTurnEntered")) {
            final String[] workingCopy = l[0].split("_");

            ZoneType destination = ZoneType.smartValueOf(workingCopy[1]);
            final boolean hasFrom = workingCopy[2].equals("from");
            ZoneType origin = hasFrom ? ZoneType.smartValueOf(workingCopy[3]) : null;
            String validFilter = workingCopy[hasFrom ? 4 : 2] ;

            final List<Card> res = CardUtil.getThisTurnEntered(destination, origin, validFilter, source, ctb, player);
            return doXMath(res.size(), m, source, ctb);
        }

        //SacrificedThisTurn <type>
        if (l[0].startsWith("SacrificedThisTurn")) {
            List<Card> list = player.getSacrificedThisTurn();
            if (l[0].contains(" ")) {
                String[] lparts = l[0].split(" ", 2);
                String restrictions = TextUtil.fastReplace(l[0], TextUtil.addSuffix(lparts[0]," "), "");
                list = CardLists.getValidCardsAsList(list, restrictions, player, source, ctb);
            }
            return doXMath(list.size(), m, source, ctb);
        }

        //SacrificedPermanentTypesThisTurn
        if (l[0].startsWith("SacrificedPermanentTypesThisTurn")) {
            return doXMath(countCardTypesFromList(player.getSacrificedThisTurn(), true), m, source, ctb);
        }

        final String[] sq = l[0].split("\\.");
        final String value = sq[0];

        if (value.contains("NumPowerSurgeLands")) {
            return doXMath(player.getNumPowerSurgeLands(), m, source, ctb);
        }

        if (value.contains("DomainPlayer")) {
            int n = 0;
            final CardCollectionView someCards = player.getLandsInPlay();
            final List<String> basic = MagicColor.Constant.BASIC_LANDS;

            for (String type : basic) {
                if (!CardLists.getType(someCards, type).isEmpty()) {
                    n++;
                }
            }
            return doXMath(n, m, source, ctb);
        }

        if (value.contains("CardsInHand")) {
            return doXMath(player.getCardsIn(ZoneType.Hand).size(), m, source, ctb);
        }

        if (value.contains("CardsInLibrary")) {
            return doXMath(player.getCardsIn(ZoneType.Library).size(), m, source, ctb);
        }

        if (value.contains("CardsInGraveyard")) {
            return doXMath(player.getCardsIn(ZoneType.Graveyard).size(), m, source, ctb);
        }
        if (value.contains("LandsInGraveyard")) {
            return doXMath(CardLists.getType(player.getCardsIn(ZoneType.Graveyard), "Land").size(), m, source, ctb);
        }

        if (value.contains("CardsInPlay")) {
            return doXMath(player.getCardsIn(ZoneType.Battlefield).size(), m, source, ctb);
        }
        if (value.contains("CreaturesInPlay")) {
            return doXMath(player.getCreaturesInPlay().size(), m, source, ctb);
        }

        if (value.contains("StartingLife")) {
            return doXMath(player.getStartingLife(), m, source, ctb);
        }

        if (value.contains("LifeTotal")) {
            return doXMath(player.getLife(), m, source, ctb);
        }

        if (value.contains("LifeLostThisTurn")) {
            return doXMath(player.getLifeLostThisTurn(), m, source, ctb);
        }
        if (value.contains("LifeLostLastTurn")) {
            return doXMath(player.getLifeLostLastTurn(), m, source, ctb);
        }

        if (value.contains("LifeGainedThisTurn")) {
            return doXMath(player.getLifeGainedThisTurn(), m, source, ctb);
        }

        if (value.contains("LifeGainedByTeamThisTurn")) {
            return doXMath(player.getLifeGainedByTeamThisTurn(), m, source, ctb);
        }

        if (value.contains("LifeStartedThisTurnWith")) {
            return doXMath(player.getLifeStartedThisTurnWith(), m, source, ctb);
        }

        if (value.contains("Speed")) {
            return doXMath(player.getSpeed(), m, source, ctb);
        }

        if (value.contains("SVarAmount")) {
            return doXMath(calculateAmount(source, ctb.getSVar(player.toString()), ctb), m, source, ctb);
        }

        if (value.contains("Counters")) {
            int count = 0;
            if (sq[1].equals("ALL")) {
                count = Aggregates.sum(player.getCounters().values());
            } else {
                count = player.getCounters(CounterType.getType(sq[1]));
            }
            return doXMath(count, m, source, ctb);
        }

        if (value.contains("TopOfLibraryCMC")) {
            return doXMath(Aggregates.sum(player.getCardsIn(ZoneType.Library, 1), Card::getCMC), m, source, ctb);
        }

        if (value.contains("LandsPlayed")) {
            return doXMath(player.getLandsPlayedThisTurn(), m, source, ctb);
        }

        if (value.contains("SpellsCastThisTurn")) {
            return doXMath(player.getSpellsCastThisTurn(), m, source, ctb);
        }

        if (value.contains("CardsDrawn")) {
            return doXMath(player.getNumDrawnThisTurn(), m, source, ctb);
        }

        if (value.contains("CardsDiscardedThisTurn")) {
            return doXMath(player.getDiscardedThisTurn().size(), m, source, ctb);
        }

        if (value.contains("ExploredThisTurn")) {
            return doXMath(player.getNumExploredThisTurn(), m, source, ctb);
        }

        if (value.contains("AttackersDeclared")) {
            return doXMath(player.getCreaturesAttackedThisTurn().size(), m, source, ctb);
        }

        if (value.contains("DamageToOppsThisTurn")) {
            return doXMath(player.getOpponentsAssignedDamage(), m, source, ctb);
        }

        if (value.contains("NonCombatDamageDealtThisTurn")) {
            return doXMath(player.getAssignedDamage() - player.getAssignedCombatDamage(), m, source, ctb);
        }

        if (value.equals("OpponentsAttackedThisTurn")) {
            final Iterable<Player> opps = player.getAttackedPlayersMyTurn();
            return doXMath(opps == null ? 0 : Iterables.size(opps), m, source, ctb);
        }

        if (value.equals("OpponentsAttackedThisCombat")) {
            int amount = game.getCombat() == null ? 0 : game.getCombat().getAttackedOpponents(player).size();
            return doXMath(amount, m, source, ctb);
        }

        if (value.equals("BeenDealtCombatDamageSinceLastTurn")) {
            return doXMath(player.hasBeenDealtCombatDamageSinceLastTurn() ? 1 : 0, m, source, ctb);
        }

        if (value.equals("RingTemptedYou")) {
            return doXMath(player.getNumRingTemptedYou(), m, source, ctb);
        }

        if (value.equals("AttractionsVisitedThisTurn")) {
            return doXMath(player.getAttractionsVisitedThisTurn(), m, source, ctb);
        }

        if (value.startsWith("PlaneswalkedToThisTurn")) {
            int found = 0;
            String name = value.split(" ")[1];
            List<Card> pwTo = player.getPlaneswalkedToThisTurn();
            for (Card c : pwTo) {
                if (c.getName().equals(name)) {
                    found++;
                    break;
                }
            }
            return doXMath(found, m, source, ctb);
        }

        return doXMath(0, m, source, ctb);
    }

    /**
     * <p>
     * Parse player targeted X variables.
     * </p>
     *
     * @param objects
     *            a {@link java.util.ArrayList} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int objectXCount(final List<?> objects, final String s, final Card source, CardTraitBase ctb) {
        if (objects.isEmpty()) {
            return 0;
        }

        if (s.startsWith("Valid")) {
            return handlePaid(IterableUtil.filter(objects, Card.class), s, source, ctb);
        }

        int n = s.startsWith("Amount") ? objects.size() : 0;
        return doXMath(n, CardFactoryUtil.extractOperators(s), source, ctb);
    }

    /**
     * <p>
     * handlePaid.
     * </p>
     *
     * @param paidList
     *            a {@link forge.game.card.CardCollectionView} object.
     * @param def
     *            a {@link java.lang.String} object.
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int handlePaid(final Iterable<Card> paidList, final String def, final Card source, final CardTraitBase ctb) {
        if (Iterables.isEmpty(paidList)) {
            return doXMath(0, CardFactoryUtil.extractOperators(def), source, ctb);
        }
        if (def.startsWith("Amount")) {
            return doXMath(Iterables.size(paidList), CardFactoryUtil.extractOperators(def), source, ctb);
        }

        if (def.startsWith("TapPowerValue")) {
            return CardLists.getTotalPower(paidList, ctb);
        }

        if (def.equals("Colors")) {
            return CardUtil.getColorsFromCards(paidList).countColors();
        }

        if (def.startsWith("DifferentCardNames")) {
            return doXMath(CardLists.getDifferentNamesCount(paidList), CardFactoryUtil.extractOperators(def), source, ctb);
        }

        if (def.equals("DifferentColorPair")) {
            final Set<ColorSet> diffPair = new HashSet<>();
            for (final Card card : paidList) {
                if (card.getColor().countColors() == 2) {
                    diffPair.add(card.getColor());
                }
            }
            return diffPair.size();
        }

        // shortcut to filter from Defined directly
        if (def.startsWith("Valid")) {
            final String[] splitString = def.split("/", 2);
            String valid = splitString[0].substring(6);
            final int num = CardLists.getValidCardCount(paidList, valid, source.getController(), source, ctb);
            return doXMath(num, splitString.length > 1 ? splitString[1] : null, source, ctb);
        }

        if (def.startsWith("AllTypes")) {
            return countCardTypesFromList(paidList, false) +
                    countSuperTypesFromList(paidList) +
                    countSubTypesFromList(paidList);
        }

        if (def.startsWith("CardTypes")) {
            return doXMath(countCardTypesFromList(paidList, def.startsWith("CardTypesPermanent")), CardFactoryUtil.extractOperators(def), source, ctb);
        }

        if (def.startsWith("CreatureType")) {
            final Set<String> creatTypes = Sets.newHashSet();
            for (Card card : paidList) {
                creatTypes.addAll(card.getType().getCreatureTypes());
            }
            // filter out fun types?
            return doXMath(creatTypes.size(), CardFactoryUtil.extractOperators(def), source, ctb);
        }

        Function<IntStream, Integer> func;
        String finalDef;
        if (def.startsWith("Least")) {
            func = s -> s.min().getAsInt();
            finalDef = def.substring(5);
        } else if (def.startsWith("Greatest")) {
            func = s -> s.max().getAsInt();
            finalDef = def.substring(8);
        } else if (def.startsWith("Different")) {
            func = s -> Math.toIntExact(s.distinct().count());
            finalDef = def.substring(9);
        } else {
            func = IntStream::sum;
            finalDef = def;
        }
        return func.apply(StreamUtil.stream(paidList).mapToInt(c -> xCount(c, finalDef, ctb)));
    }

    private static CardCollectionView getCardListForXCount(final Card c, final Player cc, final String[] sq, CardTraitBase ctb) {
        final List<Player> opps = cc.getOpponents();
        CardCollection someCards = new CardCollection();
        final Game game = c.getGame();

        // Generic Zone-based counting
        // Count$QualityAndZones.Subquality

        // build a list of cards in each possible specified zone

        if (sq[0].contains("YouCtrl")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Battlefield));
        }

        if (sq[0].contains("InYourYard")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Graveyard));
        }

        if (sq[0].contains("InYourLibrary")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Library));
        }

        if (sq[0].contains("InYourHand")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Hand));
        }

        if (sq[0].contains("InYourSideboard")) {
            someCards.addAll(cc.getCardsIn(ZoneType.Sideboard));
        }

        if (sq[0].contains("OppCtrl")) {
            for (final Player p : opps) {
                someCards.addAll(p.getZone(ZoneType.Battlefield).getCards());
            }
        }

        if (sq[0].contains("OnBattlefield")) {
            someCards.addAll(game.getCardsIn(ZoneType.Battlefield));
        }

        if (sq[0].contains("SpellsOnStack")) {
            someCards.addAll(game.getCardsIn(ZoneType.Stack));
        }

        if (sq[0].contains("InAllHands")) {
            someCards.addAll(game.getCardsIn(ZoneType.Hand));
        }

        // filter lists based on the specified quality

        // "Clerics you control" - Count$TypeYouCtrl.Cleric
        if (sq[0].contains("Type")) {
            someCards = CardLists.getType(someCards, sq[1]);
        }

        // "Named <CARDNAME> in all graveyards" - Count$NamedAllYards.<CARDNAME>

        if (sq[0].contains("Named")) {
            if (sq[1].equals("CARDNAME")) {
                sq[1] = c.getName();
            }
            someCards = CardLists.filter(someCards, CardPredicates.nameEquals(sq[1]));
        }

        // Refined qualities

        // "Untapped Lands" - Count$UntappedTypeYouCtrl.Land
        // if (sq[0].contains("Untapped")) { someCards = CardLists.filter(someCards, CardPredicates.UNTAPPED); }

        // if (sq[0].contains("Tapped")) { someCards = CardLists.filter(someCards, CardPredicates.TAPPED); }

//        String sq0 = sq[0].toLowerCase();
//        for (String color : MagicColor.Constant.ONLY_COLORS) {
//            if (sq0.contains(color))
//                someCards = someCards.filter(CardListFilter.WHITE);
//        }
        // "White Creatures" - Count$WhiteTypeYouCtrl.Creature
        // if (sq[0].contains("White")) someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.WHITE));
        // if (sq[0].contains("Blue"))  someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.BLUE));
        // if (sq[0].contains("Black")) someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.BLACK));
        // if (sq[0].contains("Red"))   someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.RED));
        // if (sq[0].contains("Green")) someCards = CardLists.filter(someCards, CardPredicates.isColor(MagicColor.GREEN));

        if (sq[0].contains("Multicolor")) {
            someCards = CardLists.filter(someCards, c1 -> c1.getColor().isMulticolor());
        }

        if (sq[0].contains("Monocolor")) {
            someCards = CardLists.filter(someCards, c12 -> c12.getColor().isMonoColor());
        }
        return someCards;
    }

    private static CardCollection getPaidCards(CardTraitBase sa, String defined) {
        CardCollection list = null;
        if (sa instanceof SpellAbility) {
            SpellAbility root = ((SpellAbility)sa).getRootAbility();
            list = root.getPaidList(defined, true);
        }
        return list;
    }

    public static int countCardTypesFromList(final Iterable<Card> list, boolean permanentTypes) {
        EnumSet<CardType.CoreType> types = EnumSet.noneOf(CardType.CoreType.class);
        for (Card c1 : list) {
            c1.getType().getCoreTypes().forEach(types::add);
        }
        if (permanentTypes)
            return (int) types.stream().filter(type -> type.isPermanent).count();
        return types.size();
    }

    public static int countSuperTypesFromList(final Iterable<Card> list) {
        EnumSet<CardType.Supertype> types = EnumSet.noneOf(CardType.Supertype.class);
        for (Card c1 : list) {
            c1.getType().getSupertypes().forEach(types::add);
        }

        return types.size();
    }

    public static int countSubTypesFromList(final Iterable<Card> list) {
        Set<String> types = new HashSet<>();
        for (Card c1 : list) {
            c1.getType().getSubtypes().forEach(types::add);
            c1.getType().getCreatureTypes().forEach(types::add);
        }

        return types.size();
    }

    /**
     * Checks if an ability source can be considered a "broken link" on a specific host
     * (which usually means it won't have its normal effect).
     * <br>
     * Because castSA gets used to compare it can only make a safe conclusion for
     * links that depend on stack decisions and can't be gained by other means
     * e.g. Kicker costs.
     *
     * @param ctb the source of the ability
     * @param card the host that it should be linked to
     * @return true if the ability can't be linked
     */
    public static boolean isUnlinkedFromCastSA(final CardTraitBase ctb, final Card card) {
        // check if it should come from same host
        if (ctb != null && ctb.isIntrinsic() && ctb.getHostCard().equals(card)) {
            Card host = ctb.getOriginalHost();
            SpellAbility castSA = card.getCastSA();
            if (host != null && castSA != null) {
                Card castHost = castSA.getOriginalHost();
                if (castHost == null) {
                    castHost = castSA.getHostCard();
                }
                // impossible to match with the other part when not even from same host
                if (!host.equals(castHost)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static CardTraitBase adjustTriggerContext(String[] def, final CardTraitBase ctb) {
        if (def[0].startsWith("Spawner>") && ctb instanceof SpellAbility) {
            Trigger trig = ((SpellAbility) ctb).getTrigger();
            if (trig == null) {
                return ctb;
            }
            SpellAbility spawner = trig.getSpawningAbility();
            if (spawner == null) {
                return ctb;
            }
            def[0] = def[0].substring(8);
            return spawner;
        }
        if (def[0].startsWith("TriggeredSpellAbility>") && ctb instanceof SpellAbility) {
            SpellAbility trig = (SpellAbility) ((SpellAbility) ctb).getTriggeringObject(AbilityKey.SpellAbility);
            if (trig == null) {
                return ctb;
            }
            def[0] = def[0].substring(22);
            return trig;
        }
        if (def[0].startsWith("CastSA>")) {
            SpellAbility sa = ctb.getHostCard().getCastSA();
            if (sa == null) {
                return ctb;
            }
            def[0] = def[0].substring(7);
            return sa;
        }
        return ctb;
    }
}
