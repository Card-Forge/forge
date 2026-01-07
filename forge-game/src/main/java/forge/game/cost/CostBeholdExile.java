package forge.game.cost;

public class CostBeholdExile extends CostBehold {

    private static final long serialVersionUID = 1L;

    public CostBeholdExile(String amount, String type, String description) {
        super(amount, type, description);
    }

    @Override
    public String toString() {
        return super.toString() + " and exile it";
    }

    @Override
    protected CardCollectionView doListPayment(Player payer, SpellAbility ability, CardCollectionView targetCards, final boolean effect) {
        CardCollection result = new CardCollection();
        for (Card targetCard : super.doListPayment(payer, ability, targetCards, effect)) {
            Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
            AbilityKey.addCardZoneTableParams(moveParams, table);

            Card newCard = targetCard.getGame().getAction().exile(targetCard, null, moveParams);
            SpellAbilityEffect.handleExiledWith(newCard, ability);
            result.add(newCard);
        }

        return result;
    }

    // Inputs
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }
}