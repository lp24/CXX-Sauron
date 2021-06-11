package pt.tecnico.sauron.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.grpc.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.time.Instant;
import java.util.*;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;


public class SiloFrontend {
	private final int MAX_LOOP = 10;
	private final String serverPath="/grpc/sauron/silo";

	private SiloServiceGrpc.SiloServiceBlockingStub _stub;
	private ZKNaming zkNaming;
	private boolean reconnect = true;
	private HashMap<ByteString, ResponseTimeStamp> myCache = new HashMap<ByteString,ResponseTimeStamp>();
	//Each ByteString(user Request) have an associated TreeMap(the TimeStamp), that is associated to a ByteString(the server Response)
	
	private class ResponseTimeStamp{
		private TreeMap<Integer,Integer> _timeStamp;
		private ByteString _response;
		public ResponseTimeStamp(TreeMap<Integer,Integer> timestamp, ByteString response) {
			_timeStamp=timestamp;
			_response=response;
		}
		public TreeMap<Integer, Integer> getTimeStamp() {
			return _timeStamp;
		}
		public ByteString getResponse() {
			return _response;
		}
	}
	
	public SiloFrontend(String zooHost, String zooPort, String instance) throws ZKNamingException{	
		zkNaming = new ZKNaming(zooHost, zooPort);
		_stub = connectServerInstance(instance);
		reconnect = false;
	}
	
	public SiloFrontend(String zooHost, String zooPort)throws ZKNamingException {
		zkNaming = new ZKNaming(zooHost, zooPort);
		_stub = connectRandomServer();
		//reconnect=true
	}
	
	/*------------------------------------------------------------------
	 * 
	 * 
	 * CAM JOIN / CAM INFO / REPORT
	 * 
	 * 
	 * 
	 --------------------------------------------------------------*/

	public String camJoin (String camName, double latitude, double longitude)throws StatusRuntimeException, ZKNamingException{
		//Transform Client to grpc
		Location camLocation = Location.newBuilder().setCamLatitude(latitude).setCamLongitude(longitude).build();
		CamJoinRequest request= CamJoinRequest.newBuilder().setCamName(camName).setCamLocation(camLocation).build();
		return camJoin(request,0);
	}
	
	//Function to try a random server if the currently connected server is Down. Tries at most MAX_LOOP times.
	private String camJoin (CamJoinRequest request, int loopCtrl)throws StatusRuntimeException, ZKNamingException{
		try{
			//Send To Server
			CamJoinResponse response = _stub.camJoin(request);			
			//Inform Client
			return "Joined!\n";
		}
		catch(StatusRuntimeException e){
			if ((e.getStatus()==Status.DEADLINE_EXCEEDED || e.getStatus().getCode()==Status.Code.UNAVAILABLE)&& loopCtrl<MAX_LOOP && reconnect){ //Server Down
				System.out.println("catching JOIN SERVER DOWN exception: " + e.getMessage());
				_stub=connectRandomServer();
				return camJoin(request, loopCtrl+1);
			}
			else{
				throw e;
			}
		}	

	}

	public ClientLocation camInfo (String camName)throws StatusRuntimeException, ZKNamingException{
		//Transform Client to grpc
		CamInfoRequest request = CamInfoRequest.newBuilder().setCamName(camName).build();
		return camInfo(request,0);
	}
	
	//Function to try a random server if the currently connected server is Down. Tries at most MAX_LOOP times.
	private ClientLocation camInfo (CamInfoRequest request, int loopCtrl)throws StatusRuntimeException, ZKNamingException{
		try{
			//Send to Server
			CamInfoResponse response = _stub.camInfo(request);
			
			//CheckCache
			ByteString responseByteString = checkCache(request.toByteString(),response.toByteString(),response.getTimeStamp());
			response = CamInfoResponse.parseFrom(responseByteString);
			
			if(!response.hasCamLocation()) {
				return null;
			}
			
			//Inform Client
			return new ClientLocation(response.getCamLocation().getCamLatitude(), response.getCamLocation().getCamLongitude());
		}
		catch(StatusRuntimeException e){
			System.out.println("catching INFO exception");
			System.out.println("STATUS: " + e.getStatus());

			if ((e.getStatus()==Status.DEADLINE_EXCEEDED || e.getStatus().getCode()==Status.Code.UNAVAILABLE)&& loopCtrl<MAX_LOOP && reconnect){ //Server Down
				System.out.println("catching INFO SERVER DOWN exception: " + e.getMessage());
				_stub=connectRandomServer();
				return camInfo(request, loopCtrl+1);
			}
			else{
				throw e;
			}
		}
		catch (InvalidProtocolBufferException e) {
			System.out.println("\nSomething Wrong With Cache!\n");
			e.printStackTrace();
			return null;
		}	
	}
	
