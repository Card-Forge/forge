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
package forge.screens.settings;

import java.net.Proxy;

import com.badlogic.gdx.Gdx;

import forge.UiCommand;
import forge.assets.FSkinFont;
import forge.download.GuiDownloadService;
import forge.toolbox.*;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FRadioButton.RadioButtonGroup;

public class GuiDownloader extends FDialog {
    public static final Proxy.Type[] TYPES = Proxy.Type.values();

    private final FProgressBar progressBar = add(new FProgressBar());
    private final FButton btnStart = add(new FButton("Start"));
    private final FButton btnCancel = add(new FButton("Cancel"));
    private final FTextField txtAddress = add(new FTextField());
    private final FTextField txtPort = add(new FTextField());
    private final FRadioButton radProxyNone = add(new FRadioButton("No Proxy"));
    private final FRadioButton radProxySocks = add(new FRadioButton("SOCKS Proxy"));
    private final FRadioButton radProxyHTTP = add(new FRadioButton("HTTP Proxy"));

    @SuppressWarnings("serial")
    private final UiCommand cmdClose = new UiCommand() {
        @Override
        public void run() {
            service.setCancel(true);
            hide();
        }
    };

    private final GuiDownloadService service;

    public GuiDownloader(GuiDownloadService service0) {
        super(service0.getTitle());
        service = service0;

        txtAddress.setGhostText("Proxy Address");
        txtPort.setGhostText("Proxy Port");
        txtAddress.setEnabled(false);
        txtPort.setEnabled(false);

        RadioButtonGroup group = new RadioButtonGroup();
        radProxyNone.setGroup(group);
        radProxyHTTP.setGroup(group);
        radProxySocks.setGroup(group);

        radProxyNone.setCommand(new ProxyHandler(0));
        radProxyHTTP.setCommand(new ProxyHandler(1));
        radProxySocks.setCommand(new ProxyHandler(2));
        radProxyNone.setSelected(true);

        btnStart.setFont(FSkinFont.get(18));
        btnStart.setEnabled(false);
        btnCancel.setFont(btnStart.getFont());
        btnCancel.setCommand(cmdClose);

        progressBar.reset();
        progressBar.setShowProgressTrail(true);
        progressBar.setDescription("Scanning for existing items...");
        Gdx.graphics.setContinuousRendering(true);

        show();

        service.initialize(txtAddress, txtPort, progressBar, btnStart, cmdClose, new Runnable() {
            @Override
            public void run() {
                Gdx.graphics.setContinuousRendering(false);
                progressBar.setShowProgressTrail(false);
            }
        }, null);
    }

    private class ProxyHandler implements FEventHandler {
        private final int type;

        public ProxyHandler(final int type) {
            this.type = type;
        }

        @Override
        public void handleEvent(FEvent e) {
            if (((FRadioButton) e.getSource()).isSelected()) {
                service.setType(this.type);
                txtAddress.setEnabled(this.type != 0);
                txtPort.setEnabled(this.type != 0);
            }
        }
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FDialog.INSETS;
        float x = padding;
        float y = padding;
        float w = width - 2 * padding;
        float radioButtonWidth = w / 3;
        float radioButtonHeight = radProxyNone.getAutoSizeBounds().height;

        radProxyNone.setBounds(x, y, radioButtonWidth, radioButtonHeight);
        x += radioButtonWidth;
        radProxyHTTP.setBounds(x, y, radioButtonWidth, radioButtonHeight);
        x += radioButtonWidth;
        radProxySocks.setBounds(x, y, radioButtonWidth, radioButtonHeight);

        x = padding;
        y += radioButtonHeight + padding;
        txtAddress.setBounds(x, y, w, txtAddress.getHeight());
        y += txtAddress.getHeight() + padding;
        txtPort.setBounds(x, y, w, txtPort.getHeight());
        y += txtPort.getHeight() + padding * 2;
        progressBar.setBounds(x, y, w, txtPort.getHeight() * 1.5f);
        y += progressBar.getHeight() + padding * 2;

        float buttonWidth = (w - padding) / 2;
        float buttonHeight = txtPort.getHeight() * 1.5f;
        btnStart.setBounds(x, y, buttonWidth, buttonHeight);
        x += w - buttonWidth;
        btnCancel.setBounds(x, y, buttonWidth, buttonHeight);
        return y + buttonHeight + padding;
    }
}
