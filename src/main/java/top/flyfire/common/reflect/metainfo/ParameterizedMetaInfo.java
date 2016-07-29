package top.flyfire.common.reflect.metainfo;

import top.flyfire.common.reflect.*;

import java.lang.reflect.Type;

/**
 * Created by flyfire[dev.lluo@outlook.com] on 2016/5/31.
 */
public class ParameterizedMetaInfo extends MetaInfo implements GenericTypeAdapted ,Instanceable {

    private final MetaInfo[] actualTypeArguments;

    private final MetaInfo rawType;

    private final MetaInfo ownerType;

    public ParameterizedMetaInfo(MetaInfo[] actualTypeArguments, MetaInfo rawType, MetaInfo ownerType) {
        this.actualTypeArguments = actualTypeArguments;
        this.rawType = rawType;
        this.ownerType = ownerType;
    }

    @Override
    protected String buildTypeName() {
        if(this.actualTypeArguments==null||this.actualTypeArguments.length==0){
            return this.rawType.getTypeName();
        }else{
            StringBuilder toString = new StringBuilder(this.rawType.getTypeName());
            toString.append('<').append(this.actualTypeArguments[0].getTypeName());
            for(int i =1;i<this.actualTypeArguments.length;i++){
                toString.append(',').append(this.actualTypeArguments[i].getTypeName());
            }
            toString.append('>');
            return toString.toString();
        }
    }

    @Override
    public boolean compatible(Type type) {
        return false;
    }

    public MetaInfo[] getActualTypeArguments() {
        return actualTypeArguments;
    }

    public MetaInfo getRawType() {
        return rawType;
    }

    public MetaInfo getOwnerType() {
        return ownerType;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj==this){
            return true;
        }else if(obj instanceof ParameterizedMetaInfo){
            ParameterizedMetaInfo other = ((ParameterizedMetaInfo) obj);
            return rawType.equals(other.rawType)&&ownerType.equals(other.ownerType)&&ReflectUtils.metaInfoArrEquals(actualTypeArguments,other.actualTypeArguments);
        }else{
            return false;
        }
    }

    public MetaInfo adapt(MetaInfo[] variableMetaInfos, MetaInfo[] typeStore) {
        MetaInfo[] actualTypeArguments = new MetaInfo[this.actualTypeArguments.length];
        ParameterizedMetaInfo parameterizedMetaInfo = new ParameterizedMetaInfo(actualTypeArguments,rawType,ownerType);
        for(int i = 0;i<actualTypeArguments.length;i++){
            if(this.actualTypeArguments[i] instanceof GenericTypeAdapted)
                actualTypeArguments[i] = ((GenericTypeAdapted) this.actualTypeArguments[i]).adapt(variableMetaInfos,typeStore);
            else{
                actualTypeArguments[i] = this.actualTypeArguments[i];
            }
        }
        return parameterizedMetaInfo;
    }

    public Object newInstance() {
        if(rawType instanceof  Instanceable){
            return ((Instanceable) rawType).newInstance();
        }else{
            throw new ReflectiveException(rawType+"is not implements Instanceable...");
        }
    }
}
