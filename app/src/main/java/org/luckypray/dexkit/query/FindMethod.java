package org.luckypray.dexkit.query;

import org.luckypray.dexkit.query.matchers.MethodMatcher;

/**
 * Clase stub para FindMethod
 */
public class FindMethod {
    
    private FindMethod() {}
    
    public static FindMethod create() {
        return new FindMethod();
    }
    
    public FindMethod matcher(MethodMatcher matcher) {
        return this;
    }
    
    public FindMethod searchPackages(String packageFilter) {
        return this;
    }
}
