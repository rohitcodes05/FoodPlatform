# Phase 11E — Raw Seller Module Implementation Plan

## 1. Current Architecture
- **Backend:** NestJS + Prisma + PostgreSQL.
- **Android:** Jetpack Compose + MVVM + Retrofit.
- **Product Model:** Flat catalog representation (`Product` table has `price`, `type`, `isAvailable`).
- **Cart/Order:** `CartItem` links directly to `Product` via FK. `OrderItem` snapshots the `purchasePrice` but has no fields for specific variations.

## 2. Existing Raw Seller Support
- **Enums exist:** `ProductType.RAW_MEAT` and `PartnerType.RAW_SELLER` are defined in the Prisma schema.
- **Missing Models:** There are no DB models, relations, or controllers for `CutOption` and `WeightUnit`.
- **Missing UI:** No Android partner UI for configuring raw options, and no customer UI for selecting them.

## 3. Database Design (Confirmed Naming: Option)
**Proposed Prisma Changes:**
```prisma
model CutOption {
  id          String   @id @default(uuid())
  productId   String
  name        String
  isAvailable Boolean  @default(true)
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt

  product     Product  @relation(fields: [productId], references: [id], onDelete: Cascade)
}

model WeightOption {
  id            String   @id @default(uuid())
  productId     String
  weightLabel   String   // e.g., "500g", "1kg"
  priceOverride Decimal  @db.Decimal(12, 2) // Absolute final price for this weight
  isAvailable   Boolean  @default(true)
  createdAt     DateTime @default(now())
  updatedAt     DateTime @updatedAt

  product       Product  @relation(fields: [productId], references: [id], onDelete: Cascade)
}
```
*Note: `Product` model gets `cutOptions CutOption[]` and `weightOptions WeightOption[]`.*
*Note: `CartItem` unique constraint will be updated to `@@unique([cartId, productId, cutOptionId, weightOptionId])`.*

## 4. Confirmed Business Rules
1. **Pricing:** `WeightOption.priceOverride` defines the absolute final price of the cart item. If a weight is selected, its `priceOverride` replaces the base product price entirely. Price calculation is strictly server-side authoritative.
2. **Mandatory Selection:** For `ProductType.RAW_MEAT`, if any `CutOption` or `WeightOption` exists for the product, the customer MUST select them. Backend will throw `400 Bad Request` if they are missing. No silent defaults.
3. **Independence:** Any valid `CutOption` can be combined with any valid `WeightOption` belonging to the same product.
4. **Deleted/Disabled Variations in Cart:** 
   - If a variation is hard-deleted, Prisma `onDelete: Cascade` handles it.
   - If `isAvailable` becomes false, the `GET /cart` endpoint will flag the item or the Checkout endpoint will throw a `400` preventing purchase of unavailable variations.

## 5. Explicit Verifications & Architectural Guarantees
Before and during implementation, the following invariants are strictly guaranteed:
- [x] **CartItem Uniqueness:** Constraint updated to `@@unique([cartId, productId, cutOptionId, weightOptionId])`. Customers can add "Chicken (Curry Cut, 500g)" and "Chicken (Keema, 1kg)" as separate distinct cart items.
- [x] **OrderItem Immutability:** `OrderItem` will store `selectedCut String?` and `selectedWeight String?`. This guarantees the historical invoice remains perfectly intact even if the Partner later deletes the `CutOption` or `WeightOption`.
- [x] **Backward Compatibility:** `cutOptionId` and `weightOptionId` are strictly optional at the DB level. Existing non-RAW_MEAT products (and historical cart/order items) will remain `NULL` and continue functioning normally without regression.
- [x] **Partner Ownership (IDOR):** Every variation mutation (`POST/PATCH/DELETE` on cuts/weights) will verify `product.partnerId === currentUser.partner.id`.
- [x] **Cross-Product Submission Rejection:** When adding to cart, the backend will verify that the submitted `cutOptionId` and `weightOptionId` actually belong to the submitted `productId`. Mix-and-match hacking returns `403/400`.
- [x] **Server-Side Pricing:** Cart totals and Order totals will calculate using the DB-fetched `priceOverride` from the `WeightOption`, completely ignoring any price claims sent by the Android client.

## 6. Backend API Design
**Customer Cart API:**
- `POST /cart/items` (`AddCartItemDto`): add optional `cutOptionId` and `weightOptionId`.

**Partner Product Variation API:**
- Sub-resource endpoints for cleaner IDOR checks and targeted UI updates:
  - `POST /partners/products/:id/cuts` & `PATCH` & `DELETE`
  - `POST /partners/products/:id/weights` & `PATCH` & `DELETE`

## 7. Android Changes
- **DTOs:** Add `cutOptionId`, `weightOptionId` to `CartItemDto`, `AddCartItemRequest`. Add `selectedCut`, `selectedWeight` to `OrderItemDto`.
- **PartnerDashboardScreen:** Tapping a `RAW_MEAT` product opens a variation management sheet for Cuts and Weights.
- **ProductDetailScreen:** Render Cut/Weight dropdowns/chips for `RAW_MEAT`. Block "Add to Cart" until options are selected. Pass IDs in `AddCartItemRequest`.

## 8. Migration Plan
- Generate Prisma migration: `CutOption`, `WeightOption`, update `CartItem` constraint, update `OrderItem` fields.
- **Data Compatibility:** Safe for existing data since new fields are nullable.

## 9. Testing Plan (`test/partners.raw-options.idor.e2e-spec.ts`)
- `RAW_SELLER` variation creation and IDOR rejection tests.
- Customer variation validation tests (missing mandatory, mismatching IDs, unavailable variations).
- Checkout variation snapshot tests.
- Server-side price calculation tests.

## 10. Implementation Order
1. **DB/Schema:** Update Prisma schema, create test DB sync script, update `CartItem` unique constraint.
2. **Backend API:** Sub-resource controllers for Partner Cut/Weight management.
3. **Cart & Order:** Update Cart logic to accept & validate variations, update Checkout to snapshot them.
4. **Security Audit:** E2E tests for IDOR and Cart validations.
5. **Android Data Layer:** Update DTOs and API interfaces.
6. **Android UI:** Partner management screens and Customer selection flows.
