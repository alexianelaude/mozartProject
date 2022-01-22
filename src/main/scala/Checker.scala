package upmc.akka.leader

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import HeartStatuses._
import com.typesafe.config.ConfigFactory


object Checker {

	import Heart._

	case class Check ()
	case class RunElection(musiciansAlive:List[HeartStatus])
	case class AvailableMusicians(musicians:List[Int])

}

class Checker () extends Actor {

	import Checker._
	import Heart._

 	implicit val timeout = Timeout(1.5 seconds)

     val config0 = ConfigFactory.load().getConfig("system0.akka.remote.netty.tcp")
     val config1 = ConfigFactory.load().getConfig("system1.akka.remote.netty.tcp")
     val config2 = ConfigFactory.load().getConfig("system2.akka.remote.netty.tcp")
     val config3 = ConfigFactory.load().getConfig("system3.akka.remote.netty.tcp")

	val heart0 = context.actorSelection("akka.tcp://LeaderSystem0@" + config0.getString("hostname") + ":" + config0.getString("port") + "/user/Node0/Heart")
	val heart1 = context.actorSelection("akka.tcp://LeaderSystem1@" + config1.getString("hostname") + ":" + config1.getString("port") + "/user/Node1/Heart")
	val heart2 = context.actorSelection("akka.tcp://LeaderSystem2@" + config2.getString("hostname") + ":" + config2.getString("port") + "/user/Node2/Heart")
	val heart3 = context.actorSelection("akka.tcp://LeaderSystem3@" + config3.getString("hostname") + ":" + config3.getString("port") + "/user/Node3/Heart")

	val hearts : List[ActorSelection] = List(heart0, heart1, heart2, heart3)

     def receive = {
          case Check => {
               val musiciansAlive = for {
                    h0 <- ask(heart0, CheckLiveness).recover {
                         case _:Throwable => Dead
                         }.mapTo[HeartStatus]
                    h1 <- ask(heart1, CheckLiveness).recover {
                         case _:Throwable => Dead
                         }.mapTo[HeartStatus]
                    h2 <- ask(heart2, CheckLiveness).recover {
                         case _:Throwable => Dead
                         }.mapTo[HeartStatus]  
                    h3 <- ask(heart3, CheckLiveness).recover {
                         case _:Throwable => Dead
                         }.mapTo[HeartStatus] 
               } yield List(h0,h1,h2,h3)
               musiciansAlive.onComplete {
                    case Success(musiciansAlive:List[HeartStatus]) => {
                         if (musiciansAlive.forall( _ != LiveConductor)) {
                              context.parent ! RunElection(musiciansAlive)
                         }
                         var musicians: List[Int] = List()
                         for (i <- 0 to 3) {
                              if (musiciansAlive(i) == LivePlayer) {
                                   musicians = musicians ::: List(i)
                              }
                         }
                         context.parent ! AvailableMusicians(musicians) //Keep node up to date of available musicians (for Conductor)
                         Thread.sleep(1000)
                          receive(Check)
                    }
                    case Failure(e) => {
                         context.stop(self)
                    }
               }
          	         
          }

     }
}