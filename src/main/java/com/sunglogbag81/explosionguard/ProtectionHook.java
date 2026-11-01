package com.sunglogbag81.explosionguard;

import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Method;

/** Optional API linkage; unavailable or failed protection checks preserve the original display. */
final class ProtectionHook {
    private final JavaPlugin owner;
    private final Plugin blockLocker;
    private final Method protectedMethod;
    private boolean warned;
    ProtectionHook(JavaPlugin owner) {
        this.owner=owner;blockLocker=owner.getServer().getPluginManager().getPlugin("BlockLocker");
        Method method=null;
        if(blockLocker!=null)try {
            var api=Class.forName("nl.rutgerkok.blocklocker.BlockLockerAPIv2",true,blockLocker.getClass().getClassLoader());
            method=api.getMethod("isProtected",Block.class);
        }catch(ReflectiveOperationException e){warn(e);}
        protectedMethod=method;
    }
    boolean protectedBlock(Block block) {
        if(blockLocker==null)return false;
        if(!blockLocker.isEnabled()||protectedMethod==null)return true;
        try{return Boolean.TRUE.equals(protectedMethod.invoke(null,block));}
        catch(ReflectiveOperationException|RuntimeException e){warn(e);return true;}
    }
    private void warn(Exception error) {
        if(warned)return;warned=true;
        owner.getLogger().warning("BlockLocker 보호 확인 실패: 원본 블록 표시를 유지합니다. "+error.getMessage());
    }
}
