/**
 * Copyright: Copyright (C) 2016, Jaguar Land Rover
 * License: MPL-2.0
 */
package com.advancedtelematic.libats.data

import java.util.UUID
import io.circe.{Decoder, Encoder, Json}


/**
  * Errors are presented to the user of the core and resolver API as
  * JSON objects, this is done semi-automatically by the Circe library.
  *
  * @see {@link https://github.com/travisbrown/circe}
  */

object ErrorCodes {
  val InvalidEntity = ErrorCode("invalid_entity")
  val MissingEntity = ErrorCode("missing_entity")
  val ConflictingEntity = ErrorCode("conflicting_entity")
  val TooManyElements = ErrorCode("too_many_elements")
  val InternalServerError = ErrorCode("internal_server_error")
  val RemoteServiceError = ErrorCode("remote_service_error")
}

case class ErrorRepresentation(code: ErrorCode, description: String, cause: Option[Json] = None, errorId: Option[UUID] = Some(UUID.randomUUID()))

object ErrorRepresentation {
  import io.circe.generic.semiauto.*
  implicit val encoderInstance: Encoder[ErrorRepresentation] = deriveEncoder[ErrorRepresentation]
  implicit val decoderInstance: Decoder[ErrorRepresentation] = deriveDecoder[ErrorRepresentation]

  trait ToErrorRepr[T] {
    def apply(value: T): ErrorRepresentation
  }

  implicit class ToErrorReprOps[E: ToErrorRepr](value: E) {
    def toErrorRepr: ErrorRepresentation = implicitly[ToErrorRepr[E]].apply(value)
  }

}

case class ErrorCode(code: String) extends AnyVal

object ErrorCode {
  implicit val encoderInstance : Encoder[ErrorCode] = Encoder[String].contramap( _.code )
  implicit val decoderInstance : Decoder[ErrorCode] = Decoder[String].map( ErrorCode.apply )
}
