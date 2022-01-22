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
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import com.typesafe.config.ConfigFactory


 object Conductor {

	case class Conduct ()
	case class StartConductor ()
	case class RequestMusicians ()

	case class ConductorStoppedException() extends Exception
	case class NoMusiciansException() extends Exception
 }

 class Conductor (id:Int, displayRef:ActorRef) extends Actor {
 	import Conductor._
 	import ProviderActor._
 	import DataBaseActor._
 	import Checker._
 	import scala.util.Random
 	import HeartStatuses._

 	implicit val resolveTimeout = Timeout(5 seconds)

  	val provider = context.actorOf(Props[ProviderActor], name = "Provider")
  	var counter = 0

  	final override val supervisorStrategy = OneForOneStrategy() { 
     	case _: ProviderFailedException => Restart 
	}

 	def receive = {
 		case StartConductor => {
  			displayRef ! Message("Conductor waiting for musicians")
 			Thread.sleep(10000)
 			val future = context.parent ? RequestMusicians
 			future.onComplete {
 				case Success(AvailableMusicians(musicians:List[Int])) => {
 					if (musicians.length >= 1) receive(Conduct)
 					else stopActor("No available musicians found")
 				}
 				case Success(a:Any) => stopActor("Received " + a + " instead of musicians list")
 				case Failure(e) => stopActor("Failed to retrieve musicians list")
 			}
 		}
 		case Conduct => {
 			val futureMusicians = context.parent ? RequestMusicians
 			val dice1 = (new Random).nextInt(6)
 			val dice2 = (new Random).nextInt(6)
 			displayRef ! Message("Rolled dices:" + dice1 + dice2)
 			val futureMeasure = provider ? GetMeasure(dice1 + dice2, counter)
 			try {
 				futureMusicians.onComplete{
	 				case Success(AvailableMusicians(musicians:List[Int])) => {
	 					if (musicians.isEmpty) {
		 					displayRef ! Message("Received empty musicians")
							throw NoMusiciansException()
						}
						else {
							val rand = (new Random).nextInt(musicians.length)
							val playerIdx = musicians(rand).toString
							val playerConfig = ConfigFactory.load().getConfig("system" + playerIdx + ".akka.remote.netty.tcp")
							val player = context.actorSelection("akka.tcp://LeaderSystem" + playerIdx + "@" + playerConfig.getString("hostname") 
								+ ":" + playerConfig.getString("port") + "/user/Node" + playerIdx + "/Player")
							displayRef ! Message("Chose node " + playerIdx + " as player")
							futureMeasure.onComplete {
								case Success(measure:Measure) => {
									player ! measure
				 					counter = counter + 1
							 		if (counter >= 16) {
							 			counter = 0
							 		}
							 		Thread.sleep(1800)
	 								receive(Conduct)
								}
								case Success(a) => 
									{
										stopActor("Received " + a + " instead of measure")
									}
								case Failure(e) => {
									stopActor("Retrieving measure failed with " + e)
								}
							}		
						}
					}
 					case Success(a) => throw NoMusiciansException()
 					case Failure(e) => throw NoMusiciansException()
 				}
 			} catch {
 			 	case NoMusiciansException() => {
	 				displayRef ! Message("No available musicians found, waiting")
				 	Thread.sleep(10000)
				 	val future = context.parent ? RequestMusicians
				 	future.onComplete {
				 		case Success(AvailableMusicians(musicians:List[Int])) => {
			 				if (musicians.isEmpty) {
			 					stopActor("No available musicians found")
			 				}
			 				else {
			 					receive(Conduct)
			 				}
				 		}
				 		case Success(a) => stopActor("Received " + a + " instead of musicians list")
				 		case Failure(e) => stopActor("Failed to retrieve musicians list")
 					}
 				}
 			}		
 		}
 	}

 	def stopActor(e:String) : Unit = {
 		displayRef ! Message(e + ", stopping conductor")
 		throw ConductorStoppedException()
 		context.stop(self)
 	}

 }
