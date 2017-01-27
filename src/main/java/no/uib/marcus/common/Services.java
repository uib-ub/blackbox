package no.uib.marcus.common;

/**
 * A list of service names
 ***/
public enum Services {
    /**
     * Marcus service name
     */
    MARCUS {
        @Override
        public String toString() {
            return "marcus";
        }
    },
    /**
     * Skeivtarkiv service name
     */
    SKA {
        @Override
        public String toString() {
            return "ska";
        }
    },
    /**
     * Marcus Admin service name
     */
    WAB {
        @Override
        public String toString() {
            return "wab";
        }
    },

    /**
     * Marcus Admin service name
     */
    MARCUS_ADMIN {
        @Override
        public String toString() {
            return "marcus-admin";
        }
    }
}
