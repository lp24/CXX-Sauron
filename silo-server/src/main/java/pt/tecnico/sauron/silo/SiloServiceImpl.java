package pt.tecnico.sauron.silo;

import io.grpc.stub.StreamObserver;
import pt.tecnico.sauron.silo.Domain.*;
import pt.tecnico.sauron.silo.Domain.Exceptions.*;

import pt.tecnico.sauron.silo.grpc.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.protobuf.Timestamp;

import java.time.Instant;

import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.FAILED_PRECONDITION;


public class SiloServiceImpl extends SiloServiceGrpc.SiloServiceImplBase {
	private SiloDomain silo;
	
	public SiloServiceImpl(SiloDomain siloDomain) {
		silo=siloDomain;
	}


	@Override
	public void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver){
		
		//Transform Request Information to Server Domain
		String camName= request.getCamName();
		Location camLocation = request.getCamLocation();
		Double longitude = camLocation.getCamLongitude();
		Double latitude = camLocation.getCamLatitude();

		if (camName == null || camName.isBlank() || longitude==null || latitude==null || 
				!SiloCam.isLocation(latitude,longitude) ||!SiloCam.isCamName(camName)) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;
		}

		SiloCam c1 = new SiloCam(camName, latitude, longitude);
		try{
			silo.camJoin(camName,c1);

			//Build Response
			CamJoinResponse response = CamJoinResponse.newBuilder().build();
			//Send Response to Client
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		catch(InvalidCamNameException e) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;			
		}
		catch(CameraAlreadyExistsException e) {
			responseObserver.onError(FAILED_PRECONDITION.withDescription("Camera " + e.getMessage() + " already Exists!").asRuntimeException());
			return;

		}

	}

	@Override
	public void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver){
		//Transform Request Information to Server Domain
		String camName= request.getCamName();
		if (camName == null || camName.isBlank() || !SiloCam.isCamName(camName)) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;
		}
		try{
			SiloCam.CamLocation siloCamLocation = silo.camInfo(camName);
			TreeMap<Integer,Integer> siloTimeStamp = silo.getLastUpdates(); 
			//Possible Sync Problem? - Create Object to return Location and TimeStamp at once
			VectorialTimeStamp timeStamp = TimeStampToGrpc(siloTimeStamp);
			
			//Build Response
			Location grpcCamLocation = Location.newBuilder().setCamLatitude(siloCamLocation.getLatitude())
								  							.setCamLongitude(siloCamLocation.getLongitude())
								  							.build();
			CamInfoResponse response = CamInfoResponse.newBuilder().setCamLocation(grpcCamLocation).setTimeStamp(timeStamp).build();

			//Send Response to Client
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		catch(InvalidCamNameException e) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;			
		}
		catch(CameraNotFoundException e) {
			TreeMap<Integer,Integer> siloTimeStamp = silo.getLastUpdates(); 
			//Possible Sync Problem? - Create Object to return Location and TimeStamp at once
			VectorialTimeStamp timeStamp = TimeStampToGrpc(siloTimeStamp);	
			CamInfoResponse response = CamInfoResponse.newBuilder().setTimeStamp(timeStamp).build();

			//Send Response to Client
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}

	@Override	
	public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver){

		//Transform Request Information to Server Domain & Verify Args
		String camName= request.getCamName();
		List<Observed> grpcList=request.getObservedListList();

		if (camName == null || camName.isBlank() || grpcList==null || grpcList.isEmpty() || !SiloCam.isCamName(camName)) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
	        return;
	    }
		
		ArrayList<SiloObserved> observedList = grpcListToDomain(grpcList);
		if(observedList.isEmpty()) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
	        return;
		}
				
		//Send to Server and Receive Response
		try {
			silo.report(camName, observedList);
			
			//Build Response
			ReportResponse response= ReportResponse.newBuilder().build();
					
			//Send Response to Client														
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		catch(InvalidCamNameException e) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Camera Name!").asRuntimeException());
			return;			
		}
		catch(CameraNotFoundException e) {
	        responseObserver.onError(FAILED_PRECONDITION.withDescription("Camera "+e.getMessage()+ " Not Found!").asRuntimeException());
			return;
		}
		catch(InvalidIdException e) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Id Not Valid For Type "+e.getMessage()).asRuntimeException());
			return;
		}

	}

	@Override
	public void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) { 
		//Transform Request Information to Server Domain
		Observed grpcObserved = request.getObserved();
		
		if (grpcObserved == null) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;
	    }
		SiloObserved siloObserved = this.grpcObservedToDomain(grpcObserved);
		if (siloObserved == null) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;
	    }
		//Send to Server and Receive Response
		try{
			SiloObservation siloObservation = silo.track(siloObserved);
			TreeMap<Integer,Integer> siloTimeStamp = silo.getLastUpdates(); 
			//Possible Sync Problem? - Create Object to return Location and TimeStamp at once
			VectorialTimeStamp timeStamp = TimeStampToGrpc(siloTimeStamp);
			
			//Transform Server Domain Information to Response Information	
			Observation grpcObservation = domainObservationToGrpc(siloObservation);

			TrackResponse response = TrackResponse.newBuilder().setObservation(grpcObservation).setTimeStamp(timeStamp).build();

			//Send Response to Client
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		catch(InvalidIdException e) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
		}
		catch(ObservationNotFoundException e) {
			TreeMap<Integer,Integer> siloTimeStamp = silo.getLastUpdates(); 
			//Possible Sync Problem? - Create Object to return Location and TimeStamp at once
			VectorialTimeStamp timeStamp = TimeStampToGrpc(siloTimeStamp);	
			TrackResponse response = TrackResponse.newBuilder().setTimeStamp(timeStamp).build();

			//Send Response to Client
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}


	@Override
	public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
		//Transform Request Information to Server Domain
		Observed grpcObserved = request.getObserved();
		if (grpcObserved == null) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;
	    }
		SiloObserved siloObserved = this.grpcObservedToDomain(grpcObserved);
		if (siloObserved == null || !SiloObserved.isPartialId(siloObserved.getId())) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;
	    }
		//Send to Server and Receive Response
		try{
			ArrayList<SiloObservation> siloObservations = silo.trackMatch(siloObserved);
			TreeMap<Integer,Integer> siloTimeStamp = silo.getLastUpdates(); 
			//Possible Sync Problem? - Create Object to return Location and TimeStamp at once
			VectorialTimeStamp timeStamp = TimeStampToGrpc(siloTimeStamp);
	
			//Transform Server Domain Information to Response Information
			TrackMatchResponse.Builder responseBuilder = TrackMatchResponse.newBuilder();
	
			for(SiloObservation siloObservation:siloObservations){
				Observation grpcObservation = domainObservationToGrpc(siloObservation);
				responseBuilder.addObservationList(grpcObservation);
			}
			TrackMatchResponse response = responseBuilder.setTimeStamp(timeStamp).build();
	
			//Send Response to Client
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		catch(InvalidIdException e) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
		}
		catch(ObservationNotFoundException e) {
			TreeMap<Integer,Integer> siloTimeStamp = silo.getLastUpdates(); 
			//Possible Sync Problem? - Create Object to return Location and TimeStamp at once
			VectorialTimeStamp timeStamp = TimeStampToGrpc(siloTimeStamp);	
			TrackMatchResponse response =TrackMatchResponse.newBuilder().setTimeStamp(timeStamp).build();

			//Send Response to Client
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}


	@Override
	public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
		//Transform Request Information to Server Domain
		Observed grpcObserved = request.getObserved();
		if (grpcObserved == null) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;
	    }
		SiloObserved siloObserved = this.grpcObservedToDomain(grpcObserved);
		if (siloObserved == null) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;
	    }
		//Send to Server and Receive Response
		try {
			ArrayList<SiloObservation> siloObservations = silo.trace(siloObserved);
			TreeMap<Integer,Integer> siloTimeStamp = silo.getLastUpdates(); 
			//Possible Sync Problem? - Create Object to return Location and TimeStamp at once
			VectorialTimeStamp timeStamp = TimeStampToGrpc(siloTimeStamp);

			//Transform Server Domain Information to Response Information
			TraceResponse.Builder responseBuilder = TraceResponse.newBuilder();
	
			for(SiloObservation siloObservation:siloObservations){
				Observation grpcObservation = domainObservationToGrpc(siloObservation);
				responseBuilder.addObservationList(grpcObservation);
			}
			TraceResponse response = responseBuilder.setTimeStamp(timeStamp).build();
	
			//Send Response to Client
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		catch(InvalidIdException e) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
		}
		catch(ObservationNotFoundException e) {
			TreeMap<Integer,Integer> siloTimeStamp = silo.getLastUpdates(); 
			//Possible Sync Problem? - Create Object to return Location and TimeStamp at once
			VectorialTimeStamp timeStamp = TimeStampToGrpc(siloTimeStamp);	
			TraceResponse response = TraceResponse.newBuilder().setTimeStamp(timeStamp).build();

			//Send Response to Client
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
	}
	
	/*------------------------------------------------------------
	 * 
	 * 
	 * Control Operations
	 * 
	 * 
	 * 
	 -----------------------------------------------------------*/

	@Override
	public void ctrlPing(PingRequest request, StreamObserver<PingResponse> responseObserver) {
		    String input = request.getText();
		    if (input == null || input.isBlank()) {
		        responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty!").asRuntimeException());
				return;
		    }
		    String output = "Hello " + input + "!";
		    PingResponse response = PingResponse.newBuilder().setText(output).build();
		    responseObserver.onNext(response);
		    responseObserver.onCompleted();
	}
	
	@Override
	public void ctrlClear(ClearRequest request, StreamObserver<ClearResponse> responseObserver) {
		silo.ctrlClear();
		ClearResponse response = ClearResponse.newBuilder().build();
	    responseObserver.onNext(response);
	    responseObserver.onCompleted();
	}
	
	@Override
	public void ctrlInit(InitRequest request, StreamObserver<InitResponse> responseObserver) {
		silo.ctrlInit();
		InitResponse response = InitResponse.newBuilder().build();
	    responseObserver.onNext(response);
	    responseObserver.onCompleted();
	}
	
	/***************************
	 * 
	 * Gossip
	 *
	 ****************************/
	
	@Override
	public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {
		
		//Request->Domain
		List<UpdateNr> updateNrList = request.getVectorialTimeStamp().getUpdateNrList();
		
		if(	updateNrList == null || updateNrList.isEmpty()) {	
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Input!").asRuntimeException());
			return;	
		}

		TreeMap<Integer,Integer> vectorialTimeStamp = new TreeMap<Integer,Integer>();
		for(UpdateNr upNr : updateNrList) {
			int replicaNr = upNr.getReplicaNr();
			int replicaUpNr = upNr.getReplicaUpdateNr();
			vectorialTimeStamp.put(replicaNr, replicaUpNr);
		}
		
		try{
			//Ask Server
			TreeMap<Integer,TreeMap<Integer,String>> log = silo.gossipRequest(vectorialTimeStamp);
			
			//Domain->Response

			GossipResponse.Builder responseBuilder = GossipResponse.newBuilder();
			for(int replicaNr: log.keySet()) {
				for (int updateNr: log.get(replicaNr).keySet()){
					UpdateNr upNr = UpdateNr.newBuilder().setReplicaNr(replicaNr).setReplicaUpdateNr(updateNr).build();
					Update update = Update.newBuilder().setUpdateNr(upNr).setOperation(log.get(replicaNr).get(updateNr)).build();
					responseBuilder.addLog(update);
				}
			}
			GossipResponse response = responseBuilder.build();
			
			//Inform Client
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		catch(GossipException e) {
	        responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());			
		}
		
		
	}
	
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	//
	//
	//Aux Functions
	//
	//
	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	//Transforms from Grpc Observed to Domain Observed
	private SiloObserved grpcObservedToDomain(pt.tecnico.sauron.silo.grpc.Observed grpcObserved){
		ObservedType type = grpcObserved.getType();
		String id = grpcObserved.getId();
		switch(type) {
			case PERSON:
				return new SiloPerson(id);
			case CAR:
				return new SiloCar(id);
			default:
				return null;		
		}
	}
	
	//Transforms from domainObservation to GrpcObservation	
	private Observation domainObservationToGrpc(SiloObservation siloObservation){
		Instant time = siloObservation.getTime();
		SiloCam cam = siloObservation.getCam();
		Timestamp protoNow = Timestamp.newBuilder().setSeconds(time.getEpochSecond()).build();
		Location location = Location.newBuilder().setCamLatitude (cam.getLocation().getLatitude()).
												  setCamLongitude(cam.getLocation().getLongitude()).build();
		Observed.Builder grpcObservedBuilder = Observed.newBuilder().setId(siloObservation.getObserved().getId());
		
		switch(siloObservation.getObserved().getType()) {
			case CAR: 
				grpcObservedBuilder.setType(ObservedType.CAR);
				break;
			case PERSON:
				grpcObservedBuilder.setType(ObservedType.PERSON);	
				break;
		}
		Observed grpcObserved = grpcObservedBuilder.build();		
		Observation grpcObservation=Observation.newBuilder().
																			 setObserved(grpcObserved).
																			 setTime(protoNow).
																			 setCamLocation(location).
																			 setCamName(cam.getCamName()).
																			 build();
		return grpcObservation;	
	}
		
	//Transforms from grpcObserved List to domain Observed List
	private ArrayList<SiloObserved> grpcListToDomain(List<Observed> grpcList){
		ArrayList<SiloObserved> DomainObsList = new ArrayList<SiloObserved>();
		for(Observed obs : grpcList) {
			SiloObserved observed=grpcObservedToDomain(obs);
			if(observed==null) {
				return new ArrayList<SiloObserved>();
			}
			DomainObsList.add(observed);
		}
		return DomainObsList;
	}
	
	//Transforms TimeStamp to Grpc TimeStamp
	private VectorialTimeStamp TimeStampToGrpc(TreeMap<Integer, Integer> vectorialTimeStamp) {
		VectorialTimeStamp.Builder timestampBuilder = VectorialTimeStamp.newBuilder();

		for(int replicaNr: vectorialTimeStamp.keySet()){
			int lastUpdate = vectorialTimeStamp.get(replicaNr);
			UpdateNr upNr = UpdateNr.newBuilder().setReplicaNr(replicaNr).setReplicaUpdateNr(lastUpdate).build();
			timestampBuilder.addUpdateNr(upNr);
		}
		return timestampBuilder.build();
	}
}
