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
package forge.gamemodes.match.input;

import java.util.List;

import com.google.common.collect.ImmutableList;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.spellability.SpellAbility;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.util.Localizer;

 /**
  * <p>
  * InputConfirm class.
  * </p>
  * 
  * @author Forge
  * @version $Id: InputConfirm.java 21647 2013-05-24 22:31:11Z Max mtg $
  */
public class InputConfirm extends InputSyncronizedBase {
    private static final long serialVersionUID = -3591794991788531626L;

    private final String message;
    private final String yesButtonText;
    private final String noButtonText;
    private final boolean defaultYes;
    private boolean result;
    private SpellAbility sa;
    private CardView card;

    // simple interface to hide ugliness deciding how to confirm
    protected static ImmutableList<String> defaultOptions = ImmutableList.of(Localizer.getInstance().getMessage("lblYes"), Localizer.getInstance().getMessage("lblNo"));
    public static boolean confirm(final PlayerControllerHuman controller, final CardView card, final String message) {
        return InputConfirm.confirm(controller, card, message, true, defaultOptions);
    }
    public static boolean confirm(final PlayerControllerHuman controller, final CardView card, final String message, final boolean defaultIsYes, final List<String> options) {
         if (GuiBase.getInterface().isLibgdxPort()) {
             return controller.getGui().confirm(card, message, defaultIsYes, options);
         } else {
             InputConfirm inp;
             if (options.size() == 2) {
                 inp = new InputConfirm(controller, message, options.get(0), options.get(1), defaultIsYes, card);
             } else { 
                 inp = new InputConfirm(controller, message, defaultOptions.get(0), defaultOptions.get(1), defaultIsYes, card);
             }
             inp.showAndWait();
             return inp.getResult();
         }
    }
    public static boolean confirm(final PlayerControllerHuman controller, final SpellAbility sa, final String message) {
        return InputConfirm.confirm(controller, sa, message, true, defaultOptions);
    }
    public static boolean confirm(final PlayerControllerHuman controller, final SpellAbility sa, final String message, final boolean defaultIsYes, final List<String> options) {
         if (GuiBase.getInterface().isLibgdxPort()) {
             if (sa == null)
                 return controller.getGui().confirm(null, message, defaultIsYes, options);
             if (sa.getTargets() != null && sa.getTargets().isTargetingAnyCard() && sa.getTargets().size() == 1)
                 return controller.getGui().confirm((sa.getTargetCard()==null)?null:CardView.get(sa.getTargetCard()), message, defaultIsYes, options);
             if (ApiType.Play.equals(sa.getApi()) && sa.getHostCard() != null && sa.getHostCard().getImprintedCards().size() == 1)
                 return controller.getGui().confirm((sa.getHostCard().getImprintedCards().get(0)==null)?null:CardView.get(sa.getHostCard().getImprintedCards().get(0)), message, defaultIsYes, options);
             return controller.getGui().confirm(CardView.get(sa.getHostCard()), message, defaultIsYes, options);
         } else {
             InputConfirm inp;
             if (options.size() == 2) {
                 inp = new InputConfirm(controller, message, options.get(0), options.get(1), defaultIsYes, sa);
             } else { 
                 inp = new InputConfirm(controller, message, defaultOptions.get(0), defaultOptions.get(1), defaultIsYes, sa);
             }
             inp.showAndWait();
             return inp.getResult();
         }
    }

    public InputConfirm(final PlayerControllerHuman controller, String message0) {
        this(controller, message0, Localizer.getInstance().getMessage("lblYes"), Localizer.getInstance().getMessage("lblNo"), true);
    }

    public InputConfirm(final PlayerControllerHuman controller, String message0, String yesButtonText0, String noButtonText0) {
        this(controller, message0, yesButtonText0, noButtonText0, true);
    }

    public InputConfirm(final PlayerControllerHuman controller, String message0, String yesButtonText0, String noButtonText0, boolean defaultYes0) {
        super(controller);
        message = message0;
        yesButtonText = yesButtonText0;
        noButtonText = noButtonText0;
        defaultYes = defaultYes0;
        result = defaultYes0;
        this.sa = null;
        this.card = null;
    }

    public InputConfirm(final PlayerControllerHuman controller, String message0, SpellAbility sa0) {
        this(controller, message0, Localizer.getInstance().getMessage("lblYes"), Localizer.getInstance().getMessage("lblNo"), true, sa0);
    }

    public InputConfirm(final PlayerControllerHuman controller, String message0, String yesButtonText0, String noButtonText0, SpellAbility sa0) {
        this(controller, message0, yesButtonText0, noButtonText0, true, sa0);
    }

    public InputConfirm(final PlayerControllerHuman controller, String message0, String yesButtonText0, String noButtonText0, boolean defaultYes0, SpellAbility sa0) {
        super(controller);
        message = message0;
        yesButtonText = yesButtonText0;
        noButtonText = noButtonText0;
        defaultYes = defaultYes0;
        result = defaultYes0;
        this.sa = sa0;
        this.card = sa != null ? sa.getView().getHostCard() : null;
    }

    public InputConfirm(final PlayerControllerHuman controller, String message0, CardView card0) {
        this(controller, message0, Localizer.getInstance().getMessage("lblYes"), Localizer.getInstance().getMessage("lblNo"), true, card0);
    }

    public InputConfirm(final PlayerControllerHuman controller, String message0, String yesButtonText0, String noButtonText0, CardView card0) {
        this(controller, message0, yesButtonText0, noButtonText0, true, card0);
    }

    public InputConfirm(final PlayerControllerHuman controller, String message0, String yesButtonText0, String noButtonText0, boolean defaultYes0, CardView card0) {
        super(controller);
        message = message0;
        yesButtonText = yesButtonText0;
        noButtonText = noButtonText0;
        defaultYes = defaultYes0;
        result = defaultYes0;
        this.sa = null;
        this.card = card0;
    }

    /** {@inheritDoc} */
    @Override
    protected final void showMessage() {
        getController().getGui().updateButtons(getOwner(), yesButtonText, noButtonText, true, true, defaultYes);
        if (FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_DETAILED_SPELLDESC_IN_PROMPT) && card != null) {
            final StringBuilder sb = new StringBuilder();
            sb.append(card.toString());
            if (sa != null && sa.toString().length() > 1) { // some spell abilities have no useful string value
                sb.append(" - ").append(sa.toString());
            }
            sb.append("\n\n").append(message);
            showMessage(sb.toString(), card);
        } else {
            if (card != null) {
                showMessage(message, card);
            } else {
                showMessage(message);
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        result = true;
        done();
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCancel() {
        result = false;
        done();
    }

    private void done() {
        stop();
    }

    public final boolean getResult() {
        return result;
    }

    @Override
    public String getActivateAction(Card card) {
        return null;
    }
}
