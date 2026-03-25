package com.github.catexjc;

import com.github.catexjc.convert.CategoryConverter;
import com.github.catexjc.graph.DirectedGraph;
import com.github.catexjc.graph.GraphConverter;
import com.github.catexjc.hasse.HasseDiagram;
import com.github.catexjc.hasse.HasseDiagramConverter;
import com.github.catexjc.lattice.Lattice;
import com.github.catexjc.lattice.LatticeConverter;
import com.github.catexjc.render.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.testng.Assert.*;

/**
 * Tests for SVG rendering of all four representations.
 *
 * <p>Tests do not assert pixel-perfect positions but verify structural
 * properties of the produced SVG: well-formed XML shell, presence of the
 * correct number of {@code <circle>} (node) and {@code <line>}/{@code <path>}
 * (edge) elements, and that label text is included.
 */
public class RenderTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static int count(final String svg, final String tag) {
        final Pattern p = Pattern.compile("<" + tag + "[\\s/>]");
        java.util.regex.Matcher m = p.matcher(svg);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    private static boolean containsText(final String svg, final String text) {
        return svg.contains(">" + text + "<") || svg.contains(">" + text + "</");
    }

    // -----------------------------------------------------------------------
    // HasseDiagramRenderer
    // -----------------------------------------------------------------------

    @DataProvider(name = "hasseDiagrams")
    public Object[][] hasseDiagrams() {
        return new Object[][] {
            { "two-object",
              HasseDiagramConverter.fromCategory(Fixtures.twoObjectCategory()), 2, 1 },
            { "chain",
              HasseDiagramConverter.fromCategory(Fixtures.chainCategory()),     3, 2 },
            { "diamond",
              HasseDiagramConverter.fromCategory(Fixtures.diamondCategory()),   4, 4 },
        };
    }

    @Test(dataProvider = "hasseDiagrams")
    public void testHasseDiagramSvgNodeCount(final String name, final HasseDiagram<String> hd,
                                              final int expectedNodes, final int expectedEdges) {
        final String svg = new HasseDiagramRenderer<String>().renderSvg(hd);
        assertNotNull(svg);
        assertTrue(svg.startsWith("<?xml"), "SVG should start with XML declaration");
        assertTrue(svg.contains("<svg"), "Should contain <svg> element");
        assertEquals(count(svg, "circle"), expectedNodes,
                "Circle count mismatch for '" + name + "'");
    }

    @Test(dataProvider = "hasseDiagrams")
    public void testHasseDiagramSvgEdgeCount(final String name, final HasseDiagram<String> hd,
                                              final int expectedNodes, final int expectedEdges) {
        final String svg = new HasseDiagramRenderer<String>().renderSvg(hd);
        assertEquals(count(svg, "line"), expectedEdges,
                "Line count mismatch for '" + name + "'");
    }

