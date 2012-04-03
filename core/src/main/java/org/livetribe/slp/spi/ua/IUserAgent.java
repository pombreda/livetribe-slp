/*
 * Copyright 2005-2008 the original author or authors
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

import java.util.List;

import org.livetribe.slp.Attributes;
import org.livetribe.slp.Scopes;
import org.livetribe.slp.ServiceInfo;
import org.livetribe.slp.ServiceType;
import org.livetribe.slp.ServiceURL;


/**
 *
 */
public interface IUserAgent
{
    public List<ServiceInfo> findServices(ServiceType serviceType, String language, Scopes scopes, String filter);

    public Attributes findAttributes(ServiceType serviceType, String language, Scopes scopes, Attributes tags);

    public Attributes findAttributes(ServiceURL serviceURL, String language, Scopes scopes, Attributes tags);

    public List<ServiceType> findServiceTypes(String namingAuthority, Scopes scopes);
}
