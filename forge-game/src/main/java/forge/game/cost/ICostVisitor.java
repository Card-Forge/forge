package forge.game.cost;

public interface ICostVisitor<T> {

    T visit(CostGainControl cost);
    T visit(CostChooseCreatureType cost);
    T visit(CostDiscard cost);
    T visit(CostDamage cost);
    T visit(CostDraw cost);
    T visit(CostExile cost);
    T visit(CostExileFromStack cost);
    T visit(CostExiledMoveToGrave cost);
    T visit(CostExert cost);
    T visit(CostFlipCoin cost);
    T visit(CostRollDice cost);
    T visit(CostMill cost);
    T visit(CostAddMana cost);
    T visit(CostPayLife cost);
    T visit(CostPayEnergy cost);
    T visit(CostGainLife cost);
    T visit(CostPartMana cost);
    T visit(CostPutCardToLib cost);
    T visit(CostTap cost);
    T visit(CostSacrifice cost);
    T visit(CostReturn cost);
    T visit(CostReveal cost);
    T visit(CostRevealChosenPlayer cost);
    T visit(CostRemoveAnyCounter cost);
    T visit(CostRemoveCounter cost);
    T visit(CostPutCounter cost);
    T visit(CostUntapType cost);
    T visit(CostUntap cost);
    T visit(CostUnattach cost);
    T visit(CostTapType cost);

    class Base<T> implements ICostVisitor<T> {

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
        public T visit(CostExert cost) {
            return null;
        }

        @Override
        public T visit(CostFlipCoin cost) {
            return null;
        }

        @Override
        public T visit(CostRollDice cost) {
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
        public T visit(CostPayEnergy cost) {
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
        public T visit(CostRevealChosenPlayer cost) {
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
