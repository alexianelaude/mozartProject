package upmc.akka.leader

import akka.actor.{Props,  Actor,  ActorRef,  ActorSystem}
import akka.actor.ActorKilledException
import akka.pattern.ask
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global


 object Conductor {

	case class Conduct ()
	case class StartConductor ()
	case class RequestMusicians ()
 }

 class Conductor (id:Int) extends Actor {
 	import Conductor._
 	import ProviderActor._
 	import DataBaseActor._
 	import Checker._
 	import scala.util.Random
 	import HeartStatuses._

 	implicit val resolveTimeout = Timeout(5 seconds)

  	val provider = context.actorOf(Props[ProviderActor], name = "Provider")
  	val future_checker = context.actorSelection("akka.tcp://LeaderSystem" + id.toString + "@127.0.0.1:600" + id.toString + "/user/Node" + id.toString + "/Checker")
  	var counter = 0

 	def receive = {
 		case StartConductor => {
 			Thread.sleep(10000)
 			println("Conductor looking for musicians")
 			val future = context.parent ? RequestMusicians
 			future.onComplete {
 				case Success(musicians:List[Int]) => {
 					if (musicians.length >= 1) receive(Conduct)
 					else context.stop(self)
 				}
 				case Failure(e) => context.stop(self)
 			}
 		}
 		case Conduct => {
 			val dice1 = (new Random).nextInt(5)
 			val dice2 = (new Random).nextInt(5)
 			println("Rolled dices:" + dice1 + dice2)
 			provider ! GetMeasure(dice1 + dice2, counter)
 			Thread.sleep(1800)
 			receive(Conduct)
 		}
 		 
 		case Measure(chords:List[Chord]) => {
 			val future = context.parent ? RequestMusicians
 			future.onComplete {
 				case Success(musicians:List[Int]) => {
 					if (musicians.isEmpty){
 						println("No available musicians found")
 					}
 					else {
	 					val rand = (new Random).nextInt(musicians.length - 1)
	 					val playerIdx = musicians(rand).toString
	 					val player = context.actorSelection("akka.tcp://LeaderSystem" + playerIdx + "@127.0.0.1:600" + playerIdx + "/user/Node" + playerIdx + "/Player")
	 					player ! new Measure(chords) 
			 			counter = counter + 1
			 			if (counter >= 16) {
			 				counter = 0
			 			}
		 			}
		 		}
          	   	case Failure(e) => println("No available musicians found")
 			}
 			
 			Thread.sleep(1800)
 			receive(Conduct)
 		}
 	}
 }
