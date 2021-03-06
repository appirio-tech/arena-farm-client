/*
 * HierarchicalIdBuilder
 * 
 * Created 09/29/2006
 */
package com.topcoder.farm.client.util;

/**
 * This class provides a simple way to construct hierarchical keys. <p>
 * 
 * String keys follows the pattern:<p>
 *  {T1}{K1}.{T2}{K2}.{T3}{K3}..
 * 
 * 
 * @author Diego Belfer (mural)
 * @version $Id$
 */
public class HierarchicalIdBuilder {
    /**
     * Delimiter used to generate Ids
     */
    public static final char DELIMITER = '.';
    private StringBuffer sb = null;
    
    
    /**
     * Creates a new HierarchicalIdBuilder
     */
    public HierarchicalIdBuilder() {
        sb = new StringBuffer(50);
    }

    /**
     * Creates a new HierarchicalIdBuilder with the contents of the given builder
     * 
     * @param builder The builder to use as source
     */
    public HierarchicalIdBuilder(HierarchicalIdBuilder builder) {
        sb = builder.newStringBuffer();
    }
    
    /**
     * Add a new level to the Key being generated
     * 
     * @param idType The Type of the level key 
     * @param value The value of the level key
     * 
     * @return this
     */
    public HierarchicalIdBuilder add(char idType, long value) {
        verifyType(idType);
        sb.append(idType).append(value).append(DELIMITER);
        return this;
    }

    /**
     * Add a new level to the Key being generated
     * 
     * @param idType The Type of the level key 
     * @param value The value of the level key
     * 
     * @return this
     */
    public HierarchicalIdBuilder add(char idType, Object value) {
        verifyType(idType);
        sb.append(idType).append(value).append(DELIMITER);
        return this;
    }
    
    /**
     * Add a new level to the Key being generated
     * and return the key generated.
     * This instance is not modified and can be reused
     * to generate more keys using the same top levels.
     * 
     * @param idType The Type of the level key 
     * @param value The value of the level key
     * 
     * @return a Hierarchical String Key representation
     */
    public String buildId(char idType, Object value) {
        verifyType(idType);
        return newStringBuffer().append(idType).append(value).append(DELIMITER).append(DELIMITER).toString(); 
    }

    /**
     * Add a new level to the Key being generated
     * and return the key generated.
     * This instance is not modified and can be reused
     * to generate more keys using the same top levels.
     * 
     * @param idType The Type of the level key 
     * @param value The value of the level key
     * 
     * @return a Hierarchical String Key representation
     */
    public String buildId(char idType, long value) {
        verifyType(idType);
        return newStringBuffer().append(idType).append(value).append(DELIMITER).append(DELIMITER).toString(); 
    }

    /**
     * Obtains the current prefix as generated by the added key levels
     * 
     * @return The current key prefix 
     */
    public String getPrefix() {
        return sb.toString();
    }
    
    /**
     * Obtains the id generated by the added key levels
     * 
     * @return The id generated 
     */
    public String getId() {
        return sb.append(DELIMITER).toString();
    }
    
    private void verifyType(char idType) {
        if (idType == DELIMITER) throw new IllegalArgumentException(". cannot be used as key type identifier");
    }

    /**
     * Java 1.4 compatibility
     */
    private StringBuffer newStringBuffer() {
        return new StringBuffer(sb.length()+16).append(sb);
    }

}
