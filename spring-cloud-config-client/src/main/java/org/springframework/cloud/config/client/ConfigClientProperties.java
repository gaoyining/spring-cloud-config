/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.config.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Dave Syer
 *
 */
@ConfigurationProperties(ConfigClientProperties.PREFIX)
public class ConfigClientProperties {

	public static final String PREFIX = "spring.cloud.config";
	public static final String TOKEN_HEADER = "X-Config-Token";
	public static final String STATE_HEADER = "X-Config-State";
	public static final String AUTHORIZATION = "authorization";

	/**
	 * Flag to say that remote configuration is enabled. Default true;
	 *
	 * 标记表示已启用远程配置。 默认为true;
	 */
	private boolean enabled = true;

	/**
	 * The default profile to use when fetching remote configuration (comma-separated).
	 * Default is "default".
	 *
	 * 获取远程配置时使用的默认配置文件（以逗号分隔）。 默认为“默认”。
	 */
	private String profile = "default";

	/**
	 * Name of application used to fetch remote properties.
	 *
	 * 用于获取远程属性的应用程序的名称。
	 */
	@Value("${spring.application.name:application}")
	private String name;

	/**
	 * The label name to use to pull remote configuration properties. The default is set
	 * on the server (generally "master" for a git based server).
	 *
	 * 用于提取远程配置属性的标签名称。 默认值在服务器上设置（对于基于git的服务器通常为“master”）。
	 */
	private String label;

	/**
	 * The username to use (HTTP Basic) when contacting the remote server.
	 *
	 * 联系远程服务器时要使用的用户名（HTTP Basic）。
	 */
	private String username;

	/**
	 * The password to use (HTTP Basic) when contacting the remote server.
	 *
	 * 联系远程服务器时要使用的密码（HTTP Basic）。
	 */
	private String password;

	/**
	 * The URI of the remote server (default http://localhost:8888).
	 *
	 * 远程服务器的URI（默认为http：// localhost：8888）。
	 */
	private String[] uri = { "http://localhost:8888" };

	/**
	 * Discovery properties.
	 *
	 * 发现属性。
	 */
	private Discovery discovery = new Discovery();

	/**
	 * Flag to indicate that failure to connect to the server is fatal (default false).
	 *
	 * 用于指示无法连接到服务器的标志是致命的（默认为false）。
	 */
	private boolean failFast = false;

	/**
	 * Security Token passed thru to underlying environment repository.
	 *
	 * 安全令牌通过底层环境存储库。
	 */
	private String token;

	/**
	 * timeout on waiting to read data from the Config Server.
	 *
	 * 等待从Config Server读取数据时超时。
	 */
	private int requestReadTimeout = (60 * 1000 * 3) + 5000;

	/**
	 * Flag to indicate whether to send state. Default true.
	 *
	 * 用于指示是否发送状态的标志。 默认为true。
	 */
	private boolean sendState = true;

	/**
	 * Additional headers used to create the client request.
	 *
	 * 用于创建客户端请求的其他标头。
	 */
	private Map<String, String> headers = new HashMap<>();

	private ConfigClientProperties() {
	}

