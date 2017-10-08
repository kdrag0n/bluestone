#!/bin/sh
java -Xms10M -Xmx500M -XX:+UseG1GC -server -XX:+CMSClassUnloadingEnabled -XX:+AggressiveOpts -XX:MaxGCPauseMillis=2500 -Xverify:none -cp build/classes/java/main:build/libs/bluestone-1.0-SNAPSHOT.jar:build/libs/bluestone-1.0-SNAPSHOT-all.jar -Dapple.awt.UIElement=true com.khronodragon.bluestone.Start
