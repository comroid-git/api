package org.comroid.api;

import org.comroid.annotations.inheritance.MustExtend;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

import static org.comroid.util.StackTraceUtils.callerClass;

@Experimental
@MustExtend(ContextualProvider.Base.class)
public interface ContextualProvider extends Named, Upgradeable<ContextualProvider>, LoggerCarrier {
    @Internal
    default @Nullable ContextualProvider getParentContext() {
        return null;
    }

    @Internal
    @NonExtendable
    default boolean isRoot() {
        return getParentContext() == null;
    }

    static ContextualProvider create(Object... members) {
        return new Base(Base.ROOT, callerClass(1), members);
    }

    static ContextualProvider create(String name, Object... members) {
        return new Base(Base.ROOT, name, members);
    }

    static ContextualProvider create(ContextualProvider parent, Object... members) {
        return new Base((Base) parent, callerClass(1), members);
    }

    static <T> T requireFromContexts(Class<T> member) throws NoSuchElementException {
        return requireFromContexts(member, false);
    }

    static <T> T requireFromContexts(Class<T> member, boolean includeChildren) throws NoSuchElementException {
        return requireFromContexts(member, String.format("No member of type %s found", member));
    }

    static <T> T requireFromContexts(Class<T> member, String message) throws NoSuchElementException {
        return requireFromContexts(member, message, false);
    }

    static <T> T requireFromContexts(Class<T> member, String message, boolean includeChildren) throws NoSuchElementException {
        return getFromContexts(member, includeChildren).orElseThrow(() -> new NoSuchElementException(message));
    }

    static <T> Rewrapper<T> getFromContexts(final Class<T> member) {
        return getFromContexts(member, false);
    }

    static <T> Rewrapper<T> getFromContexts(final Class<T> member, boolean includeChildren) {
        return () -> Base.ROOT.children.stream()
                .flatMap(sub -> sub.getFromContext(member, includeChildren).stream())
                .findFirst()
                .orElse(null);
    }

    @Internal
    static String wrapContextStr(String subStr) {
        return "Context<" + subStr + ">";
    }

    @Deprecated
    default Stream<Object> streamContextMembers() {
        return streamContextMembers(false);
    }

    Stream<Object> streamContextMembers(boolean includeChildren);

    default <T> Serializer<T> findSerializer(@Nullable CharSequence mimetype) {
        return streamContextMembers(true)
                .filter(Serializer.class::isInstance)
                .map(Serializer.class::cast)
                .filter(seri -> mimetype == null || seri.getMimeType().equals(mimetype.toString()))
                .findFirst()
                .map(Polyfill::<Serializer<T>>uncheckedCast)
                .orElseThrow(() -> new NoSuchElementException(String.format("No Serializer with Mime Type %s was found in %s", mimetype, this)));
    }

    @NonExtendable
    default <T> Rewrapper<T> getFromContext(final Class<T> memberType) {
        return getFromContext(memberType, false);
    }

    @NonExtendable
    default <T> Rewrapper<T> getFromContext(final Class<T> memberType, boolean includeChildren) {
        return () -> streamContextMembers(includeChildren)
                .filter(memberType::isInstance)
                .findFirst()
                .map(memberType::cast)
                .orElse(null);
    }

    @NonExtendable
    default ContextualProvider plus() {
        return plus(new Object[0]);
    }

    @NonExtendable
    default ContextualProvider plus(Object... plus) {
        return plus(callerClass(1).getSimpleName(), plus);
    }

    @NonExtendable
    default ContextualProvider plus(String name, Object... plus) {
        return new Base(this, name, plus);
    }

    default boolean addToContext(Object... plus) {
        return false;
    }

    @Override
    default <R extends ContextualProvider> Optional<R> as(Class<R> type) {
        if (isType(type))
            return Optional.ofNullable(type.cast(self().get()));
        return getFromContext(type).wrap();
    }

    @NonExtendable
    default <T> @NotNull T requireFromContext(final Class<? super T> memberType) throws NoSuchElementException {
        return requireFromContext(memberType, false);
    }

    @NonExtendable
    default <T> @NotNull T requireFromContext(final Class<? super T> memberType, boolean includeChildren) throws NoSuchElementException {
        return requireFromContext(memberType, String.format("No member of type %s found in %s", memberType, this));
    }

    @NonExtendable
    default <T> @NotNull T requireFromContext(final Class<? super T> memberType, String message) throws NoSuchElementException {
        return requireFromContext(memberType, message, false);
    }

    @NonExtendable
    default <T> @NotNull T requireFromContext(final Class<? super T> memberType, String message, boolean includeChildren) throws NoSuchElementException {
        return Polyfill.uncheckedCast(getFromContext(memberType, includeChildren).assertion(String.format("<%s => %s>", this, message)));
    }

