package forge.screens.settings;

import forge.Forge;
import forge.screens.FScreen;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextArea;
import forge.util.CommunityEditionInfo;

/** Scrollable release notes bundled with the community localization. */
public final class CommunityReleaseNotesScreen extends FScreen {
    private final FTextArea releaseNotes = add(new FTextArea(false, CommunityEditionInfo.getReleaseNotes()));

    public CommunityReleaseNotesScreen() {
        super(Forge.getLocalizer().getMessage("ReleaseNotes"));
    }

    @Override
    protected void doLayout(final float startY, final float width, final float height) {
        final float padding = FOptionPane.PADDING;
        releaseNotes.setBounds(padding, startY + padding,
                width - 2 * padding, height - startY - 2 * padding);
    }
}
