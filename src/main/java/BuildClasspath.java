import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class BuildClasspath implements Runnable {
    private final Logger log;

    public BuildClasspath(Object org_apache_maven_Maven) throws Exception {
        this.log = getLogger(org_apache_maven_Maven);
    }

    public void run() {
        try {
            char separator = System.getProperty("os.name", "").toLowerCase().contains("windows") ? ';' : ':';
            String outputFile = Optional.ofNullable(System.getProperty("classpath.output")).map(String::trim).filter(not(String::isEmpty)).orElse(".classpath");
            Path outputPath = Path.of(outputFile).toAbsolutePath();
            log.info("Writing runtime classpath to {}", outputPath);
            Files.writeString(outputPath, System.getProperty("maven.local.path") + separator + System.getProperty("maven.runtime.classpath"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Logger getLogger(Object maven) throws ClassNotFoundException {
        ClassLoader classLoader = maven.getClass().getClassLoader();
        Class<?> LoggerFactory_class = classLoader.loadClass(LoggerFactory.class.getName());
        LoggerFactoryCLassProxy loggerFactory = proxy(LoggerFactoryCLassProxy.class, getTwinMethodOn(LoggerFactory_class), null);
        return loggerFactory.getLogger(BuildClasspath.class);
    }

    private interface LoggerFactoryCLassProxy {
        Logger getLogger(Class<?> clazz);
    }

    private static UnaryOperator<Method> getTwinMethodOn(Class<?> targetClass) {
        return method -> getTwinMethod(method, targetClass);
    }

    private static Stream<Class<?>> getSuperClassAndInterfaces(Class<?> clazz) {
        Stream<Class<?>> stream;

        if (clazz == Object.class) {
            stream = Stream.empty();
        } else {
            Class<?> superClass = Objects.requireNonNullElse(clazz.getSuperclass(), Object.class);
            stream = Stream.concat(Stream.of(superClass), Arrays.stream(clazz.getInterfaces()));
        }
        return Stream.concat(Stream.of(clazz), stream.flatMap(BuildClasspath::getSuperClassAndInterfaces));
    }

    private static Method getTwinMethod(Method method, Class<?> targetClass) {
        return getSuperClassAndInterfaces(targetClass)
                .map(c -> {
                    try {
                        return c.getMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not found method on class %s or any of its superclasses: %s".formatted(targetClass, method)));
    }

    private static <T> T proxy(Class<? extends T> targetInterface, UnaryOperator<Method> methodMapper, @Nullable Object delegate) {
        return targetInterface.cast(Proxy.newProxyInstance(targetInterface.getClassLoader(), new Class<?>[]{targetInterface}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Method mappedMethod = methodMapper.apply(method);
                Object result = mappedMethod.invoke(delegate, args);
                if (isProxyable(mappedMethod.getReturnType(), method.getReturnType())) {
                    return proxy(method.getReturnType(), getTwinMethodOn(mappedMethod.getReturnType()), result);
                } else {
                    return result;
                }
            }
        }));
    }

    private static boolean isProxyable(Class<?> sourceType, Class<?> targetType) {
        return targetType != sourceType &&
               targetType.isInterface() &&
               targetType.getName().equals(sourceType.getName());
    }

}
