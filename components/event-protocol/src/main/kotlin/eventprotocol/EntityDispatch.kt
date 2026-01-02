package eventprotocol

import kotlin.reflect.KClass

/**
 * Polymorphic entity update dispatcher using map-based multimethod pattern.
 * 
 * EntityDispatch provides type-safe polymorphic dispatch for updating entities
 * based on events. It uses a map of entity types to updater functions, allowing
 * different update logic for different entity types.
 * 
 * Example usage:
 * ```kotlin
 * val dispatch = EntityDispatch.create<Any>()
 *     .register(User::class) { user, event ->
 *         user.copy(email = event.data["email"] as String)
 *     }
 *     .register(Order::class) { order, event ->
 *         order.copy(status = event.data["status"] as String)
 *     }
 * 
 * val updatedUser = dispatch.update(user, event) as User
 * ```
 * 
 * @param T Base type for all entities (typically Any)
 */
class EntityDispatch<T : Any> private constructor() {
    
    private val updaters = mutableMapOf<KClass<*>, (Any, BaseEvent) -> Any>()
    
    /**
     * Register an updater function for a specific entity type.
     * 
     * @param entityClass The entity class to register
     * @param updater Function to update entities of this type
     * @return This EntityDispatch instance for chaining
     */
    fun <E : T> register(
        entityClass: KClass<E>,
        updater: (E, BaseEvent) -> E
    ): EntityDispatch<T> {
        @Suppress("UNCHECKED_CAST")
        updaters[entityClass] = updater as (Any, BaseEvent) -> Any
        return this
    }
    
    /**
     * Update an entity based on an event.
     * 
     * Dispatches to the appropriate updater function based on the entity's
     * runtime type.
     * 
     * @param entity The entity to update
     * @param event The event containing update data
     * @return Updated entity
     * @throws IllegalArgumentException if no updater is registered for the entity type
     */
    fun update(entity: T, event: BaseEvent): T {
        val entityClass = entity::class
        val updater = updaters[entityClass]
            ?: throw IllegalArgumentException(
                "No updater registered for entity type: ${entityClass.simpleName}"
            )
        
        @Suppress("UNCHECKED_CAST")
        return updater(entity, event) as T
    }
    
    companion object {
        /**
         * Create a new EntityDispatch instance.
         * 
         * @param T Base type for all entities
         * @return New EntityDispatch instance
         */
        fun <T : Any> create(): EntityDispatch<T> {
            return EntityDispatch()
        }
    }
}

