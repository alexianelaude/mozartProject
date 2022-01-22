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

class Heart (displayRef:ActorRef) extends Actor {

    import Heart._
    import HeartStatuses._

    var heartStatus: HeartStatus = LivePlayer

    displayRef ! Message("Heart initialized")

    def receive = {
      case CheckLiveness => {
        sender ! heartStatus
      }
      case ChangeStatus(status:HeartStatus) => {
        displayRef ! Message("Changing status from " + heartStatus + " to " + status)
        heartStatus = status
      }
  }
	
}