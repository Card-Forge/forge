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
package forge.gui.download;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;

import forge.CardUtil;
import forge.Singletons;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.properties.NewConstants;
import forge.util.Base64Coder;

@SuppressWarnings("serial")
public class GuiDownloadSetPicturesLQ extends GuiDownloader {
    public GuiDownloadSetPicturesLQ() {
        super();
    }

    private final void addCardToList(ArrayList<DownloadObject> cList, CardPrinted c, String nameToUse) {
        File file = new File(NewConstants.CACHE_CARD_PICS_DIR, CardUtil.buildFilename(c, nameToUse) + ".jpg");
        if (!file.exists()) {
            cList.add(new DownloadObject(getCardPictureUrl(c, nameToUse), file));
        }

        final String setCode3 = c.getEdition();
        final CardEdition thisSet = Singletons.getModel().getEditions().get(setCode3);
        System.out.println(String.format("%s [%s - %s]", nameToUse, setCode3, thisSet.getName()));
    }

    private static String cleanMWS(String in) {
        final StringBuffer out = new StringBuffer();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c == '"') || (c == '/') || (c == ':') || (c == '?')) {
                out.append("");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
    
    public static String getCardPictureUrl(final CardPrinted c, final String cardName) {
        String setCode3 = c.getEdition();
        String setCode2 = Singletons.getModel().getEditions().get(setCode3).getCode2();
        int artsCnt = c.getRules().getEditionInfo(setCode3).getCopiesCount();
        String filename = CardUtil.buildFilename(cleanMWS(cardName), null, c.getArtIndex(), artsCnt, false) + ".jpg";

        return NewConstants.URL_PIC_DOWNLOAD + setCode2 + "/" + Base64Coder.encodeString(filename, true);
    }

    @Override
    protected final ArrayList<DownloadObject> getNeededImages() {
        final ArrayList<DownloadObject> cList = new ArrayList<DownloadObject>();

        Iterable<CardPrinted> allPrinted = Iterables.concat(CardDb.instance().getAllCards(), CardDb.variants().getAllCards());

        for (final CardPrinted c : allPrinted) {
            final String setCode3 = c.getEdition();
            if (StringUtils.isBlank(setCode3) || "???".equals(setCode3)) {
                continue; // we don't want cards from unknown sets
            }
            
            CardRules cr = c.getRules();
            String firstPartName = cr.getSplitType() == CardSplitType.Split ? CardUtil.buildSplitCardFilename(cr) : c.getName();
            addCardToList(cList, c, firstPartName);

            if (cr.getSplitType() == CardSplitType.Transform) {
                addCardToList(cList, c, cr.getOtherPart().getName());
            }
        }

        // add missing tokens to the list of things to download
        for (final DownloadObject element : GuiDownloader.readFileWithNames(NewConstants.IMAGE_LIST_TOKENS_FILE, NewConstants.CACHE_TOKEN_PICS_DIR)) {
            if (!element.getDestination().exists()) {
                cList.add(element);
            }
        }

        return cList;
    }
}
