package org.inchain.network;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class that holds all the registered NetworkParameters types used for Address auto discovery.
 * By default only MainNetParams and TestNet3Params are used. If you want to use TestNet2, RegTestParams or
 * UnitTestParams use the register and unregister the TestNet3Params as they don't have their own address
 * version/type code.
 */
public class Networks {
    /** Registered networks */
    private static Set<NetworkParams> networks = new HashSet<NetworkParams>();

    public static Set<? extends NetworkParams> get() {
        return networks;
    }

    public static void register(NetworkParams network) {
    	networks.add(network);
    }

    public static void register(Set<NetworkParams> networks) {
        Networks.networks = networks;
    }

    public static void unregister(NetworkParams network) {
        if (networks.contains(network)) {
            networks.remove(network);
        }
    }
}
