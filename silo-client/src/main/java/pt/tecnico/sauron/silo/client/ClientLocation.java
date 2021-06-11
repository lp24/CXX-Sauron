package pt.tecnico.sauron.silo.client;

import java.time.Instant;

public class ClientLocation{

	private double _latitude;
	private double _longitude;

	public ClientLocation(double latitude, double longitude){

		_latitude=latitude;
		_longitude=longitude;
	}	
	


	public double getLatitude(){
		return  _latitude;
	}
	public double getLongitude(){
		return _longitude;
	}


	public void setLatitude(long latitude){
		_latitude=latitude;
	}
	public void setLongitude(long longitude){
		_longitude=longitude;
	}



}

