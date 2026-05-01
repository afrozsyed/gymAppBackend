package com.gymcrm.multitenancy;

import com.gymcrm.exception.GymContextException;

public final class GymContext {

    private static final ThreadLocal<Long> CONTEXT = new ThreadLocal<>();

    private GymContext() {}

    public static void set(Long gymId) {
        CONTEXT.set(gymId);
    }

    public static Long get() {
        Long gymId = CONTEXT.get();
        if (gymId == null) {
            throw new GymContextException("No gym context found on current thread");
        }
        return gymId;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
