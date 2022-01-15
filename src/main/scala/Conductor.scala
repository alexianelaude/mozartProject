package concert

import akka.actor.{Props,  Actor,  ActorRef,  ActorSystem}

 object Conductor {

	case class Conduct ()
 	case class AddMusician(ref:ActorRef)
 	case class GetMeasure(num:Int, counter:Int)
 }

 class Conductor () extends Actor {
 	import Conductor._
 	import ProviderActor._
 	import scala.util.Random

  	val provider = context.actorOf(Props[ProviderActor], name = "Provider")
  	var counter = 0
  	var musicians : List[ActorRef] = List()

 	def receive = {
 		case Conduct => {
 			if (musicians.length >= 3) {
	 			 val dice1 = (new Random).nextInt(5)
	 			 val dice2 = (new Random).nextInt(5)
	 			 println("Rolled dices:" + dice1 + dice2)
	 			 provider ! GetMeasure(dice1 + dice2, counter)
 			}
 			else {
 				println("Insufficient number of musicians, only " + musicians.length)
 				Thread.sleep(10000)
 				receive(Conduct)
 			}
 		} 
 		case Measure(chords:List[Chord]) => {
 			println("Sending Measure")
 			val musicianIndex = (new Random).nextInt(musicians.length - 1)
 			musicians(musicianIndex) ! new Measure(chords) 
 			counter = counter + 1
 			if (counter >= 16) {
 				counter = 0
 			}
 			Thread.sleep(1800)
 			receive(Conduct)
 		}
 		case AddMusician(ref:ActorRef) => {
 			musicians = musicians ::: List(ref)
 		}
 	}
 }
