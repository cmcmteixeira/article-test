package elevio.common.model

import net.ceedubs.ficus.readers.{StringReader, ValueReader}

case class JWT(value: String) extends AnyVal

object JWT {
  implicit val valueReader: ValueReader[JWT] = StringReader.stringValueReader.map(JWT(_))
}
