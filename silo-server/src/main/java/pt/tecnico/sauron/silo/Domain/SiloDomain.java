package pt.tecnico.sauron.silo.Domain;

import java.time.Instant;
import java.util.*;

import pt.tecnico.sauron.silo.Domain.Exceptions.*;


public class SiloDomain {
	private int _replicaNr;
	private int _instance;
	private enum Match {
		BEGIN,
		MIDDLE,
		END
	}
	private Map<String, SiloCam>  _cameraList   =  Collections.synchronizedMap (new HashMap<String, SiloCam>()); //Cam name->cam
    private List<SiloObservation> _observationList =  Collections.synchronizedList(new ArrayList <SiloObservation>());
    private Map<Integer, TreeMap<Integer, String>> _log = Collections.synchronizedMap(new TreeMap<Integer,TreeMap<Integer,String>>()); 
    //_log: Replica Nr -> (Update NR -> Operation)    
    
    public SiloDomain(Integer instance, Integer replicaNr) {
    	_instance=instance;
    	_replicaNr = replicaNr;
    	for(int i=1;i<=replicaNr;i++) {
    		_log.put(i,new TreeMap<Integer,String>());
    	}
    }

	//CamJoin for Client
	public void camJoin(String camName, SiloCam c1) throws CameraAlreadyExistsException, InvalidCamNameException{
		camJoin(camName, c1, _instance,_log.get(_instance).size()+1);
	}
	
	//CamJoin for Gossip
	public void camJoin(String camName, SiloCam c1, int replica, int update) throws CameraAlreadyExistsException, InvalidCamNameException{
		if(!SiloCam.isCamName(camName)) {
			throw new InvalidCamNameException("Invalid Cam Name!");
		}
		if (_cameraList.containsKey(camName)){
			SiloCam cam = _cameraList.get(camName);
			if(cam.getLocation().getLatitude()!=c1.getLocation().getLatitude() || cam.getLocation().getLongitude()!=c1.getLocation().getLongitude()) {
				throw new CameraAlreadyExistsException(camName);
			}
		}
		else {
			_cameraList.put(camName, c1);
		}
		updateMyLog(replica,update,"camJoin " + c1.toString());		
		return;
	}

	//Cam Info
	public SiloCam.CamLocation camInfo(String camName) throws CameraNotFoundException, InvalidCamNameException{
		if(!SiloCam.isCamName(camName)) {
			throw new InvalidCamNameException("Invalid Cam Name!");
		}
		if (!_cameraList.containsKey(camName)){
			throw new CameraNotFoundException(camName);
		}
		else{
			SiloCam c1 = _cameraList.get(camName);
			SiloCam.CamLocation l1 = c1.getLocation();
			return l1;
		}
	}

	//Report for Client
	public void report(String camName, ArrayList<SiloObserved> observed) throws CameraNotFoundException, InvalidIdException, InvalidCamNameException{
		Instant currentTime = Instant.now();
		report(camName,observed,currentTime, _instance, _log.get(_instance).size()+1 );
	}
	
	//Report for Gossip
	public void report(String camName, ArrayList<SiloObserved> observed, Instant time, int replica, int update )throws CameraNotFoundException, InvalidIdException, InvalidCamNameException{
		if(!SiloCam.isCamName(camName)) {
			throw new InvalidCamNameException("Invalid Cam Name!");
		}
		if (!_cameraList.containsKey(camName)){
			throw new CameraNotFoundException(camName);
		}	
		
		else{
			SiloCam cam = _cameraList.get(camName);
			ArrayList<SiloObservation> temp = new ArrayList<SiloObservation>();

			for (SiloObserved obs : observed) {
				if(!obs.checkId()) {
					throw new InvalidIdException(obs.getType().toString()+": "+obs.getId());
				}
				temp.add(new SiloObservation(obs, time, cam));
			}
			_observationList.addAll(temp);
			updateMyLog(replica, update, "report " + temp.toString());
			return;
		}
	}
	
	
	/*-----------------------------------------------------
	 * 
	 * 
	 * 
	 * Track/TrackMatch/Trace
	 * 
	 * 
	 ---------------------------------------------------*/

