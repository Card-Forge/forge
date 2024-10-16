package forge.game.player;

import java.util.Comparator;
import java.util.function.Predicate;

import forge.game.CardTraitBase;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public final class PlayerPredicates {

    public static Predicate<Player> isTargetableBy(final SpellAbility source) {
        return source::canTarget;
    }

    public static Predicate<Player> canDiscardBy(final SpellAbility source, final boolean effect) {
        return p -> p.canDiscardBy(source, effect);
    }

    public static Predicate<Player> isOpponentOf(final Player player) {
        return p -> p.isOpponentOf(player);
    }
    
    public static Predicate<Player> sameTeam(final Player player) {
        return player::sameTeam;
    }

    public static Predicate<Player> isCardInPlay(final String cardName) {
        return p -> p.isCardInPlay(cardName);
    }
    
    public static Predicate<Player> isNotCardInPlay(final String cardName) {
        return isCardInPlay(cardName).negate();
    }

    public static Predicate<Player> hasCounters() {
        return GameEntity::hasCounters;
    }

    public static Predicate<Player> lifeLessOrEqualTo(final int n) {
        return p -> p.getLife() <= n;
    }

    public static Predicate<Player> lifeGreaterOrEqualTo(final int n) {
        return p -> p.getLife() >= n;
    }

    public static Predicate<Player> hasCounter(final CounterType type) {
        return hasCounter(type, 1);
    }

    public static Predicate<Player> hasCounter(final CounterType type, final int n) {
        return p -> p.getCounters(type) >= n;
    }

    public static Predicate<Player> hasCounter(final CounterEnumType type) {
        return hasCounter(CounterType.get(type), 1);
    }

    public static Predicate<Player> hasCounter(final CounterEnumType type, final int n) {
        return hasCounter(CounterType.get(type), n);
    }
    
    public static Predicate<Player> hasKeyword(final String keyword) {
        return p -> p.hasKeyword(keyword);
    }

    public static Predicate<Player> canBeAttached(final Card aura, SpellAbility sa) {
        return p -> p.canBeAttached(aura, sa);
    }

    public static Predicate<Player> restriction(final String[] restrictions, final Player sourceController, final Card source, final CardTraitBase spellAbility) {
        return c -> c != null && c.isValid(restrictions, sourceController, source, spellAbility);
    }

    public static Comparator<Player> compareByZoneSize(final ZoneType zone) {
        return Comparator.comparingInt(arg0 -> arg0.getCardsIn(zone).size());
    }
    
    public static Comparator<Player> compareByZoneSize(final ZoneType zone, final Predicate<Card> pred) {
        return Comparator.comparingInt(arg0 -> CardLists.count(arg0.getCardsIn(zone), pred));
    }
    
    public static Comparator<Player> compareByLife() {
        return Comparator.comparingInt(Player::getLife);
    }
    
    public static Comparator<Player> compareByPoison() {
        return Comparator.comparingInt(Player::getPoisonCounters);
    }

    public static final Predicate<Player> NOT_LOST = p -> p.getOutcome() == null || p.getOutcome().hasWon();
}
