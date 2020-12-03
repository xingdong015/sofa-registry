/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.registry.observer.impl;

import com.alipay.sofa.registry.log.Logger;
import com.alipay.sofa.registry.log.LoggerFactory;
import com.alipay.sofa.registry.observer.Observable;
import com.alipay.sofa.registry.observer.Observer;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author zhuchen
 * @date Nov 25, 2020, 11:16:06 AM
 */
public abstract class AbstractObservable implements Observable {

    protected Logger       logger    = LoggerFactory.getLogger(AbstractObservable.class);

    private ReadWriteLock  lock      = new ReentrantReadWriteLock();
    private List<Observer> observers = new ArrayList<>();

    protected Executor     executors = MoreExecutors.directExecutor();

    public AbstractObservable() {
    }

    public AbstractObservable(Executor executors) {
        this.executors = executors;
    }

    public void setExecutors(Executor executors) {
        this.executors = executors;
    }

    public AbstractObservable setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    @Override
    public void addObserver(Observer observer) {
        try {
            lock.writeLock().lock();
            observers.add(observer);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeObserver(Observer observer) {
        try {
            lock.writeLock().lock();
            observers.remove(observer);
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void notifyObservers(final Object message) {
        Object[] tmpObservers;

        try {
            lock.readLock().lock();
            tmpObservers = observers.toArray();
        } finally {
            lock.readLock().unlock();
        }

        for (final Object observer : tmpObservers) {

            executors.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        ((Observer) observer).update(AbstractObservable.this, message);
                    } catch (Exception e) {
                        logger.error("[notifyObservers][{}]", observer, e);
                    }
                }
            });
        }
    }
}
