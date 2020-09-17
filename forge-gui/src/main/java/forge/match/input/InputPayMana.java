package forge.match.input;

import java.util.*;

import forge.GuiBase;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.spellability.SpellAbilityView;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.FThreads;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.player.HumanPlay;
import forge.player.PlayerControllerHuman;
import forge.util.Evaluator;
import forge.util.ITriggerEvent;
import forge.util.Localizer;

public abstract class InputPayMana extends InputSyncronizedBase {
    private static final long serialVersionUID = 718128600948280315L;

    protected int phyLifeToLose = 0;

    protected final Player player;
    protected final Game game;
    protected ManaCostBeingPaid manaCost;
    protected final SpellAbility saPaidFor;
    private final boolean wasFloatingMana;
    private final Queue<Card> delaySelectCards = new LinkedList<>();

    private boolean bPaid = false;
    protected Boolean canPayManaCost = null;

    private boolean locked = false;

    protected InputPayMana(final PlayerControllerHuman controller, final SpellAbility saPaidFor0, final Player player0) {
        super(controller);
        player = player0;
        game = player.getGame();
        saPaidFor = saPaidFor0;

        //if player is floating mana, show mana pool to make it easier to use that mana
        wasFloatingMana = !player.getManaPool().isEmpty();
        if (wasFloatingMana) {
            getController().getGui().showManaPool(PlayerView.get(player));
        }
    }

    @Override
    protected void onStop() {
        if (wasFloatingMana) { //hide mana pool if it was shown due to floating mana
            getController().getGui().hideManaPool(PlayerView.get(player));
        }
    }

