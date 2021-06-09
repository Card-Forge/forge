package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.StaticData;
import forge.card.CardFacePredicates;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.CardSplitType;
import forge.card.ICardFace;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.ComparableOp;
import forge.util.Localizer;

public class ChooseCardNameEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("names a card.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        String valid = "Card";
        String validDesc = null;
        String message = null;

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
            validDesc = sa.getParam("ValidDesc");
        }

        boolean randomChoice = sa.hasParam("AtRandom");
        boolean chooseFromDefined = sa.hasParam("ChooseFromDefinedCards");
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

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                String chosen = "";

                if (randomChoice) {
                    // Currently only used for Momir Avatar, if something else gets added here, make it more generic

                    String numericAmount = "X";
                    final int validAmount = StringUtils.isNumeric(numericAmount) ? Integer.parseInt(numericAmount) :
                        AbilityUtils.calculateAmount(host, numericAmount, sa);

                    // Momir needs PaperCard
                    Collection<PaperCard> cards = StaticData.instance().getCommonCards().getUniqueCards();
                    Predicate<PaperCard> cpp = Predicates.and(
                        Predicates.compose(CardRulesPredicates.Presets.IS_CREATURE, PaperCard.FN_GET_RULES),
                        Predicates.compose(CardRulesPredicates.cmc(ComparableOp.EQUALS, validAmount), PaperCard.FN_GET_RULES)
                    );

                    cards = Lists.newArrayList(Iterables.filter(cards, cpp));
                    if (!cards.isEmpty()) {
                        chosen = Aggregates.random(cards).getName();
                    } else {
                        chosen = "";
                    }
                } else if (chooseFromDefined) {
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
                        cpp = CardFacePredicates.valid(valid);
                    }

                    chosen = p.getController().chooseCardName(sa, cpp, valid, message);
                }

                host.setNamedCard(chosen);
                if(!randomChoice) {
                    p.getGame().getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblPlayerPickedChosen", p.getName(), chosen), p);
                    p.setNamedCard(chosen);
                }
                if (sa.hasParam("NoteFor")) {
                    p.addNoteForName(sa.getParam("NoteFor"), "Name:" + chosen);
                }
            }
        }
    }

}
