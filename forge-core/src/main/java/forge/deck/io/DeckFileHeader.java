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
package forge.deck.io;

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import forge.deck.DeckFormat;
import forge.util.FileSection;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class DeckFileHeader {

    /** The Constant NAME. */
    public static final String NAME = "Name";

    /** The Constant DECK_TYPE. */
    public static final String DECK_TYPE = "Deck Type";
    public static final String TAGS = "Tags";
    
    public static final String TAGS_SEPARATOR = ",";
    
    /** The Constant COMMENT. */
    public static final String COMMENT = "Comment";
    private static final String PLAYER = "Player";
    private static final String CSTM_POOL = "Custom Pool";
    private static final String PLAYER_TYPE = "PlayerType";
    public static final String AI_HINTS = "AiHints";

    private final DeckFormat deckType;
    private final boolean customPool;

    private final String name;
    private final String comment;

    private final Set<String> tags;

    private final boolean intendedForAi;
    private final String aiHints;

    /**
     * @return the intendedForAi
     */
    public boolean isIntendedForAi() {
        return intendedForAi;
    }

    /**
     * @return the AI hints
     */
    public String getAiHints() {
        return aiHints;
    }

    /**
     * TODO: Write javadoc for Constructor.
     * 
     * @param kvPairs
     *            the kv pairs
     */
    public DeckFileHeader(final FileSection kvPairs) {
        this.name = kvPairs.get(DeckFileHeader.NAME);
        this.comment = kvPairs.get(DeckFileHeader.COMMENT);
        this.deckType = DeckFormat.smartValueOf(kvPairs.get(DeckFileHeader.DECK_TYPE), DeckFormat.Constructed);
        this.customPool = kvPairs.getBoolean(DeckFileHeader.CSTM_POOL);
        this.intendedForAi = "computer".equalsIgnoreCase(kvPairs.get(DeckFileHeader.PLAYER)) || "ai".equalsIgnoreCase(kvPairs.get(DeckFileHeader.PLAYER_TYPE));
        this.aiHints = kvPairs.get(DeckFileHeader.AI_HINTS);
        this.tags = new TreeSet<>();
        
        String rawTags = kvPairs.get(DeckFileHeader.TAGS);
        if( StringUtils.isNotBlank(rawTags) ) {
            for( String t: rawTags.split(TAGS_SEPARATOR))
                if ( StringUtils.isNotBlank(t))
                    tags.add(t.trim());
        }
        
            
    }

    /**
     * Checks if is custom pool.
     * 
     * @return true, if is custom pool
     */
    public final boolean isCustomPool() {
        return this.customPool;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Gets the comment.
     * 
     * @return the comment
     */
    public final String getComment() {
        return this.comment;
    }

    /**
     * Gets the deck type.
     * 
     * @return the deck type
     */
    public final DeckFormat getDeckType() {
        return this.deckType;
    }

    public final Set<String> getTags() {
        return tags;
    }

}
