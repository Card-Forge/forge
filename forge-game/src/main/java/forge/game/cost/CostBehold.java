package forge.game.cost;

public class CostBehold extends CostReveal {

    private static final long serialVersionUID = 1L;

    public CostBehold(String amount, String type, String description) {
        super(amount, type, description, "Hand,Battlefield");
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Behold ");

        final Integer i = this.convertAmount();

        final String desc = this.getTypeDescription() == null ? this.getType() : this.getTypeDescription();

        sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc));

        return sb.toString();
    }

    // Inputs
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
