package forge.game.ability.effects;

import forge.GameCommand;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class DebuffEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final List<String> kws = sa.hasParam("Keywords") ? Arrays.asList(sa.getParam("Keywords").split(" & ")) : new ArrayList<String>();
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
            if (!sa.hasParam("Permanent")) {
                sb.append(" until end of turn");
            }
            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final List<String> kws = new ArrayList<String>();
        if (sa.hasParam("Keywords")) {
            kws.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }
        final Game game = sa.getActivatingPlayer().getGame();
        final long timestamp = game.getNextTimestamp();

        for (final Card tgtC : getTargetCards(sa)) {
            final List<String> hadIntrinsic = new ArrayList<String>();
            final List<String> addedKW = new ArrayList<String>();
            final List<String> removedKW = new ArrayList<String>();
            if (tgtC.isInPlay() && tgtC.canBeTargetedBy(sa)) {
                if (sa.hasParam("AllSuffixKeywords")) {
                    String suffix = sa.getParam("AllSuffixKeywords");
                    for (final String keyword : tgtC.getKeywords()) {
                        if (keyword.endsWith(suffix)) {
                            kws.add(keyword);
                        }
                    }
                }

                // special for Protection:Card.<color>:Protection from <color>:*             
                for (final String keyword : tgtC.getKeywords()) {
                    if (keyword.startsWith("Protection:")) {
                        for (final String kw : kws) {
                            if (keyword.matches("(?i).*:" + kw + ":.*"))
                                removedKW.add(keyword);
                        }
                    }
                }

                boolean ProtectionFromColor = false;
                for (final String kw : kws) {
                    // Check if some of the Keywords are Protection from <color>
                    if (!ProtectionFromColor && kw.startsWith("Protection from ")) {
                        for(byte col : MagicColor.WUBRG) {
                            final String colString = MagicColor.toLongString(col);
                            if (kw.endsWith(colString.toLowerCase())) {
                                ProtectionFromColor = true;
                            }
                        }
                    }

                    if (tgtC.getCurrentState().hasIntrinsicKeyword(kw)) {
                        hadIntrinsic.add(kw);
                    }
                    tgtC.removeIntrinsicKeyword(kw);
                    tgtC.removeAllExtrinsicKeyword(kw);
                }

                // Split "Protection from all colors" into extra Protection from <color>
                String allColors = "Protection from all colors";
                if (ProtectionFromColor && tgtC.hasKeyword(allColors)) {
                    final List<String> allColorsProtect = new ArrayList<String>();

                    for(byte col : MagicColor.WUBRG) {
                        allColorsProtect.add("Protection from " + MagicColor.toLongString(col).toLowerCase());
                    }
                    if (tgtC.getCurrentState().hasIntrinsicKeyword(allColors)) {
                        hadIntrinsic.add(allColors);
                    }
                    tgtC.removeIntrinsicKeyword(allColors);
                    tgtC.removeAllExtrinsicKeyword(allColors);
                    allColorsProtect.removeAll(kws);
                    addedKW.addAll(allColorsProtect);
                    removedKW.add(allColors);
                }

                // Extra for Spectra Ward
                allColors = "Protection:Card.nonColorless:Protection from all colors:Aura";
                if (ProtectionFromColor && tgtC.hasKeyword(allColors)) {
                    final List<String> allColorsProtect = new ArrayList<String>();

                    for(byte col : MagicColor.WUBRG) {
                        final String colString = MagicColor.toLongString(col);
                        if (!kws.contains("Protection from " + colString)) {
                            allColorsProtect.add(
                                "Protection:Card." + StringUtils.capitalize(colString) +
                                ":Protection from " + colString + ":Aura"
                            );
                        }
                    }
                    if (tgtC.getCurrentState().hasIntrinsicKeyword(allColors)) {
                        hadIntrinsic.add(allColors);
                    }
                    tgtC.removeIntrinsicKeyword(allColors);
                    tgtC.removeAllExtrinsicKeyword(allColors);
                    addedKW.addAll(allColorsProtect);
                    removedKW.add(allColors);
                }

                removedKW.addAll(kws);
                tgtC.addChangedCardKeywords(addedKW, removedKW, false, timestamp);
            }
            if (!sa.hasParam("Permanent")) {
                game.getEndOfTurn().addUntil(new GameCommand() {
                    private static final long serialVersionUID = 5387486776282932314L;

                    @Override
                    public void run() {
                        tgtC.removeChangedCardKeywords(timestamp);
                        if (tgtC.isInPlay()) {
                            for (final String kw : hadIntrinsic) {
                                tgtC.addIntrinsicKeyword(kw);
                            }
                        }
                    }
                });
            }
        }

    } // debuffResolve

}
