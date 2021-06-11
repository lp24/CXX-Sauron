package pt.tecnico.sauron.silo.client;

import static io.grpc.Status.FAILED_PRECONDITION;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class TrackIT extends BaseIT{
	
	@BeforeEach
	public void setUp() throws StatusRuntimeException, ZKNamingException {
		frontend.ctrlClear();
		frontend.camJoin(camName1, lat1, lg1);
		frontend.report(camName1, reportList);
	}

	@Test
	public void trackOkTest()throws InterruptedException, StatusRuntimeException, ZKNamingException {  //throws Exception when thread sleeping is interruped. Shouldnt Happen
		TimeUnit.SECONDS.sleep(2);
		Instant now = Instant.now(); //to make sure observation returned is the more recent
		frontend.report(camName1, reportList);
		ClientObservation o = frontend.track("car", "BV1234");
		assertEquals("BV1234",o.getId());
		assertTrue(o.getTime().getEpochSecond()>=now.getEpochSecond());		
	}
	
	@Test
	public void trackNotFoundTest() {
		assertEquals(
	            FAILED_PRECONDITION.getCode(),
	            assertThrows(
	                    StatusRuntimeException.class, () ->		frontend.track("person", "1"))
	                .getStatus()
	                .getCode());
	}
	
	@AfterEach
	public void clear() {
		frontend.ctrlClear();
	}
}
