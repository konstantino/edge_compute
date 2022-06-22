package testsystem

import IOPackage._
import akka.actor._
import scala.language.postfixOps
import scala.concurrent.duration._
import com.github.nscala_time.time.Imports._
import java.io._

case object Tick
case object Print
case class AverageMessage(io_nr: Int, stream_nr: Int, average: Double)
case object OutOfBoundsValue

class Stream extends Actor with IOTrait {
	import context.dispatcher
	val starttime = DateTime.now	
	val writer = new PrintWriter(new File("Stream.txt" ))
	val queue = new scala.collection.mutable.Queue[DateTime]
	val r = new scala.util.Random
	var value: Int = 0
	/* Scheduled messages */
	val ticker = context.system.scheduler.schedule(
				2000 milliseconds,
				1000 milliseconds,
				self,
				Tick)
	context.system.scheduler.scheduleOnce(DurationInt(3600).seconds, self, Stop)
	context.system.scheduler.scheduleOnce(DurationInt(60).seconds, self, Print)
	/* Stats */
	var counter: Long = 0
	var oobcounter: Long = 0
	var responsetimesum: Long = 0
	var responsetime: Long = 0

	/* Specify nr of outputs of the actor */
	addOutputs(1)

	def receive = {
		case RegisterOutput(output_nr, input_nr, actor) =>
			regOutput(output_nr, input_nr, actor)
		case Tick =>
			value = r.nextInt(1001)
			counter += 1
			writer.write("["+DateTime.now+"]MSGNR:"+ counter + " VALUE:" + value +"\n")
			if(value > 995 || value < 5){
				oobcounter += 1
				queue += DateTime.now
			}
			Outputs(0)._2 ! IntMessage(Outputs(0)._1, value)
			//println("["+DateTime.now+"] VALUE:" + value)
		case OutOfBoundsValue =>
			responsetime = (queue.dequeue to DateTime.now).millis
			responsetimesum += responsetime
			writer.write("["+DateTime.now+"]Responsetime:" + responsetime +"\n")
			//println("["+DateTime.now+"]OOB RESPONSE")
		case Print =>
			println("-----------------------------------------------------------")
			println("["+DateTime.now+"]")
			println("Messages sent:" + counter)
			println("Outofboundmsgs:" + oobcounter)
			println("Runningtime(ms):"+ (starttime to DateTime.now).millis) 
			println("-----------------------------------------------------------")
			context.system.scheduler.scheduleOnce(DurationInt(1).minute, self, Print)
		case Stop => 
			ticker.cancel()
			writer.write("*************STATS*************"+"\n")
			writer.write("Messages sent:" + counter+"\n")
			writer.write("Outofboundmsgs:" + oobcounter+"\n")
			writer.write("Average responsetime:" + responsetimesum/oobcounter+"\n")
			writer.write("Runningtime(ms):"+ (starttime to DateTime.now).millis+"\n") 
			writer.write("**************END**************"+"\n")
			writer.close()
	}
}

class StreamGateway extends Actor with IOTrait {
	import context.dispatcher
	val starttime = DateTime.now	
	val writer = new PrintWriter(new File("StreamGW.txt" ))

	/* Stats */
	var rcvdmsgs: Long = 0
	var sentmsgs: Long = 0
	var receivedMessages: Array[Int] = Array(0,0,0)
	var correctMessages: Array[Int] = Array(0,0,0)
	var sums: Array[Int] = Array(0,0,0)

	context.system.scheduler.scheduleOnce(DurationInt(3650).seconds, self, Stop)

	/* Specify nr of inputs and outputs of the actor */
	addInputs(3)
	addOutputs(4)

	def receive = {
		case RegisterOutput(output_nr, input_nr, actor) =>
			regOutput(output_nr, input_nr, actor)
		case RegisterInput(nr, actor) =>
			regInput(nr, actor)
		case IntMessage(nr,value) =>
			writer.write("["+DateTime.now+"]RECEIVED: " + value + "FROM: Stream"+nr+"\n")
		 	receivedMessages(nr) += 1
		 	rcvdmsgs += 1
			if(value > 995 || value < 5) {
				writer.write("["+DateTime.now+"]VALUE:"+value+" OUT OF BOUNDS. FROM: Stream"+nr+"\n")
				sender ! OutOfBoundsValue
				sentmsgs += 1
			} else {
				correctMessages(nr) += 1
				sums(nr) += value
			}
			if(receivedMessages(nr) == 60) {
				Outputs(0)._2 ! AverageMessage(Outputs(0)._1,nr, sums(nr)/correctMessages(nr))
				sentmsgs += 1
				sums(nr) = 0
			 	receivedMessages(nr) = 0
			 	correctMessages(nr) = 0
			}
		case Stop =>
			writer.write("*************STATS*************"+"\n")
			writer.write("Messages received:" + rcvdmsgs+"\n")
			writer.write("Messages sent:" + sentmsgs+"\n")
			writer.write("Runningtime(ms):"+ (starttime to DateTime.now).millis +"\n") 
			writer.write("**************END**************"+"\n")
			writer.close()
	}
}


class Datastore extends Actor with IOTrait {
	import context.dispatcher
	val writer = new PrintWriter(new File("Datastore.txt" ))
	val starttime = DateTime.now	

	context.system.scheduler.scheduleOnce(DurationInt(3650).seconds, self, Stop)
	/* Stats */
	var rcvdmsgs = 0
	/* Specify nr of inputs and outputs of the actor */
	addInputs(1)

	def receive = {
		case RegisterInput(nr, actor) =>
			regInput(nr, actor)
		case AverageMessage(nr, stream_nr, average) =>
			rcvdmsgs += 1
			writer.write("["+DateTime.now+"]STORE VALUE:"+ average + " FROM: Stream"+stream_nr+"\n")
		case Stop =>
			writer.write("*************STATS*************"+"\n")
			writer.write("Messages received:" + rcvdmsgs+"\n")
			writer.write("Runningtime(ms):"+ (starttime to DateTime.now).millis+"\n") 
			writer.write("**************END**************"+"\n")
			writer.close()
	}
}




