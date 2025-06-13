package org.luckypray.dexkit.query;

import org.luckypray.dexkit.query.matchers.ClassMatcher;

/**
 * Clase stub para FindClass
 */
public class FindClass {
    
    private FindClass() {}
    
    public static FindClass create() {
        return new FindClass();
    }
    
    public FindClass matcher(ClassMatcher matcher) {
        return this;
    }
    
    public FindClass searchPackages(String packageFilter) {
        return this;
    }
}
