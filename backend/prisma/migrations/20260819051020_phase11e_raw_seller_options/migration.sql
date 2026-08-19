-- DropIndex
DROP INDEX "CartItem_cartId_productId_key";

-- AlterTable
ALTER TABLE "CartItem" ADD COLUMN     "cutOptionId" TEXT,
ADD COLUMN     "weightOptionId" TEXT;

-- AlterTable
ALTER TABLE "OrderItem" ADD COLUMN     "selectedCut" TEXT,
ADD COLUMN     "selectedWeight" TEXT;

-- CreateTable
CREATE TABLE "CutOption" (
    "id" TEXT NOT NULL,
    "productId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "isAvailable" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "CutOption_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "WeightOption" (
    "id" TEXT NOT NULL,
    "productId" TEXT NOT NULL,
    "weightLabel" TEXT NOT NULL,
    "priceOverride" DECIMAL(12,2) NOT NULL,
    "isAvailable" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "WeightOption_pkey" PRIMARY KEY ("id")
);

-- AddForeignKey
ALTER TABLE "CutOption" ADD CONSTRAINT "CutOption_productId_fkey" FOREIGN KEY ("productId") REFERENCES "Product"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "WeightOption" ADD CONSTRAINT "WeightOption_productId_fkey" FOREIGN KEY ("productId") REFERENCES "Product"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "CartItem" ADD CONSTRAINT "CartItem_cutOptionId_fkey" FOREIGN KEY ("cutOptionId") REFERENCES "CutOption"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "CartItem" ADD CONSTRAINT "CartItem_weightOptionId_fkey" FOREIGN KEY ("weightOptionId") REFERENCES "WeightOption"("id") ON DELETE SET NULL ON UPDATE CASCADE;
