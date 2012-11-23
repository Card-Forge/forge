/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package forge.game.event;

/**
 *
 * @author Agetian
 */
public class SetTappedEvent extends Event {
    public final boolean Tapped;
    
    public SetTappedEvent(boolean tapped) {
        Tapped = tapped;
    }
}
