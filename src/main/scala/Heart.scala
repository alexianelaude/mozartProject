package upmc.akka.leader

import akka.actor._
import akka.util.Timeout

object HeartStatuses extends Enumeration {
  type HeartStatus = Value

  val LiveConductor = Value("Conductor")
  val LivePlayer = Value("Player")
  val Dead = Value("Dead")
}

object Heart {

  import HeartStatuses._

  case class CheckLiveness()
  case class ChangeStatus(status:HeartStatus)

}

class Heart () extends Actor {

    import Heart._
    import HeartStatuses._

    var heartStatus: HeartStatus = LivePlayer

    def receive = {
      case CheckLiveness => {
        sender ! heartStatus
      }
    case ChangeStatus(status:HeartStatus) => {
      println("Changing status from " + heartStatus + " to " + status)
      heartStatus = status
    }
    }
	
}