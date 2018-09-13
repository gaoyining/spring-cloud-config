/*
 * Copyright 2013-2016 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.core.OrderComparator;

/**
 * An {@link EnvironmentRepository} composed of multiple ordered {@link EnvironmentRepository}s.
 *
 * {@link EnvironmentRepository}由多个有序{@link EnvironmentRepository}组成。
 * 组合环境变量仓库
 *
 * @author Ryan Baxter
 */
public class CompositeEnvironmentRepository implements EnvironmentRepository {

	protected List<EnvironmentRepository> environmentRepositories;

	/**
	 * Creates a new {@link CompositeEnvironmentRepository}.
	 * @param environmentRepositories The list of {@link EnvironmentRepository}s to create the composite from.
	 */
	public CompositeEnvironmentRepository(List<EnvironmentRepository> environmentRepositories) {
		// Sort the environment repositories by the priority
		// 按优先级排序环境存储库
		Collections.sort(environmentRepositories, OrderComparator.INSTANCE);
		this.environmentRepositories = environmentRepositories;
	}

    /**
     * 通过{@link org.springframework.boot.actuate.health.AbstractHealthIndicator}的doHealthCheck 方法
     * @param application
     * @param profile
     * @param label
     * @return
     */
	@Override
	public Environment findOne(String application, String profile, String label) {
		Environment env = new Environment(application, new String[]{profile}, label, null, null);
		if(environmentRepositories.size() == 1) {
		    // -------------------关键方法-----------------
            // 获得一个环境变量的DTO
			Environment envRepo = environmentRepositories.get(0).findOne(application, profile, label);
			env.addAll(envRepo.getPropertySources());
			env.setVersion(envRepo.getVersion());
			env.setState(envRepo.getState());
		} else {
			for (EnvironmentRepository repo : environmentRepositories) {
				env.addAll(repo.findOne(application, profile, label).getPropertySources());
			}
		}
		// 实际对应的环境变量
		return env;
	}
}
