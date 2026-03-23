package com.catex;

import com.catex.hasse.HasseDiagram;
import com.catex.hasse.HasseDiagramConverter;
import com.catex.lattice.LatticeConverter;
import com.catex.poset.PartialOrder;
import com.catex.poset.PosetConverter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

public class HasseDiagramTest {

    // -----------------------------------------------------------------------
    // DataProvider: node counts per fixture
    // -----------------------------------------------------------------------

    @DataProvider(name = "nodeCounts")
    public Object[][] nodeCounts() {
        return new Object[][] {
            { "two-object",  HasseDiagramConverter.fromCategory(Fixtures.twoObjectCategory()), 2 },
            { "chain",       HasseDiagramConverter.fromCategory(Fixtures.chainCategory()),     3 },
            { "diamond",     HasseDiagramConverter.fromCategory(Fixtures.diamondCategory()),   4 },
        };
    }

    @Test(dataProvider = "nodeCounts")
    public void testNodeCount(String name, HasseDiagram<String> hd, int expected) {
        assertEquals(hd.getNodes().size(), expected,
                "Node count mismatch for '" + name + "'");
    }

    // -----------------------------------------------------------------------
    // DataProvider: cover counts per fixture
    // -----------------------------------------------------------------------

    @DataProvider(name = "coverCounts")
    public Object[][] coverCounts() {
        return new Object[][] {
            // two-object: only f : A→B
            { "two-object", HasseDiagramConverter.fromCategory(Fixtures.twoObjectCategory()), 1 },
            // chain: A<B, B<C  (A<C is NOT a cover)
            { "chain",      HasseDiagramConverter.fromCategory(Fixtures.chainCategory()),     2 },
            // diamond: bot<a, bot<b, a<top, b<top  (bot<top is NOT a cover)
            { "diamond",    HasseDiagramConverter.fromCategory(Fixtures.diamondCategory()),   4 },
        };
    }

    @Test(dataProvider = "coverCounts")
    public void testCoverCount(String name, HasseDiagram<String> hd, int expected) {
        assertEquals(hd.getCovers().size(), expected,
                "Cover count mismatch for '" + name + "'");
    }

    // -----------------------------------------------------------------------
    // DataProvider: rank assignments in chain A≤B≤C
    // -----------------------------------------------------------------------

