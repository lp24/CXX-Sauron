package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;


public class BaseIT {

	private static final String TEST_PROP_FILE = "/test.properties";
	protected static Properties testProps;
	static SiloFrontend frontend;
	
	final static String camName1 = "Cam1";
	final static String camName2 = "Cam2";
	final static String camName3 = "Cam3";

	final static double lat1 = 38.737613;
	final static double lg1 = -9.303164;
	final static double lat2 = 8.737613;
	final static double lg2 = -19.303164;
	final static ArrayList<String> reportList = new ArrayList<String>(Arrays.asList(
			"car,BV1234",
			"car,BX6782",
			"car,93JH92",
			"person,1254",
			"person,1234",
			"person,8765",
			"car,6930FG"));
			
	
	@BeforeAll
	public static void oneTimeSetup () throws IOException, ZKNamingException {
		testProps = new Properties();
		
		try {
			testProps.load(BaseIT.class.getResourceAsStream(TEST_PROP_FILE));
			System.out.println("Test properties:");
			System.out.println(testProps);
		}catch (IOException e) {
			final String msg = String.format("Could not load properties file {}", TEST_PROP_FILE);
			System.out.println(msg);
			throw e;
		}
		final String host = testProps.getProperty("zoo.host");
		final String port = testProps.getProperty("zoo.port");

		frontend=new SiloFrontend(host,port,"1");	
	}
	
	@AfterAll
	public static void cleanup() {
		frontend.ctrlClear();		
	}

}