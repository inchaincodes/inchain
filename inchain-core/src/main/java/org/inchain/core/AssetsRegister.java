package org.inchain.core;

import java.util.List;

/**
 * 资产注册信息
 * Created by v.chou on 2017/7/10.
 */
public class AssetsRegister {
    //资产注册信息
    private List<AssetsRegKeyValue> contents;

    public AssetsRegister(List<AssetsRegKeyValue> contents) {
        this.contents = contents;
    }


}
