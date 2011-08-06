/**
 * Color.java
 * 
 * Created on 5.1.2010
 */

package forge;

/**
 * The class Color.
 * 
 * @author dennis.r.friedrichsen
 */
public enum Color {
	BLACK("black"),
	BLUE("blue"),
	GREEN("green"),
	RED("red"),
	WHITE("white"); //,
	//COLORLESS("colorless");
    
    private String name;
    
    private Color() {
        this.name = name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
    }
    
    private Color(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
