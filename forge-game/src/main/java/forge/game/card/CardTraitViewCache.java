package forge.game.card;

import forge.game.Game;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

import java.util.IdentityHashMap;

/** Request-scoped reuse of derived card trait views; card traits must remain stable while open. */
public final class CardTraitViewCache implements AutoCloseable {
    private static final ThreadLocal<CardTraitViewCache> CURRENT = new ThreadLocal<>();
    private final IdentityHashMap<Card, FCollectionView<StaticAbility>> staticAbilities = new IdentityHashMap<>();
    private final IdentityHashMap<Card, FCollectionView<SpellAbility>> spellAbilities = new IdentityHashMap<>();
    private final IdentityHashMap<Card, FCollectionView<SpellAbility>> allSpellAbilities = new IdentityHashMap<>();
    private final IdentityHashMap<Card, FCollectionView<Trigger>> triggers = new IdentityHashMap<>();
    private final IdentityHashMap<Card, FCollectionView<Trigger>> combatTriggers = new IdentityHashMap<>();
    private final IdentityHashMap<Card, FCollectionView<ReplacementEffect>> replacementEffects = new IdentityHashMap<>();
    private final IdentityHashMap<Game, FCollectionView<Trigger>> battlefieldTriggers = new IdentityHashMap<>();
    private final IdentityHashMap<Game, FCollectionView<Trigger>> globalCombatTriggers = new IdentityHashMap<>();
    private int scopeDepth = 1;

    private CardTraitViewCache() {}

    public static CardTraitViewCache open() {
        CardTraitViewCache cache = CURRENT.get();
        if (cache == null) {
            cache = new CardTraitViewCache();
            CURRENT.set(cache);
        } else {
            cache.scopeDepth++;
        }
        return cache;
    }

    static FCollectionView<StaticAbility> getStaticAbilities(final Card card) {
        final CardTraitViewCache cache = CURRENT.get();
        return cache == null ? card.getStaticAbilitiesUncached()
                : cache.staticAbilities.computeIfAbsent(
                        card, Card::getStaticAbilitiesUncached);
    }

    static FCollectionView<SpellAbility> getSpellAbilities(final Card card) {
        final CardTraitViewCache cache = CURRENT.get();
        return cache == null ? card.getSpellAbilitiesUncached()
                : cache.spellAbilities.computeIfAbsent(
                        card, Card::getSpellAbilitiesUncached);
    }

    static FCollectionView<SpellAbility> getAllSpellAbilities(final Card card) {
        final CardTraitViewCache cache = CURRENT.get();
        return cache == null ? card.getAllSpellAbilitiesUncached()
                : cache.allSpellAbilities.computeIfAbsent(
                        card, Card::getAllSpellAbilitiesUncached);
    }

    static FCollectionView<Trigger> getTriggers(final Card card) {
        final CardTraitViewCache cache = CURRENT.get();
        return cache == null ? card.getTriggersUncached()
                : cache.triggers.computeIfAbsent(card, Card::getTriggersUncached);
    }

    public static FCollectionView<Trigger> getBattlefieldTriggers(final Game game) {
        final CardTraitViewCache cache = CURRENT.get();
        if (cache == null) {
            return collectBattlefieldTriggers(game);
        }
        return cache.battlefieldTriggers.computeIfAbsent(
                game, CardTraitViewCache::collectBattlefieldTriggers);
    }

    public static FCollectionView<Trigger> getCombatTriggers(final Card combatant) {
        if (combatant.isInZone(ZoneType.Battlefield)
                || combatant.isInZone(ZoneType.Command)) {
            return getGlobalCombatTriggers(combatant.getGame());
        }
        final CardTraitViewCache cache = CURRENT.get();
        return cache == null ? collectCombatTriggers(combatant)
                : cache.combatTriggers.computeIfAbsent(
                        combatant, CardTraitViewCache::collectCombatTriggers);
    }

    public static FCollectionView<Trigger> getCombatTriggers(final Game game,
            final Card combatant) {
        if (combatant != null) {
            return getCombatTriggers(combatant);
        }
        return getGlobalCombatTriggers(game);
    }

    private static FCollectionView<Trigger> getGlobalCombatTriggers(final Game game) {
        final CardTraitViewCache cache = CURRENT.get();
        return cache == null ? collectGlobalCombatTriggers(game)
                : cache.globalCombatTriggers.computeIfAbsent(
                        game, CardTraitViewCache::collectGlobalCombatTriggers);
    }

    static FCollectionView<ReplacementEffect> getReplacementEffects(final Card card) {
        final CardTraitViewCache cache = CURRENT.get();
        return cache == null ? card.getReplacementEffectsUncached()
                : cache.replacementEffects.computeIfAbsent(
                        card, Card::getReplacementEffectsUncached);
    }

    private static FCollectionView<Trigger> collectBattlefieldTriggers(final Game game) {
        final FCollection<Trigger> result = new FCollection<>();
        for (Card card : game.getCardsIn(ZoneType.Battlefield)) {
            result.addAll(card.getTriggers());
        }
        return result;
    }

    private static FCollectionView<Trigger> collectCombatTriggers(final Card combatant) {
        final FCollection<Trigger> result = new FCollection<>(
                getGlobalCombatTriggers(combatant.getGame()));
        result.addAll(combatant.getTriggers());
        return result;
    }

    private static FCollectionView<Trigger> collectGlobalCombatTriggers(final Game game) {
        final FCollection<Trigger> result = new FCollection<>(getBattlefieldTriggers(game));
        for (Card card : game.getCardsIn(ZoneType.Command)) {
            result.addAll(card.getTriggers());
        }
        return result;
    }

    @Override
    public void close() {
        if (--scopeDepth == 0) {
            CURRENT.remove();
        }
    }
}
