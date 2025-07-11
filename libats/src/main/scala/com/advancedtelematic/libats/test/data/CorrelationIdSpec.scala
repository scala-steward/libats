package com.advancedtelematic.libats.test.data

import java.util.UUID
import com.advancedtelematic.libats.data.DataType.{
  CorrelationId,
  MultiTargetUpdateCorrelationId
}
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec

class CorrelationIdSpec extends AnyPropSpec with Matchers with EitherValues {

  property("should convert a MultiTargetUpdateId to string") {
    val uuid = "6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
    val cid = MultiTargetUpdateCorrelationId(UUID.fromString(uuid))
    cid.toString shouldBe "urn:here-ota:mtu:6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
  }

  property("should parse a string as a MultiTargetUpdateId") {
    val uuid = "6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
    val s = s"urn:here-ota:mtu:$uuid"
    val cid = CorrelationId.fromString(s)
    cid.value shouldBe MultiTargetUpdateCorrelationId(UUID.fromString(uuid))
  }

  property("should fail with an invalid correlationId string") {
    val uuid = "6a65f47f-5258-4adc-aa20-d8eda0d5a6e2"
    val s = s"invalid:here-ota:campaign:$uuid"
    val cid = CorrelationId.fromString(s)
    cid.left.value shouldBe s"Invalid correlationId: '$s'."
  }
}
