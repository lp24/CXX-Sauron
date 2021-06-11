package pt.tecnico.sauron.silo.client;

import java.time.Instant;

public class ClientObservation implements Comparable<ClientObservation>{
	private String _type;
	private String _id;
	private Instant  _time;
	private String _camName;
	private double _latitude;
	private double _longitude;
	
	public ClientObservation(String type, String id, Instant time, String camName, double latitude, double longitude){
		_type=type;
		_id=id;
		_time=time;
		_camName=camName;
		_latitude=latitude;
		_longitude=longitude;
	}	
	
	public String getType(){
		return _type;
	}
	public String getId(){
		return _id;
	}
	public Instant getTime(){
		return _time;
	}
	public String getCamName(){
		return _camName;
	}
	public double getLatitude(){
		return  _latitude;
	}
	public double getLongitude(){
		return _longitude;
	}
	public void setType(String type){
		_type=type;
	}
	public void setId(String id){
		_id=id;
	}
	public void setTime(Instant time){
		_time=time;
	}	
	public void setCamName(String name){
		_camName=name;
	}	
	public void setLatitude(long latitude){
		_latitude=latitude;
	}
	public void setLongitude(long longitude){
		_longitude=longitude;
	}
	
	@Override
	public String toString(){					
		return 	getType().toLowerCase()		+ "," + 
				getId()   					+ "," + 
				timeRemoveZ(getTime()) 		+ "," + 
				getCamName() 					+ "," + 
				Double.toString(getLatitude())+ "," + 
				Double.toString(getLongitude());
	}

	private String timeRemoveZ(Instant instant) {
		String s = instant.toString();
		return s.substring(0, s.length()-1);
	}

	@Override
	public int compareTo(ClientObservation o){
		if(getId()!=o.getId()){
			return getId().compareTo(o.getId());
		}
        return getTime().compareTo(o.getTime());
    }	
}

