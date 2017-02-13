package org.mistfx.decoration;

import com.sun.javafx.css.converters.EffectConverter;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.css.converters.SizeConverter;
import javafx.beans.DefaultProperty;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.css.*;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.mistfx.decoration.skin.DecorationSkin;

import java.lang.ref.WeakReference;

/**
 * Created on 2015/6/10.
 *
 * @author Misty
 */
@DefaultProperty("root")
public class Decoration extends Control {
    private static final String TAG = Decoration.class.getSimpleName();

    private final DropShadow initialUnfocusedEffect = new DropShadow();
    private final DropShadow initialFocusedEffect = new DropShadow(BlurType.THREE_PASS_BOX, Color.BLACK, 15, 0.1, 0, 0);

    private WeakReference<Stage> stageRef;

    private final ChangeListener<Boolean> stageFocusedListener = (observable, oldValue, newValue) ->
            this.setFocused(newValue);
    private final ChangeListener<Boolean> weakStageFocusedListener = new WeakChangeListener<>(stageFocusedListener);


    private final ChangeListener<Window> stageChangeListener = (observable, oldValue, newValue) -> {
        // TODO
        if (oldValue != null) {
            Stage stage = (Stage) newValue;
            oldValue.focusedProperty().removeListener(weakStageFocusedListener);
            fullScreenPropertyImpl().unbind();
            maximizedPropertyImpl().unbind();
            iconifiedPropertyImpl().unbind();
            titleProperty().unbindBidirectional(stage.titleProperty());
        }
        if (newValue != null) {
            Stage stage = (Stage) newValue;
            stageRef = new WeakReference<>(stage);

            stage.focusedProperty().addListener(weakStageFocusedListener);
            fullScreenPropertyImpl().bind(stage.fullScreenProperty());
            maximizedPropertyImpl().bind(stage.maximizedProperty());
            iconifiedPropertyImpl().bind(stage.iconifiedProperty());
            titleProperty().bindBidirectional(stage.titleProperty());
        }
    };
    private final ChangeListener<Window> weakStageChangeListener = new WeakChangeListener<>(stageChangeListener);


