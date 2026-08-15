/*
  Warnings:

  - Added the required column `fulfillmentPointId` to the `Order` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE "Order" ADD COLUMN     "fulfillmentPointId" TEXT NOT NULL;

-- AddForeignKey
ALTER TABLE "Order" ADD CONSTRAINT "Order_fulfillmentPointId_fkey" FOREIGN KEY ("fulfillmentPointId") REFERENCES "FulfillmentPoint"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
