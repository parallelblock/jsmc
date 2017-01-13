package io.ibj.jsmc.api;

import java.util.Collection;

// todo - tests

/**
 *
 * Able to consume {@link Dependency}s. Also able to reevaluate all of the dependencies in which it holds
 *
 * @author Joseph Hirschfeld (Ichbinjoe) [joe@ibj.io]
 * @since 9/2/16
 */
public interface DependencyConsumer {

    /**
     * Returns a collection of all dependencies to which this consumer holds
     * @return Collection of necessary dependencies
     */
    Collection<Dependency> getDependencies();

    /**
     * Reevaluates the consumer's dependence on its dependencies, usually triggered by an upstream dependency change. If
     * any consumers depend on this module, then those modules must be triggered to reevaluate as well.
     *
     * At the beginning of a reevaluate sequence, a clean collection must be passed in to begin evaluation. As each
     * consumer reevaluates, it must add itself to the previouslyEvaluatedConsumers. If triggering more consumers,
     * implementations must not trigger ones which are already present in 'previouslyEvaluatedConsumers'
     * @param previouslyEvaluatedConsumers Collection of consumers which have already been evaluated, and should not be
     *                                     reevaluated
     */
    void reevaluate(Collection<DependencyConsumer> previouslyEvaluatedConsumers);

}
