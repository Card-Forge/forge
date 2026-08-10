package forge.game.decision;

import com.google.common.collect.Multiset;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Canonical internal Forge state used only by determinism audits and neutrality tests. */
public final class ForgeStateFingerprint {
    public static final String VERSION = "FORGE_STATE_V1";
    private static final List<ZoneType> COUNTED_ZONES = List.of(ZoneType.Hand, ZoneType.Library,
            ZoneType.Graveyard, ZoneType.Exile, ZoneType.Battlefield);

    private ForgeStateFingerprint() {
    }

    public static String canonical(final Game game) {
        final List<String> fields = new ArrayList<>();
        fields.add(VERSION);
        appendPhase(fields, game.getPhaseHandler());
        appendPlayers(fields, game);
        appendBattlefield(fields, game);
        appendStack(fields, game);
        appendCombat(fields, game.getCombat());
        fields.add("gameOver=" + game.isGameOver());
        return String.join("|", fields);
    }

    private static void appendPhase(final List<String> fields, final PhaseHandler phase) {
        fields.add("turn=" + phase.getTurn());
        fields.add("phase=" + value(phase.getPhase()));
        fields.add("active=" + playerId(phase.getPlayerTurn()));
        fields.add("priority=" + playerId(phase.getPriorityPlayer()));
    }

    private static void appendPlayers(final List<String> fields, final Game game) {
        final List<Player> players = new ArrayList<>(game.getRegisteredPlayers());
        players.sort(Comparator.comparingInt(Player::getId));
        for (final Player player : players) {
            final StringBuilder record = new StringBuilder("player=").append(player.getId())
                    .append(",life=").append(player.getLife())
                    .append(",poison=").append(player.getPoisonCounters())
                    .append(",won=").append(player.hasWon())
                    .append(",lost=").append(player.hasLost());
            for (final ZoneType zone : COUNTED_ZONES) {
                final List<Card> cards = new ArrayList<>(player.getCardsIn(zone, false));
                record.append(',').append(zone.name()).append('=').append(cards.size());
                if (zone != ZoneType.Battlefield) {
                    if (!ZoneType.ORDERED_ZONES.contains(zone)) {
                        cards.sort(Comparator.comparing(ForgeStateFingerprint::cardKey));
                    }
                    record.append(':').append(cards.stream().map(ForgeStateFingerprint::cardKey).toList());
                }
            }
            fields.add(record.toString());
        }
    }

    private static void appendBattlefield(final List<String> fields, final Game game) {
        final List<Card> cards = new ArrayList<>(game.getCardsIncludePhasingIn(ZoneType.Battlefield));
        cards.sort(Comparator.comparing(ForgeStateFingerprint::cardKey));
        for (final Card card : cards) {
            fields.add("battlefield=" + cardKey(card) + ",owner=" + playerId(card.getOwner())
                    + ",controller=" + playerId(card.getController()) + ",tapped=" + card.isTapped()
                    + ",phased=" + card.isPhasedOut() + ",state=" + card.getCurrentStateName().name()
                    + ",counters=" + counters(card));
        }
    }

    private static void appendStack(final List<String> fields, final Game game) {
        int index = 0;
        for (final SpellAbilityStackInstance instance : game.getStack()) {
            final SpellAbility ability = instance.getSpellAbility();
            fields.add("stack=" + index++ + ",source=" + cardKey(instance.getSourceCard())
                    + ",api=" + value(ability.getApi()) + ",spell=" + instance.isSpell());
        }
    }

    private static void appendCombat(final List<String> fields, final Combat combat) {
        if (combat == null) {
            fields.add("combat=NONE");
            return;
        }
        final List<Card> attackers = new ArrayList<>(combat.getAttackers());
        attackers.sort(Comparator.comparing(ForgeStateFingerprint::cardKey));
        for (final Card attacker : attackers) {
            final List<Card> blockers = new ArrayList<>(combat.getBlockers(attacker));
            blockers.sort(Comparator.comparing(ForgeStateFingerprint::cardKey));
            fields.add("combat=" + cardKey(attacker) + ",defender="
                    + entityKey(combat.getDefenderByAttacker(attacker)) + ",blockers="
                    + blockers.stream().map(ForgeStateFingerprint::cardKey).toList());
        }
    }

    private static String counters(final Card card) {
        final List<Multiset.Entry<CounterType>> counters = new ArrayList<>(card.getCounters().entrySet());
        counters.sort(Comparator.comparing(entry -> entry.getElement().getName()));
        return counters.stream().map(entry -> entry.getElement().getName() + ':' + entry.getCount()).toList()
                .toString();
    }

    private static String cardKey(final Card card) {
        return card == null ? "NONE" : card.getId() + ":" + card.getGameTimestamp();
    }

    private static String entityKey(final GameEntity entity) {
        if (entity instanceof Card card) {
            return "CARD:" + cardKey(card);
        }
        if (entity instanceof Player player) {
            return "PLAYER:" + player.getId();
        }
        return entity == null ? "NONE" : entity.getClass().getSimpleName() + ':' + entity.getId();
    }

    private static int playerId(final Player player) {
        return player == null ? -1 : player.getId();
    }

    private static String value(final Object value) {
        return value == null ? "NONE" : value.toString();
    }
}
