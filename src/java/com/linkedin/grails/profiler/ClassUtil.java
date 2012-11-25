package com.linkedin.grails.profiler;

public class ClassUtil {
    public static Class<?> getRealClass(Class<?> cls) {
        // strip off annoying cglib stuff
        if(cls.getName().contains("$$EnhancerByCGLIB")) {
            // superclass of that is what we're interested in
            System.out.println(cls);
            System.out.println(cls.getSuperclass());
            System.out.println(cls.getInterfaces());
            cls = cls.getSuperclass();
            System.out.println(cls);
        }
        return cls;
    }
}
