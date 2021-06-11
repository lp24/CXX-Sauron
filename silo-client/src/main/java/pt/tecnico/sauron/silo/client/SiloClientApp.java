package pt.tecnico.sauron.silo.client;

import java.util.*;

import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SiloClientApp {
	static SiloFrontend _stub;
	
	public static void main(String[] args)throws ZKNamingException{
		System.out.println(SiloClientApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}
		
		// check arguments
		if (args.length ==2) {
			final String host = args[0];
			final String port =args[1];
			System.out.print("Initializing frontend RANDOM \n");												
			_stub = new SiloFrontend(host,port);
			System.out.print("Frontend started\n");		
		}
		
		else if (args.length ==3) {
			final String host = args[0];
			final String port =args[1];
			final String instance = args[2];
			System.out.println("Initializing frontend INSTANCE:" + instance);												
			_stub = new SiloFrontend(host,port, instance);
		}
		else {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s host port <instance>%n", SiloClientApp.class.getName());
			return;
		}
		
		try {
			System.out.println(_stub.ctrlPing("friend"));
		}
		catch(StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			return;
		}
		/* ------------------------------------------------------------------------------------ //
		 * 
		 * 
		 * Read and Process User Input
		 * 
		 * 
		// ------------------------------------------------------------------------------------ */

		Scanner keyboardSc;
		keyboardSc = new Scanner(System.in);
		
		do {String command = keyboardSc.nextLine();
			String commandArgs[] = command.split(" ");
			
			if ("QUIT".equalsIgnoreCase(commandArgs[0])){
				keyboardSc.close();
				return;
			}
			else if("join".equalsIgnoreCase(commandArgs[0])) {
				camJoin(commandArgs);
			}
			else if ("info".equalsIgnoreCase(commandArgs[0])) {
				camInfo(commandArgs);
			}

			else if("report".equalsIgnoreCase(commandArgs[0])) {
				report(commandArgs);
			}
			else if ("spot".equalsIgnoreCase(commandArgs[0])) {
				spot(commandArgs);
			}
			else if ("trail".equalsIgnoreCase(commandArgs[0])) {
				trail(commandArgs);
			}
			else if ("ctrl_ping".equalsIgnoreCase(commandArgs[0])) {
				ctrl_ping(commandArgs);
			}
			else if ("ctrl_clear".equalsIgnoreCase(commandArgs[0])) {
				ctrl_clear(commandArgs);
			}
			else if("ctrl_init".equalsIgnoreCase(commandArgs[0])) {
				ctrl_init(commandArgs);
			}
			else if("help".equalsIgnoreCase(commandArgs[0])) {
				help();
			}
			else if("checkpoint".equalsIgnoreCase(commandArgs[0])) {
				System.out.println("Checkpoint " + commandArgs[1]);
			}
			else {
				System.out.println("Unknown Command\n");
				help();
			}
			
		}while(true);
	}
	
	/*--------------------------------------------------------
	 * Eye Operations (CamJoin, CamInfo, Report)
	 * 
	 * 
	 * 
	----------------------------------------------- */

	private static void camJoin(String[] commandArgs) throws ZKNamingException {
		if(commandArgs.length!=4){ //join CamName Latitude Longitude
			help();
			return;			
		}
		
		try {
			double lat = Double.parseDouble(commandArgs[2]);
			double lg = Double.parseDouble(commandArgs[3]);
			
			_stub.camJoin(commandArgs[1],lat,lg);
		}
		
		catch(NullPointerException | NumberFormatException e) {
			help();
			return;
		}	
		catch(StatusRuntimeException e) {
			if(e.getStatus()==Status.FAILED_PRECONDITION) {
				return;
			}
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			help();
			return;
		}
	}

	private static void camInfo(String[] commandArgs) throws ZKNamingException {
		if(commandArgs.length!=2){ //CamInfo camName
			help();
			return;
		}

		try {
			ClientLocation clientLocation = _stub.camInfo(commandArgs[1]);
			if(clientLocation!=null) {
				System.out.println("Latitude " + clientLocation.getLatitude());
				System.out.println("Longitude " + clientLocation.getLongitude());
			}
		}
		catch(StatusRuntimeException e) {
			if(e.getStatus()==Status.FAILED_PRECONDITION) {
				return;
			}
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			help();
			return;
		}
	}
	
	private static void report(String[] commandArgs) throws ZKNamingException {
		if(commandArgs.length!=4){ //Report CamName Type Id
			help();
			return;			
		}
		try {
			String camName = commandArgs[1];
			String type= commandArgs[2];
			String id = commandArgs[3];
			ArrayList<String> myList = new ArrayList<String>();
			myList.add(type+","+id);
			_stub.report(camName, myList);			
		}
		catch(StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			help();
			return;
		}
	}
	
	/*--------------------------------------------------------
	 * Spotter Operations (Trail, Spot)
	 * 
	 * 
	 * 
	----------------------------------------------- */

	private static void trail(String[] commandArgs) throws ZKNamingException {
		if(commandArgs.length!=3){ //trail Type Id
			help();
			return;			
		}
		try {			
			List<ClientObservation> trailList=_stub.trace(commandArgs[1], commandArgs[2]);
			if(trailList!=null) {
				Collections.sort(trailList); //Order by Time
				for(ClientObservation o: trailList) {
					System.out.println(o.toString());
				}
			}
		}
		catch(StatusRuntimeException e) {
			if(e.getStatus()==Status.FAILED_PRECONDITION) {
				return;
			}
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			help();
			return;
		}
	}

	private static void spot(String[] commandArgs) throws ZKNamingException {
		if(commandArgs.length!=3){ //Spot Type Id
			help();
			return;			
		}
		try {
			if(commandArgs[2].contains("*")) {
				List<ClientObservation> spotList = _stub.trackMatch(commandArgs[1],commandArgs[2]);
				if(spotList!=null) {
					Collections.sort(spotList); //Order by Id
					for(ClientObservation o: spotList) {
						System.out.println(o.toString());
					}
				}
			}
			else{
				ClientObservation spotObs = _stub.track(commandArgs[1],commandArgs[2]);
				if(spotObs!=null) {
					System.out.println(spotObs.toString());
				}
			}
		}
		catch(StatusRuntimeException e) {
			if(e.getStatus()==Status.FAILED_PRECONDITION) {
				return;
			}
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			help();
			return;
		}	
	}
	
	/*--------------------------------------------------------
	 * Control Operations (Ping, Init, Clear)
	 * 
	 * 
	 * 
	----------------------------------------------- */
	
	private static void ctrl_ping(String[] commandArgs) {
		if(commandArgs.length!=2){
			help();
			return;			
		}
		try {
			System.out.println(_stub.ctrlPing(commandArgs[1]));
		}
		catch(StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			help();
			return;
		}	
	}
	
	private static void ctrl_init(String[] commandArgs) {
		if(commandArgs.length!=1){
			help();
			return;			
		}
		System.out.println(_stub.ctrlInit());		
	}

	private static void ctrl_clear(String[] commandArgs) {
		if(commandArgs.length!=1){
			help();
			return;			
		}
		System.out.println(_stub.ctrlClear());	
	}

	//Available Commands
	private static void help() {
		System.out.println("To join a camera: >>join <camName> <latitude> <longitude>. eg: join Tagus 38.737613 -9.303164");
		System.out.println("To report an observation:>>report <camName> <type> <id>. eg: report Tagus car AB1234");
		System.out.println("To spot an object type: >>spot <type> <id>. eg: spot person 14388236");
		System.out.println("To trail an object type: >>trail <type> <id>. eg:trail person 14388236");
		System.out.println("To Ping: >>ctrl_pint <name>. eg: ctrl_ping friend");
		System.out.println("To Clear: >>ctrl_clear");
		System.out.println("To Init: >>ctrl_init");
		System.out.println("To quit type: >>QUIT");
	}

}
