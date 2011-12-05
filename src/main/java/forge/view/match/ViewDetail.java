/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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

    /**
     * Instantiates a new view detail.
     */
    public ViewDetail() {
        super();
        pnlDetail = new CardDetailPanel(null);
        pnlDetail.setOpaque(false);

        this.setBackground(AllZone.getSkin().getClrTheme());
        this.setLayout(new MigLayout("insets 0, gap 0"));

        add(pnlDetail, "w 100%!, h 100%!");
        control = new ControlDetail(this);
    }

    /**
     * Gets the controller.
     *
     * @return ControlDetail
     */
    public ControlDetail getController() {
        return control;
    }

    /**
     * Gets the pnl detail.
     *
     * @return CardDetailPanel
     */
    public CardDetailPanel getPnlDetail() {
        return pnlDetail;
    }
}
