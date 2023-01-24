package com.convallyria.forcepack.api;

public abstract class ForcePackImpl {

    protected static final class Instance { // Invisible except for implementation & NPC class
        private static ForcePackAPI implementation = null;

        public static void setImplementation(ForcePackAPI implementation) {
            if (ForcePackImpl.Instance.implementation != null) {
                throw new IllegalArgumentException("Implementation already set!");
            }
            ForcePackImpl.Instance.implementation = implementation;
        }

        public static ForcePackAPI getImplementation() {
            return implementation;
        }
    }
}
