/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.config.server.support;

import java.io.File;

import org.springframework.core.Ordered;

/**
 * @author Dylan Roberts
 */
public class AbstractScmAccessorProperties implements EnvironmentRepositoryProperties {
    static final String[] DEFAULT_LOCATIONS = new String[] { "/" };

    /**
     * URI of remote repository.
     *
     * 远程存储库的URI。
     */
    private String uri;
    /**
     * Base directory for local working copy of repository.
     *
     * 存储库的本地工作副本的基本目录。
     */
    private File basedir;
    /**
     * Search paths to use within local working copy. By default searches only the root.
     *
     * 搜索在本地工作副本中使用的路径。 默认情况下，仅搜索根。
     */
    private String[] searchPaths = DEFAULT_LOCATIONS.clone();;
    /**
     * Username for authentication with remote repository.
     *
     * 用于远程存储库验证的用户名。
     */
    private String username;
    /**
     * Password for authentication with remote repository.
     *
     * 远程存储库验证密码。
     */
    private String password;
    /**
     * Passphrase for unlocking your ssh private key.
     *
     * 用于解锁ssh私钥的密码。
     */
    private String passphrase;
    /**
     * Reject incoming SSH host keys from remote servers not in the known host list.
     *
     * 从不在已知主机列表中的远程服务器拒绝传入的SSH主机密钥。
     */
    private boolean strictHostKeyChecking = true;

    /**
     * The order of the environment repository.
     *
     * 环境存储库的顺序。
     */
    private int order = Ordered.LOWEST_PRECEDENCE;

    /**
     * The default label to be used with the remore repository
     *
     * 与远程存储库一起使用的默认标签
     */
    private String defaultLabel;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public File getBasedir() {
        return basedir;
    }

    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    public String[] getSearchPaths() {
        return searchPaths;
    }

    public void setSearchPaths(String... searchPaths) {
        this.searchPaths = searchPaths;
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

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public boolean isStrictHostKeyChecking() {
        return strictHostKeyChecking;
    }

    public void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
        this.strictHostKeyChecking = strictHostKeyChecking;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public void setOrder(int order) {
        this.order = order;
    }

    public String getDefaultLabel() {
        return defaultLabel;
    }

    public void setDefaultLabel(String defaultLabel) {
        this.defaultLabel = defaultLabel;
    }
}