    interface Underlying extends ContextualProvider {
        ContextualProvider getUnderlyingContextualProvider();

        @Override
        @Nullable
        default ContextualProvider getParentContext() {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.getParentContext();
        }

        @Override
        default String getName() {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.getName();
        }

        @Override
        default Stream<Object> streamContextMembers(boolean includeChildren) {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.streamContextMembers(includeChildren);
        }

        @Override
        default <T> Rewrapper<T> getFromContext(final Class<T> memberType, boolean includeChildren) {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.getFromContext(memberType, includeChildren);
        }

        @Override
        default ContextualProvider plus(String name, Object... plus) {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.plus(name, plus);
        }

        @Override
        default boolean addToContext(Object... plus) {
            ContextualProvider context = getUnderlyingContextualProvider();
            if (context == this)
                throw new IllegalStateException("Bad inheritance: Underlying can't provide itself");
            return context.addToContext(plus);
        }
    }

    @Experimental
    interface This<T> extends ContextualProvider {
        @Override
        default String getName() {
            Class<? extends This> cls = getClass();
            if (cls == null)
                return "uninitialized context";
            return wrapContextStr(cls.getSimpleName());
        }

        default Stream<Object> streamContextMembers(boolean includeChildren) {
            return Stream.of(this);
        }

        @Override
        default <R> Rewrapper<R> getFromContext(final Class<R> memberType) {
            if (memberType.isAssignableFrom(getClass()))
                return () -> Polyfill.uncheckedCast(this);
            return Rewrapper.empty();
        }

        @Override
        default ContextualProvider plus(Object... plus) {
            return create(this, plus);
        }
    }

    @Experimental
    interface Member<T> extends ContextualProvider {
        T getFromContext();

        @Override
        default String getName() {
            return wrapContextStr(getFromContext().getClass().getSimpleName());
        }

        @Override
        default Stream<Object> streamContextMembers(boolean includeChildren) {
            return Stream.of(getFromContext());
        }

        @Override
        default <R> Rewrapper<R> getFromContext(final Class<R> memberType) {
            if (memberType.isAssignableFrom(getFromContext().getClass()))
                return () -> Polyfill.uncheckedCast(getFromContext());
            return Rewrapper.empty();
        }

        @Override
        default ContextualProvider plus(Object... plus) {
            return create(getFromContext(), plus);
        }
    }

    @Internal
    class Base implements ContextualProvider {
        @SuppressWarnings("ConstantConditions")
        public static final ContextualProvider.Base ROOT
                = new ContextualProvider.Base((ContextualProvider.Base) null, "ROOT");
        protected final Set<ContextualProvider> children;
        private final Set<Object> myMembers;
        private final ContextualProvider parent;
        private final String name;

        @Override
        public final @Nullable ContextualProvider getParentContext() {
            return parent;
        }

        @Override
        public String getName() {
            return wrapContextStr(name);
        }

        protected Base(Object... initialMembers) {
            this(ROOT, initialMembers);
        }

        protected Base(@NotNull ContextualProvider parent, Object... initialMembers) {
            this(parent, callerClass(1).getSimpleName(), initialMembers);
        }

        protected Base(String name, Object... initialMembers) {
            this(ROOT, name, initialMembers);
        }

        protected Base(@NotNull ContextualProvider parent, String name, Object... initialMembers) {
            this.myMembers = new HashSet<>();
            this.children = new HashSet<>();
            this.parent = name.equals("ROOT") && callerClass(1).equals(ContextualProvider.Base.class)
                    ? parent
                    : Objects.requireNonNull(parent);
            this.name = name;
            if (!isRoot())
                parent.addToContext(this);
            addToContext(initialMembers);
        }

        @Override
        public final Stream<Object> streamContextMembers(final boolean includeChildren) {
            Stream<Object> stream1 = Stream.concat(
                    Stream.of(parent)
                            .filter(Objects::nonNull)
                            .flatMap(contextualProvider -> contextualProvider.streamContextMembers(false)),
                    Stream.of(this)
            );
            Stream<Object> stream2 = Stream.concat(
                    myMembers.stream(),
                    includeChildren
                            ? children.stream().flatMap(sub -> sub.streamContextMembers(includeChildren))
                            : Stream.empty()
            );
            return Stream.concat(stream1, stream2).filter(Objects::nonNull)
                    .flatMap(it -> {
                        if (it.getClass().isArray())
                            return Stream.of((Object[]) it);
                        return Stream.of(it);
                    }).distinct();
        }

        @Override
        public final boolean addToContext(Object... plus) {
            boolean anyAdded = false;
            for (Object each : plus)
                if (myMembers.add(each))
                    anyAdded = true;
            return anyAdded;
        }
    }
}
