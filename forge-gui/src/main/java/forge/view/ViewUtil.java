package forge.view;

import java.util.Collections;

import forge.card.CardCharacteristicName;
import forge.game.card.Card;
import forge.game.card.CardCharacteristics;
import forge.item.IPaperCard;
import forge.view.CardView.CardStateView;

public final class ViewUtil {

    private ViewUtil() {
    }

    /**
     * Write those properties of a {@link Card} to a {@link CardView} that do
     * <i>not</i> depend on other cards.
     * 
     * @param c
     *            the {@link Card} to read from.
     * @param view
     *            the {@link CardView} to write to.
     */
    public static void writeNonDependentCardViewProperties(final Card c, final CardView view) {
        final boolean hasAltState = c.isDoubleFaced() || c.isFlipCard() || c.isFaceDown();
        view.setZone(c.getZone() == null ? null : c.getZone().getZoneType());
        view.setHasAltState(hasAltState);
        view.setFaceDown(c.isFaceDown());
        view.setFoilIndex(c.getFoil());
        view.setCloned(c.isCloned());
        view.setFlipCard(c.isFlipCard());
        view.setFlipped(c.getCurState().equals(CardCharacteristicName.Flipped));
        view.setSplitCard(c.isSplitCard());
        view.setTransformed(c.getCurState().equals(CardCharacteristicName.Transformed));
        view.setSetCode(c.getCurSetCode());
        view.setRarity(c.getRarity());
        view.setTimestamp(c.getTimestamp());
        view.setPhasedOut(c.isPhasedOut());
        view.setSick(c.isInPlay() && c.isSick());
        view.setTapped(c.isTapped());
        view.setToken(c.isToken());
        view.setCounters(c.getCounters());
        view.setDamage(c.getDamage());
        view.setAssignedDamage(c.getTotalAssignedDamage());
        view.setRegenerationShields(c.getShield().size());
        view.setPreventNextDamage(c.getPreventNextDamageTotalShields());
        view.setChosenType(c.getChosenType());
        view.setChosenColors(c.getChosenColor());
        view.setNamedCard(c.getNamedCard());
    
        final CardStateView origView = view.getOriginal();
        origView.setName(c.getName());
        origView.setColors(c.determineColor());
        origView.setImageKey(c.getImageKey());
        origView.setType(Collections.unmodifiableList(c.getType()));
        origView.setManaCost(c.getManaCost());
        origView.setPower(c.getNetAttack());
        origView.setToughness(c.getNetDefense());
        origView.setLoyalty(c.getCurrentLoyalty());
        origView.setText(c.getText());
        origView.setChangedColorWords(c.getChangedTextColorWords());
        origView.setChangedTypes(c.getChangedTextTypeWords());
        origView.setManaCost(c.getManaCost());
    
        final CardStateView altView = view.getAlternate();
        CardCharacteristicName altState = null;
        if (hasAltState) {
            for (final CardCharacteristicName s : c.getStates()) {
                if (!s.equals(CardCharacteristicName.Original) && !s.equals(CardCharacteristicName.FaceDown)) {
                    altState = s;
                }
            }
            if (altState != null) {
                final CardCharacteristics alt = c.getState(altState);
                altView.setName(alt.getName());
                altView.setColors(alt.determineColor());
                altView.setImageKey(alt.getImageKey());
                altView.setType(Collections.unmodifiableList(alt.getType()));
                altView.setManaCost(alt.getManaCost());
                altView.setPower(alt.getBaseAttack());
                altView.setPower(alt.getBaseDefense());
                altView.setLoyalty(0); // FIXME why is loyalty not a property of CardCharacteristic?
            }
        }

        if (altState == null) {
            altView.reset();
        }
    }

    public static CardView getCardForUi(final IPaperCard pc) {
        final Card c = Card.getCardForUi(pc);
        final CardView view = new CardView(-1, true);
        writeNonDependentCardViewProperties(c, view);
        return view;
    }
}
