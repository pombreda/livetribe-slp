/*
 * Copyright 2006-2008 the original author or authors
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
package org.livetribe.slp.sa;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.settings.Factory;
import static org.livetribe.slp.settings.Keys.*;
import org.livetribe.slp.settings.PropertiesSettings;
import org.livetribe.slp.settings.Settings;
import org.livetribe.slp.srv.Server;
import org.livetribe.slp.srv.ServiceInfoCache;
import org.livetribe.slp.srv.TCPSrvAckPerformer;
import org.livetribe.slp.srv.msg.Message;
import org.livetribe.slp.srv.msg.SrvAck;
import org.livetribe.slp.srv.msg.SrvDeReg;
import org.livetribe.slp.srv.msg.SrvReg;
import org.livetribe.slp.srv.net.MessageEvent;
import org.livetribe.slp.srv.net.MessageListener;
import org.livetribe.slp.srv.net.TCPConnector;
import org.livetribe.slp.srv.net.TCPConnectorServer;
import org.livetribe.slp.srv.net.UDPConnector;
import org.livetribe.slp.srv.net.UDPConnectorServer;
import org.livetribe.slp.srv.sa.AbstractServiceAgent;
import org.livetribe.slp.srv.sa.SAServiceInfo;
import org.livetribe.slp.srv.sa.ServiceAgentInfo;

/**
 * Implementation of an SLP service agent standalone server that can be started as a service in a host.
 * <br />
 * Only one instance of this server can be started per each host, as it listens on the SLP TCP port.
 * In SLP, a service agent standalone server exposes the services of all applications in the host it resides,
 * so that each application does not need to start a {@link ServiceAgent}, but only uses a {@link ServiceAgentClient}
 * to contact the service agent standalone server.
 *
 * @version $Rev$ $Date$
 */
public class StandardServiceAgentServer extends AbstractServiceAgent
{
    /**
     * Main method to start this service agent.
     * <br />
     * It accepts a single program argument, the file path of the configuration file that overrides the
     * defaults for this service agent
     *
     * @param args the program arguments
     * @throws IOException in case the configuration file cannot be read
     */
    public static void main(String[] args) throws IOException
    {
        Settings settings = null;
        if (args.length > 0) settings = PropertiesSettings.from(new File(args[0]));
        Server server = newInstance(settings);
        server.start();
    }

    /**
     * @param settings the configuration settings that override the defaults
     * @return a new instance of this service agent
     */
    public static StandardServiceAgentServer newInstance(Settings settings)
    {
        UDPConnector.Factory udpFactory = Factory.newInstance(settings, UDP_CONNECTOR_FACTORY_KEY);
        TCPConnector.Factory tcpFactory = Factory.newInstance(settings, TCP_CONNECTOR_FACTORY_KEY);
        UDPConnectorServer.Factory udpServerFactory = Factory.newInstance(settings, UDP_CONNECTOR_SERVER_FACTORY_KEY);
        TCPConnectorServer.Factory tcpServerFactory = Factory.newInstance(settings, TCP_CONNECTOR_SERVER_FACTORY_KEY);
        return new StandardServiceAgentServer(udpFactory.newUDPConnector(settings), tcpFactory.newTCPConnector(settings), udpServerFactory.newUDPConnectorServer(settings), tcpServerFactory.newTCPConnectorServer(settings), settings);
    }

    private final MessageListener listener = new ServiceAgentMessageListener();
    private final TCPConnectorServer tcpConnectorServer;
    private final TCPSrvAckPerformer tcpSrvAck;

    /**
     * Creates a new StandardServiceAgentServer
     *
     * @param udpConnector       the connector that handles udp traffic
     * @param tcpConnector       the connector that handles tcp traffic
     * @param udpConnectorServer the connector that listens for udp traffic
     * @param tcpConnectorServer the connector that listens for tcp traffic
     * @param settings           the configuration settings that override the defaults
     */
    public StandardServiceAgentServer(UDPConnector udpConnector, TCPConnector tcpConnector, UDPConnectorServer udpConnectorServer, TCPConnectorServer tcpConnectorServer, Settings settings)
    {
        super(udpConnector, tcpConnector, udpConnectorServer, settings);
        this.tcpConnectorServer = tcpConnectorServer;
        this.tcpSrvAck = new TCPSrvAckPerformer(tcpConnector, settings);
        if (settings != null) setSettings(settings);
    }

