package com.linkedin.grails.profiler;

public class ClassUtil {
    public static Class<?> getRealClass(Class<?> cls) {
        // strip off annoying cglib stuff
        if(cls.getName().contains("$$EnhancerByCGLIB")) {
            // superclass of that is what we're interested in
            cls = cls.getSuperclass();
        }
        return cls;
    }
}
