package pt.tecnico.sauron.silo.client;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import org.junit.jupiter.api.Test;

import static io.grpc.Status.FAILED_PRECONDITION;
import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class CamJoinIT extends BaseIT{
	
	@BeforeEach
	public void setUp() throws StatusRuntimeException, ZKNamingException {
		frontend.camJoin(camName1, lat1, lg1);
		frontend.camJoin(camName2, lat2, lg2);
	}

	@Test
	public void camJoinOkTest() throws StatusRuntimeException, ZKNamingException {
		ClientLocation clientLocation = frontend.camInfo(camName1);
		assertTrue(lat1==clientLocation.getLatitude());
		assertTrue(lg1==clientLocation.getLongitude());
	}
	
	@Test
	public void camJoinRepeatedTest() {
        assertEquals(
                FAILED_PRECONDITION.getCode(),
                assertThrows(
                        StatusRuntimeException.class, () ->		frontend.camJoin(camName1, lat2, lg2))
                        .getStatus()
                        .getCode());
	}
	@Test
	public void camJoinInvalidTest() {
        assertEquals(
                INVALID_ARGUMENT.getCode(),
                assertThrows(
                        StatusRuntimeException.class, () ->		frontend.camJoin(camName1, 5, 6))
                        .getStatus()
                        .getCode());
        assertEquals(
                INVALID_ARGUMENT.getCode(),
                assertThrows(
                        StatusRuntimeException.class, () ->		frontend.camJoin("ab", lat1, lg1))
                        .getStatus()
                        .getCode());
        assertEquals(
                INVALID_ARGUMENT.getCode(),
                assertThrows(
                        StatusRuntimeException.class, () ->		frontend.camJoin("abcdefghijklmnopqrst", lat1, lg1))
                        .getStatus()
                        .getCode());
	}
	@AfterEach
	public void clear() {
		frontend.ctrlClear();
	}



}
