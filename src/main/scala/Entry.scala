

import java.util.concurrent.ConcurrentLinkedDeque

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch._
import com.typesafe.config.Config

object Entry {
  def main(args: Array[String]): Unit = {

    val actorSystem  = ActorSystem("MyActorSystem")
    // if we use Request message queue we need not to declare mailbox configuration in actorRef explicitly
    // because `Request message queue` makes the actor compulsory to use the required type mailbox.
    val rActorRef = actorSystem.actorOf(Props[RActor].withDispatcher("custom-dispatcher"))
    val myActorREf = actorSystem.actorOf(MyActor.props(rActorRef), "MyActor")

  }

}

trait MyUnboundedMessageQueueSemantics

object MyUnboundedMailbox {
  class MyMessageQueue extends MessageQueue with MyUnboundedMessageQueueSemantics {

    private val myQueue: ConcurrentLinkedDeque[Envelope] = new ConcurrentLinkedDeque[Envelope]()

    override def enqueue(receiver: ActorRef, handle: Envelope): Unit = {
      if(handle.sender.path.name == "MyActor") {
        handle.sender ! "Hey dude, How are you?, I Know your name,processing your request"
          myQueue.offer(handle)
          }
          else handle.sender ! "I don't talk to strangers, I can't process your request"
    }

    override def dequeue(): Envelope = myQueue.poll()

    override def numberOfMessages: Int = myQueue.size()

    override def hasMessages: Boolean = !myQueue.isEmpty

    override def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = myQueue.clear()
  }
}


class MyUnboundedMailbox extends MailboxType with ProducesMessageQueue[MyUnboundedMailbox.MyMessageQueue] {

  // This constructor signature must exist, it will be called by Akka
  def this(settings: ActorSystem.Settings, config: Config) = {
    // put your initialization code here
    this()
  }


  override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = new MyUnboundedMailbox.MyMessageQueue()
}


class MyActor(actorRef: ActorRef) extends Actor{
  actorRef ! "hello"

  override def receive: Receive = {
    case x => println(x)
  }
}
object MyActor{
  def props(actorRef: ActorRef) = Props(new MyActor(actorRef))
}


class RActor extends Actor with RequiresMessageQueue[MyUnboundedMessageQueueSemantics]{
  override def receive: Receive = {
    case x => println(x)
  }
}
