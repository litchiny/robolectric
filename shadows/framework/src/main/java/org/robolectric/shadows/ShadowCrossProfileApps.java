package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.P;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.CrossProfileApps;
import android.content.pm.ICrossProfileApps;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

/** Robolectric implementation of {@link CrossProfileApps}. */
@Implements(value = CrossProfileApps.class, minSdk = P)
public class ShadowCrossProfileApps {

  @RealObject CrossProfileApps realObject;

  private final List<UserHandle> targetUserProfiles = new ArrayList<>();
  private final List<StartedMainActivity> startedMainActivities = new ArrayList<>();

  @Implementation
  protected void __constructor__(Context context, ICrossProfileApps service) {}

  @Implementation
  public List<UserHandle> getTargetUserProfiles() {
    return ImmutableList.copyOf(targetUserProfiles);
  }

  @Implementation
  public Drawable getProfileSwitchingIconDrawable(UserHandle userHandle) {
    verifyCanAccessUser(userHandle);
    return new ColorDrawable(Color.BLUE);
  }

  @Implementation
  public CharSequence getProfileSwitchingLabel(UserHandle userHandle) {
    verifyCanAccessUser(userHandle);
    return "Switch to " + userHandle;
  }

  @Implementation
  public void startMainActivity(ComponentName componentName, UserHandle targetUser) {
    verifyCanAccessUser(targetUser);
    startedMainActivities.add(new StartedMainActivity(componentName, targetUser));
  }

  /** Adds {@code userHandle} to the list of accessible handles. */
  public void addTargetUserProfile(UserHandle userHandle) {
    if (userHandle.equals(Process.myUserHandle())) {
      throw new IllegalArgumentException("Cannot target current user");
    }
    targetUserProfiles.add(userHandle);
  }

  /**
   * Returns the most recent {@link ComponentName}, {@link UserHandle} pair started by {@link
   * CrossProfileApps#startMainActivity(ComponentName, UserHandle)}, wrapped in {@link
   * StartedMainActivity}.
   */
  public StartedMainActivity peekNextStartedMainActivity() {
    if (startedMainActivities.isEmpty()) {
      return null;
    } else {
      return startedMainActivities.get(startedMainActivities.size() - 1);
    }
  }

  private void verifyCanAccessUser(UserHandle userHandle) {
    if (!targetUserProfiles.contains(userHandle)) {
      throw new SecurityException(
          "Not allowed to access "
              + userHandle
              + " (did you forget to call addTargetUserProfile?)");
    }
  }

  /**
   * Container object to hold parameters passed to {@link #startMainActivity(ComponentName,
   * UserHandle)}.
   */
  public static class StartedMainActivity {

    private final ComponentName componentName;
    private final UserHandle userHandle;

    private StartedMainActivity(ComponentName componentName, UserHandle userHandle) {
      this.componentName = componentName;
      this.userHandle = userHandle;
    }

    public ComponentName getComponentName() {
      return componentName;
    }

    public UserHandle getUserHandle() {
      return userHandle;
    }
  }
}
