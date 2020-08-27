package bao.ho.repo

import java.util.UUID

import bao.ho.newtypes.NewTypes.{LanguageCode, ProductId, ProductName}
import bao.ho.models.Product
import cats.effect.Sync
import doobie._
import io.estatico.newtype.Coercible
import doobie.postgres.implicits._
//import doobie.refined.implicits._
//import eu.timepit.refined.auto._
import fs2.Stream
import doobie.implicits._

final class DoobieRepository[F[_]: Sync](tx: Transactor[F]) extends Repository[F] {
  implicit def newTypePut[R, N](implicit ev: Coercible[Put[R], Put[N]], R: Put[R]): Put[N] = ev(R)

  implicit def newTypeRead[R, N](implicit ev: Coercible[Read[R], Read[N]], R: Read[R]): Read[N] =
    ev(R)

  type NameInfo = (UUID, String, String)
//  implicit val write: doobie.Write[Product] =
//    doobie.Write[(UUID, String, String)].contramap(p => (p.id.value, ))

  /**
    * Load a product from the database repository.
    *
    * @param id The unique ID of the product.
    * @return A list of database rows for a single product which you'll need to combine.
    */
  override def loadProduct(id: ProductId): F[Seq[(ProductId, LanguageCode, ProductName)]] = ???
//    sql"""SELECT products.id, names.lang_code, names.name
//          FROM products
//          JOIN names ON products.id = names.product_id
//          WHERE products.id = $id"""
//      .query[(ProductId, LanguageCode, ProductName)]
//      .to[List]
//      .transact(tx)

  /**
    * Load all products from the database repository.
    *
    * @return A stream of database rows which you'll need to combine.
    */
  override def loadProducts(): Stream[F, (ProductId, LanguageCode, ProductName)] = ???
//    sql"""SELECT products.id, names.lang_code, names.name
//        FROM products
//        JOIN names ON products.id = names.product_id
//        ORDER BY products.id"""
//      .query[(ProductId, LanguageCode, ProductName)]
//      .stream
//      .transact(tx)

  /**
    * Save the given product in the database.
    *
    * @param p A product to be saved.
    * @return The number of affected database rows (product + translations).
    */
  override def saveProduct(p: Product): F[Int] = {
    val namesSql    = "INSERT INTO names (product_id, lang_code, name) VALUES (?, ?, ?)"
    val namesValues = p.names.map(t => (p.id.value, t.lang.value.value, t.name.value.value))
    val id          = p.id.value.toString
    val program = for {
      pi <- sql"INSERT INTO products (id) VALUES($id)".update.run
      ni <- Update[NameInfo](namesSql).updateMany(namesValues)
    } yield pi + ni
    program.transact(tx)
  }

  /**
    * Update the given product in the database.
    *
    * @param p The product to be updated.
    * @return The number of affected database rows.
    */
  override def updateProduct(p: Product): F[Int] = {
    val namesSql = "INSERT INTO names (product_id, lang_code, name) VALUES (?, ?, ?)"
    val namesValues =
      p.names.map[NameInfo](t => (p.id.value, t.lang.value.value, t.name.value.value))
    val id = p.id.value.toString
    val program = for {
      dl <- sql"DELETE FROM names WHERE product_id = ${id}".update.run
      ts <- Update[NameInfo](namesSql).updateMany(namesValues)
    } yield dl + ts
    program.transact(tx)
  }
}