package deployment

import IOPackage._
import oscar.cp.modeling._
import oscar.cp.core._
import oscar.util._
//import akka.actor._
import akka.actor.Address
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.AddressFromURIString
import akka.actor.Props
import akka.actor.Deploy
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.pattern.AskableActorSelection
import akka.remote.RemoteScope;

case class Link(output_actor: Int, output_nr: Int, input_actor: Int, input_nr: Int, flow: Int)

case class ActorInfo(nr_actors: Int, actorClass: Array[String], placement: List[(Int, Address)], links: List[Link], args: Array[Option[Any]])

case class DeviceInfo(nr_nodes: Int, deviceAddress: Array[Address], latencyMatrix: Array[Array[Int]])

case class RegisterLinks(links: List[Link], refs: Array[ActorRef]) {
  links.foreach { link => 
    refs(link.output_actor) ! RegisterOutput(link.output_nr, link.input_nr, refs(link.input_actor))
    refs(link.input_actor) ! RegisterInput(link.input_nr, refs(link.output_actor))
  }
}
case class Init(addrs: Array[Address], latencyMatrix: Array[Array[Int]])
case class AddActorGraph(actorinfo:ActorInfo)
case class Schedule(actorinfo: ActorInfo, deviceinfo: DeviceInfo)


class Scheduler extends Actor with ActorLogging {
  def receive = {
    case Schedule(actorinfo: ActorInfo, deviceinfo: DeviceInfo) =>
      var result = Algorithm(actorinfo, deviceinfo)
      sender ! result
  }
}

case class Algorithm(actorinfo: ActorInfo, deviceinfo: DeviceInfo) extends CPModel {
      
  val x = (0 until actorinfo.nr_actors) map (v => CPIntVar(0 until deviceinfo.nr_nodes))
  val bestSol = Array.fill(actorinfo.nr_actors)(0)
  
  //set static actors
  actorinfo.placement.foreach { tuple =>
    add(x(tuple._1) == CPIntVar(deviceinfo.deviceAddress.indexOf(tuple._2))) 
  }

  onSolution { 
    println("solution: " + x.mkString(","))
    for(i <- 0 until actorinfo.nr_actors) {
      bestSol(i) = x(i).value
    }
  }
  
  minimize(sum(0 until actorinfo.links.length) { case i =>
      deviceinfo.latencyMatrix( x(actorinfo.links(i).output_actor) )( x(actorinfo.links(i).input_actor) ) * actorinfo.links(i).flow 
    })  search {
      binaryFirstFail(x)
    } 

  val stats = start()
  println(stats)
  
}

class LandmarkNode extends Actor {

  var deviceinfo = DeviceInfo(0, Array(), Array())

  def receive = {
    case Init(addrs: Array[Address], latMat: Array[Array[Int]]) => {
      println("recevied init")
      deviceinfo = DeviceInfo(addrs.length, addrs, latMat)
    }
    case "devicelist" => {
      println("received devicelist")
      sender ! deviceinfo
    }
  }
}

class Deployer extends Actor with ActorLogging {
  import scala.collection.mutable.ArrayBuffer
  import akka.util.Timeout
  val refBuffer = ArrayBuffer[ActorRef]()
  
  implicit val timeout = Timeout(5 seconds)
  def receive = {
    case AddActorGraph(actorinfo: ActorInfo) => {
      println("received actorgraph")
      var future: Future[Any] =  new AskableActorSelection(context.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:6000/user/Landmark")) ? "devicelist"
      future onComplete {
        case Success(deviceinfo: DeviceInfo) => {
          var future2: Future[Any] = new AskableActorSelection(context.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:6000/user/Scheduler")) ? Schedule(actorinfo, deviceinfo)
          future2 onComplete {
            case Success(result: Algorithm) => {
              result.bestSol.view.zipWithIndex foreach {case (x,i) =>
                println("Trying to create actor "+i+" with class "+actorinfo.actorClass(i)+" on node "+x+" with address"+deviceinfo.deviceAddress(x))
                // TODO: Currently only supports 1 argument for actors
                actorinfo.args(i) match{
                  case None =>
                    refBuffer += context.actorOf(Props(Class.forName(actorinfo.actorClass(i)).asInstanceOf[Class[Actor]]).withDeploy(Deploy(scope = RemoteScope(deviceinfo.deviceAddress(x)))), name = "NAME:" + actorinfo.actorClass(i) + i)
                  case Some(arg) => 
                   refBuffer += context.actorOf(Props(Class.forName(actorinfo.actorClass(i)).asInstanceOf[Class[Actor]], arg).withDeploy(Deploy(scope = RemoteScope(deviceinfo.deviceAddress(x)))), name = "NAME:" + actorinfo.actorClass(i) + i)
                }
              }
              RegisterLinks(actorinfo.links, refBuffer.toArray)
              refBuffer.clear
            }
            case _ => println("Failed to schedule...")
          }
        }
        case Success(somethingelse) => println("I got something that was not a deviceinfo!")
        case Failure(t) => println("An error has occured: " + t.getMessage)
      } 
    }
  }
}
