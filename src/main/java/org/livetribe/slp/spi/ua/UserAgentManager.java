/*
 * Copyright 2006 the original author or authors
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
package org.livetribe.slp.spi.ua;

import java.io.IOException;
import java.net.InetAddress;

import org.livetribe.slp.ServiceType;
import org.livetribe.slp.spi.AgentManager;
import org.livetribe.slp.spi.msg.DAAdvert;
import org.livetribe.slp.spi.msg.SAAdvert;
import org.livetribe.slp.spi.msg.SrvRply;

/**
 * @version $Rev$ $Date$
 */
public interface UserAgentManager extends AgentManager
{
    public DAAdvert[] multicastDASrvRqst(String[] scopes, String filter, long timeframe) throws IOException;

    public SAAdvert[] multicastSASrvRqst(String[] scopes, String filter, int timeframe) throws IOException;

    public SrvRply unicastSrvRqst(InetAddress address, ServiceType serviceType, String[] scopes, String filter) throws IOException;
}