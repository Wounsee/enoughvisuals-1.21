package punchy.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import punchy.platform.Services;

/* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/EmfFirstPersonHandCompat.class */
public final class EmfFirstPersonHandCompat {
    private static final String EMF_MOD_ID = "entity_model_features";
    private static final String EMF_SHORT_MOD_ID = "emf";
    private static final String ETF_MOD_ID = "entity_texture_features";
    private static final String ETF_SHORT_MOD_ID = "etf";
    private static final String CONTEXT_CLASS = "traben.entity_model_features.models.animation.EMFAnimationEntityContext";
    private static final String EMF_STATE_CLASS = "traben.entity_model_features.models.animation.state.EMFEntityRenderState";
    private static final String ETF_ENTITY_CLASS = "traben.entity_texture_features.utils.ETFEntity";
    private static final String ETF_STATE_CLASS = "traben.entity_texture_features.features.state.ETFEntityRenderState";
    private static boolean checked;
    private static Field firstPersonHandField;
    private static Method setCurrentEntityIterationMethod;
    private static Method resetMethod;
    private static Method etfForEntityMethod;

    private EmfFirstPersonHandCompat() {
    }

    public static Scope enter(Object player) {
        Field field = getField();
        if (field == null) {
            return Scope.NOOP;
        }
        try {
            boolean previous = field.getBoolean(null);
            setPlayerContext(player);
            field.setBoolean(null, true);
            return new Scope(field, previous);
        } catch (Throwable th) {
            return Scope.NOOP;
        }
    }

    private static Field getField() {
        if (checked) {
            return firstPersonHandField;
        }
        checked = true;
        if (Services.PLATFORM != null) {
            if (!Services.PLATFORM.isModLoaded(EMF_MOD_ID) && !Services.PLATFORM.isModLoaded(EMF_SHORT_MOD_ID)) {
                return null;
            }
            try {
                Class<?> context = Class.forName(CONTEXT_CLASS);
                Field field = context.getField("isFirstPersonHand");
                if (field.getType() == Boolean.TYPE) {
                    firstPersonHandField = field;
                }
                resetMethod = context.getMethod("reset", new Class[0]);
                Class<?> emfState = Class.forName(EMF_STATE_CLASS);
                setCurrentEntityIterationMethod = context.getMethod("setCurrentEntityIteration", emfState);
                if (Services.PLATFORM.isModLoaded(ETF_MOD_ID) || Services.PLATFORM.isModLoaded(ETF_SHORT_MOD_ID)) {
                    Class<?> etfEntity = Class.forName(ETF_ENTITY_CLASS);
                    Class<?> etfState = Class.forName(ETF_STATE_CLASS);
                    etfForEntityMethod = etfState.getMethod("forEntity", etfEntity);
                }
            } catch (Throwable th) {
                firstPersonHandField = null;
                setCurrentEntityIterationMethod = null;
                resetMethod = null;
                etfForEntityMethod = null;
            }
            return firstPersonHandField;
        }
        return null;
    }

    private static void setPlayerContext(Object player) {
        if (player == null || setCurrentEntityIterationMethod == null || etfForEntityMethod == null) {
            return;
        }
        try {
            Object state = etfForEntityMethod.invoke(null, player);
            if (state != null) {
                setCurrentEntityIterationMethod.invoke(null, state);
            }
        } catch (Throwable th) {
        }
    }

    private static void resetContext() {
        if (resetMethod == null) {
            return;
        }
        try {
            resetMethod.invoke(null, new Object[0]);
        } catch (Throwable th) {
        }
    }

    /* JADX INFO: loaded from: punchy-2.6.2-fabric-1.21.5.jar:punchy/compat/EmfFirstPersonHandCompat$Scope.class */
    public static final class Scope implements AutoCloseable {
        private static final Scope NOOP = new Scope(null, false);
        private final Field field;
        private final boolean previous;

        private Scope(Field field, boolean previous) {
            this.field = field;
            this.previous = previous;
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            if (this.field == null) {
                return;
            }
            try {
                this.field.setBoolean(null, this.previous);
            } catch (Throwable th) {
            }
            EmfFirstPersonHandCompat.resetContext();
        }
    }
}
