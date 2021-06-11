package pt.tecnico.sauron.silo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.Domain.SiloCam;
import pt.tecnico.sauron.silo.Domain.SiloCar;
import pt.tecnico.sauron.silo.Domain.SiloDomain;
import pt.tecnico.sauron.silo.Domain.SiloObserved;
import pt.tecnico.sauron.silo.Domain.SiloPerson;
import pt.tecnico.sauron.silo.Domain.Exceptions.CameraAlreadyExistsException;
import pt.tecnico.sauron.silo.Domain.Exceptions.CameraNotFoundException;
import pt.tecnico.sauron.silo.Domain.Exceptions.GossipException;
import pt.tecnico.sauron.silo.Domain.Exceptions.InvalidCamNameException;
import pt.tecnico.sauron.silo.Domain.Exceptions.InvalidIdException;
import pt.tecnico.sauron.silo.grpc.GossipRequest;
import pt.tecnico.sauron.silo.grpc.GossipResponse;
import pt.tecnico.sauron.silo.grpc.Update;
import pt.tecnico.sauron.silo.grpc.UpdateNr;
import pt.tecnico.sauron.silo.grpc.VectorialTimeStamp;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;
import pt.tecnico.sauron.silo.grpc.SiloServiceGrpc;

public class SiloGossip {
	private SiloDomain _silo;
	private ZKNaming _zk;
	private String _serverPath;
	private int _instance;
	private int _totalReplicasNr;
	private boolean _isConnected=false;
	SiloServiceGrpc.SiloServiceBlockingStub _stub;

	public SiloGossip(SiloDomain silo, ZKNaming zkNaming, String serverPath, int instance, int totalReplicasNr) {
		_silo=silo;
		_zk=zkNaming;
		_serverPath = serverPath;
		_instance=instance;
		_totalReplicasNr = totalReplicasNr;
	}
	
	//Sends Own TimeStamp, Receives Missing Updates to Apply
	/* Tries to Use Current Open Channel, only if is to the 'closer' replica
	 *  i.e. Replica 1 will always try to connect with replica 2, and replica 2 with replica 3 */
	public void spreadGossip(){
		System.out.println("Spreading Gossip");
		int i =0;
		while (i<_totalReplicasNr-1) {	
			String targetInstance = Integer.toString((_instance + i) % _totalReplicasNr + 1);
			try {
				if(!_isConnected) {
					_stub = connectServerInstance(targetInstance);
					if(i==0) { //Only Saves the Connection with the closer Replica
						_isConnected=true;
					}
				}
				
				TreeMap<Integer,Integer> vectorialTimeStamp = _silo.getLastUpdates();
	
				System.out.println("TimeStamp : " + vectorialTimeStamp.toString());
				
				GossipRequest request = GossipTimeStampToRequest(vectorialTimeStamp);	
				GossipResponse response = _stub.gossip(request);
				
				System.out.println("Response : " + response.toString());

				applyOps(response.getLogList());
				return;
			} 
			catch (GossipException e) {
					System.out.println("catching GOSSIP exception: " + e.getMessage());
					return;
			}	
			catch (ZKNamingException e) {
				System.out.println("Replica " + targetInstance + " unavailable");
				i+=1;
				_isConnected=false; //If it Connects to another replica, next gossip will try to reconnect with the 'closer'
				continue;
			}
			catch(StatusRuntimeException e){
				System.out.println("catching StatusRunTime exception: " + e.getMessage() );
				if (e.getStatus()==Status.DEADLINE_EXCEEDED || e.getStatus().getCode()==Status.Code.UNAVAILABLE){	//Server Down
					System.out.println("Replica " + targetInstance + " unavailable");
					i+=1;
					_isConnected=false;//If it Connects to another replica, next gossip will try to reconnect with the 'closer'
					continue;
				}
				else{
					throw e;
				}
			}
		}
		System.out.println("\nNo Targets Found!\n");	
    }

	//Apply the missing updates received from gossip
	private void applyOps(List<Update> logList) throws GossipException {
		ArrayList<Update> reportList = new ArrayList<Update>(); //Apply CamJoins Before Reports
		for (Update up: logList) {
			int replica = up.getUpdateNr().getReplicaNr();
			int update = up.getUpdateNr().getReplicaUpdateNr();
			String operation = up.getOperation();
			
			String[] string = operation.split("[ ]+",2);
			
			if (string[0].equals("camJoin")) {
				String[] camJoinString = string[1].split("[ ]+");
				//CamName Latitude Longitude
				if(camJoinString.length!=3) {	
					System.out.println("OperationException, Args: " + logList);
					throw new GossipException("\n Wrong Join Args Length!\n");
				}
				camJoin(replica, update, camJoinString);
			}
			else if (string[0].equals("report")) {
				//Wait for Joins
				reportList.add(up);	
			}
			else {
				System.out.println("OperationException, Args: " + logList);
				throw new GossipException ("\nUnknow Operation!\n");
			}
		}
		
		for (Update up: reportList) {
			int replica = up.getUpdateNr().getReplicaNr();
			int update = up.getUpdateNr().getReplicaUpdateNr();
			String operation = up.getOperation();
			
			String string = operation.substring("report [".length(),operation.length()-1); //"report [myList]" -> "myList"

			String[] reportString = string.split("[ ,]+");
			//Repeated  type, id, time, camName, lat, lg 
			if(reportString.length%6!=0) {	
				System.out.println("OperationException, Args: " + logList);
				throw new GossipException("\n Wrong Report Args Length!\n");
			}
			report(replica, update, reportString);
		}
		
	}

