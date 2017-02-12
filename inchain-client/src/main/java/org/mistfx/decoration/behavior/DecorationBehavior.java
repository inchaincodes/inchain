package org.mistfx.decoration.behavior;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import org.mistfx.decoration.Decoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2015/6/10.
 *
 * @author Misty
 */

public class DecorationBehavior extends BehaviorBase<Decoration> {
    protected static final List<KeyBinding> DECORATOR_BINDINGS = new ArrayList<>();

    public DecorationBehavior(Decoration decoration) {
        super(decoration, DECORATOR_BINDINGS);
    }
}
