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
package forge.card;

import java.io.File;

/**
 * Info pertaining to a card script file
 * 
 */
public final class CardScriptInfo {
	private String text;
	private File file;
	private CardRules rules;

    public CardScriptInfo(String text0, File file0, CardRules rules0) {
    	this.text = text0;
        this.file = file0;
        this.rules = rules0;
    }

    public String getText() {
    	return this.text;
    }

    public File getFile() {
    	return this.file;
    }

    public boolean canEdit() {
    	return this.file != null;
    }

    public CardRules getRules() {
    	return this.rules;
    }
}
