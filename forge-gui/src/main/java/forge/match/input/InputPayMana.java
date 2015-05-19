package forge.match.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

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
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.player.HumanPlay;
import forge.player.PlayerControllerHuman;
import forge.util.Evaluator;
import forge.util.ITriggerEvent;

public abstract class InputPayMana extends InputSyncronizedBase {
    private static final long serialVersionUID = 718128600948280315L;

    protected int phyLifeToLose = 0;

    protected final Player player;
    protected final Game game;
    protected ManaCostBeingPaid manaCost;
    protected final SpellAbility saPaidFor;
    private final boolean wasFloatingMana;
    private final Queue<Card> delaySelectCards = new LinkedList<Card>();

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
        if (!card.getManaAbilities().isEmpty() && activateManaAbility(card, manaCost)) {
            return true;
        }
        return activateDelayedCard();
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
        if (activateManaAbility(delaySelectCards.poll(), manaCost)) {
            return true;
        }
        return activateDelayedCard();
    }

    @Override
    public boolean selectAbility(final SpellAbility ab) {
        if (ab != null && ab.isManaAbility()) {
            return activateManaAbility(ab.getHostCard(), manaCost, ab);
        }
        return false;
    }

    public List<SpellAbility> getUsefulManaAbilities(Card card) {
        List<SpellAbility> abilities = new ArrayList<SpellAbility>();

        if (card.getController() != player) {
            return abilities;
        }

        byte colorCanUse = 0;
        for (final byte color : MagicColor.WUBRG) {
            if (manaCost.isAnyPartPayableWith(color, player.getManaPool())) {
                colorCanUse |= color;
            }
        }
        if (manaCost.isAnyPartPayableWith((byte) ManaAtom.COLORLESS, player.getManaPool())) {
            colorCanUse |= ManaAtom.COLORLESS;
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

    protected boolean activateManaAbility(final Card card, ManaCostBeingPaid manaCost) {
        return activateManaAbility(card, manaCost, null);
    }
    protected boolean activateManaAbility(final Card card, ManaCostBeingPaid manaCost, SpellAbility chosenAbility) {
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

        for (final byte color : MagicColor.WUBRG) {
            if (manaCost.isAnyPartPayableWith(color, player.getManaPool())) { colorCanUse |= color; }
            if (manaCost.needsColor(color, player.getManaPool()))           { colorNeeded |= color; }
        }
        if (manaCost.isAnyPartPayableWith((byte) ManaAtom.COLORLESS, player.getManaPool())) {
            colorCanUse |= ManaAtom.COLORLESS;
        }

        if (colorCanUse == 0) { // no mana cost or something 
            return false;
        }

        List<SpellAbility> abilities = new ArrayList<SpellAbility>();
        // you can't remove unneeded abilities inside a for (am:abilities) loop :(

        final String typeRes = manaCost.getSourceRestriction();
        if (StringUtils.isNotBlank(typeRes) && !card.getType().hasStringType(typeRes)) {
            return false;
        }

        boolean guessAbilityWithRequiredColors = true;
        for (SpellAbility ma : card.getManaAbilities()) {
            ma.setActivatingPlayer(player);

            AbilityManaPart m = ma.getManaPartRecursive();
            if (m == null || !ma.canPlay())                                 { continue; }
            if (!abilityProducesManaColor(ma, m, colorCanUse))              { continue; }
            if (ma.isAbility() && ma.getRestrictions().isInstantSpeed())    { continue; }
            if (!m.meetsManaRestrictions(saPaidFor))                        { continue; }

            abilities.add(ma);

            // skip express mana if the ability is not undoable or reusable
            if (!ma.isUndoable() || !ma.getPayCosts().isRenewableResource() || ma.getSubAbility() != null) {
                guessAbilityWithRequiredColors = false;
            }
        }

        if (abilities.isEmpty() || (chosenAbility != null && !abilities.contains(chosenAbility))) {
            return false;
        }

        // Store some information about color costs to help with any mana choices
        if (colorNeeded == 0) {  // only colorless left
            if (saPaidFor.getHostCard() != null && saPaidFor.getHostCard().hasSVar("ManaNeededToAvoidNegativeEffect")) {
                String[] negEffects = saPaidFor.getHostCard().getSVar("ManaNeededToAvoidNegativeEffect").split(",");
                for (String negColor : negEffects) {
                    byte col = MagicColor.fromName(negColor);
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
        if (guessAbilityWithRequiredColors) {
            // express Mana Choice
            if (colorNeeded == 0) {
                choice = false;
                //avoid unnecessary prompt by pretending we need White
                //for the sake of "Add one mana of any color" effects
                colorNeeded = MagicColor.WHITE;
            }
            else {
                final List<SpellAbility> colorMatches = new ArrayList<SpellAbility>();
                for (SpellAbility sa : abilities) {
                    if (abilityProducesManaColor(sa, sa.getManaPartRecursive(), colorNeeded)) {
                        colorMatches.add(sa);
                    }
                }

                if (colorMatches.isEmpty()) {
                    // can only match colorless just grab the first and move on.
                    // This is wrong. Sometimes all abilities aren't created equal
                    choice = false;
                }
                else if (colorMatches.size() < abilities.size()) {
                    // leave behind only color matches
                    abilities = colorMatches;
                }
            }
        }

        final SpellAbility chosen;
        if (chosenAbility == null) {
            chosen = abilities.size() > 1 && choice ? getController().getGui().one("Choose mana ability", abilities) : abilities.get(0);
        } else {
            chosen = chosenAbility;
        }
        ColorSet colors = ColorSet.fromMask(0 == colorNeeded ? colorCanUse : colorNeeded);
        chosen.getManaPartRecursive().setExpressChoice(colors);

        // System.out.println("Chosen sa=" + chosen + " of " + chosen.getHostCard() + " to pay mana");

        locked = true;
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                HumanPlay.playSpellAbility(getController(), chosen.getActivatingPlayer(), chosen);
                player.getManaPool().payManaFromAbility(saPaidFor, InputPayMana.this.manaCost, chosen);

                onManaAbilityPaid();
                onStateChanged();
            }
        });
        return true;
    }

    private static boolean abilityProducesManaColor(final SpellAbility am, AbilityManaPart m, final byte neededColor) {
        if (0 != (neededColor & ManaAtom.COLORLESS)) {
            return true;
        }

        if (m.isAnyMana()) {
            return true;
        }

        // check for produce mana replacement effects - they mess this up, so just use the mana ability
        final Card source = am.getHostCard();
        final Player activator = am.getActivatingPlayer();
        final Game g = source.getGame();
        final HashMap<String, Object> repParams = new HashMap<String, Object>();
        repParams.put("Event", "ProduceMana");
        repParams.put("Mana", m.getOrigProduced());
        repParams.put("Affected", source);
        repParams.put("Player", activator);
        repParams.put("AbilityMana", am);

        for (final Player p : g.getPlayers()) {
            for (final Card crd : p.getAllCards()) {
                for (final ReplacementEffect replacementEffect : crd.getReplacementEffects()) {
                    if (replacementEffect.requirementsCheck(g)
                            && replacementEffect.canReplace(repParams)
                            && replacementEffect.getMapParams().containsKey("ManaReplacement")
                            && replacementEffect.zonesCheck(g.getZoneOf(crd))) {
                        return true;
                    }
                }
            }
        }

        if (am.getApi() == ApiType.ManaReflected) {
            final Iterable<String> reflectableColors = CardUtil.getReflectableManaColors(am);
            for (final String color : reflectableColors) {
                if (0 != (neededColor & MagicColor.fromName(color))) {
                    return true;
                }
            }
        }
        else {
            String colorsProduced = m.isComboMana() ? m.getComboColors() : m.getOrigProduced();
            for (final String color : colorsProduced.split(" ")) {
                if (0 != (neededColor & MagicColor.fromName(color))) {
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
            getController().getGui().updateButtons(getOwner(), "Auto", "Cancel", false, true, false);
        } else {
            getController().getGui().updateButtons(getOwner(), "", "Cancel", false, true, false);
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
                getController().getGui().updateButtons(getOwner(), "Auto", "Cancel", true, true, true);
            }
        }
        showMessage(getMessage());
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
        return String.format("PayManaBase %s left", manaCost.toString());
    }

    public boolean isPaid() { return bPaid; }

    protected String messagePrefix;
    public void setMessagePrefix(String prompt) {
        // TODO Auto-generated method stub
        messagePrefix = prompt;
    }
}
