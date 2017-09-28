package org.robolectric.shadows;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import java.io.FileNotFoundException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.R;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.TestRunners;
import org.robolectric.annotation.Config;
import org.robolectric.res.android.DataType;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.Strings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.os.Build.VERSION_CODES.N_MR1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.ShadowArscAssetManager.isLegacyAssetManager;

@RunWith(TestRunners.MultiApiSelfTest.class)
@Config(sdk = VERSION_CODES.N_MR1)
public class ShadowAssetManagerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AssetManager assetManager;
  private ShadowArscAssetManager shadowAssetManager;
  private Resources resources;

  @Before
  public void setUp() throws Exception {
    resources = RuntimeEnvironment.application.getResources();
    assetManager = resources.getAssets();
    shadowAssetManager = Shadow.extract(assetManager);
  }

  @Test
  public void assertGetAssetsNotNull() {
    AssetManager.getSystem();
    assertNotNull(assetManager);

    assetManager = RuntimeEnvironment.application.getAssets();
    assertNotNull(assetManager);

    assetManager = resources.getAssets();
    assertNotNull(assetManager);
  }

  @Test
  public void assetsPathListing() throws IOException {
    assertThat(assetManager.list("")).containsExactlyInAnyOrder("assetsHome.txt", "deflatedAsset.xml", "docs", "myFont.ttf", "images", "sounds", "webkit");

    assertThat(assetManager.list("docs")).containsExactlyInAnyOrder("extra");

    assertThat(assetManager.list("docs/extra")).containsExactlyInAnyOrder("testing");

    assertThat(assetManager.list("docs/extra/testing")).containsExactlyInAnyOrder("hello.txt");

    assertThat(assetManager.list("assetsHome.txt")).isEmpty();

    assertThat(assetManager.list("bogus.file")).isEmpty();
  }

  @Test
  public void open_shouldOpenFile() throws IOException {
    final String contents = Strings.fromStream(assetManager.open("assetsHome.txt"));
    assertThat(contents).isEqualTo("assetsHome!");
  }

  @Test
  public void open_withAccessMode_shouldOpenFile() throws IOException {
    final String contents = Strings.fromStream(assetManager.open("assetsHome.txt", AssetManager.ACCESS_BUFFER));
    assertThat(contents).isEqualTo("assetsHome!");
  }

  @Test
  public void openFd_shouldProvideFileDescriptorForAsset() throws Exception {
    AssetFileDescriptor assetFileDescriptor = assetManager.openFd("assetsHome.txt");
    assertThat(Strings.fromStream(assetFileDescriptor.createInputStream())).isEqualTo("assetsHome!");
    assertThat(assetFileDescriptor.getLength()).isEqualTo(11);
  }

  @Test
  public void openFd_shouldProvideFileDescriptorForDeflatedAsset() throws Exception {
    expectedException.expect(FileNotFoundException.class);
    expectedException.expectMessage("This file can not be opened as a file descriptor; it is probably compressed");

    assetManager.openFd("deflatedAsset.xml");
  }

  @Test
  public void openNonAssetShouldOpenRealAssetFromResources() throws IOException {
    InputStream inputStream = assetManager.openNonAsset(0, "res/drawable/an_image.png", 0);

    // TODO: different sizes in binary vs file resources
    // assertThat(countBytes(inputStream)).isEqualTo(6559);
    assertThat(countBytes(inputStream)).isEqualTo(5138);
  }

  private static int countBytes(InputStream i) throws IOException {
    int count = 0;
    while (i.read() != -1) {
      count++;
    }
    i.close();
    return count;
  }

  @Test
  public void openNonAssetShouldOpenRealAssetFromAndroidJar() throws IOException {
    if (!isLegacyAssetManager(assetManager)) return;

    // Not the real full path (it's in .m2/repository), but it only cares about the last folder and file name
    final String jarFile = "jar:/android-all-5.0.0_r2-robolectric-0.jar!/res/drawable-hdpi/bottom_bar.png";

    InputStream inputStream = assetManager.openNonAsset(0, jarFile, 0);
    assertThat(countBytes(inputStream)).isEqualTo(389);
  }

  @Test
  public void openNonAssetShouldThrowExceptionWhenFileDoesNotExist() throws IOException {
    if (!isLegacyAssetManager(assetManager)) return;

    expectedException.expect(FileNotFoundException.class);
    expectedException.expectMessage("./res/drawable/does_not_exist.png");

    assetManager.openNonAsset(0, "./res/drawable/does_not_exist.png", 0);
  }

  @Test
  public void unknownResourceIdsShouldReportPackagesSearched() throws IOException {
    if (!isLegacyAssetManager(assetManager)) return;

    expectedException.expect(Resources.NotFoundException.class);
    expectedException.expectMessage("Unable to find resource ID #0xffffffff in packages [android, org.robolectric]");

    resources.newTheme().applyStyle(-1, false);
    assetManager.openNonAsset(0, "./res/drawable/does_not_exist.png", 0);
  }

  @Test
  public void forSystemResources_unknownResourceIdsShouldReportPackagesSearched() throws IOException {
    if (!isLegacyAssetManager(assetManager)) return;

    expectedException.expect(Resources.NotFoundException.class);
    expectedException.expectMessage("Unable to find resource ID #0xffffffff in packages [android]");

    Resources.getSystem().newTheme().applyStyle(-1, false);
    assetManager.openNonAsset(0, "./res/drawable/does_not_exist.png", 0);
  }

  @Test
  @Config(qualifiers = "mdpi")
  public void openNonAssetShouldOpenCorrectAssetBasedOnQualifierMdpi() throws IOException {
    if (!isLegacyAssetManager(assetManager)) return;

    InputStream inputStream = assetManager.openNonAsset(0, "./res/drawable/robolectric.png", 0);
    assertThat(countBytes(inputStream)).isEqualTo(8141);
  }

  @Test
  @Config(qualifiers = "hdpi")
  public void openNonAssetShouldOpenCorrectAssetBasedOnQualifierHdpi() throws IOException {
    if (!isLegacyAssetManager(assetManager)) return;

    InputStream inputStream = assetManager.openNonAsset(0, "./res/drawable/robolectric.png", 0);
    assertThat(countBytes(inputStream)).isEqualTo(23447);
  }

  @Test
  public void attrsToTypedArray_shouldAllowMockedAttributeSets() throws Exception {
    if (!isLegacyAssetManager(assetManager)) return;

    AttributeSet mockAttributeSet = mock(AttributeSet.class);
    when(mockAttributeSet.getAttributeCount()).thenReturn(1);
    when(mockAttributeSet.getAttributeNameResource(0)).thenReturn(android.R.attr.windowBackground);
    when(mockAttributeSet.getAttributeName(0)).thenReturn("android:windowBackground");
    when(mockAttributeSet.getAttributeValue(0)).thenReturn("value");

    resources.obtainAttributes(mockAttributeSet, new int[]{android.R.attr.windowBackground});
  }

  @Test
  public void forUntouchedThemes_copyTheme_shouldCopyNothing() throws Exception {
    Resources.Theme theme1 = resources.newTheme();
    Resources.Theme theme2 = resources.newTheme();
    theme2.setTo(theme1);
  }

