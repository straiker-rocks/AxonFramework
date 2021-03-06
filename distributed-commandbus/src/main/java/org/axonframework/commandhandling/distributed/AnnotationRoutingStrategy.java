/*
 * Copyright (c) 2010-2012. Axon Framework
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

package org.axonframework.commandhandling.distributed;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.axonframework.common.AxonConfigurationException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.axonframework.common.ReflectionUtils.*;

/**
 * RoutingStrategy that expects an {@link org.axonframework.commandhandling.annotation.TargetAggregateIdentifier}
 * annotation on the command message's payload. Commands are routed based on the identifier of the aggregate that they
 * target. This approach ensures that commands to be processed by the same aggregate are dispatched to the same node in
 * a DistributedCommandBus. See {@link org.axonframework.commandhandling.annotation.AnnotationCommandTargetResolver}
 * for more details.
 * <p/>
 * This class requires the returned Aggregate Identifiers to implement a proper {@link Object#toString()} method. An
 * inconsistent toString() method may result in different members using different routing keys for the same identifier.
 *
 * @author Allard Buijze
 * @see org.axonframework.commandhandling.annotation.AnnotationCommandTargetResolver
 * @see DistributedCommandBus
 * @since 2.0
 */
public class AnnotationRoutingStrategy extends AbstractRoutingStrategy {

    /**
     * Initializes a Routing Strategy that fails when an incoming command does not define an AggregateIdentifier to
     * base the routing key on.
     */
    public AnnotationRoutingStrategy() {
        super(UnresolvedRoutingKeyPolicy.ERROR);
    }

    /**
     * Initializes a Routing Strategy that uses the given <code>unresolvedRoutingKeyPolicy</code> when an incoming
     * command does not define an AggregateIdentifier to base the routing key on.
     *
     * @param unresolvedRoutingKeyPolicy The policy indication what should be done when a Command does not contain
     *                                   information about the routing key to use.
     */
    public AnnotationRoutingStrategy(UnresolvedRoutingKeyPolicy unresolvedRoutingKeyPolicy) {
        super(unresolvedRoutingKeyPolicy);
    }

    @Override
    protected String doResolveRoutingKey(CommandMessage<?> command) {
        Object aggregateIdentifier;
        try {
            aggregateIdentifier = findIdentifier(command);
        } catch (InvocationTargetException e) {
            throw new AxonConfigurationException("An exception occurred while extracting aggregate "
                                                         + "information form a command", e);
        } catch (IllegalAccessException e) {
            throw new AxonConfigurationException("The current security context does not allow extraction of "
                                                         + "aggregate information from the given command.", e);
        }
        return aggregateIdentifier != null ? aggregateIdentifier.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private <I> I findIdentifier(CommandMessage<?> command) throws InvocationTargetException, IllegalAccessException {
        for (Method m : methodsOf(command.getPayloadType())) {
            if (m.isAnnotationPresent(TargetAggregateIdentifier.class)) {
                ensureAccessible(m);
                return (I) m.invoke(command.getPayload());
            }
        }
        for (Field f : fieldsOf(command.getPayloadType())) {
            if (f.isAnnotationPresent(TargetAggregateIdentifier.class)) {
                return (I) getFieldValue(f, command.getPayload());
            }
        }
        return null;
    }
}
