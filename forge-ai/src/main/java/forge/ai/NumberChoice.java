package forge.ai;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The number the AI names for a "choose a number" effect that has no logic of its own: it reads what the rest of the
 * spell does with the number. A number that feeds something good for the AI (cards, life, counters, tokens, damage or
 * life loss at an opponent) is the highest one allowed; a number that hurts the AI (damage or life loss to itself, a
 * discard) is the lowest. Used to be a random pick, or the first option.
 */
public final class NumberChoice {
    private NumberChoice() {
    }

    private static final Set<ApiType> GOOD = EnumSet.of(ApiType.Draw, ApiType.GainLife, ApiType.PutCounter, ApiType.Token,
            ApiType.Scry, ApiType.Surveil, ApiType.Mana);
    private static final Set<ApiType> HARMFUL = EnumSet.of(ApiType.LoseLife, ApiType.DealDamage, ApiType.Discard,
            ApiType.Mill, ApiType.Sacrifice);

    /** The option to name, or the first one when nothing says which way the number cuts. */
    public static int choose(final SpellAbility sa, final List<Integer> options, final Player related) {
        if (options == null || options.isEmpty()) {
            return 0;
        }
        try {
            final int direction = direction(sa);
            if (direction > 0) {
                return Collections.max(options);
            }
            if (direction < 0) {
                return Collections.min(options);
            }
        } catch (final RuntimeException e) {
            // a refinement only
        }
        return options.get(0);
    }

    /** Positive when a high number helps the controller of the spell, negative when it hurts, zero when unknown. */
    static int direction(final SpellAbility sa) {
        int score = 0;
        for (SpellAbility s = sa; s != null; s = s.getSubAbility()) {
            if (s == sa && s.getApi() == ApiType.ChooseNumber) {
                continue;
            }
            final ApiType api = s.getApi();
            final boolean good = GOOD.contains(api);
            if ((!good && !HARMFUL.contains(api)) || !usesNumber(s)) {
                continue;
            }
            final Boolean mine = receiverIsController(s);
            if (mine == null) {
                continue;
            }
            score += good == mine.booleanValue() ? 1 : -1;
        }
        return score;
    }

    private static Boolean receiverIsController(final SpellAbility s) {
        final String defined = s.getParamOrDefault("Defined", "");
        final String tgts = s.getParamOrDefault("ValidTgts", "");
        final String players = s.getParamOrDefault("ValidPlayers", "");
        if (!players.isEmpty() || defined.contains("Player") && !defined.contains("Opponent")) {
            return null; // everybody: the number cuts both ways
        }
        if (defined.contains("Opponent") || tgts.contains("Opponent")) {
            return Boolean.FALSE;
        }
        if (tgts.startsWith("Player") || tgts.equals("Any")) {
            // the AI aims a good effect at itself and a harmful one at an opponent: whatever the effect, the answer is
            // the same for it
            return HARMFUL.contains(s.getApi()) ? Boolean.FALSE : Boolean.TRUE;
        }
        return Boolean.TRUE; // "You", no target, a creature of its own
    }

    private static boolean usesNumber(final SpellAbility s) {
        final Card host = s.getHostCard();
        for (final String value : s.getMapParams().values()) {
            if (value.contains("ChosenNumber")) {
                return true;
            }
            if (host != null && host.hasSVar(value) && host.getSVar(value).contains("ChosenNumber")) {
                return true;
            }
        }
        return false;
    }
}
