# Cluster 3D Navigator -- Developer Guide

Pure-Java QuPath 0.7 extension. No Appose, no Python. The 3D viewer lives in the sibling
Apache-2.0 library `cluster3d-core`; this repo is a thin GPL shell.

<details open>
<summary><b>Building from source</b></summary>

- Requires **JDK 21** (pin `-Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64` if the
  default `java` is newer).
- **Build order (core first):** the extension depends on `cluster3d-core` from Maven Local, so
  publish it before building the navigator:

  ```bash
  cd ../cluster3d-core
  ./gradlew -Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64 publishToMavenLocal test
  cd ../qupath-extension-cluster-3d-navigator
  ./gradlew -Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64 shadowJar test
  ```

  Output: `build/libs/qupath-extension-cluster-3d-navigator-0.1.0-all.jar` (shades core).
- Drop the jar into `~/QuPath/v0.7/extensions/` and restart QuPath to test (extensions do not
  hot-load).
- The navigator depends on `io.github.uw-loci:cluster3d-core:0.1.0` as a non-transitive
  `implementation` and shades it into the `-all.jar`. QuPath + JavaFX are host-provided (not
  bundled). The unit tests live in `cluster3d-core`.

</details>

<details>
<summary><b>Architecture overview</b></summary>

The extension is a thin **GPL** shell over the **Apache-2.0** `cluster3d-core` library.

```
qupath-extension-cluster-3d-navigator (GPL-3.0 shell)
  qupath.ext.cluster3dnav
  |- Cluster3DNavigatorExtension   QuPathExtension; registers the menu action.
  |- ui/Cluster3DNavigatorWindow   Singleton non-modal Stage; wraps the core pane, binds its
                                   title, persists window geometry, disposes on hide.

cluster3d-core (Apache-2.0 library, package qupath.ext.cluster3d)
  |- ui/
  |   |- Cluster3DNavigatorPane   Reusable BorderPane: all controls + follow-active-image +
  |   |                           reload/read logic + legend + preview + LOD wiring. Host-agnostic
  |   |                           (Stage OR Tab). API: ctor(QuPathGUI), titleProperty(),
  |   |                           initialLoad(), reload(), dispose().
  |   |- PointCloudView           Canvas panel: project -> depth-sort -> paint; gestures; PickGrid
  |   |                           each repaint; theme-aware background; in-cloud LOD thumbnails.
  |   |- AxisPickerDialog         WINDOW_MODAL "Choose axes" popup (3 combos + remember).
  |   |- ClassLegend              Per-class rows (checkbox + swatch + name + count) + All/None.
  |   |- ProjectImageSelector     Project-image subset picker (name + metadata filter, checkboxes).
  |   |- ThemeUtils               Light/dark detection + AA-contrast accent colors.
  |- render/Camera3D, render/PickGrid   PURE math (projection; bucket-grid pick).
  |- model/CellRef, model/PointCloudData
  |- io/DetectionReader (readImage/readEntries; pure numeric-filter/union/buildPointCloud),
  |     io/AxisAutoDetect (PURE)
  |- service/ViewerNavigator, service/CellCropService
  |- prefs/Cluster3DNavPreferences   PathPrefs namespace (geometry, mode, per-project axes, display).
```

Some core classes (ViewerNavigator, CellCropService, CellRef, ProjectImageSelector, and the
Canvas idioms of PointCloudView) are Apache-2.0 adaptations of QP-CAT, credited in the core
source headers and `cluster3d-core/NOTICE`.

- **Renderer:** a Canvas 2D projection with a hand-rolled camera. This is the primary and only
  renderer in v1 -- JavaFX-3D (`TriangleMesh`) renders blank where
  `Platform.isSupported(SCENE3D)==false` (WSL/CI/remote/GPU-less), so it is deferred, not shipped.
- **Picking:** a uniform screen-space bucket grid rebuilt each redraw; hit-test scans the
  pointer's bucket and neighbors -> effectively O(1). Ties resolve to the front-most (min-depth)
  point. Pick radius is an internal constant (7 px).
