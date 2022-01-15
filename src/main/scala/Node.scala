package upmc.akka.leader

import akka.actor._

case class Start ()

class Node (val id:Int) extends Actor {

	import Checker._
	import Heart._
	import PlayerActor._
	import Conductor._

     // Les differents acteurs du systeme
     val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")
     val checker = context.actorOf(Props[Checker], name = "Checker")
     val conductor = context.actorOf(Props(classOf[Conductor], id), name = "Conductor")
     val player = context.actorOf(Props[PlayerActor], name = "Player")
     val heart = context.actorOf(Props[Heart], name = "Heart")

     def receive = {

          // Initialisation
          case Start => {
               displayActor ! Message ("Node " + this.id + " is created")  
               checker ! Check
            }
          case RunElection(musiciansAlive:List[Heart]) => {
          	//Election protocol: the node with the lowest id currently running becomes conductor
          	//All Nodes check whether they have become conductor, and start conducting if needed
          		var conductorId = -1
          		var i = 0
          		while (i <= 3 && conductorId < 0) {
          			if (musiciansAlive(i) == LivePlayer()) {
          				conductorId = i
          			}
          			i = i + 1
          		}
          		if (i == id) {
          			// Node becomes conductor
          			conductor ! Conduct
          			heart ! ChangeStatus(LiveConductor())
          		}
          }

     }
}
