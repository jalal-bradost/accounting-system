# Grocery dataset import

Bundled CSVs for a mini-market. Master data (categories, partners, products) is complete.

`purchases.csv` + `purchase_lines.csv` create confirmed POs, receipts, vendor bills, and payments (`pay_bill` on the header).

Sales / POS **headers** are included, but **line files are empty** until you add:

- `sale_lines.csv`
- `pos_lines.csv`

## API

Login, then:

```
POST /api/v1/import/grocery
Authorization: Bearer <jwt>
X-Company-Id: 00000000-0000-0000-0000-000000000001
```

Or upload your own files:

```
POST /api/v1/import/csv
```

multipart parts: `categories`, `partners`, `products`, `purchases`, `purchase_lines`, `sales_orders`, `sale_lines`, `pos_sessions`, `pos_tickets`, `pos_lines`.
