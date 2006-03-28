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
package org.livetribe.slp.api.da;

import org.livetribe.slp.api.Agent;
import org.livetribe.slp.spi.da.DirectoryAgentManager;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;
import org.livetribe.slp.ServiceLocationException;

/**
 * @version $Rev$ $Date$
 */
public interface DirectoryAgent extends Agent
{
    public void setDirectoryAgentManager(DirectoryAgentManager manager);

    // TODO: add equivalent to deregister a service ?
    public void registerService(ServiceType serviceType, ServiceURL serviceURL, String[] scopes, String[] attributes, String language) throws ServiceLocationException;

    public void addDirectoryAgentListener(DirectoryAgentListener listener);

    public void removeDirectoryAgentListener(DirectoryAgentListener listener);
}