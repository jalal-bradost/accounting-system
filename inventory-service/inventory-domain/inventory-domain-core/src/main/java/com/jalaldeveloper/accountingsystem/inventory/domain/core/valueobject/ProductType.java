package com.jalaldeveloper.accountingsystem.inventory.domain.core.valueobject;

/**
 * Inspired by Odoo's product.template.type.
 * <ul>
 *   <li>{@link #STOCKABLE} - participates in stock moves and valuation (creates quants/SVLs).</li>
 *   <li>{@link #CONSUMABLE} - moved on pickings but no on-hand tracking, no valuation.</li>
 *   <li>{@link #SERVICE} - never moved physically, never valued.</li>
 * </ul>
 */
public enum ProductType {
    STOCKABLE,
    CONSUMABLE,
    SERVICE
}
