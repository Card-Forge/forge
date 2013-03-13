package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardUtil;
import forge.Command;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.cardfactory.CardFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;
import forge.gui.GuiDialog;

public class CloneEffect extends SpellAbilityEffect {
    // TODO update this method

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getSourceCard();
        Card tgtCard = host;

        Card cardToCopy = host;
        final Target tgt = sa.getTarget();
        if (sa.hasParam("Defined")) {
            List<Card> cloneSources = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            if (!cloneSources.isEmpty()) {
                cardToCopy = cloneSources.get(0);
            }
        } else if (tgt != null) {
            cardToCopy = tgt.getTargetCards().get(0);
        }

        List<Card> cloneTargets = AbilityUtils.getDefinedCards(host, sa.getParam("CloneTarget"), sa);
        if (!cloneTargets.isEmpty()) {
            tgtCard = cloneTargets.get(0);
        }

        sb.append(tgtCard);
        sb.append(" becomes a copy of " + cardToCopy + ".");

        return sb.toString();
    } // end cloneStackDescription()

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        Card tgtCard = host;
        Map<String, String> origSVars = host.getSVars();

        // find cloning source i.e. thing to be copied
        Card cardToCopy = null;
        final Target tgt = sa.getTarget();
        if (sa.hasParam("Defined")) {
            List<Card> cloneSources = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
            if (!cloneSources.isEmpty()) {
                cardToCopy = cloneSources.get(0);
            }
        } else if (tgt != null) {
            cardToCopy = tgt.getTargetCards().get(0);
        }
        if (cardToCopy == null) {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Do you want to copy " + cardToCopy + "?");
        boolean optional = sa.hasParam("Optional");
        if (host.getController().isHuman() && optional
                && !GuiDialog.confirm(host, sb.toString())) {
            return;
        }

        // find target of cloning i.e. card becoming a clone
        List<Card> cloneTargets = AbilityUtils.getDefinedCards(host, sa.getParam("CloneTarget"), sa);
        if (!cloneTargets.isEmpty()) {
            tgtCard = cloneTargets.get(0);
        }

        String imageFileName = host.getImageKey();

        boolean keepName = sa.hasParam("KeepName");
        String originalName = tgtCard.getName();
        boolean copyingSelf = (tgtCard == cardToCopy);

        if (!copyingSelf) {
            if (tgtCard.isCloned()) { // cloning again
                tgtCard.switchStates(CardCharacteristicName.Cloner, CardCharacteristicName.Original);
                tgtCard.setState(CardCharacteristicName.Original);
                tgtCard.clearStates(CardCharacteristicName.Cloner);
            }
            // add "Cloner" state to clone
            tgtCard.addAlternateState(CardCharacteristicName.Cloner);
            tgtCard.switchStates(CardCharacteristicName.Original, CardCharacteristicName.Cloner);
            tgtCard.setState(CardCharacteristicName.Original);
        }
        else {
            //copy Original state to Cloned
            tgtCard.addAlternateState(CardCharacteristicName.Cloned);
            tgtCard.switchStates(CardCharacteristicName.Original, CardCharacteristicName.Cloned);
            if (tgtCard.isFlipCard()) {
                tgtCard.setState(CardCharacteristicName.Original);
            }
        }

        CardCharacteristicName stateToCopy = null;
        if (copyingSelf) {
            stateToCopy = CardCharacteristicName.Cloned;
        }
        else if (cardToCopy.isFlipCard()) {
            stateToCopy = CardCharacteristicName.Original;
        }
        else {
            stateToCopy = cardToCopy.getCurState();
        }

        CardFactory.copyState(cardToCopy, stateToCopy, tgtCard);
        // must call this before addAbilityFactoryAbilities so cloned added abilities are handled correctly
        addExtraCharacteristics(tgtCard, sa, origSVars);
        CardFactoryUtil.addAbilityFactoryAbilities(tgtCard);
        for (int i = 0; i < tgtCard.getStaticAbilityStrings().size(); i++) {
            tgtCard.addStaticAbility(tgtCard.getStaticAbilityStrings().get(i));
        }
        if (keepName) {
            tgtCard.setName(originalName);
        }

        // If target is a flipped card, also copy the flipped
        // state.
        if (cardToCopy.isFlipCard()) {
            if (!copyingSelf) {
                tgtCard.addAlternateState(CardCharacteristicName.Flipped);
                tgtCard.setState(CardCharacteristicName.Flipped);
            }
            CardFactory.copyState(cardToCopy, CardCharacteristicName.Flipped, tgtCard);
            addExtraCharacteristics(tgtCard, sa, origSVars);
            CardFactoryUtil.addAbilityFactoryAbilities(tgtCard);
            for (int i = 0; i < tgtCard.getStaticAbilityStrings().size(); i++) {
                tgtCard.addStaticAbility(tgtCard.getStaticAbilityStrings().get(i));
            }
            if (keepName) {
                tgtCard.setName(originalName);
            }
            tgtCard.setFlipCard(true);
            //keep the Clone card image for the cloned card
            tgtCard.setImageKey(imageFileName);

            if (!tgtCard.isFlipped()) {
              tgtCard.setState(CardCharacteristicName.Original);
            }
        } else {
            tgtCard.setFlipCard(false);
        }

        //Clean up copy of cloned state
        if (copyingSelf) {
            tgtCard.clearStates(CardCharacteristicName.Cloned);
        }

        //Clear Remembered and Imprint lists
        tgtCard.clearRemembered();
        tgtCard.clearImprinted();

        //keep the Clone card image for the cloned card
        tgtCard.setImageKey(imageFileName);

        // check if clone is now an Aura that needs to be attached
        if (tgtCard.isAura()) {
            AttachEffect.attachAuraOnIndirectEnterBattlefield(tgtCard);
        }

        if (sa.hasParam("Duration")) {
            final Card cloneCard = tgtCard;
            final Command unclone = new Command() {
                private static final long serialVersionUID = -78375985476256279L;

                @Override
                public void execute() {
                    if (cloneCard.isCloned()) {
                      cloneCard.switchStates(CardCharacteristicName.Cloner, CardCharacteristicName.Original);
                      cloneCard.setState(CardCharacteristicName.Original);
                      cloneCard.clearStates(CardCharacteristicName.Cloner);
                    }
                }
            };

            String duration = sa.getParam("Duration");
            if (duration.equals("UntilEndOfTurn")) {
                Singletons.getModel().getGame().getEndOfTurn().addUntil(unclone);
            } else if (duration.equals("UntilYourNextTurn")) {
                Singletons.getModel().getGame().getCleanup().addUntilYourNextTurn(host.getController(), unclone);
            }
        }

    } // cloneResolve

    private void addExtraCharacteristics(final Card tgtCard, final SpellAbility sa, final Map<String, String> origSVars) {
        // additional types to clone
        if (sa.hasParam("AddTypes")) {
           for (final String type : Arrays.asList(sa.getParam("AddTypes").split(","))) {
               tgtCard.addType(type);
           }
        }

        // triggers to add to clone
        final ArrayList<String> triggers = new ArrayList<String>();
        if (sa.hasParam("AddTriggers")) {
            triggers.addAll(Arrays.asList(sa.getParam("AddTriggers").split(",")));
            for (final String s : triggers) {
                if (origSVars.containsKey(s)) {
                    final String actualTrigger = origSVars.get(s);
                    final Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, tgtCard, true);
                    tgtCard.addTrigger(parsedTrigger);
                }
            }
        }

        // SVars to add to clone
        if (sa.hasParam("AddSVars")) {
            for (final String s : Arrays.asList(sa.getParam("AddSVars").split(","))) {
                if (origSVars.containsKey(s)) {
                    final String actualsVar = origSVars.get(s);
                    tgtCard.setSVar(s, actualsVar);
                }
            }
        }

        // abilities to add to clone
        if (sa.hasParam("AddAbilities")) {
            for (final String s : Arrays.asList(sa.getParam("AddAbilities").split(","))) {
                if (origSVars.containsKey(s)) {
                    //final AbilityFactory newAF = new AbilityFactory();
                    final String actualAbility = origSVars.get(s);
                    // final SpellAbility grantedAbility = newAF.getAbility(actualAbility, tgtCard);
                    // tgtCard.addSpellAbility(grantedAbility);
                    tgtCard.getIntrinsicAbilities().add(actualAbility);
                }
            }
        }

        // keywords to add to clone
        final ArrayList<String> keywords = new ArrayList<String>();
        if (sa.hasParam("AddKeywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("AddKeywords").split(" & ")));
            // allow SVar substitution for keywords
            for (int i = 0; i < keywords.size(); i++) {
                final String k = keywords.get(i);
                if (origSVars.containsKey(k)) {
                    keywords.add("\"" + k + "\"");
                    keywords.remove(k);
                }
                tgtCard.addIntrinsicKeyword(keywords.get(i));
            }
        }

        // set power of clone
        if (sa.hasParam("IntoPlayTapped")) {
            tgtCard.setTapped(true);
        }

        // set power of clone
        if (sa.hasParam("SetPower")) {
            String rhs = sa.getParam("SetPower");
            int power = -1;
            try {
                power = Integer.parseInt(rhs);
            } catch (final NumberFormatException e) {
                power = CardFactoryUtil.xCount(tgtCard, tgtCard.getSVar(rhs));
            }
            tgtCard.setBaseAttack(power);
        }

        // set toughness of clone
        if (sa.hasParam("SetToughness")) {
            String rhs = sa.getParam("SetToughness");
            int toughness = -1;
            try {
                toughness = Integer.parseInt(rhs);
            } catch (final NumberFormatException e) {
                toughness = CardFactoryUtil.xCount(tgtCard, tgtCard.getSVar(rhs));
            }
            tgtCard.setBaseDefense(toughness);
        }

        // colors to be added or changed to
        String shortColors = "";
        if (sa.hasParam("Colors")) {
            final String colors = sa.getParam("Colors");
            if (colors.equals("ChosenColor")) {
                shortColors = CardUtil.getShortColorsString(tgtCard.getChosenColor());
            } else {
                shortColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(colors.split(","))));
            }
        }
        tgtCard.addColor(shortColors, tgtCard, !sa.hasParam("OverwriteColors"), true);
    }

}
