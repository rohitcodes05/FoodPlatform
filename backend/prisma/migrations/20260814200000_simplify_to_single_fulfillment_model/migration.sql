-- DropForeignKey
ALTER TABLE "CartItem" DROP CONSTRAINT "CartItem_storeProductId_fkey";

-- DropForeignKey
ALTER TABLE "Inventory" DROP CONSTRAINT "Inventory_storeProductId_fkey";

-- DropForeignKey
ALTER TABLE "OrderItem" DROP CONSTRAINT "OrderItem_storeProductId_fkey";

-- DropForeignKey
ALTER TABLE "Store" DROP CONSTRAINT "Store_vendorId_fkey";

-- DropForeignKey
ALTER TABLE "StoreProduct" DROP CONSTRAINT "StoreProduct_productId_fkey";

-- DropForeignKey
ALTER TABLE "StoreProduct" DROP CONSTRAINT "StoreProduct_storeId_fkey";

-- DropForeignKey
ALTER TABLE "VendorStaff" DROP CONSTRAINT "VendorStaff_userId_fkey";

-- DropForeignKey
ALTER TABLE "VendorStaff" DROP CONSTRAINT "VendorStaff_vendorId_fkey";

-- DropIndex
DROP INDEX "CartItem_cartId_storeProductId_key";

-- AlterTable
ALTER TABLE "CartItem" DROP COLUMN "storeProductId",
ADD COLUMN     "productId" TEXT NOT NULL;

-- AlterTable
ALTER TABLE "OrderItem" DROP COLUMN "storeProductId",
ADD COLUMN     "productId" TEXT NOT NULL;

-- AlterTable
ALTER TABLE "Product" ADD COLUMN     "isAvailable" BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN     "price" DECIMAL(12,2) NOT NULL;

-- DropTable
DROP TABLE "Inventory";

-- DropTable
DROP TABLE "Store";

-- DropTable
DROP TABLE "StoreProduct";

-- DropTable
DROP TABLE "Vendor";

-- DropTable
DROP TABLE "VendorStaff";

-- CreateTable
CREATE TABLE "FulfillmentPoint" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "street" TEXT NOT NULL,
    "city" TEXT NOT NULL,
    "state" TEXT NOT NULL,
    "postalCode" TEXT NOT NULL,
    "country" TEXT NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "FulfillmentPoint_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "CartItem_cartId_productId_key" ON "CartItem"("cartId", "productId");

-- AddForeignKey
ALTER TABLE "CartItem" ADD CONSTRAINT "CartItem_productId_fkey" FOREIGN KEY ("productId") REFERENCES "Product"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "OrderItem" ADD CONSTRAINT "OrderItem_productId_fkey" FOREIGN KEY ("productId") REFERENCES "Product"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
