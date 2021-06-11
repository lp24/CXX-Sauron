package pt.tecnico.sauron.silo.client;

import static io.grpc.Status.FAILED_PRECONDITION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.concurrent.TimeUnit;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class TraceIT extends BaseIT {
	
	@BeforeEach
	public void setUp() throws StatusRuntimeException, ZKNamingException {
		frontend.ctrlClear();
		frontend.camJoin(camName1, lat1, lg1);
		frontend.report(camName1, reportList);
	}
	
	@Test
	public void traceSingleObservationTest() throws StatusRuntimeException, ZKNamingException {
		List<ClientObservation> traceList = frontend.trace("car", "BV1234");
		assertEquals(1,traceList.size());
		assertEquals("BV1234",traceList.get(0).getId());
	}
	
	@Test
	public void traceDoubleObservationTest() throws InterruptedException, StatusRuntimeException, ZKNamingException{ //throws Exception when thread sleeping is interruped. Shouldnt Happen
		TimeUnit.SECONDS.sleep(1);
		frontend.report(camName1, reportList);
		List<ClientObservation> traceList = frontend.trace("person", "1234");
		assertEquals(2,traceList.size());
		assertEquals("1234",traceList.get(0).getId());
		assertEquals("1234",traceList.get(1).getId());
		assertNotEquals(traceList.get(0).getTime(),traceList.get(1).getTime());
	}
	
	@Test	
	public void traceNotFoundTest() {
		assertEquals(
	            FAILED_PRECONDITION.getCode(),
	            assertThrows(
	                    StatusRuntimeException.class, () ->		frontend.trace("person", "1"))
	                .getStatus()
	                .getCode());
	}

	@AfterEach
	public void clear() {
		frontend.ctrlClear();
	}
}
