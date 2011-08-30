package forge.card;

import net.slightlymagic.braids.util.lambda.Lambda1;

/**
 * <p>CardSet class.</p>
 *
 * @author Forge
 * @version $Id: CardSet.java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardSet implements Comparable<CardSet> { // immutable
    private int index;
    private String code;
    private String code2;
    private String name;

    public CardSet(final int index, final String name, final String code, final String code2) {
        this.code = code;
        this.code2 = code2;
        this.index = index;
        this.name = name;
    }

    public String getName() { return name; }
    public String getCode() { return code; }
    public String getCode2() { return code2; }
    public int getIndex() { return index; }

    
    public static final Lambda1<String, CardSet> fnGetName = new Lambda1<String, CardSet>() {
        @Override public String apply(final CardSet arg1) { return arg1.name; } };
    public static final Lambda1<CardSet, CardSet> fn1 = new Lambda1<CardSet, CardSet>() {
        @Override public CardSet apply(final CardSet arg1) { return arg1; } };

    @Override
    public int compareTo(final CardSet o) {
        if (o == null) { return 1; }
        return o.index - this.index;
    }

    @Override
    public int hashCode() {
        return code.hashCode() * 17 + name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }

        CardSet other = (CardSet) obj;
        return other.name.equals(this.name) && this.code.equals(other.code);
    }
}
