package forge.util;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class Lang {

    /**
     * TODO: Write javadoc for this method.
     * @param position
     * @return
     */
    public static Object getOrdinal(int position) {
        String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (position % 100) {
        case 11:
        case 12:
        case 13:
            return position + "th";
        default:
            return position + sufixes[position % 10];
        }
    }

}