	//Function to do CamJoin if Cam doesnt exist in the replica yet
	public String report (String camName, double latitude, double longitude, ArrayList<String> observedStringList) throws StatusRuntimeException, ZKNamingException{ //list of strings "type,id"
		try {
			return report(camName,observedStringList);
		}
		catch(StatusRuntimeException e){
			if (e.getStatus()==Status.FAILED_PRECONDITION){
				camJoin(camName,latitude,longitude);
				return report(camName,observedStringList);
			}
			else {
				throw e;
			}
		}
	}
	public String report (String camName, ArrayList<String> observedStringList) throws StatusRuntimeException, ZKNamingException{ //list of strings "type,id"
		
		//Transform Client to grpc	
		ReportRequest.Builder requestBuilder = ReportRequest.newBuilder().setCamName(camName);

		for (String s : observedStringList) {
			String[] parts = s.split(",");
			requestBuilder.addObservedList(buildObservedFromString(parts[0],parts[1]));												
		}		
		ReportRequest request = requestBuilder.build();
		return report(request,0);
	}
	
	//Function to try a random server if the currently connected server is Down. Tries at most MAX_LOOP times.	
	private String report (ReportRequest request, int loopCtrl) throws StatusRuntimeException, ZKNamingException{ //list of strings "type,id"
		try{
			//Send to Server
			ReportResponse response = _stub.report(request); 
			
			//Inform Client
			return "Reported!\n";
		}
		catch(StatusRuntimeException e){
			if ((e.getStatus()==Status.DEADLINE_EXCEEDED || e.getStatus().getCode()==Status.Code.UNAVAILABLE)&& loopCtrl<MAX_LOOP && reconnect){ //Server Down
				_stub=connectRandomServer();
				return report(request, loopCtrl+1);
			}
			else{
				throw e;
			}
		}	

	}
	/*-----------------------------------------------------------
	 * 
	 * 
	 * 
	 * TRACK / TRACE / TRACKMATCH
	 * 
	 * 
	 * 
	 * 
	 ----------------------------------------------------------*/
	public ClientObservation track (String type, String id)throws StatusRuntimeException, ZKNamingException{	
		//Transform Client To grpc
		Observed obs= buildObservedFromString(type,id);
		TrackRequest request = TrackRequest.newBuilder().setObserved(obs).build();
		return track(request,0);
	}

	//Function to try a random server if the currently connected server is Down. Tries at most MAX_LOOP times.
	private ClientObservation track (TrackRequest request, int loopCtrl)throws StatusRuntimeException, ZKNamingException{	
		try{
			//Send to Server
			TrackResponse response = _stub.track(request);
			
			//Check Cache
			ByteString responseByteString = checkCache(request.toByteString(),response.toByteString(),response.getTimeStamp());
			response = TrackResponse.parseFrom(responseByteString);
			
			if(!response.hasObservation()) {
				return null;
			}
			
			//Inform Client
			return grpcObservationToClient(response.getObservation());
		}
		catch(StatusRuntimeException e){
			if ((e.getStatus()==Status.DEADLINE_EXCEEDED || e.getStatus().getCode()==Status.Code.UNAVAILABLE)&& loopCtrl<MAX_LOOP && reconnect){ //Server Down
				System.out.println("catching TRACK SERVER DOWN exception: " + e.getMessage());
				_stub=connectRandomServer();
				return track(request, loopCtrl+1);
			}
			else{
				throw e;
			}
		}
		catch (InvalidProtocolBufferException e) {
			System.out.println("\nSomething Wrong With Cache!\n");
			e.printStackTrace();
			return null;
		}	

	}
	
	public List<ClientObservation> trackMatch (String type, String id)throws StatusRuntimeException, ZKNamingException{
		//Transform Client to grpc
		Observed obs= buildObservedFromString(type,id);
		TrackMatchRequest request = TrackMatchRequest.newBuilder().setObserved(obs).build();
		return trackMatch(request,0);
	}
	
