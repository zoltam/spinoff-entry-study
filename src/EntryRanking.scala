package spinoff

case class EntrySummary(
  entryDay: Int,
  holdingDays: Int,
  companyCount: Int,
  meanReturn: Double,
  medianReturn: Double,
  positiveRate: Double,
  worstReturn: Double
)

object EntryRanking:

  /** Latest entry day that leaves the requested holding period
    * available for every company. */
  def latestPossibleEntry(rows: Vector[PriceRow], holdingDays: Int): Int =
    require(rows.nonEmpty, "No price observations.")
    require(holdingDays >= 1, "Holding period must be positive.")

    val companies = rows.groupBy(_.spinoffId)

    val lastCommonDay =
      companies.values
        .map(companyRows => companyRows.map(_.tradingDay).max)
        .min

    val latestEntry = lastCommonDay - holdingDays

    require(
      latestEntry >= 1,
      "Not enough observations for that holding period."
    )

    latestEntry


  /** Compares entry days 1 through lastEntryDay.
    * Every candidate uses the same companies and holding period.
    */
  def rankEntries(
    rows: Vector[PriceRow], holdingDays: Int, lastEntryDay: Int
  ): Vector[EntrySummary] =
    val latestEntry = latestPossibleEntry(rows, holdingDays)

    require(
      lastEntryDay >= 1 && lastEntryDay <= latestEntry,
      s"Last entry day must be between 1 and $latestEntry."
    )

    val summaries =
      (1 to lastEntryDay).map { entryDay =>
        val paths = EntryStudy.allReturnPaths(rows, entryDay, holdingDays)

        require(paths.nonEmpty, "No company return paths.")

        // Get each company's cumulative return at the end
        // of the selected holding period.
        val finalReturns =
          paths.values.toVector.map { path =>
            path.find(_._1 == holdingDays) match
              case Some((_, returnValue)) => returnValue
              case None =>
                throw new IllegalArgumentException(
                  s"Missing return at $holdingDays days held."
                )
          }

        val sortedReturns = finalReturns.sorted
        val count = sortedReturns.size
        val middle = count / 2

        val median =
          if count % 2 == 0 then
            (sortedReturns(middle - 1) + sortedReturns(middle)) / 2
          else
            sortedReturns(middle)

        EntrySummary(
          entryDay = entryDay,
          holdingDays = holdingDays,
          companyCount = count,
          meanReturn = finalReturns.sum / count,
          medianReturn = median,
          positiveRate = finalReturns.count(_ > 0).toDouble / count,
          worstReturn = sortedReturns.head
        )
      }.toVector

    summaries.sortBy(summary => (-summary.meanReturn, summary.entryDay))
