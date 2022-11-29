package kraii.api

/**
 * Marks a property or variable as a "scoped resource".
 *
 * Scoped resources are automatically [AutoCloseable.close]d in reverse order of declaration when
 * their surrounding scope is exited. This is used to bind the life cycle of a resource (e.g. file,
 * disk space, connection, locked mutex, hardware, anything) to the lifetime to their controlling
 * object.
 *
 * Limitations:
 * * The type must implement [AutoCloseable]
 * * It must be read-only (`val`). Reassignment (`var`) is not supported.
 * * If a property of a class is annotated, then the class must also implement [AutoCloseable].
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.SOURCE)
annotation class Scoped
