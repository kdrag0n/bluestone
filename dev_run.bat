@echo off
java -Xms10M -Xmx1G -XX:+UseG1GC -server -XX:+CMSClassUnloadingEnabled -XX:+AggressiveOpts -XX:MaxGCPauseMillis=2500 -Xverify:none -cp build/classes/java/main;build/libs/bluestone-1.0-SNAPSHOT.jar;build/libs/bluestone-1.0-SNAPSHOT-all.jar com.khronodragon.bluestone.Start
