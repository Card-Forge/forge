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
import forge.download.GuiDownloadService;
import forge.download.GuiDownloadZipService;
import forge.toolbox.*;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FRadioButton.RadioButtonGroup;
import forge.util.Callback;
import forge.util.Utils;

public class GuiDownloader extends FDialog {
    public static final Proxy.Type[] TYPES = Proxy.Type.values();
    private static final float PADDING = Utils.scale(10);

    private final FProgressBar progressBar = add(new FProgressBar());
    private final FTextField txtAddress = add(new FTextField());
    private final FTextField txtPort = add(new FTextField());
    private final FRadioButton radProxyNone = add(new FRadioButton("No Proxy"));
    private final FRadioButton radProxySocks = add(new FRadioButton("SOCKS Proxy"));
    private final FRadioButton radProxyHTTP = add(new FRadioButton("HTTP Proxy"));

    @SuppressWarnings("serial")
    private final UiCommand cmdClose = new UiCommand() {
        @Override
        public void run() {
            Gdx.graphics.setContinuousRendering(false);
            service.setCancel(true);
            hide();
            if (callback != null) {
                callback.run(getButton(0).getText() == "OK"); //determine result based on whether download finished
            }
        }
    };

    private final GuiDownloadService service;
    private final Callback<Boolean> callback;

    public GuiDownloader(GuiDownloadService service0) {
        this(service0, null);
    }
    public GuiDownloader(GuiDownloadService service0, Callback<Boolean> callback0) {
        super(service0.getTitle(), 2);
        service = service0;
        callback = callback0;

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

        getButton(0).setText("Start");
        initButton(1, "Cancel", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                cmdClose.run();
            }
        });

        progressBar.reset();
        progressBar.setShowProgressTrail(true);
        progressBar.setDescription("Scanning for existing items...");
        Gdx.graphics.setContinuousRendering(true);

        show();

        service.initialize(txtAddress, txtPort, progressBar, getButton(0), cmdClose, new Runnable() {
            @Override
            public void run() {
                if (!(service instanceof GuiDownloadZipService)) { //retain continuous rendering for zip service
                    Gdx.graphics.setContinuousRendering(false);
                }
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
        float x = PADDING;
        float y = PADDING;
        float w = width - 2 * PADDING;
        float radioButtonWidth = w / 3;
        float radioButtonHeight = radProxyNone.getAutoSizeBounds().height;

        radProxyNone.setBounds(x, y, radioButtonWidth, radioButtonHeight);
        x += radioButtonWidth;
        radProxyHTTP.setBounds(x, y, radioButtonWidth, radioButtonHeight);
        x += radioButtonWidth;
        radProxySocks.setBounds(x, y, radioButtonWidth, radioButtonHeight);

        x = PADDING;
        y += radioButtonHeight + PADDING;
        txtAddress.setBounds(x, y, w, txtAddress.getHeight());
        y += txtAddress.getHeight() + PADDING;
        txtPort.setBounds(x, y, w, txtPort.getHeight());
        y += txtPort.getHeight() + PADDING * 2;
        progressBar.setBounds(x, y, w, txtPort.getHeight() * 1.5f);
        y += progressBar.getHeight() + PADDING * 2;
        return y;
    }
}
