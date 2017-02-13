package org.mistfx.decoration.skin;

import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.mistfx.decoration.Decoration;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static org.mistfx.decoration.skin.DecorationController.FullscreenStyleClass.FULLSCREEN;
import static org.mistfx.decoration.skin.DecorationController.FullscreenStyleClass.UNFULLSCREEN;
import static org.mistfx.decoration.skin.DecorationController.MaximizeStyleClass.MAXIMIZE;
import static org.mistfx.decoration.skin.DecorationController.MaximizeStyleClass.RESTORE;

/**
 * Created on 2015/6/12.
 *
 * @author Misty
 */
/*
    TODO replace listeners and bindings by notification
    Never bind to decoration and skin because she skin will contain multiple DecorationControllers
 */
public class DecorationController implements Initializable {
    private final Logger logger = Logger.getLogger(getClass().getSimpleName());

    private final Decoration control;

    @FXML AnchorPane root;

    @FXML protected Button close;
    @FXML protected Button maximize;
    @FXML protected Button iconified;

    @FXML protected Parent dragbar;
    @FXML protected Button resize;
    @FXML protected Button fullscreen;
    @FXML protected Button menu;
    @FXML protected Label title;
    @FXML protected ContextMenu contextMenu;

    @FXML protected Region nEdge;
    @FXML protected Region sEdge;
    @FXML protected Region wEdge;
    @FXML protected Region eEdge;

    @FXML protected Region swEdge;
    @FXML protected Region seEdge;
    @FXML protected Region nwEdge;
    @FXML protected Region neEdge;

    private TranslateTransition fullscreenTransition;

    private ChangeListener<Boolean> maximizeListener = (observable, oldValue, newValue) -> {
        maximize.getStyleClass().removeAll(MAXIMIZE.styleClass, RESTORE.styleClass);
        maximize.getStyleClass().add(MaximizeStyleClass.of(newValue).styleClass);
    };

    private ChangeListener<Boolean> fullscreenListener = (observable, oldValue, newValue) -> {
        fullscreen.getStyleClass().removeAll(FULLSCREEN.styleClass, UNFULLSCREEN.styleClass);
        fullscreen.getStyleClass().add(FullscreenStyleClass.of(newValue).styleClass);

        fullscreenTransition.stop();
        fullscreenTransition.setToX(newValue ? 68 : 0);
        fullscreenTransition.play();
    };

