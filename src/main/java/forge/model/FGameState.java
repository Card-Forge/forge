package forge.model;

import java.util.HashMap;
import java.util.Map;

import forge.AIPlayer;
import forge.Combat;
import forge.Constant;
import forge.DefaultPlayerZone;
import forge.EndOfCombat;
import forge.EndOfTurn;
import forge.GameAction;
import forge.HumanPlayer;
import forge.MagicStack;
import forge.Phase;
import forge.Player;
import forge.PlayerZone;
import forge.PlayerZone_ComesIntoPlay;
import forge.StaticEffects;
import forge.Upkeep;
import forge.card.trigger.TriggerHandler;

/**
 * Represents the Forge Game State.
 */
public class FGameState {
    private Player humanPlayer = new HumanPlayer("Human");
    private Player computerPlayer = new AIPlayer("Computer");
    private EndOfTurn endOfTurn = new EndOfTurn();
    private EndOfCombat endOfCombat = new EndOfCombat();
    private Upkeep upkeep = new Upkeep();
    private Phase phase = new Phase();
    private MagicStack stack = new MagicStack();
    private GameAction gameAction = new GameAction();
    private StaticEffects staticEffects = new StaticEffects();
    private TriggerHandler triggerHandler = new TriggerHandler();
    private Combat combat = new Combat();


    // These fields should be moved to the player class(es) and implementation(s), and the getters
    // should be moved there.  PMD complains of too many fields, and it is right.

    // The battlefields are different because Card.comesIntoPlay() is called when a card is added by
    // PlayerZone.add(Card).
    private PlayerZone humanBattlefield = new PlayerZone_ComesIntoPlay(Constant.Zone.Battlefield, getHumanPlayer());
    private PlayerZone humanHand = new DefaultPlayerZone(Constant.Zone.Hand, getHumanPlayer());
    private PlayerZone humanGraveyard = new DefaultPlayerZone(Constant.Zone.Graveyard, getHumanPlayer());
    private PlayerZone humanLibrary = new DefaultPlayerZone(Constant.Zone.Library, getHumanPlayer());
    private PlayerZone humanExile = new DefaultPlayerZone(Constant.Zone.Exile, getHumanPlayer());
    private PlayerZone humanCommand = new DefaultPlayerZone(Constant.Zone.Command, getHumanPlayer());

    private PlayerZone computerBattlefield = // NOPMD by Braids on 8/27/11 10:50 PM
            new PlayerZone_ComesIntoPlay(Constant.Zone.Battlefield, getComputerPlayer());

    private PlayerZone computerHand = new DefaultPlayerZone(Constant.Zone.Hand, getComputerPlayer());
    private PlayerZone computerGraveyard = new DefaultPlayerZone(Constant.Zone.Graveyard, getComputerPlayer());
    private PlayerZone computerLibrary = new DefaultPlayerZone(Constant.Zone.Library, getComputerPlayer());
    private PlayerZone computerExile = new DefaultPlayerZone(Constant.Zone.Exile, getComputerPlayer());
    private PlayerZone computerCommand = new DefaultPlayerZone(Constant.Zone.Command, getComputerPlayer());

    private PlayerZone stackZone = new DefaultPlayerZone(Constant.Zone.Stack, null);

    // Maps zone names to PlayerZone instances.
    private Map<String, PlayerZone> zoneNamesToPlayerZones = // NOPMD by Braids on 8/27/11 10:50 PM
            new HashMap<String, PlayerZone>();

    private long timestamp = 0;

    /**
     * Constructor.
     */
    public FGameState() {
        getZoneNamesToPlayerZones().put(Constant.Zone.Graveyard + getHumanPlayer(), getHumanGraveyard());
        getZoneNamesToPlayerZones().put(Constant.Zone.Hand + getHumanPlayer(), getHumanHand());
        getZoneNamesToPlayerZones().put(Constant.Zone.Library + getHumanPlayer(), getHumanLibrary());
        getZoneNamesToPlayerZones().put(Constant.Zone.Battlefield + getHumanPlayer(), getHumanBattlefield());
        getZoneNamesToPlayerZones().put(Constant.Zone.Exile + getHumanPlayer(), getHumanExile());
        getZoneNamesToPlayerZones().put(Constant.Zone.Command + getHumanPlayer(), getHumanCommand());

        getZoneNamesToPlayerZones().put(Constant.Zone.Graveyard + getComputerPlayer(), getComputerGraveyard());
        getZoneNamesToPlayerZones().put(Constant.Zone.Hand + getComputerPlayer(), getComputerHand());
        getZoneNamesToPlayerZones().put(Constant.Zone.Library + getComputerPlayer(), getComputerLibrary());
        getZoneNamesToPlayerZones().put(Constant.Zone.Battlefield + getComputerPlayer(), getComputerBattlefield());
        getZoneNamesToPlayerZones().put(Constant.Zone.Exile + getComputerPlayer(), getComputerExile());
        getZoneNamesToPlayerZones().put(Constant.Zone.Command + getComputerPlayer(), getComputerCommand());

        getZoneNamesToPlayerZones().put(Constant.Zone.Stack + null, getStackZone());
    }


