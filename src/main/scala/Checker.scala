package upmc.akka.leader

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.Await
import scala.util.Failure
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import HeartStatuses._


object Checker {

	import Heart._

	case class Check ()
	case class RunElection(musiciansAlive:Array[HeartStatus])
	case class AvailableMusicians()

}

class Checker () extends Actor {

	import Checker._
	import Heart._

 	implicit val timeout = Timeout(3 seconds)

	var musiciansAlive : Array[HeartStatus] = Array(Dead, Dead, Dead, Dead)

	val heart0 = context.actorSelection("akka.tcp://LeaderSystem0@127.0.0.1:6000/user/Node0/Heart")
	val heart1 = context.actorSelection("akka.tcp://LeaderSystem1@127.0.0.1:6001/user/Node1/Heart")
	val heart2 = context.actorSelection("akka.tcp://LeaderSystem2@127.0.0.1:6002/user/Node2/Heart")
	val heart3 = context.actorSelection("akka.tcp://LeaderSystem3@127.0.0.1:6003/user/Node3/Heart")

	val hearts : List[ActorSelection] = List(heart0, heart1, heart2, heart3)

     def receive = {
          // Initialisation
          case Check => {
          	   for (i <- 0 to 3) {
          	   		var life = hearts(i) ? CheckLiveness
          	   		life.onComplete {
          	   			case Success(status:HeartStatus) => {
          	   				println("Found musician " + i.toString + " with status " + status)
          	   				musiciansAlive(i) =  status
          	   			}
          	   			case Failure(e) => musiciansAlive(i) = Dead
          	   		}
          	   }
          	   if (musiciansAlive.forall( _ != LiveConductor)) {
          	   	  context.parent ! RunElection(musiciansAlive)
          	   }
      		   Thread.sleep(10000)
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