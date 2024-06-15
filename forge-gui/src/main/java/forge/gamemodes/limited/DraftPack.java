package forge.gamemodes.limited;

import forge.item.PaperCard;

import java.util.*;

public class DraftPack implements List<PaperCard> {
    private List<PaperCard> cards;
    private int id;
    private LimitedPlayer passedFrom;
    private Map.Entry<LimitedPlayer, PaperCard> awaitingGuess;

    public DraftPack(List<PaperCard> cards, int id) {
        this.cards = cards;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public LimitedPlayer getPassedFrom() {
        return passedFrom;
    }

    public void setPassedFrom(LimitedPlayer passedFrom) {
        this.passedFrom = passedFrom;
    }

    public void setAwaitingGuess(LimitedPlayer player, PaperCard card) {
        this.awaitingGuess = new AbstractMap.SimpleEntry<>(player, card);
    }

    public Map.Entry<LimitedPlayer, PaperCard> getAwaitingGuess() {
        return awaitingGuess;
    }

    public void resetAwaitingGuess() {
        this.awaitingGuess = null;
    }

    @Override
    public int size() {
        return cards.size();
    }

    @Override
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return cards.contains(o);
    }

    @Override
    public Iterator<PaperCard> iterator() {
        return cards.iterator();
    }

    @Override
    public Object[] toArray() {
        return cards.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return cards.toArray(a);
    }

    @Override
    public boolean add(PaperCard paperCard) {
        return cards.add(paperCard);
    }

    @Override
    public boolean remove(Object o) {
        return cards.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return new HashSet<>(cards).containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends PaperCard> c) {
        return cards.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends PaperCard> c) {
        return cards.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return cards.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return cards.retainAll(c);
    }

    @Override
    public void clear() {
        cards.clear();
    }

    @Override
    public boolean equals(Object o) {
        return cards.equals(o);
    }

    @Override
    public int hashCode() {
        return cards.hashCode();
    }

    @Override
    public PaperCard get(int index) {
        return cards.get(index);
    }

    @Override
    public PaperCard set(int index, PaperCard element) {
        return cards.set(index, element);
    }

    @Override
    public void add(int index, PaperCard element) {
        cards.add(index, element);
    }

    @Override
    public PaperCard remove(int index) {
        return cards.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return cards.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return cards.lastIndexOf(o);
    }

    @Override
    public ListIterator<PaperCard> listIterator() {
        return cards.listIterator();
    }

    @Override
    public ListIterator<PaperCard> listIterator(int index) {
        return cards.listIterator(index);
    }

    @Override
    public List<PaperCard> subList(int fromIndex, int toIndex) {
        return cards.subList(fromIndex, toIndex);
    }

    public String toString() {
        return cards.toString();
    }
}
