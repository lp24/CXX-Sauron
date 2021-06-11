package pt.tecnico.sauron.silo.client;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.*;
import io.grpc.StatusRuntimeException;
import static io.grpc.Status.INVALID_ARGUMENT;

public class PingIT extends BaseIT {
	
	@Test
    public void pingOKTest() {	
        assertEquals("Hello friend!", frontend.ctrlPing("friend"));
    }
	
	@Test
    public void emptyPingTest() {
        assertEquals(
            INVALID_ARGUMENT.getCode(),
            assertThrows(
                    StatusRuntimeException.class, () -> frontend.ctrlPing(""))
                .getStatus()
                .getCode());
    }
}