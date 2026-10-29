package spinoff

object EntryStudy:

  /** Calculates currentPrice / entryPrice - 1. */
  def priceReturn(entryPrice: Double, currentPrice: Double): Double =
    require(
      entryPrice.isFinite && currentPrice.isFinite && entryPrice > 0 && currentPrice > 0,
      "Prices must be positive and finite."
    )
    currentPrice / entryPrice - 1

  /** Calculates one company's cumulative returns, including day zero. */
  def returnPath(
    companyRows: Vector[PriceRow], entryDay: Int, holdingDays: Int
  ): Vector[(Int, Double)] =
    require(entryDay >= 1 && holdingDays >= 0, "Invalid entry day or holding period.")
    val sorted = companyRows.sortBy(_.tradingDay)
    require(sorted.map(_.tradingDay).distinct.size == sorted.size, "Duplicate trading days.")

    val entry = sorted.find(_.tradingDay == entryDay).getOrElse {
      throw new IllegalArgumentException(s"Missing entry day $entryDay.")
    }

    (0 to holdingDays).toVector.map { day =>
      val current = sorted.find(_.tradingDay == entryDay + day).getOrElse {
        throw new IllegalArgumentException(s"Missing trading day ${entryDay + day}.")
      }
      (day, priceReturn(entry.close, current.close))
    }

  /** Groups observations by company and calculates each return path. */
  def allReturnPaths(
    rows: Vector[PriceRow], entryDay: Int, holdingDays: Int
  ): Map[String, Vector[(Int, Double)]] =
    require(rows.nonEmpty, "Dataset is empty.")
    val companies = rows.groupBy(_.spinoffId)
    companies.map { case (id, companyRows) =>
      (id, returnPath(companyRows, entryDay, holdingDays))
    }

  /** Averages company returns at matching holding days. */
  def meanReturnPath(paths: Map[String, Vector[(Int, Double)]]): Vector[(Int, Double)] =
    require(paths.nonEmpty && paths.values.forall(_.nonEmpty), "Return paths cannot be empty.")
    val expectedDays = paths.values.head.map(_._1).sorted
    require(
      paths.values.forall(path => path.map(_._1).sorted == expectedDays),
      "All companies must have the same holding days."
    )

    val points = paths.values.toVector.flatten
    val grouped = points.groupBy(_._1)
    grouped.toVector.map { case (day, returns) =>
      val average = returns.map(_._2).sum / returns.size
      (day, average)
    }.sortBy(_._1)
