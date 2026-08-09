package forge.game.decision;

/** One session-local blocker-to-attacker assignment. */
public final class BlockDeclarationAssignment {
    private final BlockDeclarationCard blocker;
    private final BlockDeclarationCard attacker;

    BlockDeclarationAssignment(final BlockDeclarationCard blocker, final BlockDeclarationCard attacker) {
        this.blocker = blocker;
        this.attacker = attacker;
    }

    public BlockDeclarationCard getBlocker() {
        return blocker;
    }

    public BlockDeclarationCard getAttacker() {
        return attacker;
    }

    String semanticKey() {
        return blocker.semanticKey() + "|" + attacker.attackerSemanticKey();
    }
}