    @Override
    protected boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        if (GuiBase.getInterface().isLibgdxPort()) {
            // Mobile Forge allows to tap cards underneath the current card even if the current one is tapped
            if (otherCardsToSelect != null) {
                for (Card c : otherCardsToSelect) {
                    for (SpellAbility sa : c.getManaAbilities()) {
                        if (sa.canPlay()) {
                            delaySelectCards.add(c);
                            break;
                        }
                    }
                }
            }
            if (!card.getManaAbilities().isEmpty() && activateManaAbility(card)) {
                return true;
            }
            return activateDelayedCard();
        } else {
            // Desktop Forge floating menu functionality
            if (card.getManaAbilities().size() == 1) {
                activateManaAbility(card, card.getManaAbilities().get(0));
            } else {
                SpellAbility spellAbility = getController().getAbilityToPlay(card, Lists.newArrayList(card.getManaAbilities()), triggerEvent);
                if (spellAbility != null) {
                    activateManaAbility(card, spellAbility);
                }
            }
            return true;
        }
    }

    @Override
    public String getActivateAction(Card card) {
        for (SpellAbility sa : card.getManaAbilities()) {
            if (sa.canPlay()) {
                return "pay mana with card";
            }
        }
        return null;
    }

    private boolean activateDelayedCard() {
        if (delaySelectCards.isEmpty()) {
            return false;
        }
        if (manaCost.isPaid()) {
            delaySelectCards.clear(); //clear delayed cards if mana cost already paid
            return false;
        }
        if (activateManaAbility(delaySelectCards.poll())) {
            return true;
        }
        return activateDelayedCard();
    }

    @Override
    public boolean selectAbility(final SpellAbility ab) {
        if (ab != null && ab.isManaAbility()) {
            return activateManaAbility(ab.getHostCard(), ab);
        }
        return false;
    }

    public List<SpellAbility> getUsefulManaAbilities(Card card) {
        List<SpellAbility> abilities = new ArrayList<>();

        if (card.getController() != player) {
            return abilities;
        }

        byte colorCanUse = 0;
        for (final byte color : ManaAtom.MANATYPES) {
            if (manaCost.isAnyPartPayableWith(color, player.getManaPool())) {
                colorCanUse |= color;
            }
        }
        if (manaCost.isAnyPartPayableWith((byte) ManaAtom.GENERIC, player.getManaPool())) {
            colorCanUse |= ManaAtom.GENERIC;
        }
        if (colorCanUse == 0) { // no mana cost or something 
            return abilities;
        }

        final String typeRes = manaCost.getSourceRestriction();
        if (StringUtils.isNotBlank(typeRes) && !card.getType().hasStringType(typeRes)) {
            return abilities;
        }

        for (SpellAbility ma : card.getManaAbilities()) {
            ma.setActivatingPlayer(player);
            AbilityManaPart m = ma.getManaPartRecursive();
            if (m == null || !ma.canPlay())                                 { continue; }
            if (!abilityProducesManaColor(ma, m, colorCanUse))              { continue; }
            if (ma.isAbility() && ma.getRestrictions().isInstantSpeed())    { continue; }
            if (!m.meetsManaRestrictions(saPaidFor))                        { continue; }

            abilities.add(ma);
        }
        return abilities;
    }

    public void useManaFromPool(byte colorCode) {
        // find the matching mana in pool.
        if (player.getManaPool().tryPayCostWithColor(colorCode, saPaidFor, manaCost)) {
            onManaAbilityPaid();
            showMessage();
        }
    }

    protected boolean activateManaAbility(final Card card) {
        return activateManaAbility(card, null);
    }
    protected boolean activateManaAbility(final Card card, SpellAbility chosenAbility) {
        if (locked) {
            System.err.print("Should wait till previous call to playAbility finishes.");
            return false;
        }
        
        // make sure computer's lands aren't selected
        if (card.getController() != player) {
            return false;
        }

        byte colorCanUse = 0;
        byte colorNeeded = 0;

        for (final byte color : ManaAtom.MANATYPES) {
            if (manaCost.isAnyPartPayableWith(color, player.getManaPool())) { colorCanUse |= color; }
            if (manaCost.needsColor(color, player.getManaPool()))           { colorNeeded |= color; }
        }
        if (manaCost.isAnyPartPayableWith((byte) ManaAtom.GENERIC, player.getManaPool())) {
            colorCanUse |= ManaAtom.GENERIC;
        }

        if (colorCanUse == 0) { // no mana cost or something 
            return false;
        }

        HashMap<SpellAbilityView, SpellAbility> abilitiesMap = new HashMap<>();
        // you can't remove unneeded abilities inside a for (am:abilities) loop :(

        final String typeRes = manaCost.getSourceRestriction();
        if (StringUtils.isNotBlank(typeRes) && !card.getType().hasStringType(typeRes)) {
            return false;
        }

        boolean guessAbilityWithRequiredColors = true;
        int amountOfMana = -1;
        for (SpellAbility ma : card.getManaAbilities()) {
            ma.setActivatingPlayer(player);

            AbilityManaPart m = ma.getManaPartRecursive();
            if (m == null || !ma.canPlay())                                 { continue; }
            if (!abilityProducesManaColor(ma, m, colorCanUse))              { continue; }
            if (ma.isAbility() && ma.getRestrictions().isInstantSpeed())    { continue; }
            if (!m.meetsManaRestrictions(saPaidFor))                        { continue; }

            // If Mana Abilities produce differing amounts of mana, let the player choose
            int maAmount = GameActionUtil.amountOfManaGenerated(ma, true);
            if (amountOfMana == -1) {
                amountOfMana = maAmount;
            } else {
                if (amountOfMana != maAmount) {
                    guessAbilityWithRequiredColors = false;
                }
            }

            abilitiesMap.put(ma.getView(), ma);

            // skip express mana if the ability is not undoable or reusable
            if (!ma.isUndoable() || !ma.getPayCosts().isRenewableResource() || ma.getSubAbility() != null) {
                guessAbilityWithRequiredColors = false;
            }
        }

        if (abilitiesMap.isEmpty() || (chosenAbility != null && !abilitiesMap.containsKey(chosenAbility.getView()))) {
            return false;
        }

        // Store some information about color costs to help with any mana choices
        if (colorNeeded == 0) {  // only colorless left
            if (saPaidFor.getHostCard() != null && saPaidFor.getHostCard().hasSVar("ManaNeededToAvoidNegativeEffect")) {
                String[] negEffects = saPaidFor.getHostCard().getSVar("ManaNeededToAvoidNegativeEffect").split(",");
                for (String negColor : negEffects) {
                    byte col = ManaAtom.fromName(negColor);
                    colorCanUse |= col;
                }
            }
        }

        // If the card has any ability that tracks mana spent, skip express Mana choice
        if (saPaidFor.tracksManaSpent()) {
            colorCanUse = ColorSet.ALL_COLORS.getColor();
            guessAbilityWithRequiredColors = false;
        }

        boolean choice = true;
        boolean isPayingGeneric = false;
        if (guessAbilityWithRequiredColors) {
            // express Mana Choice
            if (colorNeeded == 0) {
                choice = false;
                //avoid unnecessary prompt by pretending we need White
                //for the sake of "Add one mana of any color" effects
                colorNeeded = MagicColor.WHITE;
                isPayingGeneric = true; // for further processing
            }
            else {
                final HashMap<SpellAbilityView, SpellAbility> colorMatches = new HashMap<>();
                for (SpellAbility sa : abilitiesMap.values()) {
                    if (abilityProducesManaColor(sa, sa.getManaPartRecursive(), colorNeeded)) {
                        colorMatches.put(sa.getView(), sa);
                    }
                }

                if (colorMatches.isEmpty()) {
                    // can only match colorless just grab the first and move on.
                    // This is wrong. Sometimes all abilities aren't created equal
                    choice = false;
                }
                else if (colorMatches.size() < abilitiesMap.size()) {
                    // leave behind only color matches
                    abilitiesMap = colorMatches;
                }
            }
        }

        // Exceptions for cards that have conditional abilities which are better handled manually
        if (card.getName().equals("Cavern of Souls") && isPayingGeneric) {
            choice = true;
        }

        final SpellAbility chosen;
        if (chosenAbility == null) {
            ArrayList<SpellAbilityView> choices = new ArrayList<>(abilitiesMap.keySet());
            chosen = abilitiesMap.size() > 1 && choice ? abilitiesMap.get(getController().getGui().one(Localizer.getInstance().getMessage("lblChooseManaAbility"),  choices)) : abilitiesMap.get(choices.get(0));
        } else {
            chosen = chosenAbility;
        }

        ColorSet colors = ColorSet.fromMask(0 == colorNeeded ? colorCanUse : colorNeeded);

        // Filter the colors for the express choice so that only actually producible colors can be chosen
        int producedColorMask = 0;
        for (final byte color : ManaAtom.MANATYPES) {
            if (chosen.getManaPartRecursive().getOrigProduced().contains(MagicColor.toShortString(color))
                    && colors.hasAnyColor(color)) {
                producedColorMask |= color;
            }
        }
        ColorSet producedAndNeededColors = ColorSet.fromMask(producedColorMask);

        chosen.getManaPartRecursive().setExpressChoice(producedAndNeededColors);

        // System.out.println("Chosen sa=" + chosen + " of " + chosen.getHostCard() + " to pay mana");

        locked = true;
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                if (HumanPlay.playSpellAbility(getController(), chosen.getActivatingPlayer(), chosen)) {
                    player.getManaPool().payManaFromAbility(saPaidFor, InputPayMana.this.manaCost, chosen);

                    onManaAbilityPaid();
                    onStateChanged();
                }
            }
        });

        return true;
    }

    private static boolean abilityProducesManaColor(final SpellAbility am, AbilityManaPart m, final byte neededColor) {
        if (0 != (neededColor & ManaAtom.GENERIC)) {
            return true;
        }

        if (m.isAnyMana()) {
            return true;
        }

        // check for produce mana replacement effects - they mess this up, so just use the mana ability
        final Card source = am.getHostCard();
        final Player activator = am.getActivatingPlayer();
        final Game g = source.getGame();
        final Map<AbilityKey, Object> repParams = AbilityKey.newMap();
        repParams.put(AbilityKey.Mana, m.getOrigProduced());
        repParams.put(AbilityKey.Affected, source);
        repParams.put(AbilityKey.Player, activator);
        repParams.put(AbilityKey.AbilityMana, am);

        for (final Player p : g.getPlayers()) {
            for (final Card crd : p.getAllCards()) {
                for (final ReplacementEffect replacementEffect : crd.getReplacementEffects()) {
                    if (replacementEffect.requirementsCheck(g)
                            && replacementEffect.getMode() == ReplacementType.ProduceMana
                            && replacementEffect.canReplace(repParams)
                            && replacementEffect.hasParam("ManaReplacement")
                            && replacementEffect.zonesCheck(g.getZoneOf(crd))) {
                        return true;
                    }
                }
            }
        }

        if (am.getApi() == ApiType.ManaReflected) {
            final Iterable<String> reflectableColors = CardUtil.getReflectableManaColors(am);
            for (final String color : reflectableColors) {
                if (0 != (neededColor & ManaAtom.fromName(color))) {
                    return true;
                }
            }
        }
        else {
            // treat special mana if it always can be paid
            if (m.isSpecialMana()) {
                return true;
            }
            String colorsProduced = m.isComboMana() ? m.getComboColors() : m.mana();
            for (final String color : colorsProduced.split(" ")) {
                if (0 != (neededColor & ManaAtom.fromName(color))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isAlreadyPaid() {
        if (manaCost.isPaid()) {
            bPaid = true;
        }
        return bPaid;
    }

    protected boolean supportAutoPay() {
        return true;
    }

    protected void runAsAi(Runnable proc) {
        player.runWithController(proc, new PlayerControllerAi(game, player, player.getOriginalLobbyPlayer()));
    }

    @Override
    protected void onOk() {
        if (supportAutoPay() && !locked) { //prevent AI taking over from double-clicking Auto
            locked = true;
            //use AI utility to automatically pay mana cost if possible
            final Runnable proc = new Runnable() {
                @Override
                public void run() {
                    ComputerUtilMana.payManaCost(manaCost, saPaidFor, player);
                }
            };
            //must run in game thread as certain payment actions can only be automated there
            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    runAsAi(proc);
                    onStateChanged();
                }
            });
        }
    }

    protected void updateButtons() {
        if (supportAutoPay()) {
            getController().getGui().updateButtons(getOwner(), Localizer.getInstance().getMessage("lblAuto"), Localizer.getInstance().getMessage("lblCancel"), false, true, false);
        } else {
            getController().getGui().updateButtons(getOwner(), "", Localizer.getInstance().getMessage("lblCancel"), false, true, false);
        }
    }

    protected final void updateMessage() {
        locked = false;
        if (activateDelayedCard()) {
            return;
        }
        if (supportAutoPay()) {
            if (canPayManaCost == null) {
                //use AI utility to determine if mana cost can be paid if that hasn't been determined yet
                Evaluator<Boolean> proc = new Evaluator<Boolean>() {
                    @Override
                    public Boolean evaluate() {
                        return ComputerUtilMana.canPayManaCost(manaCost, saPaidFor, player);
                    }
                };
                runAsAi(proc);
                canPayManaCost = proc.getResult();
            }
            if (canPayManaCost) { //enabled Auto button if mana cost can be paid
                getController().getGui().updateButtons(getOwner(), Localizer.getInstance().getMessage("lblAuto"), Localizer.getInstance().getMessage("lblCancel"), true, true, true);
            }
        }
        showMessage(getMessage(), saPaidFor.getView());
    }

    @Override
    public void showMessage() {
        if (isFinished()) { return; }
        updateButtons();
        onStateChanged();
    }

    protected void onStateChanged() {
        if (isAlreadyPaid()) {
            done();
            stop();
        }
        else {
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override
                public void run() {
                    updateMessage();
                }
            });
        }
    }

    protected void onManaAbilityPaid() {} // some inputs overload it
    protected abstract void done();
    protected abstract String getMessage();

    @Override
    public String toString() {
        return TextUtil.concatNoSpace("PayManaBase ", manaCost.toString(), " left");
    }

    public boolean isPaid() { return bPaid; }

    protected String messagePrefix;
    public void setMessagePrefix(String prompt) {
        // TODO Auto-generated method stub
        messagePrefix = prompt;
    }
}
