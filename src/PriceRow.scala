package spinoff

case class PriceRow(
  spinoffId: String,
  companyName: String,
  tradingDay: Int,
  close: Double
)