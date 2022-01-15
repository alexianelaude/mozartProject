package upmc.akka.leader

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.Await
import scala.util.Failure
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


object Checker {

	import Heart._

	case class Check ()
	case class RunElection(musiciansAlive:List[HeartStatus])
	case class AvailableMusicians()

}

class Checker () extends Actor {

	import Checker._
	import Heart._

 	implicit val timeout = Timeout(3 seconds)

	var musiciansAlive : List[HeartStatus] = List(Dead(), Dead(), Dead(), Dead())

	val heart0 = Await.result(context.actorSelection("akka.tcp://LeaderSystem0@10.0.0.1:6000/user/Heart")
		.resolveOne()(timeout), timeout.duration)
	val heart1 = Await.result(context.actorSelection("akka.tcp://LeaderSystem1@10.0.0.1:6001/user/Heart")
		.resolveOne()(timeout), timeout.duration)
	val heart2 = Await.result(context.actorSelection("akka.tcp://LeaderSystem2@10.0.0.1:6002/user/Heart")
		.resolveOne()(timeout), timeout.duration)
	val heart3 = Await.result(context.actorSelection("akka.tcp://LeaderSystem3@10.0.0.1:6003/user/Heart")
		.resolveOne()(timeout), timeout.duration)

	val hearts : List[ActorRef] = List(heart0, heart1, heart2, heart3)

     def receive = {
          // Initialisation
          case Check => {
          	   for (i <- 0 to 3) {
          	   		var life = hearts(i) ? CheckLiveness
          	   		life.onComplete {
          	   			case Success(status:HeartStatus) => musiciansAlive.updated(i, status)
          	   			case Failure(e) => musiciansAlive.updated(i, Dead())
          	   		}
          	   }
          	   if (musiciansAlive.forall( _ != LiveConductor)) {
          	   	  context.parent ! RunElection(musiciansAlive)
          	   }
      		   Thread.sleep(1000)
               receive(Check)         
          }
         case AvailableMusicians => {
         	var musicians: List[Int] = List()
         	for (i <- 0 to 3) {
         		if (musiciansAlive(i) == LivePlayer) {
         			musicians = musicians ::: List(i)
         		}
         	}
         	sender ! musicians
         }

     }
}