package upmc.akka.leader

import akka.actor.{Props,  Actor,  ActorRef,  ActorSystem}
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
 }

 class Conductor (id:Int) extends Actor {
 	import Conductor._
 	import ProviderActor._
 	import DataBaseActor._
 	import Checker._
 	import scala.util.Random

 	implicit val resolveTimeout = Timeout(3 seconds)

  	val provider = context.actorOf(Props[ProviderActor], name = "Provider")
  	val future_checker = context.actorSelection("akka.tcp://LeaderSystem" + id.toString + "@10.0.0.1:600" + id.toString + "/user/Checker")
  	val checker = Await.result(future_checker.resolveOne()(resolveTimeout), resolveTimeout.duration)
  	var counter = 0

 	def receive = {
 		case Conduct => {
 			val dice1 = (new Random).nextInt(5)
 			val dice2 = (new Random).nextInt(5)
 			println("Rolled dices:" + dice1 + dice2)
 			provider ! GetMeasure(dice1 + dice2, counter)
 			Thread.sleep(10000)
 			receive(Conduct)
 		}
 		 
 		case Measure(chords:List[Chord]) => {
 			val future = checker ? AvailableMusicians
 			future.onComplete {
 				case Success(musicians:List[Int]) => {
 					val rand = (new Random).nextInt(musicians.length - 1)
 					val playerIdx = musicians(rand).toString
 					val player = context.actorSelection("akka.tcp://LeaderSystem" + playerIdx + "@10.0.0.1:600" + playerIdx + "/user/Player")
 					player ! new Measure(chords) 
		 			counter = counter + 1
		 			if (counter >= 16) {
		 				counter = 0
		 			}
		 		}
          	   	case Failure(e) => println("No available musicians found")
 			}
 			
 			Thread.sleep(1800)
 			receive(Conduct)
 		}
 	}
 }
