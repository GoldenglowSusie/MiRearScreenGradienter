// 应用 init.gradle 脚本为所有插件提供默认配置
apply(from = "init.gradle")

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory = rootProject.layout.buildDirectory.dir("../../build").get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
    
    // 为所有子项目（包括插件）提供默认的 compileSdk 配置
    // 使用 afterEvaluate 确保在插件 build.gradle 评估后设置
    afterEvaluate {
        if (project.hasProperty("android")) {
            try {
                val android = project.extensions.getByName("android")
                val androidClass = android.javaClass
                val methods = androidClass.methods
                
                // 查找 setCompileSdkVersion 或 setCompileSdk 方法
                val setMethod = methods.find { 
                    it.name == "setCompileSdkVersion" && it.parameterCount == 1 && 
                    it.parameterTypes[0] == Int::class.java
                } ?: methods.find { 
                    it.name == "setCompileSdk" && it.parameterCount == 1 && 
                    it.parameterTypes[0] == Int::class.java
                }
                
                if (setMethod != null) {
                    // 检查当前值
                    val getMethod = methods.find { 
                        it.name == "getCompileSdkVersion" && it.parameterCount == 0
                    } ?: methods.find { 
                        it.name == "getCompileSdk" && it.parameterCount == 0
                    }
                    
                    val currentValue = getMethod?.invoke(android)
                    if (currentValue == null) {
                        // 设置默认值
                        setMethod.invoke(android, 34)
                    }
                }
            } catch (e: Exception) {
                // 忽略配置错误
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
