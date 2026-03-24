# catex-jc

Experiments with category theory.

A Java library for rendering your domain-specific data structure as a (finite) category by implementing the
CategorySource interface. Then use the category-theoretic representation to gain insights about your data and
structures by reasoning about their properties; how they relate to mathematical structures such as graphs,
posets, or lattices; render and visualize them as SVG diagrams.

## Features

- Create a finite category with objects, morphisms, and a composition table
- Validate category laws (identity, associativity, unit laws)
- Convert categories to graphs, partial orders, lattices, and Hasse diagrams
- Render any structure to SVG with customizable layouts and styles

## Requirements

- Java 17+
- Gradle (provided via wrapper)

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```

## Usage

### Define a category

Implement `CategorySource` on your domain object and provide it as a `FiniteCategory`:

```java
var category = myDomain.toCategory();
category.validate(); // checks identity and associativity laws
```

### Convert to other structures

Use the fluent `CategoryConverter` facade:

```java
// To a directed graph
DirectedGraph<L> graph = CategoryConverter.from(category).toGraph();

// To a partial order (requires a preorder category)
PartialOrder<L> poset = CategoryConverter.from(category).toPoset();

// To a lattice (requires a preorder lattice category)
Lattice<L> lattice = CategoryConverter.from(category).toLattice();

// To a Hasse diagram
HasseDiagram<L> hasse = CategoryConverter.from(category).toHasseDiagram();
```

### Render to SVG

```java
var renderer = new HasseDiagramRenderer<>();
renderer.renderSvgToFile(hasse, Path.of("output.svg"));
```

Customize the output with `RenderOptions`:

```java
RenderOptions opts = RenderOptions.builder()
    .width(1000).height(700)
    .nodeColor("#4A90D9")
    .labelColor("#FFFFFF")
    .fontSize(14)
    .layerHeight(120)
    .build();

renderer.renderSvgToFile(hasse, Path.of("output.svg"), opts);
```

Renderers are available for all structures: `CategoryRenderer`, `GraphRenderer`, `LatticeRenderer`, `HasseDiagramRenderer`.

## Package Structure

| Package | Description |
|---|---|
| `com.github.catexjc.core` | `FiniteCategory`, `CategoryObject`, `Morphism`, `CategorySource` |
| `com.github.catexjc.convert` | `CategoryConverter` — unified conversion facade |
| `com.github.catexjc.graph` | `DirectedGraph`, `GraphConverter` |
| `com.github.catexjc.poset` | `PartialOrder`, `PosetConverter` |
| `com.github.catexjc.lattice` | `Lattice`, `LatticeConverter` |
| `com.github.catexjc.hasse` | `HasseDiagram`, `HasseDiagramConverter` |
| `com.github.catexjc.render` | Renderers, `RenderOptions`, SVG layout internals |

## Notes

- All data structures are immutable.
- Category law validation is lazy — call `validate()` explicitly.
- Conversions to poset/lattice/Hasse diagram require a *preorder* category (at most one morphism between any two objects). An `IllegalArgumentException` is thrown if the category does not meet the required properties.
