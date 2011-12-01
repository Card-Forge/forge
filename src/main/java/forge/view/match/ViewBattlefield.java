/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
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

import java.util.ArrayList;
import java.util.List;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.view.toolbox.FPanel;

/**
 * Battlefield, assembles and contains instances of MatchPlayer. SHOULD PROBABLY
 * COLLAPSE INTO TOP LEVEL.
 * 
 */
@SuppressWarnings("serial")
public class ViewBattlefield extends FPanel {
    private final List<ViewField> fields;

    /**
     * An FPanel that adds instances of ViewField fields from player name list.
     * 
     */
    public ViewBattlefield() {
        super();
        this.setOpaque(false);
        this.setLayout(new MigLayout("wrap, insets 0, gap 0"));

        // When future codebase upgrades allow, as many fields as
        // necessary can be instantiated here. Doublestrike 29-10-11

        this.fields = new ArrayList<ViewField>();

        ViewField temp;
        String constraints = "h 49.5%!, w 100%!";

        temp = new ViewField(AllZone.getComputerPlayer());
        this.add(temp, constraints);
        this.fields.add(temp);

        temp = new ViewField(AllZone.getHumanPlayer());
        this.add(temp, constraints + ", gaptop 1%");
        this.fields.add(temp);
    }

    /**
     * Returns a list of field components in battlefield.
     * 
     * @return List<ViewFields>
     */
    public List<ViewField> getFields() {
        return this.fields;
    }
}
