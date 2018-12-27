package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.P;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.CrossProfileApps;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowCrossProfileApps.StartedMainActivity;

@RunWith(AndroidJUnit4.class)
@Config(sdk = P)
public class ShadowCrossProfileAppsTest {

  private final Context context = ApplicationProvider.getApplicationContext();
  private final CrossProfileApps crossProfileApps =
      context.getSystemService(CrossProfileApps.class);

  private final ComponentName mainActivityComponentName =
      ComponentName.createRelative(context, ".MainActivity");
  private final UserHandle userHandle1 = UserHandle.of(10);
  private final UserHandle userHandle2 = UserHandle.of(11);

  @Test
  public void getTargetUserProfiles_noProfilesAdded_shouldReturnEmpty() {
    assertThat(crossProfileApps.getTargetUserProfiles()).isEmpty();
  }

  @Test
  public void getTargetUserProfiles_oneProfileAdded_shouldReturnProfileAdded() {
    shadowOf(crossProfileApps).addTargetUserProfile(userHandle1);

    assertThat(crossProfileApps.getTargetUserProfiles()).containsExactly(userHandle1);
  }

  @Test
  public void getTargetUserProfiles_multipleProfileAdded_shouldReturnAllProfilesAdded() {
    shadowOf(crossProfileApps).addTargetUserProfile(userHandle1);
    shadowOf(crossProfileApps).addTargetUserProfile(userHandle2);

    assertThat(crossProfileApps.getTargetUserProfiles()).containsExactly(userHandle1, userHandle2);
  }

  @Test
  public void getProfileSwitchingLabel() {
    shadowOf(crossProfileApps).addTargetUserProfile(userHandle1);

    CharSequence label = crossProfileApps.getProfileSwitchingLabel(userHandle1);
    assertThat(label.toString()).isNotEmpty();
  }

  @Test
  public void getProfileSwitchingLabel_userNotAvailable_shouldThrowSecurityException() {
    assertThrowsSecurityException(() -> crossProfileApps.getProfileSwitchingLabel(userHandle1));
  }

  @Test
  public void getProfileSwitchingIconDrawable() {
    shadowOf(crossProfileApps).addTargetUserProfile(userHandle1);

    Drawable icon = crossProfileApps.getProfileSwitchingIconDrawable(userHandle1);
    assertThat(icon).isNotNull();
  }

  @Test
  public void getProfileSwitchingIconDrawable_userNotAvailable_shouldThrowSecurityException() {
    assertThrowsSecurityException(
        () -> crossProfileApps.getProfileSwitchingIconDrawable(userHandle1));
  }

  @Test
  public void startMainActivity() {
    shadowOf(crossProfileApps).addTargetUserProfile(userHandle1);

    crossProfileApps.startMainActivity(mainActivityComponentName, userHandle1);

    StartedMainActivity startedMainActivity =
        shadowOf(crossProfileApps).peekNextStartedMainActivity();
    assertThat(startedMainActivity.getComponentName()).isEqualTo(mainActivityComponentName);
    assertThat(startedMainActivity.getUserHandle()).isEqualTo(userHandle1);
  }

  @Test
  public void startMainActivity_userNotAvailable_shouldThrowSecurityException() {
    assertThrowsSecurityException(
        () -> crossProfileApps.startMainActivity(mainActivityComponentName, userHandle1));
  }

  private static void assertThrowsSecurityException(Runnable runnable) {
    try {
      runnable.run();
    } catch (SecurityException e) {
      // expected
      return;
    }
    fail("did not throw SecurityException");
  }
}
