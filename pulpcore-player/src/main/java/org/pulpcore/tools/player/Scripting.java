/*
    Copyright (c) 2007-2010, Interactive Pulp, LLC
    All rights reserved.
    
    Redistribution and use in source and binary forms, with or without 
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright 
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright 
          notice, this list of conditions and the following disclaimer in the 
          documentation and/or other materials provided with the distribution.
        * Neither the name of Interactive Pulp, LLC nor the names of its 
          contributors may be used to endorse or promote products derived from 
          this software without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
    POSSIBILITY OF SUCH DAMAGE.
*/

package org.pulpcore.tools.player;

import java.lang.reflect.Method;

/**
    Allows calls to PulpCore code.
    Since the app exists in a seperate ClassLoader that isn't a parent of Player's ClassLoader,
    PulpCore app code has to be invoked via reflection.
    TODO: Once Java 6 is on the Mac, explore doing scripting via JavaScript.
*/
public class Scripting {
    
    private final Object applet;
    private final ClassLoader classLoader;
    private final Method invokeLater;
    private final Method invokeAndWait;
    
    public Scripting(Object applet) {
        this.applet = applet;
        this.classLoader = applet.getClass().getClassLoader();
        this.invokeLater = getMethod("pulpcore.platform.applet.CoreApplet",
            "invokeLater", Runnable.class);
        this.invokeAndWait = getMethod("pulpcore.platform.applet.CoreApplet",
            "invokeAndWait", Runnable.class);
    }
    
    public Object invoke(String methodName) {
        Method method = getMethod("pulpcore.platform.applet.CoreApplet", methodName);
        try {
            return method.invoke(applet);
        }
        catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public Object invoke(String methodName, Class[] argsClasses, Object[] args) {
        Method method = getMethod("pulpcore.platform.applet.CoreApplet", methodName, argsClasses);
        try {
            return method.invoke(applet, args);
        }
        catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
    
    public Object invokeInAnimationThread(String methodName) {
        Method method = getMethod("pulpcore.platform.applet.CoreApplet", methodName);
        Runnable code = createMethod(applet, method);
        return invokeAndWait(code);
    }
    
    public Object invokeStatic(String className, String methodName) {
        Method method = getMethod(className, methodName);
        try {
            return method.invoke(null);
        }
        catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
    
    public Object invokeStaticInAnimationThread(String className, String methodName) {
        Method method = getMethod(className, methodName);
        Runnable code = createMethod(null, method);
        return invokeAndWait(code);
    }
    
    private Class getClass(String className) {
        try {
            return classLoader.loadClass(className);
        }
        catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
    
    public Method getMethod(String className, String methodName, Class... args) {
        Class c = getClass(className);
        Method method = null;
        if (c != null) {
            try {
                method = c.getMethod(methodName, args);
            }
            catch (NoSuchMethodException e) {
                try {
                    method = c.getDeclaredMethod(methodName, args);
                }
                catch (NoSuchMethodException e2) { 
                    e2.printStackTrace();
                    return null;
                }
            }
            
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
        }
        return method;
    }

    private Runnable createMethod(Object object, Method method, Object... args) {
        return new Code(object, method, args);
    }
    
    private void invokeLater(Runnable runnable) {
        try {
            invokeLater.invoke(applet, runnable);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    private Object invokeAndWait(Runnable runnable) {
        try {
            invokeAndWait.invoke(applet, runnable);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
        if (runnable instanceof Code) {
            return ((Code)runnable).getReturnValue();
        }
        else {
            return null;
        }
    }
    
    private static class Code implements Runnable {
        private final Object object;
        private final Method method;
        private final Object[] args;
        private Object returnValue;
        
        private Code(Object object, Method method, Object... args) {
            this.object = object;
            this.method = method;
            this.args = args;
        }
        
        public Object getReturnValue() {
            return returnValue;
        }
        
        public void run() {
            if (method != null) {
                returnValue = null;
                try {
                    returnValue = method.invoke(object, args);
                }
                catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
}