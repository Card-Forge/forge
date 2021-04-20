package forge.game.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.card.mana.ManaCostShard;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityFactory.AbilityRecordType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CounterType;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityRestriction;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.SpellPermanent;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Expressions;
import forge.util.TextUtil;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;


public class AbilityUtils {
    private final static ImmutableList<String> cmpList = ImmutableList.of("LT", "LE", "EQ", "GE", "GT", "NE");

    public static CounterType getCounterType(String name, SpellAbility sa) throws Exception {
        CounterType counterType;
        if ("ReplacedCounterType".equals(name)) {
            name = (String) sa.getReplacingObject(AbilityKey.CounterType);
        }
        //try {
            counterType = CounterType.getType(name);
        /*
        } catch (Exception e) {
            String type = sa.getSVar(name);
            if (type.equals("")) {
                type = sa.getHostCard().getSVar(name);
            }

            if (type.equals("")) {
                throw new Exception("Counter type doesn't match, nor does an SVar exist with the type name.");
            }
            counterType = CounterType.getType(type);
        }
        //*/

        return counterType;
    }

    // should the three getDefined functions be merged into one? Or better to
    // have separate?
    // If we only have one, each function needs to Cast the Object to the
    // appropriate type when using
    // But then we only need update one function at a time once the casting is
    // everywhere.
    // Probably will move to One function solution sometime in the future
    public static CardCollection getDefinedCards(final Card hostCard, final String def, final CardTraitBase sa) {
        CardCollection cards = new CardCollection();
        String defined = (def == null) ? "Self" : applyAbilityTextChangeEffects(def, sa); // default to Self
        final String[] incR = defined.split("\\.", 2);
        defined = incR[0];
        final Game game = hostCard.getGame();

        Card c = null;

        if (defined.equals("Self")) {
            c = hostCard;
        } else if (defined.equals("CorrectedSelf")) {
            c = game.getCardState(hostCard);
        }
        else if (defined.equals("OriginalHost")) {
            if (sa instanceof SpellAbility) {
                c = ((SpellAbility)sa).getRootAbility().getOriginalHost();
            } else {
                c = sa.getOriginalHost();
            }
        }
        else if (defined.equals("EffectSource")) {
            if (hostCard.isEmblem() || hostCard.getType().hasSubtype("Effect")) {
                c = AbilityUtils.findEffectRoot(hostCard);
            }
        }
        else if (defined.equals("Equipped")) {
            c = hostCard.getEquipping();
        }
        else if (defined.startsWith("AttachedTo ")) {
            String v = defined.split(" ")[1];
            for (GameEntity ge : getDefinedEntities(hostCard, v, sa)) {
                // TODO handle phased out inside attachedCards
                Iterables.addAll(cards, ge.getAttachedCards());
            }
        }
        else if (defined.startsWith("AttachedBy ")) {
            String v = defined.split(" ")[1];
            for (Card attachment : getDefinedCards(hostCard, v, sa)) {
                Card attached = attachment.getAttachedTo();
                if (attached != null) {
                    cards.add(attached);
                }
            }
        }
        else if (defined.equals("Enchanted")) {
            c = hostCard.getEnchantingCard();
            if (c == null && sa instanceof SpellAbility) {
                SpellAbility root = ((SpellAbility)sa).getRootAbility();
                CardCollection sacrificed = root.getPaidList("Sacrificed");
                if (sacrificed != null && !sacrificed.isEmpty()) {
                    c = sacrificed.getFirst().getEnchantingCard();
                }
            }
        }
        else if (defined.endsWith("OfLibrary")) {
            final CardCollectionView lib = hostCard.getController().getCardsIn(ZoneType.Library);
            if (lib.size() > 0) { // TopOfLibrary or BottomOfLibrary
                c = lib.get(defined.startsWith("Top") ? 0 : lib.size() - 1);
            } else {
                // we don't want this to fall through and return the "Self"
                return cards;
            }
        }
        else if (defined.equals("Targeted") && sa instanceof SpellAbility) {
            final SpellAbility saTargeting = ((SpellAbility)sa).getSATargetingCard();
            if (saTargeting != null) {
                Iterables.addAll(cards, saTargeting.getTargets().getTargetCards());
            }
        }
        else if (defined.equals("TargetedSource") && sa instanceof SpellAbility) {
            final SpellAbility saTargeting = ((SpellAbility)sa).getSATargetingSA();
            if (saTargeting != null) {
                for (SpellAbility s : saTargeting.getTargets().getTargetSpells()) {
                    cards.add(s.getHostCard());
                }
            }
        }
        else if (defined.equals("ThisTargetedCard") && sa instanceof SpellAbility) { // do not add parent targeted
            if (((SpellAbility)sa).getTargets() != null) {
                Iterables.addAll(cards, ((SpellAbility)sa).getTargets().getTargetCards());
            }
        }
        else if (defined.equals("ParentTarget") && sa instanceof SpellAbility) {
            final SpellAbility parent = ((SpellAbility)sa).getParentTargetingCard();
            if (parent != null) {
                Iterables.addAll(cards, parent.getTargets().getTargetCards());
            }
        }
        else if (defined.startsWith("Triggered") && sa instanceof SpellAbility) {
            final SpellAbility root = ((SpellAbility)sa).getRootAbility();
            if (defined.contains("LKICopy")) { //Triggered*LKICopy
                int lkiPosition = defined.indexOf("LKICopy");
                AbilityKey type = AbilityKey.fromString(defined.substring(9, lkiPosition));
                final Object crd = root.getTriggeringObject(type);
                if (crd instanceof Card) {
                    c = (Card) crd;
                } else if (crd instanceof Iterable) {
                    cards.addAll(Iterables.filter((Iterable<?>) crd, Card.class));
                }
            }
            else if (defined.contains("HostCard")) { //Triggered*HostCard
                int hcPosition = defined.indexOf("HostCard");
                AbilityKey type = AbilityKey.fromString(defined.substring(9, hcPosition));
                final Object o = root.getTriggeringObject(type);
                if (o instanceof SpellAbility) {
                    c = ((SpellAbility) o).getHostCard();
                }
            }
            else {
                AbilityKey type = AbilityKey.fromString(defined.substring(9));
                final Object crd = root.getTriggeringObject(type);
                if (crd instanceof Card) {
                    c = game.getCardState((Card) crd);
                } else if (crd instanceof Iterable) {
                    cards.addAll(Iterables.filter((Iterable<?>) crd, Card.class));
                }
            }
        }
        else if (defined.startsWith("Replaced") && sa instanceof SpellAbility) {
            final SpellAbility root = ((SpellAbility)sa).getRootAbility();
            AbilityKey type = AbilityKey.fromString(defined.substring(8));
            final Object crd = root.getReplacingObject(type);

            if (crd instanceof Card) {
                c = game.getCardState((Card) crd);
            } else if (crd instanceof Iterable<?>) {
                cards.addAll(Iterables.filter((Iterable<?>) crd, Card.class));
            }
        }
        else if (defined.equals("Remembered") || defined.equals("RememberedCard")) {
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
        } else if (defined.equals("DelayTriggerRememberedLKI") && sa instanceof SpellAbility) {
            SpellAbility root = ((SpellAbility)sa).getRootAbility();
            if (root != null) {
                for (Object o : root.getTriggerRemembered()) {
                    if (o instanceof Card) {
                        cards.add((Card)o);
                    }
                }
            } else {
                System.err.println("Warning: couldn't find trigger SA in the chain of SpellAbility " + sa);
            }
        } else if (defined.equals("DelayTriggerRemembered") && sa instanceof SpellAbility) {
            SpellAbility root = ((SpellAbility)sa).getRootAbility();
            if (root != null) {
                for (Object o : root.getTriggerRemembered()) {
                    if (o instanceof Card) {
                        cards.addAll(addRememberedFromCardState(game, (Card)o));
                    }
                }
            } else {
                System.err.println("Warning: couldn't find trigger SA in the chain of SpellAbility " + sa);
            }
        } else if (defined.equals("FirstRemembered")) {
            Object o = Iterables.getFirst(hostCard.getRemembered(), null);
            if (o != null && o instanceof Card) {
                cards.add(game.getCardState((Card) o));
            }
        } else if (defined.equals("LastRemembered")) {
            Object o = Iterables.getLast(hostCard.getRemembered(), null);
            if (o != null && o instanceof Card) {
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
        } else if (defined.startsWith("ThisTurnEntered")) {
            final String[] workingCopy = defined.split("_");
            ZoneType destination, origin;
            String validFilter;

            destination = ZoneType.smartValueOf(workingCopy[1]);
            if (workingCopy[2].equals("from")) {
                origin = ZoneType.smartValueOf(workingCopy[3]);
                validFilter = workingCopy[4];
            } else {
                origin = null;
                validFilter = workingCopy[2];
            }
            for (final Card cl : CardUtil.getThisTurnEntered(destination, origin, validFilter, hostCard)) {
                Card gameState = game.getCardState(cl, null);
                // cards that use this should only care about if it is still in that zone
                // TODO if all LKI needs to be returned, need to change CardCollection return from this function
                if (gameState != null && gameState.equalsWithTimestamp(cl)) {
                    cards.add(gameState);
                }
            }
        } else if (defined.equals("ChosenCard")) {
            for (final Card chosen : hostCard.getChosenCards()) {
                cards.add(game.getCardState(chosen));
            }
        } else if (defined.startsWith("CardUID_")) {
            String idString = defined.substring(8);
            for (final Card cardByID : game.getCardsInGame()) {
                if (cardByID.getId() == Integer.valueOf(idString)) {
                    cards.add(game.getCardState(cardByID));
                }
            }
        } else {
            CardCollection list = null;
            if (sa instanceof SpellAbility) {
                SpellAbility root = ((SpellAbility)sa).getRootAbility();
                if (defined.startsWith("SacrificedCards")) {
                    list = root.getPaidList("SacrificedCards");
                } else if (defined.startsWith("Sacrificed")) {
                    list = root.getPaidList("Sacrificed");
                } else if (defined.startsWith("Revealed")) {
                    list = root.getPaidList("Revealed");
                } else if (defined.startsWith("DiscardedCards")) {
                    list = root.getPaidList("DiscardedCards");
                } else if (defined.startsWith("Discarded")) {
                    list = root.getPaidList("Discarded");
                } else if (defined.startsWith("ExiledCards")) {
                    list = root.getPaidList("ExiledCards");
                } else if (defined.startsWith("Exiled")) {
                    list = root.getPaidList("Exiled");
                } else if (defined.startsWith("Milled")) {
                    list = root.getPaidList("Milled");
                } else if (defined.startsWith("TappedCards")) {
                    list = root.getPaidList("TappedCards");
                } else if (defined.startsWith("Tapped")) {
                    list = root.getPaidList("Tapped");
                } else if (defined.startsWith("UntappedCards")) {
                    list = root.getPaidList("UntappedCards");
                } else if (defined.startsWith("Untapped")) {
                    list = root.getPaidList("Untapped");
                }
            }

            if (defined.startsWith("Valid ")) {
                String validDefined = defined.substring("Valid ".length());
                list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), validDefined.split(","), hostCard.getController(), hostCard, sa);
            } else if (defined.startsWith("ValidAll ")) {
                String validDefined = defined.substring("ValidAll ".length());
                list = CardLists.getValidCards(game.getCardsInGame(), validDefined.split(","), hostCard.getController(), hostCard, sa);
            } else if (defined.startsWith("Valid")) {
                String[] s = defined.split(" ");
                String zone = s[0].substring("Valid".length());
                String validDefined = s[1];
                list = CardLists.getValidCards(game.getCardsIn(ZoneType.smartValueOf(zone)), validDefined.split(","), hostCard.getController(), hostCard, sa);
            }

            if (list != null) {
                cards.addAll(list);
            }
        }

