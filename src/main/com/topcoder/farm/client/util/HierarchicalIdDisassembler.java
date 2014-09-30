/*
 * HierarchicalIdDisassembler
 *
 * Created 11/16/2006
 */
package com.topcoder.farm.client.util;

/**
 * Helper class to obtain values from an id generated by {@link HierarchicalIdBuilder}
 *
 * @author Diego Belfer (mural)
 * @version $Id$
 */
public class HierarchicalIdDisassembler {
    private static final int MAX_SIZE = 16;
    private int[] indexes = new int[MAX_SIZE];
    private String id;
    private int size;


    public HierarchicalIdDisassembler() {
    }

    public HierarchicalIdDisassembler(String id) {
        this();
        parseId(id);
    }

    public void parseId(String idValue) {
        this.id = idValue;
        this.size = 1;
        this.indexes[0] = -1;
        if (id.charAt(id.length()-1) != HierarchicalIdBuilder.DELIMITER) {
            id = id + '.';
        }
        for (int i = 0; i < id.length() && size < MAX_SIZE; i++) {
            if (id.charAt(i) == HierarchicalIdBuilder.DELIMITER) {
                this.indexes[size] = i;
                size++;
            }
        }
        if (size == MAX_SIZE) {
            throw new IllegalArgumentException("The max number of levels is "+MAX_SIZE);
        }
        if (size > 2 && indexes[size-2] == indexes[size-1]-1) {
            //Is .. (termination indicator)
            size--;
        }
        size--;
    }

    public char getType(int index) {
        return id.charAt(indexes[index]+1);
    }

    public long getValueAsLong(int index) {
        return Long.parseLong(getValueAsString(index));
    }

    public int getValueAsInt(int index) {
        return Integer.parseInt(getValueAsString(index));
    }
    public String getValueAsString(int index) {
        return id.substring(indexes[index]+2, indexes[index+1]);
    }

    public int delimiterCount() {
        return size;
    }
}
