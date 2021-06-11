package pt.tecnico.sauron.silo;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.tecnico.sauron.silo.Domain.SiloDomain;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class SiloServerApp {
	private final static String serverPath = "/grpc/sauron/silo";
		
	//Class to Unbind After Ctrl^C
	static class ZooUnbind extends Thread {
		private ZKNaming _zkNaming;
		private String _instancePath;
		private String _host;
		private String _port;
		ZooUnbind(ZKNaming zkNaming, String instancePath, String host, String port){
			_zkNaming = zkNaming;
			_instancePath = instancePath;
			_host=host;
			_port=port;			
		}
		public void run() {
			try{
				System.out.println("Unbinding!");
				_zkNaming.unbind(_instancePath,_host,_port);
			}
			catch(ZKNamingException e) {
				System.out.println("Unable to Unbind");
				e.printStackTrace();
			}
	     }
	}
	
	//Class to Schedule Gossip every x seconds
	static class spreadGossip extends TimerTask{
		private SiloGossip _siloGossip;
		spreadGossip(SiloGossip siloGossip){
			_siloGossip=siloGossip;
		}
		@Override
	    public void run() {
	    	_siloGossip.spreadGossip();
		}
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, ZKNamingException {

		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}
		
		if (args.length < 6) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s zooHost zooPort host port path n_instances%n", Server.class.getName());
			return;
		}
		
		final String zooHost = args[0];
		final String zooPort = args[1];
		final String host = args[2];
		final String port = args[3];
		final String instance = args[4];
		final String totalReplicasNr = args[5];
		final int GOSSIP_TIMER = Integer.parseInt(args[6]);

		final String instancePath = serverPath + "/" + instance;
		
		SiloDomain silo = new SiloDomain(Integer.parseInt(instance), Integer.parseInt(totalReplicasNr));
		SiloServiceImpl impl = new SiloServiceImpl(silo);

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService((BindableService) impl).build();
		
		// Start the server
		server.start();

		// Server threads are running in the background.
		System.out.println("Server started");
		
		//Zookeeper
		ZKNaming zkNaming = new ZKNaming(zooHost, zooPort);
		zkNaming.rebind(instancePath, host, port);		
        Runtime.getRuntime().addShutdownHook(new ZooUnbind(zkNaming, instancePath, host, port) );
        
		System.out.println("Zookeper bound");
		
		//Gossip
		Timer timer = new Timer(/*isDaemon*/ true);
		spreadGossip myTimerTask = new spreadGossip(new SiloGossip(silo, zkNaming, serverPath, Integer.parseInt(instance), Integer.parseInt(totalReplicasNr)));
        timer.schedule(myTimerTask, /*delay*/ 20 * 1000, /*period*/ GOSSIP_TIMER * 1000);
        
		System.out.println("Scheduled Gossip");

		// Do not exit the main thread. Wait until server is terminated.
		server.awaitTermination();		
	}

	

}