	//Track
	public SiloObservation track(SiloObserved observed) throws ObservationNotFoundException, InvalidIdException{
		if(!observed.checkId()) {
			throw new InvalidIdException(observed.getType().toString()+": "+observed.getId());
		}
		int len = _observationList.size();
		for (int i = len - 1 ; i >= 0 ; i--){
			SiloObservation obs = _observationList.get(i);
			if(obs.getObserved().getType().equals(observed.getType())
					&& obs.getObserved().getId().equals(observed.getId()))
				return obs;
		}
		throw new ObservationNotFoundException(observed.getType().toString() +":"+observed.getId());
	}

	//TrackMatch
	public ArrayList <SiloObservation> trackMatch(SiloObserved observed) throws ObservationNotFoundException, InvalidIdException{
		if(!SiloObserved.isPartialId(observed.getId())) {
			throw new InvalidIdException("Partial Id Must Contain '*' !");
		}
		ArrayList<SiloObservation> observations = new ArrayList<SiloObservation>();
		Match matchCase = this.checkMatchCase(observed.getId());
		for (int i = _observationList.size() - 1 ; i >= 0 ; i--){
			SiloObservation obs = _observationList.get(i);
			
			if(obs.getObserved().getType().equals(observed.getType())){
				String matchString = this.checkMatch(observed.getId(), obs.getObserved().getId(), matchCase);

				if(this.check(matchString, obs.getObserved().getId())){
					if(!this.existsDuplicated(obs.getObserved().getId(), observations))
						observations.add(obs);
				}
			}
		}
		if(observations.isEmpty()) {
			throw new ObservationNotFoundException(observed.getType().toString() +":"+observed.getId());
		}
		return observations;
	}

	//Trace
	public ArrayList <SiloObservation> trace(SiloObserved observed) throws ObservationNotFoundException, InvalidIdException{
		if(!observed.checkId()) {
			throw new InvalidIdException(observed.getType().toString()+": "+observed.getId());
		}
		ArrayList<SiloObservation> observations = new ArrayList<SiloObservation>();
		for (int i = _observationList.size() - 1 ; i >= 0 ; i--){
			SiloObservation obs = _observationList.get(i);
			if(		obs.getObserved().getType().equals(observed.getType())&& 
					obs.getObserved().getId().equals(observed.getId())) {
				observations.add(obs);
			}
		}
		if(observations.isEmpty()) {
			throw new ObservationNotFoundException(observed.getType().toString() +":"+observed.getId());
		}
		return observations;
	}
	
	/*-----------------------------------------
	 * 
	 * 
	 * Control Operations
	 * 
	-------------------------------------- */

	//Clear
	public void ctrlClear() {
		System.out.println("Clear!");
		 _cameraList      =   new HashMap<String, SiloCam>();
		 _observationList =   new ArrayList <SiloObservation>();
		return;
	}
	
	//Init
	public void ctrlInit() {
		return;
	}
	
	/*---------------------------------------------------------------------------------------
	 * Gossip Functions
	 * EX: Gossip from s1 to s2:
	 * -- siloGossip s1 asks serverdomain s1 getLastUpdates() to know needed updates
	 * -- siloGossip s1 asks serviceimpl s2 gossip(LastUpdates)
	 * -- serviceimpl s2 asks serverdomain s2 gossipRequest(LastUpdates)
	 * -- serverDomain s2 returns a _log of the needed updates to serviceimpl s2, who returns it to siloGossip s1
	 * -- siloGossip s1 calls serverdomain s1 functions to update it with received log
	 *
	 *
	 *
	 *
	 * 
	 ---------------------------------------------------------------------------------------*/