    /**
     * @return the humanPlayer
     */
    public final Player getHumanPlayer() {
        return humanPlayer;
    }


    /**
     * @param humanPlayer0 the humanPlayer to set
     */
    protected final void setHumanPlayer(final Player humanPlayer0) {
        this.humanPlayer = humanPlayer0;
    }


    /**
     * @return the computerPlayer
     */
    public final Player getComputerPlayer() {
        return computerPlayer;
    }


    /**
     * @param computerPlayer0 the computerPlayer to set
     */
    protected final void setComputerPlayer(final Player computerPlayer0) {
        this.computerPlayer = computerPlayer0;
    }


    /**
     * @return the endOfTurn
     */
    public final EndOfTurn getEndOfTurn() {
        return endOfTurn;
    }


    /**
     * @param endOfTurn0 the endOfTurn to set
     */
    protected final void setEndOfTurn(final EndOfTurn endOfTurn0) {
        this.endOfTurn = endOfTurn0;
    }


    /**
     * @return the endOfCombat
     */
    public final EndOfCombat getEndOfCombat() {
        return endOfCombat;
    }


    /**
     * @param endOfCombat0 the endOfCombat to set
     */
    protected final void setEndOfCombat(final EndOfCombat endOfCombat0) {
        this.endOfCombat = endOfCombat0;
    }


    /**
     * @return the upkeep
     */
    public final Upkeep getUpkeep() {
        return upkeep;
    }


    /**
     * @param upkeep0 the upkeep to set
     */
    protected final void setUpkeep(final Upkeep upkeep0) {
        this.upkeep = upkeep0;
    }


    /**
     * @return the phase
     */
    public final Phase getPhase() {
        return phase;
    }


    /**
     * @param phase0 the phase to set
     */
    protected final void setPhase(final Phase phase0) {
        this.phase = phase0;
    }


    /**
     * @return the stack
     */
    public final MagicStack getStack() {
        return stack;
    }


    /**
     * @param stack0 the stack to set
     */
    protected final void setStack(final MagicStack stack0) {
        this.stack = stack0;
    }


    /**
     * @return the gameAction
     */
    public final GameAction getGameAction() {
        return gameAction;
    }


    /**
     * @param gameAction0 the gameAction to set
     */
    protected final void setGameAction(final GameAction gameAction0) {
        this.gameAction = gameAction0;
    }


    /**
     * @return the staticEffects
     */
    public final StaticEffects getStaticEffects() {
        return staticEffects;
    }


    /**
     * @param staticEffects0 the staticEffects to set
     */
    protected final void setStaticEffects(final StaticEffects staticEffects0) {
        this.staticEffects = staticEffects0;
    }


    /**
     * @return the triggerHandler
     */
    public final TriggerHandler getTriggerHandler() {
        return triggerHandler;
    }


    /**
     * @param triggerHandler0 the triggerHandler to set
     */
    protected final void setTriggerHandler(final TriggerHandler triggerHandler0) {
        this.triggerHandler = triggerHandler0;
    }


    /**
     * @return the combat
     */
    public final Combat getCombat() {
        return combat;
    }


    /**
     * @param combat0 the combat to set
     */
    public final void setCombat(final Combat combat0) {
        this.combat = combat0;
    }


    /**
     * @return the humanBattlefield
     */
    public final PlayerZone getHumanBattlefield() {
        return humanBattlefield;
    }


    /**
     * @param humanBattlefield0 the humanBattlefield to set
     */
    protected final void setHumanBattlefield(final PlayerZone humanBattlefield0) {
        this.humanBattlefield = humanBattlefield0;
    }


    /**
     * @return the humanHand
     */
    public final PlayerZone getHumanHand() {
        return humanHand;
    }


    /**
     * @param humanHand0 the humanHand to set
     */
    protected final void setHumanHand(final PlayerZone humanHand0) {
        this.humanHand = humanHand0;
    }


    /**
     * @return the humanGraveyard
     */
    public final PlayerZone getHumanGraveyard() {
        return humanGraveyard;
    }


