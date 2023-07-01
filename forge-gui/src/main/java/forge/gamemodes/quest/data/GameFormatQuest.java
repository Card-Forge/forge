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
package forge.gamemodes.quest.data;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import forge.card.CardEdition;
import forge.game.GameFormat;
import forge.gamemodes.quest.setrotation.ISetRotation;
import forge.item.PaperCard;
import forge.model.FModel;


/**
 * This is an alternate game format type, the main difference is that this
 * is not immutable. This class is necessary because we may wish to update
 * its contents in certain circumstances, and it was safer to create a new
 * class than to make the preset game formats modifiable.
 */
public final class GameFormatQuest extends GameFormat {

	private final boolean allowUnlocks;
	private int unlocksUsed = 0;

	private ISetRotation setRotation;

	@Override
	public List<String> getAllowedSetCodes() {
		if(setRotation != null)
			return setRotation.getCurrentSetCodes(super.getAllowedSetCodes());
		return super.getAllowedSetCodes();
	}

	@Override
	public Predicate<PaperCard> getFilterRules() {
		// Filter must be continuously rebuilt if the format is mutable
		if(setRotation != null)
			return super.buildFilter(false);
		return super.getFilterRules();
	}

	@Override
	public Predicate<PaperCard> getFilterPrinted() {
		// Filter must be continuously rebuilt if the format is mutable
		if(setRotation != null)
			return super.buildFilter(true);
		return super.getFilterPrinted();
	}

	/**
	 * Instantiates a new game format based on two lists.
	 *
	 * @param newName     String, the name
	 * @param setsToAllow List<String>, these are the allowed sets
	 * @param cardsToBan  List<String>, these will be the banned cards
	 */
	public GameFormatQuest(final String newName, final List<String> setsToAllow, final List<String> cardsToBan) {
		super(newName, setsToAllow, cardsToBan);
		allowUnlocks = false;
		setRotation = null;
	}

	public GameFormatQuest(final String newName, final List<String> setsToAllow, final List<String> cardsToBan, boolean allowSetUnlocks) {
		super(newName, setsToAllow, cardsToBan);
		allowUnlocks = allowSetUnlocks;
		setRotation = null;
	}

	/**
	 * Instantiates a format that uses an ISetRotation to automatically rotate sets in and out
	 * @param newName
	 * @param setsToAllow
	 * @param cardsToBan
	 * @param allowSetUnlocks
	 * @param setRotation	  an ISetRotation that determines the currently allowed sets
	 */
	public GameFormatQuest(final String newName, final List<String> setsToAllow, final List<String> cardsToBan, boolean allowSetUnlocks, ISetRotation setRotation) {
		super(newName, setsToAllow, cardsToBan);
		allowUnlocks = allowSetUnlocks;
		this.setRotation = setRotation;
	}

	/**
	 * Instantiates a new game format based on an existing format.
	 *
	 * @param toCopy          an existing format
	 * @param allowSetUnlocks
	 * @param setRotation
	 */
	public GameFormatQuest(final GameFormat toCopy, boolean allowSetUnlocks, ISetRotation setRotation) {
		super(toCopy.getName(), toCopy.getEffectiveDate(), toCopy.getAllowedSetCodes(), toCopy.getBannedCardNames(), toCopy.getRestrictedCards(),
				toCopy.isRestrictedLegendary(),toCopy.getAdditionalCards(), toCopy.getAllowedRarities(),
				toCopy.getIndex(), FormatType.CUSTOM, FormatSubType.CUSTOM);
		allowUnlocks = allowSetUnlocks;
		this.setRotation = setRotation;
	}

	/**
	 * Get the list of excluded sets.
	 *
	 * @return unmodifiable list of excluded sets.
	 */
	public List<String> getLockedSets() {

		List<String> exSets = new ArrayList<>();
		if (this.allowedSetCodes.isEmpty()) {
			return exSets;
		}

		for (CardEdition ce : FModel.getMagicDb().getEditions()) {
			if (!isSetLegal(ce.getCode())) {
				exSets.add(ce.getCode());
			}
		}
		return exSets;
	}

	/**
	 * Add a set to allowed set codes.
	 *
	 * @param setCode String, set code.
	 */
	public void unlockSet(final String setCode) {
		if (!canUnlockSets() || this.allowedSetCodes_ro.isEmpty() || this.allowedSetCodes_ro.contains(setCode)) {
			return;
		}
		this.allowedSetCodes.add(setCode);
		unlocksUsed++;
	}

	/**
	 * Checks if the current format contains sets with snow-land (horrible hack...).
	 *
	 * @return boolean, contains snow-land sets.
	 */
	public boolean hasSnowLands() {
		return (this.isSetLegal("ICE") || this.isSetLegal("CSP"));
	}

	public boolean canUnlockSets() {
		return allowUnlocks;
	}

	public int getUnlocksUsed() {
		return unlocksUsed;
	}

	public abstract static class QPredicates {
		/**
		 * Checks if is legal in quest format.
		 *
		 * @param qFormat the format
		 * @return the predicate
		 */
		public static Predicate<CardEdition> isLegalInFormatQuest(final GameFormatQuest qFormat) {
			return new LegalInFormatQuest(qFormat);
		}

		private static final class LegalInFormatQuest implements Predicate<CardEdition> {
			private final GameFormatQuest qFormat;

			private LegalInFormatQuest(final GameFormatQuest fmt) {
				this.qFormat = fmt;
			}

			@Override
			public boolean apply(final CardEdition subject) {
				return this.qFormat.isSetLegal(subject.getCode());
			}
		}

	}

}
