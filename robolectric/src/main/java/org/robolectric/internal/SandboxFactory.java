package org.robolectric.internal;

import android.annotation.SuppressLint;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.robolectric.ApkLoader;
import org.robolectric.internal.bytecode.InstrumentationConfiguration;
import org.robolectric.internal.bytecode.SandboxClassLoader;
import org.robolectric.internal.dependency.DependencyResolver;

@SuppressLint("NewApi")
public class SandboxFactory {
  public static final SandboxFactory INSTANCE = new SandboxFactory();

  /** The factor for cache size. See {@link #CACHE_SIZE} for details. */
  private static final int CACHE_SIZE_FACTOR = 3;

  /** We need to set the cache size of class loaders more than the number of supported APIs as different tests may have different configurations. */
  private static final int CACHE_SIZE = SdkConfig.getSupportedApis().size() * CACHE_SIZE_FACTOR;

  // Simple LRU Cache. Sandboxes are unique across InstrumentationConfiguration and SdkConfig.
  private final LinkedHashMap<SandboxKey, AndroidSandbox> sdkToEnvironment = new LinkedHashMap<SandboxKey, AndroidSandbox>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<SandboxKey, AndroidSandbox> eldest) {
      return size() > CACHE_SIZE;
    }
  };

  public synchronized AndroidSandbox getSandbox(
      InstrumentationConfiguration instrumentationConfig, SdkConfig sdkConfig,
      boolean useLegacyResources, DependencyResolver dependencyResolver,
      ApkLoader apkLoader) {
    SandboxKey key = new SandboxKey(sdkConfig, instrumentationConfig, useLegacyResources);

    AndroidSandbox androidSandbox = sdkToEnvironment.get(key);
    if (androidSandbox == null) {
      URL[] urls = dependencyResolver.getLocalArtifactUrls(sdkConfig.getAndroidSdkDependency());

      ClassLoader robolectricClassLoader = createClassLoader(instrumentationConfig, urls);
      androidSandbox = createSandbox(sdkConfig, useLegacyResources, robolectricClassLoader, apkLoader);

      sdkToEnvironment.put(key, androidSandbox);
    }
    return androidSandbox;
  }

  protected AndroidSandbox createSandbox(SdkConfig sdkConfig, boolean useLegacyResources,
      ClassLoader robolectricClassLoader, ApkLoader apkLoader) {
    return new AndroidSandbox(sdkConfig, useLegacyResources, robolectricClassLoader, apkLoader);
  }

  @Nonnull
  public ClassLoader createClassLoader(InstrumentationConfiguration instrumentationConfig, URL... urls) {
    return new SandboxClassLoader(ClassLoader.getSystemClassLoader(), instrumentationConfig, urls);
  }

  static class SandboxKey {
    private final SdkConfig sdkConfig;
    private final InstrumentationConfiguration instrumentationConfiguration;
    private final boolean useLegacyResources;

    public SandboxKey(SdkConfig sdkConfig,
        InstrumentationConfiguration instrumentationConfiguration, boolean useLegacyResources) {
      this.sdkConfig = sdkConfig;
      this.instrumentationConfiguration = instrumentationConfiguration;
      this.useLegacyResources = useLegacyResources;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SandboxKey that = (SandboxKey) o;
      return useLegacyResources == that.useLegacyResources
          && Objects.equals(sdkConfig, that.sdkConfig)
          && Objects.equals(instrumentationConfiguration, that.instrumentationConfiguration);
    }

    @Override
    public int hashCode() {

      return Objects.hash(sdkConfig, instrumentationConfiguration, useLegacyResources);
    }
  }
}
