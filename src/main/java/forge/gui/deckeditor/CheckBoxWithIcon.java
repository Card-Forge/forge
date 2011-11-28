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
package forge.gui.deckeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;

/**
 * Custom check box class for filter icon.
 */
public class CheckBoxWithIcon extends JCheckBox {
    /* CHOPPIC */
    /* Custom check box class for filter icons */
    private static final long serialVersionUID = -8099263807219520120L;

    private final String imagePath = "res/images/deckeditor/";
    private final String iconYes;
    private final String iconNo;
    private final CheckBoxWithIcon cb;

    /**
     * Instantiates a new check box with icon.
     * 
     * @param filterName
     *            the filter name
     * @param toolTip
     *            the tool tip
     */
    CheckBoxWithIcon(final String filterName, final String toolTip) {
        super("", true);
        this.cb = this;
        this.iconYes = this.imagePath + "filter_" + filterName + "_y.png";
        this.iconNo = this.imagePath + "filter_" + filterName + "_n.png";
        this.setIcon(new ImageIcon(this.iconYes));
        this.setToolTipText(toolTip);
        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                if (CheckBoxWithIcon.this.cb.isSelected()) {
                    CheckBoxWithIcon.this.cb.setIcon(new ImageIcon(CheckBoxWithIcon.this.iconYes));
                } else {
                    CheckBoxWithIcon.this.cb.setIcon(new ImageIcon(CheckBoxWithIcon.this.iconNo));
                }
            }
        });
    }
}
