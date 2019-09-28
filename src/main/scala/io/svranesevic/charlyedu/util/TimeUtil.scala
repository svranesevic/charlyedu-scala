package io.svranesevic.charlyedu.util

import java.time.{ LocalDate, LocalTime, ZoneId, ZonedDateTime }

object TimeUtil {

  def daysBetween(inclusiveFrom: ZonedDateTime, inclusiveTo: ZonedDateTime): List[ZonedDateTime] =
    inclusiveFrom.toLocalDate.toEpochDay
      .until(inclusiveTo.plusDays(1).toLocalDate.toEpochDay)
      .map(LocalDate.ofEpochDay)
      .map(ZonedDateTime.of(_, LocalTime.MIDNIGHT, ZoneId.of("UTC")))
      .toList
}
