package forge.game.decision;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;

/** Resolver-independent closed ownership classifier for simultaneous ORDER callbacks. */
public final class OrderProfileRouter {
    public enum PreClassification {
        L1_EXACT,
        COPY_SPELL_FAMILY_INTENT,
        UNOWNED_OTHER
    }

    public enum Classification {
        L1_EXACT,
        L1C_EXACT,
        MALFORMED_L1C_INTENT,
        UNOWNED_OTHER
    }

    private OrderProfileRouter() {
    }

    /** Dispatches one callback after resolver-independent pre-classification. */
    public static List<SpellAbility> order(final List<SpellAbility> active, final Player chooser,
            final SimultaneousTriggerOrderDecisionCoordinator l1Coordinator,
            final SimultaneousTriggerOrderDecisionProvider l1Provider,
            final CopySpellResolveFirstOrderDecisionCoordinator l1cCoordinator,
            final CopySpellResolveFirstOrderDecisionProvider l1cProvider,
            final SimultaneousTriggerOrderDecisionCoordinator.NativeOrderer nativeOrderer) {
        SimultaneousTriggerOrderAuditDiagnostics.recordRawInvocation(active == null ? -1 : active.size());
        final PreClassification preClassification = preClassify(active);
        switch (preClassification) {
        case L1_EXACT:
            return l1Coordinator.order(active, chooser, l1Provider, nativeOrderer);
        case COPY_SPELL_FAMILY_INTENT:
            return l1cCoordinator.order(active, chooser, l1cProvider, nativeOrderer::order);
        case UNOWNED_OTHER:
        default:
            if (active != null && active.size() >= 2) {
                SimultaneousTriggerOrderAuditDiagnostics.recordNonL1MultiItemCallback();
                SimultaneousTriggerOrderAuditDiagnostics.recordOutsideL1NativeFallback();
            }
            return nativeOrderer.order(active);
        }
    }

    public static PreClassification preClassify(final List<SpellAbility> active) {
        if (active == null || active.size() < 2) {
            return PreClassification.UNOWNED_OTHER;
        }
        if (SimultaneousTriggerOrderDecisionCoordinator.isSimultaneousTriggerProfileCandidate(active)) {
            return PreClassification.L1_EXACT;
        }
        if (isCopySpellFamilyIntent(active)) {
            return PreClassification.COPY_SPELL_FAMILY_INTENT;
        }
        return PreClassification.UNOWNED_OTHER;
    }

    static Classification classify(final List<SpellAbility> active, final Player chooser,
            final CopySpellResolveFirstOrderDecisionCoordinator coordinator) {
        final PreClassification preClassification = preClassify(active);
        if (preClassification == PreClassification.L1_EXACT) {
            return Classification.L1_EXACT;
        }
        if (preClassification != PreClassification.COPY_SPELL_FAMILY_INTENT) {
            return Classification.UNOWNED_OTHER;
        }
        try {
            return coordinator.admit(active, chooser, 1L) == null
                    ? Classification.MALFORMED_L1C_INTENT : Classification.L1C_EXACT;
        } catch (final SimultaneousTriggerOrderIntegrityException ex) {
            return Classification.MALFORMED_L1C_INTENT;
        }
    }

    private static boolean isCopySpellFamilyIntent(final List<SpellAbility> active) {
        try {
            for (final SpellAbility entry : active) {
                if (entry == null || !entry.isSpell() || !entry.isCopied()) {
                    return false;
                }
                final Card host = entry.getHostCard();
                if (host == null || !host.isCopiedSpell()) {
                    return false;
                }
            }
            return true;
        } catch (final RuntimeException ex) {
            return false;
        }
    }

}
