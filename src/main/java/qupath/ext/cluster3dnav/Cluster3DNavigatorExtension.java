/*
 * Copyright 2026 Mike Nelson and contributors.
 *
 * This file is part of the Cluster 3D Navigator QuPath extension, which is
 * licensed under the GNU General Public License v3.0. See the LICENSE file at
 * the repository root for the full license text.
 */
package qupath.ext.cluster3dnav;

import javafx.application.Platform;
import javafx.scene.control.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.cluster3d.prefs.Cluster3DNavPreferences;
import qupath.ext.cluster3dnav.ui.Cluster3DNavigatorWindow;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;

/**
 * Cluster 3D Navigator: shows already-clustered cells as an interactive 3D point
 * cloud (one point per detection, colored by PathClass) and lets you click a
 * point to select and center that cell in the QuPath viewer.
 *
 * <p>Pure Java (no Appose / Python). Registers a single menu action
 * {@code Extensions > Cluster 3D Navigator > Open 3D navigator...} that shows a
 * singleton non-modal navigator window.</p>
 */
public class Cluster3DNavigatorExtension implements QuPathExtension {

    private static final Logger logger = LoggerFactory.getLogger(Cluster3DNavigatorExtension.class);

    private static final String EXTENSION_NAME = "Cluster 3D Navigator";
    private static final String EXTENSION_DESCRIPTION =
            "Interactive in-QuPath 3D point cloud of clustered cells; click a point to select "
                    + "and center that cell in the viewer. Generic across any clustering tool.";
    private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.7.0");

    private boolean installed = false;

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    @Override
    public String getDescription() {
        return EXTENSION_DESCRIPTION;
    }

    @Override
    public Version getQuPathVersion() {
        return EXTENSION_QUPATH_VERSION;
    }

    @Override
    public void installExtension(QuPathGUI qupath) {
        if (installed) {
            logger.debug("Cluster 3D Navigator extension already installed; skipping");
            return;
        }
        installed = true;
        logger.info("Installing extension: {}", EXTENSION_NAME);

        Cluster3DNavPreferences.installPreferences();

        Platform.runLater(() -> addMenuItems(qupath));
    }

    private void addMenuItems(QuPathGUI qupath) {
        var menu = qupath.getMenu("Extensions>" + EXTENSION_NAME, true);
        MenuItem openItem = new MenuItem("Open 3D navigator...");
        openItem.setOnAction(e -> {
            try {
                Cluster3DNavigatorWindow.showFor(qupath);
            } catch (Exception ex) {
                logger.error("Failed to open Cluster 3D Navigator", ex);
                Dialogs.showErrorMessage(EXTENSION_NAME, "Failed to open navigator: " + ex.getMessage());
            }
        });
        menu.getItems().add(openItem);
        logger.info("Menu items added for extension: {}", EXTENSION_NAME);
    }
}
