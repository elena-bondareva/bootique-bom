package io.bootique.bom.jersey;

import io.bootique.bom.jersey.r1.Resource1;
import io.bootique.bom.jersey.r2.Resource2;
import io.bootique.command.CommandOutcome;
import io.bootique.jersey.JerseyModule;
import io.bootique.jersey.JerseyModuleProvider;
import io.bootique.test.TestIO;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JerseyAppIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    private BQTestFactory.Builder appBuilder(String... args) {
        return testFactory.app(args)
                .module(new JerseyModuleProvider())
                .module(b -> JerseyModule.extend(b)
                        .addFeature(JerseyAppFeature.class)
                        .addPackage(Resource1.class.getPackage())
                        .addResource(Resource2.class));
    }

    @Test
    public void testRun_Help() {

        TestIO io = TestIO.noTrace();
        CommandOutcome outcome = appBuilder("--help").bootLogger(io.getBootLogger()).run();
        assertEquals(0, outcome.getExitCode());

        assertTrue(io.getStdout().contains("--help"));
        assertTrue(io.getStdout().contains("--config"));
    }

    @Test
    public void testRun() {

        appBuilder("--config=src/test/resources/io/bootique/bom/jersey/test.yml", "--server").run();

        WebTarget base = ClientBuilder.newClient().target("http://localhost:12009/");

        // added as a part of a package
        Response r1 = base.path("/jt/jr/1").request().get();
        assertEquals(Status.OK.getStatusCode(), r1.getStatus());
        String expected1 = "bootique-jersey-resource1:--config=src/test/resources/io/bootique/bom/jersey/test.yml,--server";
        assertEquals(expected1, r1.readEntity(String.class));

        // added as individual resource
        Response r2 = base.path("/jt/jr/2").request().get();
        assertEquals(Status.OK.getStatusCode(), r2.getStatus());
        String expected2 = "bootique-jersey-resource2:--config=src/test/resources/io/bootique/bom/jersey/test.yml,--server";
        assertEquals(expected2, r2.readEntity(String.class));
    }

}
