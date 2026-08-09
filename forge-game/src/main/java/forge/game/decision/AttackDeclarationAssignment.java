package forge.game.decision;

/** One session-local attacker-to-defender assignment. */
public final class AttackDeclarationAssignment {
    private final AttackDeclarationCard card;
    private final AttackDeclarationDefender defender;

    AttackDeclarationAssignment(final AttackDeclarationCard card, final AttackDeclarationDefender defender) {
        this.card = card;
        this.defender = defender;
    }

    public AttackDeclarationCard getCard() {
        return card;
    }

    public AttackDeclarationDefender getDefender() {
        return defender;
    }

    String semanticKey() {
        return card.semanticKey() + "|" + defender.semanticKey();
    }
}
