package bao.ho.repo

import bao.ho.newtypes.NewTypes.{LanguageCode, ProductId, ProductName}
import bao.ho.models.Product
import fs2._

trait Repository[F[_]] {
  def loadProduct(id: ProductId): F[Seq[(ProductId, LanguageCode, ProductName)]]

  def loadProducts(): Stream[F, (ProductId, LanguageCode, ProductName)]

  def saveProduct(p: Product): F[Int]

  def updateProduct(p: Product): F[Int]
}