- **In-cloud cell images (VEST-style LOD):** optional (`Show cell images when zoomed in`). Each
  billboard is the crop drawn axis-aligned at the cell's projected `(sx, sy)` -- it always faces
  the camera by construction. `PointCloudView.planLod` (pure, unit-tested) is the footprint gate
  (thumbnails activate only when zoomed in enough). **Selection is screen-space occlusion by
  overlap:** `selectVisibleThumbnails` (pure, unit-tested) runs greedy nearest-first non-max
  suppression -- a candidate is drawn only if it overlaps every already-accepted (nearer) thumbnail
  by <= 10% of its area (front-most wins); occluded cells stay points and their crops are never
  loaded, bounding thumbnails to ~`visibleArea / (0.9*thumb)^2` (a few hundred), independent of
  cluster size. **Per-cluster representatives:** each class also contributes up to K (0-5, default
  3) "representative" cells -- computed on `setData` via `farthestPointSample` (pure, unit-tested:
  class medoid then greedy farthest-point spread) -- that are thumbnail candidates at ANY zoom
  (they bypass the footprint gate; `isThumbnailCandidate` = rep OR gated), then flow through the
  same occlusion + culling, so a couple of reps per visible cluster show even zoomed out. Off-screen
  cells are excluded (`isOnScreenCandidate`, pure). The
  draw loop re-projects + re-sorts only when the projection is dirty (camera/data/visibility
  change) using reused primitive `int[]`/`long[]` buffers (no per-frame boxed `Integer[]`);
  crop-load completions coalesce into at most one redraw per FX pulse. Crops load off-thread into a
  bounded LRU cache (400) with per-frame (64) + global in-flight (96) caps, dedup, and a generation
  counter that discards stale loads on a dataset/display change. Picking is unchanged (it keys off
  projected positions), so clicking an image navigates like clicking a point.

</details>

<details>
<summary><b>The generic input contract</b> (what it reads)</summary>

- **Cells:** `getDetectionObjects()` for the current image; project mode iterates the
  user-selected `ProjectImageEntry` subset (`DetectionReader.readEntries`) and reads each
  hierarchy. One detection = one point.
- **Group / color:** `detection.getPathClass()` (any class); color from `PathClass.getColor()`,
  with a neutral gray fallback for null/unclassified.
- **Axes:** the union of numeric measurement names (a name is numeric if any cell has a finite
  value for it); auto-detect a UMAP/PCA/PC/tSNE triple by case-insensitive name match, else the
  user picks any three. Each axis is normalized independently to [-1, 1] for display.
- **Back-reference:** a `CellRef[]` index-aligned with the point arrays (imageId, name, centroid
  X/Y in full-res px, bbox half-size) drives navigation and cropping.

This uses only standard QuPath concepts, so QP-CAT, InstanSeg, Cellpose add-ons, or a Groovy
script all work equally.

</details>

<details>
<summary><b>Scripting API</b></summary>

**None in v1.** The extension is dialog-driven only; there is no `*Scripts.java` public API and
no runnable Workflow step in this version.

</details>

<details>
<summary><b>Testing</b></summary>

Unit tests cover the pure logic only (headless-safe): `Camera3D` projection determinism,
fit-all, reset, and pitch clamp; `PickGrid` nearest / front-most-on-tie / empty-bucket miss;
`AxisAutoDetect` positives (separators, case, family preference) and negatives; `DetectionReader`
numeric-filter / measurement-union / `buildPointCloud` / `normalize`; and
`CellCropService.computeCropWindow`.

Live render, gesture disambiguation, pick-under-cursor, hover preview, follow-active-image,
dark-mode appearance, HiDPI pointer mapping, and project-wide read timing are NOT covered by
headless tests -- they need a WSL GUI smoke run and Windows verification.

</details>

<details>
<summary><b>Contributing / releasing</b></summary>

- Java formatting via Spotless (palantirJavaFormat) on `check`; ASCII-only enforced by the
  `checkAsciiOnly` task.
- Release: add `.github/workflows/notify-catalog.yml` targeting `qupath-catalog-mikenelson`. A
  catalog bump must PREPEND the new release and keep prior entries (monorepo catalog policy).

</details>
