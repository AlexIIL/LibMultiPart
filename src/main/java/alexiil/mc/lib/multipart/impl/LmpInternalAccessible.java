package alexiil.mc.lib.multipart.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Indicates that a field or method is accessed reflectively by LMP. (This is just used to ensure we can make some
 * parts of the API not publicly accessible, but still have some safety when using reflection). */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface LmpInternalAccessible {

}
