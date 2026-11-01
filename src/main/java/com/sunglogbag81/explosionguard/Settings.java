package com.sunglogbag81.explosionguard;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

record Settings(long delayMillis,long lifetimeMillis,int restoreBudget,int explosionLimit,int pendingLimit,int particleLimit,int refreshBudget,int animationBudget) {
    static Settings defaults(){return new Settings(10000,30000,50,1000,10000,32,2,1);}
    static Settings load(Path file)throws IOException {
        var yaml=new YamlConfiguration();
        if(Files.exists(file)) {
            String text=Files.readString(file,StandardCharsets.UTF_8);
            try{yaml.loadFromString(text);}catch(org.bukkit.configuration.InvalidConfigurationException ignored){}
        }
        int delay=integer(yaml,"delay-seconds",10,1,60);
        int lifetime=integer(yaml,"max-mask-seconds",30,delay,300);
        int budget=integer(yaml,"restore-blocks-per-tick",50,1,1000);
        int perExplosion=integer(yaml,"max-blocks-per-explosion",1000,1,10000);
        int pending=integer(yaml,"max-pending-blocks",10000,1,100000);
        int particles=integer(yaml,"max-particle-blocks-per-explosion",32,0,256);
        int refresh=integer(yaml,"refresh-chunks-per-tick",2,1,16);
        int animation=integer(yaml,"animation-blocks-per-tick",1,1,50);
        String output=yaml.saveToString();
        if(!Files.exists(file)||!Files.readString(file).equals(output))atomic(file,output);
        return new Settings(delay*1000L,lifetime*1000L,budget,perExplosion,pending,particles,refresh,animation);
    }
    private static int integer(YamlConfiguration yaml,String path,int fallback,int min,int max) {
        Object raw=yaml.get(path);
        int value=raw instanceof Integer||raw instanceof Long?((Number)raw).intValue():fallback;
        if(raw instanceof Long n&&(n<Integer.MIN_VALUE||n>Integer.MAX_VALUE))value=fallback;
        value=Math.max(min,Math.min(max,value));yaml.set(path,value);return value;
    }
    private static void atomic(Path file,String text)throws IOException {
        Path target=file.toAbsolutePath();Files.createDirectories(target.getParent());
        Path temp=Files.createTempFile(target.getParent(),"explosionguard-",".tmp");
        try {
            Files.writeString(temp,text,StandardCharsets.UTF_8);
            try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ignored){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
}
