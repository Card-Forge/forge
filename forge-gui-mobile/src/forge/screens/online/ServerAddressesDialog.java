package forge.screens.online;

import com.badlogic.gdx.utils.Align;
import com.google.common.collect.ImmutableList;

import forge.Forge;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.gamemodes.net.NetConnectUtil;
import forge.gui.GuiBase;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.toolbox.FTextArea;
import forge.util.Localizer;
import forge.util.Utils;

/**
 * Mobile equivalent of the desktop "Server URL" dialog. Lists every reachable address
 * (External + each local interface) with its own Copy button. Auto-copies and stars
 * the previously remembered URL so repeat hosts on the same network don't need to
 * pick the same row each time.
 */
public final class ServerAddressesDialog {
    private ServerAddressesDialog() { }

    private static final float SIDE_PADDING = Utils.scale(10);
    private static final float ROW_GAP = Utils.scale(8);
    private static final float ROW_PADDING = Utils.scale(10);
    private static final float COPY_BTN_WIDTH = Utils.AVG_FINGER_WIDTH * 1.1f;
    private static final float COPY_BTN_HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.9f;

    public static void show() {
        final Localizer localizer = Localizer.getInstance();
        final NetConnectUtil.ServerAddressList addresses = NetConnectUtil.collectHostedServerAddresses();
        if (addresses.starIndex >= 0) {
            GuiBase.getInterface().copyToClipboard(addresses.urls.get(addresses.starIndex));
        }

        final FOptionPane[] holder = new FOptionPane[1];
        final AddressList list = new AddressList(addresses, holder, localizer);
        holder[0] = new FOptionPane(
                localizer.getMessage("lblChooseAddressToCopy"),
                FSkinFont.get(12),
                localizer.getMessage("lblServerURL"),
                FOptionPane.INFORMATION_ICON,
                list,
                ImmutableList.of(localizer.getMessage("lblOK")),
                0,
                null);
        holder[0].show();
    }

    private static final class AddressList extends FScrollPane {
        private final FLabel[] labels;
        private final FLabel[] urls;
        private final FButton[] copyButtons;
        private final FTextArea footer;
        private final FSkinFont labelFont = FSkinFont.get(12);
        private final FSkinFont urlFont = FSkinFont.get(14);

        AddressList(final NetConnectUtil.ServerAddressList addresses, final FOptionPane[] holder, final Localizer localizer) {
            final int n = addresses.urls.size();
            labels = new FLabel[n];
            urls = new FLabel[n];
            copyButtons = new FButton[n];

            for (int i = 0; i < n; i++) {
                final String url = addresses.urls.get(i);
                final FLabel.Builder labelBuilder = new FLabel.Builder()
                        .text(addresses.labels.get(i))
                        .font(labelFont)
                        .align(Align.left)
                        .textColor(FSkinColor.get(FSkinColor.Colors.CLR_TEXT));
                if (i == addresses.starIndex) {
                    labelBuilder.icon(FSkinImage.STAR_FILLED).iconScaleFactor(1f);
                }
                labels[i] = add(labelBuilder.build());
                urls[i] = add(new FLabel.Builder()
                        .text(url)
                        .font(urlFont)
                        .align(Align.left)
                        .textColor(FSkinColor.get(FSkinColor.Colors.CLR_TEXT))
                        .build());
                final FButton btnCopy = new FButton(localizer.getMessage("lblCopy"));
                btnCopy.setCommand(e -> {
                    GuiBase.getInterface().copyToClipboard(url);
                    NetConnectUtil.rememberCopiedServerUrl(url);
                    if (holder[0] != null) {
                        holder[0].hide();
                    }
                });
                copyButtons[i] = add(btnCopy);
            }

            if (addresses.starIndex >= 0) {
                footer = new FTextArea(false,
                        localizer.getMessage("lblServerUrlCopiedToClipboard", addresses.urls.get(addresses.starIndex)));
                footer.setFont(FSkinFont.get(11));
                add(footer);
            } else {
                footer = null;
            }

            final float contentWidth = Forge.getScreenWidth() - 2 * FOptionPane.PADDING - 2 * SIDE_PADDING;
            // Reserve some headroom for the icon + prompt + buttons that sit above and below the scroll viewport.
            final float maxVisibleHeight = FOptionPane.getMaxDisplayObjHeight() * 0.85f;
            setHeight(Math.min(computeFullHeight(contentWidth), maxVisibleHeight));
        }

        private float computeFullHeight(final float contentWidth) {
            final float rowTextHeight = labelFont.getLineHeight() + urlFont.getLineHeight();
            final float rowHeight = Math.max(rowTextHeight, COPY_BTN_HEIGHT) + 2 * ROW_PADDING;
            float h = rowHeight * labels.length;
            if (footer != null) {
                h += ROW_GAP + footer.getPreferredHeight(contentWidth) + SIDE_PADDING;
            }
            return h;
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(final float visibleWidth, final float visibleHeight) {
            final float contentX = SIDE_PADDING;
            final float contentWidth = visibleWidth - 2 * SIDE_PADDING;
            float y = 0;

            final float rowTextHeight = labelFont.getLineHeight() + urlFont.getLineHeight();
            final float rowHeight = Math.max(rowTextHeight, COPY_BTN_HEIGHT) + 2 * ROW_PADDING;
            final float btnWidth = Math.min(COPY_BTN_WIDTH, contentWidth * 0.25f);
            final float textX = contentX + btnWidth + 2 * ROW_PADDING;
            final float textWidth = contentWidth - btnWidth - 2 * ROW_PADDING;

            for (int i = 0; i < labels.length; i++) {
                final float rowTop = y + ROW_PADDING;
                final float labelHeight = labelFont.getLineHeight();
                final float urlHeight = urlFont.getLineHeight();
                copyButtons[i].setBounds(contentX, y + (rowHeight - COPY_BTN_HEIGHT) / 2, btnWidth, COPY_BTN_HEIGHT);
                labels[i].setBounds(textX, rowTop, textWidth, labelHeight);
                urls[i].setBounds(textX, rowTop + labelHeight, textWidth, urlHeight);
                y += rowHeight;
            }

            if (footer != null) {
                y += ROW_GAP;
                footer.setBounds(contentX, y, contentWidth, footer.getPreferredHeight(contentWidth));
                y += footer.getHeight() + SIDE_PADDING;
            }

            return new ScrollBounds(visibleWidth, y);
        }
    }
}
