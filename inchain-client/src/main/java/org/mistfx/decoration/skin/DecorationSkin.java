package org.mistfx.decoration.skin;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.List;

import org.mistfx.decoration.Decoration;
import org.mistfx.decoration.Utils;
import org.mistfx.decoration.behavior.DecorationBehavior;

import com.sun.javafx.scene.control.skin.BehaviorSkinBase;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * Created on 2015/6/10.
 *
 * @author Misty
 */
public class DecorationSkin extends BehaviorSkinBase<Decoration, DecorationBehavior> {
    public static final String ROOT_STYLE_CLASS = "skin-root";
    public static final String SHADOW_STYLE_CLASS = "skin-shadow";
    public static final String BACKGROUND_STYLE_CLASS = "skin-background";

    private MfxContainer mRoot;
    // root contains follows
    private MfxShadow mShadow;
    private MfxBackground mBackground;
    private Node mClientRoot;
    // ---------------------
    private Insets mSavedRootInsets;
    private StackPane clientPlaceHolder = new StackPane(new Label("Decoration"));
    private Rectangle mShadowClipExternal = new Rectangle();
    private Rectangle mShadowClipInternal = new Rectangle(0, 0, -1, -1);
    private Rectangle mShadowClipEmpty = new Rectangle(0, 0, 0, 0);

    public DecorationSkin(Decoration decoration) {
        super(decoration, new DecorationBehavior(decoration));

        decoration.setBackground(null);
        _initUI(decoration);
        getChildren().addAll(mRoot);
    }

    private void _initUI(Decoration decoration) {
        mSavedRootInsets = Utils.getEffectPadding(decoration);
        mShadowClipInternal.setStyle("-fx-fill: black; -fx-stroke-width: 0");
        mShadowClipExternal.setStyle("-fx-fill: black; -fx-stroke-width: 0");
        // root
        mRoot = new MfxContainer();
    }

    class MfxShadow extends Rectangle {
        MfxShadow() {
            getStyleClass().add(SHADOW_STYLE_CLASS);
            setMouseTransparent(true);
            setFill(Color.WHITE);
            setStrokeWidth(0);
            layoutBoundsProperty().addListener((observable, oldValue, bounds) -> {
                boolean isMaximized = getSkinnable().isMaximized();
                if (isMaximized) {
                    this.setClip(mShadowClipEmpty);
                } else {
                    Insets padding = mRoot.getPadding();
                    Utils.resizeRelocate(mShadowClipExternal,
                            bounds.getWidth() + padding.getLeft() + padding.getRight(),
                            bounds.getHeight() + padding.getTop() + padding.getBottom(),
                            bounds.getMinX() - padding.getLeft(),
                            bounds.getMinY() - padding.getTop());
                    Utils.resizeRelocate(mShadowClipInternal,
                            this.getWidth(),
                            this.getHeight(),
                            bounds.getMinX(),
                            bounds.getMinY());
                    Shape clip = Shape.subtract(mShadowClipExternal, mShadowClipInternal);
                    this.setClip(clip);
                }
            });
            toggleVisible();
            toggleEffect();
        }

        void toggleVisible() {
            boolean visible = getSkinnable().isShadowVisible();
            this.setVisible(visible);
            this.setManaged(visible);
        }

        void toggleEffect() {
            this.setEffect(Utils.getEffect(getSkinnable()));
        }
    }

    class MfxBackground extends Rectangle {
        MfxBackground() {
            getStyleClass().add(BACKGROUND_STYLE_CLASS);
            setStrokeWidth(0);
            toggleOpacity();
            toggleFill();
        }

        void toggleFill() {
            setFill(getSkinnable().getBackgroundFill());
        }

        void toggleOpacity() {
            setOpacity(getSkinnable().getBackgroundOpacity());
        }
    }

    class MfxContainer extends StackPane {
        ObjectProperty<Node> _decoration = new SimpleObjectProperty<>();

        private String decorationNormalFXML;
        private String decorationUtilityFXML;
        private Node decorationNormal;
        private Node decorationUtility;

        private boolean performingLayout = true;

        MfxContainer() {
            getStyleClass().addAll(ROOT_STYLE_CLASS);
            setMinSize(0, 0);
            togglePadding();

            // shadow
            mShadow = new MfxShadow();
            // background
            mBackground = new MfxBackground();
            // client area
            Parent clientRoot = getSkinnable().getRoot();
            mClientRoot = clientRoot != null ? clientRoot : clientPlaceHolder;
            mClientRoot.setPickOnBounds(false);

            // decoration
            toggleDecoration();

            this.getChildren().addAll(
                    mShadow,            // shadow
                    mBackground,        // background
                    _decoration.get(),  // decoration root
                    mClientRoot         // client root
            );

            _decoration.addListener((observable, oldValue, newValue) -> {
                if (nonNull(oldValue) && nonNull(newValue)) {
                    FXCollections.replaceAll(this.getChildren(), oldValue, newValue);
                }
            });
        }

        @Override
        protected void layoutChildren() {
            performingLayout = true;
            List<Node> managed = getManagedChildren();
            if (!managed.isEmpty()) {
                final double width = getWidth(),
                        height = getHeight();
                final double top = getInsets().getTop(),
                        right = getInsets().getRight(),
                        bottom = getInsets().getBottom(),
                        left = getInsets().getLeft();
                final double contentWidth = width - left - right,
                        contentHeight = height - top - bottom;
                managed.stream().forEach(child -> {
                    if (child == getSkinnable().getRoot()) { // decoration padding
                        Insets padding = getSkinnable().getPadding();
                        final double border = 5;
                        final double header = 30;
                        final double w = contentWidth - padding.getLeft() - padding.getRight()  /**/ - border * 2;
                        final double h = contentHeight - padding.getTop() - padding.getBottom() /**/ - border - header;
                        final double l = left + padding.getLeft()                               /**/ + border;
                        final double t = top + padding.getTop()                                 /**/ + header;
                        layoutInArea(child, l, t, w, h, 0, getMargin(child), HPos.LEFT, VPos.TOP); // child margin
                    } else {
                        layoutInArea(child, left, top, contentWidth, contentHeight, 0, HPos.LEFT, VPos.TOP);
                        if (!child.isResizable()) {
                            Utils.resize(child, contentWidth, contentHeight);
                        }
                    }
                });
            }
            performingLayout = false;
        }

        @Override
        public void requestLayout() {
            if (performingLayout) {
                return;
            }
            super.requestLayout();
        }

        public void togglePadding() {
            boolean isShadowVisible = getSkinnable().isShadowVisible();
            boolean isMax = getSkinnable().isMaximized();
            boolean isFull = getSkinnable().isFullScreen();
            this.setPadding(!isShadowVisible || isMax || isFull ? Insets.EMPTY : mSavedRootInsets);
        }

        public void toggleDecoration() {
            Node root = null;
            boolean isUtilityMode = getSkinnable().isUtilityMode();
            String fxml;
            if (isUtilityMode) {
                fxml = "decoration_utility.fxml";
                if (fxml.equals(decorationUtilityFXML)) {
                    root = decorationUtility;
                }
            } else {
                fxml = "decoration.fxml";
                if (fxml.equals(decorationNormalFXML)) {
                    root = decorationNormal;
                }
            }
            if (root == null) {
                DecorationController controller = new DecorationController(getSkinnable());
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                loader.setController(controller);
                try {
                    root = loader.load();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (isUtilityMode) {
                    decorationUtilityFXML = fxml;
                    decorationUtility = root;
                } else {
                    decorationNormalFXML = fxml;
                    decorationNormal = root;
                }
            }
            _decoration.set(root);
        }
    }
}
