package com.sunglogbag81.explosionguard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class SettingsTest {
    @TempDir Path dir;
    @Test void missingFileCreatesDefaultsWithoutBackups()throws Exception {
        var settings=Settings.load(dir.resolve("config.yml"));assertEquals(10000,settings.delayMillis());assertEquals(50,settings.restoreBudget());
        assertEquals(Settings.defaults(),settings);
        try(var files=Files.list(dir)){assertEquals(1,files.count());}
    }
    @Test void deletedFileStartsFromDefaultsInsteadOfPreviousValues()throws Exception {
        Path file=dir.resolve("config.yml");Files.writeString(file,"delay-seconds: 2\n");assertEquals(2000,Settings.load(file).delayMillis());
        Files.delete(file);assertEquals(10000,Settings.load(file).delayMillis());
    }
    @Test void malformedYamlIsInitializedWithoutTouchingOtherFiles()throws Exception {
        Path file=dir.resolve("config.yml"),other=dir.resolve("keep.yml");Files.writeString(file,"delay-seconds: [broken");Files.writeString(other,"keep-me");
        assertEquals(10000,Settings.load(file).delayMillis());assertEquals("keep-me",Files.readString(other));
        try(var files=Files.list(dir)){assertEquals(2,files.count());}
    }
    @Test void invalidValuesAreBoundedAndLifetimeCannotBeShorterThanDelay()throws Exception {
        Path file=dir.resolve("config.yml");Files.writeString(file,"delay-seconds: 60\nmax-mask-seconds: 1\nrestore-blocks-per-tick: -1\nmax-pending-blocks: 9223372036854775807\nmax-particle-blocks-per-explosion: 0\n");
        var settings=Settings.load(file);assertEquals(60000,settings.lifetimeMillis());assertEquals(1,settings.restoreBudget());assertEquals(10000,settings.pendingLimit());assertEquals(0,settings.particleLimit());
    }
    @Test void preservesValidCustomSettingsAndUnknownKeys()throws Exception {
        Path file=dir.resolve("config.yml");Files.writeString(file,"delay-seconds: 3\ncustom-note: keep\n");
        assertEquals(3000,Settings.load(file).delayMillis());assertTrue(Files.readString(file).contains("custom-note: keep"));
    }
    @Test void upgradeAddsSingleBlockAnimationWithoutOverwritingOldBudget()throws Exception {
        Path file=dir.resolve("config.yml");Files.writeString(file,"restore-blocks-per-tick: 50\n");
        var settings=Settings.load(file);assertEquals(1,settings.animationBudget());assertEquals(50,settings.restoreBudget());assertEquals(2,settings.refreshBudget());
        assertTrue(Files.readString(file).contains("animation-blocks-per-tick: 1"));
    }
}
