package org.luckypray.dexkit.query.matchers;

import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.base.OpCodesMatcher;

/**
 * Clase stub para MethodMatcher
 */
public class MethodMatcher {
    
    public MethodMatcher() {
    }
    
    public MethodMatcher addUsingString(String str, StringMatchType type) {
        return this;
    }
    
    public MethodMatcher addOpCodesMatcher(OpCodesMatcher matcher) {
        return this;
    }
}
