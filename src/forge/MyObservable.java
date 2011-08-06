package forge;
import java.util.Observable;

public class MyObservable extends Observable
{
	public final void updateObservers()
	{	
		this.setChanged();
		this.notifyObservers();
		
		if(AllZone.Phase != null && AllZone.Phase.isNeedToNextPhase()){
		    	if(AllZone.Phase.isNeedToNextPhaseInit()){
		    		// this is used.
		    		AllZone.Phase.setNeedToNextPhase(false);
		    		AllZone.Phase.nextPhase();
		    }
		}
	}
}

