package kg.musabaev.megalabnews.util.validator;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * @see org.hibernate.validator.constraints.URL
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UrlOrNullValidator.class)
@Documented
public @interface UrlOrNull {
	String message() default "is not valid url";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	String protocol() default "";

	String host() default "";

	int port() default -1;
}
