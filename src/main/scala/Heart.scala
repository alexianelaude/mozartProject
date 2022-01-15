package upmc.akka.leader

import akka.actor._
import akka.util.Timeout

object Heart {

  case class CheckLiveness()

  abstract class HeartStatus
  case class LiveConductor() extends HeartStatus
  case class LivePlayer() extends HeartStatus
  case class Dead() extends HeartStatus

  case class ChangeStatus(status:HeartStatus)

}

class Heart () extends Actor {

    import Heart._

    var heartStatus: HeartStatus = LivePlayer()

    def receive = {
      case CheckLiveness => {
        sender ! heartStatus
      }
    }
	
}