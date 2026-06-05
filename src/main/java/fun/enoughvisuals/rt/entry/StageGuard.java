package fun.enoughvisuals.rt.entry;

public final class StageGuard {

    private StageGuard() {
    }

    public static boolean check(String stage) {
        if (stage == null || stage.isEmpty()) {
            return false;
        }

        // Reserved stages for future subscription/HWID checks.
        // Keep permissive by default so release builds stay stable.
        return true;
    }
}
