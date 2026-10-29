package spinoff

import scala.io.Source

object CsvReader:

  /** Converts one data line into a PriceRow. */
  def parseRow(line: String): PriceRow =
    val columns = line.split(",", -1).map(_.trim)
    require(columns.length == 4, "Expected four CSV fields.")

    val row = PriceRow(columns(0), columns(1), columns(2).toInt, columns(3).toDouble)
    require(row.tradingDay >= 1, "Trading day must be positive.")
    require(row.close.isFinite && row.close > 0, "Price must be positive and finite.")
    row

  /** Reads the observations, checking and skipping the header. */
  def readPrices(filePath: String): Vector[PriceRow] =
    val source = Source.fromFile(filePath, "UTF-8")
    val lines =
      try source.getLines().toVector
      finally source.close()

    require(lines.nonEmpty, "The CSV file is empty.")
    require(
      lines.head.trim == "spinoff_id,company_name,trading_day,close_eur",
      "Unexpected CSV header."
    )
    lines.drop(1).map(parseRow)
