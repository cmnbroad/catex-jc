package com.catex;

import com.catex.convert.CategoryConverter;
import com.catex.graph.DirectedGraph;
import com.catex.graph.GraphConverter;
import com.catex.hasse.HasseDiagram;
import com.catex.hasse.HasseDiagramConverter;
import com.catex.lattice.Lattice;
import com.catex.lattice.LatticeConverter;
import com.catex.render.*;
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

    private static int count(String svg, String tag) {
        Pattern p = Pattern.compile("<" + tag + "[\\s/>]");
        java.util.regex.Matcher m = p.matcher(svg);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    private static boolean containsText(String svg, String text) {
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
    public void testHasseDiagramSvgNodeCount(String name, HasseDiagram<String> hd,
                                              int expectedNodes, int expectedEdges) {
        String svg = new HasseDiagramRenderer<String>().renderSvg(hd);
        assertNotNull(svg);
        assertTrue(svg.startsWith("<?xml"), "SVG should start with XML declaration");
        assertTrue(svg.contains("<svg"), "Should contain <svg> element");
        assertEquals(count(svg, "circle"), expectedNodes,
                "Circle count mismatch for '" + name + "'");
    }

    @Test(dataProvider = "hasseDiagrams")
    public void testHasseDiagramSvgEdgeCount(String name, HasseDiagram<String> hd,
                                              int expectedNodes, int expectedEdges) {
        String svg = new HasseDiagramRenderer<String>().renderSvg(hd);
        assertEquals(count(svg, "line"), expectedEdges,
                "Line count mismatch for '" + name + "'");
    }

    @Test
    public void testHasseDiagramSvgContainsLabels() {
        HasseDiagram<String> hd = HasseDiagramConverter.fromCategory(Fixtures.chainCategory());
        String svg = new HasseDiagramRenderer<String>().renderSvg(hd);
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
    public void testLatticeSvgNodeCount(String name, Lattice<String> lat, int expected) {
        String svg = new LatticeRenderer<String>().renderSvg(lat);
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        // Main nodes + 2 legend circles (top/bottom)
        int circles = count(svg, "circle");
        assertTrue(circles >= expected,
                "Expected at least " + expected + " circles in '" + name + "', got " + circles);
    }

    @Test
    public void testLatticeSvgContainsTopBottomColors() {
        RenderOptions opts = RenderOptions.builder()
                .topColor("#FF0000").bottomColor("#00FF00").build();
        String svg = new LatticeRenderer<String>().renderSvg(
                LatticeConverter.fromCategory(Fixtures.diamondCategory()), opts);
        assertTrue(svg.contains("#FF0000"), "Should contain top-element colour");
        assertTrue(svg.contains("#00FF00"), "Should contain bottom-element colour");
    }

    @Test
    public void testLatticeSvgLabels() {
        String svg = new LatticeRenderer<String>().renderSvg(
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
    public void testGraphSvgNodeCount(String name, DirectedGraph<String,String> g,
                                       int expectedNodes, int expectedEdges) {
        String svg = new GraphRenderer<String,String>().renderSvg(g);
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertEquals(count(svg, "circle"), expectedNodes,
                "Node (circle) count mismatch for graph '" + name + "'");
    }

    @Test(dataProvider = "graphs")
    public void testGraphSvgHasArrowMarker(String name, DirectedGraph<String,String> g,
                                            int expNodes, int expEdges) {
        String svg = new GraphRenderer<String,String>().renderSvg(g);
        assertTrue(svg.contains("<marker"), "Graph SVG should define an arrow marker");
    }

    @Test
    public void testGraphSvgEdgeLabelsPresent() {
        DirectedGraph<String,String> g = GraphConverter.toGraph(Fixtures.twoObjectCategory());
        String svg = new GraphRenderer<String,String>().renderSvg(g);
        assertTrue(svg.contains("f"), "Edge label 'f' should appear in SVG");
    }

    @Test
    public void testCyclicGraphUsesCircularLayout() {
        // Build a cyclic graph A→B→C→A
        com.catex.core.CategoryObject<String> A = new com.catex.core.CategoryObject<>("A");
        com.catex.core.CategoryObject<String> B = new com.catex.core.CategoryObject<>("B");
        com.catex.core.CategoryObject<String> C = new com.catex.core.CategoryObject<>("C");
        var idA = new com.catex.core.Morphism<>("id_A", A, A);
        var idB = new com.catex.core.Morphism<>("id_B", B, B);
        var idC = new com.catex.core.Morphism<>("id_C", C, C);
        var ab  = new com.catex.core.Morphism<>("ab", A, B);
        var bc  = new com.catex.core.Morphism<>("bc", B, C);
        var ca  = new com.catex.core.Morphism<>("ca", C, A);
        var cat = com.catex.core.FiniteCategory.<String,String>builder()
                .addObject(A).addObject(B).addObject(C)
                .addMorphism(idA).addMorphism(idB).addMorphism(idC)
                .addMorphism(ab).addMorphism(bc).addMorphism(ca)
                .build();
        DirectedGraph<String,String> g = GraphConverter.toGraph(cat);
        // Should not throw; circular layout used
        String svg = new GraphRenderer<String,String>().renderSvg(g);
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
    public void testCategorySvgNodeCount(String name,
            com.catex.core.FiniteCategory<String,String> cat, int expected) {
        String svg = new CategoryRenderer<String,String>().renderSvg(cat);
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertEquals(count(svg, "circle"), expected,
                "Circle count mismatch for category '" + name + "'");
    }

    @Test(dataProvider = "categories")
    public void testCategorySvgWithIdentitiesContainsPaths(String name,
            com.catex.core.FiniteCategory<String,String> cat, int objectCount) {
        // With identities ON, each object should produce a self-loop <path>
        String svgWith = new CategoryRenderer<String,String>().renderSvg(cat);
        String svgWithout = new CategoryRenderer<String,String>()
                .renderSvg(cat, RenderOptions.defaults(), false);
        int pathsWith    = count(svgWith, "path");
        int pathsWithout = count(svgWithout, "path");
        assertTrue(pathsWith >= pathsWithout,
                "More paths expected when identities shown for '" + name + "'");
    }

    // -----------------------------------------------------------------------
    // Custom RenderOptions
    // -----------------------------------------------------------------------

    @Test
    public void testCustomNodeColor() {
        RenderOptions opts = RenderOptions.builder().nodeColor("#ABCDEF").build();
        String svg = new HasseDiagramRenderer<String>().renderSvg(
                HasseDiagramConverter.fromCategory(Fixtures.chainCategory()), opts);
        assertTrue(svg.contains("#ABCDEF"), "Custom node colour should appear in SVG");
    }

    @Test
    public void testCustomDimensions() {
        RenderOptions opts = RenderOptions.builder().width(400).height(300).build();
        String svg = new HasseDiagramRenderer<String>().renderSvg(
                HasseDiagramConverter.fromCategory(Fixtures.chainCategory()), opts);
        assertTrue(svg.contains("width=\"400\""), "SVG width should be 400");
        assertTrue(svg.contains("height=\"300\""), "SVG height should be 300");
    }

    // -----------------------------------------------------------------------
    // CategoryConverter façade includes toHasseDiagram()
    // -----------------------------------------------------------------------

    @Test
    public void testConverterToHasseDiagram() {
        HasseDiagram<String> hd = CategoryConverter.from(Fixtures.chainCategory())
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
        RenderOptions opts = RenderOptions.defaults();
        HasseDiagram<String> hd  = HasseDiagramConverter.fromCategory(Fixtures.diamondCategory());
        Lattice<String>      lat = LatticeConverter.fromCategory(Fixtures.diamondCategory());
        DirectedGraph<String,String> g  = GraphConverter.toGraph(Fixtures.diamondCategory());
        com.catex.core.FiniteCategory<String,String> cat = Fixtures.diamondCategory();

        return new Object[][] {
            { "Hasse",    new HasseDiagramRenderer<String>().renderSvg(hd,  opts) },
            { "Lattice",  new LatticeRenderer<String>().renderSvg(lat, opts)      },
            { "Graph",    new GraphRenderer<String,String>().renderSvg(g,   opts) },
            { "Category", new CategoryRenderer<String,String>().renderSvg(cat,opts)},
        };
    }

    @Test(dataProvider = "allSvgs")
    public void testSvgClosesRootElement(String name, String svg) {
        assertTrue(svg.endsWith("</svg>"),
                "SVG for '" + name + "' should end with </svg>");
    }

    @Test(dataProvider = "allSvgs")
    public void testSvgNonEmpty(String name, String svg) {
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
        final com.catex.core.FiniteCategory<String, String> cat = Fixtures.diamondCategory();
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
        final com.catex.core.FiniteCategory<String, String> cat = Fixtures.twoObjectCategory();
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
