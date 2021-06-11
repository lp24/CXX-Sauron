package pt.tecnico.sauron.silo.client;

import static io.grpc.Status.FAILED_PRECONDITION;
import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class CamInfoIT extends BaseIT{
	
	@BeforeEach
	public void setUp() throws StatusRuntimeException, ZKNamingException {
		frontend.camJoin(camName1, lat1, lg1);
		frontend.camJoin(camName2, lat2, lg2);
	}

	@Test
	public void camInfoOkTest() throws StatusRuntimeException, ZKNamingException{
		ClientLocation clientLocation = frontend.camInfo(camName1);
		assertTrue(lat1==clientLocation.getLatitude());
		assertTrue(lg1==clientLocation.getLongitude());
	}
	
	@Test
	public void camInfoNotFoundTest() {
        assertEquals(
                FAILED_PRECONDITION.getCode(),
                assertThrows(
                        StatusRuntimeException.class, () ->		frontend.camInfo(camName3))
                        .getStatus()
                        .getCode());
	}
	@Test
	public void camInfoInvalidTest() {
        assertEquals(
                INVALID_ARGUMENT.getCode(),
                assertThrows(
                        StatusRuntimeException.class, () ->		frontend.camInfo("Ab"))
                        .getStatus()
                        .getCode());
	}
	
	@AfterEach
	public void clear() {
		frontend.ctrlClear();
	}

}