    private void setSettings(Settings settings)
    {
    }

    protected void doStart()
    {
        setAttributes(getAttributes().merge(Attributes.from("(" + ServiceAgentInfo.TCP_PORT_TAG + "=" + getPort() + ")")));

        super.doStart();

        tcpConnectorServer.addMessageListener(listener);
        tcpConnectorServer.start();

        Runtime.getRuntime().addShutdownHook(new Shutdown());
    }

    protected ServiceAgentInfo newServiceAgentInfo(String address, Scopes scopes, Attributes attributes, String language)
    {
        return ServiceAgentInfo.from(null, address, scopes, attributes, language);
    }

    protected void doStop()
    {
        super.doStop();
        tcpConnectorServer.removeMessageListener(listener);
        tcpConnectorServer.stop();
    }

    /**
     * Handles a unicast TCP SrvReg message arrived to this service agent.
     * <br />
     * This service agent will reply with an acknowledge containing the result of the registration.
     *
     * @param srvReg the SrvReg message to handle
     * @param socket the socket connected to th client where to write the reply
     */
    protected void handleTCPSrvReg(SrvReg srvReg, Socket socket)
    {
        try
        {
            boolean update = srvReg.isUpdating();
            SAServiceInfo givenService = new SAServiceInfo(ServiceInfo.from(srvReg));
            ServiceInfoCache.Result<SAServiceInfo> result = cacheService(givenService, update);
            forwardRegistration(givenService, result.getPrevious(), result.getCurrent(), update);
            tcpSrvAck.perform(socket, srvReg, SrvAck.SUCCESS);
        }
        catch (ServiceLocationException x)
        {
            tcpSrvAck.perform(socket, srvReg, x.getErrorCode());
        }
    }

    /**
     * Handles a unicast TCP SrvDeReg message arrived to this service agent.
     * <br />
     * This service agent will reply with an acknowledge containing the result of the deregistration.
     *
     * @param srvDeReg the SrvDeReg message to handle
     * @param socket   the socket connected to the client where to write the reply
     */
    protected void handleTCPSrvDeReg(SrvDeReg srvDeReg, Socket socket)
    {
        try
        {
            boolean update = srvDeReg.isUpdating();
            SAServiceInfo givenService = new SAServiceInfo(ServiceInfo.from(srvDeReg));
            ServiceInfoCache.Result<SAServiceInfo> result = uncacheService(givenService, update);
            forwardDeregistration(givenService, result.getPrevious(), result.getCurrent(), update);
            tcpSrvAck.perform(socket, srvDeReg, SrvAck.SUCCESS);
        }
        catch (ServiceLocationException x)
        {
            tcpSrvAck.perform(socket, srvDeReg, x.getErrorCode());
        }
    }

    /**
     * ServiceAgents listen for tcp messages from ServiceAgentClients.
     * They are interested in:
     * <ul>
     * <li>SrvReg, from ServiceAgentClients that want register services; the reply is a SrvAck</li>
     * <li>SrvDeReg, from ServiceAgentClients that want deregister services; the reply is a SrvAck</li>
     * </ul>
     */
    private class ServiceAgentMessageListener implements MessageListener
    {
        public void handle(MessageEvent event)
        {
            Message message = event.getMessage();
            if (logger.isLoggable(Level.FINEST))
                logger.finest("ServiceAgent server message listener received message " + message);

            Socket socket = (Socket)event.getSource();
            if (socket.getInetAddress().isLoopbackAddress())
            {
                switch (message.getMessageType())
                {
                    case Message.SRV_REG_TYPE:
                        handleTCPSrvReg((SrvReg)message, socket);
                        break;
                    case Message.SRV_DEREG_TYPE:
                        handleTCPSrvDeReg((SrvDeReg)message, socket);
                        break;
                    default:
                        if (logger.isLoggable(Level.FINE))
                            logger.fine("ServiceAgent server " + this + " dropping tcp message " + message + ": not handled by ServiceAgents");
                        break;
                }
            }
            else
            {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("ServiceAgent server " + this + " dropping tcp message " + message + ": not from loopback address");
            }
        }
    }

    private class Shutdown extends Thread
    {
        @Override
        public void run()
        {
            if (StandardServiceAgentServer.this.isRunning()) StandardServiceAgentServer.this.stop();
        }
    }
}