        if (c != null) {
            cards.add(c);
        }

        if (incR.length > 1 && !cards.isEmpty()) {
            final String excR = "Card." + incR[1];
            cards = CardLists.getValidCards(cards, excR.split(","), hostCard.getController(), hostCard, sa);
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
            if (cc.isEmblem() || cc.getType().hasSubtype("Effect")) {
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

    public static int calculateAmount(final Card card, String amount, final CardTraitBase ability, boolean maxto) {
        // return empty strings and constants
        if (StringUtils.isBlank(amount)) { return 0; }
        if (card == null) { return 0; }
        final Player player = card.getController();
        final Game game = player == null ? card.getGame() : player.getGame();

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
        calcX[1] = AbilityUtils.applyAbilityTextChangeEffects(calcX[1], ability);

        Integer val = null;
        if (calcX[0].startsWith("Count")) {
            val = AbilityUtils.xCount(card, calcX[1], ability);
        } else if (calcX[0].startsWith("Number")) {
            val = CardFactoryUtil.xCount(card, svarval);
        } else if (calcX[0].startsWith("SVar")) {
            final String[] l = calcX[1].split("/");
            final String m = CardFactoryUtil.extractOperators(calcX[1]);
            val = CardFactoryUtil.doXMath(AbilityUtils.calculateAmount(card, l[0], ability), m, card);
        } else if (calcX[0].startsWith("PlayerCount")) {
            final String hType = calcX[0].substring(11);
            final FCollection<Player> players = new FCollection<>();
            if (hType.equals("Players") || hType.equals("")) {
                players.addAll(game.getPlayers());
                val = CardFactoryUtil.playerXCount(players, calcX[1], card);
            }
            else if (hType.equals("YourTeam")) {
                players.addAll(player.getYourTeam());
                val = CardFactoryUtil.playerXCount(players, calcX[1], card);
            }
            else if (hType.equals("Opponents")) {
                players.addAll(player.getOpponents());
                val = CardFactoryUtil.playerXCount(players, calcX[1], card);
            }
            else if (hType.equals("RegisteredOpponents")) {
                players.addAll(Iterables.filter(game.getRegisteredPlayers(),PlayerPredicates.isOpponentOf(player)));
                val = CardFactoryUtil.playerXCount(players, calcX[1], card);
            }
            else if (hType.equals("Other")) {
                players.addAll(player.getAllOtherPlayers());
                val = CardFactoryUtil.playerXCount(players, calcX[1], card);
            }
            else if (hType.equals("Remembered")) {
                for (final Object o : card.getRemembered()) {
                    if (o instanceof Player) {
                        players.add((Player) o);
                    }
                }
                val = CardFactoryUtil.playerXCount(players, calcX[1], card);
            }
            else if (hType.equals("NonActive")) {
                players.addAll(game.getPlayers());
                players.remove(game.getPhaseHandler().getPlayerTurn());
                val = CardFactoryUtil.playerXCount(players, calcX[1], card);
            }
            else if (hType.startsWith("PropertyYou")) {
                if (ability instanceof SpellAbility) {
                    // Hollow One
                    players.add(((SpellAbility) ability).getActivatingPlayer());
                } else {
                    players.add(player);
                }
                val = CardFactoryUtil.playerXCount(players, calcX[1], card);
            }
            else if (hType.startsWith("Property")) {
                String defined = hType.split("Property")[1];
                for (Player p : game.getPlayersInTurnOrder()) {
                    if (ability instanceof SpellAbility && p.hasProperty(defined, ((SpellAbility) ability).getActivatingPlayer(), ability.getHostCard(), ability)) {
                        players.add(p);
                    } else if (!(ability instanceof SpellAbility) && p.hasProperty(defined, player, ability.getHostCard(), ability)) {
                        players.add(p);
                    }
                }
                val = CardFactoryUtil.playerXCount(players, calcX[1], card);
            }
            else {
                val = 0;
            }
        }

        if (val != null) {
            if (maxto) {
                val = Math.max(val, 0);
            }
            return val * multiplier;
        }

        if (calcX[0].equals("OriginalHost")) {
            return AbilityUtils.xCount(ability.getOriginalHost(), calcX[1], ability) * multiplier;
        }

        if (calcX[0].startsWith("Remembered")) {
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

            return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;
        }

        if (calcX[0].startsWith("Imprinted")) {
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

            return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;
        }

        if (calcX[0].matches("Enchanted")) {
            // Add whole Enchanted list to handlePaid
            final CardCollection list = new CardCollection();
            if (card.isEnchanting()) {
                Object o = card.getEntityAttachedTo();
                if (o instanceof Card) {
                    list.add(game.getCardState((Card) o));
                }
            }
            return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;
        }

        // All the following only work for SpellAbilities
        if (!(ability instanceof SpellAbility)) {
            return 0;
        }

        final SpellAbility sa = (SpellAbility) ability;
        if (calcX[0].startsWith("Modes")) {
            int chosenModes = 0;
            SpellAbility sub = sa;
            while(sub != null) {
                if (!sub.getSVar("CharmOrder").equals("")) {
                    chosenModes++;
                }
                sub = sub.getSubAbility();
            }
            // Count Math
            final String m = CardFactoryUtil.extractOperators(calcX[1]);
            return CardFactoryUtil.doXMath(chosenModes, m, card) * multiplier;
        }

        // Player attribute counting
        if (calcX[0].startsWith("TargetedPlayer")) {
            final List<Player> players = new ArrayList<>();
            final SpellAbility saTargeting = sa.getSATargetingPlayer();
            if (null != saTargeting) {
                Iterables.addAll(players, saTargeting.getTargets().getTargetPlayers());
            }
            return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
        }
        if (calcX[0].startsWith("ThisTargetedPlayer")) {
            final List<Player> players = new ArrayList<>();
            Iterables.addAll(players, sa.getTargets().getTargetPlayers());
            return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
        }
        if (calcX[0].startsWith("TargetedObjects")) {
            final List<GameObject> objects = new ArrayList<>();
            // Make list of all targeted objects starting with the root SpellAbility
            SpellAbility loopSA = sa.getRootAbility();
            while (loopSA != null) {
                if (loopSA.getTargetRestrictions() != null) {
                    Iterables.addAll(objects, loopSA.getTargets());
                }
                loopSA = loopSA.getSubAbility();
            }
            return CardFactoryUtil.objectXCount(objects, calcX[1], card) * multiplier;
        }
        if (calcX[0].startsWith("TargetedController")) {
            final List<Player> players = new ArrayList<>();
            final CardCollection list = getDefinedCards(card, "Targeted", sa);
            final List<SpellAbility> sas = AbilityUtils.getDefinedSpellAbilities(card, "Targeted", sa);

            for (final Card c : list) {
                final Player p = c.getController();
                if (!players.contains(p)) {
                    players.add(p);
                }
            }
            for (final SpellAbility s : sas) {
                final Player p = s.getHostCard().getController();
                if (!players.contains(p)) {
                    players.add(p);
                }
            }
            return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
        }
        if (calcX[0].startsWith("TargetedByTarget")) {
            final CardCollection tgtList = new CardCollection();
            final List<SpellAbility> saList = getDefinedSpellAbilities(card, "Targeted", sa);

            for (final SpellAbility s : saList) {
                tgtList.addAll(getDefinedCards(s.getHostCard(), "Targeted", s));
                // Check sub-abilities, so that modal cards like Abzan Charm are correctly handled.
                // TODO: Should this be done in a more general place, like in getDefinedCards()?
                AbilitySub abSub = s.getSubAbility();
                while (abSub != null) {
                    tgtList.addAll(getDefinedCards(abSub.getHostCard(), "Targeted", abSub));
                    abSub = abSub.getSubAbility();
                }
            }
            return CardFactoryUtil.handlePaid(tgtList, calcX[1], card) * multiplier;
        }
        if (calcX[0].startsWith("TriggeredPlayers") || calcX[0].equals("TriggeredCardController")) {
            String key = calcX[0];
            if (calcX[0].startsWith("TriggeredPlayers")) {
                key = "Triggered" + key.substring(16);
            }
            return CardFactoryUtil.playerXCount(getDefinedPlayers(card, key, sa), calcX[1], card) * multiplier;
        }
        if (calcX[0].startsWith("TriggeredPlayer") || calcX[0].startsWith("TriggeredTarget")) {
            final SpellAbility root = sa.getRootAbility();
            Object o = root.getTriggeringObject(AbilityKey.fromString(calcX[0].substring(9)));
            return o instanceof Player ? CardFactoryUtil.playerXProperty((Player) o, calcX[1], card) * multiplier : 0;
        }
        if (calcX[0].equals("TriggeredSpellAbility")) {
            final SpellAbility root = sa.getRootAbility();
            SpellAbility sat = (SpellAbility) root.getTriggeringObject(AbilityKey.SpellAbility);
            return calculateAmount(sat.getHostCard(), calcX[1], sat);
        }
        // Added on 9/30/12 (ArsenalNut) - Ended up not using but might be useful in future
        /*
        if (calcX[0].startsWith("EnchantedController")) {
            final ArrayList<Player> players = new ArrayList<Player>();
            players.addAll(AbilityFactory.getDefinedPlayers(card, "EnchantedController", ability));
            return CardFactoryUtil.playerXCount(players, calcX[1], card) * multiplier;
        }
         */

        Iterable<Card> list;
        if (calcX[0].startsWith("Sacrificed")) {
            list = sa.getRootAbility().getPaidList("Sacrificed");
        }
        else if (calcX[0].startsWith("Discarded")) {
            final SpellAbility root = sa.getRootAbility();
            list = root.getPaidList("Discarded");
            if ((null == list) && root.isTrigger()) {
                list = root.getHostCard().getSpellPermanent().getPaidList("Discarded");
            }
        }
        else if (calcX[0].startsWith("Exiled")) {
            list = sa.getRootAbility().getPaidList("Exiled");
        }
        else if (calcX[0].startsWith("Milled")) {
            list = sa.getRootAbility().getPaidList("Milled");
        }
        else if (calcX[0].startsWith("Tapped")) {
            list = sa.getRootAbility().getPaidList("Tapped");
        }
        else if (calcX[0].startsWith("Revealed")) {
            list = sa.getRootAbility().getPaidList("Revealed");
        }
        else if (calcX[0].startsWith("Targeted")) {
            list = sa.findTargetedCards();
        }
        else if (calcX[0].startsWith("ParentTargeted")) {
            SpellAbility parent = sa.getParentTargetingCard();
            if (parent != null) {
                list = parent.findTargetedCards();
            }
            else {
                list = null;
            }
        }
        else if (calcX[0].startsWith("TriggerRemembered")) {
            final SpellAbility root = sa.getRootAbility();
            list = Iterables.filter(root.getTriggerRemembered(), Card.class);
        }
        else if (calcX[0].startsWith("TriggerObjects")) {
            final SpellAbility root = sa.getRootAbility();
            list = Iterables.filter((Iterable<?>) root.getTriggeringObject(AbilityKey.fromString(calcX[0].substring(14))), Card.class);
        }
        else if (calcX[0].startsWith("Triggered")) {
            final SpellAbility root = sa.getRootAbility();
            list = new CardCollection((Card) root.getTriggeringObject(AbilityKey.fromString(calcX[0].substring(9))));
        }
        else if (calcX[0].startsWith("TriggerCount")) {
            // TriggerCount is similar to a regular Count, but just
            // pulls Integer Values from Trigger objects
            final SpellAbility root = sa.getRootAbility();
            final String[] l = calcX[1].split("/");
            final String m = CardFactoryUtil.extractOperators(calcX[1]);
            final Integer count = (Integer) root.getTriggeringObject(AbilityKey.fromString(l[0]));

            return CardFactoryUtil.doXMath(ObjectUtils.firstNonNull(count, 0), m, card) * multiplier;
        }
        else if (calcX[0].startsWith("Replaced")) {
            final SpellAbility root = sa.getRootAbility();
            list = new CardCollection((Card) root.getReplacingObject(AbilityKey.fromString(calcX[0].substring(8))));
        }
        else if (calcX[0].startsWith("ReplaceCount")) {
            // ReplaceCount is similar to a regular Count, but just
            // pulls Integer Values from Replacement objects
            final SpellAbility root = sa.getRootAbility();
            final String[] l = calcX[1].split("/");
            final String m = CardFactoryUtil.extractOperators(calcX[1]);
            final Integer count = (Integer) root.getReplacingObject(AbilityKey.fromString(l[0]));

            return CardFactoryUtil.doXMath(ObjectUtils.firstNonNull(count, 0), m, card) * multiplier;
        }
        else {
            return 0;
        }
        return CardFactoryUtil.handlePaid(list, calcX[1], card) * multiplier;
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
                    String varName = valid.split(",")[0].split(t)[1].split("\\+")[0];
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
        return CardLists.getValidCards(list, valid.split(","), sa.getActivatingPlayer(), source, sa);
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
    public static PlayerCollection getDefinedPlayers(final Card card, final String def, final CardTraitBase sa) {
        final PlayerCollection players = new PlayerCollection();
        final String defined = (def == null) ? "You" : applyAbilityTextChangeEffects(def, sa);
        final Game game = card == null ? null : card.getGame();

        final Player player = sa instanceof SpellAbility ? ((SpellAbility)sa).getActivatingPlayer() : card.getController();

        if (defined.equals("TargetedOrController")) {
            players.addAll(getDefinedPlayers(card, "Targeted", sa));
            players.addAll(getDefinedPlayers(card, "TargetedController", sa));
        }
        else if ((defined.equals("Targeted") || defined.equals("TargetedPlayer")) && sa instanceof SpellAbility) {
            final SpellAbility saTargeting = ((SpellAbility)sa).getSATargetingPlayer();
            if (saTargeting != null) {
                players.addAll(saTargeting.getTargets().getTargetPlayers());
            }
        }
        else if (defined.equals("ParentTarget") && sa instanceof SpellAbility) {
            final SpellAbility parent = ((SpellAbility)sa).getParentTargetingPlayer();
            if (parent != null) {
                players.addAll(parent.getTargets().getTargetPlayers());
            }
        }
        else if (defined.equals("ThisTargetedPlayer") && sa instanceof SpellAbility) { // do not add parent targeted
            if (((SpellAbility)sa).getTargets() != null) {
                Iterables.addAll(players, ((SpellAbility)sa).getTargets().getTargetPlayers());
            }
        }
        else if (defined.equals("TargetedController")) {
            for (final Card c : getDefinedCards(card, "Targeted", sa)) {
                players.add(c.getController());
            }
            for (final SpellAbility s : getDefinedSpellAbilities(card, "Targeted", sa)) {
                players.add(s.getActivatingPlayer());
            }
        }
        else if (defined.equals("TargetedOwner")) {
            for (final Card c : getDefinedCards(card, "Targeted", sa)) {
                players.add(c.getOwner());
            }
        }
        else if (defined.equals("TargetedAndYou") && sa instanceof SpellAbility) {
            final SpellAbility saTargeting = ((SpellAbility)sa).getSATargetingPlayer();
            if (saTargeting != null) {
                players.addAll(saTargeting.getTargets().getTargetPlayers());
                players.add(((SpellAbility)sa).getActivatingPlayer());
            }
        }
        else if (defined.equals("ParentTargetedController")) {
            for (final Card c : getDefinedCards(card, "ParentTarget", sa)) {
                players.add(c.getController());
            }
            for (final SpellAbility s : getDefinedSpellAbilities(card, "Targeted", sa)) {
                players.add(s.getActivatingPlayer());
            }
        }
        else if (defined.startsWith("Remembered")) {
            addPlayer(card.getRemembered(), defined, players);
        }
        else if (defined.startsWith("DelayTriggerRemembered") && sa instanceof SpellAbility) {
            SpellAbility root = ((SpellAbility)sa).getRootAbility();
            if (root != null) {
                addPlayer(root.getTriggerRemembered(), defined, players);
            } else {
                System.err.println("Warning: couldn't find trigger SA in the chain of SpellAbility " + sa);
            }
        }
        else if (defined.equals("ImprintedController")) {
            for (final Card rem : card.getImprintedCards()) {
                players.add(rem.getController());
            }
        }
        else if (defined.equals("ImprintedOwner")) {
            for (final Card rem : card.getImprintedCards()) {
                players.add(rem.getOwner());
            }
        }
        else if (defined.startsWith("Triggered") && sa instanceof SpellAbility) {
            String defParsed = defined.endsWith("AndYou") ? defined.substring(0, defined.indexOf("AndYou")) : defined;
            if (defined.endsWith("AndYou")) {
                players.add(((SpellAbility)sa).getActivatingPlayer());
            }
            final SpellAbility root = ((SpellAbility)sa).getRootAbility();
            Object o = null;
            if (defParsed.endsWith("Controller")) {
                String triggeringType = defParsed.substring(9);
                triggeringType = triggeringType.substring(0, triggeringType.length() - 10);
                final Object c = root.getTriggeringObject(AbilityKey.fromString(triggeringType));
                if (c instanceof Card) {
                    o = ((Card) c).getController();
                }
                if (c instanceof SpellAbility) {
                    o = ((SpellAbility) c).getActivatingPlayer();
                }
                // For merged permanent
                if (c instanceof CardCollection) {
                    o = ((CardCollection) c).get(0).getController();
                }
            }
            else if (defParsed.endsWith("Opponent")) {
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
            }
            else if (defParsed.endsWith("Owner")) {
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
                final String triggeringType = defParsed.substring(9);
                o = root.getTriggeringObject(AbilityKey.fromString(triggeringType));
            }
            if (o != null) {
                if (o instanceof Player) {
                    players.add((Player) o);
                }
                if (o instanceof Iterable) {
                    players.addAll(Iterables.filter((Iterable<?>)o, Player.class));
                }
            }
        }
        else if (defined.startsWith("OppNon")) {
            players.addAll(player.getOpponents());
            players.removeAll(getDefinedPlayers(card, defined.substring(6), sa));
        }
        else if (defined.startsWith("Replaced") && sa instanceof SpellAbility) {
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
            }
            else if (defined.endsWith("Owner")) {
                String replacingType = defined.substring(8);
                replacingType = replacingType.substring(0, replacingType.length() - 5);
                final Object c = root.getReplacingObject(AbilityKey.fromString(replacingType));
                if (c instanceof Card) {
                    o = ((Card) c).getOwner();
                }
            }
            else {
                final String replacingType = defined.substring(8);
                o = root.getReplacingObject(AbilityKey.fromString(replacingType));
            }
            if (o != null) {
                if (o instanceof Player) {
                    players.add((Player) o);
                }
            }
        }
        else if (defined.startsWith("Non")) {
            players.addAll(game.getPlayersInTurnOrder());
            players.removeAll((FCollectionView<Player>)getDefinedPlayers(card, defined.substring(3), sa));
        }
        else if (defined.equals("EnchantedController")) {
            if (card.getEnchantingCard() == null) {
                return players;
            }
            players.add(card.getEnchantingCard().getController());
        }
        else if (defined.equals("EnchantedOwner")) {
            if (card.getEnchantingCard() == null) {
                return players;
            }
            players.add(card.getEnchantingCard().getOwner());
        }
        else if (defined.equals("EnchantedPlayer")) {
            final Object o = sa.getHostCard().getEntityAttachedTo();
            if (o instanceof Player) {
                players.add((Player) o);
            }
        }
        else if (defined.equals("AttackingPlayer")) {
            if (!game.getPhaseHandler().inCombat()) {
                return players;
            }
            players.add(game.getCombat().getAttackingPlayer());
        }
        else if (defined.equals("DefendingPlayer")) {
            players.add(game.getCombat().getDefendingPlayerRelatedTo(card));
        }
        else if (defined.equals("OpponentsOtherThanDefendingPlayer")) {
            players.addAll(player.getOpponents());
            players.remove(game.getCombat().getDefendingPlayerRelatedTo(card));
        }
        else if (defined.equals("ChosenPlayer")) {
            final Player p = card.getChosenPlayer();
            if (p != null) {
                players.add(p);
            }
        }
        else if (defined.equals("ChosenAndYou")) {
            players.add(player);
            final Player p = card.getChosenPlayer();
            if (p != null) {
                players.add(p);
            }
        }
        else if (defined.equals("ChosenCardController")) {
            for (final Card chosen : card.getChosenCards()) {
                players.add(game.getCardState(chosen).getController());
            }
        }
        else if (defined.equals("SourceController")) {
            players.add(sa.getHostCard().getController());
        }
        else if (defined.equals("CardController")) {
            players.add(card.getController());
        }
        else if (defined.equals("CardOwner")) {
            players.add(card.getOwner());
        }
        else if (defined.startsWith("PlayerNamed_")) {
            for (Player p : game.getPlayersInTurnOrder()) {
                //System.out.println("Named player " + defined.substring(12));
                if (p.getName().equals(defined.substring(12))) {
                    players.add(p);
                }
            }
        }
        else if (defined.startsWith("Flipped")) {
            for (Player p : game.getPlayersInTurnOrder()) {
                if (null != sa.getHostCard().getFlipResult(p)) {
                    if (sa.getHostCard().getFlipResult(p).equals(defined.substring(7))) {
                        players.add(p);
                    }
                }
            }
        }
        else if (defined.equals("ActivePlayer")) {
            players.add(game.getPhaseHandler().getPlayerTurn());
        }
        else if (defined.equals("You")) {
            players.add(player);
        }
        else if (defined.equals("Opponent")) {
            players.addAll(player.getOpponents());
        }
        else {
            for (Player p : game.getPlayersInTurnOrder()) {
                if (p.isValid(defined, player, card, sa)) {
                    players.add(p);
                }
            }
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
    public static FCollection<SpellAbility> getDefinedSpellAbilities(final Card card, final String def,
            final CardTraitBase sa) {
        final FCollection<SpellAbility> sas = new FCollection<>();
        final String defined = (def == null) ? "Self" : applyAbilityTextChangeEffects(def, sa); // default to Self
        final Game game = card.getGame();

        SpellAbility s = null;

        // TODO - this probably needs to be fleshed out a bit, but the basics
        // work
        if (defined.equals("Self") && sa instanceof SpellAbility) {
            s = (SpellAbility)sa;
        }
        else if (defined.equals("Parent") && sa instanceof SpellAbility) {
            s = ((SpellAbility)sa).getRootAbility();
        }
        else if (defined.equals("Targeted") && sa instanceof SpellAbility) {
            final SpellAbility saTargeting = ((SpellAbility)sa).getSATargetingSA();
            if (saTargeting != null) {
                for (SpellAbility targetSpell : saTargeting.getTargets().getTargetSpells()) {
                    SpellAbilityStackInstance stackInstance = game.getStack().getInstanceFromSpellAbility(targetSpell);
                    if (stackInstance != null) {
                        SpellAbility instanceSA = stackInstance.getSpellAbility(true);
                        if (instanceSA != null) {
                            sas.add(instanceSA);
                        }
                    }
                    else {
                        sas.add(targetSpell);
                    }
                }
            }
        }
        else if (defined.startsWith("Triggered") && sa instanceof SpellAbility) {
            final SpellAbility root = ((SpellAbility)sa).getRootAbility();

            final String triggeringType = defined.substring(9);
            final Object o = root.getTriggeringObject(AbilityKey.fromString(triggeringType));
            if (o instanceof SpellAbility) {
                s = (SpellAbility) o;
                // if there is no target information in SA but targets are listed in SpellAbilityTargeting cards, copy that
                // information so it's not lost if the calling code is interested in targets of the triggered SA.
                if (triggeringType.equals("SpellAbility")) {
                    final CardCollectionView tgtList = (CardCollectionView)root.getTriggeringObject(AbilityKey.SpellAbilityTargetingCards);
                    if (s.getTargets() != null && s.getTargets().size() == 0) {
                        if (tgtList != null && tgtList.size() > 0) {
                            TargetChoices tc = new TargetChoices();
                            for (Card c : tgtList) {
                                tc.add(c);
                            }
                            s.setTargets(tc);
                        }
                    }
                }
            } else if (o instanceof SpellAbilityStackInstance) {
                s = ((SpellAbilityStackInstance) o).getSpellAbility(true);
            }
        }
        else if (defined.equals("Remembered")) {
            for (final Object o : card.getRemembered()) {
                if (o instanceof Card) {
                    final Card rem = (Card) o;
                    sas.addAll(game.getCardState(rem).getSpellAbilities());
                } else if (o instanceof SpellAbility) {
                    sas.add((SpellAbility) o);
                }
            }
        }
        else if (defined.equals("Imprinted")) {
            for (final Card imp : card.getImprintedCards()) {
                sas.addAll(imp.getSpellAbilities());
            }
        }
        else if (defined.equals("EffectSource")) {
            if (card.getEffectSourceAbility() != null) {
                sas.add(card.getEffectSourceAbility().getRootAbility());
            }
        }
        else if (defined.equals("SourceFirstSpell")) {
            sas.add(card.getFirstSpellAbility());
        }

        if (s != null) {
            sas.add(s);
        }

        return sas;
    }


    /////////////////////////////////////////////////////////////////////////////////////
    //
    // BELOW ARE resove() METHOD AND ITS DEPENDANTS, CONSIDER MOVING TO DEDICATED CLASS
    //
    /////////////////////////////////////////////////////////////////////////////////////
    public static void resolve(final SpellAbility sa) {
        if (sa == null) {
            return;
        }

        Player pl = sa.getActivatingPlayer();
        final Game game = pl.getGame();

        if (sa.isTrigger() && sa.getParent() == null) {
            // when trigger cost are paid before the effect does resolve, need to clean the trigger
            game.getTriggerHandler().resetActiveTriggers();
        }

        // do blessing there before condition checks
        if (sa.isSpell() && sa.isBlessing() && !sa.getHostCard().isPermanent()) {
            if (pl != null && pl.getZone(ZoneType.Battlefield).size() >= 10) {
                pl.setBlessing(true);
            }
        }

        // count times ability resolves this turn
        if (!sa.isWrapper()) {
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

        AbilityUtils.resolveApiAbility(sa, game);
    }

    private static void resolveSubAbilities(final SpellAbility sa, final Game game) {
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub == null || sa.isWrapper()) {
            return;
        }

        // Needed - Equip an untapped creature with Sword of the Paruns then cast Deadshot on it. Should deal 2 more damage.
        game.getAction().checkStaticAbilities(); // this will refresh continuous abilities for players and permanents.
        if (sa.isReplacementAbility() && abSub.getApi() == ApiType.InternalEtbReplacement) {
            game.getTriggerHandler().resetActiveTriggers(false);
        } else {
            game.getTriggerHandler().resetActiveTriggers();
        }
        AbilityUtils.resolveApiAbility(abSub, game);
    }

    private static void resolveApiAbility(final SpellAbility sa, final Game game) {
        final Card card = sa.getHostCard();

        String msg = "AbilityUtils:resolveApiAbility: try to resolve API ability";
        Sentry.getContext().recordBreadcrumb(
                new BreadcrumbBuilder().setMessage(msg)
                .withData("Api", sa.getApi().toString())
                .withData("Card", card.getName()).withData("SA", sa.toString()).build()
        );

        // check conditions
        if (sa.metConditions()) {
            if (sa.isWrapper() || StringUtils.isBlank(sa.getParam("UnlessCost"))) {
                sa.resolve();
            }
            else {
                handleUnlessCost(sa, game);
                return;
            }
        }
        resolveSubAbilities(sa, game);
    }

    private static void handleUnlessCost(final SpellAbility sa, final Game game) {
        final Card source = sa.getHostCard();

        // The player who has the chance to cancel the ability
        final String pays = sa.hasParam("UnlessPayer") ? sa.getParam("UnlessPayer") : "TargetedController";
        final FCollectionView<Player> allPayers = getDefinedPlayers(sa.getHostCard(), pays, sa);
        final String  resolveSubs = sa.getParam("UnlessResolveSubs"); // no value means 'Always'
        final boolean execSubsWhenPaid = "WhenPaid".equals(resolveSubs) || StringUtils.isBlank(resolveSubs);
        final boolean execSubsWhenNotPaid = "WhenNotPaid".equals(resolveSubs) || StringUtils.isBlank(resolveSubs);
        final boolean isSwitched = sa.hasParam("UnlessSwitched");

        // The cost
        Cost cost;
        String unlessCost = sa.getParam("UnlessCost").trim();
        if (unlessCost.equals("CardManaCost")) {
            cost = new Cost(source.getManaCost(), true);
        }
        else if (unlessCost.equals("TriggeredSpellManaCost")) {
            SpellAbility triggered = (SpellAbility) sa.getRootAbility().getTriggeringObject(AbilityKey.SpellAbility);
            Card triggeredCard = triggered.getHostCard();
            if (triggeredCard.getManaCost() == null) {
                cost = new Cost(ManaCost.ZERO, true);
            } else {
                int xCount = triggeredCard.getManaCost().countX();
                int xPaid = triggeredCard.getXManaCostPaid() * xCount;
                ManaCostBeingPaid toPay = new ManaCostBeingPaid(triggeredCard.getManaCost());
                toPay.decreaseShard(ManaCostShard.X, xCount);
                toPay.increaseGenericMana(xPaid);
                cost = new Cost(toPay.toManaCost(), true);
            }
        }
        else if (unlessCost.equals("ChosenManaCost")) {
            if (!source.hasChosenCard()) {
                cost = new Cost(ManaCost.ZERO, true);
            }
            else {
                cost = new Cost(Iterables.getFirst(source.getChosenCards(), null).getManaCost(), true);
            }
        }
        else if (unlessCost.equals("ChosenNumber")) {
            cost = new Cost(new ManaCost(new ManaCostParser(String.valueOf(source.getChosenNumber()))), true);
        }
        else if (unlessCost.startsWith("DefinedCost")) {
            CardCollection definedCards = AbilityUtils.getDefinedCards(sa.getHostCard(), unlessCost.split("_")[1], sa);
            if (definedCards.isEmpty()) {
                sa.resolve();
                resolveSubAbilities(sa, game);
                return;
            }
            Card card = definedCards.getFirst();
            ManaCostBeingPaid newCost = new ManaCostBeingPaid(card.getManaCost());
            // Check if there's a third underscore for cost modifying
            if (unlessCost.split("_").length == 3) {
                String modifier = unlessCost.split("_")[2];
                if (modifier.startsWith("Minus")) {
                    newCost.decreaseGenericMana(Integer.parseInt(modifier.substring(5)));
                } else {
                    newCost.increaseGenericMana(Integer.parseInt(modifier.substring(4)));
                }
            }
            cost = new Cost(newCost.toManaCost(), true);
        }
        else if (unlessCost.startsWith("DefinedSACost")) {
            FCollection<SpellAbility> definedSAs = AbilityUtils.getDefinedSpellAbilities(sa.getHostCard(), unlessCost.split("_")[1], sa);
            if (definedSAs.isEmpty()) {
                sa.resolve();
                resolveSubAbilities(sa, game);
                return;
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
        else if (!StringUtils.isBlank(sa.getSVar(unlessCost)) || !StringUtils.isBlank(source.getSVar(unlessCost))) {
            // check for X costs (stored in SVars
            int xCost = calculateAmount(source, TextUtil.fastReplace(sa.getParam("UnlessCost"),
                    " ", ""), sa);
            //Check for XColor
            ManaCostBeingPaid toPay = new ManaCostBeingPaid(ManaCost.ZERO);
            byte xColor = ManaAtom.fromName(sa.hasParam("UnlessXColor") ? sa.getParam("UnlessXColor") : "1");
            toPay.increaseShard(ManaCostShard.valueOf(xColor), xCost);
            cost = new Cost(toPay.toManaCost(), true);
        }
        else {
            cost = new Cost(unlessCost, true);
        }

        boolean alreadyPaid = false;
        for (Player payer : allPayers) {
            if (unlessCost.equals("LifeTotalHalfUp")) {
                String halfup = Integer.toString((int) Math.ceil(payer.getLife() / 2.0));
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

        if (sa.hasParam("RememberTargets") && sa.getTargetRestrictions() != null) {
            if (sa.hasParam("ForgetOtherTargets")) {
                host.clearRemembered();
            }
            for (final GameObject o : sa.getTargets()) {
                host.addRemembered(o);
            }
        }

        if (sa.hasParam("ImprintTargets") && sa.getTargetRestrictions() != null) {
            for (final Card c : sa.getTargets().getTargetCards()) {
                host.addImprintedCard(c);
            }
        }

        if (sa.hasParam("RememberCostMana")) {
            host.clearRemembered();
            host.addRemembered(sa.getPayingMana());
        }

        if (sa.hasParam("RememberCostCards") && !sa.getPaidHash().isEmpty()) {
            if (sa.getParam("Cost").contains("Exile")) {
                final CardCollection paidListExiled = sa.getPaidList("Exiled");
                for (final Card exiledAsCost : paidListExiled) {
                    host.addRemembered(exiledAsCost);
                }
            }
            else if (sa.getParam("Cost").contains("Sac")) {
                final CardCollection paidListSacrificed = sa.getPaidList("Sacrificed");
                for (final Card sacrificedAsCost : paidListSacrificed) {
                    host.addRemembered(sacrificedAsCost);
                }
            }
            else if (sa.getParam("Cost").contains("tapXType")) {
                final CardCollection paidListTapped = sa.getPaidList("Tapped");
                for (final Card tappedAsCost : paidListTapped) {
                    host.addRemembered(tappedAsCost);
                }
            }
            else if (sa.getParam("Cost").contains("Unattach")) {
                final CardCollection paidListUnattached = sa.getPaidList("Unattached");
                for (final Card unattachedAsCost : paidListUnattached) {
                    host.addRemembered(unattachedAsCost);
                }
            }
            else if (sa.getParam("Cost").contains("Discard")) {
                final CardCollection paidListDiscarded = sa.getPaidList("Discarded");
                for (final Card discardedAsCost : paidListDiscarded) {
                    host.addRemembered(discardedAsCost);
                }
            }
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
    public static int xCount(final Card c, final String s, final CardTraitBase ctb) {
        final String s2 = AbilityUtils.applyAbilityTextChangeEffects(s, ctb);
        final String[] l = s2.split("/");
        final String expr = CardFactoryUtil.extractOperators(s2);
        final Player player = ctb == null ? null : ctb instanceof SpellAbility ? ((SpellAbility)ctb).getActivatingPlayer() : ctb.getHostCard().getController();

        final String[] sq;
        sq = l[0].split("\\.");

        final Game game = c.getGame();

        if (ctb != null) {
            // Count$Compare <int comparator value>.<True>.<False>
            if (sq[0].startsWith("Compare")) {
                final String[] compString = sq[0].split(" ");
                final int lhs = calculateAmount(c, compString[1], ctb);
                final int rhs =  calculateAmount(c, compString[2].substring(2), ctb);
                if (Expressions.compare(lhs, compString[2], rhs)) {
                    return CardFactoryUtil.doXMath(calculateAmount(c, sq[1], ctb), expr, c);
                }
                else {
                    return CardFactoryUtil.doXMath(calculateAmount(c, sq[2], ctb), expr, c);
                }
            }
            if (ctb instanceof SpellAbility) {
                final SpellAbility sa = (SpellAbility) ctb;

                // special logic for xPaid in SpellAbility
                if (sq[0].contains("xPaid")) {
                    SpellAbility root = sa.getRootAbility();

                    // 107.3i If an object gains an ability, the value of X within that ability is the value defined by that ability,
                    // or 0 if that ability doesn’t define a value of X. This is an exception to rule 107.3h. This may occur with ability-adding effects, text-changing effects, or copy effects.
                    if (root.getXManaCostPaid() != null) {
                        return CardFactoryUtil.doXMath(root.getXManaCostPaid(), expr, c);
                    }

                    if (root.isTrigger()) {
                        Trigger t = root.getTrigger();
                        if (t == null) {
                            return CardFactoryUtil.doXMath(0, expr, c);
                        }

                        // ImmediateTrigger should check for the Ability which created the trigger
                        if (t.getSpawningAbility() != null) {
                            root = t.getSpawningAbility().getRootAbility();
                            return CardFactoryUtil.doXMath(root.getXManaCostPaid(), expr, c);
                        }

                        // 107.3k If an object’s enters-the-battlefield triggered ability or replacement effect refers to X,
                        // and the spell that became that object as it resolved had a value of X chosen for any of its costs,
                        // the value of X for that ability is the same as the value of X for that spell, although the value of X for that permanent is 0.
                        if (TriggerType.ChangesZone.equals(t.getMode())
                                && ZoneType.Battlefield.name().equals(t.getParam("Destination"))) {
                           return CardFactoryUtil.doXMath(c.getXManaCostPaid(), expr, c);
                        } else if (TriggerType.SpellCast.equals(t.getMode())) {
                            // Cast Trigger like  Hydroid Krasis
                            SpellAbility castSA = (SpellAbility) root.getTriggeringObject(AbilityKey.SpellAbility);
                            if (castSA == null || castSA.getXManaCostPaid() == null) {
                                return CardFactoryUtil.doXMath(0, expr, c);
                            }
                            return CardFactoryUtil.doXMath(castSA.getXManaCostPaid(), expr, c);
                        } else if (TriggerType.Cycled.equals(t.getMode())) {
                            SpellAbility cycleSA = (SpellAbility) sa.getTriggeringObject(AbilityKey.Cause);
                            if (cycleSA == null || cycleSA.getXManaCostPaid() == null) {
                                return CardFactoryUtil.doXMath(0, expr, c);
                            }
                            return CardFactoryUtil.doXMath(cycleSA.getXManaCostPaid(), expr, c);
                        } else if (TriggerType.TurnFaceUp.equals(t.getMode())) {
                            SpellAbility turnupSA = (SpellAbility) sa.getTriggeringObject(AbilityKey.Cause);
                            if (turnupSA == null || turnupSA.getXManaCostPaid() == null) {
                                return CardFactoryUtil.doXMath(0, expr, c);
                            }
                            return CardFactoryUtil.doXMath(turnupSA.getXManaCostPaid(), expr, c);
                        }
                    }

                    // If the chosen creature has X in its mana cost, that X is considered to be 0.
                    // The value of X in Altered Ego’s last ability will be whatever value was chosen for X while casting Altered Ego.
                    if (sa.isCopiedTrait() || !sa.getHostCard().equals(c)) {
                        return CardFactoryUtil.doXMath(0, expr, c);
                    }

                    if (root.isReplacementAbility()) {
                        if (sa.hasParam("ETB")) {
                            return CardFactoryUtil.doXMath(c.getXManaCostPaid(), expr, c);
                        }
                    }

                    return CardFactoryUtil.doXMath(0, expr, c);
                }

                // Count$Kicked.<numHB>.<numNotHB>
                if (sq[0].startsWith("Kicked")) {
                    boolean kicked = sa.isKicked() || c.getKickerMagnitude() > 0;
                    return CardFactoryUtil.doXMath(Integer.parseInt(kicked ? sq[1] : sq[2]), expr, c);
                }

                // Count$UrzaLands.<numHB>.<numNotHB>
                if (sq[0].startsWith("UrzaLands")) {
                    return CardFactoryUtil.doXMath(Integer.parseInt(sa.getActivatingPlayer().hasUrzaLands() ? sq[1] : sq[2]), expr, c);
                }

                //Count$SearchedLibrary.<DefinedPlayer>
                if (sq[0].contains("SearchedLibrary")) {
                    int sum = 0;
                    for (Player p : AbilityUtils.getDefinedPlayers(c, sq[1], sa)) {
                        sum += p.getLibrarySearched();
                    }

                    return sum;
                }
                //Count$HasNumChosenColors.<DefinedCards related to spellability>
                if (sq[0].contains("HasNumChosenColors")) {
                    int sum = 0;
                    for (Card card : AbilityUtils.getDefinedCards(sa.getHostCard(), sq[1], sa)) {
                        sum += CardUtil.getColors(card).getSharedColors(ColorSet.fromNames(c.getChosenColors())).countColors();
                    }
                    return sum;
                }
                if (sq[0].startsWith("TriggerRememberAmount")) {
                    SpellAbility root = sa.getRootAbility();
                    int count = 0;
                    for (final Object o : root.getTriggerRemembered()) {
                        if (o instanceof Integer) {
                            count += (Integer) o;
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
                // Count$AttachedTo <DefinedCards related to spellability> <restriction>
                if (sq[0].startsWith("AttachedTo")) {
                    final String[] k = l[0].split(" ");
                    int sum = 0;
                    for (Card card : AbilityUtils.getDefinedCards(sa.getHostCard(), k[1], sa)) {
                        // Hateful Eidolon: the script uses LKI so that the attached cards have to be defined
                        // This card needs the spellability ("Auras You control",  you refers to the activating player)
                        // CardFactoryUtils.xCount doesn't have the sa parameter, SVar:X:TriggeredCard$Valid <restriction> cannot handle this
                        CardCollection list = CardLists.getValidCards(card.getAttachedCards(), k[2].split(","), sa.getActivatingPlayer(), c, sa);
                        sum += list.size();
                    }
                    return sum;
                }
                // Count$Adamant.<Color>.<True>.<False>
                if (sq[0].startsWith("Adamant")) {
                    final String payingMana = StringUtils.join(sa.getRootAbility().getPayingMana());
                    final int num = sq[0].length() > 7 ? Integer.parseInt(sq[0].split("_")[1]) : 3;
                    final boolean adamant = StringUtils.countMatches(payingMana, MagicColor.toShortString(sq[1])) >= num;
                    return CardFactoryUtil.doXMath(Integer.parseInt(sq[adamant ? 2 : 3]), expr, c);
                }

                if (l[0].startsWith("LastStateBattlefield")) {
                    final String[] k = l[0].split(" ");
                    CardCollectionView list = null;
                    if (sa.getLastStateBattlefield() != null) {
                        list = sa.getLastStateBattlefield();
                    } else { // LastState is Empty
                        return CardFactoryUtil.doXMath(0, expr, c);
                    }
                    list = CardLists.getValidCards(list, k[1].split(","), sa.getActivatingPlayer(), c, sa);
                    if (k[0].contains("TotalToughness")) {
                        return CardFactoryUtil.doXMath(Aggregates.sum(list, CardPredicates.Accessors.fnGetNetToughness), expr, c);
                    } else {
                        return CardFactoryUtil.doXMath(list.size(), expr, c);
                    }
                }

                if (l[0].startsWith("LastStateGraveyard")) {
                    final String[] k = l[0].split(" ");
                    CardCollectionView list = null;
                    if (sa.getLastStateGraveyard() != null) {
                        list = sa.getLastStateGraveyard();
                    } else { // LastState is Empty
                        return CardFactoryUtil.doXMath(0, expr, c);
                    }
                    list = CardLists.getValidCards(list, k[1].split(","), sa.getActivatingPlayer(), c, sa);
                    return CardFactoryUtil.doXMath(list.size(), expr, c);
                }

                // Count$TargetedLifeTotal (targeted player's life total)
                // Not optimal but since xCount doesn't take SAs, we need to replicate while we have it
                // Probably would be best if xCount took an optional SA to use in these circumstances
                if (sq[0].contains("TargetedLifeTotal")) {
                    final SpellAbility saTargeting = sa.getSATargetingPlayer();
                    if (saTargeting != null) {
                        for (final Player tgtP : saTargeting.getTargets().getTargetPlayers()) {
                            return CardFactoryUtil.doXMath(tgtP.getLife(), expr, c);
                        }
                    }
                }

                if (sq[0].startsWith("CastTotalManaSpent")) {
                    return CardFactoryUtil.doXMath(c.getCastSA() != null ? c.getCastSA().getTotalManaSpent() : 0, expr, c);
                }

                if (sq[0].equals("CastTotalSnowManaSpent")) {
                    int v = 0;
                    if (c.getCastSA() != null) {
                        for (Mana m : c.getCastSA().getPayingMana()) {
                            if (m.isSnow()) {
                                v += 1;
                            }
                        }
                    }
                    return CardFactoryUtil.doXMath(v, expr, c);
                }

                if (sq[0].equals("ResolvedThisTurn")) {
                    return CardFactoryUtil.doXMath(sa.getResolvedThisTurn(), expr, c);
                }
            }

            if (l[0].startsWith("CountersAddedThisTurn")) {
                final String[] parts = l[0].split(" ");
                CounterType cType = CounterType.getType(parts[1]);

                return CardFactoryUtil.doXMath(game.getCounterAddedThisTurn(cType, parts[2], parts[3], c, player, ctb), expr, c);
            }

            // count valid cards in any specified zone/s
            if (l[0].startsWith("Valid")) {
                String[] lparts = l[0].split(" ", 2);
                final String[] rest = lparts[1].split(",");

                final CardCollectionView cardsInZones = lparts[0].length() > 5
                    ? game.getCardsIn(ZoneType.listValueOf(lparts[0].substring(5)))
                    : game.getCardsIn(ZoneType.Battlefield);

                CardCollection cards = CardLists.getValidCards(cardsInZones, rest, player, c, ctb);
                return CardFactoryUtil.doXMath(cards.size(), expr, c);
            }
        }
        return CardFactoryUtil.xCount(c, s2);
    }

    public static final void applyManaColorConversion(ManaConversionMatrix matrix, final Map<String, String> params) {
        String conversion = params.get("ManaConversion");

        for (String pair : conversion.split(" ")) {
            // Check if conversion is additive or restrictive and how to split
            boolean additive = pair.contains("->");
            String[] sides = pair.split(additive ? "->" : "<-");

            if (sides[0].equals("AnyColor") || sides[0].equals("AnyType")) {
                for (byte c : (sides[0].equals("AnyColor") ? MagicColor.WUBRG : MagicColor.WUBRGC)) {
                    matrix.adjustColorReplacement(c, ManaAtom.fromConversion(sides[1]), additive);
                }
            } else {
                matrix.adjustColorReplacement(ManaAtom.fromConversion(sides[0]), ManaAtom.fromConversion(sides[1]), additive);
            }
        }
    }

    public static final List<SpellAbility> getBasicSpellsFromPlayEffect(final Card tgtCard, final Player controller) {
        List<SpellAbility> sas = new ArrayList<>();
        List<SpellAbility> list = Lists.newArrayList(tgtCard.getBasicSpells());
        if (tgtCard.isModal()) {
            list.addAll(Lists.newArrayList(tgtCard.getBasicSpells(tgtCard.getState(CardStateName.Modal))));
        }
        for (SpellAbility s : list) {
            final Spell newSA = (Spell) s.copy();
            newSA.setActivatingPlayer(controller);
            SpellAbilityRestriction res = new SpellAbilityRestriction();
            // timing restrictions still apply
            res.setPlayerTurn(s.getRestrictions().getPlayerTurn());
            res.setOpponentTurn(s.getRestrictions().getOpponentTurn());
            res.setPhases(s.getRestrictions().getPhases());
            res.setZone(null);
            newSA.setRestrictions(res);
            // timing restrictions still apply
            if (res.checkTimingRestrictions(tgtCard, newSA)
                    // still need to check the other restrictions like Aftermath
                    && res.checkOtherRestrictions(tgtCard, newSA, controller)) {
                sas.add(newSA);
            }
        }
        return sas;
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

    private static final String applyTextChangeEffects(final String def, final Card card, final boolean isDescriptive) {
        return applyTextChangeEffects(def, isDescriptive,
                card.getChangedTextColorWords(), card.getChangedTextTypeWords());
    }

    public static final String applyTextChangeEffects(final String def, final boolean isDescriptive,
            Map<String,String> colorMap, Map<String,String> typeMap) {
        if (StringUtils.isEmpty(def)) {
            return def;
        }

        String replaced = def;
        for (final Entry<String, String> e : colorMap.entrySet()) {
            final String key = e.getKey();
            String value;
            if (key.equals("Any")) {
                for (final byte c : MagicColor.WUBRG) {
                    final String colorLowerCase = MagicColor.toLongString(c).toLowerCase(),
                            colorCaptCase = StringUtils.capitalize(MagicColor.toLongString(c));
                    // Color should not replace itself.
                    if (e.getValue().equalsIgnoreCase(colorLowerCase)) {
                        continue;
                    }
                    value = getReplacedText(colorLowerCase, e.getValue(), isDescriptive);
                    replaced = replaced.replaceAll("(?<!>)" + colorLowerCase, value.toLowerCase());
                    value = getReplacedText(colorCaptCase, e.getValue(), isDescriptive);
                    replaced = replaced.replaceAll("(?<!>)" + colorCaptCase, StringUtils.capitalize(value));
                }
            } else {
                value = getReplacedText(key, e.getValue(), isDescriptive);
                replaced = replaced.replaceAll("(?<!>)" + key, value);
            }
        }
        for (final Entry<String, String> e : typeMap.entrySet()) {
            final String key = e.getKey();
            final String pkey = CardType.getPluralType(key);
            final String pvalue = getReplacedText(pkey, CardType.getPluralType(e.getValue()), isDescriptive);
            replaced = replaced.replaceAll("(?<!>)" + pkey, pvalue);
            final String value = getReplacedText(key, e.getValue(), isDescriptive);
            replaced = replaced.replaceAll("(?<!>)" + key, value);
        }
        return replaced;
    }

    private static final String getReplacedText(final String originalWord, final String newWord, final boolean isDescriptive) {
        if (isDescriptive) {
            return "<strike>" + originalWord + "</strike> " + newWord;
        }
        return newWord;
    }

    public static final String getSVar(final CardTraitBase ability, final String sVarName) {
        String val = ability.getSVar(sVarName);
        if (!ability.isIntrinsic() || StringUtils.isEmpty(val)) {
            return val;
        }
        return applyAbilityTextChangeEffects(val, ability);
    }

    private static void addPlayer(Iterable<Object> objects, final String def, FCollection<Player> players) {
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

        final CardCollection splices = CardLists.filter(hand, new Predicate<Card>() {
            @Override
            public boolean apply(Card input) {
                for (final KeywordInterface inst : input.getKeywords(Keyword.SPLICE)) {
                    String k = inst.getOriginal();
                    final String[] n = k.split(":");
                    if (source.isValid(n[1].split(","), player, input, sa)) {
                        return true;
                    }
                }
                return false;
            }
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
            final String k = inst.getOriginal();
            final String[] n = k.split(":");
            spliceCost = new Cost(n[2], false);
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
        sa.setPayCosts(spliceCost.add(sa.getPayCosts()));
        sa.setDescription(sa.getDescription() + " (Splicing " + c + " onto it)");
        sa.addSplicedCards(c);
    }
}
