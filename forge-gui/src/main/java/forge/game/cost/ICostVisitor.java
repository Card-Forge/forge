package forge.game.cost;

public interface ICostVisitor<T> {

    public T visit(CostGainControl costGainControl);
    public T visit(CostChooseCreatureType costChooseCreatureType);
    public T visit(CostDiscard costDiscard);
    public T visit(CostDamage costDamage);
    public T visit(CostDraw costDraw);
    public T visit(CostExile costExile);
    public T visit(CostExileAndPay costExileAndPay);
    public T visit(CostExiledMoveToGrave costExiledMoveToGrave);
    public T visit(CostFlipCoin costFlipCoin);
    public T visit(CostMill costMill);
    public T visit(CostAddMana costAddMana);
    public T visit(CostPayLife costPayLife);
    public T visit(CostGainLife costGainLife);
    public T visit(CostPartMana costPartMana);
    public T visit(CostPutCardToLib costPutCardToLib);
    public T visit(CostTap costTap);
    public T visit(CostSacrifice costSacrifice);
    public T visit(CostReturn costReturn);
    public T visit(CostReveal costReveal);
    public T visit(CostRemoveAnyCounter costRemoveAnyCounter);
    public T visit(CostRemoveCounter costRemoveCounter);
    public T visit(CostPutCounter costPutCounter);
    public T visit(CostUntapType costUntapType);
    public T visit(CostUntap costUntap);
    public T visit(CostUnattach costUnattach);
    public T visit(CostTapType costTapType);

    public static class Base<T> implements ICostVisitor<T> {

        @Override
        public T visit(CostGainControl costGainControl) {
            return null;
        }

        @Override
        public T visit(CostChooseCreatureType costChooseCreatureType) {
            return null;
        }

        @Override
        public T visit(CostDiscard costDiscard) {
            return null;
        }

        @Override
        public T visit(CostDamage costDamage) {
            return null;
        }

        @Override
        public T visit(CostDraw costDraw) {
            return null;
        }

        @Override
        public T visit(CostExile costExile) {
            return null;
        }

        @Override
        public T visit(CostExileAndPay costExileAndPay) {
            return null;
        }

        @Override
        public T visit(CostExiledMoveToGrave costExiledMoveToGrave) {
            return null;
        }

        @Override
        public T visit(CostFlipCoin costFlipCoin) {
            return null;
        }

        @Override
        public T visit(CostMill costMill) {
            return null;
        }

        @Override
        public T visit(CostAddMana costAddMana) {
            return null;
        }

        @Override
        public T visit(CostPayLife costPayLife) {
            return null;
        }

        @Override
        public T visit(CostGainLife costGainLife) {
            return null;
        }

        @Override
        public T visit(CostPartMana costPartMana) {
            return null;
        }

        @Override
        public T visit(CostPutCardToLib costPutCardToLib) {
            return null;
        }

        @Override
        public T visit(CostTap costTap) {
            return null;
        }

        @Override
        public T visit(CostSacrifice costSacrifice) {
            return null;
        }

        @Override
        public T visit(CostReturn costReturn) {
            return null;
        }

        @Override
        public T visit(CostReveal costReveal) {
            return null;
        }

        @Override
        public T visit(CostRemoveAnyCounter costRemoveAnyCounter) {
            return null;
        }

        @Override
        public T visit(CostRemoveCounter costRemoveCounter) {
            return null;
        }

        @Override
        public T visit(CostPutCounter costPutCounter) {
            return null;
        }

        @Override
        public T visit(CostUntapType costUntapType) {
            return null;
        }

        @Override
        public T visit(CostUntap costUntap) {
            return null;
        }

        @Override
        public T visit(CostUnattach costUnattach) {
            return null;
        }
        
        @Override
        public T visit(CostTapType costTapType) {
            return null;
        }
    }
}
