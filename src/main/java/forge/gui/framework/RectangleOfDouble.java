package forge.gui.framework;

/** 
 * Dimensions for a cell, stored as % of available space
 *
 */
public class RectangleOfDouble {
    private final double x;
    private final double y;
    private final double w;
    private final double h;

    
    public RectangleOfDouble(final double x0, final double y0, final double w0, final double h0) {
        if (x0 > 1) { throw new IllegalArgumentException("X value greater than 100%!"); }
        x = x0;
        if (y0 > 1) { throw new IllegalArgumentException("Y value greater than 100%!"); }
        y = y0;
        if (w0 > 1) { throw new IllegalArgumentException("W value greater than 100%!"); }
        w = w0;
        if (h0 > 1) { throw new IllegalArgumentException("H value greater than 100%!"); }
        h = h0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(h);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(w);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RectangleOfDouble other = (RectangleOfDouble) obj;
        if (Double.doubleToLongBits(h) != Double.doubleToLongBits(other.h))
            return false;
        if (Double.doubleToLongBits(w) != Double.doubleToLongBits(other.w))
            return false;
        if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
            return false;
        if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
            return false;
        return true;
    }

    public final double getX() {
        return x;
    }


    public final double getY() {
        return y;
    }


    public final double getW() {
        return w;
    }


    public final double getH() {
        return h;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Rectangle @(%f, %f) sz=(%f, %f)", x,y, w,h);
    }


    
}
