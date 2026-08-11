package fun.enoughvisuals.systems.hud;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Keeps list rows alive during removal and springs surviving rows into place. */
public final class HudListAnimator {
    public static final class Row {
        private final String key;
        private final HudMotion.Spring position;
        private float visibility;
        private float targetPosition;
        private boolean present;

        private Row(String key, float position) {
            this.key = key;
            this.position = new HudMotion.Spring(position);
            this.targetPosition = position;
        }

        public String key() {
            return key;
        }

        public float position() {
            return position.value();
        }

        public float visibility() {
            return visibility;
        }

        public boolean present() {
            return present;
        }
    }

    private final Map<String, Row> rows = new LinkedHashMap<>();

    public void update(List<String> orderedKeys, float step, float frameDelta) {
        Set<String> incoming = new HashSet<>(orderedKeys);
        for (Row row : rows.values()) row.present = incoming.contains(row.key);

        for (int index = 0; index < orderedKeys.size(); index++) {
            String key = orderedKeys.get(index);
            float target = index * step;
            Row row = rows.get(key);
            if (row == null) {
                row = new Row(key, target + step * 0.42f);
                rows.put(key, row);
            }
            row.present = true;
            row.targetPosition = target;
        }

        List<String> expired = new ArrayList<>();
        for (Row row : rows.values()) {
            row.visibility = HudMotion.damp(row.visibility, row.present ? 1f : 0f,
                    row.present ? 0.24f : 0.18f, frameDelta);
            row.position.step(row.targetPosition, frameDelta);
            if (!row.present && row.visibility <= 0.005f) expired.add(row.key);
        }
        expired.forEach(rows::remove);
    }

    public List<Row> rows() {
        List<Row> result = new ArrayList<>(rows.values());
        result.sort((a, b) -> Float.compare(a.position(), b.position()));
        return result;
    }

    public void clear() {
        rows.clear();
    }
}
