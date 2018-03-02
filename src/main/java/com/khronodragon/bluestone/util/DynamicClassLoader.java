package com.khronodragon.bluestone.util;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

class DynamicClassLoader extends ClassLoader {
    private static final ClassLoader parent = DynamicClassLoader.class.getClassLoader();
    private final Set<String> loadedClasses = new HashSet<>();
    private final Set<String> unavailableClasses = new HashSet<>();
    private Function<String, byte[]> loader;

    private DynamicClassLoader(String... paths) {
        for (String strPath: paths) {
            Path path = Paths.get(strPath);

            loader = getLoader(path);
            if (loader == null) {
                throw new RuntimeException(path + " does not exist");
            }
        }
    }

    private static Function<String, byte[]> getLoader(Path path) {
        if (!Files.exists(path)) {
            return null;
        } else if (Files.isDirectory(path)) {
            return getDirLoader(path);
        } else {
            try {
                JarFile jarFile = new JarFile(new File(path.toUri()));

                return getJarLoader(jarFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Function<String, byte[]> getDirLoader(Path baseDir) {
        return path -> {
            Path file = Paths.get(baseDir.toString(), path);

            try {
                return Files.readAllBytes(file);
            } catch (FileNotFoundException|NoSuchFileException ignored) {
                return null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Function<String, byte[]> getJarLoader(JarFile jarFile) {
        return path -> {
            ZipEntry entry = jarFile.getJarEntry(path);
            if (entry == null) {
                return null;
            }

            InputStream inputStream = null;
            try {
                inputStream = jarFile.getInputStream(entry);

                return IOUtils.toByteArray(inputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
        };
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (loadedClasses.contains(name) || unavailableClasses.contains(name)) {
            return super.loadClass(name); // Use default ClassLoader cache
        }

        byte[] newClassData = loader.apply(toFilePath(name));
        if (newClassData != null) {
            loadedClasses.add(name);
            return loadClass(newClassData, name);
        } else {
            unavailableClasses.add(name);
            return parent.loadClass(name);
        }
    }

    private Class<?> loadClass(byte[] classData, String name) {
        Class<?> clazz = defineClass(name, classData, 0, classData.length);

        if (clazz != null) {
            if (clazz.getPackage() == null) {
                definePackage(name.replaceAll("\\.\\w+$", ""),
                        null, null, null, null, null, null, null);
            }

            resolveClass(clazz);
        }
        throw new RuntimeException();
//        return clazz;
    }

    private static String toFilePath(String name) {
        return name.replaceAll("\\.", "/") + ".class";
    }
}
