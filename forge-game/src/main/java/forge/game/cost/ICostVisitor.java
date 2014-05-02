package forge.game.cost;

public interface ICostVisitor<T> {

    public T visit(CostGainControl cost);
    public T visit(CostChooseCreatureType cost);
    public T visit(CostDiscard cost);
    public T visit(CostDamage cost);
    public T visit(CostDraw cost);
    public T visit(CostExile cost);
    public T visit(CostExileFromStack cost);
    public T visit(CostExiledMoveToGrave cost);
    public T visit(CostFlipCoin cost);
    public T visit(CostMill cost);
    public T visit(CostAddMana cost);
    public T visit(CostPayLife cost);
    public T visit(CostGainLife cost);
    public T visit(CostPartMana cost);
    public T visit(CostPutCardToLib cost);
    public T visit(CostTap cost);
    public T visit(CostSacrifice cost);
    public T visit(CostReturn cost);
    public T visit(CostReveal cost);
    public T visit(CostRemoveAnyCounter cost);
    public T visit(CostRemoveCounter cost);
    public T visit(CostPutCounter cost);
    public T visit(CostUntapType cost);
    public T visit(CostUntap cost);
    public T visit(CostUnattach cost);
    public T visit(CostTapType cost);

    public static class Base<T> implements ICostVisitor<T> {

        @Override
        public T visit(CostGainControl cost) {
            return null;
        }

        @Override
        public T visit(CostChooseCreatureType cost) {
            return null;
        }

        @Override
        public T visit(CostDiscard cost) {
            return null;
        }

        @Override
        public T visit(CostDamage cost) {
            return null;
        }

        @Override
        public T visit(CostDraw cost) {
            return null;
        }

        @Override
        public T visit(CostExile cost) {
            return null;
        }

        @Override
        public T visit(CostExileFromStack cost) {
            return null;
        }

        @Override
        public T visit(CostExiledMoveToGrave cost) {
            return null;
        }

        @Override
        public T visit(CostFlipCoin cost) {
            return null;
        }

        @Override
        public T visit(CostMill cost) {
            return null;
        }

        @Override
        public T visit(CostAddMana cost) {
            return null;
        }

        @Override
        public T visit(CostPayLife cost) {
            return null;
        }

        @Override
        public T visit(CostGainLife cost) {
            return null;
        }

        @Override
        public T visit(CostPartMana cost) {
            return null;
        }

        @Override
        public T visit(CostPutCardToLib cost) {
            return null;
        }

        @Override
        public T visit(CostTap cost) {
            return null;
        }

        @Override
        public T visit(CostSacrifice cost) {
            return null;
        }

        @Override
        public T visit(CostReturn cost) {
            return null;
        }

        @Override
        public T visit(CostReveal cost) {
            return null;
        }

        @Override
        public T visit(CostRemoveAnyCounter cost) {
            return null;
        }

        @Override
        public T visit(CostRemoveCounter cost) {
            return null;
        }

        @Override
        public T visit(CostPutCounter cost) {
            return null;
        }

        @Override
        public T visit(CostUntapType cost) {
            return null;
        }

        @Override
        public T visit(CostUntap cost) {
            return null;
        }

        @Override
        public T visit(CostUnattach cost) {
            return null;
        }
        
        @Override
        public T visit(CostTapType cost) {
            return null;
        }
    }

}
