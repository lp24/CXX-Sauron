package pt.tecnico.sauron.silo.Domain;

public abstract class SiloObserved{
	
	public enum SiloObservedType{
		PERSON,
		CAR
	}
	
	private SiloObservedType _type;
	private String _id;

	public SiloObserved( String id, SiloObservedType type) {
		_id = id;
		_type=type;
	}
	
	public SiloObservedType getType(){
		return _type;
	}
	
	public void setType(SiloObservedType type ){
		_type=type;
	}
	
	public String getId(){
		 return _id;
	}
	
	public void setId(String id){
		 _id=id;
	}
	
	public abstract boolean checkId();
	
	public static boolean isPartialId(String s) {
		return s.contains("*");
	}
	
	public String toString() {
		return _type.toString() + " " + _id;
	}
}