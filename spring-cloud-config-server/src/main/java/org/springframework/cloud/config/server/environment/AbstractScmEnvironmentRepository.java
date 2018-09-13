/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.config.server.environment;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.support.AbstractScmAccessor;
import org.springframework.cloud.config.server.support.AbstractScmAccessorProperties;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author Dave Syer
 *
 */
public abstract class AbstractScmEnvironmentRepository extends AbstractScmAccessor
		implements EnvironmentRepository, SearchPathLocator, Ordered {

	private EnvironmentCleaner cleaner = new EnvironmentCleaner();
	private int order = Ordered.LOWEST_PRECEDENCE;

	public AbstractScmEnvironmentRepository(ConfigurableEnvironment environment) {
		super(environment);
	}

	public AbstractScmEnvironmentRepository(ConfigurableEnvironment environment, AbstractScmAccessorProperties properties) {
		super(environment, properties);
		this.order = properties.getOrder();
	}

	@Override
	public synchronized Environment findOne(String application, String profile, String label) {
		NativeEnvironmentRepository delegate = new NativeEnvironmentRepository(getEnvironment(),
				new NativeEnvironmentProperties());
		// --------------------关键方法-----------------------
        // 获得git，会在这里面做git初始化，checkout，merge，等等
		Locations locations = getLocations(application, profile, label);
		delegate.setSearchLocations(locations.getLocations());
		// --------------------关键方法-----------------------
        // 通过委托获得环境变量
		Environment result = delegate.findOne(application, profile, "");
		result.setVersion(locations.getVersion());
		result.setLabel(label);
		// --------------------关键方法-----------------------
		// 对上面的result的数据，进行一些replace， 获得application.yml
		return this.cleaner.clean(result, getWorkingDirectory().toURI().toString(),
				getUri());
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}
