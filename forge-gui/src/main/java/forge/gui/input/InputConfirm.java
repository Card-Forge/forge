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
package forge.gui.input;


import forge.view.ButtonUtil;
 /**
  * <p>
  * InputMulligan class.
  * </p>
  * 
  * @author Forge
  * @version $Id: InputConfirmMulligan.java 21647 2013-05-24 22:31:11Z Max mtg $
  */
public class InputConfirm extends InputSyncronizedBase {
    private static final long serialVersionUID = -3591794991788531626L;

    private final String message;
    private final String yesButtonText;
    private final String noButtonText;
    private final boolean defaultYes;
    private boolean result;

    public InputConfirm(String message0) {
        this(message0, "Yes", "No", true);
    }

    public InputConfirm(String message0, boolean defaultYes0) {
        this(message0, "Yes", "No", defaultYes0);
    }

    public InputConfirm(String message0, String yesButtonText0, String noButtonText0) {
        this(message0, yesButtonText0, noButtonText0, true);
    }

    public InputConfirm(String message0, String yesButtonText0, String noButtonText0, boolean defaultYes0) {
        this.message = message0;
        this.yesButtonText = yesButtonText0;
        this.noButtonText = noButtonText0;
        this.defaultYes = defaultYes0;
        result = defaultYes0;
    }

    /** {@inheritDoc} */
    @Override
    protected final void showMessage() {
        ButtonUtil.setButtonText(this.yesButtonText, this.noButtonText);
        if (this.defaultYes) {
            ButtonUtil.enableAllFocusOk();
        }
        else {
            ButtonUtil.enableAllFocusCancel();
        }
        showMessage(this.message);
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        this.result = true;
        done();
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCancel() {
        this.result = false;
        done();
    }

    private void done() {
        ButtonUtil.reset();
        stop();
    }

    public final boolean getResult() {
        return this.result;
    }
}
