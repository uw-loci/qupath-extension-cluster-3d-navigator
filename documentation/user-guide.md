# Cluster 3D Navigator -- User Guide

Cluster 3D Navigator shows your already-clustered cells as an interactive point cloud --
one point per detection, colored by its class -- in a rotatable **3D** view or a flat
**2D** scatter, and lets you click a point to select and center that cell in the QuPath
viewer.

<details open>
<summary><b>Getting started</b> (read this first)</summary>

**What you need before you open it.** The navigator reads existing detections; it does not
cluster for you. Prerequisites:

- Detections that carry a **PathClass** (the group/color), and
- **At least 2 numeric measurement columns** for the flat 2D view (X / Y), or **3** for the
  rotatable 3D view (X / Y / Z).

**How to get those from any clustering tool.** Any tool that classifies detections and writes
numeric measurements qualifies -- QP-CAT (UMAP/PCA/tSNE + cluster classes), an InstanSeg or
Cellpose segmentation plus a downstream cluster step, or a hand-rolled Groovy script that
sets a PathClass and adds measurements. If you clustered in 2D (a 2D UMAP, etc.), switch the
**View** toggle to **2D** and the tool plots a flat X / Y scatter -- no need to invent a third
axis. (Auto-computing an embedding is not in this version -- see *Advanced features*.)

**First run.** Open the navigator, confirm the axes, and see the cloud. There is no
environment build and no download -- it is pure Java and starts immediately.

</details>

<details>
<summary><b>Common tasks</b></summary>

- **Open the navigator.** `Extensions > Cluster 3D Navigator > Open 3D navigator...`. It opens
  on the current image's detections by default and follows the active image -- switch images
  in QuPath and the cloud re-reads (in "Current image" mode).
- **Choose 2D or 3D.** The **View** toggle at the top switches between a rotatable **3D** cloud
  (needs a 3-component embedding) and a flat top-down **2D** scatter (needs two axes). The tool
  starts in your last-used mode; if the data has only two numeric measurements the 3D option is
  disabled and it opens in 2D automatically.
  - **Use a real 2D embedding for the 2D view.** For 2D, plot a genuine 2D dimensionality
    reduction (a 2D UMAP / t-SNE computed for two components). Do **not** plot two axes of a 3D
    UMAP: a 2D UMAP is optimized for two dimensions and looks different from any 2-axis slice of
    a 3D one. If you switch to 2D while plotting two components of a 3D embedding, an on-screen
    warning says so.
- **Pick or auto-detect the axes.** The tool scans measurement names and preselects a recognized
  embedding (UMAP1/2[/3], PCA1/2[/3], PC1/2[/3], tSNE1/2[/3]); a green "(auto-detected: ...)"
  tag tells you it guessed. Otherwise choose the axes from the dropdowns of every numeric
  measurement (Z is hidden in 2D), or click **Change axes...** for a focused picker. Your choice
  is remembered per project.
- **Rotate / zoom / pan.** In 3D, left-drag to rotate the cloud; in 2D there is nothing to
  rotate, so left-drag pans. Scroll to zoom toward the cursor; middle-drag OR **Shift+left-drag**
  also pans (the Shift fallback works on laptops and trackpads with no middle button). **Reset
  view** returns to a fit-all view.
- **Click a point to jump to the cell.** A single click selects that cell in QuPath, centers
  the viewer on it, and loads its crop into the **Cell preview** panel; if the cell lives in a
  different project image, that image opens first.
- **Hover to inspect.** Hovering a point shows a tooltip with the class and the RAW measurement
  values under the real axis names. Turn on **Preview crop on hover** (Display options) to also
  load the crop thumbnail while hovering; by default the crop loads on click.
- **Refresh the crop after changing display settings.** The preview crop is rendered with the
  viewer's channel visibility and brightness/contrast at the moment you clicked. After you change
  those in the viewer, click **Update from viewer** (under the Cell preview) to re-render the crop
  with the current settings. The point cloud itself is unaffected -- points are colored by class,
  not by display settings. ("Update from viewer" also refreshes any in-cloud cell images.)
