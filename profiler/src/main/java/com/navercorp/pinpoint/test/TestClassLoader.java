/*
 * Copyright 2014 NAVER Corp.
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

package com.navercorp.pinpoint.test;

import com.navercorp.pinpoint.bootstrap.Agent;
import com.navercorp.pinpoint.bootstrap.instrument.ByteCodeInstrumentor;
import com.navercorp.pinpoint.profiler.DefaultAgent;
import com.navercorp.pinpoint.profiler.interceptor.bci.JavaAssistByteCodeInstrumentor;
import com.navercorp.pinpoint.profiler.modifier.AbstractModifier;

import javassist.CannotCompileException;
import javassist.Loader;
import javassist.NotFoundException;

import org.junit.runners.model.InitializationError;

/**
 * @author emeroad
 * @author hyungil.jeong
 */
public class TestClassLoader extends Loader {
    private Agent agent;
    private ByteCodeInstrumentor instrumentor;
    private InstrumentTranslator instrumentTranslator;

    public TestClassLoader(DefaultAgent agent) {
        if (agent == null) {
            throw new NullPointerException("agent must not be null");
        }
        this.agent = agent;
        this.instrumentor = agent.getByteCodeInstrumentor();
        this.instrumentTranslator = new InstrumentTranslator(this, agent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    public void initialize() throws InitializationError {
        addDefaultDelegateLoadingOf();
        addTranslator();
    }

    public Agent getAgent() {
        if (this.agent == null) {
            throw new IllegalStateException("TestClassLoader is not initialized.");
        }
        return agent;
    }

    public ByteCodeInstrumentor getInstrumentor() {
        if (this.instrumentor == null) {
            throw new IllegalStateException("TestClassLoader is not initialized.");
        }
        return instrumentor;
    }

    public AbstractModifier addModifier(AbstractModifier modifier) {
        if (this.instrumentTranslator == null) {
            throw new IllegalStateException("TestClassLoader is not initialized.");
        }
        return this.instrumentTranslator.addModifier(modifier);
    }

    private void addDefaultDelegateLoadingOf() {
        this.delegateLoadingOf("com.navercorp.pinpoint.bootstrap.");
        this.delegateLoadingOf("com.navercorp.pinpoint.common.");
        this.delegateLoadingOf("com.navercorp.pinpoint.thrift.");
        this.delegateLoadingOf("com.navercorp.pinpoint.profiler.context.");
        this.delegateLoadingOf("com.navercorp.pinpoint.test.PeekableDataSender");
        this.delegateLoadingOf("com.navercorp.pinpoint.test.junit4.IsRootSpan");
        this.delegateLoadingOf("org.apache.thrift.TBase");
        this.delegateLoadingOf("junit.");
        this.delegateLoadingOf("org.hamcrest.");
        this.delegateLoadingOf("org.junit.");
    }

    @Override
    protected Class<?> loadClassByDelegation(String name) throws ClassNotFoundException {
        return super.loadClassByDelegation(name);
    }

    private void addTranslator() {
        try {
            addTranslator(((JavaAssistByteCodeInstrumentor)instrumentor).getClassPool(), instrumentTranslator);
        } catch (NotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (CannotCompileException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void runTest(String className, String methodName) throws Throwable {
        Class<?> c = loadClass(className);
        Object o = c.newInstance();
        try {
            c.getDeclaredMethod(methodName).invoke(o);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