    DecorationController(Decoration control) {
        this.control = control;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        title.textProperty().bind(control.titleProperty());

        fullscreenTransition = new TranslateTransition(Duration.millis(500), fullscreen);
        fullscreenTransition.setToX(66);
        /** close button */
        if (this.close != null) {
            this.close.addEventHandler(ActionEvent.ACTION,
                    event -> control.close());
        }
        /** maximize button */
        if (this.maximize != null) {
            this.maximize.addEventHandler(ActionEvent.ACTION,
                    event -> control.setMaximized(!control.isMaximized()));
            this.maximize.visibleProperty().bind(control.fullScreenProperty().not());
            control.maximizedProperty().addListener(new WeakChangeListener<>(maximizeListener));
        }
        /** iconified button */
        if (this.iconified != null) {
            this.iconified.addEventHandler(ActionEvent.ACTION,
                    event -> control.setIconified(!control.isIconified()));
            this.iconified.visibleProperty().bind(control.fullScreenProperty().not());
        }
        /** fullscreen button */
        if (this.fullscreen != null) {
            this.fullscreen.addEventHandler(ActionEvent.ACTION,
                    event -> control.setFullScreen(!control.isFullScreen()));
            this.fullscreen.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {// TODO did I need ?
                if (control.isFullScreen()) {
                    this.fullscreen.setOpacity(1);
                }
            });
            this.fullscreen.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {// TODO did I need ?
                if (control.isFullScreen()) {
                    this.fullscreen.setOpacity(0.4);
                }
            });
            control.fullScreenProperty().addListener(new WeakChangeListener<>(fullscreenListener));
        }

        _initRoot();
        _initEdge();
    }

    /** root */
    private void _initRoot() {
    }

    /** edge */
    private Point2D dragPoint;
    private Bounds stageInfo;
    static boolean changeing;
    
    private void _initEdge() {
        EventHandler<MouseEvent> pressed = event -> {
            if (event.isPrimaryButtonDown() && isResizable()) {
                Stage stage = control.getStage();
                dragPoint = new Point2D(event.getScreenX(), event.getScreenY());
                stageInfo = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
                event.consume();
            }
        };
        Callback<Cursors, EventHandler<MouseEvent>> entered = cursor ->
                event -> {
                    Node source = (Node) event.getSource();
                    if (!isResizable()) {
                        source.setCursor(Cursor.DEFAULT);
                    } else {
                        source.setCursor(cursor.cursor);
                    }
                };
        Callback<Cursors, EventHandler<MouseEvent>> dragged = cursor ->
                event -> {
                    if (changeing || !isEventResizable(event)) {
                        return;
                    }
                    changeing = true;
                    cursor.resize(event, dragPoint, stageInfo, control.getStage());
                    event.consume();
                    changeing = false;
                };
        /** North */
        if (nEdge != null) {
            nEdge.addEventHandler(MouseEvent.MOUSE_ENTERED, entered.call(Cursors.N_RESIZE));
            nEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
            nEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragged.call(Cursors.N_RESIZE));
        } else {
            logger.warning("North resize area not exists!");
        }
        /** South */
        if (sEdge != null) {
            sEdge.addEventHandler(MouseEvent.MOUSE_ENTERED, entered.call(Cursors.S_RESIZE));
            sEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
            sEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragged.call(Cursors.S_RESIZE));
        } else {
            logger.warning("South resize area not exists!");
        }
        /** West */
        if (wEdge != null) {
            wEdge.addEventHandler(MouseEvent.MOUSE_ENTERED, entered.call(Cursors.W_RESIZE));
            wEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
            wEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragged.call(Cursors.W_RESIZE));
        } else {
            logger.warning("West resize area not exists!");
        }
        /** East */
        if (eEdge != null) {
            eEdge.addEventHandler(MouseEvent.MOUSE_ENTERED, entered.call(Cursors.E_RESIZE));
            eEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
            eEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragged.call(Cursors.E_RESIZE));
        } else {
            logger.warning("East resize area not exists!");
        }
        /** South west */
        if (swEdge != null) {
            swEdge.addEventHandler(MouseEvent.MOUSE_ENTERED, entered.call(Cursors.SW_RESIZE));
            swEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
            swEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragged.call(Cursors.SW_RESIZE));
        } else {
            logger.warning("South west resize area not exists!");
        }
        /** South east */
        if (seEdge != null) {
            seEdge.addEventHandler(MouseEvent.MOUSE_ENTERED, entered.call(Cursors.SE_RESIZE));
            seEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
            seEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragged.call(Cursors.SE_RESIZE));
        } else {
            logger.warning("South east resize area not exists!");
        }
        /** North west */
        if (nwEdge != null) {
            nwEdge.addEventHandler(MouseEvent.MOUSE_ENTERED, entered.call(Cursors.NW_RESIZE));
            nwEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
            nwEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragged.call(Cursors.NW_RESIZE));
        } else {
            logger.warning("North west resize area not exists!");
        }
        /** North east */
        if (neEdge != null) {
            neEdge.addEventHandler(MouseEvent.MOUSE_ENTERED, entered.call(Cursors.NE_RESIZE));
            neEdge.addEventHandler(MouseEvent.MOUSE_PRESSED, pressed);
            neEdge.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragged.call(Cursors.NE_RESIZE));
        } else {
            logger.warning("North west resize area not exists!");
        }
        /** Header */
        if (dragbar != null) {
            dragbar.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    if (isMaximizable()) {
                        control.setMaximized(!control.isMaximized());
                        event.consume();
                    }
                }
            });
            dragbar.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.isPrimaryButtonDown() && isMovable()) {
                    Stage stage = control.getStage();
                    dragPoint = new Point2D(event.getScreenX(), event.getScreenY());
                    stageInfo = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
                    event.consume();
                }
            });
            dragbar.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
                if (isEventMovable(event)) {
                    Stage stage = control.getStage();
                    final double x = dragPoint.getX();
                    final double y = dragPoint.getY();
                    final double dx = event.getScreenX() - x;
                    final double dy = event.getScreenY() - y;
                    if ((dx != 0 || dy != 0) && stageInfo.getMinX() == 0 && stageInfo.getMinY() == 0
                            && control.isMaximized()) {
                        // if moved in Maximized, then exit Maximized
                        Region source = (Region) event.getSource();
                        Point2D p1 = source.localToScene(0, 0, true);
                        control.setMaximized(false);
                        Point2D p2 = source.localToScene(0, 0, true);
                        // compute the shadow padding
                        Point2D padding = p2.subtract(p1);

                        double halfW = stage.getWidth() / 2;

                        double newX;
                        double newY = -padding.getY();
                        if (x < halfW) {
                            newX = -padding.getX();
                        } else if (x > stageInfo.getWidth() - halfW) {
                            newX = stageInfo.getWidth() - stage.getWidth() + padding.getX();
                        } else {
                            newX = x - halfW;
                        }
                        stageInfo = new BoundingBox(newX, newY, 0, 0);
                    }
                    Cursors.setStageX(stage, stageInfo.getMinX() + dx);
                    Cursors.setStageY(stage, stageInfo.getMinY() + dy);
                }
            });
        }
    }

    /** Is decoration control resizable */
    private boolean isResizable() {
        return control.isResizable() && !control.isMaximized() && !control.isFullScreen();
    }

    /** Does decoration control can resize by this event */
    private boolean isEventResizable(MouseEvent event) {
        return stageInfo != null && dragPoint != null &&
                event.isPrimaryButtonDown() && !event.isStillSincePress() && isResizable();
    }

    /** Is decoration control movable */
    private boolean isMovable() {
        return !control.isFullScreen();
    }

    /** Does decoration control can move by this event */
    private boolean isEventMovable(MouseEvent event) {
        return stageInfo != null && dragPoint != null &&
                event.isPrimaryButtonDown() && !event.isStillSincePress() && isMovable();
    }

    private boolean isMaximizable() {
        return control.isResizable() && !control.isFullScreen();
    }

    public enum MaximizeStyleClass {
        MAXIMIZE("decoration-button-maximize"), RESTORE("decoration-button-restore");

        final String styleClass;

        MaximizeStyleClass(String styleClass) {
            this.styleClass = styleClass;
        }

        static MaximizeStyleClass of(boolean isMax) {
            return isMax ? RESTORE : MAXIMIZE;
        }
    }

    public enum FullscreenStyleClass {
        FULLSCREEN("decoration-button-fullscreen"), UNFULLSCREEN("decoration-button-unfullscreen");
        final String styleClass;

        FullscreenStyleClass(String styleClass) {
            this.styleClass = styleClass;
        }

        static FullscreenStyleClass of(boolean isFull) {
            return isFull ? UNFULLSCREEN : FULLSCREEN;
        }
    }

    public enum Cursors {
        N_RESIZE(Cursor.N_RESIZE) {
            @Override
            void resize(MouseEvent event, Point2D dragPoint, Bounds stageInfo, Stage stage) {
                double dy = event.getScreenY() - dragPoint.getY();
                if (setStageHeight(stage, stageInfo.getHeight() - dy)) {
                    setStageY(stage, stageInfo.getMinY() + dy);
                }
            }
        },
        S_RESIZE(Cursor.S_RESIZE) {
            @Override
            void resize(MouseEvent event, Point2D dragPoint, Bounds stageInfo, Stage stage) {
                double dy = event.getScreenY() - dragPoint.getY();
                setStageHeight(stage, stageInfo.getHeight() + dy);
            }
        },
        W_RESIZE(Cursor.W_RESIZE) {
            @Override
            void resize(MouseEvent event, Point2D dragPoint, Bounds stageInfo, Stage stage) {
                double dx = event.getScreenX() - dragPoint.getX();
                if (setStageWidth(stage, stageInfo.getWidth() - dx)) {
                    setStageX(stage, stageInfo.getMinX() + dx);
                }
            }
        },
        E_RESIZE(Cursor.E_RESIZE) {
            @Override
            void resize(MouseEvent event, Point2D dragPoint, Bounds stageInfo, Stage stage) {
                double dx = event.getScreenX() - dragPoint.getX();
                setStageWidth(stage, stageInfo.getWidth() + dx);
            }
        },
        SW_RESIZE(Cursor.SW_RESIZE) {
            @Override
            void resize(MouseEvent event, Point2D dragPoint, Bounds stageInfo, Stage stage) {
                S_RESIZE.resize(event, dragPoint, stageInfo, stage);
                W_RESIZE.resize(event, dragPoint, stageInfo, stage);
            }
        },
        SE_RESIZE(Cursor.SE_RESIZE) {
            @Override
            void resize(MouseEvent event, Point2D dragPoint, Bounds stageInfo, Stage stage) {
                S_RESIZE.resize(event, dragPoint, stageInfo, stage);
                E_RESIZE.resize(event, dragPoint, stageInfo, stage);
            }
        },
        NW_RESIZE(Cursor.NW_RESIZE) {
            @Override
            void resize(MouseEvent event, Point2D dragPoint, Bounds stageInfo, Stage stage) {
                N_RESIZE.resize(event, dragPoint, stageInfo, stage);
                W_RESIZE.resize(event, dragPoint, stageInfo, stage);
            }
        },
        NE_RESIZE(Cursor.NE_RESIZE) {
            @Override
            void resize(MouseEvent event, Point2D dragPoint, Bounds stageInfo, Stage stage) {
                N_RESIZE.resize(event, dragPoint, stageInfo, stage);
                E_RESIZE.resize(event, dragPoint, stageInfo, stage);
            }
        };
        Cursor cursor;

        Cursors(Cursor cursor) {
            this.cursor = cursor;
        }

        abstract void resize(MouseEvent event, Point2D dragPoint, Bounds stageInfo, Stage stage);

        static void setStageX(Stage stage, double x) { // TODO
            stage.setX(x);
        }

        static void setStageY(Stage stage, double y) { // TODO
            stage.setY(y);
        }

        static boolean setStageWidth(Stage stage, double width) {
            if (width >= stage.getMinWidth() && width <= stage.getMaxWidth()) {
                stage.setWidth(width);
                return true;
            } else {
            	return false;
            }
        }

        static boolean setStageHeight(Stage stage, double height) {
            if (height >= stage.getMinHeight() && height <= stage.getMaxHeight()) {
                stage.setHeight(height);
                return true;
            } else {
            	return false;
            }
        }
    }
}
