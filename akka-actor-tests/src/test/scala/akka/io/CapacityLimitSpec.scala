/**
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.io

import akka.testkit.{ TestProbe, AkkaSpec }
import akka.testkit.SocketUtil._
import Tcp._

class CapacityLimitSpec extends AkkaSpec("""
    akka.loglevel = ERROR
    akka.io.tcp.max-channels = 4
    akka.actor.serialize-creators = on
    """)
  with TcpIntegrationSpecSupport {

  "The TCP transport implementation" should {

    "reply with CommandFailed to a Bind or Connect command if max-channels capacity has been reached" in new TestSetup {
      establishNewClientConnection()

      // we now have three channels registered: a listener, a server connection and a client connection
      // so register one more channel
      val commander = TestProbe()
      val addresses = temporaryServerAddresses(2)
      commander.send(IO(Tcp), Bind(bindHandler.ref, addresses(0)))
      commander.expectMsg(Bound(addresses(0)))

      // we are now at the configured max-channel capacity of 4

      val bindToFail = Bind(bindHandler.ref, addresses(1))
      commander.send(IO(Tcp), bindToFail)
      commander.expectMsgType[CommandFailed].cmd should be theSameInstanceAs (bindToFail)

      val connectToFail = Connect(endpoint)
      commander.send(IO(Tcp), connectToFail)
      commander.expectMsgType[CommandFailed].cmd should be theSameInstanceAs (connectToFail)
    }

  }

}
