import deployment._
import com.typesafe.config.ConfigFactory;
import akka.actor.ActorSystem
import akka.actor.AddressFromURIString
import akka.actor.Props
import akka.actor.Deploy
import akka.cluster.Cluster._
import akka.cluster.ClusterEvent;
import akka.pattern.AskableActorSelection
import akka.remote.RemoteScope;
import scala.io.Source
import java.lang._


object DeploymentApp{
	def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup("6000")
    else
      startup(args(0))
  }

  def startup(port: String): Unit = {
    // Override the configuration of the port
  	val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())

    // Create an Akka system
    val system = ActorSystem("ClusterSystem", config)

  	if(port == "6000") {
    	val schedulerAddr = AddressFromURIString("akka.tcp://ClusterSystem@127.0.0.1:6000")
      val landmarkActor = system.actorOf(Props[LandmarkNode].withDeploy(Deploy(scope = RemoteScope(schedulerAddr))), name = "Landmark")
    	val schedulerActor = system.actorOf(Props[Scheduler].withDeploy(Deploy(scope = RemoteScope(schedulerAddr))), name = "Scheduler")
      val deployerActor = system.actorOf(Props[Deployer].withDeploy(Deploy(scope = RemoteScope(schedulerAddr))), name = "Deployer")	
    }
  }	
	
}
  

