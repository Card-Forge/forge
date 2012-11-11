package forge.control.input;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class InputPayMana extends Input {

    private static final long serialVersionUID = -9133423708688480255L;

    /**
     * <p>
     * selectManaPool.
     * </p>
     * @param color a String that represents the Color the mana is coming from
     */
    public abstract void selectManaPool(String color);
}
