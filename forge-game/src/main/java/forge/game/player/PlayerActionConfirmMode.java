package forge.game.player;

/** 
 * Used by PlayerController.confirmAction - to avoid a lot of hardcoded strings for mode
 *
 */
public enum PlayerActionConfirmMode {
    Random,
    // BraidOfFire,
    FromOpeningHand,
    ChangeZoneToAltDestination,
    ChangeZoneFromAltSource,
    ChangeZoneGeneral,
    BidLife,
    Tribute;
    // Ripple;
    
    
}