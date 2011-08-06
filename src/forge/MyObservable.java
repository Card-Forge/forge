package forge;
import java.util.Observable;

public class MyObservable extends Observable
{
  public final void updateObservers()
  {	

    this.setChanged();
    this.notifyObservers();
    
    if(AllZone.Phase != null){
	    if(AllZone.Phase.isNeedToNextPhase()==true){
	    	if(AllZone.Phase.isNeedToNextPhaseInit() == true){
	    		AllZone.Phase.setNeedToNextPhase(false);
	    		AllZone.Phase.nextPhase();
	    	}
	    }
    }
     
  }
}

