
package forge;


public class Input_Draw extends Input {
    private static final long serialVersionUID = -2341125041806280507L;
    
    @Override
    public void showMessage() {
    	if (AllZone.Phase.isNeedToNextPhase())		// prevent input_draw from going when it should be the next phase
    		return;

    	AllZone.GameInfo.setHumanPlayedLands(0);	// this should move to untap phase probably.
    	
    	Player playerTurn = AllZone.GameAction.getPlayerTurn();
    	
    	if (GameActionUtil.draw_ShouldSkipDraw(playerTurn)){
    		AllZone.Phase.setNeedToNextPhase(true);
    		return;
    	}
    	
    	AllZone.GameAction.drawCard(playerTurn);
        GameActionUtil.executeDrawStepEffects();
        
        if(AllZone.Phase.getPhase().equals(Constant.Phase.Draw)) {
            AllZone.Phase.setNeedToNextPhase(true);
        } else stop();
    }
}