	//Parses 'camJoinArgs' to call SiloDomain.camJoin
	private void camJoin(int replica, int update, String[] camJoinArgs ) throws GossipException {
		try {
			
			String camName = camJoinArgs[0];
			String lat = camJoinArgs[1];
			String lg = camJoinArgs[2];
			SiloCam cam = new SiloCam (camName,Double.valueOf(lat), Double.valueOf(lg));
			_silo.camJoin(camName, cam, replica, update);
		}
		catch (NumberFormatException | NullPointerException e) {
			System.out.println("joinException, Args: " + camJoinArgs);
			throw new GossipException("\n Wrong Join Args!\n");
		}
		catch (CameraAlreadyExistsException | InvalidCamNameException e) {
			System.out.println("joinException, Args: " + camJoinArgs);
			throw new GossipException(e.getMessage());
		}
	}

	//Parses 'reportArgs' to call SiloDomain.report
	private void report(int replica, int update, String[] reportArgs) throws GossipException {
		try {
	
			ArrayList<SiloObserved> observationList = new ArrayList<SiloObserved>();
			int len = reportArgs.length; 
			int div = 6; //every 6 words is an SiloObservation: type, id, time, camName, lat, lg
		
			String compareTime = reportArgs[2];
			Instant instant = Instant.parse(compareTime);
			String compareCamName = reportArgs[3];
			String compareLat = reportArgs[4];
			String compareLg = reportArgs[5];
			//Check if values are double
			Double.valueOf(compareLat); 
			Double.valueOf(compareLg);
	
	
			for(int i=0;i<len;i+=div) {
				String type = reportArgs[i];
				String id = reportArgs[i+1];
				String time = reportArgs[i+2];
				String camName = reportArgs[i+3];
				String lat = reportArgs[i+4];
				String lg = reportArgs[i+5];
				
				if(!compareTime.equals(time)) {
					System.out.println("reportException, Args: " + reportArgs);
					throw new GossipException("\nReport Times Differ!\n");
				}
				if(!compareCamName.equals(camName) || !compareLat.equals(lat) || !compareLg.equals(lg) ) {
					System.out.println("reportException, Args: " + reportArgs);
					throw new GossipException("\nCams Differ!\n");
				}
				
				if(type.equals("CAR")) {
					observationList.add	(new SiloCar (id));
				}
				else if (type.equals("PERSON")) {
					observationList.add	(new SiloPerson(id));
				}
				else {
					System.out.println("reportException, Args: " + reportArgs);
					throw new GossipException("\nUnkown Observed Type!\n");
				}
			}	
			_silo.report(compareCamName, observationList, instant, replica, update);
		} 
		catch (CameraNotFoundException | InvalidIdException | InvalidCamNameException e) {
			System.out.println("ReportException, Args: " + reportArgs);
			throw new GossipException(e.getMessage());
		}
		catch(NumberFormatException | NullPointerException e) {
			System.out.println("reportException, Args: " + reportArgs);
			throw new GossipException("\n Wrong Report Args!\n");	
		}
	}

	//Transforms TimeStamp to Grpc TimeStamp
	private GossipRequest GossipTimeStampToRequest(TreeMap<Integer, Integer> vectorialTimeStamp) {
		VectorialTimeStamp.Builder timestampBuilder = VectorialTimeStamp.newBuilder();

		for(int replicaNr: vectorialTimeStamp.keySet()){
			int lastUpdate = vectorialTimeStamp.get(replicaNr);
			UpdateNr upNr = UpdateNr.newBuilder().setReplicaNr(replicaNr).setReplicaUpdateNr(lastUpdate).build();
			timestampBuilder.addUpdateNr(upNr);
		}
		VectorialTimeStamp timestamp = timestampBuilder.build();
		return GossipRequest.newBuilder().setVectorialTimeStamp(timestamp).build();
	}
	
	//Connects to a Specific Server Replica
	private SiloServiceGrpc.SiloServiceBlockingStub connectServerInstance(String instance) throws ZKNamingException {
		String instancePath = _serverPath+"/"+instance;
		ZKRecord record = _zk.lookup(instancePath);
		String target = record.getURI();
		System.out.println("Connection to target: "+ target);
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();		
		return SiloServiceGrpc.newBlockingStub(channel);	
	}

}
