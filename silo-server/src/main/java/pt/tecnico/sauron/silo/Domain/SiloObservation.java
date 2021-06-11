package pt.tecnico.sauron.silo.Domain;

import java.time.Instant;
import pt.tecnico.sauron.silo.Domain.SiloObserved;
import pt.tecnico.sauron.silo.Domain.SiloCam;

public class SiloObservation{
	private SiloObserved _observed;
	private Instant  _time;
	private SiloCam _cam;
	
	public SiloObservation(SiloObserved obs, Instant time, SiloCam cam){
		_observed=obs;
		_time=time;
		_cam=cam;
	}
	
	public SiloObserved getObserved(){
		return _observed;
	}
	
	public void setObserved(SiloObserved obs){
		_observed=obs;
	}
	
	public Instant getTime(){
		return _time;
	}
	
	public void setTime(Instant time){
		_time=time;
	}	
	
	public void setCam(SiloCam cam) {
		_cam=cam;
	}
	
	public SiloCam getCam() {
		return _cam;
	}
	
	@Override
	public String toString() {
		return _observed.toString() + " " + _time.toString() + " " + _cam.toString();
	}
}