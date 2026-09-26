package forge.deck;

import forge.deck.io.DeckStorage;
import forge.localinstance.properties.ForgeConstants;
import forge.util.Localizer;
import forge.util.storage.StorageImmediatelySerialized;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Syncs all public Deckbox.org decks for a username into local Forge deck folders.
 */
public final class DeckboxSync {
    private static final Localizer localizer = Localizer.getInstance();

    private DeckboxSync() {
    }

    /**
     * Download every public folder/deck for {@code username} into
     * {@code decks/deckbox/{username}/{folder}/}.
     *
     * @return number of decks written
     */
    public static int sync(final String username) throws IOException {
        if (!DeckboxUtil.hasUsername(username)) {
            throw new IOException(localizer.getMessage("lblDeckboxUsernameRequired"));
        }
        if (!DeckboxUtil.ensureUserDirectory(username)) {
            throw new IOException(localizer.getMessage("lblDeckboxCouldNotCreateDirectory"));
        }

        final Map<String, List<DeckboxClient.RemoteDeckRef>> folders = DeckboxClient.listFolders(username);
        DeckboxUtil.clearLocalFolders(username);
        // Always expose {username}-root after a successful listing, even if it is empty.
        DeckboxUtil.ensureDeckDirectory(username, DeckboxUtil.getRootFolderName(username));
        int written = 0;
        IOException lastError = null;

        for (final Map.Entry<String, List<DeckboxClient.RemoteDeckRef>> entry : folders.entrySet()) {
            final String folderName = entry.getKey();
            final List<DeckboxClient.RemoteDeckRef> remoteDecks = entry.getValue();
            if (remoteDecks == null || remoteDecks.isEmpty()) {
                continue;
            }
            if (!DeckboxUtil.ensureDeckDirectory(username, folderName)) {
                lastError = new IOException(localizer.getMessage("lblDeckboxCouldNotCreateDirectory"));
                continue;
            }

            final File dir = DeckboxUtil.getDeckDirectory(username, folderName);
            final StorageImmediatelySerialized<Deck> storage = new StorageImmediatelySerialized<>("Deckbox decks",
                    new DeckStorage(dir, ForgeConstants.DECK_BASE_DIR), true);

            for (final DeckboxClient.RemoteDeckRef remote : remoteDecks) {
                try {
                    final DeckUrlProvider.RemoteDeck loaded = DeckboxClient.loadDeck(remote, storage);
                    final Deck deck = DeckUrlLoader.importDeck(loaded);
                    storage.add(deck);
                    written++;
                } catch (final IOException ex) {
                    lastError = ex;
                }
            }
        }

        if (written == 0 && lastError != null) {
            throw lastError;
        }
        if (written == 0) {
            throw new IOException(localizer.getMessage("lblDeckboxSyncFailed", username));
        }
        return written;
    }
}
