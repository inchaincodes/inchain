package org.mistfx.decoration;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.Effect;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Created on 2015/6/11.
 *
 * @author Misty
 */
public class Utils {
    private final static boolean DEBUG = true;
    private final static Logger LOGGER = Logger.getLogger(Utils.class.getName());
    private final static BoundsAccessor accessor = (bounds, tx, node) -> bounds;

    public static Screen getScreen(Stage stage) {
        return getScreens(stage).stream().findFirst().orElse(null);
    }

    public static ObservableList<Screen> getScreens(Stage stage) {
        return Screen.getScreensForRectangle(stage.getX(), stage.getY(),
                stage.getWidth(), stage.getHeight());
    }

    public static void applyToStage(Node node, Consumer<Stage> consumer) {
        Scene scene = node.getScene();
        if (scene != null) {
            Stage stage = (Stage) scene.getWindow();
            if (stage != null) {
                consumer.accept(stage);
            } else {
                throw new IllegalStateException(node + " is not inside a scene-graph");
            }
        }
    }

    public static Effect getEffect(Decoration decoration) {
        return decoration.isFocused() ? decoration.getFocusedEffect() : decoration.getUnfocusedEffect();
    }

    public static Insets getEffectPadding(Decoration decoration) {
        Effect focused = decoration.getFocusedEffect();
        Effect unfocused = decoration.getUnfocusedEffect();
        return Utils.max(Utils.getEffectPadding(focused), Utils.getEffectPadding(unfocused));
    }

    @SuppressWarnings("deprecation")
    public static Insets getEffectPadding(Effect effect) {
        if (effect == null) {
            return Insets.EMPTY;
        }
        BaseBounds bounds = new RectBounds();
        bounds = effect.impl_getBounds(bounds, BaseTransform.IDENTITY_TRANSFORM,
                null, accessor);
        return new Insets(
                Math.max(Math.abs(bounds.getMinX()), 0),
                Math.max(Math.abs(bounds.getMaxX()), 0),
                Math.max(Math.abs(bounds.getMaxY()), 0),
                Math.max(Math.abs(bounds.getMinX()), 0)
        );
    }

    public static Insets max(Insets i1, Insets i2) {
        if (i1 == null || i2 == null) {
            if (i1 != null) return i1;
            if (i2 != null) return i2;
            return Insets.EMPTY;
        }
        return new Insets(
                Math.max(i1.getTop(), i2.getTop()),
                Math.max(i1.getRight(), i2.getRight()),
                Math.max(i1.getBottom(), i2.getBottom()),
                Math.max(i1.getLeft(), i2.getLeft())
        );
    }

    public static Insets maxCentered(Insets i1, Insets i2) {
        double t = Math.max(i1.getTop(), i2.getTop()),
                b = Math.max(i1.getBottom(), i2.getBottom()),
                r = Math.max(i1.getRight(), i2.getRight()),
                l = Math.max(i1.getLeft(), i2.getLeft());
        double tb = Math.max(t, b);
        double lr = Math.max(l, r);
        return new Insets(tb, lr, tb, lr);
    }


    public static void resize(Node node, double width, double height) {
        if (node instanceof Rectangle) {
            resize((Rectangle) node, width, height);
        } else {
            if (!node.isResizable()) {
                if (DEBUG) {
                    LOGGER.warning("Try to resize witch is not resizable " + node);
                }
            }
            node.resize(width, height);
        }
    }

    public static void resize(Rectangle rect, double width, double height) {
        rect.setWidth(width);
        rect.setHeight(height);
    }

    public static void relocate(Node node, double x, double y) {
        node.setLayoutX(x);
        node.setLayoutY(y);
    }

    public static void resizeRelocate(final Node node, final double width, final double height,
                                      final double x, final double y) {
        resize(node, width, height);
        relocate(node, x, y);
    }

    public static class MutableBounds {
        double minX, minY, minZ;
        double maxX, maxY, maxZ;

        public double getMinX() {
            return minX;
        }

        public double getMinY() {
            return minY;
        }

        public double getMinZ() {
            return minZ;
        }

        public double getMaxX() {
            return maxX;
        }

        public double getMaxY() {
            return maxY;
        }

        public double getMaxZ() {
            return maxZ;
        }

        public double getWidth() {
            return getMaxX() - getMinX();
        }

        public double getHeight() {
            return getMaxY() - getMinY();
        }

        public double getDepth() {
            return getMaxZ() - getMinZ();
        }

        @Override
        public String toString() {
            return "BoundingBox ["
                    + "minX:" + getMinX()
                    + ", minY:" + getMinY()
                    + ", minZ:" + getMinZ()
                    + ", width:" + getWidth()
                    + ", height:" + getHeight()
                    + ", depth:" + getDepth()
                    + ", maxX:" + getMaxX()
                    + ", maxY:" + getMaxY()
                    + ", maxZ:" + getMaxZ()
                    + "]";
        }
    }
}
