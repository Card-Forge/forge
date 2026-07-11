package forge.ai;

import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug numbered payment plan at the human payment prompt ({@code -Dforge.debugManaPayment.plan=true}).
 */
final class ManaPaymentPlanTracer {
    private ManaPaymentPlanTracer() {
    }

    /** Explicit payment-plan state passed through nested {@code payManaCost} calls. */
    static final class Context {
        private final int depth;
        private final List<String> planSteps;
        final String costLabel;
        final boolean tracePaymentPlan;

        private Context(final int depth, final List<String> planSteps, final String costLabel,
                final boolean tracePaymentPlan) {
            this.depth = depth;
            this.planSteps = planSteps;
            this.costLabel = costLabel;
            this.tracePaymentPlan = tracePaymentPlan;
        }

        boolean isOutermost() {
            return depth == 1;
        }

        Context withCostLabel(final String label) {
            return new Context(depth, planSteps, label, tracePaymentPlan);
        }

        void recordStep(final SpellAbility sa, final boolean test, final String msg) {
            if (msg == null || planSteps == null || !shouldTracePaymentPlan(sa, test, this)) {
                return;
            }
            planSteps.add(msg);
        }

        void finishIfOutermost(final boolean test, final SpellAbility sa, final boolean paid) {
            if (!isOutermost() || planSteps == null || planSteps.isEmpty()) {
                return;
            }
            if (!shouldTracePaymentPlan(sa, test, this)) {
                return;
            }
            printToConsole(test, costLabel != null ? costLabel : "?", sa, planSteps, paid);
        }
    }

    static Context outer() {
        return new Context(1, null, null, false);
    }

    /** Payment-prompt Auto preview dry-run ({@code [test]} plan). */
    static Context outerForPaymentPrompt() {
        if (!debugEnabled()) {
            return outer();
        }
        return new Context(1, new ArrayList<>(), null, true);
    }

    /** Payment-prompt Auto commit ({@code [prod]} plan). */
    static Context outerForPaymentPromptCommit() {
        return outerForPaymentPrompt();
    }

    /** {@code Plains (12)} — card name plus in-game entity id for plan/debug lines. */
    static String formatSourceLabel(final Card card) {
        if (card == null) {
            return "?";
        }
        return card.getName() + " (" + card.getId() + ")";
    }

    private static boolean debugEnabled() {
        return Boolean.getBoolean("forge.debugManaPayment.plan");
    }

    /** Console plans at payment prompt only: preview dry-run {@code [test]} and Auto commit {@code [prod]}. */
    private static boolean shouldTracePaymentPlan(final SpellAbility sa, final boolean test,
            final Context ctx) {
        if (!debugEnabled() || sa == null || ctx == null || !ctx.tracePaymentPlan || !ctx.isOutermost()) {
            return false;
        }
        final Card host = sa.getHostCard();
        if (host == null || host.isInZone(ZoneType.Hand)) {
            return false;
        }
        if (host.isInZone(ZoneType.Stack)) {
            return true;
        }
        if (host.isInZone(ZoneType.Battlefield) && isBattlefieldAbilityPayment(sa)) {
            return true;
        }
        return host.isInZone(ZoneType.Command) && isCommandZoneAbilityPayment(sa);
    }

    /** True when paying mana to activate a non-spell ability (equip, crew, companion ST$, etc.). */
    private static boolean isAbilityManaPayment(final SpellAbility sa) {
        if (sa == null || sa.isSpell() || sa.isManaAbility()) {
            return false;
        }
        final Cost payCosts = sa.getPayCosts();
        if (payCosts == null || !payCosts.hasManaCost()) {
            return false;
        }
        // ST$ scripted abilities (e.g. Companion put-into-hand) are AbilityStatic, not AbilityActivated.
        return sa.isActivatedAbility() || sa.isLandAbility() || sa instanceof AbilityStatic;
    }

    private static boolean isBattlefieldAbilityPayment(final SpellAbility sa) {
        return isAbilityManaPayment(sa);
    }

    private static boolean isCommandZoneAbilityPayment(final SpellAbility sa) {
        return isAbilityManaPayment(sa);
    }

    private static void printToConsole(final boolean test, final String costLabel,
            final SpellAbility sa, final List<String> rawSteps, final boolean paid) {
        if (rawSteps == null || rawSteps.isEmpty()) {
            return;
        }
        final String spellLabel = sa != null && sa.getHostCard() != null ? sa.getHostCard().getName() : "?";
        System.out.println("MANA_PAYMENT_PLAN [" + (test ? "test" : "prod") + "] " + costLabel + " for "
                + spellLabel + (paid ? "" : " (unpaid)"));
        int step = 1;
        for (final String line : rawSteps) {
            if (line != null) {
                System.out.println("  " + step++ + ". " + formatStep(line));
            }
        }
    }

    private static String formatStep(final String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("tap ")) {
            s = "Tap " + s.substring(4);
        }
        return s;
    }
}
