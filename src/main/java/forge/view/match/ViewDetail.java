package forge.view.match;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.control.match.ControlDetail;
import forge.gui.game.CardDetailPanel;
import forge.view.toolbox.FRoundedPanel;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class ViewDetail extends FRoundedPanel {
    private ControlDetail control;

    private CardDetailPanel pnlDetail;

    /** */
    public ViewDetail() {
        super();
        pnlDetail = new CardDetailPanel(null);
        pnlDetail.setOpaque(false);

        this.setBackground(AllZone.getSkin().getClrTheme());
        this.setLayout(new MigLayout("insets 0, gap 0"));

        add(pnlDetail, "w 100%!, h 100%!");
        control = new ControlDetail(this);
    }

    /** @return ControlDetail */
    public ControlDetail getController() {
        return control;
    }

    /** @return CardDetailPanel */
    public CardDetailPanel getPnlDetail() {
        return pnlDetail;
    }
}
