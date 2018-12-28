package org.robolectric;

import java.util.List;
import javax.annotation.Nonnull;
import org.robolectric.annotation.Config;
import org.robolectric.internal.SdkConfig;

@FunctionalInterface
public interface SdkPicker {

  @Nonnull
  List<SdkConfig> selectSdks(Config config, UsesSdk usesSdk);
}
