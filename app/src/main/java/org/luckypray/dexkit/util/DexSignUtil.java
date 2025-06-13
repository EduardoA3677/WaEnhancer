package org.luckypray.dexkit.util;

/**
 * Clase stub para DexSignUtil
 */
public class DexSignUtil {
    
    public static String getMethodSign(String className, String methodName, String... parameterTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(className.replace(".", "/"));
        sb.append("->");
        sb.append(methodName);
        sb.append("(");
        
        for (String paramType : parameterTypes) {
            sb.append(paramType.replace(".", "/"));
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    public static String getMethodFuzzySign(String className, String methodName) {
        return className.replace(".", "/") + "->" + methodName;
    }
}
