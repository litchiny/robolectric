package org.robolectric.util;

import static com.sun.xml.internal.fastinfoset.vocab.Vocabulary.PREFIX;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;

@SuppressWarnings("NewApi")
public class Injector {

  private final Map<Key, Provider<?>> providers = new HashMap<>();

  synchronized public <T> void register(Class<T> type, Class<? extends T> defaultClass) {
    providers.put(new Key(type), new MemoizingProvider<>(() -> inject(defaultClass)));
  }

  synchronized public <T> void registerDefaultService(Class<T> type,
      Class<? extends T> defaultClass) {
  }

  private <T> T inject(Class<? extends T> clazz) {
    try {
      Constructor<T> defaultCtor = null;
      Constructor<T> injectCtor = null;

      for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
        if (ctor.getParameterCount() == 0) {
          defaultCtor = (Constructor<T>) ctor;
        } else if (ctor.getAnnotation(Inject.class) != null) {
          if (injectCtor != null) {
            throw new IllegalStateException("multiple @Inject constructors for " + clazz);
          }
          injectCtor = (Constructor<T>) ctor;
        }
      }

      if (defaultCtor != null) {
        return defaultCtor.newInstance();
      }

      if (injectCtor != null) {
        final Object[] params = new Object[injectCtor.getParameterCount()];

        Class<?>[] paramTypes = injectCtor.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
          Class<?> paramType = paramTypes[i];
          params[i] = getInstance(paramType);
        }

        return injectCtor.newInstance(params);
      }

      throw new IllegalStateException("no default or @Inject constructor for " + clazz);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  synchronized private <T> Provider<?> getProvider(Class<T> clazz) {
    Provider<?> provider = providers.get(new Key(clazz));
    if (provider == null) {
      provider = findService(clazz);
    }
    return provider;
  }

  public <T> T getInstance(Class<T> clazz) {
    Provider<?> provider = getProvider(clazz);

    if (provider == null) {
      throw new IllegalStateException("no provider registered for " + clazz);
    }

    return ((Provider<T>) provider).provide();
  }

  private <T> Provider<T> findService(Class<T> serviceType) {
    ClassLoader loader = serviceType.getClassLoader();
    Enumeration<URL> configs;
    try {
      String fullName = PREFIX + serviceType.getName();
      if (loader == null) {
        configs = ClassLoader.getSystemResources(fullName);
      } else {
        configs = loader.getResources(fullName);
      }
    } catch (IOException x) {
      throw new RuntimeException(serviceType + ": Error locating configuration files", x);
    }

    List<URL> urls = new ArrayList<>();
    while (configs.hasMoreElements()) {
      URL url = configs.nextElement();
      urls.add(url);
    }

    if (urls.isEmpty()) {
      return null;
    } else if (urls.size() > 1) {
      throw new IllegalArgumentException(serviceType + ": too many implementations: " + urls);
    }

    URL url = urls.get(0);
    String className = null;
    try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"))) {
      for (String line = in.readLine(); line != null; line = in.readLine()) {
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) {
          if (className != null) {
            throw new IllegalArgumentException(serviceType + ": too many implementations in " + url);
          }

          className = line;
        }
      }
    } catch (IOException x) {
      throw new RuntimeException(serviceType + ": Error reading configuration file", x);
    }

    try {
      Class<T> theClass = (Class<T>) loader.loadClass(className);
      return () -> inject(theClass);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(serviceType + ": no such implementation class", e);
    }
  }

  private static class Key {

    private Class<?> theInterface;

    public <T> Key(Class<T> theInterface) {
      this.theInterface = theInterface;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Key key = (Key) o;
      return Objects.equals(theInterface, key.theInterface);
    }

    @Override
    public int hashCode() {
      return Objects.hash(theInterface);
    }
  }

  private interface Provider<T> {

    T provide();
  }

  private static class MemoizingProvider<T> implements Provider<T> {

    private Provider<T> delegate;
    private T instance;

    public MemoizingProvider(Provider<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    synchronized public T provide() {
      if (instance == null) {
        instance = delegate.provide();
        delegate = null;
      }
      return instance;
    }
  }
}
