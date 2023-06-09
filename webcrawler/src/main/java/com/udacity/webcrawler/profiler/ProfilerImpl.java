package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    // The interface contains should contain @Profiled method
    if (!isValidProfilerInterface(klass)) {
      throw new IllegalArgumentException("Class/Interface should have at least one @Profiled method");
    }

    ProfilingMethodInterceptor interceptor = new ProfilingMethodInterceptor(clock, delegate, state, startTime);
    Object proxy = Proxy.newProxyInstance(
            ProfilerImpl.class.getClassLoader(),
            new Class[] {klass},
            interceptor
    );

    return (T) proxy;
  }

  private boolean isValidProfilerInterface(Class<?> clazz) {
    Method[] methods = clazz.getDeclaredMethods();

    for (Method method : methods) {
      if (method.isAnnotationPresent(Profiled.class)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void writeData(Path path) {
    try (Writer writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      writeData(writer);
    } catch (Exception e) {
      System.out.println("Got error in method ProfilerImpl.writeData(Path path), error message = " + e.getMessage());
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write(System.lineSeparator());
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
