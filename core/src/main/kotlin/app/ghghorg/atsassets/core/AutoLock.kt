package app.ghghorg.atsassets.core

/** The four auto-lock choices of spec section 9, a setting (section 3). K8 applies them (20). */
enum class AutoLock {
    /** The default. */
    EVERY_LEAVE,
    IDLE_1_MIN,
    IDLE_5_MIN,
    PHONE_LOCK_OR_RESTART,
}