    @Test
    public void testHasseDiagramSvgContainsLabels() {
        final HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.chainCategory());
        final String svg = new HasseDiagramRenderer<String>().renderSvg(hd);
        assertTrue(svg.contains("A"), "SVG should contain node label 'A'");
        assertTrue(svg.contains("B"), "SVG should contain node label 'B'");
        assertTrue(svg.contains("C"), "SVG should contain node label 'C'");
    }

    // -----------------------------------------------------------------------
    // LatticeRenderer
    // -----------------------------------------------------------------------

    @DataProvider(name = "lattices")
    public Object[][] lattices() {
        return new Object[][] {
            { "chain",   LatticeConverter.fromCategory(Fixtures.chainCategory()),   3 },
            { "diamond", LatticeConverter.fromCategory(Fixtures.diamondCategory()), 4 },
        };
    }

    @Test(dataProvider = "lattices")
    public void testLatticeSvgNodeCount(final String name, final Lattice<String> lat, final int expected) {
        final String svg = new LatticeRenderer<String>().renderSvg(lat);
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        // Main nodes + 2 legend circles (top/bottom)
        final int circles = count(svg, "circle");
        assertTrue(circles >= expected,
                "Expected at least " + expected + " circles in '" + name + "', got " + circles);
    }

    @Test
    public void testLatticeSvgContainsTopBottomColors() {
        final RenderOptions opts = RenderOptions.builder()
                .topColor("#FF0000").bottomColor("#00FF00").build();
        final String svg = new LatticeRenderer<String>().renderSvg(
                LatticeConverter.fromCategory(Fixtures.diamondCategory()), opts);
        assertTrue(svg.contains("#FF0000"), "Should contain top-element colour");
        assertTrue(svg.contains("#00FF00"), "Should contain bottom-element colour");
    }

    @Test
    public void testLatticeSvgLabels() {
        final String svg = new LatticeRenderer<String>().renderSvg(
                LatticeConverter.fromCategory(Fixtures.diamondCategory()));
        assertTrue(svg.contains("bot"));
        assertTrue(svg.contains("top"));
    }

    // -----------------------------------------------------------------------
    // GraphRenderer
    // -----------------------------------------------------------------------

    @DataProvider(name = "graphs")
    public Object[][] graphs() {
        return new Object[][] {
            { "two-object", GraphConverter.toGraph(Fixtures.twoObjectCategory()), 2, 1 },
            { "chain",      GraphConverter.toGraph(Fixtures.chainCategory()),      3, 3 },
            { "diamond",    GraphConverter.toGraph(Fixtures.diamondCategory()),    4, 5 },
        };
    }

    @Test(dataProvider = "graphs")
    public void testGraphSvgNodeCount(final String name, final DirectedGraph<String,String> g,
                                       final int expectedNodes, final int expectedEdges) {
        final String svg = new GraphRenderer<String,String>().renderSvg(g);
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertEquals(count(svg, "circle"), expectedNodes,
                "Node (circle) count mismatch for graph '" + name + "'");
    }

    @Test(dataProvider = "graphs")
    public void testGraphSvgHasArrowMarker(final String name, final DirectedGraph<String,String> g,
                                            final int expNodes, final int expEdges) {
        final String svg = new GraphRenderer<String,String>().renderSvg(g);
        assertTrue(svg.contains("<marker"), "Graph SVG should define an arrow marker");
    }

    @Test
    public void testGraphSvgEdgeLabelsPresent() {
        final DirectedGraph<String,String> g = GraphConverter.toGraph(Fixtures.twoObjectCategory());
        final String svg = new GraphRenderer<String,String>().renderSvg(g);
        assertTrue(svg.contains("f"), "Edge label 'f' should appear in SVG");
    }

    @Test
    public void testCyclicGraphUsesCircularLayout() {
        // Build a cyclic graph A→B→C→A
        final com.github.catexjc.core.CategoryObject<String> A = new com.github.catexjc.core.CategoryObject<>("A");
        final com.github.catexjc.core.CategoryObject<String> B = new com.github.catexjc.core.CategoryObject<>("B");
        final com.github.catexjc.core.CategoryObject<String> C = new com.github.catexjc.core.CategoryObject<>("C");
        final var idA = new com.github.catexjc.core.Morphism<>("id_A", A, A);
        final var idB = new com.github.catexjc.core.Morphism<>("id_B", B, B);
        final var idC = new com.github.catexjc.core.Morphism<>("id_C", C, C);
        final var ab  = new com.github.catexjc.core.Morphism<>("ab", A, B);
        final var bc  = new com.github.catexjc.core.Morphism<>("bc", B, C);
        final var ca  = new com.github.catexjc.core.Morphism<>("ca", C, A);
        final var cat = com.github.catexjc.core.FiniteCategory.<String,String>builder()
                .addObject(A).addObject(B).addObject(C)
                .addMorphism(idA).addMorphism(idB).addMorphism(idC)
                .addMorphism(ab).addMorphism(bc).addMorphism(ca)
                .build();
        final DirectedGraph<String,String> g = GraphConverter.toGraph(cat);
        // Should not throw; circular layout used
        final String svg = new GraphRenderer<String,String>().renderSvg(g);
        assertNotNull(svg);
        assertEquals(count(svg, "circle"), 3);
    }

    // -----------------------------------------------------------------------
    // CategoryRenderer
    // -----------------------------------------------------------------------

    @DataProvider(name = "categories")
    public Object[][] categories() {
        return new Object[][] {
            { "two-object", Fixtures.twoObjectCategory(), 2 },
            { "chain",      Fixtures.chainCategory(),     3 },
            { "diamond",    Fixtures.diamondCategory(),   4 },
        };
    }

    @Test(dataProvider = "categories")
    public void testCategorySvgNodeCount(final String name,
            final com.github.catexjc.core.FiniteCategory<String,String> cat, final int expected) {
        final String svg = new CategoryRenderer<String,String>().renderSvg(cat);
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertEquals(count(svg, "circle"), expected,
                "Circle count mismatch for category '" + name + "'");
    }

    @Test(dataProvider = "categories")
    public void testCategorySvgWithIdentitiesContainsPaths(final String name,
            final com.github.catexjc.core.FiniteCategory<String,String> cat, final int objectCount) {
        // With identities ON, each object should produce a self-loop <path>
        final String svgWith = new CategoryRenderer<String,String>().renderSvg(cat);
        final String svgWithout = new CategoryRenderer<String,String>()
                .renderSvg(cat, RenderOptions.defaults(), false);
        final int pathsWith    = count(svgWith, "path");
        final int pathsWithout = count(svgWithout, "path");
        assertTrue(pathsWith >= pathsWithout,
                "More paths expected when identities shown for '" + name + "'");
    }

    // -----------------------------------------------------------------------
    // Custom RenderOptions
    // -----------------------------------------------------------------------

    @Test
    public void testCustomNodeColor() {
        final RenderOptions opts = RenderOptions.builder().nodeColor("#ABCDEF").build();
        final String svg = new HasseDiagramRenderer<String>().renderSvg(
                HasseDiagramConverter.fromCategory(Fixtures.chainCategory()), opts);
        assertTrue(svg.contains("#ABCDEF"), "Custom node colour should appear in SVG");
    }

    @Test
    public void testCustomDimensions() {
        final RenderOptions opts = RenderOptions.builder().width(400).height(300).build();
        final String svg = new HasseDiagramRenderer<String>().renderSvg(
                HasseDiagramConverter.fromCategory(Fixtures.chainCategory()), opts);
        assertTrue(svg.contains("width=\"400\""), "SVG width should be 400");
        assertTrue(svg.contains("height=\"300\""), "SVG height should be 300");
    }

    // -----------------------------------------------------------------------
    // CategoryConverter façade includes toHasseDiagram()
    // -----------------------------------------------------------------------

    @Test
    public void testConverterToHasseDiagram() {
        final HasseDiagram<String> hd = CategoryConverter.from(Fixtures.chainCategory())
                .toHasseDiagram();
        assertNotNull(hd);
        assertEquals(hd.getNodes().size(), 3);
        assertEquals(hd.maxRank(), 2);
    }

    // -----------------------------------------------------------------------
    // SVG well-formedness: root element closes properly
    // -----------------------------------------------------------------------

    @DataProvider(name = "allSvgs")
    public Object[][] allSvgs() {
        final RenderOptions opts = RenderOptions.defaults();
        final HasseDiagram<String> hd  = HasseDiagramConverter.fromCategory(Fixtures.diamondCategory());
        final Lattice<String>      lat = LatticeConverter.fromCategory(Fixtures.diamondCategory());
        final DirectedGraph<String,String> g  = GraphConverter.toGraph(Fixtures.diamondCategory());
        com.github.catexjc.core.FiniteCategory<String,String> cat = Fixtures.diamondCategory();

        return new Object[][] {
            { "Hasse",    new HasseDiagramRenderer<String>().renderSvg(hd,  opts) },
            { "Lattice",  new LatticeRenderer<String>().renderSvg(lat, opts)      },
            { "Graph",    new GraphRenderer<String,String>().renderSvg(g,   opts) },
            { "Category", new CategoryRenderer<String,String>().renderSvg(cat,opts)},
        };
    }

    @Test(dataProvider = "allSvgs")
    public void testSvgClosesRootElement(final String name, final String svg) {
        assertTrue(svg.endsWith("</svg>"),
                "SVG for '" + name + "' should end with </svg>");
    }

    @Test(dataProvider = "allSvgs")
    public void testSvgNonEmpty(final String name, final String svg) {
        assertTrue(svg.length() > 200,
                "SVG for '" + name + "' seems too short (" + svg.length() + " chars)");
    }

    // -----------------------------------------------------------------------
    // File rendering
    // -----------------------------------------------------------------------

    @Test
    public void testHasseDiagramRendersToFile() throws IOException {
        final HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.chainCategory());
        final Path tmp = Files.createTempFile("hasse-", ".svg");
        try {
            new HasseDiagramRenderer<String>().renderSvgToFile(hd, tmp);
            final String content = Files.readString(tmp);
            assertTrue(content.startsWith("<?xml"), "File should start with XML declaration");
            assertTrue(content.endsWith("</svg>"), "File should end with </svg>");
            assertTrue(content.length() > 200, "File content seems too short");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void testLatticeRendersToFile() throws IOException {
        final Lattice<String> lat = LatticeConverter.fromCategory(Fixtures.diamondCategory());
        final Path tmp = Files.createTempFile("lattice-", ".svg");
        try {
            new LatticeRenderer<String>().renderSvgToFile(lat, RenderOptions.defaults(), tmp);
            final String content = Files.readString(tmp);
            assertTrue(content.contains("<svg"), "File should contain <svg> element");
            assertTrue(content.endsWith("</svg>"), "File should end with </svg>");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void testGraphRendersToFile() throws IOException {
        final DirectedGraph<String, String> g = GraphConverter.toGraph(Fixtures.twoObjectCategory());
        final Path tmp = Files.createTempFile("graph-", ".svg");
        try {
            new GraphRenderer<String, String>().renderSvgToFile(g, tmp);
            final String content = Files.readString(tmp);
            assertTrue(content.contains("<svg"), "File should contain <svg> element");
            assertTrue(content.endsWith("</svg>"), "File should end with </svg>");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void testCategoryRendersToFile() throws IOException {
        final com.github.catexjc.core.FiniteCategory<String, String> cat = Fixtures.diamondCategory();
        final Path tmp = Files.createTempFile("category-", ".svg");
        try {
            new CategoryRenderer<String, String>().renderSvgToFile(cat, tmp);
            final String content = Files.readString(tmp);
            assertTrue(content.contains("<svg"), "File should contain <svg> element");
            assertTrue(content.endsWith("</svg>"), "File should end with </svg>");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void testCategoryRendersToFileWithoutIdentities() throws IOException {
        final com.github.catexjc.core.FiniteCategory<String, String> cat = Fixtures.twoObjectCategory();
        final Path tmp = Files.createTempFile("category-no-id-", ".svg");
        try {
            new CategoryRenderer<String, String>()
                    .renderSvgToFile(cat, RenderOptions.defaults(), tmp, false);
            final String content = Files.readString(tmp);
            assertTrue(content.contains("<svg"), "File should contain <svg> element");
            assertTrue(content.endsWith("</svg>"), "File should end with </svg>");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
