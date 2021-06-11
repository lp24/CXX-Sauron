package pt.tecnico.sauron.silo.Domain;

public class SiloCar extends SiloObserved{
	
	public SiloCar(String id){
		super(id, SiloObservedType.CAR);
	}
	
	public boolean checkId(){
		if(getId().length()!=6) {
			return false;
		}
		String first=getId().substring(0,2);
		String second=getId().substring(2,4);
		String third=getId().substring(4,6);
		
		if(isInteger(first)) {
			if(isInteger(second)) {
				if(isUpperCase(third)) {
					return true; //Int-Int-AZ
				}
			}
			else if(isUpperCase(second)) {
				if(isUpperCase(third)||isInteger(third)) {
					return true; //Int-AZ-Int, Int-AZ-AZ
				}
			}
		}
		else if(isUpperCase(first)) {
			if(isUpperCase(second)) {
				if(isInteger(third)) {
					return true; //AZ-AZ-Int
				}
			}
			else if(isInteger(second)) {
				if(isUpperCase(third)||isInteger(third)) {
					return true; //AZ-Int-Az, AZ-Int-Int
				}
			}
		}
		return false;
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
	
	private boolean isUpperCase(String str){
	        //convert String to char array
	        char[] charArray = str.toCharArray();
	        for(int i=0; i < charArray.length; i++){
	            if( !Character.isUpperCase( charArray[i] ))
	            return false;
	        }	        
	        return true;
	}
}