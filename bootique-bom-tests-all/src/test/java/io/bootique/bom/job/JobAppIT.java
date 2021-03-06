package io.bootique.bom.job;

import io.bootique.command.CommandOutcome;
import io.bootique.job.runtime.JobModule;
import io.bootique.job.runtime.JobModuleProvider;
import io.bootique.test.TestIO;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JobAppIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    private BQTestFactory.Builder appBuilder(String... args) {
        return testFactory.app(args)
                .module(new JobModuleProvider())
                .module(b -> JobModule.extend(b).addJob(BomJob.class).addJob(BomParameterizedJob.class));
    }

    @Test
    public void testRun_Help() {

        TestIO io = TestIO.noTrace();
        CommandOutcome outcome = appBuilder("--help").bootLogger(io.getBootLogger()).run();
        assertEquals(0, outcome.getExitCode());

        String help = io.getStdout();

        assertTrue(help.contains("--exec"));
        assertTrue(help.contains("--list"));
        assertTrue(help.contains("--schedule"));
        assertTrue(help.contains("--help"));
        assertTrue(help.contains("--config"));
        assertTrue(help.contains("--job"));
    }

    @Test
    public void testList() {
        TestIO io = TestIO.noTrace();
        CommandOutcome outcome = appBuilder("--list").bootLogger(io.getBootLogger()).run();

        assertEquals(0, outcome.getExitCode());
        assertTrue(io.getStdout().contains("- bom"));
    }

    @Test
    public void testExec() {

        BomJob.COUNTER.set(0);

        CommandOutcome outcome = appBuilder("--exec", "--job=bom").run();
        assertEquals(0, outcome.getExitCode());
        assertEquals(1l, BomJob.COUNTER.get());
    }

    @Test
    public void testSchedule() throws InterruptedException {

        BomJob.COUNTER.set(0);
        BomParameterizedJob.COUNTER.set(0);

        CommandOutcome outcome = appBuilder("--schedule", "-c", "classpath:io/bootique/bom/job/test.yml").run();

        // wait for scheduler to run jobs...
        Thread.sleep(3000);

        assertEquals(0, outcome.getExitCode());

        assertTrue("Unexpected job count: " + BomJob.COUNTER.get(), BomJob.COUNTER.get() > 3);
        assertTrue(BomParameterizedJob.COUNTER.get() > 17 * 3);
        assertEquals(0, BomParameterizedJob.COUNTER.get() % 17);
    }

}
