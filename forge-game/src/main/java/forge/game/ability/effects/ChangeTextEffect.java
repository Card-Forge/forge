package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;
import forge.util.TextUtil;

public class ChangeTextEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final Long timestamp = game.getNextTimestamp();
        final boolean permanent = "Permanent".equals(sa.getParam("Duration"));

        final String changedColorWordOriginal, changedColorWordNew;
        if (sa.hasParam("ChangeColorWord")) {
            // all instances are Choose Choose
            MagicColor.Color originalColor = null;
            final String[] changedColorWordsArray = sa.getParam("ChangeColorWord").split(" ");
            if (changedColorWordsArray[0].equals("Choose")) {
                originalColor = sa.getActivatingPlayer().getController().chooseColor(
                        Localizer.getInstance().getMessage("lblChooseColorReplace"), sa, ColorSet.ALL_COLORS);
                changedColorWordOriginal = TextUtil.capitalize(originalColor.getName());
            } else {
                changedColorWordOriginal = changedColorWordsArray[0];
                originalColor = MagicColor.Color.fromByte(MagicColor.fromName(changedColorWordOriginal));
            }

            if (changedColorWordsArray[1].equals("Choose")) {
                final ColorSet possibleNewColors;
                if (originalColor == null) { // no original color (ie. any or absent)
                    possibleNewColors = ColorSet.ALL_COLORS;
                } else { // may choose any except original color
                    possibleNewColors = ColorSet.fromMask(originalColor.getColormask()).inverse();
                }
                final byte newColor = sa.getActivatingPlayer().getController().chooseColor(
                        Localizer.getInstance().getMessage("lblChooseNewColor"), sa, possibleNewColors).getColormask();
                changedColorWordNew = TextUtil.capitalize(MagicColor.toLongString(newColor));
            } else {
                changedColorWordNew = changedColorWordsArray[1];
            }
        } else {
            changedColorWordOriginal = null;
            changedColorWordNew = null;
        }

        final String changedTypeWordOriginal, changedTypeWordNew;
        if (sa.hasParam("ChangeTypeWord")) {
            String kindOfType = "";
            final List<String> validTypes = Lists.newArrayList();
            final String[] changedTypeWordsArray = sa.getParam("ChangeTypeWord").split(" ");
            if (changedTypeWordsArray[0].equals("ChooseBasicLandType") || changedTypeWordsArray[0].equals("ChooseCreatureType")) {
                if (changedTypeWordsArray[0].equals("ChooseBasicLandType")) {
                    validTypes.addAll(CardType.getBasicTypes());
                    kindOfType = "basic land";
                } else if (changedTypeWordsArray[0].equals("ChooseCreatureType")) {
                    validTypes.addAll(CardType.Constant.CREATURE_TYPES);
                    kindOfType = "Creature";
                }
                changedTypeWordOriginal = sa.getActivatingPlayer().getController().chooseSomeType(kindOfType, sa, validTypes);
            } else {
                changedTypeWordOriginal = changedTypeWordsArray[0];
            }

            validTypes.clear();
            final List<String> forbiddenTypes = sa.hasParam("ForbiddenNewTypes") ? Lists.newArrayList(sa.getParam("ForbiddenNewTypes").split(",")) : Lists.newArrayList();
            forbiddenTypes.add(changedTypeWordOriginal);
            if (changedTypeWordsArray[1].startsWith("Choose")) {
                if (changedTypeWordsArray[1].equals("ChooseBasicLandType")) {
                    validTypes.addAll(CardType.getBasicTypes());
                    kindOfType = "basic land";
                } else if (changedTypeWordsArray[1].equals("ChooseCreatureType")) {
                    validTypes.addAll(CardType.Constant.CREATURE_TYPES);
                    kindOfType = "Creature";
                }
                validTypes.removeAll(forbiddenTypes);
                changedTypeWordNew = sa.getActivatingPlayer().getController().chooseSomeType(kindOfType, sa, validTypes);
            } else {
                changedTypeWordNew = changedTypeWordsArray[1];
            }
        } else {
            changedTypeWordOriginal = null;
            changedTypeWordNew = null;
        }

        final List<Card> tgts = getCardsfromTargets(sa);
        for (final Card c : tgts) {
            if (changedColorWordOriginal != null && changedColorWordNew != null) {
                c.addChangedTextColorWord(changedColorWordOriginal, changedColorWordNew, timestamp, 0);
            }
            if (changedTypeWordOriginal != null && changedTypeWordNew != null) {
                c.addChangedTextTypeWord(changedTypeWordOriginal, changedTypeWordNew, timestamp, 0);
            }

            if (!permanent) {
                final GameCommand revert = new GameCommand() {
                    private static final long serialVersionUID = -7802388880114360593L;
                    @Override
                    public void run() {
                        if (changedColorWordNew != null) {
                            c.removeChangedTextColorWord(timestamp, 0);
                        }
                        if (changedTypeWordNew != null) {
                            c.removeChangedTextTypeWord(timestamp, 0);
                        }
                    }
                };
                game.getEndOfTurn().addUntil(revert);
            }

            game.fireEvent(new GameEventCardStatsChanged(c));
            c.updateStateForView();
            c.updateTypesForView();
        }
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final String changedColorWordOriginal, changedColorWordNew;
        if (sa.hasParam("ChangeColorWord")) {
            final String[] changedColorWordsArray = sa.getParam("ChangeColorWord").split(" ");
            changedColorWordOriginal = changedColorWordsArray[0];
            changedColorWordNew = changedColorWordsArray[1];
        } else {
            changedColorWordOriginal = null;
            changedColorWordNew = null;
        }

        final String changedTypeWordOriginal, changedTypeWordNew;
        if (sa.hasParam("ChangeTypeWord")) {
            final String[] changedTypeWordsArray = sa.getParam("ChangeTypeWord").split(" ");
            changedTypeWordOriginal = changedTypeWordsArray[0];
            changedTypeWordNew = changedTypeWordsArray[1];
        } else {
            changedTypeWordOriginal = null;
            changedTypeWordNew = null;
        }

        final boolean permanent = "Permanent".equals(sa.getParam("Duration"));

        final StringBuilder sb = new StringBuilder();
        sb.append("Change the text of ");

        final List<Card> tgts = getCardsfromTargets(sa);
        for (final Card c : tgts) {
            sb.append(c).append(" ");
        }

        if (changedColorWordOriginal != null) {
            sb.append(" by replacing all instances of ");
            if (changedColorWordOriginal.equals("Choose")) {
                sb.append("one color word");
            } else if (changedColorWordOriginal.equals("Any")) {
                sb.append("each color word");
            } else {
                sb.append(changedColorWordOriginal);
            }
            sb.append(" with ");
            if (changedColorWordNew.equals("Choose")) {
                if (changedColorWordOriginal.equals("Choose")) {
                    sb.append("another");
                } else {
                    sb.append("a color word of your choice");
                }
            } else {
                sb.append(changedColorWordNew);
            }
        }

        if (changedTypeWordOriginal != null) {
            sb.append(" by replacing all instances of ");
            if (changedTypeWordOriginal.equals("ChooseBasicLandType")) {
                sb.append("one basic land type");
            } else if (changedTypeWordOriginal.equals("ChooseCreatureType")) {
                sb.append("one creature type");
            } else {
                sb.append(changedTypeWordOriginal);
            }
            sb.append(" with ");
            if (changedTypeWordNew.equals("ChooseBasicLandType")) {
                if (changedTypeWordOriginal.equals("ChooseBasicLandType")) {
                    sb.append("another");
                } else {
                    sb.append("a basic land type of your choice");
                }
            } else if (changedTypeWordNew.equals("ChooseCreatureType")) {
                if (changedTypeWordOriginal.equals("ChooseCreatureType")) {
                    sb.append("another");
                } else {
                    sb.append("a creature type of your choice");
                }
            } else {
                sb.append(changedTypeWordNew);
            }
        }

        if (!permanent) {
            sb.append(" until end of turn");
        }
        sb.append('.');

        if (sa.hasParam("ForbiddenNewTypes")) {
            sb.append(" The new creature type can't be ");
            sb.append(sa.getParam("ForbiddenNewTypes"));
            sb.append('.');
        }

        if (permanent) {
            sb.append(" (This effect lasts indefinitely.)");
        }

        return sb.toString();
    }

}