//  @Test
//  public void whenStyleAttrResolutionFails_attrsToTypedArray_returnsNiceErrorMessage() throws Exception {
//    expectedException.expect(RuntimeException.class);
//    expectedException.expectMessage(
//        "no value for org.robolectric:attr/styleNotSpecifiedInAnyTheme " +
//            "in theme with applied styles: [Style org.robolectric:Theme_Robolectric (and parents)]");
//
//    Resources.Theme theme = resources.newTheme();
//    theme.applyStyle(R.style.Theme_Robolectric, false);
//
//    shadowAssetManager.attrsToTypedArray(resources,
//        Robolectric.buildAttributeSet().setStyleAttribute("?attr/styleNotSpecifiedInAnyTheme").build(),
//        new int[]{R.attr.string1}, 0, shadowOf(theme).getNativePtr(), 0);
//  }

  @Test
  public void getResourceIdentifier_shouldReturnValueFromRClass() throws Exception {
    assertThat(
        shadowAssetManager.getResourceIdentifier("id_declared_in_item_tag", "id", "org.robolectric"))
        .isEqualTo(R.id.id_declared_in_item_tag);
    assertThat(shadowAssetManager
        .getResourceIdentifier("id/id_declared_in_item_tag", null, "org.robolectric"))
        .isEqualTo(R.id.id_declared_in_item_tag);
    assertThat(shadowAssetManager
        .getResourceIdentifier("org.robolectric:id_declared_in_item_tag", "id", null))
        .isEqualTo(R.id.id_declared_in_item_tag);
    assertThat(shadowAssetManager
        .getResourceIdentifier("org.robolectric:id/id_declared_in_item_tag", "other", "other"))
        .isEqualTo(R.id.id_declared_in_item_tag);
  }

  @Test
  public void whenPackageIsUnknown_getResourceIdentifier_shouldReturnZero() throws Exception {
    assertThat(shadowAssetManager.getResourceIdentifier("whatever", "id", "some.unknown.package"))
        .isEqualTo(0);
    assertThat(shadowAssetManager.getResourceIdentifier("id/whatever", null, "some.unknown.package"))
        .isEqualTo(0);
    assertThat(shadowAssetManager.getResourceIdentifier("some.unknown.package:whatever", "id", null))
        .isEqualTo(0);
    assertThat(shadowAssetManager
        .getResourceIdentifier("some.unknown.package:id/whatever", "other", "other"))
        .isEqualTo(0);

    assertThat(
        shadowAssetManager.getResourceIdentifier("whatever", "drawable", "some.unknown.package"))
        .isEqualTo(0);
    assertThat(
        shadowAssetManager.getResourceIdentifier("drawable/whatever", null, "some.unknown.package"))
        .isEqualTo(0);
    assertThat(
        shadowAssetManager.getResourceIdentifier("some.unknown.package:whatever", "drawable", null))
        .isEqualTo(0);
    assertThat(shadowAssetManager
        .getResourceIdentifier("some.unknown.package:id/whatever", "other", "other"))
        .isEqualTo(0);
  }

  @Test @Ignore("currently ids are always automatically assigned a value; to fix this we'd need to check layouts for +@id/___, which is expensive")
  public void whenCalledForIdWithNameNotInRClassOrXml_getResourceIdentifier_shouldReturnZero() throws Exception {
    assertThat(shadowAssetManager
        .getResourceIdentifier("org.robolectric:id/idThatDoesntExistAnywhere", "other", "other"))
        .isEqualTo(0);
  }

  @Test
  public void whenIdIsAbsentInXmlButPresentInRClass_getResourceIdentifier_shouldReturnIdFromRClass_probablyBecauseItWasDeclaredInALayout() throws Exception {
    assertThat(
        shadowAssetManager.getResourceIdentifier("id_declared_in_layout", "id", "org.robolectric"))
        .isEqualTo(R.id.id_declared_in_layout);
  }

  @Test
  public void whenResourceIsAbsentInXml_getResourceIdentifier_shouldReturn0() throws Exception {
    assertThat(
        shadowAssetManager.getResourceIdentifier("fictitiousDrawable", "drawable", "org.robolectric"))
        .isEqualTo(0);
  }

  @Test
  public void whenResourceIsAbsentInXml_getResourceIdentifier_shouldReturnId() throws Exception {
    assertThat(shadowAssetManager.getResourceIdentifier("an_image", "drawable", "org.robolectric"))
        .isEqualTo(R.drawable.an_image);
  }

  @Test
  public void whenResourceIsXml_getResourceIdentifier_shouldReturnId() throws Exception {
    assertThat(shadowAssetManager.getResourceIdentifier("preferences", "xml", "org.robolectric"))
        .isEqualTo(R.xml.preferences);
  }

  @Test
  public void whenResourceIsRaw_getResourceIdentifier_shouldReturnId() throws Exception {
    assertThat(shadowAssetManager.getResourceIdentifier("raw_resource", "raw", "org.robolectric"))
        .isEqualTo(R.raw.raw_resource);
  }

  @Test
  public void getResourceValue_boolean() {
    TypedValue outValue = new TypedValue();
    assertThat(shadowAssetManager.getResourceValue(R.bool.false_bool_value, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.INT_BOOLEAN.code());
    assertThat(outValue.data).isEqualTo(0);

    outValue = new TypedValue();
    assertThat(shadowAssetManager.getResourceValue(R.bool.true_as_item, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.INT_BOOLEAN.code());
    assertThat(outValue.data).isNotEqualTo(0);
  }

  @Test
  public void getResourceValue_int() {
    TypedValue outValue = new TypedValue();
    assertThat(shadowAssetManager.getResourceValue(R.integer.test_integer1, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.INT_DEC.code());
    assertThat(outValue.data).isEqualTo(2000);
  }

  @Test
  public void getResourceValue_intHex() {
    TypedValue outValue = new TypedValue();
    assertThat(shadowAssetManager.getResourceValue(R.integer.hex_int, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.INT_HEX.code());
    assertThat(outValue.data).isEqualTo(0xFFFF0000);
  }

  @Test
  public void getResourceValue_fraction() {
    TypedValue outValue = new TypedValue();
    assertThat(shadowAssetManager.getResourceValue(R.fraction.half, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.FRACTION.code());
    assertThat(outValue.getFraction(1, 1)).isEqualTo(0.5f);
  }

  @Test
  public void getResourceValue_dimension() {
    TypedValue outValue = new TypedValue();
    assertThat(shadowAssetManager.getResourceValue(R.dimen.test_px_dimen, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.DIMENSION.code());
    assertThat(outValue.getDimension(new DisplayMetrics())).isEqualTo(15);
  }

  @Test
  public void getResourceValue_colorARGB8() {
    TypedValue outValue = new TypedValue();
    assertThat(shadowAssetManager.getResourceValue(R.color.test_ARGB8, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.INT_COLOR_ARGB8.code());
    assertThat(Color.blue(outValue.data)).isEqualTo(2);
  }

  @Test
  public void getResourceValue_colorRGB8() {
    TypedValue outValue = new TypedValue();
    assertThat(shadowAssetManager.getResourceValue(R.color.test_RGB8, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.INT_COLOR_RGB8.code());
    assertThat(Color.blue(outValue.data)).isEqualTo(4);
  }

  @Test
  public void getResourceValue_string() {
    TypedValue outValue = new TypedValue();
    assertThat(shadowAssetManager.getResourceValue(R.string.hello, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.STRING.code());
    assertThat(outValue.string).isEqualTo("Hello");
  }

  @Test
  public void getResourceValue_frameworkString() {
    TypedValue outValue = new TypedValue();
    assertThat(shadowAssetManager.getResourceValue(android.R.string.ok, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.STRING.code());
    assertThat(outValue.string).isEqualTo("OK");
  }

  @Test @Config(sdk = N_MR1) // todo unpin
  public void getResourceValue_fromSystem() {
    TypedValue outValue = new TypedValue();
    ShadowArscAssetManager systemShadowAssetManager = shadowOf(AssetManager.getSystem());
    assertThat(systemShadowAssetManager.getResourceValue(android.R.string.ok, 0, outValue, false)).isTrue();
    assertThat(outValue.type).isEqualTo(DataType.STRING.code());
    assertThat(outValue.string).isEqualTo("OK");
  }
}
