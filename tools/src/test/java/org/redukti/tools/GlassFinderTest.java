package org.redukti.tools;

import org.junit.jupiter.api.Test;
import org.redukti.rayoptics.seq.Glass;

import static org.junit.jupiter.api.Assertions.*;

class GlassFinderTest {

    @Test
    void selectsAnExactMatchAndPreservesOtherSections() {
        String input = "[descriptive data]\n"
                + "title\tExample\n"
                + "[lens data]\n"
                + "1\t50\t4\t1.58267\t20\t46.48\n"
                + "[notes]\n"
                + "unchanged\n";

        GlassFinder.EnrichmentResult result = GlassFinder.enrich(input);
        Glass expected = Glass.find_glasses(1.58267, 46.48).get(0).glass();

        assertEquals(1, result.selected());
        assertEquals(0, result.ambiguous());
        assertTrue(result.text().contains(
                "1\t50\t4\t1.58267\t20\t46.48\t"
                        + expected.label + "\t" + expected.catalog_name));
        assertTrue(result.text().contains("[notes]\nunchanged\n"));
    }

    @Test
    void appendsCandidatesWithoutAssigningAnAmbiguousApproximateMatch() {
        String input = "[lens data]\n"
                + "1\t50\t4\t1.58270\t20\t46.50\n";

        GlassFinder.EnrichmentResult result = GlassFinder.enrich(input);

        assertEquals(0, result.selected());
        assertEquals(1, result.ambiguous());
        assertTrue(result.text().contains("\t\tcandidate="));
        assertTrue(result.text().split("candidate=", -1).length >= 3);
    }

    @Test
    void leavesExistingGlassAssignmentsUntouched() {
        String input = "[lens data]\n"
                + "1\t50\t4\t1.58267\t20\t46.19\tBAF3\tSchott\n";

        GlassFinder.EnrichmentResult result = GlassFinder.enrich(input);

        assertEquals(input, result.text());
        assertEquals(0, result.selected());
        assertEquals(0, result.ambiguous());
    }

    @Test
    void enrichmentIsIdempotentForCandidateRows() {
        String input = "[lens data]\n"
                + "1\t50\t4\t1.58270\t20\t46.50\n";

        String first = GlassFinder.enrich(input).text();
        String second = GlassFinder.enrich(first).text();

        assertEquals(first, second);
    }

    @Test
    void promotesExistingCandidatesWhenNdMatchesAndVdIsWithinOneDecimalPoint() {
        String input = "[lens data]\n"
                + "5\t90.458\t10.27\t1.497\t69.92\t81.6\t\t"
                + "\tcandidate=Hoya/FCD1,nd=1.49700,vd=81.61,dnd=0.00000,dvd=0.01"
                + "\tcandidate=Ohara/FPL51,nd=1.49700,vd=81.61,dnd=0.00000,dvd=0.01"
                + "\tcandidate=Schott/N-PK52A,nd=1.49700,vd=81.61,dnd=0.00000,dvd=0.01\n";

        GlassFinder.EnrichmentResult result = GlassFinder.enrich(input);

        assertEquals(1, result.selected());
        assertEquals(0, result.ambiguous());
        assertTrue(result.text().contains(
                "5\t90.458\t10.27\t1.497\t69.92\t81.6\tFCD1\tHoya\t"));
        assertEquals(3, result.text().split("candidate=", -1).length - 1);
    }

    @Test
    void reportsAndPreservesUnmatchedGlassData() {
        String input = "[lens data]\n"
                + "1\t50\t4\t1.10\t20\t10.0\n";

        GlassFinder.EnrichmentResult result = GlassFinder.enrich(input);

        assertEquals(1, result.unmatched());
        assertEquals(input, result.text());
    }
}
