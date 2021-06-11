package pt.tecnico.sauron.silo.client;

import static io.grpc.Status.FAILED_PRECONDITION;
import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
public class TrackMatchIT extends BaseIT {
	
	@BeforeEach
	public void setUp() throws StatusRuntimeException, ZKNamingException {
		frontend.ctrlClear();
		frontend.camJoin(camName1, lat1, lg1);
		frontend.report(camName1, reportList);
	}

	@Test
	public void trackMatchOkCarTest() throws StatusRuntimeException, ZKNamingException {
		List<ClientObservation> matchList1 = frontend.trackMatch("car", "B*");
		List<ClientObservation> matchList2 = frontend.trackMatch("car", "*4");
		assertEquals(2,matchList1.size());
		assertEquals(1,matchList2.size());
		assertTrue(	
				("BX6782".equals(matchList1.get(0).getId()) && "BV1234".equals(matchList1.get(1).getId()))|| 		
				("BX6782".equals(matchList1.get(1).getId()) && "BV1234".equals(matchList1.get(0).getId()))	
		);
		assertEquals("BV1234",matchList2.get(0).getId());
		assertEquals("CAR",matchList1.get(0).getType());
		assertEquals("CAR",matchList1.get(1).getType());
		assertEquals("CAR",matchList2.get(0).getType());	
	}
	@Test
	public void trackMatchOkPersonTest() throws StatusRuntimeException, ZKNamingException {
		List<ClientObservation> matchList1 = frontend.trackMatch("person", "12*");
		List<ClientObservation> matchList2 = frontend.trackMatch("person", "*5");
		assertEquals(2,matchList1.size());
		assertEquals(1,matchList2.size());
		assertTrue(	
				("1234".equals(matchList1.get(0).getId()) && "1254".equals(matchList1.get(1).getId())) ||	
				("1234".equals(matchList1.get(1).getId()) && "1254".equals(matchList1.get(0).getId()))
		);
		assertEquals("PERSON",matchList1.get(0).getType());
		assertEquals("PERSON",matchList1.get(1).getType());
		assertEquals("PERSON",matchList2.get(0).getType());	
	}
	
	@Test 
	public void trackMatchNotFoundTest() {
		assertEquals(
	            INVALID_ARGUMENT.getCode(),
	            assertThrows(
	                    StatusRuntimeException.class, () ->		frontend.trackMatch("person", "1"))
	                .getStatus()
	                .getCode());
		assertEquals(
	            FAILED_PRECONDITION.getCode(),
	            assertThrows(
	                    StatusRuntimeException.class, () ->		frontend.trackMatch("person", "10*"))
	                .getStatus()
	                .getCode());
	}
	
	@AfterEach
	public void clear() {
		frontend.ctrlClear();
	}
}