    /**
     * @param humanGraveyard0 the humanGraveyard to set
     */
    protected final void setHumanGraveyard(final PlayerZone humanGraveyard0) {
        this.humanGraveyard = humanGraveyard0;
    }


    /**
     * @return the humanLibrary
     */
    public final PlayerZone getHumanLibrary() {
        return humanLibrary;
    }


    /**
     * @param humanLibrary0 the humanLibrary to set
     */
    protected final void setHumanLibrary(final PlayerZone humanLibrary0) {
        this.humanLibrary = humanLibrary0;
    }


    /**
     * @return the humanExile
     */
    public final PlayerZone getHumanExile() {
        return humanExile;
    }


    /**
     * @param humanExile0 the humanExile to set
     */
    protected final void setHumanExile(final PlayerZone humanExile0) {
        this.humanExile = humanExile0;
    }


    /**
     * @return the humanCommand
     */
    public final PlayerZone getHumanCommand() {
        return humanCommand;
    }


    /**
     * @param humanCommand0 the humanCommand to set
     */
    protected final void setHumanCommand(final PlayerZone humanCommand0) {
        this.humanCommand = humanCommand0;
    }


    /**
     * @return the computerBattlefield
     */
    public final PlayerZone getComputerBattlefield() {
        return computerBattlefield;
    }


    /**
     * @param computerBattlefield0 the computerBattlefield to set
     */
    protected final void setComputerBattlefield(
            final PlayerZone computerBattlefield0) // NOPMD by Braids on 8/27/11 10:53 PM
    {
        this.computerBattlefield = computerBattlefield0;
    }


    /**
     * @return the computerHand
     */
    public final PlayerZone getComputerHand() {
        return computerHand;
    }


    /**
     * @param computerHand0 the computerHand to set
     */
    protected final void setComputerHand(final PlayerZone computerHand0) {
        this.computerHand = computerHand0;
    }


    /**
     * @return the computerGraveyard
     */
    public final PlayerZone getComputerGraveyard() {
        return computerGraveyard;
    }


    /**
     * @param computerGraveyard0 the computerGraveyard to set
     */
    protected final void setComputerGraveyard(
            final PlayerZone computerGraveyard0) // NOPMD by Braids on 8/27/11 10:53 PM
    {
        this.computerGraveyard = computerGraveyard0;
    }


    /**
     * @return the computerLibrary
     */
    public final PlayerZone getComputerLibrary() {
        return computerLibrary;
    }


    /**
     * @param computerLibrary0 the computerLibrary to set
     */
    protected final void setComputerLibrary(final PlayerZone computerLibrary0) {
        this.computerLibrary = computerLibrary0;
    }


    /**
     * @return the computerExile
     */
    public final PlayerZone getComputerExile() {
        return computerExile;
    }


    /**
     * @param computerExile0 the computerExile to set
     */
    protected final void setComputerExile(final PlayerZone computerExile0) {
        this.computerExile = computerExile0;
    }


    /**
     * @return the computerCommand
     */
    public final PlayerZone getComputerCommand() {
        return computerCommand;
    }


    /**
     * @param computerCommand0 the computerCommand to set
     */
    protected final void setComputerCommand(final PlayerZone computerCommand0) {
        this.computerCommand = computerCommand0;
    }


    /**
     * @return the stackZone
     */
    public final PlayerZone getStackZone() {
        return stackZone;
    }


    /**
     * @param stackZone0 the stackZone to set
     */
    protected final void setStackZone(final PlayerZone stackZone0) {
        this.stackZone = stackZone0;
    }


    /**
     * @return the zoneNamesToPlayerZones
     */
    public final Map<String, PlayerZone> getZoneNamesToPlayerZones() {
        return zoneNamesToPlayerZones;
    }


    /**
     * @param zoneNamesToPlayerZones0 the zoneNamesToPlayerZones to set
     */
    protected final void setZoneNamesToPlayerZones(
            final Map<String, PlayerZone> zoneNamesToPlayerZones0) // NOPMD by Braids on 8/27/11 10:53 PM
    {
        this.zoneNamesToPlayerZones = zoneNamesToPlayerZones0;
    }


    /**
     * Create and return the next timestamp.
     * 
     * @return the next timestamp
     */
    public final long getNextTimestamp() {
        setTimestamp(getTimestamp() + 1);
        return getTimestamp();
    }


    /**
     * @return the timestamp
     */
    public final long getTimestamp() {
        return timestamp;
    }


    /**
     * @param timestamp0 the timestamp to set
     */
    protected final void setTimestamp(final long timestamp0) {
        this.timestamp = timestamp0;
    }

}
