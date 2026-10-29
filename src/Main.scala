package spinoff

import scala.io.StdIn
import scala.util.control.NonFatal

object Main:

  def main(args: Array[String]): Unit =
    try
      val rows = CsvReader.readPrices("data/real_spinoff_prices.csv")
      val companyCount = rows.map(_.spinoffId).distinct.size

      println(s"Loaded $companyCount companies.")
      println("Compare entry days using a fixed holding period.")

      val holdingDays = readInt("How many trading days will each purchase be held? ")
      val latestEntry = EntryRanking.latestPossibleEntry(rows, holdingDays)
      val lastEntryDay = readInt(s"Test entry days 1 through which day? (1–$latestEntry): ")
      val ranking = EntryRanking.rankEntries(rows, holdingDays, lastEntryDay)
      val baseline = ranking.find(_.entryDay == 1).get
      val best = ranking.head

      println(s"\nDay-1 baseline — holding for $holdingDays trading days")
      printSummaryHeader()
      printSummary(baseline, baseline.meanReturn)

      println(s"\nTop entry days — holding for $holdingDays trading days")
      printSummaryHeader()
      ranking.take(5).foreach(summary => printSummary(summary, baseline.meanReturn))
      println("Vs day 1 is the difference in average return, in percentage points (pp).")
      println("All entries use the same holding period; their exit trading days differ.")

      println(s"\nBest entry in this sample: trading day ${best.entryDay}")
      println(s"Companies included: ${best.companyCount}")

      val bestPaths = EntryStudy.allReturnPaths(rows, best.entryDay, holdingDays)
      val baselinePaths = EntryStudy.allReturnPaths(rows, 1, holdingDays)
      val meanPath = EntryStudy.meanReturnPath(bestPaths)
      val baselineMeanPath = EntryStudy.meanReturnPath(baselinePaths).toMap
      val checkpoints = (Vector(0, 1, 5, 10, 20, 40, 60) :+ holdingDays)
        .filter(_ <= holdingDays).distinct.sorted

      println("\nAverage cumulative returns after each entry:")
      println("Days held   Best entry       Day 1     Difference")
      println("------------------------------------------------")
      meanPath.filter(point => checkpoints.contains(point._1)).foreach {
        case (daysHeld, returnValue) =>
          val baselineReturn = baselineMeanPath(daysHeld)
          val difference = (returnValue - baselineReturn) * 100
          println(
            f"$daysHeld%9d  ${returnValue * 100}%9.2f%%" +
            f"  ${baselineReturn * 100}%9.2f%%  $difference%+9.2f pp"
          )
      }

    catch
      case NonFatal(error) =>
        println(s"Could not complete the study: ${error.getMessage}")

  def printSummaryHeader(): Unit =
    println("Entry day    Average     Median   Positive      Worst    Vs day 1")
    println("---------------------------------------------------------------")

  def printSummary(summary: EntrySummary, baselineReturn: Double): Unit =
    val difference = (summary.meanReturn - baselineReturn) * 100
    println(
      f"${summary.entryDay}%9d  ${summary.meanReturn * 100}%8.2f%%" +
      f"  ${summary.medianReturn * 100}%8.2f%%  ${summary.positiveRate * 100}%7.1f%%" +
      f"  ${summary.worstReturn * 100}%8.2f%%  $difference%+8.2f pp"
    )

  def readInt(prompt: String): Int =
    val input = StdIn.readLine(prompt)
    require(input != null, "No input received.")
    input.trim.toIntOption.getOrElse {
      throw new IllegalArgumentException("Please enter a whole number.")
    }