- **See the actual cells as you zoom in (VEST-style).** Turn on **Show cell images when zoomed
  in** (Display options). Zoomed out, the cloud stays fast points; as you zoom in, cells are drawn
  as their real crop images at their 3D positions (with a thin class-colored border). Small clouds
  (up to ~2500 cells) render the whole cloud as cell images when zoomed in; larger clouds show the
  nearest ~1200 as images with the rest as points, to stay responsive at tens of thousands of
  cells. Crops load on demand, so the first zoom-in briefly shows points before the images fill in.
  Clicking an image still selects and centers that cell, exactly like clicking a point.
- **Show / hide classes.** The legend lists each class with its color, count, and a checkbox.
  Untick a class to hide those points (useful for isolating a rare population). **All** / **None**
  flip every class at once. The counter reads "Points: shown / total"; if some cells lack a value
  on a chosen axis they are counted as omitted (not silently dropped) and a note explains it.
- **Current image vs a subset of project images.** Switch the mode toggle to **Project
  images...** and a picker opens so you choose *which* project images to include -- filter by
  name or by a metadata key/value, then check the images you want (Select all / Select none act
  on the filtered rows). Reopen the picker any time with the **Select images...** button next to
  the mode toggle. The chosen subset is read off the UI thread with a busy indicator ("Reading
  image k of N...", where N is the number you selected); wait for it to finish. If you cancel the
  picker the mode reverts to Current image; if you confirm with nothing checked the view shows
  "No images selected".

</details>

<details>
<summary><b>Advanced features</b></summary>

**Display options** (a collapsed panel in the top strip) hold refinements a first-timer never
needs: a **Background** color picker (with an "Auto (theme)" checkbox that matches the QuPath
light/dark theme), crop scale, point size, "Shade points by depth", "Preview crop on hover",
"Show axis tripod", **"Show detection outlines"**, and "Representative cells per cluster". These
persist between sessions. (The **"Show cell images"** toggle itself lives in the top control
strip, grouped with the cell-limit / seed / points controls.)

**See the segmentation on each cell.** Turn on **Show detection outlines** (Display options) to
draw each cell's detection boundary (its ROI) on top of its crop, in the cell's cluster color --
both in the in-cloud cell images and in the **Cell preview** panel. This is the quickest way to
tell whether a cluster is real or an artifact of **segmentation errors** (merged, clipped, or
over-split cells): if a cluster's crops show bad outlines, the cluster is a segmentation problem,
not biology. The outline is crisp in the Cell preview and in zoomed-in thumbnails; it fades to
invisible on the tiniest far-out thumbnails (where the class-colored border still conveys class).
Toggling it re-renders the crops.

**Planned / not yet available** (none of these ship in this version):

- Optional hardware-accelerated (JavaFX-3D) rendering for nicer depth/lighting on machines
  with a supported GPU. The current Canvas renderer already works everywhere, including
  remote/headless/WSL; the hardware path is a future add-on, not a replacement.
- Compute a fresh 3D embedding when cells lack 3D coordinates.
- Export to VEST (a one-way, browser-viewable bundle for sharing). This extension's
  click-to-explore idea is inspired by **VEST** (scads/vest, MIT license), a browser-based 3D
  embedding-travel tool; we mirror the concept natively in QuPath and do not embed VEST itself
  (a browser cannot drive the QuPath viewer).
- Import from CSV (`id,x,y,z,label`).
- Lasso / box select in 3D to select many cells at once.
- Image billboards in 3D and save/restore camera viewpoints.

Do not rely on the planned items in this version.

</details>

<details>
<summary><b>Settings and preferences</b></summary>

Persisted between sessions:

- **Window size and position** -- the navigator remembers its last geometry.
- **Mode** -- current-image vs project-images.
- **View (2D / 3D)** -- the last dimensionality you chose. On data with only two numeric
  measurements the tool always opens in 2D (3D is impossible) regardless of this setting.
- **Last axis choice (per project)** -- the X / Y / Z measurements you last used; the picker
  preselects them next time (auto-detection still runs when there is nothing remembered).
- **Background** -- the 3D view background color. Defaults to **Auto (theme)** (matches the
  QuPath light/dark theme); uncheck Auto to pick a custom color. Point, text, and tripod colors
  follow the background's luminance so labels stay readable on any color.
- **Crop scale** -- preview crop size as a multiple of the cell's bounding box (default 3.0).
- **Point size** -- on-screen size of each plotted point (default 2.0).
- **Shade points by depth** -- depth-cue shading (default on).
- **Preview crop on hover** -- load a thumbnail on hover, not only on click (default off).
- **Show axis tripod** -- the corner X/Y/Z direction marker (default on).
- **Show detection outlines** -- draw each cell's segmentation boundary (its detection ROI) on
  its crop, in the cell's cluster color, in both the in-cloud cell images and the Cell preview
  (default off). Helps spot clusters caused by segmentation errors.
- **Cell limit per image** (top strip) -- caps how many cells per image are loaded (0 = no
  limit). A performance option for slower computers: it improves responsiveness but limits the
  data shown. Cells are chosen so every cluster stays represented (each cluster keeps at least its
  representative cells; the rest of the budget is filled at random using the **Seed**). The "?"
  button next to it explains the selection; the Points counter shows "(limited)" when active.
- **Seed** (top strip) -- the random seed used to choose which cells are shown when a cell limit
  is set; change it to resample (default 42).
- **Show cell images** (top strip, after Change axes...) -- draw the front-most cells as their
  crop images at their 3D positions once you zoom in far enough (default off; crops load on demand,
  bounded per frame). Crops are rendered with the current viewer's brightness/contrast/channel
  settings for every image (when its channel count matches), and crops nearest the cursor load
  first. While images are still filling in, an **"Image still populating"** banner shows at the top.
- **Representative cells per cluster** -- how many cells per cluster show as images even when
  zoomed out (0-5, default 3; 0 = only when zoomed in). Requires "Show cell images".
  The cells are chosen to spread across each cluster (its most central cell plus the most distinct
  ones), and clusters hidden behind nearer ones are suppressed, so far out you see a couple of
  representative images per visible cluster.

Not persisted (by design): the camera viewpoint (always opens at fit-all tilt), the
hidden-class set, and the selected project-image subset (remembered for the current session
only -- reopen the picker with **Select images...** to change it).

</details>

<details>
<summary><b>Troubleshooting</b></summary>

- **Empty / blank cloud after opening.** Usually the current image has no detections, or the
  chosen axes have no numeric values for these cells. Confirm detections exist and are
  classified, and that the axis dropdowns point at real numeric columns.
- **"Need at least 2 numeric measurements..."** The detections carry fewer than 2 numeric
  measurement columns (2 are needed for a 2D view, 3 for 3D). Cluster or embed upstream (QP-CAT
  UMAP/PCA, or a script that adds numeric measurements), then reopen.
- **"No detections in this image..."** The image (or whole project, in project-wide mode) has
  no detection objects. Detect and classify cells first.
- **Clicking a point jumps to the wrong cell.** Usually a stale hierarchy, or a point in a
  different project image that had to be opened first. Reopen the navigator to rebuild the
  point-to-cell map, and allow the target image a moment to load before the viewer centers.
- **Project-wide mode is slow.** Reading detections across every image is heavy on large
  projects. Wait for the busy indicator; prefer current-image mode for quick checks.
- **Platforms.** This build is verified on Linux only. Windows is a claimed target still owed
  real verification; macOS is not verified.

Where to file issues: GitHub. Include QuPath version, OS, cell count, and which measurements
you plotted.

</details>
