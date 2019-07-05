package elevio.common.model

import net.ceedubs.ficus.readers.{StringReader, ValueReader}

case class ApiKey(value: String) extends AnyVal

object ApiKey {
  implicit val valueReader: ValueReader[ApiKey] = StringReader.stringValueReader.map(ApiKey(_))
}
