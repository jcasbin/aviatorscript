package com.googlecode.aviator.runtime.function;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.utils.Reflector;

/**
 * An aviator function wraps a class's static method.
 *
 * @since 4.2.2
 * @author dennis(killme2008@gmail.com)
 *
 */
public class ClassMethodFunction extends AbstractVariadicFunction {

  private Method method; // Only for one-arity function.
  private final String name;
  private final String methodName;
  private List<Method> methods; // For reflection.
  private final Class<?> clazz;
  private final boolean isStatic;

  public ClassMethodFunction(final Class<?> clazz, final boolean isStatic, final String name,
      final String methodName, final List<Method> methods)
      throws IllegalAccessException, NoSuchMethodException {
    this.name = name; // String.valueOf
    this.clazz = clazz; // String
    this.isStatic = isStatic; // true
    this.methodName = methodName;

    if (methods.size() == 1) {
      this.method = methods.get(0);
      if (this.method == null) {
        throw new NoSuchMethodException("Method handle for " + methodName + " not found");
      }
    } else {
      // Slow path by reflection
      this.methods = methods;
    }
  }


  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public AviatorObject variadicCall(final Map<String, Object> env, final AviatorObject... args) {
    Object[] jArgs = null;

    Object target = null;

    if (this.isStatic) {
      jArgs = new Object[args.length];
      for (int i = 0; i < args.length; i++) {
        jArgs[i] = args[i].getValue(env);
      }
    } else {
      if (args.length < 1) {
        throw new IllegalArgumentException("Class<" + this.clazz + "> instance method "
            + this.methodName + " needs at least one argument as instance.");
      }
      jArgs = new Object[args.length - 1];
      target = args[0].getValue(env);
      for (int i = 1; i < args.length; i++) {
        jArgs[i - 1] = args[i].getValue(env);
      }
    }

    if (this.method != null) {
      try {
        return FunctionUtils.wrapReturn(this.isStatic
            ? this.method.invoke(null, Reflector.boxArgs(this.method.getParameterTypes(), jArgs))
            : this.method.invoke(target,
                Reflector.boxArgs(this.method.getParameterTypes(), jArgs)));
      } catch (Throwable t) {
        throw Reflector.sneakyThrow(t);
      }
    } else {
      return FunctionUtils.wrapReturn(this.isStatic
          ? Reflector.invokeStaticMethod(this.clazz, this.methodName, this.methods, jArgs)
          : Reflector.invokeInstanceMethod(this.clazz, this.methodName, target, this.methods,
              jArgs));
    }
  }
}
