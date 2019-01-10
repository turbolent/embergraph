package com.bigdata.btree.keys;

import java.text.Collator;

/**
 * Type safe enumeration for the strength.
 * <p>
 * Note: ICU and the JDK use different integer constants for the
 * #IDENTICAL strength
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public enum StrengthEnum {
    
    /**
     * See {@link Collator#PRIMARY}.
     */
    Primary,
    /**
     * See {@link Collator#SECONDARY}.
     */
    Secondary,
    /**
     * See {@link Collator#TERTIARY}.
     */
    Tertiary,
    /**
     * Note: this option is NOT supported by the JDK.
     */
    Quaternary,
    /**
     * See {@link Collator#IDENTICAL}.
     */
    Identical;
    
}