package org.luckypray.dexkit.query.matchers.base;

import org.luckypray.dexkit.query.enums.OpCodeMatchType;

/**
 * Clase stub para OpCodesMatcher
 */
public class OpCodesMatcher {
    
    private OpCodesMatcher() {
    }
    
    public static OpCodesMatcher create() {
        return new OpCodesMatcher();
    }
    
    public OpCodesMatcher setMatchType(OpCodeMatchType type) {
        return this;
    }
    
    public OpCodesMatcher add(String opCode) {
        return this;
    }
}
