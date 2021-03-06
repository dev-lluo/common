package top.flyfire.common.reflect;

import top.flyfire.common.*;
import top.flyfire.common.multhread.ThreadProxy;
import top.flyfire.common.reflect.metainfo.*;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by flyfire[dev.lluo@outlook.com] on 2016/4/27.
 */
public class ReflectUtils {
    public final static boolean isJdkDataType(Class clzz){
        return ObjectUtils.isNotNull(clzz)&&(isJdkPrimitiveType(clzz)||String.class.isAssignableFrom(clzz)||Date.class.isAssignableFrom(clzz)|| Collection.class.isAssignableFrom(clzz)|| Map.class.isAssignableFrom(clzz));
    }

    public final static boolean isJdkPrimitiveType(Class clzz){
        return ObjectUtils.isNotNull(clzz)&&(clzz.isPrimitive()||Number.class.isAssignableFrom(clzz)|| Boolean.class.isAssignableFrom(clzz));
    }

    public final static boolean isInterface(Class clzz){
        return ObjectUtils.isNotNull(clzz)&&clzz.isInterface();
    }

    public final static Object newInstance(Class clzz){
        try {
            return clzz.newInstance();
        }catch (ReflectiveOperationException e){
            return new ReflectiveException(e);
        }
    }

    private static final ThreadProxy<Type,MetaInfo> META_INFO_CACHED = new ThreadProxy<Type,MetaInfo>(){

        @Override
        protected void init() {

        }

        @Override
        protected MetaInfo proxy(Type type) {
            MetaInfo metaInfo = $proxy(type);
            metaInfo.initialize();
            return metaInfo;
        }

        private MetaInfo $proxy(Type type){
            if(type instanceof Class){
                Class<?> clzz = (Class<?>) type;
                if(clzz.isArray()){
                    return new ArrayMetaInfo(getMetaInfo(clzz.getComponentType()));
                }else {
                    return getMetaInfo(clzz);
                }
            }else if(type instanceof ParameterizedType){
                ParameterizedType parameterizedType = (ParameterizedType) type;
                return new ParameterizeMetaInfo(getMetaInfo(parameterizedType.getActualTypeArguments()), getMetaInfo(parameterizedType.getRawType()), getMetaInfo(parameterizedType.getOwnerType()));
            }else if(type instanceof WildcardType){
                WildcardType wildcardType = ((WildcardType) type);
                Type[] upperBounds = wildcardType.getUpperBounds();
                Type[] lowerBounds = wildcardType.getLowerBounds();
                return new WildcardMetaInfo(ArrayUtils.isEmpty(upperBounds)?MetaInfo.NULL: getMetaInfo(upperBounds[0]),ArrayUtils.isEmpty(lowerBounds)?MetaInfo.NULL: getMetaInfo(lowerBounds[0]));
            }else if(type instanceof GenericArrayType){
                return new ArrayMetaInfo(getMetaInfo(((GenericArrayType) type).getGenericComponentType()));
            }else if(type instanceof TypeVariable){
                TypeVariable typeVariable = (TypeVariable) type;
                Type[] bounds = typeVariable.getBounds();
                return new VariableMetaInfo(typeVariable.getName(),ArrayUtils.isEmpty(bounds)?MetaInfo.NULL: getMetaInfo(bounds[0]),typeVariable.getGenericDeclaration());
            }else if(type instanceof MetaInfo){
                return (MetaInfo) type;
            }else{
                throw ExceptionMsgPool.ReflectUtils.unWrap$Type$;
            }
        }

    };

    public final static MetaInfo getMetaInfo(Type type){
        if(ObjectUtils.isNull(type))
            return null;
        return META_INFO_CACHED.get(type);
    }

    public final static MetaInfo[] getMetaInfo(final Type[] types){
        final MetaInfo[] metaInfos = new MetaInfo[types.length];
        LoopUtils.forEach(types, new LoopUtils.ArrItemProxy() {
            public void proxy(int index, Object value, Object[] arr) {
                metaInfos[index] = getMetaInfo(types[index]);
            }
        });
        return metaInfos;
    }

