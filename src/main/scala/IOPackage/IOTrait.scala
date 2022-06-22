package IOPackage

import akka.actor._

trait IOTrait {
	var Inputs: Array[ActorRef] = Array.empty[ActorRef]

	def addInputs(nr:Int) = {
		Inputs = new Array[ActorRef](nr)
	}

	def regInput(nr: Int, addr: ActorRef) = {
		Inputs(nr) = addr
	}
	
	var Outputs: Array[(Int, ActorRef)] = Array.empty[(Int,ActorRef)]
	
	def addOutputs(nr:Int) = {
		Outputs = new Array[(Int,ActorRef)](nr)
	}

	def regOutput(output_nr: Int, input_nr: Int, addr: ActorRef) = {
		Outputs(output_nr) = (input_nr, addr)
	}
}

trait Message {
		def number: Int
	}


case class IntMessage(number: Int, value: Int) extends Message

case class StringMessage(number:Int, value: String) extends Message

case class DoubleMessage(number:Int, value: Double) extends Message 

case class RegisterOutput(number: Int, input_nr: Int,  value: ActorRef) extends Message

case class RegisterInput(number: Int, value: ActorRef) extends Message

case class Stop() 

case class Start() 

object NetworkLanguage {
	def connect(from: ActorRef ,output_nr: Int , to: ActorRef, input_nr: Int) = {
		from ! RegisterOutput(output_nr, input_nr, to)
	 	to ! RegisterInput(input_nr, from)
	}
}

case class Output(actor: ActorRef, port_nr: Int) {
	def ->(that: Input) = {
		this.actor ! RegisterOutput(this.port_nr, that.port_nr, that.actor)
		that.actor ! RegisterInput(that.port_nr, this.actor)
		//println("CONNECTING:")
		//println("OUTPUT: " + this.actor + " on port " +  this.port_nr)
		//println("INPUT: " + that.actor + " on port " + that.port_nr)
	}  
}

case class Input(ref: ActorRef, input_nr: Int) {
	def actor = ref
	def port_nr = input_nr
} 