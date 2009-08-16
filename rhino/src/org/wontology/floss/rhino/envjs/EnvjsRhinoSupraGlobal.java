/*
 * This file is a component of env.js, 
 *     http://github.com/gleneivey/env-js/commits/master/README
 * a Pure JavaScript Browser Environment
 * Copyright 2009 John Resig, licensed under the MIT License
 *     http://www.opensource.org/licenses/mit-license.php
 */


package org.wontology.floss.rhino.envjs;

import java.util.*;
import java.lang.reflect.*;
import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.Environment;

public class EnvjsRhinoSupraGlobal extends Global
{
    public void init(Context cx)
    {
        // let the Rhino shell base class do its init
        super.init(cx);

        // now, we add the JavaScript methods we want to provide for env.js
        String[] names = {
            "createAGlobalObject",
            "getFunctionObjectsScope",
            "setFunctionObjectsScope",
            "configureFunctionObjectsScopeChain",
            "restoreScopeOfSetOfObjects",
            "load",                       // overrides ...shell.Global.load()
            // debug helper functions
//            "whereAmI",
//            "javaHashCode"
        };
        defineFunctionProperties(names, EnvjsRhinoSupraGlobal.class,
                                 ScriptableObject.DONTENUM);


        // defineFunctionProperties assigns the scope of new function objects
        //   to its caller ("this"), which isn't what we want.  So, find all
        //   of them and reassign their parent scope object.
        Object[] propIds = this.getAllIds();
        for (Object anId: propIds) {
            try {
                Scriptable aProp = (Scriptable) (this.get((String) anId, this));
                if (aProp.getClassName() == "Function")
                    aProp.setParentScope(EnvjsRhinoMain.global);
            }
            catch (ClassCastException ccExcept) {
                ; // ignore properties that don't cast to Scriptable
            }
        }
    }




    /* class methods intended to be called as JavaScript global functions */


    public static void load(Context cx, Scriptable thisObj,
                            Object[] args, Function funObj)
    {
        Global.load(cx, funObj.getParentScope(), args, funObj);
    }


    public static Scriptable createAGlobalObject(Context cx, Scriptable thisObj,
                                                 Object[] args, Function funObj)
    {
        Global gObj = (Global) (ScriptableObject.getTopLevelScope(funObj));
        Class c = EnvjsRhinoSupraGlobal.class;
        while (gObj != null && gObj.getClass() != c)
            gObj = (Global) gObj.getPrototype();
        if (gObj == null)
            throw new IllegalStateException(
                "EnvjsRhinoSupraGlobal.createAGlobalObject: couldn't find " +
                "our Global scope obj.");
        return new EnvjsRhinoGlobal(gObj);
    }


    public static Scriptable getFunctionObjectsScope(Context cx,
                                                     Scriptable thisObj,
                                                     Object[] args,
                                                     Function funObj)
    {
        if (args.length != 1)
            throw new IllegalArgumentException(
                "EnvjsRhinoSupraGlobal.getFunctionObjectsScope: wrong " +
                "argument count.");
        return ScriptableObject.getTopLevelScope((Function) args[0]);
    }

    public static void setFunctionObjectsScope(Context cx, Scriptable thisObj,
                                               Object[] args, Function funObj)
    {
        if (args.length != 2)
            throw new IllegalArgumentException(
                "EnvjsRhinoSupraGlobal.setFunctionObjectsScope: wrong " +
                "argument count.");
        // rely on Java to throw an exception if we can't do the casts we want
        //   instead of explicitly checking our argument types

        Function targetFn = (Function) args[0];
        Scriptable previousObj = targetFn;
        Scriptable currentObj = targetFn.getParentScope();
        Scriptable nextObj;
        while ((nextObj = currentObj.getParentScope()) != null) {
            previousObj = currentObj;
            currentObj = nextObj;
        }

        previousObj.setParentScope((Scriptable) args[1]);
    }


    public static NativeArray configureFunctionObjectsScopeChain(Context cx,
        Scriptable thisObj, Object[] args, Function funObj)
    {
        Object[] objectPair;
        List pairs = new ArrayList();

        // save original scope for our target function object
        Scriptable targetFn = (Scriptable) args[0];
        objectPair = new Object[] { targetFn, targetFn.getParentScope() };
        pairs.add(cx.newArray(funObj, objectPair));

        NativeArray argArray = (NativeArray) args[1];
        // change fn obj's scope to point to first element in array
        targetFn.setParentScope((Scriptable) argArray.get(0, thisObj));


        int c;
        long len = argArray.getLength();
        for (c=0; c < len-1; c++){
            // save original scopes from objects we're putting into new chain
            Scriptable elem = (Scriptable) argArray.get(c, thisObj);
	    objectPair = new Object[] { elem, elem.getParentScope() };
            pairs.add(cx.newArray(funObj, objectPair));

            // set current obj's scope to point to next object
            Scriptable scope = (Scriptable) argArray.get(c+1, thisObj);
            elem.setParentScope(scope);
        }

        // return original scope information for later restore
        return (NativeArray) cx.newArray(funObj, pairs.toArray());
    }


    public static void restoreScopeOfSetOfObjects(Context cx,
        Scriptable thisObj, Object[] args, Function funObj)
    {
        NativeArray jsPairs = (NativeArray) args[0];

        int c;
        long len = jsPairs.getLength();
        for (c=0; c < len; c++){
            NativeArray objAndItsScope = (NativeArray) jsPairs.get(c, thisObj);
            Scriptable anObj    = (Scriptable) objAndItsScope.get(0, thisObj);
            Scriptable oldScope = (Scriptable) objAndItsScope.get(1, thisObj);
            anObj.setParentScope(oldScope);
        }
    }


/*
    public static void whereAmI(Context cx, Scriptable thisObj, Object[] args,
                                Function funObj)
    {
        System.out.println("whereAmI : " + Context.toString(args[0]));
            ////////////
//        System.out.println(" this: " + thisObj.getClass().getName() +
//                             " (" + thisObj.hashCode() + ")");
//        if (args[1] != null)
//            System.out.println("   fn: " + args[1].getClass().getName() +
//                               " (" + args[1].hashCode() + ")");
            ////////////
        System.out.println("  scope:");
        Scriptable temp = thisObj;
        while (temp != null){
            System.out.println("    this " + temp.getClass().getName() +
                               " (" + temp.hashCode() + ")");
            temp = temp.getParentScope();
        }
        if (args[1] != null){
            temp = (Function) args[1];
            while (temp != null){
                System.out.println("    fun  " + temp.getClass().getName() +
                                   " (" + temp.hashCode() + ")");
                temp = temp.getParentScope();
            }
        }
        System.out.println("  prototypes:");
        temp = thisObj;
        while (temp != null){
            System.out.println("    this " + temp.getClass().getName() +
                               " (" + temp.hashCode() + ")");
            temp = temp.getPrototype();
        }
        if (args[1] != null){
            temp = (Function) args[1];
            while (temp != null){
                System.out.println("    fun  " + temp.getClass().getName() +
                                   " (" + temp.hashCode() + ")");
                temp = temp.getPrototype();
            }
        }
    }
    public static Integer javaHashCode(Context cx, Scriptable thisObj,
                                       Object[] args, Function funObj)
    {
        return new Integer(args[0].hashCode());
    }
*/
}