    public Decoration(Parent root) {
        // TODO handle empty root
        setRoot(root);

        sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.windowProperty().removeListener(weakStageChangeListener);
            }
            if (newValue != null) {
                newValue.windowProperty().addListener(weakStageChangeListener);
            }
        });
    }

    public void close() {
        Utils.applyToStage(this, stage ->
                stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
    }

    public Stage getStage() {
        Stage ret = stageRef != null ? stageRef.get() : null;
        if (ret == null) {
            throw new IllegalStateException(this + " is not inside a scene-graph");
        }
        return ret;
    }

    /** root */
    private ObjectProperty<Parent> root;

    public final void setRoot(Parent value) {
        rootProperty().set(value);
    }

    public final Parent getRoot() {
        return root == null ? null : root.get();
    }

    Parent oldRoot;

    public final ObjectProperty<Parent> rootProperty() {
        if (root == null) {
            root = new ObjectPropertyBase<Parent>() {

                private void forceUnbind() {
                    System.err.println("Unbinding illegal root.");
                    unbind();
                }

                @Override
                protected void invalidated() {
                    Parent _value = get();

                    if (_value == null) {
                        if (isBound()) forceUnbind();
                        throw new NullPointerException(TAG + "'s root cannot be null");
                    }

                    if (_value.getParent() != null) {
                        if (isBound()) forceUnbind();
                        throw new IllegalArgumentException(_value +
                                "is already inside a scene-graph and cannot be set as root");
                    }
                    oldRoot = _value;
                    _value.resize(getWidth(), getHeight()); // maybe no-op if root is not resizable
                    _value.requestLayout();
                }

                @Override
                public Object getBean() {
                    return Decoration.this;
                }

                @Override
                public String getName() {
                    return "root";
                }
            };
        }
        return root;
    }

    /** utility mode */
    private BooleanProperty utilityMode = new SimpleBooleanProperty(this, "utilityMode", false);

    public boolean isUtilityMode() {
        return utilityMode.get();
    }

    public void setUtilityMode(boolean utilityMode) {
        this.utilityMode.set(utilityMode);
    }

    public BooleanProperty utilityModeProperty() {
        return utilityMode;
    }

    /** title */
    private StringProperty title = new SimpleStringProperty(this, "title", "");

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public StringProperty titleProperty() {
        return title;
    }

    /** shadowVisible */
    private BooleanProperty shadowVisible = new SimpleBooleanProperty(this, "shadowVisible", true);

    public boolean isShadowVisible() {
        return shadowVisible.get();
    }

    public void setShadowVisible(boolean shadowVisible) {
        this.shadowVisible.set(shadowVisible);
    }

    public BooleanProperty shadowVisibleProperty() {
        return shadowVisible;
    }

    /**
     * full screen
     */
    private ReadOnlyBooleanWrapper fullScreen;

    public final boolean isFullScreen() {
        return fullScreen != null && fullScreen.get();
    }

    public final void setFullScreen(boolean value) {
        Utils.applyToStage(this, stage -> stage.setFullScreen(value));
    }

    public final ReadOnlyBooleanProperty fullScreenProperty() {
        return fullScreenPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper fullScreenPropertyImpl() {
        if (fullScreen == null) {
            fullScreen = new ReadOnlyBooleanWrapper(this, "fullScreen");
        }
        return fullScreen;
    }

    /** maximized */
    private ReadOnlyBooleanWrapper maximized;

    public final boolean isMaximized() {
        return maximized != null && maximized.get();
    }

    public final void setMaximized(boolean value) {
        Utils.applyToStage(this, stage -> stage.setMaximized(value));
    }

    public final ReadOnlyBooleanProperty maximizedProperty() {
        return maximizedPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper maximizedPropertyImpl() {
        if (maximized == null) {
            maximized = new ReadOnlyBooleanWrapper(this, "maximized");
        }
        return maximized;
    }

    /** iconified */
    private ReadOnlyBooleanWrapper iconified;

    public final boolean isIconified() {
        return iconified != null && iconified.get();
    }

    public final void setIconified(boolean value) {
        Utils.applyToStage(this, stage -> stage.setIconified(value));
    }

    public final ReadOnlyBooleanProperty iconifiedProperty() {
        return iconifiedPropertyImpl().getReadOnlyProperty();
    }

    private ReadOnlyBooleanWrapper iconifiedPropertyImpl() {
        if (iconified == null) {
            iconified = new ReadOnlyBooleanWrapper(this, "iconified");
        }
        return iconified;
    }

    /**
     * focused effect
     */
    private ObjectProperty<Effect> focusedEffect = new StyleableObjectProperty<Effect>(initialFocusedEffect) {
        @Override
        public Object getBean() {
            return Decoration.this;
        }

        @Override
        public String getName() {
            return "focusedEffect";
        }

        @Override
        public CssMetaData<? extends Styleable, Effect> getCssMetaData() {
            return StyleableProperties.FOCUSED_EFFECT;
        }
    };

    public Effect getFocusedEffect() {
        return focusedEffect.get();
    }

    public ObjectProperty<Effect> focusedEffectProperty() {
        return focusedEffect;
    }

    public void setFocusedEffect(Effect focusedEffect) {
        this.focusedEffect.set(focusedEffect);
    }

    /**
     * unfocused effect
     */
    private ObjectProperty<Effect> unfocusedEffect = new StyleableObjectProperty<Effect>(initialUnfocusedEffect) {
        @Override
        public Object getBean() {
            return Decoration.this;
        }

        @Override
        public String getName() {
            return "unfocusedEffect";
        }

        @Override
        public CssMetaData<? extends Styleable, Effect> getCssMetaData() {
            return StyleableProperties.UNFOCUSED_EFFECT;
        }
    };

    public Effect getUnfocusedEffect() {
        return unfocusedEffect.get();
    }

    public ObjectProperty<Effect> unfocusedEffectProperty() {
        return unfocusedEffect;
    }

    public void setUnfocusedEffect(Effect unfocusedEffect) {
        this.unfocusedEffect.set(unfocusedEffect);
    }

    // TODO effect

    /**
     * backgroundFill
     */
    private ObjectProperty<Paint> backgroundFill = new StyleableObjectProperty<Paint>(Color.WHITE) {
        @Override
        public Object getBean() {
            return Decoration.this;
        }

        @Override
        public String getName() {
            return "backgroundFill";
        }

        @Override
        public CssMetaData<? extends Styleable, Paint> getCssMetaData() {
            return StyleableProperties.BACKGROUND_FILL;
        }
    };

    public Paint getBackgroundFill() {
        return backgroundFill.get();
    }

    public ObjectProperty<Paint> backgroundFillProperty() {
        return backgroundFill;
    }

    public void setBackgroundFill(Paint backgroundFill) {
        this.backgroundFill.set(backgroundFill);
    }

    /**
     * background opacity
     * <p>
     * defaultValue 0.9
     */
    private DoubleProperty backgroundOpacity = new StyleableDoubleProperty(0.9) {
        @Override
        public Object getBean() {
            return Decoration.this;
        }

        @Override
        public String getName() {
            return "backgroundOpacity";
        }

        @Override
        public CssMetaData<? extends Styleable, Number> getCssMetaData() {
            return StyleableProperties.BACKGROUND_OPACITY;
        }
    };

    public double getBackgroundOpacity() {
        return backgroundOpacity.get();
    }

    public DoubleProperty backgroundOpacityProperty() {
        return backgroundOpacity;
    }

    public void setBackgroundOpacity(double backgroundOpacity) {
        this.backgroundOpacity.set(backgroundOpacity);
    }

/* =============================================== */
    /* =============================================== */
    /* =============================================== */

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DecorationSkin(this);
    }

    // TODO style support
    @SuppressWarnings("unchecked")
    private static class StyleableProperties {
        private static final CssMetaData<Decoration, Effect> FOCUSED_EFFECT =
                new CssMetaData<Decoration, Effect>("-fx-focused-effect", EffectConverter.getInstance()) {
                    @Override
                    public boolean isSettable(Decoration n) {
                        return n.focusedEffect == null || !n.focusedEffect.isBound();
                    }

                    @Override
                    public StyleableProperty<Effect> getStyleableProperty(Decoration n) {
                        return (StyleableProperty<Effect>) n.focusedEffectProperty();
                    }
                };
        private static final CssMetaData<Decoration, Effect> UNFOCUSED_EFFECT =
                new CssMetaData<Decoration, Effect>("-fx-unfocused-effect", EffectConverter.getInstance()) {
                    @Override
                    public boolean isSettable(Decoration n) {
                        return n.unfocusedEffect == null || !n.unfocusedEffect.isBound();
                    }

                    @Override
                    public StyleableProperty<Effect> getStyleableProperty(Decoration n) {
                        return (StyleableProperty<Effect>) n.unfocusedEffectProperty();
                    }
                };
        private static final CssMetaData<Decoration, Paint> BACKGROUND_FILL =
                new CssMetaData<Decoration, Paint>("-fx-background-fill", PaintConverter.getInstance()) {
                    @Override
                    public boolean isSettable(Decoration n) {
                        return n.backgroundFill == null || !n.backgroundFill.isBound();
                    }

                    @Override
                    public StyleableProperty<Paint> getStyleableProperty(Decoration n) {
                        return (StyleableProperty<Paint>) n.backgroundFillProperty();
                    }
                };
        private static final CssMetaData<Decoration, Number> BACKGROUND_OPACITY =
                new CssMetaData<Decoration, Number>("-fx-background-opacity", SizeConverter.getInstance(), 0.9) {
                    @Override
                    public boolean isSettable(Decoration n) {
                        return n.backgroundOpacity == null || !n.backgroundOpacity.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(Decoration n) {
                        return (StyleableProperty<Number>) n.backgroundOpacityProperty();
                    }
                };
    }
}
