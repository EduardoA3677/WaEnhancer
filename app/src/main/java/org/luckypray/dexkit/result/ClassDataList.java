package org.luckypray.dexkit.result;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Clase stub para ClassDataList
 */
public class ClassDataList implements Iterable<ClassData> {
    private final List<ClassData> list = new ArrayList<>();
    
    public ClassDataList() {
    }
    
    public boolean isEmpty() {
        return true;
    }
    
    @Override
    public Iterator<ClassData> iterator() {
        return list.iterator();
    }
}