	//Function to try a random server if the currently connected server is Down. Tries at most MAX_LOOP times.
	private List<ClientObservation> trackMatch (TrackMatchRequest request, int loopCtrl)throws StatusRuntimeException, ZKNamingException{
		try{
			//Send to Server
			TrackMatchResponse response = _stub.trackMatch(request);
			
			//Check Cache
			ByteString responseByteString = checkCache(request.toByteString(),response.toByteString(),response.getTimeStamp());
			response = TrackMatchResponse.parseFrom(responseByteString);
			
			if(response.getObservationListList().isEmpty()) {
				return null;
			}
			
			//Inform Client
			List<Observation> obsList = response.getObservationListList();
			List<ClientObservation> clientList = new ArrayList<ClientObservation>();
			for(Observation observation: obsList) {
				clientList.add(grpcObservationToClient(observation));
			}
			return clientList;
		}
		catch(StatusRuntimeException e){
			if ((e.getStatus()==Status.DEADLINE_EXCEEDED || e.getStatus().getCode()==Status.Code.UNAVAILABLE)&& loopCtrl<MAX_LOOP && reconnect){ //Server Down
				System.out.println("catching TRACKMATCH SERVER DOWN exception: " + e.getMessage());
				_stub=connectRandomServer();
				return trackMatch(request, loopCtrl+1);
			}
			else{
				throw e;
			}
		}	
		catch (InvalidProtocolBufferException e) {
			System.out.println("\nSomething Wrong With Cache!\n");
			e.printStackTrace();
			return null;
		}	
	}

	public List<ClientObservation> trace (String type, String id)throws StatusRuntimeException, ZKNamingException{
		//Transform Client to grpc
		Observed obs= buildObservedFromString(type,id);
		TraceRequest request = TraceRequest.newBuilder().setObserved(obs).build();
		return trace(request,0);
	}

	//Function to try a random server if the currently connected server is Down. Tries at most MAX_LOOP times.
	private List<ClientObservation> trace (TraceRequest request, int loopCtrl)throws StatusRuntimeException, ZKNamingException{		
		try{
			//Send to Server
			TraceResponse response = _stub.trace(request);
			
			//Check Cache
			ByteString responseByteString = checkCache(request.toByteString(),response.toByteString(),response.getTimeStamp());
			response = TraceResponse.parseFrom(responseByteString);
			
			if(response.getObservationListList().isEmpty()) {
				return null;
			}
			
			//Inform Client
			List<Observation> obsList = response.getObservationListList();
			List<ClientObservation> clientList = new ArrayList<ClientObservation>();
			for(Observation observation: obsList) {
				clientList.add(grpcObservationToClient(observation));
			}
			return clientList;
		}
		catch(StatusRuntimeException e){
			if ((e.getStatus()==Status.DEADLINE_EXCEEDED || e.getStatus().getCode()==Status.Code.UNAVAILABLE)&& loopCtrl<MAX_LOOP && reconnect){ //Server Down
				System.out.println("catching TRACE SERVER DOWN exception: " + e.getMessage());
				_stub=connectRandomServer();
				return trace(request, loopCtrl+1);
			}
			else{
				throw e;
			}
		}
		catch (InvalidProtocolBufferException e) {
			System.out.println("\nSomething Wrong With Cache!\n");
			e.printStackTrace();
			return null;
		}	

	}	
	
	/*-----------------------------------
	 *
	 * 
	 * CONTROL OPERATIONS
	 * 
	 * 
	 * 
	 ---------------------------------------*/
	public String ctrlPing(String input) throws StatusRuntimeException{
		PingRequest request = PingRequest.newBuilder().setText(input).build();
	    PingResponse response = _stub.ctrlPing(request);
	    return response.getText();
	}
	

	public String ctrlClear() {
		ClearRequest request = ClearRequest.newBuilder().build();
		ClearResponse response = _stub.ctrlClear(request);
		return "Cleared!\n";
	}
	
	public String ctrlInit() {
		InitRequest request = InitRequest.newBuilder().build();
		InitResponse response = _stub.ctrlInit(request);
		return "Initialized!\n";
	}
	
	/* -------------------------------------------------------------------------------
	 * 
	 * 
	 * 
	 * 
	 * Auxiliary Functions
	 * 
	 * 
	 * 
	 * 
	 ----------------------------------------------------------------------------------*/
	