    public final static boolean metaInfoArrEquals(MetaInfo[] aBounds,MetaInfo[] bBounds){
        if(ObjectUtils.isNotNull(aBounds,bBounds)&&aBounds.length==bBounds.length){
            for(int i = 0;i<aBounds.length;i++){
                if(!aBounds[i].equals(bBounds[i])) return false;
            }
            return true;
        } else if(ObjectUtils.isNull(aBounds,bBounds)){
            return true;
        }else{
            return false;
        }
    }

    private final static ClassMetaInfo getMetaInfo(Class<?> clzz){
        ClassMetaInfo classMetaInfo = new ClassMetaInfo(clzz, getMetaInfo(clzz.getTypeParameters()));
        Field[] fields;
        Method[] methods,result = new Method[2];
        Map<String,Method> methodMap = new HashMap<String,Method>();
        int getter = 0,setter = 1;
        String fieldName,methodName;
        syntax:{
            prepared:
            {
                fields = clzz.getDeclaredFields();
                methods = clzz.getDeclaredMethods();
                for (int i = 0; i < methods.length; i++) {
                    methodName=methods[i].getName();
                        methodMap.put(methodName, methods[i]);
                }

            }
            buildField:
            {
                for(int i = 0;i<fields.length;i++){
                    fieldName = fields[i].getName();
                    if(hasAccess(fieldName,methodMap,result)){
                        classMetaInfo.setFieldMetaInfo(fieldName,new FieldMetaInfo(fieldName,fields[i], getMetaInfo(fields[i].getGenericType()),result[getter],result[setter]));
                    }
                }
            }
            buildSuper:
            {
                Type superType = getMetaInfo(clzz.getGenericSuperclass());
                if(null==superType){
                    break syntax;
                }else if(superType instanceof ClassMetaInfo){
                    classMetaInfo.extendSuper((ClassMetaInfo)superType);
                }else if(superType instanceof ParameterizeMetaInfo){
                    classMetaInfo.extendSuper((ParameterizeMetaInfo)superType);
                }else{
                    throw new ReflectiveSyntaxException("[A ClassMetaInfo or TypeParameterized is expected in the buidlSuper , but superType is of type "+superType+" .]");
                }
            }
        }
        return classMetaInfo;
    }

    private static boolean hasAccess(String name,Map<String,Method> methodMap,Method[] result){
        String _gname,_sname;
        int getter = 0,setter = 1;
        boolean has = true;
        build:
        {
            format:
            {
                char[] nameChars = name.toCharArray(), gnameChars = new char[nameChars.length + 3], snameChars = new char[nameChars.length + 3];
                nameChars[0] = CharUtils.toUpper(nameChars[0]);
                System.arraycopy(nameChars, 0, gnameChars, 3, nameChars.length);
                System.arraycopy(nameChars, 0, snameChars, 3, nameChars.length);
                gnameChars[0] = 'g';
                snameChars[0] = 's';
                gnameChars[1] = snameChars[1] = 'e';
                gnameChars[2] = snameChars[2] = 't';
                _gname = new String(gnameChars);
                _sname = new String(snameChars);
            }
            findGetter:
            {
                if (null==(result[getter]=methodMap.get(_gname))){
                    has = false;
                    break build;
                }
                int modifiers = result[getter].getModifiers();
                if(Modifier.isAbstract(modifiers)||!Modifier.isPublic(modifiers)){
                    has = false;
                    break build;
                }
            }
            findSetter:{
                if (null==(result[setter]=methodMap.get(_sname))){
                    has = false;
                    break build;
                }
                int modifiers = result[getter].getModifiers();
                if(Modifier.isAbstract(modifiers)||!Modifier.isPublic(modifiers)){
                    has = false;
                    break build;
                }
            }
        }
        return has;
    }
}
