package forge.game.ability.effects;

import java.util.*;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.card.CardFacePredicates;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.ICardFace;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.Localizer;

public class ChooseCardNameEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append(Lang.joinHomogenous(getTargetPlayers(sa)));
        sb.append("names a card.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        String valid = "Card";
        String validDesc = null;
        String message = null;

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
            validDesc = sa.getParam("ValidDesc");
        }

        boolean randomChoice = sa.hasParam("AtRandom");
        boolean chooseFromDefined = sa.hasParam("ChooseFromDefinedCards");
        boolean chooseFromList = sa.hasParam("ChooseFromList");
        boolean chooseFromOneTimeList = sa.hasParam("ChooseFromOneTimeList");

        if (!randomChoice) {
            if (sa.hasParam("SelectPrompt")) {
                message = sa.getParam("SelectPrompt");
            } else if (null == validDesc) {
                message = Localizer.getInstance().getMessage("lblChooseACardName");
            } else {
                message = Localizer.getInstance().getMessage("lblChooseASpecificCard", validDesc);
            }
        }

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            String chosen = "";
            //This section was used for Momir Avatar, which no longer uses it - commented out 7/28/2021
            //if (randomChoice) {
            //String numericAmount = "X";
            //final int validAmount = StringUtils.isNumeric(numericAmount) ? Integer.parseInt(numericAmount) :
            //    AbilityUtils.calculateAmount(host, numericAmount, sa);
            // Momir needs PaperCard
            //Collection<PaperCard> cards = StaticData.instance().getCommonCards().getUniqueCards();
            //Predicate<PaperCard> cpp = Predicates.and(
            //    Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES),
            //    Predicates.compose(CardRulesPredicates.cmc(ComparableOp.EQUALS, validAmount), PaperCard.FN_GET_RULES));
            //cards = Lists.newArrayList(Iterables.filter(cards, cpp));
            //if (!cards.isEmpty()) { chosen = Aggregates.random(cards).getName();
            //} else {
            //    chosen = "";
            //}
            if (chooseFromDefined) {
                CardCollection choices = AbilityUtils.getDefinedCards(host, sa.getParam("ChooseFromDefinedCards"), sa);
                choices = CardLists.getValidCards(choices, valid, host.getController(), host, sa);
                List<ICardFace> faces = new ArrayList<>();
                // get Card
                for (final Card c : choices) {
                    final CardRules rules = c.getRules();
                    if (faces.contains(rules.getMainPart()))
                        continue;
                    faces.add(rules.getMainPart());
                    // Alhammarret only allows Split for other faces
                    if (rules.getSplitType() == CardSplitType.Split) {
                        faces.add(rules.getOtherPart());
                    }
                }
                Collections.sort(faces);
                chosen = p.getController().chooseCardName(sa, faces, message);
            } else if (chooseFromList) {
                String [] names = sa.getParam("ChooseFromList").split(",");
                List<ICardFace> faces = new ArrayList<>();
                for (String name : names) {
                    // Cardnames that include "," must use ";" instead in ChooseFromList$ (i.e. Tovolar; Dire Overlord)
                    name = name.replace(";", ",");
                    faces.add(StaticData.instance().getCommonCards().getFaceByName(name));
                }
                if (randomChoice) {
                    chosen = Aggregates.random(faces).getName();
                } else {
                    chosen = p.getController().chooseCardName(sa, faces, message);
                }
            } else if (chooseFromOneTimeList) {
                String [] names = sa.getParam("ChooseFromOneTimeList").split(",");
                List<ICardFace> faces = new ArrayList<>();
                for (String name : names) {
                    faces.add(StaticData.instance().getCommonCards().getFaceByName(name));
                }
                chosen = p.getController().chooseCardName(sa, faces, message);

                // Remove chosen Name from List
                StringBuilder sb = new StringBuilder();
                for (String name : names) {
                    if (chosen.equals(name)) continue;
                    if (sb.length() > 0) sb.append(',');
                    sb.append(name);
                }
                sa.putParam("ChooseFromOneTimeList", sb.toString());
            } else {
                // use CardFace because you might name a alternate names
                Predicate<ICardFace> cpp = Predicates.alwaysTrue();
                if (sa.hasParam("ValidCards")) {
                    //Calculating/replacing this must happen before running valid in CardFacePredicates
                    if (valid.contains("ManaCost=")) {
                        if (valid.contains("ManaCost=Equipped")) {
                            String s = host.getEquipping().getManaCost().getShortString();
                            valid = valid.replace("=Equipped", s);
                        } else if (valid.contains("ManaCost=Imprinted")) {
                            String s = host.getImprintedCards().getFirst().getManaCost().getShortString();
                            valid = valid.replace("=Imprinted", s);
                        }
                    }
                    cpp = CardFacePredicates.valid(valid);
                }
                if (randomChoice) {
                    final Iterable<ICardFace> cards = Iterables.filter(StaticData.instance().getCommonCards().getAllFaces(), cpp);
                    chosen = Aggregates.random(cards).getName();
                } else {
                    chosen = p.getController().chooseCardName(sa, cpp, valid, message);
                }
            }

            host.setNamedCard(chosen);
            if (!randomChoice) {
                p.setNamedCard(chosen);
            }
            if (sa.hasParam("NoteFor")) {
                p.addNoteForName(sa.getParam("NoteFor"), "Name:" + chosen);
            }
        }
    }

}
