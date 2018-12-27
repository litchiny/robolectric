package org.robolectric.util;

import static com.google.common.truth.Truth.assertThat;

import javax.inject.Inject;
import org.junit.Before;
import org.junit.Test;

public class InjectorTest {

  private Injector injector;

  @Before
  public void setUp() throws Exception {
    injector = new Injector();
  }

  @Test
  public void shouldProvideInstance() throws Exception {
    injector.register(Thing.class, MyThing.class);

    assertThat(injector.getInstance(Thing.class))
        .isInstanceOf(MyThing.class);
  }

  @Test
  public void shouldUseSameInstances() throws Exception {
    injector.register(Thing.class, MyThing.class);

    Thing thing = injector.getInstance(Thing.class);
    assertThat(injector.getInstance(Thing.class))
        .isSameAs(thing);
  }

  // see resources/META-INF/services/org.robolectric.utils.Thing
  @Test
  public void shouldUseImplementationRegisteredAsService() throws Exception {
    assertThat(injector.getInstance(Thing.class))
        .isInstanceOf(ThingFromServiceConfig.class);
  }

  @Test
  public void shouldInjectConstructor() throws Exception {
    injector.register(Thing.class, MyThing.class);
    injector.register(Err.class, MyErr.class);

    Err err = injector.getInstance(Err.class);
    assertThat(err).isNotNull();
    assertThat(err).isInstanceOf(MyErr.class);

    MyErr myErr = (MyErr) err;
    assertThat(myErr.thing).isNotNull();
    assertThat(myErr.thing).isInstanceOf(MyThing.class);

    assertThat(myErr.thing).isSameAs(injector.getInstance(Thing.class));
  }

  /////////////////////////////

  public static class MyThing implements Thing {

  }

  public static class ThingFromServiceConfig implements Thing {

  }

  private interface Err {

  }

  public static class MyErr implements Err {

    private final Thing thing;

    @Inject
    MyErr(Thing thing) {
      this.thing = thing;
    }
  }
}