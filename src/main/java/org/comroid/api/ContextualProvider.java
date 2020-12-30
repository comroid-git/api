package org.comroid.api;

import org.comroid.util.ReflectionHelper;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.comroid.util.ReflectionHelper.extendingClassesCount;

@Experimental
public interface ContextualProvider {
    static ContextualProvider create(Object... members) {
        return new Base(members);
    }

    Iterable<Object> getContextMembers();

    <T> Rewrapper<T> getFromContext(final Class<T> memberType);

    ContextualProvider plus(Object plus);

    @NonExtendable
    default <T> @NotNull T requireFromContext(final Class<T> memberType) throws NoSuchElementException {
        return getFromContext(memberType).requireNonNull(() -> String.format("No member of type %s found", memberType));
    }

    interface Underlying extends ContextualProvider {
        ContextualProvider getUnderlyingContextualProvider();

        @Override
        default <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
            return getUnderlyingContextualProvider().getFromContext(memberType);
        }

        @Override
        default ContextualProvider plus(Object plus) {
            return getUnderlyingContextualProvider().plus(plus);
        }

        @Override
        @NonExtendable
        default Iterable<Object> getContextMembers() {
            return getUnderlyingContextualProvider().getContextMembers();
        }
    }

    @Internal
    class Base implements ContextualProvider {
        private final Set<Object> members = new HashSet<>();

        @Override
        public Collection<Object> getContextMembers() {
            return members;
        }

        protected Base(Object... initialMembers) {
            members.addAll(Arrays.asList(initialMembers));
        }

        @Override
        public final <T> Rewrapper<T> getFromContext(Class<T> memberType) {
            return () -> members.stream()
                    .max(Comparator.comparingInt(member -> extendingClassesCount(member.getClass(), memberType)))
                    .map(Polyfill::<T>uncheckedCast)
                    .orElse(null);
        }

        @Override
        public final ContextualProvider plus(Object plus) {
            members.add(plus);
            return this;
        }
    }
}
