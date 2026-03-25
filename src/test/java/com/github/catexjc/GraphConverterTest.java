// Copyright 2026 Christopher Norman
package com.github.catexjc;

import com.github.catexjc.graph.DirectedGraph;
import com.github.catexjc.graph.GraphConverter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class GraphConverterTest {

    // -----------------------------------------------------------------------
    // DataProvider: vertex counts after excluding identity self-loops
    // -----------------------------------------------------------------------

    @DataProvider(name = "graphVertexCounts")
    public Object[][] graphVertexCounts() {
        return new Object[][] {
            { "two-object",  Fixtures.twoObjectCategory(), 2 },
            { "chain A≤B≤C", Fixtures.chainCategory(),    3 },
            { "diamond",     Fixtures.diamondCategory(),   4 },
        };
    }

    @Test(dataProvider = "graphVertexCounts")
    public void testVertexCount(final String name, final com.github.catexjc.core.FiniteCategory<String,String> cat, final int expected) {
        final DirectedGraph<String, String> g = GraphConverter.toGraph(cat);
        assertEquals(g.getVertices().size(), expected,
                "Vertex count mismatch for '" + name + "'");
    }

    // -----------------------------------------------------------------------
    // DataProvider: edge counts (excluding identities)
    // -----------------------------------------------------------------------

    @DataProvider(name = "graphEdgeCounts")
    public Object[][] graphEdgeCounts() {
        return new Object[][] {
            { "two-object",  Fixtures.twoObjectCategory(), 1 },  // only f
            { "chain A≤B≤C", Fixtures.chainCategory(),    3 },  // ab, bc, ac
            { "diamond",     Fixtures.diamondCategory(),   5 },  // botA, botB, aTop, bTop, botTop
        };
    }

    @Test(dataProvider = "graphEdgeCounts")
    public void testEdgeCount(final String name, final com.github.catexjc.core.FiniteCategory<String,String> cat, final int expected) {
        final DirectedGraph<String, String> g = GraphConverter.toGraph(cat);
        assertEquals(g.getEdges().size(), expected,
                "Edge count mismatch for '" + name + "'");
    }

    // -----------------------------------------------------------------------
    // Edge count with identities included
    // -----------------------------------------------------------------------

    @DataProvider(name = "graphEdgeCountsWithIdentities")
    public Object[][] graphEdgeCountsWithIdentities() {
        return new Object[][] {
            { "two-object",  Fixtures.twoObjectCategory(), 3 },  // f + id_A + id_B
            { "chain A≤B≤C", Fixtures.chainCategory(),    6 },
            { "diamond",     Fixtures.diamondCategory(),   9 },
        };
    }

    @Test(dataProvider = "graphEdgeCountsWithIdentities")
    public void testEdgeCountWithIdentities(final String name, final com.github.catexjc.core.FiniteCategory<String,String> cat, final int expected) {
        final DirectedGraph<String, String> g = GraphConverter.toGraph(cat, true);
        assertEquals(g.getEdges().size(), expected,
                "Edge count (with identities) mismatch for '" + name + "'");
    }

    // -----------------------------------------------------------------------
    // hasEdge queries
    // -----------------------------------------------------------------------

    @DataProvider(name = "hasEdgeCases")
    public Object[][] hasEdgeCases() {
        final DirectedGraph<String, String> g = GraphConverter.toGraph(Fixtures.chainCategory());
        return new Object[][] {
            { g, "A", "B", true  },
            { g, "B", "C", true  },
            { g, "A", "C", true  },
            { g, "C", "A", false },
            { g, "B", "A", false },
        };
    }

    @Test(dataProvider = "hasEdgeCases")
    public void testHasEdge(final DirectedGraph<String,String> g, final String u, final String v, final boolean expected) {
        assertEquals(g.hasEdge(u, v), expected,
                "hasEdge(" + u + "," + v + ") expected " + expected);
    }

    // -----------------------------------------------------------------------
    // Round-trip: graph → category → graph
    // -----------------------------------------------------------------------

    @Test
    public void testRoundTripGraphToCategory() {
        final DirectedGraph<String, String> g = GraphConverter.toGraph(Fixtures.twoObjectCategory());
        // g has vertices A, B and edge f: A→B
        final var cat2 = GraphConverter.fromGraph(g);
        // objects must match
        assertEquals(cat2.getObjects().size(), 2);
        // identities were added back
        final List<String> errors = cat2.validate();
        assertTrue(errors.isEmpty(), "Round-tripped category should be valid: " + errors);
    }

    // -----------------------------------------------------------------------
    // edgesFrom / edgesTo
    // -----------------------------------------------------------------------

    @Test
    public void testEdgesFromA() {
        final var g = GraphConverter.toGraph(Fixtures.chainCategory());
        final List<DirectedGraph.Edge<String, String>> from = g.edgesFrom("A");
        assertEquals(from.size(), 2, "A should have edges to B and C");
    }

    @Test
    public void testEdgesTo() {
        final var g = GraphConverter.toGraph(Fixtures.chainCategory());
        final List<DirectedGraph.Edge<String, String>> to = g.edgesTo("C");
        assertEquals(to.size(), 2, "C should have edges from B and A");
    }
}
