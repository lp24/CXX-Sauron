package pt.tecnico.sauron.silo.Domain;

import java.math.BigDecimal;

public class SiloCam {
	public class CamLocation{
		private double _latitude;
		private double _longitude;
		
		public CamLocation(double lat, double lg){
			_latitude=lat;
			_longitude=lg;
		}
	
		public double getLatitude(){
			return _latitude;
		}
		public double getLongitude(){
			return _longitude;
		}
		public void setLatitude(double lat){
			_latitude=lat;
		}
		
		public void setLongitude(double lg){
			_longitude=lg;
		}
	
	}
	
	private String _camName;
	private CamLocation _camLocation;
	
	public SiloCam(String name, double lat, double lg){
		_camName=name;
		_camLocation=new CamLocation(lat,lg);
	}

	public CamLocation getLocation(){
		return _camLocation;
	}
	public String getCamName(){
		return _camName;
	}
	public void setCamName(String name){
		_camName=name;
	}
	public void setLocation(CamLocation location){
		_camLocation=location;
	}

	public void setLocation(double lat, double lg){
		_camLocation.setLatitude(lat);
		_camLocation.setLongitude(lg);
	}
	
	public static boolean isLocation(double lat, double lg) {

		if(Math.abs(lat)>90 || Math.abs(lg)>180){
			return false;
		}
		BigDecimal blat = BigDecimal.valueOf(lat);
		BigDecimal blg = BigDecimal.valueOf(lg);
		if(blat.scale()!=6 || blg.scale()!=6) {
			return false;
		}
		return true;
	}
	
	public static boolean isCamName(String s) {
		if(s.length()<3 || s.length()>15) {
			return false;
		}
		return s.matches("[A-Za-z0-9]+");
	}
	
	@Override
	public String toString() {
		return _camName + " " + Double.toString(_camLocation.getLatitude()) + " " + Double.toString(_camLocation.getLongitude());
	}
	
}