	//Transform String in Grpc Observed
	private Observed buildObservedFromString(String type, String id) {
		if(type.equals("car")) {
			return Observed.newBuilder().setId(id).setType(ObservedType.CAR).build();
		}
		if(type.equals("person")) {
			return Observed.newBuilder().setId(id).setType(ObservedType.PERSON).build();												
		}
		else {
			return null;
		}
	}
	
	//Transforms Grpc Observation in ClientObservation
	private ClientObservation grpcObservationToClient(Observation observation) {
		return new ClientObservation(
			observation.getObserved().getType().toString(), 
			observation.getObserved().getId(),
			Instant.ofEpochSecond(observation.getTime().getSeconds()),
			observation.getCamName(),
			observation.getCamLocation().getCamLatitude(),
			observation.getCamLocation().getCamLongitude());
	}
	
	//Connects to a Random Server Replica
	private SiloServiceGrpc.SiloServiceBlockingStub connectRandomServer() throws ZKNamingException {
		Collection<ZKRecord> recordList = zkNaming.listRecords(serverPath);
		ZKRecord record = getRandom(recordList);
		String target = record.getURI();
		System.out.println("Connecting to target: " + target);
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();		
		return SiloServiceGrpc.newBlockingStub(channel);
	}
	
	//Connects to a Specific Server Replica
	private SiloServiceGrpc.SiloServiceBlockingStub connectServerInstance(String instance) throws ZKNamingException {
		String instancePath = serverPath+"/"+instance;
		ZKRecord record = zkNaming.lookup(instancePath);
		String target = record.getURI();
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();		
		return SiloServiceGrpc.newBlockingStub(channel);	
	}
	
	//Returns a Random ZKRecord from a Collection
	private static ZKRecord getRandom(Collection<ZKRecord> coll) {
		int num = (int) (Math.random() * coll.size());
		for(ZKRecord record: coll) {
			if (--num < 0) {
		    	return record;
		    }
		}
		return null;
	}
	
	//Transforms Grpc TimeStamp in TreeMap<int,int> timeStamp
	private TreeMap<Integer, Integer> grpcTimeStampToClient(VectorialTimeStamp receivedTimeStamp) {
		TreeMap<Integer,Integer> vectorialTimeStamp = new TreeMap<Integer,Integer>();
		List<UpdateNr> updateNrList = receivedTimeStamp.getUpdateNrList();
		for(UpdateNr upNr : updateNrList) {
			int replicaNr = upNr.getReplicaNr();
			int replicaUpNr = upNr.getReplicaUpdateNr();
			vectorialTimeStamp.put(replicaNr, replicaUpNr);
		}
		return vectorialTimeStamp;
	}
	
	//Checks Cache to see if it has a more Recent Response
	private ByteString checkCache(ByteString requestByteString, ByteString responseByteString, VectorialTimeStamp grpcTimeStamp) {
		TreeMap<Integer,Integer> timeStamp = grpcTimeStampToClient(grpcTimeStamp);

		if(myCache.containsKey(requestByteString)) {
			if( isMoreRecent(myCache.get(requestByteString).getTimeStamp(), timeStamp)) {
				//Received Answer is more recent, Update Cache, return serverAnswer
				myCache.replace(requestByteString, new ResponseTimeStamp(timeStamp, responseByteString));
				return responseByteString;
			}
			else {
				//Cache answer is more recent, return cached answer
				return myCache.get(requestByteString).getResponse();
			}
		}
		else { //Nothing in Cache, put in cache, return serverAnswer
			myCache.put(requestByteString, new ResponseTimeStamp(timeStamp,responseByteString));
			return responseByteString;
		}
	}
	
	
	//Returns true if receivedTimeStamp is more recent than cacheTimeStamp
	private boolean isMoreRecent(TreeMap<Integer,Integer> cacheTimeStamp, TreeMap<Integer,Integer> receivedTimeStamp) {
		boolean hasBigger = false;
		boolean hasSmaller= false;
		
		for (int replicaNr: cacheTimeStamp.keySet()) {
			int cacheUpdateNr = cacheTimeStamp.get(replicaNr);
			int receivedUpdateNr = receivedTimeStamp.get(replicaNr);
			if(receivedUpdateNr>cacheUpdateNr) {
				hasBigger = true;
			}
			else if(receivedUpdateNr<cacheUpdateNr) {
				hasSmaller = true;
			}
		}
		return (hasBigger && !hasSmaller);
	}
}

