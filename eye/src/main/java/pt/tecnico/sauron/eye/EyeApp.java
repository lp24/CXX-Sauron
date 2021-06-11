package pt.tecnico.sauron.eye;

import java.util.*;

import java.util.ArrayList; // import the ArrayList class
import java.util.concurrent.TimeUnit;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

//import pt.tecnico.sauron.silo.client.SiloFrontend;
import java.math.BigDecimal;

import pt.tecnico.sauron.silo.client.ClientLocation;
import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class EyeApp {
	private static SiloFrontend _stub;

	public static void main(String[] args) throws InterruptedException, ZKNamingException { //for sleep, shouldnt happen
		System.out.println(EyeApp.class.getSimpleName());
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments   $ eye zoohost zooport Tagus 38.737613 -9.303164 <instance>
		if (args.length ==5) { //connects to random replica
			final String host = args[0];
			final String port =args[1];
			System.out.print("EyeApp: Initializing RANDOM frontend \n");
			_stub = new SiloFrontend(host,port);
			System.out.print("Frontend started\n");		
		}
		
		else if (args.length ==6) { //connects to specific replica
			final String host = args[0];
			final String port =args[1];
			final String instance = args[5];
			System.out.print("EyeApp: Initializing INSTANCE frontend \n" + instance);
			_stub = new SiloFrontend(host,port, instance);
		}
		else {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s host port camName latitude longitude <instance>%n", EyeApp.class.getName());
			return;
		}
		
		final String camName = args[2];
		final String latitude = args[3];
		final String longitude = args[4];
		if (!isLocation(latitude, longitude)) {
			System.out.println("Location argument(s) Error!");
			System.out.printf("Usage: java %s host port camName latitude longitude <instance>%n", EyeApp.class.getName());
			return;
		}
		final double lat = Double.parseDouble(latitude);
		final double lg = Double.parseDouble(longitude);

		try {
			System.out.println(_stub.ctrlPing("friend"));
		}
		catch(StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			return;
		}		
		
		Scanner keyboardSc;
		keyboardSc = new Scanner(System.in);
		ArrayList<String> observationArray = new ArrayList<String>();

		//--------------------------------------------------------------
		
		
		
		camJoin(camName, lat, lg);
		
		
		
		//--------------------------------------------------------------
		//Receive and Process User Input
		do {
			String observation = keyboardSc.nextLine();
			if (observation.isBlank()) {
				// Send data if accumulated previously
				if (observationArray.isEmpty() == false) {
					try{
						_stub.report(camName, lat, lg, observationArray);
						observationArray.clear();
					}
					catch(StatusRuntimeException e){
						System.out.println("Caught exception with description: " + e.getStatus().getDescription());
					}
				}
				continue;
			}
			
			if (observation.startsWith("#")) {
				continue; // Ignore the lines starting with #
			} 
			
			String observationArgs[] = observation.split(",");	
			String infoArgs[] = observation.split(" ");	

			if ("PERSON".equalsIgnoreCase(observationArgs[0]) || "CAR".equalsIgnoreCase(observationArgs[0])) {
				//Store to send
				observationArray.add(observation);
			}

			else if ("zzz".equals(observationArgs[0])) {
				//Pause data processing
				int time = Integer.parseInt(observationArgs[1]);
				TimeUnit.MILLISECONDS.sleep(time);
			} 
			else if("quit".equalsIgnoreCase(observationArgs[0])) {
				keyboardSc.close();	
				return;					
			}
			else if ("ctrl_ping".equalsIgnoreCase(observationArgs[0])) {
				ctrl_ping(observationArgs);
			}

			else if ("info".equals(infoArgs[0])) {
				camInfo(infoArgs);
			}
			else {
				System.out.println("Can't recognize this command\n");
				help();
			}
	
		} while (keyboardSc.hasNextLine());
			
		keyboardSc.close();	
	}
	
	
	/*-----------------------------------
	 * 
	 * 
	 * Controls Operations
	 * 
	 * 
	 -----------------------------------------*/
	
	private static void ctrl_ping(String[] commandArgs) {
		if(commandArgs.length!=1){
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
	
	/*-------------------------------------------
	 * 
	 * 
	 * Cam Join / Cam Info
	 * 
	 * 
	 * 
	 ---------------------------------------------*/

	private static void camJoin(String camName,double lat, double lg ) throws ZKNamingException {
		try {
			_stub.camJoin(camName, lat, lg);
			System.out.println("Camera added");
		}
		catch(StatusRuntimeException e) {
			if(e.getStatus()== Status.FAILED_PRECONDITION) {
				return;
			}
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			help();
			return;
		}
	}

	private static void camInfo(String[] commandArgs) throws ZKNamingException {
		if(commandArgs.length!=2){ //info camName
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
	
	/*------------------------------
	 * 
	 * Aux Functions
	 * 
	 * 
	 *------------------------------*/

	private static void help() {
		System.out.println("To join a camera: >>join <camName> <latitude> <longitude>. eg: join Tagus 38.737613 -9.303164");
		System.out.println("To report an observation:>>report <camName> <type> <id>. eg: report car AB1234");
		System.out.println("To spot an object type: >>spot <type> <id>. eg: spot person 14388236");
		System.out.println("To trail an object type: >>trail <type> <id>. eg:trail person 14388236");
		System.out.println("To Ping: >>ctrl_pint <name>. eg: ctrl_ping friend");
		System.out.println("To Clear: >>ctrl_clear");
		System.out.println("To Init: >>ctrl_init");
		System.out.println("To quit type: >>QUIT");
	}
	
	private static boolean isLocation(String latitutde, String longitude) {
		try {
			double lat = Double.parseDouble(latitutde);
			double lg = Double.parseDouble(longitude);
			
			if (Math.abs(lat) > 90 || Math.abs(lg) > 180) {
				return false;
			}
			BigDecimal blat = BigDecimal.valueOf(lat);
			BigDecimal blg = BigDecimal.valueOf(lg);
			if (blat.scale() != 6 || blg.scale() != 6) {
				return false;
			}
			return true;
		}
		catch(NumberFormatException e) {
			return false;
		}
	}
}