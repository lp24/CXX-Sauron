package pt.tecnico.sauron.silo.Domain;

public class SiloPerson extends SiloObserved{

	public SiloPerson(String id){
		super(id, SiloObservedType.PERSON);
	}
	
	public boolean checkId(){
		if(isInteger(getId())){
			return true;
		}
		else {
			return false;
		}
	}
	
	private boolean isInteger( String input ) {
	    try {
	        Integer.parseInt( input );
	        return true;
	    }
	    catch( NumberFormatException e ) {
	        return false;
	    }
	}
}