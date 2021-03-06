package org.apache.ibatis.binding;

import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class MapperProxy implements InvocationHandler
{

    private static final Set<String> OBJECT_METHODS = new HashSet<String>()
    {
        {
            add("toString");
            add("getClass");
            add("hashCode");
            add("equals");
            add("wait");
            add("notify");
            add("notifyAll");
        }
    };

    private SqlSession sqlSession;

    private <T> MapperProxy(SqlSession sqlSession)
    {
        this.sqlSession = sqlSession;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        try
        {
            if (!OBJECT_METHODS.contains(method.getName()))
            {
                final Class declaringInterface = findDeclaringInterface(proxy, method);
                final MapperMethod mapperMethod = new MapperMethod(declaringInterface, method, sqlSession);
                final Object result = mapperMethod.execute(args);
                if (result == null && method.getReturnType().isPrimitive())
                {
                    throw new BindingException("Mapper method '" + method.getName() + "' (" + method.getDeclaringClass() + ") attempted to return null from a method with a primitive return type ("
                            + method.getReturnType() + ").");
                }
                return result;
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private Class findDeclaringInterface(Object proxy, Method method)
    {
        Class declaringInterface = null;
        for (Class iface : proxy.getClass().getInterfaces())
        {
            try
            {
                Method m = iface.getMethod(method.getName(), method.getParameterTypes());
                if (declaringInterface != null)
                {
                    throw new BindingException("Ambiguous method mapping.  Two mapper interfaces contain the identical method signature for " + method);
                }
                else if (m != null)
                {
                    declaringInterface = iface;
                }
            }
            catch (Exception e)
            {
                // Intentionally ignore.
                // This is using exceptions for flow control,
                // but it's definitely faster.
            }
        }
        if (declaringInterface == null)
        {
            throw new BindingException("Could not find interface with the given method " + method);
        }
        return declaringInterface;
    }

    public static <T> T newMapperProxy(Class<T> mapperInterface, SqlSession sqlSession)
    {
        ClassLoader classLoader = mapperInterface.getClassLoader();
        Class[] interfaces = new Class[] { mapperInterface };
        MapperProxy proxy = new MapperProxy(sqlSession);
        return (T) Proxy.newProxyInstance(classLoader, interfaces, proxy);
    }

}
