package forge.view.match;

import java.util.ArrayList;
import java.util.List;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.view.toolbox.FPanel;

/**
 * Battlefield, assembles and contains instances of MatchPlayer.
 * SHOULD PROBABLY COLLAPSE INTO TOP LEVEL.
 *
 */
@SuppressWarnings("serial")
public class ViewAreaBattlefield extends FPanel {
    private List<ViewField> fields;

    /**
     * An FPanel that adds instances of ViewField fields
     * from player name list.
     * 
     */
    public ViewAreaBattlefield() {
        super();
        setOpaque(false);
        setLayout(new MigLayout("wrap, insets 1% 0.5% 0 0, gap 1%, nocache"));

        // When future codebase upgrades allow, as many fields as
        // necessary can be instantiated here. Doublestrike 29-10-11

        fields = new ArrayList<ViewField>();

        ViewField temp;

        temp = new ViewField(AllZone.getComputerPlayer());
        this.add(temp, "h 48.5%!, w 99.5%!");
        fields.add(temp);

        temp = new ViewField(AllZone.getHumanPlayer());
        this.add(temp, "h 48.5%!, w 99.5%!");
        fields.add(temp);
    }

    /** 
     * Returns a list of field components in battlefield.
     * @return List<ViewFields>
     */
    public List<ViewField> getFields() {
        return fields;
    }
}
