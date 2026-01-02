pluginManagement {
    val flutterSdkPath = run {
        val properties = java.util.Properties()
        file("local.properties").inputStream().use { properties.load(it) }
        val flutterSdkPath = properties.getProperty("flutter.sdk")
        require(flutterSdkPath != null) { "flutter.sdk not set in local.properties" }
        flutterSdkPath
    }

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
}

// 为所有子项目（包括插件）提供默认配置
gradle.projectsEvaluated {
    rootProject.allprojects.forEach { project ->
        if (project.hasProperty("android")) {
            try {
                val android = project.extensions.findByName("android")
                if (android != null) {
                    val androidClass = android.javaClass
                    val methods = androidClass.methods
                    
                    val setMethod = methods.find { 
                        it.name == "setCompileSdkVersion" && it.parameterCount == 1 && 
                        it.parameterTypes[0] == Int::class.java
                    } ?: methods.find { 
                        it.name == "setCompileSdk" && it.parameterCount == 1 && 
                        it.parameterTypes[0] == Int::class.java
                    }
                    
                    if (setMethod != null) {
                        val getMethod = methods.find { 
                            it.name == "getCompileSdkVersion" && it.parameterCount == 0
                        } ?: methods.find { 
                            it.name == "getCompileSdk" && it.parameterCount == 0
                        }
                        
                        val currentValue = getMethod?.invoke(android)
                        if (currentValue == null) {
                            setMethod.invoke(android, 34)
                        }
                    }
                }
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
}

include(":app")
