package forge.game.player;

import java.util.Comparator;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public final class PlayerPredicates {

    public static final Predicate<Player> isTargetableBy(final SpellAbility source) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return source.canTarget(p);
            }
        };
    }

    public static final Predicate<Player> canDiscardBy(final SpellAbility source) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return p.canDiscardBy(source);
            }
        };
    }

    public static final Predicate<Player> isOpponentOf(final Player player) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return p.isOpponentOf(player);
            }
        };
    }
    
    public static final Predicate<Player> sameTeam(final Player player) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return player.sameTeam(p);
            }
        };
    }

    public static final Predicate<Player> isCardInPlay(final String cardName) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return p.isCardInPlay(cardName);
            }
        };
    }
    
    public static final Predicate<Player> isNotCardInPlay(final String cardName) {
        return Predicates.not(isCardInPlay(cardName));
    }

    public static final Predicate<Player> hasCounters() {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return p.hasCounters();
            }
        };
    }

    public static final Predicate<Player> lifeLessOrEqualTo(final int n) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return p.getLife() <= n;
            }
        };
    }

    public static final Predicate<Player> lifeGreaterOrEqualTo(final int n) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return p.getLife() >= n;
            }
        };
    }

    public static final Predicate<Player> hasCounter(final CounterType type) {
        return hasCounter(type, 1);
    }

    public static final Predicate<Player> hasCounter(final CounterType type, final int n) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return p.getCounters(type) >= n;
            }
        };
    }

    public static final Predicate<Player> hasCounter(final CounterEnumType type) {
        return hasCounter(CounterType.get(type), 1);
    }

    public static final Predicate<Player> hasCounter(final CounterEnumType type, final int n) {
        return hasCounter(CounterType.get(type), n);
    }
    
    public static final Predicate<Player> hasKeyword(final String keyword) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return p.hasKeyword(keyword);
            }
        };
    }

    public static final Predicate<Player> canBeAttached(final Card aura) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return p.canBeAttached(aura);
            }
        };
    }

    public static final Comparator<Player> compareByZoneSize(final ZoneType zone) {
        return new Comparator<Player>() {
            @Override
            public int compare(Player arg0, Player arg1) {
                return Integer.compare(arg0.getCardsIn(zone).size(), arg1.getCardsIn(zone).size());
            }
        };
    }
    
    public static final Comparator<Player> compareByZoneSize(final ZoneType zone, final Predicate<Card> pred) {
        return new Comparator<Player>() {
            @Override
            public int compare(Player arg0, Player arg1) {
                return Integer.compare(CardLists.count(arg0.getCardsIn(zone), pred),
                        CardLists.count(arg1.getCardsIn(zone), pred));
            }
        };
    }
    
    public static final Comparator<Player> compareByLife() {
        return new Comparator<Player>() {
            @Override
            public int compare(Player arg0, Player arg1) {
                return Integer.compare(arg0.getLife(), arg1.getLife());
            }
        };
    }
    
    public static final Comparator<Player> compareByPoison() {
        return new Comparator<Player>() {
            @Override
            public int compare(Player arg0, Player arg1) {
                return Integer.compare(arg0.getPoisonCounters(), arg1.getPoisonCounters());
            }
        };
    }

    public static final Predicate<Player> NOT_LOST = new Predicate<Player>() {
        @Override
        public boolean apply(Player p) {
            return p.getOutcome() == null || p.getOutcome().hasWon();
        }
    };
    public static final Predicate<Player> CANT_WIN = new Predicate<Player>() {
        @Override
        public boolean apply(final Player p) {
            return p.hasKeyword("You can't win the game.");
        }
    };
}
