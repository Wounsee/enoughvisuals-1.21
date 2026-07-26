package fun.enoughvisuals.port.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import fun.enoughvisuals.port.Category;
import fun.enoughvisuals.port.ModuleRiskLevel;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleRegister {
    String name();
    String description();
    Category category();
    ModuleRiskLevel[] riskLevels() default {};
}
