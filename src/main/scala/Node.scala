package upmc.akka.leader

import akka.actor._
import akka.actor.{ActorInitializationException , ActorKilledException, OneForOneStrategy} 
import akka.actor.SupervisorStrategy._

case class Start ()

class Node (val id:Int) extends Actor {

	import Checker._
	import HeartStatuses._
	import Heart._
	import PlayerActor._
	import Conductor._

     // Les differents acteurs du systeme
     val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")
     val heart = context.actorOf(Props(classOf[Heart], displayActor), name = "Heart")
     val checker = context.actorOf(Props[Checker], name = "Checker")
     val conductor = context.actorOf(Props(classOf[Conductor], id, displayActor), name = "Conductor")
     val player = context.actorOf(Props(classOf[PlayerActor], displayActor), name = "Player")

     var availableMusicians : List[Int] = List()

     final override val supervisorStrategy = OneForOneStrategy() { 
     	case _: ActorInitializationException => Restart 
     	case _: ActorKilledException => Stop
     	case _: ConductorStoppedException => Stop
		case _: Exception => Restart 
		case _ => Escalate
	}


     def receive = {

          // Initialisation
          case Start => {
               displayActor ! Message ("Node " + this.id + " is created")
               checker ! Check
            }
          case RunElection(musiciansAlive:List[HeartStatus]) => {
          	//Election protocol: the node with the lowest id currently running becomes conductor
          	//All Nodes check whether they have become conductor, and start conducting if needed
          		displayActor ! Message ("Beginning conductor election, with musicians' status: " + musiciansAlive.mkString(", "))
          		var conductorId = -1
          		var i = -1
          		while (i <= 2 && conductorId < 0) {
          			i = i + 1
          			if (musiciansAlive(i) == LivePlayer) {
          				conductorId = i
          			}
          		}
          		if (i == id) {
          			// Node becomes conductor
                         heart ! ChangeStatus(LiveConductor)
          			displayActor ! Message("Node " + this.id + " has been elected conductor")
          			conductor ! StartConductor
          		}
          	}
          case AvailableMusicians(musicians:List[Int]) => {
          		availableMusicians = musicians
          }
	      case RequestMusicians => {
	      	displayActor ! Message("Sending avaible musicians: " + availableMusicians.mkString(", "))
	      	sender ! AvailableMusicians(availableMusicians)
	      }
     }
}
