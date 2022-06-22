import deployment._

import akka.actor.AddressFromURIString
import akka.actor.Address
import akka.actor.ActorIdentity
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory;
	

object TestCaseDeployment {
	def main(args: Array[String]): Unit = {
	 sendDeployment()
	}
	def sendDeployment(): Unit = {
		
		var addrs: Array[Address] = Array(
		    AddressFromURIString("akka.tcp://ClusterSystem@127.0.0.1:6001"),
		    AddressFromURIString("akka.tcp://ClusterSystem@127.0.0.1:6002"),
		    AddressFromURIString("akka.tcp://ClusterSystem@127.0.0.1:6003"),
		    AddressFromURIString("akka.tcp://ClusterSystem@127.0.0.1:6004"),
		    AddressFromURIString("akka.tcp://ClusterSystem@127.0.0.1:6005"))

		var latencyMatrix: Array[Array[Int]] = Array(
		    Array(0  , 40 , 40 , 20 , 100),
		    Array(40 , 0  , 40 , 20 , 100),
		    Array(40 , 40 , 0  , 20 , 100),
		    Array(20 , 20 , 20 , 0  , 80),
		    Array(100, 100, 100, 80 , 0))

		// The classes of each actor in the array 
		var classes: Array[String]   = Array("testsystem.Stream", "testsystem.Stream", "testsystem.Stream", "testsystem.StreamGateway", "testsystem.Datastore")
		val args: Array[Option[Any]] = Array(None, None, None, None, None)
		// The predefined placements of the static actors
		val place: List[(Int,Address)] = List(
			(0,addrs(0)),
			(1,addrs(1)),
			(2,addrs(2)),
			(3,addrs(3)),
			(4,addrs(4))
			)

		// Streams -> StreamGateway
		val e1 = new Link(0,0,3,0,60)  
		val e2 = new Link(1,0,3,1,60)
		val e3 = new Link(2,0,3,2,60)

		//StreamGateway -> Streams
		val e4 = new Link(3,1,0,0,1)
		val e5 = new Link(3,2,1,0,1)
		val e6 = new Link(3,3,2,0,1)

		// StreamGateway -> DataCenterGateway
		val e7 = new Link(3,0,4,0,3)	



		var connections = List(e1,e2,e3,e4,e5,e5,e7)
		// Send the deployment to the scheduler actor
		val actorinfo = new ActorInfo(5, classes, place, connections, args)

		//
		val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 6020).
        withFallback(ConfigFactory.load())

      	// Create an Akka system
      	val system = ActorSystem("ClusterSystem", config)

      	Thread.sleep(3000)
      	println("SENDING Landmark info")
      	val landmarkRef = system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:6000/user/Landmark")
      	landmarkRef ! Init(addrs,latencyMatrix)
      	Thread.sleep(1000)
      	println("SENDING AddActorGraph")
		val deployerRef = system.actorSelection("akka.tcp://ClusterSystem@127.0.0.1:6000/user/Deployer")
		
		deployerRef ! AddActorGraph(actorinfo)

	}
}









