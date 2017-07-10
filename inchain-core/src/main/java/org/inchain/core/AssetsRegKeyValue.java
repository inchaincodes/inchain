package org.inchain.core;

import java.io.UnsupportedEncodingException;

/**
 * Created by v.chou on 2017/7/10.
 */
public class AssetsRegKeyValue extends KeyValue {

    public final static AssetsRegKeyValue NAME = new AssetsRegKeyValue("name", "资产名称");
    public final static AssetsRegKeyValue DESCRIPTION = new AssetsRegKeyValue("description", "资产描述");
    public final static AssetsRegKeyValue CODE = new AssetsRegKeyValue("code", "资产代号");
    public final static AssetsRegKeyValue LOGO = new AssetsRegKeyValue("logo", "资产图标");
    public final static AssetsRegKeyValue REMARK = new AssetsRegKeyValue("remark", "资产描述");

    public AssetsRegKeyValue(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public AssetsRegKeyValue(String code, String name, byte[] value) {
        this.code = code;
        this.name = name;
        this.value = value;
    }

    public AssetsRegKeyValue(String code, String name, String value) {
        this.code = code;
        this.name = name;
        try {
            this.value = value.getBytes(CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
