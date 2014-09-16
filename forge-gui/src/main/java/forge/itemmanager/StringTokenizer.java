package forge.itemmanager;

import java.util.Iterator;

public class StringTokenizer implements Iterator<String> {
    
    private String string;
    private int index = 0;

    public StringTokenizer(String string) {
        this.string = string;
    }

    @Override
    public boolean hasNext() {
        return index < string.length();
    }

    @Override
    public String next() {
        return string.charAt(index++) + "";
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

}
