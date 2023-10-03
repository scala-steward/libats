/*
 * Copyright: Copyright (C) 2016, ATS Advanced Telematic Systems GmbH
 *  License: MPL-2.0
 */
package com.advancedtelematic.libats.slick.db

import com.typesafe.config.Config
import slick.jdbc.MySQLProfile.api._


trait DatabaseSupport {
  val dbConfig: Config

  implicit lazy val db: slick.jdbc.MySQLProfile.backend.Database = Database.forConfig("", dbConfig)

  lazy val dbSource = db.source
}
