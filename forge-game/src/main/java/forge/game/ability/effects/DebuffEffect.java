package forge.game.ability.effects;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.spellability.SpellAbility;

public class DebuffEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final List<String> kws = Lists.newArrayList();
        if (sa.hasParam("Keywords")) {
            kws.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getTargetCards(sa);

        if (tgtCards.size() > 0) {
            final Iterator<Card> it = tgtCards.iterator();
            while (it.hasNext()) {
                final Card tgtC = it.next();
                if (tgtC.isFaceDown()) {
                    sb.append("Morph");
                } else {
                    sb.append(tgtC);
                }

                if (it.hasNext()) {
                    sb.append(" ");
                }
            }
            sb.append(" loses ");
            /*
             * Iterator<String> kwit = kws.iterator(); while(it.hasNext()) {
             * String kw = kwit.next(); sb.append(kw); if(it.hasNext())
             * sb.append(" "); }
             */
            sb.append(kws);
            if (!"Permanent".equals(sa.getParam("Duration"))) {
                sb.append(" until end of turn");
            }
            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final List<String> kws = Lists.newArrayList();
        if (sa.hasParam("Keywords")) {
            kws.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }
        final Game game = sa.getActivatingPlayer().getGame();
        final long timestamp = game.getNextTimestamp();

        for (final Card tgtC : getTargetCards(sa)) {
            if (!tgtC.isInPlay()) {
                continue;
            }
            if (tgtC.isPhasedOut()) {
                continue;
            }

            // check if the object is still in game or if it was moved
            Card gameCard = game.getCardState(tgtC, null);
            // gameCard is LKI in that case, the card is not in game anymore
            // or the timestamp did change
            // this should check Self too
            if (gameCard == null || !tgtC.equalsWithGameTimestamp(gameCard)) {
                continue;
            }

            final List<String> addedKW = Lists.newArrayList();
            final List<String> removedKW = Lists.newArrayList();
            if (sa.hasParam("AllSuffixKeywords")) {
                // this only for walk abilities, may to try better
                if (sa.getParam("AllSuffixKeywords").equals("walk")) {
                    for (final KeywordInterface kw : gameCard.getKeywords(Keyword.LANDWALK)) {
                        removedKW.add(kw.getOriginal());
                    }
                }
            }

            boolean ProtectionFromColor = false;
            for (final String kw : kws) {
                // Check if some of the Keywords are Protection from <color>
                if (!kw.startsWith("Protection from ")) {
                    continue;
                }
                for (byte col : MagicColor.WUBRG) {
                    final String colString = MagicColor.toLongString(col);
                    if (!kw.endsWith(colString)) {
                        continue;
                    }
                    final String wardString = StringUtils.capitalize(colString) + ":" + colString;
                    for (final KeywordInterface inst : gameCard.getKeywords(Keyword.PROTECTION)) {
                        // special for the Ward Auras Protection:Card.<Color>:<color>:*
                        String keyword = inst.getOriginal();
                        if (keyword.startsWith("Protection:") && keyword.contains(wardString)) {
                            removedKW.add(keyword);
                        }
                    }
                }
                ProtectionFromColor = true;
            }
            if (ProtectionFromColor) {
                // Split "Protection from each color" into extra Protection from <color>
                String allColors = "Protection from each color";
                if (gameCard.hasKeyword(allColors)) {
                    final List<String> allColorsProtect = Lists.newArrayList();

                    for (byte col : MagicColor.WUBRG) {
                        allColorsProtect.add("Protection from " + MagicColor.toLongString(col));

                    }
                    allColorsProtect.removeAll(kws);
                    addedKW.addAll(allColorsProtect);
                    removedKW.add(allColors);
                }

                // Extra for Spectra Ward
                allColors = "Protection:Card.nonColorless:each color:Aura";
                if (gameCard.hasKeyword(allColors)) {
                    final List<String> allColorsProtect = Lists.newArrayList();

                    for (byte col : MagicColor.WUBRG) {
                        final String colString = MagicColor.toLongString(col);
                        if (!kws.contains("Protection from " + colString)) {
                            allColorsProtect.add("Protection:Card." + StringUtils.capitalize(colString) + ":" + colString + ":Aura");
                        }
                    }
                    addedKW.addAll(allColorsProtect);
                    removedKW.add(allColors);
                }
            }

            removedKW.addAll(kws);
            gameCard.addChangedCardKeywords(addedKW, removedKW, false, timestamp, null);

            if (!"Permanent".equals(sa.getParam("Duration"))) {
                final GameCommand until = new GameCommand() {
                    private static final long serialVersionUID = 5387486776282932314L;

                    @Override
                    public void run() {
                        gameCard.removeChangedCardKeywords(timestamp, 0);
                    }
                };
                addUntilCommand(sa, until);
            }
        }
    }

}
