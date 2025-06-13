package org.luckypray.dexkit.result;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Clase stub para MethodDataList
 */
public class MethodDataList implements Iterable<MethodData> {
    private final List<MethodData> list = new ArrayList<>();
    
    public MethodDataList() {
    }
    
    public boolean isEmpty() {
        return true;
    }
    
    @Override
    public Iterator<MethodData> iterator() {
        return list.iterator();
    }
}
