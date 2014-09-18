package forge.view;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.CardCharacteristicName;
import forge.game.card.Card;
import forge.game.card.CardCharacteristics;
import forge.item.IPaperCard;
import forge.view.CardView.CardStateView;

/**
 * Static class providing utility methods to the view classes.
 * 
 * @author elcnesh
 */
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
    public static void writeNonDependentCardViewProperties(final Card c, final CardView view, final boolean mayShowCardFace) {
        final boolean hasAltState = c.isDoubleFaced() || c.isFlipCard() || c.isSplitCard() || (c.isFaceDown() && mayShowCardFace);
        view.setId(c.getUniqueNumber());
        view.setZone(c.getZone() == null ? null : c.getZone().getZoneType());
        view.setHasAltState(hasAltState);
        view.setFaceDown(c.isFaceDown());
        view.setCloned(c.isCloned());
        view.setFlipCard(c.isFlipCard());
        view.setFlipped(c.getCurState().equals(CardCharacteristicName.Flipped));
        view.setSplitCard(c.isSplitCard());
        view.setTransformed(c.getCurState().equals(CardCharacteristicName.Transformed));
        view.setSetCode(c.getCurSetCode());
        view.setRarity(c.getRarity());
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

        if (c.isSplitCard()) {
            final CardCharacteristicName orig, alt;
            if (c.getCurState() == CardCharacteristicName.RightSplit) {
                // If right half on stack, place it first
                orig = CardCharacteristicName.RightSplit;
                alt = CardCharacteristicName.LeftSplit;
            } else {
                orig = CardCharacteristicName.LeftSplit;
                alt = CardCharacteristicName.RightSplit;
            }
            writeCardStateViewProperties(c, view.getOriginal(), orig);
            writeCardStateViewProperties(c, view.getAlternate(), alt);
            return;
        }

        final CardStateView origView = view.getOriginal();
        origView.setName(c.getName());
        origView.setColors(c.determineColor());
        origView.setImageKey(c.getImageKey() );
        origView.setType(Collections.unmodifiableList(c.getType()));
        origView.setManaCost(c.getManaCost());
        origView.setPower(c.getNetAttack());
        origView.setToughness(c.getNetDefense());
        origView.setLoyalty(c.getCurrentLoyalty());
        origView.setText(c.getText());
        origView.setChangedColorWords(c.getChangedTextColorWords());
        origView.setChangedTypes(c.getChangedTextTypeWords());
        origView.setManaCost(c.getManaCost());
        origView.setHasDeathtouch(c.hasKeyword("Deathtouch"));
        origView.setHasInfect(c.hasKeyword("Infect"));
        origView.setHasStorm(c.hasKeyword("Storm"));
        origView.setHasTrample(c.hasKeyword("Trample"));
        origView.setFoilIndex(c.getCharacteristics().getFoil());

        final CardStateView altView = view.getAlternate();
        CardCharacteristicName altState = null;
        if (hasAltState) {
            if (c.isFlipCard() && !c.getCurState().equals(CardCharacteristicName.Flipped)) {
                altState = CardCharacteristicName.Flipped;
            } else if (c.isDoubleFaced() && !c.getCurState().equals(CardCharacteristicName.Transformed)) {
                altState = CardCharacteristicName.Transformed;
            } else {
                altState = CardCharacteristicName.Original;
            }

            if (altState != null) {
                writeCardStateViewProperties(c, altView, altState);
            }
        }

        if (altState == null) {
            altView.reset();
        }
    }

    private static void writeCardStateViewProperties(final Card c, final CardStateView view, final CardCharacteristicName state) {
        final CardCharacteristics chars = c.getState(state);
        view.setName(chars.getName());
        view.setColors(chars.determineColor());
        view.setImageKey(chars.getImageKey());
        view.setType(Collections.unmodifiableList(chars.getType()));
        view.setManaCost(chars.getManaCost());
        view.setPower(chars.getBaseAttack());
        view.setToughness(chars.getBaseDefense());
        view.setLoyalty(0); // Q why is loyalty not a property of CardCharacteristic? A: because no alt states have a base loyalty (only candidate is Garruk Relentless).
        view.setText(chars.getOracleText());
        view.setFoilIndex(chars.getFoil());
    }

    public static CardView getCardForUi(final IPaperCard pc) {
        final Card c = Card.getCardForUi(pc);
        final CardView view = new CardView(true);
        writeNonDependentCardViewProperties(c, view, c.getCardForUi() == c);
        return view;
    }

    public static <T,V> List<V> transformIfNotNull(final Iterable<T> input, final Function<T, V> transformation) {
        final List<V> ret = Lists.newLinkedList();
        synchronized (input) {
            for (final T t : input) {
                final V v = transformation.apply(t);
                if (v != null) {
                    ret.add(v);
                }
            }
        }
        return ret;
    }

    public static boolean mayViewAny(final Iterable<CardView> cards) {
        return Iterables.any(cards, new Predicate<CardView>() {
            @Override
            public boolean apply(final CardView input) {
                return input.getId() >= 0;
            }
        });
    }
}
