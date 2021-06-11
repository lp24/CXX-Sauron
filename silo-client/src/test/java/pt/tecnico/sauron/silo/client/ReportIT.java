package pt.tecnico.sauron.silo.client;

import static io.grpc.Status.FAILED_PRECONDITION;
import static io.grpc.Status.INVALID_ARGUMENT;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class ReportIT extends BaseIT {
	
	@BeforeEach
		public void setUp() throws StatusRuntimeException, ZKNamingException {
		frontend.camJoin(camName1, lat1, lg1);
	}

	@Test
	public void reportOkTest() throws StatusRuntimeException, ZKNamingException {
		System.out.println("OK TEST\n");
		frontend.report(camName1, reportList);
		frontend.track("car", "BV1234");
		frontend.track("car", "BX6782");
		frontend.track("car", "93JH92");
		frontend.track("person", "1254");
		frontend.track("person", "1234");
		frontend.track("person", "8765");
		frontend.track("car", "6930FG");
	}

	@Test
	public void reportCamNotFoundTest() {
		System.out.println("CAM NOT FOUND TEST\n");
		assertEquals(
	                FAILED_PRECONDITION.getCode(),
	                assertThrows(
	                        StatusRuntimeException.class, () ->		frontend.report(camName2, reportList))
	                        .getStatus()
	                        .getCode());
	}
	@Test
	public void reportInvalidCamNameTest() {
		System.out.println("INVALID CAM NAME TEST\n");
		assertEquals(
	                INVALID_ARGUMENT.getCode(),
	                assertThrows(
	                        StatusRuntimeException.class, () ->		frontend.report("ab", reportList))
	                        .getStatus()
	                        .getCode());
	}
	
	@Test
	public void reportInvalidIdTest() {
		System.out.println("INVALID ID TEST\n");
		ArrayList<String> reportListInvalid = new ArrayList<String>(reportList);
		reportListInvalid.add("person,ab");
		assertEquals(
	                INVALID_ARGUMENT.getCode(),
	                assertThrows(
	                        StatusRuntimeException.class, () ->		frontend.report(camName1, reportListInvalid))
	                        .getStatus()
	                        .getCode());
		assertEquals(
	            FAILED_PRECONDITION.getCode(),
	            assertThrows(
	                    StatusRuntimeException.class, () ->		frontend.track("car","BV1234"))
	                .getStatus()
	                .getCode());
		
	}
	
	@AfterEach
	public void clear() {
		frontend.ctrlClear();
	}
}
