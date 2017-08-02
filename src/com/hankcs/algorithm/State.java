/*
 * AhoCorasickDoubleArrayTrie Project
 *      https://github.com/hankcs/AhoCorasickDoubleArrayTrie
 *
 * Copyright 2008-2016 hankcs <me@hankcs.com>
 * You may modify and redistribute as long as this attribution remains.
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

package com.hankcs.algorithm;

import java.util.*;

/**
 * <p>
 * �?��状�?有如下几个功�? * </p>
 * <p/>
 * <ul>
 * <li>success; 成功转移到另�?��状�?</li>
 * <li>failure; 不可顺着字符串跳转的话，则跳转到�?��浅一点的节点</li>
 * <li>emits; 命中�?��模式�?/li>
 * </ul>
 * <p/>
 * <p>
 * 根节点稍有不同，根节点没�?failure 功能，它的�?failure”指的是按照字符串路径转移到下一个状态�?其他节点则都有failure状�?�? * </p>
 *
 * @author Robert Bor
 */
public class State {

    /**
     * 模式串的长度，也是这个状态的深度
     */
    protected final int depth;

    /**
     * fail 函数，如果没有匹配到，则跳转到此状�?�?     */
    private State failure = null;

    /**
     * 只要这个状�?可达，则记录模式�?     */
    private Set<Integer> emits = null;
    /**
     * goto 表，也称转移函数。根据字符串的下�?��字符转移到下�?��状�?
     */
    private Map<Character, State> success = new TreeMap<Character, State>();

    /**
     * 在双数组中的对应下标
     */
    private int index;

    /**
     * 构�?深度�?的节�?     */
    public State() {
        this(0);
    }

    /**
     * 构�?深度为depth的节�?     *
     * @param depth
     */
    public State(int depth) {
        this.depth = depth;
    }

    /**
     * 获取节点深度
     *
     * @return
     */
    public int getDepth() {
        return this.depth;
    }

    /**
     * 添加�?��匹配到的模式串（这个状�?对应�?��个模式串)
     *
     * @param keyword
     */
    public void addEmit(int keyword) {
        if (this.emits == null) {
            this.emits = new TreeSet<Integer>(Collections.reverseOrder());
        }
        this.emits.add(keyword);
    }

    /**
     * 获取�?��的�?
     *
     * @return
     */
    public Integer getLargestValueId() {
        if (emits == null || emits.size() == 0) return null;

        return emits.iterator().next();
    }

    /**
     * 添加�?��匹配到的模式�?     *
     * @param emits
     */
    public void addEmit(Collection<Integer> emits) {
        for (int emit : emits) {
            addEmit(emit);
        }
    }

    /**
     * 获取这个节点代表的模式串（们�?     *
     * @return
     */
    public Collection<Integer> emit() {
        return this.emits == null ? Collections.<Integer>emptyList() : this.emits;
    }

    /**
     * 是否是终止状�?     *
     * @return
     */
    public boolean isAcceptable() {
        return this.depth > 0 && this.emits != null;
    }

    /**
     * 获取failure状�?
     *
     * @return
     */
    public State failure() {
        return this.failure;
    }

    /**
     * 设置failure状�?
     *
     * @param failState
     */
    public void setFailure(State failState, int fail[]) {
        this.failure = failState;
        fail[index] = failState.index;
    }

    /**
     * 转移到下�?��状�?
     *
     * @param character       希望按此字符转移
     * @param ignoreRootState 是否忽略根节点，如果是根节点自己调用则应该是true，否则为false
     * @return 转移结果
     */
    private State nextState(Character character, boolean ignoreRootState) {
        State nextState = this.success.get(character);
        if (!ignoreRootState && nextState == null && this.depth == 0) {
            nextState = this;
        }
        return nextState;
    }

    /**
     * 按照character转移，根节点转移失败会返回自己（永远不会返回null�?     *
     * @param character
     * @return
     */
    public State nextState(Character character) {
        return nextState(character, false);
    }

    /**
     * 按照character转移，任何节点转移失败会返回null
     *
     * @param character
     * @return
     */
    public State nextStateIgnoreRootState(Character character) {
        return nextState(character, true);
    }

    public State addState(Character character) {
        State nextState = nextStateIgnoreRootState(character);
        if (nextState == null) {
            nextState = new State(this.depth + 1);
            this.success.put(character, nextState);
        }
        return nextState;
    }

    public Collection<State> getStates() {
        return this.success.values();
    }

    public Collection<Character> getTransitions() {
        return this.success.keySet();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("State{");
        sb.append("depth=").append(depth);
        sb.append(", ID=").append(index);
        sb.append(", emits=").append(emits);
        sb.append(", success=").append(success.keySet());
        sb.append(", failureID=").append(failure == null ? "-1" : failure.index);
        sb.append(", failure=").append(failure);
        sb.append('}');
        return sb.toString();
    }

    /**
     * 获取goto�?     *
     * @return
     */
    public Map<Character, State> getSuccess() {
        return success;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}