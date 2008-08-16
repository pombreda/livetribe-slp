/*
 * Copyright 2008-2008 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.spi.sa;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import org.livetribe.slp.SLPError;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.spi.msg.Message;
import org.livetribe.slp.spi.msg.SrvAck;
import org.livetribe.slp.spi.msg.SrvReg;
import org.livetribe.slp.spi.net.UDPConnector;

/**
 * @version $Revision$ $Date$
 */
public class UDPSrvRegPerformer extends SrvRegPerformer
{
    private final UDPConnector udpConnector;

    public UDPSrvRegPerformer(UDPConnector udpConnector, Settings settings)
    {
        this.udpConnector = udpConnector;
    }

    public SrvAck perform(InetSocketAddress remoteAddress, ServiceInfo service, boolean update)
    {
        SrvReg srvReg = newSrvReg(service, update);
        byte[] requestBytes = srvReg.serialize();

        DatagramPacket packet = udpConnector.sendAndReceive(remoteAddress, requestBytes);
        if (packet == null)
            throw new ServiceLocationException("Unable to contact " + remoteAddress, SLPError.NETWORK_ERROR);

        byte[] replyBytes = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), replyBytes, 0, replyBytes.length);
        return (SrvAck)Message.deserialize(replyBytes);
    }
}