    @DataProvider(name = "chainRanks")
    public Object[][] chainRanks() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.chainCategory());
        return new Object[][] {
            { hd, "A", 0 },
            { hd, "B", 1 },
            { hd, "C", 2 },
        };
    }

    @Test(dataProvider = "chainRanks")
    public void testChainRanks(HasseDiagram<String> hd, String node, int expectedRank) {
        assertEquals(hd.rank(node), expectedRank,
                "Rank of '" + node + "' should be " + expectedRank);
    }

    // -----------------------------------------------------------------------
    // DataProvider: rank assignments in diamond
    // -----------------------------------------------------------------------

    @DataProvider(name = "diamondRanks")
    public Object[][] diamondRanks() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.diamondCategory());
        return new Object[][] {
            { hd, "bot", 0 },
            { hd, "a",   1 },
            { hd, "b",   1 },
            { hd, "top", 2 },
        };
    }

    @Test(dataProvider = "diamondRanks")
    public void testDiamondRanks(HasseDiagram<String> hd, String node, int expectedRank) {
        assertEquals(hd.rank(node), expectedRank,
                "Diamond rank of '" + node + "' should be " + expectedRank);
    }

    // -----------------------------------------------------------------------
    // maxRank
    // -----------------------------------------------------------------------

    @DataProvider(name = "maxRanks")
    public Object[][] maxRanks() {
        return new Object[][] {
            { "two-object", HasseDiagramConverter.fromCategory(Fixtures.twoObjectCategory()), 1 },
            { "chain",      HasseDiagramConverter.fromCategory(Fixtures.chainCategory()),     2 },
            { "diamond",    HasseDiagramConverter.fromCategory(Fixtures.diamondCategory()),   2 },
        };
    }

    @Test(dataProvider = "maxRanks")
    public void testMaxRank(String name, HasseDiagram<String> hd, int expected) {
        assertEquals(hd.maxRank(), expected, "maxRank mismatch for '" + name + "'");
    }

    // -----------------------------------------------------------------------
    // Layer contents
    // -----------------------------------------------------------------------

    @Test
    public void testChainLayerContents() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.chainCategory());
        assertEquals(hd.layer(0), List.of("A"));
        assertEquals(hd.layer(1), List.of("B"));
        assertEquals(hd.layer(2), List.of("C"));
    }

    @Test
    public void testDiamondLayer0() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.diamondCategory());
        assertEquals(hd.layer(0), List.of("bot"));
    }

    @Test
    public void testDiamondLayer1() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.diamondCategory());
        List<String> layer1 = hd.layer(1);
        assertEquals(layer1.size(), 2);
        assertTrue(layer1.containsAll(Set.of("a", "b")));
    }

    @Test
    public void testDiamondLayer2() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.diamondCategory());
        assertEquals(hd.layer(2), List.of("top"));
    }

    // -----------------------------------------------------------------------
    // coversAbove / coversBelow
    // -----------------------------------------------------------------------

    @Test
    public void testCoversAboveBotInDiamond() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.diamondCategory());
        List<HasseDiagram.Cover<String>> above = hd.coversAbove("bot");
        assertEquals(above.size(), 2, "bot should be covered by a and b");
        assertTrue(above.stream().anyMatch(c -> c.upper().equals("a")));
        assertTrue(above.stream().anyMatch(c -> c.upper().equals("b")));
    }

    @Test
    public void testCoversBelowTopInDiamond() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.diamondCategory());
        List<HasseDiagram.Cover<String>> below = hd.coversBelow("top");
        assertEquals(below.size(), 2, "top should be directly above a and b");
        assertTrue(below.stream().anyMatch(c -> c.lower().equals("a")));
        assertTrue(below.stream().anyMatch(c -> c.lower().equals("b")));
    }

    @Test
    public void testCoversAboveCInChain() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.chainCategory());
        List<HasseDiagram.Cover<String>> above = hd.coversAbove("C");
        assertTrue(above.isEmpty(), "C is the top of the chain, nothing above");
    }

    // -----------------------------------------------------------------------
    // nodesByRank ordering
    // -----------------------------------------------------------------------

    @Test
    public void testNodesByRankOrder() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.chainCategory());
        List<String> ordered = hd.nodesByRank();
        // A(rank 0), B(rank 1), C(rank 2)
        assertEquals(ordered.indexOf("A"), 0);
        assertEquals(ordered.indexOf("B"), 1);
        assertEquals(ordered.indexOf("C"), 2);
    }

    // -----------------------------------------------------------------------
    // fromLattice
    // -----------------------------------------------------------------------

    @Test
    public void testFromLattice() {
        var lattice = LatticeConverter.fromCategory(Fixtures.diamondCategory());
        HasseDiagram<String> hd = HasseDiagramConverter.fromLattice(lattice);
        assertEquals(hd.getNodes().size(), 4);
        assertEquals(hd.getCovers().size(), 4);
    }

    // -----------------------------------------------------------------------
    // fromPoset directly
    // -----------------------------------------------------------------------

    @Test
    public void testFromPoset() {
        PartialOrder<String> pos = PosetConverter.toPoset(Fixtures.chainCategory());
        HasseDiagram<String> hd  = HasseDiagramConverter.fromPoset(pos);
        assertEquals(hd.getCovers().size(), 2); // A<B and B<C
    }

    // -----------------------------------------------------------------------
    // Transitive relation NOT a cover (chain: A<C excluded)
    // -----------------------------------------------------------------------

    @Test
    public void testTransitiveRelationNotACover() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.chainCategory());
        boolean hasAC = hd.getCovers().stream()
                .anyMatch(c -> c.lower().equals("A") && c.upper().equals("C"));
        assertFalse(hasAC, "A<C is not a direct cover in the chain");
    }
}