	public ConfigClientProperties(Environment environment) {
		String[] profiles = environment.getActiveProfiles();
		if (profiles.length == 0) {
			profiles = environment.getDefaultProfiles();
		}
		this.setProfile(StringUtils.arrayToCommaDelimitedString(profiles));
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String[] getUri() {
		return this.uri;
	}

	public void setUri(String[] url) {
		this.uri = url;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProfile() {
		return this.profile;
	}

	public void setProfile(String env) {
		this.profile = env;
	}

	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Credentials getCredentials(int index) {
		return extractCredentials(index);
	}

	public Discovery getDiscovery() {
		return this.discovery;
	}

	public void setDiscovery(Discovery discovery) {
		this.discovery = discovery;
	}

	public boolean isFailFast() {
		return this.failFast;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	public String getToken() {
		return this.token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public int getRequestReadTimeout() {
		return requestReadTimeout;
	}

	public void setRequestReadTimeout(int requestReadTimeout) {
		this.requestReadTimeout = requestReadTimeout;
	}

	public boolean isSendState() {
		return sendState;
	}

	public void setSendState(boolean sendState) {
		this.sendState = sendState;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	private Credentials extractCredentials(int index) {
		Credentials result = new Credentials();
		int noOfUrl = this.uri.length;
		if (index < 0 || index >= noOfUrl) {
			throw new IllegalStateException("Trying to access an invalid array index");
		}
		String uri = this.uri[index];
		result.uri = uri;
		Credentials explicitCredentials = getUsernamePassword();
		result.username = explicitCredentials.username;
		result.password = explicitCredentials.password;
		try {
			URL url = new URL(uri);
			String userInfo = url.getUserInfo();
			// no credentials in url, return explicit credentials
			if (StringUtils.isEmpty(userInfo) || ":".equals(userInfo)) {
				return result;
			}
			String bare = UriComponentsBuilder.fromHttpUrl(uri).userInfo(null).build()
					.toUriString();
			result.uri = bare;

			// if userInfo does not contain a :, then append a : to it
			if (!userInfo.contains(":")) {
				userInfo = userInfo + ":";
			}

			int sepIndex = userInfo.indexOf(":");
			// set username and password from uri
			result.username = userInfo.substring(0, sepIndex);
			result.password = userInfo.substring(sepIndex + 1);

			// override password if explicitly set
			if (explicitCredentials.password != null) {
				// Explicit username / password takes precedence
				result.password = explicitCredentials.password;
			}
			// override username if explicitly set
			if (!"user".equals(explicitCredentials.username)) {
				// But the username can be overridden
				result.username = explicitCredentials.username;
			}
			return result;
		}
		catch (MalformedURLException e) {
			throw new IllegalStateException("Invalid URL: " + uri);
		}
	}

	private Credentials getUsernamePassword() {
		Credentials credentials = new Credentials();

		if (StringUtils.hasText(this.password)) {
			credentials.password = this.password.trim();
		}

		if (StringUtils.hasText(this.username)) {
			credentials.username = this.username.trim();
		}
		else {
			credentials.username = "user";
		}
		return credentials;
	}

	public static class Credentials {

		private String username;
		private String password;
		private String uri;

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}

		public String getUri() {
			return uri;
		}
	}

	public static class Discovery {
		public static final String DEFAULT_CONFIG_SERVER = "configserver";

		/**
		 * Flag to indicate that config server discovery is enabled (config server URL
		 * will be looked up via discovery).
		 */
		private boolean enabled;
		/**
		 * Service id to locate config server.
		 */
		private String serviceId = DEFAULT_CONFIG_SERVER;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getServiceId() {
			return this.serviceId;
		}

		public void setServiceId(String serviceId) {
			this.serviceId = serviceId;
		}

	}

	public ConfigClientProperties override(
			org.springframework.core.env.Environment environment) {
		ConfigClientProperties override = new ConfigClientProperties();
		BeanUtils.copyProperties(this, override);
		override.setName(
				environment.resolvePlaceholders("${" + ConfigClientProperties.PREFIX
						+ ".name:${spring.application.name:application}}"));
		if (environment.containsProperty(ConfigClientProperties.PREFIX + ".profile")) {
			override.setProfile(
					environment.getProperty(ConfigClientProperties.PREFIX + ".profile"));
		}
		if (environment.containsProperty(ConfigClientProperties.PREFIX + ".label")) {
			override.setLabel(
					environment.getProperty(ConfigClientProperties.PREFIX + ".label"));
		}
		return override;
	}

	@Override
	public String toString() {
		return "ConfigClientProperties [enabled=" + enabled + ", profile=" + profile
				+ ", name=" + name + ", label=" + label + ", username=" + username
				+ ", password=" + password + ", uri=" + Arrays.toString(uri)
				+ ", discovery=" + discovery + ", failFast=" + failFast + ", token="
				+ token + ", requestReadTimeout=" + requestReadTimeout + ", sendState="
				+ sendState + ", headers=" + headers + "]";
	}

	

}
