package org.redukti.optim;

import org.junit.jupiter.api.Test;
import org.redukti.examples.ExampleFinder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Reading and writing [pipeline n], and how it shares its numbering with the trials. */
class OptimizationPipelineTest {

    private static final String SUMMICRON = "Examples/jfotoptix/leica-summicron-50mm-f2/US004123144_Example08P.txt";

    private static final String TRIALS = """
            [trial 1]
            fields        0
            frequencies   20
            vary thicknesses 10

            [trial 2]
            fields        0
            frequencies   20
            vary thicknesses 0
            """;

    private static String withSections(String sections) throws Exception {
        return Files.readString(Path.of(ExampleFinder.geoPathToExample(SUMMICRON))) + "\n" + sections;
    }

    @Test
    void readsAndWritesAPipeline() throws Exception {
        String text = withSections(TRIALS + """

                [pipeline 7]
                description  Wide, then tele, twice   # a comment is not kept
                outdir       trials/zoom
                trials       1 2 1 2
                """);
        var pipeline = OptimizationTrial.readPipeline(text, 7);
        assertEquals(7, pipeline.number());
        assertEquals("Wide, then tele, twice", pipeline.description());
        assertEquals("trials/zoom", pipeline.outdir());
        assertArrayEquals(new int[]{1, 2, 1, 2}, pipeline.trials());
        assertArrayEquals(new int[]{1, 2}, pipeline.distinctTrials());
        assertEquals("""
                [pipeline 7]
                description           Wide, then tele, twice
                outdir                trials/zoom
                trials                1 2 1 2
                """, pipeline.toPipeline());
        // What it writes reads back the same.
        assertEquals(pipeline.toPipeline(),
                OptimizationTrial.readPipeline(withSections(TRIALS + "\n" + pipeline.toPipeline()), 7).toPipeline());
    }

    @Test
    void aTrialNumberIsNotAPipeline() throws Exception {
        String text = withSections(TRIALS + "\n[pipeline 7]\ntrials 1 2\n");
        assertNull(OptimizationTrial.readPipeline(text, 1));
        assertNotNull(OptimizationTrial.readPipeline(text, 7));
    }

    private static void assertRejected(String sections, int number, String message) throws Exception {
        String text = withSections(sections);
        var e = assertThrows(OptimizationTrial.TrialException.class,
                () -> OptimizationTrial.readPipeline(text, number));
        assertTrue(e.getMessage().contains(message), e.getMessage());
    }

    @Test
    void rejectsMistakes() throws Exception {
        assertRejected(TRIALS + "\n[pipeline 1]\ntrials 1 2\n", 1,
                "the number 1 is used by both [trial 1]");
        assertRejected(TRIALS + "\n[pipeline 7]\ntrials 1 3\n", 7,
                "there is no [trial 3] in this prescription");
        assertRejected(TRIALS + "\n[pipeline 7]\ntrials 1 8\n[pipeline 8]\ntrials 2\n", 7,
                "stage 8 is a pipeline; a pipeline runs trials, not other pipelines");
        assertRejected(TRIALS + "\n[pipeline 7]\ndescription x\n", 7,
                "pipeline 7: 'trials' is required");
        assertRejected(TRIALS + "\n[pipeline 7]\ntrials 1\nbogus 2\n", 7,
                "unknown keyword 'bogus'; a pipeline takes description, outdir and trials");
        assertRejected(TRIALS + "\n[pipeline 7]\ntrials 1\ntrials 2\n", 7,
                "'trials' is given more than once");
        assertRejected(TRIALS + "\n[pipeline 7]\ntrials one\n", 7,
                "expected a trial number, found 'one'");
        assertRejected(TRIALS + "\n[pipeline 7]\ntrials 1\n[pipeline 7]\ntrials 2\n", 7,
                "[pipeline 7] is defined twice");
        assertRejected(TRIALS, 9, "there is no [trial 9] or [pipeline 9] in this prescription; "
                + "it defines trials 1, 2 and no pipelines");
    }

    @Test
    void reportsProblemsWithTheirLine() throws Exception {
        String text = withSections(TRIALS + "\n[pipeline 7]\ntrials 1\nbogus 2\n");
        int line = java.util.List.of(text.split("\n", -1)).indexOf("bogus 2") + 1;
        var e = assertThrows(OptimizationTrial.TrialException.class,
                () -> OptimizationTrial.readPipeline(text, 7));
        assertTrue(e.getMessage().startsWith("pipeline 7, line " + line + ":"), e.getMessage());
    }
}
