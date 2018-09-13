/*
 * Copyright 2013-2018 the original author or authors.
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

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnvironmentRepository} that based on one or more git repositories. Can be
 * configured just like a single {@link JGitEnvironmentRepository}, for the "default"
 * properties, and then additional repositories can be registered by name. The simplest
 * form of the registration is just a map from name to uri (plus credentials if needed),
 * where each app has its own git repository. As well as a name you can provide a pattern
 * that matches on the application name (or even a list of patterns). Each sub-repository
 * additionally can have its own search paths (subdirectories inside the top level of the
 * repository).
 *
 * 基于一个或多个git存储库的{@link EnvironmentRepository}。
 * 对于“默认”属性，可以像单个{@link JGitEnvironmentRepository}一样进行配置，
 * 然后可以按名称注册其他存储库。 最简单的注册形式只是从名称到uri的地图（如果需要，还有凭证），
 * 其中每个应用程序都有自己的git存储库。 除了名称，您还可以提供与应用程序名称（甚至模式列表）匹配的模式。
 * 每个子存储库还可以有自己的搜索路径（存储库顶层内的子目录）。
 *
 * @author Andy Chan (iceycake)
 * @author Dave Syer
 * @author Gareth Clay
 *
 */
public class MultipleJGitEnvironmentRepository extends JGitEnvironmentRepository {

	/**
	 * Map of repository identifier to location and other properties.
     *
     * 存储库标识符到位置和其他属性的映射。
	 */
	private Map<String, PatternMatchingJGitEnvironmentRepository> repos;

	private Map<String, JGitEnvironmentRepository> placeholders = new LinkedHashMap<>();

