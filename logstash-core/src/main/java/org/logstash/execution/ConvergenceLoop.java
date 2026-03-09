/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.logstash.execution;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Pure Java implementation of the Logstash convergence loop.
 *
 * <p>This is the heart of the agent's pipeline management. Each convergence
 * cycle performs the following steps:</p>
 * <ol>
 *   <li>Fetch the desired pipeline configurations from the {@link ConfigSource}</li>
 *   <li>Resolve the actions needed via the {@link StateResolver}</li>
 *   <li>Execute actions in parallel using a thread pool</li>
 *   <li>Collect results and notify the {@link EventDispatcher}</li>
 * </ol>
 * <p>The convergence lock ensures that only one convergence cycle runs at a time.</p>
 */
public class ConvergenceLoop {

    private static final Logger LOGGER = LogManager.getLogger(ConvergenceLoop.class);

    /**
     * Default timeout for individual action execution, in minutes.
     */
    private static final long ACTION_TIMEOUT_MINUTES = 5;

    private final ConfigSource configSource;
    private final StateResolver stateResolver;
    private final PipelinesRegistry registry;
    private final EventDispatcher eventDispatcher;
    private final Object convergenceLock = new Object();

    /**
     * Constructs a new ConvergenceLoop.
     *
     * @param configSource    the source from which to fetch pipeline configurations
     * @param stateResolver   the resolver that computes the actions needed
     * @param registry        the pipelines registry tracking current pipeline states
     * @param eventDispatcher the event dispatcher for lifecycle notifications
     */
    public ConvergenceLoop(ConfigSource configSource,
                           StateResolver stateResolver,
                           PipelinesRegistry registry,
                           EventDispatcher eventDispatcher) {
        this.configSource = configSource;
        this.stateResolver = stateResolver;
        this.registry = registry;
        this.eventDispatcher = eventDispatcher;
    }

    /**
     * Execute a single convergence cycle.
     * <p>
     * Thread-safe: uses a convergence lock to prevent concurrent cycles.
     * If the config source returns empty (fetch failed), the cycle is skipped
     * and a result with {@code configFetchSucceeded = false} is returned.
     * </p>
     *
     * @return the result of the convergence cycle
     */
    public ConvergenceResult converge() {
        Optional<Map<String, Object>> configs = configSource.fetch();
        if (configs.isEmpty()) {
            LOGGER.warn("Config fetch returned empty; skipping convergence cycle");
            return new ConvergenceResult(null, Collections.emptyList(),
                    Instant.now(), false);
        }

        synchronized (convergenceLock) {
            List<PipelineAction> actions = stateResolver.resolve(registry, configs.get());

            if (actions.isEmpty()) {
                LOGGER.debug("No pipeline actions to execute in this convergence cycle");
                ConvergeResult emptyResult = new ConvergeResult(0);
                return new ConvergenceResult(emptyResult, Collections.emptyList(),
                        Instant.now(), true);
            }

            LOGGER.info("Converging pipelines state, {} action(s) to execute", actions.size());
            ConvergeResult result = executeActions(actions);

            if (result.failsCount() > 0) {
                LOGGER.error("Failed to execute {} out of {} pipeline action(s)",
                        result.failsCount(), actions.size());
            }

            eventDispatcher.fire("converge_complete", result);

            return new ConvergenceResult(result, actions, Instant.now(), true);
        }
    }

    /**
     * Execute actions in parallel using an ExecutorService.
     * <p>
     * Each action is submitted to a thread pool for concurrent execution.
     * The pool size is bounded by the number of available processors and
     * the number of actions.
     * </p>
     *
     * @param actions the list of pipeline actions to execute
     * @return the converge result with per-action outcomes
     */
    private ConvergeResult executeActions(List<PipelineAction> actions) {
        ConvergeResult result = new ConvergeResult(actions.size());

        if (actions.isEmpty()) {
            return result;
        }

        int poolSize = Math.min(actions.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, poolSize));

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (PipelineAction action : actions) {
                futures.add(executor.submit(() -> {
                    try {
                        LOGGER.debug("Executing pipeline action: {}", action);
                        ConvergeResult.ActionResult actionResult = action.execute(null);
                        result.add(action, actionResult);
                        LOGGER.debug("Pipeline action completed: {} (success={})",
                                action, actionResult.isSuccessful());
                    } catch (Exception e) {
                        LOGGER.error("Pipeline action failed with exception: {}", action, e);
                        result.add(action, ConvergeResult.FailedAction.fromException(e));
                    }
                }));
            }

            for (Future<?> future : futures) {
                try {
                    future.get(ACTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                } catch (TimeoutException e) {
                    LOGGER.warn("Pipeline action timed out after {} minutes", ACTION_TIMEOUT_MINUTES);
                } catch (InterruptedException e) {
                    LOGGER.warn("Pipeline action execution interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.error("Pipeline action execution error", e);
                }
            }
        } finally {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    LOGGER.warn("Executor did not terminate within 30 seconds");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return result;
    }
}
