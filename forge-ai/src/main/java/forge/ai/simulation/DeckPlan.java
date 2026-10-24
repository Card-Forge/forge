package forge.ai.simulation;

import java.util.Locale;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * What the AI's own deck is trying to do, worked out once from the whole list before the first decision and then
 * fixed for the game (the same deck always gives the same plan).
 * <p>
 * The rollouts are deck independent; the plan only tilts what they value: an aggro or burn deck counts damage to the
 * opponent more and its own life less, a control deck the opposite, a combo deck is given long playouts, a ramp deck
 * values mana development. The tilt is small (a few evaluator points per life point) so that the rollouts can still
 * disagree with it when the position says otherwise.
 */
public final class DeckPlan {
    public enum Archetype { AGGRO, BURN, CONTROL, COMBO, RAMP, MIDRANGE }

    public final Archetype archetype;
    /** Extra evaluator points for each point of life the opponent has lost (from 20). */
    public final int opponentLifeExtra;
    /** Extra evaluator points for each point of life the AI has lost (from 20). */
    public final int ownLifeExtra;
    /** Scales how much a dangerous crack-back counts, in percent. */
    public final int riskPercent;
    /** Mana development value per land and mana source beyond the first four, evaluator points. */
    public final int manaDevelopmentExtra;
    /** Long playouts always, not only when the hand looks like a combo. */
    public final boolean longPlayouts;

    private DeckPlan(final Archetype archetype, final int oppLife, final int ownLife, final int risk, final int mana,
                     final boolean longPlayouts) {
        this.archetype = archetype;
        this.opponentLifeExtra = oppLife;
        this.ownLifeExtra = ownLife;
        this.riskPercent = risk;
        this.manaDevelopmentExtra = mana;
        this.longPlayouts = longPlayouts;
    }

    public static final DeckPlan NONE = new DeckPlan(Archetype.MIDRANGE, 0, 0, 100, 0, false);

    static DeckPlan of(final Archetype a) {
        switch (a) {
            case AGGRO:
                return new DeckPlan(a, 4, 1, 70, 0, false);
            case BURN:
                return new DeckPlan(a, 6, 0, 60, 0, false);
            case CONTROL:
                return new DeckPlan(a, 0, 3, 130, 0, false);
            case COMBO:
                return new DeckPlan(a, 1, 1, 100, 0, true);
            case RAMP:
                return new DeckPlan(a, 0, 1, 100, 8, false);
            default:
                return NONE;
        }
    }

    /** The plan for a profile setting: "off", "auto" or the name of an archetype. */
    public static DeckPlan forSetting(final String setting, final Player player) {
        final String s = setting == null ? "off" : setting.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty() || s.equals("off") || s.equals("false")) {
            return NONE;
        }
        if (!s.equals("auto") && !s.equals("true")) {
            for (final Archetype a : Archetype.values()) {
                if (a.name().toLowerCase(Locale.ROOT).equals(s)) {
                    return of(a);
                }
            }
        }
        return of(classify(player));
    }

    /** Reads the player's whole deck (every card the player owns, wherever it is now). */
    static Archetype classify(final Player player) {
        int spells = 0;
        int creatures = 0;
        int cmcSum = 0;
        int burn = 0;
        int comboPieces = 0;
        int manaSources = 0;
        int bigSpells = 0;
        int reactive = 0;
        int counters = 0;
        for (final Card c : player.getAllCards()) {
            if (c.isLand() || c.isToken() || c.isImmutable()) {
                continue;
            }
            spells++;
            cmcSum += c.getCMC();
            if (c.isCreature()) {
                creatures++;
            }
            if (c.getCMC() >= 5) {
                bigSpells++;
            }
            if (c.hasKeyword(Keyword.STORM)) {
                comboPieces += 2;
            }
            if (c.isInstant() || c.hasKeyword(Keyword.FLASH)) {
                reactive++;
            }
            boolean mana = false;
            for (final SpellAbility sa : c.getSpellAbilities()) {
                final ApiType api = sa.getApi();
                if (api == ApiType.Counter) {
                    counters++;
                } else if (api == ApiType.DealDamage && c.isSpell() && (c.isInstant() || c.isSorcery())) {
                    burn++;
                } else if (api == ApiType.Mana) {
                    mana = true;
                    if (c.getCMC() <= 1 && !c.isCreature()) {
                        comboPieces++; // a ritual
                    }
                }
            }
            if (mana && c.getCMC() <= 3) {
                manaSources++;
            }
        }
        if (spells < 12) {
            return Archetype.MIDRANGE;
        }
        final double avgCmc = (double) cmcSum / spells;
        if (comboPieces >= 6) {
            return Archetype.COMBO;
        }
        if (burn * 100 / spells >= 25 && creatures * 100 / spells <= 35) {
            return Archetype.BURN;
        }
        if (counters >= 3 || reactive * 100 / spells >= 30 && creatures * 100 / spells <= 40) {
            return Archetype.CONTROL;
        }
        if (manaSources >= 6 && bigSpells >= 4) {
            return Archetype.RAMP;
        }
        if (avgCmc <= 2.4 && creatures * 100 / spells >= 45) {
            return Archetype.AGGRO;
        }
        return Archetype.MIDRANGE;
    }

    @Override
    public String toString() {
        return archetype.name();
    }
}
