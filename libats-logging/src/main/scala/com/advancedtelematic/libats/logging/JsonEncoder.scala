package com.advancedtelematic.libats.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.pattern.{
  TargetLengthBasedClassNameAbbreviator,
  ThrowableProxyConverter
}
import ch.qos.logback.classic.spi.ILoggingEvent
import io.circe.syntax._
import io.circe.{Encoder, Json}
import java.util.Date
import java.time.Instant
import scala.jdk.CollectionConverters._

class JsonEncoder
    extends ch.qos.logback.core.encoder.EncoderBase[ILoggingEvent] {
  private var includeContext = false
  private var includeThread = false
  private var includeMdc = false
  private var includeHttpQuery = false
  private var prettyPrint = false
  private var loggerLength = 36
  private var msgIsJson = false

  private val throwableProxyConverter = new ThrowableProxyConverter
  private val abbreviator = new TargetLengthBasedClassNameAbbreviator(
    loggerLength)

  implicit private val levelEncoder: Encoder[Level] =
    Encoder.encodeString.contramap(_.toString)

  def setLoggerLength(value: Int): Unit = loggerLength = 36

  def setIncludeContext(value: Boolean): Unit = includeContext = value

  def setIncludeThread(value: Boolean): Unit = includeThread = value

  def setPrettyPrint(value: Boolean): Unit = prettyPrint = value

  def setIncludeMdc(value: Boolean): Unit = includeMdc = value

  def setIncludeQuery(value: Boolean): Unit = includeHttpQuery = value

  def setMsgIsJson(value: Boolean): Unit = msgIsJson = value

  override def start(): Unit = {
    throwableProxyConverter.start()
    super.start()
  }

  override def stop(): Unit = {
    throwableProxyConverter.stop()
    super.stop()
  }

  private def formatMsgJson(msg: String): Json =
    if (msgIsJson)
      io.circe.jawn.parse(msg).toOption.getOrElse(msg.asJson)
    else
      msg.asJson

  override def encode(event: ILoggingEvent): Array[Byte] = {
    val mdc = event.getMDCPropertyMap.asScala.view.mapValues(_.asJson).toMap

    val map = Map[String, Json](
      "at" -> Instant.ofEpochMilli(event.getTimeStamp).asJson,
      "level" -> event.getLevel.asJson,
      "logger" -> abbreviator.abbreviate(event.getLoggerName).asJson,
      "msg" -> formatMsgJson(event.getFormattedMessage)
    ).withValue(includeContext,
                 "ctx" -> event.getLoggerContextVO.toString.asJson)
      .withValue(includeThread, "thread" -> event.getThreadName.asJson)
      .withValue(includeMdc, "mdc" -> mdc.asJson)
      .withValue("throwable" -> encodeThrowable(event))
      .maybeWithValue(
        "logger_service_name" -> AtsLayoutBase
          .svcName(event.getLoggerName)
          .map(_.asJson)
      )

    val mdcMap = mdc.view
      .filterKeys { key =>
        key != "http_query" || includeHttpQuery
      }
      .filterKeys { key =>
        !key.startsWith("akka")
      }

    val withKeyValues = Option(event.getKeyValuePairs).toList
      .flatMap(_.asScala)
      .map { pair =>
        pair.key -> anyToJson(pair.value)
      }
      .toMap ++ map ++ mdcMap

    val str =
      if (prettyPrint) withKeyValues.asJson.spaces2
      else withKeyValues.asJson.noSpaces

    (str + "\n").getBytes
  }

  private def anyToJson(input: Any): Json = input match {
    case str: String      => Json.fromString(str)
    case num: Int         => Json.fromInt(num)
    case num: Long        => Json.fromLong(num)
    case bool: Boolean    => Json.fromBoolean(bool)
    case date: Date       => Json.fromString(date.toInstant.toString)
    case instant: Instant => Json.fromString(instant.toString)
    case json: Json       => json
    case other            => Json.fromString(other.toString)
  }

  protected def encodeThrowable(value: ILoggingEvent): Json = {
    val maybeEx = Option(throwableProxyConverter.convert(value)).filter(_ != "")
    maybeEx.map(_.asJson).getOrElse(Json.Null)
  }

  override def headerBytes(): Array[Byte] = null

  override def footerBytes(): Array[Byte] = null

  implicit private class MapJsonOps(map: Map[String, Json]) {

    def maybeWithValue(value: => (String, Option[Json])): Map[String, Json] =
      value match {
        case (key, Some(v)) =>
          map + (key -> v)
        case _ =>
          map
      }

    def withValue(enabled: Boolean,
                  value: => (String, Json)): Map[String, Json] =
      if (enabled)
        map + value
      else
        map

    def withValue(value: => (String, Json)): Map[String, Json] = value match {
      case (_, v) if v != Json.Null && v != Json.obj() => map + value
      case _                                           => map
    }

  }

}
