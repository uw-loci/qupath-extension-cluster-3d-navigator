/*
 * Copyright 2026 Mike Nelson and contributors.
 *
 * This file is part of the Cluster 3D Navigator QuPath extension, which is
 * licensed under the GNU General Public License v3.0. See the LICENSE file at
 * the repository root for the full license text.
 */
package qupath.ext.cluster3dnav.ui;

import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.cluster3d.prefs.Cluster3DNavPreferences;
import qupath.ext.cluster3d.ui.Cluster3DNavigatorPane;
import qupath.lib.gui.QuPathGUI;

/**
 * Thin GPL shell around the Apache-2.0 {@link Cluster3DNavigatorPane}: a singleton,
 * non-modal {@link Stage} that hosts the reusable core pane, binds its title, persists
 * window geometry, and disposes the pane on hide. All navigator UI + logic lives in the
 * {@code cluster3d-core} library; this class only owns the window.
 */
public final class Cluster3DNavigatorWindow {

    private static final Logger logger = LoggerFactory.getLogger(Cluster3DNavigatorWindow.class);

    private static Cluster3DNavigatorWindow instance;

    private final Stage stage;
    private final Cluster3DNavigatorPane pane;

    private Cluster3DNavigatorWindow(QuPathGUI qupath) {
        this.pane = new Cluster3DNavigatorPane(qupath);

        stage = new Stage();
        stage.initModality(Modality.NONE);
        if (qupath.getStage() != null) {
            stage.initOwner(qupath.getStage());
        }
        stage.titleProperty().bind(pane.titleProperty());
        stage.setScene(new Scene(pane));
        applyGeometryFromPrefs();
        stage.setOnHidden(e -> dispose());
    }

    /** Show the singleton window (create once, bring to front thereafter). */
    public static synchronized void showFor(QuPathGUI qupath) {
        boolean created = false;
        if (instance == null) {
            instance = new Cluster3DNavigatorWindow(qupath);
            created = true;
        }
        instance.stage.show();
        instance.stage.toFront();
        if (created) {
            // Run the first read (and the project-image picker) over the now-visible window.
            instance.pane.initialLoad();
        }
    }

    private void applyGeometryFromPrefs() {
        double x = Cluster3DNavPreferences.windowXProperty().get();
        double y = Cluster3DNavPreferences.windowYProperty().get();
        double w = Cluster3DNavPreferences.windowWProperty().get();
        double h = Cluster3DNavPreferences.windowHProperty().get();
        stage.setWidth(w > 0 ? w : Cluster3DNavPreferences.DEFAULT_WINDOW_W);
        stage.setHeight(h > 0 ? h : Cluster3DNavPreferences.DEFAULT_WINDOW_H);
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        if (x >= 0 && y >= 0) {
            stage.setX(x);
            stage.setY(y);
        }
        stage.xProperty()
                .addListener(
                        (o, a, b) -> Cluster3DNavPreferences.windowXProperty().set(b.doubleValue()));
        stage.yProperty()
                .addListener(
                        (o, a, b) -> Cluster3DNavPreferences.windowYProperty().set(b.doubleValue()));
        stage.widthProperty()
                .addListener(
                        (o, a, b) -> Cluster3DNavPreferences.windowWProperty().set(b.doubleValue()));
        stage.heightProperty()
                .addListener(
                        (o, a, b) -> Cluster3DNavPreferences.windowHProperty().set(b.doubleValue()));
    }

    private synchronized void dispose() {
        logger.info("Closing Cluster 3D Navigator window");
        pane.dispose();
        instance = null;
    }
}
