package pt.tecnico.sauron.spotter;

import java.util.*;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.tecnico.sauron.silo.client.ClientObservation;
import pt.tecnico.sauron.silo.client.SiloFrontend;


public class SpotterApp {
	
	private static SiloFrontend _stub;
	
	public static void main(String[] args) throws ZKNamingException {
		System.out.println(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}
				
		// check arguments
		if (args.length ==2) {		//to connect to random replica
			final String host = args[0];
			final String port =args[1];
			System.out.print("Spotter: Initializing frontend RANDOM \n");												
			_stub = new SiloFrontend(host,port);
			System.out.print("Frontend started\n");		
		}
		
		else if (args.length ==3) { 		//to connect to specific replica
			final String host = args[0];
			final String port =args[1];
			final String instance = args[2];
			System.out.println("Spotter: Initializing frontend INSTANCE:" + instance);												
			_stub = new SiloFrontend(host,port, instance);
		}
		else {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s host port <instance>%n", SpotterApp.class.getName());
			return;
		}
		
		try {
			System.out.println(_stub.ctrlPing("friend"));
		}
		catch(StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			return;
		}		
		
		Scanner keyboardSc;
		keyboardSc = new Scanner(System.in);
		
		/*-------------------------------
		 * 
		 * 
		 * Read and Process User Input
		 * 
		 * 
		 * 
		 --------------------------------------*/
		
		do {String command = keyboardSc.nextLine();
			String commandArgs[] = command.split(" ");
			
			if ("QUIT".equalsIgnoreCase(commandArgs[0])){
				keyboardSc.close();
				return;
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
			else {
				System.out.println("Unknown Command\n");
				help();
			}
			
		}while(true);
	}
	
	/*--------------------------------------------
	 * 
	 * 
	 * Trail/Spot Functions
	 * 
	 --------------------------------------*/
	
	private static void trail(String[] commandArgs) throws ZKNamingException {
		if(commandArgs.length!=3){ //trail type id
			help();
			return;			
		}
		try {			
			List<ClientObservation> trailList=_stub.trace(commandArgs[1], commandArgs[2]);
			if(trailList!=null) {

				Collections.sort(trailList); //order by time
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
		}
	}

	private static void spot(String[] commandArgs) throws ZKNamingException {
		if(commandArgs.length!=3){ //spot type id
			help();
			return;			
		}
		try {
			if(commandArgs[2].contains("*")) {
				List<ClientObservation> spotList = _stub.trackMatch(commandArgs[1],commandArgs[2]);
				if(spotList!=null) {

					Collections.sort(spotList); //order by id
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
		}	
	}
	
	/*--------------------------------------------
	 * 
	 * 
	 * Control Operations
	 * 
	 * 
	 * 
	 --------------------------------------*/
	
	
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
		}	
	}
	
	private static void ctrl_init(String[] commandArgs) {
		if(commandArgs.length!=1){
			help();
			return;			
		}
		try {
			System.out.println(_stub.ctrlInit());	
		}
		catch(StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			help();
		}	
	}

	private static void ctrl_clear(String[] commandArgs) {
		if(commandArgs.length!=1){
			help();
			return;			
		}
		try {
			System.out.println(_stub.ctrlClear());	
		}
		catch(StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			help();
		}	
	}
	
	//Available Commands
	private static void help() { 		
		System.out.println("To spot an object type: >>spot <type> <id>. eg: spot person 14388236");
		System.out.println("To trail an object type: >>trail <type> <id>. eg:trail person 14388236");
		System.out.println("To Ping: >>ctr_pint <name>. eg: ctr_ping friend");
		System.out.println("To Clear: >>ctr_clear");
		System.out.println("To Init: >>ctr_init");
		System.out.println("To quit type: >>QUIT");
	}

}
