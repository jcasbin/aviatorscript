package com.googlecode.aviator.code.asm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.googlecode.aviator.parser.AviatorClassLoader;

/**
 * A class definer
 *
 * @author dennis
 *
 */
public class ClassDefiner {

  private static final Object[] EMPTY_OBJS = new Object[] {};
  private static Method DEFINE_CLASS_Method;
  private static Object unsafe;

  static {
    // Try to get defineAnonymousClass method handle.
    try {
      Class<?> clazz = Class.forName("sun.misc.Unsafe");
      if (clazz != null) {
        Field f = clazz.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = f.get(null);
        Method defineAnonymousClass = clazz.getDeclaredMethod("defineAnonymousClass", Class.class,
            byte[].class, Object[].class);
        DEFINE_CLASS_Method = defineAnonymousClass;
      }

    } catch (Throwable e) {
      // ignore
    }
  }

  private static boolean isIBMJdk() {
    String vendor = (System.getProperty("java.vendor"));
    try {
      return vendor != null && vendor.toLowerCase().contains("ibm corporation");
    } catch (Throwable e) {
      return false;
    }
  }

  private static boolean isJDK7() {
    String version = (System.getProperty("java.version"));
    try {
      return version != null && version.startsWith("1.7");
    } catch (Throwable e) {
      return false;
    }
  }

  public static final boolean IS_JDK7 = isJDK7();
  public static final boolean IS_IBM_SDK = isIBMJdk();

  private static boolean preferClassLoader = Boolean.valueOf(System
      .getProperty("aviator.preferClassloaderDefiner", String.valueOf(IS_JDK7 || IS_IBM_SDK)));


  private static int errorTimes = 0;

  public static final Class<?> defineClass(final String className, final Class<?> clazz,
      final byte[] bytes, final AviatorClassLoader classLoader)
      throws NoSuchFieldException, IllegalAccessException {
    if (!preferClassLoader && DEFINE_CLASS_Method != null) {
      try {
        Class<?> defineClass =
            (Class<?>) DEFINE_CLASS_Method.invoke(unsafe, clazz, bytes, EMPTY_OBJS);
        return defineClass;
      } catch (Throwable e) {
        // fallback to class loader mode.
        if (errorTimes++ > 10000) {
          preferClassLoader = true;
        }
        return defineClassByClassLoader(className, bytes, classLoader);
      }
    } else {
      return defineClassByClassLoader(className, bytes, classLoader);
    }
  }

  public static Class<?> defineClassByClassLoader(final String className, final byte[] bytes,
      final AviatorClassLoader classLoader) {
    return classLoader.defineClass(className, bytes);
  }
}