	//Returns this Replica TimeStamp
	public TreeMap<Integer,Integer> getLastUpdates() {//For each Replica
		TreeMap<Integer,Integer> myMap = new TreeMap<Integer,Integer>();
		for(Integer replicaNr : _log.keySet()) {
			myMap.put(replicaNr, _log.get(replicaNr).size());
		}
		return myMap;
	}
	
	
	//Returns the Updates after a Timestamp
	public TreeMap<Integer,TreeMap<Integer,String>> gossipRequest(TreeMap<Integer,Integer> vectorialTimeStamp) throws GossipException{
		if(vectorialTimeStamp.size()!=_replicaNr) {
			throw new GossipException("\nWrong TimeStamp Length!\n");
		}
		TreeMap<Integer,TreeMap<Integer,String>> myMap = new TreeMap<Integer,TreeMap<Integer,String>>();		
		
		for(Integer replicaNr : vectorialTimeStamp.keySet()) {
			int lastUpdateNr = vectorialTimeStamp.get(replicaNr);
			NavigableMap<Integer, String> missingUpdates = _log.get(replicaNr).tailMap(lastUpdateNr, false); //Gets SubMap that key>lastUpdateNr
			if(!missingUpdates.isEmpty()) {
				myMap.put(replicaNr, new TreeMap<Integer,String>(missingUpdates));
			}	
		}
		return myMap;		
	}
	
	/*------------------------------------------------------
	 * 
	 * 
	 * 
	 * Aux Functions
	 * 
	 * 
	 * 
	 * 
	------------------------------------------------------*/
    
	private void updateMyLog(int replica, int update, String operation) {
		System.out.println("Updating Log for Replica:" + replica + " update: " + update);
		_log.get(replica).put(update, operation);
	}
	
	public boolean existsDuplicated(String observationId, ArrayList<SiloObservation> observations ){
		for (SiloObservation obs: observations){
			if (observationId.compareToIgnoreCase(obs.getObserved().getId()) == 0) return true;
		}
		return false;
	}
	
	public boolean check(String matchString, String id){

		for (int i = 0, n = id.length(); i < n; i++){
			char c = matchString.charAt(i);
			char v = id.charAt(i);
			if ( (c != '*') && (v != c)) return false;
		}
		return true;
	}

	public String checkMatch(String partialId, String id, Match matchCase){

		char[] matchString = new char[id.length()];
		for (int i = 0, n = id.length(); i < n; i++) matchString[i] = '*';

		if(matchCase.equals(Match.END)){
			for (int i = 0, n = id.length(); i < n; i++) {
				if(i < partialId.length() ){
					char c = partialId.charAt(i);
					matchString[i] = c;
				}
				else {
					matchString[i] = '*';
				}
			}

		}
		else if(matchCase.equals(Match.BEGIN)){
			int j = partialId.length()-1;
			for (int i = id.length()-1; i >= 0; i--) {
				if(j>=0){
					char c = partialId.charAt(j);
					matchString[i] = c;
					j--;
				}
				else{
					matchString[i] = '*';
				}
			}

		}
		else {
			for (int i = 0, n = id.length(); i < n; i++) {
				if(i < partialId.length() ) {
					char c = partialId.charAt(i);
					if (c == '*') break;
					else matchString[i] = c;
				}
				else break;
			}

			int j = partialId.length()-1;
			for (int i = id.length()-1; i >= 0; i--) {
				if(j>=0) {
					char c = partialId.charAt(j);
					if (c == '*') break;
					else {
						matchString[i] = c;
						j--;
					}
				}
				else break;
			}

		}
		return new String(matchString);

	}
	public Match checkMatchCase(String partialId) {
		String delimiter = "*";

		int indexDelimiter = partialId.indexOf(delimiter);
		if(indexDelimiter == (partialId.length()- 1) ){
			return Match.END;
		}
		else if(indexDelimiter == 0){
			return Match.BEGIN;
		}
		return Match.MIDDLE;
	}

}