	public MultipleJGitEnvironmentRepository(ConfigurableEnvironment environment,
											 MultipleJGitEnvironmentProperties properties) {
		super(environment, properties);
		this.repos = properties.getRepos().entrySet().stream()
				.map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),
						new PatternMatchingJGitEnvironmentRepository(environment, e.getValue())))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		for (String name : this.repos.keySet()) {
			PatternMatchingJGitEnvironmentRepository repo = this.repos.get(name);
			repo.setEnvironment(getEnvironment());
			if (!StringUtils.hasText(repo.getName())) {
				repo.setName(name);
			}
			if (repo.getPattern() == null || repo.getPattern().length == 0) {
				// ------------------关键方法------------------
				// 设置partten："0/*" , "0" , "0,*"
				repo.setPattern(new String[] { name });
			}
			if (repo.getTransportConfigCallback() == null) {
				repo.setTransportConfigCallback(getTransportConfigCallback());
			}
			if (getTimeout() != 0 && repo.getTimeout() == 0) {
				repo.setTimeout(getTimeout());
			}
			if (getRefreshRate() != 0 && repo.getRefreshRate() == 0) {
				repo.setRefreshRate(getRefreshRate());
			}
			String user = repo.getUsername();
			String passphrase = repo.getPassphrase();
			if (user == null) {
				repo.setUsername(getUsername());
				repo.setPassword(getPassword());
			}
			if (passphrase == null) {
				repo.setPassphrase(getPassphrase());
			}
			if (isSkipSslValidation()) {
				repo.setSkipSslValidation(true);
			}
			repo.afterPropertiesSet();
		}
		if (!getBasedir().exists() &&
			!getBasedir().mkdirs()) {
				throw new IllegalStateException(
					"Basedir does not exist and can not be created: "	+ getBasedir());
		}
		if (!getBasedir().getParentFile().canWrite()) {
			throw new IllegalStateException(
					"Cannot write parent of basedir (please configure a writable location): "
							+ getBasedir());
		}
	}

	public void setRepos(Map<String, PatternMatchingJGitEnvironmentRepository> repos) {
		this.repos.putAll(repos);
	}

	public Map<String, PatternMatchingJGitEnvironmentRepository> getRepos() {
		return this.repos;
	}

	@Override
	public Locations getLocations(String application, String profile, String label) {
		for (PatternMatchingJGitEnvironmentRepository repository : this.repos.values()) {
			if (repository.matches(application, profile, label)) {
				for (JGitEnvironmentRepository candidate : getRepositories(repository,
						application, profile, label)) {
					try {
						Environment source = candidate.findOne(application, profile,
								label);
						if (source != null) {
							return candidate.getLocations(application, profile, label);
						}
					}
					catch (Exception e) {
						if (logger.isDebugEnabled()) {
							this.logger.debug("Cannot retrieve resource locations from "
									+ candidate.getUri() + ", cause: ("
									+ e.getClass().getSimpleName() + ") "
									+ e.getMessage(), e);
						}
						continue;
					}
				}
			}
		}
		// ------------------关键方法---------------
		// 获得资源库
		JGitEnvironmentRepository candidate = getRepository(this, application, profile,
				label);
		if (candidate == this) {
			// ------------------关键方法---------------
			// 获得git，会在这里面做git初始化，checkout，merge，等等
			return super.getLocations(application, profile, label);
		}
		return candidate.getLocations(application, profile, label);
	}

	@Override
	public Environment findOne(String application, String profile, String label) {
		for (PatternMatchingJGitEnvironmentRepository repository : this.repos.values()) {
		    // -------------------关键方法----------------------
            // 查看是否匹配
			if (repository.matches(application, profile, label)) {
				for (JGitEnvironmentRepository candidate : getRepositories(repository,
						application, profile, label)) {
					try {
						if (label == null) {
							label = candidate.getDefaultLabel();
						}
						Environment source = candidate.findOne(application, profile,
								label);
						if (source != null) {
							return source;
						}
					}
					catch (Exception e) {
						if (logger.isDebugEnabled()) {
							this.logger.debug(
									"Cannot load configuration from " + candidate.getUri()
											+ ", cause: (" + e.getClass().getSimpleName()
											+ ") " + e.getMessage(),
									e);
						}
						continue;
					}
				}
			}
		}
		// ---------------关键方法----------------
		// 获得资源库
		JGitEnvironmentRepository candidate = getRepository(this, application, profile,
				label);
		if (label == null) {
			label = candidate.getDefaultLabel();
		}
		if (candidate == this) {
			// ---------------关键方法----------------
			return super.findOne(application, profile, label);
		}
		return candidate.findOne(application, profile, label);
	}

	private List<JGitEnvironmentRepository> getRepositories(
			JGitEnvironmentRepository repository, String application, String profile,
			String label) {
		List<JGitEnvironmentRepository> list = new ArrayList<>();
		String[] profiles = profile == null ? new String[] { null }
				: StringUtils.commaDelimitedListToStringArray(profile);
		for (int i = profiles.length; i-- > 0;) {
			list.add(getRepository(repository, application, profiles[i], label));
		}
		return list;
	}

	private JGitEnvironmentRepository getRepository(JGitEnvironmentRepository repository,
			String application, String profile, String label) {
		if (!repository.getUri().contains("{")) {
			// 如果uri中没有{，则直接返回
			return repository;
		}

		// 如果uri中有{
		String key = repository.getUri();

		// cover the case where label is in the uri, but no label was sent with the
		// request
        // 覆盖标签在uri中的情况，但没有发送标签
        // 请求
		if (key.contains("{label}") && label == null) {
		    // 如果uri中包含{label} && label == null ，label设置为默认的
			label = repository.getDefaultLabel();
		}
		if (application != null) {
			// application 不为空，直接替换
			key = key.replace("{application}", application);
		}
		if (profile != null) {
			// profile 不为空，直接替换
			key = key.replace("{profile}", profile);
		}
		if (label != null) {
			// label 不为空，直接替换
			key = key.replace("{label}", label);
		}

		// placeholders 不存在，则存入map
		if (!this.placeholders.containsKey(key)) {
			// ----------------关键方法--------------
			this.placeholders.put(key, getRepository(repository, key));
		}
		return this.placeholders.get(key);
	}

	private JGitEnvironmentRepository getRepository(JGitEnvironmentRepository source,
			String uri) {
		JGitEnvironmentRepository repository = new JGitEnvironmentRepository(null,
				new JGitEnvironmentProperties());
		File basedir = repository.getBasedir();
		BeanUtils.copyProperties(source, repository);
		repository.setUri(uri);
		repository.setBasedir(
				new File(source.getBasedir().getParentFile(), basedir.getName()));
		return repository;
	}

    /**
     * 模式匹配JGit环境存储库
     */
	public static class PatternMatchingJGitEnvironmentRepository
			extends JGitEnvironmentRepository {

		/**
		 * Pattern to match on application name and profiles.
         *
         * 应用程序名称和配置文件匹配的模式。
		 */
		private String[] pattern = new String[0];
		/**
		 * Name of repository (same as map key by default).
         *
         * 存储库的名称（默认情况下与映射键相同）。
		 */
		private String name;

		public PatternMatchingJGitEnvironmentRepository() {
			super(null, new JGitEnvironmentProperties());
		}

		public PatternMatchingJGitEnvironmentRepository(
				ConfigurableEnvironment environment,
				MultipleJGitEnvironmentProperties.PatternMatchingJGitEnvironmentProperties properties) {
			super(environment, properties);
			this.setPattern(properties.getPattern());
			this.name = properties.getName();
		}

        /**
         * 查看是否匹配
         * @param application
         * @param profile
         * @param label
         * @return
         */
		public boolean matches(String application, String profile, String label) {
			if (this.pattern == null || this.pattern.length == 0) {
				return false;
			}
			String[] profiles = StringUtils.commaDelimitedListToStringArray(profile);
			for (int i = profiles.length; i-- > 0;) {
				if (PatternMatchUtils.simpleMatch(this.pattern,
						application + "/" + profiles[i])) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Environment findOne(String application, String profile, String label) {

			if (this.pattern == null || this.pattern.length == 0) {
				return null;
			}

			if (PatternMatchUtils.simpleMatch(this.pattern,
					application + "/" + profile)) {
				return super.findOne(application, profile, label);
			}

			return null;

		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String[] getPattern() {
			return this.pattern;
		}

		public void setPattern(String[] pattern) {
			Collection<String> patterns = new ArrayList<>();
			List<String> otherProfiles = new ArrayList<>();
			for (String p : pattern) {
				if (p != null) {
					if (!p.contains("/")) {
						// Match any profile
						// 匹配任何profile
						patterns.add(p + "/*");
					}
					if (!p.endsWith("*")) {
						// If user supplies only one profile, allow others
						// 如果用户只提供一个配置文件，请允许其他人
						otherProfiles.add(p + ",*");
					}
				}
				patterns.add(p);
			}
			patterns.addAll(otherProfiles);
			if (!patterns.contains(null)) {
				// Make sure they are unique
				// 确保它们是独一无二的
				patterns = new LinkedHashSet<>(patterns);
			}
			this.pattern = patterns.toArray(new String[0]);
		}

	}

	@Override
	public void setOrder(int order) {
		super.setOrder(order);
	}
}
