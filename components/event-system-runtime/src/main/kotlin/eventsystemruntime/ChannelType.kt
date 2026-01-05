package eventsystemruntime

/**
 * Channel type enum distinguishing transient vs persistent channels.
 */
enum class ChannelType {
    /**
     * In-memory Vert.x EventBus channel (non-durable).
     */
    TRANSIENT,
    
    /**
     * Database-backed postevent system channel (durable).
     */
    PERSISTENT
}

