package kraii.api

/**
 * Marks a property as a "scoped resource".
 *
 * Scoped resources are automatically [AutoCloseable.close]d in reverse order of declaration when
 * their surrounding scope is exited. This is used to bind the life cycle of a resource (e.g. file,
 * disk space, connection, locked mutex, hardware, anything) to the lifetime to their controlling
 * object.
 *
 * Containers (e.g. [List]s) which hold instances of [AutoCloseable]s are also supported. The
 * elements are closed in their reverse natural order of the collection.
 *
 * Limitations:
 * * The type must implement [AutoCloseable] or [Iterable] of [AutoCloseable].
 * * It must be read-only (`val`). Reassignment (`var`) is not supported.
 * * The declaring class must implement [AutoCloseable].
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Scoped
