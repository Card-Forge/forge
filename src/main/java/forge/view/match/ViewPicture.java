package forge.view.match;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.control.match.ControlPicture;
import forge.gui.game.CardPicturePanel;
import forge.view.toolbox.FRoundedPanel;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class ViewPicture extends FRoundedPanel {
    private ControlPicture control;

    private CardPicturePanel pnlPicture;

    /** */
    public ViewPicture() {
        super();
        pnlPicture = new CardPicturePanel(null);
        pnlPicture.setOpaque(false);

        this.setBackground(AllZone.getSkin().getClrTheme());
        this.setLayout(new MigLayout("insets 0, gap 0, center"));

        add(pnlPicture, "w 96%!, h 96%!, gapleft 2%, gapright 2%, gaptop 2%");
        control = new ControlPicture(this);
    }

    /** @return ControlPicture */
    public ControlPicture getController() {
        return control;
    }

    /** @return CardPicturePanel */
    public CardPicturePanel getPnlPicture() {
        return pnlPicture;
    }
}
