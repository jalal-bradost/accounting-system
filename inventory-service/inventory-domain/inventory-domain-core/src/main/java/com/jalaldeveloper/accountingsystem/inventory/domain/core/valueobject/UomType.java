package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

/**
 * Role of a unit of measure inside its {@code UomCategory}.
 * <ul>
 *   <li>{@link #REFERENCE} - the canonical unit (factor 1).</li>
 *   <li>{@link #BIGGER} - factor &gt; 1 of the reference (e.g. 12 for "dozen" if "unit" is reference).</li>
 *   <li>{@link #SMALLER} - factor &lt; 1 of the reference (e.g. 0.001 for "g" if "kg" is reference).</li>
 * </ul>
 */
public enum UomType {
    REFERENCE,
    BIGGER,
    SMALLER
}
