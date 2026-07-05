# JCL — Jar Class Loader

[![CI](https://github.com/connellite/jcl/actions/workflows/ci.yml/badge.svg)](https://github.com/connellite/jcl/actions/workflows/ci.yml)

JCL is a configurable, dynamic class loader that loads Java classes from JAR files, directories, URLs, and streams. It supports multiple isolated loaders in one application and integrates with XML-based context configuration.

This repository is a maintained fork of [kamranzafar/JCL](https://github.com/kamranzafar/JCL). The library is published as **`io.github.connellite:jcl-core`** and uses package **`io.github.connellite.jcl`**.

Licensed under [Apache License 2.0](LICENSE).

## Requirements

- Java **11** or later
- Maven 3.6+

## Project structure

```
jcl/
├── core/       # jcl-core — main library (JPMS module io.github.connellite.jcl)
└── test-jcl/   # test JAR used by unit tests
```

Legacy modules from upstream (`spring`, `web`, `test-web`, JCL v1) are not part of this fork.

## Build

```bash
mvn clean verify
```

This builds `test-jcl`, then `jcl-core`, runs unit tests, and produces:

- `core/target/jcl-core-*.jar`
- `test-jcl/target/test-jcl-*.jar`

## Maven dependency

```xml
<dependency>
    <groupId>io.github.connellite</groupId>
    <artifactId>jcl-core</artifactId>
    <version>3.0</version>
</dependency>
```

Runtime dependencies: `slf4j-api`, `objenesis`, `cglib-nodep`.

## Quick start

```java
import io.github.connellite.jcl.JarClassLoader;
import io.github.connellite.jcl.JclObjectFactory;

JarClassLoader jcl = new JarClassLoader();

// Load from paths, URLs, streams, or directories
jcl.add("mylib/myapp.jar");
jcl.add("mylib/");                    // recursive: jars, classes, resources
jcl.add(new URL("https://example.com/lib.jar"));
jcl.add(new FileInputStream("other.jar"));

JclObjectFactory factory = JclObjectFactory.getInstance();
Object obj = factory.create(jcl, "com.example.MyClass");
```

`JarClassLoader` also accepts sources in the constructor:

```java
JarClassLoader jcl = new JarClassLoader(new String[] { "lib/app.jar" });
```

## JCL context

To access a `JarClassLoader` globally, register it with `JclContext`:

```java
import io.github.connellite.jcl.context.DefaultContextLoader;
import io.github.connellite.jcl.context.JclContext;
import io.github.connellite.jcl.context.XmlContextLoader;

// Programmatic single loader
JarClassLoader jcl = new JarClassLoader();
jcl.add("lib/");
new DefaultContextLoader(jcl).loadContext();

JarClassLoader shared = JclContext.get();

// Or load from XML (see core/src/test/resources/jcl.xml)
new XmlContextLoader("classpath:jcl.xml").loadContext();
JarClassLoader named = JclContext.get("jcl1");
```

## Casting and proxies

Objects loaded in an isolated class loader cannot be cast directly to types from the application class loader. Use `JclUtils` or auto-proxying:

```java
import io.github.connellite.jcl.JclUtils;
import io.github.connellite.jcl.proxy.CglibProxyProvider;
import io.github.connellite.jcl.proxy.ProxyProviderFactory;

// Manual cast via JDK proxy
MyInterface api = JclUtils.cast(obj, MyInterface.class);

// Auto-proxy on every factory.create()
ProxyProviderFactory.setDefaultProxyProvider(new CglibProxyProvider());
JclObjectFactory factory = JclObjectFactory.getInstance(true);
MyInterface api = (MyInterface) factory.create(jcl, "com.example.Impl");

// Deep / shallow clone (reflection-based, no Serializable required)
Object copy = JclUtils.deepClone(obj);
```

Enable auto-proxy globally with:

```text
-Djcl.autoProxy=true
```

## Class loading order and custom loaders

By default, JCL consults delegate loaders in order (lower `order` first):

| Loader        | Default order | Default enabled |
|---------------|---------------|-----------------|
| Local (JCL)   | 10            | yes             |
| Current       | 5             | yes             |
| Parent        | 5             | yes             |
| Thread context| 5             | no              |
| System        | 5             | yes             |
| OSGi boot     | —             | no              |

```java
jcl.getLocalLoader().setOrder(1);
jcl.getSystemLoader().setOrder(2);
jcl.getParentLoader().setEnabled(false);

jcl.addLoader(new MyLoader());  // extend ProxyClassLoader
```

Disable a built-in loader via system property (fully qualified inner class name):

```text
-Dio.github.connellite.jcl.AbstractClassLoader$ParentLoader=false
```

## OSGi boot delegation

```text
-Dosgi.bootdelegation=true
-Dorg.osgi.framework.bootdelegation=com.example.shared.*
-Dosgi.bootdelegation.strict=true
```

Or configure in XML under `<loader name="jcl.bootosgi">` (see test `jcl.xml`).

## Configuration (system properties)

| Property | Default | Description |
|----------|---------|-------------|
| `jcl.suppressCollisionException` | `true` | Ignore duplicate class/resource entries |
| `jcl.suppressMissingResourceException` | `true` | Ignore missing source paths |
| `jcl.autoProxy` | `false` | Auto-create castable proxies in `JclObjectFactory` |
| `osgi.bootdelegation` | `false` | Enable OSGi boot delegation loader |
| `osgi.bootdelegation.strict` | `true` | Fail if class not found in parent during boot delegation |
| `org.osgi.framework.bootdelegation` | — | Comma-separated packages/classes for boot delegation |

Per-loader enable flags use the loader class name as the property key.

## JPMS

`jcl-core` ships as a Java module:

```java
module my.app {
    requires io.github.connellite.jcl;
}
```

Exported packages: `io.github.connellite.jcl`, `.context`, `.proxy`, `.cloner`, `.exception`, `.utils`.

When using CGLIB or reflection-heavy features on the module path, you may need `--add-opens` (the project's tests use opens on `java.lang` and `java.lang.reflect`).

## What's changed in this fork

- Package rename: `org.xeustechnologies.jcl` → `io.github.connellite.jcl`
- Java 11 baseline with `module-info.java`
- Core-only build (`spring` / `web` modules removed)
- Built-in object cloning (`ReflectionCloning`) — no external `object-cloner` dependency
- `package-info.class` support for package-level annotations
- Thread-safe class loading (concurrent loader registry, per-class locks)

## Credits

Original library by [Kamran Zafar](https://github.com/kamranzafar). See [LICENSE](LICENSE) for copyright and license terms.
