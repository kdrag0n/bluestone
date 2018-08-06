# Bluestone Java Discord bot: ProGuard rules

# Library
-libraryjars <java.home>/lib/rt.jar
-libraryjars <java.home>/lib/jce.jar

# Optimization
-optimizations *
-optimizationpasses 5
-allowaccessmodification

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Mappings
-printmapping mapping.txt

# Reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Enums, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Stack traces
-keepattributes SourceFile,LineNumberTable

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform

# META-INF/versions/9: Java 9 alternate classes
-ignorewarnings

# Main class
-keep class com.kdragon.bluestone.Start { *; }

# Log4j
-keep class org.apache.logging.log4j.** { *; }
-dontwarn org.apache.logging.slf4j.EventDataConverter # SLF4J extension: EventData
-dontwarn org.apache.logging.log4j.util.** # OSGI, Java 10 ObjectInputFilter
-dontwarn org.apache.logging.log4j.core.** # Commons CSV, OSGI, JavaX mail, JANSI, disruptor, Jackson XML, queues, concurrency, Apacke Kafka, ZeroMQ, Commons Compression, JavaX JMS, JavaX servlet
-dontwarn org.apache.commons.logging.impl.** # Apache Avalon, old Log4j, old Log, JavaX servlet

# SLF4J
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.MDC # MDC binder
-dontwarn org.slf4j.MarkerFactory # Marker binder

# Antlr
-dontwarn org.antlr.runtime.tree.DOTTreeGenerator # Antlr string template library

# Ical4j
-dontwarn net.fortuna.ical4j.model.ContentBuilder # Groovy utilities

# Javassist
-dontwarn javassist.util.HotSwapper* # Sun JDI JVM utilities

# JSR-223 scripting engines
-keep class org.codehaus.groovy.jsr223.GroovyScriptEngineFactory { *; }
-keep class org.luaj.vm2.script.LuaScriptEngineFactory { *; }

# Sentry
-keep class io.sentry.** { *; }
-dontwarn io.sentry.servlet.SentryServlet* # JavaX servlet
-dontwarn io.sentry.event.interfaces.HttpInterface # JavaX servlet
-dontwarn io.sentry.event.helper.** # JavaX servlet HTTP request

# Jackson (indirect dependency)
-keep class com.fasterxml.jackson.core.JsonFactory { *; }
-keep class com.fasterxml.jackson.databind.ObjectMapper { *; }

# JDBC
-keep class org.h2.Driver { *; }
-keep class com.mysql.cj.jdbc.Driver { *; }

# Groovy
-keep class groovy.grape.GrabAnnotationTransformation { *; }
-keep class org.codehaus.groovy.ast.builder.AstBuilderTransformation { *; }
-keep class org.codehaus.groovy.jsr223.** { *; }
-dontwarn org.codehaus.groovy.control.XStreamUtils # XStream
-dontwarn org.codehaus.groovy.tools.** # Apache Ivy, JANSI
-dontwarn groovyjar*.** # ICU, stringtemplate, Abego Treelayout
-dontwarn groovy.grape.** # Apache Ivy
-dontwarn org.apache.groovy.internal.util.ReevaluatingReference # MethodHandle#invokeExact()
-dontoptimize

# PrettyTime
-keep class org.ocpsoft.prettytime.PrettyTime { *; }

# MySQL
-keep class com.mysql.cj.** { *; }
-dontwarn com.mysql.cj.jdbc.integration.** # C3P0, JBoss

# ORM models
-keep class com.kdragon.bluestone.sql.** { *; }

# HikariCP
-keep class com.zaxxer.hikari.** { *; }
-dontwarn com.zaxxer.hikari.pool.HikariPool # Codehale & Micrometer metrics
-dontwarn com.zaxxer.hikari.metrics.** # Prometheus, Micrometer, Dropwizard metrics
-dontwarn com.zaxxer.hikari.hibernate.** # Hibernate ORM
-dontwarn com.zaxxer.hikari.HikariConfig # Codehale Dropwizard metrics

# ORMLite
-keepnames class com.j256.ormlite.table.TableUtils
-keepnames class com.j256.ormlite.table.DatabaseTableConfig
-keep class com.j256.ormlite.logger.** { *; }
-dontwarn com.j256.ormlite.misc.JavaxPersistenceImpl # JavaX persistence annotations
-dontwarn com.j256.ormlite.logger.Log4jLog # Old Log4j

# Serializable
-keepnames class * implements java.io.Serializable

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Cogs
-keep class com.kdragon.bluestone.cogs.** { *; }
-dontwarn com.kdragon.bluestone.cogs.ReplCog # Nashorn JS engine

# Reflections
-dontwarn org.reflections.** # Dom4J

# LuaJ
-dontwarn org.luaj.vm2.luajc.** # Apache bcel

# H2 Database
-dontwarn org.h2.** # OSGI, locationtech jts, javax servlet, Apache Lucene, etc

# ImageIO
-dontwarn com.twelvemonkeys.image.Magick* # ImageMagick bindings